package com.aispeech.kernel;

import com.aispeech.common.Log;

/**
 * Created by yu on 2018/5/4.
 */

public class Wakeup extends BaseLiteSo{
    private static final String TAG = "Wakeup";

    private static boolean loadWakeupOk = false;

    static {
        try {
            Log.d(TAG, "before load wakeup library");
            System.loadLibrary("wakeup");
            Log.d(TAG, "after load wakeup library");
            loadWakeupOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadWakeupOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libwakeup.so, and put it in your libs dir!\n" + e);

        }
    }

    public static boolean isWakeupSoValid() {
        Log.i(TAG, "isWakeupSoValid: "+loadWakeupOk);
        return loadWakeupOk;
    }

    public static native long dds_wakeup_new(String cfg, wakeup_callback callback);

    public static native int dds_wakeup_getConfig(String cfg, config_callback callback);

    public static native int dds_wakeup_start(long engine, String param);

    public static native int dds_wakeup_feed(long engine, byte[] data, int size);

    public static native int dds_wakeup_stop(long engine);

    public static native int dds_wakeup_cancel(long engine);

    public static native int dds_wakeup_delete(long engine);

    public static native int dds_wakeup_set(long engine, String param);

    public static native int dds_wakeup_setvprintcutcb(long engine, vprintcut_callback callback);

    public static native String dds_wakeup_get_version_info(long engine);
    /**
     * 唤醒引擎初始化
     *
     * @param cfg            　初始化配置
     * @param callback       　唤醒回调
     * @param configCallback 获取资源内唤醒信息的回调
     * @return engineId
     */
    public long initWakeup(String cfg, wakeup_callback callback, config_callback configCallback) {
        Log.v(TAG, "before dds_wakeup_new cfg:" + cfg);
        mEngineId = dds_wakeup_new(cfg, callback);
        if (mEngineId != 0 && configCallback != null) {
            int ret = dds_wakeup_getConfig(cfg, configCallback);
            Log.d(TAG, "dds_wakeup_getConfig ret:" + ret);
        }
        init(cfg);
        return mEngineId;
    }

    /**
     * 启动唤醒引擎
     *
     * @param param start参数
     * @return ret
     */
    public int startWakeup(String param) {
        int ret = 0;
        Log.v(TAG, "before dds_wakeup_start param:" + param);
        ret = dds_wakeup_start(mEngineId, param);
        if (ret < 0) {
            Log.e(TAG, "dds_wakeup_start() failed! Error code: " + ret);
            return -1;
        }
        return ret;
    }

    /**
     * 动态设置唤醒引擎env
     *
     * @param setParam
     * @return
     */
    public int setWakeup(String setParam) {
        Log.d(TAG, "dds_wakeup_set :" + setParam);
        return dds_wakeup_set(mEngineId, setParam);
    }

    public int setVprintcutcb(vprintcut_callback callback) {
        Log.v(TAG, "before dds_wakeup_setvprintcutcb :");
        int ret = dds_wakeup_setvprintcutcb(mEngineId, callback);
        Log.d(TAG, "dds_wakeup_setvprintcutcb :" + ret);
        return ret;
    }

    /**
     * feed音频数据
     *
     * @param wakeupData 　音频数据
     * @param size       数据大小
     * @return opt
     */
    public int feedWakeupData(byte[] wakeupData, int size) {
        //  Log.v(TAG, "before dds_wakeup_feed : length" +wakeupData.length );
        Log.dfAudio(TAG, "AIEngine.feed():" + mEngineId);
        return dds_wakeup_feed(mEngineId, wakeupData, size);
    }

    /**
     * stop唤醒引擎
     *
     * @return ret
     */
    public int stopWakeup() {
        Log.d(TAG, "dds_wakeup_stop");
        return dds_wakeup_stop(mEngineId);
    }

    /**
     * cancel唤醒引擎
     *
     * @return ret
     */
    public int cancelWakeup() {
        Log.d(TAG, "dds_wakeup_cancel before");
        int ret = dds_wakeup_cancel(mEngineId);
        Log.d(TAG, "dds_wakeup_cancel after");
        return ret;
    }

    /**
     * destroy唤醒引擎
     *
     * @return ret
     */
    public int destroyWakeup() {
        destroyEngine();
        Log.d(TAG, "dds_wakeup_delete before");
        int ret = dds_wakeup_delete(mEngineId);
        Log.d(TAG, "dds_wakeup_delete after");
        return ret;
    }


    @Override
    protected String JNIVersionInfo() {
        String versionInfo = dds_wakeup_get_version_info(mEngineId);
        Log.i(TAG, "JNIVersionInfo: " + versionInfo);
        return versionInfo;
    }


    public interface wakeup_callback {
        int run(int type, byte[] data, int size);
    }

    public interface config_callback {
        int run(int type, byte[] data, int size);
    }

    public static class vprintcut_callback {
        private static final int BYTE_LENGTH = 3200;
        public static byte[] bufferData = new byte[BYTE_LENGTH];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }
}
