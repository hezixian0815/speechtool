package com.aispeech.kernel;

import com.aispeech.common.Log;

/**
 * Created by wuwei on 18-5-11.
 */

public class Gram {
    private static final String TAG = "Gram";
    private static boolean loadGramOk = false;

    static {
        try {
            Log.d(TAG, "before load gram library");
            System.loadLibrary("gram");
            Log.d(TAG, "after load gram library");
            loadGramOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadGramOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libgram.so, and put it in your libs dir!");

        }
    }

    private long engineId;

    public static boolean isGramSoValid() {
        return loadGramOk;
    }

    /*
     * core_gram
     * */
    public static native long dds_gram_new(String cfg);

    public static native int dds_gram_start(long engine, String param);

    public static native int dds_gram_delete(long engine);

    /**
     * 初始化AIEngine实例，在调用start, delete等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException} 线程安全
     *
     * @param cfg 引擎配置串
     * @return engineId
     */
    public long init(String cfg) {
        Log.v(TAG, "before AIEngine.new():cfg:" + cfg);
        engineId = dds_gram_new(cfg);
        Log.d(TAG, "AIEngine.new():" + engineId);
        return engineId;
    }

    /**
     * 开始AIEngine的一次请求
     *
     * @param param 开始请求参数,JSON格式字符串
     * @return recordId：成功；null: 失败；
     */
    public int start(String param) {
        int ret = 0;
        if (param == null || "".equals(param)) {
            return -1;
        }

        Log.d(TAG, "AIEngine.start():" + engineId);
        ret = dds_gram_start(engineId, param);
        if (ret < 0) {
            Log.e(TAG, "AIEngine.start() failed! Error code: " + ret);
            return -1;
        }

        return ret;
    }

    /**
     * 释放AIEngine资源 线程安全
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public void destroy() {
        Log.d(TAG, "AIEngine.delete():" + engineId);
        dds_gram_delete(engineId);
        Log.d(TAG, "AIEngine.delete() finished:" + engineId);
        engineId = 0;
    }
}
