package com.aispeech.lite.vprint;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.kernel.Vprint;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.LocalVprintConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.VprintParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * 声纹内核封装模块
 */
public class VprintKernel extends BaseKernel implements Vprint.vprint_callback {
    public static final int MSG_FEED_DATA_TLV = 41;
    private static final String TAG = "VprintKernel";
    private static final String KEY_OPTION = "option";
    private static final String KEY_STATE = "state";
    private VprintKernelListener mListener;
    private Vprint mEngine;
    private volatile boolean isCanceled = false;
    private volatile VprintQueryData vprintQueryData;
    private volatile VprintDatabaseManager dbManager;
    private FileOutputStream tlvPcmOutputStream = null;

    public VprintKernel(VprintKernelListener listener) {
        super(TAG, listener);
        this.mListener = listener;
    }

    private static int byteArrayToInt(byte[] byteArray) {
        if (byteArray == null || byteArray.length == 0) {
            return 0;
        }
        int len = byteArray.length;
        int value = byteArray[0] & 0xFF;
        for (int i = 1; i < len; i++) {
            value |= (byteArray[i] & 0xff) << 8 * i;
        }
        return value;
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
                        LocalVprintConfig localVprintConfig = (LocalVprintConfig) message.mObject;
                        if (localVprintConfig.isUseDatabaseStorage() && dbManager == null) {
                            synchronized (VprintKernel.this) {
                                if (dbManager == null) {
                                    Log.d(TAG, "user set dbPath: " + localVprintConfig.getVprintDatabasePath());
                                    dbManager = new VprintDatabaseManager(localVprintConfig.getVprintDatabasePath());
                                    vprintQueryData = null;
                                }
                            }
                        }

                        if (mEngine == null) {
                            mEngine = new Vprint();
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
                    if (mState == EngineState.STATE_NEWED) {
                        VprintParams param = (VprintParams) message.mObject;
                        Log.d(TAG, "VPRINT.ENV: " + param.toJSON().toString());
                        isCanceled = false;
                        createVprintCutOutputStream(param.getVprintCutSaveDir() + File.separator + "vprintCut", param.getAction());
                        int ret = mEngine.start(param.toJSON().toString());
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
                case Message.MSG_CANCEL:
                    if (mState == EngineState.STATE_RUNNING) {
                        transferState(EngineState.STATE_NEWED);
                    } else {
                        trackInvalidState("cancel");
                    }
                    break;
                case Message.MSG_RELEASE:
                    if (mState != EngineState.STATE_IDLE) {
                        // 销毁引擎
                        mEngine.destroy();
                        mEngine = null;
                        synchronized (this) {
                            if (dbManager != null) {
                                dbManager.close();
                                dbManager = null;
                            }
                            vprintQueryData = null;
                        }
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
                        int status = mEngine.feed(data, data.length, AIConstant.AIENGINE_MESSAGE_TYPE_BIN);
                        if (status == -1) {
                            Log.e(TAG, "声纹内核发生未知错误");
                            mListener.onError(new AIError(AIError.ERR_DEFAULT, AIError.ERR_DESCRIPTION_DEFAULT));
                        }
                    } else {
                        trackInvalidState("feed");
                    }
                    break;
                case MSG_FEED_DATA_TLV:
                    byte[] tlvData = (byte[]) message.mObject;
                    if (mState == EngineState.STATE_RUNNING) {
                        parseTLV(tlvData);
                        int status = mEngine.feed(tlvData, tlvData.length, AIConstant.DUILITE_MSG_TYPE_TLV);
                        if (status == -1) {
                            Log.e(TAG, "声纹内核发生未知错误");
                            mListener.onError(new AIError(AIError.ERR_DEFAULT, AIError.ERR_DESCRIPTION_DEFAULT));
                        }
                    } else {
                        trackInvalidState("feed tlv");
                    }
                    break;
                case Message.MSG_EVENT:
                    String event = (String) message.mObject;
                    if (mState == EngineState.STATE_RUNNING) {
                        mEngine.feed(event.getBytes(), event.getBytes().length, AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
                    } else {
                        trackInvalidState("event");
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
            super.cancelKernel();
        } else {
            trackInvalidState("cancel");
        }
    }

    public void notifyEvent(String event) {
        sendMessage(new Message(Message.MSG_EVENT, event));
    }

    private int initEngine(LocalVprintConfig config, Vprint engine) {
        int status;
        if (config != null) {
            Log.d(TAG, "VPRINT.CONFIG: " + config.toJson().toString());
            long engineId = engine.init(config.isUseDatabaseStorage(), config.toJson().toString(), this);
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
                    String option = object.optString("option");
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

    private synchronized VprintQueryData getVprintQueryData() {
        if (vprintQueryData == null) {
            if (dbManager != null)
                vprintQueryData = VprintQueryData.toVprintQueryData(dbManager.queryAll());
            else {
                vprintQueryData = new VprintQueryData(new int[0], new byte[0][0]);
                Log.d(TAG, "dbManager is null");
            }
        }
        return vprintQueryData;
    }

    @Override
    public int getModelNum() {
        int num = getVprintQueryData().num();
        Log.d(TAG, "getModelNum: " + num);
        return num;
    }

    @Override
    public int[] getModelSize() {
        int[] size = getVprintQueryData().getModelSize();
        Log.d(TAG, "getModelSize: " + Arrays.toString(size));
        return size;
    }

    @Override
    public byte[][] getModelBin() {
        Log.d(TAG, "getModelBin");
        return getVprintQueryData().getModelBin();
    }

    /**
     * 对声纹数据的操作，增删改
     *
     * @param type 操作类型
     * @param id   用户名+唤醒词
     * @param data 声纹模型数据
     * @param size 声纹模型数据的长度
     * @param num  模型个数
     * @return 0 成功 -1 数据库操作失败 -2 参数错误 -3 数据库未初始化，一般由于状态不对导致
     */
    @Override
    public synchronized int model_run(int type, String id, byte[][] data, int[] size, int num) {
        Log.d(TAG, "model_run type:" + type + " id:" + id + " num:" + num + " size:" + Arrays.toString(size));
        if (TextUtils.isEmpty(id))
            return -2;
        if (dbManager == null) {
            Log.d(TAG, "dbManager is null");
            return -3;
        }
        switch (type) {
            case 1:
                // VP_UPDATE
                if (data == null || data.length <= 0 || num <= 0) {
                    Log.d(TAG, "VP_UPDATE data is empty");
                    return -2;
                } else {
                    boolean suc = dbManager.update(id, data[0]);
                    Log.d(TAG, "VP_UPDATE " + suc + " length " + data[0].length);
                    return suc ? 0 : -1;
                }
            case 2:
                // VP_INSERT
                if (data == null || data.length <= 0 || num <= 0) {
                    Log.d(TAG, "VP_INSERT data is empty");
                    return -2;
                } else {
                    boolean suc = dbManager.insertOrUpdate(new VprintSqlEntity(id, data[0], System.currentTimeMillis()));
                    Log.d(TAG, "VP_INSERT " + suc + " length " + data[0].length);
                    return suc ? 0 : -1;
                }
            case 3:
                // VP_DELETE
                boolean suc = dbManager.delete(id);
                Log.d(TAG, "VP_DELETE " + suc);
                return suc ? 0 : -1;
            default:
                Log.d(TAG, "wrong type:" + type);
                return -2;
        }
    }

    private void handleCallbackStr(AIResult aiResult) {
        String callbackStr = aiResult.getResultObject().toString();
        try {
            JSONObject jsonObject = new JSONObject(callbackStr);
            if (jsonObject.has(KEY_STATE)) {
                int state = jsonObject.optInt(KEY_STATE);
                String option = jsonObject.optString(KEY_OPTION);
                Map<Object, Object> optionMap = new HashMap<>();
                optionMap.put(KEY_OPTION, option);
                Log.d(TAG, "state " + state + " option " + option);
                switch (state) {
                    case 0://成功
                    case 8://用户的注册的音频条数不够
                    case 10://升级模型结束
                        if (mListener != null) {
                            mListener.onResults(aiResult);
                        }
                        break;
                    case 1://未知错误
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_DEFAULT, AIError.ERR_DESCRIPTION_DEFAULT).setEventMap(optionMap)));
                        break;
                    case 2://用户尚未注册
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_NO_SPKEAKER, AIError.ERR_DESCRIPTION_NO_SPEAKER).setEventMap(optionMap)));
                        break;
                    case 3://用户未注册过该文本
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_NO_REGISTERED_WORD, AIError.ERR_DESCRIPTION_NO_REGISTERED_WORD).setEventMap(optionMap)));
                        break;
                    case 4://用户已经注册过该文本
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_SPK_REGISTERED_WORD, AIError.ERR_DESCRIPTION_SPK_REGISTERED_WORD).setEventMap(optionMap)));
                        break;
                    case 5://注册用户数量已满
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_REGISTER_SPK_FULL, AIError.ERR_DESCRIPTION_REGISTER_SPK_FULL).setEventMap(optionMap)));
                        break;
                    case 6://不支持该性别
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_UNSUPPORT_GENDER, AIError.ERR_DESCRIPTION_UNSUPPORT_GENDER).setEventMap(optionMap)));
                        break;
                    case 7://不支持该词语
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_UNSUPPORT_WORD, AIError.ERR_DESCRIPTION_UNSUPPORT_WORD).setEventMap(optionMap)));
                        break;
                    case 11://语速过快
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_SPEECH_SPEED_FAST, AIError.ERR_DESCRIPTION_SPEECH_SPEED_FAST).setEventMap(optionMap)));
                        break;
                    case 12://语速过慢
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_SPEECH_SPEED_SLOW, AIError.ERR_DESCRIPTION_SPEECH_SPEED_SLOW).setEventMap(optionMap)));
                        break;
                    case 13://信噪比过低
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_SNR_LOW, AIError.ERR_DESCRIPTION_SNR_LOW).setEventMap(optionMap)));
                        break;
                    case 14://音频截幅
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_SPEECH_CLIPPING, AIError.ERR_DESCRIPTION_SPEECH_CLIPPING).setEventMap(optionMap)));
                        break;
                    default:
                        break;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void unPackResult(String vprintRetString) {
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

    public void feedTLV(byte[] data) {
        sendMessage(new Message(MSG_FEED_DATA_TLV, data));
    }

    private void parseTLV(byte[] data) {
        if (tlvPcmOutputStream == null)
            return;
        if (data.length <= 12)
            return;
        byte[] magic = new byte[4];
        System.arraycopy(data, 0, magic, 0, 4);
        byte[] msgType = new byte[2];
        System.arraycopy(data, 8, msgType, 0, 2);
        byte[] msgLen = new byte[2];
        System.arraycopy(data, 10, msgLen, 0, 2);
        int magicInt = byteArrayToInt(magic);
        int msgTypeInt = byteArrayToInt(msgType);
        int msgLenInt = byteArrayToInt(msgLen);

        if (1095324496 != magicInt || 1 != msgTypeInt
                || msgLenInt <= 0) {
            Log.w(TAG, "TLV magic " + magicInt + " msgType " + msgTypeInt + " msgLen " + msgLenInt);
            return;
        }

        int elementStartPos = 12;
        int allLen = msgLenInt + elementStartPos;
        if (allLen != data.length && allLen > data.length) {
            // 长度做个校验，防止越界崩溃
            allLen = data.length;
        }
        while (true) {
            // 要复制4 byte数据
            if (elementStartPos >= allLen - 4) {
                break;
            }
            byte[] elementType = new byte[2];
            System.arraycopy(data, elementStartPos, elementType, 0, 2);
            elementStartPos += 2;
            byte[] elementLen = new byte[2];
            System.arraycopy(data, elementStartPos, elementLen, 0, 2);
            int elementTypeInt = byteArrayToInt(elementType);
            int elementLenInt = byteArrayToInt(elementLen);
            elementStartPos += 2;
            // type 为1是pcm音频
            if (elementTypeInt == 1 && AISpeech.isLocalSaveEnabled()) {
                savePcmData(data, elementStartPos, elementLenInt);
            }
            elementStartPos += elementLenInt;
        }
    }

    private void createVprintCutOutputStream(String dirPath, String action) {
        releaseFileOutputStream();
        if (TextUtils.isEmpty(dirPath))
            return;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dirPath,
                "vprintcut_" + System.currentTimeMillis() + "_" + action + ".pcm");
        Log.d(TAG, "vprint cut path " + file.getAbsolutePath());
        try {
            tlvPcmOutputStream = new FileOutputStream(file);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void savePcmData(byte[] data, int pcmStartPos, int size) {
        if (tlvPcmOutputStream == null || data == null || pcmStartPos < 0 || size <= 0)
            return;
        try {
            tlvPcmOutputStream.write(data, pcmStartPos, size);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void releaseFileOutputStream() {
        if (tlvPcmOutputStream != null) {
            Log.d(TAG, "vprint cut releaseFileOutputStream");
            try {
                tlvPcmOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                tlvPcmOutputStream = null;
            }
        }
    }

}
