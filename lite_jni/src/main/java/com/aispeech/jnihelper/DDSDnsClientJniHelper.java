package com.aispeech.jnihelper;

import com.aispeech.DDSDnsClient;
import com.aispeech.common.Log;

public class DDSDnsClientJniHelper {

    private static final String TAG = "DDSDnsClientJniHelper";
    private static IDUIJni duiJni = null;

    private DDSDnsClientJniHelper() {
    }

    public static IDUIJni getDuiJni() {
        return duiJni;
    }

    public static void setDuiJni(IDUIJni duiJni) {
        DDSDnsClientJniHelper.duiJni = duiJni;
    }

    public static String dds_get_host_by_name(String hostname, int udpTimeOutSec, int httpTimeOutSec) {
        if (duiJni == null) {
            try {
                return DDSDnsClient.dds_get_host_by_name(hostname, udpTimeOutSec, httpTimeOutSec, null);
            } catch (NoClassDefFoundError error) {
                Log.d(TAG, "dds_get_host_by_name  " + error);
            }
            return "";
        } else
            return duiJni.DnsDdsGetHostByName(hostname, udpTimeOutSec, httpTimeOutSec);
    }
}
