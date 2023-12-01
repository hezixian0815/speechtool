package com.aispeech.lite.vprint;

import android.annotation.SuppressLint;
import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.LocalVprintConfig;
import com.aispeech.lite.param.VprintParams;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by wuwei on 2019/5/24.
 */

public class VprintProcessor extends BaseProcessor {
    private static final String KEY_OPERATION = "operation";
    @SuppressLint("StaticFieldLeak")
    private static VprintProcessor mInstance = null;
    private String tag = "";
    private VprintKernel mVprintKernel;
    private VprintParams mVprintParams;
    private LocalVprintConfig mVprintConfig;

    private VprintProcessor() {
    }


    public static synchronized VprintProcessor getInstance() {
        if (mInstance == null) {
            mInstance = new VprintProcessor();
        }
        return mInstance;
    }

    public void init(SpeechListener listener,
                     LocalVprintConfig localVprintConfig) {
        tag = "VprintProcessor";
        threadCount = 1;
        mScope = Scope.LOCAL_VPRINT;
        init(listener, localVprintConfig.getContext(), tag);
        this.mVprintConfig = localVprintConfig;
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }


    public void start(VprintParams vprintParams) {
        if (mProfileState != null && mProfileState.isValid()) {
            this.mVprintParams = vprintParams;
            Log.d(tag, "vprint param: " + mVprintParams.toJSON().toString());
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    private void initKernel(LocalVprintConfig vprintConfig) {
        int status = copyAssetsRes(vprintConfig);
        if (status == AIConstant.OPT_SUCCESS) {
            mVprintKernel = new VprintKernel(new MyVprintKernelListener());
            mVprintKernel.newKernel(vprintConfig);
        } else {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
        }
    }


    public void notifyEvent(String event) {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_VPRINT_NOTIFY, event);
    }

    public void feedTLV(byte[] data, int size) {
        byte[] buffer = new byte[size];
        System.arraycopy(data, 0, buffer, 0, size);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_VPRINT_TLV, buffer);
    }

    /**
     * 处理内部消息
     *
     * @param engineMsg engineMsg
     * @param msg       msg
     */
    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                handleMsgNew();
                break;
            case MSG_SET:
                handleMsgSet(msg);
                break;
            case MSG_START:
                handleMsgStart();
                break;
            case MSG_STOP:
                handleMsgStop();
                break;
            case MSG_CANCEL:
                handleMsgCancel();
                break;
            case MSG_RESULT_RECEIVE_DATA:
                handleMsgResultData(msg);
                break;
            case MSG_VPRINT_RESULT:
                handleMsgVprintResult(msg);
                break;
            case MSG_RELEASE:
                handleMsgRelease();
                break;
            case MSG_ERROR:
                handleMsgError(msg);
                break;
            case MSG_VPRINT_NOTIFY:
                handleMsgVprintNotify(msg);
                break;
            case MSG_VPRINT_TLV:
                handleMsgVprintTlv(msg);
                break;
            default:
                break;
        }
    }

    private void handleMsgVprintTlv(Message msg) {
        byte[] data = (byte[]) msg.obj;
        if (mState != EngineState.STATE_IDLE) {
            if (mVprintKernel != null) {
                mVprintKernel.feedTLV(data);
            }
        } else {
            trackInvalidState("vprint_tlv");
        }
    }

    private void handleMsgVprintNotify(Message msg) {
        String notifyEvent = (String) msg.obj;
        if (mState != EngineState.STATE_IDLE) {
            if (mVprintKernel != null) {
                mVprintKernel.notifyEvent(notifyEvent);
            }
        } else {
            trackInvalidState("notifyEvent");
        }
    }

    private void handleMsgError(Message msg) {
        AIError error = (AIError) msg.obj;
        Log.w(tag, error.toString());
        sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, msg.obj);
    }

    private void handleMsgRelease() {
        if (mState != EngineState.STATE_IDLE) {
            if (mVprintKernel != null) {
                mVprintKernel.releaseKernel();
            }
            clearObject();//清除实例
            transferState(EngineState.STATE_IDLE);
        } else {
            trackInvalidState("release");
        }
    }

    private void handleMsgVprintResult(Message msg) {
        AIResult vprintResult = (AIResult) msg.obj;
        vprintResult.setRecordId(mRecorderId);
        if (mState != EngineState.STATE_IDLE) {
            vprintResult.setRecordId(mRecorderId + "");
            if (mVprintParams != null && mVprintParams.getAction().equals(VprintIntent.Action.TEST.getValue()) && !updateTrails(mProfileState)) {
                return;
            }
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, vprintResult);
        } else {
            trackInvalidState("result");
        }
    }

    private void handleMsgResultData(Message msg) {
        //此MSG包含录音机/自定义feed音频来源
        final byte[] bufferData = (byte[]) msg.obj;
        if (mState != EngineState.STATE_IDLE) {
            //送vad 进行检测
            if (mVprintKernel != null) {
                mVprintKernel.feed(bufferData);
            }
            if (mOutListener != null) {
                mOutListener.onResultDataReceived(bufferData, bufferData.length, 0);
            }
        }
    }

    private void handleMsgCancel() {
        if (mState == EngineState.STATE_RUNNING) {
            if (mVprintKernel != null) {
                mVprintKernel.cancelKernel();
            }
            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("cancel");
        }
    }

    private void handleMsgStop() {
        if (mState == EngineState.STATE_RUNNING) {
            //非删除模式下才开启vad检测
            if (mVprintKernel != null) {
                mVprintKernel.stopKernel();
            }
            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("stop");
        }
    }

    private void handleMsgStart() {
        if (mState == EngineState.STATE_NEWED) {
            mRecorderId = Utils.getRecorderId();
            mVprintKernel.startKernel(mVprintParams);
            transferState(EngineState.STATE_RUNNING);
        } else {
            trackInvalidState("start");
        }
    }

    private void handleMsgSet(Message msg) {
        final String setStr = (String) msg.obj;
        if (mState != EngineState.STATE_IDLE) {
            if (mVprintKernel != null) {
                //查询模型中信息，开放接口
                mVprintKernel.set(setStr);
            }
        } else {
            trackInvalidState("query");
        }
    }

    private void handleMsgNew() {
        if (mState == EngineState.STATE_IDLE) {
            initKernel(mVprintConfig);
        } else {
            trackInvalidState("new");
        }
    }


    @Override
    public synchronized void release() {
        super.release();
        if (mInstance != null) {
            mInstance = null;
        }
    }

    @Override
    public void clearObject() {
        super.clearObject();
        if (mVprintParams != null) {
            mVprintParams = null;
        }
        if (mVprintConfig != null) {
            mVprintConfig = null;
        }
        if (mVprintKernel != null) {
            mVprintKernel = null;
        }
    }

    private void filterResults(AIResult aiResult) {
        if (aiResult.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
            handleResultTypeJson(aiResult);
        } else if (aiResult.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VPRINT_RESULT, aiResult);
        }
    }

    private void handleResultTypeJson(AIResult aiResult) {
        String vprintRetString = aiResult.getResultObject().toString();
        try {
            JSONObject vprintJsonStr = new JSONObject(vprintRetString);
            String option = vprintJsonStr.optString("option");
            int state = vprintJsonStr.optInt("state");
            Log.d(tag, "state " + state + " option " + option);
            if (TextUtils.equals(option, "RegisterStart") ||
                    TextUtils.equals(option, "TestStart") ||
                    TextUtils.equals(option, "UpdateStart") ||
                    TextUtils.equals(option, "AppendStart")) {
                return;
            }
            if (TextUtils.equals(option, "Register") ||
                    TextUtils.equals(option, "Update") ||
                    TextUtils.equals(option, "Append") ||
                    TextUtils.equals(option, "UnRegister") ||
                    TextUtils.equals(option, "UnRegisterALL")) {
                if (state == 8) {
                    vprintJsonStr.put(KEY_OPERATION, "continue");
                    aiResult.setResultObject(vprintJsonStr.toString());
                } else if (state == 0) {
                    vprintJsonStr.put(KEY_OPERATION, "success");
                    aiResult.setResultObject(vprintJsonStr.toString());
                } else {
                    vprintJsonStr.put(KEY_OPERATION, "failed");
                    aiResult.setResultObject(vprintJsonStr.toString());
                }
            } else if (TextUtils.equals(option, "NewInstance")) {
                aiResult.setResultObject(vprintRetString);
            }
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VPRINT_RESULT, aiResult);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void processNoSpeechError() {
        //nothing
    }

    @Override
    public void processMaxSpeechError() {
        //nothing
    }

    private class MyVprintKernelListener implements VprintKernelListener {

        @Override
        public void onInit(int status) {
            Log.d(tag, "vprint Init status: " + status);
            processInit(status);
        }

        @Override
        public void onError(AIError error) {
            Log.d(tag, "MyVprintKernelListener onError: " + error.toString());
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onResults(AIResult result) {
            filterResults(result);
        }
    }

}
