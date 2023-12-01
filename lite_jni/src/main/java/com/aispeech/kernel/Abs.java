package com.aispeech.kernel;


import android.util.Log;

/**
 * 基类
 */
public abstract class Abs {
    protected static final String TAG = "Opus";
    protected long mEngineId = 0;// 内核对应的地址
    protected static boolean mLoadSoOk = false;// so文件加载是否成功
    protected boolean mIsStarted = false;// 是否start

    protected boolean checkCore(String method) {
        if (mEngineId == 0) {
            Log.e(TAG, "core is null when call " + method);
            return false;
        }
        return true;
    }

    protected boolean checkStart(String method) {
        if (!mIsStarted) {
            Log.e(TAG, "must call realStart before call " + method);
        }
        return mIsStarted;
    }

}
