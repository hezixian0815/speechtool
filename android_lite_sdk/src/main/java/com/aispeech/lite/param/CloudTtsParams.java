/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.common.AIConstant;
import com.aispeech.common.Util;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * 合成引擎参数配置类
 */
public class CloudTtsParams extends TTSParams {

    public static final String TAG = "CloudTTSParams";

    private String speaker = "zhilingfa";
    private String speed = "1";
    private String volume = "50";
    private String useSSML = "1";
    private String refText = "";
    private String textType = "text";
    private String speakingStyle = "default"; // 支持sad,default,happy ==> emotion
    private String language = "";
    private boolean enableRealTimeFeedback = true;
    private String userId = "1001";

    private String pitchChange = "0";
    private String audioPath = "";
    private String server = AISpeechSDK.DEFAULT_CLOUD_TTS_SERVER;
    private String cachePath = AISpeech.getContext().getExternalCacheDir() + File.separator + "ttsCache";
    /* copy from master */
//    private String server = "https://tts.duiopen.com/runtime/v2/synthesize";
    private String serverApiKey;
    private String productId;
    private String deviceName;
    private String sdkName;
    private String utteranceId;
    private boolean returnPhone = false;
    /**
     * request
     */
    private String requestId;
    /**
     * request->audio
     */
    private String audioType = "mp3";
    private String mp3Quality = "low";
    private int channel = 1;
    private int sampleBytes = 2;
    /**
     * speakUrl 有值时忽略其它参数，使用 GET 方式获取已经生成的tts音频。目前给 dds 使用
     */
    private String speakUrl;
    private long waitingTimeout = 3000;

    public static String getDecodeStr(String str) {
        String s = str;
        s = s.replaceAll("\\\\\"", "\"");
        s = s.replaceAll("\\\\", "");
        s = s.replaceAll("\"\\{", "{");
        s = s.replaceAll("\\}\"", "}");
        return s;
    }

    public JSONObject getContextJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("productId", productId);
            jsonObject.put("userId", userId);
            jsonObject.put("deviceName", deviceName);
            jsonObject.put("sdkName", sdkName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public String getTextType() {
        return textType;
    }

    public void setTextType(String textType) {
        this.textType = textType;
    }

    @Override
    public String getType() {
        return TYPE_CLOUD;
    }

    public boolean isRealBack() {
        return enableRealTimeFeedback;
    }

    public void setRealBack(boolean isRealBack) {
        this.enableRealTimeFeedback = isRealBack;
    }

    public String getSHA1Name() {
        return Util.SHA1(toString());
    }

    public String getSpeakingStyle() {
        return speakingStyle;
    }

    public void setSpeakingStyle(String speakingStyle) {
        this.speakingStyle = speakingStyle;
    }

    @Override
    public String getProductId() {
        return productId;
    }

    @Override
    public void setProductId(String productId) {
        this.productId = productId;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getSdkName() {
        return sdkName;
    }

    public void setSdkName(String sdkName) {
        this.sdkName = sdkName;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getAudioType() {
        return audioType;
    }

    @Override
    public void setAudioType(String audioType) {
        this.audioType = audioType;
    }

    public String getMp3Quality() {
        return mp3Quality;
    }

    public void setMp3Quality(String mp3Quality) {
        this.mp3Quality = mp3Quality;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public int getSampleBytes() {
        return sampleBytes;
    }

    public void setSampleBytes(int sampleBytes) {
        this.sampleBytes = sampleBytes;
    }

    public String getSpeakUrl() {
        return speakUrl;
    }

    public void setSpeakUrl(String speakUrl) {
        this.speakUrl = speakUrl;
    }

    public String getUtteranceId() {
        return utteranceId;
    }

    public void setUtteranceId(String utteranceId) {
        this.utteranceId = utteranceId;
    }


    public void setServerApiKey(String serverApiKey) {
        this.serverApiKey = serverApiKey;
    }

    public String getServerApiKey() {
        return serverApiKey;
    }

    @Override
    public boolean isReturnPhone() {
        return returnPhone;
    }

    public void setReturnPhone(boolean returnPhone) {
        this.returnPhone = returnPhone;
    }

    public JSONObject getRequestAudioJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("audioType", audioType);
            jsonObject.put("sampleRate", getSampleRate().getValue());
            if (audioType.equals(AIConstant.TTS_AUDIO_TYPE_MP3)) {
                jsonObject.put("mp3Quality", mp3Quality);
            }
            jsonObject.put("channel", channel);
            jsonObject.put("sampleBytes", sampleBytes);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject getRequestJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("requestId", requestId);
            jsonObject.put("audio", getRequestAudioJSON());
            jsonObject.put("tts", getRequestTtsJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private JSONObject getRequestTtsJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("useSSML", useSSML);
            jsonObject.put("text", refText);
            jsonObject.put("voiceId", speaker);
            jsonObject.put("textType", textType);
            jsonObject.put("enableRealTimeFeedback", true);
            jsonObject.put("speed", speed);
            jsonObject.put("volume", volume);
            jsonObject.put("speakingStyle", speakingStyle);
            jsonObject.put("pitchChange", pitchChange);
            jsonObject.put("returnPhone", returnPhone);
            if (!TextUtils.isEmpty(language)) {
                jsonObject.put("language", language);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public String getTtsJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("context", getContextJSON());
            jsonObject.put("request", getRequestJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public void setServer(String server) {
        this.server = server;
    }

    public String getSpeaker() {
        return speaker;
    }

    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public String getSpeed() {
        return speed;
    }

    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public void setSpeed(float speed) {
        this.speed = String.valueOf(speed);
    }

    public String getVolume() {
        return volume;
    }

    public void setVolume(String volume) {
        this.volume = volume;
    }

    public void setVolume(int volume) {
        this.volume = String.valueOf(volume);
    }

    public String getRefText() {
        return refText;
    }

    public void setRefText(String refText) {
        this.refText = refText;
    }

    public String getRefTextJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("refText", refText);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    public String getPitchChange() {
        return pitchChange;
    }

    public void setPitchChange(String pitchChange) {
        this.pitchChange = pitchChange;
    }


    public String getCachePath() {
        return cachePath;
    }

    public void setCachePath(String cachePath) {
        this.cachePath = cachePath;
    }

    public String getUseSSML() {
        return useSSML;
    }

    public void setUseSSML(String useSSML) {
        this.useSSML = useSSML;
    }

    public String getAudioPath() {
        if (audioPath.endsWith(File.separator) || TextUtils.isEmpty(audioPath)) {
            return audioPath;
        } else {
            return audioPath + File.separator;
        }
    }

    public void setAudioPath(String audioPath) {
        this.audioPath = audioPath;
    }

    @Override
    public String getSaveAudioFileName() {
        if (TextUtils.isEmpty(getSaveAudioPath())) {
            return null;
        } else {
            return getSaveAudioPath() + File.separator + getSHA1Name() + "_" + utteranceId + "." + getAudioType();
        }
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    @Override
    public String toString() {
        return "CloudTtsParams{" +
                "speaker='" + speaker + '\'' +
                ", speed='" + speed + '\'' +
                ", volume='" + volume + '\'' +
                ", useSSML='" + useSSML + '\'' +
                ", pitchChange='" + pitchChange + '\'' +
                ", audioPath='" + audioPath + '\'' +
                ", cachePath='" + cachePath + '\'' +
                ", refText='" + refText + '\'' +
                ", textType='" + textType + '\'' +
                ", enableRealTimeFeedback=" + enableRealTimeFeedback +
                ", userId='" + userId + '\'' +
                ", server='" + server + '\'' +
                ", productId='" + productId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", sdkName='" + sdkName + '\'' +
                ", requestId='" + requestId + '\'' +
                ", audioType='" + audioType + '\'' +
                ", mp3Quality='" + mp3Quality + '\'' +
                ", sampleRate=" + getSampleRate().getValue() +
                ", channel=" + channel +
                ", sampleBytes=" + sampleBytes +
                ", speakUrl='" + speakUrl + '\'' +
                ", streamType=" + streamType +
                ", speakingStyle=" + speakingStyle +
                ", language=" + language +
                ", waitingTimeout=" + waitingTimeout +
                '}';
    }
}
