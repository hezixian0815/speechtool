package com.aispeech.lite.dds;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.DUILiteConfig;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.asr.CloudAsrKernelListener;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.CloudSemanticConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.vad.VadKernel;
import com.aispeech.lite.vad.VadKernelListener;

/**
 * DDS云端识别 processor
 *
 * @author hehr
 */
public class CloudSemanticProcessor extends BaseProcessor {

    private BaseKernel mDDSCloudAsrKernel;

    private SpeechParams mAsrParams;

    private CloudSemanticConfig mAsrConfig;

    private BaseKernel mVadKernel;

    private VadParams mVadParams;

    private LocalVadConfig mVadConfig;

    private static final String TAG = "CloudSemanticProcessor";

    protected SpeechListener mOutListener;


    public void init(SpeechListener listener, AIEngineConfig asrConfig, LocalVadConfig vadConfig) {

        this.mOutListener = listener;

        if (vadConfig.isVadEnable()) {
            threadCount++;
        }

        mAsrConfig = (CloudSemanticConfig) asrConfig;

        mVadConfig = vadConfig;
        mScope = Scope.CLOUD_MODEL;
        init(listener, asrConfig.getContext(), TAG);

        if (mDDSCloudAsrKernel == null) {
            mDDSCloudAsrKernel = new CloudSemanticKernel(new DDSCloudAsrListenerImpl());
        }

        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);

    }


    public void start(SpeechParams asrParams, VadParams vadParams) {
        if (mProfileState != null && mProfileState.isValid()) {
            this.mAsrParams = asrParams;
            this.mVadParams = vadParams;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage(mProfileState);
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
                    if (mVadConfig.isVadEnable()) {
                        int status = copyAssetsRes(mVadConfig);
                        if (status == AIConstant.OPT_FAILED) {
                            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                            break;
                        }
                        mVadKernel = new VadKernel("csem",new MyVadKernelListener());
                        mVadKernel.newKernel(mVadConfig);
                    }
                    mDDSCloudAsrKernel.newKernel(mAsrConfig);

                } else {
                    trackInvalidState("new");
                }

                break;
            case MSG_START:
                if (mState == EngineState.STATE_NEWED) {
                    mCallbackState = EngineState.STATE_RUNNING;
                    if (!mAsrParams.isUseCustomFeed()) {
                        if (mAIRecorder == null) {
                            if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_MIC ||//音频来源于AudioRecorder节点
                                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
                                mAIRecorder = createRecorder(this);
                                if (mAIRecorder == null) {
                                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                                            AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE));
                                    return;
                                }
                            } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_DUAL ||//音频来源于信号处理引擎节点
                                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_FESPCAR ||
                                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_FESPCAR4 ||
                                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE4 ||
                                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE4 ||
                                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE6 ||
                                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_DUAL ||
                                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_LINE4 ||
                                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_LINE6 ||
                                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE6 ||
                                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE4) {
                                if (mAsrParams.getFespxEngine() == null) {
                                    throw new RuntimeException("need to setFespxEngine before start engine");
                                }
                                mAIRecorder = createSignalProcessingRecorder(mAsrParams.getFespxEngine().getFespxProcessor());
                                if (mAIRecorder == null) {
                                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                                            AIError.ERR_SIGNAL_PROCESSING_NOT_STARTED,
                                            AIError.ERR_DESCRIPTION_SIGNAL_PROCESSING_NOT_STARTED));
                                    return;
                                }
                            }
                        }
                    } else {
                        unRegisterRecorderIfIsRecording(this);
                        releaseRecorder();
                    }
                    if (mState == EngineState.STATE_NEWED) {
                        if (mAsrConfig.isUseCustomFeed()) {
                            Log.i(TAG, "isUseCustomFeed");
                            mDDSCloudAsrKernel.startKernel(mAsrParams);
                            if (mVadConfig.isVadEnable()) {
                                mVadKernel.startKernel(mVadParams);
                                startNoSpeechTimer(mAsrParams);
                            }
                            transferState(EngineState.STATE_RUNNING);
                        } else {
                            // 启动SDK内部录音机
                            startRecorder(mAsrParams, CloudSemanticProcessor.this);
                        }
                    }
                } else {
                    trackInvalidState("start");
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
                    releaseRecorder();
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
                    releaseRecorder();
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
                    error.setRecordId(Utils.getRecorderId());
                }
                if (error.getErrId() == AIError.ERR_RES_PREPARE_FAILED) {
                    Log.w(TAG, error.toString());
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
//                    uploadError(error);
//                    Upload.startUploadTimer();
                    return;
                }
                if (mState == EngineState.STATE_IDLE) {
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
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
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, msg.obj);
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
                        mDDSCloudAsrKernel.feed(bufferData);
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
                    mDDSCloudAsrKernel.feed(vadData);
                }
                break;
            case MSG_VAD_START:
                if (mState == EngineState.STATE_RUNNING) {
                    Log.d(TAG, "VAD.BEGIN");
                    cancelNoSpeechTimer();
                    startMaxSpeechTimerTask(mAsrParams);
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_BEGINNING_OF_SPEECH, null);
                } else {
                    trackInvalidState("VAD.BEGIN");
                }
                break;

            case MSG_VAD_END:
                if (mState == EngineState.STATE_RUNNING) {
                    Log.d(TAG, "VAD.END");
                    if (mAsrParams.isUseOneShot()) {
                        Log.d(TAG, "use one shot");
                        //使用oneshot
                        long currentTime = System.currentTimeMillis();
                        long intervalTime = currentTime - mAsrParams.getWakeupTime();
                        Log.d(TAG, "interval time is : " + intervalTime);
                        Log.d(TAG, "interval thresh time is : " + mAsrParams.getOneShotIntervalTime());
                        if (intervalTime < mAsrParams.getOneShotIntervalTime()) {
                            //小于阈值，认为不是oneshot, 则取消本次识别
                            unRegisterRecorderIfIsRecording(CloudSemanticProcessor.this);
                            mDDSCloudAsrKernel.cancelKernel();
                            if (mVadConfig.isVadEnable()) {
                                mVadKernel.stopKernel();
                            }
                            transferState(EngineState.STATE_NEWED);
                            Log.d(TAG, "not one shot");
                            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_NOT_ONE_SHOT, null);
                        } else {
                            //大于阈值，直接用本次识别结果，结果中带唤醒词
                            unRegisterRecorderIfIsRecording(CloudSemanticProcessor.this);
                            mDDSCloudAsrKernel.stopKernel();
                            if (mVadConfig.isVadEnable()) {
                                mVadKernel.stopKernel();
                            }
                            transferState(EngineState.STATE_WAITING);
                            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_END_OF_SPEECH, null);
                        }
                    } else {
                        unRegisterRecorderIfIsRecording(CloudSemanticProcessor.this);
                        mDDSCloudAsrKernel.stopKernel();
                        if (mVadConfig.isVadEnable()) {
                            mVadKernel.stopKernel();
                        }
                        transferState(EngineState.STATE_WAITING);
                        sendMsgToCallbackMsgQueue(CallbackMsg.MSG_END_OF_SPEECH, null);
                    }
                } else {
                    trackInvalidState("VAD.END");
                }
                break;
            case MSG_VOLUME_CHANGED:
                float rmsDb = (float) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RMS_CHANGED, rmsDb);
                } else {
                    trackInvalidState("volume changed");
                }
                break;
            case MSG_RESULT:
                AIResult result = (AIResult) msg.obj;
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
                    if (result.isLast()) {
                        transferState(EngineState.STATE_NEWED);
                        unRegisterRecorderIfIsRecording(CloudSemanticProcessor.this);
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
            default:
                break;
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
        public void onVadStart(String recordId) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_START, null);
        }

        @Override
        public void onVadEnd(String recordId) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_END, null);
        }

        @Override
        public void onRmsChanged(float rmsDb) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VOLUME_CHANGED, rmsDb);
        }

        @Override
        public void onBufferReceived(byte[] data) {
            byte[] buffer = new byte[data.length];
            System.arraycopy(data, 0, buffer, 0, data.length);
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_RECEIVE_DATA, buffer);
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
//            TODO:解决保持 暂未迁移
//            if (mListener != null) {
//                mListener.onConnect(isConnected);
//            }
        }
    }

}
