package com.aispeech.lite;


import com.aispeech.common.ThreadNameUtil;

import java.util.concurrent.ThreadFactory;

/**
 * @auther wuwei
 */
public class AIThreadFactory implements ThreadFactory {
    public static final String TAG = "AIThreadFactory";
    private String mThreadName;
    private int mThreadPriority = Thread.NORM_PRIORITY;
    private static final String SUFFIX = "-F";

    public AIThreadFactory() {
    }

    public AIThreadFactory(String threadTag, int threadPriority) {
        this.mThreadName = ThreadNameUtil.getFixedThreadName(threadTag) + SUFFIX;
        this.mThreadPriority = threadPriority;
//        Log.d(threadTag, "current thread is : " + mThreadName);
//        Log.d(threadTag, "current thread priority is : " + mThreadPriority);
    }

    public AIThreadFactory(String threadTag) {
        this.mThreadName = ThreadNameUtil.getFixedThreadName(threadTag) + SUFFIX;
    }

    public static AIThreadFactory newSimpleTAGFactory(String name) {
        AIThreadFactory threadFactory = new AIThreadFactory();
        threadFactory.mThreadName = ThreadNameUtil.getSimpleThreadName(name);
        return threadFactory;
    }

    public static AIThreadFactory newSimpleNameFactory(String name) {
        AIThreadFactory threadFactory = new AIThreadFactory();
        threadFactory.mThreadName = name;
        return threadFactory;
    }

    public Thread newThread(Runnable r) {
        Thread t = new Thread(r, mThreadName);
        if (t.isDaemon())
            t.setDaemon(true);
        if (t.getPriority() != mThreadPriority)
            t.setPriority(mThreadPriority);
        return t;
    }
}
