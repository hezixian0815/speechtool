package com.aispeech.kernel;


import android.text.TextUtils;

import com.aispeech.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by djk
 * 合成
 */
public class Cntts extends AbsTts {

    static {
        try {
            Log.d(TAG, "before load cntts library");
            System.loadLibrary("cntts");
            Log.d(TAG, "after load cntts library");
            mLoadSoOk = true;
        } catch (UnsatisfiedLinkError e) {
            mLoadSoOk = false;
            e.printStackTrace();
            Log.e(TAG, "Please check useful libcntts.so, and put it in your libs dir!");
        }
    }

    public static boolean isSoValid() {
        return mLoadSoOk;
    }

    public static native long dds_cntts_new(String cfg, cntts_callback callback);

    public static native int dds_cntts_start(long id, String param);

    public static native int dds_cntts_set(long id, String param);

    public static native int dds_cntts_feed(long id, String text);

    public static native int dds_cntts_delete(long id);

    public static native String dds_cntts_get_version_info(long id);

    public boolean init(String cfg, cntts_callback callback) {
        if (!isSoValid()) {
            Log.e(TAG, "libfespl.so load error!");
            return false;
        }
        Log.d(TAG, "before Tts new cfg = " + cfg);
        mEngineId = dds_cntts_new(cfg, callback);
        if (mEngineId == 0) {
            Log.e(TAG, "Tts new error!");
        } else {
            Log.d(TAG, "Tts new success!");
        }
        init(cfg);
        return mEngineId != 0;
    }

    public boolean start(String param) {
        if (!checkCore("realStart")) {
            return false;
        }
        Log.d(TAG, "Tts realStart cfg = " + param);
        int ret = dds_cntts_start(mEngineId, param);
        if (ret < 0) {
            Log.e(TAG, "Tts realStart failed! Error code = " + ret);
            return false;
        } else {
            mIsStarted = true;
            Log.d(TAG, "Tts realStart success! ret = " + ret);
        }
        return true;
    }

    public int setBackBinPath(String backBinPath) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("backBinPath", backBinPath);
            jsonObject.put("optimization", 1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "Tts setBackBinPath = " + jsonObject.toString());
        return dds_cntts_set(mEngineId, jsonObject.toString());
    }

    public int set(String setParam) {
        if (TextUtils.isEmpty(setParam)) {
            return -1;
        }
        int satus = dds_cntts_set(mEngineId, setParam);
        Log.d(TAG, "Tts set dynamic param = " + setParam  +"     satus:"+satus);
        return satus;
    }

    public int feed(String refText) {
        Log.v(TAG, "Tts feed text = " + refText);
        if (refText == null || "".equals(refText)) {
            Log.w(TAG, "Tts feed error, data == null");
            return ERROR_DATA;
        }
        if (!checkCore("feed")) {
            return ERROR_CORE;
        }
        if (!checkStart("feed")) {
            return ERROR_START;
        }
        int ret = dds_cntts_feed(mEngineId, refText);
        Log.v(TAG, "Tts feed success! ret = " + ret);
        return ret;
    }

    public int release() {
        if (!checkCore("release")) {
            return ERROR_CORE;
        }
        destroyEngine();
        Log.v(TAG, "before release: ");
        int ret = dds_cntts_delete(mEngineId);
        mIsStarted = false;
        mEngineId = 0;
        Log.d(TAG, "Tts release success! ret = " + ret);
        return ret;
    }

    public String getVersion() {
        return null;
    }

    @Override
    protected String JNIVersionInfo() {
        String versionInfo = dds_cntts_get_version_info(mEngineId);
        Log.i(TAG, "JNIVersionInfo: "+versionInfo);
        return versionInfo;
    }


    public static class cntts_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }
}
