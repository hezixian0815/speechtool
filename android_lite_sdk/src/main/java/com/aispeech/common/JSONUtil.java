/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.common;

import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JSON put/get utils
 */
public class JSONUtil {

    /**
     * build json with not JSONException throws
     *
     * @param str
     */
    public static JSONObject build(String str) {

        JSONObject jo = null;

        try {
            jo = new JSONObject(str);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jo;


    }

    /**
     * Put key/value to JSONObject with no JSONException throws
     *
     * @param jo    JSONObject
     * @param key   key
     * @param value value
     */
    public static void putQuietly(JSONObject jo, String key, Object value) {
        try {
            jo.put(key, value);
        } catch (JSONException je) {
            je.printStackTrace();
        }
    }

    /**
     * remove key from JSONObject
     *
     * @param jo
     * @param key
     */
    public static void removeQuietly(JSONObject jo, String key) {
        try {
            jo.remove(key);
        } catch (Exception je) {
            je.printStackTrace();
        }
    }

    /**
     * Put Map entries to JSONObject with no JSONException throws
     *
     * @param jo  JSONObject
     * @param map key/value map
     */
    public static void putQuietly(JSONObject jo, Map<String, Object> map) {
        if (map == null)
            return;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            putQuietly(jo, e.getKey(), e.getValue());
        }
    }

    public static void putQuietly(JSONObject jo, String key, int []list) {
        if (list == null)
            return;
        try {
            JSONArray jsonArray = new JSONArray();
            for (int data : list) {
                jsonArray.put(data);
            }
            jo.put(key,jsonArray);
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * Get value from JSONObject
     *
     * @param jo  JSONObject
     * @param key key
     * @return value of key
     */
    public static Object getQuietly(JSONObject jo, String key) {
        try {
            return jo.get(key);
        } catch (JSONException je) {
            je.printStackTrace();
            return null;
        }
    }

    /**
     * opt value from JSONObject
     *
     * @param jo  JSONObject
     * @param key key
     * @return value of key
     */
    public static Object optQuietly(JSONObject jo, String key) {
        return jo.opt(key);
    }

    /**
     * Return a new JSONObject and put Bundle key/values
     *
     * @param bundle bundle
     * @return JSONObject
     */
    public static JSONObject bundleToJSON(Bundle bundle) {
        JSONObject json = new JSONObject();
        for (Iterator<String> it = bundle.keySet().iterator(); it.hasNext(); ) {
            String key = it.next();
            putQuietly(json, key, bundle.get(key));
        }
        return json;
    }

    /**
     * Return a new JSONObject and put a pair key/value
     *
     * @param key
     * @param value
     * @return JSONObject
     */
    public static JSONObject pairToJSON(String key, Object value) {
        JSONObject jo = new JSONObject();
        putQuietly(jo, key, value);
        return jo;
    }

    /**
     * Return a new JSONObject and put map key/values
     *
     * @param map
     * @return JSONObject
     */
    public static JSONObject mapToJSON(Map<String, Object> map) {
        JSONObject jo = new JSONObject();
        putQuietly(jo, map);
        return jo;
    }

    public static String decode(String str) {
        String s = str;
        s = s.replaceAll("\\\\\"", "\"");
        s = s.replaceAll("\\\\", "");
        s = s.replaceAll("\"\\{", "{");
        s = s.replaceAll("\\}\"", "}");
        return s;
    }

    /**
     * 去除中文之间的空格，但是保留英文之间的空格
     * @return 返回无空格中文与带空格的英语
     */
    public static String replaceSpace(String str) {
        if (!TextUtils.isEmpty(str)) {
            return str.replaceAll("\\s*([^A-Za-z0-9])\\s*", "$1");
        }
        return str;
    }

    /**
     * 按照标点来分割句子，并且自带标点
     * @param regEx 标点
     * @param str 需要分割的句子
     * @return 返回按照标点分开的句子
     */
    public static String[] splitWords(String str, String regEx) {
        /*正则表达式：句子结束符*/
        Pattern p =Pattern.compile(regEx);
        Matcher m = p.matcher(str);
        /*按照句子结束符分割句子*/
        String[] words = p.split(str);
        /*将句子结束符连接到相应的句子后*/
        if(words.length > 0)
        {
            int count = 0;
            while(count < words.length)
            {
                if(m.find())
                {
                    words[count] += m.group();
                }
                count++;
            }
        }
        /*输出结果*/
        return words;
    }

    /**
     * 处理云端语义槽格式不统一的问题：https://jira.aispeech.com.cn/browse/YJYB-6030
     *
     * @param nluObj 预转化的语义对象
     */
    public static JSONObject normalSemanticSlots(JSONObject nluObj) {
        if (nluObj != null && nluObj.has("semantics")) {
            JSONObject semanticsObj = nluObj.optJSONObject("semantics");
            if (semanticsObj != null && semanticsObj.has("request")) {
                JSONObject requestObj = semanticsObj.optJSONObject("request");
                if (requestObj != null && requestObj.has("slots")) {
                    try {
                        Object slotsObj = requestObj.get("slots");
                        if (slotsObj instanceof JSONObject) {
                            requestObj.remove("slots");
                            requestObj.put("slots", new JSONArray());
                            semanticsObj.put("request", requestObj);
                            nluObj.put("semantics", semanticsObj);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }
            }
        }
        return nluObj;
    }
}
