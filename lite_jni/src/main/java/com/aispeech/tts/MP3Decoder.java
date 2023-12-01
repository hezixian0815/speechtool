package com.aispeech.tts;

import com.aispeech.common.Log;

public class MP3Decoder {
    private static final String TAG = "MP3Decoder";

    static {
        try {
            System.loadLibrary("lame");
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Please check useful liblame.so, and put it in your libs dir!");
        }
    }

    public static native void init();

    public static native int decode(byte[] mp3_buf, int len, short[] pcm_l, short[] pcm_r);

    public static native void destroy();

    public synchronized void initDecoder() {
        Log.d(TAG, "init");
        init();
    }

    public synchronized int processDecode(byte[] mp3_buf, int len, short[] pcm_l, short[] pcm_r) {
        Log.d(TAG, "decode");
        return decode(mp3_buf, len, pcm_l, pcm_r);
    }

    public synchronized void release() {
        Log.d(TAG, "release");
        destroy();
    }
}
