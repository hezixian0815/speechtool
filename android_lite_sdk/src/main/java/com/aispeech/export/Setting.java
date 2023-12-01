package com.aispeech.export;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * 配置类
 *
 * @author hehr
 */
public class Setting {

    private String key;
    private Object value;

    public Setting(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public Setting(String key, int value) {
        this.key = key;
        this.value = value;
    }

    public Setting(String key, double value) {
        this.key = key;
        this.value = value;
    }

    public Setting(String key, boolean value) {
        this.key = key;
        this.value = value;
    }

    public Setting(String key, float value) {
        this.key = key;
        this.value = value;
    }

    public Setting(String key, long value) {
        this.key = key;
        this.value = value;
    }

    public Setting(String key, JSONObject value) {
        this.key = key;
        this.value = value;
    }

    public Setting(String key, JSONArray value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public Object getValue() {
        return value;
    }

    public JSONObject toJSON() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("key", this.key);
            jsonObject.put("value", this.value);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * 定位信息
     */
    public static final String LOCATION = "location";
    /**
     * 技能优先级
     */
    public static final String SKILL_PRIORITY = "skillPriority";
    /**
     * 全双工锁定技能调度列表
     */
    public static final String DISPATCH_SKILL_LIST = "dispatchSkillList";
    /**
     * 调度后过滤；技能都参与调度，命中技能在该名单中，则过滤该skillid，如：闲聊
     */
    public static final String FILTER_SKILL_LIST = "filterSkillList";

    /**
     * 指定技能调度黑名单列表，DM中控调度时,对于指定的技能不参与调度
     */
    public static final String EXCLUDE_DISPATCH_SKILL_LIST = "excludeDispatchSkillList";

    /**
     * 全双工对话超时时间
     */
    public static final String FULL_DUPLEX_SESSION_TIMEOUT = "fullduplexSessionTimeout";

    /**
     * 全双工过滤策略开关
     */
    public static final String FULL_DUPLEX_FILTER_SWITCH = "filterSwitch";
}
