package com.aispeech.common;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 识别结果解析器
 */
public class JSONResultParser {

    JSONObject jso;

    String var = "";
    String text = "";
    String allText = "";
    JSONArray speakerLabels = null;
    String sessionId = "";
    String recordId = "";
    String error = "";
    String pinyin = "";
    int errId = -1;
    int eof = 1;

    /**
     * @param jsonString 传入的JSON格式的字串
     */
    public JSONResultParser(String jsonString) {
        try {
            jso = new JSONObject(jsonString);
            // handle common elements
            sessionId = jso.optString("sessionId");
            recordId = jso.optString("recordId");
            JSONObject res = jso.optJSONObject("result");
            eof = jso.optInt("eof", 0);
            if (res != null && res.has("rec")) {
                text = JSONUtil.replaceSpace(res.optString("rec"));
            }
            if (res != null && res.has("var")) {
                var = JSONUtil.replaceSpace(res.optString("var"));
            }
            if (res != null && res.has("pinyin")) {
                pinyin = res.optString("pinyin");
            }
            if (res != null && res.has("allText")) {
                allText = res.optString("allText");
            }
            if (res != null && res.has("speakerLabels")) {
                speakerLabels = res.optJSONArray("speakerLabels");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public boolean haveVprintInfo() {
        return speakerLabels != null;
    }

    /**
     * 获取JSON对象
     *
     * @return JSON对象
     */
    public JSONObject getJSON() {
        return jso;
    }


    /**
     * 取得已确定不变的识别结果(用于实时反馈)
     *
     * @return null 如果不存在该字段
     */
    public String getVar() {
        return var == null ? "" : var;
    }


    /**
     * 获得是否终止位
     *
     * @return -1 如果不存在该字段
     */
    public int getEof() {
        return eof;
    }

    /**
     * 取得sessionId
     *
     * @return "" 如果不存在该字段
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 取得recordId
     *
     * @return "" 如果不存在该字段
     */
    public String getRecordId() {
        return recordId;
    }


    /**
     * 取得错误码
     *
     * @return -1 如果不存在该字段
     */
    public int getErrId() {
        return errId;
    }

    /**
     * 取得错误描述
     *
     * @return "" 如果不存在该字段
     */
    public String getError() {
        return error;
    }


    /**
     * 获取text字段
     *
     * @return null 如果没有改字段
     */
    public String getText() {
        return text == null ? "" : text;
    }

    public String getPinyin() {
        return pinyin;
    }


    public String toString() {
        return jso != null ? jso.toString() : "";
    }

    /**
     * 语音结束时rec为空，把上一次的结果放进来
     *
     * @param rec    asr识别出的文字
     * @param pinyin asr识别出的文字对应的拼音
     */
    public void setRecPinyinWhenLast(String rec, String pinyin) {
        if ((1 == eof || 2 == eof) && !TextUtils.isEmpty(rec) && TextUtils.isEmpty(text)
                && jso != null && jso.optJSONObject("result") != null) {
            try {
                JSONObject result = jso.getJSONObject("result");
                result.put("rec", rec);
                result.put("pinyin", pinyin);
                this.text = rec;
                this.pinyin = pinyin;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getAllText() {
        return allText == null ? "" : allText;
    }

    public void setAllText(String allText) {
        if (allText == null)
            this.allText = "";
        else
            this.allText = allText;
        if (jso != null && jso.optJSONObject("result") != null) {
            try {
                jso.getJSONObject("result").put("allText", this.allText);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getAllTextAndCurrentVar() {
        return (allText == null ? "" : allText) + (var == null ? "" : var);
    }
}
