package com.aispeech.export.widget.feedback;

import java.util.ArrayList;

/**
 * 列表控件类,展示多条候选结果
 */

public class ListFeedbackWidget extends FeedbackWidget {
    ArrayList<ContentFeedbackWidget> mList = new ArrayList<>();

    /**
     * ListWidget构造方法
     */
    public ListFeedbackWidget() {
        super(TYPE_LIST);
    }

    /**
     * 增加列表项数据 ContentCallbackWidget
     * @param widget ContentWidget实例
     * @return ListCallbackWidget ListWidget实例
     */
    public ListFeedbackWidget addContentWidget(ContentFeedbackWidget widget) {
        return (ListFeedbackWidget) super.addWidget(widget);
    }

    /**
     * 增加自定义参数的k,v对
     * @param key 键
     * @param value 值
     * @return ListCallbackWidget ListWidget实例
     */
    public ListFeedbackWidget addExtra(String key, String value) {
        return (ListFeedbackWidget) super.addExtra(key, value);
    }

}
