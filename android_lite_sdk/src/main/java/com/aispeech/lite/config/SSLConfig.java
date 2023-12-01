package com.aispeech.lite.config;


public class SSLConfig extends LocalVadConfig {


    /**
     * ssl 一路vad触发后其他路等待时间，单位毫秒
     */
    private int waitTime = 32 * 4;

    /**
     * 每一帧音频时长,dmasp默认每帧音频长度32ms
     */
    private int frameLength = 32;

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }

    public int getFrameLength() {
        return frameLength;
    }

    public void setFrameLength(int frameLength) {
        this.frameLength = frameLength;
    }
}
