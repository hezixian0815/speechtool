package com.aispeech.lite.vad;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.VadParams;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/3/27
 */
public class LocalVadProcessor extends BaseProcessor {

    public String TAG = "LocalVadProcessor";
    private VadKernel mVadKernel;
    private LocalVadConfig mVadConfig;
    private IVadProcessorListener mListener;


    public void init(LocalVadConfig config, IVadProcessorListener listener) {
        this.mVadConfig = config;
        mListener = listener;
        mScope = Scope.LOCAL_VAD;
        init(listener, config.getContext(), TAG);

        // 认证非法的时候已经在init回调回去了
        if (mProfileState == null || !mProfileState.isValid()) return;

        mVadKernel = new VadKernel("VadEngine", new VadKernelListenerImpl());
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }


    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {

        switch (engineMsg) {
            case MSG_NEW:
                if (mState == EngineState.STATE_IDLE) {
                    int status = copyAssetsRes(mVadConfig);
                    if (status == AIConstant.OPT_FAILED) {
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                        break;
                    }


                    mVadKernel.newKernel(mVadConfig);
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("new");
                }
                break;
            case MSG_START:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
                    VadParams params = (VadParams) msg.obj;
                    syncRecorderId(null, params);
                    if (mVadKernel != null) mVadKernel.startKernel(params);
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("start");
                }
                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    mVadKernel.stopKernel();
                    clearRecorderId();
                    transferState(EngineState.STATE_WAITING);
                } else {
                    trackInvalidState("stop");
                }
                break;
            case MSG_ERROR:
                AIError error = (AIError) msg.obj;
                Log.w(TAG, error.toString());
                if (TextUtils.isEmpty(error.getRecordId()) && !TextUtils.isEmpty(mRecorderId)) {
                    error.setRecordId(mRecorderId);
                } else if (TextUtils.isEmpty(error.getRecordId())) {
                    error.setRecordId(Utils.get_recordid());
                }
                if (error.getErrId() == AIError.ERR_RES_PREPARE_FAILED) {
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
                if (mState != EngineState.STATE_NEWED) {
                    unRegisterRecorderIfIsRecording(this);
                    transferState(EngineState.STATE_NEWED);

                    if (mVadKernel != null) {
                        mVadKernel.stopKernel();
                    }

                    if (mOutListener != null) {
                        mOutListener.onError(error);
                    }
                } else {
                    trackInvalidState("error");
                }
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    realReleaseVad();
                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_RESULT_RECEIVE_DATA:
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    if (mVadKernel != null) {
                        if (msg.obj instanceof byte[]) {
                            mVadKernel.feed((byte[]) msg.obj);
                        } else if (msg.obj instanceof byte[][]) {
                            mVadKernel.feed((byte[][]) msg.obj);
                        }
                    }
                }
                break;
            case MSG_UPDATE_VAD_PAUSETIME:
                int pauseTime = (int) msg.obj;
                if (mVadKernel != null) {
                    mVadKernel.executeVadPauseTime(pauseTime);
                }
                break;
            default:
                Log.w(TAG, "unhandle msg:" + engineMsg.name());
                break;

        }


    }

    private void realReleaseVad() {
        if (mState == EngineState.STATE_RUNNING) {
            unRegisterRecorderIfIsRecording(this);
            if (mVadKernel != null) mVadKernel.stopKernel();
        }

        if (mVadKernel != null) {
            mVadKernel.releaseKernel();
            mVadKernel = null;
        }
        clearObject();//清除实例
        transferState(EngineState.STATE_IDLE);
    }

    public void start(VadParams vadParams) {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, vadParams);
        } else {
            showErrorMessage();
        }
    }

    public void feedData(byte[] dataVad, byte[] dataAsr) {
        if (isAuthorized()) {

            byte[] vadBuffer = new byte[dataVad.length];
            System.arraycopy(dataVad, 0, vadBuffer, 0, dataVad.length);

            byte[] asrBuffer = new byte[dataAsr.length];
            System.arraycopy(dataAsr, 0, asrBuffer, 0, dataAsr.length);

            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT_RECEIVE_DATA, new byte[][]{vadBuffer, asrBuffer});
        } else {
            showErrorMessage();
        }
    }

    public void executeVadPauseTime(int pauseTime) {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE_VAD_PAUSETIME, pauseTime);
        } else {
            showErrorMessage();
        }
    }

    public int releaseSynchronize() {
        Log.i(TAG, "releaseSynchronize");
        if (isAuthorized()) {
            realReleaseVad();
            return AIConstant.OPT_SUCCESS;
        } else {
            showErrorMessage();
            return AIConstant.OPT_FAILED;
        }
    }

    @Override
    public void processNoSpeechError() {

    }

    @Override
    public void processMaxSpeechError() {

    }

    private class VadKernelListenerImpl implements VadKernelListener {

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onVadStart(String recordid) {
            if (mListener != null) {
                mListener.onVadStart(recordid);
            }
        }

        @Override
        public void onVadEnd(String recordid) {
            if (mListener != null) {
                mListener.onVadEnd(recordid);
            }
        }

        @Override
        public void onRmsChanged(float rmsDb) {
            if (mListener != null) {
                mListener.onRmsChanged(rmsDb);
            }
        }

        @Override
        public void onBufferReceived(byte[] data) {
            if (mListener != null) {
                mListener.onBufferReceived(data);
            }
        }

        @Override
        public void onResults(String result) {
            if (mListener != null) {
                mListener.onResults(result);
            }
        }

        @Override
        public void onError(AIError error) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
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
