package com.aispeech.export.widget.callback;

import org.json.JSONObject;

/**
 * 文本控件
 *
 * @author hehr
 */
public class TextCallbackWidget extends CallbackWidget {

    /**
     * 输出文本,必须按
     */
    private String text;

    public TextCallbackWidget(JSONObject widget, int type, String skillId, String taskName, String intentName) {
        super(widget, type, skillId, taskName, intentName);
        setText(widget.optString(TEXT));
    }


    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    /**
     * 输出文本,必选
     */
    private static final String TEXT = "text";

}
