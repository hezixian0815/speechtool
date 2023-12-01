package com.aispeech.jnihelper;

import com.aispeech.auth.Auth;
import com.aispeech.common.Log;

public class AuthJniHelper {

    private static final String TAG = "AuthJniHelper";
    private static IDUIJni duiJni = null;

    public static IDUIJni getDuiJni() {
        return duiJni;
    }

    private AuthJniHelper() {
    }

    public static void setDuiJni(IDUIJni duiJni) {
        AuthJniHelper.duiJni = duiJni;
    }

    public static int decryptProfile(String profile, byte[] decProfile) {
        if (duiJni == null) {
            try {
                return Auth.DecryptProfile(profile, decProfile);
            } catch (NoClassDefFoundError error) {
                Log.d(TAG, "DecryptProfile  " + error);
            } catch (UnsatisfiedLinkError error) {
                Log.d(TAG, "DecryptProfile  " + error);
            }
            return -99;
        } else
            return duiJni.AuthDecryptProfile(profile, decProfile);
    }

    public static boolean checkApikey(String apiKey, String secretCode) {
        if (duiJni == null) {
            try {
                return Auth.CheckApikey(apiKey, secretCode);
            } catch (NoClassDefFoundError error) {
                Log.d(TAG, "CheckApikey  " + error);
            } catch (UnsatisfiedLinkError error) {
                Log.d(TAG, "CheckApikey  " + error);
            }
            return false;
        } else
            return duiJni.AuthCheckApikey(apiKey, secretCode);
    }
}
