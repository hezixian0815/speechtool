package com.aispeech.lite.param;

import com.aispeech.common.JSONUtil;
import com.aispeech.export.widget.feedback.FeedbackWidget;

import org.json.JSONObject;

/**
 * 客户端发送文本请求参数
 *
 * @author :     WangBaoBao
 * @create :     2020/2/27 23:26
 */
public class FeedbackTextParams extends FeedbackParams {

    private String refText; //请求的文本

    public FeedbackTextParams(FeedbackWidget widget) {
        super(widget);
    }

    /**
     * 获取请求文本
     *
     * @return 请求文本
     */
    public String getRefText() {
        return refText;
    }

    /**
     * 设置请求文本
     *
     * @param refText 请求文本
     */
    public FeedbackTextParams setRefText(String refText) {
        this.refText = refText;
        return this;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject feedback = new JSONObject();
        JSONUtil.putQuietly(feedback, KEY_TOPIC, VALUE_TOPIC_NLU);
        JSONUtil.putQuietly(feedback, KEY_SESSION_ID, getSessionId());
        JSONUtil.putQuietly(feedback, KEY_RECORDER_ID, getRecorderId());
        JSONUtil.putQuietly(feedback, "refText", getRefText());
        return feedback;
    }
}
