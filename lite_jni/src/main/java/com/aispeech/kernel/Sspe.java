
package com.aispeech.kernel;

import com.aispeech.common.JNIFlag;
import com.aispeech.common.Log;

public class Sspe extends BaseLiteSo implements LiteSoFunction {

    private static final String TAG =  Log.TagPrefix.KERNEL + "Sspe";
    private static boolean loadSoSuc;

    private final Object mSetLock = new Object();

    static {
        try {
            boolean doubleVad = JNIFlag.useDoubleVad;
            Log.d(TAG, "before load sspe library,doubleVad:" + doubleVad);
            System.loadLibrary("sspe");
            Log.d(TAG, "after load sspe library");
            loadSoSuc = true;
        } catch (UnsatisfiedLinkError e) {
            loadSoSuc = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libsspe.so, and put it in your libs dir!");
        }
    }

    public static boolean isSoValid() {
        return loadSoSuc;
    }

    public static native long dds_sspe_new(String cfg);

    public static native int dds_sspe_getWakeupConfig(String cfg, config_callback callback);

    public static native int dds_sspe_setwakeupcb(long id, wakeup_callback callback);

    public static native int dds_sspe_setbeamformingcb(long id, beamforming_callback callback);

    public static native int dds_sspe_setdoacb(long id, doa_callback callback);

    public static native int dds_sspe_start(long id, String param);

    public static native int dds_sspe_set(long id, String setParam);

    public static native int dds_sspe_get(long id, String param);

    public static native int dds_sspe_getChanNum(long id);

    public static native int dds_sspe_feed(long id, byte[] data, int size);

    public static native int dds_sspe_stop(long id);

    public static native int dds_sspe_delete(long id);

    public static native int dds_sspe_setvprintcutcb(long id, vprintcut_callback callback);

    public static native int dds_sspe_setinputcb(long id, input_callback callback);

    public static native int dds_sspe_setoutputcb(long id, output_callback callback);

    public static native int dds_sspe_setechocb(long id, echo_callback callback);

    public static native int dds_sspe_setsevcdoacb(long id, sevc_doa_callback callback);

    public static native int dds_sspe_setsevcnoisecb(long id, sevc_noise_callback callback);

    public static native int dds_sspe_setmultibfcb(long id, multibf_callback callback);

    public static native int dds_sspe_setechovoipcb(long id, echo_voip_callback callback);

    public static native int dds_sspe_setagccb(long id, agc_callback callback);

    public static native int dds_sspe_setvad_audio_cb(long id, vad_audio_callback callback);

    public static native String dds_sspe_get_version_info(long engine);

    public static native int dds_sspe_setasr_audio_cb(long id, asr_audio_callback callback);

    public static native int dds_sspe_setdoa_audio_cb(long id, doa_audio_callback callback);

    public static native int dds_sspe_setdoacommoncb(long id, doa_common_callback callback);

    @Override
    public long init(String cfg, Object... callbacks) {
        Log.i(TAG, "init " + cfg);
        mEngineId = dds_sspe_new(cfg);
        Log.d(TAG, "init return " + mEngineId + " cfg:" + cfg);
        init(cfg);
        return mEngineId;
    }

    public int getWakeupConfig(String initConfig, config_callback callback) {
        int ret = dds_sspe_getWakeupConfig(initConfig, callback);
        Log.i(TAG, "dds_sspe_getWakeupConfig ret:" + ret);
        return ret;
    }

    public int start(String param) {
        int ret = dds_sspe_start(mEngineId, "");
//        int chanNum = dds_sspe_getChanNum(mEngineId);
        Log.i(TAG, "start ret = " + ret);
        return ret;
    }

    public int set(String setParam) {
        int ret;
        synchronized (mSetLock) {
            ret = dds_sspe_set(mEngineId, setParam);
            if (ret < 0) {
                Log.e(TAG, "set error !");
                return -1;
            }
        }
        return ret;
    }

    public int setCallback(Object... callbacks) {
        if (callbacks == null || callbacks.length == 0) {
            return 0;
        }
        Log.i(TAG, "setCallback callbacks.length:" + callbacks.length);

        for (Object callback : callbacks) {
            int ret = 0;
            if (callback instanceof beamforming_callback) {
                if (isImplBfCk()) {
                    ret = dds_sspe_setbeamformingcb(mEngineId, (beamforming_callback) callback);
                    Log.d(TAG, "dds_sspe_setbeamformingcb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setbeamformingcb  not register");
                }
            } else if (callback instanceof wakeup_callback) {
                if (isImplWakeupCk()) {
                    ret = dds_sspe_setwakeupcb(mEngineId, (wakeup_callback) callback);
                    Log.d(TAG, "dds_sspe_setwakeupcb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setwakeupcb  not register");
                }
            } else if (callback instanceof doa_callback) {
                if (isImplDoaCk()) {
                    ret = dds_sspe_setdoacb(mEngineId, (doa_callback) callback);
                    Log.d(TAG, "dds_sspe_setdoacb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setdoacb  not register");
                }
            } else if (callback instanceof vprintcut_callback) {
                if (isImplVprintCutCk()) {
                    ret = dds_sspe_setvprintcutcb(mEngineId, (vprintcut_callback) callback);
                    Log.d(TAG, "dds_sspe_setvprintcutcb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setvprintcutcb  not register");
                }
            } else if (callback instanceof input_callback) {
                if (isImplInputCk()) {
                    ret = dds_sspe_setinputcb(mEngineId, (input_callback) callback);
                    Log.d(TAG, "dds_sspe_setinputcb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setinputcb  not register");
                }
            } else if (callback instanceof output_callback) {
                if (isImplOutputCk()) {
                    ret = dds_sspe_setoutputcb(mEngineId, (output_callback) callback);
                    Log.d(TAG, "dds_sspe_setoutputcb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setoutputcb  not register");
                }
            } else if (callback instanceof echo_callback) {
                if (isImplEchoCk()) {
                    ret = dds_sspe_setechocb(mEngineId, (echo_callback) callback);
                    Log.d(TAG, "dds_sspe_setechocb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setoutputcb  not register");
                }
            } else if (callback instanceof sevc_noise_callback) {
                if (isImplSevcNoiseCk()) {
                    ret = dds_sspe_setsevcnoisecb(mEngineId, (sevc_noise_callback) callback);
                    Log.d(TAG, "dds_sspe_setsevcnoisecb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setsevcnoisecb  not register");
                }
            } else if (callback instanceof sevc_doa_callback) {
                if (isImplSevcDoaCk()) {
                    ret = dds_sspe_setsevcdoacb(mEngineId, (sevc_doa_callback) callback);
                    Log.d(TAG, "dds_sspe_setsevcdoacb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setsevcdoacb  not register");
                }
            } else if (callback instanceof multibf_callback) {
                if (isImplMultiBfCk()) {
                    ret = dds_sspe_setmultibfcb(mEngineId, (multibf_callback) callback);
                    Log.d(TAG, "dds_sspe_setmultibfcb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setmultibfcb  not register");
                }
            } else if (callback instanceof echo_voip_callback) {
                if (isImplEchoVoipCk()) {
                    ret = dds_sspe_setechovoipcb(mEngineId, (echo_voip_callback) callback);
                    Log.d(TAG, "dds_sspe_setechovoipcb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setechovoipcb  not register");
                }
            } else if (callback instanceof agc_callback) {
                if (isImplAgcCk()) {
                    ret = dds_sspe_setagccb(mEngineId, (agc_callback) callback);
                    Log.d(TAG, "dds_sspe_setagccb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setagccb  not register");
                }
            } else if (callback instanceof vad_audio_callback) {
                if (isImplVadCk()) {
                    ret = dds_sspe_setvad_audio_cb(mEngineId, (vad_audio_callback) callback);
                    Log.d(TAG, "dds_sspe_setvadcb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setvadcb  not register");
                }
            } else if (callback instanceof asr_audio_callback) {
                if (isImplAsrCk()) {
                    ret = dds_sspe_setasr_audio_cb(mEngineId, (asr_audio_callback) callback);
                    Log.d(TAG, "dds_sspe_setasrcb ret : " + ret);
                } else {
                    Log.d(TAG, "dds_sspe_setasrcb  not register");
                }
            } else if (callback instanceof doa_common_callback) {
                ret = dds_sspe_setdoacommoncb(mEngineId, (doa_common_callback) callback);
            } else {
                Log.e(TAG, "setCallback err  callback:" + callback);
                ret = -1;
            }
            // 0 成功  -9892 表示内核不支持该功能，由资源确定是否支持
            if (ret != 0 && ret != -9892 && ret != -9893)
                return ret;
        }

        return 0;
    }

    public int feed(byte[] data, int size) {
        long timeStart = System.currentTimeMillis();
//        Log.dfAudio(TAG, "AIEngine.feed():" + mEngineId);
//        Log.v(TAG, "feed:data " + size);
        int ret = dds_sspe_feed(mEngineId, data, size);
        Log.dfAudio(TAG, "feed:data " + size + " ret " + ret + " cost " + (System.currentTimeMillis() - timeStart));
        return ret;
    }

    @Override
    public int cancel() {
        return stop();
    }

    public int stop() {
        int ret = dds_sspe_stop(mEngineId);
        Log.d(TAG, "stop ret : " + ret);
        return ret;
    }

    public int destroy() {
        destroyEngine();
        int ret = dds_sspe_delete(mEngineId);
        Log.d(TAG, "destroy return " + ret);
        mEngineId = 0;
        return ret;
    }

    @Override
    protected String JNIVersionInfo() {
        String versionInfo = dds_sspe_get_version_info(mEngineId);
        Log.i(TAG, "JNIVersionInfo: " + versionInfo);
        return versionInfo;
    }

    public int get(String getParam) {
        int value = dds_sspe_get(mEngineId, getParam);
        Log.d(TAG, getParam + " is : " + value);
        return value;
    }

    /**
     * 是否注册接口的标记
     */
    public boolean implWakeupCk = true;
    public boolean implMultiBfCk = true;
    public boolean implOutputCk = true;
    public boolean implInputCk = true;
    public boolean implBfCk = true;
    public boolean implDoaCk = true;
    public boolean implVprintCutCk = true;
    public boolean implEchoCk = true;
    public boolean implEchoVoipCk = true;
    public boolean implSevcDoaCk = true;
    public boolean implSevcNoiseCk = true;
    public boolean implVoipCk = true;
    public boolean implAgcCk = true;
    public boolean implVadCk = true;
    public boolean implAsrCk = true;

    public boolean isImplVadCk() {
        return implVadCk;
    }

    public boolean isImplAsrCk() {
        return implAsrCk;
    }

    public void setImplAsrCk(boolean implAsrCk) {
        this.implAsrCk = implAsrCk;
    }

    public void setImplVadCk(boolean implVadCk) {
        this.implVadCk = implVadCk;
    }

    public boolean isImplMultiBfCk() {
        return implMultiBfCk;
    }

    public void setImplMultiBfCk(boolean implMultiBfCk) {
        this.implMultiBfCk = implMultiBfCk;
    }

    public boolean isImplWakeupCk() {
        return implWakeupCk;
    }

    public void setImplWakeupCk(boolean implWakeupCk) {
        this.implWakeupCk = implWakeupCk;
    }

    public boolean isImplOutputCk() {
        return implOutputCk;
    }

    public void setImplOutputCk(boolean implOutputCk) {
        this.implOutputCk = implOutputCk;
    }

    public boolean isImplInputCk() {
        return implInputCk;
    }

    public void setImplInputCk(boolean implInputCk) {
        this.implInputCk = implInputCk;
    }

    public boolean isImplBfCk() {
        return implBfCk;
    }

    public void setImplBfCk(boolean implBfCk) {
        this.implBfCk = implBfCk;
    }

    public boolean isImplDoaCk() {
        return implDoaCk;
    }

    public void setImplDoaCk(boolean implDoaCk) {
        this.implDoaCk = implDoaCk;
    }

    public boolean isImplVprintCutCk() {
        return implVprintCutCk;
    }

    public void setImplVprintCutCk(boolean implVprintCutCk) {
        this.implVprintCutCk = implVprintCutCk;
    }

    public boolean isImplEchoCk() {
        return implEchoCk;
    }

    public void setImplEchoCk(boolean implEchoCk) {
        this.implEchoCk = implEchoCk;
    }

    public boolean isImplEchoVoipCk() {
        return implEchoVoipCk;
    }

    public void setImplEchoVoipCk(boolean implEchoVoipCk) {
        this.implEchoVoipCk = implEchoVoipCk;
    }

    public boolean isImplSevcDoaCk() {
        return implSevcDoaCk;
    }

    public void setImplSevcDoaCk(boolean implSevcDoaCk) {
        this.implSevcDoaCk = implSevcDoaCk;
    }

    public boolean isImplSevcNoiseCk() {
        return implSevcNoiseCk;
    }

    public void setImplSevcNoiseCk(boolean implSevcNoiseCk) {
        this.implSevcNoiseCk = implSevcNoiseCk;
    }

    public boolean isImplAgcCk() {
        return implAgcCk;
    }

    public void setImplAgcCk(boolean implAgcCk) {
        this.implAgcCk = implAgcCk;
    }

    public static class output_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        // 内部多线程处理，回调函数与 `feed` 主线程不属于同一个线程。
        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    public static class vad_audio_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        // 内部多线程处理，回调函数与 `feed` 主线程不属于同一个线程。
        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    public static class asr_audio_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        // 内部多线程处理，回调函数与 `feed` 主线程不属于同一个线程。
        public int run(int index, byte[] data, int size) {
            return 0;
        }
    }

    public static class doa_audio_callback {

        // 内部多线程处理，回调函数与 `feed` 主线程不属于同一个线程。
        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    public static class input_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        // 内部多线程处理，回调函数与 `feed` 主线程不属于同一个线程。
        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    public static class echo_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        // 内部多线程处理，回调函数与 `feed` 主线程不属于同一个线程。
        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    /**
     * 唤醒回调
     */
    public interface wakeup_callback {
        int run(int type, byte[] data, int size);
    }

    public interface information_callback {
        int run(int type, byte[] data, int size);
    }

    /**
     * doa回调
     */
    public interface doa_callback {
        int run(int type, byte[] data, int size);
    }

    public interface config_callback {
        int run(int type, byte[] data, int size);
    }

    public interface doa_common_callback {
        int run(int type, byte[] data, int size);
    }

    /**
     * bf音频回调接口
     */
    public static class beamforming_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    /**
     * vp_cut音频回调接口
     */
    public static class vprintcut_callback {
        public static byte[] bufferData = new byte[3200];
        public static volatile byte[] bufferData2 = null;
        public volatile byte[] bufferData_V2 = null;

        public static byte[] getBufferData() {
            return bufferData;
        }

        public static byte[] getBufferData(int length) {
            if (length < 0)
                length = 0;
            if (bufferData2 == null || bufferData2.length != length)
                synchronized (vprintcut_callback.class) {
                    if (bufferData2 == null || bufferData2.length != length)
                        bufferData2 = new byte[length];
                }
            return bufferData2;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }

        public byte[] getBufferData_v2(int length) {
            if (length < 0)
                length = 0;
            if (bufferData_V2 == null || bufferData_V2.length != length)
                synchronized (this) {
                    if (bufferData_V2 == null || bufferData_V2.length != length)
                        bufferData_V2 = new byte[length];
                }
            return bufferData_V2;
        }
    }

    public interface sevc_doa_callback {
        int run(int type, byte[] data, int size);
    }

    public interface sevc_noise_callback {
        int run(int type, byte[] data, int size);
    }

    public interface multibf_callback {
        int run(int type, byte[] data, int size);
    }

    public interface echo_voip_callback {
        int run(int type, byte[] data, int size);
    }

    public interface agc_callback {
        int run(int type, byte[] data, int size);
    }

}
