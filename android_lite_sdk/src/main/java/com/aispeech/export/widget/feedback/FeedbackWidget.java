package com.aispeech.export.widget.feedback;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FeedbackWidget {
    public static final String TYPE_TEXT = "text";
    public static final String TYPE_CONTENT = "content";
    public static final String TYPE_LIST = "list";
    public static final String TYPE_MEDIA = "media";
    public static final String TYPE_WEB = "web";

    private static final String WIDGET_TYPE = "type";
    private static final String WIDGET_TYPE_EX = "duiWidget";//FIXME: duiWidget is Deprecated
    private static final String WIDGET_COUNT = "count";
    private static final String WIDGET_CONTENT = "content";

    public static final String WIDGET_TEXT = "text";
    public static final String WIDGET_TITLE = "title";
    public static final String WIDGET_SUBTITLE = "subTitle";
    public static final String WIDGET_LABEL = "label";
    public static final String WIDGET_IMAGEURL = "imageUrl";
    public static final String WIDGET_LINKURL = "linkUrl";
    public static final String WIDGET_URL = "url";
    public static final String WIDGET_EXTRA = "extra";

    private static class WidgetMap<S, O> extends HashMap<String, Object> {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            Iterator<String> iterator = this.keySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                Object value = this.get(key);
                sb.append("\"");
                sb.append(key);
                sb.append("\":");
                if (value instanceof String) {
                    sb.append("\"");
                    sb.append(value);
                    sb.append("\"");
                } else {
                    sb.append(value);
                }
                if (iterator.hasNext()) {
                    sb.append(",");
                } else {
                    break;
                }
            }
            sb.append("}");
            return sb.toString();
        }
    }

    public static class WidgetArray<O> extends ArrayList<FeedbackWidget> {
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < this.size(); i++) {
                Object value = this.get(i);
                sb.append("{");
                sb.append(value);
                sb.append("}");
            }
            sb.append("]");
            return sb.toString();
        }
    }

    private Map<String, Object> mWidget = new WidgetMap<>();
    private Map<String, Object> mExtra = new WidgetMap<>();
    ArrayList<FeedbackWidget> mList = new WidgetArray<FeedbackWidget>();

    /**
     * DuiWidget构造方法
     *
     * @param type widget类型
     */
    public FeedbackWidget(String type) {
        mWidget.put(WIDGET_TYPE, type);
        mWidget.put(WIDGET_TYPE_EX, type);
    }

    /**
     * 设置widget类型
     *
     * @param type widget类型
     * @return FeedbackWidget DuiWidget实例
     */
    public FeedbackWidget setType(String type) {
        mWidget.put(WIDGET_TYPE, type);
        mWidget.put(WIDGET_TYPE_EX, type);
        return this;
    }

    /**
     * 设置k,v对
     *
     * @param contentName  键
     * @param contentValue 值
     * @return FeedbackWidget DuiWidget实例
     */
    public FeedbackWidget addContent(String contentName, String contentValue) {
        mWidget.put(contentName, contentValue);
        return this;
    }

    /**
     * 增加widget
     *
     * @param widget DuiWidget实例
     * @return FeedbackWidget DuiWidget实例
     */
    public FeedbackWidget addWidget(FeedbackWidget widget) {
        mList.add(widget);
        return this;
    }

    /**
     * 增加自定义参数的k,v对
     *
     * @param key   键
     * @param value 值
     * @return FeedbackWidget DuiWidget实例
     */
    public FeedbackWidget addExtra(String key, String value) {
        mExtra.put(key, value);
        return this;
    }

    /**
     * 增加自定义参数的k,v对
     *
     * @param key   键
     * @param value 值
     * @return FeedbackWidget DuiWidget实例
     */
    public FeedbackWidget addExtra(String key, Object value) {
        mExtra.put(key, value);
        return this;
    }

    /**
     * toString
     *
     * @return String String
     */
    @Override
    public String toString() {
        //insert extra
        if (mExtra.size() > 0) {
            mWidget.put(WIDGET_EXTRA, mExtra);
        }

        //insert sub-widget
        String type = (String) mWidget.get(WIDGET_TYPE);
        if (TYPE_LIST.equals(type) || TYPE_MEDIA.equals(type)) {
            ArrayList<FeedbackWidget> arr = new ArrayList<>(mList);
            mWidget.put(WIDGET_COUNT, arr.size());
            mWidget.put(WIDGET_CONTENT, arr);
        }
        return mWidget.toString();
    }
}
