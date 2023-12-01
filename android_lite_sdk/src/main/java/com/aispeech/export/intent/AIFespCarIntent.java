package com.aispeech.export.intent;

import com.aispeech.lite.base.BaseIntent;

public class AIFespCarIntent extends BaseIntent {

    private String saveAudioFilePath;

    private boolean inputContinuousAudio = true;

    /**
     * feed段音频时是否增加额外的音频，目的是为了使本应唤醒的音频更容易唤醒
     */
    private boolean addExtraAudioWhenFeedNotContinuousAudio = false;

    /**
     * dump唤醒音频保存的文件夹，比如/sdcard/speech/dumpwkp。不设置则不dump音频
     */
    private String dumpWakeupAudioPath;

    /**
     * 设置dump唤醒点回退音频的时间，默认5000ms。
     */
    private int dumpWakeupTime = 5000;

    /**
     * 车载双麦时才有效。当定位模式时，根据唤醒角度自动设置成主驾模式或者副驾模式。
     * false 不自动设置（default），true 自动设置
     */
    private boolean autoHoldBeamforming = false;

    /**
     * doa信息可能为30、90、180角度信息，可能为 1、2 等主副驾信息
     * true 统一处理为主副驾信息，false 不处理doa信息，直接回传给用户处理
     */
    private boolean processDoaInformation = true;

    public boolean isProcessDoaInformation() {
        return processDoaInformation;
    }

    /**
     * doa信息可能为30、90、180角度信息，可能为 1、2 等主副驾信息
     *
     * @param processDoaInformation true 统一处理为主副驾信息，false 不处理doa信息，直接回传给用户处理
     */
    public void setProcessDoaInformation(boolean processDoaInformation) {
        this.processDoaInformation = processDoaInformation;
    }

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

    /**
     * 设置是否输入实时的长音频，默认接受长音频为true(如果是一二级唤醒，即每个唤醒词独立且非实时，则需要设置为false，如果不设置会影响性能)
     * 当设置为false时,每次送一段音频段都会给予是否唤醒的反馈，如果没有被唤醒，则抛出wakeupWord:null, confidence:0的信息
     *
     * @param inputContinuousAudio 是否输入实时的长音频,默认为true
     */
    public void setInputContinuousAudio(boolean inputContinuousAudio) {
        this.inputContinuousAudio = inputContinuousAudio;
    }

    public boolean isAddExtraAudioWhenFeedNotContinuousAudio() {
        return addExtraAudioWhenFeedNotContinuousAudio;
    }

    /**
     * 设置是否输入实时的长音频，默认接受长音频为true(如果是一二级唤醒，即每个唤醒词独立且非实时，则需要设置为false，如果不设置会影响性能)
     * <p>
     * 当设置为false时,每次送一段音频段都会给予是否唤醒的反馈，如果没有被唤醒，则抛出wakeupWord:null, confidence:0的信息
     * </p>
     * <p>
     * 当 inputContinuousAudio 设置为  false 时，可以设置 addExtraAudioWhenFeedNotContinuousAudio 为 true，
     * 会在段音频尾部再加上一些音频，使本应能够唤醒的音频更容易唤醒
     * </p>
     * <b>addExtraAudioWhenFeedNotContinuousAudio 设置只有双麦有效</b>
     *
     * @param inputContinuousAudio                    是否输入实时的长音频,默认为true
     * @param addExtraAudioWhenFeedNotContinuousAudio false（默认）不增加额外音频，true 增加额外音频
     */
    public void setInputContinuousAudio(boolean inputContinuousAudio, boolean addExtraAudioWhenFeedNotContinuousAudio) {
        this.inputContinuousAudio = inputContinuousAudio;
        this.addExtraAudioWhenFeedNotContinuousAudio = addExtraAudioWhenFeedNotContinuousAudio;
    }

    public boolean isInputContinuousAudio() {
        return inputContinuousAudio;
    }

    public String getDumpWakeupAudioPath() {
        return dumpWakeupAudioPath;
    }

    /**
     * 设置dump唤醒音频保存的文件夹，比如/sdcard/speech/dumpwkp。不设置则不dump音频
     *
     * @param dumpWakeupAudioPath dump唤醒音频保存的文件夹
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

    /**
     * @return autoHoldBeamforming
     * @deprecated 该方法不再使用
     */
    public boolean isAutoHoldBeamforming() {
        return autoHoldBeamforming;
    }

    /**
     * 车载双麦时才有效。当定位模式时，根据唤醒角度自动设置成主驾模式或者副驾模式。
     *
     * @param autoHoldBeamforming false 不自动设置（default），true 自动设置
     * @deprecated 该方法不再使用
     */
    public void setAutoHoldBeamforming(boolean autoHoldBeamforming) {
        this.autoHoldBeamforming = autoHoldBeamforming;
    }

    @Override
    public String toString() {
        return "AILocalSignalAndWakeupIntent{" +
                "useCustomFeed=" + useCustomFeed +
                ", saveAudioPath='" + saveAudioFilePath + '\'' +
                ", inputContinuousAudio=" + inputContinuousAudio +
                ", addExtraAudioWhenFeedNotContinuousAudio=" + addExtraAudioWhenFeedNotContinuousAudio +
                ", dumpWakeupAudioPath='" + dumpWakeupAudioPath + '\'' +
                ", dumpWakeupTime=" + dumpWakeupTime +
                ", autoSetDriveMode=" + autoHoldBeamforming +
                ", processDoaInformation = " + processDoaInformation +
                '}';
    }
}
