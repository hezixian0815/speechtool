package com.aispeech.kernel;

import com.aispeech.common.Log;


/**
 * Created by wuwei on 2018/7/11.
 */

public class Vprint extends BaseLiteSo {
    private static final String TAG = "Vprint";
    private static boolean loadVprintOk = false;

    static {
        try {
            Log.d(TAG, "before load vprint library");
            System.loadLibrary("vprint");
            Log.d(TAG, "after load vprint library");
            loadVprintOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadVprintOk = false;
            e.printStackTrace();
            Log.e(TAG, "Please check useful libvprint.so, and put it in your libs dir!");

        }
    }

    public static boolean isVprintSoValid() {
        return loadVprintOk;
    }

    // dds_vprint_new 模型在内部存储（兼容旧版本）
    public static native long dds_vprint_new(String cfg, vprint_callback callback);

    // dds_vprint_new2 表示模型在外部存储
    public static native long dds_vprint_new2(String cfg, vprint_callback callback);

    public static native int dds_vprint_start(long engine, String param);

    public static native int dds_vprint_feed(long engine, byte[] data, int size, int type);

    public static native int dds_vprint_stop(long engine);

    public static native int dds_vprint_delete(long engine);

    public static native String dds_vprint_get_version_info(long engine);

    /**
     * 初始化AIEngine实例，在调用start, stop, cancel等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException} 线程安全
     *
     * @param cfg      引擎配置串
     * @param callback callback
     * @return mEngineId
     */
    public long init(boolean useDatabaseStorage, String cfg, vprint_callback callback) {
        Log.d(TAG, "before AIEngine.new(): cfg:" + cfg);
        if (useDatabaseStorage)
            mEngineId = dds_vprint_new2(cfg, callback);
        else
            mEngineId = dds_vprint_new(cfg, callback);
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
        if (param == null || "".equals(param)) {
            return -1;
        }
        Log.d(TAG, "AIEngine.start():" + mEngineId);
        ret = dds_vprint_start(mEngineId, param);
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
    public int feed(byte[] buffer, int size, int type) {
        Log.v(TAG, "AIEngine.feed(): buffer.length" + buffer.length);
        int opt = dds_vprint_feed(mEngineId, buffer, size, type);
        Log.v(TAG, "AIEngine.feed(): buffer.length" + buffer.length + " opt " + opt);
        return opt;
    }

    /**
     * 停止向服务器提交数据，请求结果 线程安全
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int stop() {
        Log.d(TAG, "AIEngine.stop():" + mEngineId);
        return dds_vprint_stop(mEngineId);
    }

    /**
     * 释放AIEngine资源 线程安全
     * <p>
     * 0:操作成功；
     * -1 :操作失败；
     */
    public void destroy() {
        destroyEngine();
        Log.d(TAG, "AIEngine.delete():" + mEngineId);
        dds_vprint_delete(mEngineId);
        Log.d(TAG, "AIEngine.delete() finished:" + mEngineId);
        mEngineId = 0;
    }

    @Override
    protected String JNIVersionInfo() {
        String versionInfo = dds_vprint_get_version_info(mEngineId);
        Log.i(TAG, "JNIVersionInfo: " + versionInfo);
        return versionInfo;
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

    /**
     * AIEngine回调接口
     */
    public interface vprint_callback {

        /**
         * 回调方法
         *
         * @param type json/binary
         * @param data data
         * @param size data size
         * @return 0 成功
         */
        int run(int type, byte[] data, int size);

        /**
         * 查询模型数量
         *
         * @return 模型数量
         */
        int getModelNum();

        /**
         * 获取每个模型的大小
         *
         * @return 每个模型的大小
         */
        int[] getModelSize();

        /**
         * 获取所有模型的数据
         *
         * @return 所有模型的数据
         */
        byte[][] getModelBin();

        /**
         * 对声纹数据的操作，增删改
         *
         * @param type 操作类型 {@linkplain MODEL_MSG_TYPE}
         * @param id   用户名+唤醒词
         * @param data 声纹模型数据
         * @param size 声纹模型数据的长度
         * @param num  模型个数
         * @return 0 成功  小于0 失败
         */
        int model_run(int type, String id, byte[][] data, int[] size, int num);
    }
}
