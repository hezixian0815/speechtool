package com.aispeech.common;

import com.aispeech.lite.config.LocalTtsConfig;

import org.json.JSONException;
import org.json.JSONObject;

public class TTSDynamicParamUtils {

    /**
     * 动态参数
     *
     * @param backBinPath 资源路径
     * @param language    语言
     * @param localTtsConfig 配置参数
     * @return
     */
    public static String getTtsDynamicParam(String backBinPath, int language, LocalTtsConfig localTtsConfig) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("backBinPath", backBinPath);
            if (localTtsConfig != null) {
                jsonObject.put("optimization", localTtsConfig.isEnableOptimization() ? 1 : 0);
            }
            jsonObject.put("lang", language);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
