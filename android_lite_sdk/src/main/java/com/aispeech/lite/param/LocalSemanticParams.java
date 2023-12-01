package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.common.JSONUtil;
import com.aispeech.lite.SemanticType;

import org.json.JSONException;
import org.json.JSONObject;

public class LocalSemanticParams extends SpeechParams {

    protected JSONObject jsonObject;

    //公用字段
    private static final String KEY_REF_TEXT = "refText";

    //semantic navi & aidui & bcdv2
    private static final String KEY_CORE_TYPE = "coreType";
    private static final String KEY_ENV = "env";
    private static final String KEY_OUTPUT_FORMAT = "output_format";

    //semantic dui
    private static final String KEY_SKILLID = "skillid";
    private static final String KEY_PINYIN = "pinyin";
    private static final String KEY_CASE = "case";

    private static final String KEY_USE_PINYIN = "use_pinyin";

    private static final String KEY_ENV_JSON = "envjson";

    //semantic navi
    private String refText = "";
    //设置前一轮的对话领域信息
    private String domain = "";
    //前一轮命中的task信息，需要外部传入
    private String task = "";
    //增加默认值
    private String outputFomat = "DUI";
    /**
     * feed的时候设置文本的长度
     */
    private int useRefTextLength = 20;

    //semantic dui,技能ID，指定技能查询，缩短语义出结果时间
    private String skillID = "";//
    //设置reftext拼音，可减少内核加载 pinyin 库的内存占用
    private String pinyin = "";

    private JSONObject envJson;

    private int duiCase = 2;
    private final SemanticType semanticType = SemanticType.MIX;

    private String builtInSemanticSkillID = "";

    private boolean usePinyin = true; // 默认开启拼音
    private double semanticThreshold = 0.9;

    private boolean enableBCDDiscard = false;
    private boolean useHarshDiscard = true; // 延续之前逻辑 默认开启 https://wiki.aispeech.com.cn/pages/viewpage.action?pageId=150826926

    public boolean isUsePinyin() {
        return usePinyin;
    }

    public void setUsePinyin(boolean usePinyin) {
        this.usePinyin = usePinyin;
    }

    public double getSemanticThreshold() {
        return semanticThreshold;
    }

    public void setSemanticThreshold(double semanticThreshold) {
        this.semanticThreshold = semanticThreshold;
    }

    public boolean isUseHarshDiscard() {
        return useHarshDiscard;
    }

    public void setUseHarshDiscard(boolean useHarshDiscard) {
        this.useHarshDiscard = useHarshDiscard;
    }

    public boolean isEnableBCDDiscard() {
        return enableBCDDiscard;
    }

    public void setEnableBCDDiscard(boolean enableBCDDiscard) {
        this.enableBCDDiscard = enableBCDDiscard;
    }

    public String getTask() {
        return task;
    }

    public LocalSemanticParams setTask(String task) {
        this.task = task;
        return this;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getRefText() {
        return refText;
    }

    public void setRefText(String refText) {
        this.refText = refText;
    }

    public JSONObject getEnvJson() {
        return envJson;
    }

    public void setEnvJson(JSONObject envJson) {
        this.envJson = envJson;
    }

    public int getUseRefTextLength() {
        return useRefTextLength;
    }

    public void setUseRefTextLength(int useRefTextLength) {
        this.useRefTextLength = useRefTextLength;
    }

    public String getDomain() {
        return "dlg_domain=" + domain + ";";//拼接env信息
    }

    //拼接env信息
    public String getEnv() {
        String env = "";

        // bcdv2 语义拒识判断标志的入参，见：https:/wiki.aispeech.com.cn/pages/viewpage.action?pageId=149078383
        if (isUseHarshDiscard()) {
            env = "use_harsh_discard=1;";
        }

        if (!TextUtils.isEmpty(domain)) {
            env = env + "dlg_domain=" + domain + ";";
        }

        if (enableBCDDiscard) {
            env = env + "use_recall_policy=1;";
        }

        return env;
    }

    public String getOutputFomat() {
        return outputFomat;
    }

    public void setOutputFomat(String outputFomat) {
        this.outputFomat = outputFomat;
    }

    public String getBuiltInSemanticSkillID() {
        return builtInSemanticSkillID;
    }

    public void setBuiltInSemanticSkillID(String builtInSemanticSkillID) {
        this.builtInSemanticSkillID = builtInSemanticSkillID;
    }


    private JSONObject getTaskJSON() {
        JSONObject object = new JSONObject();
        try {
            object.put("dlg_domain", task);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return object;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public void setSkillID(String skillID) {
        this.skillID = skillID;
    }

    public String getSkillID() {
        return skillID;
    }

    public JSONObject toJSON() {

        jsonObject = new JSONObject();

        if (!TextUtils.isEmpty(refText)) {
            String text = refText.length() > useRefTextLength ? refText.substring(0, useRefTextLength) : refText;
            JSONUtil.putQuietly(jsonObject, KEY_REF_TEXT, text);
        }

        if (!TextUtils.isEmpty(pinyin)) {
            JSONUtil.putQuietly(jsonObject, KEY_PINYIN, pinyin);
        }

        if (!TextUtils.isEmpty(outputFomat)) {
            JSONUtil.putQuietly(jsonObject, KEY_OUTPUT_FORMAT, outputFomat);
        }

        JSONUtil.putQuietly(jsonObject, KEY_CORE_TYPE, "cn.semantic");

        JSONUtil.putQuietly(jsonObject, KEY_ENV, getEnv());

        if (getEnvJson() != null) {
            JSONUtil.putQuietly(jsonObject, KEY_ENV_JSON, getEnvJson());
        }

        return jsonObject;
    }

    public JSONObject toDUIJSON() {

        jsonObject = new JSONObject();

        if (!TextUtils.isEmpty(refText)) {
            String text = refText.length() > useRefTextLength ? refText.substring(0, useRefTextLength) : refText;
            JSONUtil.putQuietly(jsonObject, KEY_REF_TEXT, text);
        }

        if (!TextUtils.isEmpty(pinyin)) {
            JSONUtil.putQuietly(jsonObject, KEY_PINYIN, pinyin);
        }

        if (!TextUtils.isEmpty(skillID)) {
            JSONUtil.putQuietly(jsonObject, KEY_SKILLID, skillID);
            duiCase = 0;
        } else {
            duiCase = 2;
        }

        if (!TextUtils.isEmpty(task)) {
            JSONUtil.putQuietly(jsonObject, KEY_ENV, getTaskJSON());
        }

        JSONUtil.putQuietly(jsonObject, KEY_USE_PINYIN, usePinyin ? 1 : 0);
        JSONUtil.putQuietly(jsonObject, KEY_CASE, duiCase);

        return jsonObject;
    }

    @Override
    public LocalSemanticParams clone() throws CloneNotSupportedException {
        return (LocalSemanticParams) super.clone();
    }
}
