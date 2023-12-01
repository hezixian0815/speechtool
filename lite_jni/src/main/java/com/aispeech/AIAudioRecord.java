package com.aispeech;

import com.aispeech.common.Log;

/**
 * Created by yu on 2018/9/19.
 */

public class AIAudioRecord {
    public static final String TAG = "JNI-AIAudioRecord";
    static {
        try {
            Log.d(TAG, "before load aispeechaudio library");
            System.loadLibrary("aispeechaudio");
            Log.d(TAG, "after load aispeechaudio library");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libaispeechaudio.so, and put it in your libs dir!");
        }
    }

    public native final int _native_setup(int audioSource, int sampleRate, int channelNum);

    public native final int _native_start();

    public native final int _native_stop();

    public native final int _native_read_in_byte_array(byte[] audioData, int offsetInBytes, int sizeInBytes);

}
