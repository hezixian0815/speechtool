package com.aispeech.lite.vprintlite;

import android.text.TextUtils;

public class VprintLiteConfig {
    private String vprintResBin;
    private String vprintModelPath;
    private String vprintType = "";

    private VprintLiteConfig(Builder builder) {
        this.vprintResBin = builder.mVprintResBin;
        this.vprintModelPath = builder.mVprintModelPath;
        this.vprintType = builder.vprintType;

        if (TextUtils.isEmpty(vprintResBin)) {
            //必填参数声纹资源
            throw new IllegalArgumentException("Vprint config is invalid, lost vprintResBin");
        }
    }

    public String getVprintResBin() {
        return vprintResBin;
    }

    public String getVprintModelPath() {
        return vprintModelPath;
    }

    public String getVprintType() {
        return vprintType;
    }

    public static class Builder {
        private String mVprintResBin;
        private String mVprintModelPath;
        private String vprintType = "";

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
         * 设置声纹类型
         * @param vprintType 离线文本相关{@link com.aispeech.common.AIConstant#VPRINTLITE_TYPE_TD}
         *                   离线文本无关{@link com.aispeech.common.AIConstant#VPRINTLITE_TYPE_SR}
         *                   防欺诈模式{@link com.aispeech.common.AIConstant#VPRINTLITE_TYPE_ANTI_SPOOFING}
         * @return {@link Builder}
         */
        public Builder setVprintType(String vprintType) {
            this.vprintType = vprintType;
            return this;
        }

        public VprintLiteConfig create() {
            return new VprintLiteConfig(this);
        }
    }
}
