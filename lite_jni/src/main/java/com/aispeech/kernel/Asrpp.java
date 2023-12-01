package com.aispeech.kernel;

import com.aispeech.common.Log;

/**
 * Created by wuwei on 2018/7/17.
 */

public class Asrpp extends BaseLiteSo {
    private static final String TAG = "Asrpp";
    private static boolean loadAsrppOk = false;

    static {
        try {
            Log.d(TAG, "before load Asrpp library");
            System.loadLibrary("asrpp");
            Log.d(TAG, "after load Asrpp library");
            loadAsrppOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadAsrppOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libasrpp.so, and put it in your libs dir!");

        }
    }

    public static boolean isAsrppSoValid() {
        return loadAsrppOk;
    }

    /*
     * core_asr
     * */
    public static native long dds_asrpp_new(String cfg, asrpp_callback callback);

    public static native int dds_asrpp_start(long engine, String param);

    public static native int dds_asrpp_feed(long engine, byte[] data, int size);

    public static native int dds_asrpp_stop(long engine);

    public static native int dds_asrpp_cancel(long engine);

    public static native int dds_asrpp_delete(long engine);

    public static native String dds_asrpp_get_version_info(long engine);

    /**
     * 初始化AIEngine实例，在调用start, stop, cancel等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException} 线程安全
     *
     * @param cfg      引擎配置串
     * @param callback callback
     * @return mEngineId
     */
    public long init(String cfg, asrpp_callback callback) {
        Log.v(TAG, "before AIEngine.new():");
        mEngineId = dds_asrpp_new(cfg, callback);
        Log.d(TAG, "AIEngine.new():" + mEngineId);
        init(cfg);
        return mEngineId;
    }

    /**
     * 开始AIEngine的一次请求，请求过程中会以aiengine_callback回调的形式响应事件 线程安全
     *
     * @param param 开始请求参数,JSON格式字符串
     * @return recordId：成功；null: 失败；
     */
    public int start(String param) {
        int ret = 0;

        Log.v(TAG, "before AIEngine.start():" + param);
        ret = dds_asrpp_start(mEngineId, param);
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
    public int feed(byte[] buffer, int size) {
        Log.df(10, TAG, "AIEngine.feed():" + mEngineId);
        return dds_asrpp_feed(mEngineId, buffer, size);
    }

    /**
     * 停止向服务器提交数据，请求结果 线程安全
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int stop() {
        Log.v(TAG, "AIEngine.stop():" + mEngineId);
        return dds_asrpp_stop(mEngineId);
    }

    public int cancel() {
        Log.d(TAG, "AIEngine.cancel():" + mEngineId);
        return dds_asrpp_cancel(mEngineId);
    }

    /**
     * 释放AIEngine资源 线程安全
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public void destroy() {
        destroyEngine();
        Log.d(TAG, "AIEngine.delete():" + mEngineId);
        dds_asrpp_delete(mEngineId);
        Log.d(TAG, "AIEngine.delete() finished:" + mEngineId);
        mEngineId = 0;
    }


    @Override
    protected String JNIVersionInfo() {
        String versionInfo = dds_asrpp_get_version_info(mEngineId);
        Log.i(TAG, "JNIVersionInfo: " + versionInfo);
        return versionInfo;
    }


    /**
     * AIEngine回调接口
     */
    public interface asrpp_callback {

        /**
         * 回调方法
         *
         * @param type json/binary
         * @param data data
         * @param size data size
         * @return
         */
        public abstract int run(int type, byte[] data, int size);
    }
}
