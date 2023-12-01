package com.aispeech.lite.nr;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.NR;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.LocalNRConfig;
import com.aispeech.lite.message.Message;

public class NRKernel extends BaseKernel {
    public static final String TAG = "NRKernel";
    private NRKernelListener mListener;
    private NR mEngine;
    private MyNRCallback myNRCallback;
    private volatile boolean isStopped = true;


    public NRKernel(NRKernelListener listener) {
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
                    LocalNRConfig config = (LocalNRConfig) message.mObject;
                    mEngine = new NR();
                    myNRCallback = new MyNRCallback();
                    int flag = initNR(config, mEngine, myNRCallback);
                    if (mListener != null)
                        mListener.onInit(flag);
                    break;
                case Message.MSG_START:
                    if (mEngine != null) {
                        mEngine.start("");
                    }
                    isStopped = false;
                    break;
                case Message.MSG_STOP:
                    if (mEngine != null) {
                        mEngine.stop();
                    }
                    isStopped = true;
                    break;
                case Message.MSG_RELEASE:
                    // 销毁引擎
                    if (mEngine != null) {
                        mEngine.destroy();
                        mEngine = null;
                    }
                    mListener = null;
                    isReleased = true;
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    if (mEngine != null && !isStopped) {
                        mEngine.feed(data);
                    }
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


    private class MyNRCallback extends NR.nr_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] nrData = new byte[size];
                System.arraycopy(data, 0, nrData, 0, size);
                if (mListener != null) {
                    mListener.onBufferReceived(nrData, size);
                }
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }


    private int initNR(AIEngineConfig config, NR engine, MyNRCallback callback) {
        int status = AIConstant.OPT_FAILED;
        if (config != null) {
            // 创建引擎
            String cfg = (config == null) ? null : config.toString();
            Log.d(TAG, "config" + cfg);
            long engineId = engine.init(cfg, callback);
            Log.d(TAG, "nr create return " + engineId + ".");
            if (engineId == 0) {
                Log.d(TAG, "引擎初始化失败");
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
}
