package com.aispeech.kernel;

import com.aispeech.common.Log;

/**
 * Created by wuwei on 18-6-19.
 */

public class Echo {
    private static final String TAG = "Echo";
    private static boolean loadEchoOk = false;

    static {
        try {
            Log.d(TAG, "before load echo library");
            System.loadLibrary("echo");
            Log.d(TAG, "after load echo library");
            loadEchoOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadEchoOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libecho.so, and put it in your libs dir!");

        }
    }

    private long engineId;

    public static boolean isEchoSoValid() {
        return loadEchoOk;
    }

    public static native long dds_echo_new(String cfg, echo_callback callback);

    public static native int dds_echo_setvoipcb(long engine, echo_voip_callback callback);

    public static native int dds_echo_start(long engine, String param);

    public static native int dds_echo_feed(long engine, byte[] data, int size);

    public static native int dds_echo_stop(long engine);

    public static native int dds_echo_cancel(long engine);

    public static native int dds_echo_delete(long engine);

    public long init(String cfg, echo_callback callback) {
        Log.v(TAG, "before dds_echo_new():cfg:" + cfg);
        engineId = dds_echo_new(cfg, callback);
        Log.d(TAG, "dds_echo_new():" + engineId);
        return engineId;
    }

    public int setCallback(echo_voip_callback callback) {
        int ret = dds_echo_setvoipcb(engineId, callback);
        Log.d(TAG, "setCallback() ret:" + ret);
        return ret;
    }

    /**
     * 开始dds_echo的一次请求，请求过程中会以echo_callback回调的形式响应事件 线程安全
     *
     * @param param 开始请求参数,JSON格式字符串
     * @return recordId：成功；null: 失败；
     */
    public int start(String param) {
        int ret = 0;

        Log.d(TAG, "dds_echo_start():" + engineId);
        ret = dds_echo_start(engineId, param);
        if (ret < 0) {
            Log.e(TAG, "dds_echo_start() failed! Error code: " + ret);
            return -1;
        }

        return ret;
    }

    /**
     * 向内核提交数据 线程安全
     *
     * @param buffer 提交的数据
     * @return 0:操作成功 -1 :操作失败；
     */
    public int feed(byte[] buffer) {
//        Log.v(TAG, "before dds_echo_feed():buffer size=" + buffer.length);
        Log.dfAudio(TAG, "AIEngine.feed():" + engineId);
        return dds_echo_feed(engineId, buffer, buffer.length);
    }

    /**
     * 停止向服务器提交数据，请求结果 线程安全
     *
     * @return 0:操作成功 -1 :操作失败；
     */
    public int stop() {
        Log.d(TAG, "dds_echo_stop():" + engineId);
        return dds_echo_stop(engineId);
    }

    /**
     * 取消本次start操作 线程安全
     *
     * @return 0:操作成功 -1 :操作失败；
     */
    public int cancel() {
        Log.d(TAG, "dds_echo_cancel():" + engineId);
        return dds_echo_cancel(engineId);
    }

    /**
     * 释放dds_echo资源 线程安全
     *
     * @return 0:操作成功 -1 :操作失败；
     */
    public void destroy() {
        Log.d(TAG, "dds_echo_delete():" + engineId);
        dds_echo_delete(engineId);
        Log.d(TAG, "dds_echo_delete() finished:" + engineId);
        engineId = 0;
    }

    /**
     * dds_echo回调接口
     */
    public static class echo_callback {

        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        /**
         * 回调方法
         *
         * @param type json/binary
         * @param data data
         * @param size data size
         * @return
         */
        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    public static class echo_voip_callback {

        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }
}
