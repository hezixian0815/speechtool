package com.aispeech.lite.dm;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
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
 * DDS云端识别 processor
 *
 * @author hehr
 */
public class CloudDmProcessor extends BaseProcessor implements IDmProcessor, ICInfo {

    private CloudDmKernel mCloudDmKernel;

    private SpeechParams mAsrParams;

    private CloudDMConfig mAsrConfig;

    private BaseKernel mVadKernel;

    private VadParams mVadParams;

    private LocalVadConfig mVadConfig;

    private static final String TAG = "CloudDmProcessor";

    protected SpeechListener mOutListener;

    /**
     * 对话解析
     */
    private Dispatcher mDispatcher;

    /**
     * native api timer
     */
    private Timer mTimer;

    /**
     * 用来标识是否 VAD_BEGIN （开始说话）
     * <p>
     * 新增该标识为了解决启动对话后不识别的问题，使用场景为：上层 app 在对话中重新启动一个新的对话，
     * 会先结束当前的会话，即会执行 stopVadKernel 的操作，而停止 vadKernel 是异步的操作，
     * 就会偶现触发 VAD_END 的情况；而 对话 start 就检测到 VAD_END （VAD_END时会停止 vad 内核）就会
     * 使得后面的说话都无法被检测到，从而导致一直无法被识别的问题。
     */
    private boolean isVadBegin;

    @Override
    public void init(SpeechListener listener, CloudDMConfig asrConfig, LocalVadConfig vadConfig) {
        this.mOutListener = listener;
        if (vadConfig.isVadEnable() && !asrConfig.isUseRefText()) {
            threadCount++;
        }
        mAsrConfig = asrConfig;
        mVadConfig = vadConfig;
        mScope = Scope.CLOUD_MODEL;
        init(listener, asrConfig.getContext(), TAG);
        if (mCloudDmKernel == null) {
            mCloudDmKernel = new CloudDmKernel(new DDSCloudAsrListenerImpl());
            mCloudDmKernel.setProfile(mProfile);
        }
        if (mDispatcher == null) {
            mDispatcher = new Dispatcher(new DispatcherListenerImpl(), mAsrConfig.isDMRoute());
        }
        if (mSession == null) {
            mSession = new Session();
        }
        if (mTimer == null) {
            mTimer = new Timer(mAsrConfig.getNativeApiTimeout());
        }
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }

    @Override
    public void start(SpeechParams asrParams, VadParams vadParams) {
        if (isAuthorized()) {
            this.mAsrParams = asrParams;
            if (mDispatcher != null) {
                mDispatcher.setEnableAlignment(((CloudSemanticParams) mAsrParams).isEnableAlignment());
            }
            this.mVadParams = vadParams;
            mSession.clearId();
            if (!TextUtils.isEmpty(asrParams.getSessionId())) {
                mSession.syncId(asrParams.getSessionId());
            }
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage();
        }
    }


    @Override
    public void startWithText(SpeechParams asrParams, VadParams vadParams) {
        if (isAuthorized()) {
            this.mAsrParams = asrParams;
            this.mVadParams = vadParams;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START_WITH_TEXT, null);
        } else {
            showErrorMessage();
        }
    }

    /**
     * 外部通知引擎内部，nlg播报完成
     */
    @Override
    public void notifyNlgEnd() {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_NLG_END, null);
        } else {
            showErrorMessage();
        }
    }

    /**
     * 回复对话数据
     *
     * @param widget {@link FeedbackWidget}
     */
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

    /**
     * 主动触发技能
     *
     * @param intent    {@link SkillIntent}
     * @param asrParams {@link SpeechParams}
     * @param vadParams {@link VadParams}
     */
    @Override
    public void triggerIntent(SkillIntent intent, SpeechParams asrParams, VadParams vadParams) {
        if (isAuthorized()) {
            this.mAsrParams = asrParams;
            this.mVadParams = vadParams;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_TRIGGER_INTENT, new TriggerIntentParams(intent));
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void async(MultiModal multiModal) {
        if (isAuthorized()) {
            if (TextUtils.isEmpty(multiModal.getSessionId())) {
                multiModal.setSessionId(mSession.getId());
            }
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ASYNC, new MultiModalParams(multiModal));
        } else {
            showErrorMessage();
        }
    }

    /**
     * 外部主动结束对话流程
     */
    @Override
    public void close() {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_CLOSE, null);
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void clearObject() {
        super.clearObject();

        if (mCloudDmKernel != null) {
            mCloudDmKernel.releaseKernel();
            mCloudDmKernel = null;
        }
        if (mTimer != null) {
            mTimer.clear();
            mTimer = null;
        }
        if (mDispatcher != null) {
            mDispatcher.release();
            mDispatcher = null;
        }
        if (mSession != null)
            mSession = null;
        if (mAsrParams != null)
            mAsrParams = null;
        if (mAsrConfig != null)
            mAsrConfig = null;
        if (mVadKernel != null)
            mVadKernel = null;
        if (mVadParams != null)
            mVadParams = null;
        if (mVadConfig != null)
            mVadConfig = null;
        if (mOutListener != null)
            mOutListener = null;

    }

    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {

        switch (engineMsg) {

            case MSG_NEW:
                if (mState == EngineState.STATE_IDLE) {
                    if (!mAsrConfig.isUseRefText()) {//非语义
                        if (mVadConfig.isVadEnable()) {
                            int status = copyAssetsRes(mVadConfig);
                            if (status == AIConstant.OPT_FAILED) {
                                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                                break;
                            }
                            mVadKernel = new VadKernel("CloudDM", new MyVadKernelListener());
                            mVadKernel.newKernel(mVadConfig);
                        }
                    }
                    mCloudDmKernel.newKernel(mAsrConfig);
                    transferState(EngineState.STATE_NEWED);

                } else {
                    trackInvalidState("new");
                }

                break;
            case MSG_START:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
                    if (mDispatcher != null && mDispatcher.hasCache()) {
                        mDispatcher.clearCache();
                    }
                    syncRecorderId(Utils.get_recordid(), mAsrParams, mVadParams);
                    mAsrParams.setSessionId(mSession.getId());
                    if (mAsrParams.getOneshotCache() != null) {
                        mCloudDmKernel.setOneShotCache(mAsrParams.getOneshotCache());
                        startOneShotNoSpeechTimer(mAsrParams);
                    }
                    Log.i(TAG, "isUseCustomFeed: "+mAsrParams.isUseCustomFeed());
                    if (mAsrParams.isUseCustomFeed()) {
                        Log.i(TAG, "isUseCustomFeed");
                        mCloudDmKernel.startKernel(mAsrParams);
                        if (mVadConfig.isVadEnable()) {
                            isVadBegin = false;
                            mVadKernel.startKernel(mVadParams);
                            startNoSpeechTimer(mAsrParams);
                        }
                        //notify ready to speech
                        if (mOutListener != null) {
                            mOutListener.onReadyForSpeech();
                        }
                        transferState(EngineState.STATE_RUNNING);
                    } else {
                        if (mAIRecorder == null) {
                            if (AISpeech.getRecoderType() == RecorderConfig.TYPE_COMMON_MIC ||//音频来源于AudioRecorder节点
                                    AISpeech.getRecoderType() == RecorderConfig.TYPE_COMMON_ECHO) {
                                mAIRecorder = createRecorder(CloudDmProcessor.this);
                                if (mAIRecorder == null) {
                                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                                            AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE));
                                    return;
                                }
                            }
                        }
                        // 启动SDK内部录音机
                        startRecorder(mAsrParams, CloudDmProcessor.this);
                    }
                } else {
                    trackInvalidState("start");
                }

                break;
            case MSG_START_WITH_TEXT:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING || mState == EngineState.STATE_RUNNING) {

                    if (mState != EngineState.STATE_RUNNING) {
                        if (mDispatcher != null && mDispatcher.hasCache()) {
                            mDispatcher.clearCache();
                        }
                        mSession.clearId();
                    }

                    syncRecorderId(Utils.get_recordid(), mAsrParams, mVadParams);
                    mAsrParams.setSessionId(mSession.getId());

                    if (mCloudDmKernel != null) {
                        mAsrParams.setTopic(CloudSemanticParams.TOPIC_NLU_INPUT_TEXT);//设置输入文本模式
                        mCloudDmKernel.startKernel(mAsrParams);
                        transferState(EngineState.STATE_RUNNING);
                    }
                } else {
                    trackInvalidState("startWithText");
                }
                break;
            case MSG_RECORDER_START:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
                    syncRecorderId(mAsrParams, mVadParams);
                    mCloudDmKernel.startKernel(mAsrParams);
                    if (mVadConfig.isVadEnable()) {
                        startNoSpeechTimer(mAsrParams);
                        isVadBegin = false;
                        mVadKernel.startKernel(mVadParams);
                    }
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("recorder start");
                }

                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(CloudDmProcessor.this);
                    mCloudDmKernel.stopKernel();
                    if (mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_WAITING);
                } else {
                    trackInvalidState("stop");
                }

                break;
            case MSG_CANCEL:
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    unRegisterRecorderIfIsRecording(CloudDmProcessor.this);
                    mCloudDmKernel.cancelKernel();
                    if (mVadConfig != null && mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("cancel");
                }
                break;

            case MSG_ERROR:
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
                    unRegisterRecorderIfIsRecording(CloudDmProcessor.this);
                    mCloudDmKernel.close();
                    if (mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                    Log.w(TAG, error.toString());
//                    uploadError(error);
//                    Upload.startUploadTimer();
                    if (error.getErrId() == AIError.ERR_DNS) {
                        error.setErrId(AIError.ERR_NETWORK);
                        error.setError(AIError.ERR_DESCRIPTION_ERR_NETWORK);
                    }
                    if (mOutListener != null) {
                        mOutListener.onError(error);
                    }
                } else {
                    trackInvalidState("error");
                }

                break;

            case MSG_RAW_RECEIVE_DATA:
                final byte[] rawBufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mOutListener != null) {
                        mOutListener.onRawDataReceived(rawBufferData, rawBufferData.length);
                    }
                }
                break;

            case MSG_RESULT_RECEIVE_DATA:
                //此MSG包含录音机/自定义feed音频来源
                final byte[] bufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mVadConfig.isVadEnable()) {//送vad模块，vad处理后再送asr
                        mVadKernel.feed(bufferData);
                    } else {
                        mCloudDmKernel.feed(bufferData);
                    }
                    //sendMsgToCallbackMsgQueue(CallbackMsg.MSG_BUFFER_RECEIVED, bufferData);
                    if (mOutListener != null) {
                        mOutListener.onResultDataReceived(bufferData, bufferData.length, 0);
                    }
                }
                break;
            case MSG_VAD_RECEIVE_DATA:
                final byte[] vadData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    mCloudDmKernel.feed(vadData);
                }
                break;
            case MSG_VAD_START:
                if (mState == EngineState.STATE_RUNNING) {
                    Log.d(TAG, "VAD.BEGIN");
                    cancelNoSpeechTimer();
                    cancelOneShotNoSpeechTimer();
                    startMaxSpeechTimerTask(mAsrParams);
                    if (mOutListener != null) {
                        mOutListener.onBeginningOfSpeech();
                    }
                } else {
                    trackInvalidState("VAD.BEGIN");
                }
                break;

            case MSG_VAD_END:
                if (mState == EngineState.STATE_RUNNING) {
                    Log.d(TAG, "VAD.END");
                    unRegisterRecorderIfIsRecording(CloudDmProcessor.this);
                    mCloudDmKernel.stopKernel();
                    if (mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_WAITING);
                    if (mOutListener != null) {
                        mOutListener.onEndOfSpeech();
                    }
                } else {
                    trackInvalidState("VAD.END");
                }
                break;
            case MSG_VOLUME_CHANGED:
                float rmsDb = (float) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mOutListener != null) {
                        mOutListener.onRmsChanged(rmsDb);
                    }
                } else {
                    trackInvalidState("volume changed");
                }
                break;
            case MSG_RESULT:
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    AIResult result = (AIResult) msg.obj;
                    try {
                        if (mDispatcher != null) {
                            mDispatcher.deal(new JSONObject(result.getResultObject().toString()), getRecorderId());
                        }
                        JSONObject retObj = new JSONObject(result.getResultObject().toString());
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
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(CloudDmProcessor.this);
                    }
                    releaseRecorder();
                    cancelNoSpeechTimer();
                    mCloudDmKernel.releaseKernel();
                    mCloudDmKernel = null;
                    if (mVadKernel != null) {
                        mVadKernel.releaseKernel();
                        mVadKernel = null;
                    }
                    clearObject();//清除实例
                    transferState(EngineState.STATE_IDLE);

                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_NLG_END:
                if (mState == EngineState.STATE_WAITING) {
                    mDispatcher.notifyNlgEnd();
                } else {
                    trackInvalidState("nlg.end");
                }
                break;

            case MSG_CLOSE:
                if (mState != EngineState.STATE_IDLE) {
                    String sessionId = (String) msg.obj;
                    Log.d(TAG, "close session id:" + sessionId);
                    if (mOutListener != null) {
                        mOutListener.onEnd(sessionId);
                    }
                    mSession.clearId();
                    clearRecorderId();
                    unRegisterRecorderIfIsRecording(CloudDmProcessor.this);
                    mCloudDmKernel.close();
                    if (mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("close");
                }
                break;
            case MSG_FEEDBACK:
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    mCloudDmKernel.feedback((FeedbackParams) msg.obj);
                    if (mNativeApi != null) {
                        mTimer.cancel(mNativeApi.getApi());
                    }
                } else {
                    trackInvalidState("feedback");
                }
                break;
            case MSG_TRIGGER_INTENT:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_RUNNING) {
                    syncRecorderId(Utils.get_recordid());
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
            case MSG_ASYNC:
                if (mState != EngineState.STATE_IDLE) {
                    mCloudDmKernel.async((MultiModalParams) msg.obj);
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
            default:
                break;
        }

    }


    @Override
    public void processNoSpeechError() {
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
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                AIError.ERR_MAX_SPEECH, AIError.ERR_DESCRIPTION_MAX_SPEECH));
    }

    @Override
    public void processOneShotNoSpeechError() {
        super.processOneShotNoSpeechError();
        sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_END, null);
    }

    private class MyVadKernelListener implements VadKernelListener {


        @Override
        public void onInit(int status) {
            Log.i(TAG, "MyVadKernelListener onInit : " + status);
            processInit(status);
        }

        @Override
        public void onVadStart(String recordID) {
            isVadBegin = true;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_START, null);
        }

        @Override
        public void onVadEnd(String recordID) {
            if (!isVadBegin) {
                Log.e(TAG, "filter out abnormal VAD end.");
                return;
            }
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_END, null);
        }

        @Override
        public void onRmsChanged(float rmsDb) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VOLUME_CHANGED, rmsDb);
        }

        @Override
        public void onBufferReceived(byte[] data) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_RECEIVE_DATA, data);
        }

        @Override
        public void onResults(String result) {

        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
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

    private class DDSCloudAsrListenerImpl implements CloudAsrKernelListener {

        @Override
        public void onInit(int status) {
            Log.i(TAG, "DDSCloudAsrListener onInit : " + status);
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

        }

        @Override
        public void onConnect(boolean isConnected) {
            if (mOutListener != null) {
                mOutListener.onConnect(isConnected);
            }
        }
    }

    /**
     * cache native api
     */
    private NativeApi mNativeApi = null;
    /**
     * 对话 session
     */
    private Session mSession;

    private class DispatcherListenerImpl implements DispatcherListener {

        @Override
        public void onAsr(int eof, String asr) {
            if (!TextUtils.isEmpty(asr)) {
                if (mOutListener != null) {
                    mOutListener.onAsr(eof == 1, asr);
                }
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
            if (callbackWidget != null) {
                if (mOutListener != null) {
                    mOutListener.onDisplay(callbackWidget.getType(), callbackWidget);
                }
            }
        }

        @Override
        public void onSpeak(Speaker speaker) {
            if (speaker != null) {
                if (mOutListener != null) {
                    mOutListener.onPlay(speaker);
                }
            }
        }

        @Override
        public void onCommand(Command command) {
            if (command != null) {
                if (mOutListener != null) {
                    mOutListener.onCall(command);
                }
            }
        }

        @Override
        public void onNativeApi(NativeApi api) {
            if (api != null) {
                mNativeApi = api;
                if (mOutListener != null) {

                    mOutListener.onQuery(api);
                }
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
            Log.e(TAG, "dm error : " + error.toString());
            // 对话错误直接往外抛即可，不改变对话状态和逻辑
            if (mOutListener != null) {
                mOutListener.onError(new AIError(Integer.parseInt(error.getErrId()), error.getErrMsg()));
            }
        }

        @Override
        public void onListen() {
            if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                transferState(EngineState.STATE_NEWED);
            }
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        }

    }

    @Override
    public void startRecording() {
        Log.d(TAG, "startRecording");
        if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
            transferState(EngineState.STATE_NEWED);
        }
        sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
    }

    @Override
    public boolean isConnected() {
        if (mCloudDmKernel != null) {
            return mCloudDmKernel.isConnected();
        }
        return false;
    }
}
