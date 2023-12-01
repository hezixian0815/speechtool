package com.aispeech.export.config;

import android.text.TextUtils;

import com.aispeech.lite.config.CloudDMConfig;

/**
 * @author hehr
 * 云端语义引擎初始化参数
 */
public class AICloudSemanticConfig {

    /**
     * 是否客户自定义feed 引擎
     */
    private boolean useCustomFeed;
    /**
     * 设置服务地址，默认  wss://dds.dui.ai/dds/v2/"
     */
    private String serverAddress;
    /**
     * 是否启用内置vad
     */
    private boolean useVad;
    /**
     * 设置内置vad资源路径或名称(assets路径下)
     */
    private String vadRes;
    /**
     * 设置产品分支名称
     */
    private String aliasKey;
    /**
     * 是否启用纯语义模式
     */
    private boolean useRefText;

    /**
     * 连接超时
     */
    private int connectTimeout;

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public boolean isUseCustomFeed() {
        return useCustomFeed;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public boolean isUseVad() {
        return useVad;
    }

    public String getVadRes() {
        return vadRes;
    }

    public String getAliasKey() {
        return aliasKey;
    }

    public boolean isUseRefText() {
        return useRefText;
    }

    private AICloudSemanticConfig(Builder builder) {
        this.useRefText = builder.useRefText;
        this.useCustomFeed = builder.useCustomFeed;
        this.serverAddress = builder.serverAddress;
        this.useVad = builder.useVad;
        this.vadRes = builder.vadRes;
        this.aliasKey = builder.aliasKey;
        this.connectTimeout = builder.connectTimeout;
    }

    public static class Builder {

        /**
         * 是否启用纯语义模式
         */
        private boolean useRefText = false;

        /**
         * 是否客户自定义feed 引擎
         */
        private boolean useCustomFeed = false;
        /**
         * 设置服务地址
         */
        private String serverAddress = CloudDMConfig.DDS_SERVER;
        /**
         * 是否启用内置vad
         */
        private boolean useVad = true;
        /**
         * 设置内置vad资源路径或名称(assets路径下)
         */
        private String vadRes = "";
        /**
         * 设置产品分支名称
         */
        private String aliasKey = "prod";

        /**
         * 连接超时
         */
        private int connectTimeout;


        /**
         * 设置连接超时
         *
         * @param connectTimeout 超时时间
         * @return {@link Builder}
         */
        public Builder setConnectTimeout(int connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        /**
         * 设置服务地址，默认地址:  wss://dds.dui.ai/dds/v2/
         *
         * @param serverAddress 服务地址
         * @return {@link Builder}
         */
        public Builder setServerAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        /**
         * 是否启用内置vad
         *
         * @param useVad boolean
         * @return {@link Builder}
         */
        public Builder setUseVad(boolean useVad) {
            this.useVad = useVad;
            return this;
        }

        /**
         * 设置vad 资源
         *
         * @param vadRes 资源名称或资源完整路径
         * @return {@link Builder}
         */
        public Builder setVadRes(String vadRes) {
            this.vadRes = vadRes;
            return this;
        }

        /**
         * 设置产品分支，默认 prod
         *
         * @param aliasKey 产品分支
         * @return {@link Builder}
         */
        public Builder setAliasKey(String aliasKey) {
            this.aliasKey = aliasKey;
            return this;
        }

        /**
         * 是否启用纯语义模式
         *
         * @param useRefText boolean
         * @return {@link Builder}
         */
        public Builder setUseRefText(boolean useRefText) {
            this.useRefText = useRefText;
            return this;
        }

        /**
         * 是否自定义feed音频
         *
         * @param useCustomFeed boolean
         * @return {@link Builder}
         */
        public Builder setUseCustomFeed(boolean useCustomFeed) {
            this.useCustomFeed = useCustomFeed;
            return this;
        }

        public AICloudSemanticConfig build() {
            check();//参数非法校验
            return new AICloudSemanticConfig(this);
        }

        /**
         * 检查初始化参数
         *
         * @return boolean
         */
        boolean check() {

            if (useVad && TextUtils.isEmpty(vadRes)) {
                throw new IllegalArgumentException("请设置vad资源!");
            }

            if (TextUtils.isEmpty(serverAddress) || !serverAddress.startsWith("ws")) {
                throw new IllegalArgumentException("非法的服务地址!");
            }

            return true;
        }
    }
}
