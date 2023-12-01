package com.aispeech.kernel;


import android.util.Log;

/**
 * 基类
 */
public abstract class AbsTts extends BaseLiteSo {
    protected static final String TAG = "Cntts";
    protected static final int ERROR_SO = -1;// so加载失败
    protected static final int ERROR_CORE = -2;// core创建失败
    protected static final int ERROR_START = -3;// start失败
    protected static final int ERROR_DATA = -4;// 数据为空
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
