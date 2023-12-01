package com.aispeech.export.config;


import com.aispeech.lite.base.BaseConfig;

/**
 * ProjectName: duilite-for-car-android
 * Author: huwei
 * Describe:
 * Since 2021/2/26 10:15
 * Copyright(c) 2019 苏州思必驰信息科技有限公司  www.aispeech.com
 */

public class AILocalVadConfig extends BaseConfig {
    private String vadResource;
    private int pauseTime;
    private int pauseTimeArray[];
    private int multiMode;
    private boolean useFullMode;
    private boolean useDoubleVad;

    public int getMultiMode() {
        return multiMode;
    }

    public String getVadResource() {
        return vadResource;
    }

    public int getPauseTime() {
        return pauseTime;
    }

    public int[] getPauseTimeArray() {
        return pauseTimeArray;
    }

    public boolean isUseFullMode() {
        return useFullMode;
    }

    public boolean isUseDoubleVad() {
        return useDoubleVad;
    }

    public void setVadResource(String vadResource) {
        this.vadResource = vadResource;
    }

    public void setPauseTime(int pauseTime) {
        this.pauseTime = pauseTime;
    }

    public void setUseFullMode(boolean useFullMode) {
        this.useFullMode = useFullMode;
    }

    public void setUseDoubleVad(boolean useDoubleVad) {
        this.useDoubleVad = useDoubleVad;
    }

    public static final class Builder extends BaseConfig.Builder {
        private String vadResource;
        private int pauseTime = 300;
        private int pauseTimeArray[] = new int[]{300,500,800};
        private int multiMode = 0;
        private boolean useFullMode;
        private boolean useDoubleVad = false;

        /**
         * 设置VAD资源路径
         * 需要在init之前调用
         *
         * @param vadResource vad资源名
         * @return Builder
         */
        public Builder setVadResource(String vadResource) {
            this.vadResource = vadResource;
            return this;
        }

        /**
         * 设置VAD右边界
         * 需要在init之前调用
         *
         * @param pauseTime pauseTime 单位为ms,默认为300ms
         * @return Builder
         */
        public Builder setPauseTime(int pauseTime) {
            this.pauseTime = pauseTime;
            return this;
        }

        public Builder setPauseTimeArray(int[] pauseTimeArray) {
            this.pauseTimeArray = pauseTimeArray;
            return this;
        }

        public Builder setMultiMode(int multiMode) {
            this.multiMode = multiMode;
            return this;
        }

        /**
         * 设置是否启用vad常开模式
         * 初始化参数，init之前设置生效
         *
         * @param useFullMode boolean
         * @return Builder
         */
        public Builder setUseFullMode(boolean useFullMode) {
            this.useFullMode = useFullMode;
            return this;
        }

        /**
         * 设置是否启动双VAD模式
         * 初始化参数，init之前设置生效
         *
         * @param useDoubleVad boolean
         * @return Builder
         */
        public Builder setUseDoubleVad(boolean useDoubleVad) {
            this.useDoubleVad = useDoubleVad;
            return this;
        }

        @Override
        public Builder setTagSuffix(String tagSuffix) {
            return (Builder) super.setTagSuffix(tagSuffix);
        }

        public AILocalVadConfig build() {
            AILocalVadConfig aILocalVadConfig = new AILocalVadConfig();
            aILocalVadConfig.vadResource = this.vadResource;
            aILocalVadConfig.pauseTime = this.pauseTime;
            aILocalVadConfig.pauseTimeArray = this.pauseTimeArray;
            aILocalVadConfig.multiMode = this.multiMode;
            aILocalVadConfig.useFullMode = this.useFullMode;
            aILocalVadConfig.useDoubleVad = this.useDoubleVad;
            return super.build(aILocalVadConfig);
        }
    }
}