package com.aispeech.lite.wakeup;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.FileSaveUtil;
import com.aispeech.common.FileUtil;
import com.aispeech.common.LimitQueue;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.kernel.Wakeup;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.BuiltinWakeupWords;
import com.aispeech.lite.config.WakeupConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.param.WakeupParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * Created by wuwei on 2018/3/29.
 */

public class WakeupKernel extends BaseKernel implements Wakeup.wakeup_callback {

    private static final String TAG = "WakeupKernel";
    private final BuiltinWakeupWords builtinWakeupWords = new BuiltinWakeupWords();
    private static final String NO_WAKEUP_JSON = "{\"status\":0,\"wakeupWord\":\"null\",\"confidence\":0}";
    private WakeupThreadListener mListener;
    private Wakeup mEngine;
    private volatile boolean isStopped = true;
    private volatile boolean isWakeuped = false;
    /**
     * 流式长音频调用 forceRequestWakeupResult 也会调用内核 stop，强制输出唤醒结果
     */
    private boolean isForceRequestWakeupResult = false;
    private WakeupParams mParams;
    private volatile String mBestWkpStrStatus_1;//存放最新唤醒状态为1的结果
    private volatile String mBestWkpStrStatus_2;//存放最新的唤醒状态为2的结果
    //    private AudioFileWriter mAudioFileWriter; //保存原始录音
    private FileSaveUtil mAudioFileSaver;
    private Wakeup.config_callback configCallback = new Wakeup.config_callback() {

        @Override
        public int run(int type, byte[] data, int size) {
            if (type == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String config = new String(data).trim();
                Log.d(TAG, "configCallback config: " + config);
                builtinWakeupWords.parseConfig(config);
            }
            return 0;
        }
    };
    private int mWakeupStatus = 0;
    private volatile int mHasVpOut = 0;

    //    private AudioFileWriter mWakeupFileWriter; //唤醒前的音频数据
    private FileSaveUtil mWakeupFileSaver; //唤醒前的音频数据
    /**
     * 保存唤醒之前的音频
     */
    private LimitQueue<byte[]> mWakeupQueue;


    private Wakeup.vprintcut_callback vprintCallback = new Wakeup.vprintcut_callback() {
        @Override
        public int run(int type, byte[] data, int size) {
            byte[] buffer = new byte[size];
            System.arraycopy(data, 0, buffer, 0, size);
            if (!mParams.inputContinuousAudio() && type == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String vprintStr = Util.newUTF8String(buffer);
                Log.d(TAG, "vprintStr is " + vprintStr);
                if (mHasVpOut <= 2) {
                    // 一次唤醒吐2次
                    mHasVpOut++;
                    if (mListener != null && mState != EngineState.STATE_IDLE) {
                        mListener.onVprintCutDataReceived(type, buffer, size);
                    }
                } else {
                    Log.w(TAG, "more than one vp, ignore");
                }
            } else {
                if (mListener != null && mState != EngineState.STATE_IDLE) {
                    mListener.onVprintCutDataReceived(type, buffer, size);
                }
            }
            return 0;
        }
    };


    public WakeupKernel(WakeupThreadListener listener) {
        super(TAG, listener);
        this.mListener = listener;
    }

    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    if (mState == EngineState.STATE_IDLE) {
                        mEngine = new Wakeup();
                        WakeupConfig wakeupConfig = (WakeupConfig) message.mObject;
                        int flag = initWakeupEngine(wakeupConfig, mEngine);
                        if (flag == AIConstant.OPT_SUCCESS)
                            transferState(EngineState.STATE_NEWED);
                        mListener.onInit(flag);
                    } else {
                        trackInvalidState("new");
                    }
                    break;
                case Message.MSG_START:
                    if (mState == EngineState.STATE_NEWED) {
                        this.mParams = (WakeupParams) message.mObject;
                        if (!useBuiltinWakeupWords()) {
                            // 唤醒词参数设置有问题，与唤醒资源内的信息不符
                            sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_WAKEUP_NOT_SUPPORT_IN_RES, AIError.ERR_DESCRIPTION_WAKEUP_NOT_SUPPORT_IN_RES)));
                            break;
                        }

                        //Log.d(TAG, mParams.toString());
                        startWakeupAIEngine(mParams, mEngine);
                        Log.w(TAG, "feed data module is inputContinuousAudio:  "
                                + mParams.inputContinuousAudio());
                        isStopped = false;
                        isWakeuped = false;
                        mWakeupStatus = 0;
                        if(!TextUtils.isEmpty(mParams.getSaveAudioPath())){
                            mWakeupQueue = new LimitQueue<>(20);
                        }
                        createFileWriter();
                        transferState(EngineState.STATE_RUNNING);
                    } else {
                        trackInvalidState("start");
                    }
                    break;
                case Message.MSG_SET:
                    if (mState != EngineState.STATE_IDLE) {
                        String setParam = (String) message.mObject;
                        if (TextUtils.isEmpty(setParam))return;
                        setParam = builtinWakeupWords.processWakeupWordInSetMethod(setParam);
                        if (setParam == null) {
                            // 处理后的 setParam 可能为 null
                            sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_WAKEUP_NOT_SUPPORT_IN_RES, AIError.ERR_DESCRIPTION_WAKEUP_NOT_SUPPORT_IN_RES)));
                        } else if (mEngine != null) {
                            mEngine.setWakeup(setParam);
                        }
                    } else {
                        trackInvalidState("set");
                    }
                    break;
                case Message.MSG_STOP:
                    if (mState == EngineState.STATE_RUNNING) {
                        stopWakeupAIEngine(mEngine);
                        isStopped = true;
                        closeFileWriter();
                        transferState(EngineState.STATE_NEWED);
                    } else {
                        trackInvalidState("stop");
                    }
                    break;
                case Message.MSG_CANCEL:
                    if (mState == EngineState.STATE_RUNNING) {
                        cancelWakeupAIEngine(mEngine);
                        isStopped = true;
                        transferState(EngineState.STATE_NEWED);
                    } else {
                        trackInvalidState("cancel");
                    }
                    break;
                case Message.MSG_RELEASE:
                    if (mState != EngineState.STATE_IDLE) {
                        // 销毁引擎
                        destroyWakeupAIEngine(mEngine);
                        isReleased = true;
                        isStopped = true;
                        closeFileWriter();
                        transferState(EngineState.STATE_IDLE);
                    } else {
                        trackInvalidState("release");
                    }
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    if (mState == EngineState.STATE_RUNNING) {
                        byte[] data = (byte[]) message.mObject;
                        if (mEngine != null && !isStopped) {
                            if (mParams.inputContinuousAudio()) {
                                if (mEngine != null && !isStopped) {
                                    mEngine.feedWakeupData(data, data.length);
                                }
                                if(mWakeupQueue != null){
                                    mWakeupQueue.offer(data);
                                }
                            } else {
                                mBestWkpStrStatus_1 = "";
                                mBestWkpStrStatus_2 = "";
                                mHasVpOut = 0;
                                /***********输入非实时长音频需要模拟非唤醒信息***********/
                                Log.d(TAG, "before feed wakeup, feed wakeup data size: " + data.length);
                                isWakeuped = false;
                                mEngine.feedWakeupData(data, data.length);
                                Log.d(TAG, "before stop wakeup");
                                mEngine.stopWakeup();
                                Log.d(TAG, "after stop wakeup");
                                //主动检测是否被唤醒
                                if (!isWakeuped) {//没有被唤醒
                                    Log.d(TAG, "NO.WAKEUP.CALLBACK: " + "{\"status\":0,\"wakeupWord\":\"null\",\"confidence\":0}");
                                    processWakeupCallback("{\"status\":0,\"wakeupWord\":\"null\",\"confidence\":0}");
                                } else {
                                    isWakeuped = false;
                                }
                                if(mWakeupQueue != null){
                                    mWakeupQueue.offer(data);
                                }

                                /***********输入非实时长音频需要模拟非唤醒信息***********/
                                Log.i(TAG, "before start wakeup again");
                                startWakeupAIEngine(mParams, mEngine); // stop之后再开启唤醒引擎，先前已start过，避免两次内核start
                                Log.i(TAG, "after start wakeup again");
                            }
                            saveData(data, data.length);
                        }
                    } else {
                        trackInvalidState("feed");
                    }
                    break;
                case Message.MSG_FORCE_REQUEST_WAKEUP_RESULT:
                    if (mState == EngineState.STATE_RUNNING) {
                        isForceRequestWakeupResult = true;
                        mBestWkpStrStatus_1 = "";
                        mBestWkpStrStatus_2 = "";
                        mHasVpOut = 0;
                        isWakeuped = false;
                        mEngine.stopWakeup();
                        if (!isWakeuped) {//没有被唤醒
                            Log.d(TAG, "NO.WAKEUP.CALLBACK: " + "{\"status\":0,\"wakeupWord\":\"null\",\"confidence\":0}");
                            processWakeupCallback("{\"status\":0,\"wakeupWord\":\"null\",\"confidence\":0}");
                        } else {
                            isWakeuped = false;
                        }
                        isForceRequestWakeupResult = false;
                        startWakeupAIEngine(mParams, mEngine);
                    } else {
                        trackInvalidState("MSG_FORCE_REQUEST_WAKEUP_RESULT");
                    }
                    break;
                case Message.MSG_ERROR:
                    closeFileWriter();
                    if (mListener != null) {
                        mListener.onError((AIError) message.mObject);
                    }
                    break;
                default:
                    break;
            }
            if (isReleased) {
                innerRelease();
                break;//release后跳出while循环
            }
        }
    }

    private boolean useBuiltinWakeupWords() {
        Log.d(TAG, builtinWakeupWords.toString());
        if (mParams == null || !builtinWakeupWords.isUseBuiltInWakeupWords()) {
            return true;
        }

        if (!builtinWakeupWords.checkWords(mParams.getWords())) {
            Log.d(TAG, "useBuiltinWakeupWords 唤醒词检查不通过");
            return false;
        }

        String[] builtinThresh = builtinWakeupWords.getThreshString(mParams.getWords());
        if (builtinThresh == null) {
            Log.d(TAG, "useBuiltinWakeupWords 获取资源内置阈值error");
            return false;
        }
        mParams.setThreshold(builtinThresh);
        Log.d(TAG, "useBuiltinWakeupWords success");
        return true;
    }

    private int initWakeupEngine(WakeupConfig config, Wakeup engine) {
        int status;
        if (config != null) {
            String configStr = config.toJson().toString();
            Log.d(TAG, "WakeupCfg:\t" + configStr);
            long engineId = engine.initWakeup(configStr, this, configCallback);
            if (engineId == 0) {
                Log.e(TAG, "引擎唤醒初始化失败");
                status = AIConstant.OPT_FAILED;
            } else {
                int ret = engine.setVprintcutcb(vprintCallback);
                if (ret == 0) {
                    Log.d(TAG, "引擎初始化成功");
                    status = AIConstant.OPT_SUCCESS;
                } else {
                    Log.e(TAG, "引擎唤醒初始化失败");
                    status = AIConstant.OPT_FAILED;
                }
            }

        } else {
            status = AIConstant.OPT_FAILED;
        }
        return status;
    }

    private int startWakeupAIEngine(SpeechParams param, Wakeup engine) {
        String paramString = param.toString();
        Log.d(TAG, "WakeupParams:\t" + paramString);
        int startId = engine.startWakeup(paramString);
        Log.d(TAG, "startWakeupAIEngine end and ret: " + startId);
        if (startId != 0) {
            sendMessage(new Message(Message.MSG_ERROR,
                    new AIError(AIError.ERR_AI_ENGINE,
                            AIError.ERR_DESCRIPTION_AI_ENGINE)));
        }
        return startId;
    }

    private void stopWakeupAIEngine(Wakeup engine) {
        Log.d(TAG, "stopWakeupAIEngine");
        if (engine != null) {
            engine.stopWakeup();
        }
    }

    private void cancelWakeupAIEngine(Wakeup engine) {
        Log.d(TAG, "cancelWakeupAIEngine");
        if (engine != null) {
            engine.cancelWakeup();
        }
    }

    private void destroyWakeupAIEngine(Wakeup engine) {
        if (engine != null) {
            engine.destroyWakeup();
        }
    }

    @Override
    public int run(int type, byte[] retData, int size) {
        byte[] data = new byte[size];
        System.arraycopy(retData, 0, data, 0, size);
        if (type == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
            String wakeupStr = new String(data).trim();
            Log.d(TAG, "WAKEUP.CALLBACK: " + wakeupStr);
            /***********输入非实时长音频需要模拟非唤醒信息***********/
            if (!mParams.inputContinuousAudio() || isForceRequestWakeupResult) {
                try {
                    JSONObject wakeupJson = new JSONObject(wakeupStr);
                    if (wakeupJson.has("status")) {
                        mWakeupStatus = wakeupJson.optInt("status");
                        if (mWakeupStatus == 1) {
                            if (TextUtils.isEmpty(mBestWkpStrStatus_1))
                                mBestWkpStrStatus_1 = wakeupStr;
                            else
                                return 0;
                            isWakeuped = true;
                            Log.d(TAG, "real wakeup");
                        } else if (mWakeupStatus == 2) {
                            if (TextUtils.isEmpty(mBestWkpStrStatus_2))
                                mBestWkpStrStatus_2 = wakeupStr;
                            else
                                return 0;
                            isWakeuped = true;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            /***********输入非实时长音频需要模拟非唤醒信息***********/
            processWakeupCallback(wakeupStr);
        }
        return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
    }

    public void processWakeupCallback(String wakeupRetString) {
        if (!TextUtils.isEmpty(wakeupRetString)) {
            AIResult result = new AIResult();
            result.setLast(true);
            result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
            result.setResultObject(wakeupRetString);
            result.setTimestamp(System.currentTimeMillis());
            if (mListener != null && mState != EngineState.STATE_IDLE) {
                mListener.onResults(result);
                if(!TextUtils.isEmpty(mParams.getSaveAudioPath())) {
                    try {
                        JSONObject resultJO = new JSONObject(result.getResultObject().toString());
                        saveWakeupData(resultJO.optString("wakeupWord"));
                    }catch (JSONException  e){
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void saveWakeupData(String result){
        Log.d(TAG, "wake up result = " + result);
        if (mWakeupQueue == null) {
            Log.w(TAG, "wakeupQueue is null,Please turn on the audio save switch before starting the engine.");
            return;
        }

        DateFormat sdf = new SimpleDateFormat("yyyy-dd-MM_HH-mm-ss", Locale.CHINA);
        if (!TextUtils.isEmpty(mParams.getSaveAudioPath())) {
            Log.d(TAG, "audio wake path = " + mParams.getSaveAudioPath());
            mWakeupFileSaver = new FileSaveUtil();
            mWakeupFileSaver.init(mParams.getSaveAudioPath());
            mWakeupFileSaver.prepare("wakeup_" + result);

            while (!mWakeupQueue.isEmpty()) {
                byte[] data = mWakeupQueue.poll();
                mWakeupFileSaver.feedTypeCustom(data);
            }
            mWakeupFileSaver.close();
            mWakeupFileSaver = null;
            FileUtil.limitFileTotalSize(mParams.getSaveAudioPath(), (int) (AISpeechSDK.GLOBAL_AUDIO_FILE_ALL_SIZE * 0.1), "wakeup");
        }
    }


    private void createFileWriter() {
        Log.i(TAG, "raw path: " + mParams.getSaveAudioPath());
        if (!TextUtils.isEmpty(mParams.getSaveAudioPath())) {
            mAudioFileSaver = new FileSaveUtil();
            mAudioFileSaver.init(mParams.getSaveAudioPath());
            mAudioFileSaver.prepare();
        }
    }

    private void saveData(final byte[] data, final int size) {
        if (mAudioFileSaver != null) {
            mAudioFileSaver.feedTypeIn(data, size);
        }
    }


    private void closeFileWriter() {
        if (mAudioFileSaver != null) {
            mAudioFileSaver.close();
            mAudioFileSaver = null;
        }
    }
}