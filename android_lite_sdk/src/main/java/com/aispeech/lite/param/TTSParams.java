package com.aispeech.lite.param;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.text.TextUtils;

import org.json.JSONObject;

import java.io.File;

public abstract class TTSParams extends SpeechParams implements Cloneable {

    protected int streamType = AudioManager.STREAM_MUSIC;
    //    protected int audioAttributesUsage = 0; // AudioAttributes.USAGE_UNKNOWN
//    protected int audioAttributesContentType = 0; // AudioAttributes.CONTENT_TYPE_UNKNOWN
    private boolean isAutoPlay = true;
    private String saveAudioFileName = "";
    private boolean outRealData = false;
    private String type = TYPE_CLOUD;
    private String utteranceId;
    private boolean useStreamType = false;
    protected AudioAttributes audioAttributes;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TTSParams() {
        super();
        setUseRecorder(false);
    }

    /**
     * 设置音频流通道
     *
     * @param streamType
     */
    public void setStreamType(int streamType) {
        this.streamType = streamType;
    }

    /**
     * 获取音频流通道
     *
     * @return
     */
    public int getStreamType() {
        return streamType;
    }

//    public int getAudioAttributesContentType() {
//        return audioAttributesContentType;
//    }
//
//    public void setAudioAttributesContentType(int audioAttributesContentType) {
//        this.audioAttributesContentType = audioAttributesContentType;
//    }
//
//    public int getAudioAttributesUsage() {
//        return audioAttributesUsage;
//    }
//
//    public void setAudioAttributesUsage(int audioAttributesUsage) {
//        this.audioAttributesUsage = audioAttributesUsage;
//    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public boolean isReturnPhone() {
        return false;
    }

    public abstract String getRefText();

    public abstract void setRefText(String refText);

    public abstract boolean isRealBack();


    public abstract String getSHA1Name();

    public abstract String getAudioType();

    /**
     * 获取是否自动播放状态
     *
     * @return
     */
    public boolean isAutoPlay() {
        return this.isAutoPlay;
    }

    /**
     * 设置是否自动播放
     *
     * @param isAutoPlay
     */
    public void setIsAutoPlay(boolean isAutoPlay) {
        this.isAutoPlay = isAutoPlay;
    }

    /**
     * 设置是否抛出实时合成音频
     *
     * @param outRealData 抛出音频标志位
     */
    public void setOutRealData(boolean outRealData) {
        this.outRealData = outRealData;
    }

    /**
     * 是否抛出实时合成音频
     *
     * @return 标志位
     */
    public boolean isOutRealData() {
        return outRealData;
    }


    //public void setSaveToFile(boolean )

    @TargetApi(Build.VERSION_CODES.M)
    public void setAudioAttributes(AudioAttributes audioAttributes) {
        this.audioAttributes = audioAttributes;
    }

    /**
     * @return AudioAttributes
     */
    @TargetApi(Build.VERSION_CODES.M)
    public AudioAttributes getAudioAttributes() {
        return audioAttributes;
    }

    /**
     * 获取是否强制播放为 StreamType
     *
     * @return
     */
    public boolean isUseStreamType() {
        return useStreamType;
    }

    /**
     * 设置是否强制播放为 StreamType
     *
     * @param useStreamType 默认是false
     */
    public void setUseStreamType(boolean useStreamType) {
        this.useStreamType = useStreamType;
    }

    public void setUtteranceId(String utteranceId) {
        this.utteranceId = utteranceId;
    }

    public String getUtteranceId() {
        return utteranceId;
    }

    /**
     * 获取保存的合成音频文件名
     *
     * @return path 返回文件保存路径
     */
    public String getSaveAudioFileName() {
        if (!TextUtils.isEmpty(saveAudioFileName)) {
            return this.saveAudioFileName + File.separator + (System.currentTimeMillis() + ".wav");
        }
        return "";
    }

    /**
     * 设置保存的合成音频的文件名
     *
     * @param saveAudioFileName
     */
    public void setSaveAudioFileName(String saveAudioFileName) {
        this.saveAudioFileName = saveAudioFileName;
    }

    public JSONObject toJson() {
        return null;
    }
}