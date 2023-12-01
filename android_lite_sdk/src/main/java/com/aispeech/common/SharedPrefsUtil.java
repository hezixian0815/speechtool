package com.aispeech.common;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefsUtil {
    private static final String TAG = "SharedPrefsUtil";


    /**
     * Local TTS模块sp名，建议各模块分开存储，避免加载多余数据
     */
    private final static String LOCAL_TTS = "sp_local_tts";
    private static final String KEY_LOCAL_TTS_CUSTOM_AUDIO = "KEY_LOCAL_TTS_CUSTOM_AUDIO";


    public static void putLocalTTSCustomAudio(Context mContext, String customAudio) {
        SharedPreferences.Editor sp = mContext.getSharedPreferences(LOCAL_TTS, Context.MODE_PRIVATE).edit();
        sp.putString(KEY_LOCAL_TTS_CUSTOM_AUDIO, customAudio);
        sp.apply();
    }

    public static String getLocalTTSCustomAudio(Context mContext) {
        SharedPreferences sp = mContext.getSharedPreferences(LOCAL_TTS, Context.MODE_PRIVATE);
        return sp.getString(KEY_LOCAL_TTS_CUSTOM_AUDIO, null);
    }


}
