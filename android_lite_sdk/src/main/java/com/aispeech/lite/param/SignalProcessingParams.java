package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.lite.AISpeechSDK;

import java.io.File;

/**
 * Created by wuwei on 18-6-4.
 */

public class SignalProcessingParams extends SpeechParams {

    private boolean inputContinuousAudio = true;
    /**
     * feed段音频时是否增加额外的音频，目的是为了使本应唤醒的音频更容易唤醒
     */
    private boolean addExtraAudioWhenFeedNotContinuousAudio = false;

    private String saveAudioFilePath;
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

    public String getSaveAudioFilePath() {
        if (TextUtils.isEmpty(saveAudioFilePath)) {
            if (!TextUtils.isEmpty(AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH)) {
                return AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH + "/fespWakeup";
            }
        }
        return saveAudioFilePath;
    }

    public void setSaveAudioFilePath(String path) {
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        this.saveAudioFilePath = path;
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
        if (TextUtils.isEmpty(mSaveWakeupCutDataPath)) {
            if (!TextUtils.isEmpty(AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH)) {
                return AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH + "/fespCut";
            }
        }
        return mSaveWakeupCutDataPath;
    }

    public boolean isAutoSetDriveMode() {
        return autoSetDriveMode;
    }

    public void setAutoSetDriveMode(boolean autoSetDriveMode) {
        this.autoSetDriveMode = autoSetDriveMode;
    }
}
