package com.aispeech.export;


import com.aispeech.lite.dm.Protocol;

import org.json.JSONObject;

/**
 * native api
 *
 * @author hehr
 */
public class NativeApi {

    /**
     * 产品定义的api名称,必选
     */
    private String api;
    /**
     * native api 定义的参数信息,可选
     */
    private JSONObject param;
    /**
     * intent 名称
     */
    private String intentName;
    /**
     * skill id
     */
    private String skillId;
    /**
     * task name
     */
    private String taskName;

    public String getApi() {
        return api;
    }

    public void setApi(String api) {
        this.api = api;
    }

    public JSONObject getParam() {
        return param;
    }

    public void setParam(JSONObject param) {
        this.param = param;
    }

    public String getIntentName() {
        return intentName;
    }

    public void setIntentName(String intentName) {
        this.intentName = intentName;
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    private NativeApi(String api, JSONObject param, String intentName, String skillId, String taskName) {
        this.api = api;
        this.param = param;
        this.intentName = intentName;
        this.skillId = skillId;
        this.taskName = taskName;
    }

    public static NativeApi transform(JSONObject dm, String skillId, String taskName, String intentName) {

        return new NativeApi(
                dm.optString(Protocol.DM_API),
                dm.optJSONObject(Protocol.DM_PARAM),
                intentName,
                skillId,
                taskName
        );
    }

    @Override
    public String toString() {
        return "NativeApi{" +
                "api='" + api + '\'' +
                ", param=" + param +
                ", intentName='" + intentName + '\'' +
                ", skillId='" + skillId + '\'' +
                ", taskName='" + taskName + '\'' +
                '}';
    }
}
