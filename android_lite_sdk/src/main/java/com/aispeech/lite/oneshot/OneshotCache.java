package com.aispeech.lite.oneshot;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * 定长的环形队列
 *
 * @param <T> 泛型
 */
public class OneshotCache<T> extends LinkedList<T> implements Cloneable {

    /**
     * 队列长度限制
     */
    private int limit;

    public OneshotCache(int size) {
        this.limit = size;
    }

    @Override
    public boolean offer(T o) {

        if (super.size() > limit) {
            remove();
        }

        return super.offer(o);

    }

    @Override
    public T poll() {
        return super.poll();
    }

    public boolean isValid() {
        return size() != 0;
    }


    @Override
    public OneshotIterator iterator() {
        return new OneshotIterator();
    }

    public class OneshotIterator implements Iterator<T> {

        @Override
        public boolean hasNext() {
            return size() != 0;
        }

        @Override
        public T next() {

            if (!hasNext()) {
                return null;
            }
            return poll();
        }
    }


    @Override
    public OneshotCache clone() {
        return (OneshotCache) super.clone();
    }
}
