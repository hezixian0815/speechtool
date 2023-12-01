package com.aispeech.lite.dm.cache;

public interface Iterator {
    /**
     * if has next
     *
     * @return boolean
     */
    boolean hasNext();

    /**
     * Iterate the mQueue
     *
     * @return Item
     */
    Item next();

}
