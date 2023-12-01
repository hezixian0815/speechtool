package com.aispeech.lite.config;

import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalSevcConfig extends AIEngineConfig {

    private static final String SSPE_BIN_PATH = "sspeBinPath";

    public LocalSevcConfig() {
    }

    public LocalSevcConfig(String sspeBinPath) {
        this.sspeBinPath = sspeBinPath;
    }

    private String sspeBinPath;

    public String getSspeBinPath() {
        return sspeBinPath;
    }

    public void setSspeBinPath(String sspeBinPath) {
        this.sspeBinPath = sspeBinPath;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.putQuietly(jsonObject, "prof", Log.parseDuiliteLog());
        try {
            jsonObject.put(SSPE_BIN_PATH, sspeBinPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}
