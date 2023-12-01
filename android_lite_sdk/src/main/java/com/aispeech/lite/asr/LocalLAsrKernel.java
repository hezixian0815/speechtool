package com.aispeech.lite.asr;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.LAsr;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.LocalLAsrConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.LocalLAsrParams;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalLAsrKernel extends BaseKernel implements LAsr.lasr_callback {

    private static final String TAG = "LocalLAsrKernel";
    private AsrKernelListener mListener;
    private LAsr engine;
    private LocalLAsrParams params;
    private LocalLAsrConfig mConfig;
    private String mRecordId;

    public LocalLAsrKernel(AsrKernelListener listener) {
        super(TAG, listener);
        this.mListener = listener;
    }


    protected int initEngine(LocalLAsrConfig config) {
        if (engine == null) {
            engine = new LAsr();
        }
        long engineId = engine.init(config.getResourcePath(), this);
        if (engineId != 0) {
            Log.d(TAG, "引擎初始化成功");
            return AIConstant.OPT_SUCCESS;
        } else {
            Log.e(TAG, "引擎初始化失败,请检查资源文件是否在指定路径下！");
            return AIConstant.OPT_FAILED;
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
                    if (mState != EngineState.STATE_IDLE) {
                        trackInvalidState("new");
                    } else {
                        mConfig = (LocalLAsrConfig) message.mObject;
                        engine = new LAsr();
                        int flag = initEngine(mConfig);
                        if (flag == AIConstant.OPT_SUCCESS)
                            transferState(EngineState.STATE_NEWED);
                        if (mListener != null) {
                            mListener.onInit(flag);
                        }
                    }
                    break;
                case Message.MSG_START:
                    if (mState != EngineState.STATE_NEWED) {
                        trackInvalidState("start");
                    } else {
                        params = (LocalLAsrParams) message.mObject;
                        mRecordId = Utils.getRecorderId();
                        Log.d(TAG, "start : ");
                        if (engine != null) {
                            startLAsr(params.getLAsrParamJsonString());
                            transferState(EngineState.STATE_RUNNING);
                        }
                    }
                    break;
                case Message.MSG_STOP:
                    if (mState != EngineState.STATE_RUNNING) {
                        trackInvalidState("stop");
                    } else {
                        Log.d(TAG, "stop");
                        if (engine != null) {
                            stopLAsr();
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
                        engine.feed(data);
                    }
                    break;
                case Message.MSG_ERROR:
                    if (mListener != null) {
                        mListener.onError((AIError) message.mObject);
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

    protected int stopLAsr() {
        Log.d(TAG, "engine stop before");
        int ret = engine.stop();
        Log.d(TAG, "engine stop end  ret : " + ret);
        return ret;
    }

    protected int startLAsr(String paramStr) {
        Log.d(TAG, "engine start before");
        int ret = engine.start(paramStr);
        Log.d(TAG, "engine start end");
        Log.d(TAG, "ret:" + ret);
        if (ret < 0) {
            AIError aiError = new AIError(AIError.ERR_AI_ENGINE, AIError.ERR_DESCRIPTION_AI_ENGINE);
            sendMessage(new Message(Message.MSG_ERROR, aiError));
            return ret;
        }
        return ret;
    }

    private enum ResultType {
        EngineStart(0), EngineFeed(1), EngineStop(2), EngineResult(3);

        private ResultType(int value) {
            this.type = value;
        }

        private int type;
    }

    private enum EngineCallbackErrorType {

        Success(0), UnKnow(1), CallSequenceError(2), StartParamError(3);
        private int type;

        private EngineCallbackErrorType(int value) {
            this.type = value;
        }
    }

    @Override
    public int run(byte[] data, int size) {
        String jsonString = new String(data);
        try {
            // byte to string
            JSONObject jsonObject = new JSONObject(jsonString);
            if (jsonObject.getInt("type") == ResultType.EngineStart.type) {
                AIError error = new AIError();
                int errorCode = jsonObject.getInt("err");
                if (errorCode == EngineCallbackErrorType.UnKnow.type) {
                    Log.e(TAG, "start unknown error, current json msg: " + jsonString);
                    error.setErrId(AIError.ERR_DEFAULT);
                    error.setError(AIError.ERR_DESCRIPTION_DEFAULT);
                    sendMessage(new Message(Message.MSG_ERROR, error));
                } else if (errorCode == EngineCallbackErrorType.CallSequenceError.type) {
                    Log.e(TAG, "start sequence error, current json msg: " + jsonString);
                    error.setErrId(AIError.ERR_INTERFACE_CALL_SEQUENCE);
                    error.setError(AIError.ERR_DESCRIPTION_CALL_SEQUENCE);
                    sendMessage(new Message(Message.MSG_ERROR, error));
                } else if (errorCode == EngineCallbackErrorType.StartParamError.type) {
                    Log.e(TAG, "start param error, current json msg: " + jsonString);
                    error.setErrId(AIError.ERR_INTERFACE_PARAM_ERROR);
                    error.setError(AIError.ERR_DESCRIPTION_PARM_ERROR);
                    sendMessage(new Message(Message.MSG_ERROR, error));
                }
            } else if (jsonObject.getInt("type") == ResultType.EngineStop.type) {
                AIError error = new AIError();
                int errorCode = jsonObject.getInt("err");
                if (errorCode == EngineCallbackErrorType.UnKnow.type) {
                    Log.e(TAG, "start unknown error, current json msg: " + jsonString);
                    error.setErrId(AIError.ERR_DEFAULT);
                    error.setError(AIError.ERR_DESCRIPTION_DEFAULT);
                    sendMessage(new Message(Message.MSG_ERROR, error));
                } else if (errorCode == EngineCallbackErrorType.CallSequenceError.type) {
                    Log.e(TAG, "start sequence error, current json msg: " + jsonString);
                    error.setErrId(AIError.ERR_INTERFACE_CALL_SEQUENCE);
                    error.setError(AIError.ERR_DESCRIPTION_CALL_SEQUENCE);
                    sendMessage(new Message(Message.MSG_ERROR, error));
                }
            } else if (jsonObject.getInt("type") == ResultType.EngineFeed.type) {
                Log.e(TAG, "feed data error, json msg" + jsonString);
            } else {
                boolean isLast = false;
                if (jsonObject.optJSONObject("data") != null
                        && jsonObject.optJSONObject("data").optInt("eof") == 1) {
                    isLast = true;
                }
                filter(jsonString, isLast);
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e(TAG, "Parse Json exception, json string: " + jsonString);
        }
        return 0;
    }

    private AIResult filter(String jsonString, boolean isLast) {
        AIResult result = new AIResult();
        try {
            result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
            result.setResultObject(jsonString);
            result.setLast(isLast);
            result.setTimestamp(System.currentTimeMillis());
            result.setRecordId(mRecordId);
            if (mListener != null) {
                mListener.onResults(result);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }
}
