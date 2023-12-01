package com.aispeech.lite.base;

import com.aispeech.export.bean.VoiceQueueStrategy;

/**
 * Description: 所有config的基类 用于实现一些通用的配置
 * Author: junlong.huang
 * CreateTime: 2023/2/15
 */
public class BaseConfig {

    /**
     * 当前tag后缀，用于区分当前引擎实例用途
     */
    protected String tagSuffix;
    protected VoiceQueueStrategy voiceQueueStrategy = null;


    public String getTagSuffix() {
        return tagSuffix;
    }

    /**
     * 设置引擎实例tag后缀，用于区分不同场景
     *
     * @param tagSuffix
     * @return
     */
    public void setTagSuffix(String tagSuffix) {
        this.tagSuffix = tagSuffix;
    }

    public static class Builder {

        protected String tagSuffix;

        /**
         * 设置引擎实例tag后缀，用于区分不同场景
         *
         * @param tagSuffix
         * @return
         */
        public Builder setTagSuffix(String tagSuffix) {
            this.tagSuffix = tagSuffix;

            return this;
        }


        protected <T extends BaseConfig> T build(T config) {
            config.tagSuffix = this.tagSuffix;

            return config;
        }

    }
}
