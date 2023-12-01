package com.aispeech.lite.config;

public class CloudSemanticConfig extends AIEngineConfig {
    /**
     * 默认服务器地址
     */
    private String CINFO_SERVER_V2 = "wss://dds.dui.ai/dds/v2/";
    private String CINFO_SERVER_V1 = "https://dds.dui.ai/cinfo/v1";
    /**
     * 服务协议
     */
    private String serviceType = "websocket";
    /**
     * 分支名
     */
    private String aliasKey = "prod";
    private String deviceName ;
    /**
     * 保留字段用户编号
     */
    private String userId  = "dui-lite";
    private String productId;
    private String apiKey;
    private String secret;
    private String productKey;

    private String deviceSecret;

    public CloudSemanticConfig() {
    }


    public String getDeviceSecret() {
        return deviceSecret;
    }

    public CloudSemanticConfig setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
        return this;
    }

    public CloudSemanticConfig setUserId(String userId) {
        this.userId = userId;
        return this;
    }

    public CloudSemanticConfig setProductId(String productId) {
        this.productId = productId;
        return this;
    }

    public CloudSemanticConfig setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public CloudSemanticConfig setSecret(String secret) {
        this.secret = secret;
        return this;
    }

    public CloudSemanticConfig setProductKey(String productKey) {
        this.productKey = productKey;
        return this;
    }



    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
    public String getCinfoServerV2() { return CINFO_SERVER_V2; }
    public String getCinfoServerV1() {
        return CINFO_SERVER_V1;
    }
    public void setServer(String server) { this.CINFO_SERVER_V2 = server; }
    public String getServiceType() { return serviceType; }
    //    public void setServiceType(String serviceType) { this.serviceType = serviceType; }
    public String getAliasKey() { return aliasKey; }
    /**
     * 设置请求语义产品分支
     * @param aliasKey
     */
    public void setAliasKey(String aliasKey) {
        this.aliasKey = aliasKey;
    }
    public String getUserId() {
        return userId;
    }
    public String getProductId() {
        return productId;
    }
    public String getApiKey() {
        return apiKey;
    }
    public String getSecret() {
        return secret;
    }
    public String getProductKey() { return productKey; }
    @Override
    public String toString() {
        return "CloudSemanticConfig{" +
                "aliasKey='" + aliasKey + '\'' +
                ", userId='" + userId + '\'' +
                ", productId='" + productId + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", secret='" + secret + '\'' +
                ", productKey='" + productKey + '\'' +
                '}';
    }
}