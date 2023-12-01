package com.aispeech.export.intent;

import android.text.TextUtils;

import com.aispeech.lite.base.BaseIntent;

import java.util.HashMap;
import java.util.Map;

public class AILocalWakeupIncrementIntent extends BaseIntent {

    /**
     * 默认打开
     */
    private boolean useContinuousRecognition = true;
    /**
     * 默认10s
     */
    private int maxSpeechTime = 10;

    /**
     * 默认不开启,0s
     */
    private int noSpeechTime = 0;

    /**
     * 设置保存的音频路径，最终的音频路径为 path + local_asr_+ recordId + ".pcm"
     * 需要在start之前调用
     */
    private String saveAudioPath;

    /**
     * 自定义词和阈值
     */
    private Map<String, Double> customThreshold = new HashMap<>();

    /**
     * 词表
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

    /**
     * 设置是否使用解码网络里增加的filler路径
     */
    private boolean useFiller;

    /**
     * 根据误唤醒集合设置的值
     */
    private double fillerPenaltyScore;

    /**
     * 是否忽略阈值，直接将结果透传
     */
    private boolean isIgnoreThreshold = false;

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
     * 设置是否开启连续识别,默认 true
     * start之前设置有效
     *
     * @param useContinuousRecognition useContinuousRecognition
     */
    public void setUseContinuousRecognition(boolean useContinuousRecognition) {
        this.useContinuousRecognition = useContinuousRecognition;
    }

    /**
     * 设置音频最大录音时长，达到该值将取消语音引擎并抛出异常<br>
     * 允许的最大录音时长 单位秒
     * <ul>
     * <li>0 表示无最大录音时长限制</li>
     * <li>默认大小为10S</li>
     * </ul>
     * 须在start之前设置才生效
     *
     * @param maxSpeechTime 最大人声时长
     */
    public void setMaxSpeechTime(int maxSpeechTime) {
        this.maxSpeechTime = maxSpeechTime;
    }

    /**
     * 设置启动识别后无人声输入最大录音时常，到达该值后将跑出异常
     * 默认 0 s ，热词引擎不开启此配置
     *
     * @param noSpeechTime 无人声时常
     */
    public void setNoSpeechTime(int noSpeechTime) {
        this.noSpeechTime = noSpeechTime;
    }

    /**
     * 设置音频文件存储路径,release 版本建议关闭
     *
     * @param path 文件路径
     */
    public void setSaveAudioPath(String path) {
        this.saveAudioPath = path;
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
     * 设置热词黑名单
     *
     * @param blackWords 黑名单词表
     */
    public void setBlackWords(String[] blackWords) {

        if (blackWords == null || blackWords.length == 0)
            throw new IllegalArgumentException("empty hot words");

        this.blackWords = transformDynamic(blackWords);

    }

    /**
     * 设置置信度阈值
     *
     * @param threshold 阈值
     */
    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setEnglishThreshold(double englishThreshold) {
        this.englishThreshold = englishThreshold;
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
     * 获取NoSpeechTime
     *
     * @return int
     */
    public int getNoSpeechTime() {
        return noSpeechTime;
    }

    /**
     * 获取热词置信度阈值
     *
     * @return double
     */
    public double getThreshold() {
        return threshold;
    }

    public double getEnglishThreshold() {
        return englishThreshold;
    }

    /**
     * 获取单词允许最大录音时长，单位 s
     *
     * @return int
     */
    public int getMaxSpeechTime() {
        return maxSpeechTime;
    }

    /**
     * 获取词表
     *
     * @return String[]
     */
    public String getWords() {
        return words;
    }

    /**
     * 获取音频保存路径
     *
     * @return String
     */
    public String getSaveAudioPath() {
        return saveAudioPath;
    }

    /**
     * 是否支持连续识别，默认打开
     *
     * @return boolean
     */
    public boolean isUseContinuousRecognition() {
        return useContinuousRecognition;
    }

    /**
     * 获取黑名单
     *
     * @return string
     */
    public String getBlackWords() {
        return blackWords;
    }

    /**
     * 获取自定义的词表
     *
     * @return {@link Map}
     */
    public Map<String, Double> getCustomThreshold() {
        return customThreshold;
    }

    /**
     * 转换String [] to 内核要求的字符串格式
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

    public boolean isUseFiller() {
        return useFiller;
    }

    /**
     * 设置是否使用解码网络里增加的filler路径
     *
     * @param useFiller 默认不开启
     */
    public void setUseFiller(boolean useFiller) {
        this.useFiller = useFiller;
    }

    public double getFillerPenaltyScore() {
        return fillerPenaltyScore;
    }

    /**
     * 根据误唤醒集合设置的值
     *
     * @param fillerPenaltyScore 默认 1.5，可根据实际情况再调整
     */
    public void setFillerPenaltyScore(double fillerPenaltyScore) {
        this.fillerPenaltyScore = fillerPenaltyScore;
    }
}
