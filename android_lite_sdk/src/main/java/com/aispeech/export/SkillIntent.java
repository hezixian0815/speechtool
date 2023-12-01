package com.aispeech.export;

import android.text.TextUtils;

import com.aispeech.common.Util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yu on 2018/12/20.
 */

public class SkillIntent {
    private String skillId;
    private String taskName;
    private String intentName;
    private String slots;
    private String recordId;
    private String sessionId;
    private String skill;
    private String input;

    /**
     * 技能意图类构造方法
     */
    public SkillIntent() {
    }

    /**
     * 技能意图类构造方法
     *
     * @param skillId    技能id， 必填
     * @param taskName   任务名称， 必填
     * @param intentName 意图名称， 必填
     * @param slots      语义槽， key-value Json， 可选， 不想填可以填null
     */
    public SkillIntent(String skillId, String taskName, String intentName,
                       String slots) {
        this.skillId = skillId;
        this.taskName = taskName;
        this.intentName = intentName;
        this.slots = slots;
    }

    /**
     * 获取技能id
     *
     * @return 技能id
     */
    public String getSkillId() {
        return skillId;
    }

    /**
     * 设置技能id
     *
     * @param skillId 技能id
     */
    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    /**
     * 获取任务名称
     *
     * @return 任务名称
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * 设置任务名称
     *
     * @param taskName 任务名称
     */
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    /**
     * 获取意图名称
     *
     * @return 意图名称
     */
    public String getIntentName() {
        return intentName;
    }

    /**
     * 设置意图名称
     *
     * @param intentName 意图名称
     */
    public void setIntentName(String intentName) {
        this.intentName = intentName;
    }

    /**
     * 获取语义槽
     *
     * @return 语义槽
     */
    public String getSlots() {
        return slots;
    }

    /**
     * 设置语义槽
     *
     * @param slots 语义槽
     * @deprecated Use {@link SkillIntent#setSlots(Slots)} instead.
     */
    public void setSlots(String slots) {
        this.slots = slots;
    }

    /**
     * 设置语义槽
     *
     * @param slots 语义槽
     */
    public void setSlots(Slots slots) {
        this.slots = slots.toJSON();
    }

    /**
     * 获取录音id
     *
     * @return 录音id
     */
    public String getRecordId() {
        return recordId;
    }

    /**
     * 设置录音id
     *
     * @param recordId 录音id
     */
    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    /**
     * 获取会话id
     *
     * @return 会话id
     */
    public String getSessionId() {
        if(TextUtils.isEmpty(sessionId)){
            sessionId = Util.uuid();
        }
        return sessionId;
    }

    /**
     * 设置会话 id
     *
     * @param sessionId 会话id
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 获取技能名称
     *
     * @return 技能名称
     */
    public String getSkill() {
        return skill;
    }

    /**
     * 设置技能名称
     *
     * @param skill 技能名称
     */
    public void setSkill(String skill) {
        this.skill = skill;
    }

    /**
     * 获取输入信息
     *
     * @return String
     */
    public String getInput() {
        return input;
    }

    /**
     * 设置输入信息
     *
     * @param input String
     */
    public void setInput(String input) {
        this.input = input;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("skillId", skillId);
            obj.put("task", taskName);
            obj.put("intent", intentName);
            if (!TextUtils.isEmpty(slots)) {
                if (slots.startsWith("[")) {
                    obj.put("slots", new JSONArray(slots));
                } else {
                    obj.put("slots", new JSONObject(slots));
                }
            }

            if (!TextUtils.isEmpty(recordId)) {
                obj.put("recordId", recordId);
            }
            if (!TextUtils.isEmpty(sessionId)) {
                obj.put("sessionId", sessionId);
            }
            if (!TextUtils.isEmpty(skill)) {
                obj.put("skill", skill);
            }
            if (!TextUtils.isEmpty(input)) {
                obj.put("input", input);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }
}
