package com.aispeech.lite.config;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalAecAgcConfig extends AIEngineConfig {

    public static final String KEY_AEC_BIN_PATH = "aecBinPath";
    public static final String KEY_AGC_BIN_PATH = "agcBinPath";

    private String aecBinPath = "";
    private String agcBinPath = "";


    public String getAecBinPath() {
        return aecBinPath;
    }

    public void setAecBinPath(String aecBinPath) {
        this.aecBinPath = aecBinPath;
    }

    public String getAgcBinPath() {
        return agcBinPath;
    }

    public void setAgcBinPath(String agcBinPath) {
        this.agcBinPath = agcBinPath;
    }

    public LocalAecAgcConfig() {
    }

    public LocalAecAgcConfig(String aecBinPath, String agcBinPath) {
        this.aecBinPath = aecBinPath;
        this.agcBinPath = agcBinPath;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_AEC_BIN_PATH, aecBinPath);
            jsonObject.put(KEY_AGC_BIN_PATH, agcBinPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return "LocalAecAgcConfig{" +
                "aecBinPath='" + aecBinPath + '\'' +
                ", agcBinPath='" + agcBinPath + '\'' +
                '}';
    }
}
