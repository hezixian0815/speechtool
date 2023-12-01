package com.aispeech.lite;

import static com.aispeech.lite.AISpeechSDK.KEY_LOG_WK_TYPE;
import static com.aispeech.lite.AISpeechSDK.KEY_MIC_MATRIX;
import static com.aispeech.lite.AISpeechSDK.KEY_SCENE;
import static com.aispeech.lite.AISpeechSDK.KEY_UPLOAD_ENTRY;
import static com.aispeech.lite.AISpeechSDK.LOG_MIC_MATRIX_TYPE_ONE;
import static com.aispeech.lite.AISpeechSDK.LOG_SCENE_TYPE_AIHOME;
import static com.aispeech.lite.AISpeechSDK.LOG_WK_TYPE_PREWAKEUP;
import static com.aispeech.lite.AISpeechSDK.LOG_WK_TYPE_WAKEUP;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.auth.AIAuthEngine;
import com.aispeech.auth.AIProfile;
import com.aispeech.auth.ProfileState;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AITimer;
import com.aispeech.common.AssetsHelper;
import com.aispeech.common.FileUtil;
import com.aispeech.common.Log;
import com.aispeech.common.ThreadNameUtil;
import com.aispeech.common.Util;
import com.aispeech.gourd.FileBuilder;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.audio.AIAudioRecorderProxy;
import com.aispeech.lite.audio.AIRecordListener;
import com.aispeech.lite.audio.AISignalProcessingRecorderProxy;
import com.aispeech.lite.audio.IAudioRecorder;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.LocalSignalProcessingConfig;
import com.aispeech.lite.fespx.FespxProcessor;
import com.aispeech.lite.mds.MdsProcessor;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.param.LocalAsrParams;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.sevc.SevcProcessor;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.TimerTask;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by yuruilong on 2017/5/15.
 */

public abstract class BaseProcessor implements AIRecordListener, MessageProcess.Handle {
    //upload config
    protected static final int DEFAULT_ONE_CHANNEL_DATA_SIZE = 3200 * 35;//默认为1路3.5s的音频
    protected static final int DEFAULT_VALID_TIME_THRESH = 500;//有效唤醒时长，默认500ms
    protected SpeechListener mOutListener;
    protected Context mContext;
    protected IAudioRecorder mAIRecorder;
    protected boolean isInitFailed = false;
    protected int threadCount = 1;
    protected volatile EngineState mState = EngineState.STATE_IDLE;
    protected EngineState mCallbackState = EngineState.STATE_IDLE;
    protected AIEngineConfig mBaseConfig;
    protected String mRecorderId;
    protected Queue<byte[]> mUploadCacheQueue = new LinkedList<>();//缓存上传大数据的唤醒音频
    protected int mCurrentDataSize = 0;
    protected int mDataSizeThresh = DEFAULT_ONE_CHANNEL_DATA_SIZE;
    protected volatile boolean mNeedCache = true;
    protected volatile boolean mIsRealWakeup = true;
    protected Object mLock = new Object();
    protected String mMicMatrixStr = LOG_MIC_MATRIX_TYPE_ONE;//麦克风阵列类型：1, 2_line, 2_car, 4_circle, 4_line, 4_car, 6
    protected String mSceneStr = LOG_SCENE_TYPE_AIHOME;//aihome, aicar, airobot
    protected volatile boolean mHasPreWakeup = false;//是否含有预唤醒状态，默认不含有
    protected volatile boolean mHasHalfWakeup = false;//是否含有半字唤醒状态，默认不含有
    protected volatile boolean needCopyFeedData = true; // 是否需要拷贝feed的数据，默认拷贝
    protected JSONObject mWakeupJson;
    protected long mLastWakeupTime = 0;
    protected String mScope = Scope.CLOUD_MODEL;//默认CLOUD_MODEL字段
    //dump wkp data
    protected Queue<byte[]> mDumpWkpDataQueue = new LinkedList<>();
    protected int mDumpCurrentDataSize = 0;
    protected int mDumpDataThresh = 3200 * 50;//默认每路缓存共5s音频，唤醒点4.5s，唤醒后0.5s
    protected Object mDumpLock = new Object();
    protected boolean mNeedDumpData = false;//默认不保存dump的音频
    /**
     * 单麦、多麦唤醒feed的是短音频还是连续的长音频，用于上传唤醒音频的日志
     */
    protected boolean inputContinuousAudio = true;
    //授权处理
    protected ProfileState mProfileState;
    protected AIProfile mProfile;
    private String tag = "BaseProcessor";
    private Handler mCallbackHandler;
    private MessageProcess messageProcess;
    private CyclicBarrier mBarrier;
    private int initState = AIConstant.OPT_FAILED;
    private byte[] mStateLock = new byte[0];
    private ScheduledExecutorService mExecutorService;
    private NoSpeechTimeoutTask mNoSpeechTimeoutTask = null;
    private MaxSpeechTimerTask mMaxSpeechTimerTask = null;
    private OneShotNoSpeechTimeoutTask mOneShotNoSpeechTimeoutTask;
    private static final int ONE_SHOT_TIMEOUT = 1200;
    private boolean useSingleMessageProcess = true; // 是否使用统一的消息处理器,默认统一一个线程处理；此变量不对外开放，针对一些耗时的引擎使用
    private volatile boolean callbackInMainThread = true;

    public void init(SpeechListener listener, Context context, String tag) {
        this.tag = tag;
        Log.i(tag, "new " + tag);
        isInitFailed = false;
        initState = AIConstant.OPT_FAILED;
        mOutListener = listener;
        Log.d(tag, "current scope is -> " + mScope);
        mProfile = AIAuthEngine.getInstance().getProfile();
        mProfileState = mProfile.isProfileValid(mScope);
        mContext = context;
        Log.d(tag, "authstate: " + mProfileState.toString());
        if (mProfileState.isValid()) {
            Log.d(tag, "threadCount: " + threadCount);
            if (mBarrier == null) {
                mBarrier = new CyclicBarrier(threadCount, new MyBarrierRunnable());
            }
            if (mCallbackHandler == null) {
                mCallbackHandler = createCallbackHandler();
            }
            if (useSingleMessageProcess) {
                messageProcess = MessageProcess.getInstance();
            } else {
                messageProcess = MessageProcess.newInstance(ThreadNameUtil.getFixedThreadName(tag));
            }
            messageProcess.registerHandle(this);

        } else {
            showErrorMessage(mProfileState);
        }
    }

    public void update(AIEngineConfig updateConfig) {
        if (mProfileState != null && mProfileState.isValid()) {
            copyAssetsRes(updateConfig);
            sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE, updateConfig.toJson().toString());
        } else {
            showErrorMessage(mProfileState);
        }
    }

    public void updateVocab(String updateStr) {
        if (mProfileState != null && mProfileState.isValid()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE_VOCAB, updateStr);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    public void set(String setStr) {
        if (mProfileState != null && mProfileState.isValid()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_SET, setStr);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    public int get(String param) {
        return 0;
    }

    protected String getRecorderId() {
        return mRecorderId;
    }

    protected void clearRecorderId() {
        mRecorderId = "";
    }

    /**
     * 设置recorder id
     *
     * @param id
     */
    protected void syncRecorderId(String id) {
        syncRecorderId(id, null, null);
    }

    /**
     * 设置recorder id
     *
     * @param asrParam
     * @param vadParams
     */
    protected void syncRecorderId(SpeechParams asrParam, VadParams vadParams) {
        syncRecorderId(Utils.getRecorderId(), asrParam, vadParams);
    }


    /**
     * 设置recorder id
     *
     * @param recorderId recorderId
     * @param asrParam   {@link SpeechParams}
     * @param vadParams  {@link VadParams }
     */
    protected void syncRecorderId(String recorderId, SpeechParams asrParam, VadParams vadParams) {
        Log.i(tag, "sync recorderId : " + recorderId);
        mRecorderId = recorderId;
        if (asrParam != null) {
            asrParam.setRecordId(recorderId);
        }
        if (vadParams != null) {
            vadParams.setRecordId(recorderId);
        }
    }

    /**
     * 设置sessionId
     *
     * @param sessionId sessionId
     * @param param     {@link SpeechParams }
     */
    protected void syncSessionId(String sessionId, SpeechParams param) {
        Log.i(tag, "sync sessionId : " + sessionId);
        if (param != null) {
            param.setSessionId(sessionId);
        }
    }

    public void feedData(byte[] data, int size) {
        // 兼容原有逻辑 默认拷贝
        feedData(data, size, needCopyFeedData);
    }

    /**
     * 自定义feed音频
     *
     * @param data
     * @param size
     * @param needCopyData
     */
    public void feedData(byte[] data, int size, boolean needCopyData) {
        if (mProfileState != null && mProfileState.isValid()) {
            byte[] bufferData;

            if (needCopyData) {
                bufferData = new byte[size];
                System.arraycopy(data, 0, bufferData, 0, size);
            } else {
                bufferData = data;
            }

            if (this instanceof FespxProcessor || this instanceof MdsProcessor || this instanceof SevcProcessor) {//使用前端信号处理和唤醒模块需要feed多路数据
                sendMsgToInnerMsgQueue(EngineMsg.MSG_RAW_RECEIVE_DATA, bufferData);
            } else {//只需feed单路数据
                sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT_RECEIVE_DATA, bufferData);
            }
        } else {
            showErrorMessage(mProfileState);
        }
    }

    public void stop() {
        if (mProfileState != null && mProfileState.isValid()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_STOP, null);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    public void cancel() {
        if (mProfileState != null && mProfileState.isValid()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_CANCEL, null);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    public void release() {
        if (mProfileState != null && mProfileState.isValid()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_RELEASE, null);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    public void forceRequestWakeupResult() {
        if (mProfileState != null && mProfileState.isValid()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_FORCE_REQUEST_WAKEUP_RESULT, null);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    @Override
    public void onRecordStarted(long sessionId) {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_RECORDER_START, null);
        sendMsgToCallbackMsgQueue(CallbackMsg.MSG_READY_FOR_SPEECH, null);
    }

    @Override
    public void onRawDataReceived(long sessionId, byte[] buffer, int size) {
        byte[] rawData = new byte[size];
        System.arraycopy(buffer, 0, rawData, 0, size);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_RAW_RECEIVE_DATA, rawData);
    }

    @Override
    public void onResultDataReceived(byte[] buffer, int size) {
        byte[] resultData = new byte[size];
        System.arraycopy(buffer, 0, resultData, 0, size);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT_RECEIVE_DATA, resultData);
    }

    @Override
    public void onPowerChanged(boolean isHighPower) {
        if (isHighPower) {
            //大音量
            sendMsgToInnerMsgQueue(EngineMsg.MSG_SET, AIConstant.MAX_VOLUME_ON);
        } else {
            //非大音量
            sendMsgToInnerMsgQueue(EngineMsg.MSG_SET, AIConstant.MAX_VOLUME_OFF);
        }
    }

    @Override
    public void onRecordStopped(long sessionId) {

    }

    @Override
    public void onRecordReleased() {

    }

    @Override
    public void onException(AIError aiError) {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, aiError);
    }

    @Override
    public String getTag() {
        return this.tag;
    }

    public void clearObject() {
        if (mExecutorService != null) {
            mExecutorService.shutdown();
            mExecutorService = null;
        }
        if (mAIRecorder != null)
            mAIRecorder = null;
        if (mContext != null)
            mContext = null;
        if (messageProcess != null) {
            messageProcess.unregisterHandle(this);
            messageProcess = null;
        }
        if (mCallbackHandler != null) {
            mCallbackHandler = null;
        }
        if (mBarrier != null) {
            mBarrier = null;
        }

    }

    private boolean isFilterMsg(EngineMsg msgWhat) {
        return (msgWhat != EngineMsg.MSG_RAW_RECEIVE_DATA &&
                msgWhat != EngineMsg.MSG_RESULT_RECEIVE_DATA &&
                msgWhat != EngineMsg.MSG_VOLUME_CHANGED &&
                msgWhat != EngineMsg.MSG_VAD_RECEIVE_DATA &&
                msgWhat != EngineMsg.MSG_SET &&
                msgWhat != EngineMsg.MSG_VPRINT_DATA &&
                msgWhat != EngineMsg.MSG_VPRINT_TLV);
    }


    /**
     * 需要继承类实现
     *
     * @param engineMsg
     * @param msg
     */
    protected abstract void handlerInnerMsg(EngineMsg engineMsg, Message msg);

    /**
     * 需要继承类实现（因涉及较多引擎，暂不用abstract修饰，逐步按需实现）
     *
     * @param callbackMsg {@link CallbackMsg}
     * @param msg         {@link Message}
     */
    protected void handlerCallbackMsg(CallbackMsg callbackMsg, Message msg) {
    }


    @Override
    public String getHandleName() {
        return tag;
    }

    @Override
    public void handleMessage(EngineMsg engineMsg, Message msg) {
        if (isFilterMsg(engineMsg)) {
            Log.d(tag, ">>>>>>Event: " + engineMsg.name());
            Log.d(tag, "[Current]:" + mState.name());
        }
        handlerInnerMsg(engineMsg, msg);
    }

    /**
     * 拷贝assets目录下的制定资源
     *
     * @param config
     * @return
     */
    protected int copyAssetsRes(AIEngineConfig config) {
        if (config == null) {
            Log.e(tag, "copyAssetsRes,config is null!!!");
            return AIConstant.OPT_FAILED;
        }
        int status = AIConstant.OPT_SUCCESS;
        String[] assetsResNames = config.getAssetsResNames();
        Map<String, String> assetsResMd5sumMap = config.getAssetsResMd5sum();
        if (assetsResNames != null && assetsResNames.length > 0) {
            for (String resName : assetsResNames) {
                String resMd5sumName = null;
                if (assetsResMd5sumMap != null) {
                    resMd5sumName = assetsResMd5sumMap.get(resName);
                }
                int ret = AssetsHelper.copyResource(mContext, resName, resMd5sumName);
                if (ret == -1) {
                    Log.e(tag, "file " + resName + " not found in assest folder, Did you forget add it?");
                    return ret;
                }
            }
            AssetsHelper.updateMapFile(mContext);
        }
        return status;
    }


    /***
     * 拷贝assets目录下的多个文件夹
     * @return
     */
    protected int copyAssetsFolders(AIEngineConfig config) {
        int status = AIConstant.OPT_SUCCESS;
        String[] assetsResNames = config.getAssetsResNames();
        if (assetsResNames != null && assetsResNames.length > 0) {
            for (String resFloderName : assetsResNames) {
                int ret = AssetsHelper.copyFilesFromAssets(AISpeech.getContext(), resFloderName,
                        Util.getResourceDir(AISpeech.getContext()) + File.separator + resFloderName);
                if (ret == -1) {
                    Log.e(tag, "folder" + resFloderName + " not found in assest folder, Did you forget add it?");
                    return AIConstant.OPT_FAILED;
                }
            }
            AssetsHelper.updateMapFile(mContext);
            Log.d(tag, "copy folder from assets success");
        }

        return status;
    }

    /**
     * 拷贝assets下的文件夹
     *
     * @param config
     * @return
     */
    protected int copyAssetsFolder(AIEngineConfig config) {
        int status = AIConstant.OPT_SUCCESS;
        if (!TextUtils.isEmpty(config.getResFolderName())) {
            int ret = AssetsHelper.copyFilesFromAssets(AISpeech.getContext(), config.getResFolderName(),
                    Util.getResourceDir(AISpeech.getContext()) + File.separator + config.getResFolderName());
            if (ret == -1) {
                Log.e(tag, "folder" + config.getResFolderName() + " not found in assest folder, Did you forget add it?");
                return ret;
            }
            AssetsHelper.updateMapFile(mContext);
            Log.d(tag, "copy folder from assets success");
        }
        return status;
    }

    /**
     * 拷贝assets下的文件夹
     *
     * @param config
     * @return
     */
    //fix https://jira.aispeech.com.cn/browse/YJGGZC-12899
    protected int copyAssetsFolderMd5(AIEngineConfig config) {
        int status = AIConstant.OPT_SUCCESS;
        if (!TextUtils.isEmpty(config.getResFolderName())) {
            int ret = AssetsHelper.copyFilesFromAssets(AISpeech.getContext(),
                    config.getResFolderName(),
                    Util.getResourceDir(AISpeech.getContext()) + File.separator + config.getResFolderName());
            if (ret == -1) {
                Log.e(tag, "folder" + config.getResFolderName() + " not found in assest folder, Did you forget add it?");
                return ret;
            }
            AssetsHelper.updateMapFile(mContext);
            Log.d(tag, "copy folder from assets success");
        }
        return status;
    }


    /**
     * 类说明： 对外回调消息列表
     */
    public enum CallbackMsg {
        MSG_INIT(1), MSG_BEGINNING_OF_SPEECH(2), MSG_END_OF_SPEECH(3), MSG_BUFFER_RECEIVED(
                4), MSG_RECORED_RELEASED(5), MSG_ERROR(6), MSG_READY_FOR_SPEECH(
                7), MSG_RESULTS(8), MSG_RMS_CHANGED(9), MSG_RECORED_STOPPED(
                10), MSG_WAKEUP_STOPPED(11), MSG_NOT_ONE_SHOT(12), MSG_GRAMMAR_SUCCESS(
                13), MSG_DOA_RESULT(14), MSG_NEAR_INFORMATION(33), MSG_UPDATE_RESULT(
                34), MSG_SEVC_DOA_RESULT(35), MSG_SEVC_NOISE_RESULT(36), MSG_ONE_SHOT(37);

        private int value;

        private CallbackMsg(int value) {
            this.value = value;
        }

        /**
         * @return the value
         * @Override
         */
        public int getValue() {
            return value;
        }

        public static CallbackMsg getMsgByValue(int value) {
            for (CallbackMsg msg : CallbackMsg.values()) {
                if (value == msg.value) {
                    return msg;
                }
            }
            return null;
        }
    }


    /**
     * 用于给主UI线程抛消息
     *
     * @return
     */
    private Handler createCallbackHandler() {
        Looper looper = mContext.getMainLooper();
        return new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                CallbackMsg callbackMsg = CallbackMsg.getMsgByValue(msg.what);
                if (mCallbackState == EngineState.STATE_CANCELED) {
                    Log.d(tag, "mCallbackState is STATE_CANCELED , throw message");
                    return;
                }
                if (callbackMsg == null) {
                    Log.e(tag, "callbackMsg is null");
                    return;
                }
                switch (callbackMsg) {
                    case MSG_INIT:
                        initState = (Integer) msg.obj;
                        if (mOutListener != null) {
                            mOutListener.onInit(initState);
                        }
                        break;
                    case MSG_BEGINNING_OF_SPEECH:
                        if (mOutListener != null) {
                            mOutListener.onBeginningOfSpeech();
                        }
                        break;
                    case MSG_BUFFER_RECEIVED:
                        byte[] bytes = (byte[]) msg.obj;
                        if (mOutListener != null) {
                            mOutListener.onRawDataReceived(bytes, bytes.length);
                        }
                        break;
                    case MSG_END_OF_SPEECH:
                        if (mOutListener != null) {
                            mOutListener.onEndOfSpeech();
                        }
                        break;
                    case MSG_RECORED_STOPPED:
                        if (mOutListener != null) {
                            mOutListener.onRecorderStopped();
                        }
                        break;
                    case MSG_ERROR:
                        if (mOutListener != null) {
                            mOutListener.onError((AIError) msg.obj);
                        }
                        break;
                    case MSG_READY_FOR_SPEECH:
                        if (mOutListener != null) {
                            mOutListener.onReadyForSpeech();
                        }
                        break;
                    case MSG_RESULTS:
                        if (mOutListener != null) {
                            mOutListener.onResults((AIResult) msg.obj);
                        }
                        break;
                    case MSG_NEAR_INFORMATION:
                        if (mOutListener != null) {
                            mOutListener.onNearInformation((String) msg.obj);
                        }
                        break;
                    case MSG_DOA_RESULT:
                        Object[] objectsData = (Object[]) msg.obj;
                        if (mOutListener != null) {
                            int doa = (Integer) objectsData[0];
                            int doaType = (Integer) objectsData[1];
                            if (doaType == AIConstant.DOA.TYPE_WAKEUP)
                                mOutListener.onDoaResult(doa);

                            mOutListener.onDoaResult(doa, doaType);
                        }
                        break;
                    case MSG_SEVC_DOA_RESULT:
                        if (mOutListener != null) {
                            mOutListener.onSevcDoaResult((Integer) msg.obj);
                        }
                        break;
                    case MSG_SEVC_NOISE_RESULT:
                        if (mOutListener != null) {
                            mOutListener.onSevcNoiseResult((String) msg.obj);
                        }
                        break;
                    case MSG_RMS_CHANGED:
                        if (mOutListener != null) {
                            mOutListener.onRmsChanged((Float) msg.obj);
                        }
                        break;
                    case MSG_NOT_ONE_SHOT:
                        if (mOutListener != null) {
                            mOutListener.onNotOneShot();
                        }
                        break;
                    case MSG_ONE_SHOT:
                        if (mOutListener != null) {
                            Map oneshotMap = (Map) msg.obj;
                            mOutListener.onOneShot((String) oneshotMap.get("words"), (OneshotCache<byte[]>) oneshotMap.get("audio"));
                        }
                        break;
                    case MSG_GRAMMAR_SUCCESS:
                        if (mOutListener != null) {
                            mOutListener.onBuildCompleted((AIResult) msg.obj);
                        }
                        break;
                    case MSG_UPDATE_RESULT:
                        Integer ret = (Integer) msg.obj;
                        if (mOutListener != null) {
                            mOutListener.onUpdateResult(ret);
                        }
                        break;
                    default:
                        break;
                }
                handlerCallbackMsg(callbackMsg, msg);
            }
        };
    }

    /**
     * 向内部消息队列发送消息
     *
     * @param msg SynthesizeMsg枚举
     * @param obj msg.obj
     */
    protected void sendMsgToInnerMsgQueue(EngineMsg msg, Object obj) {
        if (messageProcess != null) {
            Message mm = new Message();
            mm.what = msg.getValue();
            mm.obj = obj;
            messageProcess.sendMessage(this, mm);
        }
    }

    protected void sendMsgToInnerMsgQueue(EngineMsg msg, int arg1, int arg2, Object obj) {
        if (messageProcess != null) {
            Message mm = Message.obtain();
            mm.what = msg.getValue();
            mm.arg1 = arg1;
            mm.arg2 = arg2;
            mm.obj = obj;
            messageProcess.sendMessage(this, mm);
        }

    }

    protected void sendMsgToCallbackMsgQueue(CallbackMsg msg, Object obj) {
        if (mCallbackHandler != null) {
            if (!AISpeech.callbackInThread)
                Message.obtain(mCallbackHandler, msg.getValue(), obj).sendToTarget();
            else
                mCallbackHandler.handleMessage(Message.obtain(mCallbackHandler, msg.getValue(), obj));
        }
    }

    protected void removeCallbackMsg() {
        if (mCallbackHandler != null) {
            mCallbackHandler.removeCallbacksAndMessages(null);
        }
    }

    protected void trackInvalidState(String msg) {
        Log.w(tag, "Invalid State：" + mState.name() + " when MSG: " + msg);
    }

    protected void transferState(EngineState nextState) {
        synchronized (mStateLock) {
            Log.d(tag, "transfer:" + mState + " to:" + nextState);
            mState = nextState;
        }
    }

    public EngineState getCurrentState() {
        EngineState currentState;
        synchronized (mStateLock) {
            currentState = mState;
        }
        Log.d(tag, "getCurrentState " + currentState);
        return currentState;
    }

    protected IAudioRecorder createRecorder(AIRecordListener listener) {
        Log.i(tag, "createRecorder");
        SpeechParams params = new SpeechParams();
        return AIAudioRecorderProxy.getInstance(params.getSampleRate(), listener);
    }

    /**
     * 创建前端信号处理录音机
     *
     * @return
     */
    protected IAudioRecorder createSignalProcessingRecorder(FespxProcessor processor) {
        Log.i(tag, "createSignalProcessingRecorder");
        return AISignalProcessingRecorderProxy.create(processor);
    }

    protected void startRecorder(SpeechParams params, AIRecordListener listener) {
        Log.i(tag, "startRecorder");
        if (mAIRecorder != null) {
            try {
                mAIRecorder.startRecorder(listener);
            } catch (Exception e) {
                // catch java.lang.IllegalStateException: startRecording() called on an uninitialized AudioRecord
                e.printStackTrace();
                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                        AIError.ERR_RECORDING, AIError.ERR_DESCRIPTION_RECORDING));
                return;
            }
            if (isWakeup() && params.isUseOneShot()) {//使用唤醒引擎，只缓存音频
                Log.d(tag, "is wakeup module and use oneshot function");
                mAIRecorder.startCachingData();
            } else if (params.isUseOneShot()) {//使用识别等语音引擎，优先抛出缓存的音频
                Log.d(tag, "use oneshot function and send cache data");
                mAIRecorder.startSendCachingData();
            }
        }
    }

    /**
     * 是否是唤醒/唤醒识别任务
     *
     * @return true is wakeup or wakeupasr, false otherwise
     */
    private boolean isWakeup() {
        return (TextUtils.equals(tag, "WakeupProcessor")
                || (TextUtils.equals(tag, "VprintProcessor")));
    }

    /**
     * 销毁录音机
     *
     * @deprecated
     */
    @Deprecated
    protected void releaseRecorder() {
        // unRegisterRecorderIfIsRecording 时如果没有了 listener 就会自动关闭录音机
    }

    /**
     * 注销当前录音的回调，如果内部listener数量为空，则会销毁录音机
     *
     * @param listener 回调
     */
    protected void unRegisterRecorderIfIsRecording(AIRecordListener listener) {
        Log.i(tag, "unRegisterRecorderIfIsRecording");
        cancelMaxSpeechTimeTask();
        cancelNoSpeechTimer();
        if (mAIRecorder != null && mAIRecorder.isRecording(listener)) {
            Log.d(tag, "detect recording , stop recorder!");
            mAIRecorder.unRegisterRecorder(listener);
            mAIRecorder = null;
            Log.i(tag, "releaseRecorder");
        }
    }

    public void processInit(int status) {
        if (status == AIConstant.OPT_FAILED) {
            isInitFailed = true;
        }
        try {
            if (mBarrier != null) {
                mBarrier.await();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } catch (BrokenBarrierException e1) {
            e1.printStackTrace();
        }
    }

    /**
     * 启动无语音检测定时器
     */
    protected void startNoSpeechTimer(SpeechParams params) {
        if (mNoSpeechTimeoutTask != null) {
            mNoSpeechTimeoutTask.cancel();
            mNoSpeechTimeoutTask = null;
        }
        mNoSpeechTimeoutTask = new NoSpeechTimeoutTask();
        try {
            if (params.getNoSpeechTimeout() > 0) {
                Log.d(tag, "start no Speech timeout task time is set to:" + params.getNoSpeechTimeout());
                AITimer.getInstance().schedule(mNoSpeechTimeoutTask, params.getNoSpeechTimeout());
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    /**
     * 取消无语音检测定时器
     */
    protected void cancelNoSpeechTimer() {
        if (mNoSpeechTimeoutTask != null) {
            mNoSpeechTimeoutTask.cancel();
            mNoSpeechTimeoutTask = null;
        }
    }

    protected void startMaxSpeechTimerTask(SpeechParams params) {
        if (mMaxSpeechTimerTask != null) {
            mMaxSpeechTimerTask.cancel();
            mMaxSpeechTimerTask = null;
        }
        if (params.getMaxSpeechTimeS() > 0) {
            mMaxSpeechTimerTask = new MaxSpeechTimerTask();
            try {
                AITimer.getInstance().schedule(mMaxSpeechTimerTask, (long) params.getMaxSpeechTimeS() * 1000);
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    protected void cancelMaxSpeechTimeTask() {
        if (mMaxSpeechTimerTask != null) {
            mMaxSpeechTimerTask.cancel();
            mMaxSpeechTimerTask = null;
        }
    }

    protected void startDumpWaitingTimerTask(String dumpPath) {
        if (mNeedDumpData) {
            if (mExecutorService == null) {
                mExecutorService = Executors.newSingleThreadScheduledExecutor(
                        new AIThreadFactory(tag.replace("Processor", "_dump"))
                );
            }
            DumpWaitingTimerTask dumpWaitingTimerTask = new DumpWaitingTimerTask(dumpPath);
            mExecutorService.schedule(dumpWaitingTimerTask, 500, TimeUnit.MILLISECONDS);
            Log.d(tag, "need wait 500ms when wkp");
        }
    }

    /**
     * 上传预唤醒/唤醒音频
     *
     * @param recordId
     */
    private int uploadWakeupAudio(int wakeupType, String recordId) {
        String wakeupTypeStr = (wakeupType == LOG_WK_TYPE_WAKEUP) ? "wakeup" : "preWakeup";
        String filePath;
        if (TextUtils.isEmpty(AISpeech.uploadAudioPath)) {
            if (Environment.isExternalStorageEmulated())
                filePath = AISpeech.getContext().getExternalCacheDir()
                        .getPath() + File.separator + "gourd" + File.separator +
                        wakeupTypeStr + File.separator + recordId + ".pcm";
            else
                filePath = AISpeech.getContext().getCacheDir()
                        .getPath() + File.separator + "gourd" + File.separator +
                        wakeupTypeStr + File.separator + recordId + ".pcm";
        } else {
            filePath = AISpeech.uploadAudioPath + File.separator +
                    wakeupTypeStr + File.separator + recordId + ".pcm";
        }
        int audioLength;
        FileUtil fileUtil = new FileUtil();
        Log.d(tag,"uploadWakeupAudio "+filePath + "   uploadEnable:"+AISpeech.cacheUploadEnable);
        if (AISpeech.cacheUploadEnable) {
            fileUtil.createFile(filePath);
        }
        synchronized (mLock) {
            audioLength = mCurrentDataSize;
            while (mUploadCacheQueue.peek() != null) {
                byte[] data = mUploadCacheQueue.poll();
                fileUtil.write(data);
            }
            mUploadCacheQueue.clear();
            mCurrentDataSize = 0;
        }
        Log.d(tag, "wakeupAudio prepared ok!");
        mNeedCache = true;
        fileUtil.closeFile();
        FileBuilder fileBuilder = new FileBuilder();
        fileBuilder.setPath(filePath);
        fileBuilder.setFileName(recordId + ".pcm");
        fileBuilder.setEncode(AnalysisProxy.FILE_FORMAT_PCM);
        JSONObject object = new JSONObject();
        try {
            object.put("mode", wakeupTypeStr);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        fileBuilder.setExtraString(object.toString());
        AnalysisProxy.getInstance().getAnalysisAudio(wakeupType).cacheFileBuilder(fileBuilder);
        return audioLength;
    }

    /**
     * 获取 ASR 返回的阈值
     *
     * @param rec    识别热词结果
     * @param params 本地识别参数
     * @return 热词对应的阈值大小
     */
    public double getThreshold(String rec, LocalAsrParams params) {

        if (params.getUseEnglishThreshold() != 0 && isContainsEnglish(rec)) {
            return params.getUseEnglishThreshold();
        }
        //判断是否为自定义热词阈值
        if (!params.getCustomThresholdMap().isEmpty() && params.getCustomThresholdMap().containsKey(rec)) {
            //返回自定义热词所对应的阈值
            return params.getCustomThresholdMap().get(rec);
        } else {
            //返回统一设定的热词所对应的阈值
            return params.getUseThreshold();
        }
    }

    protected void startWakeupUploadWaitingTimerTask() {
        WakeupUploadWaitingTimerTask wakeupUploadWaitingTimerTask = new WakeupUploadWaitingTimerTask();
        try {
            AITimer.getInstance().schedule(wakeupUploadWaitingTimerTask, DEFAULT_VALID_TIME_THRESH);
            Log.d(tag, "need to wait " + DEFAULT_VALID_TIME_THRESH + "ms");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    protected boolean isUploadEnable() {
        return AnalysisProxy.getInstance().getAnalysisAudio(LOG_WK_TYPE_PREWAKEUP).isUploadEnable() ||
                AnalysisProxy.getInstance().getAnalysisAudio(LOG_WK_TYPE_WAKEUP).isUploadEnable();
    }

    protected boolean updateTrails(ProfileState profileState) {
        if (profileState.getAuthType() == ProfileState.AUTH_TYPE.TRIAL
                && profileState.getTimesLimit() != -1) {
            ProfileState state = AIAuthEngine.getInstance().getProfile().isProfileValid(mScope);
            if (!state.isValid()) {
                if (initState == AIConstant.OPT_SUCCESS) {
                    // 离线授权在认证过程中直接忽略
                    AIAuthEngine.getInstance().getProfile().updateUsedTimes(mScope);
                    return true;
                } else {
                    showErrorMessage(state);
                    return false;
                }
            } else {
                AIAuthEngine.getInstance().getProfile().updateUsedTimes(mScope);
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * 授权状态
     *
     * @return boolean
     */
    protected boolean isAuthorized() {
        return mProfileState != null && mProfileState.isValid();
    }

    public void setUseSingleMessageProcess(boolean useSingleMessageProcess) {
        this.useSingleMessageProcess = useSingleMessageProcess;
    }

    public void setCallbackInMainThread(boolean callbackInMainThread) {
        this.callbackInMainThread = callbackInMainThread;
    }

    protected void showErrorMessage() {
        AIError error = new AIError();
        if (mProfileState == null || mProfileState.getAuthErrMsg() == null) {
            error.setErrId(AIError.ERR_SDK_NOT_INIT);
            error.setError(AIError.ERR_DESCRIPTION_ERR_SDK_NOT_INIT);
        } else {
            error.setErrId(mProfileState.getAuthErrMsg().getId());
            error.setError(mProfileState.getAuthErrMsg().getValue());
        }
        if (mOutListener != null) {
            mOutListener.onError(error);
        }
    }

    protected void showErrorMessage(ProfileState state) {
        AIError error = new AIError();
        if (state == null) {
            error.setErrId(AIError.ERR_SDK_NOT_INIT);
            error.setError(AIError.ERR_DESCRIPTION_ERR_SDK_NOT_INIT);
        } else {
            error.setErrId(state.getAuthErrMsg().getId());
            error.setError(state.getAuthErrMsg().getValue());
        }
        if (mOutListener != null) {
            mOutListener.onError(error);
        }
    }

    public void update(String updateStr) {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE, updateStr);
        } else {
            showErrorMessage();
        }
    }

    public void setNeedCopyFeedData(boolean needCopyFeedData) {
        this.needCopyFeedData = needCopyFeedData;
    }

    public boolean isNeedCopyFeedData() {
        return needCopyFeedData;
    }


    public abstract void processNoSpeechError();

    public abstract void processMaxSpeechError();


    /**
     * 类说明： 引擎内部状态列表
     */
    public enum EngineState {
        /**
         * 空闲状态
         */
        STATE_IDLE(0),
        /**
         * 初始化完毕等待就绪状态
         */
        STATE_NEWED(1),
        /**
         * 录音中或运行中状态
         */
        STATE_RUNNING(2),
        /**
         * 录音停止，等待结果状态
         */
        STATE_WAITING(3),
        /**
         * 错误状态
         */
        STATE_ERROR(4),
        /**
         * 取消状态，该状态暂时给callback handler用
         */
        STATE_CANCELED(5);

        private int value;

        private EngineState(int value) {
            this.value = value;
        }

        /**
         * @return the value
         * @Override
         */
        public int getValue() {
            return value;
        }
    }


    /**
     * 引擎内部消息
     */
    public enum EngineMsg {
        /**
         * 引擎init
         */
        MSG_NEW(1),
        /**
         * 引擎start
         */
        MSG_START(2),
        /**
         * 录音机start
         */
        MSG_RECORDER_START(3),
        /**
         * 接收录音机/外部feed的原始pcm音频数据源
         */
        MSG_RAW_RECEIVE_DATA(4),
        /**
         * 经过本地vad之后的pcm音频数据源
         */
        MSG_VAD_RECEIVE_DATA(5),
        /**
         * 引擎stop
         */
        MSG_STOP(6),
        /**
         * 引擎cancel
         */
        MSG_CANCEL(7),
        /**
         * 引擎destroy
         */
        MSG_RELEASE(8),
        /**
         * 内部error
         */
        MSG_ERROR(9),
        /**
         * 检测到vad开始
         */
        MSG_VAD_START(10),
        /**
         * 检测到vad结束
         */
        MSG_VAD_END(11),
        /**
         * 录音机节点检测到音量的变化
         */
        MSG_VOLUME_CHANGED(12),
        /**
         * 引擎内部消息(识别结果/唤醒等信息)
         */
        MSG_RESULT(13),

        /**
         * 信号处理后的单路数据
         */
        MSG_RESULT_RECEIVE_DATA(14),

        /**
         * doa角度值
         */
        MSG_DOA(15),

        /**
         * 设置参数
         */
        MSG_SET(16),

        /**
         * 声纹抛出的唤醒信息
         */
        MSG_VPRINT_RESULT(17),

        /**
         * 性别识别结果
         */
        MSG_GENDER_RESULT(18),

        /**
         * 内核抛出的唤醒词音频
         */
        MSG_WAKEUP_DATA(19),

        /**
         * 识别结果
         */
        MSG_ASR_RESULT(20),

        /**
         * 送声纹数据
         */
        MSG_VPRINT_DATA(21),

        /**
         * 更新引擎设置
         */
        MSG_UPDATE(22),

        /**
         * 更新vocab
         */
        MSG_UPDATE_VOCAB(23),

        /**
         * VAD 抛出首帧音频
         */
        MSG_VAD_FIRST(24),

        /**
         * 声纹 notify event
         */
        MSG_VPRINT_NOTIFY(25),

        /**
         * fasp
         */
        MSG_FASP_INPUT_WAV_CHAN(26),

        /**
         * AecAgc 设置 voipSwitch。AecAgc去掉后这个就不需要了，保留
         */
        MSG_VOIP_SET(30),

        /**
         * AecAgc 内核 AEC 之后的音频. AecAgc去掉后这个就不需要了，保留
         */
        MSG_VOIP_AEC_DATA(31),

        MSG_VPRINT_TLV(32),

        /**
         * 就近唤醒的中间信息
         */
        MSG_NEAR_INFORMATION(33),
        MSG_VPRINT_KWS(34),
        /**
         * 单麦、多麦唤醒使用。流式音频模式下强制请求唤醒结果，即使没有唤醒也会给出唤醒词为null的结果。
         */
        MSG_FORCE_REQUEST_WAKEUP_RESULT(40),
        /**
         * asr动态update result
         */
        MSG_UPDATE_RESULT(41),
        /**
         * 唤醒点前dumpthresh长度的音频
         */
        MSG_RAW_WAKEUP_RECEIVED_DATA(42),
        /**
         * 输出信号处理估计噪声最大的beam index 信息和该方向的音量信息
         */
        MSG_SEVC_DOA(43),
        /**
         * 输出信号处理估计噪声最大的
         * beam index 信息和该方向的音量信
         * 息
         */
        MSG_SEVC_NOISE(44),

        /**
         * 云端唤醒校验
         */
        MSG_WAKEUP_CLOUD_CHECK(45),

        /**
         * AEC之后的音频数据
         */
        MSG_ECHO_RECEIVE_DATA(46),

        /**
         * 对话NLG播报完成
         */
        MSG_NLG_END(47),
        /**
         * 关闭对话流
         */
        MSG_CLOSE(48),
        /**
         * 回复对话数据
         */
        MSG_FEEDBACK(49),
        /**
         * 触发某个技能
         */
        MSG_TRIGGER_INTENT(50),

        /**
         * 同步多模态数据
         */
        MSG_ASYNC(51),

        /**
         * 回复对话数据(私有云)
         */
        MSG_FEEDBACK_2_PRIV_CLOUD(52),

        /**
         * 引擎 start （通过纯语义模式）
         */
        MSG_START_WITH_TEXT(53),

        /**
         * 开始 Build
         */
        MSG_BUILD(54),

        /**
         * 本地asr多路解码操作
         */
        MSG_DECODER(55),
        /**
         * Build完成
         */
        MSG_BUILD_END(56),
        /**
         * 更新引擎设置完成
         */
        MSG_UPDATE_END(57),

        /**
         * 重启内核
         */
        MSG_RESTART(58),
        /**
         * 更新内置 vocab
         */
        MSG_UPDATE_NAVI_VOCAB(59),
        /**
         * 动态设置vad pauseTime
         */
        MSG_UPDATE_VAD_PAUSETIME(60);

        private int value;

        private EngineMsg(int value) {
            this.value = value;
        }

        public static EngineMsg getMsgByValue(int value) {
            for (EngineMsg msg : EngineMsg.values()) {
                if (value == msg.value) {
                    return msg;
                }
            }
            return null;
        }

        public int getValue() {
            return value;
        }
    }

    public class MyBarrierRunnable implements Runnable {

        @Override
        public void run() {
            com.aispeech.util.Utils.checkThreadAffinity();
            if (isInitFailed) {
                transferState(EngineState.STATE_IDLE);
                if (callbackInMainThread) {
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_INIT, AIConstant.OPT_FAILED);
                } else {
                    if (mOutListener != null) mOutListener.onInit(AIConstant.OPT_FAILED);
                }
            } else {
                transferState(EngineState.STATE_NEWED);
                if (callbackInMainThread) {
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_INIT, AIConstant.OPT_SUCCESS);
                } else {
                    if (mOutListener != null) mOutListener.onInit(AIConstant.OPT_SUCCESS);
                }
            }
        }
    }

    class NoSpeechTimeoutTask extends TimerTask {
        @Override
        public void run() {
            processNoSpeechError();
        }
    }

    class MaxSpeechTimerTask extends TimerTask {

        @Override
        public void run() {
            processMaxSpeechError();
            Log.w(tag, "max speech timeout");
        }
    }

    class DumpWaitingTimerTask extends TimerTask {
        String dumpPath;

        DumpWaitingTimerTask(String dumpPath) {
            this.dumpPath = dumpPath;
        }

        @Override
        public void run() {
            String module = tag.replace("Processor", "");
            String filePath = dumpPath + File.separator + module + (mIsRealWakeup ? "wakeup" : "") + File.separator + Util.getCurrentTimeStamp() + ".pcm";
            FileUtil fileUtil = new FileUtil();
            fileUtil.createFile(filePath);
            synchronized (mDumpLock) {
                while (mDumpWkpDataQueue.peek() != null) {
                    byte[] dumpData = mDumpWkpDataQueue.poll();
                    fileUtil.write(dumpData);
                }
                mDumpWkpDataQueue.clear();
                mDumpCurrentDataSize = 0;
            }
            Log.d(tag, "dump audio at " + filePath);
            fileUtil.closeFile();
        }
    }

    class WakeupUploadWaitingTimerTask extends TimerTask {
        @Override
        public void run() {
            mRecorderId = Utils.getRecorderId();
            int wakeupType;
            if (!mIsRealWakeup && AnalysisProxy.getInstance().getAnalysisAudio(LOG_WK_TYPE_PREWAKEUP).isUploadEnable()) {//misWakeup(only has pre wakeup), need to upload preWakeup audio
                wakeupType = LOG_WK_TYPE_PREWAKEUP;
                Log.d(tag, "pre wakeup happened, gourd prepare");
            } else if (mIsRealWakeup && AnalysisProxy.getInstance().getAnalysisAudio(LOG_WK_TYPE_WAKEUP).isUploadEnable()) {//real wakeup, need to upload real wakeup audio
                wakeupType = LOG_WK_TYPE_WAKEUP;
                Log.d(tag, "real wakeup happened, gourd prepare");
            } else {
                Log.d(tag, "invalid mode, ignore");
                return;
            }
            mNeedCache = false;

            int channel = 1;
            if (BaseProcessor.this instanceof FespxProcessor && mBaseConfig instanceof LocalSignalProcessingConfig) {
                channel = ((FespxProcessor) BaseProcessor.this).getChannelNum((LocalSignalProcessingConfig) mBaseConfig);
            }
            int audioSize = uploadWakeupAudio(wakeupType, mRecorderId);//upload preWakeup/wakeup audio
            int duration = audioSize / (32 * channel);
            Log.d(tag, "audioSize " + audioSize + " duration " + duration);

            Map<String, Object> msgObject = new HashMap<>();
            msgObject.put(KEY_MIC_MATRIX, mMicMatrixStr);
            msgObject.put(KEY_SCENE, mSceneStr);
            msgObject.put(KEY_LOG_WK_TYPE, wakeupType);
            Map<String, Object> entryObject = new HashMap<>();
            //添加message外面的字段
            entryObject.put("wakeupType", wakeupType);
            entryObject.put("inputContinuousAudio", inputContinuousAudio);
            entryObject.put("audioUrl", mRecorderId + ".pcm");
            entryObject.put("duration", duration);
            entryObject.put("recordId", mRecorderId);
            entryObject.put("mode", "lite");
            entryObject.put("module", "local_wakeup");
            msgObject.put(KEY_UPLOAD_ENTRY, entryObject);
            AnalysisProxy.getInstance().getAnalysisAudio(wakeupType).cacheData("local_wakeup_input_output",
                    "info", "local_wakeup", mRecorderId, mBaseConfig.toJson(), mWakeupJson, msgObject);//upload preWakeup/wakeup json
            AnalysisProxy.getInstance().getAnalysisAudio(wakeupType).start();
        }
    }

    class OneShotNoSpeechTimeoutTask extends TimerTask {
        @Override
        public void run() {
            processOneShotNoSpeechError();
        }
    }

    public void processOneShotNoSpeechError() {
        Log.i(tag, "---processOneShotNoSpeechError()");
    }

    protected void startOneShotNoSpeechTimer(SpeechParams params) {
        if (mOneShotNoSpeechTimeoutTask != null) {
            mOneShotNoSpeechTimeoutTask.cancel();
            mOneShotNoSpeechTimeoutTask = null;
        }
        mOneShotNoSpeechTimeoutTask = new OneShotNoSpeechTimeoutTask();
        try {
            if (params.getNoSpeechTimeout() > 0) {
                Log.d(tag, "OneShot.TIMEOUT");
                Log.d(tag, "start one-shot no Speech timeout task time is set to:" + ONE_SHOT_TIMEOUT);
                AITimer.getInstance().schedule(mOneShotNoSpeechTimeoutTask, ONE_SHOT_TIMEOUT);
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    protected void cancelOneShotNoSpeechTimer() {
        if (mOneShotNoSpeechTimeoutTask != null) {
            mOneShotNoSpeechTimeoutTask.cancel();
            mOneShotNoSpeechTimeoutTask = null;
        }
    }

    public boolean isContainsEnglish(String rec) {
        boolean hasEnglish = com.aispeech.export.itn.Utils.hasEnglish(rec);
        return hasEnglish;
    }
}
