package com.aispeech.lite.dm.cache;

/**
 * pending task item
 * @author hehr
 */
public class Item {

    public Item(String key, Object value) {
        this.key = key;
        this.value = value;
    }

    private String key;

    private Object value;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }
}
