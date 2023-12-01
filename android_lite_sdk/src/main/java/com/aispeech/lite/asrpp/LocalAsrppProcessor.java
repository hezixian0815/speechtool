package com.aispeech.lite.asrpp;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.DUILiteConfig;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.LocalAsrppConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.vad.VadKernel;
import com.aispeech.lite.vad.VadKernelListener;

public class LocalAsrppProcessor extends BaseProcessor {
    private static final String TAG = "LocalAsrppProcessor";
    private AsrppKernel mAsrppKernel;
    private LocalAsrppConfig mAsrppConfig;
    private SpeechParams mAsrppParams;
    private VadKernel mVadKernel;
    private VadParams mVadParams;
    private LocalVadConfig mVadConfig;

    public void init(SpeechListener listener, LocalAsrppConfig asrppConfig, LocalVadConfig vadConfig) {
        this.mVadConfig = vadConfig;
        this.mAsrppConfig = asrppConfig;
        if (vadConfig.isVadEnable()) {
            threadCount++;
        }
        mScope = Scope.LOCAL_ASRPP;
        init(listener, asrppConfig.getContext(), TAG);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }

    public void start(SpeechParams asrParams, VadParams vadParams) {
        if (mProfileState != null && mProfileState.isValid()) {
            this.mAsrppParams = asrParams;
            this.mVadParams = vadParams;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage(mProfileState);
        }
    }


    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                handleMsgNew();
                break;
            case MSG_START:
                handleMsgStart();
                break;
            case MSG_RECORDER_START:
                handleMsgRecorderStart();
                break;
            case MSG_STOP:
                handleMsgStop();
                break;
            case MSG_CANCEL:
                handleMsgCancel();
                break;
            case MSG_RAW_RECEIVE_DATA:
                final byte[] rawBufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING && mOutListener != null) {
                    mOutListener.onRawDataReceived(rawBufferData, rawBufferData.length);
                }
                break;
            case MSG_RESULT_RECEIVE_DATA:
                //此MSG包含录音机/自定义feed音频来源
                final byte[] bufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mVadConfig.isVadEnable()) {//送vad模块，vad处理后再送asr
                        mVadKernel.feed(bufferData);
                    } else {
                        mAsrppKernel.feed(bufferData);
                    }
                    if (mOutListener != null) {
                        mOutListener.onResultDataReceived(bufferData, bufferData.length, 0);
                    }
                }
                break;
            case MSG_VAD_RECEIVE_DATA:
                final byte[] vadData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    mAsrppKernel.feed(vadData);
                }
                break;
            case MSG_VAD_START:
                handleMsgVadStart();
                break;
            case MSG_VAD_END:
                handleMsgVadEnd();
                break;
            case MSG_VOLUME_CHANGED:
                handleMsgVolumeChanged(msg);
                break;
            case MSG_RESULT:
                handleMsgResult(msg);
                break;
            case MSG_RELEASE:
                handleMsgRelease();
                break;
            case MSG_ERROR:
                handleMsgError(msg);

                break;
            default:
                break;
        }
    }

    private void handleMsgError(Message msg) {
        AIError error = (AIError) msg.obj;
        if (TextUtils.isEmpty(error.getRecordId())) {
            error.setRecordId(Utils.getRecorderId());
        }
        if (error.getErrId() == AIError.ERR_RES_PREPARE_FAILED) {
            Log.w(TAG, error.toString());
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
            return;
        }
        if (mState == EngineState.STATE_IDLE) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
            return;
        }
        if (mState != EngineState.STATE_NEWED && mState != EngineState.STATE_IDLE) {
            unRegisterRecorderIfIsRecording(this);
            mAsrppKernel.cancelKernel();
            if (mVadConfig.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_NEWED);
            Log.w(TAG, error.toString());
            if (error.getErrId() == AIError.ERR_DNS) {
                error.setErrId(AIError.ERR_NETWORK);
                error.setError(AIError.ERR_DESCRIPTION_ERR_NETWORK);
            }
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, msg.obj);
        } else {
            trackInvalidState("error");
        }
    }

    private void handleMsgRelease() {
        if (mState != EngineState.STATE_IDLE) {
            if (mState == EngineState.STATE_RUNNING) {
                unRegisterRecorderIfIsRecording(this);
            }
            cancelNoSpeechTimer();
            mAsrppKernel.releaseKernel();
            if (mVadKernel != null) {
                mVadKernel.releaseKernel();
            }
            clearObject();//清除实例
            transferState(EngineState.STATE_IDLE);

        } else {
            trackInvalidState("release");
        }
    }

    private void handleMsgResult(Message msg) {
        AIResult result = (AIResult) msg.obj;
        if (mState != EngineState.STATE_IDLE) {
            result.setRecordId(mRecorderId);
            if (updateTrails(mProfileState)) {
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
            }
        } else {
            trackInvalidState("result");
        }
    }

    private void handleMsgVolumeChanged(Message msg) {
        float rmsDb = (float) msg.obj;
        if (mState == EngineState.STATE_RUNNING) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RMS_CHANGED, rmsDb);
        } else {
            trackInvalidState("volume changed");
        }
    }

    private void handleMsgVadEnd() {
        if (mState == EngineState.STATE_RUNNING) {
            Log.d(TAG, "VAD.END");
            if (mAsrppParams.isUseOneShot()) {
                handleOneshot();
            } else {
                unRegisterRecorderIfIsRecording(this);
                mAsrppKernel.stopKernel();
                if (mVadConfig.isVadEnable()) {
                    mVadKernel.stopKernel();
                }
                transferState(EngineState.STATE_NEWED);
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_END_OF_SPEECH, null);
            }
        } else {
            trackInvalidState("VAD.END");
        }
    }

    private void handleOneshot() {
        Log.d(TAG, "use one shot");
        //使用oneshot
        long currentTime = System.currentTimeMillis();
        long intervalTime = currentTime - mAsrppParams.getWakeupTime();
        Log.d(TAG, "interval time is : " + intervalTime);
        Log.d(TAG, "interval thresh time is : " + mAsrppParams.getOneShotIntervalTime());
        if (intervalTime < mAsrppParams.getOneShotIntervalTime()) {
            //小于阈值，认为不是oneshot, 则取消本次识别
            unRegisterRecorderIfIsRecording(this);
            mAsrppKernel.cancelKernel();
            if (mVadConfig.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_NEWED);
            Log.d(TAG, "not one shot");
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_NOT_ONE_SHOT, null);
        } else {
            //大于阈值，直接用本次识别结果，结果中带唤醒词
            unRegisterRecorderIfIsRecording(this);
            mAsrppKernel.stopKernel();
            if (mVadConfig.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_NEWED);
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_END_OF_SPEECH, null);
        }
    }

    private void handleMsgVadStart() {
        if (mState == EngineState.STATE_RUNNING) {
            Log.d(TAG, "VAD.BEGIN");
            cancelNoSpeechTimer();
            startMaxSpeechTimerTask(mAsrppParams);
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_BEGINNING_OF_SPEECH, null);
        } else {
            trackInvalidState("VAD.BEGIN");
        }
    }

    private void handleMsgCancel() {
        if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING
                || mState == EngineState.STATE_NEWED) {
            unRegisterRecorderIfIsRecording(this);
            mAsrppKernel.cancelKernel();
            if (mVadConfig != null && mVadConfig.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("cancel");
        }
    }

    private void handleMsgStop() {
        if (mState == EngineState.STATE_RUNNING) {
            unRegisterRecorderIfIsRecording(this);
            mAsrppKernel.stopKernel();
            if (mVadConfig.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("stop");
        }
    }

    private void handleMsgRecorderStart() {
        if (mState == EngineState.STATE_NEWED) {
            mAsrppKernel.startKernel(mAsrppParams);
            if (mVadConfig.isVadEnable()) {
                startNoSpeechTimer(mAsrppParams);
                mVadKernel.startKernel(mVadParams);
            }
            transferState(EngineState.STATE_RUNNING);
        } else {
            trackInvalidState("recorder start");
        }
    }

    private void handleMsgStart() {
        if (mState == EngineState.STATE_NEWED) {
            mCallbackState = EngineState.STATE_RUNNING;
            if (!mAsrppParams.isUseCustomFeed()) {
                if (initAiRecorder()) return;
            } else {
                unRegisterRecorderIfIsRecording(this);
            }
            mRecorderId = Utils.getRecorderId();
            if (mAsrppParams.isUseCustomFeed()) {
                Log.i(TAG, "isUseCustomFeed");
                mAsrppKernel.startKernel(mAsrppParams);
                if (mVadConfig.isVadEnable()) {
                    startNoSpeechTimer(mAsrppParams);
                    mVadKernel.startKernel(mVadParams);
                }
                transferState(EngineState.STATE_RUNNING);
            } else {
                // 启动SDK内部录音机
                startRecorder(mAsrppParams, this);
            }
        } else {
            trackInvalidState("start");
        }
    }

    private boolean initAiRecorder() {
        if (mAIRecorder == null) {
            if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_MIC ||//音频来源于AudioRecorder节点
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
                mAIRecorder = createRecorder(this);
                if (mAIRecorder == null) {
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                            AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE));
                    return true;
                }
            } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_DUAL ||//音频来源于信号处理引擎节点
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_FESPCAR ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_FESPCAR4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_DUAL ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_LINE4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_LINE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE4) {
                if (mAsrppParams.getFespxEngine() == null) {
                    throw new RuntimeException("need to setFespxEngine before start engine");
                }
                mAIRecorder = createSignalProcessingRecorder(mAsrppParams.getFespxEngine().getFespxProcessor());
                if (mAIRecorder == null) {
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                            AIError.ERR_SIGNAL_PROCESSING_NOT_STARTED,
                            AIError.ERR_DESCRIPTION_SIGNAL_PROCESSING_NOT_STARTED));
                    return true;
                }
            }
        }
        return false;
    }

    private void handleMsgNew() {
        if (mState == EngineState.STATE_IDLE) {
            int status = copyAssetsRes(mAsrppConfig);
            if (status == AIConstant.OPT_FAILED) {
                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                return;
            }
            if (mVadConfig.isVadEnable()) {
                status = copyAssetsRes(mVadConfig);
                if (status == AIConstant.OPT_FAILED) {
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                            AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                    return;
                }
                mVadKernel = new VadKernel("LocalAsrpp",new MyVadKernelListener());
                mVadKernel.newKernel(mVadConfig);
            }
            mAsrppKernel = new AsrppKernel(new MyAsrppKernelListener());
            mAsrppKernel.newKernel(mAsrppConfig);
        } else {
            trackInvalidState("new");
        }
    }


    @Override
    public void clearObject() {
        super.clearObject();
        if (mAsrppKernel != null)
            mAsrppKernel = null;
        if (mAsrppParams != null)
            mAsrppParams = null;
        if (mAsrppConfig != null)
            mAsrppConfig = null;
        if (mVadKernel != null)
            mVadKernel = null;
        if (mVadParams != null)
            mVadParams = null;
        if (mVadConfig != null)
            mVadConfig = null;
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


    /**
     * Asrpp模块
     */
    private class MyAsrppKernelListener implements AsrppKernelListener {

        @Override
        public void onInit(int status) {
            Log.i(TAG, "MyAsrppKernelListener onInit : " + status);
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
    }

    /**
     * vad模块回调
     */
    private class MyVadKernelListener implements VadKernelListener {

        @Override
        public void onInit(int status) {
            Log.i(TAG, "MyVadKernelListener onInit : " + status);
            processInit(status);
        }

        @Override
        public void onVadStart(String recordID) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_START, null);
        }

        @Override
        public void onVadEnd(String recordID) {
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
}
