package com.aispeech.lite.sspe;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.DUILiteConfig;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AudioFileWriter;
import com.aispeech.common.FileSaveUtil;
import com.aispeech.common.FileUtil;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.kernel.LiteSoFunction;
import com.aispeech.kernel.Sspe;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.AIWakeupProcessor;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.BuiltinWakeupWords;
import com.aispeech.lite.config.LocalSignalProcessingConfig;
import com.aispeech.lite.fespx.FespxKernelListener;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.BuiltinE2EWakeupWords;
import com.aispeech.lite.param.SignalProcessingParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class BaseSspeKernel extends BaseKernel implements AIWakeupProcessor.WakeupProcessorListener {

    public static final String TAG = "SspeKernel";
    private static final String NO_WAKEUP_CALLBACK_STR = "{\"status\":0,\"wakeupWord\":\"null\",\"confidence\":0}";
    protected FespxKernelListener mListener;
    protected Sspe engine;

    protected AIWakeupProcessor mWakeupProcessor;
    protected volatile boolean isStopped = true;
    protected volatile boolean isWakeuped = false;
    protected volatile boolean mHasVpOut = false;
    protected volatile boolean mHasDoaOut = false;
    protected SignalProcessingParams params = null;
    protected volatile boolean mHasWkpOut = false;
    /**
     * 流式长音频调用 forceRequestWakeupResult 也会调用内核 stop，强制输出唤醒结果
     */
    private boolean isForceRequestWakeupResult = false;
    protected volatile boolean mInAudioFileNeedCreated = false;
    protected volatile boolean mOutAudioFileNeedCreated = false;
    DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.CHINA);
    protected LocalSignalProcessingConfig mConfig;
    private AudioFileWriter mInAudioFileWriter; //保存原始录音
    private AudioFileWriter mOutAudioFileWriter; //保存beamforming后的录音
    private FileSaveUtil mEchoFileSaveUtil; // echo保存

    // 默认内置唤醒参数，
    private BuiltinWakeupWords builtinWakeupWords;
    // 使用默认内置唤醒参数时，记录唤醒参数类型
    private String builtinWakeupWordType;

    private Sspe.config_callback configCallback = new Sspe.config_callback() {

        @Override
        public int run(int type, byte[] data, int size) {
            if (type == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String config = new String(data).trim();
                Log.d(TAG, "configCallback config: " + config);

                if (!TextUtils.isEmpty(config)) {
                    if (config.contains("thresh2")) {
                        // 唤醒资源内置 家具双唤醒阈值
                        builtinWakeupWordType = AIConstant.BUILTIN_WAKEUPWORD_THRESH2;
                        builtinWakeupWords = new BuiltinWakeupWords();
                        builtinWakeupWords.parseConfig(config);
                    } else if (config.contains("thresh_high")) {
                        // 唤醒资源内置 高低阈值唤醒参数
                        builtinWakeupWordType = AIConstant.BUILTIN_WAKEUPWORD_E2E;
                        builtinWakeupWords = new BuiltinE2EWakeupWords();
                        ((BuiltinE2EWakeupWords) builtinWakeupWords).parseBuiltinWakeupConfig(config);
                    }
                }
            }
            return 0;
        }
    };


    public BaseSspeKernel(String tag, FespxKernelListener listener) {
        super(tag, listener);
        this.mListener = listener;

    }

    @Override
    public void onWakeup(AIResult result) {
        if (mListener != null && mState != EngineState.STATE_IDLE) {
            mListener.onResult(result);
        }
    }

    @Override
    public void onDoaResult(int doa, int doaType) {
        if (mListener != null && mState != EngineState.STATE_IDLE) {
            mListener.onDoaResult(doa, doaType);
        }
        //查询模式下的doa不做任何处理，直接抛数据給上层
        if (doaType == AIConstant.DOA.TYPE_QUERY) {
            Log.d(TAG, "Currently in doa query mode!");
            return;
        }
        if (mListener != null && mState != EngineState.STATE_IDLE) {
            mListener.onDoaResult(doa);
        }
    }

    @Override
    public synchronized int getValueOf(String param) {
        if (engine != null) {
            return engine.get(param);
        } else {
            Log.e(TAG, "getValueOf() err: engine is null");
            return -1;
        }
    }

    @Override
    public String getNewConf() {
        if (mConfig != null) {
            return mConfig.toJson().toString();
        }
        return super.getNewConf();
    }

    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    if (mState != EngineState.STATE_IDLE) {
                        trackInvalidState("new");
                    } else {
                        mConfig = (LocalSignalProcessingConfig) message.mObject;
                        int mMicType;
                        if (mConfig.getMicType() >= 0) {
                            mMicType = mConfig.getMicType();
                        } else {
                            mMicType = AISpeech.getRecoderType();
                        }
                        if (mMicType == DUILiteConfig.TYPE_COMMON_MIC) {
                            if (mListener != null) {
                                mListener.onError(new AIError(AIError.ERR_INVALID_RECORDER_TYPE,
                                        AIError.ERR_DESCRIPTION_INVALID_RECORDER_TYPE));
                                mListener.onInit(AIConstant.OPT_FAILED);
                            }
                            Log.e(TAG, "invalid recorder type " + mMicType);
                            return;
                        }
                        engine = new Sspe();
                        engine.setImplMultiBfCk(mConfig.isImplMultiBfCk());
                        engine.setImplWakeupCk(mConfig.isImplWakeupCk());
                        engine.setImplInputCk(mConfig.isImplInputCk());
                        engine.setImplOutputCk(mConfig.isImplOutputCk());
                        engine.setImplEchoCk(mConfig.isImplEchoCk());
                        engine.setImplEchoVoipCk(mConfig.isImplEchoVoipCk());
                        engine.setImplAgcCk(mConfig.isImplAgcCk());
                        engine.setImplBfCk(mConfig.isImplBfCk());
                        engine.setImplDoaCk(mConfig.isImplDoaCk());
                        engine.setImplSevcNoiseCk(mConfig.isImplSevcNoiseCk());
                        engine.setImplVprintCutCk(mConfig.isImplVprintCutCk());
                        engine.setImplSevcDoaCk(mConfig.isImplSevcDoaCk());
                        engine.setImplVadCk(mConfig.isImplVadCk());


                        mWakeupProcessor = new AIWakeupProcessor(BaseSspeKernel.this);

                        int flag = initEngine(engine, mConfig);
                        if (flag == AIConstant.OPT_SUCCESS)
                            transferState(EngineState.STATE_NEWED);
                        mListener.onInit(flag);
                    }
                    break;
                case Message.MSG_START:
                    handleMsgStart(message);
                    break;
                case Message.MSG_SET:
                    if (mState == EngineState.STATE_IDLE) {
                        trackInvalidState("set");
                    } else {
                        String setParam = (String) message.mObject;
                        if (TextUtils.isEmpty(setParam)) return;
                        if (builtinWakeupWords != null) {
                            // 补齐内嵌唤醒默认参数
                            if (AIConstant.BUILTIN_WAKEUPWORD_THRESH2.equals(builtinWakeupWordType)) {
                                setParam = builtinWakeupWords.processWakeupWordInSetMethod(setParam);
                            } else if (AIConstant.BUILTIN_WAKEUPWORD_E2E.equals(builtinWakeupWordType)) {
                                setParam = ((BuiltinE2EWakeupWords) builtinWakeupWords).processEnvJsonString(setParam);
                            }
                        }

                        if (setParam == null) {
                            Log.d(TAG, " setParam " + setParam);
                            // 处理后的 setParam 可能为 null
                            sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_WAKEUP_NOT_SUPPORT_IN_RES, AIError.ERR_DESCRIPTION_WAKEUP_NOT_SUPPORT_IN_RES)));
                        } else if (engine != null) {
                            int ret = engine.set(setParam);
                            Log.d(TAG, "set ret: " + ret + " setParam " + setParam);
                            if (ret < 0) {
                                sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_WAKEUP_NOT_SUPPORT_IN_RES, AIError.ERR_DESCRIPTION_WAKEUP_NOT_SUPPORT_IN_RES)));
                            }
                        }
                    }
                    break;
                case Message.MSG_STOP:
                    handleMsgStop();
                    break;
                case Message.MSG_RELEASE:
                    handleMsgRelease();
                    isReleased = true;
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    if (mState != EngineState.STATE_RUNNING) {
                        trackInvalidState("feed");
                    } else {
                        byte[] data = (byte[]) message.mObject;
                        saveInData(data, data.length);
                        feedRawAudio(data);
                    }
                    break;
                case Message.MSG_BEAMFORMING_DATA:
                    byte[] data = (byte[]) message.mObject;
                    saveOutData(data, data.length);
                    break;
                case Message.MSG_ERROR:
                    closeFileWriter();
                    mListener.onError((AIError) message.mObject);
                    break;
                case Message.MSG_FORCE_REQUEST_WAKEUP_RESULT:
                    if (mState == EngineState.STATE_RUNNING) {
                        isForceRequestWakeupResult = true;
                        mHasWkpOut = false;
                        mHasVpOut = false;
                        mHasDoaOut = false;
                        isWakeuped = false;
                        engine.stop();
                        if (!isWakeuped && mWakeupProcessor != null) {
                            Log.d(TAG, "NO.WAKEUP.CALLBACK: " + NO_WAKEUP_CALLBACK_STR);
                            mWakeupProcessor.processWakeupCallback(NO_WAKEUP_CALLBACK_STR);
                        } else {
                            isWakeuped = false;
                        }
                        isForceRequestWakeupResult = false;
                        engine.start("");
                    } else {
                        trackInvalidState("MSG_FORCE_REQUEST_WAKEUP_RESULT");
                    }
                    break;
                default:
                    break;
            }

            handleCustomMessage(message);

            if (isReleased) {
                innerRelease();
                break;//release后跳出while循环
            }
        }
    }

    protected void handleCustomMessage(Message message) {

    }

    protected void handleMsgStart(Message message) {
        if (mState != EngineState.STATE_NEWED) {
            trackInvalidState("start");
        } else {
            params = (SignalProcessingParams) message.mObject;
            Log.w(TAG, "feed data module isInputContinuousAudio:  "
                    + params.isInputContinuousAudio());
            Log.d(TAG, "start");
            createFileWriter();
            if (engine != null) {
                engine.start("");
                isStopped = false;
                isWakeuped = false;
                transferState(EngineState.STATE_RUNNING);
            }
        }
    }

    protected void handleMsgRelease() {
        if (mState == EngineState.STATE_IDLE) {
            trackInvalidState("release");
        } else {
            Log.d(TAG, "release");
            if (engine != null) {
                engine.destroy();
                engine = null;
            }
            closeFileWriter();
            if (params != null && TextUtils.isEmpty(params.getSaveAudioPath())) {
                FileUtil.limitFileTotalSize(params.getSaveAudioPath(), (int) (AISpeechSDK.GLOBAL_AUDIO_FILE_ALL_SIZE * 0.4), "fesp");
            }
            transferState(EngineState.STATE_IDLE);
        }

        isStopped = true;
    }

    protected void handleMsgStop() {
        if (mState != EngineState.STATE_RUNNING) {
            trackInvalidState("stop");
        } else {
            Log.d(TAG, "stop");
            if (engine != null) {
                engine.stop();
            }
            isStopped = true;
            closeFileWriter();
            transferState(EngineState.STATE_NEWED);
        }
    }

    protected void preInitEngine(LiteSoFunction engine, LocalSignalProcessingConfig config) {

    }

    protected int initEngine(LiteSoFunction engine, LocalSignalProcessingConfig config) {
        if (config == null) {
            return AIConstant.OPT_FAILED;
        }

        preInitEngine(engine, config);

        String cfg = config.toJson().toString();
        Log.d(TAG, "initEngine config: " + cfg);
        ((Sspe) engine).getWakeupConfig(cfg, configCallback);

        // 检查内置唤醒参数是否正确
        boolean isCorrectWakeupWordInfo = true;
        if (builtinWakeupWords != null) {
            if (AIConstant.BUILTIN_WAKEUPWORD_THRESH2.equals(builtinWakeupWordType)) {
                isCorrectWakeupWordInfo = builtinWakeupWords.useBuiltinWakeupWords(config);
                cfg = config.toJson().toString();
            } else if (AIConstant.BUILTIN_WAKEUPWORD_E2E.equals(builtinWakeupWordType)) {
                // 解析内核中配置的默认唤醒参数
                cfg = ((BuiltinE2EWakeupWords) builtinWakeupWords).processEnvJsonString(cfg);
                isCorrectWakeupWordInfo = ((BuiltinE2EWakeupWords) builtinWakeupWords).isUseE2EWakeupWord();
            }
        }

        if (!isCorrectWakeupWordInfo) {
            // 唤醒词参数设置有问题，与唤醒资源内的信息不符
            sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_WAKEUP_NOT_SUPPORT_IN_RES, AIError.ERR_DESCRIPTION_WAKEUP_NOT_SUPPORT_IN_RES)));
            return AIConstant.OPT_FAILED;
        }
        long engineId = engine.init(cfg);
        Log.d(TAG, "initEngine create engineId " + engineId);
        if (engineId == 0) {
            Log.d(TAG, "引擎初始化失败");
            return AIConstant.OPT_FAILED;
        }
        Log.d(TAG, "引擎初始化成功");

        int ret = engine.setCallback(getSspeCallbacks());
        if (ret != 0 && ret != -9892 && ret != -9893) {
            // 0 成功  -9892 表示内核不支持该功能，由资源确定是否支持
            Log.e(TAG, "setCallback failed");
            return AIConstant.OPT_FAILED;
        } else
            return AIConstant.OPT_SUCCESS;
    }


    /**
     * 先抽离方法，方便后续拓展
     *
     * @return
     */
    protected Object[] getSspeCallbacks() {
        return new Object[]{new MyOutputCallbackImpl(),
                new MyVprintCutCallbackImpl(),
                new MyInputCallbackImpl(),
                new MyEchoCallbackImpl(), new MySevcDoaCallback(),
                new MySevcNoiseCallback(),
                new MyEchoVoipCallbackImpl(), new MyAgcCallbackImpl()};
    }

    protected void feedRawAudio(byte[] data) {
        if (engine != null && !isStopped) {
            if (params.isInputContinuousAudio()) {
                engine.feed(data, data.length);
            } else {
                /***********输入非实时长音频需要模拟非唤醒信息***********/
                Log.d(TAG, "feed wakeup data size: " + data.length);
                mHasWkpOut = false;
                mHasVpOut = false;
                mHasDoaOut = false;
                isWakeuped = false;
                engine.feed(data, data.length);
                Log.d(TAG, "stop begin");
                engine.stop();
                Log.d(TAG, "stop end");
                //主动检测是否被唤醒
                if (!isWakeuped && mWakeupProcessor != null) {//没有被唤醒
                    Log.d(TAG, "NO.WAKEUP.CALLBACK: " + NO_WAKEUP_CALLBACK_STR);
                    mWakeupProcessor.processWakeupCallback(NO_WAKEUP_CALLBACK_STR);
                } else {
                    isWakeuped = false;
                }
                /***********输入非实时长音频需要模拟非唤醒信息***********/
                engine.start(""); // stop之后再开启唤醒引擎，先前已start过，避免两次start内核报错
            }
        }
    }

    protected void createFileWriter() {
        Log.i(TAG, "raw path: " + params.getSaveAudioFilePath());
        mInAudioFileNeedCreated = !TextUtils.isEmpty(params.getSaveAudioFilePath());
        mOutAudioFileNeedCreated = !TextUtils.isEmpty(params.getSaveAudioFilePath());
        createInFileIfNeed();
        createOutFileIfNeed();
        createFileSaveUtil();
    }

    private void createInFileIfNeed() {
        if (mInAudioFileNeedCreated && AISpeech.isLocalSaveEnabled()) {
            mInAudioFileWriter = new AudioFileWriter();
            mInAudioFileWriter.createFile(params.getSaveAudioFilePath() + "/in_" + sdf.format(new Date()) + ".pcm");
            mInAudioFileNeedCreated = false;
            Log.i(TAG, "raw path: " + params.getSaveAudioFilePath());
        }
    }

    private void createOutFileIfNeed() {
        if (mOutAudioFileNeedCreated && AISpeech.isLocalSaveEnabled()) {
            mOutAudioFileWriter = new AudioFileWriter();
            mOutAudioFileWriter.createFile(params.getSaveAudioFilePath() + "/out_" + sdf.format(new Date()) + ".pcm");
            mOutAudioFileNeedCreated = false;
        }
    }

    private void saveInData(final byte[] data, final int size) {
        createInFileIfNeed();
        if (mInAudioFileWriter != null && AISpeech.isLocalSaveEnabled()) {
            mInAudioFileWriter.write(data, size);
        }
    }

    private void saveOutData(final byte[] data, final int size) {
        createOutFileIfNeed();
        if (mOutAudioFileWriter != null && AISpeech.isLocalSaveEnabled()) {
            mOutAudioFileWriter.write(data, size);
        }
    }

    protected void createFileSaveUtil() {
        String saveAudioPath = params.getSaveAudioPath();
        if (!TextUtils.isEmpty(saveAudioPath)) {
            Log.d(TAG, "raw path: " + saveAudioPath);

            mEchoFileSaveUtil = new FileSaveUtil();
            mEchoFileSaveUtil.init(saveAudioPath);
            mEchoFileSaveUtil.prepare("k-echo");
        }
    }

    protected void closeFileWriter() {
        if (mInAudioFileWriter != null) {
            mInAudioFileWriter.close();
            mInAudioFileWriter = null;
        }
        if (mOutAudioFileWriter != null) {
            mOutAudioFileWriter.close();
            mOutAudioFileWriter = null;
        }
        if (mEchoFileSaveUtil != null) {
            mEchoFileSaveUtil.close();
            mEchoFileSaveUtil = null;
        }
    }

    @Override
    public void set(String setParam) {
        super.set(setParam);
    }

    /**
     * 处理唤醒回调信息
     *
     * @param retString
     */
    protected void processWakeupCallback(String retString) {

        if (!params.isInputContinuousAudio() || isForceRequestWakeupResult) {
            if (!mHasWkpOut) {
                mWakeupProcessor.processWakeupCallback(retString);
            } else {
                Log.w(TAG, "more than one wkp, ignore");
            }
            try {
                JSONObject wakeupJson = new JSONObject(retString);
                if (wakeupJson.has("status")) {
                    int mWakeupStatus = wakeupJson.optInt("status");
                    if (mWakeupStatus == 1 || mWakeupStatus == 2) {
                        isWakeuped = true;
                        mHasWkpOut = true;
                        Log.d(TAG, "real wakeup");
                        Log.d(TAG, "first wkp cb end");
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            /***********输入非实时长音频需要模拟非唤醒信息***********/
            mWakeupProcessor.processWakeupCallback(retString);
        }
    }

    private class MyVprintCutCallbackImpl extends Sspe.vprintcut_callback {
        @Override
        public int run(int type, byte[] data, int size) {
            byte[] buffer = new byte[size];
            System.arraycopy(data, 0, buffer, 0, size);
            if (params != null && !params.isInputContinuousAudio() && type == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String vprintStr = Util.newUTF8String(buffer);
                Log.d(TAG, "vprintStr is " + vprintStr);
                if (!mHasVpOut) {
                    if (mListener != null) {
                        mListener.onVprintCutDataReceived(type, buffer, size);
                    }
                } else {
                    Log.w(TAG, "more than one vp, ignore");
                }
                if (vprintStr.contains("selectedChan")) {
                    mHasVpOut = true;
                    Log.d(TAG, "first vprint cb end");
                }
            } else {
                if (mListener != null) {
                    mListener.onVprintCutDataReceived(type, buffer, size);
                }
            }
            return 0;
        }
    }

    private class MySevcDoaCallback implements Sspe.sevc_doa_callback {

        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "MySevcDoaCallback return : " + retString);
                try {
                    JSONObject object = new JSONObject(retString);
                    if (object.has("doa")) {
                        mListener.onSevcDoaResult(object.get("doa"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
    }

    private class MySevcNoiseCallback implements Sspe.sevc_noise_callback {

        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "MySevcDoaCallback return : " + retString);
                mListener.onSevcNoiseResult(retString);
            }
            return 0;
        }
    }

    /**
     * input音频回调，返回喂给算法内核的音频数据
     */
    private class MyInputCallbackImpl extends Sspe.input_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bufferData = new byte[size];
                System.arraycopy(data, 0, bufferData, 0, size);
                mListener.onInputDataReceived(bufferData, size);
            }
            return 0;
        }
    }

    /**
     * output音频回调，返回内核定制资源输出的音频数据
     */
    private class MyOutputCallbackImpl extends Sspe.output_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bufferData = new byte[size];
                System.arraycopy(data, 0, bufferData, 0, size);
                mListener.onOutputDataReceived(bufferData, size);
            }
            return 0;
        }
    }

    /**
     * echo音频回调，返回去除参考音的音频数据
     */
    private class MyEchoCallbackImpl extends Sspe.echo_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bufferData = new byte[size];
                System.arraycopy(data, 0, bufferData, 0, size);
                mListener.onEchoDataReceived(bufferData, size);
                if (mEchoFileSaveUtil != null) {
                    mEchoFileSaveUtil.feedTypeOut(bufferData);
                }
            }
            return 0;
        }
    }
    /**
     * 经过回声消除的送voip的音频数据
     */
    private class MyEchoVoipCallbackImpl implements Sspe.echo_voip_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bufferData = new byte[size];
                System.arraycopy(data, 0, bufferData, 0, size);
                mListener.onEchoVoipDataReceived(dataType, bufferData, size);
            }
            return 0;
        }
    }

    /**
     * agc音频放大功能
     */
    private class MyAgcCallbackImpl implements Sspe.agc_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bufferData = new byte[size];
                System.arraycopy(data, 0, bufferData, 0, size);
                mListener.onAgcDataReceived(bufferData, size);
            }
            return 0;
        }
    }




}
