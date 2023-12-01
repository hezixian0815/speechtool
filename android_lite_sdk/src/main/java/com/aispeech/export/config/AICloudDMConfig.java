package com.aispeech.export.config;

import android.text.TextUtils;

import com.aispeech.lite.base.BaseConfig;
import com.aispeech.lite.config.CloudDMConfig;

/**
 * 云端对话引擎初始化参数
 *
 * @author hehr
 */
public class AICloudDMConfig extends BaseConfig {

    /**
     * native api timeout
     */
    int nativeApiTimeout;
    /**
     * 半双工服务地址
     */
    String serverAddress;

    /**
     * CInfo 服务地址
     */
    String cInfoServerAddress;

    /**
     * 对话透传模式
     */
    boolean isRoute;
    /**
     * 是否启用内置vad
     */
    boolean useVad;
    /**
     *
     */
    String vadRes;
    /**
     * 产品分支
     */
    String aliasKey;
    /**
     * 是否启用全双工模式
     */
    boolean useFullDuplex = false;

    /**
     * 是否在全双工模式下未检测到语音超时反馈语播报
     */
    boolean useFullDuplexNoSpeechTimeOut = false;

    /**
     * 是否启用纯语义模式
     */
    private boolean useRefText;

    /**
     * 自定义请求参数
     */
    private String[] keys;
    private String[] values;

    /**
     * 连接超时
     */
    private int connectTimeout;

    private AICloudDMConfig(Builder builder) {
        this.nativeApiTimeout = builder.nativeApiTimeout;
        this.serverAddress = builder.serverAddress;
        this.cInfoServerAddress = builder.cInfoServerAddress;
        this.isRoute = builder.isRoute;
        this.useVad = builder.useVad;
        this.vadRes = builder.vadRes;
        this.aliasKey = builder.aliasKey;
        this.useFullDuplex = builder.useFullDuplex;
        this.useRefText = builder.useRefText;
        this.keys = builder.keys;
        this.values = builder.values;
        this.useFullDuplexNoSpeechTimeOut = builder.useFullDuplexNoSpeechTimeOut;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public int getNativeApiTimeout() {
        return nativeApiTimeout;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getCInfoServerAddress() {
        return cInfoServerAddress;
    }

    public boolean isRoute() {
        return isRoute;
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

    public boolean isUseFullDuplex() {
        return useFullDuplex;
    }

    public boolean isUseFullDuplexNoSpeechTimeOut() {
        return useFullDuplexNoSpeechTimeOut;
    }

    public boolean isUseRefText() {
        return useRefText;
    }

    public String[] getKeys() {
        return keys;
    }

    public String[] getValues() {
        return values;
    }

    public static class Builder extends BaseConfig.Builder {
        /**
         * native api timeout
         */
        int nativeApiTimeout = 10 * 1000;

        String serverAddress = CloudDMConfig.DDS_SERVER;

        /**
         * CInfo 服务地址
         */
        String cInfoServerAddress = CloudDMConfig.CINFO_SERVER;

        /**
         * 对话透传模式
         */
        boolean isRoute = false;
        /**
         * 是否启用内置vad
         */
        boolean useVad = false;
        /**
         *
         */
        String vadRes = "";
        /**
         * 产品分支
         */
        String aliasKey = "prod";

        /**
         * 是否启用全双工模式
         */
        boolean useFullDuplex = false;

        /**
         * 是否在全双工模式下未检测到语音超时反馈语播报
         */
        boolean useFullDuplexNoSpeechTimeOut = false;

        /**
         * 是否启用纯语义模式
         */
        private boolean useRefText = false;

        /**
         * 自定义请求参数
         */
        private String[] keys;
        private String[] values;

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
         * 设置native api 响应超时时间，单位 毫秒
         *
         * @param nativeApiTimeout 单位毫秒
         * @return {@link Builder}
         */
        public Builder setNativeApiTimeout(int nativeApiTimeout) {
            this.nativeApiTimeout = nativeApiTimeout;
            return this;
        }

        /**
         * 设置服务器地址，默认不用设置
         *
         * @param serverAddress 服务地址
         * @return {@link Builder}
         */
        public Builder setServerAddress(String serverAddress) {
            this.serverAddress = serverAddress;
            return this;
        }

        /**
         * 设置 CInfo服务地址，默认不用设置
         *
         * @param cInfoServerAddress CInfo 服务地址
         * @return {@link Builder}
         */
        public Builder setCInfoServerAddress(String cInfoServerAddress) {
            this.cInfoServerAddress = cInfoServerAddress;
            return this;
        }

        /**
         * 对话结果透传模式
         *
         * @param route boolean
         * @return {@link Builder}
         */
        public Builder setRoute(boolean route) {
            isRoute = route;
            return this;
        }

        /**
         * 是否启用内置vad,启用该项配置，需语音在产品高级配置界面打开 空帧识别结果过滤 配置
         *
         * @param useVad boolean
         * @return {@link Builder}
         */
        public Builder setUseVad(boolean useVad) {
            this.useVad = useVad;
            return this;
        }

        /**
         * 设置vad资源
         *
         * @param vadRes 资源名称(assets路径下)或绝对路径
         * @return {@link Builder}
         */
        public Builder setVadRes(String vadRes) {
            this.vadRes = vadRes;
            return this;
        }

        /**
         * 设置请求产品分支
         *
         * @param aliasKey 分支名称，默认 prod
         * @return {@link Builder }
         */
        public Builder setAliasKey(String aliasKey) {
            this.aliasKey = aliasKey;
            return this;
        }

        /**
         * 是否启用全双工模式
         *
         * @param useFullDuplex 是否启用全双工对话模式,默认 false
         * @return {@link Builder }
         */
        public Builder setUseFullDuplex(boolean useFullDuplex) {
            this.useFullDuplex = useFullDuplex;
            return this;
        }

        /**
         * 是否在全双工模式下未检测到语音超时反馈语播报
         *
         * @param useFullDuplexNoSpeechTimeOut 默认 false
         * @return {@link Builder }
         */
        public Builder setUseFullDuplexNoSpeechTimeOut(boolean useFullDuplexNoSpeechTimeOut) {
            this.useFullDuplexNoSpeechTimeOut = useFullDuplexNoSpeechTimeOut;
            return this;
        }

        /**
         * 设置自定义参数
         *
         * @param keys   key
         * @param values value
         * @return {@link Builder }
         */
        public Builder setCustomParams(String[] keys, String[] values) {
            this.keys = keys;
            this.values = values;
            return this;
        }

        @Override
        public Builder setTagSuffix(String tagSuffix) {
            return (Builder) super.setTagSuffix(tagSuffix);
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

//            if (useFullDuplex && !useVad) {
//                throw new IllegalArgumentException("全双工模式vad资源必选!");
//            }

            return true;
        }

        public AICloudDMConfig build() {
            check();
            return super.build(new AICloudDMConfig(this));
        }

    }
}
