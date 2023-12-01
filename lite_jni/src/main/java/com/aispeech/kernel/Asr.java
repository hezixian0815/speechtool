package com.aispeech.kernel;

import com.aispeech.common.Log;
import com.aispeech.export.engines2.bean.Decoder;

/**
 * Created by wuwei on 18-5-11.
 */

public class Asr extends BaseLiteSo {
    private static final String TAG = Log.TagPrefix.KERNEL + "Asr";
    private static boolean loadAsrOk = false;
    static {
        try {
            Log.d(TAG, "before load asr library");
            System.loadLibrary("asr");
            Log.d(TAG, "after load asr library");
            loadAsrOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadAsrOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libasr.so, and put it in your libs dir!");

        }
    }


    public static boolean isAsrSoValid() {
        return loadAsrOk;
    }

    /*
     * core_asr
     * */
    public static native long dds_asr_new(String cfg, asr_callback callback);

    public static native int dds_asr_start(long engine, String param);

    public static native int dds_asr_feed(long engine, byte[] data, int size);

    public static native int dds_asr_stop(long engine);

    public static native int dds_asr_cancel(long engine);

    public static native int dds_asr_delete(long engine);

    public static native int dds_asr_set(long engine, String param);

    public static native int dds_asr_decoder(long engine, String operation, String param);

    public static native String dds_asr_get_version_info(long engine);


    /**
     * 初始化AIEngine实例，在调用start, stop, cancel等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException} 线程安全
     *
     * @param cfg      引擎配置串
     * @param callback callback
     * @return engineId
     */
    public long init(String cfg, asr_callback callback) {
        Log.v(TAG, "before AIEngine.new():" + cfg);
        mEngineId = dds_asr_new(cfg, callback);
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

        Log.v(TAG, "before AIEngine.start(): param:" + param);
        ret = dds_asr_start(mEngineId, param);
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
    public int feed(byte[] buffer) {
        Log.df(10, TAG, "AIEngine.feed():" + mEngineId);
        int opt = dds_asr_feed(mEngineId, buffer, buffer.length);
        if (opt != 0) {
            Log.d(TAG, "feed() opt:" + opt + " length:" + buffer.length);
        }
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
        return dds_asr_stop(mEngineId);
    }

    /**
     * 取消本次start操作 线程安全
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int cancel() {
        Log.d(TAG, "AIEngine.cancel():" + mEngineId);
        return dds_asr_cancel(mEngineId);
    }

    /**
     * 更新识别资源
     *
     * @param cfg "{\"netBinPath\":\"*******\"}"
     * @return {@link 'AIConstant#OPT_SUCCESS}:操作成功；
     * {@link 'AIConstant#OPT_FAILED} :操作失败；
     */
    public int update(String cfg) {
        Log.d(TAG, "AIEngine.update():" + mEngineId);
        return dds_asr_set(mEngineId, cfg);
    }

    /**
     * 多路解码新方案新增接口，详细见：
     * https://jira.aispeech.com.cn/browse/YJGGZC-6457
     *
     * @param operation 取值参考：{@link Decoder.Action}
     * @param param     env 参数，新增字段active_decoder_list
     * @return {@link 'AIConstant#OPT_SUCCESS}:操作成功；
     * {@link 'AIConstant#OPT_FAILED} :操作失败；
     */
    public int decoder(String operation, String param) {
        Log.out(TAG, "AIEngine.decoder():" + mEngineId);
        int opt;
        if (Decoder.Action.REGISTER.getAction().equals(operation)) {
            //note 先unregister再register非推荐做法，但目前暂无好时机去调用
            opt = dds_asr_decoder(mEngineId, Decoder.Action.UNREGISTER.getAction(), param);
            Log.out(TAG, Decoder.Action.UNREGISTER.getAction() + " opt=" + opt);
        }
        opt = dds_asr_decoder(mEngineId, operation, param);
        Log.out(TAG, operation + " opt=" + opt);
        return opt;
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
        dds_asr_delete(mEngineId);
        Log.d(TAG, "AIEngine.delete() finished:" + mEngineId);
        mEngineId = 0;
    }

    /**
     * 释放AIEngine资源 线程安全
     *
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int set(String params) {
        Log.v(TAG, "before AIEngine.set():" + mEngineId + " params " + params);
        return dds_asr_set(mEngineId, params);
    }

    @Override
    protected String JNIVersionInfo() {
        String versionInfo = dds_asr_get_version_info(mEngineId);
        Log.i(TAG, "JNIVersionInfo: " + versionInfo);
        return versionInfo;
    }

    /**
     * AIEngine回调接口
     */
    public interface asr_callback {

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
