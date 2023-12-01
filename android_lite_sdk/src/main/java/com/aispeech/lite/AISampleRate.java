/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite;

import com.aispeech.common.Log;

/**
 * 采样率定义类
 */
public class AISampleRate {

    private static final String TAG = AISampleRate.class.getName();

    public static final String KEY_SAMPLE_RATE = "sampleRate";

    /**
     * 48000采样率
     */
    public static final AISampleRate SAMPLE_RATE_48K = new AISampleRate(48000);

    /**
     * 44100采样率
     */
    public static final AISampleRate SAMPLE_RATE_44K = new AISampleRate(44100);

    /**
     * 32000采样率
     */
    public static final AISampleRate SAMPLE_RATE_32K = new AISampleRate(32000);

    /**
     * 24000采样率
     */
    public static final AISampleRate SAMPLE_RATE_24K = new AISampleRate(24000);

    /**
     * 22050采样率，云端TTS使用
     */
    public static final AISampleRate SAMPLE_RATE_22K = new AISampleRate(22050);

    /**
     * 16000采样率
     */
    public static final AISampleRate SAMPLE_RATE_16K = new AISampleRate(16000);

    /**
     * 11025采样率,云端TTS使用
     */
    public static final AISampleRate SAMPLE_RATE_11K = new AISampleRate(11025);

    /**
     * 8000采样率
     */
    public static final AISampleRate SAMPLE_RATE_8K = new AISampleRate(8000);

    /**
     * 将数值转换为采样率实例
     *
     * @param sampleRate sampleRate
     * @return AISampleRate AISampleRate实例
     */
    public static AISampleRate toAISampleRate(int sampleRate) {
        if (sampleRate == SAMPLE_RATE_16K.getValue()) {
            return SAMPLE_RATE_16K;
        } else if (sampleRate == SAMPLE_RATE_8K.getValue()) {
            return SAMPLE_RATE_8K;
        }  else if (sampleRate == SAMPLE_RATE_24K.getValue()) {
            return SAMPLE_RATE_24K;
        } else {
            Log.w(TAG, "Unsupported sampleRate!");
            return null;
        }
    }

    int sampleRate;

    private AISampleRate(int rate) {
        this.sampleRate = rate;
    }

    /**
     * 获取Int类型的采样率值
     *
     * @return Int类型的采样率
     */
    public int getValue() {
        return sampleRate;
    }

    @Override
    public boolean equals(Object aiSampleRate) {
        if (aiSampleRate == this) {
            return true;
        } /*else if (aiSampleRate.getValue() == this.getValue()) {
            return true;
        }*/ else {
            return false;
        }
    }

}
