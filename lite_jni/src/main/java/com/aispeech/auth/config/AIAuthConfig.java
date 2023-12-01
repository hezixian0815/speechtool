package com.aispeech.auth.config;

import java.util.Map;

/**
 * @author wuwei
 * @date 2019-09-25 10:57
 * @email wei.wu@aispeech.com
 */
public class AIAuthConfig {
    private String productId;
    private String productKey;
    private String productSecret;
    private String apiKey;
    private String customDeviceId;
    private String customDeviceName;
    private String deviceNameType;
    private String customSHA256;
    private String buildModel;
    private String profilePath;
    private String offlineProfileName;
    private String authServer;
    private String registerPath = "/auth/device/register";
    private String loginPath = "/auth/device/login";
    private String verifyPath = "/auth/apikey/verify";
    private boolean ignoreLogin;
    private boolean needReplaceProfile = true;
    private long authTimeout = 5000;
    private String licenseId;
    private Map<String, Object> deviceInfoMap;
    private boolean encryptCustomDeviceName = false;
    private String sharePkgName;
    private String shareSHA256;

    public String getProductId() {
        return productId;
    }

    public String getProductKey() {
        return productKey;
    }

    public String getProductSecret() {
        return productSecret;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getAuthServer() {
        return authServer;
    }

    public String getCustomDeviceId() {
        return customDeviceId;
    }

    public String getCustomDeviceName() {
        return customDeviceName;
    }

    public String getDeviceNameType() {
        return deviceNameType;
    }

    public String getCustomSHA256() {
        return customSHA256;
    }

    public String getBuildModel() {
        return buildModel;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public boolean isNeedReplaceProfile() {
        return needReplaceProfile;
    }

    public boolean isEncryptCustomDeviceName() {
        return encryptCustomDeviceName;
    }

    public boolean isIgnoreLogin() {
        return ignoreLogin;
    }

    public String getOfflineProfileName() {
        return offlineProfileName;
    }

    public long getAuthTimeout() {
        return authTimeout;
    }

    public String getRegisterPath() {
        return registerPath;
    }

    public String getLoginPath() {
        return loginPath;
    }

    public String getLicenseId() {
        return licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }

    public String getVerifyPath() {
        return verifyPath;
    }

    public Map<String, Object> getDeviceInfoMap() {
        return deviceInfoMap;
    }

    public String getSharePkgName() {
        return sharePkgName;
    }

    public void setSharePkgName(String sharePkgName) {
        this.sharePkgName = sharePkgName;
    }

    public String getShareSHA256() {
        return shareSHA256;
    }

    public void setShareSHA256(String shareSHA256) {
        this.shareSHA256 = shareSHA256;
    }

    public AIAuthConfig setNeedReplaceProfile(boolean needReplaceProfile) {
        this.needReplaceProfile = needReplaceProfile;
        return this;
    }

    private AIAuthConfig(Builder builder) {
        this.productId = builder.productId;
        this.productKey = builder.productKey;
        this.productSecret = builder.productSecret;
        this.apiKey = builder.apiKey;
        this.authServer = builder.authServer;
        this.customDeviceId = builder.customDeviceId;
        this.customDeviceName = builder.customDeviceName;
        this.deviceNameType = builder.deviceNameType;
        this.customSHA256 = builder.customSHA256;
        this.buildModel = builder.buildModel;
        this.deviceInfoMap = builder.deviceInfoMap;
        this.authTimeout = builder.authTimeout;
        this.needReplaceProfile = builder.needReplaceProfile;
        this.offlineProfileName = builder.offlineProfileName;
        this.profilePath = builder.profilePath;
        this.ignoreLogin = builder.ignoreLogin;
        this.licenseId = builder.licenseId;
        this.encryptCustomDeviceName = builder.encryptCustomDeviceName;
        this.sharePkgName = builder.sharePkgName;
        this.shareSHA256 = builder.shareSHA256;
    }

    @Override
    public String toString() {
        return "AIAuthConfig{" +
                "productId='" + productId + '\'' +
                ", productKey='" + productKey + '\'' +
                ", productSecret='" + productSecret + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", customDeviceId='" + customDeviceId + '\'' +
                ", customDeviceName='" + customDeviceName + '\'' +
                ", deviceNameType='" + deviceNameType + '\'' +
                ", customSHA256='" + customSHA256 + '\'' +
                ", buildModel='" + buildModel + '\'' +
                ", profilePath='" + profilePath + '\'' +
                ", offlineProfileName='" + offlineProfileName + '\'' +
                ", authServer='" + authServer + '\'' +
                ", registerPath='" + registerPath + '\'' +
                ", loginPath='" + loginPath + '\'' +
                ", verifyPath='" + verifyPath + '\'' +
                ", needReplaceProfile=" + needReplaceProfile +
                ", needReplaceProfile=" + encryptCustomDeviceName +
                ", authTimeout=" + authTimeout +
                ", deviceInfoMap=" + deviceInfoMap +
                ", ignoreLogin=" + ignoreLogin +
                ", licenceId=" + licenseId +
                ", licenceId=" + licenseId +
                '}';
    }

    public static class Builder {
        private String productId;
        private String productKey;
        private String productSecret;
        private String apiKey;
        private String authServer = "https://auth.duiopen.com";
        private String customDeviceId;
        private String customDeviceName;
        private String deviceNameType;
        private String customSHA256;
        private String buildModel;
        private String profilePath;
        private String licenseId;
        private String offlineProfileName;
        private boolean needReplaceProfile = true;
        private boolean ignoreLogin;
        private long authTimeout = 5000;
        private Map<String, Object> deviceInfoMap;
        private boolean encryptCustomDeviceName = false;
        private String sharePkgName;
        private String shareSHA256;
        /**
         * 设置productId
         *
         * @param productId dui网站上申请的产品id
         * @return Builder
         */
        public Builder setProductId(String productId) {
            this.productId = productId;
            return this;
        }

        /**
         * 设置productKey
         *
         * @param productKey dui网站上申请的产品key
         * @return Builder
         */
        public Builder setProductKey(String productKey) {
            this.productKey = productKey;
            return this;
        }

        /**
         * 设置productSecret
         *
         * @param productSecret dui网站上申请的产品secret
         * @return Builder
         */
        public Builder setProductSecret(String productSecret) {
            this.productSecret = productSecret;
            return this;
        }

        /**
         * 设置apiKey
         *
         * @param apiKey dui网站上申请的应用key
         * @return Builder
         */
        public Builder setApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        /**
         * 设置授权服务器地址
         *
         * @param authServer dui授权服务器地址
         * @return Builder
         */
        public Builder setAuthServer(String authServer) {
            this.authServer = authServer;
            return this;
        }


        public Builder setCustomDeviceId(String customDeviceId) {
            this.customDeviceId = customDeviceId;
            return this;
        }

        public Builder setCustomDeviceName(String customDeviceName) {
            this.customDeviceName = customDeviceName;
            return this;
        }

        public Builder setEncryptCustomDeviceName(boolean encryptCustomDeviceName) {
            this.encryptCustomDeviceName = encryptCustomDeviceName;
            return this;
        }

        public Builder setLicenseId(String licenseId) {
            this.licenseId = licenseId;
            return this;
        }

        public Builder setDeviceNameType(String deviceNameType) {
            this.deviceNameType = deviceNameType;
            return this;
        }

        public Builder setCustomSHA256(String customSHA256) {
            this.customSHA256 = customSHA256;
            return this;
        }

        public Builder setBuildModel(String buildModel) {
            this.buildModel = buildModel;
            return this;
        }

        public Builder setDeviceInfoMap(Map<String, Object> deviceInfoMap) {
            this.deviceInfoMap = deviceInfoMap;
            return this;
        }

        public Builder setProfilePath(String profilePath) {
            this.profilePath = profilePath;
            return this;
        }

        public Builder setOfflineProfileName(String offlineProfileName) {
            this.offlineProfileName = offlineProfileName;
            return this;
        }

        public Builder setNeedReplaceProfile(boolean needReplaceProfile) {
            this.needReplaceProfile = needReplaceProfile;
            return this;
        }

        public Builder setIgnoreLogin(boolean ignoreLogin) {
            this.ignoreLogin = ignoreLogin;
            return this;
        }

        public Builder setAuthTimeout(long authTimeout) {
            this.authTimeout = authTimeout;
            return this;
        }

        public Builder setSharePkgName(String sharePkgName) {
            this.sharePkgName = sharePkgName;
            return this;
        }

        public Builder setShareSHA256(String shareSHA256) {
            this.shareSHA256 = shareSHA256;
            return this;
        }

        /**
         * 构建AIAuthConfig实例
         *
         * @return AIAuthConfig
         */
        public AIAuthConfig build() {
            return new AIAuthConfig(this);
        }
    }
}
