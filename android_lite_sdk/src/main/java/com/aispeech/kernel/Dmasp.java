package com.aispeech.kernel;

import android.text.TextUtils;

import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;

public class Dmasp {

    final static String TAG = "Dmasp";
    private static boolean loadDmaspOk = false;
    private long engineId;

    static {
        try {
            Log.d(TAG, "before load dmasp library");
            System.loadLibrary("dmasp");
            Log.d(TAG, "after load dmasp library");
            loadDmaspOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadDmaspOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libdmasp.so, and put it in your libs dir!");
        }
    }

    public static boolean isDmaspSoValid() {
        return loadDmaspOk;
    }

    public long init(String cfg, dmasp_callback callback) {
        Log.d(TAG, "dds_dmasp_new cfg = " + cfg);
        engineId = dds_dmasp_new(cfg, callback);
        Log.d(TAG, "dds_dmasp_new():" + engineId);
        return engineId;
    }

    public int setvprintcutcb(vprintcut_callback vprintcutCallback) {
        Log.d(TAG, "dds_dmasp_setvprintcutcb()");
        int ret = dds_dmasp_setvprintcutcb(engineId, vprintcutCallback);
        Log.d(TAG, "dds_dmasp_setvprintcutcb() ret:" + ret);
        return ret;
    }

    /**
     * 开始dds_dmasp的一次请求，请求过程中会以dmasp_callback回调的形式响应事件 线程安全
     *
     * @param param 开始请求参数,JSON格式字符串
     * @return recordId：成功；null: 失败；
     */
    public int start(String param) {
        if (TextUtils.isEmpty(param)) {
            Log.e(TAG, "dds_dmasp_start param is NULL, no start.");
            return -1;
        }

        if (engineId != 0) {
            Log.d(TAG, "dds_dmasp_stop() ");
            int ret = dds_dmasp_stop(engineId);
            Log.d(TAG, "dds_dmasp_stop() ret = " + ret);
        }

        Log.d(TAG, "dds_dmasp_start() param:" + param);
        int ret = dds_dmasp_start(engineId, param);
        Log.d(TAG, "dds_dmasp_start() ret:" + ret);
        if (ret < 0) {
            Log.e(TAG, "dds_dmasp_start() failed! Error code: " + ret);
            return -1;
        }

        return ret;
    }

    /**
     * 内核动态设置开关，目前用于唤醒模块的单独控制开关
     */
    public int set(String param) {
        if (TextUtils.isEmpty(param)) {
            Log.e(TAG, "dds_dmasp_set param is NULL, no set.");
            return -1;
        }

        int ret = dds_dmasp_set(engineId, param);
        Log.d(TAG, "dds_dmasp_set():" + param + "   ret " + ret);
        return ret;
    }

    public int get(String getParam) {
        int value = dds_dmasp_get(engineId, getParam);
        Log.d(TAG, getParam + " is : " + value);
        return value;
    }

    /**
     * 向内核提交数据 线程安全
     *
     * @param buffer 提交的数据
     * @return {@link AIConstant#OPT_SUCCESS}:操作成功；
     * {@link AIConstant#OPT_FAILED} :操作失败；
     */
    public int feed(byte[] buffer) {
        Log.dfAudio(TAG, "AIEngine.feed():" + engineId);
        int opt = dds_dmasp_feed(engineId, buffer, buffer.length);
        return opt;
    }

    /**
     * 停止向服务器提交数据，请求结果 线程安全
     *
     * @return {@link AIConstant#OPT_SUCCESS}:操作成功；
     * {@link AIConstant#OPT_FAILED} :操作失败；
     */
    public int stop() {
        Log.d(TAG, "dds_dmasp_stop():" + engineId);
        return dds_dmasp_stop(engineId);
    }

    /**
     * 取消本次start操作 线程安全
     *
     * @return {@link AIConstant#OPT_SUCCESS}:操作成功；
     * {@link AIConstant#OPT_FAILED} :操作失败；
     */
    public int cancel() {
        Log.d(TAG, "dds_dmasp_cancel():" + engineId);
        return dds_dmasp_cancel(engineId);
    }

    /**
     * 释放dds_dmasp资源 线程安全
     *
     * @return {@link AIConstant#OPT_SUCCESS}:操作成功；
     * {@link AIConstant#OPT_FAILED} :操作失败；
     */
    public void destroy() {
        Log.d(TAG, "dds_dmasp_delete():" + engineId);
        dds_dmasp_delete(engineId);
        Log.d(TAG, "dds_dmasp_delete() finished:" + engineId);
        engineId = 0;
    }

    /**
     * 获取唤醒资源是否带VAD状态流
     */
    public boolean isWakeupSsp() {
        Log.d(TAG, "dds_dmasp_get_ssp_flag():");
        return dds_dmasp_get_ssp_flag(engineId) == 1;
    }

    /**
     * dds_dmasp回调接口
     */
    public static class dmasp_callback {

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

        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }
    }

    /**
     * vp_cut音频回调接口
     */
    public static class vprintcut_callback {
        public int run(int type, byte[] data, int size) {
            return 0;
        }

        public static byte[] bufferData = new byte[3200];
        public static volatile byte[] bufferData2 = null;

        public static byte[] getBufferData() {
            return bufferData;
        }

        public static byte[] getBufferData(int length) {
            if (length < 0)
                length = 0;
            if (bufferData2 == null || bufferData2.length != length)
                synchronized (Fespx.vprintcut_callback.class) {
                    if (bufferData2 == null || bufferData2.length != length)
                        bufferData2 = new byte[length];
                }
            return bufferData2;
        }

        public volatile byte[] bufferData_V2 = null;

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


    /**
     * @param cfg 内核资源以及唤醒定制
     *            {
     *            "resBinPath": "res/dmasp/dmasp_cfg.bin",
     *            "wakeupBinPath": "res/dmasp/res_xpcar_kws_v6.bin",
     *            "parallel_mode": 1 //个性化定制，此文件路径为加密后的本地通用唤醒的配置信息，内容详见[个性化定制配置格式]
     *            }
     */
    public static native long dds_dmasp_new(String cfg, dmasp_callback callback);

    /**
     * @param param env 唤醒参数， 重新生效
     */
    public static native int dds_dmasp_start(long engine, String param);

    public static native int dds_dmasp_feed(long engine, byte[] data, int size);

    public static native int dds_dmasp_stop(long engine);

    public static native int dds_dmasp_cancel(long engine);

    public static native int dds_dmasp_delete(long engine);

    public static native int dds_dmasp_setvprintcutcb(long engine, vprintcut_callback vprintcutCallback);

    /**
     * @param param 内核控制开关
     *              <p>
     *              "wakeupSwitch":1 //0表示关闭，非0表示打开
     */
    public static native int dds_dmasp_set(long engine, String param);

    /**
     * @param key 属性键值
     */
    public static native int dds_dmasp_get(long engine, String key);


    /**
     * @return 1带ssp，0不带ssp
     */
    public static native int dds_dmasp_get_ssp_flag(long engine);
}
