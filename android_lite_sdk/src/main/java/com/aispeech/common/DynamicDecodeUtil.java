package com.aispeech.common;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.regex.Pattern;

public class DynamicDecodeUtil {
    private static final String TAG = "DynalmicDecodeUtil";

    private static final String DYNAMIC_ITEM_HEAD = "$dynamic";
    private static final String DYNAMIC_ALL_HEAD = "$dynamic_all =";
    private static final String DYNAMIC_ALL_END = "( \\<s\\> ($dynamic_all) \\<\\/s\\> )\r\n";
    private static final String DYNAMIC_ITEM_END = ";\r\n";
    private static final Pattern englishPattern = Pattern.compile("[a-zA-Z]");
    private static final Pattern pattern = Pattern.compile("[^a-zA-Z0-9\\u4e00-\\u9fa5]");//仅保留中文，英文，数字

    public static String jsonToXbnfStr(String jsonStr) {
        StringBuilder resultSb = new StringBuilder();
        try {
            Log.d(TAG, "input = " + jsonStr);
            JSONArray inputArr = new JSONArray(jsonStr);
            int dynamicSize = inputArr.length();
            for (int i = 0; i < dynamicSize; i++) {
                StringBuilder itemSb = new StringBuilder();
                JSONObject itemObj = inputArr.getJSONObject(i);
                JSONArray segmentArr = itemObj.optJSONArray("segment");
                String originStr = itemObj.optString("origin");
                originStr = originStr.replaceAll(" ", "");//语义去掉空格
                StringBuilder textSb = null;
                if (segmentArr != null && segmentArr.length() > 0) {
                    textSb = new StringBuilder();
                    textSb.append(addBrackets(originStr));
                    for (int j = 0; j < segmentArr.length(); j++) {
                        String itemSegment = segmentArr.getString(j);
                        textSb.append("|").append(addBrackets(itemSegment));
                    }
                    if (hasEnglish(originStr)) {
                        textSb.append("|").append(originStr);
                        for (int j = 0; j < segmentArr.length(); j++) {
                            String itemSegment = segmentArr.getString(j);
                            textSb.append("|").append(itemSegment);
                        }
                    }
                }

                String textStr = textSb != null ? textSb.toString() : addBrackets(originStr);
                if (hasEnglish(originStr)) {
                    textStr = textStr + "|" + originStr;
                }
                itemSb.append(DYNAMIC_ITEM_HEAD).append((i + 1)).append("=(").append(textStr).append(")");
                itemSb.append("/\"origin\"=\"").append(originStr).append("\"");

                Iterator<String> iterator = itemObj.keys();
                while (iterator.hasNext()) {
                    String key = iterator.next();
                    if (!TextUtils.equals(key, "origin") && !TextUtils.equals(key, "segment") && !TextUtils.isEmpty(key)) {
                        Object value = itemObj.opt(key);
                        itemSb.append(",");
                        itemSb.append("\"").append(key).append("\"=\"").append(value).append("\"");
                    }
                }
                itemSb.append("/");
                itemSb.append(DYNAMIC_ITEM_END);

                resultSb.append(itemSb.toString());
            }

            resultSb.append(DYNAMIC_ALL_HEAD);
            for (int i = 0; i < dynamicSize; i++) {
                if (i > 0) {
                    resultSb.append("|");
                }
                resultSb.append(DYNAMIC_ITEM_HEAD).append((i + 1));
            }
            resultSb.append(DYNAMIC_ITEM_END);
            resultSb.append(DYNAMIC_ALL_END);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultSb.toString();
    }

    /**
     * xbnf语法中对每个英文添加括号(或者空格)
     *
     * @param text
     * @return
     */
    public static String addBrackets(String text) {
        String result = pattern.matcher(text).replaceAll("");
        StringBuilder sb = new StringBuilder();
        String[] spilt = result.split("");
        boolean isFirst = true;
        for (int i = 0; i < spilt.length; i++) {
            String item = spilt[i];
            if (englishPattern.matcher(item).find()) {
                sb.append((isFirst ? "" : " ") + item);
                isFirst = false;
            } else {
                sb.append(item);
            }
        }
        return sb.toString();
    }

    public static boolean hasEnglish(String text) {
        String result = pattern.matcher(text).replaceAll("");
        String[] spilt = result.split("");
        for (String item : spilt) {
            if (englishPattern.matcher(item).find()) {
                return true;
            }
        }
        return false;
    }
}
