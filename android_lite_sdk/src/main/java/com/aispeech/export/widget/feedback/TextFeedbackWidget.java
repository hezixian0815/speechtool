package com.aispeech.export.widget.feedback;

/**
 * 文本控件类, 用来显示文本卡片
 */

public class TextFeedbackWidget extends FeedbackWidget {

    /**
     * TextWidget构造方法
     */
    public TextFeedbackWidget() {
        super(TYPE_TEXT);
    }

    /**
     * 设置输出文本
     * @param text 输出文本
     * @return TextCallbackWidget TextWidget实例
     */
    public TextFeedbackWidget setText(String text) {
        return (TextFeedbackWidget) super.addContent(WIDGET_TEXT, text);
    }

    /**
     * 增加自定义参数的k,v对
     * @param key 键
     * @param value 值
     * @return TextCallbackWidget TextWidget实例
     */
    public TextFeedbackWidget addExtra(String key, String value) {
        return (TextFeedbackWidget) super.addExtra(key, value);
    }

}
