package com.aispeech.lite.vprint;

import android.text.TextUtils;

public class VprintConfig {
    private String vprintResBin;
    private String asrppResBin;
    private String vprintModelPath;
    private boolean useDatabaseStorage;
    private String vprintDatabasePath;

    private VprintConfig(Builder builder) {
        this.vprintResBin = builder.mVprintResBin;
        this.asrppResBin = builder.mAsrppResBin;
        this.vprintModelPath = builder.mVprintModelPath;
        this.useDatabaseStorage = builder.useDatabaseStorage;
        this.vprintDatabasePath = builder.vprintDatabasePath;
        if (TextUtils.isEmpty(vprintResBin)) {
            //必填参数声纹资源
            throw new IllegalArgumentException("Vprint config is invalid, lost vprintResBin");
        }
        if (TextUtils.isEmpty(asrppResBin)) {
            //必填参数asr++资源
            throw new IllegalArgumentException("Vprint config is invalid, lost asrppResBin");
        }
        if (!this.useDatabaseStorage && TextUtils.isEmpty(vprintModelPath)) {
            //必填参数声纹模型保存绝对路径
            throw new IllegalArgumentException("Vprint config is invalid, lost vprintModelPath");
        }
        if (this.useDatabaseStorage && TextUtils.isEmpty(this.vprintDatabasePath)) {
            throw new IllegalArgumentException("Vprint config is invalid, lost vprintDatabasePath");
        }
    }

    public String getVprintResBin() {
        return vprintResBin;
    }

    public String getAsrppResBin() {
        return asrppResBin;
    }

    public String getVprintModelPath() {
        return vprintModelPath;
    }

    public boolean isUseDatabaseStorage() {
        return useDatabaseStorage;
    }

    public String getVprintDatabasePath() {
        return vprintDatabasePath;
    }

    @Override
    public String toString() {
        return "VprintConfig{" +
                "vprintResBin='" + vprintResBin + '\'' +
                ", asrppResBin='" + asrppResBin + '\'' +
                ", vprintModelPath='" + vprintModelPath + '\'' +
                ", useDatabaseStorage=" + useDatabaseStorage +
                ", vprintDatabasePath='" + vprintDatabasePath + '\'' +
                '}';
    }

    public static class Builder {
        private String mVprintResBin;
        private String mAsrppResBin;
        private String mVprintModelPath;
        private boolean useDatabaseStorage = false;
        private String vprintDatabasePath = null;

        /**
         * 设置声纹资源
         * 若在assets目录下，则指定文件名即可，如vprint.bin
         * 若在外部路径目录下，则需要指定绝对路径，如/sdcard/speech/vprint.bin
         *
         * @param vprintResBin 声纹资源
         * @return {@link Builder}
         */
        public Builder setVprintResBin(String vprintResBin) {
            this.mVprintResBin = vprintResBin;
            return this;
        }

        /**
         * 设置asr++资源
         * 若在assets目录下，则指定文件名即可，如asrpp.bin
         * 若在外部路径目录下，则需要指定绝对路径，如/sdcard/speech/asrpp.bin
         *
         * @param asrppResBin asr++资源
         * @return {@link Builder}
         */
        public Builder setAsrppResBin(String asrppResBin) {
            this.mAsrppResBin = asrppResBin;
            return this;
        }

        /**
         * 设置声纹模型保存路径，包含文件名，如/sdcard/speech/vprint.model
         *
         * @param vprintModelPath 声纹模型保存路径
         * @return {@link Builder}
         */
        public Builder setVprintModelPath(String vprintModelPath) {
            this.mVprintModelPath = vprintModelPath;
            return this;
        }

        /**
         * 设置是否使用数据库方式存储声纹信息，以及数据库的路径
         * {@code
         * // 也直接查询数据库里的数据，可以和云端等其它地方的声纹数据合并等操作
         * VprintDatabaseManager dbManager = new VprintDatabaseManager("/sdcard/speech/vprint.db");
         * Log.d(TAG, "dbManager.queryAll() " + dbManager.queryAll().toString());
         * dbManager.close();
         * }
         *
         * @param useDatabaseStorage 是否使用数据库存储声纹信息的方式，默认为false
         * @param vprintDatabasePath 数据库路径，例如 "/sdcard/speech/vprint.db"
         * @return {@link Builder}
         */
        public Builder setUseDatabaseStorage(boolean useDatabaseStorage, String vprintDatabasePath) {
            this.useDatabaseStorage = useDatabaseStorage;
            this.vprintDatabasePath = vprintDatabasePath;
            return this;
        }

        public VprintConfig create() {
            return new VprintConfig(this);
        }
    }
}
