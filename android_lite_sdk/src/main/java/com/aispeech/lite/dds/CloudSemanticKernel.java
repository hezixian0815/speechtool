package com.aispeech.lite.dds;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AuthUtil;
import com.aispeech.common.DDSJSONResultParser;
import com.aispeech.common.FileUtil;
import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;
import com.aispeech.common.PcmToOgg;
import com.aispeech.export.itn.Convert;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.AIType;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.asr.CloudAsrKernelListener;
import com.aispeech.lite.config.CloudDMConfig;
import com.aispeech.lite.dm.Timer;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.net.WSClientListener;
import com.aispeech.lite.net.WebsocketClient;
import com.aispeech.lite.param.CloudASRParams;
import com.aispeech.lite.param.CloudSemanticParams;
import com.aispeech.lite.param.TriggerIntentParams;
import com.aispeech.net.NetProxy;
import com.aispeech.net.http.HttpCallback;
import com.aispeech.net.http.IHttp;
import com.aispeech.net.http.IResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * 云端客户端中直链DUI server
 *
 * @author hehr
 */
public class CloudSemanticKernel extends BaseKernel implements WSClientListener {

    private static final String TAG = "CloudSemanticKernel";

    private CloudAsrKernelListener mListener;
    private WebsocketClient mWebsocketClient;
    private WebsocketClient mWebsocketClientForCInfo;
    private CloudDMConfig mConfig;
    private CloudSemanticParams mParams;
    private PcmToOgg mPcmToOgg;

    //判断是否feed了第一帧音频数据
    private final AtomicBoolean firstFeedFlag = new AtomicBoolean(true);
    private final AtomicBoolean firstResultFlag = new AtomicBoolean(false);

    private volatile boolean isStarted = false;

    FileUtil mFileUtil = new FileUtil(AISpeech.getContext());

    private long mFirstFeedTime;
    private long mAsrFirstResultTime;
    private long mStopTime;
    private long mAsrLastResultTime;

    private String mRecordId;

    private String mUrl;

    private String mInput;

    public CloudSemanticKernel(CloudAsrKernelListener listener) {
        super(TAG,listener);
        mListener = listener;
    }


    /**
     * 生成dds服务url
     *
     * @param config
     * @return
     */
    private String getV2Url(CloudDMConfig config) {

        String timestamp = new Date().getTime() + "";
        String nonce = UUID.randomUUID() + "";
        String sig = AuthUtil.getSignature(config.getDeviceName() + nonce + config.getProductId() + timestamp, profile.getDeviceSecret());

        StringBuilder url = new StringBuilder();
        url.append(config.getServerAddress());
        url.append(config.getAliasKey());
        url.append("?serviceType=");
        url.append(config.getServiceType());
        url.append("&productId=");
        url.append(config.getProductId());
        url.append("&aliasKey=");
        url.append(config.getAliasKey());
        url.append("&deviceName=");
        url.append(config.getDeviceName());
        url.append("&nonce=");
        url.append(nonce);
        url.append("&sig=");
        url.append(sig);
        url.append("&timestamp=");
        url.append(timestamp);
        if (!TextUtils.isEmpty(mParams.getUserId())) {
            url.append("&userId=");
            url.append(mParams.getUserId());
        }
        return url.toString();
    }

    /* 生成v1 url */
    private String getV1Url(String ctype, String vocabName, CloudDMConfig config) {
        String timestamp = new Date().getTime() + "";
        String nonce = UUID.randomUUID() + "";
        String sig = AuthUtil.getSignature(config.getDeviceName() + nonce + config.getProductId() + timestamp, profile.getDeviceSecret());
        StringBuilder url = new StringBuilder();
        url.append(CloudDMConfig.CINFO_SERVER);
        url.append("/").append(ctype).append("/").append(vocabName).append("?");
        url.append("productId=");
        url.append(config.getProductId());
        url.append("&aliasKey=");
        url.append(config.getAliasKey());
        url.append("&deviceName=");
        url.append(config.getDeviceName());
        url.append("&nonce=");
        url.append(nonce);
        url.append("&sig=");
        url.append(sig);
        url.append("&timestamp=");
        url.append(timestamp);
        if (!TextUtils.isEmpty(mParams.getUserId())) {
            url.append("&userId=");
            url.append(mParams.getUserId());
        }
        Log.i(TAG, "===url==" + url.toString());
        return url.toString();
    }

    private void updateVocabInfo(String data) {
        if (TextUtils.isEmpty(data)) {
            Log.w(TAG, "data == null ");
            return;
        }
        JSONObject jsonObject = JSONUtil.build(data);
        String ctype = (String) JSONUtil.getQuietly(jsonObject, "ctype");
        String vocabName = (String) JSONUtil.getQuietly(jsonObject, "vocabName");
        String sendData = (String) JSONUtil.getQuietly(jsonObject, "data");

        NetProxy.getHttp().post(getV1Url(ctype, vocabName, mConfig), sendData, new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.w(TAG, iHttp.toString());
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse response) throws IOException {
                Log.d(TAG, "http response code: " + response.code());
                if (response.code() == 200) {
                    String s = response.string();
                    Log.d(TAG, "== success : " + s);
                }
            }
        });
    }

    class SpeexCallbackImpl implements PcmToOgg.Callback {

        @Override
        public int run(byte[] data) {
//            Log.d(TAG, "解码之后的大小：" + size);
            int size = data.length;
            if (mWebsocketClient != null && isStarted) {
                if (firstFeedFlag.compareAndSet(false, true) && (size != 0)) {
                    Log.d(TAG, "ASR.FIRST.FEED");
                    mFirstFeedTime = System.currentTimeMillis();
                    Log.d(TAG, "mFirstFeedTime is " + mFirstFeedTime);
                }
                if (size != 0) {
                    byte[] oggBuffer = new byte[size];
                    System.arraycopy(data, 0, oggBuffer, 0, size);
                    mWebsocketClient.sendBinary(oggBuffer);
                    mFileUtil.write(oggBuffer);
                }

            } else {
                Log.e(TAG, " ERROR ERROR ERROR ERROR ");
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }

    }



    @Override
    public void run() {
        Message message;
        while ((message = waitMessage()) != null) {
//            Log.d(TAG, "get message : " + message.mId);
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    mConfig = (CloudDMConfig) message.mObject;
                    mWebsocketClient = new WebsocketClient(mConfig.getConnectTimeout());
                    mWebsocketClientForCInfo = new WebsocketClient(mConfig.getConnectTimeout());
                    mPcmToOgg = new PcmToOgg();
                    mPcmToOgg.initEncode(CloudASRParams.OGG, false, new SpeexCallbackImpl());
                    mListener.onInit(AIConstant.OPT_SUCCESS);
                    break;
                case Message.MSG_START:
                    mParams = (CloudSemanticParams) message.mObject;
                    mRecordId = mParams.getRecordId();

                    String saveAudioPath = mParams.getSaveAudioPath();
                    if (!TextUtils.isEmpty(saveAudioPath)) {
                        Log.d(TAG, "create local ogg file at: " + saveAudioPath + "/" + "cloud_semantic_" + mRecordId + ".ogg");
                        mFileUtil.createFile(saveAudioPath + "/" + "cloud_semantic_" + mRecordId + ".ogg");
                    }
                    mUrl = getV2Url(mConfig);
                    Log.d(TAG, "connect server url: " + mUrl);
                    if (mListener != null) {
                        mListener.onConnect(mWebsocketClient.isConnected());
                    }
                    mWebsocketClient.startRequest(mUrl, this);
                    mInput = mParams.toJSON().toString();
                    mWebsocketClient.sendText(mInput);
                    isStarted = true;

                    mPcmToOgg.startEncode();
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
                        if (mFileUtil != null)
                            mFileUtil.closeFile();
                        isStarted = false;
                    }
                    break;
                case Message.MSG_CANCEL:
                    mRecordId = "";
                    if (isStarted) {
                        if (mPcmToOgg != null)
                            mPcmToOgg.stopEncode();
                        if (mFileUtil != null)
                            mFileUtil.closeFile();
                        isStarted = false;
                    }
//                    if (mWebsocketClient != null)
//                        mWebsocketClient.closeWebSocket();
                    break;
                case Message.MSG_RELEASE:
                    // 销毁引擎
                    Log.d(TAG, "MSG_RELEASE");
                    disconnect();
                    socketTimer.clear();
                    if (mWebsocketClient != null)
                        mWebsocketClient.destroy();
                    if (mWebsocketClientForCInfo != null)
                        mWebsocketClientForCInfo.destroy();
                    if (mPcmToOgg != null)
                        mPcmToOgg.destroyEncode();
                    if (parser != null)
                        parser.destroy();
                    NetProxy.getHttp().cancel();
                    isReleased = true;
                    Log.d(TAG, "MSG_RELEASE END");
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    if (mPcmToOgg != null) {
                        mPcmToOgg.feedData(data, data.length);
                    }
                    break;
                case Message.MSG_ERROR:
                    mRecordId = "";
                    mListener.onError((AIError) message.mObject);
                    break;

                case Message.MSG_UPDATE:
                    String cinfoParams = (String) message.mObject;
                    Log.i(TAG, "update params : " + cinfoParams);
                    if (mWebsocketClientForCInfo != null && mConfig != null) {
                        mWebsocketClientForCInfo.startRequest(getV2Url(mConfig), mCInfoUpdateImpl);
                        mWebsocketClientForCInfo.sendText(cinfoParams);
                    }

                    break;
                case Message.MSG_UPDATE_VOCAB:
                    String cinfoVocabParams = (String) message.mObject;
                    Log.i(TAG, "update params : " + cinfoVocabParams);
                    updateVocabInfo(cinfoVocabParams);
                    break;
                case Message.MSG_EVENT:
                    String event = (String) message.mObject;
                    if (TextUtils.equals(EVENT_DISCONNECT, event)) {
                        disconnect();
                    }
                    break;
                case Message.MSG_TRIGGER_INTENT:
                    checkConnect();
                    TriggerIntentParams triggerIntentParams = (TriggerIntentParams) message.mObject;
                    mRecordId = triggerIntentParams.getIntent().getRecordId();
                    mParams = (CloudSemanticParams) triggerIntentParams.getParam();
                    mInput = mParams.toJSON().toString();
                    Log.d(TAG, "trigger : " + triggerIntentParams.toJSON());
                    mWebsocketClient.sendText(triggerIntentParams.toJSON().toString());
                    isStarted = true;
                    break;
                case Message.MSG_CLOSE:
                    disconnect();
                    if (mPcmToOgg != null)
                        mPcmToOgg.stopEncode();
                    if (mFileUtil != null)
                        mFileUtil.closeFile();
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

    /***
     * 检查当前的链接状态
     */
    private boolean checkConnect() {
        if (!mWebsocketClient.isConnected()) {
            mUrl = getV2Url(mConfig);
            Log.d(TAG, "connect server url: " + mUrl);
            mWebsocketClient.startRequest(mUrl, this);
            return false;
        }
        return true;
    }

    /**
     * @param remoteId
     * @param currentId
     * @return boolean 检查报文信息是否当前是当前请求的报文信息
     */
    private boolean checkRecorderId(String remoteId, String currentId) {
        if (TextUtils.isEmpty(currentId) || TextUtils.isEmpty(remoteId)) {
            return false;
        }
        return remoteId.contains(currentId);
    }


    /**
     * 60s之后断开链接
     */
    private Timer socketTimer = new Timer(59 * 1000);
    private static final String alias = "__webSocket";
    private static final String EVENT_DISCONNECT = "disconnect";
    private volatile boolean isConnect = false;


    /***
     * 创建链接 copy from @CloudDmKernel.java
     */
    private void connect() {
        if (!isConnect) {
            mUrl = getV2Url(mConfig);
            Log.d(TAG, "connect server url: " + mUrl);
            mWebsocketClient.startRequest(mUrl, this);
        }
        //每次链接创建之后，60s之后自己断开
        socketTimer.start(alias, new TimerTask() {
            @Override
            public void run() {
                sendDisConnectMsg();
            }
        });
        isConnect = true;
    }

    /**
     * 断开链接 copy from @CloudDmKernel.java
     */
    private void disconnect() {
        Log.d(TAG, "disconnect server");
        if (isConnect) {
            mWebsocketClient.closeWebSocket();
        } else {
            Log.d(TAG, "socket disconnected ,drop disconnect operation");
        }
        isConnect = false;
    }

    private void sendDisConnectMsg() {
        Message msg = new Message(Message.MSG_EVENT, EVENT_DISCONNECT);
        sendMessage(msg);
    }

    /**
     * 识别结果解析器
     */
    private DDSJSONResultParser parser;

    /**
     * @param text 服务端返回报文
     */
    @Override
    public void onMessage(String text) {
        if (mListener != null) {
            Log.d(TAG, "ASR.RESULT: " + text);
            if (firstResultFlag.compareAndSet(false, true)) {
                Log.d(TAG, "receive first result after start " + text);
                mAsrFirstResultTime = System.currentTimeMillis();
                Log.d(TAG, "mAsrFirstResultTime is " + mAsrFirstResultTime);
                parser = new DDSJSONResultParser();
            }
            DDSJSONResultParser.DDSResultParseBean bean = parser.parse(text);
            if (!checkRecorderId(bean.getRecordId(), mRecordId)) {
                Log.w(TAG, "invalid recordId,mRecordId=" + mRecordId + " drop this ASR.RESULT!!!");
                return;
            }
            JSONObject nluObj = JSONUtil.normalSemanticSlots(bean.getNlu());
            bean.setNlu(nluObj);
            Log.d(TAG, "throws:" + bean.getJso().toString());
            AIResult results = AIResult.bundleResults(AIConstant.AIENGINE_MESSAGE_TYPE_JSON, bean.getRecordId(), bean.getJso().toString());
            if (mParams.getAIType() == AIType.DM && bean.getEof() == 1) {
                results.setLast(true);
            }
            if (bean.getNlu() != null || bean.getDm() != null || bean.getError() != null) {
                results.setLast(true);
            }

            if (results.isLast()) {
                mAsrLastResultTime = System.currentTimeMillis();
                Log.d(TAG, "mAsrLastResultTime is " + mAsrLastResultTime);
                long totalCost = mAsrLastResultTime - mStopTime; //最终识别结果时间点 — 结束说话的时间点
                Log.d(TAG, "ASR.RESULT.DELAY: " + totalCost + "ms");

                if (mParams != null && mParams.isEnableVocabsConvert()) {
                    String ret = Convert.getInstance().restore(results.getResultObject().toString());
                    results.setResultObject(ret);
                }
            }
            mListener.onResults(results);
        }
    }

    private CInfoUpdateImpl mCInfoUpdateImpl = new CInfoUpdateImpl();

    /**
     * cinfo 服务监听器实现
     */
    private static class CInfoUpdateImpl implements WSClientListener {

        @Override
        public void onMessage(String text) {
            Log.d(TAG, "cinfo result" + text);
        }

        @Override
        public void onError(String text) {
            Log.e(TAG, "cinfo error" + text);
        }

        @Override
        public void onOpen() {

        }

        @Override
        public void onClose() {

        }
    }

    /**
     * @param text 错误报文
     */
    @Override
    public void onError(String text) {

        Log.d(TAG, "onError : " + (TextUtils.isEmpty(text) ? "is null" : text));
        AIError aiError = null;
        if (!TextUtils.isEmpty(text) && text.contains("dns")) {
            aiError = new AIError(AIError.ERR_DNS, AIError.ERR_DESCRIPTION_ERR_DNS, mRecordId);
        } else if (!TextUtils.isEmpty(text) && text.contains("401 Unauthorized")) {// https://jira.aispeech.com.cn/browse/XIAOPENGDUI-655
            aiError = new AIError(AIError.ERR_RETRY_INIT, AIError.ERR_RETRY_INIT_MSG, mRecordId);
        } else if (AIError.ERR_DESCRIPTION_CONNECT_TIMEOUT.equals(text)) {
            aiError = new AIError(AIError.ERR_CONNECT_TIMEOUT, AIError.ERR_DESCRIPTION_CONNECT_TIMEOUT);
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

        if (aiError != null) {
            //断开链接
            sendDisConnectMsg();
            mListener.onError(aiError);
        }
    }

    @Override
    public void onOpen() {
        if (mListener != null) {
            mListener.onConnect(true);
        }
    }

    @Override
    public void onClose() {
        if (mListener != null) {
            mListener.onConnect(false);
        }
    }
}
