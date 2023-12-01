package com.aispeech.export.widget.feedback;


/**
 * 媒体控件类,用于展示多媒体信息
 */
public class MediaFeedbackWidget extends FeedbackWidget {

    /**
     * MediaWidget实例
     */
    public MediaFeedbackWidget() {
        super(TYPE_MEDIA);
    }

    /**
     * 增加列表项数据 ContentCallbackWidget
     * @param widget ContentWidget实例
     * @return MediaCallbackWidget MediaWidget实例
     */
    public MediaFeedbackWidget addContentWidget(ContentFeedbackWidget widget) {
        return (MediaFeedbackWidget) super.addWidget(widget);
    }

    /**
     * 增加自定义参数的k,v对
     * @param key 键
     * @param value 值
     * @return MediaCallbackWidget MediaWidget实例
     */
    public MediaFeedbackWidget addExtra(String key, String value) {
        return (MediaFeedbackWidget) super.addExtra(key, value);
    }

}
