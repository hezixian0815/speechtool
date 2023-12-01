package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.common.JSONUtil;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 回复对话结果参数
 *
 * @version 1.0
 * Created by BaoBao.Wang on 2021/7/5 17:24
 */
public class FeedbackJsonParams {

    public static final String KEY_SESSION_ID = "sessionId";
    public static final String KEY_RECORDER_ID = "recordId";

    private String topic;
    private String data;
    public String sessionId;
    public String recorderId;

    public FeedbackJsonParams(String topic, String data) {
        this.topic = topic;
        this.data = data;
    }

    public FeedbackJsonParams setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    public FeedbackJsonParams setRecorderId(String recorderId) {
        this.recorderId = recorderId;
        return this;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRecorderId() {
        return recorderId;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    public JSONObject toJSON() {
        JSONObject feedback = new JSONObject();
        if (!TextUtils.isEmpty(sessionId)) {
            JSONUtil.putQuietly(feedback, KEY_SESSION_ID, sessionId);
        }
        if (!TextUtils.isEmpty(recorderId)) {
            JSONUtil.putQuietly(feedback, KEY_RECORDER_ID, recorderId);
        }
        JSONObject dataObj = null;
        if (!TextUtils.isEmpty(data)) {
            try {
                dataObj = new JSONObject(data);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        JSONUtil.putQuietly(feedback, topic, dataObj != null ? dataObj : "");
        return feedback;
    }
}
