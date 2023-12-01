package com.aispeech.lite.config;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wuwei on 2018/7/11.
 */

public class LocalVprintConfig extends AIEngineConfig {
    private static final String KEY_VPRINT_PATH = "vprintBinPath";
    private static final String KEY_ASRPP_PATH = "asrppBinPath";
    private static final String KEY_MODEL_PATH = "modelFile";

    private String vprintResBin;
    private String asrppResBin;
    private String vprintModelFile;
    private boolean useDatabaseStorage = false;
    private String vprintDatabasePath;

    public boolean isUseDatabaseStorage() {
        return useDatabaseStorage;
    }

    public void setUseDatabaseStorage(boolean useDatabaseStorage) {
        this.useDatabaseStorage = useDatabaseStorage;
    }

    public String getVprintDatabasePath() {
        return vprintDatabasePath;
    }

    public void setVprintDatabasePath(String vprintDatabasePath) {
        this.vprintDatabasePath = vprintDatabasePath;
    }

    public String getVprintResBin() {
        return vprintResBin;
    }

    public void setVprintResBin(String vprintResBin) {
        this.vprintResBin = vprintResBin;
    }

    public String getAsrppResBin() {
        return asrppResBin;
    }

    public void setAsrppResBin(String asrppResBin) {
        this.asrppResBin = asrppResBin;
    }

    public String getVprintModelFile() {
        return vprintModelFile;
    }

    public void setVprintModelFile(String vprintModelFile) {
        this.vprintModelFile = vprintModelFile;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        try {
            if (!TextUtils.isEmpty(vprintResBin))
                jsonObject.put(KEY_VPRINT_PATH, vprintResBin);
            if (!TextUtils.isEmpty(asrppResBin))
                jsonObject.put(KEY_ASRPP_PATH, asrppResBin);
            if (!TextUtils.isEmpty(vprintModelFile))
                jsonObject.put(KEY_MODEL_PATH, vprintModelFile);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }


    @Override
    public LocalVprintConfig clone() throws CloneNotSupportedException {
        return (LocalVprintConfig) super.clone();
    }
}
