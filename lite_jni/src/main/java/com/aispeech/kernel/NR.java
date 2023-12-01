package com.aispeech.kernel;

import com.aispeech.common.Log;

public class NR {
    private static final String TAG = "NR";
    private static boolean loadNrOk = false;

    static {
        try {
            Log.d(TAG, "before load nr library");
            System.loadLibrary("nr");
            Log.d(TAG, "after load nr library");
            loadNrOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadNrOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libnr.so, and put it in your libs dir!");

        }
    }

    private long engineId;

    public static boolean isNrSoValid() {
        return loadNrOk;
    }

    /*
     * core_vad
     * */
    public static native long dds_nr_new(String cfg, nr_callback callback);

    public static native int dds_nr_start(long engine, String param);

    public static native int dds_nr_feed(long engine, byte[] data, int size);

    public static native int dds_nr_stop(long engine);

    public static native int dds_nr_delete(long engine);

    /**
     * 初始化AIEngine实例，在调用start, stop, cancel等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException} 线程安全
     *
     * @param cfg      引擎配置串
     * @param callback callback
     * @return engineId
     */
    public long init(String cfg, nr_callback callback) {
        engineId = dds_nr_new(cfg, callback);
        Log.d(TAG, "AIEngine.new():" + engineId);
        return engineId;
    }

    /**
     * 开始AIEngine的一次请求，请求过程中会以aiengine_callback回调的形式响应事件 线程安全
     *
     * @param param 开始请求参数,JSON格式字符串
     * @return recordId：成功；null: 失败；
     */
    public int start(String param) {
        int ret = 0;
        Log.d(TAG, "AIEngine.start():" + engineId);
        ret = dds_nr_start(engineId, param);
        if (ret < 0) {
            Log.e(TAG, "AIEngine.start() failed! Error code: " + ret);
            return -1;
        }
        return ret;
    }

    /**
     * 向内核提交数据 线程安全
     *
     * @param buffer 提交的数据
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int feed(byte[] buffer) {
        return dds_nr_feed(engineId, buffer, buffer.length);
    }

    /**
     * 停止向服务器提交数据，请求结果 线程安全
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int stop() {
        Log.d(TAG, "AIEngine.stop():" + engineId);
        return dds_nr_stop(engineId);
    }

    /**
     * 释放AIEngine资源 线程安全
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public void destroy() {
        Log.d(TAG, "AIEngine.delete():" + engineId);
        dds_nr_delete(engineId);
        Log.d(TAG, "AIEngine.delete() finished:" + engineId);
        engineId = 0;
    }

    /**
     * AIEngine回调接口
     */
    public static class nr_callback {

        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }
}
