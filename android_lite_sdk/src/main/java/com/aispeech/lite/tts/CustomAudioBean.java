package com.aispeech.lite.tts;

public class CustomAudioBean {
    /**
     * 自定义录音文本
     */
    private String text;

    /**
     * 自定义录音文件路径
     */
    private String audioPath;

    public CustomAudioBean(String text, String audioPath) {
        this.text = text;
        this.audioPath = audioPath;
    }

    public String getText() {
        return text;
    }

    public String getAudioPath() {
        return audioPath;
    }


    @Override
    public String toString() {
        return "{" +
                "text='" + text + '\'' +
                ", audioPath='" + audioPath + '\'' +
                '}';
    }
}
