package com.aispeech.export.intent;

import android.text.TextUtils;

import com.aispeech.common.AIConstant;
import com.aispeech.export.function.IIntent;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 离线语义引擎启动参数
 *
 * @author hehr
 */
public class AILocalSemanticIntent implements IIntent {

    /**
     * 识别启动时的参数设置
     */
    private AILocalASRIntent asrIntent;

    /**
     * 纯文本模式送语义的文本内容
     */
    private String refText;
    /**
     * feed的时候设置文本的长度
     */
    private int useRefTextLength = 20;
    /**
     * 对应的 refText 的拼音，建议开启，可以减少内存占用
     */
    private String pinyin;

    private String domain = "";//设置前一轮的对话领域信息

    private String task = "";//前一轮命中的task信息，需要外部传入

    private String skillID = ""; //dui离线语义 skillID支持快速查找

    private int maxWaitingTimeout;

    /**
     * 热词词条限制，默认为0，不截取；如果配置2 则截取txt的前两条
     */
    private int hotwordsMax;
    /**
     * 热词结构 {“词库名”:["词条名"]}
     * {"sys.联系人":["十二饭点","新梅华", "香椿炒蛋", "你好"], "对象":["新梅华", "香椿炒蛋", "你好"]}
     */
    private JSONObject hotwords;

    private boolean enableBCDDiscard = false;
    private double semanticThreshold = 0.9;
    private boolean useHarshDiscard = true; // 延续之前逻辑 默认开启 https://wiki.aispeech.com.cn/pages/viewpage.action?pageId=150826926

    /**
     * 设置是否开启拼音输出
     * 须在start之前设置才生效
     */
    private boolean usePinyin = true; // 默认开启拼音

    private String envJson = "";

    public boolean isUsePinyin() {
        return usePinyin;
    }

    public void setUsePinyin(boolean usePinyin) {
        this.usePinyin = usePinyin;
    }

    public AILocalASRIntent getAsrIntent() {
        return asrIntent;
    }

    public void setAsrIntent(AILocalASRIntent asrIntent) {
        this.asrIntent = asrIntent;
    }

    public String getRefText() {
        return refText;
    }

    public void setRefText(String refText) {
        this.refText = refText;
    }

    public int getUseRefTextLength() {
        return useRefTextLength;
    }

    public void setUseRefTextLength(int useRefTextLength) {
        this.useRefTextLength = useRefTextLength;
    }

    public String getDomain() {
        return domain;
    }

    /**
     * 设置 buidlin+navi 前一轮的对话领域信息，实现多轮交互
     *
     * @param domain 上一轮领域
     * {@link #setContext(String)}
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getTask() {
        return task;
    }

    /**
     * 设置dui语义前一轮的对话task，实现多轮交互
     *
     * @param task 上一轮技能中的task
     * {@link #setContext(String)}
     */
    public void setTask(String task) {
        this.task = task;
    }

    /**
     * 设置当前上下文，实现多轮交互
     *
     * @param lastNlu 上一轮的语义
     */
    public void setContext(String lastNlu) {
        if (TextUtils.isEmpty(lastNlu)) {
            return;
        }
        try {
            JSONObject nluObj = new JSONObject(lastNlu);
            if (nluObj.has("semantics")) {
                String source = nluObj.optString(AIConstant.Nlu.KEY_SOURCE);
                JSONObject semanticsObj = nluObj.optJSONObject("semantics");
                if (semanticsObj.has("request")) {
                    JSONObject requestObj = semanticsObj.optJSONObject("request");
                    if (AIConstant.Nlu.SOURCE_NAVI.equals(source) || AIConstant.Nlu.SOURCE_AIDUI.equals(source)) {
                        if (requestObj.has("domain")) {
                            this.domain = requestObj.optString("domain");
                        }
                    }
                    if (AIConstant.Nlu.SOURCE_DUI.equals(source) && requestObj.has("task")) {
                        this.task = requestObj.optString("task");
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setSkillID(String skillID) {
        this.skillID = skillID;
    }

    public String getSkillID() {
        return skillID;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public int getMaxWaitingTimeout() {
        return maxWaitingTimeout;
    }

    public void setMaxWaitingTimeout(int maxWaitingTimeout) {
        this.maxWaitingTimeout = maxWaitingTimeout;
    }

    public void setSemanticThreshold(double semanticThreshold) {
        this.semanticThreshold = semanticThreshold;
    }

    public double getSemanticThreshold() {
        return semanticThreshold;
    }

    public boolean isEnableBCDDiscard() {
        return enableBCDDiscard;
    }

    public void setEnableBCDDiscard(boolean enableBCDDiscard) {
        this.enableBCDDiscard = enableBCDDiscard;
    }

    public boolean isUseHarshDiscard() {
        return useHarshDiscard;
    }

    public void setUseHarshDiscard(boolean useHarshDiscard) {
        this.useHarshDiscard = useHarshDiscard;
    }

    public int getHotwordsMax() {
        return hotwordsMax;
    }

    /**
     * 设置截取txt的热词条数
     * 热词词条限制，默认为0，不截取；如果配置2 则截取txt的前两条
     *
     * @param hotwordsMax
     */
    public void setHotwordsMax(int hotwordsMax) {
        this.hotwordsMax = hotwordsMax;
    }

    public JSONObject getHotwords() {
        return hotwords;
    }

    /**
     * 设置词库
     * 热词结构 {“词库名”:["词条名"]}
     *  {"sys.联系人":["十二饭点","新梅华", "香椿炒蛋", "你好"], "对象":["新梅华", "香椿炒蛋", "你好"]}
     * @param hotwords
     */
    public void setHotwords(JSONObject hotwords) {
        this.hotwords = hotwords;
    }

    public String getEnvJson() {
        return envJson;
    }

    public void setEnvJson(String envJson) {
        this.envJson = envJson;
    }

}
