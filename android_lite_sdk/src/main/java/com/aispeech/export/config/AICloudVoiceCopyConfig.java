package com.aispeech.export.config;

/**
 * 云端声音复刻配置信息
 */
public class AICloudVoiceCopyConfig {

    /**
     * 产品ID
     */
    private String productId;

    /**
     * 产品API_KEY
     */
    private String apiKey;

    /**
     * 服务器域名，填空则为默认域名，默认是: https://tts.duiopen.com
     */
    private String host;

    /**
     * 端侧的 token ：用来 DUI 网关进行用户认证
     */
    private String token;

    /**
     * 端侧的 rememberToken：普通token没有操作超过2h没有动作就会过期，但是remember token可以一直有效。
     */
    private String rememberToken;


    private AICloudVoiceCopyConfig(Builder builder) {
        this.productId = builder.productId;
        this.apiKey = builder.apiKey;
        this.host = builder.host;
        this.token = builder.token;
        this.rememberToken = builder.rememberToken;
    }

    public String getProductId() {
        return productId;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getHost() {
        return host;
    }

    public String getToken() {
        return token;
    }

    public String getRememberToken() {
        return rememberToken;
    }

    public static class Builder {
        /**
         * 产品ID
         */
        private String productId;

        /**
         * 产品API_KEY
         */
        private String apiKey;

        /**
         * 服务器域名，填空则为默认域名，默认是: https://tts.duiopen.com
         */
        private String host;

        /**
         * 端侧的 token ：用来 DUI 网关进行用户认证
         */
        private String token;

        /**
         * 端侧的 rememberToken：普通token没有操作超过2h没有动作就会过期，但是remember token可以一直有效。
         */
        private String rememberToken;

        public Builder setProductId(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder setApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder setHost(String host) {
            this.host = host;
            return this;
        }

        public Builder setRememberToken(String rememberToken) {
            this.rememberToken = rememberToken;
            return this;
        }

        public Builder setToken(String token) {
            this.token = token;
            return this;
        }

        public AICloudVoiceCopyConfig create() {
            return new AICloudVoiceCopyConfig(this);
        }
    }

}
