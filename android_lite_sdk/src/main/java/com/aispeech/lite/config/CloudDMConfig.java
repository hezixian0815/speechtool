package com.aispeech.lite.config;

import com.aispeech.lite.AISpeech;

public class CloudDMConfig extends AIEngineConfig {

    /**
     * DDS 服务地址
     */
    public static String DDS_SERVER = "wss://dds.dui.ai/dds/v3/";

    /**
     * cinfo 服务地址
     */
    public static String CINFO_SERVER = "http://dds.dui.ai/cinfo/v1";
    /**
     * 服务协议
     */
    private String serviceType = "websocket";
    /**
     * 分支名
     */
    private String aliasKey = "prod";
    /**
     * device name
     */
    private String deviceName;
    /**
     * native api timeout
     * default 10 S
     */
    public int nativeApiTimeout = 10 * 1000;

    public int getNativeApiTimeout() {
        return nativeApiTimeout;
    }

    public CloudDMConfig setNativeApiTimeout(int nativeApiTimeout) {
        this.nativeApiTimeout = nativeApiTimeout;
        return this;
    }

    /**
     * 保留字段用户编号
     */
    private String userId = "dui-lite";

    private String productId;

    private String apiKey;

    private String secret;

    private String productKey;

    private String serverAddress = DDS_SERVER;

    private String cInfoServerAddress = CINFO_SERVER;

    /**
     * 对话结果透传
     */
    private boolean isDMRoute = false;
    /**
     * 纯语义模式
     */
    private boolean useRefText = false;
    /**
     * 是否启用全双工模式
     */
    private boolean useFullDuplex = false;

    /**
     * 是否在全双工模式下未检测到语音超时反馈语播报
     */
    boolean useFullDuplexNoSpeechTimeOut = false;

    /**
     * 自定义请求参数
     */
    private String[] keys;
    private String[] values;


    public CloudDMConfig() {
        this.productId = AISpeech.getProfile().getProductId();
        this.apiKey = AISpeech.getProfile().getApiKey();
        this.secret = AISpeech.getProfile().getProductSecret();
        this.productKey = AISpeech.getProfile().getProductKey();
        this.deviceName = AISpeech.getProfile().getDeviceName();
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getAliasKey() {
        return aliasKey;
    }

    public boolean isUseFullDuplex() {
        return useFullDuplex;
    }

    public boolean isUseFullDuplexNoSpeechTimeOut() {
        return useFullDuplexNoSpeechTimeOut;
    }

    public CloudDMConfig setUseFullDuplex(boolean useFullDuplex) {
        this.useFullDuplex = useFullDuplex;
        return this;
    }

    public CloudDMConfig setUseFullDuplexNoSpeechTimeOut(boolean useFullDuplexNoSpeechTimeOut) {
        this.useFullDuplexNoSpeechTimeOut = useFullDuplexNoSpeechTimeOut;
        return this;
    }

    /**
     * 设置请求语义产品分支
     *
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

    public String getProductKey() {
        return productKey;
    }

    public boolean isDMRoute() {
        return isDMRoute;
    }

    public void setDMRoute(boolean DMRoute) {
        isDMRoute = DMRoute;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public CloudDMConfig setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
        return this;
    }


    public CloudDMConfig setCInfoServerAddress(String cInfoServerAddress) {
        this.cInfoServerAddress = cInfoServerAddress;
        return this;
    }

    public boolean isUseRefText() {
        return useRefText;
    }

    public CloudDMConfig setUseRefText(boolean useRefText) {
        this.useRefText = useRefText;
        return this;
    }

    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    public String[] getKeys() {
        return keys;
    }

    public void setValues(String[] values) {
        this.values = values;
    }

    public String[] getValues() {
        return values;
    }


    public String getCInfoServerAddress() {
        return cInfoServerAddress;
    }

    @Override
    public String toString() {
        return "CloudDMConfig{" +
                "serviceType='" + serviceType + '\'' +
                ", aliasKey='" + aliasKey + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", nativeApiTimeout=" + nativeApiTimeout +
                ", userId='" + userId + '\'' +
                ", productId='" + productId + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", secret='" + secret + '\'' +
                ", productKey='" + productKey + '\'' +
                ", isDMRoute=" + isDMRoute +
                ", cInfoServerAddress=" + cInfoServerAddress +
                '}';
    }
}
