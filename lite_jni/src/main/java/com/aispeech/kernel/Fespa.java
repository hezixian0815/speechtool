package com.aispeech.kernel;

import com.aispeech.common.Log;

/**
 * Created by wuwei on 18-6-1.
 */

public class Fespa extends Fespx {
    public static final String LIBRARY_NAME = "fespa";
    private static final String TAG = "Fespa";
    private static boolean mLoadfespaOk = false;

    static {
        try {
            Log.d(TAG, "before load fespa library");
        //    System.loadLibrary(LIBRARY_NAME);
            Log.d(TAG, "after load fespa library");
            mLoadfespaOk = true;
        } catch (UnsatisfiedLinkError e) {
            mLoadfespaOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libfespa.so, and put it in your libs dir!");

        }
    }

    private long mfespaId;

    public static boolean isFespxSoValid() {
        return mLoadfespaOk;
    }

    public static native long dds_fespa_new(String cfg);

    public static native int dds_fespa_getWakeupConfig(String cfg, Sspe.config_callback callback);

    public static native int dds_fespa_start(long id, String param);

    public static native int dds_fespa_set(long id, String setParam);

    public static native int dds_fespa_setwakeupcb(long id, Sspe.wakeup_callback wakeupCallback);

    public static native int dds_fespa_setdoacb(long id, Sspe.doa_callback doaCallback);

    public static native int dds_fespa_setbeamformingcb(long id, Sspe.beamforming_callback beamformingCallback);

    public static native int dds_fespa_setvprintcutcb(long id, Sspe.vprintcut_callback vprintcutCallback);

    public static native int dds_fespa_setinputcb(long id, Sspe.input_callback callback);

    public static native int dds_fespa_setoutputcb(long id, Sspe.output_callback callback);

    public static native int dds_fespa_feed(long id, byte[] data, int size);

    public static native int dds_fespa_stop(long id);

    public static native int dds_fespa_delete(long id);

    public static native int dds_fespa_get(long id, String param);

    public static native int dds_fespa_setechocb(long id, Sspe.echo_callback callback);

    public static native int dds_fespa_setsevcdoacb(long id, Sspe.sevc_doa_callback callback);

    public static native int dds_fespa_setsevcnoisecb(long id, Sspe.sevc_noise_callback callback);

    public static native int dds_fespa_setmultibfcb(long id, Sspe.multibf_callback callback);
    @Override
    public long initFespx(String cfg) {
        Log.d(TAG, "init Fespa " + cfg);
        mfespaId = dds_fespa_new(cfg);
        Log.d(TAG, "init Fespa return " + mfespaId);
        return mfespaId;
    }

    @Override
    public int startFespx() {
        int ret = dds_fespa_start(mfespaId, "");
        Log.d(TAG, "start Fespa return " + ret);
        return ret;
    }

    @Override
    public int setFespx(String setParam) {
        return dds_fespa_set(mfespaId, setParam);
    }

    public int setFespxWakeupcb(Sspe.wakeup_callback callback) {
        int ret = dds_fespa_setwakeupcb(mfespaId, callback);
        Log.d(TAG, "dds_fespa_setwakeupcb ret : " + ret);
        return ret;
    }

    public int setFespxDoacb(Sspe.doa_callback callback) {
        int ret = dds_fespa_setdoacb(mfespaId, callback);
        Log.d(TAG, "dds_fespa_setdoacb ret : " + ret);
        return ret;
    }

    public int setFespxBeamformingcb(Sspe.beamforming_callback callback) {
        int ret = dds_fespa_setbeamformingcb(mfespaId, callback);
        Log.d(TAG, "dds_fespa_setbeamformingcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxVprintCutcb(Sspe.vprintcut_callback callback) {
        int ret = dds_fespa_setvprintcutcb(mfespaId, callback);
        Log.d(TAG, "dds_fespa_setvprintcutcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxInputcb(Sspe.input_callback callback) {
        int ret = dds_fespa_setinputcb(mfespaId, callback);
        Log.d(TAG, "dds_fespa_setinputcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxOutputcb(Sspe.output_callback callback) {
        int ret = dds_fespa_setoutputcb(mfespaId, callback);
        Log.d(TAG, "dds_fespa_setoutputcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxEchocb(Sspe.echo_callback callback) {
        int ret = dds_fespa_setechocb(mfespaId, callback);
        Log.d(TAG, "dds_fespa_setechocb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxSevcNoise(Sspe.sevc_noise_callback callback) {
        int ret = dds_fespa_setsevcnoisecb(mfespaId, callback);
        Log.d(TAG, "dds_fespa_setsevcnoisecb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxSevcDoa(Sspe.sevc_doa_callback callback) {
        int ret = dds_fespa_setsevcdoacb(mfespaId, callback);
        Log.d(TAG, "dds_fespa_setsevcdoacb ret : " + ret);
        return ret;
    }

    @Override
    public int setMultBfcb(Sspe.multibf_callback callback) {
        int ret = dds_fespa_setmultibfcb(mfespaId, callback);
        Log.d(TAG, "dds_fespa_setmultibfcb ret : " + ret);
        return ret;
    }

    @Override
    public int feedFespx(byte[] data, int size) {
        return dds_fespa_feed(mfespaId, data, size);
    }

    @Override
    public int stopFespx() {
        int ret = dds_fespa_stop(mfespaId);
        Log.d(TAG, "dds_fespa_stop ret : " + ret);
        return ret;
    }

    @Override
    public int destroyFespx() {
        int ret = dds_fespa_delete(mfespaId);
        Log.d(TAG, "destroy Fespa return " + ret);
        return ret;
    }

    @Override
    public int getWakeupConfig(String initConfig, Sspe.config_callback callback) {
        int ret = dds_fespa_getWakeupConfig(initConfig, callback);
        Log.d(TAG, "dds_fespa_getWakeupConfig ret:" + ret);
        return ret;
    }

    @Override
    public int getFespx(String getParam) {
        int value = dds_fespa_get(mfespaId, getParam);
        Log.d(TAG, getParam + " is : " + value);
        return value;
    }
}
