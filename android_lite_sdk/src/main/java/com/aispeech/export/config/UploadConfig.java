package com.aispeech.export.config;


import com.aispeech.common.Log;

import java.io.File;

/**
 * 日志上传初始化配置
 *
 * @author hehr
 */
public class UploadConfig {

    /**
     * 离线状态下最大缓存文件数据个数, default is 100<br>
     */
    private int cacheUploadMaxNumber;

    /**
     * 设置 sdk 缓存唤醒、预唤醒音频文件，默认不缓存
     */
    private boolean cacheUploadEnable = false; //是否开启音频缓存，默认不开启

    /**
     * 上传音频等级, default is {@link #UPLOAD_AUDIO_LEVEL_NONE}<br>
     * 音频都不上传{@link #UPLOAD_AUDIO_LEVEL_NONE}<br>
     * 只上传唤醒音频{@link #UPLOAD_AUDIO_LEVEL_WAKEUP}<br>
     * 只上传预唤醒音频{@link #UPLOAD_AUDIO_LEVEL_PREWAKEUP}<br>
     * 上传唤醒和预唤醒音频{@link #UPLOAD_AUDIO_LEVEL_ALL}<br>
     */
    private int uploadAudioLevel;


    /**
     * 上传大数据的唤醒音频保存的路径
     */
    private String uploadAudioPath;

    /**
     * 上传唤醒音频的延迟时间，单位:毫秒，default is {@value}毫秒 <br>
     * 0表示不延迟上传，即收到唤醒音频后立即上传。&gt;0 时延迟有效，建议在1分钟到10分钟内<br>
     * 前置条件：{@link #uploadAudioLevel} <strong>不能为</strong> {@link #UPLOAD_AUDIO_LEVEL_NONE}
     */
    private int uploadAudioDelayTime;

    /**
     * 设置大数据上传url地址 <br>
     * <p>
     * 取值：字符串<br>
     * 是否必需：否<br>
     * 默认值：https://log.aispeech.com<br>
     */
    private String uploadUrl;

    public UploadConfig() {
    }

    public void setCacheUploadMaxNumber(int cacheUploadMaxNumber) {
        this.cacheUploadMaxNumber = cacheUploadMaxNumber;
    }

    public void setCacheUploadEnable(boolean cacheUploadEnable) {
        this.cacheUploadEnable = cacheUploadEnable;
    }

    public void setUploadAudioLevel(int uploadAudioLevel) {
        this.uploadAudioLevel = uploadAudioLevel;
    }

    public void setUploadAudioPath(String uploadAudioPath) {
        this.uploadAudioPath = uploadAudioPath;
    }

    public void setUploadAudioDelayTime(int uploadAudioDelayTime) {
        this.uploadAudioDelayTime = uploadAudioDelayTime;
    }

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public void setUploadEnable(boolean uploadEnable) {
        this.uploadEnable = uploadEnable;
    }

    /**
     * 设置 SDK信息（音频）是否上传，默认不上传
     */
    private boolean uploadEnable;

    /**
     * 设置 SDK信息（日志）是否上传，默认不上传
     */
    private boolean uploadLogEnable;

    /**
     * 设置上传音频时常，单位是ms，默认是5s
     */
    private int uploadAudioTime = 6 * 1000;

    /**
     * 单个上传的音频文件的最大长度，默认10M
     */
    private long uploadAudioMaxLength = 10 * 1024 * 1024;

    public int getCacheUploadMaxNumber() {
        return cacheUploadMaxNumber;
    }

    public int getUploadAudioLevel() {
        return uploadAudioLevel;
    }

    public String getUploadAudioPath() {
        if (uploadAudioPath != null && uploadAudioPath.length() > 1
                && uploadAudioPath.lastIndexOf(File.separator) == uploadAudioPath.length() - 1) {
            uploadAudioPath = uploadAudioPath.substring(0, uploadAudioPath.length() - 1);
            Log.d("DUILiteSDK", "del last separator: " + uploadAudioPath);
        }
        return uploadAudioPath;
    }

    public int getUploadAudioDelayTime() {
        return uploadAudioDelayTime;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public boolean isUploadEnable() {
        return uploadEnable;
    }

    public boolean isUploadLogEnable() {
        return uploadLogEnable;
    }

    public boolean isCacheUploadEnable() {
        return cacheUploadEnable;
    }

    public int getUploadAudioTime() {
        return uploadAudioTime;
    }

    public long getUploadAudioMaxLength() {
        return uploadAudioMaxLength;
    }

    private UploadConfig(boolean uploadEnable, int cacheUploadMaxNumber, int uploadAudioLevel,
                         String uploadAudioPath, int uploadAudioDelayTime, String uploadUrl,
                         boolean cacheUploadEnable, int uploadAudioTime, boolean uploadLogEnable,
                         long uploadAudioMaxLength) {
        this.uploadEnable = uploadEnable;
        this.uploadLogEnable = uploadLogEnable;
        this.cacheUploadMaxNumber = cacheUploadMaxNumber;
        this.uploadAudioLevel = uploadAudioLevel;
        this.uploadAudioPath = uploadAudioPath;
        this.uploadAudioDelayTime = uploadAudioDelayTime;
        this.uploadUrl = uploadUrl;
        this.cacheUploadEnable = cacheUploadEnable;
        this.uploadAudioTime = uploadAudioTime;
        this.uploadAudioMaxLength = uploadAudioMaxLength;
    }

    private UploadConfig(Builder builder) {
        this(builder.uploadEnable, builder.cacheUploadMaxNumber, builder.uploadAudioLevel,
                builder.uploadAudioPath, builder.uploadAudioDelayTime, builder.uploadUrl,
                builder.cacheUploadEnable, builder.uploadAudioTime,
                builder.uploadLogEnable, builder.uploadAudioMaxLength);
    }

    /**
     * 音频都不上传
     */
    public static final int UPLOAD_AUDIO_LEVEL_NONE = 0x00;
    /**
     * 只上传预唤醒音频
     */
    public static final byte UPLOAD_AUDIO_LEVEL_PREWAKEUP = 0x01;
    /**
     * 只上传唤醒音频
     */
    public static final int UPLOAD_AUDIO_LEVEL_WAKEUP = 0x10;
    /**
     * 上传唤醒和预唤醒音频
     */
    public static final int UPLOAD_AUDIO_LEVEL_ALL = 0x11;


    public static class Builder {

        /**
         * 设置 SDK信息（音频）是否上传，默认不上传
         */
        private boolean uploadEnable = true;//车载的音频收集，默认关闭

        /**
         * 设置SDK信息（日志）是否上传，默认不上传
         */
        private boolean uploadLogEnable = false;//车载的日志收集，默认关闭

        /**
         * 设置 sdk 缓存唤醒、预唤醒音频文件，默认不缓存
         */
        private boolean cacheUploadEnable = false; //是否开启音频缓存，默认不开启

        private int cacheUploadMaxNumber = 100;

        private int uploadAudioLevel = UPLOAD_AUDIO_LEVEL_NONE;

        private String uploadAudioPath = null;

        private int uploadAudioDelayTime = 5 * 60 * 1000;

        private String uploadUrl = "https://log.aispeech.com";

        /**
         * 设置上传音频时常，单位是ms，默认是6s
         */
        private int uploadAudioTime = 6 * 1000;
        /**
         * 单个上传的音频文件的最大长度，默认10M
         */
        private long uploadAudioMaxLength = 10 * 1024 * 1024;


        /**
         * @param uploadLogEnable boolean 设置 SDK信息（异常监控信息）是否上传,默认false
         * @return {@link Builder}
         */
        public Builder setUploadLogEnable(boolean uploadLogEnable) {
            this.uploadLogEnable = uploadLogEnable;
            return this;
        }

        /**
         * 设置单个上传的音频文件的最大长度，默认10M
         *
         * @param uploadAudioMaxLength
         * @return
         */
        public Builder setUploadAudioMaxLength(long uploadAudioMaxLength) {
            this.uploadAudioMaxLength = uploadAudioMaxLength;
            return this;
        }

        /**
         * @param uploadEnable boolean 设置 SDK信息（异常监控信息/音频）是否上传,默认true
         * @return {@link Builder}
         */
        public Builder setUploadEnable(boolean uploadEnable) {
            this.uploadEnable = uploadEnable;
            return this;
        }

        /**
         * @param cacheUploadEnable boolean 设置 sdk 缓存唤醒、预唤醒音频文件，默认不缓存
         * @return {@link Builder}
         */
        public Builder setCacheUploadEnable(boolean cacheUploadEnable) {
            this.cacheUploadEnable = cacheUploadEnable;
            return this;
        }

        /**
         * @param cacheUploadMaxNumber 离线状态下最大缓存文件数据个数, default is 100<br>
         * @return {@link Builder}
         */
        public Builder setCacheUploadMaxNumber(int cacheUploadMaxNumber) {
            this.cacheUploadMaxNumber = cacheUploadMaxNumber;
            return this;
        }

        /**
         * @param uploadAudioLevel 上传音频等级, default is {@link #UPLOAD_AUDIO_LEVEL_NONE}<br>
         *                         音频都不上传{@link #UPLOAD_AUDIO_LEVEL_NONE}<br>
         *                         只上传唤醒音频{@link #UPLOAD_AUDIO_LEVEL_WAKEUP}<br>
         *                         只上传预唤醒音频{@link #UPLOAD_AUDIO_LEVEL_PREWAKEUP}<br>
         *                         上传唤醒和预唤醒音频{@link #UPLOAD_AUDIO_LEVEL_ALL}<br>
         * @return {@link Builder}
         */
        public Builder setUploadAudioLevel(int uploadAudioLevel) {
            this.uploadAudioLevel = uploadAudioLevel;
            return this;
        }

        /**
         * @param uploadAudioPath 上传大数据的唤醒音频保存的路径
         * @return {@link Builder}
         */
        public Builder setUploadAudioPath(String uploadAudioPath) {
            this.uploadAudioPath = uploadAudioPath;
            return this;
        }

        /**
         * @param uploadAudioDelayTime 上传唤醒音频的延迟时间，单位:毫秒<br>
         *                             前置条件：{@link #uploadAudioLevel} <strong>不能为</strong> {@link #UPLOAD_AUDIO_LEVEL_NONE}
         * @return {@link Builder}
         */
        public Builder setUploadAudioDelayTime(int uploadAudioDelayTime) {
            this.uploadAudioDelayTime = uploadAudioDelayTime;
            return this;
        }

        /**
         * @param uploadUrl 设置大数据上传url地址 <br>
         *                  <p>
         *                  取值：字符串<br>
         *                  是否必需：否<br>
         *                  默认值：https://log.aispeech.com<br>
         * @return {@link Builder}
         */
        public Builder setUploadUrl(String uploadUrl) {
            this.uploadUrl = uploadUrl;
            return this;
        }

        /**
         * @param uploadAudioTime 设置上传的唤醒音频时长 <br>
         *                        <p>
         *                        取值：int<br>
         *                        是否必需：否<br>
         *                        默认值：5000ms<br>
         * @return {@link Builder}
         */
        public Builder setUploadAudioTime(int uploadAudioTime) {
            this.uploadAudioTime = uploadAudioTime;
            return this;
        }

        public UploadConfig create() {
            return new UploadConfig(this);
        }
    }


}

