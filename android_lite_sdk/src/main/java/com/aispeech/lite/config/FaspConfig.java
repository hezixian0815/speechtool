package com.aispeech.lite.config;

import org.json.JSONException;
import org.json.JSONObject;

public class FaspConfig extends AIEngineConfig {
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
            jsonObject.put("resBinPath", resBinPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return "FaspConfig{" +
                "resBinPath='" + resBinPath + '\'' +
                '}';
    }
}
