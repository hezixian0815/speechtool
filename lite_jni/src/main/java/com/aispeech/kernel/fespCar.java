package com.aispeech.kernel;

import com.aispeech.common.Log;

/**
 * 车载双麦（fespCar），包含回声消除（echo），声源定位（doa），波束成形
 * （beamforming）和语音唤醒（wakeup）等功能模块。
 */
public class fespCar extends Fespx {
    public static final String LIBRARY_NAME = "fespcar";
    static final String TAG = "fespCar";
    private static final String CONFIG_CALLBACK_STR = "{\"use_built_in_wakeupwords\":false, \"built_in_wakeupwords\":{}}";
    private static boolean loadedSo;

    static {
        try {
            Log.d(TAG, "before load fespcar library");
            System.loadLibrary(LIBRARY_NAME);
            Log.d(TAG, "after load fespcar library");
            loadedSo = true;
        } catch (UnsatisfiedLinkError e) {
            loadedSo = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libfespcar.so, and put it in your libs dir!");
        }
    }

    private long engineId;

    public static boolean isFespxSoValid() {
        return loadedSo;
    }

    public static native long dds_fespCar_new(String cfg);

    public static native int dds_fespCar_delete(long engineId);

    public static native int dds_fespCar_start(long engine, String param);

    public static native int dds_fespCar_feed(long engine, byte[] data, int size);

    /**
     * <pre>
     * {
     *  "env": "words=ni hao xiao le;thresh=2.0;major=1;",
     *  "maxVolumeState": 1,
     *  "driveMode": 1
     * }
     * </pre>
     * maxVolumeState 用于设置大音量状态，启用大音量检测功能时，在每次 feed 之前调用，0 表示非大音量，1 表示大音量；<br>
     * driveMode 用于设置驾驶模式，0 为全驾模式，1 为主驾模式。
     *
     * @param engine
     * @param setParam
     * @return
     */
    public static native int dds_fespCar_set(long engine, String setParam);

    public static native int dds_fespCar_getDriveMode(long engine);

    public static native int dds_fespCar_stop(long engine);

    public static native int dds_fespCar_setwakeupcb(long engine, wakeup_callback callback);

    public static native int dds_fespCar_setdoacb(long engine, doa_callback callback);

    public static native int dds_fespCar_setbeamformingcb(long engine, beamforming_callback callback);

    public static native int dds_fespCar_get(long engine, String param);

    public static native int dds_fespCar_setvprintcutcb(long id, vprintcut_callback callback);

    @Override
    public long initFespx(String cfg) {
        engineId = dds_fespCar_new(cfg);
        Log.d(TAG, "dds_fespCar_new():" + engineId);
        return engineId;
    }

    @Override
    public int getWakeupConfig(String initConfig, Sspe.config_callback callback) {
        if (callback != null) {
            byte[] bytes = CONFIG_CALLBACK_STR.getBytes();
            callback.run(0, bytes, bytes.length);
        }
        return 0;
    }

    @Override
    public int destroyFespx() {
        Log.d(TAG, "dds_fespCar_delete():" + engineId);
        int ret = dds_fespCar_delete(engineId);
        Log.d(TAG, "dds_fespCar_delete() finished:" + engineId);
        engineId = 0;
        return ret;
    }

    @Override
    public int getFespx(String param) {
        int ret = dds_fespCar_get(engineId, param);
        Log.d(TAG, "getFespx " + param + ":" + ret);
        return ret;
    }

    @Override
    public int stopFespx() {
        Log.d(TAG, "dds_fespCar_stop():" + engineId);
        return dds_fespCar_stop(engineId);
    }

    @Override
    public int startFespx() {
        int ret;
        Log.d(TAG, "dds_fespCar_start():" + engineId);
        ret = dds_fespCar_start(engineId, "");
        if (ret < 0) {
            Log.e(TAG, "dds_fespCar_start() failed! Error code: " + ret);
            return -1;
        }

        return ret;
    }

    @Override
    public int feedFespx(byte[] buffer, int size) {
        return dds_fespCar_feed(engineId, buffer, size);
    }

    @Override
    public int setFespx(String setParam) {
        int ret;
        Log.d(TAG, "dds_fespCar_set():" + engineId);
        ret = dds_fespCar_set(engineId, setParam);
        if (ret < 0) {
            Log.e(TAG, "dds_fespCar_set() failed! Error code: " + ret);
            return -1;
        }
        return ret;
    }

    public int getDriveMode() {
        int ret;
        ret = dds_fespCar_get(engineId, "driveMode");
        Log.d(TAG, "dds_fespCar_get():" + ret);
        return ret;
    }

    public int setFespxWakeupcb(Sspe.wakeup_callback callback) {
        WakeupCallbackImpl wakeupCallback = new WakeupCallbackImpl();
        wakeupCallback.setListener(callback);
        int ret = dds_fespCar_setwakeupcb(engineId, wakeupCallback);
        Log.d(TAG, "dds_fespCar_setwakeupcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxVprintCutcb(Sspe.vprintcut_callback callback) {
        VprintCallbackImpl vprintCallback = new VprintCallbackImpl();
        vprintCallback.setListener(callback);
        int ret = dds_fespCar_setvprintcutcb(engineId, vprintCallback);
        Log.d(TAG, "dds_fespCar_setvprintcutcb ret : " + ret);
        return ret;
    }

    public int setFespxDoacb(Sspe.doa_callback callback) {
        DoaCallbackImpl doaCallback = new DoaCallbackImpl();
        doaCallback.setListener(callback);
        int ret = dds_fespCar_setdoacb(engineId, doaCallback);
        Log.d(TAG, "dds_fespCar_setdoacb ret : " + ret);
        return ret;
    }

    public int setFespxBeamformingcb(Sspe.beamforming_callback callback) {
        BeamformingCallbackImpl beamformingCallback = new BeamformingCallbackImpl();
        beamformingCallback.setListener(callback);
        int ret = dds_fespCar_setbeamformingcb(engineId, beamformingCallback);
        Log.d(TAG, "dds_fespCar_setbeamformingcb ret : " + ret);
        return ret;
    }

    @Override
    public int setFespxInputcb(Sspe.input_callback callback) {
        return 0;
    }

    @Override
    public int setFespxOutputcb(Sspe.output_callback callback) {
        return 0;
    }

    @Override
    public int setFespxEchocb(Sspe.echo_callback callback) {
        return 0;
    }

    @Override
    public int setFespxSevcNoise(Sspe.sevc_noise_callback callback) {
        return 0;
    }

    @Override
    public int setFespxSevcDoa(Sspe.sevc_doa_callback callback) {
        return 0;
    }

    private static class BeamformingCallbackImpl extends beamforming_callback {
        Sspe.beamforming_callback mCallback;
        public void setListener(Sspe.beamforming_callback callback) {
            mCallback = callback;
        }

        @Override
        public int run(int type, byte[] data, int size) {
            if (mCallback != null) {
                mCallback.run(type, data, size);
            }
            return 0;
        }
    }

    private static class DoaCallbackImpl implements doa_callback {
        Sspe.doa_callback mCallback;
        public void setListener(Sspe.doa_callback callback) {
            mCallback = callback;
        }

        @Override
        public int run(int type, byte[] data, int size) {
            if (mCallback != null) {
                mCallback.run(type, data, size);
            }
            return 0;
        }
    }

    private static class WakeupCallbackImpl implements wakeup_callback {
        Sspe.wakeup_callback mCallback;
        public void setListener(Sspe.wakeup_callback callback) {
            mCallback = callback;
        }

        @Override
        public int run(int type, byte[] data, int size) {
            if (mCallback != null) {
                mCallback.run(type, data, size);
            }
            return 0;
        }
    }

    private static class VprintCallbackImpl extends vprintcut_callback {
        Sspe.vprintcut_callback mCallback;
        public void setListener(Sspe.vprintcut_callback callback) {
            mCallback = callback;
        }

        @Override
        public int run(int type, byte[] data, int size) {
            if (mCallback != null) {
                mCallback.run(type, data, size);
            }
            return 0;
        }
    }

    @Override
    public int setMultBfcb(Sspe.multibf_callback callback) {
        return 0;
    }
}
