package com.aispeech.common;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wuwei on 2018/7/16.
 */

public class VprintUtils {

    /**
     * 获取已经注册人列表
     * @param jsonArray  jsonArray
     * @return list
     */
    public static List<String> getRegisterList(JSONArray jsonArray) {
        List<String> list = new ArrayList<>();
        if (jsonArray == null) {
            return list;
        }
        try {
            if (jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    list.add(jsonObject.optString("name"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }


    /**
     {"registers":
     [
        {
            "name":"dss",
            "wordlist":[
                {
                    "gender":"male",
                    "word":"xiao ou xiao ou",
                    "thresh":-17
                }
            ]
        }
     ]
     }
     */

    /**
     * 获取指定注册人注册的唤醒词列表
     * @param jsonArray jsonArray
     * @param registerName registerName
     * @return list
     */
    public static List<String> getWordList(JSONArray jsonArray, String registerName) {
        List<String> wordList = new ArrayList<>();
        if (jsonArray == null || TextUtils.isEmpty(registerName)) {
            return wordList;
        }
        JSONArray wordListJsonArray = null;
        try {
            if (jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    if (TextUtils.equals(registerName, jsonObject.optString("name"))) {
                        wordListJsonArray = jsonObject.getJSONArray("wordlist");
                        break;
                    }
                }
                if (wordListJsonArray != null && wordListJsonArray.length() > 0) {
                    for (int i = 0; i < wordListJsonArray.length(); i++) {
                        JSONObject jsonObject = wordListJsonArray.getJSONObject(i);
                        wordList.add(jsonObject.optString("word"));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return wordList;
    }
}
