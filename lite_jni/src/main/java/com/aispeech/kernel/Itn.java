package com.aispeech.kernel;


import com.aispeech.common.Log;

/**
 * 实现中文文本转数字
 */
public class Itn {
    final static String TAG = "ITN";
    private static boolean loadDuiItnOk = false;
    private long engineId;

    static {
        try {
            Log.d(TAG, "before load duiitn library");
            System.loadLibrary("duiitn");
            Log.d(TAG, "after load duiitn library");
            loadDuiItnOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadDuiItnOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libduiitn.so, and put it in your libs dir!");

        }
    }

    public static boolean isUtilsSoValid() {
        return loadDuiItnOk;
    }


    /**
     * 初始化AIEngine实例，在调用feed 等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException} 线程安全
     *
     * @param cfg      引擎配置串
     * @param callback callbackInMainLooper
     * @return engineId
     */
    public long init(String cfg, itn_callback callback) {
        Log.d(TAG, "dds_itn_new: cfg=" + cfg);
        engineId = dds_itn_new(cfg, callback);
        Log.d(TAG, "dds_itn_new end engineId=" + engineId);
        return engineId;
    }

    /**
     * 送入需转换的"字符串"，通过itn_callback实时送出（非异步处理）
     *
     * @param
     * @return {@link 'AIConstant#OPT_SUCCESS}:操作成功；
     * {@link 'AIConstant#OPT_FAILED} :操作失败；
     */
    public int feed(String str) {
        Log.d(TAG, "dds_itn_feed: str=" + str);
        int status = dds_itn_feed(engineId, str);
        Log.d(TAG, "dds_itn_feed end status=" + status);
        return status;
    }

    /**
     * 释放AIEngine资源 线程安全
     *
     * @return {@link 'AIConstant#OPT_SUCCESS}:操作成功；
     * {@link 'AIConstant#OPT_FAILED} :操作失败；
     */
    public void destroy() {
        Log.d(TAG, "dds_itn_delete:" + engineId);
        dds_itn_delete(engineId);
        Log.d(TAG, "dds_itn_delete finished:");
        engineId = 0;
    }

    /**
     * AIEngine回调接口
     */
    public interface itn_callback {

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

    public static native long dds_itn_new(String cfg, itn_callback callback);

    public static native int dds_itn_feed(long engineId, String str);

    public static native int dds_itn_delete(long engineId);

}
