package com.aispeech.export.widget.callback;

import org.json.JSONObject;

/**
 * web 类控件
 *
 * @author hehr
 */
public class WebCallbackWidget extends CallbackWidget {

    public WebCallbackWidget(JSONObject widget, int type, String skillId, String taskName, String intentName) {
        super(widget, type, skillId, taskName, intentName);
        setUrl(widget.optString(URL));
    }

    /**
     * url地址,必选
     */
    private String url;


    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * url地址,必选
     */
    public static final String URL = "url";


}
