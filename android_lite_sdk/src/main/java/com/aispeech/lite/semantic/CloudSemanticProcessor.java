package com.aispeech.lite.semantic;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.export.SkillIntent;
import com.aispeech.export.config.RecorderConfig;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.asr.CloudAsrKernelListener;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.CloudDMConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.dds.CloudSemanticKernel;
import com.aispeech.lite.dm.Protocol;
import com.aispeech.lite.param.CloudSemanticParams;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.param.TriggerIntentParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.vad.VadKernel;
import com.aispeech.lite.vad.VadKernelListener;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * DDS云端识别 processor
 *
 * @author hehr
 */
public class CloudSemanticProcessor extends BaseProcessor {

    private BaseKernel mDDSCloudAsrKernel;

    private SpeechParams mAsrParams;

    private CloudDMConfig mCloudConfig;

    private BaseKernel mVadKernel;

    private VadParams mVadParams;

    private LocalVadConfig mVadConfig;

    private static final String TAG = "CloudSemanticProcessor";

    protected SpeechListener mListener;
    private long mVadBeginTime;
    private long mVadEndTime;
    private long mStartTime;
    private long mSemanticResultTime;
    private long mAsrResultTime;

    public void init(SpeechListener listener, AIEngineConfig config, LocalVadConfig vadConfig) {

        this.mListener = listener;

        mCloudConfig = (CloudDMConfig) config;
        mVadConfig = vadConfig;
        if (!mCloudConfig.isUseRefText() && vadConfig.isVadEnable()) {
            threadCount++;
        }

        mScope = Scope.CLOUD_MODEL;
        init(listener, config.getContext(), TAG);

        if (mDDSCloudAsrKernel == null) {
            mDDSCloudAsrKernel = new CloudSemanticKernel(new DDSCloudAsrListenerImpl());
            mDDSCloudAsrKernel.setProfile(mProfile);
        }

        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);

    }


    public void start(SpeechParams asrParams, VadParams vadParams) {
        if (isAuthorized()) {
            this.mAsrParams = asrParams;
            this.mVadParams = vadParams;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage();
        }
    }

    /**
     * 外部主动结束连接
     */
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

        if (mDDSCloudAsrKernel != null) {
            mDDSCloudAsrKernel.releaseKernel();
            mDDSCloudAsrKernel = null;
        }

        if (mAsrParams != null)
            mAsrParams = null;
        if (mCloudConfig != null)
            mCloudConfig = null;
        if (mVadKernel != null)
            mVadKernel = null;
        if (mVadParams != null)
            mVadParams = null;
        if (mVadConfig != null)
            mVadConfig = null;
        if (mListener != null)
            mListener = null;

    }

    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {

        switch (engineMsg) {

            case MSG_NEW:

                if (mState == EngineState.STATE_IDLE) {
                    if (!mCloudConfig.isUseRefText()) {//非语义
                        if (!mCloudConfig.isUseCustomFeed()) {
                            if (mAIRecorder == null) {
                                if (AISpeech.getRecoderType() == RecorderConfig.TYPE_COMMON_MIC ||//音频来源于AudioRecorder节点
                                        AISpeech.getRecoderType() == RecorderConfig.TYPE_COMMON_ECHO) {
                                    mAIRecorder = createRecorder(CloudSemanticProcessor.this);
                                    if (mAIRecorder == null) {
                                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                                                AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE));
                                        return;
                                    }
                                }
                            }
                        }
                        if (mVadConfig.isVadEnable()) {
                            int status = copyAssetsRes(mVadConfig);
                            if (status == AIConstant.OPT_FAILED) {
                                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                                break;
                            }
                            mVadKernel = new VadKernel("csem", new MyVadKernelListener());
                            mVadKernel.newKernel(mVadConfig);
                        }
                    }
                    mDDSCloudAsrKernel.newKernel(mCloudConfig);

                } else {
                    trackInvalidState("new");
                }

                break;
            case MSG_START:
                if (mState == EngineState.STATE_NEWED) {
                    mStartTime = System.currentTimeMillis();
                    syncRecorderId(mAsrParams, mVadParams);
                    if (!mCloudConfig.isUseRefText()) {
                        if (mCloudConfig.isUseCustomFeed()) {
                            Log.i(TAG, "isUseCustomFeed");
                            if (mVadConfig.isVadEnable()) {
                                mVadKernel.startKernel(mVadParams);
                                startNoSpeechTimer(mAsrParams);
                            }
                            mDDSCloudAsrKernel.startKernel(mAsrParams);
                            transferState(EngineState.STATE_RUNNING);
                        } else {
                            // 启动SDK内部录音机
                            startRecorder(mAsrParams, CloudSemanticProcessor.this);
                        }
                    } else {
                        mAsrParams.setTopic(CloudSemanticParams.TOPIC_NLU_INPUT_TEXT);//设置输入文本模式
                        mDDSCloudAsrKernel.startKernel(mAsrParams);
                        transferState(EngineState.STATE_RUNNING);
                    }
                } else {
                    trackInvalidState("start");
                }
                break;
            case MSG_CLOSE:
                if (mState != EngineState.STATE_IDLE) {
                    clearRecorderId();
                    unRegisterRecorderIfIsRecording(CloudSemanticProcessor.this);
                    mDDSCloudAsrKernel.close();
                    if (mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("close");
                }
                break;
            case MSG_RECORDER_START:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
                    mDDSCloudAsrKernel.startKernel(mAsrParams);
                    if (mVadConfig.isVadEnable()) {
                        startNoSpeechTimer(mAsrParams);
                        mVadKernel.startKernel(mVadParams);
                    }
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("recorder start");
                }

                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(CloudSemanticProcessor.this);
                    mDDSCloudAsrKernel.stopKernel();
                    if (mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_WAITING);
                } else {
                    trackInvalidState("stop");
                }

                break;
            case MSG_CANCEL:

                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING
                        || mState == EngineState.STATE_NEWED) {
                    unRegisterRecorderIfIsRecording(CloudSemanticProcessor.this);
                    mDDSCloudAsrKernel.cancelKernel();
                    if (mVadConfig != null && mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("cancel");
                }
                break;

            case MSG_UPDATE:
                if (mState != EngineState.STATE_IDLE) {
                    String param = (String) msg.obj;
                    if (!TextUtils.isEmpty(param))
                        mDDSCloudAsrKernel.update(param);
                    else
                        Log.e(TAG, "illegal param!");

                } else {
                    trackInvalidState("update info");
                }
                break;
            case MSG_UPDATE_VOCAB:
                if (mState != EngineState.STATE_IDLE) {
                    String param = (String) msg.obj;
                    if (!TextUtils.isEmpty(param))
                        mDDSCloudAsrKernel.updateVocab(param);
                    else
                        Log.e(TAG, "illegal param!");

                } else {
                    trackInvalidState("update vocab info");
                }
                break;
            case MSG_ERROR:

                AIError error = (AIError) msg.obj;
                if (TextUtils.isEmpty(error.getRecordId())) {
                    error.setRecordId(Utils.get_recordid());
                }
                if (error.getErrId() == AIError.ERR_RES_PREPARE_FAILED) {
                    Log.w(TAG, error.toString());
                    if (mListener != null) {
                        mListener.onError(error);
                    }
//                    uploadError(error);
//                    Upload.startUploadTimer();
                    return;
                }
                if (mState == EngineState.STATE_IDLE) {
                    if (mListener != null) {
                        mListener.onError(error);
                    }
                    return;
                }
                if (mState != EngineState.STATE_NEWED && mState != EngineState.STATE_IDLE) {
                    unRegisterRecorderIfIsRecording(CloudSemanticProcessor.this);
                    mDDSCloudAsrKernel.stopKernel();
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
                    if (mListener != null) {
                        mListener.onError((AIError) msg.obj);
                    }
                } else {
                    trackInvalidState("error");
                }

                break;

            case MSG_RAW_RECEIVE_DATA:
                final byte[] rawBufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mListener != null) {
                        mListener.onRawDataReceived(rawBufferData, rawBufferData.length);
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
                        mDDSCloudAsrKernel.feed(bufferData);
                    }
                    //sendMsgToCallbackMsgQueue(CallbackMsg.MSG_BUFFER_RECEIVED, bufferData);
                    if (mListener != null) {
                        mListener.onResultDataReceived(bufferData, bufferData.length, 0);
                    }
                } else {
                    trackInvalidState("feed");
                }
                break;
            case MSG_VAD_RECEIVE_DATA:
                final byte[] vadData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    mDDSCloudAsrKernel.feed(vadData);
                }
                break;
            case MSG_VAD_START:
                if (mState == EngineState.STATE_RUNNING) {
                    Log.d(TAG, "VAD.BEGIN");
                    cancelNoSpeechTimer();
                    startMaxSpeechTimerTask(mAsrParams);
                    if (mListener != null) {
                        mListener.onBeginningOfSpeech();
                    }
                } else {
                    trackInvalidState("VAD.BEGIN");
                }
                break;

            case MSG_VAD_END:
                if (mState == EngineState.STATE_RUNNING) {
                    Log.d(TAG, "VAD.END");
                    unRegisterRecorderIfIsRecording(CloudSemanticProcessor.this);
                    mDDSCloudAsrKernel.stopKernel();
                    if (mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_WAITING);
                    if (mListener != null) {
                        mListener.onEndOfSpeech();
                    }
                } else {
                    trackInvalidState("VAD.END");
                }
                break;
            case MSG_VOLUME_CHANGED:
                float rmsDb = (float) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mListener != null) {
                        mListener.onRmsChanged(rmsDb);
                    }
                } else {
                    trackInvalidState("volume changed");
                }
                break;
            case MSG_RESULT:
                AIResult result = (AIResult) msg.obj;
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    if (mListener != null) {
                        mListener.onResults(result);
                    }
                    String ret = result.getResultObject().toString();
                    try {
                        JSONObject retObj = new JSONObject(ret);
                        if (retObj.has(Protocol.DM) || retObj.has(Protocol.NLU)) {
                            mSemanticResultTime = System.currentTimeMillis();
                            Log.d(TAG, "ASR.START.SEMANTIC.RESULT.DELAY : " + (mSemanticResultTime - mStartTime));
                            Log.d(TAG, "FINAL.ASR.RESULT.SEMANTIC.RESULT.DELAY : " + (mSemanticResultTime - mAsrResultTime));
                            Log.d(TAG, "VAD.BEGIN.SEMANTIC.RESULT.DELAY : " + (mSemanticResultTime - mVadBeginTime));
                            Log.d(TAG, "VAD.END.SEMANTIC.RESULT.DELAY : " + (mSemanticResultTime - mVadEndTime));
                            transferState(EngineState.STATE_NEWED);
                            unRegisterRecorderIfIsRecording(CloudSemanticProcessor.this);
                        } else {
                            if (result.isLast()) {
                                mAsrResultTime = System.currentTimeMillis();
                            }
                        }
                    } catch (JSONException exception) {
                        exception.printStackTrace();
                    }
                } else {
                    trackInvalidState("result");
                }
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(CloudSemanticProcessor.this);
                    }
                    releaseRecorder();
                    cancelNoSpeechTimer();
                    mDDSCloudAsrKernel.releaseKernel();
                    mDDSCloudAsrKernel = null;
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
            case MSG_TRIGGER_INTENT:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_RUNNING) {
                    if (mState != EngineState.STATE_RUNNING) {
                        syncRecorderId(mAsrParams, mVadParams);
                    }
                    TriggerIntentParams intentParams = (TriggerIntentParams) msg.obj;
                    SkillIntent skillIntent = intentParams.getIntent();
                    skillIntent.setRecordId(getRecorderId());
                    intentParams.setParam(mAsrParams);
                    mDDSCloudAsrKernel.triggerIntent(intentParams);
                    transferState(EngineState.STATE_WAITING);
                } else {
                    trackInvalidState("trigger.intent");
                }
                break;
            default:
                break;
        }

    }

    /**
     * 主动触发技能
     *
     * @param intent    {@link SkillIntent}
     * @param asrParams {@link SpeechParams}
     * @param vadParams {@link VadParams}
     */
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
    public void processNoSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_NO_SPEECH,
                AIError.ERR_DESCRIPTION_NO_SPEECH));
        Log.w(TAG, "no speech timeout!");
    }

    @Override
    public void processMaxSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                AIError.ERR_MAX_SPEECH, AIError.ERR_DESCRIPTION_MAX_SPEECH));
    }


    private class MyVadKernelListener implements VadKernelListener {


        @Override
        public void onInit(int status) {
            Log.i(TAG, "MyVadKernelListener onInit : " + status);
            processInit(status);
        }

        @Override
        public void onVadStart(String recordID) {
            mVadBeginTime = System.currentTimeMillis();
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_START, null);
        }

        @Override
        public void onVadEnd(String recordID) {
            mVadEndTime = System.currentTimeMillis();
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_END, null);
        }

        @Override
        public void onRmsChanged(float rmsDb) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VOLUME_CHANGED, rmsDb);
        }

        @Override
        public void onBufferReceived(byte[] data) {
            byte[] buffer = new byte[data.length];
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

    /**
     * DDS cloud Asr kernel listener
     */
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
            if (mListener != null) {
                mListener.onConnect(isConnected);
            }
        }
    }

}
