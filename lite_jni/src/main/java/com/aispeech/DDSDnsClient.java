package com.aispeech;

import com.aispeech.common.Log;

/**
 * Created by yu on 2018/8/13.
 */

public class DDSDnsClient {
    private static final String TAG = "DDSDnsClient";

    private DDSDnsClient() {
    }

    static {
        try {
            Log.d(TAG, "before load duidns library");
            System.loadLibrary("duidns");
            Log.d(TAG, "after load duidns library");
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libduidns.so, and put it in your libs dir!");

        }
    }

    public static native String dds_get_host_by_name(String hostname, int udpTimeOutSec, int httpTimeOutSec, String dnsServer);
}
