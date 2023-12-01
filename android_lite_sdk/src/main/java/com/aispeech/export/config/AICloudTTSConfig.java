package com.aispeech.export.config;

import com.aispeech.lite.base.BaseConfig;

public class AICloudTTSConfig extends BaseConfig {

    /**
     * 是否使用缓存功能, default is {@value}
     */
    private boolean useCache = true;

    public boolean isUseStopCallback() {
        return useStopCallback;
    }

    public void setUseStopCallback(boolean useStopCallback) {
        this.useStopCallback = useStopCallback;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getCacheWordCount() {
        return cacheWordCount;
    }

    public void setCacheWordCount(int cacheWordCount) {
        this.cacheWordCount = cacheWordCount;
    }

    private boolean useStopCallback;
    private int cacheSize = 100;
    private int cacheWordCount = 200;

    /**
     * TTS缓存目录，null 则为默认文件夹
     */
    private String cacheDirectory = null;

    public boolean isUseCache() {
        return useCache;
    }

    public String getCacheDirectory() {
        return cacheDirectory;
    }

    /**
     * 设置是否使用缓存，默认为true <br>
     * 缓存TTS缓存信息和音频文件，存放在应用外部缓存目录下的 ttsCache 文件夹下。
     *
     * @param useCache 是否使用缓存，默认为true
     */
    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    /**
     * 设置是否使用缓存和缓存的文件夹
     *
     * @param useCache       是否使用缓存，默认为true
     * @param cacheDirectory 缓存目录，设置为 null，则为默认缓存目录：应用外部缓存目录下的 ttsCache 文件夹
     */
    public void setUseCache(boolean useCache, String cacheDirectory) {
        this.useCache = useCache;
        this.cacheDirectory = cacheDirectory;
    }

    @Override
    public String toString() {
        return "AICloudTTSConfig{" +
                "useCache=" + useCache +
                "useStopCallback=" + useStopCallback +
                ", cacheDirectory='" + cacheDirectory + '\'' +
                ", cacheWordCount='" + cacheWordCount + '\'' +
                ", cacheSize='" + cacheSize + '\'' +
                '}';
    }

    public static final class Builder extends BaseConfig.Builder {
        private boolean useCache = true;
        private boolean useStopCallback = true;
        private int cacheSize = 100;
        private int cacheWordCount = 200;
        private String cacheDirectory = null;

        /**
         * 设置缓存的文件夹
         *
         * @param cacheDirectory 缓存目录，设置为 null，则为默认缓存目录：应用外部缓存目录下的 ttsCache 文件夹
         */
        public Builder setCacheDirectory(String cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
            return this;
        }

        /**
         * 设置是否使用缓存，默认为true <br>
         * 缓存TTS缓存信息和音频文件，存放在应用外部缓存目录下的 ttsCache 文件夹下。
         *
         * @param useCache 是否使用缓存，默认为true
         * @return Builder.this
         */
        public Builder setUseCache(boolean useCache) {
            this.useCache = useCache;
            return this;
        }

        /**
         * 设置是否在stop之后回调 onSpeechFinish ,默认是true 回调
         *
         * @param useStopCallback stop后是否回调 onSpeechFinish ，需要在init之前设置生效
         * @return Builder.this
         */
        public Builder setUseStopCallback(boolean useStopCallback) {
            this.useStopCallback = useStopCallback;
            return this;
        }

        /**
         * 设置tts缓存数量上限,默认为100
         *
         * @param cacheSize 是否使用缓存，默认为true
         * @return Builder.this
         */
        public Builder setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
            return this;
        }

        /**
         * 设置支持的单词缓存文字个数,默认为200
         *
         * @param wordCount 文字字数
         * @return Builder.this
         */
        public Builder setCacheWordCount(int wordCount) {
            this.cacheWordCount = wordCount;
            return this;
        }

        @Override
        public Builder setTagSuffix(String tagSuffix) {
            return (Builder) super.setTagSuffix(tagSuffix);
        }

        public AICloudTTSConfig build() {
            AICloudTTSConfig aICloudTTSConfig = new AICloudTTSConfig();
            aICloudTTSConfig.useStopCallback = this.useStopCallback;
            aICloudTTSConfig.useCache = this.useCache;
            aICloudTTSConfig.cacheSize = this.cacheSize;
            aICloudTTSConfig.cacheWordCount = this.cacheWordCount;
            aICloudTTSConfig.cacheDirectory = this.cacheDirectory;
            return super.build(aICloudTTSConfig);
        }
    }
}
