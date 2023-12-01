package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wuwei on 2018/7/11.
 */

public class LocalAsrppConfig extends AIEngineConfig {
    private String resBinPath;

    public String getResBinPath() {
        return resBinPath;
    }

    public void setResBinPath(String resBinPath) {
        this.resBinPath = resBinPath;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("prof", Log.parseDuiliteLog());
            if (!TextUtils.isEmpty(resBinPath))
                jsonObject.put("resBinPath", resBinPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }


    @Override
    public LocalAsrppConfig clone() throws CloneNotSupportedException {
        return (LocalAsrppConfig) super.clone();
    }
}
