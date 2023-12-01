package com.aispeech.kernel;

import com.aispeech.common.Log;

import org.json.JSONObject;

public class NearFespx extends Fespx {

    public static final String NET_CONFIG = "netCfg";
    public static final String MDS_CONFIG = "mdsCfg";
    private static final String TAG = "NearFespx";
    private static boolean mLoadSoOk;

    static {
        try {
            Log.d(TAG, "before load library: nearsspe");
            System.loadLibrary("nearsspe");
            Log.d(TAG, "after load library: nearsspe");
            mLoadSoOk = true;
        } catch (UnsatisfiedLinkError e) {
            mLoadSoOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libnearsspe.so, and put it in your libs dir!");
        }
    }

    private long engineId;

    public static boolean isFespxSoValid() {
        return mLoadSoOk;
    }

    private static native long fespx_new(String wkpCfg, String netCfg, String mdsCfg);

    private static native int fespx_start(long handle, String cfg);

    private static native int fespx_get(long handle, String key);

    private static native int fespx_set(long handle, String param);

    private static native int fespx_feed(long handle, byte[] data);

    private static native int fespx_stop(long handle);

    private static native int fespx_delete(long handle);

    private static native int fespx_setwakeupcb(long handle, Sspe.wakeup_callback callback);

    private static native int fespx_setdoacb(long handle, Sspe.doa_callback callback);

    private static native int fespx_setbeamformingcb(long handle, Sspe.beamforming_callback callback);

    private static native int fespx_setvprintcutcb(long handle, Sspe.vprintcut_callback callback);

    private static native int fespx_setinformationcb(long handle, Sspe.information_callback callback);

    public static native int fespx_setinputcb(long id, Sspe.input_callback callback);

    public static native int fespx_setoutputcb(long id, Sspe.output_callback callback);

    public static native int fespx_setechocb(long id, Sspe.echo_callback callback);

    private static native int fespx_setsevcdoacb(long id, Sspe.sevc_doa_callback callback);

    private static native int fespx_setsevcnoisecb(long id, Sspe.sevc_noise_callback callback);

    public static native int fespx_setmultibfcb(long id, Sspe.multibf_callback callback);

    public static native int fespx_setechovoipcb(long id, Sspe.echo_voip_callback callback);

    public static native int fespx_setagccb(long id, Sspe.agc_callback callback);

    @Override
    public long initFespx(String config) {
        Log.d(TAG, "initFespx " + config);
        try {
            JSONObject jsonObject = new JSONObject(config);
            engineId = fespx_new(config,
                    jsonObject.getJSONObject(NET_CONFIG).toString(),
                    jsonObject.getJSONObject(MDS_CONFIG).toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d(TAG, "initFespx return " + engineId);
        return engineId;
    }

    @Override
    public int startFespx() {
        Log.v(TAG, "before startFespx  "  );
        int ret = fespx_start(engineId, "");
        Log.d(TAG, "startFespx return " + ret);
        return ret;
    }

    @Override
    public int setFespx(String setParam) {
        Log.v(TAG, "before startFespx param: "+setParam  );
        return fespx_set(engineId, setParam);
    }

    @Override
    public int setFespxWakeupcb(Sspe.wakeup_callback callback) {
        Log.v(TAG, "before setFespxWakeupcb  "  );
        int ret = fespx_setwakeupcb(engineId, callback);
        Log.d(TAG, "setFespxWakeupcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxDoacb(Sspe.doa_callback callback) {
        Log.v(TAG, "before setFespxDoacb ");
        int ret = fespx_setdoacb(engineId, callback);
        Log.d(TAG, "setFespxDoacb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxBeamformingcb(Sspe.beamforming_callback callback) {
        Log.v(TAG, "before setFespxBeamformingcb ");
        int ret = fespx_setbeamformingcb(engineId, callback);
        Log.d(TAG, "setFespxBeamformingcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxVprintCutcb(Sspe.vprintcut_callback callback) {
        Log.v(TAG, "before setFespxVprintCutcb ");
        int ret = fespx_setvprintcutcb(engineId, callback);
        Log.d(TAG, "setFespxVprintCutcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxInputcb(Sspe.input_callback callback) {
        Log.v(TAG, "before setFespxInputcb ");
        int ret = fespx_setinputcb(engineId, callback);
        Log.d(TAG, "fespx_setinputcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxOutputcb(Sspe.output_callback callback) {
        Log.v(TAG, "before setFespxOutputcb ");
        int ret = fespx_setoutputcb(engineId, callback);
        Log.d(TAG, "fespx_setoutputcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxEchocb(Sspe.echo_callback callback) {
        Log.v(TAG, "before setFespxEchocb ");
        int ret = fespx_setechocb(engineId, callback);
        Log.d(TAG, "fespx_setechocb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxSevcNoise(Sspe.sevc_noise_callback callback) {
        Log.v(TAG, "before setFespxSevcNoise ");
        int ret = fespx_setsevcnoisecb(engineId, callback);
        Log.d(TAG, "fespx_setsevcnoisecb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxSevcDoa(Sspe.sevc_doa_callback callback) {
        Log.v(TAG, "before fespx_setsevcdoacb ");
        int ret = fespx_setsevcdoacb(engineId, callback);
        Log.d(TAG, "fespx_setsevcdoacb ret : " + ret);
        return ret;
    }

    @Override
    public int setMultBfcb(Sspe.multibf_callback callback) {
        Log.v(TAG, "before setMultBfcb ");
        int ret = fespx_setmultibfcb(engineId, callback);
        Log.d(TAG, "fespx_setmultibfcb ret : " + ret);
        return ret;
    }

    @Override
    public int getFespx(String param) {
        Log.v(TAG, "before getFespx ");
        int value = fespx_get(engineId, param);
        Log.d(TAG, "getFespx " + param + ":" + value);
        return value;
    }

    @Override
    public int feedFespx(byte[] data, int size) {
        //Log.v(TAG, "before feedFespx ");
        return fespx_feed(engineId, data);
    }

    @Override
    public int stopFespx() {
        Log.v(TAG, "before stopFespx ");
        int ret = fespx_stop(engineId);
        Log.d(TAG, "stopFespx ret : " + ret);
        return ret;
    }

    @Override
    public int destroyFespx() {
        Log.v(TAG, "before fespx_delete ");
        int ret = fespx_delete(engineId);
        engineId = 0;
        Log.d(TAG, "destroyFespx return " + ret);
        return ret;
    }

    public int setInformationCallback(Sspe.information_callback callback) {
        Log.v(TAG, "before setInformationCallback ");
        return fespx_setinformationcb(engineId, callback);
    }

}
