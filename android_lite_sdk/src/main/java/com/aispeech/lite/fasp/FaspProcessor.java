package com.aispeech.lite.fasp;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.config.FaspConfig;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.speech.SpeechListener;

public class FaspProcessor extends BaseProcessor {

    private FaspConfig config;
    private SpeechParams mParams;
    private FaspKernel faspKernel;

    public void init(SpeechListener listener, FaspConfig config) {
        threadCount = 1;
        init(listener, config.getContext(), getTag());
        this.mBaseConfig = config;
        this.config = config;
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }

    public void start(SpeechParams param) {
        if (mProfileState != null && mProfileState.isValid()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, param);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    public void feedData(byte[] data, int size) {
        if (mProfileState != null && mProfileState.isValid()) {
            byte[] bufferData = new byte[size];
            System.arraycopy(data, 0, bufferData, 0, size);
            sendMsgToInnerMsgQueue(EngineMsg.MSG_RAW_RECEIVE_DATA, bufferData);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    public void getInputWavChan() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_FASP_INPUT_WAV_CHAN, null);
    }

    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                if (mState == EngineState.STATE_IDLE) {
                    int status = copyAssetsRes(config);
                    if (status == AIConstant.OPT_SUCCESS) {
                        faspKernel = new FaspKernel(new MyFaspListener());
                        faspKernel.newKernel(config);
                    } else {
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                    }
                } else {
                    trackInvalidState("new");
                }
                break;
            case MSG_START:
                if (mState == EngineState.STATE_NEWED) {
                    mParams = (SpeechParams) msg.obj;
                    if (!mParams.isUseCustomFeed()) {
                        if (mAIRecorder == null) {
                            mAIRecorder = createRecorder(this);
                            if (mAIRecorder == null) {
                                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                                        AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE));
                                return;
                            }
                        }
                    } else {
                        unRegisterRecorderIfIsRecording(this);
                        releaseRecorder();
                    }

                    if (mParams.isUseCustomFeed()) {
                        Log.i(getTag(), "isUseCustomFeed");
                        faspKernel.startKernel(mParams);
                        transferState(EngineState.STATE_RUNNING);
                    } else {
                        // 启动SDK内部录音机
                        startRecorder(mParams, this);
                    }
                } else {
                    trackInvalidState("start");
                }
                break;
            case MSG_SET:
                final String setStr = (String) msg.obj;
                if (mState != EngineState.STATE_IDLE) {
                    faspKernel.set(setStr);
                } else {
                    trackInvalidState("set info");
                }
                break;
            case MSG_RECORDER_START:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
                    faspKernel.startKernel(mParams);
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("recorder start");
                }
                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(this);
                    releaseRecorder();
                    faspKernel.stopKernel();
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("stop");
                }
                break;
            case MSG_RAW_RECEIVE_DATA:
                final byte[] rawBufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mOutListener != null) {
                        mOutListener.onRawDataReceived(rawBufferData, rawBufferData.length);
                    }
                    faspKernel.feed(rawBufferData);
                }
                break;
            case MSG_RESULT_RECEIVE_DATA:
                break;
            case MSG_RESULT:
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(this);
                    }
                    releaseRecorder();
                    faspKernel.releaseKernel();
                    clearObject();//清除实例
                    transferState(EngineState.STATE_IDLE);
                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_ERROR:
                AIError error = (AIError) msg.obj;
                if (TextUtils.isEmpty(error.getRecordId())) {
                    error.setRecordId(Utils.getRecorderId());
                }
                Log.w(getTag(), error.toString());
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(this);
                    faspKernel.stopKernel();
                    transferState(EngineState.STATE_NEWED);
                }
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, msg.obj);
                break;
            case MSG_FASP_INPUT_WAV_CHAN:
                if (mState != EngineState.STATE_NEWED) {
                    faspKernel.getInputWavChan();
                } else {
                    trackInvalidState("get input_wav_chan");
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void processNoSpeechError() {

    }

    @Override
    public void processMaxSpeechError() {

    }

    private class MyFaspListener implements FaspListener {

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onChs1DataReceived(int type, byte[] data) {
            // 不增加接口，先用这个回调
            if (mOutListener != null)
                mOutListener.onVprintCutDataReceived(1, data, type);
        }

        @Override
        public void onChs2DataReceived(int type, byte[] data) {
            // 不增加接口，先用这个回调
            if (mOutListener != null)
                mOutListener.onVprintCutDataReceived(2, data, type);
        }

        @Override
        public void onGotInputWavChan(int inputWavChan) {
            // 不增加接口，先用这个回调
            if (mOutListener != null)
                mOutListener.onVprintCutDataReceived(3, null, inputWavChan);
        }

    }
}
