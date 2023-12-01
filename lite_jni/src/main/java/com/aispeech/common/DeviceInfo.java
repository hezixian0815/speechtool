package com.aispeech.common;

import android.text.TextUtils;

public class DeviceInfo {
    private String platform;
    private String deviceId;
    private String packageName;
    private String buildModel;
    private String buildManufacture;
    private String buildDevice;
    private String applicationLabel;
    private String applicationVersion;
    private String buildSdkInt;
    private String displayMatrix;
    private String buildVariant;
    private String imei;
    private String mac;
    private String androidId;
    private String sdkName;
    private String sdkVersion;
    @Override
    public String toString() {
        return "{" +
                "\"deviceId\":\"" + deviceId + '\"' +
                ",\"platform\":\"" + platform + '\"' +
                ",\"buildVariant\":\"" + buildVariant + '\"' +
                ",\"packageName\":\"" + packageName + '\"' +
                ",\"buildModel\":\"" + buildModel + '\"' +
                ",\"buildManufacture\":\"" + buildManufacture + '\"' +
                ",\"buildDevice\":\"" + buildDevice + '\"' +
                ",\"applicationLabel\":\"" + applicationLabel + '\"' +
                ",\"applicationVersion\":\"" + applicationVersion + '\"' +
                ",\"buildSdkInt\":\"" + buildSdkInt + '\"' +
                ",\"displayMatrix\":\"" + displayMatrix + '\"' +
                ",\"imei\":\"" + imei + '\"' +
                ",\"mac\":\"" + mac + '\"' +
                ",\"androidId\":\"" + androidId + '\"' +
                getSdkName() +
                getSdkVersion() +
                "}";
    }

    private String getSdkName() {
        return !TextUtils.isEmpty(sdkName) ? ",\"sdkName\":\"" + sdkName + '\"' : "";
    }

    private String getSdkVersion() {
        return !TextUtils.isEmpty(sdkVersion) ? ",\"sdkVersion\":\"" + sdkVersion + '\"' : "";
    }
}
