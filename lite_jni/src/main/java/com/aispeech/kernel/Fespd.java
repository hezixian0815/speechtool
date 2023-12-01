package com.aispeech.kernel;

import com.aispeech.common.Log;

public class Fespd extends Fespx {

    public static final String LIBRARY_NAME = "fespd";
    private static final String TAG = "Fespd";
    private static boolean mLoadSoOk = false;

    static {
        try {
            Log.d(TAG, "before load fespd library");
        //    System.loadLibrary(LIBRARY_NAME);
            Log.d(TAG, "after load fespd library");
            mLoadSoOk = true;
        } catch (UnsatisfiedLinkError e) {
            mLoadSoOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libfespd.so, and put it in your libs dir!");

        }
    }

    private long mFespdId;

    public static boolean isFespxSoValid() {
        return mLoadSoOk;
    }

    public static native long dds_fespd_new(String cfg);

    public static native int dds_fespd_getWakeupConfig(String cfg, Sspe.config_callback callback);

    public static native int dds_fespd_start(long id, String param);

    public static native int dds_fespd_set(long id, String setParam);

    public static native int dds_fespd_setwakeupcb(long id, Sspe.wakeup_callback wakeupCallback);

    public static native int dds_fespd_setdoacb(long id, Sspe.doa_callback doaCallback);

    public static native int dds_fespd_setbeamformingcb(long id, Sspe.beamforming_callback beamformingCallback);

    public static native int dds_fespd_feed(long id, byte[] data, int size);

    public static native int dds_fespd_stop(long id);

    public static native int dds_fespd_delete(long id);

    public static native int dds_fespd_get(long id, String param);

    public static native int dds_fespd_setvprintcutcb(long id, Sspe.vprintcut_callback callback);

    public static native int dds_fespd_setoutputcb(long id, Sspe.output_callback callback);

    public static native int dds_fespd_setinputcb(long id, Sspe.input_callback callback);

    public static native int dds_fespd_setechocb(long id, Sspe.echo_callback callback);

    public static native int dds_fespd_setsevcdoacb(long id, Sspe.sevc_doa_callback callback);

    public static native int dds_fespd_setsevcnoisecb(long id, Sspe.sevc_noise_callback callback);

    public static native int dds_fespd_setmultibfcb(long id, Sspe.multibf_callback callback);
    @Override
    public int getWakeupConfig(String initConfig, Sspe.config_callback callback) {
        int ret = dds_fespd_getWakeupConfig(initConfig, callback);
        Log.d(TAG, "dds_fespd_getWakeupConfig ret:" + ret);
        return ret;
    }

    @Override
    public long initFespx(String cfg) {
        Log.d(TAG, "init Fespd " + cfg);
        mFespdId = dds_fespd_new(cfg);
        Log.d(TAG, "init Fespd return " + mFespdId);
        return mFespdId;
    }

    @Override
    public int startFespx() {
        int ret = dds_fespd_start(mFespdId, "");
        Log.d(TAG, "start Fespd return " + ret);
        return ret;
    }

    @Override
    public int setFespx(String setParam) {
        return dds_fespd_set(mFespdId, setParam);
    }

    @Override
    public int setFespxWakeupcb(Sspe.wakeup_callback callback) {
        int ret = dds_fespd_setwakeupcb(mFespdId, callback);
        Log.d(TAG, "dds_fespd_setwakeupcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxDoacb(Sspe.doa_callback callback) {
        int ret = dds_fespd_setdoacb(mFespdId, callback);
        Log.d(TAG, "dds_fespd_setdoacb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxBeamformingcb(Sspe.beamforming_callback callback) {
        int ret = dds_fespd_setbeamformingcb(mFespdId, callback);
        Log.d(TAG, "dds_fespd_setbeamformingcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxVprintCutcb(Sspe.vprintcut_callback callback) {
        int ret = dds_fespd_setvprintcutcb(mFespdId, callback);
        Log.d(TAG, "dds_fespd_setvprintcutcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxInputcb(Sspe.input_callback callback) {
        int ret = dds_fespd_setinputcb(mFespdId, callback);
        Log.d(TAG, "dds_fespd_setinputcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxOutputcb(Sspe.output_callback callback) {
        int ret = dds_fespd_setoutputcb(mFespdId, callback);
        Log.d(TAG, "dds_fespd_setoutputcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxEchocb(Sspe.echo_callback callback) {
        int ret = dds_fespd_setechocb(mFespdId, callback);
        Log.d(TAG, "dds_fespd_setechocb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxSevcNoise(Sspe.sevc_noise_callback callback) {
        int ret = dds_fespd_setsevcnoisecb(mFespdId, callback);
        Log.d(TAG, "dds_fespd_setsevcnoisecb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxSevcDoa(Sspe.sevc_doa_callback callback) {
        int ret = dds_fespd_setsevcdoacb(mFespdId, callback);
        Log.d(TAG, "dds_fespd_setsevcdoacb ret : " + ret);
        return ret;
    }

    @Override
    public int setMultBfcb(Sspe.multibf_callback callback) {
        int ret = dds_fespd_setmultibfcb(mFespdId, callback);
        Log.d(TAG, "dds_fespd_setmultibfcb ret : " + ret);
        return ret;
    }

    @Override
    public int getFespx(String param) {
        int value = dds_fespd_get(mFespdId, param);
        Log.d(TAG, "getFespx " + param + ":" + value);
        return value;
    }

    @Override
    public int feedFespx(byte[] data, int size) {
        return dds_fespd_feed(mFespdId, data, size);
    }

    @Override
    public int stopFespx() {
        int ret = dds_fespd_stop(mFespdId);
        Log.d(TAG, "dds_fespd_stop ret : " + ret);
        return ret;
    }

    @Override
    public int destroyFespx() {
        int ret = dds_fespd_delete(mFespdId);
        Log.d(TAG, "destroy Fespd return " + ret);
        return ret;
    }
}
