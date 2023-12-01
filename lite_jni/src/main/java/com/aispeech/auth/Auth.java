package com.aispeech.auth;

import com.aispeech.common.Log;

public class Auth {

    private static final String TAG = "DUI-Auth";
    static final String KEY_PRODUCT_ID = "productId";
    static final String KEY_DEVICE_ID = "deviceId";
    static final String KEY_DEVICE_NAME = "deviceName";
    static final String KEY_DEVICE_SECRET = "deviceSecret";
    static final String KEY_DEVICE_INFO = "deviceInfo";
    static final String KEY_TIMES_LIMIT = "timesLimit";
    static final String KEY_BIND_API_KEY = "bindApiKey";
    static final String KEY_EXPIRE = "expire";
    static final String KEY_SCOPE = "scope";
    static final String KEY_DEFAULT_SCOPE = "all";
    static final String KEY_TRIAL = "trial";
    static final String KEY_DEFAULT_PROFILE_NAME = ".profile";
    static final String KEY_ALLOW = "allow";

    private Auth() {
    }

    static {
        try {
            Log.d(TAG, "before load ca library");
            System.loadLibrary("liteca");
            Log.d(TAG, "after load ca library");
        } catch (Exception e) {
            Log.e(TAG, "load libliteca.so failed, please check jniLibs folder");
            try {
                Log.d(TAG, "before2 load ca library");
                System.loadLibrary("ca");
                Log.d(TAG, "after2 load ca library");
            } catch (Exception e1) {
                e.printStackTrace();
                Log.e(TAG, "load libca.so failed, please check jniLibs folder");
            }
        }
    }


    public static native int DecryptProfile(String profile, byte[] decProfile);

    public static native boolean CheckApikey(String apiKey, String secretCode);

}
