package com.aispeech.lite.mds;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.LiteSoFunction;
import com.aispeech.kernel.Mds;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.LocalMdsConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.MdsParams;

public class MdsKernel extends BaseKernel {

    private static final String TAG = "MdsKernel";
    private MdsKernelListener mListener;
    private LiteSoFunction engine;
    private MdsParams params;
    private LocalMdsConfig mConfig;

    public MdsKernel(MdsKernelListener listener) {
        super(TAG, listener);
        this.mListener = listener;
    }

    @Override
    public synchronized int getValueOf(String param) {
        return -1;
    }

    @Override
    public String getNewConf() {
        if (mConfig != null) {
            return mConfig.toJson().toString();
        }
        return super.getNewConf();
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
                        mConfig = (LocalMdsConfig) message.mObject;
                        engine = new Mds();

                        int flag = initEngine(engine, mConfig);
                        if (flag == AIConstant.OPT_SUCCESS)
                            transferState(EngineState.STATE_NEWED);
                        mListener.onInit(flag);
                    }
                    break;
                case Message.MSG_START:
                    if (mState != EngineState.STATE_NEWED) {
                        trackInvalidState("start");
                    } else {
                        params = (MdsParams) message.mObject;
                        Log.d(TAG, "start");
                        if (engine != null) {
                            engine.start(params.toJSON().toString());
                            transferState(EngineState.STATE_RUNNING);
                        }
                    }
                    break;
                case Message.MSG_CANCEL:
                    if (mState != EngineState.STATE_RUNNING) {
                        trackInvalidState("cancel");
                    } else {
                        Log.d(TAG, "cancel");
                        if (engine != null) {
                            engine.cancel();
                        }

                        transferState(EngineState.STATE_NEWED);
                    }
                    break;
                case Message.MSG_STOP:
                    if (mState != EngineState.STATE_RUNNING) {
                        trackInvalidState("stop");
                    } else {
                        Log.d(TAG, "stop");
                        if (engine != null) {
                            engine.stop();
                        }

                        transferState(EngineState.STATE_NEWED);
                    }
                    break;
                case Message.MSG_RELEASE:
                    if (mState == EngineState.STATE_IDLE) {
                        trackInvalidState("release");
                    } else {
                        Log.d(TAG, "release");
                        if (engine != null) {
                            engine.destroy();
                            engine = null;
                        }
                        transferState(EngineState.STATE_IDLE);
                    }

                    isReleased = true;
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    if (mState != EngineState.STATE_RUNNING) {
                        trackInvalidState("feed");
                    } else {
                        byte[] data = (byte[]) message.mObject;
                        engine.feed(data, data.length);
                    }
                    break;
                case Message.MSG_ERROR:
                    mListener.onError((AIError) message.mObject);
                    break;
                case Message.MSG_SET:
                    if (mState != EngineState.STATE_NEWED) {
                        trackInvalidState("set");
                    } else {
                        String paramsSet = (String) message.mObject;
                        engine.set(paramsSet);
                    }
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


    /**
     * 用户通过此方法传入mds的算法值，决策出哪个是最优设备
     * @param data float数组：每个设备的snr算法值有三个，实例：如三台设备a、b、c各有三个值 数组格式为 [a1,b1,c1,a2,b2,c2,a3,b3,c3]
     * @param num
     * @param size
     * @return 唤醒设备的索引
     */
    public int mcdmFeed(float[] data, int num, int size) {
        if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_RUNNING) {
            return ((Mds) engine).mcdmFeed(data, num, size);

        }else {
            trackInvalidState("mcdmFeed");
            return -1;
        }

    }

    protected int initEngine(LiteSoFunction engine, AIEngineConfig config) {
        String cfg = (config == null) ? "" : config.toJson().toString();
        Log.d(TAG, "initEngine config: " + cfg);
        long engineId = engine.init(cfg, new MyMdsCB());
        Log.d(TAG, "initEngine create engineId " + engineId);
        if (engineId == 0) {
            Log.d(TAG, "引擎初始化失败");
            return AIConstant.OPT_FAILED;
        }
        Log.d(TAG, "引擎初始化成功");
        return AIConstant.OPT_SUCCESS;
    }

    private class MyMdsCB implements Mds.mds_callback {

        @Override
        public int run(int type, byte[] data, int size) {
            if (type == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String str = new String(data).trim();
                Log.d(TAG, "MyMdsCB: " + str);

                AIResult result = new AIResult();
                result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
                result.setResultObject(str);
                result.setTimestamp(System.currentTimeMillis());
                result.setRecordId(Utils.getRecorderId());

                if (mListener != null) {
                    mListener.onResults(result);
                }
            } else {
                Log.d(TAG, "should not have other type:" + type);
            }
            return 0;
        }
    }

}
