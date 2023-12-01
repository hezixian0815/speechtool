package com.aispeech.export;

import android.text.TextUtils;

import com.aispeech.lite.dm.Protocol;

import org.json.JSONObject;

/**
 * command实体类
 *
 * @author hehr
 */
public class Command {
    /**
     * 客户端定义执行动作,非必选
     */
    private String api;
    /**
     * 定义参数,非必选
     */
    private JSONObject param;

    /**
     * {@link RunSequence}
     * command和nlg先后顺序
     */
    public String runSequence;

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

    public String getRunSequence() {
        return runSequence;
    }

    public void setRunSequence(String runSequence) {
        this.runSequence = runSequence;
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

    public static class RunSequence {
        /**
         * 先播报后执行动作
         */
        public static final String NLG_FIRST = "nlgFirst";
        /**
         * 先执行动作,后播报nlg
         */
        public static final String COMMAND_FIRST = "commandFirst";
    }

    @Override
    public String toString() {
        return "Command{" +
                "api='" + api + '\'' +
                ", param=" + param +
                ", runSequence='" + runSequence + '\'' +
                '}';
    }

    /**
     * 构建函数
     *
     * @param api         api
     * @param param       参数
     * @param runSequence command执行顺序
     * @param intentName  意图名称
     * @param skillId     技能编号
     * @param taskName    task名称
     */
    private Command(String api, JSONObject param, String runSequence, String intentName, String skillId, String taskName) {
        this.api = api;
        this.param = param;
        this.runSequence = runSequence;
        this.intentName = intentName;
        this.skillId = skillId;
        this.taskName = taskName;
    }

    public static Command transform(JSONObject dm, String skillId, String taskName, String intentName) {
        JSONObject command = dm.optJSONObject(Protocol.DM_COMMAND);
        if (command != null) {
            String api = command.optString(Protocol.DM_API);
            if (!TextUtils.isEmpty(api)) {
                return new Command(
                        api,
                        command.optJSONObject(Protocol.DM_PARAM),
                        dm.optString(Protocol.DM_RUN_SEQUENCE),
                        intentName,
                        skillId,
                        taskName
                );
            }
        }
        return null;
    }
}
