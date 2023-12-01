package com.aispeech.export.config;

import android.text.TextUtils;

public class AntiSpoofConfig {
    private String antiSpoofResBin;

    private AntiSpoofConfig(Builder builder) {
        this.antiSpoofResBin = builder.antiSpoofResBin;

        if (TextUtils.isEmpty(antiSpoofResBin)) {
            //必填参数声纹资源
            throw new IllegalArgumentException("antiSpoof config is invalid, lost antiSpoofResbin");
        }
    }

    public String getAntiSpoofResBin() {
        return antiSpoofResBin;
    }

    public static class Builder {
        private String antiSpoofResBin;

        /**
         * 设置仿冒攻击资源
         * 若在assets目录下，则指定文件名即可，如vprint.bin
         * 若在外部路径目录下，则需要指定绝对路径，如/sdcard/speech/vprint.bin
         *
         * @param antiSpoofResBin 仿冒攻击资源
         * @return {@link Builder}
         */
        public Builder setAntiSpoofResBin(String antiSpoofResBin) {
            this.antiSpoofResBin = antiSpoofResBin;
            return this;
        }

        public AntiSpoofConfig create() {
            return new AntiSpoofConfig(this);
        }
    }
}
