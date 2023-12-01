package com.aispeech.lite.mds;

import android.os.Message;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.LocalMdsConfig;
import com.aispeech.lite.param.MdsParams;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.speech.SpeechListener;

public class MdsProcessor extends BaseProcessor {
    private static final String TAG = "MdsProcessor";
    private MdsKernel kernel;
    private LocalMdsConfig mConfig;
    private MdsParams mParams;

    public void init(SpeechListener listener, LocalMdsConfig config) {
        mScope = Scope.LOCAL_MDS;
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
                    mConfig = (LocalMdsConfig) msg.obj;
                    int status = copyAssetsRes(mConfig);
                    if (status == AIConstant.OPT_FAILED) {
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                        break;
                    }
                    kernel = new MdsKernel(new MdsKernelListenerImpl());
                    kernel.newKernel(mConfig);
                } else {
                    trackInvalidState("new");
                }
                break;
            case MSG_START:
                if (mState == EngineState.STATE_NEWED) {
                    mParams = (MdsParams) msg.obj;
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
                    }
                    mCallbackState = EngineState.STATE_RUNNING;

                    if (mParams.isUseCustomFeed()) {
                        Log.i(TAG, "isUseCustomFeed");
                        kernel.startKernel(mParams);
                        transferState(EngineState.STATE_RUNNING);
                    } else {
                        // 启动SDK内部录音机
                        startRecorder(mParams, this);
                    }
                } else {
                    trackInvalidState("start");
                }
                break;
            case MSG_RECORDER_START:
                if (mState == EngineState.STATE_NEWED) {
                    kernel.startKernel(mParams);
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("recorder start");
                }
                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(this);
                    kernel.stopKernel();
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("stop");
                }
                break;
            case MSG_CANCEL:
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(this);
                    kernel.cancelKernel();
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("cancel");
                }
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(this);
                    }
                    kernel.releaseKernel();
                    clearObject();//清除实例
                    transferState(EngineState.STATE_IDLE);
                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_RAW_RECEIVE_DATA:
                //此MSG包含录音机多路音频/自定义feed音频来源
                if (mState == EngineState.STATE_RUNNING) {
                    final byte[] bufferData = (byte[]) msg.obj;
                    kernel.feed(bufferData);
                }
                break;
            case MSG_RESULT_RECEIVE_DATA:
                //此MSG包含录音机单路音频
                if (mState == EngineState.STATE_RUNNING && mConfig.getChannels() == 1) {
                    final byte[] bufferData = (byte[]) msg.obj;
                    kernel.feed(bufferData);
                }
                break;
            case MSG_RESULT:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(this);
                        kernel.stopKernel();
                        transferState(EngineState.STATE_NEWED);
                    }
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, msg.obj);
                } else {
                    trackInvalidState("result");
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
            case MSG_SET:
                if (mState == EngineState.STATE_NEWED) {
                    String paramsSet = (String) msg.obj;
                    kernel.set(paramsSet);
                } else {
                    trackInvalidState("set");
                }
                break;
            default:
                break;
        }
    }

    /**
     * 用户通过此方法传入mds的算法值，决策出哪个是最优设备
     *
     * @param data float数组：每个设备的snr算法值有三个，实例：如三台设备a、b、c各有三个值 数组格式为 [a1,b1,c1,a2,b2,c2,a3,b3,c3]
     * @param num
     * @param size
     * @return 唤醒设备的索引
     */
    public int mcdmFeed(float[] data, int num, int size) {
        if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_RUNNING) {
            return kernel.mcdmFeed(data, num, size);
        }else {
            trackInvalidState("mcdmFeed");
            return -1;
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
        //do nothing
    }

    @Override
    public void processMaxSpeechError() {
        //do nothing
    }

    private class MdsKernelListenerImpl implements MdsKernelListener {

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
    }
}
