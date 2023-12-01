package com.aispeech.export.config;

public class AILocalMdsConfig {

    /**
     * 资源文件
     */
    private String mdsResource;

    /**
     * 输入音频通道数
     */
    private int channels;

    public String getMdsResource() {
        return mdsResource;
    }

    /**
     * 设置mds资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param mdsResource mds资源
     */
    public void setMdsResource(String mdsResource) {
        this.mdsResource = mdsResource;
    }

    public int getChannels() {
        return channels;
    }

    /**
     * 输入音频通道数
     *
     * @param channels 音频通道数
     */
    public void setChannels(int channels) {
        this.channels = channels;
    }

    @Override
    public String toString() {
        return "AILocalMdsConfig{" +
                "mdsResource='" + mdsResource + '\'' +
                ", channels=" + channels +
                '}';
    }
}
