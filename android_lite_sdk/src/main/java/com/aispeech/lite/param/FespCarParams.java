package com.aispeech.lite.param;

import android.text.TextUtils;

import java.io.File;

/**
 * Created by wuwei on 18-6-4.
 */

public class FespCarParams extends SpeechParams {

    private boolean inputContinuousAudio = true;
    private String saveAudioFilePath;
    private String mSaveWakeupCutDataPath;
    /**
     * 按照doa角度自动设置beamforming指向，该设置仅在定位模式下生效
     */
    private boolean autoHoldBeamforming = false;

    public boolean isInputContinuousAudio() {
        return inputContinuousAudio;
    }

    public void setInputContinuousAudio(boolean inputContinuousAudio) {
        this.inputContinuousAudio = inputContinuousAudio;
    }

    public String getSaveAudioFilePath() {
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
        return mSaveWakeupCutDataPath;
    }

    public boolean isAutoHoldBeamforming() {
        return autoHoldBeamforming;
    }

    public void setAutoHoldBeamforming(boolean autoHoldBeamforming) {
        this.autoHoldBeamforming = autoHoldBeamforming;
    }
}
