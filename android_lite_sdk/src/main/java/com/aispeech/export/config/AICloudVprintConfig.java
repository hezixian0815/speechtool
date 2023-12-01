package com.aispeech.export.config;

public class AICloudVprintConfig {
    public enum Mode {
        /**
         * 离线一句话文本无关,适用于自由文本的一句话离线说话人识别场景。
         */
        TEXT_OFFLINE_NO_RELATED("ti-sr", "离线一句话文本无关"),
        /**
         * 离线文本半相关,适用于数字串，固定文本（唤醒词），固定文本（唤醒词）+数字串的一句话离线说话人识别场景。
         */
        TEXT_OFFLINE_HALF_RELATED("dp-sr", "离线文本半相关"),
        /**
         * 实时短语音文本无关,适用于问答形式的场景下，说话人身份的确认。可以有效拒绝非注册人，但不支持一个vad片段内多个说话人的分离。（如多轮对话场景）。
         */
        TEXT_NO_RELATED_SHORT_TIME("sti-sr", "实时短语音文本无关");
        /**
         * 实时长语音文本无关,适用于多说话人轮流交替说话的场景下的说话人分离。不可以有效拒绝非注册人，支持一个vad片段内多个说话人的分离。（如会议转写场景）。
         */
//        TEXT_NO_RELATED_LONG_TIME("lti-sr", "实时长语音文本无关");


        private final String value;
        private final String desc;

        Mode(String value, String desc) {
            this.value = value;
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }

        public String getValue() {
            return value;
        }

        /**
         * 当前模式下支持的采样率
         *
         * @return 8000 or 16000
         */
        public int getSupportSampleRate() {
//            return this == Mode.TEXT_OFFLINE_NO_RELATED ? 8000 : 16000;
            return 16000;
        }

        /**
         * 是否支持 Http 方式进行声纹验证
         *
         * @return true 支持Http方式验证，false 不支持Http方式验证
         */
        public boolean isHttpVerify() {
            return this == TEXT_OFFLINE_NO_RELATED || this == TEXT_OFFLINE_HALF_RELATED;
        }

        /**
         * 是否支持 WebSocket 方式进行声纹验证
         *
         * @return 支持 WebSocket 方式验证，false 不支持 WebSocket 方式验证
         */
        public boolean isWebSocketVerify() {
            return !isHttpVerify();
        }

        @Override
        public String toString() {
            return desc + ":" + value;
        }
    }

    private Mode mode = Mode.TEXT_OFFLINE_HALF_RELATED;
    private String host = "https://asr.dui.ai";

    public Mode getMode() {
        return mode;
    }

    /**
     * 注册声纹的类型, default is 文本半相关(数字串）（dp-sr）
     *
     * @param mode 模式
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public boolean isModTextRelated(Mode mode) {
        return mode == Mode.TEXT_OFFLINE_HALF_RELATED;
    }

    public boolean isModeTextNoRelated(Mode mode) {
        return mode == Mode.TEXT_NO_RELATED_SHORT_TIME
                || mode == Mode.TEXT_OFFLINE_NO_RELATED;
    }

    @Override
    public String toString() {
        return "AICloudVprintConfig{" +
                "mode=" + mode +
                ", host='" + host + '\'' +
                '}';
    }
}
