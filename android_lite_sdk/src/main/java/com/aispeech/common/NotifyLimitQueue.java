package com.aispeech.common;


import java.util.LinkedList;
import java.util.Queue;

/**
 * 带回调的定长队列
 *
 * @param <E> 指定范形
 * @author hehr
 */
public class NotifyLimitQueue<E> {

    private Queue<E> mQueue;

    private Listener mListener;

    private int limit;

    public NotifyLimitQueue(final int limit, Listener<E> listener) {
        this.limit = limit;
        this.mQueue = new LinkedList<>();
        this.mListener = listener;
    }

    /**
     * 入队
     *
     * @param obj 入队参数
     * @return boolean
     */
    public boolean offer(E obj) {

        if (mQueue.size() == limit) {
            if (mListener != null) {
                mListener.onPop(mQueue.poll());
            }
        }

        return mQueue.offer(obj);
    }

    /**
     * 队列容量
     *
     * @return 队列容量
     */
    public int size() {
        return mQueue == null ? 0 : mQueue.size();
    }

    /**
     * 清空队列内容
     */
    public void clear() {
        if (mQueue != null)
            mQueue.clear();
    }

    /**
     * 队列满对调
     *
     * @param <E> 范形
     */
    public interface Listener<E> {
        /**
         * 队列满回调
         *
         * @param e 范形
         */
        void onPop(E e);
    }


}
