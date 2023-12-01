package com.aispeech.lite.vprintlite;

import static com.aispeech.AIError.ERR_CODE_VPRINT_LITE_CACHE_FAILED;
import static com.aispeech.AIError.ERR_CODE_VPRINT_LITE_CANT_ENHANCE;
import static com.aispeech.AIError.ERR_CODE_VPRINT_LITE_CONTEXT_NOT_MATCHED;
import static com.aispeech.AIError.ERR_CODE_VPRINT_LITE_MODEL_LOADED_FAILED;
import static com.aispeech.AIError.ERR_CODE_VPRINT_LITE_MODEL_SERVER_FAILED;
import static com.aispeech.AIError.ERR_CODE_VPRINT_LITE_NO_REGISTER_SPEAKER;
import static com.aispeech.AIError.ERR_CODE_VPRINT_LITE_VIDEO_LEAK;
import static com.aispeech.AIError.ERR_DEFAULT;
import static com.aispeech.AIError.ERR_DESCRIPTION_VPRINT_LITE_CACHE_FAILED;
import static com.aispeech.AIError.ERR_DESCRIPTION_VPRINT_LITE_CANT_ENHANCE;
import static com.aispeech.AIError.ERR_DESCRIPTION_VPRINT_LITE_CONTEXT_NOT_MATCHED;
import static com.aispeech.AIError.ERR_DESCRIPTION_VPRINT_LITE_MODEL_LOADED_FAILED;
import static com.aispeech.AIError.ERR_DESCRIPTION_VPRINT_LITE_MODEL_SERVER_FAILED;
import static com.aispeech.AIError.ERR_DESCRIPTION_VPRINT_LITE_NO_REGISTER_SPEAKER;
import static com.aispeech.AIError.ERR_DESCRIPTION_VPRINT_LITE_VIDEO_LEAK;

import android.os.Bundle;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.VprintLite;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.LocalVprintLiteConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.VprintLiteParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * 声纹内核封装模块
 */
public class VprintLiteKernel extends BaseKernel implements VprintLite.vprintlite_callback {
    public static final int MSG_FEED_DATA_TLV = 41;
    public static final int MSG_FEED_DATA_KWS = 42;
    private static final String TAG = "VprintLiteKernel";
    LocalVprintLiteConfig localVprintConfig;
    private VprintLiteKernelListener mListener;
    private VprintLite mEngine;
    private volatile boolean isCanceled = false;
    private FileOutputStream pcmOutputStream = null;

    public VprintLiteKernel(VprintLiteKernelListener listener) {
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
                    if (mState == EngineState.STATE_IDLE) {
                        isCanceled = false;
                        localVprintConfig = (LocalVprintLiteConfig) message.mObject;

                        if (mEngine == null) {
                            mEngine = new VprintLite();
                        }
                        int flag = initEngine(localVprintConfig, mEngine);
                        mListener.onInit(flag);
                        if (flag == AIConstant.OPT_SUCCESS) {
                            transferState(EngineState.STATE_NEWED);
                        }
                    } else {
                        trackInvalidState("new");
                    }
                    break;
                case Message.MSG_START:
                    Log.e(TAG, "run: MSG_START" +mState);
                    if (mState == EngineState.STATE_NEWED) {
                        VprintLiteParams param = (VprintLiteParams) message.mObject;
                        Log.d(TAG, "VPRINT.ENV: " + param.toStartJSON(localVprintConfig.getVprintType()).toString());
                        isCanceled = false;
                        createVprintLiteOutputStream(param.getVprintLiteSaveDir(), param.getAction());
                        int ret = mEngine.start(param.toStartJSON(localVprintConfig.getVprintType()).toString());
                        if (ret == 0) {
                            //只在vprint start 成功才置位，保护feed接口
                            if (!param.getAction().equals(AIConstant.VP_UNREGISTER)
                                    && !param.getAction().equals(AIConstant.VP_UNREGISTER_ALL)) {//删除时候不切换状态，保护feed接口
                                transferState(EngineState.STATE_RUNNING);
                            } else {
                                Log.d(TAG, "unregister model success, need not feed data");
                            }
                        }
                    } else {
                        trackInvalidState("start");
                    }
                    break;
                case Message.MSG_SET:
                    if (mState != EngineState.STATE_IDLE) {
                        String queryEnv = (String) message.mObject;
                        if (mEngine != null) {
                            Log.d(TAG, "query env: " + queryEnv);
                            mEngine.start(queryEnv);
                        }
                    } else {
                        trackInvalidState("set");
                    }
                    break;
                case Message.MSG_STOP:
                    if (mState == EngineState.STATE_RUNNING) {
                        mEngine.stop();
                        releaseFileOutputStream();
                        transferState(EngineState.STATE_NEWED);
                    } else {
                        trackInvalidState("stop");
                    }
                    break;
                case Message.MSG_RELEASE:
                    if (mState != EngineState.STATE_IDLE) {
                        // 销毁引擎
                        mEngine.destroy();
                        mEngine = null;
                        releaseFileOutputStream();
                        transferState(EngineState.STATE_IDLE);
                    } else {
                        trackInvalidState("release");
                    }
                    isReleased = true;
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    if (mState == EngineState.STATE_RUNNING) {
                        int status = mEngine.feed(data, data.length, "");
                        if (status == -1) {
                            Log.e(TAG, "声纹内核发生未知错误");
                            mListener.onError(new AIError(AIError.ERR_DEFAULT, AIError.ERR_DESCRIPTION_DEFAULT));
                            //发生错误需要重新走start-feed-stop的流程
                            transferState(EngineState.STATE_NEWED);
                        }
                    } else {
                        trackInvalidState("feed");
                    }
                    break;
                case MSG_FEED_DATA_KWS:
                    Bundle bundle = (Bundle) message.mObject;
                    byte[] kwsData = bundle.getByteArray("bytes");
                    String params = bundle.getString("params");
                    if (mState == EngineState.STATE_RUNNING) {
                        savePcmData(kwsData, kwsData.length);
                        int status = mEngine.feed(kwsData, kwsData.length, params);
                        if (status == -1) {
                            Log.e(TAG, "声纹内核发生未知错误");
                            mListener.onError(new AIError(AIError.ERR_DEFAULT, AIError.ERR_DESCRIPTION_DEFAULT));
                        }
                    } else {
                        trackInvalidState("feed kws");
                    }
                    break;
                case Message.MSG_AIENGINE_RESULT:
                    AIResult aiResult = (AIResult) message.mObject;
                    if (mState != EngineState.STATE_IDLE) {
                        if (aiResult.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON)
                            handleCallbackStr(aiResult);
                        else if (aiResult.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                            if (mListener != null) {
                                mListener.onResults(aiResult);
                            }
                        }
                    } else {
                        trackInvalidState("result");
                    }
                    break;
                case Message.MSG_ERROR:
                    releaseFileOutputStream();
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

    public void feed(byte[] data, int size) {
        byte[] bufferData = new byte[size];
        System.arraycopy(data, 0, bufferData, 0, size);
        sendMessage(new Message(Message.MSG_FEED_DATA_BY_STREAM, bufferData));
    }

    @Override
    public void cancelKernel() {
        if (mState == EngineState.STATE_RUNNING) {
            //让外面调用瞬间生效，否则走到消息队列callback已经送出去了
            isCanceled = true;
            Log.d(TAG, "reset cancel flag");
            releaseFileOutputStream();
            clearMessage();
            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("cancel");
        }
    }

    private int initEngine(LocalVprintLiteConfig config, VprintLite engine) {
        int status;
        if (config != null) {
            Log.d(TAG, "VPRINT.CONFIG: " + config.toJson().toString());
            long engineId = engine.init(config.toJson().toString(), this);
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

    @Override
    public int run(int type, byte[] retData, int size) {
        byte[] data = new byte[size];
        System.arraycopy(retData, 0, data, 0, size);
        if (type == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
            try {
                String vprintStr = new String(data, "UTF-8").trim();
                Log.d(TAG, "VPRINT.CALLBACK: " + vprintStr);
                try {
                    JSONObject object = new JSONObject(vprintStr);
                    String option = object.optString("activity");
                    if (!isCanceled || "Query".equals(option) || "QueryRegisterAudio".equals(option)) {
                        AIResult result = new AIResult();
                        result.setLast(true);
                        result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
                        result.setResultObject(vprintStr);
                        result.setTimestamp(System.currentTimeMillis());
                        sendMessage(new Message(Message.MSG_AIENGINE_RESULT, result));
                    } else {
                        Log.d(TAG, "vprint engine canceled, ignore callback");
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        } else if (type == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
            Log.d(TAG, "VPRINT.query_register_audio: size " + size);
            AIResult result = new AIResult();
            result.setLast(true);
            result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_BIN);
            result.setResultObject(data);
            result.setTimestamp(System.currentTimeMillis());
            sendMessage(new Message(Message.MSG_AIENGINE_RESULT, result));
        }
        return 0;
    }

    private void handleCallbackStr(AIResult aiResult) {
        String callbackStr = aiResult.getResultObject().toString();
        try {
            JSONObject jsonObject = new JSONObject(callbackStr);
            if (jsonObject.has("state")) {
                int state = jsonObject.optInt("state");
                String option = jsonObject.optString("activity");
                Map<Object, Object> optionMap = new HashMap<>();
                optionMap.put("activity", option);
                Log.d(TAG, "state " + state + " activity " + option);
                switch (state) {
                    case 1:
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(ERR_CODE_VPRINT_LITE_VIDEO_LEAK, ERR_DESCRIPTION_VPRINT_LITE_VIDEO_LEAK).setEventMap(optionMap)));
                        break;
                    case 10:
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(ERR_CODE_VPRINT_LITE_CONTEXT_NOT_MATCHED, ERR_DESCRIPTION_VPRINT_LITE_CONTEXT_NOT_MATCHED).setEventMap(optionMap)));
                        break;
                    case 11:
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(ERR_CODE_VPRINT_LITE_MODEL_SERVER_FAILED, ERR_DESCRIPTION_VPRINT_LITE_MODEL_SERVER_FAILED).setEventMap(optionMap)));
                        break;
                    case 15:
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(ERR_CODE_VPRINT_LITE_MODEL_LOADED_FAILED, ERR_DESCRIPTION_VPRINT_LITE_MODEL_LOADED_FAILED).setEventMap(optionMap)));
                        break;
                    case 16:
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(ERR_CODE_VPRINT_LITE_CANT_ENHANCE, ERR_DESCRIPTION_VPRINT_LITE_CANT_ENHANCE).setEventMap(optionMap)));
                        break;
                    case 17:
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(ERR_CODE_VPRINT_LITE_CACHE_FAILED, ERR_DESCRIPTION_VPRINT_LITE_CACHE_FAILED).setEventMap(optionMap)));
                        break;
                    case 100:
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(ERR_CODE_VPRINT_LITE_NO_REGISTER_SPEAKER, ERR_DESCRIPTION_VPRINT_LITE_NO_REGISTER_SPEAKER).setEventMap(optionMap)));
                        break;
                    case 0://成功
                    case 3://非同一user
                    case 4://声纹验证成功
                        if (mListener != null) {
                            mListener.onResults(aiResult);
                        }
                        break;
                    default:
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(ERR_DEFAULT, "未知错误，内核错误码" + state).setEventMap(optionMap)));
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void feedTLV(byte[] data) {
        sendMessage(new Message(MSG_FEED_DATA_TLV, data));
    }

    public void feedKWS(Bundle data) {
        sendMessage(new Message(MSG_FEED_DATA_KWS, data));
    }

    private void createVprintLiteOutputStream(String dirPath, String action) {
        releaseFileOutputStream();
        if (TextUtils.isEmpty(dirPath))
            return;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dirPath,
                "vprintlite_" + System.currentTimeMillis() + "_" + action + ".pcm");
        Log.d(TAG, "vprint cut path " + file.getAbsolutePath());
        try {
            pcmOutputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void savePcmData(byte[] data, int size) {
        if (pcmOutputStream == null || data == null || size <= 0)
            return;
        try {
            Log.d(TAG, "vprint lite save " + size);
            pcmOutputStream.write(data, 0, size);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseFileOutputStream() {
        if (pcmOutputStream != null) {
            Log.d(TAG, "vprint cut releaseFileOutputStream");
            try {
                pcmOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                pcmOutputStream = null;
            }
        }
    }
}
