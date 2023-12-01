package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.export.widget.Scene;
import com.aispeech.lite.Languages;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by wuwei on 18-5-14.
 */

public class LocalAsrConfig extends AIEngineConfig {
    private String resBinPath;
    private String netBinPath;
    private boolean vadEnable = true;
    private String dspBinPath;
    private String ngramSlotBinPath;
    private Scene[] scenes;

    private boolean useItn = false;
    private String itnLuaResFolderName;
    private String numBinPath;
    private boolean itnUpperCase = true;

    private boolean useAggregateMate;//是否启用集内匹配
    private String aggregateMateCommandBinPath;//集内匹配资源路径,command.bin
    private String aggregateMateBinPath;//集内匹配资源路径,cmd.bin

    /**
     * 内核引擎加载的资源范围，区分中文与海外资源等
     */
    private String scope = Languages.CHINESE.getLanguage();

    public String getScope() {
        return scope;
    }

    /**
     * 内核引擎加载的资源范围，区分中文与海外资源等
     *
     * @param scope 语言类型
     * @see Languages
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    @Override
    public void setVadEnable(boolean vadEnable) {
        this.vadEnable = vadEnable;
    }

    @Override
    public boolean isVadEnable() {
        return vadEnable;
    }

    public void setResBinPath(String resBinPath) {
        this.resBinPath = resBinPath;
    }

    public String getResBinPath() {
        return resBinPath;
    }

    public void setNetBinPath(String netBinPath) {
        this.netBinPath = netBinPath;
    }

    public String getNetBinPath() {
        return netBinPath;
    }

    public String getDspBinPath() {
        return dspBinPath;
    }

    public void setDspBinPath(String dspBinPath) {
        this.dspBinPath = dspBinPath;
    }

    public String getNgramSlotBinPath() {
        return ngramSlotBinPath;
    }

    public void setNgramSlotBinPath(String ngramSlotBinPath) {
        this.ngramSlotBinPath = ngramSlotBinPath;
    }

    public Scene[] getScene() {
        return scenes;
    }

    public void setScene(Scene[] scenes) {
        this.scenes = scenes;
    }

    public boolean isUseItn() {
        return useItn;
    }

    public void setUseItn(boolean useItn) {
        this.useItn = useItn;
    }

    public boolean isItnUpperCase() {
        return itnUpperCase;
    }

    public void setItnUpperCase(boolean itnUpperCase) {
        this.itnUpperCase = itnUpperCase;
    }

    public String getItnLuaResFolderName() {
        return itnLuaResFolderName;
    }

    public void setItnLuaResFolderName(String itnLuaResFolderName) {
        this.itnLuaResFolderName = itnLuaResFolderName;
    }

    public String getNumBinPath() {
        return numBinPath;
    }

    public void setNumBinPath(String numBinPath) {
        this.numBinPath = numBinPath;
    }

    public boolean isUseAggregateMate() {
        return useAggregateMate;
    }

    public void setUseAggregateMate(boolean useAggregateMate) {
        this.useAggregateMate = useAggregateMate;
    }

    public String getAggregateMateCommandBinPath() {
        return aggregateMateCommandBinPath;
    }

    public void setAggregateMateCommandBinPath(String aggregateMateCommandBinPath) {
        this.aggregateMateCommandBinPath = aggregateMateCommandBinPath;
    }

    public String getAggregateMateBinPath() {
        return aggregateMateBinPath;
    }

    public void setAggregateMateBinPath(String aggregateMateBinPath) {
        this.aggregateMateBinPath = aggregateMateBinPath;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        try {
            jsonObject.put("prof", Log.parseDuiliteLog());
            if (!TextUtils.isEmpty(resBinPath))
                jsonObject.put("resBinPath", resBinPath);
            if (!TextUtils.isEmpty(netBinPath))
                jsonObject.put("netBinPath", netBinPath);

            if (!TextUtils.isEmpty(dspBinPath)) {
                File dspFile = new File(getDspBinPath());
                jsonObject.put("dspPath", dspFile.getParent());
            }
            if (!TextUtils.isEmpty(scope)) {
                jsonObject.put("scope", scope);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject toItnJson() {
        JSONObject jsonObject = super.toJson();
        try {
            if (!TextUtils.isEmpty(itnLuaResFolderName))
                jsonObject.put("luaPath", itnLuaResFolderName + "/post_lex.lua");
            if (!TextUtils.isEmpty(numBinPath)) jsonObject.put("binPath", numBinPath);
            jsonObject.put("upperCase", itnUpperCase ? 1 : 0);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public JSONObject toUpdateJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            if (!TextUtils.isEmpty(netBinPath))
                jsonObject.put("netBinPath", netBinPath);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject toAggregateMateJson() {
        JSONObject jsonObject = super.toJson();
        try {
            if (!TextUtils.isEmpty(itnLuaResFolderName))
                jsonObject.put("luaPath", itnLuaResFolderName + "/post_lex.lua");
            if (!TextUtils.isEmpty(aggregateMateBinPath)) jsonObject.put("binPath", aggregateMateBinPath);
            jsonObject.put("upperCase", itnUpperCase ? 1 : 0);
            if (!TextUtils.isEmpty(aggregateMateCommandBinPath)) {
                JSONObject vocabsJSONObject = new JSONObject();
                vocabsJSONObject.put("use_mmap",0);

                JSONObject key_setJSONObject = new JSONObject();
                key_setJSONObject.put("command", aggregateMateCommandBinPath);
                vocabsJSONObject.put("kv_set",key_setJSONObject);

                vocabsJSONObject.put("use_trie",1);
                vocabsJSONObject.put("use_kv_copy",0);

                jsonObject.put("vocabs", vocabsJSONObject);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public LocalAsrConfig clone() throws CloneNotSupportedException {
        return (LocalAsrConfig) super.clone();
    }
}
