package com.aispeech.export.widget.callback;

import org.json.JSONObject;

/**
 * 自定义控件
 *
 * @author hehr
 */
public class CustomCallbackWidget extends CallbackWidget {

    protected CustomCallbackWidget(JSONObject widget, int type, String skillId, String taskName, String intentName) {
        super(widget, type, skillId, taskName, intentName);
        setWidget(widget);
    }

    private JSONObject widget;

    public JSONObject getWidget() {
        return widget;
    }

    public void setWidget(JSONObject widget) {
        this.widget = widget;
    }
}
