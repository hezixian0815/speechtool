package com.aispeech.kernel;

import com.aispeech.common.Log;

public class LAsr {
    private static final String TAG = "LAsr";
    private static boolean loadAsrOk = false;

    static {
        try {
            Log.d(TAG, "before load lasr library");
            System.loadLibrary("stream_asr");
            Log.d(TAG, "after load lasr library");
            loadAsrOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadAsrOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libstream_asr.so, and put it in your libs dir!");
        }
    }

    private long engineId;

    /**
     * 是否库文件已经加载
     *
     * @return the boolean
     */
    public static boolean isEngineSoValid() {
        return loadAsrOk;
    }

    /*
     *  native methods
     *
     */
    public static native long stream_asr_engine_new(String cfg, lasr_callback callback);
    public static native int stream_asr_engine_start(long engine, String param);
    public static native int stream_asr_engine_feed(long engine, byte[] data);
    public static native int stream_asr_engine_stop(long engine);
    public static native int stream_asr_engine_delete(long engine);
    public static native String stream_asr_engine_get_version();

    /**
     * 初始化stream_asr_engine实例，在调用start, stop, cancel等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException} 线程安全
     *
     * @param cfg      引擎配置串
     * @param callback callback
     * @return engineId
     */
    public long init(String cfg, lasr_callback callback) {
        Log.d(TAG, "before stream_asr_engine.new():" + cfg);
        engineId = stream_asr_engine_new(cfg, callback);
        Log.d(TAG, "stream_asr_engine.new():" + engineId);
        return engineId;
    }

    /**
     * 开始stream_asr_engine的一次请求，请求过程中会以StreamASREngineListener回调的形式响应事件 线程安全
     *
     * @param param 开始请求参数，json格式的字符串
     * @return recordId：成功；null: 失败；
     */
    public int start(String param) {
        int ret = 0;
        if (param == null) {
            return -1;
        }

        Log.d(TAG, "lasr_engine.start():" + engineId);
        ret = stream_asr_engine_start(engineId, param);
        if (ret < 0) {
            Log.e(TAG, "lasr_engine.start() failed! Error code: " + ret);
            return -1;
        }

        return ret;
    }

    /**
     * 向引擎提交数据
     *
     * @param buffer 提交的数据
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int feed(byte[] buffer) {
        int opt = stream_asr_engine_feed(engineId, buffer);
        if (opt != 0) {
            Log.d(TAG, "lasr_engine.feed() opt:" + opt + " length:" + buffer.length +"   engineId:"+engineId);
        }
        return opt;
    }

    /**
     * 停止向引擎提交数据，请求结果
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int stop() {
        Log.d(TAG, "lasr_engine.stop():" + engineId);
        return stream_asr_engine_stop(engineId);
    }


    /**
     * 释放stream_asr_engine资源
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int destroy() {
        Log.d(TAG, "lasr_engine.destroy():" + engineId);
        int ret = stream_asr_engine_delete(engineId);
        Log.d(TAG, "lasr_engine.destroy() finished:" + engineId);
        engineId = 0;
        return ret;
    }

    public static String getVersion() {
        return stream_asr_engine_get_version();
    }


    /**
     * LAsrEngine回调接口
     */
    public interface lasr_callback {

        /**
         * 回调方法
         *
         * @param data json格式的byteArray
         * @param size data size
         * @return
         */
        public abstract int run(byte[] data, int size);
    }
}
