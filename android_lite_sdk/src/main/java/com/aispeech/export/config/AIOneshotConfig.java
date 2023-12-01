package com.aispeech.export.config;

import android.text.TextUtils;

/**
 * oneshot config
 *
 * @author hehr
 */
public class AIOneshotConfig {

    /**
     * vad 资源
     */
    private String resBin;
    /**
     * oneshot 回溯音频长度
     */
    private int cacheAudioTime;

    /**
     * oneshot 判定时间间隔
     */
    private int middleTime;
    /**
     * oneshot 唤醒词
     */
    private String[] words;

    public String getResBin() {
        return resBin;
    }

    public int getCacheAudioTime() {
        return cacheAudioTime;
    }

    public int getMiddleTime() {
        return middleTime;
    }

    public String[] getWords() {
        return words;
    }


    private AIOneshotConfig(String resBinFile, int cacheAudioTime, int middleTime, String[] words) {
        this.resBin = resBinFile;
        this.cacheAudioTime = cacheAudioTime;
        this.middleTime = middleTime;
        this.words = words;
    }

    private AIOneshotConfig(Builder builder) {
        this(builder.getResBin(),
                builder.getCacheAudioTime(),
                builder.getMiddleTime(),
                builder.getWords()
        );
    }

    public static class Builder {
        /**
         * vad 资源
         */
        private String resBin;
        /**
         * oneshot 回溯音频长度
         */
        private int cacheAudioTime = 1200;

        /**
         * oneshot 判定时间间隔
         */
        private int middleTime = 600;
        /**
         * oneshot 唤醒词
         */
        private String[] words;

        public String getResBin() {
            return resBin;
        }

        /**
         * 设置vad资源
         *
         * @param resBin vad资源
         * @return {@link Builder}
         */
        public Builder setResBin(String resBin) {
            this.resBin = resBin;
            return this;
        }

        public int getCacheAudioTime() {
            return cacheAudioTime;
        }

        /**
         * 设置回溯音频长度,单位毫秒,默认 1200ms
         *
         * @param cacheAudioTime 回溯音频长度
         * @return {@link Builder}
         */
        public Builder setCacheAudioTime(int cacheAudioTime) {
            this.cacheAudioTime = cacheAudioTime;
            return this;
        }

        public int getMiddleTime() {
            return middleTime;
        }

        /**
         * 设置唤醒后到检测人声时间长度,单位毫秒,默认600ms
         *
         * @param middleTime 唤醒后到检测人声时间长度
         * @return {@link Builder}
         */
        public Builder setMiddleTime(int middleTime) {
            this.middleTime = middleTime;
            return this;
        }

        public String[] getWords() {
            return words;
        }

        /**
         * 设置oneshot检测词汇列表
         *
         * @param words 唤醒词列表
         * @return {@link Builder}
         */
        public Builder setWords(String[] words) {
            this.words = words;
            return this;
        }

        public AIOneshotConfig create() {

            if (TextUtils.isEmpty(getResBin())) {
                throw new IllegalArgumentException("pls set vad res bin path");
            }

            if (words == null || words.length == 0) {
                throw new IllegalArgumentException("pls set oneshot words");
            }

            return new AIOneshotConfig(this);
        }


    }
}
