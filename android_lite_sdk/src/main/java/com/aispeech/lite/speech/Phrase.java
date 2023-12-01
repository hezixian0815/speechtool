package com.aispeech.lite.speech;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class Phrase {
    //名称
    private String name = "common";
    //词组集合
    private String[] words;
    //增强指数
    private int boost = 2;

    public Phrase(String[] words) {
        this.words = words;
    }

    public Phrase(String name, String[] words) {
        this.name = name;
        this.words = words;
    }

    public Phrase(String name, String[] words, int boost) {
        this.name = name;
        this.words = words;
        this.boost = boost;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String[] getWords() {
        return words;
    }

    public void setWords(String[] words) {
        this.words = words;
    }

    public int getBoost() {
        return boost;
    }

    public void setBoost(int boost) {
        this.boost = boost;
    }

    public JSONObject toJSON() {
        // 服务器要求的格式 [{ "name" : "common"， "words"：["亿联"，"点学"]， "boost" ：2 }]
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("name", name);
            if (words != null)
                jsonObject.put("words", new JSONArray(words));
            jsonObject.put("boost", boost);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return "Phrase{" +
                "name='" + name + '\'' +
                ", words=" + Arrays.toString(words) +
                ", boost=" + boost +
                '}';
    }
}
