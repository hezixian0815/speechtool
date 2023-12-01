package com.aispeech.export.widget.callback;


import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 列表控件
 *
 * @author hehr
 */
public class ListCallbackWidget extends CallbackWidget {
    /**
     * 页面总数,必选
     */
    private int totalPages;
    /**
     * 每页显示条目数,必选
     */
    private int itemsPerPage;
    /**
     * 当前处于的页面数,必选
     */
    private int currentPage;
    /**
     * 列表项数据,必选
     */
    private JSONArray content;
    /**
     * 列表分词数据,非必选
     */
    private JSONArray segment;

    public ListCallbackWidget(JSONObject widget, int type, String skillId, String taskName, String intentName) {
        super(widget, type, skillId, taskName, intentName);
        setTotalPages(widget.optInt(TOTAL_PAGES));
        setCurrentPage(widget.optInt(CURRENT_PAGE));
        setItemsPerPage(widget.optInt(ITEMS_PERPAGE));
        setContent(widget.optJSONArray(CONTENT));
        setSegment(widget.optJSONArray(SEGMENT));
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getItemsPerPage() {
        return itemsPerPage;
    }

    public void setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public JSONArray getContent() {
        return content;
    }

    public void setContent(JSONArray content) {
        this.content = content;
    }

    public JSONArray getSegment() {
        return segment;
    }

    public void setSegment(JSONArray segment) {
        this.segment = segment;
    }

    /**
     * 页面总数,必选
     */
    public static final String TOTAL_PAGES = "totalPages";
    /**
     * 每页显示条目数,必选
     */
    public static final String ITEMS_PERPAGE = "itemsPerPage";
    /**
     * 当前处于的页面数,必选
     */
    public static final String CURRENT_PAGE = "currentPage";
    /**
     * 列表项数据,必选
     */
    public static final String CONTENT = "content";

    /**
     * 分词数据,非必选
     */
    public static final String SEGMENT = "segment";

}
