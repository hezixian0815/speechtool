package com.aispeech.export;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * description:多模态数据
 * author: WangBaoBao
 * created on: 2020/11/18 11:34
 */
public class MultiModal {

    private String sessionId;
    private String skillId;
    private String task;
    private String intent;
    private Slots slots;
    private String playerState;

    /**
     * 获取会话id
     *
     * @return 会话id
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * 设置会话id
     *
     * @param sessionId 会话id
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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
    public String getTask() {
        return task;
    }

    /**
     * 设置任务名称
     *
     * @param task 任务名称
     */
    public void setTask(String task) {
        this.task = task;
    }

    /**
     * 获取意图名称
     *
     * @return 意图名称
     */
    public String getIntent() {
        return intent;
    }

    /**
     * 设置意图名称
     *
     * @param intent 意图名称
     */
    public void setIntent(String intent) {
        this.intent = intent;
    }

    /**
     * 获取语义槽
     *
     * @return 语义槽
     */
    public Slots getSlots() {
        return slots;
    }

    /**
     * 设置语义槽
     *
     * @param slots 语义槽
     */
    public void setSlots(Slots slots) {
        this.slots = slots;
    }

    /**
     * 设置客户端播报状态
     *
     * @param playerState on/off
     */
    public void setPlayerState(String playerState) {
        this.playerState = playerState;
    }

    /**
     * 获取客户端播报状态
     *
     * @return 播报状态
     */
    public String getPlayerState() {
        return playerState;
    }


    @Override
    public String toString() {
        return toJson().toString();
    }

    public JSONObject toJson() {
        JSONObject multiModalObj = new JSONObject();
        try {
            multiModalObj.put("sessionId", sessionId);
            multiModalObj.put("skillId", skillId);

            JSONObject asyncObj = new JSONObject();
            if (!TextUtils.isEmpty(task)) {
                asyncObj.put("task", task);
            }
            if (!TextUtils.isEmpty(intent)) {
                asyncObj.put("intent", intent);
            }
            if (slots != null) {
                asyncObj.put("slots", new JSONArray(slots.toJSON()));
            }
            if (!TextUtils.isEmpty(playerState)) {
                JSONObject playerStateObj = new JSONObject();
                playerStateObj.put("playerState", playerState);
                asyncObj.put("dispatchEvent", playerStateObj);
            }
            multiModalObj.put("async", asyncObj);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return multiModalObj;
    }
}
