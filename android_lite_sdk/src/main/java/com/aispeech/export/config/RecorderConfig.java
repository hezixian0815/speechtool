package com.aispeech.export.config;

import android.media.MediaRecorder;

/**
 * 录音机初始化配置
 *
 * @author hehr
 */
public class RecorderConfig {


    /**
     * 录音机采集数据间隔
     */
    public int intervalTime = 100;
    /**
     * audio source
     */
    public int audioSource = MediaRecorder.AudioSource.MIC;

    /**
     * 录音类型
     * 默认{@link RecorderConfig#TYPE_COMMON_MIC}
     */
    public int recorderType = TYPE_COMMON_MIC;

    public int getIntervalTime() {
        return intervalTime;
    }

    public int getAudioSource() {
        return audioSource;
    }

    public int getRecorderType() {
        return recorderType;
    }

    /**
     * 单麦模式,获取单通道音频
     */
    public static final int TYPE_COMMON_MIC = 0;


    /**
     * echo模式,获取双通道音频(包含一路参考音)
     */
    public static final int TYPE_COMMON_ECHO = 4;

    public RecorderConfig() {
    }

    private RecorderConfig(int intervalTime, int audioSource, int recorderType) {
        this.intervalTime = intervalTime;
        this.audioSource = audioSource;
        this.recorderType = recorderType;
    }

    private RecorderConfig(Builder builder) {
        this(builder.intervalTime, builder.audioSource, builder.recorderType);
    }

    public static class Builder {


        public int intervalTime = 100;

        public int audioSource = MediaRecorder.AudioSource.MIC;

        public int recorderType = TYPE_COMMON_MIC;


        /**
         * 设置录音机采集数据间隔
         * @param intervalTime  录音机采样间隔,默认间隔100ms
         * @return {@link Builder}
         */
        public Builder setIntervalTime(int intervalTime) {
            this.intervalTime = intervalTime;
            return this;
        }

        /**
         * 设置audioRecorder的声音源
         *
         * @param audioSource 默认  {@link MediaRecorder.AudioSource#MIC}
         *                    可选值：{@link MediaRecorder.AudioSource#MIC} 和 {@link MediaRecorder.AudioSource#VOICE_RECOGNITION}
         * @return {@link Builder}
         */
        public Builder setAudioSource(int audioSource) {
            this.audioSource = audioSource;
            return this;
        }

        /**
         * 设置录音机类型
         *
         * @param recorderType 默认 {@link RecorderConfig#TYPE_COMMON_MIC }
         *                     可选值 {@link RecorderConfig#TYPE_COMMON_MIC } 和 {@link RecorderConfig#TYPE_COMMON_ECHO }
         * @return {@link Builder}
         */
        public Builder setRecorderType(int recorderType) {
            this.recorderType = recorderType;
            return this;
        }

        public RecorderConfig create() {
            return new RecorderConfig(this);
        }
    }


}
