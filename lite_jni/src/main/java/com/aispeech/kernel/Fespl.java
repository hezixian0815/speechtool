package com.aispeech.kernel;

import com.aispeech.common.Log;

/**
 * Created by wuwei on 18-6-1.
 */

public class Fespl extends Fespx {
    public static final String LIBRARY_NAME = "fespl";
    private static final String TAG = "Fespl";
    private static boolean mLoadFesplOk = false;

    static {
        try {
            Log.d(TAG, "before load fespl library");
         //   System.loadLibrary(LIBRARY_NAME);
            Log.d(TAG, "after load fespl library");
            mLoadFesplOk = true;
        } catch (UnsatisfiedLinkError e) {
            mLoadFesplOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libfespl.so, and put it in your libs dir!");

        }
    }

    private long mFesplId;

    public static boolean isFespxSoValid() {
        return mLoadFesplOk;
    }

    public static native long dds_fespl_new(String cfg);

    public static native int dds_fespl_getWakeupConfig(String cfg, Sspe.config_callback callback);

    public static native int dds_fespl_start(long id, String param);

    public static native int dds_fespl_set(long id, String setParam);

    public static native int dds_fespl_setwakeupcb(long id, Sspe.wakeup_callback wakeupCallback);

    public static native int dds_fespl_setdoacb(long id, Sspe.doa_callback doaCallback);

    public static native int dds_fespl_setbeamformingcb(long id, Sspe.beamforming_callback beamformingCallback);

    public static native int dds_fespl_setvprintcutcb(long id, Sspe.vprintcut_callback callback);

    public static native int dds_fespl_setinputcb(long id, Sspe.input_callback callback);

    public static native int dds_fespl_setoutputcb(long id, Sspe.output_callback callback);

    public static native int dds_fespl_feed(long id, byte[] data, int size);

    public static native int dds_fespl_stop(long id);

    public static native int dds_fespl_delete(long id);

    public static native int dds_fespl_get(long id, String param);

    public static native int dds_fespl_setechocb(long id, Sspe.echo_callback callback);

    public static native int dds_fespl_setsevcdoacb(long id, Sspe.sevc_doa_callback callback);

    public static native int dds_fespl_setsevcnoisecb(long id, Sspe.sevc_noise_callback callback);

    public static native int dds_fespl_setmultibfcb(long id, Sspe.multibf_callback callback);
    @Override
    public long initFespx(String cfg) {
        Log.d(TAG, "init Fespl " + cfg);
        mFesplId = dds_fespl_new(cfg);
        Log.d(TAG, "init Fespl return " + mFesplId);
        return mFesplId;
    }

    @Override
    public int startFespx() {
        int ret = dds_fespl_start(mFesplId, "");
        Log.d(TAG, "start Fespl return " + ret);
        return ret;
    }

    @Override
    public int setFespx(String setParam) {
        return dds_fespl_set(mFesplId, setParam);
    }

    @Override
    public int setFespxWakeupcb(Sspe.wakeup_callback callback) {
        int ret = dds_fespl_setwakeupcb(mFesplId, callback);
        Log.d(TAG, "dds_fespl_setwakeupcb ret : " + ret);
        return ret;
    }

    @Override
    public int getWakeupConfig(String initConfig, Sspe.config_callback callback) {
        int ret = dds_fespl_getWakeupConfig(initConfig, callback);
        Log.d(TAG, "dds_fespl_getWakeupConfig ret:" + ret);
        return ret;
    }

    @Override
    public int setFespxDoacb(Sspe.doa_callback callback) {
        int ret = dds_fespl_setdoacb(mFesplId, callback);
        Log.d(TAG, "dds_fespl_setdoacb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxBeamformingcb(Sspe.beamforming_callback callback) {
        int ret = dds_fespl_setbeamformingcb(mFesplId, callback);
        Log.d(TAG, "dds_fespl_setbeamformingcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxVprintCutcb(Sspe.vprintcut_callback callback) {
        int ret = dds_fespl_setvprintcutcb(mFesplId, callback);
        Log.d(TAG, "dds_fespl_setvprintcutcb ret : " + ret);
        return ret;
    }

    @Override
    public int setMultBfcb(Sspe.multibf_callback callback) {
        int ret = dds_fespl_setmultibfcb(mFesplId, callback);
        Log.d(TAG, "dds_fespl_setmultibfcb ret: " + ret);
        return ret;
    }

    @Override
    public int setFespxInputcb(Sspe.input_callback callback) {
        int ret = dds_fespl_setinputcb(mFesplId, callback);
        Log.d(TAG, "dds_fespl_setinputcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxOutputcb(Sspe.output_callback callback) {
        int ret = dds_fespl_setoutputcb(mFesplId, callback);
        Log.d(TAG, "dds_fespl_setoutputcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxEchocb(Sspe.echo_callback callback) {
        int ret = dds_fespl_setechocb(mFesplId, callback);
        Log.d(TAG, "dds_fespl_setechocb ret : " + ret);
        return ret;
    }

    @Override
    public int feedFespx(byte[] data, int size) {
        return dds_fespl_feed(mFesplId, data, size);
    }

    @Override
    public int stopFespx() {
        int ret = dds_fespl_stop(mFesplId);
        Log.d(TAG, "dds_fespl_stop ret : " + ret);
        return ret;
    }

    @Override
    public int destroyFespx() {
        int ret = dds_fespl_delete(mFesplId);
        Log.d(TAG, "destroy Fespl return " + ret);
        return ret;
    }

    @Override
    public int getFespx(String getParam) {
        int value = dds_fespl_get(mFesplId, getParam);
        Log.d(TAG, getParam + " is : " + value);
        return value;
    }

    @Override
    public int setFespxSevcNoise(Sspe.sevc_noise_callback callback) {
        int ret = dds_fespl_setsevcnoisecb(mFesplId, callback);
        Log.d(TAG, "dds_fespl_setsevcnoisecb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxSevcDoa(Sspe.sevc_doa_callback callback) {
        int ret = dds_fespl_setsevcdoacb(mFesplId, callback);
        Log.d(TAG, "dds_fespl_setsevcdoacb ret : " + ret);
        return ret;
    }
}
