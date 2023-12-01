package com.aispeech.export.intent;

public class AIDmaspIntent {

    private String saveAudioFilePath;

    /**
     * dump唤醒音频保存的文件夹，比如/sdcard/speech/dumpwkp。不设置则不dump音频
     */
    private String dumpWakeupAudioPath;

    /**
     * 设置dump唤醒点回退音频的时间，默认5000ms。
     */
    private int dumpWakeupTime = 5000;

    /**
     * 设置音频保存路径，会保存原始多声道音频和经过 beamforming 后的单声道音频
     * 如果设置了就会保存，没设置不会保存
     *
     * @param saveAudioFilePath 文件夹路径
     **/
    public void setSaveAudioFilePath(String saveAudioFilePath) {
        this.saveAudioFilePath = saveAudioFilePath;
    }

    public String getSaveAudioFilePath() {
        return saveAudioFilePath;
    }

    public String getDumpWakeupAudioPath() {
        return dumpWakeupAudioPath;
    }

    /**
     * 设置dump唤醒音频保存的文件夹，比如/sdcard/speech/dumpwkp。不设置则不dump音频
     *
     * @param dumpWakeupAudioPath dump 唤醒音频保存的文件夹
     */
    public void setDumpWakeupAudioPath(String dumpWakeupAudioPath) {
        this.dumpWakeupAudioPath = dumpWakeupAudioPath;
    }

    /**
     * 设置dump唤醒点回退音频的时间，默认5000ms。
     *
     * @param dumpWakeupTime dump唤醒点回退音频的时长
     */
    public void setDumpWakeupTime(int dumpWakeupTime) {
        this.dumpWakeupTime = dumpWakeupTime;
    }

    public int getDumpWakeupTime() {
        return dumpWakeupTime;
    }

}
