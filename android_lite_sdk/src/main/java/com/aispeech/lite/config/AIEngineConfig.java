/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.config;

import android.content.Context;
import android.text.TextUtils;

import com.aispeech.DUILiteConfig;
import com.aispeech.auth.AIAuthEngine;
import com.aispeech.auth.AIProfile;
import com.aispeech.common.Log;
import com.aispeech.export.bean.VoiceQueueStrategy;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

public class AIEngineConfig implements Cloneable {

    private String[] assetsResNames;

    private String tag;
    private boolean vadEnable = true;

    // 设置使用自定义接口流式传入数据
    private boolean useCustomFeed = false;

    // 连接超时
    private int connectTimeout = 3000;

    // 设置在播报中调用 stop 是否回调 onSpeechFinish
    private boolean useStopCallback = true;

    // JNI库日志开关  0:关闭 1:开启
    private String jniLogEnable = "enable";
    // JNI库日志保存到本地 （路径+文件名）
    private String jniLogSavePath = "output";
    // JNI库日志打印等级 (Verbose~Error : 1~5)
    private String jniLogLevel = "level";

    // JNI库授权参数 DUI平台产品ID
    private String jniAuthProductId = "productId";
    // JNI库授权参数 设备标识，一台设备对应一个设备标识，必选
    private String jniAuthDeviceName = "deviceName";
    // JNI库授权参数 授权文件绝对路径
    private String jniAuthSavedProfile = "savedProfile";

    /**
     * assets下的资源文件夹
     */
    private String resFolderName;

    //是否使用双VAD
    private boolean enableDoubleVad;

    private VoiceQueueStrategy voiceQueueStrategy;

    public VoiceQueueStrategy getVoiceQueueStrategy() {
        return voiceQueueStrategy;
    }

    public void setVoiceQueueStrategy(VoiceQueueStrategy voiceQueueStrategy) {
        this.voiceQueueStrategy = voiceQueueStrategy;
    }

    public String getResFolderName() {
        return resFolderName;
    }

    public void setResFolderName(String resFolderName) {
        this.resFolderName = resFolderName;
    }


    public void setVadEnable(boolean vadEnable) {
        this.vadEnable = vadEnable;
    }

    public boolean isVadEnable() {
        return vadEnable;
    }


    public Context getContext() {
        return AISpeech.getContext();
    }


    /**
     * 设置需要拷贝的资源名列表
     *
     * @param assetsResNames
     */
    public void setAssetsResNames(String[] assetsResNames) {
        this.assetsResNames = assetsResNames;
    }

    /**
     * 获取需要拷贝的资源名列表
     *
     * @return
     */
    public String[] getAssetsResNames() {
        return assetsResNames;
    }

    Map<String, String> mResMd5Map;

    public void setAssetsResMd5sum(Map<String, String> map) {
        this.mResMd5Map = map;
    }

    public Map<String, String> getAssetsResMd5sum() {
        return mResMd5Map;
    }


    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }


    /**
     * 是否自己feed数据,不使用内部录音机(包括MockRecord和AIAudioRecord)
     *
     * @return the useCustomFeed true 需要自行feed数据
     */
    public boolean isUseCustomFeed() {
        return useCustomFeed;
    }

    /**
     * 设置自行feed数据
     *
     * @param useCustomFeed the useCustomFeed to set
     */
    public void setUseCustomFeed(boolean useCustomFeed) {
        this.useCustomFeed = useCustomFeed;
    }


    /**
     * 是否stop之后回调 onSpeechFinish 方法
     *
     * @return the useStopCallback true 回调
     */
    public boolean isUseStopCallback() {
        return useStopCallback;
    }

    /**
     * 设置是否stop之后回调 onSpeechFinish 方法
     *
     * @param useStopCallback the useStopCallback to set
     */
    public void setUseStopCallback(boolean useStopCallback) {
        this.useStopCallback = useStopCallback;
    }

    /**
     * 设置离线识别引擎是否使用双VAD，使用双VAD 需要 feed 2通道音频，而
     * 使用非双VAD 则 feed 1 通道的音频。
     *
     * @param enableDoubleVad true 使用双VAD , false 非双VAD
     */
    public void setEnableDoubleVad(boolean enableDoubleVad) {
        this.enableDoubleVad = enableDoubleVad;
    }

    /**
     * 是否使用双VAD
     *
     * @return true使用双VAD , false 非双VAD
     */
    public boolean isEnableDoubleVad() {
        return enableDoubleVad;
    }


    @Override
    public AIEngineConfig clone() throws CloneNotSupportedException {
        return (AIEngineConfig) super.clone();
    }


    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONObject logObj = new JSONObject();
            // JNI日志开关状态与SDK Log保持一致
            logObj.put(jniLogEnable, AISpeechSDK.LOGCAT_DEBUGABLE ? 1 : 0);
            // 暂无保存到本地的需求
            logObj.put(jniLogSavePath, "");
            // Log.VERBOSE需-1与JNI层保持一致
            logObj.put(jniLogLevel, Math.max(1, Log.LOG_NATIVE_LEVEL - 1));

            jsonObject.put("prof", logObj);

            AIProfile profile = AIAuthEngine.getInstance().getProfile();
            // so库授权参数
            JSONObject authObj = new JSONObject();
            // DUI平台产品ID
            authObj.put(jniAuthProductId, profile.getProductId());
            // 设备标识，一台设备对应一个设备标识，必选
            authObj.put(jniAuthDeviceName, profile.getDeviceName());
            // 授权文件绝对路径
            authObj.put(jniAuthSavedProfile, profile.getProfilePath());
            if (TextUtils.isEmpty(AISpeech.offlineEngineAuth)) {
                jsonObject.put("authParams", authObj);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;

    }

    public JSONObject toUpdateJson() {
        return new JSONObject();
    }

    /**
     * 设置消息队列最大长度
     * <ul>
     *     <li>默认-1 使用 {@linkplain DUILiteConfig#getMaxMessageQueueSize() DUILiteConfig#getMaxMessageQueueSize()} 的配置</li>
     *     <li>0表示不限制长度, 建议大于100</li>
     * </ul>
     */
    private int maxMessageQueueSize = -1;

    public int getMaxMessageQueueSize() {
        return maxMessageQueueSize;
    }

    public void setMaxMessageQueueSize(int maxMessageQueueSize) {
        this.maxMessageQueueSize = maxMessageQueueSize;
    }

    public int getConnectTimeout() {
        if (connectTimeout <= 0) {
            connectTimeout = 3000;
        }
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }
}
