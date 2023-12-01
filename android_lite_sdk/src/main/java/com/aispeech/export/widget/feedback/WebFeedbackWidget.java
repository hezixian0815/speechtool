package com.aispeech.export.widget.feedback;

import java.util.ArrayList;

/**
 * Web控件类展示富文本内容，如：天气/股票等
 */
public class WebFeedbackWidget extends FeedbackWidget {
    ArrayList<ContentFeedbackWidget> mList = new ArrayList<>();

    /**
     * WebWidget构造方法
     */
    public WebFeedbackWidget() {
        super(TYPE_WEB);
    }

    /**
     * 设置url地址
     * @param url url地址
     * @return WebCallbackWidget WebWidget实例
     */
    public WebFeedbackWidget setUrl(String url) {
        return (WebFeedbackWidget) super.addContent(WIDGET_URL, url);
    }

    /**
     * 增加自定义参数的k,v对
     * @param key 键
     * @param value 值
     * @return WebCallbackWidget WebWidget实例
     */
    public WebFeedbackWidget addExtra(String key, String value) {
        return (WebFeedbackWidget) super.addExtra(key, value);
    }

}
