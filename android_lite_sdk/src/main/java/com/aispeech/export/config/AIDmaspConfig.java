package com.aispeech.export.config;

import com.aispeech.common.ArrayUtils;
import com.aispeech.common.PinYinUtils;

public class AIDmaspConfig {

    /**
     * 唤醒词信息
     */
    private WakeupWord wakeupWord;

    /**
     * 唤醒资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     */
    private String wakeupResource;

    private String dmaspResource;

    private String[] blackWords;

    public float[] blackWordsThreshold;

    private int dmaspChannelCount;
    /**
     * 设置多音区是否需要进行动态对齐
     */
    private boolean isDynamicAlignment = true;

    public WakeupWord getWakeupWord() {
        return wakeupWord;
    }

    public String getWakeupResource() {
        return wakeupResource;
    }

    public String getDmaspResource() {
        return dmaspResource;
    }

    public String[] getBlackWords() {
        return blackWords;
    }

    public int getDmaspChannelCount() {
        return dmaspChannelCount;
    }

    public boolean isDynamicAlignment() {
        return isDynamicAlignment;
    }

    private AIDmaspConfig(WakeupWord wakeupWord, String wakeupResource, String dmaspResource, int dmaspChannelCount, String[] blackWords, float[] blackWordsThreshold) {
        this.wakeupWord = wakeupWord;
        this.wakeupResource = wakeupResource;
        this.dmaspResource = dmaspResource;
        this.dmaspChannelCount = dmaspChannelCount;
        this.blackWords = blackWords;
        this.blackWordsThreshold = blackWordsThreshold;
    }

    private AIDmaspConfig(Builder builder) {
        this(builder.getWakeupWord(), builder.getWakeupResource(), builder.getDmaspResource(), builder.getDmaspChannelCount(), builder.getBlackWords(), builder.blackWordsThreshold);
    }

    /**
     * 唤醒词
     */
    public static class WakeupWord {

        /**
         * 设置唤醒词
         */
        public String[] pinyin;

        /**
         * 设置唤醒词对应阈值，是否需要设置和唤醒资源有关系
         */
        public float[] threshold;

        /**
         * 设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0]
         */
        public int[] majors;

        /**
         * 设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验，如 [1,0,0]
         */
        public int[] dcheck;

        /**
         * 唤醒词构造函数
         *
         * @param pinyin    唤醒词,如 {"ni hao xiao chi","ni hao a bao"}
         * @param threshold 唤醒阈值,如 {0.1 , 0.2}
         * @param majors    设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0] , 设置主唤醒词,信号处理后的音频会自动回溯音频
         * @param dcheck    设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验，如 [1,0,0]
         * @deprecated 已废弃, 使用另一个构造方法
         */
        public WakeupWord(String[] pinyin, String[] threshold, String[] majors, String[] dcheck) {
            this.pinyin = pinyin;
            this.threshold = ArrayUtils.string2Float(threshold);
            this.majors = ArrayUtils.string2Int(majors);
            this.dcheck = ArrayUtils.string2Int(dcheck);
        }

        /**
         * 唤醒词构造函数
         *
         * @param pinyin    唤醒词,如 {"ni hao xiao chi","ni hao a bao"}
         * @param threshold 唤醒阈值,如 {0.1 , 0.2}
         * @param majors    设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0] , 设置主唤醒词,信号处理后的音频会自动回溯音频
         * @param dcheck    设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验，如 [1,0,0]
         */
        public WakeupWord(String[] pinyin, float[] threshold, int[] majors, int[] dcheck) {
            this.pinyin = pinyin;
            this.threshold = threshold;
            this.majors = majors;
            this.dcheck = dcheck;
        }

        /**
         * 唤醒词构造函数
         *
         * @param pinyin    唤醒词,如 {"ni hao xiao chi","ni hao a bao"}
         * @param threshold 唤醒阈值,如 {0.1 , 0.2}
         * @param majors    设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0] , 设置主唤醒词,信号处理后的音频会自动回溯音频
         */
        public WakeupWord(String[] pinyin, String[] threshold, String[] majors) {
            this.pinyin = pinyin;
            this.threshold = ArrayUtils.string2Float(threshold);
            this.majors = ArrayUtils.string2Int(majors);
        }
    }

    public static class Builder {

        /**
         * 唤醒词信息
         */
        private WakeupWord wakeupWord;

        /**
         * 唤醒词黑名单
         */

        private String[] blackWords;

        /**
         * 唤醒词黑名单阈值
         */
        float[] blackWordsThreshold;

        /**
         * 唤醒资源
         * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
         * <p>2. 如在 assets 里设置为名称</p>
         */
        private String wakeupResource;

        private String dmaspResource;
        /**
         * 设置 Dmasp 输出的通道数 支持 4Mic 2Mic
         * 该数据将影响驾驶模式抛出的单路音频
         * input: int 2、4、6, 默认4路
         */
        private int dmaspChannelCount = 4;

        private boolean isDynamicAlignment = true;

        /**
         * oneShot
         */
        private AIOneshotConfig oneshotConfig;

        public WakeupWord getWakeupWord() {
            return wakeupWord;
        }

        public Builder setWakeupWord(WakeupWord wakeupWord) {
            this.wakeupWord = wakeupWord;
            return this;
        }

        public String getDmaspResource() {
            return dmaspResource;
        }

        public String[] getBlackWords() {
            return blackWords;
        }

        public Builder setDmaspResource(String dmaspResource) {
            this.dmaspResource = dmaspResource;
            return this;
        }

        public String getWakeupResource() {
            return wakeupResource;
        }

        public Builder setWakeupResource(String wakeupResource) {
            this.wakeupResource = wakeupResource;
            return this;
        }

        public Builder setBlackWords(String[] blackWords, float[] threshold) {
            this.blackWords = blackWords;
            this.blackWordsThreshold = threshold;
            return this;
        }

        public boolean isDynamicAlignment() {
            return isDynamicAlignment;
        }

        /**
         * 设置多音区是否需要进行动态对齐
         */
        public Builder setDynamicAlignment(boolean useDynamicAlignment) {
            isDynamicAlignment = useDynamicAlignment;
            return this;
        }

        /**
         * Dmasp 输出的通道数 支持 4Mic 2Mic
         * 该数据将影响驾驶模式抛出的单路音频
         *
         * @param channelCount int 2、4、6, 默认4路
         */
        public Builder setDmaspChannelCount(int channelCount) {
            this.dmaspChannelCount = channelCount;
            return this;
        }

        public int getDmaspChannelCount() {
            return dmaspChannelCount;
        }

        private boolean checkLength(String[] word, float[] threshold, int[] majors, int[] dcheck) {

            if (word.length == threshold.length
                    && word.length == majors.length
                    && word.length == dcheck.length) {
                return true;
            }

            return false;
        }

        private boolean checkLength(String[] word, float[] threshold, int[] majors) {
            if (word.length == threshold.length
                    && word.length == majors.length) {
                return true;
            }

            return false;
        }

        private void checkWakeupWord(WakeupWord word) {

            boolean isLengthValid;

            if (word.dcheck == null) {
                isLengthValid = checkLength(word.pinyin, word.threshold, word.majors);
            } else {
                isLengthValid = checkLength(word.pinyin, word.threshold, word.majors, word.dcheck);
            }

            if (!isLengthValid) {
                throw new IllegalArgumentException(" invalid length ! ");
            }

            PinYinUtils.checkPinyin(word.pinyin);
        }

        public AIDmaspConfig create() {

            checkWakeupWord(this.getWakeupWord());

            return new AIDmaspConfig(this);
        }
    }
}
