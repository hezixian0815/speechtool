package com.aispeech.kernel;

import com.aispeech.common.Log;

public class Mds implements LiteSoFunction {

    private static final String TAG = "Mds";
    protected static boolean loadSoSuc;

    static {
        try {
            Log.d(TAG, "before load mds library");
            System.loadLibrary("mds");
            Log.d(TAG, "after load mds library");
            loadSoSuc = true;
        } catch (UnsatisfiedLinkError e) {
            loadSoSuc = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libmds.so, and put it in your libs dir!");
        }
    }

    private long engineId;

    public static boolean isSoValid() {
        return loadSoSuc;
    }

    public static native long dds_mds_new(String cfg, mds_callback callback);

    public static native int dds_mds_start(long engine, String param);

    public static native int dds_mds_set(long engine, String param);

    public static native int dds_mds_feed(long engine, byte[] data, int size);

    public static native int dds_mds_McdmFeed(long engine, float[] data, int num, int size);

    public static native int dds_mds_cancel(long engine);

    public static native int dds_mds_stop(long engine);

    public static native int dds_mds_delete(long engine);

    @Override
    public long init(String cfg, Object... callbacks) {
        if (callbacks == null || callbacks.length == 0) {
            Log.d(TAG, "have no callbacks when init");
            return 0;
        }
        Log.v(TAG, "before init engineId:" + engineId + " cfg " + cfg);
        engineId = dds_mds_new(cfg, (mds_callback) callbacks[0]);
        Log.d(TAG, "init engineId:" + engineId + " cfg " + cfg);
        return engineId;
    }

    @Override
    public int setCallback(Object... callbacks) {
        return -1;
    }

    @Override
    public int start(String param) {
        Log.v(TAG, "before start param:" + param);
        int ret = dds_mds_start(engineId, param);
        Log.d(TAG, "start ret:" + ret);
        return ret;
    }

    @Override
    public int set(String setParam) {
        Log.v(TAG, "before set: " + setParam);
        int ret = dds_mds_set(engineId, setParam);
        Log.d(TAG, "set: " + setParam + " ret " + ret);
        return ret;
    }

    @Override
    public int get(String getParam) {
        return -1;
    }

    @Override
    public int feed(byte[] data, int size) {
        Log.v(TAG, "before feed: " + data.length);
        int ret = dds_mds_feed(engineId, data, size);
        if (ret != 0)
            Log.d(TAG, "feed ret:" + ret + " size:" + size);
        return ret;
    }

    @Override
    public int cancel() {
        Log.v(TAG, "before cancel: ");
        int ret = dds_mds_cancel(engineId);
        Log.d(TAG, "cancel ret:" + ret);
        return ret;
    }

    @Override
    public int stop() {
        Log.v(TAG, "before stop: ");
        int ret = dds_mds_stop(engineId);
        Log.d(TAG, "stop ret:" + ret);
        return ret;
    }

    @Override
    public int destroy() {
        Log.v(TAG, "before destroy: ");
        int ret = dds_mds_delete(engineId);
        engineId = 0;
        Log.d(TAG, "destroy ret:" + ret);
        return ret;
    }



    /**
     * 用户通过此方法传入mds的算法值，决策出哪个是最优设备
     * @param data float数组：每个设备的snr算法值有三个，实例：如三台设备a、b、c各有三个值 数组格式为 [a1,b1,c1,a2,b2,c2,a3,b3,c3]
     * @param num
     * @param size
     * @return 唤醒设备的索引
     */
    public int mcdmFeed(float[] data, int num, int size) {
        Log.v(TAG, "before mcdmFeed: ");
        int ret = dds_mds_McdmFeed(engineId, data, num, size);
        Log.d(TAG, "mcdmFeed ret:" + ret);
        return ret;
    }

    public interface mds_callback {

        /**
         * 回调方法,只回调json
         */
        int run(int type, byte[] data, int size);
    }
}
