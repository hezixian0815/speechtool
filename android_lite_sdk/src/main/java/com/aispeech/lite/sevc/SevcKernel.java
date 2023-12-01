package com.aispeech.lite.sevc;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.kernel.Sspe;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.LocalSevcConfig;
import com.aispeech.lite.message.Message;

import org.json.JSONObject;

/**
 * 输入多路数据，输出一路数据。内部这个数据流向: aec bbs nr agc
 */
public class SevcKernel extends BaseKernel {

    private static final String TAG = "SevcKernel";
    private SevcListener mListener;
    private Sspe mEngine;

    public SevcKernel(SevcListener listener) {
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
                    if (mState != EngineState.STATE_IDLE) {
                        trackInvalidState("new");
                    } else {
                        LocalSevcConfig config = (LocalSevcConfig) message.mObject;
                        mEngine = new Sspe();
                        int flag = initEngine(config, mEngine);
                        if (flag == AIConstant.OPT_SUCCESS)
                            transferState(EngineState.STATE_NEWED);
                        if (mListener != null)
                            mListener.onInit(flag);
                    }
                    break;
                case Message.MSG_START:
                    if (mState != EngineState.STATE_NEWED) {
                        trackInvalidState("start");
                    } else {
                        if (mEngine != null) {
                            mEngine.start("");
                            transferState(EngineState.STATE_RUNNING);
                        }
                    }
                    break;
                case Message.MSG_STOP:
                    if (mState != EngineState.STATE_RUNNING) {
                        trackInvalidState("stop");
                    } else {
                        if (mEngine != null) {
                            mEngine.stop();
                        }
                        transferState(EngineState.STATE_NEWED);
                    }
                    break;
                case Message.MSG_RELEASE:
                    if (mState == EngineState.STATE_IDLE) {
                        trackInvalidState("release");
                    } else {
                        // 销毁引擎
                        if (mEngine != null) {
                            mEngine.destroy();
                            mEngine = null;
                        }
                        transferState(EngineState.STATE_IDLE);
                    }
                    mListener = null;
                    isReleased = true;
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    if (mState != EngineState.STATE_RUNNING) {
                        trackInvalidState("feed");
                    } else {
                        byte[] data = (byte[]) message.mObject;
                        if (mEngine != null) {
                            mEngine.feed(data, data.length);
                        }
                    }
                    break;
                case Message.MSG_SET:
                    if (mState != EngineState.STATE_NEWED) {
                        trackInvalidState("set");
                    } else {
                        if (mEngine != null) {
                            mEngine.set((String) message.mObject);
                        }
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

    @Override
    public int get(String setParam) {
        if (mState != EngineState.STATE_NEWED) {
            trackInvalidState("get");
        } else {
            if (mEngine != null) {
                return mEngine.get(setParam);
            }
        }
        return -1;
    }

    private class MyOutputCallback extends Sspe.output_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] buffer = new byte[size];
                System.arraycopy(data, 0, buffer, 0, size);
                if (mListener != null) {
                    mListener.onSevcBufferReceived(buffer);
                }
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }

    private class MySevcDoaCallback implements Sspe.doa_callback {

        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "MySevcDoaCallback return : " + retString);
                try {
                    JSONObject object = new JSONObject(retString);
                    if (object.has("doa")) {
                        mListener.onSevcDoaResult((Integer) object.get("doa"));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
    }

    private class MySevcNoiseCallback implements Sspe.sevc_noise_callback {

        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "MySevcDoaCallback return : " + retString);
                mListener.onSevcNoiseResult(retString);
            }
            return 0;
        }
    }
    /**
     * input音频回调，返回喂给算法内核的音频数据
     */
    private class MyInputCallbackImpl extends Sspe.input_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bufferData = new byte[size];
                System.arraycopy(data, 0, bufferData, 0, size);
                mListener.onInputDataReceived(bufferData, size);
            }
            return 0;
        }
    }


    private int initEngine(AIEngineConfig config, Sspe engine) {
        if (config == null)
            return AIConstant.OPT_FAILED;

        // 创建引擎
        String cfg = config.toString();
        Log.d(TAG, "config" + cfg);
        long engineId = engine.init(cfg);
        if (engineId == 0) {
            Log.d(TAG, "引擎初始化失败");
            return AIConstant.OPT_FAILED;
        }

        int ret = engine.setCallback(
                new MyInputCallbackImpl(),
                new MyOutputCallback(),
                new MySevcDoaCallback(),
                new MySevcNoiseCallback());
        if (ret != 0) {
            Log.d(TAG, "引擎setCallback失败");
            return AIConstant.OPT_FAILED;
        } else {
            Log.d(TAG, "引擎初始化成功");
            return AIConstant.OPT_SUCCESS;
        }
    }
}
