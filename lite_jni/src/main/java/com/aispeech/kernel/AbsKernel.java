package com.aispeech.kernel;

import com.aispeech.common.Log;

/**
 * @decription 内核JNI映射类抽象基类
 * @auther wuwei
 * @date 2019-07-17 10:13
 * @email wei.wu@aispeech.com
 */
public abstract class AbsKernel extends BaseLiteSo{
    protected static final int ERROR_SO = -1;// so加载失败
    protected static final int ERROR_CORE = -2;// core创建失败
    protected static final int ERROR_START = -3;// start失败
    protected static final int ERROR_DATA = -4;// 数据为空
    protected static boolean mLoadSoOk = false;// so文件加载是否成功
    protected boolean mIsStarted = false;// 是否start

    protected boolean checkCore(String tag, String method) {
        if (mEngineId == 0) {
            Log.e(tag, "core is null when call " + method);
            return false;
        }
        return true;
    }

    protected boolean checkStart(String tag, String method) {
        if (!mIsStarted) {
            Log.e(tag, "must call realStart before call " + method);
        }
        return mIsStarted;
    }

    public abstract boolean start(String param);
    public abstract int feed(int dataType, byte[] data, int size);
    public abstract int set(String setParam);
    public abstract int stop();
    public abstract int cancel();
    public abstract int release();
}
