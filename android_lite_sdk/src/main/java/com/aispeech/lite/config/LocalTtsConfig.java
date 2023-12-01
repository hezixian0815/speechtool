package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yu on 2018/5/7.
 */

public class LocalTtsConfig extends AIEngineConfig {
    private static String KEY_FRONT_BIN_PATH = "frontBinPath";
    private static String KEY_BACK_BIN_PATH = "backBinPath";
    private static String KEY_DIC_BIN_PATH = "dictPath";
    private static String KEY_OPTIMIZATION = "optimization";
    private static String KEY_LANGUAGE = "lang";
    private String frontBinPath;//前端资源路径，包含文本归一化，分词的，韵律等，必选
    private String backBinPath;//后端资源路径，包含发音人音色等，必选
    private String dictPath;//合成字典路径，必选
    private boolean enableOptimization = true;//优化字段，无该字段或者该字段值为非0表示开启优化，值为0表示关闭优化。默认开启优化
    /**
     * 方言选项，用于支持粤语、上海话、四川话等音色，默认为 0，可选。
     * 4 为选择粤语发
     */
    private int language = 0;
    /**
     * 用户自定义词典，用于修复离线合成问题，如多音字发音、停顿和数字字母符号读法错误等
     * <p>非必需</p>
     */
    private String userDict = null;

    public String getFrontBinPath() {
        return frontBinPath;
    }

    public void setFrontBinPath(String frontBinPath) {
        this.frontBinPath = frontBinPath;
    }

    public String getBackBinPath() {
        return backBinPath;
    }

    public void setBackBinPath(String backBinPath) {
        this.backBinPath = backBinPath;
    }

    public String getUserDict() {
        return userDict;
    }

    public void setUserDict(String userDict) {
        this.userDict = userDict;
    }

    public void setDictPath(String dictPath) {
        this.dictPath = dictPath;
    }

    public String getDictPath() {
        return dictPath;
    }

    public boolean isEnableOptimization() {
        return enableOptimization;
    }

    public void setEnableOptimization(boolean enableOptimization) {
        this.enableOptimization = enableOptimization;
    }

    public int getLanguage() {
        return language;
    }


    /**
     * 方言选项，用于支持粤语、上海话、四川话等音色，默认为 0，可选。
     *
     * @param language  4 为选择粤语
     */
    public void setLanguage(int language) {
        this.language = language;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("prof", Log.parseDuiliteLog());
            if (!TextUtils.isEmpty(frontBinPath))
                jsonObject.put(KEY_FRONT_BIN_PATH, frontBinPath);
            if (!TextUtils.isEmpty(backBinPath))
                jsonObject.put(KEY_BACK_BIN_PATH, backBinPath);
            if (!TextUtils.isEmpty(dictPath))
                jsonObject.put(KEY_DIC_BIN_PATH, dictPath);
            jsonObject.put(KEY_OPTIMIZATION, enableOptimization ? 1 : 0);
            if (!TextUtils.isEmpty(userDict))
                jsonObject.put("userDict", userDict);
            jsonObject.put(KEY_LANGUAGE,language);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public LocalTtsConfig clone() throws CloneNotSupportedException {
        return (LocalTtsConfig) super.clone();
    }
}
