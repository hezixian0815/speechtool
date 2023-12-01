package com.aispeech.lite.hotword.ssl.n;

import com.aispeech.common.LimitQueue;
import com.aispeech.common.Log;
import com.aispeech.common.Util;

/**
 * n rms封装
 *
 * @author hehr
 */
public class NRmsEngine implements INRms {

    private static final String TAG = "NRmsEngine";

    private LimitQueue<Float> dmsQueue4Vad0;
    private LimitQueue<Float> dmsQueue4Vad1;
    private LimitQueue<Float> dmsQueue4Vad2;
    private LimitQueue<Float> dmsQueue4Vad3;

    @Override
    public void init(int frameNum) {
        Log.d(TAG, "dms queue frame number : " + frameNum);
        if (dmsQueue4Vad0 == null)
            dmsQueue4Vad0 = new LimitQueue<>(frameNum);
        if (dmsQueue4Vad1 == null)
            dmsQueue4Vad1 = new LimitQueue<>(frameNum);
        if (dmsQueue4Vad2 == null)
            dmsQueue4Vad2 = new LimitQueue<>(frameNum);
        if (dmsQueue4Vad3 == null)
            dmsQueue4Vad3 = new LimitQueue<>(frameNum);
    }

    @Override
    public void start() {
        //清空dms队列
        if (!dmsQueue4Vad0.isEmpty()) {
            dmsQueue4Vad0.clear();
        }
        if (!dmsQueue4Vad1.isEmpty()) {
            dmsQueue4Vad1.clear();
        }
        if (!dmsQueue4Vad2.isEmpty()) {
            dmsQueue4Vad2.clear();
        }
        if (!dmsQueue4Vad3.isEmpty()) {
            dmsQueue4Vad3.clear();
        }
    }

    @Override
    public void feed(byte[] chanel0, byte[] chanel1, byte[] chanel2, byte[] chanel3) {
        if (dmsQueue4Vad0 != null)
            dmsQueue4Vad0.offer((float) Util.calcVolume(Util.toShortArray(chanel0)));
        if (dmsQueue4Vad1 != null)
            dmsQueue4Vad1.offer((float) Util.calcVolume(Util.toShortArray(chanel1)));
        if (dmsQueue4Vad2 != null)
            dmsQueue4Vad2.offer((float) Util.calcVolume(Util.toShortArray(chanel2)));
        if (dmsQueue4Vad3 != null)
            dmsQueue4Vad3.offer((float) Util.calcVolume(Util.toShortArray(chanel3)));
    }

    @Override
    public void stop() {
        if (dmsQueue4Vad0 != null)
            dmsQueue4Vad0.clear();
        if (dmsQueue4Vad1 != null)
            dmsQueue4Vad1.clear();
        if (dmsQueue4Vad2 != null)
            dmsQueue4Vad2.clear();
        if (dmsQueue4Vad3 != null)
            dmsQueue4Vad3.clear();
    }

    @Override
    public void release() {

        if (dmsQueue4Vad0 != null) {
            dmsQueue4Vad0.clear();
            dmsQueue4Vad0 = null;
        }

        if (dmsQueue4Vad1 != null) {
            dmsQueue4Vad1.clear();
            dmsQueue4Vad1 = null;
        }

        if (dmsQueue4Vad2 != null) {
            dmsQueue4Vad2.clear();
            dmsQueue4Vad2 = null;
        }

        if (dmsQueue4Vad3 != null) {
            dmsQueue4Vad3.clear();
            dmsQueue4Vad3 = null;
        }
    }

    /**
     * 获取指定通道的dmsQueue
     *
     * @param index 音频通道
     * @return
     */
    private LimitQueue<Float> getQueue(int index) {
        switch (index) {
            case 0:
                return dmsQueue4Vad0;
            case 1:
                return dmsQueue4Vad1;
            case 2:
                return dmsQueue4Vad2;
            case 3:
                return dmsQueue4Vad3;
            default:
                return null;
        }
    }

    /**
     * 计算均值
     *
     * @param queue 队列
     * @return float
     */
    private float calculateAverage(LimitQueue<Float> queue) {


        if (queue == null || queue.isEmpty()) {
            return 0;
        }

        LimitQueue<Float> copyQueue = queue.clone();//拷贝当前时刻的vad队列

        float total = 0;

        int size = copyQueue.size();

        while (!copyQueue.isEmpty()) {
            total = total + copyQueue.poll();
        }

        return total / size;
    }

    @Override
    public float optDms(int index) {
        return calculateAverage(getQueue(index));
    }
}
