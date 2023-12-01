package com.aispeech.kernel;

import com.aispeech.common.Log;


/**
 * Created by wuwei on 2018/7/11.
 */

public class VprintLite {
    final static String TAG = "VprintLite";
    private static boolean loadVprintOk = false;
    private long engineId;

    static {
        try {
            Log.d(TAG, "before load vprintlite library");
            System.loadLibrary("vprintlite");
            Log.d(TAG, "after load vprintlite library");
            loadVprintOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadVprintOk = false;
            e.printStackTrace();
            Log.e(TAG, "Please check useful libvprintlite.so, and put it in your libs dir!");

        }
    }

    public static boolean isVprintSoValid() {
        return loadVprintOk;
    }


    /**
     * 初始化AIEngine实例，在调用start, stop, cancel等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException} 线程安全
     *
     * @param cfg      引擎配置串
     * @param callback callback
     * @return engineId
     */
    public long init(String cfg, vprintlite_callback callback) {
        Log.v(TAG, "before AIEngine.new():"  + " cfg " + cfg);
        engineId = dds_vprintlite_new(cfg, callback);
        Log.d(TAG, "AIEngine.new():" + engineId + " cfg " + cfg);
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
        if (param == null || "".equals(param)) {
            return -1;
        }
        Log.d(TAG, "AIEngine.start():" + engineId + " param " + param);
        ret = dds_vprintlite_start(engineId, param);
        Log.d(TAG, "start ret : " + ret);
        return ret;
    }

    /**
     * 向内核提交数据 线程安全
     *
     * @param buffer 提交的数据
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int feed(byte[] buffer, int size, String params) {
        Log.d(TAG, "AIEngine.feed():" + engineId + " time " + System.currentTimeMillis() + "params " + params);
        int opt = dds_vprintlite_feed(engineId, buffer, size, params);
        Log.v(TAG, "AIEngine.feed() end" + " time " + System.currentTimeMillis());
        return opt;
    }


    /**
     * 停止向服务器提交数据，请求结果 线程安全
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int stop() {
        Log.d(TAG, "AIEngine.stop():" + engineId);
        return dds_vprintlite_stop(engineId);
    }

    /**
     * 释放AIEngine资源 线程安全
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public void destroy() {
        Log.d(TAG, "AIEngine.delete():" + engineId);
        dds_vprintlite_delete(engineId);
        Log.d(TAG, "AIEngine.delete() finished:" + engineId);
        engineId = 0;
    }


    /**
     * AIEngine回调接口
     */
    public interface vprintlite_callback {

        /**
         * 回调方法
         *
         * @param type json/binary
         * @param data data
         * @param size data size
         * @return 0 成功
         */
        int run(int type, byte[] data, int size);
    }

    public enum MODEL_MSG_TYPE {
        // VP_SELECT(0), //内部已封装，不会输出
        VP_UPDATE(1),
        VP_INSERT(2),
        VP_DELETE(3);
        private final int value;

        MODEL_MSG_TYPE(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static native long dds_vprintlite_new(String cfg, vprintlite_callback callback);

    public static native int dds_vprintlite_start(long engine, String param);

    public static native int dds_vprintlite_feed(long engine, byte[] data, int size, String param);

    public static native int dds_vprintlite_stop(long engine);

    public static native int dds_vprintlite_delete(long engine);
}
