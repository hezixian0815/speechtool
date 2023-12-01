package com.aispeech.lite.vprintlite;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.LocalVprintLiteConfig;
import com.aispeech.lite.param.VprintLiteParams;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by wuwei on 2019/5/24.
 */

public class VprintLiteProcessor extends BaseProcessor {
    public String TAG = "";
    @SuppressLint("StaticFieldLeak")
    private static VprintLiteProcessor mInstance = null;
    private VprintLiteKernel mVprintKernel;
    private VprintLiteParams mVprintParams;
    private LocalVprintLiteConfig mVprintConfig;
    private String mRecorderId;

    public void init(SpeechListener listener,
                     LocalVprintLiteConfig localVprintConfig) {
        TAG = "VprintLiteProcessor";
        Log.d(TAG, "init: send MSG_NEW");
        threadCount = 1;
        mScope = Scope.LOCAL_VPRINT;
        init(listener, localVprintConfig.getContext(), TAG);
        this.mVprintConfig = localVprintConfig;
        mVprintKernel = new VprintLiteKernel(new MyVprintKernelListener());
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }


    public void start(VprintLiteParams vprintParams) {
        if (mProfileState != null && mProfileState.isValid()) {
            mVprintParams = vprintParams;
            Log.d(TAG, "vprint param: " + mVprintParams.toStartJSON(mVprintConfig.getVprintType()).toString());
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    private void initKernel(LocalVprintLiteConfig vprintConfig) {
        int status = AIConstant.OPT_FAILED;
        status = copyAssetsRes(vprintConfig);
        if (status == AIConstant.OPT_SUCCESS) {
            mVprintKernel.newKernel(vprintConfig);
        } else {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
        }
    }

    public void feedTLV(byte[] data, int size) {
        byte[] buffer = new byte[size];
        System.arraycopy(data, 0, buffer, 0, size);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_VPRINT_TLV, buffer);
    }

    public void feedKWS(byte[] data, int size, String params) {
        byte[] buffer = new byte[size];
        System.arraycopy(data, 0, buffer, 0, size);
        Bundle bundle = new Bundle();
        bundle.putByteArray("bytes", data);
        bundle.putString("params", params);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_VPRINT_KWS, bundle);
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
                if (mState == EngineState.STATE_IDLE) {
                    initKernel(mVprintConfig);
                } else {
                    trackInvalidState("new");
                }
                break;
            case MSG_SET:
                final String setStr = (String) msg.obj;
                if (mState != EngineState.STATE_IDLE) {
                    if (mVprintKernel != null) {
                        //查询模型中信息，开放接口
                        mVprintKernel.set(setStr);
                    }
                } else {
                    trackInvalidState("query");
                }
                break;
            case MSG_START:
                if (mState == EngineState.STATE_NEWED) {
                    mRecorderId = Utils.getRecorderId();
                    mVprintKernel.startKernel(mVprintParams);
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("start");
                }
                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    //非删除模式下才开启vad检测
                    if (mVprintKernel != null) {
                        mVprintKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("stop");
                }
                break;
            case MSG_CANCEL:
                if (mState == EngineState.STATE_RUNNING) {
                    if (mVprintKernel != null) {
                        mVprintKernel.cancelKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("cancel");
                }
                break;
            case MSG_RESULT_RECEIVE_DATA:
                //此MSG包含录音机/自定义feed音频来源
                final byte[] bufferData = (byte[]) msg.obj;
                if (mState != EngineState.STATE_IDLE) {
                    if (mVprintKernel != null) {
                        mVprintKernel.feed(bufferData);
                    }
                    if (mOutListener != null) {
                        mOutListener.onResultDataReceived(bufferData, bufferData.length, 0);
                    }
                }
                break;
            case MSG_VPRINT_RESULT:
                AIResult vprintResult = (AIResult) msg.obj;
                vprintResult.setRecordId(mRecorderId);
                if (mState != EngineState.STATE_IDLE) {
                    vprintResult.setRecordId(mRecorderId + "");
                    if (mVprintParams != null && mVprintParams.getAction().equals(VprintLiteIntent.Action.VERIFY.getValue()) && !updateTrails(mProfileState)) {
                        return;
                    }
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, vprintResult);
                } else {
                    trackInvalidState("result");
                }
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mVprintKernel != null) {
                        mVprintKernel.releaseKernel();
                    }
                    clearObject();//清除实例
                    transferState(EngineState.STATE_IDLE);
                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_ERROR:
                AIError error = (AIError) msg.obj;
                Log.w(TAG, error.toString());
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, msg.obj);
                //当RUNNING状态下，即start和feed出现异常之后，需要将对应的状态置为STATE_NEWED，需要重新进行start-feed-stop的流程
                if (mState != EngineState.STATE_NEWED && mState != EngineState.STATE_IDLE) {
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("error");
                }
                break;
            case MSG_VPRINT_TLV:
                byte[] data = (byte[]) msg.obj;
                if (mState != EngineState.STATE_IDLE) {
                    if (mVprintKernel != null) {
                        mVprintKernel.feedTLV(data);
                    }
                } else {
                    trackInvalidState("vprint_tlv");
                }
                break;
            case MSG_VPRINT_KWS:
                Bundle kwsData = (Bundle) msg.obj;
                if (mState != EngineState.STATE_IDLE) {
                    if (mVprintKernel != null) {
                        mVprintKernel.feedKWS(kwsData);
                    }
                } else {
                    trackInvalidState("vprint_kws");
                }
                break;
            default:
                break;
        }
    }


    @Override
    public void release() {
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


    private class MyVprintKernelListener implements VprintLiteKernelListener {

        @Override
        public void onInit(int status) {
            Log.d(TAG, "vprint Init status: " + status);
            processInit(status);
        }

        @Override
        public void onError(AIError error) {
            Log.d(TAG, "MyVprintKernelListener onError: " + error.toString());
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onResults(AIResult result) {
            filterResults(result);
        }
    }

    private void filterResults(AIResult aiResult) {
        if (aiResult.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
            String vprintRetString = aiResult.getResultObject().toString();
            try {
                JSONObject vprintJsonStr = new JSONObject(vprintRetString);
                String option = vprintJsonStr.optString("option");
                int state = vprintJsonStr.optInt("state");
                Log.d(TAG, "state " + state + " option " + option);
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
                        vprintJsonStr.put("operation", "continue");
                        aiResult.setResultObject(vprintJsonStr.toString());
                    } else if (state == 0) {
                        vprintJsonStr.put("operation", "success");
                        aiResult.setResultObject(vprintJsonStr.toString());
                    } else {
                        vprintJsonStr.put("operation", "failed");
                        aiResult.setResultObject(vprintJsonStr.toString());
                    }
                } else if (TextUtils.equals(option, "NewInstance")) {
                    aiResult.setResultObject(vprintRetString);
                }
                sendMsgToInnerMsgQueue(EngineMsg.MSG_VPRINT_RESULT, aiResult);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (aiResult.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VPRINT_RESULT, aiResult);
        }
    }


    @Override
    public void processNoSpeechError() {
    }

    @Override
    public void processMaxSpeechError() {

    }

}
