package com.aispeech.common;
/**
 * Created by wuwei on 17-11-28.
 */

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 固定长度队列
 *
 * @author gary
 */
public class LimitQueue<E> implements Queue<E>, Cloneable {
    LinkedList<E> queue = new LinkedList();
    //队列长度
    private int limit;

    public LimitQueue(int limit) {
        this.limit = limit;
    }

    /**
     * 入队
     *
     * @param e 范型参数
     */
    @Override
    public boolean offer(E e) {
        if (queue.size() >= limit) {
            //如果超出长度,入队时,先出队
            queue.poll();
        }
        return queue.offer(e);
    }

    /**
     * 出队
     *
     * @return 返回值
     */
    @Override
    public E poll() {
        return queue.poll();
    }

    /**
     * 获取队列
     *
     * @return 返回值
     */
    public Queue<E> getQueue() {
        return queue;
    }

    /**
     * 获取限制大小
     *
     * @return 返回值
     */
    public int getLimit() {
        return limit;
    }

    /**
     * 获取队尾元素
     *
     * @return
     */
    public E getLast() {
        return queue.getLast();
    }

    /**
     * 获取队头元素
     *
     * @return
     */
    public E getFirst() {
        return queue.getFirst();
    }

    @Override
    public boolean add(E e) {
        return queue.add(e);
    }

    @Override
    public E element() {
        return queue.element();
    }

    @Override
    public E peek() {
        return queue.peek();
    }

    @Override
    public boolean isEmpty() {
        return queue.size() == 0 ? true : false;
    }

    @Override
    public int size() {
        return queue.size();
    }

    @Override
    public E remove() {
        return queue.remove();
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return queue.addAll(c);
    }

    @Override
    public void clear() {
        queue.clear();
    }

    @Override
    public boolean contains(Object o) {
        return queue.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return queue.containsAll(c);
    }

    @Override
    public Iterator<E> iterator() {
        return queue.iterator();
    }

    @Override
    public boolean remove(Object o) {
        return queue.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return queue.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    @Override
    public Object[] toArray() {
        return queue.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return queue.toArray(a);
    }

    @Override
    public LimitQueue clone() {
        try {
            return (LimitQueue) super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }

        return null;
    }
}