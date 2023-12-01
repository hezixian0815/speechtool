package com.aispeech.export.intent;

import java.util.List;

public class AIAsrPlusIntent {

    /**
     * 声纹服务器地址
     */
    private String serverName;

    /**
     * 注册公司 用户所在的公司，项目
     */
    private String organization;

    /**
     * 注册用户：要验证的 userId 列表，userId 即注册声纹时的 userId
     */
    private List<String> users;

    /**
     * 云端VAD
     */
    private boolean cloudVprintVadEnable = true;


    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public boolean isCloudVprintVadEnable() {
        return cloudVprintVadEnable;
    }

    public void setCloudVprintVadEnable(boolean cloudVprintVadEnable) {
        this.cloudVprintVadEnable = cloudVprintVadEnable;
    }

}
