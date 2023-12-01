package com.aispeech.common;

import android.util.Log;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 异步转同步工具类
 *
 * @param <T>
 */
public abstract class SynchronizedHelper<T> {

    public String TAG = "SynchronizedHelper";
    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    private volatile boolean requesting = false;

    private T result;

    public T request(long time) {
        requesting = true;
        lock.lock();
        try {
            doRequest();
            boolean await = condition.await(time, TimeUnit.MILLISECONDS);
            Log.d(TAG, "await result:" + await);
            if (!await) return onTimeOut();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
            requesting = false;
        }

        return result;
    }

    public void notifyResult(T result) {
        this.result = result;
        lock.lock();
        try {
            condition.signal();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
    }

    public boolean isRequesting() {
        return requesting;
    }

    protected abstract void doRequest();

    protected abstract T onTimeOut();


}
