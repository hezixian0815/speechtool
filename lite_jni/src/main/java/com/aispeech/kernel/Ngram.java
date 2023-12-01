package com.aispeech.kernel;

import android.text.TextUtils;

import com.aispeech.common.Log;

/**
 * Created by thearyong on 2022-3-12.
 */

public class Ngram {
    final static String TAG = "Ngram";
    private static boolean loadNgramOk = false;
    private long engineId;

    static {
        try {
            Log.d(TAG, "before load ngram library");
            System.loadLibrary("ngram");
            Log.d(TAG, "after load ngram library");
            loadNgramOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadNgramOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libngram.so, and put it in your libs dir!");

        }
    }

    public static boolean isNgramSoValid() {
        return loadNgramOk;
    }


    /**
     * 初始化AIEngine实例，在调用start, delete等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException} 线程安全
     *
     * @param cfg 引擎配置串
     * @return engineId
     */
    public long init(String cfg) {
        engineId = dds_ngram_new(cfg);
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
        if (TextUtils.isEmpty(param)) {
            return -1;
        }

        Log.d(TAG, "AIEngine.start():" + engineId);
        ret = dds_ngram_start(engineId, param);
        if (ret < 0) {
            Log.e(TAG, "AIEngine.start() failed! Error code: " + ret);
            return -1;
        }

        return ret;
    }


    /**
     * 释放AIEngine资源 线程安全
     *
     * @return {@link ’AIConstant#OPT_SUCCESS}:操作成功；
     * {@link ‘AIConstant#OPT_FAILED} :操作失败；
     */
    public void destroy() {
        Log.d(TAG, "AIEngine.delete():" + engineId);
        dds_ngram_delete(engineId);
        Log.d(TAG, "AIEngine.delete() finished:" + engineId);
        engineId = 0;
    }


    /*
     * core_gram
     * */
    public static native long dds_ngram_new(String cfg);

    public static native int dds_ngram_start(long engine, String param);

    public static native int dds_ngram_delete(long engine);
}
