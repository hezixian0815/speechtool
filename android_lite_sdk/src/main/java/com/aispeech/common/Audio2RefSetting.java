package com.aispeech.common;

import android.content.Context;
import android.media.AudioManager;
import android.text.TextUtils;

/**
 * 直接获取带参考音的音频，
 * 通过以下设置，可将带2路参考音的原始音频直接存储在 {@link #keyPcmSaveLocation} 内
 */
public class Audio2RefSetting {

    private static final String TAG = "Audio2RefSetting";
    private static final String keyPcmSaveEnable = "pcm_save_enable";
    private static final String keyPcmSaveLocation = "pcm_save_location";

    public static void set(Context context, boolean enable, String directoryPath) {
        if (enable && TextUtils.isEmpty(directoryPath))
            return;
        if (directoryPath == null)
            directoryPath = "";

        Log.d(TAG, "enable " + enable + " directoryPath " + directoryPath);
        AudioManager mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        mAudioManager.setParameters(keyPcmSaveLocation + "=" + directoryPath);
        mAudioManager.setParameters(keyPcmSaveEnable + "=" + enable);
    }

}
