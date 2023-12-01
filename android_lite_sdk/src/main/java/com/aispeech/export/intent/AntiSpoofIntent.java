package com.aispeech.export.intent;

public class AntiSpoofIntent {
    private String antiSpoofSaveDir;
    private long resStart;
    private long resEnd;
    private String speechState = "speech";

    private AntiSpoofIntent(Builder builder) throws IllegalArgumentException {
        this.antiSpoofSaveDir = builder.antiSpoofSaveDir;
        this.resStart = builder.resStart;
        this.resEnd = builder.resEnd;
    }

    public long getResStart() {
        return resStart;
    }

    /**
     * 识别结果开始的数据点
     *
     * @param resStart 识别结果开始的字段位置
     */
    public void setResStart(long resStart) {
        this.resStart = resStart;
    }

    public long getResEnd() {
        return resEnd;
    }

    /**
     * 当前音频结束的位置
     *
     * @param resEnd 音频结束的位置
     */
    public void setResEnd(long resEnd) {
        this.resEnd = resEnd;
    }

    public String getSpeechState() {
        return speechState;
    }

    /**
     * 用于标识当前feed的音频是静音段还是有人声的
     * 其中，静音段传入 silence，人声段传入 speech
     *
     * @param speechState 声音段状态
     */
    public void setSpeechState(String speechState) {
        this.speechState = speechState;
    }

    public String getAntiSpoofSaveDir() {
        return antiSpoofSaveDir;
    }

    /**
     * 设置保存仿冒攻击的音频数据
     *
     * @param antiSpoofSaveDir feed 给仿冒攻击内核的音频数据保存的文件夹路径
     */
    public void setAntiSpoofSaveDir(String antiSpoofSaveDir) {
        this.antiSpoofSaveDir = antiSpoofSaveDir;
    }

    @Override
    public String toString() {
        return "VprintIntent{" +
                ", antiSpoofSaveDir='" + antiSpoofSaveDir + '\'' +
                '}';
    }

    public static class Builder {
        private String antiSpoofSaveDir;
        private long resStart;
        private long resEnd;

        /**
         * 识别结果开始的数据点
         *
         * @param resStart 识别结果开始的字段位置
         * @return {@link Builder}
         */
        public Builder setResStart(long resStart) {
            this.resStart = resStart;
            return this;
        }

        /**
         * 识别结果结束的数据点
         *
         * @param resEnd 识别结果结束的字段位置
         * @return {@link Builder}
         */
        public Builder setResEnd(long resEnd) {
            this.resEnd = resEnd;
            return this;
        }

        /**
         * 设置保存仿冒攻击的音频数据路径
         *
         * @param antiSpoofSaveDir feed 给仿冒攻击内核的数据保存的文件夹路径
         * @return {@link Builder}
         */
        public Builder setAntiSpoofSaveDir(String antiSpoofSaveDir) {
            this.antiSpoofSaveDir = antiSpoofSaveDir;
            return this;
        }

        public AntiSpoofIntent create() throws IllegalArgumentException {
            return new AntiSpoofIntent(this);
        }
    }
}
