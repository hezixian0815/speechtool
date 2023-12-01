package com.aispeech.lite.fespx;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.DUILiteConfig;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AudioFileWriter;
import com.aispeech.common.LimitQueue;
import com.aispeech.common.Log;
import com.aispeech.common.NotifyLimitQueue;
import com.aispeech.common.Util;
import com.aispeech.kernel.Fespa;
import com.aispeech.kernel.Fespd;
import com.aispeech.kernel.Fespl;
import com.aispeech.kernel.Fespx;
import com.aispeech.kernel.NearFespx;
import com.aispeech.kernel.Sspe;
import com.aispeech.kernel.fespCar;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AIWakeupProcessor;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.BuiltinWakeupWords;
import com.aispeech.lite.config.LocalSignalProcessingConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.SignalProcessingParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


/**
 * Created by wuwei on 18-6-1.
 */

public class FespxKernel extends BaseKernel implements AIWakeupProcessor.WakeupProcessorListener, NotifyLimitQueue.Listener<Object[]> {
    public static final String TAG = "FespxKernel";
    private static final String NO_WAKEUP_CALLBACK_STR = "{\"status\":0,\"wakeupWord\":\"null\",\"confidence\":0}";
    private final BuiltinWakeupWords builtinWakeupWords = new BuiltinWakeupWords();
    byte[] bytes = new byte[0];
    private static final int CHANEL = 2;
    /**
     * 状态流统计系数
     */
    private static final double FACTOR_STATE_FLOW = 0.15; //设定系数，消除因为状态流突变导致音频毛刺
    DateFormat sdf = new SimpleDateFormat("yyyy-dd-MM_HH-mm-ss", Locale.CHINA);
    private FespxKernelListener mListener;
    private Fespx mFespxEngine;
    private AIWakeupProcessor mWakeupProcessor;
    private MyBeamformingCallbackImpl myBeamformingCallback;
    private MyWakeupCallbackImpl myWakeupCallback;
    private MyDoaCallbackImpl myDoaCallback;
    private MyVprintCutCallbackImpl myVprintCutCallback;
    private MyInputCallbackImpl myInputCallback;
    private MyOutputCallbackImpl myOutputCallback;
    private MyEchoCallbackImpl myEchoCallback;
    private MySevcDoaCallback mySevcDoaCallback;
    private MySevcNoiseCallback mySevcNoiseCallback;
    private MyMultiBfCallback myMultiBfcb;
    private volatile boolean isStopped = true;
    private int mMicType;
    private volatile boolean isWakeuped = false;
    private volatile boolean mInAudioFileNeedCreated = false;
    private volatile boolean mOutAudioFileNeedCreated = false;
    private volatile boolean mMergeAudioFileCreated = false;
    private volatile boolean mWakeupCutAudioFileCreated = false;
    private AudioFileWriter mInAudioFileWriter; //保存原始录音
    private AudioFileWriter mOutAudioFileWriter; //保存beamforming后的录音
    private AudioFileWriter mWakeupCutFileWriter;
    private AudioFileWriter mMergeFileWriter; //合并状态流之后的音频数据
    /**
     * 定长的状态帧队列
     */
    private LimitQueue<Integer> mStateQueue;
    /**
     * 定长右端音频缓存队列
     */
    private NotifyLimitQueue<Object[]> mRightMarginQueue;
    /**
     * 流式长音频调用 forceRequestWakeupResult 也会调用内核 stop，强制输出唤醒结果
     */
    private boolean isForceRequestWakeupResult = false;
    private SignalProcessingParams params = null;
    private volatile boolean mHasWkpOut = false;
    private volatile boolean mHasVpOut = false;
    private volatile boolean mHasDoaOut = false;
    private LocalSignalProcessingConfig mConfig;
    private Sspe.config_callback configCallback = new Sspe.config_callback() {

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
    private boolean autoSetDriveMode = false;
    private int wakeupDriveMode = -1;

    public FespxKernel(FespxKernelListener listener) {
        super(TAG, listener);
        this.mListener = listener;
    }

    @Override
    public void onWakeup(AIResult result) {
        if (mListener != null && mState != EngineState.STATE_IDLE) {
            mListener.onResult(result);
        }
    }

    @Override
    public void onDoaResult(int doa, int type) {
        if (mListener != null && mState != EngineState.STATE_IDLE) {
            mListener.onDoaResult(doa);
        }
        autoSetDriveMode(doa);
    }

    @Override
    public synchronized int getValueOf(String param) {
        if (mFespxEngine != null && mListener != null) {
            return mFespxEngine.getFespx(param);
        } else {
            Log.e(TAG, "invalid state when getChannelNum");
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
                        if (mConfig.getMicType() >= 0) {
                            mMicType = mConfig.getMicType();
                        } else {
                            mMicType = AISpeech.getRecoderType();
                        }

                        if (mConfig.getNearWakeupConfig() != null) {
                            mFespxEngine = new NearFespx();
                            Log.d(TAG, "init fespx to NearFespx");
                        } else if (mMicType == DUILiteConfig.TYPE_COMMON_LINE4 ||
                                mMicType == DUILiteConfig.TYPE_TINYCAP_LINE4 ||
                                mMicType == DUILiteConfig.TYPE_TINYCAP_LINE6 ||
                                mMicType == DUILiteConfig.TYPE_COMMON_LINE8 ||
                                mMicType == DUILiteConfig.TYPE_COMMON_LINE6) {
                            mFespxEngine = new Fespl();
                            Log.d(TAG, "init fespx to fespl");
                        } else if (mMicType == DUILiteConfig.TYPE_COMMON_CIRCLE6 ||
                                mMicType == DUILiteConfig.TYPE_COMMON_CIRCLE4 ||
                                mMicType == DUILiteConfig.TYPE_TINYCAP_CIRCLE4 ||
                                mMicType == DUILiteConfig.TYPE_TINYCAP_CIRCLE6 ||
                                mMicType == DUILiteConfig.TYPE_COMMON_SHAPE_L4) {
                            mFespxEngine = new Fespa();
                            Log.d(TAG, "init fespx to fespa");
                        } else if (mMicType == DUILiteConfig.TYPE_COMMON_FESPCAR) {
                            mFespxEngine = new fespCar();
                            Log.d(TAG, "init fespx to fespCar");
                        } else if (mMicType == DUILiteConfig.TYPE_COMMON_DUAL ||
                                mMicType == DUILiteConfig.TYPE_TINYCAP_DUAL) {
                            mFespxEngine = new Fespd();
                            Log.d(TAG, "init fespx to Fespd");
                        } else {
                            if (mListener != null) {
                                mListener.onError(new AIError(AIError.ERR_INVALID_RECORDER_TYPE,
                                        AIError.ERR_DESCRIPTION_INVALID_RECORDER_TYPE));
                                mListener.onInit(AIConstant.OPT_FAILED);
                            }
                            Log.e(TAG, "invalid recorder type " + mMicType);
                            return;
                        }
                        mWakeupProcessor = new AIWakeupProcessor(FespxKernel.this);
                        myBeamformingCallback = new MyBeamformingCallbackImpl();
                        myWakeupCallback = new MyWakeupCallbackImpl();
                        myDoaCallback = new MyDoaCallbackImpl();
                        myVprintCutCallback = new MyVprintCutCallbackImpl();
                        myInputCallback = new MyInputCallbackImpl();
                        myOutputCallback = new MyOutputCallbackImpl();
                        myEchoCallback = new MyEchoCallbackImpl();
                        mySevcDoaCallback = new MySevcDoaCallback();
                        mySevcNoiseCallback = new MySevcNoiseCallback();
                        myMultiBfcb = new MyMultiBfCallback();
                        if (mMicType == DUILiteConfig.TYPE_COMMON_FESPCAR) {
                            mStateQueue = new LimitQueue(mConfig.getStateFrame());
                            Log.d(TAG, "mConfig.getRightMarginFrame() = " + mConfig.getRightMarginFrame());
                            mRightMarginQueue = new NotifyLimitQueue(mConfig.getRightMarginFrame(), this);
                        }
                        int flag = initFespx(mConfig, mFespxEngine);

                        if (flag == AIConstant.OPT_SUCCESS)
                            transferState(EngineState.STATE_NEWED);
                        mListener.onInit(flag);
                    }
                    break;
                case Message.MSG_START:
                    if (mState != EngineState.STATE_NEWED) {
                        trackInvalidState("start");
                    } else {
                        params = (SignalProcessingParams) message.mObject;
                        Log.w(TAG, "feed data module isInputContinuousAudio:  "
                                + params.isInputContinuousAudio());
                        Log.d(TAG, "fespx start");
                        createFileWriter();
                        if (mFespxEngine != null) {
                            setAutoSetDriveMode(params.isAutoSetDriveMode());
                            mFespxEngine.startFespx();
                            isStopped = false;
                            isWakeuped = false;
                            transferState(EngineState.STATE_RUNNING);
                        }
                    }
                    break;
                case Message.MSG_SET:
                    if (mState == EngineState.STATE_IDLE) {
                        trackInvalidState("set");
                    } else {
                        String setParam = (String) message.mObject;
                        if (TextUtils.isEmpty(setParam)) return;
                        setParam = builtinWakeupWords.processWakeupWordInSetMethod(setParam);
                        if (setParam == null) {
                            // 处理后的 setParam 可能为 null
                            sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_WAKEUP_NOT_SUPPORT_IN_RES, AIError.ERR_DESCRIPTION_WAKEUP_NOT_SUPPORT_IN_RES)));
                        } else if (mFespxEngine != null) {
                            int ret = mFespxEngine.setFespx(setParam);
                            Log.d(TAG, "setFespx ret: " + ret + " setParam " + setParam);
                        }
                    }
                    break;
//                case Message.MSG_WAKEUP_RESULT:
//                    //唤醒信息
//                    AIResult wakeupResult = (AIResult) message.mObject;
//                    mListener.onResult(wakeupResult);
//                    break;
//                case Message.MSG_DOA_RESULT:
//                    //唤醒doa角度
//                    int doaValue = (Integer) message.mObject;
//                    mListener.onDoaResult(doaValue);
//                    break;
                case Message.MSG_STOP:
                    if (mState != EngineState.STATE_RUNNING) {
                        trackInvalidState("stop");
                    } else {
                        Log.d(TAG, "fespx stop");
                        if (mFespxEngine != null) {
                            mFespxEngine.stopFespx();
                        }
                        isStopped = true;
                        closeFileWriter();
                        if (mStateQueue != null) {
                            mStateQueue.clear();
                        }
                        if (mRightMarginQueue != null) {
                            mRightMarginQueue.clear();
                        }
                        transferState(EngineState.STATE_NEWED);
                    }
                    break;
                case Message.MSG_RELEASE:
                    if (mState == EngineState.STATE_IDLE) {
                        trackInvalidState("release");
                    } else {
                        Log.d(TAG, "fespx release");
                        if (mFespxEngine != null) {
                            mFespxEngine.destroyFespx();
                            mFespxEngine = null;
                        }
                        closeFileWriter();
                        if (mStateQueue != null) {
                            mStateQueue = null;
                        }
                        if (mRightMarginQueue != null) {
                            mRightMarginQueue = null;
                        }
                        transferState(EngineState.STATE_IDLE);
                    }
                    isStopped = true;
                    isReleased = true;
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    if (mState != EngineState.STATE_RUNNING) {
                        trackInvalidState("feed");
                    } else {
                        byte[] data = (byte[]) message.mObject;
                        saveInData(data, data.length);
                        feedFespxRawAudio(data);
                    }
                    break;
                case Message.MSG_BEAMFORMING_DATA:
                    final Object[] objectsData = (Object[]) message.mObject;
                    final byte[] bfData = (byte[]) objectsData[0];
                    final int wakeupType = (int) objectsData[1];
                    saveOutData(bfData, bfData.length);
                    if (mMicType == DUILiteConfig.TYPE_COMMON_FESPCAR) {
                        byte[][] data = splitData(bfData, bfData.length);
                        if (mStateQueue != null) {
                            mStateQueue.offer(calFrameState(data[1]));
                        }
                        if (mRightMarginQueue != null) {
                            mRightMarginQueue.offer(new Object[]{data[0], 0});
                        }
                    } else {
                        mListener.onResultDataReceived(bfData, bfData.length, wakeupType);
                    }
                    break;
                case Message.MSG_INPUT_DATA:
                    final byte[] inputDatas = (byte[]) message.mObject;
                    mListener.onInputDataReceived(inputDatas, inputDatas.length);
                    break;
                case Message.MSG_OUTPUT_DATA:
                    final byte[] outputDatas = (byte[]) message.mObject;
                    mListener.onOutputDataReceived(outputDatas, outputDatas.length);
                    break;
                case Message.MSG_ECHO_DATA:
                    final byte[] echoDatas = (byte[]) message.mObject;
                    mListener.onEchoDataReceived(echoDatas, echoDatas.length);
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
                        mFespxEngine.stopFespx();
                        if (!isWakeuped && mWakeupProcessor != null) {
                            Log.d(TAG, "NO.WAKEUP.CALLBACK: " + "{\"status\":0,\"wakeupWord\":\"null\",\"confidence\":0}");
                            mWakeupProcessor.processWakeupCallback("{\"status\":0,\"wakeupWord\":\"null\",\"confidence\":0}");
                        } else {
                            isWakeuped = false;
                        }
                        isForceRequestWakeupResult = false;
                        mFespxEngine.startFespx();
                    } else {
                        trackInvalidState("MSG_FORCE_REQUEST_WAKEUP_RESULT");
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

    protected int initFespx(LocalSignalProcessingConfig config, Fespx engine) {
        int status;
        if (config != null) {
            // 创建引擎
            String cfg = config.toJson().toString();
            Log.d(TAG, "fespx config useBuiltinWakeupWords: " + cfg);
            engine.getWakeupConfig(cfg, configCallback);
            if (!useBuiltinWakeupWords(builtinWakeupWords, config)) {
                // 唤醒词参数设置有问题，与唤醒资源内的信息不符
                sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_WAKEUP_NOT_SUPPORT_IN_RES, AIError.ERR_DESCRIPTION_WAKEUP_NOT_SUPPORT_IN_RES)));
                return AIConstant.OPT_FAILED;
            }
            cfg = config.toJson().toString();
            Log.d(TAG, "fespx config : " + cfg);
            long engineId = engine.initFespx(cfg);
            Log.d(TAG, "fespx create return " + engineId + ".");
            if (engineId == 0) {
                Log.d(TAG, "引擎初始化失败");
                status = AIConstant.OPT_FAILED;
            } else {
                Log.d(TAG, "引擎初始化成功");
                status = AIConstant.OPT_SUCCESS;
            }
            if (setCallbacks(engine, mConfig)) return AIConstant.OPT_FAILED;
            int ret;
            if (engine instanceof NearFespx) {
                ret = ((NearFespx) engine).setInformationCallback(new InfomationCallbackImpl());
                if (ret != 0) {
                    Log.e(TAG, "setInformationCallback failed");
                    return AIConstant.OPT_FAILED;
                }
            }
        } else {
            status = AIConstant.OPT_FAILED;
        }
        return status;
    }

    private boolean setCallbacks(Fespx engine, LocalSignalProcessingConfig mConfig) {
        int ret;
        if (mConfig.isImplWakeupCk()) {
            ret = engine.setFespxWakeupcb(myWakeupCallback);
            if (ret != 0 && ret != -9892) {
                Log.e(TAG, "setWakeupcb failed");
                return true;
            }
        }
        if (mConfig.isImplDoaCk()) {
            ret = engine.setFespxDoacb(myDoaCallback);
            if (ret != 0 && ret != -9892) {
                Log.e(TAG, "setFespxDoacb failed");
                return true;
            }
        }
        if (mConfig.isImplBfCk()) {
            ret = engine.setFespxBeamformingcb(myBeamformingCallback);
            if (ret != 0 && ret != -9892) {
                // 0 成功  -9892 表示内核不支持该功能，由资源确定是否支持Beamforming
                Log.e(TAG, "setBeamformingcb failed");
                return true;
            }
        }
        if (mConfig.isImplVprintCutCk()) {
            ret = engine.setFespxVprintCutcb(myVprintCutCallback);
            if (ret != 0 && ret != -9892) {
                // 0 成功  -9892 表示内核不支持该功能，由资源确定是否支持声纹
                Log.e(TAG, "setFespxVprintCutcb failed");
                return true;
            }
        }
        if (mConfig.isImplInputCk()) {
            ret = engine.setFespxInputcb(myInputCallback);
            if (ret != 0 && ret != -9892) {
                // 0 成功  -9892 表示内核不支持该功能，由资源确定是否支持声纹
                Log.e(TAG, "setFespxInputcb failed");
                return true;
            }
        }
        if (mConfig.isImplOutputCk()) {
            ret = engine.setFespxOutputcb(myOutputCallback);
            if (ret != 0 && ret != -9892) {
                // 0 成功  -9892 表示内核不支持该功能，由资源确定是否支持声纹
                Log.e(TAG, "setFespxOutputcb failed");
                return true;
            }
        }
        if (mConfig.isImplEchoCk()) {
            ret = engine.setFespxEchocb(myEchoCallback);
            if (ret != 0 && ret != -9892) {
                // 0 成功  -9892 表示内核不支持该功能，由资源确定是否支持声纹
                Log.e(TAG, "setFespxEchocb failed");
                return true;
            }
        }
        if (mConfig.isImplSevcDoaCk()) {
            ret = engine.setFespxSevcDoa(mySevcDoaCallback);
            if (ret != 0 && ret != -9892) {
                // 0 成功  -9892 表示内核不支持该功能，由资源确定是否支持
                Log.e(TAG, "setFespxSevcDoa failed");
                return true;
            }
        }
        if (mConfig.isImplSevcNoiseCk()) {
            ret = engine.setFespxSevcNoise(mySevcNoiseCallback);
            if (ret != 0 && ret != -9892) {
                // 0 成功  -9892 表示内核不支持该功能，由资源确定是否支持
                Log.e(TAG, "setFespxSevcNoise failed");
                return true;
            }
        }
        if (mConfig.isImplMultiBfCk()) {
            ret = engine.setMultBfcb(myMultiBfcb);
            if (ret != 0 && ret != -9892) {
                // 0 成功  -9892 表示内核不支持该功能，由资源确定是否支持
                Log.e(TAG, "setFespxSevcNoise failed");
                return true;
            }
        }
        return false;
    }

    /**
     * 获取 驾驶模式，只有 fespCar 模块有这个功能
     *
     * @return 0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式，-1 错误，没有获取到
     */
    public synchronized int getDriveMode() {
        int driveMode = -1;
        if (mFespxEngine instanceof fespCar) {
            driveMode = ((fespCar) mFespxEngine).getDriveMode();
        } else {
            Log.d(TAG, "mFespxEngine is not fespCar");
        }
        Log.d(TAG, "getDriveMode:" + driveMode);
        return driveMode;
    }

    public boolean isAutoSetDriveMode() {
        return autoSetDriveMode;
    }

    /**
     * 当定位模式时，根据唤醒角度自动设置成主驾模式或者副驾模式
     *
     * @param autoSetDriveMode false 不自动设置（default），true 自动设置
     */
    public void setAutoSetDriveMode(boolean autoSetDriveMode) {
        if (mFespxEngine instanceof fespCar) {
            this.autoSetDriveMode = autoSetDriveMode;
        } else {
            this.autoSetDriveMode = false;
            Log.d(TAG, " not fespCar, ignore autoSetDriveMode");
        }
    }

    /**
     * 如果原来设置成主驾模式或者副驾模式，则还原成定位模式
     */
    public void resetDriveMode() {
        if (wakeupDriveMode != -1 && mFespxEngine instanceof fespCar && wakeupDriveMode == ((fespCar) mFespxEngine).getDriveMode()) {
            try {
                JSONObject setJson = new JSONObject();
                setJson.put("driveMode", 0);
                this.set(setJson.toString());
                wakeupDriveMode = -1;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 当定位模式时，根据唤醒角度自动设置成主驾模式或者副驾模式
     *
     * @param doa 1 为主驾方位，2 位副驾方位
     */
    private void autoSetDriveMode(int doa) {
        if (!autoSetDriveMode)
            return;
        if (doa != 1 && doa != 2)
            return;
        if (mFespxEngine instanceof fespCar && 0 == ((fespCar) mFespxEngine).getDriveMode()) {
            try {
                JSONObject setJson = new JSONObject();
                // 1为主驾模式;2为副驾模式
                setJson.put("driveMode", doa);
                this.set(setJson.toString());
                wakeupDriveMode = doa;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void feedFespxRawAudio(byte[] data) {
        if (mFespxEngine != null && !isStopped) {
            if (params.isInputContinuousAudio()) {
                mFespxEngine.feedFespx(data, data.length);
            } else {
                /***********输入非实时长音频需要模拟非唤醒信息***********/
                Log.d(TAG, "feed wakeup data size: " + data.length);
                mHasWkpOut = false;
                mHasVpOut = false;
                mHasDoaOut = false;
                isWakeuped = false;
                mFespxEngine.feedFespx(data, data.length);
                Log.d(TAG, "fespx stop begin");
                mFespxEngine.stopFespx();
                Log.d(TAG, "fespx stop end");
                //主动检测是否被唤醒
                if (!isWakeuped && mWakeupProcessor != null) {//没有被唤醒
                    Log.d(TAG, "NO.WAKEUP.CALLBACK: " + NO_WAKEUP_CALLBACK_STR);
                    mWakeupProcessor.processWakeupCallback(NO_WAKEUP_CALLBACK_STR);
                } else {
                    isWakeuped = false;
                }
                /***********输入非实时长音频需要模拟非唤醒信息***********/
                mFespxEngine.startFespx(); // stop之后再开启唤醒引擎，先前已start过，避免两次start内核报错
            }
        }
    }

    public int getFespx(String param) {
        return mFespxEngine != null ? mFespxEngine.getFespx(param) : -1;
    }

    private void createFileWriter() {
        Log.i(TAG, "raw path: " + params.getSaveAudioFilePath());
        mInAudioFileNeedCreated = !TextUtils.isEmpty(params.getSaveAudioFilePath());
        mOutAudioFileNeedCreated = !TextUtils.isEmpty(params.getSaveAudioFilePath());
        mMergeAudioFileCreated = !TextUtils.isEmpty(params.getSaveAudioFilePath());
        mWakeupCutAudioFileCreated = !TextUtils.isEmpty(params.getSaveWakeupCutFilePath());
        createInFileIfNeed();
        createOutFileIfNeed();
        createMergeFileIfNeed();
        createWakeupCutFileIfNeed();
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

    private void createMergeFileIfNeed() {
        if (mMergeAudioFileCreated && AISpeech.isLocalSaveEnabled()) {
            mMergeFileWriter = new AudioFileWriter();
            mMergeFileWriter.createFile(params.getSaveAudioFilePath() + "/merge_" + sdf.format(new Date()) + ".pcm");
            mMergeAudioFileCreated = false;
        }
    }

    private void createWakeupCutFileIfNeed() {
        if (mWakeupCutAudioFileCreated && AISpeech.isLocalSaveEnabled()) {
            mWakeupCutFileWriter = new AudioFileWriter();
            mWakeupCutFileWriter.createTextFile(params.getSaveWakeupCutFilePath() + "/wkcut_" + sdf.format(new Date()) + ".txt");
            mWakeupCutAudioFileCreated = false;
            Log.i(TAG, "wkcut path: " + params.getSaveWakeupCutFilePath());
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

    private void saveMergeData(byte[] data, int size) {
        createMergeFileIfNeed();
        if (mMergeFileWriter != null) {
            mMergeFileWriter.write(data, size);
        }
    }

    private void closeFileWriter() {
        if (mWakeupCutFileWriter != null) {
            mWakeupCutFileWriter.close();
            mWakeupCutFileWriter = null;
        }
        if (mInAudioFileWriter != null) {
            mInAudioFileWriter.close();
            mInAudioFileWriter = null;
        }
        if (mOutAudioFileWriter != null) {
            mOutAudioFileWriter.close();
            mOutAudioFileWriter = null;
        }
        if (mMergeFileWriter != null) {
            mMergeFileWriter.close();
            mMergeFileWriter = null;
        }
    }

    /**
     * 剥离2通道音频数据
     *
     * @param data 数据帧数
     * @param size
     * @return
     */
    public byte[][] splitData(byte[] data, int size) {
        byte[][] buffer = new byte[CHANEL][size / 2];
        for (int i = 0; i < CHANEL; i++) {
            for (int j = 0; j < size / (CHANEL * 2); j++) {
                buffer[i][2 * j] = data[2 * (j * CHANEL + i)];//0-0,2-8
                buffer[i][2 * j + 1] = data[2 * (j * CHANEL + i) + 1];//1-1,3-9
            }
        }
        return buffer;
    }

    /**
     * 计算当前帧状态
     *
     * @param frame 当前帧状态流
     * @return 0 非人声 1 人声
     */
    private int calFrameState(byte[] frame) {
        for (byte fs : frame) {
            if ((fs & 0x10) != 0)
                return 1;
        }
        return 0;
    }

    @Override
    public void onPop(Object[] objects) {

        byte[] data = (byte[]) objects[0];

        if (null == objects[1]) {
            Log.e(TAG, "wakeup type error!");
            return;
        }

        int wakeupType = (int) objects[1];
        byte[] mergeData = merge(data, mStateQueue.clone());

        if (mListener != null) {
            mListener.onResultDataReceived(mergeData, mergeData.length, wakeupType);
        }

        saveMergeData(mergeData, mergeData.length);
    }

    /**
     * 根据状态流数据，合并音频
     *
     * @param audio  音频数据
     * @param states 状态流
     * @return audio
     */
    private byte[] merge(byte[] audio, LimitQueue<Integer> states) {
        if (states.getLast() == 0) {
            if (!loopState(states)) {
                for (int i = 0; i < audio.length; i++)
                    audio[i] = 0;
            }
        }
        return audio;
    }

    /**
     * 遍历状态队列，检查前序状态帧数据
     *
     * @return 全 0 false 否则 true
     */
    private boolean loopState(LimitQueue<Integer> states) {
        double counter = 0.0;//计数器
        for (int vol : states) {
            if (vol == 1) {
                if (++counter / states.getLimit() > FACTOR_STATE_FLOW)
                    return true;
            }
        }
        return false;
    }

    /**
     * bf音频回调，包含wakeupType
     */
    private class MyBeamformingCallbackImpl extends Sspe.beamforming_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "beamforming_callback wakeup_type return : " + retString);
                int wakeup_type = mWakeupProcessor.processWakeupType(retString);
                Object[] objects = new Object[]{new byte[0], wakeup_type};
                sendMessage(new Message(Message.MSG_BEAMFORMING_DATA, objects));
            } else if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bufferData = new byte[size];
                System.arraycopy(data, 0, bufferData, 0, size);
                Object[] objects = new Object[]{bufferData, 0};
                sendMessage(new Message(Message.MSG_BEAMFORMING_DATA, objects));
            }
            return 0;
        }
    }

    private class MyVprintCutCallbackImpl extends Sspe.vprintcut_callback {
        @Override
        public int run(int type, byte[] data, int size) {
            byte[] buffer = new byte[size];
            System.arraycopy(data, 0, buffer, 0, size);
            if (!params.isInputContinuousAudio() && type == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String vprintStr = Util.newUTF8String(buffer);
                Log.d(TAG, "vprintStr is " + vprintStr);
                if (!mHasVpOut) {
                    if (mListener != null) {
                        mListener.onVprintCutDataReceived(type, buffer, size);
                        String wakeupStr = new String(buffer);
                        Log.i(TAG, "vprint cut info: " + wakeupStr);
                        createWakeupCutFileIfNeed();
                        if (mWakeupCutFileWriter != null && AISpeech.isLocalSaveEnabled()) {
                            mWakeupCutFileWriter.writeString(wakeupStr);
                        }
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
                    if (type == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                        Log.i(TAG, "vprint cut data: " + size);
                        createWakeupCutFileIfNeed();
                        if (mWakeupCutFileWriter != null && AISpeech.isLocalSaveEnabled()) {
                            mWakeupCutFileWriter.writeBytesAsString(buffer, size);
                        }
                    }
                }
            }
            return 0;
        }
    }

    /**
     * 唤醒信息回调
     */
    private class MyWakeupCallbackImpl implements Sspe.wakeup_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "wakeup_callback return : " + retString);
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
            return 0;
        }
    }

    private class InfomationCallbackImpl implements Sspe.information_callback {

        @Override
        public int run(int type, byte[] data, int size) {
            // 就近唤醒里的中间结果
            if (type == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                if (mListener != null)
                    mListener.onNearInformation(retString);
            }
            return 0;
        }
    }

    /**
     * doa角度回调
     */
    private class MyDoaCallbackImpl implements Sspe.doa_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "doa_callback return : " + retString);
                if (!params.isInputContinuousAudio()) {
                    if (!mHasDoaOut) {
                        mWakeupProcessor.processDoaResult(retString);
                        mHasDoaOut = true;
                        Log.d(TAG, "first doa cb end");
                    } else {
                        Log.w(TAG, "more than one doa, ignore");
                    }
                } else {
                    mWakeupProcessor.processDoaResult(retString);
                }
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
                sendMessage(new Message(Message.MSG_INPUT_DATA, bufferData));
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
                sendMessage(new Message(Message.MSG_OUTPUT_DATA, bufferData));
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
                sendMessage(new Message(Message.MSG_ECHO_DATA, bufferData));
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

    private class MyMultiBfCallback implements Sspe.multibf_callback {

        @Override
        public int run(int index, byte[] data, int size) {
            byte[] bufferData = new byte[size];
            System.arraycopy(data, 0, bufferData, 0, size);
            mListener.onMultibfDataReceived(index, bufferData, size);
            return 0;
        }
    }
}
