package com.aispeech.lite.asr;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AuthUtil;
import com.aispeech.common.FileUtil;
import com.aispeech.common.JSONResultParser;
import com.aispeech.common.Log;
import com.aispeech.common.PcmToOgg;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.net.WSClientListener;
import com.aispeech.lite.param.CloudASRParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


public class CloudAsrPlusKernel extends BaseKernel implements WSClientListener {
    public static String TAG = "CloudAsrPlusKernel";
    private AsrKernelListener mListener;
    private CloudASRParams mParams;
    private volatile boolean isStarted = false;
    private AsrPlusWebSocket asrPlusWebSocket;
    private PcmToOgg mPcmToOgg;
    private String mRecordId;
    private String mUrl;
    FileUtil mFileUtil = new FileUtil(AISpeech.getContext());
    //判断是否feed了第一帧音频数据
    private AtomicBoolean firstFeedFlag = new AtomicBoolean(true);
    private AtomicBoolean firstResultFlag = new AtomicBoolean(false);
    private String mInput;

    private long mFirstFeedTime;
    private long mAsrFirstResultTime;
    private long mStopTime;
    private long mAsrLastResultTime;

    /**
     * 上一次保存的结果
     */
    private JSONResultParser lastJsonParser;
    // errno == 7
    private boolean wsReady = false;

    public CloudAsrPlusKernel(AsrKernelListener listener) {
        super(TAG, listener);
        this.mListener = listener;
    }

    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
//            Log.d(TAG, "get message : " + message.mId);
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    asrPlusWebSocket = new AsrPlusWebSocket();
                    mPcmToOgg = new PcmToOgg();
                    mListener.onInit(AIConstant.OPT_SUCCESS);
                    break;
                case Message.MSG_START:
                    mParams = (CloudASRParams) message.mObject;
                    mParams.setProductId(this.profile.getProductId());
                    mParams.setDeviceName(this.profile.getDeviceName());
                    mParams.setDeviceId(this.profile.getDeviceId());
                    String url = getUrl(mParams);
                    mUrl = url;
                    Log.d(TAG, "url: " + url);
                    mRecordId = Utils.getRecorderId();
                    String saveAudioPath = mParams.getSaveAudioPath();
                    if (!TextUtils.isEmpty(saveAudioPath)) {
                        String audioFilePath;
                        if (mParams.getAudioType() == CloudASRParams.WAV) {
                            audioFilePath = saveAudioPath + "/" + mRecordId + ".wav";
                            mFileUtil.createFile(audioFilePath);
                            mFileUtil.writeWaveFileHeader();
                        } else {
                            String suffix;
                            switch (mParams.getAudioType()) {
                                case CloudASRParams.OGG_OPUS:
                                    suffix = "_ogg_opus.ogg";
                                    break;
                                case CloudASRParams.MP3:
                                    suffix = ".mp3";
                                    break;
                                case CloudASRParams.OPUS:
                                    suffix = "_opus.ogg";
                                    break;
                                default:
                                    suffix = ".ogg";
                                    break;
                            }
                            audioFilePath = saveAudioPath + "/" + mRecordId + suffix;
                            mFileUtil.createFile(audioFilePath);
                        }
                        Log.d(TAG, "create cloud asr audio file at: " + audioFilePath);
                    }
                    asrPlusWebSocket.startRequest(url, CloudAsrPlusKernel.this);
                    mParams.setRequestId(mRecordId);
//                    String text = mParams.toJSON().toString();
                    String text = getText(mParams);
                    mInput = text;
                    if (asrPlusWebSocket != null) {
                        asrPlusWebSocket.setLasrMessage(text);
                    }
                    wsReady = false;
                    dataWaiteToSendList.clear();
                    isStarted = true;
                    if (mPcmToOgg != null) {
                        mPcmToOgg.initEncode(mParams.getAudioType(), mParams.isEncodedAudio(), new OggCallbackImpl());
                        mPcmToOgg.startEncode();
                    }
                    Log.d(TAG, "ASR.BEGIN");
                    //置位
                    firstFeedFlag.compareAndSet(true, false);
                    firstResultFlag.set(false);
                    break;
                case Message.MSG_STOP:
                    if (isStarted) {
                        if (mPcmToOgg != null)
                            mPcmToOgg.stopEncode();
                        if (asrPlusWebSocket != null) {
                            byte[] endData = new byte[0];
                            sendBinary(endData);
                            Log.d(TAG, "ASR.END");
                            mStopTime = System.currentTimeMillis();
                            Log.d(TAG, "mStopTime is " + mStopTime);
                        }
                        if (mFileUtil != null) {
                            mFileUtil.closeFile();
                            if (mParams.getAudioType() == CloudASRParams.WAV)
                                mFileUtil.modifyWaveFileHeaderFileLength();
                        }
                        isStarted = false;
                    }
                    break;
                case Message.MSG_CANCEL:
                    if (isStarted) {
                        if (mPcmToOgg != null)
                            mPcmToOgg.stopEncode();
                        if (mFileUtil != null) {
                            mFileUtil.closeFile();
                            if (mParams.getAudioType() == CloudASRParams.WAV)
                                mFileUtil.modifyWaveFileHeaderFileLength();
                        }
                        isStarted = false;
                    }
                    if (asrPlusWebSocket != null)
                        asrPlusWebSocket.closeWebSocket();
                    break;
                case Message.MSG_RELEASE:
                    // 销毁引擎
                    Log.d(TAG, "MSG_RELEASE");
                    if (asrPlusWebSocket != null) {
                        asrPlusWebSocket.destroy();
                    }
                    if (mPcmToOgg != null)
                        mPcmToOgg.destroyEncode();
                    isReleased = true;
                    Log.d(TAG, "MSG_RELEASE END");
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    if (mPcmToOgg != null && mParams != null) {
                        mPcmToOgg.feedData(data, data.length);
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

    private String getUrl(CloudASRParams params) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String sig = AuthUtil.getSignature(params.getDeviceName() + nonce + params.getProductId() + timestamp,
                this.profile.getDeviceSecret());
        StringBuilder url = new StringBuilder();
        if (!params.isLasr()) {
            url.append(params.getServer())
                    .append("?productId=")
                    .append(params.getProductId())
                    // asrPlus 比普通 asr 多带一个 apikey 参数
                    .append("&apikey=").append(profile.getApiKey())
                    .append("&res=")
                    .append(params.getRes())
                    .append("&deviceName=")
                    .append(params.getDeviceName())
                    .append("&timestamp=")
                    .append(timestamp)
                    .append("&nonce=")
                    .append(nonce)
                    .append("&sig=")
                    .append(sig);
        } else {
            url.append(params.getServer())
                    .append("?productId=")
                    .append(params.getProductId())
                    .append("&deviceName=")
                    .append(params.getDeviceName())
                    .append("&timestamp=")
                    .append(timestamp)
                    .append("&nonce=")
                    .append(nonce)
                    .append("&sig=")
                    .append(sig)
                    .append("&device_id=")
                    .append(params.getDeviceId());
            if (!TextUtils.isEmpty(params.getLasrRes())) {
                url.append("&res=").append(params.getLasrRes());
            }
            if (!TextUtils.isEmpty(params.getLasrForwardAddresses())) {
                url.append("&forward_addresses=").append(params.getLasrForwardAddresses());
            }
        }

        return url.toString();
    }

    private String getText(CloudASRParams cloudASRParams) {
        if (!cloudASRParams.isLasr()) {
            cloudASRParams.setProductId(this.profile.getProductId());
            cloudASRParams.setDeviceName(this.profile.getDeviceName());
            return cloudASRParams.toJSON().toString();
        } else
            return cloudASRParams.getLasrRealtimeParam();
    }

    @Override
    public void onMessage(String text) {
        if (mListener != null) {
            Log.d(TAG, "ASR.RESULT: " + text);
            try {
                JSONObject jsonObject = new JSONObject(text);
                int errno = jsonObject.optInt("errno", 0);
                if (errno == 0 || errno == 2 || errno == 7 || errno == 8 || errno == 9) {
                    // 正常流程
                } else if (errno == 99) {
                    Log.d(TAG, "ignore errno 99");
                    return;
                } else {
                    mListener.onError(new AIError(AIError.ERR_ASR_PLUS_SERVER_ERR, text));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (firstResultFlag.compareAndSet(false, true)) {
                Log.d(TAG, "receive first result after start " + text);
                mAsrFirstResultTime = System.currentTimeMillis();
                Log.d(TAG, "mAsrFirstResultTime is " + mAsrFirstResultTime);
            }
            AIResult results = AIResult.bundleResults(AIConstant.AIENGINE_MESSAGE_TYPE_JSON, mRecordId, text);
            if (mParams == null || !mParams.isLasr()) {
                if (!wsReady) {
                    try {
                        JSONObject jsonObject = new JSONObject(text);
                        int errno = jsonObject.optInt("errno", -1);
                        // {"errno":7,"error":"","data":{"current state":"feeding"}}
                        wsReady = errno == 7;
                        Log.d(TAG, "wsReady " + wsReady);
                        sendDataWhenWsReady();
                        if (wsReady)
                            return; // 普通asr直接返回
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // eof 1 识别结果，eof 2 识别结果+声纹信息; 先出1，再出2
                JSONResultParser parser = new JSONResultParser(results.getResultObject().toString());
                results.setLast((parser.getEof() == 2));
                if (!(parser.haveVprintInfo() && parser.getEof() == 0)) {
                    if (lastJsonParser != null) {
                        parser.setAllText(lastJsonParser.getAllText() + parser.getText());
                    } else {
                        parser.setAllText(parser.getText());
                    }
                }
                if (parser.getEof() == 1 || parser.getEof() == 2) {
                    if (lastJsonParser != null) {
                        parser.setRecPinyinWhenLast(lastJsonParser.getText(), lastJsonParser.getPinyin());
                        if (parser.getEof() == 2)
                            lastJsonParser = null;
                        results.setResultObject(parser.toString());
                    }
                    mAsrLastResultTime = System.currentTimeMillis();
                    Log.d(TAG, "mAsrLastResultTime is " + mAsrLastResultTime);
                    long totalCost = mAsrLastResultTime - mStopTime; //最终识别结果时间点 — 结束说话的时间点
                    Log.d(TAG, "ASR.RESULT.DELAY: " + totalCost + "ms");
                    uploadCost();
                } else {
                    if (!parser.haveVprintInfo()) {
                        lastJsonParser = parser;
                    }
                    results.setResultObject(parser.toString());
                }
            } else {
                if (!wsReady) {
                    try {
                        JSONObject jsonObject = new JSONObject(text);
                        int errno = jsonObject.optInt("errno");
                        // {"errno":7,"error":"","data":{"current state":"feeding"}}
                        wsReady = errno == 7;
                        Log.d(TAG, "wsReady " + wsReady);
                        sendDataWhenWsReady();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                results.setLast(false);
            }
            mListener.onResults(results);
        }
    }

    @Override
    public void onError(String text) {
        Log.d(TAG, "onError : " + text);
        AIError aiError;
        if (text != null && text.contains("dns")) {
            aiError = new AIError(AIError.ERR_DNS, AIError.ERR_DESCRIPTION_ERR_DNS, mRecordId);
        } else if (text != null && text.contains("401 Unauthorized")) {
            // Expected HTTP 101 response but was '401 Unauthorized'
            aiError = new AIError(AIError.ERR_DEVICE_ID_CONFLICT_ASR, AIError.ERR_DESCRIPTION_DEVICE_ID_CONFLICT, mRecordId);
        } else if (text != null && text.contains("403 Forbidden")) {
            aiError = new AIError(AIError.ERR_403_FORBIDDEN, AIError.ERR_DESCRIPTION_403_FORBIDDEN, mRecordId);
        } else if (text != null && text.startsWith("Closed:1000")) {
            // 服务器主动断开
            // aiError = new AIError(AIError.ERR_SERVER_CLOSE_WEBSOCKET_1000, AIError.ERR_DESCRIPTION_SERVER_CLOSE_WEBSOCKET_1000, mRecordId);
            // 忽略
            return;
        } else {
            aiError = new AIError(AIError.ERR_NETWORK, AIError.ERR_DESCRIPTION_ERR_NETWORK, mRecordId);
        }
        try {
            JSONObject inputJson = new JSONObject(mInput);
            inputJson.put("url", mUrl);
            aiError.setInputJson(inputJson);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sendMessage(new Message(Message.MSG_ERROR, aiError));
    }

    @Override
    public void onOpen() {

    }

    @Override
    public void onClose() {

    }

    private final List<byte[]> dataWaiteToSendList = new ArrayList<>();

    private void sendDataWhenWsReady() {
        if (wsReady) {
            synchronized (dataWaiteToSendList) {
                byte[] endData = null;
                if (!dataWaiteToSendList.isEmpty()) {
                    for (byte[] d : dataWaiteToSendList) {
                        if (d.length == 0)
                            endData = d;
                        else
                            asrPlusWebSocket.sendBinary(d);
                    }
                    dataWaiteToSendList.clear();
                }
                if (endData != null) {
                    asrPlusWebSocket.sendBinary(endData);
                    Log.d(TAG, "ASR.END real");
                }
            }
        }
    }

    private class OggCallbackImpl implements PcmToOgg.Callback {

        @Override
        public synchronized int run(byte[] data) {
            if (data != null && asrPlusWebSocket != null && isStarted) {
                if (firstFeedFlag.compareAndSet(false, true) && (data.length != 0)) {
                    Log.d(TAG, "ASR.FIRST.FEED");
                    mFirstFeedTime = System.currentTimeMillis();
                    Log.d(TAG, "mFirstFeedTime is " + mFirstFeedTime);
                }
                if (data.length > 0) {
                    Log.d(TAG, "OggCallbackImpl data.length " + data.length);
                    sendBinary(data);
                    mFileUtil.write(data);
                }
            } else {
                Log.e(TAG, " ERROR ERROR ERROR ERROR ");
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }

    private void sendBinary(byte[] data) {
        synchronized (dataWaiteToSendList) {
            if (wsReady) {
                byte[] endData = null;
                if (!dataWaiteToSendList.isEmpty()) {
                    for (byte[] d : dataWaiteToSendList) {
                        if (d.length == 0)
                            endData = d;
                        else
                            asrPlusWebSocket.sendBinary(d);
                    }
                    dataWaiteToSendList.clear();
                }
                asrPlusWebSocket.sendBinary(data);
                if (endData != null) {
                    asrPlusWebSocket.sendBinary(endData);
                    Log.d(TAG, "ASR.END real");
                }
            } else {
                dataWaiteToSendList.add(data);
            }
        }
    }

    private void uploadCost() {
        //添加message外面的字段
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("recordId", mRecordId);
        entryMap.put("mode", "lite");
        entryMap.put("module", "cloud_cost");
        JSONObject inputJson = null;
        try {
            inputJson = new JSONObject(mInput);
            inputJson.put("url", mUrl);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject outputJson = new JSONObject();
        try {
            if (mParams.isEnableRealTimeFeedBack()) {
                outputJson.put("asrfirstcost", mAsrFirstResultTime - mFirstFeedTime);
            }
            outputJson.put("asrtotalcost", mAsrLastResultTime - mStopTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("cloud_asr_cost", "info", "cloud_cost",
                mRecordId, inputJson, outputJson, entryMap);
    }

    public String getRecordId() {
        return mRecordId;
    }
}
