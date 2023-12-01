package com.aispeech.lite.asrpp;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.Asrpp;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.LocalAsrppConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.LocalAsrppParams;

/**
 * Created by wuwei on 2018/7/17.
 */

public class AsrppKernel extends BaseKernel implements Asrpp.asrpp_callback {

    private static final String TAG = "AsrppKernel";
    private AsrppKernelListener mListener;
    private Asrpp mAsrpp;
    private volatile boolean isStopped = true;
    private volatile boolean isCanceled = false;

    public AsrppKernel(AsrppKernelListener listener) {
        super(TAG, listener);
        this.mListener = listener;
    }


    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    handleMsgNew(message);
                    break;
                case Message.MSG_START:
                    handleMsgStart(message);
                    break;
                case Message.MSG_STOP:
                    handleMsgStop();
                    break;
                case Message.MSG_RELEASE:
                    if (mState == EngineState.STATE_IDLE) {
                        trackInvalidState("release");
                        break;
                    }
                    // 销毁引擎
                    destroyAsrppEngine(mAsrpp);
                    transferState(EngineState.STATE_IDLE);
                    isReleased = true;
                    isStopped = true;
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    if (mState != EngineState.STATE_RUNNING) {
                        trackInvalidState("feed");
                        break;
                    }
                    if (mAsrpp != null && !isStopped) {
                        mAsrpp.feed(data, data.length);
                    }
                    break;
                case Message.MSG_ERROR:
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

    private void handleMsgStop() {
        if (mState != EngineState.STATE_RUNNING) {
            trackInvalidState("stop");
            return;
        }
        stopAsrppEngine(mAsrpp);
        isStopped = true;
        transferState(EngineState.STATE_NEWED);
    }

    private void handleMsgStart(Message message) {
        if (mState != EngineState.STATE_NEWED) {
            trackInvalidState("start");
            return;
        }
        LocalAsrppParams asrppParams = (LocalAsrppParams) message.mObject;
        if (mAsrpp != null) {
            int ret = startAsrppEngine(asrppParams.toJSON().toString(), mAsrpp);
            isStopped = false;
            isCanceled = false;
            if (ret == AIConstant.OPT_SUCCESS)
                transferState(EngineState.STATE_RUNNING);
        }
    }

    private void handleMsgNew(Message message) {
        if (mState != EngineState.STATE_IDLE) {
            trackInvalidState("new");
            return;
        }
        mAsrpp = new Asrpp();
        LocalAsrppConfig localAsrppConfig = (LocalAsrppConfig) message.mObject;
        int flag = initAsrppEngine(localAsrppConfig, mAsrpp);
        mListener.onInit(flag);
        if (flag == AIConstant.OPT_SUCCESS) {
            transferState(EngineState.STATE_NEWED);
        }
    }


    @Override
    public void cancelKernel() {
        if (mState == EngineState.STATE_RUNNING) {
            trackInvalidState("cancel");
            return;
        }
        isCanceled = true;
        Log.d(TAG, "reset cancel flag");
        clearMessage();
        transferState(EngineState.STATE_NEWED);
    }

    protected int initAsrppEngine(LocalAsrppConfig config, Asrpp engine) {
        int status;
        if (config != null) {
            String configStr = config.toJson().toString();
            Log.d(TAG, "LocalAsrppConfig:\t" + configStr);
            long engineId = engine.init(configStr, this);
            if (engineId == 0) {
                Log.e(TAG, "引擎初始化失败");
                status = AIConstant.OPT_FAILED;
            } else {
                Log.d(TAG, "引擎初始化成功");
                status = AIConstant.OPT_SUCCESS;
            }
        } else {
            status = AIConstant.OPT_FAILED;
        }
        return status;
    }


    protected int startAsrppEngine(String param, Asrpp engine) {
        int startId = engine.start(param);
        if (startId != 0) {
            sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_AI_ENGINE, AIError.ERR_DESCRIPTION_AI_ENGINE)));
        }
        return startId;
    }


    protected void stopAsrppEngine(Asrpp engine) {
        if (engine != null) {
            engine.stop();
        }
    }

    protected void cancelAsrppEngine(Asrpp engine) {
        if (engine != null) {
            engine.cancel();
        }
    }

    protected void destroyAsrppEngine(Asrpp engine) {
        if (engine != null) {
            engine.destroy();
            mAsrpp = null;
        }
    }

    @Override
    public int run(int type, byte[] retData, int size) {
        if (type == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
            byte[] data = new byte[size];
            System.arraycopy(retData, 0, data, 0, size);
            Log.d(TAG, "ASRPP.CALLBACK: " + new String(data).trim());
            if (!isCanceled) {
                processAsrppCallback(new String(data).trim());
            } else {
                Log.w(TAG, "canceled Asrpp engine, ignore callback");
            }
        }
        return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
    }

    public void processAsrppCallback(String vprintRetString) {
        if (!TextUtils.isEmpty(vprintRetString)) {
            AIResult result = new AIResult();
            result.setLast(true);
            result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
            result.setResultObject(vprintRetString);
            result.setTimestamp(System.currentTimeMillis());
            if (mListener != null) {
                mListener.onResults(result);
            }
        }
    }
}
