package com.aispeech.export.config;


import com.aispeech.lite.Languages;
import com.aispeech.lite.base.BaseConfig;

/**
 * PerjectName: duilite-for-car-android
 * Author: huwei
 * Describe:
 * Since 2020/11/9 09:40
 * Copyright(c) 2019 苏州思必驰信息科技有限公司  www.aispeech.com
 */
public class AIWakeupConfig extends BaseConfig {

    /**
     * 唤醒资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     */
    private String wakeupResource;


    /**
     * 设置oneshot功能下需要缓存的音频时长,根据具体的硬件性能和唤醒词长度调节。
     * 单位：毫秒。default is {@value}ms
     */
    private int oneShotCacheTime = 1200;

    /**
     * oneShot配置信息
     */
    private AIOneshotConfig oneshotConfig;


    private boolean preWakeupOn = false;


    private Languages languages;

    /**
     * 设置唤醒资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param wakeupResource 唤醒资源
     */
    public void setWakeupResource(String wakeupResource) {
        this.wakeupResource = wakeupResource;
    }

    /**
     * 设置oneshot功能下需要缓存的音频时长,根据具体的硬件性能和唤醒词长度调节。
     *
     * @param oneShotCacheTime 单位毫秒， 默认为1200ms
     */
    public void setOneShotCacheTime(int oneShotCacheTime) {
        this.oneShotCacheTime = oneShotCacheTime;
    }


    public String getWakeupResource() {
        return wakeupResource;
    }

    public int getOneShotCacheTime() {
        return oneShotCacheTime;
    }


    public AIOneshotConfig getOneshotConfig() {
        return oneshotConfig;
    }

    public boolean isPreWakeupOn() {
        return preWakeupOn;
    }


    public Languages getLanguages() {
        return languages;
    }

    public static final class Builder extends BaseConfig.Builder {
        private String resBinName;
        private AIOneshotConfig oneshotConfig;
        private boolean preWakeupOn = false;
        private Languages languages;
        /**
         * 设置oneshot功能下需要缓存的音频时长,根据具体的硬件性能和唤醒词长度调节。
         * 单位：毫秒。default is {@value}ms
         */
        private int oneShotCacheTime = 1200;

        public Builder setOneShotCacheTime(int oneShotCacheTime) {
            this.oneShotCacheTime = oneShotCacheTime;
            return this;
        }

        public Builder setResBinName(String resBinName) {
            this.resBinName = resBinName;
            return this;
        }

        public Builder setOneshotConfig(AIOneshotConfig oneshotConfig) {
            this.oneshotConfig = oneshotConfig;
            return this;
        }

        public Builder setPreWakeupOn(boolean preWakeupOn) {
            this.preWakeupOn = preWakeupOn;
            return this;
        }

        public Builder setLanguages(Languages languages) {
            this.languages = languages;
            return this;
        }

        @Override
        public Builder setTagSuffix(String tagSuffix) {
            return (Builder) super.setTagSuffix(tagSuffix);
        }


        public AIWakeupConfig build() {
            AIWakeupConfig aIWakeupConfig = new AIWakeupConfig();
            aIWakeupConfig.preWakeupOn = this.preWakeupOn;
            aIWakeupConfig.oneshotConfig = this.oneshotConfig;
            aIWakeupConfig.languages = this.languages;
            aIWakeupConfig.wakeupResource = this.resBinName;
            return super.build(aIWakeupConfig);
        }
    }


    @Override
    public String toString() {
        return "AIWakeupConfig{" +
                "wakeupResource='" + wakeupResource + '\'' +
                ", oneShotCacheTime=" + oneShotCacheTime +
                '}';
    }
}
