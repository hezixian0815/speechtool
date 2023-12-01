package com.aispeech.export.config;

/**
 * LocalAsrpp init 的配置信息
 */
public class AILocalAsrppConfig {

    /**
     * 设置性别识别资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     */
    private String asrppResource;

    /**
     * 设置本地vad资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     */
    private String vadResource;

    /**
     * 是否启用本地vad, default is {@value}
     */
    private boolean vadEnable = true;

    /**
     * 设置VAD右边界,单位：ms,默认{@value}ms
     */
    private int vadPauseTime = 300;

    /**
     * 设置是否启用本地vad
     *
     * @param vadEnable true:使用Vad；false:禁止Vad，默认为true
     */
    public void setVadEnable(boolean vadEnable) {
        this.vadEnable = vadEnable;
    }

    /**
     * 设置VAD右边界
     *
     * @param vadPauseTime vadPauseTime 单位：ms,默认300
     */
    public void setVadPauseTime(int vadPauseTime) {
        this.vadPauseTime = vadPauseTime;
    }

    /**
     * 设置性别识别资源
     * <p>1. 如在sd里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param asrppResource 资源的绝对路径或资源名称
     */
    public void setAsrppResource(String asrppResource) {
        this.asrppResource = asrppResource;
    }


    /**
     * 设置本地vad资源
     * <p>1. 如在sd里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param vadResource 资源的绝对路径或资源名称
     */
    public void setVadResource(String vadResource) {
        this.vadResource = vadResource;
    }

    public boolean isVadEnable() {
        return vadEnable;
    }

    public String getAsrppResource() {
        return asrppResource;
    }

    public String getVadResource() {
        return vadResource;
    }

    public int getVadPauseTime() {
        return vadPauseTime;
    }

    @Override
    public String toString() {
        return "AILocalAsrppConfig{" +
                "asrppResource='" + asrppResource + '\'' +
                ", vadResource='" + vadResource + '\'' +
                ", vadEnable=" + vadEnable +
                ", vadPauseTime=" + vadPauseTime +
                '}';
    }
}
