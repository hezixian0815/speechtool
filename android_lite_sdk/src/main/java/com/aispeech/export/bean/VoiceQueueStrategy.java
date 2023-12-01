package com.aispeech.export.bean;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/8/1
 */
public class VoiceQueueStrategy {

    int maxVoiceQueueSize = 0;

    int ignoreSize = 0;


    public VoiceQueueStrategy(int maxVoiceQueueSize, int ignoreSize) {
        this.maxVoiceQueueSize = maxVoiceQueueSize;
        this.ignoreSize = ignoreSize;
    }

    public int getMaxVoiceQueueSize() {
        return maxVoiceQueueSize;
    }

    public void setMaxVoiceQueueSize(int maxVoiceQueueSize) {
        this.maxVoiceQueueSize = maxVoiceQueueSize;
    }

    public int getIgnoreSize() {
        return ignoreSize;
    }

    public void setIgnoreSize(int ignoreSize) {
        this.ignoreSize = ignoreSize;
    }
}
