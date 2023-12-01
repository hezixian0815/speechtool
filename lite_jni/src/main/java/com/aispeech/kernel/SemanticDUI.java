package com.aispeech.kernel;

import com.aispeech.common.Log;

import org.json.JSONException;

public class SemanticDUI {
    final static String TAG =  Log.TagPrefix.KERNEL + "dui";
    private long semanticEngineId;
    private static boolean loadSemanticOk = false;
    static {
        try {
            Log.d(TAG, "before load semantic_dui library");
            System.loadLibrary("semantic_dui");
            Log.d(TAG, "after load semantic_dui library");
            loadSemanticOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadSemanticOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful semantic_dui.so, and put it in your libs dir!");

        }
    }

    public static boolean isSemanticSoValid(){
        return loadSemanticOk;
    }

    /**
     * 语义引擎初始化
     * @param cfg　初始化配置
     * @param callback　唤醒回调
     * @return engineId
     */
    public long initSemanticDUI(String cfg, semantic_callback callback) {
        Log.out(TAG , "dds_semantic_dui_new: "+cfg);
        semanticEngineId = dds_semantic_dui_new(cfg, callback);
        return semanticEngineId;
    }


    /**
     * 启动语义引擎
     * @param param start参数
     * @return ret
     */
    public int startSemanticDUI(String param) {
        int ret = 0;
        Log.out(TAG , "dds_semantic_dui_start, param: " + param);
        ret = dds_semantic_dui_start(semanticEngineId, param);
        if (ret < 0) {
            Log.e(TAG, "dds_semantic_dui_start() failed! Error code: " + ret);
            return -1;
        }
        return ret;
    }

    /**
     * destroy语义引擎
     * @return ret
     */
    public int destroySemanticDUI() {
        Log.out(TAG , "dds_semantic_dui_delete before");
        int ret = dds_semantic_dui_delete(semanticEngineId);
        Log.out(TAG , "dds_semantic_dui_delete after");
        return ret;
    }

    public interface semantic_callback{
        public abstract int run(int type, byte[] data, int size) throws JSONException;
    }

    public static native long dds_semantic_dui_new(String cfg, semantic_callback callback);

    public static native int dds_semantic_dui_start(long engine, String param);

    public static native int dds_semantic_dui_delete(long engine);
}
