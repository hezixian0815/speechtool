package com.aispeech.kernel;

import com.aispeech.common.Log;

/**
 * 输入多通道数据，输出一路数据。内部这个数据流向: aec bbs nr agc
 */
public class Sevc {

    private static final String TAG = "Sevc";
    private static boolean mLoadSoOk;

    static {
        try {
            Log.d(TAG, "before load sevc library");
            System.loadLibrary("sevc");
            Log.d(TAG, "after load sevc library");
            mLoadSoOk = true;
        } catch (UnsatisfiedLinkError e) {
            mLoadSoOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libsevc.so, and put it in your libs dir!");
        }
    }

    private long engineId;

    public static boolean isSoValid() {
        return mLoadSoOk;
    }

    public static native long dds_sevc_new(String cfg);

    public static native int dds_sevc_setinputcb(long id, data_input_callback callback);

    public static native int dds_sevc_setoutputcb(long id, data_output_callback callback);

    public static native int dds_sevc_start(long id, String param);

    public static native int dds_sevc_feed(long id, byte[] data, int size);

    public static native int dds_sevc_set(long id, String setParam);

    public static native int dds_sevc_get(long id, String param);

    public static native int dds_sevc_stop(long id);

    public static native int dds_sevc_delete(long id);

    public static native int dds_sevc_setdoacb(long id, doa_callback callback);

    public static native int dds_sevc_setnoisecb(long id, noise_callback callback);

    public long init(String cfg) {
        engineId = dds_sevc_new(cfg);
        Log.d(TAG, "init engineId " + engineId);
        return engineId;
    }

    public int setCallback(Object... callbacks) {
        if (callbacks == null || callbacks.length == 0) {
            return 0;
        }
        Log.d(TAG, "setCallback callbacks.length:" + callbacks.length);

        for (Object callback : callbacks) {
            int ret;
            if (callback instanceof data_input_callback) {
                ret = dds_sevc_setinputcb(engineId, (data_input_callback)callback);
                Log.d(TAG, "dds_sevc_setinputcb ret : " + ret);
            } else if (callback instanceof data_output_callback) {
                ret = dds_sevc_setoutputcb(engineId, (data_output_callback) callback);
                Log.d(TAG, "dds_sevc_setoutputcb ret : " + ret);
            } else if (callback instanceof doa_callback) {
                ret = dds_sevc_setdoacb(engineId, (doa_callback) callback);
                Log.d(TAG, "dds_sevc_setdoacb ret : " + ret);
            } else if (callback instanceof noise_callback) {
                ret = dds_sevc_setnoisecb(engineId, (noise_callback) callback);
                Log.d(TAG, "dds_sevc_setnoisecb ret : " + ret);
            } else {
                Log.e(TAG, "setCallback err  callback:" + callback);
                ret = -1;
            }
            // 0 成功  -9892 表示内核不支持该功能，由资源确定是否支持
            if (ret != 0 && ret != -9892)
                return ret;
        }
        return 0;
    }

    public int start(String param) {
        int ret = dds_sevc_start(engineId, param);
        Log.d(TAG, "start ret " + ret);
        return ret;
    }

    public int feed(byte[] data) {
        return dds_sevc_feed(engineId, data, data.length);
    }

    public int set(String setParam) {
        int ret = dds_sevc_set(engineId, setParam);
        Log.d(TAG, "set ret " + ret);
        return ret;
    }

    public int get(String param) {
        int ret = dds_sevc_get(engineId, param);
        Log.d(TAG, "get ret " + ret);
        return ret;
    }

    public int stop() {
        int ret = dds_sevc_stop(engineId);
        Log.d(TAG, "stop ret " + ret);
        return ret;
    }

    public int destroy() {
        int ret = dds_sevc_delete(engineId);
        Log.d(TAG, "stop ret " + ret);
        engineId = 0;
        return ret;
    }

    public static class data_input_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        // 内部多线程处理，回调函数与 `feed` 主线程不属于同一个线程。
        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    public static class data_output_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        // 内部多线程处理，回调函数与 `feed` 主线程不属于同一个线程。
        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    public interface doa_callback {
        int run(int type, byte[] data, int size);
    }

    public interface noise_callback {
        int run(int type, byte[] data, int size);
    }
}
