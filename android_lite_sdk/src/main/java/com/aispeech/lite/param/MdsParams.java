package com.aispeech.lite.param;

import org.json.JSONException;
import org.json.JSONObject;

public class MdsParams extends SpeechParams {

    /**
     * 说话人角度（多通道输入时使用），可选，根据资源配置而定
     */
    private int doa = -1;

    public int getDoa() {
        return doa;
    }

    public void setDoa(int doa) {
        this.doa = doa;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (doa >= 0)
                jsonObject.put("doa", doa);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return "MdsParams{" +
                ", doa=" + doa +
                '}';
    }
}
