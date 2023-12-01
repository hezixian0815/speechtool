package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wuwei on 18-5-11.
 */

public class LocalGrammarConfig extends AIEngineConfig {
    public static final String TAG = "GrammarConfig";
    private String resBinPath;


    /**
     * 设置语法编译资源路径
     * @param resBinPath resBinPath
     */
    public void setResBinPath(String resBinPath) {
        if (TextUtils.isEmpty(resBinPath)) {
            Log.e(TAG, "Invalid ebnfFile");
            return;
        }
        this.resBinPath = resBinPath;
    }

    public String getResBinPath() {
        return resBinPath;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONUtil.putQuietly(jsonObject, "prof", Log.parseDuiliteLog());
            if(!TextUtils.isEmpty(resBinPath))
                jsonObject.put("resBinPath", resBinPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public LocalGrammarConfig clone() throws CloneNotSupportedException {
        return (LocalGrammarConfig) super.clone();
    }
}
