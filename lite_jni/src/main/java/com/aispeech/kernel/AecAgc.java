package com.aispeech.kernel;

import com.aispeech.common.Log;

public class AecAgc {

    private static final String TAG = "AecAgc";
    private static boolean loadSoSuc;

    static {
        try {
            System.loadLibrary("aec_agc");
            Log.d(TAG, "after load AecAgc library");
            loadSoSuc = true;
        } catch (UnsatisfiedLinkError e) {
            loadSoSuc = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libaec_agc.so, and put it in your libs dir!");
        }
    }

    private long engineId;

    public static boolean isLoadSoSuc() {
        return loadSoSuc;
    }

    private static native long dds_aec_agc_new(String config);

    private static native int dds_aec_agc_start(long engine, String param);

    private static native int dds_aec_agc_feed(long engine, byte[] data, int size);

    private static native int dds_aec_agc_set(long engine, String param);

    private static native int dds_aec_agc_cancel(long engine);

    private static native int dds_aec_agc_stop(long engine);

    private static native int dds_aec_agc_delete(long engine);

    private static native int dds_aec_agc_setaeccb(long engine, aec_callback callback);

    private static native int dds_aec_agc_setvoipcb(long engine, voip_callback callback);

    public long init(String cfg) {
        engineId = dds_aec_agc_new(cfg);
        Log.d(TAG, "init:" + engineId);
        return engineId;
    }

    public boolean setCallback(aec_callback aecCallback, voip_callback voipCallback) {
        int ret = dds_aec_agc_setaeccb(engineId, aecCallback);
        Log.d(TAG, "dds_aec_agc_setaeccb ret " + ret);
        if (ret != 0) {
            return false;
        }
        ret = dds_aec_agc_setvoipcb(engineId, voipCallback);
        Log.d(TAG, "dds_aec_agc_setvoipcb ret " + ret);
        return ret == 0;
    }

    public int start(String param) {
        int ret = dds_aec_agc_start(engineId, param);
        Log.d(TAG, "start:" + ret + " " + param);
        return ret < 0 ? -1 : ret;
    }

    public int set(String setParam) {
        int ret = dds_aec_agc_set(engineId, setParam);
        Log.d(TAG, "set:" + ret + " " + setParam);
        return ret;
    }

    public int feed(byte[] buffer, int size) {
        return dds_aec_agc_feed(engineId, buffer, size);
    }

    public int cancel() {
        int ret = dds_aec_agc_cancel(engineId);
        Log.d(TAG, "cancel():" + ret + " " + engineId);
        return ret;
    }

    public int stop() {
        int ret = dds_aec_agc_stop(engineId);
        Log.d(TAG, "stop():" + ret + " " + engineId);
        return ret;
    }

    public int destroy() {
        int ret = dds_aec_agc_delete(engineId);
        Log.d(TAG, "destroy():" + ret + " " + engineId);
        engineId = 0;
        return ret;
    }

    public static class aec_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    public static class voip_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

}
