package com.aispeech.analysis;

import android.content.Context;

import java.util.Map;

public class AnalysisParam {

    private Context context;
    private String uploadUrl;
    private int logID;
    private String project;
    private String callerType;
    private String productId;
    private String deviceId;
    private String sdkVersion;
    private boolean logcatDebugable;
    private String logfilePath;
    private boolean uploadImmediately;
    private int maxCacheNum;
    private Map<String, Object> map;


    public AnalysisParam(Builder builder) {
        this.context = builder.context;
        this.uploadUrl = builder.uploadUrl;
        this.logID = builder.logID;
        this.project = builder.project;
        this.callerType = builder.callerType;
        this.productId = builder.productId;
        this.deviceId = builder.deviceId;
        this.sdkVersion = builder.sdkVersion;
        this.logcatDebugable = builder.logcatDebugable;
        this.logfilePath = builder.logfilePath;
        this.uploadImmediately = builder.uploadImmediately;
        this.maxCacheNum = builder.maxCacheNum;
        this.map = builder.map;
    }

    public Context getContext() {
        return context;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public int getLogID() {
        return logID;
    }

    public String getProject() {
        return project;
    }

    public String getCallerType() {
        return callerType;
    }

    public String getProductId() {
        return productId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSdkVersion() {
        return sdkVersion;
    }

    public boolean isLogcatDebugable() {
        return logcatDebugable;
    }

    public String getLogfilePath() {
        return logfilePath;
    }

    public boolean isUploadImmediately() {
        return uploadImmediately;
    }

    public int getMaxCacheNum() {
        return maxCacheNum;
    }

    public Map<String, Object> getMap() {
        return map;
    }

    @Override
    public String toString() {
        return "AnalysisParam{" +
                "context=" + context +
                ", uploadUrl='" + uploadUrl + '\'' +
                ", logID=" + logID +
                ", project='" + project + '\'' +
                ", callerType='" + callerType + '\'' +
                ", productId='" + productId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", sdkVersion='" + sdkVersion + '\'' +
                ", logcatDebugable=" + logcatDebugable +
                ", logfilePath='" + logfilePath + '\'' +
                ", uploadImmediately=" + uploadImmediately +
                ", maxCacheNum=" + maxCacheNum +
                '}';
    }

    public static class Builder {
        private Context context;
        private String uploadUrl;
        private int logID;
        private String project;
        private String callerType;
        private String productId;
        private String deviceId;
        private String sdkVersion;
        private boolean logcatDebugable;
        private String logfilePath;
        private boolean uploadImmediately;
        private int maxCacheNum;
        private Map<String, Object> map;

        public Context getContext() {
            return context;
        }

        public Builder setContext(Context context) {
            this.context = context;
            return this;
        }

        public String getUploadUrl() {
            return uploadUrl;
        }

        public Builder setUploadUrl(String uploadUrl) {
            this.uploadUrl = uploadUrl;
            return this;
        }

        public int getLogID() {
            return logID;
        }

        public Builder setLogID(int logID) {
            this.logID = logID;
            return this;
        }

        public String getProject() {
            return project;
        }

        public Builder setProject(String project) {
            this.project = project;
            return this;
        }

        public String getCallerType() {
            return callerType;
        }

        public Builder setCallerType(String callerType) {
            this.callerType = callerType;
            return this;
        }

        public String getProductId() {
            return productId;
        }

        public Builder setProductId(String productId) {
            this.productId = productId;
            return this;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public Builder setDeviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public String getSdkVersion() {
            return sdkVersion;
        }

        public Builder setSdkVersion(String sdkVersion) {
            this.sdkVersion = sdkVersion;
            return this;
        }

        public boolean isLogcatDebugable() {
            return logcatDebugable;
        }

        public Builder setLogcatDebugable(boolean logcatDebugable) {
            this.logcatDebugable = logcatDebugable;
            return this;
        }

        public String getLogfilePath() {
            return logfilePath;
        }

        public Builder setLogfilePath(String logfilePath) {
            this.logfilePath = logfilePath;
            return this;
        }

        public boolean isUploadImmediately() {
            return uploadImmediately;
        }

        public Builder setUploadImmediately(boolean uploadImmediately) {
            this.uploadImmediately = uploadImmediately;
            return this;
        }

        public int getMaxCacheNum() {
            return maxCacheNum;
        }

        public Builder setMaxCacheNum(int maxCacheNum) {
            this.maxCacheNum = maxCacheNum;
            return this;
        }

        public Map<String, Object> getMap() {
            return map;
        }

        public Builder setMap(Map<String, Object> map) {
            this.map = map;
            return this;
        }

        public AnalysisParam create() {
            return new AnalysisParam(this);
        }
    }
}
