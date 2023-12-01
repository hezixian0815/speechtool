package com.aispeech.net.dns;

/**
 *
 * @author yu
 * @date 2018/8/13
 */

public class DnsCache {
    public DnsCache(String ip, long createTime) {
        this.ip = ip;
        this.createTime = createTime;
    }
    private String ip;
    private long createTime;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
}
