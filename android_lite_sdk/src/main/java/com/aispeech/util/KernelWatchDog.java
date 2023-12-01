package com.aispeech.util;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.aispeech.common.ThreadNameUtil;
import com.aispeech.lite.BaseKernel;

import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description: 检测Kernel层 是否阻塞的工具类
 * Author: junlong.huang
 * CreateTime: 2023/8/21
 */
public class KernelWatchDog {

    private static final String TAG = "KernelWatchDog";
    HandlerThread innerThread;
    Handler innerHandler;
    long timeoutMillis = 2000;

    static final int MSG_INCREMENT = 0x01;

    private static volatile KernelWatchDog mInstance;
    private ConcurrentHashMap<BaseKernel, AtomicInteger> monitorMap;
    private Vector<BaseKernel> removeList;

    public static KernelWatchDog getInstance() {

        if (mInstance == null) {
            synchronized (KernelWatchDog.class) {
                if (mInstance == null) {
                    mInstance = new KernelWatchDog();
                }
            }
        }
        return mInstance;
    }

    private KernelWatchDog() {
        init();
    }

    private void init() {
        monitorMap = new ConcurrentHashMap<>();
        removeList = new Vector<>();
        innerThread = new HandlerThread(ThreadNameUtil.getSimpleThreadName("watchdog-k"));
        innerThread.start();
        innerHandler = new InnerHandler(innerThread.getLooper());
        innerHandler.sendMessage(innerHandler.obtainMessage(MSG_INCREMENT));
    }

    public void addChecker(BaseKernel baseKernel) {
        Log.i(TAG, "addChecker:" + baseKernel.getInnerThreadName());
        monitorMap.put(baseKernel, new AtomicInteger(baseKernel.getTick()));
    }

    public void removeChecker(BaseKernel baseKernel) {
        if (monitorMap.containsKey(baseKernel)) {
            Log.i(TAG, "removeChecker: " + baseKernel.getInnerThreadName());
            monitorMap.remove(baseKernel);
        }
    }

    class InnerHandler extends Handler {

        public InnerHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (innerHandler == null) return;

            switch (msg.what) {
                case MSG_INCREMENT:
                    if (monitorMap == null || monitorMap.size() == 0) {
                        innerHandler.sendMessageDelayed(innerHandler.obtainMessage(MSG_INCREMENT), timeoutMillis);
                        break;
                    }

                    for (BaseKernel baseKernel : monitorMap.keySet()) {
                        if (baseKernel.getInnerThread() != null &&
                                !baseKernel.getInnerThread().isAlive()) {
                            Log.i(TAG, "Detected thread quit,Add to list to be removed");
                            removeList.add(baseKernel);
                            continue;
                        }
                        AtomicInteger lastTick = monitorMap.get(baseKernel);
                        if (lastTick == null) lastTick = new AtomicInteger(baseKernel.getTick());

                        if (lastTick.get() != baseKernel.getTick()) {
                            Log.w(TAG, "Detected target thread may blocked,export thread stack");
                            Thread innerThread = baseKernel.getInnerThread();
                            if (innerThread != null) {
                                Log.w(TAG, getThreadStack(innerThread.getStackTrace()));
                            }
                        }

                        lastTick.incrementAndGet();
                        baseKernel.tick();
                    }

                    for (BaseKernel baseKernel : removeList) {
                        monitorMap.remove(baseKernel);
                    }
                    removeList.clear();
                    innerHandler.sendMessageDelayed(innerHandler.obtainMessage(MSG_INCREMENT), timeoutMillis);
                    break;
            }
        }
    }


    public void release() {
        innerHandler.removeMessages(MSG_INCREMENT);
        innerThread.quit();
        monitorMap.clear();
        removeList.clear();
    }

    private String getThreadStack(StackTraceElement[] elements) {
        StringBuilder stackTraceString = new StringBuilder();
        for (StackTraceElement element : elements) {
            stackTraceString.append(element.toString()).append("\n");
        }

        return stackTraceString.toString();
    }
}
