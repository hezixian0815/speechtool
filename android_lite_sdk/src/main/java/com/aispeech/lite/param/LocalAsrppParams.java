package com.aispeech.lite.param;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalAsrppParams extends SpeechParams {

    /**
     * 音量检测
     */
    private boolean volumeCheck = false;
    /**
     * 外部直接设置env
     */
    public String env;

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public boolean isVolumeCheck() {
        return volumeCheck;
    }

    /**
     * 音量检测
     *
     * @param volumeCheck 默认是false
     */
    public void setVolumeCheck(boolean volumeCheck) {
        this.volumeCheck = volumeCheck;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (TextUtils.isEmpty(env)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("volumeCheck=" + (volumeCheck ? 1 : 0) + ";");
                jsonObject.put("env", stringBuilder.toString());
            } else {
                jsonObject.put("env", env);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
