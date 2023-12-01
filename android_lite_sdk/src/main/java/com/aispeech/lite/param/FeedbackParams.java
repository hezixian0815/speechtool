package com.aispeech.lite.param;

import com.aispeech.common.JSONUtil;
import com.aispeech.export.widget.feedback.FeedbackWidget;

import org.json.JSONObject;

/**
 * feedback 参数
 *
 * @author hehr
 */
public class FeedbackParams {

    public FeedbackWidget widget;

    public String sessionId;

    public String recorderId;

    private void setWidget(FeedbackWidget widget) {
        this.widget = widget;
    }

    public String getSessionId() {
        return sessionId;
    }

    public FeedbackParams setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public String getRecorderId() {
        return recorderId;
    }

    public FeedbackParams setRecorderID(String recorderID) {
        this.recorderId = recorderID;
        return this;
    }

    public FeedbackParams(FeedbackWidget widget) {
        setWidget(widget);
    }

    public static final String KEY_SESSION_ID = "sessionId";

    public static final String KEY_RECORDER_ID = "recordId";

    public static final String KEY_TOPIC = "topic";

    public static final String VALUE_TOPIC = "dm.input.data";

    public static final String VALUE_TOPIC_NLU = "nlu.input.text";

    public JSONObject toJSON() {

        JSONObject feedback = JSONUtil.build(widget.toString());
        if (feedback != null) {
            JSONUtil.putQuietly(feedback, KEY_TOPIC, VALUE_TOPIC);
            JSONUtil.putQuietly(feedback, KEY_SESSION_ID, getSessionId());
            JSONUtil.putQuietly(feedback, KEY_RECORDER_ID, getRecorderId());
        }
        return feedback;
    }
}
