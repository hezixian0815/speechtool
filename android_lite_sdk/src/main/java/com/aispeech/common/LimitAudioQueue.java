package com.aispeech.common;

import java.util.Arrays;

/**
 * 一个缓存byte[]的数据结构，特点如下：
 * 1.data容器保证加入数据和取出数据的顺序一致；
 * 2.有最大容量限制，空余容量不足时塞入数据，最早加入的数据将被移除；
 * 3.详见容器数据变化过程便于理解；
 * <p>
 * 演示 data 塞入数据，data容器的数据变化过程
 * <p>
 * -----------------------↓
 * A B C D E F
 * -----------------------↓
 * A B C D E F G H I J
 * -----------------------↓
 * A B C D E F G H I J K L
 * -----------------------↓
 * B C D E F G H I J K L M
 * -----------------------↓
 * C D E F G H I J K L M N
 * -----------------------↓
 */
public class LimitAudioQueue {

    private int maxLen;//根据公式计算最大数据长度

    private int curLen;//当前容器中有效数据的长度
    private byte[] data;//数据容器

    private LimitAudioQueue(int maxLen) {
        this.maxLen = maxLen;
        data = new byte[maxLen];
        curLen = 0;
    }

    /**
     * @param duration 音频时长，单位ms
     * @param channels 通道数
     */
    public LimitAudioQueue(int duration, int channels) {
        //计算方式：音频大小(字节) = 采样率（16000）* 通道数 * 位深/ 每字节位数 / 1000 * 毫秒数 = 16000 * channels * 16/8 /1000 * duration
        this(16000 * channels * 16 / 8 / 1000 * duration);
    }


    /**
     * 塞入数据
     * @param offerData
     */
    public synchronized void offer(byte[] offerData) {
        if (offerData.length >= maxLen) {//offerData的长度>=maxLen,就把offerData多出的部分舍弃，剩下的数据拷贝到data中。
            System.arraycopy(offerData, offerData.length - maxLen, data, 0, data.length);
            curLen = maxLen;
        } else {
            int targetLen = offerData.length + curLen;
            if (targetLen <= maxLen) {//data的数据加上offerData的数据后总长度<=maxLen ,就可以直接把offerData塞入data中，不需要考虑移动位置的情况
                System.arraycopy(offerData, 0, data, curLen, offerData.length);
                curLen = targetLen;
            } else {//先把多出的数据从data中移除，然后把data塞到data中
                int cutLen = targetLen - maxLen;//要被移除的数据量
                System.arraycopy(data, cutLen, data, 0, curLen - cutLen);
                System.arraycopy(offerData, 0, data, curLen - cutLen, offerData.length);
                curLen = maxLen;
            }
        }
    }

    /**
     * 取出缓存的byte[]，
     * 会保证和存入时的顺序一致。
     *
     * @return
     */
    public synchronized byte[] toArray() {
        byte[] result = Arrays.copyOfRange(data, 0, curLen);
        return result;
    }

    /**
     * 重置长度标注位
     */
    public synchronized void clear() {
        curLen = 0;
    }
}
