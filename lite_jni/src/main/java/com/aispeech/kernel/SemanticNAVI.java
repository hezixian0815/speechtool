package com.aispeech.kernel;

import com.aispeech.common.Log;

public class SemanticNAVI {
    final static String TAG = Log.TagPrefix.KERNEL + "navi";
    private long semanticEngineId;
    private static boolean loadSemanticOk = false;
    static {
        try {
            Log.d(TAG, "before load semantic_navi library");
            System.loadLibrary("semantic_navi");
            Log.d(TAG, "after load semantic_navi library");
            loadSemanticOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadSemanticOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful semantic_navi.so, and put it in your libs dir!");

        }
    }

    public static boolean isSemanticSoValid() {
        return loadSemanticOk;
    }

    /**
     * 语义引擎初始化
     *
     * @param cfg      　初始化配置
     * @param callback 　唤醒回调
     * @return engineId
     */
    public long initSemantic(String cfg, semantic_callback callback) {
        Log.out(TAG, "dds_semantic_navi_new: " + cfg);
        semanticEngineId = dds_semantic_navi_new(cfg, callback);
        return semanticEngineId;
    }


    /**
     * 启动语义引擎
     * @param param start参数
     * @return ret
     */
    public int startSemantic(String param) {
        int ret = 0;
        Log.out(TAG, "dds_semantic_navi_start, param: " + param);
        ret = dds_semantic_navi_start(semanticEngineId, param);
        if (ret < 0) {
            Log.e(TAG, "dds_semantic_navi_start() failed! Error code: " + ret);
            return -1;
        }
        return ret;
    }

    /**
     * destroy语义引擎
     * @return ret
     */
    public int destroySemantic() {
        Log.out(TAG, "dds_semantic_navi_delete before");
        int ret = dds_semantic_navi_delete(semanticEngineId);
        Log.out(TAG, "dds_semantic_navi_delete after");
        return ret;
    }

    /**
     * 更新词库
     *
     * @return ret
     */

    public int updateKV2Bin(String cfg) {
        Log.out(TAG, "updateKV2Bin cfg: " + cfg);
        int ret = dds_semantic_navi_kv2bin(semanticEngineId, cfg);
        if (ret < 0) {
            Log.out(TAG, "dds_semantic_navi_kv2bin failed ！ Error code : " + ret);
            return -1;
        }
        return ret;
    }

    public interface semantic_callback {
        int run(int type, byte[] data, int size);
    }

    public static native long dds_semantic_navi_new(String cfg, semantic_callback callback);

    public static native int dds_semantic_navi_start(long engine, String param);

    public static native int dds_semantic_navi_delete(long engine);

    public static native int dds_semantic_navi_kv2bin(long engine,String cfg);
}
