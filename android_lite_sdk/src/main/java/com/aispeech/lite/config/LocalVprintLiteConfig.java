package com.aispeech.lite.config;

import static com.aispeech.common.AIConstant.VPRINTLITE_TYPE_ANTI_SPOOFING;
import static com.aispeech.common.AIConstant.VPRINTLITE_TYPE_TD;

import android.text.TextUtils;

import com.aispeech.common.AIConstant;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wuwei on 2018/7/11.
 */

public class LocalVprintLiteConfig extends AIEngineConfig {
    private static final String KEY_VPRINT_PATH = "configPath";
    private static final String KEY_VPRINT_TYPE = "type";
    private static final String KEY_MODEL_PATH = "modelPath";

    private String vprintResBin;
    private String vprintModelFile;
    private String vprintType = "";

    public String getVprintType() {
        return vprintType;
    }

    /**
     * 设置vprint类型 {@link AIConstant#VPRINTLITE_TYPE_TD}
     * {@link AIConstant#VPRINTLITE_TYPE_SR}
     * {@link AIConstant#VPRINTLITE_TYPE_ANTI_SPOOFING}
     *
     * @param vprintType vprintLite类型
     */
    public void setVprintType(String vprintType) {
        this.vprintType = vprintType;
    }

    public String getVprintResBin() {
        return vprintResBin;
    }

    /**
     * 设置声纹资源
     * 若在assets目录下，则指定文件名即可，如vprint.bin
     * 若在外部路径目录下，则需要指定绝对路径，如/sdcard/speech/vprint.bin
     *
     * @param vprintResBin 声纹资源
     */
    public void setVprintResBin(String vprintResBin) {
        this.vprintResBin = vprintResBin;
    }

    public String getVprintModelFile() {
        return vprintModelFile;
    }

    /**
     * 设置声纹模型保存路径，包含文件名，如/sdcard/speech/vprint.model
     *
     * @param vprintModelFile 声纹模型保存位置
     */
    public void setVprintModelFile(String vprintModelFile) {
        this.vprintModelFile = vprintModelFile;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (!TextUtils.isEmpty(vprintResBin)) {
                jsonObject.put(KEY_VPRINT_PATH, vprintResBin);
            }
            if (!TextUtils.isEmpty(vprintModelFile) && !vprintType.equals(VPRINTLITE_TYPE_ANTI_SPOOFING))
                jsonObject.put(KEY_MODEL_PATH, vprintModelFile);
            if (vprintType.equals(VPRINTLITE_TYPE_TD)) {
                jsonObject.put(KEY_VPRINT_TYPE, AIConstant.VPRINTLITE_TYPE_SR);
            } else {
                jsonObject.put(KEY_VPRINT_TYPE, vprintType);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }


    @Override
    public LocalVprintLiteConfig clone() throws CloneNotSupportedException {
        return (LocalVprintLiteConfig) super.clone();
    }
}
