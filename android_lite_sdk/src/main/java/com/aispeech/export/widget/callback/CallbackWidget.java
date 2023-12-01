package com.aispeech.export.widget.callback;


import android.text.TextUtils;

import com.aispeech.common.Log;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 对话控件
 *
 * @author hehr
 */
public class CallbackWidget {

    protected CallbackWidget(JSONObject widget, int type, String skillId, String taskName, String intentName) {
        this.type = type;
        this.intentName = intentName;
        this.skillId = skillId;
        this.taskName = taskName;
        setRecommendations(widget.optJSONArray(RECOMMENDATIONS));
        setExtra(widget.optJSONObject(EXTRA));
    }

    /**
     * 控件类型，取值{@link CallbackWidgetType#type}
     */
    public int type;

    /**
     * intent 名称
     */
    public String intentName;
    /**
     * skill id
     */
    public String skillId;
    /**
     * task name
     */
    public String taskName;
    /**
     * 用于显示推荐气泡的数据,非必选
     */
    public JSONArray recommendations;

    /**
     * 自定义参数,由webhook或者localhook透传出来，作为附加信息使用,非必选
     */
    public JSONObject extra;

    public String getIntentName() {
        return intentName;
    }

    public String getSkillId() {
        return skillId;
    }

    public String getTaskName() {
        return taskName;
    }

    public JSONArray getRecommendations() {
        return recommendations;
    }

    public JSONObject getExtra() {
        return extra;
    }

    public int getType() {
        return type;
    }

    protected void setType(int type) {
        this.type = type;
    }

    protected void setIntentName(String intentName) {
        this.intentName = intentName;
    }

    protected void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    protected void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    protected void setRecommendations(JSONArray recommendations) {
        this.recommendations = recommendations;
    }

    protected void setExtra(JSONObject extra) {
        this.extra = extra;
    }

    /**
     * TYPE字段
     */
    protected static final String TYPE = "type";
    /**
     * 推荐气泡数据
     */
    protected static final String RECOMMENDATIONS = "recommendations";
    /**
     * 自定义参数
     */
    protected static final String EXTRA = "extra";

    private static final String TAG = "CallbackWidget";

    /**
     * 创建widget
     *
     * @param widget json
     * @param skillId skill id
     * @param taskName taskName
     * @param intentName intentName
     * @return {@link CallbackWidget}
     */
    public static CallbackWidget transForm(JSONObject widget, String skillId, String taskName, String intentName) {

        if (widget != null && widget.has(TYPE)) {
            String type = widget.optString(TYPE);
            Log.i(TAG, "create widget type is :" + type);
            if (TextUtils.equals(CallbackWidgetType.TEXT.getName(), type)) {
                return new TextCallbackWidget(widget,
                        CallbackWidgetType.TEXT.getType(),
                        skillId,
                        taskName,
                        intentName);
            } else if (TextUtils.equals(CallbackWidgetType.MEDIA.getName(), type)) {
                return new MediaCallbackWidget(widget,
                        CallbackWidgetType.MEDIA.getType(),
                        skillId,
                        taskName,
                        intentName);
            } else if (TextUtils.equals(CallbackWidgetType.CONTENT.getName(), type)) {
                return new ContentCallbackWidget(widget,
                        CallbackWidgetType.CONTENT.getType(),
                        skillId,
                        taskName,
                        intentName);
            } else if (TextUtils.equals(CallbackWidgetType.LIST.getName(), type)) {
                return new ListCallbackWidget(widget,
                        CallbackWidgetType.LIST.getType(),
                        skillId,
                        taskName,
                        intentName);
            } else if (TextUtils.equals(CallbackWidgetType.WEB.getName(), type)) {
                return new WebCallbackWidget(widget,
                        CallbackWidgetType.WEB.getType(),
                        skillId,
                        taskName,
                        intentName
                        );
            } else if (TextUtils.equals(CallbackWidgetType.CUSTOM.getName(), type)) {
                return new CustomCallbackWidget(widget,
                        CallbackWidgetType.CUSTOM.getType(),
                        skillId,
                        taskName,
                        intentName);
            } else {
                Log.e(TAG, "unknown widget type: " + type);
                return null;
            }
        }

        return null;
    }

}
