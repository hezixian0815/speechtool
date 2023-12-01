package com.aispeech.export.config;


import android.text.TextUtils;

import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AuthType;

/**
 * 授权初始化配置
 *
 * @author hehr
 */
public class AuthConfig {

    /**
     * 设置联网授权的超时时间
     * 默认5s
     */
    private int authTimeout = 5000;
    /**
     * 授权服务地址
     */
    private String authServer = "https://auth.duiopen.com";

    /**
     * 授权文件保存目录的绝对路径<br>
     * <p>
     * 取值：绝对路径字符串, e.g./sdard/speech<br>
     * 默认存放目录 /data/data/包名/files/
     */
    private String deviceProfileDirPath;

    /**
     * assets目录下的离线授权文件名<br>
     * <p>
     * 取值：授权文件名
     * 是否必需：否<br>
     */
    private String offlineProfileName;

    /**
     * 当试用授权文件认证检查时，是否尝试更新为在线授权文件。default is true
     * <p>不进行联网更新授权文件才需要设置<br></p>
     */
    private boolean updateTrailProfileToOnlineProfile = true;


    /**
     * 自定义设置设备授权id
     * 离线授权方案需要设置该值,需要用户保证deviceId唯一性
     */
    private String customDeviceId;

    /**
     * 使用licenceId方案授权
     */
    private String licenceId;
    /**
     * 自定义设备唯一标识
     */
    private String customDeviceName;
    /**
     * 授权类型
     */
    private AuthType type = AuthType.ONLINE;

    /**
     * 设置是否支持sha1加密
     *
     * @return
     */
    private boolean encryptCustomDeviceName = false;

    public AuthType getType() {
        return type;
    }

    public String getCustomDeviceName() {
        return customDeviceName;
    }

    public int getAuthTimeout() {
        return authTimeout;
    }

    public String getAuthServer() {
        return authServer;
    }

    public String getDeviceProfileDirPath() {
        return deviceProfileDirPath;
    }

    public String getCustomDeviceId() {
        return customDeviceId;
    }

    public String getLicenceId() {
        return licenceId;
    }


    public String getOfflineProfileName() {
        return offlineProfileName;
    }

    public boolean isUpdateTrailProfileToOnlineProfile() {
        return updateTrailProfileToOnlineProfile;
    }

    public void setAuthTimeout(int authTimeout) {
        this.authTimeout = authTimeout;
    }

    public void setAuthServer(String authServer) {
        this.authServer = authServer;
    }

    public void setDeviceProfileDirPath(String deviceProfileDirPath) {
        this.deviceProfileDirPath = deviceProfileDirPath;
    }

    public void setOfflineProfileName(String offlineProfileName) {
        this.offlineProfileName = offlineProfileName;
    }

    public void setUpdateTrailProfileToOnlineProfile(boolean updateTrailProfileToOnlineProfile) {
        this.updateTrailProfileToOnlineProfile = updateTrailProfileToOnlineProfile;
    }

    public void setCustomDeviceId(String customDeviceId) {
        this.customDeviceId = customDeviceId;
    }

    public void setLicenceId(String licenceId) {
        this.licenceId = licenceId;
    }

    public void setCustomDeviceName(String customDeviceName) {
        this.customDeviceName = customDeviceName;
    }

    public void setType(AuthType type) {
        this.type = type;
    }

    public boolean encryptCustomDeviceName() {
        return encryptCustomDeviceName;
    }

    public static class Builder {

        private String customDeviceId;

        private int authTimeout = 5000;

        private String authServer = "https://auth.duiopen.com";

        private String deviceProfileDirPath;

        private String licenceId;

        private String customDeviceName;

        private AuthType type = AuthType.ONLINE;//默认在线鉴权

        private String offlineProfileName;

        private boolean updateTrailProfileToOnlineProfile = true;

        /**
         * 是否开启deviceName加密
         */
        private boolean encryptCustomDeviceName = false;

        /**
         * 设置授权类型
         *
         * @param type {@link AuthType}
         * @return {@link Builder}
         */
        public Builder setType(AuthType type) {
            this.type = type;
            return this;
        }

        /**
         * @param customDeviceId 自定义设置设备id,离线授权需要设置该值
         * @return {@link Builder}
         */
        @Deprecated
        public Builder setCustomDeviceId(String customDeviceId) {
            this.customDeviceId = customDeviceId;
            return this;
        }

        /**
         * 设置联网授权的超时时间,默认 5s
         *
         * @param authTimeout 超时时间
         * @return {@link Builder}
         */
        public Builder setAuthTimeout(int authTimeout) {
            this.authTimeout = authTimeout;
            return this;
        }

        /**
         * 设置授权服务地址,DEBUG授权使用,用户不建议使用
         *
         * @param authServer 授权服务地址,默认 "https://auth.duiopen.com"
         * 对应ip:
         * 47.111.106.49
         * 47.110.225.67
         * 47.111.81.103
         * 47.110.248.171
         * @return {@link Builder}
         */
        public Builder setAuthServer(String authServer) {
            this.authServer = authServer;
            return this;
        }

        /**
         * 授权文件保存目录的绝对路径,离线授权方案需要同时设置授权文件路径和自定义设备Id
         * 注意：若采用批量激活的方式，将大授权文件 “auth_profile.zip” 放在授权路径下：
         * 取值：绝对路径字符串, e.g./sdard/speech<br>
         * 默认存放目录 /data/data/包名/files/
         *
         * @param deviceProfileDirPath 绝对路径
         * @return {@link Builder}
         */
        public Builder setDeviceProfileDirPath(String deviceProfileDirPath) {
            this.deviceProfileDirPath = deviceProfileDirPath;
            return this;
        }

        /**
         * 设置licenceId
         *
         * @param licenceId licence id
         * @return {@link Builder}
         */
        public Builder setLicenceId(String licenceId) {
            this.licenceId = licenceId;
            return this;
        }

        /**
         * 设置deviceName
         *
         * @param customDeviceName 自定义设备唯一标识
         * @return {@link Builder}
         */
        public Builder setCustomDeviceName(String customDeviceName) {
            this.customDeviceName = customDeviceName;
            AISpeech.customDeviceName = customDeviceName;//初始化之前设置deviceName，用于日志工具初始化时传入，日志回捞时会根据设备id来回捞日志
            return this;
        }

        /**
         * assets目录下的离线授权文件名<br>
         * @param offlineProfileName 取值：授权文件名。是否必需：否
         * @return {@link Builder}
         */
        public Builder setOfflineProfileName(String offlineProfileName) {
            this.offlineProfileName = offlineProfileName;
            return this;
        }

        /**
         * 当试用授权文件认证检查时，是否尝试更新为在线授权文件。
         *
         * @param updateTrailProfileToOnlineProfile 不进行联网更新授权文件才需要设置，default is true
         * @return {@link Builder}
         */
        public Builder setUpdateTrailProfileToOnlineProfile(boolean updateTrailProfileToOnlineProfile) {
            this.updateTrailProfileToOnlineProfile = updateTrailProfileToOnlineProfile;
            return this;
        }

        /**
         * 设置是否需要对customDeviceName进行明文加密
         *
         * @return {@link Builder}
         */
        public Builder setEncryptCustomDeviceName(boolean encryptCustomDeviceName) {
            this.encryptCustomDeviceName = encryptCustomDeviceName;
            return this;
        }

        public AuthConfig create() {

            check();//check 授权参数

            AuthConfig config = new AuthConfig();
            config.type = type;
            config.customDeviceName = customDeviceName;
            config.customDeviceId = customDeviceId;
            config.authTimeout = authTimeout;
            config.authServer = authServer;
            config.deviceProfileDirPath = deviceProfileDirPath;
            config.offlineProfileName = offlineProfileName;
            config.updateTrailProfileToOnlineProfile = updateTrailProfileToOnlineProfile;
            config.licenceId = licenceId;
            config.encryptCustomDeviceName = encryptCustomDeviceName;
            return config;
        }

        /**
         * check  授权配置
         */
        public void check() {
            if (type == AuthType.TRIAL) {
                if (TextUtils.isEmpty(deviceProfileDirPath)) {
                    throw new IllegalArgumentException("offline auth must set deviceProfileDirPath.");
                }
            } else if (type == AuthType.OFFLINE) {
                if (TextUtils.isEmpty(deviceProfileDirPath) || TextUtils.isEmpty(customDeviceId)) {
                    throw new IllegalArgumentException("offline auth must set deviceProfileDirPath && customDeviceId .");
                }
            } else {
                if (TextUtils.isEmpty(customDeviceName)) {
                    throw new IllegalArgumentException("online auth must set customDeviceName .");
                }
            }
        }

    }


}
