package com.aispeech.lite.hotword.ssl;

import com.aispeech.common.Log;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 计数器类
 *
 * @author hehr
 */
public class Counter {

    private static final String TAG = "Counter";
    /**
     * 计数器
     */
    private AtomicInteger count;

    private volatile int[] index;

    public Counter(int size) {
        index = new int[size];
        clearIndex();
        count = new AtomicInteger(0);
    }

    private volatile boolean enableCount = false;

    public void setEnableCount(boolean enableCount) {
        this.enableCount = enableCount;
    }

    /**
     * 首帧vad,开启vad计数窗口期
     *
     * @param i 计数
     */
    public void begin(int i) {
        synchronized (this) {
            setEnableCount(true);
            increment(i);
        }
    }

    /**
     * 窗口期结束，不允许再计数
     */
    public void end() {
        setEnableCount(false);
    }

    /**
     * 计数器自增
     *
     * @param i index
     */
    public void increment(int i) {
        if (enableCount) {
            this.index[count.get()] = i;
            count.incrementAndGet();
        }
    }

    /**
     * 清空计数器
     */
    public void clear() {
        clearIndex();
        count.set(0);
    }

    /**
     * 当前计数器数值
     *
     * @return int
     */
    public int value() {
        return count.get();
    }

    /**
     * 获取被vad触发过的通道
     *
     * @return int[]
     */
    public int[] optIndex() {

        int[] triggeredIndex = new int[count.get()];

        for (int i = 0; i < count.get(); i++) {
            if (index[i] > -1) {
                triggeredIndex[i] = index[i];
            } else {
                Log.e(TAG, "drop illegal index! :" + index[i]);
            }

        }

        return triggeredIndex;
    }


    private void clearIndex() {
        for (int i = 0; i < index.length; i++) {
            index[i] = -1;
        }
    }
}
