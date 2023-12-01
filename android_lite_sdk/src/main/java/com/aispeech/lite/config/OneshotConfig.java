package com.aispeech.lite.config;

/**
 * oneshot config
 *
 * @author hehr
 */
public class OneshotConfig {

    private int cacheAudioTime = 1200; //默认cache 1.2秒的音频数据

    private int middleTime = 400;//毫秒 oneshot check 唤醒到vad状态调变的最大允许时间

    private String[] words;//oneshot响应词

    private LocalVadConfig vadConfig;//vad 配置

    private String saveAudioPath;

    public String[] getWords() {
        return words;
    }

    public void setWords(String[] words) {
        this.words = words;
    }

    public int getCacheAudioTime() {
        return cacheAudioTime;
    }

    public void setCacheAudioTime(int cacheAudioTime) {
        this.cacheAudioTime = cacheAudioTime;
    }

    public int getMiddleTime() {
        return middleTime;
    }

    public void setMiddleTime(int middleTime) {
        this.middleTime = middleTime;
    }

    public LocalVadConfig getVadConfig() {
        return vadConfig;
    }

    public void setVadConfig(LocalVadConfig vadConfig) {
        this.vadConfig = vadConfig;
    }

    public String getSaveAudioPath() {
        return saveAudioPath;
    }

    public void setSaveAudioPath(String saveAudioPath) {
        this.saveAudioPath = saveAudioPath;
    }

    @Override
    public String toString() {
        return "AIOneshotConfig{" +
                "cacheAudioTime=" + cacheAudioTime +
                ", middleTime=" + middleTime +
                ", wakeupWord=" + words +
                ", vadConfig=" + vadConfig.toJSON() +
                ", saveAudioPath = " + saveAudioPath +
                '}';
    }
}
