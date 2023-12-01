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
import com.aispeech.lite.net.WebsocketClient;
import com.aispeech.lite.param.CloudASRParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by yuruilong on 2017/5/19.
 */

public class CloudAsrKernel extends BaseKernel implements WSClientListener {
    public static final String TAG = "CloudAsrKernel";
    private final List<byte[]> waitingToSendAudio = new ArrayList<>();
    private final List<byte[]> waitingToSendAudio2 = new ArrayList<>();
    private AsrKernelListener mListener;
    private CloudASRParams mParams;
    private volatile boolean isStarted = false;
    private WebsocketClient mWebsocketClient;
    private PcmToOgg mPcmToOgg;
    private String mRecordId;
    private String mUrl;
    // asr 从start到stop里经过vad后上传服务器的音频，并且可能经过转码
    private FileUtil mFileUtil = new FileUtil();
    // asr 从start到stop所有的音频
    private FileUtil allPcmAudioFile = new FileUtil();
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
    private volatile boolean lasrReady = false;

    public CloudAsrKernel(AsrKernelListener listener) {
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
                    mWebsocketClient = new WebsocketClient();
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
                        if (mParams.isSaveOriginalAudio()) {
                            allPcmAudioFile.createFile(saveAudioPath + "/cloud_asr_" + mRecordId + "_all.wav");
                            allPcmAudioFile.writeWaveFileHeader();
                        }

                        String audioFilePath;
                        if (mParams.getAudioType() == CloudASRParams.WAV) {
                            audioFilePath = saveAudioPath + "/cloud_asr_" + mRecordId + ".wav";
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
                            audioFilePath = saveAudioPath + "/cloud_asr_" + mRecordId + suffix;
                            mFileUtil.createFile(audioFilePath);
                        }
                        Log.d(TAG, "create cloud asr audio file at: " + audioFilePath);
                    }
                    mWebsocketClient.startRequest(url, CloudAsrKernel.this);
                    mParams.setRequestId(mRecordId);
//                    String text = mParams.toJSON().toString();
                    String text = getText(mParams);
                    mInput = text;
                    if (mWebsocketClient != null) {
                        if (mParams.isLasr())
                            mWebsocketClient.setLasrMessage(text);
                        else
                            mWebsocketClient.sendText(text);
                    }
                    lasrReady = false;
                    waitingToSendAudio.clear();
                    waitingToSendAudio2.clear();
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
                        if (mWebsocketClient != null) {
                            byte[] endData = new byte[0];
                            mWebsocketClient.sendBinary(endData);
                            Log.d(TAG, "ASR.END");
                            mStopTime = System.currentTimeMillis();
                            Log.d(TAG, "mStopTime is " + mStopTime);
                        }
                        if (mFileUtil != null) {
                            mFileUtil.closeFile();
                            if (mParams.getAudioType() == CloudASRParams.WAV)
                                mFileUtil.modifyWaveFileHeaderFileLength();
                        }
                        allPcmAudioFile.closeFile();
                        allPcmAudioFile.modifyWaveFileHeaderFileLength();

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
                        allPcmAudioFile.closeFile();
                        allPcmAudioFile.modifyWaveFileHeaderFileLength();

                        isStarted = false;
                    }
                    if (mWebsocketClient != null)
                        mWebsocketClient.closeWebSocket();
                    break;
                case Message.MSG_RELEASE:
                    // 销毁引擎
                    Log.d(TAG, "MSG_RELEASE");
                    if (mWebsocketClient != null) {
                        mWebsocketClient.destroy();
                    }
                    if (mPcmToOgg != null)
                        mPcmToOgg.destroyEncode();
                    isReleased = true;
                    Log.d(TAG, "MSG_RELEASE END");
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
//                    if(mWebsocketClient != null && isStarted) {
//                        mWebsocketClient.sendBinary(data);
//                        mFileUtil.write(data);
//                    }
                    allPcmAudioFile.write(data);
                    if (mPcmToOgg != null && mParams != null) {
                        if (!mParams.isLasr())
                            mPcmToOgg.feedData(data, data.length);
                        else if (lasrReady) {
                            if (!waitingToSendAudio.isEmpty()) {
                                for (byte[] bb : waitingToSendAudio) {
                                    mPcmToOgg.feedData(bb, bb.length);
                                }
                                waitingToSendAudio.clear();
                            }
                            mPcmToOgg.feedData(data, data.length);
                        } else {
                            waitingToSendAudio.add(data);
                        }
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
        if (params.isUseDmAsr()) {
            url.append(params.getServer())
                    .append("?serviceType=websocket")
                    .append("&productId=")
                    .append(params.getProductId())
                    .append("&deviceName=")
                    .append(params.getDeviceName())
                    .append("&timestamp=")
                    .append(timestamp)
                    .append("&nonce=")
                    .append(nonce)
                    .append("&sig=")
                    .append(sig);
            Log.d(TAG,"DmWssCustomParams = " + params.getDmWssCustomParams());
            if(!TextUtils.isEmpty(params.getDmWssCustomParams())){
                url.append(params.getDmWssCustomParams());
            }
        } else if (!params.isLasr()) {
            url.append(params.getServer())
                    .append("?productId=")
                    .append(params.getProductId())
                    .append("&language=").append(params.getLanguage())
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
            if (!TextUtils.isEmpty(params.getLanguage()) && (params.getExtraParam() == null || !params.getExtraParam().containsKey("lang"))) {
                // 这个判断做兼容
                url.append("&lang=").append(params.getLanguage());
            }
            if (params.getExtraParam() != null && !params.getExtraParam().isEmpty()) {
                Iterator<String> iter = params.getExtraParam().keySet().iterator();
                while (iter.hasNext()) {
                    String key = iter.next();
                    // lang 是可以加到 url 里的 key
                    if ("lang".equals(key)) {
                        String value = String.valueOf(params.getExtraParam().get(key));
                        if (!TextUtils.isEmpty(value))
                            url.append("&").append(key).append("=").append(value);
                    }
                }
            }
        }

        return url.toString();
    }

    private String getText(CloudASRParams cloudASRParams) {
        if (cloudASRParams.isUseDmAsr()) {
            return cloudASRParams.getDmAsrJson().toString();
        } else if (!cloudASRParams.isLasr()) {
            return cloudASRParams.toJSON().toString();
        } else
            return cloudASRParams.getLasrRealtimeParam();
    }

    @Override
    public void onMessage(String text) {
        if (mListener != null) {
            Log.d(TAG, "ASR.RESULT: " + text);
            if (firstResultFlag.compareAndSet(false, true)) {
                Log.d(TAG, "receive first result after start " + text);
                mAsrFirstResultTime = System.currentTimeMillis();
                Log.d(TAG, "mAsrFirstResultTime is : " + mAsrFirstResultTime);
            }
            AIResult results = AIResult.bundleResults(AIConstant.AIENGINE_MESSAGE_TYPE_JSON, mRecordId, text);
            if (mParams == null || !mParams.isLasr()) {
                JSONResultParser parser = new JSONResultParser(results.getResultObject().toString());
                results.setLast((parser.getEof() == 1));
                if (lastJsonParser != null) {
                    parser.setAllText(lastJsonParser.getAllText() + parser.getText());
                } else {
                    parser.setAllText(parser.getText());
                }
                if (parser.getEof() == 1) {
                    if (lastJsonParser != null) {
                        parser.setRecPinyinWhenLast(lastJsonParser.getText(), lastJsonParser.getPinyin());
                        lastJsonParser = null;
                        results.setResultObject(parser.toString());
                    }
                    mAsrLastResultTime = System.currentTimeMillis();
                    Log.d(TAG, "mAsrLastResultTime is " + mAsrLastResultTime);
                    long totalCost = mAsrLastResultTime - mStopTime; //最终识别结果时间点 — 结束说话的时间点
                    Log.d(TAG, "ASR.RESULT.DELAY: " + totalCost + "ms");
                    uploadCost();
                } else {
                    lastJsonParser = parser;
                    results.setResultObject(parser.toString());
                }
            } else {
                if (isStarted) {
                    if (!lasrReady) {
                        try {
                            JSONObject jsonObject = new JSONObject(text);
                            int errno = jsonObject.getInt("errno");
                            // {"errno":7,"error":"","data":{"current state":"feeding"}}
                            lasrReady = errno == 7;
                            Log.d(TAG, "lasrReady " + lasrReady);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    results.setLast(false);
                } else {
                    try {
                        JSONObject jsonObject = new JSONObject(text);
                        int errno = jsonObject.getInt("errno");
                        // errno = 9，表示为客户端发完空帧后的最后一个rec，客户端可以断开链接
                        boolean serverStop = errno == 9;
                        results.setLast(serverStop);
                        if (serverStop)
                            this.cancelKernel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
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
        } else if (AIError.ERR_DESCRIPTION_CONNECT_TIMEOUT.equals(text)) {
            aiError = new AIError(AIError.ERR_CONNECT_TIMEOUT, AIError.ERR_DESCRIPTION_CONNECT_TIMEOUT);
        } else {
            aiError = new AIError(AIError.ERR_NETWORK, AIError.ERR_DESCRIPTION_ERR_NETWORK, mRecordId);
        }
        try {
            JSONObject inputJson = new JSONObject(mInput);
            inputJson.put("url", mUrl);
            aiError.setInputJson(inputJson);
        } catch (Exception e) {
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

    private class OggCallbackImpl implements PcmToOgg.Callback {


        @Override
        public int run(byte[] data) {
            if (data != null && mWebsocketClient != null && isStarted) {
                if (firstFeedFlag.compareAndSet(false, true) && (data.length != 0)) {
                    Log.d(TAG, "ASR.FIRST.FEED");
                    mFirstFeedTime = System.currentTimeMillis();
                    Log.d(TAG, "mFirstFeedTime is " + mFirstFeedTime);
                }
                if (data.length > 0) {
                    // Log.d(TAG, "OggCallbackImpl length " + data.length + " lasrReady:" + lasrReady+" mParams.isLasr()"+mParams.isLasr());
                    if (!mParams.isLasr()) {
                        mWebsocketClient.sendBinary(data);
                        if (AISpeech.isLocalSaveEnabled()) {
                            mFileUtil.write(data);
                        }
                    } else if (lasrReady) {
                        if (!waitingToSendAudio2.isEmpty()) {
                            for (byte[] bb : waitingToSendAudio2) {
                                mWebsocketClient.sendBinary(bb);
                                if (AISpeech.isLocalSaveEnabled()) {
                                    mFileUtil.write(bb);
                                }
                            }
                            waitingToSendAudio2.clear();
                        }
                        mWebsocketClient.sendBinary(data);
                        if (AISpeech.isLocalSaveEnabled()) {
                            mFileUtil.write(data);
                        }
                    } else {
                        waitingToSendAudio2.add(data);
                    }
                }
            } else {
                Log.e(TAG, " ERROR ERROR ERROR ERROR ");
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }
}
