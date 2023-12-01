package com.aispeech.lite.asr;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.DUILiteConfig;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.NetworkUtil;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.LocalLAsrConfig;
import com.aispeech.lite.param.LocalLAsrParams;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yuruilong on 2017/5/19.
 */

public class LocalLAsrProcessor extends BaseProcessor {

    private String tag = "LocalLAsrProcessor";
    private LocalLAsrKernel mLAsrKernel;
    private LocalLAsrParams mLAsrParams;
    private LocalLAsrConfig mLAsrConfig;

    /**
     * 初始化
     *
     * @param listener
     * @param asrConfig
     */
    public void init(SpeechListener listener, LocalLAsrConfig asrConfig) {
        mLAsrConfig = asrConfig;
        mScope = Scope.LOCAL_STREAM_ASR;
        init(listener, AISpeech.getContext(), tag);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }

    public void start(LocalLAsrParams asrParams) {
        if (mProfileState != null && mProfileState.isValid()) {
            this.mLAsrParams = asrParams;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    @Override
    public void clearObject() {
        super.clearObject();
        if (mLAsrKernel != null)
            mLAsrKernel = null;
        if (mLAsrParams != null)
            mLAsrParams = null;
        if (mLAsrConfig != null)
            mLAsrConfig = null;
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
                handleMsgResultData(msg);
                break;
            case MSG_VAD_RECEIVE_DATA:
                final byte[] vadData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    mLAsrKernel.feed(vadData);
                }
                break;
            case MSG_VOLUME_CHANGED:
                handleMsgVolumeChanged(msg);
                break;
            case MSG_UPDATE://离线语义引擎update消息仅用来更新net.bin文件
                if (mState == EngineState.STATE_NEWED) {
                    String config = (String) msg.obj;
                    if (mLAsrKernel != null) {
                        mLAsrKernel.update(config);
                    }
                } else {
                    trackInvalidState("update");
                }
                break;
            case MSG_RESULT:
                handleMsgResult(msg);
                break;
            case MSG_UPDATE_RESULT:
                if (mState != EngineState.STATE_IDLE) {
                    Integer ret = (Integer) msg.obj;
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, ret);
                } else {
                    trackInvalidState("update_result");
                }
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

    private void handleMsgResultData(Message msg) {
        final byte[] bufferData = (byte[]) msg.obj;
        if (mState == EngineState.STATE_RUNNING) {
            mLAsrKernel.feed(bufferData);
            if (mOutListener != null) {
                mOutListener.onResultDataReceived(bufferData, bufferData.length, 0);
            }
        }
    }

    private void handleMsgError(Message msg) {
        AIError error = (AIError) msg.obj;
        if (TextUtils.isEmpty(error.getRecordId())) {
            error.setRecordId(Utils.getRecorderId());
        }
        if (error.getErrId() == AIError.ERR_RES_PREPARE_FAILED) {
            Log.w(tag, error.toString());
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
            uploadError(error);
            return;
        }
        if (mState == EngineState.STATE_IDLE) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
            return;
        }
        if (mState != EngineState.STATE_NEWED && mState != EngineState.STATE_IDLE) {
            unRegisterRecorderIfIsRecording(LocalLAsrProcessor.this);
            mLAsrKernel.stopKernel();
            transferState(EngineState.STATE_NEWED);
            Log.w(tag, error.toString());
            uploadError(error);
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
                unRegisterRecorderIfIsRecording(LocalLAsrProcessor.this);
            }
            cancelNoSpeechTimer();
            mLAsrKernel.releaseKernel();
            mLAsrKernel = null;
            clearObject();//清除实例
            transferState(EngineState.STATE_IDLE);
        } else {
            trackInvalidState("release");
        }
    }

    private void handleMsgResult(Message msg) {
        AIResult result = (AIResult) msg.obj;
        if (mState != EngineState.STATE_IDLE) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
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

    private void handleMsgCancel() {
        if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING
                || mState == EngineState.STATE_NEWED) {
            unRegisterRecorderIfIsRecording(LocalLAsrProcessor.this);
            mLAsrKernel.cancelKernel();

            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("cancel");
        }
    }

    private void handleMsgStop() {
        if (mState == EngineState.STATE_RUNNING) {
            unRegisterRecorderIfIsRecording(LocalLAsrProcessor.this);
            mLAsrKernel.stopKernel();
            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("stop");
        }
    }

    private void handleMsgRecorderStart() {
        if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
            mLAsrKernel.startKernel(mLAsrParams);
            transferState(EngineState.STATE_RUNNING);
        } else {
            trackInvalidState("recorder start");
        }
    }

    private void handleMsgStart() {
        if (mState == EngineState.STATE_NEWED) {
            //试用检查判断
            if (!updateTrails(mProfileState)) {
                return;
            }

            if (!mLAsrParams.isUseCustomFeed()) {
                if (initRecorder()) return;
            } else {
                unRegisterRecorderIfIsRecording(this);
            }
            startKernelOrRecorder();
        } else {
            trackInvalidState("start");
        }
    }

    private void startKernelOrRecorder() {
        if (mLAsrParams.isUseCustomFeed()) {
            Log.i(tag, "isUseCustomFeed");
            mLAsrKernel.startKernel(mLAsrParams);
            transferState(EngineState.STATE_RUNNING);
        } else {
            // 启动SDK内部录音机
            startRecorder(mLAsrParams, LocalLAsrProcessor.this);
        }
    }

    private boolean initRecorder() {
        if (mAIRecorder == null) {
            if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_MIC ||//音频来源于AudioRecorder节点
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
                mAIRecorder = createRecorder(LocalLAsrProcessor.this);
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
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_LINE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE4) {
                if (mLAsrParams.getFespxEngine() == null) {
                    throw new RuntimeException("need to setFespxEngine before start engine");
                }
                mAIRecorder = createSignalProcessingRecorder(mLAsrParams.getFespxEngine().getFespxProcessor());
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
            int status = copyAssetsFolderMd5(mLAsrConfig);
            if (status == AIConstant.OPT_FAILED) {
                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                return;
            }
            mLAsrKernel = new LocalLAsrKernel(new MyLAsrKernelListener());
            mLAsrKernel.newKernel(mLAsrConfig);
        } else {
            trackInvalidState("new");
        }
    }

    private void uploadError(AIError aiError) {
        if ((aiError.getErrId() == AIError.ERR_NETWORK || aiError.getErrId() == AIError.ERR_DNS)
                && !NetworkUtil.isNetworkConnected(AISpeech.getContext())) {
            Log.d(tag, "network is not connected, ignore upload error");
            return;
        }
        String recordId = aiError.getRecordId();
        if (TextUtils.isEmpty(recordId)) {
            recordId = Utils.getRecorderId();
        }
        //添加message外面的字段
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("recordId", recordId);
        entryMap.put("mode", "lite");
        entryMap.put("module", "local_lasr_exception");
        JSONObject input = new JSONObject();
        try {
            if (mLAsrConfig != null) {
                input.put("config", ((LocalLAsrConfig) mLAsrConfig).toJson());
            }
            if (mLAsrParams != null) {
                input.put("param", ((LocalLAsrParams) mLAsrParams).toJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("local_lasr_exception", "info", "local_exception",
                recordId, input, aiError.getOutputJSON(), entryMap);
    }


    @Override
    public void processNoSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_NO_SPEECH,
                AIError.ERR_DESCRIPTION_NO_SPEECH));
        Log.w(tag, "no speech timeout!");
    }

    @Override
    public void processMaxSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                AIError.ERR_MAX_SPEECH, AIError.ERR_DESCRIPTION_MAX_SPEECH));
    }

    /**
     * asr回调
     */
    private class MyLAsrKernelListener implements AsrKernelListener {

        @Override
        public void onInit(int status) {
            Log.i(tag, "MyLAsrKernelListener onInit : " + status);
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
            //do nothing
        }

        @Override
        public void onUpdateResult(int ret) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE_RESULT, Integer.valueOf(ret));
        }
    }
}
