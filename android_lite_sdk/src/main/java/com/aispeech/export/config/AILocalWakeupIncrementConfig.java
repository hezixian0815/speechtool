package com.aispeech.export.config;

import android.text.TextUtils;

import com.aispeech.export.widget.Scene;
import com.aispeech.lite.Languages;

public class AILocalWakeupIncrementConfig {

    /**
     * 设置是否启用vad
     */
    public boolean useVad;

    /**
     * 是否启用ssl功能
     */
    public boolean useSSL;
    /**
     * 设置VAD资源名，适用于VAD资源放置在自定义目录下
     * 须在init之前设置才生效
     */
    public String vadRes;

    /**
     * 设置识别声学资源名
     */
    public String asrRes;

    /***
     * 设置grammer编译资源
     */
    public String grammerRes;

    /***
     * 设置场景
     */
    public Scene[] scenes;

    /**
     * ExpandPath 生成slot.bin的路径
     */
    public String expandPath;

    /**
     * 设置热词语种，默认中文
     */
    public Languages languages;

    /**
     * 设置vad的pauseTime
     */
    public int pauseTime;

    private AILocalWakeupIncrementConfig(boolean useVad, String vadRes, String asrRes, String grammerRes, Scene[] scenes, String expandPath, Languages languages, boolean useSSL, int pauseTime) {
        this.useVad = useVad;
        this.vadRes = vadRes;
        this.asrRes = asrRes;
        this.languages = languages;
        this.useSSL = useSSL;
        this.grammerRes = grammerRes;
        this.scenes = scenes;
        this.expandPath = expandPath;
        this.pauseTime = pauseTime;
    }

    private AILocalWakeupIncrementConfig(Builder builder) {
        this(builder.useVad, builder.vadRes, builder.asrRes, builder.grammarRes, builder.scenes, builder.expandPath, builder.languages, builder.useSSL, builder.pauseTime);
    }

    public static class Builder {

        /**
         * 设置是否启用vad
         */
        public boolean useVad = true;

        /**
         * 是否启用ssl功能
         */
        public boolean useSSL = false;

        /**
         * 设置VAD资源名，适用于VAD资源放置在自定义目录下
         * 须在init之前设置才生效
         *
         * @param path vad资源名全路径
         */
        String vadRes;

        /**
         * 设置识别声学资源名
         */
        String asrRes;

        /**
         * 设置grammar资源名
         */
        String grammarRes;

        /***
         * 设置 scenes 资源
         */
        Scene[] scenes;

        /**
         * 生成slot.bin的绝对路径
         */
        String expandPath;

        /**
         * 设置热词语种，默认中文
         */
        Languages languages = Languages.CHINESE;

        /**
         * 设置vad的pauseTime
         */
        private int pauseTime;

        /**
         * 设置是否启用vad
         *
         * @param useVad boolean
         * @return {@link Builder}
         */
        public Builder setUseVad(boolean useVad) {
            this.useVad = useVad;
            return this;
        }

        /**
         * 设置是否启用ssl功能,启用该功能,需要外部自行feed 4通道音频数据。
         *
         * @param useSSL boolean
         * @return {@link Builder}
         */
        public Builder setUseSSL(boolean useSSL) {
            this.useSSL = useSSL;
            return this;
        }

        /**
         * 设置vad res
         *
         * @param vadRes String
         * @return {@link Builder}
         */
        public Builder setVadRes(String vadRes) {
            this.vadRes = vadRes;
            return this;
        }

        /**
         * 设置识别资源
         *
         * @param asrRes String
         * @return {@link Builder}
         */
        public Builder setAsrRes(String asrRes) {
            this.asrRes = asrRes;
            return this;
        }

        /***
         * 设置grammar资源
         * @param grammarRes String
         * @return {@link Builder}
         */
        public Builder setGrammarRes(String grammarRes) {
            this.grammarRes = grammarRes;
            return this;
        }


        /**
         * 设置 net.bin资源名
         *
         * @param scenes String
         * @return {@link Builder}
         */
        public Builder setScenes(Scene[] scenes) {
            this.scenes = scenes;
            return this;
        }


        /**
         * 设置 net.bin资源名
         *
         * @param expandFnPath String
         * @return {@link Builder}
         */
        public Builder setExpandFnPath(String expandFnPath) {
            this.expandPath = expandFnPath;
            return this;
        }

        /**
         * 设置热词语种，默认 {@link Languages#CHINESE}
         *
         * @param languages {@link Languages}
         * @return {@link AILocalHotWordConfig.Builder}
         */
        public Builder setLanguages(Languages languages) {
            this.languages = languages;
            return this;
        }

        /**
         * 设置vad的pauseTime
         *
         * @param pauseTime
         * @return {@link AILocalHotWordConfig.Builder}
         */
        public Builder setVadPauseTime(int pauseTime) {
            this.pauseTime = pauseTime;
            return this;
        }

        public AILocalWakeupIncrementConfig build() {
            checkInvalid();
            return new AILocalWakeupIncrementConfig(this);
        }

        /**
         * 检测配置是否存在不允许的情况
         */
        private void checkInvalid() {
            if (TextUtils.isEmpty(asrRes)) {
                throw new IllegalArgumentException("must set asr res!");
            }

            if ((useVad || useSSL) && TextUtils.isEmpty(vadRes)) {
                throw new IllegalArgumentException("use vad or use ssl must set vad res!");
            }
        }
    }
}
