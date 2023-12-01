package com.aispeech.export.config;

import android.text.TextUtils;

import com.aispeech.lite.Languages;
import com.aispeech.lite.base.BaseConfig;

/**
 * 本地热词初始化参数
 *
 * @author hehr
 */
public class AILocalHotWordConfig extends BaseConfig {

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

    /**
     * 设置热词语种，默认中文
     */
    public Languages languages;

    private AILocalHotWordConfig(boolean useVad, String vadRes, String asrRes,Languages languages, boolean useSSL) {
        this.useVad = useVad;
        this.vadRes = vadRes;
        this.asrRes = asrRes;
        this.languages = languages;
        this.useSSL = useSSL;
    }

    private AILocalHotWordConfig(Builder builder) {
        this(builder.useVad, builder.vadRes, builder.asrRes, builder.languages, builder.useSSL);
    }

    public static class Builder extends BaseConfig.Builder {

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
         * 设置热词语种，默认中文
         */
        Languages languages = Languages.CHINESE;

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

        /**
         * 设置热词语种，默认 {@link Languages#CHINESE}
         *
         * @param languages {@link Languages}
         * @return {@link Builder}
         */
        public Builder setLanguages(Languages languages) {
            this.languages = languages;
            return this;
        }

        @Override
        public Builder setTagSuffix(String tagSuffix) {
            return (Builder) super.setTagSuffix(tagSuffix);
        }

        public AILocalHotWordConfig build() {
            checkInvalid();
            return super.build(new AILocalHotWordConfig(this));
        }

        /**
         * 检测配置是否存在不允许的情况
         */
        private void checkInvalid() {
            if (TextUtils.isEmpty(asrRes)) {
                throw new IllegalArgumentException("must set asr res!");
            }
//            if (useSSL && !useCustomFeed) {
//                throw new IllegalArgumentException("use ssl must custom feed!");
//            }
            if ((useVad || useSSL) && TextUtils.isEmpty(vadRes)) {
                throw new IllegalArgumentException("use vad or use ssl must set vad res!");
            }
        }
    }
}
