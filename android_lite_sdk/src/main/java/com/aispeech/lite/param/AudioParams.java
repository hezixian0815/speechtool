/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.param;

import android.media.AudioFormat;

import com.aispeech.common.JSONUtil;
import com.aispeech.lite.AISampleRate;

import org.json.JSONObject;

/**
 * 使用的音频配置，如果应用使用思必驰语音引擎不包括合成(tts)、资源定制(res)、唤醒(wakeup)等，则需要配置此参数
 * <ul>
 * <li>设置音频类型audiotype {@link #setAudioType(String)}</li>
 * <li>设置采样率samplerate {@link #setSampleRate(AISampleRate)}</li>
 * </ul>
 */
public class AudioParams {

    public static final String KEY_AUDIO = "audio";

    public static final String KEY_CHANNEL = "channel";
    public static final String KEY_AUDIO_TYPE = "audioType";
    public static final String KEY_SAMPLE_BYTES = "sampleBytes";
    public static final String KEY_SAMPLE_RATE = "sampleRate";
//    public static final String DEFAULT_AUDIO_TYPE = "wav";
    public static final String DEFAULT_AUDIO_TYPE = "ogg";
    public static final int DEFAULT_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private JSONObject JSON = new JSONObject();

    private String audioType;
    private int channel;
    private int sampleBytes;
    private int intervalTime = 100;
    private AISampleRate sampleRate;

    public AudioParams() {
        setChannel(1);
        setAudioType(DEFAULT_AUDIO_TYPE);
        setSampleBytes(DEFAULT_AUDIO_ENCODING);
        setSampleRate(AISampleRate.SAMPLE_RATE_16K);
        //  JSONUtil.putQuietly(JSON, "compress", "speex");
    }

    // private only support channel 1
    private void setChannel(int channel) {
        this.channel = channel;
        JSONUtil.putQuietly(JSON, KEY_CHANNEL, channel);
    }


    public void setAudioType(String audioType) {
        this.audioType = audioType;
        JSONUtil.putQuietly(JSON, KEY_AUDIO_TYPE, audioType);
    }


    private void setSampleBytes(int sampleBytes) {
        this.sampleBytes = sampleBytes;
        JSONUtil.putQuietly(JSON, KEY_SAMPLE_BYTES, sampleBytes);
    }

    /**
     * 设置语音音频采样率
     * 
     * @param sampleRate
     *            音频采样率，目前仅支持{@link AISampleRate#SAMPLE_RATE_16K}和
     *            {@link AISampleRate#SAMPLE_RATE_8K}
     * @see AISampleRate
     */
    public void setSampleRate(AISampleRate sampleRate) {
        this.sampleRate = sampleRate;
        JSONUtil.putQuietly(JSON, KEY_SAMPLE_RATE, sampleRate.getValue());
    }

    /**
     * 获取语音音频采样率
     * 
     * @return 音频采样率
     * @see AISampleRate
     */
    public AISampleRate getSampleRate() {
        return sampleRate;
    }

    /**
     * 设置音频抛出时间间隔
     * 
     * @param interval
     *            单位ms
     */
    public void setIntervalTime(int interval) {
        this.intervalTime = interval;
    }

    /**
     * 获取音频抛出时间间隔
     * 
     * @return 时间间隔，单位ms
     */
    public int getIntervalTime() {
        return intervalTime;
    }

    /**
     * 参数JSON化
     * 
     * @return JSONObject
     */
    public JSONObject toJSON() {
        return JSON;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

}
