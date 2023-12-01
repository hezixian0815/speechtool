package com.aispeech.lite.param;

import android.text.TextUtils;

import java.io.File;

/**
 * Created by wuwei on 18-6-4.
 */

public class FespxCarParams extends SpeechParams {

    private boolean inputContinuousAudio = true;
    /**
     * feed段音频时是否增加额外的音频，目的是为了使本应唤醒的音频更容易唤醒
     */
    private boolean addExtraAudioWhenFeedNotContinuousAudio = false;

    private String mSaveWakeupCutDataPath;
    private boolean autoSetDriveMode;

    public boolean isInputContinuousAudio() {
        return inputContinuousAudio;
    }

    public void setInputContinuousAudio(boolean inputContinuousAudio) {
        this.inputContinuousAudio = inputContinuousAudio;
    }

    public boolean isAddExtraAudioWhenFeedNotContinuousAudio() {
        return addExtraAudioWhenFeedNotContinuousAudio;
    }

    public void setAddExtraAudioWhenFeedNotContinuousAudio(boolean addExtraAudioWhenFeedNotContinuousAudio) {
        this.addExtraAudioWhenFeedNotContinuousAudio = addExtraAudioWhenFeedNotContinuousAudio;
    }


    public void setSaveWakeupCutDataPath(String path) {
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        this.mSaveWakeupCutDataPath = path;
    }

    public String getSaveWakeupCutFilePath() {
        return mSaveWakeupCutDataPath;
    }

    public boolean isAutoSetDriveMode() {
        return autoSetDriveMode;
    }

    public void setAutoSetDriveMode(boolean autoSetDriveMode) {
        this.autoSetDriveMode = autoSetDriveMode;
    }
}
