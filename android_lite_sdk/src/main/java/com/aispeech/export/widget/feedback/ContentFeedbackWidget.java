package com.aispeech.export.widget.feedback;


/**
 * 内容控件类,图文并茂展示内容,如wikipedia
 */

public class ContentFeedbackWidget extends FeedbackWidget {

    /**
     * ContentWidget构造方法
     */
    public ContentFeedbackWidget() {
        super(TYPE_CONTENT);
    }

    /**
     * 设置标题
     * @param title title
     * @return ContentCallbackWidget ContentWidget实例
     */
    public ContentFeedbackWidget setTitle(String title) {
        return (ContentFeedbackWidget) super.addContent(WIDGET_TITLE, title);
    }

    /**
     * 设置副标题
     * @param subTitle 副标题
     * @return ContentCallbackWidget ContentWidget实例
     */
    public ContentFeedbackWidget setSubTitle(String subTitle) {
        return (ContentFeedbackWidget) super.addContent(WIDGET_SUBTITLE, subTitle);
    }

    /**
     * 设置标签数据，建议以 "," 作为分隔
     * @param label 标签数据
     * @return ContentCallbackWidget ContentWidget实例
     */
    public ContentFeedbackWidget setLabel(String label) {
        return (ContentFeedbackWidget) super.addContent(WIDGET_LABEL, label);
    }

    /**
     * 设置图片资源地址
     * @param imageUrl 图片资源地址
     * @return ContentCallbackWidget ContentWidget实例
     */
    public ContentFeedbackWidget setImageUrl(String imageUrl) {
        return (ContentFeedbackWidget) super.addContent(WIDGET_IMAGEURL, imageUrl);
    }

    /**
     * 设置跳转地址
     * @param linkUrl 跳转地址
     * @return ContentCallbackWidget ContentWidget实例
     */
    public ContentFeedbackWidget setLinkUrl(String linkUrl) {
        return (ContentFeedbackWidget) super.addContent(WIDGET_LINKURL, linkUrl);
    }

    /**
     * 增加自定义参数的k,v对
     * @param key 键
     * @param value 值
     * @return ContentCallbackWidget ContentWidget实例
     */
    public ContentFeedbackWidget addExtra(String key, String value) {
        return (ContentFeedbackWidget) super.addExtra(key, value);
    }


}
