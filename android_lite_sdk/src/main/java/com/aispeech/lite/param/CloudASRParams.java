/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.auth.AIProfile;
import com.aispeech.common.Log;
import com.aispeech.lite.AISpeechSDK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 云端语音识别参数
 * <p>
 * <p/>
 * 设置并调用云端识别
 *
 * <pre>
 *    ...
 *    AISpeechEngine engine = new AISpeechEngine(...); //创建思必驰语音识别引擎
 *    engine.setSpeechListener(new SpeechListenerImpl());   //设置回调
 *    CloudASRParams params = new CloudASRParams();                   //创建识别参数
 *    ...
 *    engine.start(params);
 * </pre>
 *
 * </pre>
 */
public class CloudASRParams extends SpeechParams {

    /**
     * 音频编码类型 OGG
     */
    public static final int OGG = 0;
    /**
     * 音频编码类型 OGG_OPUS
     */
    public static final int OGG_OPUS = 1;
    /**
     * 音频编码类型 WAV
     */
    public static final int WAV = 2;
    /**
     * 音频编码类型 MP3
     */
    public static final int MP3 = 3;
    /**
     * 音频编码类型 OPUS
     */
    public static final int OPUS = 4;
    private static final String TAG = "CloudASRParams";
    // asrPlus ==>
    private String groupId;
    private String serverName;
    private String organization;
    private String domain = "";
    private String contextId = "";
    private List<String> users;
    private boolean cloudVprintVadEnable = true;
    private float minSpeechLength;
    private String constantContent;
    // asrPlus <==
    private String[] hotWords;
    /**
     * oneshot 功能优化，当用户说 唤醒词+命令词 时，vad在唤醒词后即结束，导致asr识别结果是空，
     * 可打开此功能，此功能会保留唤醒词后vad结束后的音频，即命令词的音频，然后重新asr识别
     */
    private boolean oneshotOptimization = false;
    private String[] oneshotOptimizationFilterWords = null;
    /**
     * 是否是oneshot优化的第二次请求，是的话不带上 自定义唤醒词
     */
    private boolean oneshotOptimizationSecond = false;
    private String server = "wss://asr.dui.ai/runtime/v2/recognize";
    private String aliasKey = "prod";
    /**
     * context
     */
    private String productId;
    private String userId;
    private String deviceName;
    private String deviceId;
    private String sdkName = "DUI-lite-android-sdk-" + AISpeechSDK.SDK_VERSION;
    //首字延时优化
    private boolean enableFirstDec = false;
    //vad=false时强制开首字优化
    private boolean enableFirstDecForce = false;
    /**
     * 是否保存asr从start到stop所有的音频
     */
    private boolean saveOriginalAudio = false;
    /**
     * request
     */
    private String requestId;
    /**
     * request->audio
     */
    private int audioType = OGG;
    private int sampleRate = 16000;
    private int channel = 1;
    private int sampleBytes = 2;
    private String url;
    /**
     * 用户 feed 的音频是否是编码后的音频。
     * 默认 false 即 feed 的是 pcm 音频，true 表示 feed 的是编码后的音频，如 MP3 OGG OPUS OGG_OPUS
     * 使用前提 用户feed，并且不使用本地vad
     */
    private boolean encodedAudio;
    /**
     * request->asr
     */
    private JSONArray customWakeupWord;//设置自定义唤醒词
    private JSONArray commonWakeupWord;//设置自定义唤醒词
    private String wakeupWord;//配置自定义唤醒词功能；配合唤醒词分数使用可增强唤醒词识别，同时结果中过滤所传唤醒词；
    private boolean visibleWakeupWord = false;//oneshot唤醒词过滤开关
    private boolean enableRealTimeFeedBack = false;//实时反馈
    private boolean enableVad = true;//云端VAD
    private boolean enablePunctuation = false;//标点符号
    private boolean enableNumberConvert = false;//数字转换
    private boolean enableTone = false;//音调功能
    private boolean enableLanguageClassifier = false;//语言分类
    private boolean enableSNTime = false;//rec结果增加对齐信息
    private boolean enableConfidence = true;//置信度
    private boolean enableAlignment = false;// alignment
    private boolean enableSensitiveWdsNorm = true;//敏感词过滤
    private boolean enableRecUppercase = true;//输出英文字母转成大写，true 为转大写，false 为不转大写。默认 true
    /**
     * 自定义唤醒词自定义分数设置，因该参数会影响识别唤醒词的效果同时会增加误识别风险，
     * 建议在专业人士指导下定制自定义分数（建议给-5），
     * 该项可以配合 wakeupWord/customWakeupWord 使用，对应内核字段：custom_wakeup_score
     */
    private int selfCustomWakeupScore = 0;//
    private boolean enableAudioDetection = true;
    /**
     * 语种，长语音、短语音都会用这个变量
     */
    private String language = null;
    private String res = "comm";
    private String lmId = "";
    private int nbest = 0;
    /**
     * 是否是对话用的ASR，用于对话使用，使用单独的请求参数
     */
    private boolean useDmAsr;
    private JSONObject dmAsrJson;
    private String dmWssCustomParams;
    private Map<String, Object> extraParam = null;
    /**
     * 是否是长语音实时识别
     */
    private boolean lasr = false;
    /**
     * 长语音识别, 当参数不为空时，启动转发模式。 当有转写结果时，会往注册的WebSocket地址实时推送转写结果。
     * <p>
     * 支持多个转写websocket服务地址，多个地址中间用英文逗号 , 隔开。
     * <p>
     * 格式： ws://xxxx:port,ws://xxxx:port,ws://xxxx:port
     */
    private String lasrForwardAddresses;
    /**
     * 长语音识别, 这个值不为空则说明是实时长语音
     */
    private String lasrRealtimeParam;
    /**
     * lasr-cn-en使用中英文混合，不设置res字段使用中文在线
     */
    private String lasrRes;

    /**
     * 设置是否忽略结果为空的结果，如果打开，则空结果不对外输出，反而使用
     */
    private boolean ignoreEmptyResult = false;

    /**
     * 空结果重试次数，默认值为3
     */
    private int ignoreEmptyResultCounts = 3;
    private int customWakeupScore = -6;

    //（默认为true，不开启）
    //falase，开启特殊唤醒词过滤功能，强制过滤唤醒词及之前内容；
    //true 关闭特殊唤醒词过滤
    private boolean useStrongWakeupVisible = true;
    //热词增强：请求级别热词功能，请求时传参传热词生效 （支持通用模型泛化）
    private JSONArray phraseList;
    /**
     * 在实时反馈的基础上支持rec快出，必须在realback=true时生效 default is {@value}
     */
    private Boolean enableRealBackFastend = null;
    /**
     * 识别结果规则矫正
     */
    private Boolean enableRuleCorrect = null;
    /**
     * 1best的按字confidence随 alignment的接口出。
     */
    private Boolean enableWordConfi = null;
    /**
     * nbest 按字confidence功能
     */
    private Boolean enableNbestConfi = null;
    /**
     * 支持口语顺滑开关
     */
    private Boolean enableTxtSmooth = null;
    /**
     * 开启后，将把每一路的nbest分开
     */
    private Boolean enableSeparateNBest = null;

    public CloudASRParams(AIProfile profile) {
        this(profile.getProductId(), profile.getDeviceName(), profile.getDeviceId());
    }

    public CloudASRParams(String productId, String deviceName, String deviceId) {
        super();
        setCoreType(CN_ASR_REC);
        setTag(TAG);
        setAiType("asr");

        this.productId = productId;
        this.userId = "";
        this.deviceName = deviceName;
        this.deviceId = deviceId;
    }

    public boolean isEnableRecUppercase() {
        return enableRecUppercase;
    }

    /**
     * 输出英文字母转成大写，true 为转大写，false 为不转大写。默认 true
     *
     * @param enableRecUppercase true/false
     */
    public void setEnableRecUppercase(boolean enableRecUppercase) {
        this.enableRecUppercase = enableRecUppercase;
    }

    public boolean isEnableSensitiveWdsNorm() {
        return enableSensitiveWdsNorm;
    }

    /**
     * 敏感词过滤
     * true 是开启，false 是关闭   默认是开启
     *
     * @param enableSensitiveWdsNorm
     */

    public void setEnableSensitiveWdsNorm(boolean enableSensitiveWdsNorm) {
        this.enableSensitiveWdsNorm = enableSensitiveWdsNorm;
    }

    public String getWakeupWord() {
        return wakeupWord;
    }

    /**
     * 配置自定义唤醒词功能；配合唤醒词分数使用可增强唤醒词识别，同时结果中过滤所传唤醒词；
     * 如果同时设置commonWakeupWord，仍会过滤唤醒词，但唤醒词的增强识别会生效
     *
     * @param wakeupWord
     */
    public void setWakeupWord(String wakeupWord) {
        this.wakeupWord = wakeupWord;
    }


    public JSONArray getCommonWakeupWord() {
        return commonWakeupWord;
    }

    /**
     * 配置自定义唤醒词；等价customWakeupWord
     *
     * @param commonWakeupWord
     */
    public void setCommonWakeupWord(JSONArray commonWakeupWord) {
        this.commonWakeupWord = commonWakeupWord;
    }

    public boolean isEnableFirstDec() {
        return enableFirstDec;
    }

    /**
     * 首字延时优化
     *
     * @param enableFirstDec 默认是false
     */
    public void setEnableFirstDec(boolean enableFirstDec) {
        this.enableFirstDec = enableFirstDec;
    }

    public boolean isEnableFirstDecForce() {
        return enableFirstDecForce;
    }

    /**
     * vad=false时强制开首字优化
     *
     * @param enableFirstDecForce 默认是false
     */
    public void setEnableFirstDecForce(boolean enableFirstDecForce) {
        this.enableFirstDecForce = enableFirstDecForce;
    }


    public Boolean getEnableRealBackFastend() {
        return enableRealBackFastend;
    }

    /**
     * 在实时反馈的基础上支持rec快出，必须在realback=true时生效 default is {@value}
     */
    public void setEnableRealBackFastend(Boolean enableRealBackFastend) {
        this.enableRealBackFastend = enableRealBackFastend;
    }

    public Boolean getEnableRuleCorrect() {
        return enableRuleCorrect;
    }

    /**
     * 识别结果规则矫正
     */
    public void setEnableRuleCorrect(Boolean enableRuleCorrect) {
        this.enableRuleCorrect = enableRuleCorrect;
    }

    public Boolean getEnableWordConfi() {
        return enableWordConfi;
    }

    /**
     * 1best的按字confidence随 alignment的接口出。
     */
    public void setEnableWordConfi(Boolean enableWordConfi) {
        this.enableWordConfi = enableWordConfi;
    }

    public Boolean getEnableNbestConfi() {
        return enableNbestConfi;
    }

    /**
     * nbest 按字confidence功能
     */
    public void setEnableNbestConfi(Boolean enableNbestConfi) {
        this.enableNbestConfi = enableNbestConfi;
    }

    public Boolean getEnableTxtSmooth() {
        return enableTxtSmooth;
    }

    /**
     * 支持口语顺滑开关
     */
    public void setEnableTxtSmooth(Boolean enableTxtSmooth) {
        this.enableTxtSmooth = enableTxtSmooth;
    }

    public Boolean getEnableSeparateNBest() {
        return enableSeparateNBest;
    }

    /**
     * 开启后，将把每一路的nbest分开
     *
     * @param enableSeparateNBest
     */
    public void setEnableSeparateNBest(Boolean enableSeparateNBest) {
        this.enableSeparateNBest = enableSeparateNBest;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("context", getContextJSON());
            jsonObject.put("request", getRequestJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject getDmAsrJson() {
        return dmAsrJson;
    }

    public void setDmAsrJson(JSONObject dmAsrJson) {
        this.dmAsrJson = dmAsrJson;
    }

    public void setDmWssCustomParams(String dmWssCustomParams) {
        this.dmWssCustomParams = dmWssCustomParams;
    }

    public String getDmWssCustomParams() {
        return dmWssCustomParams;
    }

    public Object getContextJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("productId", productId);
            jsonObject.put("userId", userId);
            jsonObject.put("deviceName", deviceName);
            jsonObject.put("deviceId", deviceId);
            jsonObject.put("sdkName", sdkName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject getRequestJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("requestId", requestId);
            jsonObject.put("audio", getRequestAudioJSON());
            jsonObject.put("asr", getRequestAsrJSON());
            jsonObject.put("asrPlus", getVPrintJSON());
            if (phraseList != null && phraseList.length() > 0) {
                jsonObject.put("phraseList", getPhraseList());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d("getRequestJSON: ", jsonObject.toString());
        return jsonObject;
    }

    public String[] getHotWords() {
        return hotWords;
    }

    public void setHotWords(String[] hotWords) {
        this.hotWords = hotWords;
    }

    public String getConstantContent() {
        return constantContent;
    }

    public void setConstantContent(String constantContent) {
        this.constantContent = constantContent;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public boolean isCloudVprintVadEnable() {
        return cloudVprintVadEnable;
    }

    public void setCloudVprintVadEnable(boolean cloudVprintVadEnable) {
        this.cloudVprintVadEnable = cloudVprintVadEnable;
    }

    /**
     * 最小有效音频长度，单位秒,当前服务配置:
     * sti-sr 1s
     * lti-sr 500ms
     * sdk默认1s，不要使用小于服务器配置的值
     */
    public float getMinSpeechLength() {
        return minSpeechLength;
    }

    /**
     * 最小有效音频长度，单位秒,当前服务配置:
     * sti-sr 1s
     * lti-sr 500ms
     * sdk默认1s，不要使用小于服务器配置的值
     */
    public void setMinSpeechLength(float minSpeechLength) {
        this.minSpeechLength = minSpeechLength;
    }

    public JSONArray getPhraseList() {
        return phraseList;
    }

    /**
     * 热词增强：请求级别热词功能，请求时传参传热词生效 （支持通用模型泛化）
     *
     * @param phraseList
     */
    public void setPhraseList(JSONArray phraseList) {
        this.phraseList = phraseList;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public JSONObject getVPrintJSON() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("serverName", serverName);
            jsonObject.put("organization", organization);
            if (!domain.isEmpty()) {
                jsonObject.put("domain", domain);
            }
            if (!contextId.isEmpty()) {
                jsonObject.put("contextId", contextId);
            }
            if (!TextUtils.isEmpty(groupId)) {
                jsonObject.put("groupId", groupId);
            }
            boolean existUser = users != null && !users.isEmpty();
            if (existUser) {
                JSONArray usersArray = new JSONArray();
                for (String s : users) {
                    usersArray.put(s);
                }
                jsonObject.put("users", usersArray);
            }
            jsonObject.put("enableAsrPlus", !TextUtils.isEmpty(serverName) && (existUser || !TextUtils.isEmpty(groupId)));
            if (!TextUtils.isEmpty(groupId)) {
                jsonObject.put("groupId", groupId);
            }

            JSONObject env = new JSONObject();
            env.put("enableVAD", cloudVprintVadEnable);
            if (minSpeechLength > 0) {
                env.put("minSpeechLength", minSpeechLength);
            }
            if (!TextUtils.isEmpty(constantContent)) {
                env.put("constantContent", constantContent);
            }
            jsonObject.put("env", env);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public Object getRequestAudioJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            String audioTypeStr;
            switch (audioType) {
                case OGG_OPUS:
                    audioTypeStr = "ogg_opus";
                    break;
                case WAV:
                    audioTypeStr = "wav";
                    break;
                case MP3:
                    audioTypeStr = "mp3";
                    break;
                case OPUS:
                    audioTypeStr = "opus";
                    break;
                default:
                    audioTypeStr = "ogg";
                    break;
            }
            jsonObject.put("audioType", audioTypeStr);
            jsonObject.put("sampleRate", sampleRate);
            jsonObject.put("channel", channel);
            jsonObject.put("sampleBytes", sampleBytes);
            //    jsonObject.put("url",url);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public Object getRequestAsrJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (!oneshotOptimizationSecond) {
                jsonObject.put("wakeupWord", wakeupWord);
            }
            if (commonWakeupWord != null && !oneshotOptimizationSecond) {
                jsonObject.put("commonWakeupWord ", commonWakeupWord);
            }
            if (customWakeupWord != null && !oneshotOptimizationSecond) {
                jsonObject.put("customWakeupWord", customWakeupWord);
            }
            //oneshot双轮网络请求，在第一轮增加oneshot数据，可以用于筛选误唤醒数据
            if (isUseOneShot() && !oneshotOptimizationSecond) {
                jsonObject.put("oneshot", isUseOneShot());
            }
            jsonObject.put("enableFirstDec", enableFirstDec);
            jsonObject.put("enableFirstDecForce", enableFirstDecForce);
            jsonObject.put("enableRealTimeFeedback", enableRealTimeFeedBack);
            jsonObject.put("enableRealBackFastend", enableRealBackFastend);
            jsonObject.put("enableRuleCorrect", enableRuleCorrect);
            jsonObject.put("enableWordConfi", enableWordConfi);
            jsonObject.put("enableNbestConfi", enableNbestConfi);
            jsonObject.put("enableTxtSmooth", enableTxtSmooth);
            jsonObject.put("enableSeparateNBest", enableSeparateNBest);
            jsonObject.put("enableVAD", enableVad);
            jsonObject.put("enablePunctuation", enablePunctuation);
            jsonObject.put("enableNumberConvert", enableNumberConvert);
            jsonObject.put("enableTone", enableTone);
            jsonObject.put("customWakeupScore", customWakeupScore);
            jsonObject.put("enableAlignment", enableAlignment);
            jsonObject.put("enableLanguageClassifier", enableLanguageClassifier);
            jsonObject.put("enableSNTime", enableSNTime);
            jsonObject.put("enableConfidence", enableConfidence);
            jsonObject.put("enableSensitiveWdsNorm", enableSensitiveWdsNorm);
            jsonObject.put("enableRecUppercase", enableRecUppercase);
            jsonObject.put("selfCustomWakeupScore", selfCustomWakeupScore);
            jsonObject.put("enableAudioDetection", enableAudioDetection);
            jsonObject.put("language", language);
            jsonObject.put("res", res);
            //默认为true，true 服务端不开启此功能， false是开启此功能，配合wakeupWord功能使用
            jsonObject.put("enableUseStrongWakeupVisible", useStrongWakeupVisible);
            JSONArray hotWordsArray = transferStringToList(getHotWords());
            if (hotWordsArray != null && hotWordsArray.length() != 0) {
                jsonObject.put("hotWords", hotWordsArray);
            }
            if (!TextUtils.isEmpty(lmId)) {
                jsonObject.put("lmId", lmId);
            }
            if (nbest > 0) {
                jsonObject.put("nbest", nbest);
            }
            if (extraParam != null && !extraParam.isEmpty()) {
                Iterator<String> iter = extraParam.keySet().iterator();
                while (iter.hasNext()) {
                    String key = iter.next();
                    Object valueObject = extraParam.get(key);
                    if (valueObject == null) {
                        continue;
                    } else if (valueObject instanceof Iterable) {
                        JSONArray jsonArray = new JSONArray();
                        for (Object o : (Iterable) valueObject) {
                            jsonArray.put(o);
                        }
                        jsonObject.put(key, jsonArray);
                    } else if (valueObject.getClass().isArray()) {
                        JSONArray jsonArray = new JSONArray();
                        int length = Array.getLength(valueObject);
                        for (int i = 0; i < length; i++) {
                            jsonArray.put(Array.get(valueObject, i));
                        }
                        jsonObject.put(key, jsonArray);
                    } else
                        jsonObject.put(key, valueObject);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public boolean isSaveOriginalAudio() {
        return saveOriginalAudio;
    }

    public void setSaveOriginalAudio(boolean saveOriginalAudio) {
        this.saveOriginalAudio = saveOriginalAudio;
    }

    public boolean isUseDmAsr() {
        return useDmAsr;
    }

    /**
     * 是否是对话用的ASR，用于对话使用，使用单独的请求参数
     *
     * @param useDmAsr 使用DM的asr，默认false
     */
    public void setUseDmAsr(boolean useDmAsr) {
        this.useDmAsr = useDmAsr;
    }

    public boolean isOneshotOptimization() {
        return oneshotOptimization;
    }

    public void setOneshotOptimization(boolean oneshotOptimization) {
        this.oneshotOptimization = oneshotOptimization;
    }

    public boolean isOneshotOptimizationSecond() {
        return oneshotOptimizationSecond;
    }

    public void setOneshotOptimizationSecond(boolean oneshotOptimizationSecond) {
        this.oneshotOptimizationSecond = oneshotOptimizationSecond;
    }

    public String[] getOneshotOptimizationFilterWords() {
        return oneshotOptimizationFilterWords;
    }

    public void setOneshotOptimizationFilterWords(String[] oneshotOptimizationFilterWords) {
        this.oneshotOptimizationFilterWords = oneshotOptimizationFilterWords;
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public void setServer(String server) {
        this.server = server;
    }

    public String getAliasKey() {
        return aliasKey;
    }

    public void setAliasKey(String aliasKey) {
        this.aliasKey = aliasKey;
    }

    @Override
    public String getProductId() {
        return productId;
    }

    @Override
    public void setProductId(String productId) {
        this.productId = productId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    @Override
    public String getDeviceId() {
        return deviceId;
    }

    @Override
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getSdkName() {
        return sdkName;
    }

    public void setSdkName(String sdkName) {
        this.sdkName = sdkName;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public int getAudioType() {
        return audioType;
    }

    public void setAudioType(int audioType) {
        this.audioType = audioType;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getSampleBytes() {
        return sampleBytes;
    }

    public void setSampleBytes(int sampleBytes) {
        this.sampleBytes = sampleBytes;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public JSONArray getCustomWakeupWord() {
        return customWakeupWord;
    }

    /**
     * 配置自定义唤醒词功能；配合唤醒词分数使用可增强唤醒词识别，同时结果中不过滤所传唤醒词
     *
     * @param customWakeupWord
     */
    public void setCustomWakeupWord(JSONArray customWakeupWord) {
        this.customWakeupWord = customWakeupWord;
    }

    public boolean isVisibleWakeupWord() {
        return visibleWakeupWord;
    }

    public void setVisibleWakeupWord(boolean visibleWakeupWord) {
        this.visibleWakeupWord = visibleWakeupWord;
    }

    public boolean isEnableRealTimeFeedBack() {
        return enableRealTimeFeedBack;
    }

    public void setEnableRealTimeFeedBack(boolean enableRealTimeFeedBack) {
        this.enableRealTimeFeedBack = enableRealTimeFeedBack;
    }

    public int getCustomWakeupScore() {
        return this.customWakeupScore;
    }

    public void setCustomWakeupScore(int customWakeupScore) {
        this.customWakeupScore = customWakeupScore;
    }

    public boolean isEnableAlignment() {
        return enableAlignment;
    }

    public void setEnableAlignment(boolean enableAlignment) {
        this.enableAlignment = enableAlignment;
    }

    public boolean isEnableVad() {
        return enableVad;
    }

    public void setEnableVad(boolean enableVad) {
        this.enableVad = enableVad;
    }

    public boolean isEnablePunctuation() {
        return enablePunctuation;
    }

    public void setEnablePunctuation(boolean enablePunctuation) {
        this.enablePunctuation = enablePunctuation;
    }

    public boolean isEnableNumberConvert() {
        return enableNumberConvert;
    }

    public void setEnableNumberConvert(boolean enableNumberConvert) {
        this.enableNumberConvert = enableNumberConvert;
    }

    public boolean isEnableTone() {
        return enableTone;
    }

    public void setEnableTone(boolean enableTone) {
        this.enableTone = enableTone;
    }

    public boolean isEnableLanguageClassifier() {
        return enableLanguageClassifier;
    }

    public void setEnableLanguageClassifier(boolean enableLanguageClassifier) {
        this.enableLanguageClassifier = enableLanguageClassifier;
    }

    public boolean isEnableSNTime() {
        return enableSNTime;
    }

    public void setEnableSNTime(boolean enableSNTime) {
        this.enableSNTime = enableSNTime;
    }

    public boolean isEnableConfidence() {
        return enableConfidence;
    }

    public void setEnableConfidence(boolean enableConfidence) {
        this.enableConfidence = enableConfidence;
    }

    public int getSelfCustomWakeupScore() {
        return selfCustomWakeupScore;
    }

    /**
     * 自定义唤醒词自定义分数设置，因该参数会影响识别唤醒词的效果同时会增加误识别风险，
     * 建议在专业人士指导下定制自定义分数（建议给-5），
     * 该项可以配合 wakeupWord/customWakeupWord 使用，对应内核字段：custom_wakeup_score
     *
     * @param selfCustomWakeupScore
     */
    public void setSelfCustomWakeupScore(int selfCustomWakeupScore) {
        this.selfCustomWakeupScore = selfCustomWakeupScore;
    }

    public boolean isEnableAudioDetection() {
        return enableAudioDetection;
    }

    public void setEnableAudioDetection(boolean enableAudioDetection) {
        this.enableAudioDetection = enableAudioDetection;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String getRes() {
        return res;
    }

    @Override
    public void setRes(String res) {
        this.res = res;
    }

    public String getLmId() {
        return lmId;
    }

    public void setLmId(String lmId) {
        this.lmId = lmId;
    }

    public int getNbest() {
        return nbest;
    }

    public void setNbest(int nbest) {
        this.nbest = nbest;
    }

    public Map<String, Object> getExtraParam() {
        return extraParam;
    }

    public void setExtraParam(Map<String, Object> extraParam) {
        this.extraParam = extraParam;
    }

    public String getLasrRealtimeParam() {
        return lasrRealtimeParam;
    }

    /**
     * 长语音识别才需要, 这个值不为空则说明是实时长语音
     *
     * @param lasrRealtimeParam 长语音实时识别的参数
     */
    public void setLasrRealtimeParam(String lasrRealtimeParam) {
        this.lasrRealtimeParam = lasrRealtimeParam;
    }

    /**
     * 是否是长语音识别
     *
     * @return true 是，false 不是 普通asr
     */
    public boolean isLasr() {
        return lasr;
    }

    public void setLasr(boolean lasr) {
        this.lasr = lasr;
    }

    public String getLasrForwardAddresses() {
        return lasrForwardAddresses;
    }

    public void setLasrForwardAddresses(String lasrForwardAddresses) {
        this.lasrForwardAddresses = lasrForwardAddresses;
    }

    public String getLasrRes() {
        return lasrRes;
    }

    public void setLasrRes(String lasrRes) {
        this.lasrRes = lasrRes;
    }

    public boolean isEncodedAudio() {
        return encodedAudio;
    }

    public void setEncodedAudio(boolean encodedAudio) {
        this.encodedAudio = encodedAudio;
    }

    public boolean isUseStrongWakeupVisible() {
        return useStrongWakeupVisible;
    }

    /**
     * default is {@value}
     * 使用场景：开启oneShot功能，开启实时返回，识别结果会有唤醒词或者是唤醒词前面有其他杂音情况。
     * 目前出现此类问题是sspe送回来的音频回滚时间较长
     * 或者三方送入的音频唤醒词前音频较长，导致唤醒词前面数据也会带入
     * 开启此功能，云端进行过滤，xxx（唤醒词）
     * 0. 不包含xxx => “”
     * 1. xxx => “”
     * 2. 123xxx => “”
     * 3. xxx456 => “456”
     * 4. 123xxx456 => “456”
     * Note: 这个功能与唤醒词后置功能互斥
     *
     * @param useStrongWakeupVisible true，开启特殊唤醒词过滤功能，强制过滤唤醒词及之前内容； false 关闭特殊唤醒词过滤
     */
    public void setUseStrongWakeupVisible(boolean useStrongWakeupVisible) {
        this.useStrongWakeupVisible = useStrongWakeupVisible;
    }

    private JSONArray transferStringToList(String[] strings) {
        if (strings == null)
            return null;
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < strings.length; i++) {
            String item = strings[i];
            if (item != null)
                jsonArray.put(item);
        }
        return jsonArray;
    }

    public boolean isIgnoreEmptyResult() {
        return ignoreEmptyResult;
    }

    public void setIgnoreEmptyResult(boolean ignoreEmptyResult) {
        this.ignoreEmptyResult = ignoreEmptyResult;
    }

    public int getIgnoreEmptyResultCounts() {
        return ignoreEmptyResultCounts;
    }

    public void setIgnoreEmptyResultCounts(int ignoreEmptyResultCounts) {
        this.ignoreEmptyResultCounts = ignoreEmptyResultCounts;
    }
}
