package com.aispeech.export.widget.callback;

/**
 * @author hehr
 * widget type
 */
public enum CallbackWidgetType {
    /**
     * 文本控件,用来显示文本卡片
     */
    TEXT(0, "text"),
    /**
     * 列表控件,展示多条候选结果
     */
    LIST(1, "list"),
    /**
     * 内容控件,图文并茂展示内容,如wikipedia
     */
    CONTENT(2, "content"),
    /**
     * Web控件,展示富文本内容，如：天气/股票等
     */
    WEB(3, "web"),
    /**
     * 媒体控件,用于展示多媒体信息
     */
    MEDIA(4, "media"),
    /**
     * 自定义控件
     */
    CUSTOM(5, "custom"),
    ;

    CallbackWidgetType(int type, String name) {
        this.type = type;
        this.name = name;
    }

    private int type;

    private String name;

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public static CallbackWidgetType getWidgetTypeByInt(int type) {
        for (CallbackWidgetType t :
                values()) {
            if (t.getType() == type) {
                return t;
            }
        }
        return null;
    }
}
