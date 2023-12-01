package com.aispeech.lite.sevc;

import android.os.Message;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.speech.SpeechListener;

public class SevcProcessor extends BaseProcessor {
    public static final String TAG = "SevcProcessor";
    private SevcKernel kernel;
    private AIEngineConfig mConfig;
    private SpeechParams mParams;

    public void init(SpeechListener listener, AIEngineConfig config) {
        mScope = Scope.LOCAL_SEVC;
        super.init(listener, config.getContext(), TAG);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, config);
    }

    public void start(SpeechParams params) {
        if (mProfileState != null && mProfileState.isValid()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, params);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                if (mState == EngineState.STATE_IDLE) {
                    mConfig = (AIEngineConfig) msg.obj;
                    int status = copyAssetsRes(mConfig);
                    if (status == AIConstant.OPT_FAILED) {
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                        break;
                    }
                    kernel = new SevcKernel(new SevcListenerImpl());
                    kernel.newKernel(mConfig);
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
                    mCallbackState = EngineState.STATE_RUNNING;

                    if (mParams.isUseCustomFeed()) {
                        Log.i(TAG, "isUseCustomFeed");
                        kernel.startKernel(new SpeechParams());
                        transferState(EngineState.STATE_RUNNING);
                    } else {
                        // 启动SDK内部录音机
                        startRecorder(mParams, SevcProcessor.this);
                    }
                } else {
                    trackInvalidState("start");
                }
                break;
            case MSG_SET:
                if (mState != EngineState.STATE_IDLE) {
                    kernel.set((String) msg.obj);
                } else {
                    trackInvalidState("set info");
                }
                break;
            case MSG_RECORDER_START:
                if (mState == EngineState.STATE_NEWED) {
                    kernel.startKernel(new SpeechParams());
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("recorder start");
                }
                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(SevcProcessor.this);
                    releaseRecorder();
                    kernel.stopKernel();
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("stop");
                }
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(SevcProcessor.this);
                    }
                    releaseRecorder();
                    kernel.releaseKernel();
                    clearObject();//清除实例
                    transferState(EngineState.STATE_IDLE);
                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_RAW_RECEIVE_DATA:
                //此MSG包含录音机/自定义feed音频来源
                final byte[] bufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    kernel.feed(bufferData);
                    if (mOutListener != null) {
                        mOutListener.onRawDataReceived(bufferData, bufferData.length);
                    }
                }
                break;
            case MSG_RESULT:
                if (mState != EngineState.STATE_IDLE) {
                    AIResult result = (AIResult) msg.obj;

                } else {
                    trackInvalidState("result");
                }
                break;
            case MSG_DOA:
                int doaValue = (Integer) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_DOA_RESULT, doaValue);
                } else {
                    trackInvalidState("doa result");
                }
                break;
            case MSG_ERROR:
                AIError error = (AIError) msg.obj;
                Log.w(TAG, error.toString());
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(this);
                    kernel.stopKernel();
                    transferState(EngineState.STATE_NEWED);
                }
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, msg.obj);
                break;
            default:
                break;
        }
    }

    @Override
    public void clearObject() {
        super.clearObject();
        removeCallbackMsg();
        kernel = null;
        mConfig = null;
        mParams = null;
    }

    @Override
    public void processNoSpeechError() {

    }

    @Override
    public void processMaxSpeechError() {

    }

    @Override
    public int get(String param) {
        if (mState != EngineState.STATE_IDLE) {
            return kernel.get(param);
        } else {
            trackInvalidState("get info");
        }
        return -1;
    }

    private class SevcListenerImpl implements SevcListener {

        @Override
        public void onInit(int status) {
            Log.i(TAG, "SevcListenerImpl onInit : " + status);
            processInit(status);
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onInputDataReceived(byte[] data, int size) {
            if (mOutListener != null) {
                mOutListener.onInputDataReceived(data, size);
            }
        }

        @Override
        public void onSevcBufferReceived(byte[] data) {
            if (mOutListener != null) {
                mOutListener.onAgcDataReceived(data);
            }
        }

        @Override
        public void onSevcNoiseResult(String retString) {
            if (mOutListener != null) {
                mOutListener.onSevcNoiseResult(retString);
            }
        }

        @Override
        public void onSevcDoaResult(int doa) {
            if (mOutListener != null) {
                mOutListener.onSevcDoaResult(doa);
            }
        }
    }
}
