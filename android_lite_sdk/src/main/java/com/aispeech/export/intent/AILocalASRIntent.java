package com.aispeech.export.intent;


import android.text.TextUtils;

import com.aispeech.base.IFespxEngine;
import com.aispeech.common.Log;
import com.aispeech.export.ASRMode;
import com.aispeech.export.config.AILocalASRConfig;
import com.aispeech.export.function.IIntent;
import com.aispeech.lite.base.BaseIntent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AILocalASRIntent extends BaseIntent implements IIntent {
    /**
     * 识别引擎模式：
     * - 1 识别模式，识别 1/2 路结果(视具体初始化配置的识别资源) {@link ASRMode#MODE_ASR}
     * - 2 热词模式，热词 1 路结果 {@link ASRMode#MODE_HOTWORD}
     * - 3 增强模式，识别+热词 3 路结果 {@link ASRMode#MODE_ASR_X}
     */
    private ASRMode mode = ASRMode.MODE_ASR;

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
     * 设置是否开启识别中间结果
     */
    private boolean useFrameSplit;

    /**
     * 添加识别结果分割符,如设置 "," 识别结果显示 ： "打,开,天,窗"
     * start 之前设置生效
     */
    private String useDelimiter;

    /**
     * 是否开启置信度
     */
    private boolean useConf = true;
    /**
     * ExpandFn 文件路径,用于动态拆分net.bin文件
     * start 之前传入有效
     */
    private String expandFnPath;
    /**
     * 无语音超时时长，单位毫秒，default is {@value}ms
     * <p> 如果达到该设置值时，自动停止录音并放弃请求识别内核 </p>
     */
    private int noSpeechTimeOut = 5000;

    /**
     * 设置音频最大录音时长，达到该值将取消语音引擎并抛出异常<br>
     * 允许的最大录音时长 单位秒
     * <ul>
     * <li>0 表示无最大录音时长限制</li>
     * <li>default is {@value}s</li>
     * </ul>
     */
    private int maxSpeechTimeS = 60;

    /**
     * 保存的音频路径，最终的音频路径为path + local_asr_+ recordId + ".pcm"
     */
    private String saveAudioPath;

    /**
     * 是否启用基于语法的语义识别
     */
    private boolean useXbnfRec = false;

    /**
     * 是否开启实时反馈
     */
    private boolean useRealBack = false;

    /**
     * 是否开启ngram置信度
     */
    private boolean useHoldConf = true;

    /**
     * 是否开启拼音输出
     */
    private boolean usePinyin = false;

    /**
     * 识别热词列表字符串,比如："北京市,YOU ARE RIGHT,BLUCE,tfboys,天安门博物馆"
     */
    private String dynamicList;

    /**
     * 设置VAD右边界,单位 ms, default is {@value}ms
     */
    private int pauseTime = 300;

    private boolean useFiller = false;
    private float fillerPenaltyScore = 2.0f;
    private String activeDecoderList;
    /**
     * 自定义词和阈值
     */
    private Map<String, Double> customThreshold = new HashMap<>();
    /**
     * 唤醒词列表，用于oneshot场景过滤开头的唤醒词
     */
    public String[] wakeupWords;
    /**
     * 热词词表
     */
    private String words;

    /**
     * 热词黑名单
     */
    private String blackWords;

    /**
     * 设置热词资源置信度阈值，不同资源推荐置信度阈值不一样，可直接联系思必驰技术支持获取推荐阈值
     */
    private double threshold;

    /**
     * 存在英文情况下设置英文置信度(中英文下识别率低，需另外设置置信度)
     */
    private double englishThreshold = 0;

    private IFespxEngine fespxEngine;

    /**
     * 是否启用本地vad, default is null，使用和 AILocalASRConfig 一样的配置
     */
    private Boolean vadEnable = null;

    public IFespxEngine getFespxEngine() {
        return fespxEngine;
    }

    /**
     * 设置是否开启识别中间结果
     *
     * @param enable true 启用,默认为false
     */
    public void enableInterimResult(boolean enable) {
        this.useRealBack = enable;
    }

    private boolean useRawRec = false;

    /**
     * 是否忽略阈值，直接将结果透传
     */
    private boolean isIgnoreThreshold = false;

    private boolean useOneshotJson = true;


    /**
     * 是否使用one_shot_json配置
     *
     * @return true 使用one_shot_json配置,不使用sys.oneshot槽位方式
     * false 使用sys.oneshot槽位方式,不使用one_shot_json配置
     */
    public boolean isUseOneshotJson() {
        return useOneshotJson;
    }

    public void setUseOneshotJson(boolean useOneshotJson) {
        this.useOneshotJson = useOneshotJson;
    }


    /**
     * 设置是否忽略阈值，不管kernel返回的结果是大于还是小于阈值，都将结果返回给上层
     */
    public void setIsIgnoreThreshold(boolean isIgnoreThreshold) {
        this.isIgnoreThreshold = isIgnoreThreshold;
    }

    public boolean getIsIgnoreThreshold() {
        return isIgnoreThreshold;
    }

    /**
     * 开启融合，
     * 并且active_decoder_list需要激活e2e和ngram
     */
    private boolean useE2EFusion;
    /**
     * 置信度
     */
    private double ngramConf = 0.7;
    /**
     * env
     */
    private String env;

    public boolean isUseE2EFusion() {
        return useE2EFusion;
    }

    /**
     * 开启融合，
     * 并且active_decoder_list需要激活e2e和ngram
     * @param useE2EFusion 是否开启融合默认是关闭
     */
    public void setUseE2EFusion(boolean useE2EFusion) {
        this.useE2EFusion = useE2EFusion;
    }

    public double getNgramConf() {
        return ngramConf;
    }

    /**
     * 设置置信度
     * @param ngramConf  置信度
     */
    public void setNgramConf(double ngramConf) {
        this.ngramConf = ngramConf;
    }

    public String getEnv() {
        return env;
    }

    /**
     * env
     * @param env env json串
     */
    public void setEnv(String env) {
        this.env = env;
    }

    /**
     * 激活解码网络
     * 注意：如果unregister后，必须更新此列表，否则 start会失败
     *
     * @param activeDecoderList 解码网络名称列表
     */
    public void setActiveDecoders(List<String> activeDecoderList) {
        StringBuilder builder = new StringBuilder();
        builder.append("\"");
        builder.append("ngram,");
        for (String name : activeDecoderList) {
            if (!TextUtils.isEmpty(name)) {
                builder.append(name + ",");
            }
        }
        builder.deleteCharAt(builder.lastIndexOf(","));
        builder.append("\"");
        this.activeDecoderList = builder.toString();
    }
    /**
     * 设置关联的信号处理引擎AILocalSignalAndWakeupEngine实例，只在使用内部录音机且多麦模式下才需要设置
     *
     * @param fespxEngine 引擎实例
     * @throws RuntimeException 内部录音机且多麦模式下没设置
     */
    public void setFespxEngine(IFespxEngine fespxEngine) {
        this.fespxEngine = fespxEngine;
    }

    /**
     * 设置无语音超时时长，单位毫秒，默认值为5000ms ；如果达到该设置值时，自动停止录音并放弃请求识别内核
     *
     * @param noSpeechTimeOut 超时时长，单位毫秒
     */
    public void setNoSpeechTimeOut(int noSpeechTimeOut) {
        this.noSpeechTimeOut = noSpeechTimeOut;
    }

    /**
     * 设置音频最大录音时长，达到该值将取消语音引擎并抛出异常<br>
     * 允许的最大录音时长 单位秒，取值范围 1-60秒，默认60秒
     *
     * @param maxSpeechTimeS seconds
     */
    public void setMaxSpeechTimeS(int maxSpeechTimeS) {
        if (maxSpeechTimeS > 0 && maxSpeechTimeS <= 60)
            this.maxSpeechTimeS = maxSpeechTimeS;
        else {
            Log.w("AILocalASRIntent", "setMaxSpeechTimeS error: user set " + maxSpeechTimeS + ", use default value");
            this.maxSpeechTimeS = 60;
        }
    }

    /**
     * 设置词表
     *
     * @param words 词表
     */
    public void setWords(String[] words) {
        if (words == null || words.length == 0)
            throw new IllegalArgumentException("empty hot words");
        this.words = transformDynamic(words);
    }


    /**
     * 直接按照内核既定格式设置词表如: \"打开副屏,打开今日头条\"
     *
     * @param words 内核词表
     */
    public void setWords(String words) {

        if (TextUtils.isEmpty(words))
            throw new IllegalArgumentException("empty hot words");

        if (!(words.startsWith("\"") && words.endsWith("\""))) {
            throw new IllegalArgumentException("illegal hot words");
        }

        this.words = words;
    }
    /**
     * 设置唤醒词列表，用于oneshot过滤唤醒词
     *
     * @param wakeupWords 唤醒词列表
     */
    public void setWakeupWords(String[] wakeupWords) {
        this.wakeupWords = wakeupWords;
    }

    public String[] getWakeupWords() {
        return wakeupWords;
    }

    /**
     * 设置热词黑名单
     *
     * @param blackWords 黑名单词表
     */
    public void setBlackWords(String[] blackWords) {

        if (blackWords == null || blackWords.length == 0)
            throw new IllegalArgumentException("empty hot words");

        this.blackWords = transformDynamic(blackWords);

    }

    public void setEnglishThreshold(double englishThreshold) {
        this.englishThreshold = englishThreshold;
    }

    /**
     * 设置置信度阈值
     *
     * @param threshold 阈值
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    /**
     * 自定义单词置信度阈值
     *
     * @param words     词表
     * @param threshold 阈值
     */
    public void setCustomThreshold(String[] words, Double[] threshold) {

        if (words == null || threshold == null || words.length != threshold.length)
            throw new IllegalArgumentException("set custom threshold data inconsistent!");


        for (int i = 0; i < words.length; i++) {
            customThreshold.put(words[i], threshold[i]);
        }
    }

    /**
     * 设置保存的音频路径，最终的音频路径为path + local_asr_+ recordId + ".pcm"
     *
     * @param saveAudioPath 路径
     */
    public void setSaveAudioPath(String saveAudioPath) {
        this.saveAudioPath = saveAudioPath;
    }

    public void setIntervalTimeThresh(int intervalTimeThresh) {
        this.intervalTimeThresh = intervalTimeThresh;
    }

    public void setDynamicList(String dynamicList) {
        this.dynamicList = dynamicList;
    }

    public void setFillerPenaltyScore(float fillerPenaltyScore) {
        this.fillerPenaltyScore = fillerPenaltyScore;
    }

    public Map<String, Double> getCustomThreshold() {
        return customThreshold;
    }

    public void setCustomThreshold(Map<String, Double> customThreshold) {
        this.customThreshold = customThreshold;
    }

    public String getWords() {
        return words;
    }

    public String getBlackWords() {
        return blackWords;
    }

    public void setBlackWords(String blackWords) {
        this.blackWords = blackWords;
    }

    public double getThreshold() {
        return threshold;
    }

    public double getEnglishThreshold() {
        return englishThreshold;
    }


    public String getActiveDecoders() {
        if (TextUtils.isEmpty(activeDecoderList)) {
            activeDecoderList = "\"ngram\"";
        }
        return activeDecoderList;
    }
    /**
     * 获取引擎模式，默认{@link ASRMode#MODE_ASR}
     *
     * @return ASRMode
     */
    public ASRMode getMode() {
        return mode;
    }

    /**
     * 设置引擎模式
     *
     * @param mode 默认{@link ASRMode#MODE_ASR}
     */
    public void setMode(ASRMode mode) {
        this.mode = mode;
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

    public String getUseDelimiter() {
        return useDelimiter;
    }


    /**
     * @deprecated
     */
    public boolean isUseFrameSplit() {
        return useFrameSplit;
    }

    /**
     * 设置是否开启识别中间结果
     *
     * @param useFrameSplit true 启用,默认为false
     * @deprecated
     */
    public void setUseFrameSplit(boolean useFrameSplit) {
        this.useFrameSplit = useFrameSplit;
    }

    /**
     * 添加识别结果分割符,如设置 "," 识别结果显示 ： "打,开,天,窗"
     * start 之前设置生效
     *
     * @param useDelimiter 分割符
     */
    public void setUseDelimiter(String useDelimiter) {
        this.useDelimiter = useDelimiter;
    }

    /**
     * 设置是否开启置信度
     *
     * @param useConf true 启用,默认为true
     */
    public void setUseConf(boolean useConf) {
        this.useConf = useConf;
    }

    /**
     * @return 是否使用rawrec结果(原始识别结果)
     */
    public boolean isUseRawRec() {
        return useRawRec;
    }

    /**
     * @param useRawRec true 使用
     */
    public void setUseRawRec(boolean useRawRec) {
        this.useRawRec = useRawRec;
    }

    public String getExpandFnPath() {
        return expandFnPath;
    }

    /**
     * ExpandFn 文件路径,用于动态拆分net.bin文件
     * start 之前传入有效
     *
     * @param expandFnPath ExpandFn 文件的绝对路径
     *                     slots 文件示例{"slot": [{"name": "DEVICE","path": "device.slot.bin" }, {"name": "WAKEUP_WORD","path": "wakeup_word.slot.bin" }]}
     */
    public void setExpandFnPath(String expandFnPath) {
        this.expandFnPath = expandFnPath;
    }

    /**
     * 设置是否启用基于语法的语义识别
     *
     * @param useXbnfRec true 启用，默认为false
     */
    public void setUseXbnfRec(boolean useXbnfRec) {
        this.useXbnfRec = useXbnfRec;
    }

    /**
     * 设置是否开启实时反馈
     *
     * @param useRealBack useRealBack,默认为false
     */
    public void setUseRealBack(boolean useRealBack) {
        this.useRealBack = useRealBack;
    }

    /**
     * 设置是否开启ngram置信度
     *
     * @param useHoldConf useRealBack,默认为true
     */
    public void setUseHoldConf(boolean useHoldConf) {
        this.useHoldConf = useHoldConf;
    }

    /**
     * 设置是否开启拼音输出
     *
     * @param usePinyin usePinyin,默认为false
     */
    public void setUsePinyin(boolean usePinyin) {
        this.usePinyin = usePinyin;
    }

    /**
     * 设置识别热词列表字符串,比如："北京市,YOU ARE RIGHT,BLUCE,tfboys,天安门博物馆"
     *
     * @param dynamicList 热词列表字符串
     */
    public void setDynamicList(List<String> dynamicList) {
        if (dynamicList == null || dynamicList.isEmpty())
            this.dynamicList = null;
        else {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < dynamicList.size(); i++) {
                sb.append(dynamicList.get(i));
                if (i < dynamicList.size() - 1)
                    sb.append(",");
            }
            this.dynamicList = sb.toString();
        }
    }

    public int getIntervalTimeThresh() {
        return intervalTimeThresh;
    }

    public boolean isUseOneShot() {
        return useOneShot;
    }

    public boolean isUseConf() {
        return useConf;
    }

    public int getNoSpeechTimeOut() {
        return noSpeechTimeOut;
    }

    public int getMaxSpeechTimeS() {
        return maxSpeechTimeS;
    }

    public String getSaveAudioPath() {
        return saveAudioPath;
    }

    public boolean isUseXbnfRec() {
        return useXbnfRec;
    }

    public boolean isUseRealBack() {
        return useRealBack;
    }

    public boolean isUseHoldConf() {
        return useHoldConf;
    }

    public boolean isUsePinyin() {
        return usePinyin;
    }

    public String getDynamicList() {
        return dynamicList;
    }

    public int getPauseTime() {
        return pauseTime;
    }

    /**
     * 设置VAD右边界
     *
     * @param pauseTime pauseTime 单位：ms,默认300
     */
    public void setPauseTime(int pauseTime) {
        this.pauseTime = pauseTime;
    }

    public boolean isUseFiller() {
        return useFiller;
    }

    /**
     * <p>设置是否开启 Filler，用于减少误识别。</p>
     * 例如：asr.xbnf 里有词 “周杰伦的歌”，用户只说了 “周杰伦”。
     * <ul>
     * <li>当不使用 Filler 时，会回调 “周杰伦的歌”</li>
     * <li>当使用 Filler 时，会回调 ""</li>
     * </ul>
     * <p>
     * 当使用<b>热词</b>时，Filler 功能一定会打开，开关设置无效
     * </p>
     *
     * @param useFiller true 使用Filler，false 不使用（default）
     */
    public void setUseFiller(boolean useFiller) {
        this.useFiller = useFiller;
    }

    public float getFillerPenaltyScore() {
        return fillerPenaltyScore;
    }

    /**
     * <p>设置是否开启 Filler，用于减少误识别。</p>
     * 例如：asr.xbnf 里有词 “周杰伦的歌”，用户只说了 “周杰伦”。
     * <ul>
     * <li>当不使用 Filler 时，会回调 “周杰伦的歌”</li>
     * <li>当使用 Filler 时，会回调 ""</li>
     * </ul>
     * <p>
     * 当使用<b>热词</b>时，Filler 功能一定会打开，开关设置无效，惩罚分数设置仍然有效
     * </p>
     *
     * @param useFiller          true 使用Filler，false 不使用（default）
     * @param fillerPenaltyScore 惩罚分数，default is 2.0。使用Filler时这个参数才有效
     */
    public void setUseFiller(boolean useFiller, float fillerPenaltyScore) {
        this.useFiller = useFiller;
        this.fillerPenaltyScore = fillerPenaltyScore;
    }

    public Boolean getVadEnable() {
        return vadEnable;
    }

    /**
     * 设置是否启用本地vad。启用前提是 {@link AILocalASRConfig#isVadEnable()} 为true，否则该参数无效。
     * 此参数可设置本轮start的引擎是否使用vad功能
     *
     * @param vadEnable null 使用和{@link AILocalASRConfig#isVadEnable()}一样的设置, true 使用Vad, false 禁止Vad
     */
    public void setVadEnable(Boolean vadEnable) {
        this.vadEnable = vadEnable;
    }

    /**
     * 转换String[] to 内核要求的字符串格式
     *
     * @param list 词表
     * @return String
     */
    private String transformDynamic(String[] list) {

        StringBuilder buffer = new StringBuilder();

        buffer.append("\"");

        for (String d : list) {
            if (!TextUtils.isEmpty(d)) {
                buffer.append(d + ",");
            }
        }

        buffer.delete(buffer.lastIndexOf(","), buffer.length());

        buffer.append("\"");

        return buffer.toString();
    }

    @Override
    public String toString() {
        return "AILocalASRIntent{" +
                "useCustomFeed=" + useCustomFeed +
                ", intervalTimeThresh=" + intervalTimeThresh +
                ", useOneShot=" + useOneShot +
                ", useConf=" + useConf +
                ", noSpeechTimeOut=" + noSpeechTimeOut +
                ", maxSpeechTimeS=" + maxSpeechTimeS +
                ", saveAudioPath='" + saveAudioPath + '\'' +
                ", useXbnfRec=" + useXbnfRec +
                ", useRealBack=" + useRealBack +
                ", useHoldConf=" + useHoldConf +
                ", usePinyin=" + usePinyin +
                ", dynamicList='" + dynamicList + '\'' +
                ", pauseTime=" + pauseTime +
                ", useFiller=" + useFiller +
                ", fillerPenaltyScore=" + fillerPenaltyScore +
                ", fespxEngine=" + fespxEngine +
                ", vadEnable=" + vadEnable +
                ", customThreshold=" + customThreshold +
                ", words='" + words + '\'' +
                ", blackWords='" + blackWords + '\'' +
                ", threshold=" + threshold +
                ", wakeupWords=" + Arrays.toString(wakeupWords) +
                ", activeDecoderList='" + activeDecoderList + '\'' +
                '}';
    }
}
