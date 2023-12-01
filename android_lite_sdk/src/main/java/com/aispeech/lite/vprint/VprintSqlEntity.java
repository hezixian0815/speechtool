package com.aispeech.lite.vprint;

/**
 * 声纹信息数据库存储实体类
 */
public class VprintSqlEntity {

    /**
     * 声纹的id，就是用户名+唤醒词，例如："小明你好小驰"
     */
    private String id;
    /**
     * 声纹数据
     */
    private byte[] data;
    /**
     * 时间戳，精确到毫秒
     */
    private long timestamp;

    public VprintSqlEntity() {
    }

    public VprintSqlEntity(String id, byte[] data, long timestamp) {
        this.id = id;
        this.data = data;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    /**
     * 声纹的id，就是用户名+唤醒词，例如："小明你好小驰"
     *
     * @param id 声纹的id
     */
    public void setId(String id) {
        this.id = id;
    }

    public byte[] getData() {
        return data;
    }

    /**
     * 声纹数据
     *
     * @param data 声纹数据
     */
    public void setData(byte[] data) {
        this.data = data;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /**
     * 时间戳，精确到毫秒
     *
     * @param timestamp 时间戳
     */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 刷新时间戳
     */
    public void refreshTimestamp() {
        this.timestamp = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        return "VprintSqlEntity{" +
                "id='" + id + '\'' +
                ", data.length=" + (data != null ? data.length : 0) +
                ", timestamp=" + timestamp +
                '}';
    }
}
