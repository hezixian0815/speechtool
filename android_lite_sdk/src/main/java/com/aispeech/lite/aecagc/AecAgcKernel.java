package com.aispeech.lite.aecagc;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.AecAgc;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.LocalAecAgcConfig;
import com.aispeech.lite.message.Message;

import org.json.JSONException;
import org.json.JSONObject;

public class AecAgcKernel extends BaseKernel {

    private static final String TAG = "AecAgcKernel";
    private AecAgcKernelListener mListener;
    private AecAgc mEngine;
    private volatile boolean isStopped = true;
    private AecAgc.aec_callback aecCallback;
    private AecAgc.voip_callback voipCallback;

    public AecAgcKernel(AecAgcKernelListener listener) {
        super(TAG, listener);
        this.mListener = listener;
    }


    private class MyAecCallback extends AecAgc.aec_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bfBuffer = new byte[size];
                System.arraycopy(data, 0, bfBuffer, 0, size);
                if (mListener != null && mState != EngineState.STATE_IDLE)
                    mListener.onAecBufferReceived(bfBuffer);
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }

    private class MyVoipCallback extends AecAgc.voip_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] voipBuffer = new byte[size];
                System.arraycopy(data, 0, voipBuffer, 0, size);
                if (mListener != null && mState != EngineState.STATE_IDLE)
                    mListener.onVoipBufferReceived(voipBuffer);
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }

    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    handleMessageNew(message);
                    break;
                case Message.MSG_START:
                    handleMsgStart();
                    break;
                case Message.MSG_SET:
                    handleMsgSet(message);
                    break;
                case Message.MSG_STOP:
                    handleMsgStop();
                    break;
                case Message.MSG_CANCEL:
                    handleMsgCancel();
                    break;
                case Message.MSG_RELEASE:
                    if (mState == EngineState.STATE_IDLE) {
                        trackInvalidState("release");
                    } else {
                        handleMsgRelease();
                        isReleased = true;
                        transferState(EngineState.STATE_IDLE);
                    }
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    handleMsgFeedData(message);
                    break;
                case Message.MSG_ERROR:
                    if (mListener != null)
                        mListener.onError((AIError) message.mObject);
                    break;
                default:
                    break;
            }
            if (isReleased) {
                innerRelease();
                break;//release后跳出while循环
            }
        }
    }

    private void handleMsgRelease() {
        if (mEngine != null) {
            mEngine.destroy();
            mEngine = null;
        }
        if (aecCallback != null)
            aecCallback = null;
        if (voipCallback != null)
            voipCallback = null;
        isStopped = true;
    }

    private void handleMsgFeedData(Message message) {
        if (mState != EngineState.STATE_RUNNING) {
            trackInvalidState("feed");
        } else {
            byte[] data = (byte[]) message.mObject;
            if (mEngine != null && !isStopped) {
                mEngine.feed(data, data.length);
            }
        }
    }

    private void handleMsgCancel() {
        if (mState != EngineState.STATE_RUNNING) {
            trackInvalidState("cancel");
        } else {
            if (mEngine != null)
                mEngine.cancel();
            isStopped = true;
            transferState(EngineState.STATE_NEWED);
        }
    }

    private void handleMsgStop() {
        if (mState != EngineState.STATE_RUNNING) {
            trackInvalidState("stop");
        } else {
            if (mEngine != null)
                mEngine.stop();
            isStopped = true;
            transferState(EngineState.STATE_NEWED);
        }
    }

    private void handleMsgSet(Message message) {
        if (mState == EngineState.STATE_IDLE) {
            trackInvalidState("set");
        } else {
            String setParam = (String) message.mObject;
            if (mEngine != null) {
                mEngine.set(setParam);
            }
        }
    }

    private void handleMsgStart() {
        if (mState != EngineState.STATE_NEWED) {
            trackInvalidState("start");
        } else {
            if (mEngine != null) {
                mEngine.start("");
                isStopped = false;
                transferState(EngineState.STATE_RUNNING);
            }
        }
    }

    private void handleMessageNew(Message message) {
        if (mState != EngineState.STATE_IDLE) {
            trackInvalidState("new");
        } else {
            LocalAecAgcConfig mAecAgcConfig = (LocalAecAgcConfig) message.mObject;
            mEngine = new AecAgc();
            this.aecCallback = new MyAecCallback();
            this.voipCallback = new MyVoipCallback();
            int flag = initAecAgc(mAecAgcConfig, mEngine);
            if (mListener != null)
                mListener.onInit(flag);
            if (flag == AIConstant.OPT_SUCCESS)
                transferState(EngineState.STATE_NEWED);
        }
    }


    private int initAecAgc(LocalAecAgcConfig config, AecAgc engine) {
        int status;
        if (config != null) {
            // 创建引擎
            String cfg = config.toJson().toString();
            Log.d(TAG, "config" + cfg);
            long engineId = engine.init(cfg);
            Log.d(TAG, "create return " + engineId + ".");
            if (engineId == 0) {
                Log.d(TAG, "引擎初始化失败");
                return AIConstant.OPT_FAILED;
            }
            Log.d(TAG, "引擎初始化成功");
            boolean suc = engine.setCallback(aecCallback, voipCallback);
            status = suc ? AIConstant.OPT_SUCCESS : AIConstant.OPT_FAILED;
            Log.d(TAG, "AecAgc setCallback status " + status);
        } else {
            status = AIConstant.OPT_FAILED;
        }
        return status;
    }

    /**
     * 开关voip功能，默认为开。
     *
     * @param voipSwitch true 开（default）,false 关
     */
    public void switchVoip(boolean voipSwitch) {
        Log.d(TAG, "switchVoip " + voipSwitch);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("voipSwitch", voipSwitch ? 1 : 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        set(jsonObject.toString());
    }
}
