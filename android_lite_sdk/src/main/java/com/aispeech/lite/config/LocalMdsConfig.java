package com.aispeech.lite.config;

import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalMdsConfig extends AIEngineConfig {

    /**
     * 资源文件
     */
    private String resBinPath;

    /**
     * 输入音频通道数
     */
    private int channels;

    public String getResBinPath() {
        return resBinPath;
    }

    public void setResBinPath(String resBinPath) {
        this.resBinPath = resBinPath;
    }

    public int getChannels() {
        return channels;
    }

    public void setChannels(int channels) {
        this.channels = channels;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONUtil.putQuietly(jsonObject, "prof", Log.parseDuiliteLog());
            jsonObject.put("resBinPath", resBinPath);
            jsonObject.put("channels", channels);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return "LocalMdsConfig{" +
                "resBinPath='" + resBinPath + '\'' +
                ", channels=" + channels +
                '}';
    }
}
