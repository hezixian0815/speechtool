package com.aispeech.export.intent;

import com.aispeech.base.IFespxEngine;
import com.aispeech.common.Log;
import com.aispeech.lite.base.BaseIntent;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.param.CloudASRParams;
import com.aispeech.lite.speech.Phrase;
import com.aispeech.lite.speech.PhraseHints;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AICloudASRIntent extends BaseIntent {
    private OneshotCache<byte[]> oneshotCache;

    public OneshotCache<byte[]> getOneshotCache() {
        return oneshotCache;
    }


    public void setOneshotCache(OneshotCache<byte[]> oneshotCache) {
        this.oneshotCache = oneshotCache;
    }

    /**
     * 设置VAD右边界,单位为ms, default is {@value}ms
     */
    private int pauseTime = 300;

    /**
     * 无语音超时时长，单位毫秒，default is {@value}
     * 如果达到该设置值时，自动停止录音
     * 0表示不进行语音超时判断
     */
    private int noSpeechTimeOut = 5000;
    /**
     * 音频最大录音时长，达到该值将取消语音引擎并抛出异常 <br>
     * 允许的最大录音时长 单位秒，default is {@value}s <br>
     * 0 表示无最大录音时长限制
     */
    private int maxSpeechTimeS = 60;
    /**
     * 保存的音频路径，最终的音频路径为path + recordId + ".ogg"
     */
    private String saveAudioPath;

    /**
     * 是否保存asr从start到stop所有的音频
     */
    private boolean saveOriginalAudio = false;

    /**
     * 等待识别结果超时时间 单位毫秒，小于或等于0则不设置超时，default is {@value}ms
     * <p> 从vad结束或者用户主动调用stop方法开始计时 </p>
     */
    private int waitingTimeout = 5000;

    /**
     * 设置oneshot功能的唤醒词和命令词之间的时间间隔阈值，单位:毫秒，default is {@value}ms <br>
     * 如果小于该阈值，就认为不是oneshot，如果大于该阈值，就认为是oneshot
     */
    private int intervalTimeThresh = 600;

    /**
     * 是否使用oneshot功能,default is {@value}  <br>
     * true 使用one shot功能，唤醒后立马起识别 <br>
     * false 合成后启动识别，或只是启动识别
     */
    private boolean useOneShot = false;

    /**
     * 是否是对话用的ASR，用于对话使用，使用单独的请求参数
     */
    private boolean useDmAsr;
    /**
     * dmAsr配置可用的dm参数
     */
    private JSONObject dmAsrJson;
    private String wssDMCustomParams;
    /**
     * 情感识别
     */
    private boolean enableEmotion = false;
    /**
     * 请求级热词
     */
    private PhraseHints[] phraseHints;
    /**
     * 热词增强：请求级别热词功能，请求时传参传热词生效 （支持通用模型泛化）
     */
    private Phrase[] phrases;
    private IFespxEngine fespxEngine;
    /**
     * 置信度
     */
    private boolean enableConfidence = true;
    /**
     * 产品级热词
     */
    private String[] hotWords;
    /**
     * 是否启用本地vad, default is null，使用和 AICloudASRConfig 一样的配置
     */
    private Boolean localVadEnable = null;
    /**
     * 服务器地址，默认不用设置
     */
    private String server = "wss://asr.dui.ai/runtime/v2/recognize";
    /**
     * 用户标示
     */
    private String userId;
    /**
     * 识别lmid
     */
    private String lmId;
    /**
     * 用户定制的语言模型列表
     */
    private String[] lmList;
    /**
     * 是否启用标点符号识别, default is {@value}
     */
    private boolean enablePunctuation = false;
    /**
     * 是否启用识别结果汉字数字转阿拉伯数字功能, default is {@value}
     */
    private boolean enableNumberConvert = false;
    /**
     * default is {@value}
     * 自定义唤醒词自定义分数设置，因该参数会影响识别唤醒词的效果同时会增加误识别风险，
     * 建议在专业人士指导下定制自定义分数（建议给-5），
     * 该项可以配合 wakeupWord/customWakeupWord 使用
     */
    private int selfCustomWakeupScore = 0;
    /**
     * 设置自定义唤醒词，可用于过滤和指定唤醒词识别比如 ["你好小乐","你好小白"]
     */
    private JSONArray customWakeupWord;
    private JSONArray commonWakeupWord;//设置自定义唤醒词
    private String wakeupWord;//配置自定义唤醒词功能；配合唤醒词分数使用可增强唤醒词识别，同时结果中过滤所传唤醒词；
    /**
     * 是否过滤句首唤醒词，用于 oneshot 功能, default is {@value}
     */
    private boolean wakeupWordFilter = false;
    /**
     * 音调功能, default is {@value}
     */
    private boolean enableTone = false;
    /**
     * 语言分类功能, default is {@value}
     */
    private boolean enableLanguageClassifier = false;
    /**
     * 设置rec结果增加对齐信息, default is {@value}
     */
    private boolean enableSNTime = false;
    /**
     * 指定语种，默认中文
     */
    private String language = "zh-cn";
    /**
     * 识别引擎的资源类型, default is {@value}
     */
    private String resourceType = "comm";
    /**
     * 是否使用实时反馈功能, default is {@value}
     */
    private boolean realback = false;
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
    /**
     * 是否开启服务端的vad功能，一般近场关闭，远场打开, default is {@value}
     */
    private boolean cloudVadEnable = true;
    /**
     * 用户 feed 的音频是否是编码后的音频。
     * 默认 false 即 feed 的是 pcm 音频，true 表示 feed 的是编码后的音频，如 MP3 OGG OPUS OGG_OPUS
     * 使用前提 用户feed，并且不使用本地vad
     */
    private boolean encodedAudio = false;
    /**
     * nbest
     */
    private int nbest = 0;
    /**
     * oneshot 功能优化，当用户说 唤醒词+命令词 时，vad在唤醒词后即结束，导致asr识别结果是空，
     * 可打开此功能，此功能会保留唤醒词后vad结束后的音频，即命令词的音频，然后重新asr识别
     */
    private boolean oneshotOptimization = false;
    private String[] oneshotOptimizationFilterWords = null;
    /**
     * 设置 PCM 编码成其它格式,以减小传输大小。
     */
    private PCM_ENCODE_TYPE audioType = PCM_ENCODE_TYPE.OGG;
    private Map<String, Object> extraParam = null;

    /**
     * 设置是否忽略结果为空的结果，如果打开，则空结果不对外输出，反而使用
     */
    private boolean ignoreEmptyResult = false;

    /**
     * 空结果重试次数，默认值为3
     */
    private int ignoreEmptyResultCounts = 3;

    /**
     * 唤醒得分
     */
    private int customWakeupScore;
    private boolean enableAlignment = false;// alignment
    private boolean enableAudioDetection = false;//音频检测
    private boolean enableSensitiveWdsNorm = true;//敏感词过滤
    private boolean enableRecUppercase = true;//输出英文字母转成大写，true 为转大写，false 为不转大写。默认 true
    private boolean enableDialectProcess = false;
    private boolean enableDialectProcessNlurec = false;

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

    //（默认为true，不开启）
    //false，开启特殊唤醒词过滤功能，强制过滤唤醒词及之前内容；
    //true 关闭特殊唤醒词过滤
    private boolean useStrongWakeupVisible = true;

    /**
     * 敏感词过滤
     * true 是开启，false 是关闭   默认是开启
     *
     * @param enableSensitiveWdsNorm 敏感词开关
     */

    public void setEnableSensitiveWdsNorm(boolean enableSensitiveWdsNorm) {
        this.enableSensitiveWdsNorm = enableSensitiveWdsNorm;
    }

    public int getCustomWakeupScore() {
        return customWakeupScore;
    }

    public void setCustomWakeupScore(int customWakeupScore) {
        this.customWakeupScore = customWakeupScore;
    }

    //首字延时优化
    private boolean enableFirstDec = false;
    //vad=false时强制开首字优化
    private boolean enableFirstDecForce = false;

    public boolean isEnableEmotion() {
        return enableEmotion;
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


    /**
     * 是否开启情感识别
     *
     * @param enableEmotion true 开启，false 不开(default)
     */
    public void setEnableEmotion(boolean enableEmotion) {
        this.enableEmotion = enableEmotion;
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
     * <p>
     * 新增输出接口"strong_wakeup_filtered"，在eof=1时输出，具体如下：
     * var部分：
     * (1) var正常进行唤醒词过滤，若第一个rec片段对应的var匹配到唤醒词，过滤唤醒词及之前的内容，且后续的rec片段对应的var不需要再过滤；
     * (2) 如果第一个rec片段对应的var配有匹配到唤醒词，则后续的rec片段对应的var继续匹配和过滤；
     * rec部分：
     * (1) 限制仅在第一个rec片段实现唤醒词匹配过滤，若匹配到唤醒词，则在eof=1的rec片段输出中，加入"strong_wakeup_filtered"=1的标记，若没有匹配到，则标记为0；
     * (2) 第一个rec和nlu_rec如果匹配到唤醒词，则正常过滤，没有匹配到，则输出原文本，其他rec和nlu_rec的内容不受过滤影响，可以正常输出；
     * <p>
     * Note: 这个功能与唤醒词后置功能互斥 ,需要与setWakeupWord 一起使用
     *
     * @param useStrongWakeupVisible true，开启特殊唤醒词过滤功能，强制过滤唤醒词及之前内容； false 关闭特殊唤醒词过滤
     */
    public void setUseStrongWakeupVisible(boolean useStrongWakeupVisible) {
        this.useStrongWakeupVisible = useStrongWakeupVisible;
    }

    public boolean isEnableDialectProcess() {
        return enableDialectProcess;
    }

    public void setEnableDialectProcess(boolean enableDialectProcess) {
        this.enableDialectProcess = enableDialectProcess;
    }

    public boolean isEnableDialectProcessNlurec() {
        return enableDialectProcessNlurec;
    }

    public void setEnableDialectProcessNlurec(boolean enableDialectProcessNlurec) {
        this.enableDialectProcessNlurec = enableDialectProcessNlurec;
    }

    public JSONArray getPhraseHintsJsonArray() {
        if (phraseHints == null)
            return null;
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < phraseHints.length; i++) {
            PhraseHints item = phraseHints[i];
            if (item != null)
                jsonArray.put(item.toJSON());
        }
        return jsonArray;
    }

    public boolean isUseDmAsr() {
        return useDmAsr;
    }

    public void setUseDmAsr(boolean useDmAsr, JSONObject dmAsrJson) {
        this.useDmAsr = useDmAsr;
        this.dmAsrJson = dmAsrJson;
    }

    public String getWakeupWord() {
        return wakeupWord;
    }

    /**
     * 配置自定义唤醒词功能；配合唤醒词分数使用可增强唤醒词识别，同时结果中过滤所传唤醒词；
     * 如果同时设置commonWakeupWord，仍会过滤唤醒词，但唤醒词的增强识别会生效
     *
     * @param wakeupWord 唤醒词
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

    /**
     * 是否是对话用的ASR，用于对话使用，使用单独的请求参数
     *
     * @param dmAsrJson         dmAsrjson
     * @param useDmAsr          use dm asr
     * @param dmWssCustomparams 是否使用鉴权参数
     */
    public void setUseDmAsr(boolean useDmAsr, JSONObject dmAsrJson, String dmWssCustomparams) {
        this.useDmAsr = useDmAsr;
        this.dmAsrJson = dmAsrJson;
        this.wssDMCustomParams = dmWssCustomparams;
    }

    public String getWssDMCustomParams() {
        return wssDMCustomParams;
    }

    public JSONObject getDmAsrJson() {
        return dmAsrJson;
    }

    /**
     * 设置请求级热词,一般和用户的训练集一起搭配使用。如果已经关联了对应的模型，则不需要设置模型ID
     * {@link AICloudASRIntent#setLmId(String)}
     * {@link AICloudASRIntent#setLmList(String[])}
     * {@code
     * aiCloudASRIntent.setPhraseHints(new PhraseHints[]{new PhraseHints("city", new String[]{"海上明月生"})});}
     *
     * @param phraseHints 请求级热词列表
     */
    public void setPhraseHints(PhraseHints[] phraseHints) {
        this.phraseHints = phraseHints;
    }

    public void setPhrasesList(Phrase[] phrases) {
        this.phrases = phrases;
    }

    public JSONArray getPhrasesList() {
        if (phrases == null)
            return null;
        JSONArray jsonArray = new JSONArray();
        for (int i = 0; i < phrases.length; i++) {
            Phrase item = phrases[i];
            if (item != null)
                jsonArray.put(item.toJSON());
        }
        return jsonArray;
    }

    public IFespxEngine getFespxEngine() {
        return fespxEngine;
    }

    /**
     * 设置关联 IFespxEngine 实例，只在使用内部录音机且多麦模式下才需要设置
     *
     * @param fespxEngine 引擎实例
     * @throws RuntimeException 内部录音机且多麦模式下没设置
     */
    public void setFespxEngine(IFespxEngine fespxEngine) {
        this.fespxEngine = fespxEngine;
    }

    public String[] getHotWords() {
        return hotWords;
    }

    /**
     * 设置产品级热词，比如"你好小驰"设置之后，识别的同音词都会被识别为 你好小驰 ["你好小驰","思必驰"]
     *
     * @param hotWords 产品级热词列表
     */
    public void setHotWords(String[] hotWords) {
        this.hotWords = hotWords;
    }

    /**
     * 设置保存的音频路径，最终的音频路径为path + recordId + ".ogg"
     *
     * @param saveAudioPath     文件路径
     * @param saveOriginalAudio 是否保存从start到stop所有的音频，default is false
     */
    public void setSaveAudioPath(String saveAudioPath, boolean saveOriginalAudio) {
        this.saveAudioPath = saveAudioPath;
        this.saveOriginalAudio = saveOriginalAudio;
    }

    /**
     * 设置是否使用oneshot功能，以及oneshot功能的唤醒词和命令词之间的时间间隔阈值，
     * 如果小于该阈值，就认为不是oneshot，如果大于该阈值，就认为是oneshot
     *
     * @param useOneShot         true 使用 oneshot功能，false 不使用
     * @param intervalTimeThresh useOneShot 为 true 时才有效，单位:毫秒，默认为600ms。
     */
    public void setUseOneShot(boolean useOneShot, int intervalTimeThresh) {
        this.useOneShot = useOneShot;
        this.intervalTimeThresh = intervalTimeThresh;
    }

    public int getNoSpeechTimeOut() {
        return noSpeechTimeOut;
    }

    /**
     * 设置无语音超时时长，单位毫秒，默认值为5000ms ；如果达到该设置值时，自动停止录音
     * 设置为0表示不进行语音超时判断
     *
     * @param milliSecond 超时时长，单位毫秒
     * @see CloudASRParams#setNoSpeechTimeout(int)
     */
    public void setNoSpeechTimeOut(int milliSecond) {
        this.noSpeechTimeOut = milliSecond;
    }

    public int getMaxSpeechTimeS() {
        return maxSpeechTimeS;
    }

    /**
     * 设置音频最大录音时长，达到该值将取消语音引擎并抛出异常<br>
     * 允许的最大录音时长 单位秒
     *
     * <ul>
     * <li>0 表示无最大录音时长限制</li>
     * <li>默认大小为60S</li>
     * </ul>
     *
     * @param seconds seconds
     * @see CloudASRParams#setMaxSpeechTimeS(int)
     */
    public void setMaxSpeechTimeS(int seconds) {
        this.maxSpeechTimeS = seconds;
    }

    public String getSaveAudioPath() {
        return saveAudioPath;
    }

    /**
     * 设置保存的音频路径，最终的音频路径为path + recordId + ".ogg"
     *
     * @param saveAudioPath 文件路径
     */
    public void setSaveAudioPath(String saveAudioPath) {
        this.saveAudioPath = saveAudioPath;
    }

    public boolean isSaveOriginalAudio() {
        return saveOriginalAudio;
    }

    public int getWaitingTimeout() {
        return waitingTimeout;
    }

    /**
     * 设置等待识别结果超时时间 单位毫秒，小于或等于0则不设置超时，默认5000ms.
     * 从vad结束或者用户主动调用stop方法开始计时
     *
     * @param waitingTimeout 超时时长
     */
    public void setWaitingTimeout(int waitingTimeout) {
        this.waitingTimeout = waitingTimeout;
    }

    public int getIntervalTimeThresh() {
        return intervalTimeThresh;
    }

    public boolean isUseOneShot() {
        return useOneShot;
    }

    /**
     * 是否使用oneshot功能,default is false  <br>
     * true 使用one shot功能，唤醒后立马起识别 <br>
     * false 不使用oneshot，合成后启动识别，或只是启动识别
     *
     * @param useOneShot true 使用 oneshot功能，false 不使用
     */
    public void setUseOneShot(boolean useOneShot) {
        setUseOneShot(useOneShot, 600);
    }

    public int getPauseTime() {
        return pauseTime;
    }

    /**
     * 设置VAD右边界
     *
     * @param pauseTime pauseTime 单位为ms,默认为300ms
     */
    public void setPauseTime(int pauseTime) {
        this.pauseTime = pauseTime;
    }

    public String[] getLmList() {
        return lmList;
    }

    /**
     * 设置用户定制的语言模型列表
     *
     * @param lmList 语言模型列表
     */
    public void setLmList(String[] lmList) {
        this.lmList = lmList;
    }

    /**
     * 设置自定义唤醒词和 是否过滤句首唤醒词，用于 oneshot 功能。
     * 比如音频输入为"你好小驰，今天天气怎么样"，filter 为 true 后识别结果即为"今天天气怎么样"
     *
     * @param customWakeupWord 自定义唤醒词 ["你好小弛"]
     * @param filter           false (default)不过滤， true 过滤
     */
    public void setCustomWakeupWord(List<String> customWakeupWord, boolean filter) {
        if (customWakeupWord != null && !customWakeupWord.isEmpty()) {
            this.customWakeupWord = new JSONArray(customWakeupWord);
        } else
            this.customWakeupWord = null;
        this.wakeupWordFilter = filter;
    }

    public String getLanguage() {
        return language;
    }

    /**
     * 指定语种，默认中文("zh-cn")。需要 res 支持语种
     *
     * @param language 语种
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     * 设置是否自行feed数据,不使用内部录音机(包括MockRecord和AIAudioRecord)
     * feed 的音频如果不是pcm音频，则不能使用 vad 功能
     *
     * @param useCustomFeed 设置是否自行feed数据，默认false
     * @param encodedAudio  feed的音频是否是编码成 MP3 OGG OPUS OGG_OPUS 等音频的
     */
    public void setUseCustomFeed(boolean useCustomFeed, boolean encodedAudio) {
        this.useCustomFeed = useCustomFeed;
        this.encodedAudio = encodedAudio;
        if (!this.useCustomFeed && this.encodedAudio) {
            Log.e("AICloudASRIntent", "encodedAudio set error, and set encodedAudio false");
            this.encodedAudio = false;
        }
    }

    public boolean isEncodedAudio() {
        return encodedAudio;
    }

    public String getServer() {
        return server;
    }

    /**
     * 设置服务器地址，默认不用设置
     *
     * @param server 服务器地址，包含ws://
     */
    public void setServer(String server) {
        this.server = server;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * 设置用户标示
     *
     * @param userId 用户标示
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getLmId() {
        return lmId;
    }

    /**
     * 设置识别lmid
     *
     * @param lmId 　custom　lmid
     */
    public void setLmId(String lmId) {
        this.lmId = lmId;
    }

    public boolean isEnablePunctuation() {
        return enablePunctuation;
    }

    /**
     * 设置是否启用标点符号识别
     *
     * @param enablePunctuation 默认为false
     */
    public void setEnablePunctuation(boolean enablePunctuation) {
        this.enablePunctuation = enablePunctuation;
    }

    public boolean isEnableNumberConvert() {
        return enableNumberConvert;
    }

    /**
     * 设置是否启用识别结果汉字数字转阿拉伯数字功能
     *
     * @param enableNumberConvert 默认为false
     */
    public void setEnableNumberConvert(boolean enableNumberConvert) {
        this.enableNumberConvert = enableNumberConvert;
    }

    public int getSelfCustomWakeupScore() {
        return selfCustomWakeupScore;
    }

    /**
     * 自定义唤醒词自定义分数设置，因该参数会影响识别唤醒词的效果同时会增加误识别风险，
     * 建议在专业人士指导下定制自定义分数（建议给-5），
     * 该项可以配合 wakeupWord/customWakeupWord 使用
     *
     * @param selfCustomWakeupScore 自定义唤醒词得分
     */
    public void setSelfCustomWakeupScore(int selfCustomWakeupScore) {
        this.selfCustomWakeupScore = selfCustomWakeupScore;
    }

    public JSONArray getCustomWakeupWord() {
        return customWakeupWord;
    }

    /**
     * 设置自定义唤醒词，可用于过滤和指定唤醒词识别比如 ["你好小乐","你好小白"]
     *
     * @param customWakeupWord customWakeupWord
     */
    public void setCustomWakeupWord(List<String> customWakeupWord) {
        setCustomWakeupWord(customWakeupWord, false);
    }

    public boolean isWakeupWordFilter() {
        return wakeupWordFilter;
    }

    public boolean isEnableTone() {
        return enableTone;
    }

    /**
     * 设置音调功能，默认为false,关闭
     *
     * @param enableTone enableTone
     */
    public void setEnableTone(boolean enableTone) {
        this.enableTone = enableTone;
    }

    public boolean isEnableLanguageClassifier() {
        return enableLanguageClassifier;
    }

    /**
     * 设置语言分类功能，默认为false，关闭
     *
     * @param enableLanguageClassifier enableLanguageClassifier
     * @deprecated 无效参数，废弃
     */
    @Deprecated
    public void setEnableLanguageClassifier(boolean enableLanguageClassifier) {
        this.enableLanguageClassifier = enableLanguageClassifier;
    }

    public boolean isEnableSNTime() {
        return enableSNTime;
    }

    /**
     * 设置rec结果增加对齐信息，默认为false,关闭
     *
     * @param enableSNTime enableSNTime
     */
    public void setEnableSNTime(boolean enableSNTime) {
        this.enableSNTime = enableSNTime;
    }

    public String getResourceType() {
        return resourceType;
    }

    /**
     * 设置识别引擎的资源类型,默认为comm
     * 通用：comm  车载：aicar  机器人：airobot 家居：aihome  英文：aienglish 中英混：aienglish-mix 电视：aitv
     *
     * @param type 资源类型
     */
    public void setResourceType(String type) {
        this.resourceType = type;
    }

    public boolean isRealback() {
        return realback;
    }

    /**
     * 设置是否使用实时反馈功能
     *
     * @param realback realback 默认为false
     */
    public void setRealback(boolean realback) {
        this.realback = realback;
    }

    public boolean isCloudVadEnable() {
        return cloudVadEnable;
    }

    /**
     * 设置是否开启服务端的vad功能，一般近场关闭，远场打开
     *
     * @param cloudVadEnable cloudVadEnable 默认为true
     */
    public void setCloudVadEnable(boolean cloudVadEnable) {
        this.cloudVadEnable = cloudVadEnable;
    }

    public int getNbest() {
        return nbest;
    }

    /**
     * 设置 nbest
     *
     * @param nbest nbest，默认为0
     */
    public void setNbest(int nbest) {
        this.nbest = nbest;
    }

    public void setEnableAlignment(boolean enableAlignment) {
        this.enableAlignment = enableAlignment;
    }

    public void setEnableAudioDetection(boolean enableAudioDetection) {
        this.enableAudioDetection = enableAudioDetection;
    }

    public boolean isEnableAlignment() {
        return enableAlignment;
    }

    public boolean isEnableAudioDetection() {
        return enableAudioDetection;
    }

    public boolean isOneshotOptimization() {
        return oneshotOptimization;
    }

    /**
     * 初始化前设置有效，oneshot 功能优化，当用户说 唤醒词+命令词 时，vad在唤醒词后即结束，导致asr识别结果是空，
     * 可打开此功能，此功能会保留唤醒词后vad结束后的音频，即命令词的音频，然后重新asr识别
     *
     * @param oneshotOptimization true 优化，false 不优化（default）
     */
    public void setOneshotOptimization(boolean oneshotOptimization) {
        this.oneshotOptimization = oneshotOptimization;
    }

    /**
     * 初始化前设置有效，oneshot 功能优化，当用户说 唤醒词+命令词 时，vad在唤醒词后即结束，导致asr识别结果是空，
     * 可打开此功能，此功能会保留唤醒词后vad结束后的音频，即命令词的音频，然后重新asr识别。
     *
     * @param oneshotOptimization            true 优化，false 不优化（default）
     * @param oneshotOptimizationFilterWords oneshot优化功能打开后，第一轮识别时可以过滤一些无意义的词，比如：呃，嗯
     */
    public void setOneshotOptimization(boolean oneshotOptimization, String[] oneshotOptimizationFilterWords) {
        this.oneshotOptimization = oneshotOptimization;
        this.oneshotOptimizationFilterWords = oneshotOptimizationFilterWords;
    }

    public String[] getOneshotOptimizationFilterWords() {
        return oneshotOptimizationFilterWords;
    }

    public PCM_ENCODE_TYPE getAudioType() {
        return audioType;
    }

    /**
     * 设置 PCM 编码成其它格式,以减小传输大小。
     * 云端识别需要将PCM音频编译成其它音频格式发送给服务器。
     *
     * @param audioType PCM 编码成其它格式
     */
    public void setAudioType(PCM_ENCODE_TYPE audioType) {
        this.audioType = audioType;
    }

    public synchronized Map<String, Object> getExtraParam() {
        return extraParam;
    }

    /**
     * 设置额外的参数
     *
     * @param extraParam 额外的参数
     */
    public synchronized void setExtraParam(Map<String, Object> extraParam) {
        this.extraParam = extraParam;
    }


    // asrPlus ==>
    // private String groupId;  // 暂不使用

    /**
     * 设置额外的参数,以 key:value 的形式发送给服务器。
     * 例如:["abc":"ABC","num":123,"bb":false,"list":["a","1","c"]]
     *
     * @param key   key，例如："abc" "num" "list"
     * @param value value，例如："ABC" 123 false ["a","1","c"]
     */
    public synchronized void putExtraParam(String key, Object value) {
        if (extraParam == null)
            extraParam = new HashMap<>();
        extraParam.put(key, value);
    }

    public boolean isEnableConfidence() {
        return enableConfidence;
    }

    /**
     * 设置置信度
     *
     * @param enableConfidence 置信度，默认为true
     */
    public void setEnableConfidence(boolean enableConfidence) {
        this.enableConfidence = enableConfidence;
    }

    public Boolean getLocalVadEnable() {
        return localVadEnable;
    }

    /**
     * 设置是否启用本地vad。启用前提是 {@link com.aispeech.export.config.AICloudASRConfig#isLocalVadEnable} 为true，否则该参数无效。
     * 此参数可设置本轮start的引擎是否使用vad功能
     *
     * @param localVadEnable null 使用和{@link com.aispeech.export.config.AICloudASRConfig#isLocalVadEnable}一样的设置, true 使用Vad, false 禁止Vad
     */
    public void setLocalVadEnable(Boolean localVadEnable) {
        this.localVadEnable = localVadEnable;
    }

    public boolean isIgnoreEmptyResult() {
        return ignoreEmptyResult;
    }

    /**
     * 设置忽略无结果的返回值，如果设置为true,则遇到返回值为空则会自动重启引擎，直接进入下一轮识别
     *
     * @param ignoreEmptyResult 是否忽略空结果，默认为false
     */
    public void setIgnoreEmptyResult(boolean ignoreEmptyResult) {
        this.ignoreEmptyResult = ignoreEmptyResult;
    }

    public int getIgnoreEmptyResultCounts() {
        return ignoreEmptyResultCounts;
    }

    /**
     * 设置忽略无结果的返回值，如果设置为true,则遇到返回值为空则会自动重启引擎，直接进入下一轮识别
     *
     * @param ignoreEmptyResult       是否忽略空结果，默认为false
     * @param ignoreEmptyResultCounts 忽略空结果的重试次数，默认为3
     */
    public void setIgnoreEmptyResult(boolean ignoreEmptyResult, int ignoreEmptyResultCounts) {
        this.ignoreEmptyResult = ignoreEmptyResult;
        this.ignoreEmptyResultCounts = ignoreEmptyResultCounts;
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
    public String toString() {
        return "AICloudASRIntent{" +
                "pauseTime=" + pauseTime +
                ", noSpeechTimeOut=" + noSpeechTimeOut +
                ", maxSpeechTimeS=" + maxSpeechTimeS +
                ", saveAudioPath='" + saveAudioPath + '\'' +
                ", saveOriginalAudio=" + saveOriginalAudio +
                ", waitingTimeout=" + waitingTimeout +
                ", intervalTimeThresh=" + intervalTimeThresh +
                ", useOneShot=" + useOneShot +
                ", enableEmotion=" + enableEmotion +
                ", phraseHints=" + Arrays.toString(phraseHints) +
                ", fespxEngine=" + fespxEngine +
                ", enableConfidence=" + enableConfidence +
                ", localVadEnable=" + localVadEnable +
                ", server='" + server + '\'' +
                ", userId='" + userId + '\'' +
                ", lmId='" + lmId + '\'' +
                ", lmList=" + Arrays.toString(lmList) +
                ", enablePunctuation=" + enablePunctuation +
                ", enableNumberConvert=" + enableNumberConvert +
                ", selfCustomWakeupScore=" + selfCustomWakeupScore +
                ", customWakeupWord=" + customWakeupWord +
                ", wakeupWord=" + wakeupWord +
                ", commonWakeupWord=" + commonWakeupWord +
                ", wakeupWordFilter=" + wakeupWordFilter +
                ", enableTone=" + enableTone +
                ", enableLanguageClassifier=" + enableLanguageClassifier +
                ", enableSNTime=" + enableSNTime +
                ", language='" + language + '\'' +
                ", resourceType='" + resourceType + '\'' +
                ", realback=" + realback +
                ", cloudVadEnable=" + cloudVadEnable +
                ", useCustomFeed=" + useCustomFeed +
                ", encodedAudio=" + encodedAudio +
                ", nbest=" + nbest +
                ", oneshotOptimization=" + oneshotOptimization +
                ", oneshotOptimizationFilterWords=" + Arrays.toString(oneshotOptimizationFilterWords) +
                ", audioType=" + audioType +
                ", extraParam=" + extraParam +
                ", serverName='" + serverName + '\'' +
                ", organization='" + organization + '\'' +
                ", users=" + users +
                ", domain=" + domain +
                ", contextId=" + contextId +
                ", cloudVprintVadEnable=" + cloudVprintVadEnable +
                ", minSpeechLength=" + minSpeechLength +
                ", constantContent=" + constantContent +
                ", groupId=" + groupId +
                ", ignoreEmptyResult=" + ignoreEmptyResult +
                ", ignoreEmptyResultCounts=" + ignoreEmptyResultCounts +
                ", enableFirstDec=" + enableFirstDec +
                ", enableFirstDecForce=" + enableFirstDecForce +
                ", enableDialectProcess=" + enableDialectProcess +
                ", enableDialectProcessNlurec=" + enableDialectProcessNlurec +
                ", enableRealBackFastend=" + enableRealBackFastend +
                ", enableRuleCorrect=" + enableRuleCorrect +
                ", enableWordConfi=" + enableWordConfi +
                ", enableNbestConfi=" + enableNbestConfi +
                ", enableTxtSmooth=" + enableTxtSmooth +
                ", enableSeparateNBest=" + enableSeparateNBest +
                ", phrases=" + Arrays.toString(phrases) +
                '}';
    }


    // asrPlus ==>
    /**
     * 声纹的服务名 vpr和sdr
     */
    private String serverName;
    private String organization;
    private String domain = "";
    private String contextId = "";
    private String groupId;
    private List<String> users;
    private boolean cloudVprintVadEnable = true;
    private String constantContent = "";

    /**
     * 最小有效音频长度，单位秒,当前服务配置:
     * sti-sr 1s
     * lti-sr 500ms
     * sdk默认1s，不要使用小于服务器配置的值
     */
    private float minSpeechLength;
    // asrPlus <==


    public String getConstantContent() {
        return constantContent;
    }

    /**
     * 标记语音中已知的固定的文本内容（如唤醒词），算法会特殊处理被标记的音频。在dp-sr、sti-sr 算法中，会触发使用定制的文本相关模型，替代通用的文本无关模型，提升声纹的识别率。
     *
     * @param constantContent 固定的文本内容
     */
    public void setConstantContent(String constantContent) {
        this.constantContent = constantContent;
    }

    /**
     * 设置要验证的声纹信息
     *
     * @param serverName   声纹的服务名 vpr和sdr
     * @param users        要验证的 userId 列表，userId 即注册声纹时的 userId
     * @param organization 用户所在的公司，项目
     */
    public void setVprintInfo(String serverName, List<String> users, String organization) {
        this.serverName = serverName;
        this.users = users;
        this.organization = organization;
    }
    // asrPlus <==

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getServerName() {
        return serverName;
    }

    public String getOrganization() {
        return organization;
    }

    public List<String> getUsers() {
        return users;
    }

    public boolean isCloudVprintVadEnable() {
        return cloudVprintVadEnable;
    }

    public void setCloudVprintVadEnable(boolean cloudVprintVadEnable) {
        this.cloudVprintVadEnable = cloudVprintVadEnable;
    }

    /**
     * 领域（comm/aihome/aitv/aicar/aiphone/airobot/aitranson-cn-16k/aitranson-en-16k）,用于asrplus,和res对应，可不设置
     *
     * @return 返回当前domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * 领域（comm/aihome/aitv/aicar/aiphone/airobot/aitranson-cn-16k/aitranson-en-16k）,用于asrplus,和res对应，可不设置
     *
     * @param domain 设置domain
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * 如果有contextId，服务端认为是同一个验证，会使用保存的cache和新的数据进行加强验证，非必须
     *
     * @return 返回当前contextID，判断是否是同一个验证
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * 如果有contextId，服务端认为是同一个验证，会使用保存的cache和新的数据进行加强验证，非必须
     *
     * @param contextId 用于校验是否是同一个验证
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    /**
     * 最小有效音频长度，单位秒,当前服务配置:
     * sti-sr 1s
     * lti-sr 500ms
     * sdk默认1s，不要使用小于服务器配置的值
     *
     * @return 返回最小有效音频长度
     */
    public float getMinSpeechLength() {
        return minSpeechLength;
    }

    /**
     * 最小有效音频长度，单位秒,当前服务配置:
     * sti-sr 1s
     * lti-sr 500ms
     * sdk默认1s，不要使用小于服务器配置的值
     *
     * @param minSpeechLength 设置最小有效音频长度
     */
    public void setMinSpeechLength(float minSpeechLength) {
        this.minSpeechLength = minSpeechLength;
    }

    /**
     * 音频编码类型
     */
    public enum PCM_ENCODE_TYPE {
        OGG, OGG_OPUS, WAV, MP3, OPUS
    }
}
