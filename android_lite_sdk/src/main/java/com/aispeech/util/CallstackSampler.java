package com.aispeech.util;

/**
 * Description:
 * 按照一定频率采样
 * 目标是找到卡顿时刻前后的堆栈，做大致定位，无法做到精准定位
 * 原则上采样越高，定位越精准
 * Author: junlong.huang
 * CreateTime: 2023/8/25
 */

import android.os.Handler;
import android.os.HandlerThread;

import com.aispeech.common.Log;

import java.util.Arrays;


public class CallstackSampler {
    private static final String TAG = "CallstackSampler";
    private final Thread thread;
    private final Handler mHandler;
    private final long sThreshold = 1000;

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            doSample();
            mHandler.postDelayed(this, sThreshold);
        }
    };

    public CallstackSampler(Thread thread) {
        this.thread = thread;
        HandlerThread mWorkThread = new HandlerThread("StackSampler" + thread.getName());
        mWorkThread.start();
        mHandler = new Handler(mWorkThread.getLooper());
    }

    private void doSample() {
        // 采集指定线程当前堆栈信息
        StackTraceElement[] stackTrace = thread.getStackTrace();
        String stackTraceString = Arrays.toString(stackTrace);
        if (!stackTraceString.contains("nativePollOnce")) {
            Log.d(TAG, thread.getName() + " Callstack sample taken at time: " + System.currentTimeMillis() + " " + stackTraceString);
        }
    }

    public void startSampling() {
        mHandler.postDelayed(mRunnable, sThreshold);
    }

    public void stopSampling() {
        mHandler.removeCallbacks(mRunnable);
    }
}
