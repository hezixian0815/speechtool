package com.aispeech.export;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 语义slots
 *
 * @author hehr
 */
public class Slots {

    private JSONArray array = new JSONArray();

    private Slots() {
    }

    public static Slots createInstance() {
        return new Slots();
    }

    /**
     * add slot
     *
     * @param slot {@link JSONObject}
     * @return {@link Slots}
     */
    public Slots add(JSONObject slot) {
        array.put(slot);
        return this;
    }

    public boolean isEmpty() {
        return array == null || array.length() == 0;
    }

    public String toJSON() {
        return array.toString();

    }
}
