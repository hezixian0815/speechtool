package com.aispeech.export.widget.callback;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 内容控件
 *
 * @author hehr
 */
public class ContentCallbackWidget extends CallbackWidget {
    /**
     * 标题,必选
     */
    private String title;
    /**
     * 副标题,非必选
     */
    private String subTitle;
    /**
     * 图片资源地址,非必选
     */
    private String imageUrl;

    /**
     * 标签数据，建议以 "," 作为分隔,非必选
     */
    private String label;
    /**
     * 跳转地址,非必选
     */
    private String linkUrl;

    /**
     * 按钮数据项, 作为按钮上的显示文案以及回传数据,非必选
     */
    private JSONArray buttons;

    public ContentCallbackWidget(JSONObject widget, int type, String skillId, String taskName, String intentName) {
        super(widget, type, skillId, taskName, intentName);
        setTitle(widget.optString(TITLE));
        setSubTitle(widget.optString(SUB_TITLE));
        setImageUrl(widget.optString(IMAGE_URL));
        setLabel(widget.optString(LABEL));
        setLinkUrl(widget.optString(LINK_URL));
        setButtons(widget.optJSONArray(BUTTONS));
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLabel() {
        return label;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public JSONArray getButtons() {
        return buttons;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public void setButtons(JSONArray buttons) {
        this.buttons = buttons;
    }

    /**
     * 标题
     */
    public static final String TITLE = "title";
    /**
     * 副标题
     */
    public static final String SUB_TITLE = "subTitle";
    /**
     * 图片资源地址
     */
    public static final String IMAGE_URL = "imageUrl";
    /**
     * 标签数据
     */
    public static final String LABEL = "label";
    /**
     * 跳转地址
     */
    public static final String LINK_URL = "linkUrl";
    /**
     * 按钮数据项
     */
    public static final String BUTTONS = "buttons";


}
