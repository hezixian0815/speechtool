package com.aispeech.lite.message;


import com.aispeech.common.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;


/**
 * Created by yuruilong on 2017/5/11.
 */

public class MessageQueue<T> {
    private String TAG = "MsgQ";
    private LinkedBlockingQueue<T> mQueue = new LinkedBlockingQueue<>();
    /**
     * 设置队列最大长度，0表示不限制长度。建议大于100
     */
    private int maxMessageQueueSize = 0;
    private int ignoreTimes = 0;

    private int maxVoiceQueueSize = 0;
    private int ignoreSize = 0;
    private int ignoreCount = 0;

    /**
     * 设置队列的最大长度，如果超过则清理队列的音频消息，与上面的 maxMessageQueueSize 相比，这个变量是引擎
     * 粒度的，且超过一定长度后，后续有一个ignoreSize 回去忽略若干个音频队列消息
     */
    public void setMaxVoiceQueueSize(int maxVoiceQueueSize, int ignoreSize) {
        this.maxVoiceQueueSize = maxVoiceQueueSize;
        this.ignoreSize = ignoreSize;
    }

    public void setMaxMessageQueueSize(int maxMessageQueueSize) {
        this.maxMessageQueueSize = maxMessageQueueSize;
        Log.d(TAG, "maxMessageQueueSize " + maxMessageQueueSize);
    }

    public void setTAG(String TAG) {
        this.TAG = "MsgQ-" + TAG;
    }

    /**
     * 将指定元素插入到此队列的尾部，如有必要，则等待空间变得可用。
     *
     * @param message
     */
    public void put(T message) {
        try {
            if (ignoreCount > 0) {
                ignoreCount--;
                return;
            }

            if (maxVoiceQueueSize > 0 && mQueue.size() >= maxVoiceQueueSize) {
                remove(new Remover<T>() {
                    @Override
                    public boolean removeCondition(T t) {
                        if (t instanceof Message) {
                            return ((Message) t).mId == Message.MSG_FEED_DATA_BY_STREAM;
                        }
                        return false;
                    }
                });
                ignoreCount = ignoreSize;
                Log.d(TAG, "trigger clear voice msg,MessageQueue size after clear:" + mQueue.size());
                return;
            }

            if (maxMessageQueueSize > 0 && mQueue.size() >= maxMessageQueueSize) {
                mQueue.take();
                if (ignoreTimes % 10 == 0) {
                    Log.d(TAG, "ignoreTimes " + ignoreTimes);
                }
                ignoreTimes++;
            }
            mQueue.put(message);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public T get() {
        try {
            return mQueue.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void clear() {
        mQueue.clear();
    }

    public void remove(T message) {
        mQueue.remove(message);
    }

    public void remove(Remover<T> removable) {
        Iterator<T> it = mQueue.iterator();
        List<T> tempList = new ArrayList<>();
        while (it.hasNext()) {
            T entry = it.next();
            if (removable.removeCondition(entry)) {
                tempList.add(entry);
            }
        }
        for (T item : tempList) {
            mQueue.remove(item);
        }
    }

    public int size() {
        return mQueue.size();
    }

    public boolean isEmpty() {
        return mQueue.isEmpty();
    }

    public interface Remover<T> {
        /**
         * @return true 删除该元素, false 不删除
         */
        boolean removeCondition(T t);
    }

}
