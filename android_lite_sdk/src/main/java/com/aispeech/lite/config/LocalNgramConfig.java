package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wuwei on 18-5-11.
 */

public class LocalNgramConfig extends AIEngineConfig {
    public static final String TAG = "LocalNgramConfig";
    private String resBinPath;


    /**
     * 设置语法编译资源路径
     * @param resBinPath resBinPath
     */
    public void setResBinPath(String resBinPath) {
        if (TextUtils.isEmpty(resBinPath)) {
            Log.e(TAG, "Invalid resBinPath");
            return;
        }
        this.resBinPath = resBinPath;
    }

    public String getResBinPath() {
        return resBinPath;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        try {
            if(!TextUtils.isEmpty(resBinPath))
                jsonObject.put("resBinPath", resBinPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public LocalNgramConfig clone() throws CloneNotSupportedException {
        return (LocalNgramConfig) super.clone();
    }
}
