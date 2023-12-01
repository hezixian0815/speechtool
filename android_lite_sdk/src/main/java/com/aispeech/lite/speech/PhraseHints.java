package com.aispeech.lite.speech;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class PhraseHints {
    // 只有 vocab 这一种
    private String type = "vocab";
    private String name;
    private String[] data;

    public PhraseHints(String name, String[] data) {
        this.name = name;
        this.data = data;
    }

    public JSONObject toJSON() {
        // 服务器要求的格式 [{"type": "vocab", "name": "词库名", "data":["短语1", "短语2"]}]
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type", type);
            jsonObject.put("name", name);
            if (data != null)
                jsonObject.put("data", new JSONArray(data));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return "PhraseHints{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", data=" + Arrays.toString(data) +
                '}';
    }
}