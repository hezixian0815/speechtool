package com.aispeech.lite.dm.cache;

/**
 * 缓存识别结果
 *
 * @version 1.0
 * Created by BaoBao.Wang on 2021/8/2 19:26
 */
public class CacheLastAsrResult {

    private String topic;
    private String pinyin;
    private String recordId;
    private String sessionId;
    private int eof;
    private String requestId;
    private String text;

    public CacheLastAsrResult(String topic, String pinyin, String recordId,
                              String sessionId, int eof, String requestId, String text) {
        this.topic = topic;
        this.pinyin = pinyin;
        this.recordId = recordId;
        this.sessionId = sessionId;
        this.eof = eof;
        this.requestId = requestId;
        this.text = text;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPinyin() {
        return pinyin;
    }

    public void setPinyin(String pinyin) {
        this.pinyin = pinyin;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public int getEof() {
        return eof;
    }

    public void setEof(int eof) {
        this.eof = eof;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
