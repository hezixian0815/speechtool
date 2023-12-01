package com.aispeech.kernel;

import com.aispeech.common.Log;

/**
 * @decription 本地vad模块JNI映射类
 * @auther wuwei
 * @date 2019-07-17 14:41
 * @email wei.wu@aispeech.com
 */
public class Vad extends AbsKernel {
    private static final String TAG = "Vad";

    static {
        try {
            Log.d(TAG, "before load " + TAG.toLowerCase() + " library");
            System.loadLibrary(TAG.toLowerCase());
            Log.d(TAG, "after load " + TAG.toLowerCase() + " library");
            mLoadSoOk = true;
        } catch (UnsatisfiedLinkError error) {
            mLoadSoOk = false;
            error.printStackTrace();
            Log.e(TAG, "Please check useful lib" + TAG.toLowerCase() + ".so, and put it in your libs dir!");
        }
    }

    public static boolean isSoValid() {
        return mLoadSoOk;
    }

    public static native long dds_vad_new(String cfg, vad_callback callback);

    public static native int dds_vad_start(long engine, String param);

    public static native int dds_vad_feed(long engine, byte[] data, int size);

    public static native int dds_vad_stop(long engine);

    public static native int dds_vad_cancel(long engine);

    public static native int dds_vad_delete(long engine);

    public static native int dds_vad_setmultionecb(long engine,multione_callback callback);

    public static native int dds_vad_setmultitwocb(long engine, multitwo_callback callback);

    public static native String dds_vad_get_version_info(long engine);

    public boolean init(String cfg, vad_callback callback) {
        if (!isSoValid()) {
            Log.e(TAG, "lib" + TAG.toLowerCase() + ".so load error!");
            return false;
        }
        Log.d(TAG, TAG + " new cfg —> " + cfg);
        mEngineId = dds_vad_new(cfg, callback);
        if (mEngineId == 0) {
            Log.e(TAG, TAG + " new error!");
        } else {
            Log.d(TAG, TAG + " new success!");
        }
        init(cfg);
        return mEngineId != 0;
    }

    @Override
    public boolean start(String param) {
        if (!checkCore(TAG, "start")) {
            Log.e(TAG, "");
            return false;
        }
        Log.d(TAG, TAG + " start param —> " + param);
        int ret = dds_vad_start(mEngineId, param);
        if (ret == 0) {
            mIsStarted = true;
        } else {
            mIsStarted = false;
        }
        return ret == 0;
    }

    @Override
    public int feed(int dataType, byte[] data, int size) {
        if (data == null || size == 0) {
            return ERROR_DATA;
        }
        if (!checkCore(TAG, "feed")) {
            return ERROR_CORE;
        }
        if (!checkStart(TAG, "feed")) {
            return ERROR_START;
        }
        Log.vfd(TAG, "AIEngine.feed():" + mEngineId);
        int ret = dds_vad_feed(mEngineId, data, size);
        return ret;
    }

    @Override
    public int set(String setParam) {
        return 0;
    }

    @Override
    public int stop() {
        if (!checkCore(TAG, "stop")) {
            return ERROR_CORE;
        }
        Log.v(TAG, TAG + " before stop  ");
        int ret = dds_vad_stop(mEngineId);
        mIsStarted = false;
        Log.d(TAG, TAG + " stop success! ret = " + ret);
        return ret;
    }

    @Override
    public int cancel() {
        if (!checkCore(TAG, "cancel")) {
            return ERROR_CORE;
        }
        Log.v(TAG, TAG + " before cancel  ");
        int ret = dds_vad_cancel(mEngineId);
        mIsStarted = false;
        Log.d(TAG, TAG + " cancel success! ret = " + ret);
        return ret;
    }

    @Override
    public int release() {
        if (!checkCore(TAG, "release")) {
            return ERROR_CORE;
        }
        destroyEngine();
        Log.v(TAG, TAG + " before delete  ");
        int ret = dds_vad_delete(mEngineId);
        mIsStarted = false;
        mEngineId = 0;
        Log.d(TAG, TAG + " release success! ret = " + ret);
        return ret;
    }
    public void setmultionecb(multione_callback callback) {
        Log.d(TAG, "AIEngine.setmultionecb():" + mEngineId);
        int status = dds_vad_setmultionecb(mEngineId,callback);
        Log.d(TAG, "AIEngine.setmultionecb() finished:" + mEngineId);
    }

    public void setmultitwocb(multitwo_callback callback) {
        Log.d(TAG, "AIEngine.setmultitwocb():" + mEngineId);
        int status = dds_vad_setmultitwocb(mEngineId,callback);
        Log.d(TAG, "AIEngine.setmultitwocb() finished:" + mEngineId);
    }

    @Override
    protected String JNIVersionInfo() {
        String versionInfo = dds_vad_get_version_info(mEngineId);
        Log.i(TAG, "JNIVersionInfo: " + versionInfo);
        return versionInfo;
    }

    public static class vad_callback {

        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }
    /**
     * AIEngine回调接口
     */
    public static class multione_callback {

        public int run(int type, byte[] data, int size){
            return 0;
        }

        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }
    }


    /**
     * AIEngine回调接口
     */
    public static class multitwo_callback {

        public int run(int type, byte[] data, int size){
            return 0;
        }

        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }
    }
}
