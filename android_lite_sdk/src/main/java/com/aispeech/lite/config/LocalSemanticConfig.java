package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.common.FileIOUtils;
import com.aispeech.common.Log;
import com.aispeech.lite.SemanticType;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;


public class LocalSemanticConfig extends AIEngineConfig {
    public static final String TAG = "SemanticConfig";
    private String naviResPath;
    private String naviLuaPath;
    private String naviVocabPath;
    private String naviSkillConfPath;
    private String buildinNaviSkillConfPath;
    private String duiResPath;
    private String duiLuaPath;
    private String duiResCustomPath;
    private String vocabCfgPath;

    /**
     * 内置语义资源文件夹路径
     */
    public String buildinResPath;

    /**
     * 内置语义lua资源文件夹路径
     */
    public String buildinLuaPath;

    /**
     * 离线内置语义与在线语义的映射表，如果外部配置，sdk内部会对齐在线skill名和补充离线skillId
     */
    public String skillMappingPath;

    private boolean isUseRefText = false;

    /**
     * 是否启用语义输出格式归一化,默认 true
     */
    private boolean useFormat = true;

    /**
     * 是否使用sdk内部的语义决策逻辑，默认 true
     */
    private boolean useSelectRule = true;

    /**
     * 最终 grammar 与 ngram 输出结果的决策阈值
     */
    private double selectRuleThreshold = 0.63;

    private SemanticType semanticType = SemanticType.MIX;

    private boolean enableNluFormatV2;

    //设置是否抛出空语义 true：抛出空语义不返回错误，false，返回识别为空的错误，不抛语义
    private boolean throwEmptySemantic;

    public boolean isUseRefText() {
        return isUseRefText;
    }

    public void setUseRefText(boolean useRefText) {
        isUseRefText = useRefText;
    }

    public boolean isUseFormat() {
        return useFormat;
    }

    public void setUseFormat(boolean useFormat) {
        this.useFormat = useFormat;
    }

    public boolean isUseSelectRule() {
        return useSelectRule;
    }

    public void setUseSelectRule(boolean useSelectRule) {
        this.useSelectRule = useSelectRule;
    }

    public void setSelectRuleThreshold(double decisionThreshold) {
        this.selectRuleThreshold = decisionThreshold;
    }

    public double getSelectRuleThreshold() {
        return selectRuleThreshold;
    }

    public String getNaviResPath() {
        return naviResPath;
    }

    public boolean isThrowEmptySemantic() {
        return throwEmptySemantic;
    }

    public void setThrowEmptySemantic(boolean throwEmptySemantic) {
        this.throwEmptySemantic = throwEmptySemantic;
    }

    /**
     * 设置离线导航语义资源路径
     *
     * @param naviResPath resPath
     */
    public void setNaviResPath(String naviResPath) {
        Log.d(TAG, "naviResPath :" + naviResPath);
        if (TextUtils.isEmpty(naviResPath)) {
            Log.e(TAG, "Invalid navi ebnfFile");
            return;
        }
        this.naviResPath = naviResPath;
    }

    public String getNaviLuaPath() {
        return naviLuaPath;
    }

    public void setNaviLuaPath(String naviLuaPath) {
        if (TextUtils.isEmpty(naviLuaPath)) {
            Log.e(TAG, "Invalid navi luaFile");
            return;
        }
        this.naviLuaPath = naviLuaPath;
    }

    public String getNaviVocabPath() {
        return naviVocabPath;
    }

    public void setNaviVocabPath(String naviVocabPath) {

        if (TextUtils.isEmpty(naviVocabPath)) {
            Log.e(TAG, "Invalid navi vocabFile");
            return;
        }
        this.naviVocabPath = naviVocabPath;
    }


    public String getVocabCfgPath() {
        return vocabCfgPath;
    }

    public void setVocabCfgPath(String vocabCfgPath) {
        if (TextUtils.isEmpty(vocabCfgPath)) {
            Log.e(TAG, "Invalid Vocab cfg");
            return;
        }
        this.vocabCfgPath = vocabCfgPath;
    }

    public String getNaviSkillConfPath() {
        return naviSkillConfPath + "/navi_skill.conf";
    }

    public void setNaviSkillConfPath(String skillConfPath) {
        if (TextUtils.isEmpty(skillConfPath)) {
            Log.e(TAG, "Invalid skill conf path");
            return;
        }
        this.naviSkillConfPath = skillConfPath;
    }

    public String getBuildinSkillConfPath() {
        return buildinNaviSkillConfPath + "/navi_skill.conf";
    }

    public void setBuildinSkillConfPath(String skillConfPath) {
        if (TextUtils.isEmpty(skillConfPath)) {
            Log.e(TAG, "Invalid skill conf path");
            return;
        }

        this.buildinNaviSkillConfPath = skillConfPath;

    }

    public String getDUIResPath() {
        return duiResPath;
    }

    /**
     * 设置语义资源路径
     *
     * @param duiResPath resPath
     */
    public void setDUIResPath(String duiResPath) {

        if (TextUtils.isEmpty(duiResPath)) {
            Log.e(TAG, "Invalid DUI ebnfFile");
            return;
        }
        this.duiResPath = duiResPath;
    }

    public String getDUILuaPath() {
        return duiLuaPath;
    }

    public void setDUILuaPath(String duiLuaPath) {
        if (TextUtils.isEmpty(duiLuaPath)) {
            Log.e(TAG, "Invalid DUI luaFile");
            return;
        }
        this.duiLuaPath = duiLuaPath;
    }


    /**
     * 设置语义资源custom路径
     *
     * @param duiResCustomPath resPath
     */
    public void setDUIResCustomPath(String duiResCustomPath) {
        if (TextUtils.isEmpty(duiResCustomPath)) {
            Log.e(TAG, "Invalid DUI custom path");
            return;
        }
        this.duiResCustomPath = duiResCustomPath;
    }

    public String getBuildinResPath() {
        return buildinResPath;
    }

    public void setBuildinResPath(String buildinResPath) {
        this.buildinResPath = buildinResPath;
    }

    public String getBuildinLuaPath() {
        return buildinLuaPath;
    }

    public void setBuildinLuaPath(String buildinLuaPath) {
        this.buildinLuaPath = buildinLuaPath;
    }

    public String getSkillMappingPath() {
        return skillMappingPath;
    }

    public void setSkillMappingPath(String skillMappingPath) {
        this.skillMappingPath = skillMappingPath;
    }

    public void setSemanticType(SemanticType semanticType) {
        this.semanticType = semanticType;
    }

    public SemanticType getSemanticType() {
        return semanticType;
    }

    public boolean isEnableNluFormatV2() {
        return enableNluFormatV2;
    }

    public void setEnableNluFormatV2(boolean enableNluFormatV2) {
        this.enableNluFormatV2 = enableNluFormatV2;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        try {
            Log.d(TAG, "naviResPath :" + naviResPath);
            if (!TextUtils.isEmpty(naviResPath)) {
                jsonObject.put("resPath", naviResPath + "/semantic");
            }
            if (!TextUtils.isEmpty(naviLuaPath)) {
                jsonObject.put("luaPath", naviLuaPath + "/semantic.lub," + naviLuaPath + "/res.lub," + naviLuaPath + "/core.lub");
            }
            if (!TextUtils.isEmpty(naviVocabPath)) {
                jsonObject.put("vocabPath", naviVocabPath + "/semantic/lex/vocabs");
            }

            if (!TextUtils.isEmpty(naviSkillConfPath)) {
                jsonObject.put("skillConfPath", getNaviSkillConfPath());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject getVocabsTxtJson(String vocabTxtPath) {
        JSONObject jsonObject = super.toJson();
        try {
            if (!TextUtils.isEmpty(vocabTxtPath)) {
                jsonObject.put("vocabsTextPath", vocabTxtPath);
            }
            if (!TextUtils.isEmpty(vocabCfgPath)) {
                jsonObject.put("vocabsCfgPath", vocabCfgPath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject toDUIJson() {
        JSONObject jsonObject = super.toJson();
        try {
            if (!TextUtils.isEmpty(duiResPath)) {
                jsonObject.put("resPath", duiResPath);
            }

            if (!TextUtils.isEmpty(duiResCustomPath)) {
                jsonObject.put("resCustomPath", duiResCustomPath);
            }

            if (!TextUtils.isEmpty(duiLuaPath)) {
                jsonObject.put("luaPath", duiLuaPath + "/semantic_dui.lub," + duiLuaPath + "/core_dui.lub");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject toBuildinBcdV2Json() {
        JSONObject jsonObject = super.toJson();
        try {
            if (!TextUtils.isEmpty(buildinResPath)) {
                jsonObject.put("resPath", buildinResPath);
            }

            if (!TextUtils.isEmpty(buildinLuaPath)) {
                jsonObject.put("luaPath", buildinLuaPath + "/semantic_bcd.lub," + buildinLuaPath + "/res.lub," + buildinLuaPath + "/core_bcd.lub");
            }

            if (!TextUtils.isEmpty(buildinResPath)) {
                jsonObject.put("vocabPath", buildinResPath + "/vocabs/bin");
            }

            if (!TextUtils.isEmpty(buildinLuaPath)) {
                File recallConfigFile = new File(buildinLuaPath + File.separator + "semantic_recall.json");
                if (recallConfigFile.exists()) {
                    try {
                        String recallConfig = FileIOUtils.readFile2String(recallConfigFile);
                        Log.i(TAG, "recallConfig: " + recallConfig);
                        jsonObject.put("newParam", new JSONObject(recallConfig));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    Log.w(TAG, "semantic_recall.json not found!!");
                }
            }

            // debug接口 暂不对外开放
//            jsonObject.put("debug",true);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject toBuildinJson() {
        JSONObject jsonObject = super.toJson();
        try {
            if (!TextUtils.isEmpty(buildinResPath)) {
                jsonObject.put("resPath", buildinResPath + "/semantic");
            }

            if (!TextUtils.isEmpty(buildinLuaPath)) {
                jsonObject.put("luaPath", buildinLuaPath + "/semantic.lub," + buildinLuaPath + "/res.lub," + buildinLuaPath + "/core.lub");
            }

            if (!TextUtils.isEmpty(buildinResPath)) {
                jsonObject.put("vocabPath", buildinResPath + "/semantic/lex/vocabs");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public LocalSemanticConfig clone() throws CloneNotSupportedException {
        return (LocalSemanticConfig) super.clone();
    }

    @Override
    public String toString() {
        return "LocalSemanticConfig{" +
                "naviResPath='" + naviResPath + '\'' +
                ", naviLuaPath='" + naviLuaPath + '\'' +
                ", naviVocabPath='" + naviVocabPath + '\'' +
                ", naviSkillConfPath='" + naviSkillConfPath + '\'' +
                ", duiResPath='" + duiResPath + '\'' +
                ", duiLuaPath='" + duiLuaPath + '\'' +
                ", duiResCustomPath='" + duiResCustomPath + '\'' +
                ", buildinResPath='" + buildinResPath + '\'' +
                ", buildinLuaPath='" + buildinLuaPath + '\'' +
                ", isUseRefText=" + isUseRefText +
                ", useFormat=" + useFormat +
                ", useSelectRule=" + useSelectRule +
                ", selectRuleThreshold=" + selectRuleThreshold +
                ", semanticType=" + semanticType +
                ", enableNluFormatV2=" + enableNluFormatV2 +
                ", throwEmptySemantic=" + throwEmptySemantic +
                ", vocabsCfgPath=" + vocabCfgPath +
                '}';
    }
}
