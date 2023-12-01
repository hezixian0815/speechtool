package com.aispeech.lite.dm;

import static com.aispeech.lite.param.CloudSemanticParams.TOPIC_RECORDER_STREAM;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AuthUtil;
import com.aispeech.common.FileUtil;
import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;
import com.aispeech.common.PcmToOgg;
import com.aispeech.common.Util;
import com.aispeech.export.ProductContext;
import com.aispeech.export.Setting;
import com.aispeech.export.SkillContext;
import com.aispeech.export.Vocab;
import com.aispeech.export.itn.Convert;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.asr.CloudAsrKernelListener;
import com.aispeech.lite.config.CloudDMConfig;
import com.aispeech.lite.dm.update.ICInfo;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.net.WSClientListener;
import com.aispeech.lite.net.WebsocketClient;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.param.CloudASRParams;
import com.aispeech.lite.param.CloudSemanticParams;
import com.aispeech.lite.param.FeedbackJsonParams;
import com.aispeech.lite.param.FeedbackParams;
import com.aispeech.lite.param.MultiModalParams;
import com.aispeech.lite.param.TriggerIntentParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * dds 协议 https://wiki.aispeech.com.cn/pages/viewpage.action?pageId=1220387
 *
 * @author hehr
 */
public class CloudDmKernel extends BaseKernel implements WSClientListener, ICInfo {

    private static final String TAG = "CloudDmKernel";

    private final CloudAsrKernelListener mListener;
    private WebsocketClient mWebsocketClient;
    private CloudDMConfig mConfig;
    private CloudSemanticParams mParams;
    private PcmToOgg mPcmToOgg;

    //判断是否feed了第一帧音频数据
    private final AtomicBoolean firstFeedFlag = new AtomicBoolean(true);
    private final AtomicBoolean firstResultFlag = new AtomicBoolean(false);

    FileUtil mFileUtil = new FileUtil(AISpeech.getContext());

    private long mFirstFeedTime;
    private long mAsrFirstResultTime;
    private long mStopTime;

    private String mRecordId;
    // 单独存储 cinfo 更新配置的 recordId，方便回调更新成功结果
    private final CopyOnWriteArrayList<String> cInfoRecordIdList = new CopyOnWriteArrayList<>();
    // 暂存 upload 内容，需要等待 socket 建立连接
    private final LinkedBlockingQueue<String> pendingUploadQueue = new LinkedBlockingQueue<>();

    private String mInput;

    private String mUrl;

    private OneshotCache<byte[]> oneShotCache;
    private List<byte[]> audioCache;

    public CloudDmKernel(CloudAsrKernelListener listener) {
        super(TAG, listener);
        mListener = listener;
        audioCache = new ArrayList<>();
    }

    public void setOneShotCache(OneshotCache<byte[]> oneShotCache) {
        this.oneShotCache = oneShotCache;
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
        url.append("&deviceName=");
        url.append(config.getDeviceName());
        url.append("&nonce=");
        url.append(nonce);
        url.append("&sig=");
        url.append(sig);
        url.append("&timestamp=");
        url.append(timestamp);
        if (mParams != null && !TextUtils.isEmpty(mParams.getUserId())) {
            url.append("&userId=");
            url.append(mParams.getUserId());
        }
        if (config.isUseFullDuplex()) {
            url.append("&communicationType=" + "fullDuplex");
        }
        if (config.getKeys() != null && config.getValues() != null) {
            if (config.getKeys().length != config.getValues().length) {
                Log.i(TAG, "自定义请求参数不合法！");
            } else {
                for (int i = 0; i < config.getKeys().length; i++) {
                    String params = "&" + config.getKeys()[i] + "=" + config.getValues()[i];
                    url.append(params);
                }
            }
        }
        return url.toString();
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
     * 断开链接
     */
    private void disconnect(boolean isClear) {
        Log.d(TAG, "disconnect server:" + isClear);
        mWebsocketClient.closeWebSocket();
        /*
         * 避免 MSG_CLOSE 时未上传的 cInfo 被清理，即断开 socket 时不要清理，主要原因如下：
         *
         * （1）在缓存 cInfo 信息之前已经做了去重处理，不用担心 cInfo 堆积问题。
         * （2）上传 cInfo 是在对话启动的时候进行，上层应用在 start 对话引擎之前 先 stop 操作，那么 cInfo 信息被清则无法生效。
         */
        if (isClear) {
            pendingUploadQueue.clear();
        }
    }

    /**
     * websocket 协议上传 cinfo 数据
     *
     * @param text text 上传文本
     */
    private void upload(String text) {
        pendingUploadQueue.offer(text);
        send();
    }

    private void send() {
        if (mWebsocketClient != null && mWebsocketClient.isConnected()) {
            while (!pendingUploadQueue.isEmpty()) {
                String text = pendingUploadQueue.poll();
                if (TextUtils.isEmpty(text)) {
                    continue;
                }
                if (text.contains(TOPIC_RECORDER_STREAM)) {
                    startKernel(mParams);
                } else {
                    mWebsocketClient.sendText(text);
                }
            }
        } else {
            Log.w(TAG, "send text waiting connected...");
        }
    }


    /**
     * 标识一个ping类型数据包
     */
    static final int OPCODE_CONTROL_PING = 0x9;

    /**
     * 标识一个pong类型数据包
     */
    static final int OPCODE_CONTROL_PONG = 0xa;

    static final int B0_FLAG_FIN = 0b10000000;
    static final int B1_FLAG_MASK = 0b00000000;

    /**
     * Each frame starts with two bytes of data.
     * <p>
     * 0 1 2 3 4 5 6 7    0 1 2 3 4 5 6 7
     * +-+-+-+-+-------+  +-+-------------+
     * |F|R|R|R| OP    |  |M| LENGTH      |
     * |I|S|S|S| CODE  |  |A|             |
     * |N|V|V|V|       |  |S|             |
     * | |1|2|3|       |  |K|             |
     * +-+-+-+-+-------+  +-+-------------+
     */
    public void sendPing() {
        if (mWebsocketClient != null && mWebsocketClient.isConnected()) {
            byte b0 = (byte) ((B0_FLAG_FIN | OPCODE_CONTROL_PING) & 0xFF);
            byte b1 = (byte) (B1_FLAG_MASK & 0xFF);
            mWebsocketClient.sendBinary(new byte[]{b0, b1});
            Log.i(TAG, "---sendPing() Send a ping with the supplied.");
        }
    }


    @Override
    public void uploadVocabs(Vocab... vocabs) {
        Log.e(TAG, "cinfo v2 not implements uploadVocabs");
    }


    /**
     * 2021/10/12 最新的上传逻辑是在启动对话 socket 连接成功之后进行，因此在上传之前有可能会产生多个
     * 相同类型的 cInfo ，实际上只需要同步最后一次传入的 cInfo 即可，为了防止 cInfo 堆积现象，需要在
     * 上传之前做去重处理。
     *
     * @param intent {@link ProductContext}
     */
    private void removeDuplicateCInfo(ProductContext intent) {
        String mOption = intent.getOption();
        List<Setting> mSettings = intent.getSettings();
        if (TextUtils.isEmpty(mOption) || mSettings == null) {
            return;
        }
        boolean isRepeated = false;
        String cInfo = "";
        String recordId = "";
        try {
            for (String s : pendingUploadQueue) {
                isRepeated = false;
                cInfo = s;

                JSONObject cInfoObj = new JSONObject(cInfo);
                if (cInfoObj.has("recordId")) {
                    recordId = cInfoObj.optString("recordId");
                }
                if (!cInfoObj.has("settings")) {
                    continue;
                }
                //通过 settings.key 是否相同来做去重处理
                JSONArray settingsArray = cInfoObj.optJSONArray("settings");
                for (int i = 0; i < settingsArray.length(); i++) {
                    JSONObject settingObj = settingsArray.getJSONObject(i);
                    if (!settingObj.has("key")) {
                        continue;
                    }
                    String mKey = settingObj.optString("key");
                    Log.i(TAG, "key:" + mKey);
                    if (TextUtils.isEmpty(mKey)) {
                        continue;
                    }

                    for (Setting setting : mSettings) {
                        if (mKey.equals(setting.getKey())) {
                            isRepeated = true;
                            break;
                        }
                    }
                }
                if (isRepeated) {
                    break;
                }
            }

            if (isRepeated) {
                pendingUploadQueue.remove(cInfo);
                cInfoRecordIdList.remove(recordId);
                Log.i(TAG, "Duplicate information detected.");
            }
            Log.i(TAG, "---removeDuplicateCInfo() pendingUploadQueue size:" + pendingUploadQueue.size());

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void uploadProductContext(ProductContext intent) {

        removeDuplicateCInfo(intent);

        JSONObject text = new JSONObject();
        try {
            String cInfoRecordId = Util.uuid();
            cInfoRecordIdList.add(cInfoRecordId);
            text.put("recordId", cInfoRecordId);
            text.put("topic", "system.settings");
            if (TextUtils.equals(ProductContext.OPTION_DELETE, intent.getOption())) {
                text.put("option", ProductContext.OPTION_DELETE);
            }
            JSONArray settings = new JSONArray();
            for (Setting s : intent.getSettings()) {
                settings.put(s.toJSON());
            }
            text.put("settings", settings);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        upload(text.toString());
    }

    @Override
    public void uploadSkillContext(SkillContext context) {
        JSONObject text = new JSONObject();
        try {
            String cInfoRecordId = Util.uuid();
            cInfoRecordIdList.add(cInfoRecordId);
            text.put("recordId", cInfoRecordId);
            text.put("topic", "skill.settings");
            if (TextUtils.equals(ProductContext.OPTION_DELETE, context.getOption())) {
                text.put("option", SkillContext.OPTION_DELETE);
            }
            JSONArray settings = new JSONArray();
            for (Setting s : context.getSettings()) {
                settings.put(s.toJSON());
            }
            text.put("settings", settings);
            text.put("skillId", context.getSkillId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        upload(text.toString());
    }

    class OggCallbackImpl implements PcmToOgg.Callback {

        @Override
        public int run(byte[] data) {
//            Log.d(TAG, "解码之后的大小：" + size);
            if (mWebsocketClient != null) {
                if (firstFeedFlag.compareAndSet(false, true) && (data.length != 0)) {
                    Log.d(TAG, "ASR.FIRST.FEED");
                    mFirstFeedTime = System.currentTimeMillis();
                    Log.d(TAG, "mFirstFeedTime is " + mFirstFeedTime);
                }
                if (data.length != 0) {
                    byte[] oggBuffer = new byte[data.length];
                    System.arraycopy(data, 0, oggBuffer, 0, data.length);
                    mWebsocketClient.sendBinary(oggBuffer);
                    mFileUtil.write(oggBuffer);
                }

            } else {
                Log.e(TAG, " ERROR ERROR ERROR ERROR ");
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }

    /**
     * 是否订阅服务端返回数据
     * cancel之后，不再返回结果
     */
    private volatile boolean isSubscribed = true;

    @Override
    public void run() {
        Message message;
        while ((message = waitMessage()) != null) {
//            Log.d(TAG, "get message : " + message.mId);
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    mConfig = (CloudDMConfig) message.mObject;
                    mPcmToOgg = new PcmToOgg();
                    mWebsocketClient = new WebsocketClient(mConfig.getConnectTimeout());
                    mPcmToOgg.initEncode(CloudASRParams.OGG, false, new OggCallbackImpl());
                    mListener.onInit(AIConstant.OPT_SUCCESS);
                    break;
                case Message.MSG_START:
                    mParams = (CloudSemanticParams) message.mObject;
                    Log.d(TAG, "start param: " + mParams);
                    mInput = mParams.toJSON().toString();
                    if (mListener != null) {
                        mListener.onConnect(mWebsocketClient.isConnected());
                    }
                    if (checkConnect()) {
                        mRecordId = mParams.getRecordId();
                        String saveAudioPath = mParams.getSaveAudioPath();
                        if (!TextUtils.isEmpty(saveAudioPath)) {
                            mFileUtil.createFile(saveAudioPath + "/" + "cloud_dm_" + mRecordId + ".ogg");
                        }
                        Log.d(TAG, "request input : " + mInput);
                        mWebsocketClient.sendText(mInput);
                        mPcmToOgg.startEncode();
                        Log.d(TAG, "ASR.BEGIN");

                        if (oneShotCache != null) {
                            Log.i(TAG, "---handleMessage() buffer:" + oneShotCache.size() +
                                    ",buffer.isValid:" + oneShotCache.isValid());
                            //上传 one-shot 的音频流
                            if (oneShotCache.isValid()) {
                                for (byte[] data : oneShotCache) {
                                    if (data != null) {
                                        Log.i(TAG, "one-shot iterator data:" + data.length);
                                        feed(data);
                                    }
                                }
                                oneShotCache = null;
                            }
                        }
                        //上传识别部分（socket 还没有连接成功）的音频
                        if (audioCache != null) {
                            for (byte[] data : audioCache) {
                                if (data != null) {
                                    Log.i(TAG, "audio cache iterator data:" + data.length);
                                    feed(data);
                                }
                            }
                            audioCache.clear();
                        }

                        //置位
                        firstFeedFlag.compareAndSet(true, false);
                        firstResultFlag.set(false);
                        isSubscribed = true;
                    } else {
                        if (!pendingUploadQueue.contains(TOPIC_RECORDER_STREAM)) {
                            upload(TOPIC_RECORDER_STREAM);
                        } else {
                            Log.w(TAG, "ignore start repeat send topic: " + TOPIC_RECORDER_STREAM);
                        }
                    }
                    break;
                case Message.MSG_STOP:

                    if (mPcmToOgg != null)
                        mPcmToOgg.stopEncode();
                    if (mWebsocketClient != null) {
                        mWebsocketClient.sendBinary(new byte[0]);
                        Log.d(TAG, "ASR.END");
                        mStopTime = System.currentTimeMillis();
                        Log.d(TAG, "mStopTime is " + mStopTime);
                    }
                    if (mFileUtil != null)
                        mFileUtil.closeFile();

                    break;
                case Message.MSG_CLOSE:
                    disconnect(false);
                    if (mPcmToOgg != null)
                        mPcmToOgg.stopEncode();
                    if (mFileUtil != null)
                        mFileUtil.closeFile();

                    isSubscribed = false;//cancel 之后不再返回服务端返回结果

                    if (audioCache != null) {
                        audioCache.clear();
                    }
                    break;
                case Message.MSG_RELEASE:
                    // 销毁引擎
                    Log.d(TAG, "MSG_RELEASE");
                    disconnect(true);
                    if (mPcmToOgg != null)
                        mPcmToOgg.destroyEncode();
                    isReleased = true;
                    if (audioCache != null) {
                        audioCache.clear();
                    }
                    Log.d(TAG, "MSG_RELEASE END");
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    //Log.d(TAG, "asr receiver data:" + data.length + ", conn:" + mWebsocketClient.isConnected());
                    if (!mWebsocketClient.isConnected()) {
                        audioCache.add(data);
                    } else {
                        if (mPcmToOgg != null) {
                            mPcmToOgg.feedData(data, data.length);
                        }
                    }
                    break;
                case Message.MSG_ERROR:
                    mRecordId = "";
                    cInfoRecordIdList.clear();
                    mListener.onError((AIError) message.mObject);
                    break;
                case Message.MSG_FEEDBACK:
                    checkConnect();
                    FeedbackParams feedbackParams = (FeedbackParams) message.mObject;
                    Log.d(TAG, "feedback: " + feedbackParams.toJSON());
                    mWebsocketClient.sendText(feedbackParams.toJSON().toString());
                    isSubscribed = true;
                    break;
                case Message.MSG_TRIGGER_INTENT:
                    checkConnect();
                    TriggerIntentParams triggerIntentParams = (TriggerIntentParams) message.mObject;
                    Log.d(TAG, "trigger : " + triggerIntentParams.toJSON());
                    mWebsocketClient.sendText(triggerIntentParams.toJSON().toString());
                    isSubscribed = true;
                    break;
                case Message.MSG_ASYNC:
                    checkConnect();
                    MultiModalParams multiModalParams = (MultiModalParams) message.mObject;
                    Log.d(TAG, "async : " + multiModalParams.toJSON());
                    mWebsocketClient.sendText(multiModalParams.toJSON().toString());
                    isSubscribed = true;
                    break;
                case Message.MSG_FEEDBACK_2_PRIV_CLOUD:
                    checkConnect();
                    FeedbackJsonParams fJsonParams = (FeedbackJsonParams) message.mObject;
                    Log.d(TAG, "feedback2PRIVCLoud : " + fJsonParams.toJSON());
                    mWebsocketClient.sendText(fJsonParams.toString());
                    isSubscribed = true;
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
    public void onMessage(String text) {
        if (mListener != null && isSubscribed) {
            try {
                JSONObject responseObj = new JSONObject(text);
                if (responseObj.has(Protocol.NLU)) {
                    JSONObject nluObj = responseObj.optJSONObject(Protocol.NLU);
                    nluObj = JSONUtil.normalSemanticSlots(nluObj);
                    responseObj.put(Protocol.NLU, nluObj);
                    text = responseObj.toString();
                }
                String recordId = responseObj.optString("recordId");
                if (cInfoRecordIdList.contains(recordId)) {
                    cInfoRecordIdList.remove(recordId);
                    mListener.onUpdateResult(AIConstant.OPT_SUCCESS);
                } else {
                    Log.d(TAG, "DM.RESULT: " + text);
                    if (firstResultFlag.compareAndSet(false, true)) {
                        Log.d(TAG, "receive first result after start " + text);
                        mAsrFirstResultTime = System.currentTimeMillis();
                        Log.d(TAG, "mAsrFirstResultTime is " + mAsrFirstResultTime);
                    }

                    AIResult aiResult = AIResult.bundleResults(AIConstant.AIENGINE_MESSAGE_TYPE_JSON, mRecordId, text);
                    if (responseObj.has(Protocol.DM) || responseObj.has(Protocol.NLU)) {
                        if (mParams != null && mParams.isEnableVocabsConvert()) {
                            String ret = Convert.getInstance().restore(aiResult.getResultObject().toString());
                            aiResult.setResultObject(ret);
                        }
                    }
                    if (responseObj.has(Protocol.TOPIC)) {
                        String dmout = (String) responseObj.opt(Protocol.TOPIC);
                        if (Protocol.DM_OUTPUT.equals(dmout)) {
                            aiResult.setLast(true);
                        }
                    }

                    mListener.onResults(aiResult);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "unsubscribe DM.RESULT: " + text);
        }
    }

    @Override
    public void onError(String text) {
        Log.e(TAG, "onError : " + (TextUtils.isEmpty(text) ? "is null" : text));
        AIError aiError;
        if (!TextUtils.isEmpty(text) && text.contains("dns")) {
            aiError = new AIError(AIError.ERR_DNS, AIError.ERR_DESCRIPTION_ERR_DNS, mRecordId);
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
            disconnect(true);
            mListener.onError(aiError);
        }
    }

    @Override
    public void onOpen() {
        // 建立连接时，如果有等待的队列，重新发送
        send();
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

    public synchronized boolean isConnected() {
        if (mWebsocketClient != null) {
            return mWebsocketClient.isConnected();
        }
        return false;
    }
}