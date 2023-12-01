package com.aispeech.lite.dm;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.ThreadNameUtil;
import com.aispeech.common.Util;
import com.aispeech.export.Command;
import com.aispeech.export.MultiModal;
import com.aispeech.export.NativeApi;
import com.aispeech.export.ProductContext;
import com.aispeech.export.SkillContext;
import com.aispeech.export.SkillIntent;
import com.aispeech.export.Speaker;
import com.aispeech.export.Vocab;
import com.aispeech.export.config.RecorderConfig;
import com.aispeech.export.widget.callback.CallbackWidget;
import com.aispeech.export.widget.feedback.FeedbackWidget;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.asr.CloudAsrKernelListener;
import com.aispeech.lite.audio.AIRecordListener;
import com.aispeech.lite.config.CloudDMConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.dm.dispather.Dispatcher;
import com.aispeech.lite.dm.dispather.DispatcherListener;
import com.aispeech.lite.dm.update.ICInfo;
import com.aispeech.lite.param.CloudSemanticParams;
import com.aispeech.lite.param.FeedbackJsonParams;
import com.aispeech.lite.param.FeedbackParams;
import com.aispeech.lite.param.FeedbackTextParams;
import com.aispeech.lite.param.MultiModalParams;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.param.TriggerIntentParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.vad.VadKernel;
import com.aispeech.lite.vad.VadKernelListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.TimerTask;

/**
 * 全双工对话processor
 * <p>
 * 全双工音频上传逻辑和对话结果分发逻辑完全独立解耦，运转在不同两个线程
 *
 * @author hehr
 */
public class CloudDmFullDuplexProcessor extends BaseProcessor implements IDmProcessor, ICInfo {

    private static final String TAG = "CloudDmFullDuplexProcessor";

    private CloudDmKernel mCloudDmKernel;
    private SpeechParams mDmParams;
    private CloudDMConfig mDmConfig;
    private VadParams mVadParams;
    private LocalVadConfig mVadConfig;
    private SpeechListener mOutListener;
    private Dispatcher mDispatcher;
    private Timer mTimer;
    private final Session mSession = new Session();

    private Timer asyncPlayerStateTimer;
    private static final int ASYNC_PLAYER_STATE_TIMEOUT = 30000;

    private boolean isVadStart;


    @Override
    public void init(SpeechListener listener, CloudDMConfig config, LocalVadConfig vadConfig) {
        this.mOutListener = listener;
        mDmConfig = config;
        mVadConfig = vadConfig;
        if (vadConfig.isVadEnable() && !mDmConfig.isUseRefText()) {
            threadCount++;
        }
        mScope = Scope.CLOUD_MODEL;
        init(listener, config.getContext(), TAG);
        if (mCloudDmKernel == null) {
            mCloudDmKernel = new CloudDmKernel(new KernelListenerImpl());
            mCloudDmKernel.setProfile(mProfile);
        }
        if (mDispatcher == null) {
            mDispatcher = new Dispatcher(new DispatcherListenerImpl(), mDmConfig.isDMRoute(), true);
        }

        if (mTimer == null) {
            mTimer = new Timer(mDmConfig.getNativeApiTimeout());
        }
        if (asyncPlayerStateTimer == null) {
            asyncPlayerStateTimer = new Timer(ASYNC_PLAYER_STATE_TIMEOUT);
        }

        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }

    @Override
    public void start(SpeechParams asrParams, VadParams vadParams) {
        if (isAuthorized()) {
            mDmParams = asrParams;
            if (mDispatcher != null) {
                mDispatcher.setEnableAlignment(((CloudSemanticParams) mDmParams).isEnableAlignment());
            }
            mVadParams = vadParams;
            mSession.clearId();
            if (!TextUtils.isEmpty(mDmParams.getSessionId())) {
                mSession.syncId(mDmParams.getSessionId());
            }
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void startWithText(SpeechParams asrParams, VadParams vadParams) {
        if (isAuthorized()) {
            mDmParams = asrParams;
            mVadParams = vadParams;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START_WITH_TEXT, null);
        } else {
            showErrorMessage();
        }
    }


    private void startByTriggerIntent() {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, "triggerIntent");
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void feedback(FeedbackWidget widget) {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_FEEDBACK, new FeedbackParams(widget)
                    .setRecorderID(getRecorderId())
                    .setSessionId(mSession.getId())
            );
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void feedback2PRIVCloud(String topic, String data) {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_FEEDBACK_2_PRIV_CLOUD,
                    new FeedbackJsonParams(topic, data)
                            .setRecorderId(getRecorderId())
                            .setSessionId(mSession.getId())
            );
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void triggerIntent(SkillIntent intent, SpeechParams asrParams, VadParams vadParams) {
        if (isAuthorized()) {
            this.mDmParams = asrParams;
            this.mVadParams = vadParams;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_TRIGGER_INTENT, new TriggerIntentParams(intent));
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void async(final MultiModal multiModal) {
        if (isAuthorized()) {
            if (TextUtils.isEmpty(multiModal.getSessionId())) {
                multiModal.setSessionId(mSession.getId());
            }
            String playerState = multiModal.getPlayerState();
            if (!TextUtils.isEmpty(playerState)) {
                if ("on".equals(playerState)) {
                    Log.i(TAG, "start player timer.");
                    asyncPlayerStateTimer.start("playerState", new TimerTask() {
                        @Override
                        public void run() {
                            Log.i(TAG, "send a ping.");
                            if (mCloudDmKernel != null) {
                                mCloudDmKernel.sendPing();
                                sendMsgToInnerMsgQueue(EngineMsg.MSG_ASYNC, new MultiModalParams(multiModal));
                            }
                        }
                    }, ASYNC_PLAYER_STATE_TIMEOUT);
                } else {
                    Log.i(TAG, "cancel player timer.");
                    asyncPlayerStateTimer.cancel("playerState");
                }
            }
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ASYNC, new MultiModalParams(multiModal));

            if (mDmConfig.isUseFullDuplexNoSpeechTimeOut()) {
                playerState = multiModal.getPlayerState();
                if (!TextUtils.isEmpty(playerState) && "off".equals(playerState) && !isVadStart) {
                    startNoSpeechTimer(mDmParams);
                }
            }

        } else {
            showErrorMessage();
        }
    }


    @Override
    public void close() {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_CLOSE, null);
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void notifyNlgEnd() {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_NLG_END, null);
        } else {
            showErrorMessage();
        }
    }

    @Override
    public boolean isConnected() {
        if (mCloudDmKernel != null) {
            return mCloudDmKernel.isConnected();
        }
        return false;
    }

    /**
     * 上行音频流线程
     */
    private final HandlerThread mUpstreamThread = new HandlerThread(ThreadNameUtil.getSimpleThreadName("upstream"));

    private Handler mUpstreamHandler;

    private UpstreamCallback mUpstreamCallback;

    /**
     * 创建上行音频流的handler
     *
     * @param t {@link HandlerThread}
     * @return {@link Handler}
     */
    private Handler createUpstream(HandlerThread t) {
        t.start();
        mUpstreamCallback = new UpstreamCallback();
        return new Handler(t.getLooper(), mUpstreamCallback);
    }

    private void releaseUpstream() {
        if (mUpstreamCallback != null) {
            mUpstreamCallback.release();
            mUpstreamCallback = null;
        }
    }

    @Override
    public void uploadVocabs(Vocab... vocabs) {
        if (mCloudDmKernel != null) {
            mCloudDmKernel.uploadVocabs(vocabs);
        }
    }

    @Override
    public void uploadProductContext(ProductContext context) {
        if (mCloudDmKernel != null) {
            mCloudDmKernel.uploadProductContext(context);
        }
    }

    @Override
    public void uploadSkillContext(SkillContext context) {
        if (mCloudDmKernel != null) {
            mCloudDmKernel.uploadSkillContext(context);
        }
    }

    private class UpstreamCallback implements Handler.Callback, AIRecordListener, VadKernelListener {

        private BaseKernel mVadKernel;

        public UpstreamCallback() {
            if (mVadConfig.isVadEnable()) {
                if (mVadKernel == null) {
                    mVadKernel = new VadKernel("CloudDM", this);
                }
                mVadKernel.newKernel(mVadConfig);
            }
        }

        @Override
        public boolean handleMessage(Message message) {

            switch (message.what) {
                case UpMsg.BEGIN:
                    Log.d(TAG, "upstream begin .");
                    if (mDmParams.getOneshotCache() != null) {
                        mCloudDmKernel.setOneShotCache(mDmParams.getOneshotCache());
                        startOneShotNoSpeechTimer(mDmParams);
                    }
                    if (mDmParams.isUseCustomFeed()) {
                        Log.i(TAG, "isUseCustomFeed");
                        mCloudDmKernel.startKernel(mDmParams);
                        if (mVadConfig.isVadEnable()) {
                            if (mVadKernel != null)
                                mVadKernel.startKernel(mVadParams);
                        }
                        if (mDmConfig.isUseFullDuplexNoSpeechTimeOut()) {
                            startNoSpeechTimer(mDmParams);
                        }
                    } else {
                        // 启动SDK内部录音机
                        startRecorder(mDmParams, this);
                    }

                    break;
                case UpMsg.END:
                    Log.d(TAG, "upstream end .");
                    if (!mDmConfig.isUseCustomFeed()) {
                        unRegisterRecorderIfIsRecording(this);
                    }
                    if (mVadConfig.isVadEnable()) {
                        if (mVadKernel != null)
                            mVadKernel.stopKernel();
                    }
                    mCloudDmKernel.close();
                    break;
                case UpMsg.FEED:
                    byte[] bytes = (byte[]) message.obj;
                    if (mVadConfig.isVadEnable()) {
                        if (mVadKernel != null) {
                            mVadKernel.feed(bytes);
                        }
                    } else {
                        mCloudDmKernel.feed(bytes);
                    }

                    break;
                default:
                    break;
            }

            return false;
        }

        @Override
        public void onRecordStarted(long sessionId) {
            mCloudDmKernel.startKernel(mDmParams);
            if (mVadConfig.isVadEnable()) {
                if (mVadKernel != null)
                    mVadKernel.startKernel(mVadParams);
            }
            if (mDmConfig.isUseFullDuplexNoSpeechTimeOut()) {
                startNoSpeechTimer(mDmParams);
            }
        }

        @Override
        public void onRawDataReceived(long sessionId, byte[] buffer, int size) {
            if (mOutListener != null) {
                mOutListener.onRawDataReceived(buffer, size);
            }
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size) {
            //录音机数据，直接feed vad
            if (mVadConfig.isVadEnable()) {
                if (mVadKernel != null) {
                    mVadKernel.feed(buffer);
                }
            } else {
                mCloudDmKernel.feed(buffer);
            }

            if (mOutListener != null) {
                mOutListener.onResultDataReceived(buffer, size, 0);
            }
        }

        @Override
        public void onRecordStopped(long sessionId) {

        }

        @Override
        public void onRecordReleased() {

        }

        @Override
        public void onException(AIError e) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, e);
        }

        @Override
        public String getTag() {
            return TAG;
        }

        @Override
        public void onPowerChanged(boolean isHighPower) {

        }

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onVadStart(String recordID) {
            Log.d(TAG, "---onVadStart() currentState = " + mState
                    + ", recorderId = " + getRecorderId() + ",mSession = " + mSession.getId());
            isVadStart = true;
            cancelNoSpeechTimer();
            cancelOneShotNoSpeechTimer();
            startRecording();
        }

        @Override
        public void onVadEnd(String recordID) {
            if (mOutListener != null) {
                mOutListener.onEndOfSpeech();
            }
            if (mCloudDmKernel != null) {
                mCloudDmKernel.stopKernel();
            }
        }


        @Override
        public void onRmsChanged(float rmsDb) {
            if (mOutListener != null) {
                mOutListener.onRmsChanged(rmsDb);
            }
        }

        @Override
        public void onBufferReceived(byte[] data) {
            if (mCloudDmKernel != null) {
                //Log.d(TAG, "asr feed .... data：" + data.length);
                mCloudDmKernel.feed(data);
            }
        }

        @Override
        public void onResults(String result) {

        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        public void release() {
            unRegisterRecorderIfIsRecording(this);
            releaseRecorder();
            if (mVadKernel != null) {
                mVadKernel.releaseKernel();
                mVadKernel = null;
            }
        }

        @Override
        public void onReadyForSpeech() {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }
    }


    private static class UpMsg {
        /**
         * 开始上传
         */
        public static final int BEGIN = 0x001;
        /**
         * 停止上传
         */
        public static final int END = 0x002;
        /**
         * 外部feed音频
         */
        public static final int FEED = 0x003;

    }

    /**
     * 开始上传音频
     */
    private void beginUpstream() {
        if (mUpstreamHandler != null) {
            Message.obtain(mUpstreamHandler, UpMsg.BEGIN).sendToTarget();
        }
    }


    /**
     * 停止音频上传
     */
    private void endUpstream() {
        if (mUpstreamHandler != null) {
            Message.obtain(mUpstreamHandler, UpMsg.END).sendToTarget();
        }
    }

    /**
     * feed 音频
     *
     * @param bytes byte 音频
     */
    private void feedUpstream(byte[] bytes) {
        if (mUpstreamHandler != null) {
            Message.obtain(mUpstreamHandler, UpMsg.FEED, bytes).sendToTarget();
        }
    }

    /***
     * 下行数据流处理线程
     * @param engineMsg msgId
     * @param msg  bundle
     */
    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW://初始化
                if (mState == EngineState.STATE_IDLE) {
                    if (!mDmConfig.isUseRefText()) {//非语义
                        if (mVadConfig.isVadEnable()) {
                            int status = copyAssetsRes(mVadConfig);
                            if (status == AIConstant.OPT_FAILED) {
                                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                                break;
                            }
                        }
                        mUpstreamHandler = createUpstream(mUpstreamThread);
                    }
                    mCloudDmKernel.newKernel(mDmConfig);
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("new");
                }
                break;
            case MSG_START://启动对话
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
                    asyncPlayerStateTimer.cancel("playerState");
                    isVadStart = false;

                    if (mDispatcher != null && mDispatcher.hasCache()) {
                        mDispatcher.clearCache();
                    }
                    Object from = msg.obj;
                    if ("triggerIntent".equals(from)) {
                        Log.d(TAG, "from triggerIntent.");
                    } else {
                        if (!mDmParams.isUseCustomFeed()) {
                            if (mAIRecorder == null) {
                                if (AISpeech.getRecoderType() == RecorderConfig.TYPE_COMMON_MIC ||//音频来源于AudioRecorder节点
                                        AISpeech.getRecoderType() == RecorderConfig.TYPE_COMMON_ECHO) {
                                    mAIRecorder = createRecorder(this);
                                    if (mAIRecorder == null) {
                                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                                                AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE));
                                        return;
                                    }
                                }
                            }
                        }
                        syncRecorderId(Utils.get_recordid(), mDmParams, mVadParams);
                        syncSessionId(mSession.getId(), mDmParams);
                    }
                    //notify ready to speech
                    if (mOutListener != null) {
                        mOutListener.onReadyForSpeech();
                    }
                    beginUpstream();//开始上传音频流
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("start");
                }
                break;
            case MSG_START_WITH_TEXT:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING || mState == EngineState.STATE_RUNNING) {
                    asyncPlayerStateTimer.cancel("playerState");
                    if (mState != EngineState.STATE_RUNNING) {
                        if (mDispatcher != null) {
                            if (mDispatcher.hasCache()) {
                                mDispatcher.clearCache();
                            }
                            mDispatcher.resetCounter();
                        }
                        mSession.clearId();
                    }
                    syncRecorderId(Utils.get_recordid(), mDmParams, mVadParams);
                    syncSessionId(mSession.getId(), mDmParams);

                    if (mCloudDmKernel != null) {
                        mDmParams.setTopic(CloudSemanticParams.TOPIC_NLU_INPUT_TEXT);//设置输入文本模式
                        mCloudDmKernel.startKernel(mDmParams);
                        transferState(EngineState.STATE_RUNNING);
                    }
                } else {
                    trackInvalidState("startWithText");
                }

                break;
            case MSG_CLOSE://关闭对话
                if (mState != EngineState.STATE_IDLE) {
                    asyncPlayerStateTimer.cancel("playerState");
                    String sessionId = (String) msg.obj;
                    Log.d(TAG, "close session id:" + sessionId);
                    if (mOutListener != null) {
                        mOutListener.onEnd(sessionId);
                    }
                    mSession.clearId();
                    clearRecorderId();
                    endUpstream(); //结束音频流
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("close");
                }
                break;
            case MSG_RESULT_RECEIVE_DATA:
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    feedUpstream((byte[]) msg.obj);
                }
                break;
            case MSG_ERROR://错误
                AIError error = (AIError) msg.obj;
                if (TextUtils.isEmpty(error.getRecordId())) {
                    error.setRecordId(Utils.get_recordid());
                }
                if (error.getErrId() == AIError.ERR_RES_PREPARE_FAILED) {
                    Log.w(TAG, error.toString());
                    if (mOutListener != null) {
                        mOutListener.onError(error);
                    }
                    return;
                }

                if (mState == EngineState.STATE_IDLE) {
                    if (mOutListener != null) {
                        mOutListener.onError(error);
                    }
                    return;
                }

                if (mState != EngineState.STATE_NEWED && mState != EngineState.STATE_IDLE) {
                    Log.w(TAG, error.toString());
                    if (error.getErrId() == 10403 || error.getErrId() == 10416
                            || error.getErrId() == AIError.ERR_DNS
                            || error.getErrId() == AIError.ERR_NETWORK
                            || error.getErrId() == AIError.ERR_CONNECT_TIMEOUT) {
                        transferState(EngineState.STATE_NEWED);
                    } else {
                        transferState(EngineState.STATE_RUNNING);
                    }
                    if (mOutListener != null) {
                        mOutListener.onError(error);
                    }
                } else {
                    trackInvalidState("error");
                }

                break;
            case MSG_FEEDBACK://feedback 对话数据
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    mCloudDmKernel.feedback((FeedbackParams) msg.obj);
                    if (mNativeApi != null) {
                        mTimer.cancel(mNativeApi.getApi());
                    }
                } else {
                    trackInvalidState("feedback");
                }
                break;
            case MSG_TRIGGER_INTENT: //trigger
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_RUNNING
                        || mState == EngineState.STATE_WAITING) {
                    syncRecorderId(Utils.get_recordid(), mDmParams, mVadParams);
                    syncSessionId(mSession.getId(), mDmParams);
                    startByTriggerIntent(); //全双工场景下trigger需要先启动对话
                    TriggerIntentParams intentParams = (TriggerIntentParams) msg.obj;
                    SkillIntent skillIntent = intentParams.getIntent();
                    skillIntent.setRecordId(getRecorderId());
                    skillIntent.setSessionId(mSession.getId());
                    mCloudDmKernel.triggerIntent(intentParams);
                    transferState(EngineState.STATE_WAITING);
                } else {
                    trackInvalidState("trigger.intent");
                }
                break;
            case MSG_RESULT://解析结果
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    AIResult result = (AIResult) msg.obj;
                    try {
                        JSONObject retObj = new JSONObject(result.getResultObject().toString());
                        if (mDispatcher != null) {
                            mDispatcher.deal(retObj, getRecorderId());
                        }
                        if (retObj.has(Protocol.DM) && mOutListener != null) {
                            mOutListener.onHasDmResult(result.getResultObject().toString());
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (mOutListener != null) {
                            mOutListener.onError(new AIError(AIError.ERR_INVALID_DM_RESULT, AIError.ERR_DESCRIPTION_ERR_INVALID_DM_RESULT));
                        }
                    }
                } else {
                    trackInvalidState("result");
                }
                break;
            case MSG_NLG_END://nlg 播报结束，外部主动通知
                if (mState == EngineState.STATE_WAITING || mState == EngineState.STATE_RUNNING) {
                    mDispatcher.notifyNlgEnd();
                } else {
                    trackInvalidState("nlg.end");
                }
                break;

            case MSG_RELEASE://release
                if (mState != EngineState.STATE_IDLE) {
                    asyncPlayerStateTimer.cancel("playerState");
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(this);
                    }
                    releaseRecorder();
                    cancelNoSpeechTimer();
                    mCloudDmKernel.releaseKernel();
                    mCloudDmKernel = null;
                    releaseUpstream();
                    clearObject();//清除实例
                    transferState(EngineState.STATE_IDLE);
                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_ASYNC:
                if (mState != EngineState.STATE_IDLE) {
                    MultiModalParams multiModalParams = (MultiModalParams) msg.obj;
                    mCloudDmKernel.async(multiModalParams);
                } else {
                    trackInvalidState("async");
                }
                break;
            case MSG_FEEDBACK_2_PRIV_CLOUD:
                if (mState != EngineState.STATE_IDLE) {
                    mCloudDmKernel.feedback2PRIVCloud((FeedbackJsonParams) msg.obj);
                } else {
                    trackInvalidState("feedback.priv.cloud");
                }
                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    if (mOutListener != null) {
                        mOutListener.onEndOfSpeech();
                    }
                    if (mCloudDmKernel != null) {
                        mCloudDmKernel.stopKernel();
                    }
                }
                break;
            default:
                break;
        }
    }

    private NativeApi mNativeApi;//cache nativeApi


    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void startRecording() {
        if (mState == EngineState.STATE_RUNNING) {
            if (mOutListener != null) {
                mOutListener.onBeginningOfSpeech();
            }
            if (TextUtils.isEmpty(mSession.getId())) {
                Log.w(TAG, "startKernel fromVadStart, mSessionId is null, ignore! ");
                return;
            }
            if (mCloudDmKernel != null) {
                syncRecorderId(Util.uuid(), mDmParams, mVadParams);
                syncSessionId(mSession.getId(), mDmParams);
                mDispatcher.resetCounter();
                mCloudDmKernel.startKernel(mDmParams);
            }
        }
    }

    @Override
    public void processNoSpeechError() {
        //全双工模式下，不再抛出
        Log.w(TAG, "no speech timeout!");
        /*
         * 若在指定的一段时间内没有说话，SDK主动发一个空报文（文本请求参数）给云端，云端作为空识别处理；
         * 首轮对话请求不需要携带；非首轮对话请求取上一次服务端返回结果中的sessionId。
         */
        String recordId = !TextUtils.isEmpty(getRecorderId()) ? getRecorderId() : Utils.get_recordid();
        FeedbackParams textParams = new FeedbackTextParams(null)
                .setRefText("")
                .setRecorderID(recordId);
        if (!TextUtils.isEmpty(mSession.getId())) {
            textParams.setSessionId(mSession.getId());
        }

        sendMsgToInnerMsgQueue(EngineMsg.MSG_FEEDBACK, textParams);
    }

    @Override
    public void processMaxSpeechError() {
        //全双工模式下，不再抛出
    }

    @Override
    public void processOneShotNoSpeechError() {
        super.processOneShotNoSpeechError();
        if (mOutListener != null) {
            mOutListener.onEndOfSpeech();
        }
        if (mCloudDmKernel != null) {
            mCloudDmKernel.stopKernel();
        }
    }

    private class KernelListenerImpl implements CloudAsrKernelListener {

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onResults(AIResult result) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, result);
        }

        @Override
        public void onStarted(String recordId) {

        }

        @Override
        public void onUpdateResult(int ret) {
            if (mOutListener != null) {
                mOutListener.onUpdateContext(ret == AIConstant.OPT_SUCCESS);
            }
        }

        @Override
        public void onConnect(boolean isConnected) {
            if (mOutListener != null) {
                mOutListener.onConnect(isConnected);
            }
        }
    }

    private class DispatcherListenerImpl implements DispatcherListener {
        @Override
        public void onAsr(int eof, String asr) {
            if (mOutListener != null) {
                mOutListener.onAsr(eof == 1, asr);
            }
        }

        @Override
        public void onSessionId(String id) {
            if (!TextUtils.isEmpty(id)) {
                mSession.syncId(id);
            }
        }

        @Override
        public void onWidget(CallbackWidget callbackWidget) {
            if (mOutListener != null) {
                mOutListener.onDisplay(callbackWidget.getType(), callbackWidget);
            }
        }

        @Override
        public void onSpeak(Speaker speaker) {
            if (mOutListener != null) {
                mOutListener.onPlay(speaker);
            }
        }

        @Override
        public void onCommand(Command command) {
            if (mOutListener != null) {
                mOutListener.onCall(command);
            }
            if (mState == EngineState.STATE_WAITING) {
                transferState(EngineState.STATE_RUNNING);
            }
        }

        @Override
        public void onNativeApi(NativeApi api) {
            if (mOutListener != null) {
                mNativeApi = api;
                mOutListener.onQuery(api);
                if (mTimer != null) {
                    mTimer.start(api.getApi(), new TimerTask() {
                        @Override
                        public void run() {
                            mNativeApi = null;
                            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_NATIVE_API_TIMEOUT,
                                    AIError.ERR_DESCRIPTION_NATIVE_API_TIMEOUT));
                        }
                    });
                }
            }
        }

        @Override
        public void onWait() {
            if (mState != EngineState.STATE_WAITING) {
                transferState(EngineState.STATE_WAITING);
            }
        }

        @Override
        public void onClose() {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_CLOSE, mSession.getId());
        }

        @Override
        public void onError(Error error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(Integer.parseInt(error.getErrId()), error.getErrMsg()));
        }

        @Override
        public void onListen() {
            // 全双工模式下，vad常开模式，无需重启识别
        }
    }
}