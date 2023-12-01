package com.aispeech.lite.vprintlite;

public class VprintLiteIntent {
    private Action action;
    private String vprintLiteSaveDir;
    private String uId;
    private int asrErrorRate;
    private String constantContent;
    private String customContent;
    private boolean enhanceRegister;
    private int topN;
    private String recWords;
    private long resStart;
    private long resEnd;
    private String speechState = "speech";

    public String getuId() {
        return uId;
    }

    public int getAsrErrorRate() {
        return asrErrorRate;
    }

    public String getConstantContent() {
        return constantContent;
    }

    public String getCustomContent() {
        return customContent;
    }

    public boolean isEnhanceRegister() {
        return enhanceRegister;
    }

    public int getTopN() {
        return topN;
    }

    public String getRecWords() {
        return recWords;
    }

    public long getResStart() {
        return resStart;
    }

    public long getResEnd() {
        return resEnd;
    }

    public Action getAction() {
        return action;
    }

    /**
     * 设置声纹工作模式,
     * 若不设置，会抛{@link IllegalArgumentException}异常
     *
     * @param action {@link Action}
     */
    public void setAction(Action action) {
        this.action = action;
    }

    /**
     * 设置保存唤醒内核给声纹的音频数据
     *
     * @param vprintLiteSaveDir feed 给声纹内核的音频数据保存的文件夹路径
     */
    public void setVprintLiteSaveDir(String vprintLiteSaveDir) {
        this.vprintLiteSaveDir = vprintLiteSaveDir;
    }

    /**
     * 设置声纹ID
     *
     * @param uId 声纹ID
     */
    public void setuId(String uId) {
        this.uId = uId;
    }

    /**
     * 设置文本半相关语音文本结果和custom_context的字错误率的百分比阈值 [0,100]，字错误率大于阈值会报错
     *
     * @param asrErrorRate 可接受错误率百分比，默认值0
     */
    public void setAsrErrorRate(int asrErrorRate) {
        this.asrErrorRate = asrErrorRate;
    }

    /**
     * 标记语音中已知的固定的文本内容（如唤醒词），算法会特殊处理被标记的音频。
     *
     * @param constantContent 固定的文本内容
     */
    public void setConstantContent(String constantContent) {
        this.constantContent = constantContent;
    }

    /**
     * 文本（半）相关时输入的语音文本。
     *
     * @param customContent 语音文本
     */
    public void setCustomContent(String customContent) {
        this.customContent = customContent;
    }

    /**
     * 注册增强，仅用于声纹注册
     *
     * @param enhanceRegister 是否使用注册增强，默认false
     */
    public void setEnhanceRegister(boolean enhanceRegister) {
        this.enhanceRegister = enhanceRegister;
    }

    /**
     * 1:N按照score排序,输出前n个得分,若不配置该选项,会在result字段中返回所有得分,开启该选项后会在topN字段中返回得分排名靠前的最多N个得分
     *
     * @param topN 前n个得分
     */
    public void setTopN(int topN) {
        this.topN = topN;
    }

    /**
     * 识别结果送声纹，用以和{@link #customContent}做对比
     *
     * @param recWords 识别需要送声纹的文本
     */
    public void setRecWords(String recWords) {
        this.recWords = recWords;
    }

    /**
     * 识别结果开始的数据点
     *
     * @param resStart 识别结果开始的字段位置
     */
    public void setResStart(long resStart) {
        this.resStart = resStart;
    }

    /**
     * 识别结果结束的数据点
     *
     * @param resEnd 识别结果结束的字段位置
     */
    public void setResEnd(long resEnd) {
        this.resEnd = resEnd;
    }

    public String getSpeechState() {
        return speechState;
    }

    /**
     * {@link com.aispeech.common.AIConstant#VPRINTLITE_TYPE_SR} 用于标识当前feed的音频是静音段还是有人声的
     * 其中，静音段传入 silence，人声段传入 speech
     * @param speechState 声音段状态
     */
    public void setSpeechState(String speechState) {
        this.speechState = speechState;
    }

    public enum Action {

        /**
         * 注册模式
         */
        REGISTER("register"),

        /**
         * 删除模式(删除模型中某条记录)
         */
        UNREGISTER("unregister"),

        /**
         * 验证模式
         */
        VERIFY("verify");

        private String value;

        Action(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

        public static Action getActionByValue(String value) {
            for (Action msg : Action.values()) {
                if (value.equals(msg.value)) {
                    return msg;
                }
            }
            return null;
        }
    }

    private VprintLiteIntent(Builder builder) throws IllegalArgumentException {
        this.action = builder.mAction;
        this.vprintLiteSaveDir = builder.mVprintLiteSaveDir;
        this.asrErrorRate = builder.asrErrorRate;
        this.constantContent = builder.constantContent;
        this.customContent = builder.customContent;
        this.enhanceRegister = builder.enhanceRegister;
        this.topN = builder.topN;
        this.recWords = builder.recWords;
        this.resStart = builder.resStart;
        this.resEnd = builder.resEnd;
        this.uId = builder.uId;
        this.speechState = builder.speechState;
        //check argument valid
        if (action == null) {
            throw new IllegalArgumentException("Vprint intent is invalid, lost action");
        }
    }

    public String getVprintLiteSaveDir() {
        return vprintLiteSaveDir;
    }

    @Override
    public String toString() {
        return "VprintIntent{" +
                ", action=" + action +
                ", vprintLiteSaveDir='" + vprintLiteSaveDir + '\'' +
                '}';
    }

    public static class Builder {
        private Action mAction;
        private String mVprintLiteSaveDir;
        private int asrErrorRate;
        private String constantContent;
        private String customContent;
        private boolean enhanceRegister;
        private int topN;
        private String recWords;
        private long resStart;
        private long resEnd;
        private String uId;
        private String speechState;

        /**
         * 设置声纹ID
         *
         * @param uId 声纹ID
         * @return {@link Builder}
         */
        public Builder setUid(String uId) {
            this.uId = uId;
            return this;
        }

        /**
         * 设置文本半相关语音文本结果和custom_context的字错误率的百分比阈值 [0,100]，字错误率大于阈值会报错
         *
         * @param asrErrorRate 可接受错误率百分比，默认值0
         * @return {@link Builder}
         */
        public Builder setAsrErrorRate(int asrErrorRate) {
            this.asrErrorRate = asrErrorRate;
            return this;
        }

        /**
         * 标记语音中已知的固定的文本内容（如唤醒词），算法会特殊处理被标记的音频。
         *
         * @param constantContent 固定的文本内容
         * @return {@link Builder}
         */
        public Builder setConstantContent(String constantContent) {
            this.constantContent = constantContent;
            return this;
        }

        /**
         * 文本（半）相关时输入的语音文本。
         *
         * @param customContent 语音文本
         * @return {@link Builder}
         */
        public Builder setCustomContent(String customContent) {
            this.customContent = customContent;
            return this;
        }

        /**
         * 注册增强，仅用于声纹注册
         *
         * @param enhanceRegister 是否使用注册增强，默认false
         * @return {@link Builder}
         */
        public Builder setEnhanceRegister(boolean enhanceRegister) {
            this.enhanceRegister = enhanceRegister;
            return this;
        }

        /**
         * 1:N按照score排序,输出前n个得分,若不配置该选项,会在result字段中返回所有得分,开启该选项后会在topN字段中返回得分排名靠前的最多N个得分
         *
         * @param topN 前n个得分
         * @return {@link Builder}
         */
        public Builder setTopN(int topN) {
            this.topN = topN;
            return this;
        }

        /**
         * 识别结果送声纹，用以和{@link #customContent}做对比
         *
         * @param recWords 识别需要送声纹的文本
         * @return {@link Builder}
         */
        public Builder setRecWords(String recWords) {
            this.recWords = recWords;
            return this;
        }

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
         * 设置声纹工作模式,
         * 若不设置，会抛{@link IllegalArgumentException}异常
         *
         * @param action {@link Action}
         * @return {@link Builder}
         */
        public Builder setAction(Action action) {
            this.mAction = action;
            return this;
        }

        /**
         * 用于标识当前feed的音频是静音段还是有人声的
         * 其中，静音段传入 silence，人声段传入 speech
         * @param speechState 声音段状态
         */
        public Builder setSpeechState(String speechState) {
            this.speechState = speechState;
            return this;
        }

        /**
         * 设置保存唤醒内核给声纹的音频数据
         *
         * @param vprintLiteSaveDir feed 给声纹内核的音频数据保存的文件夹路径
         * @return {@link Builder}
         */
        public Builder setVprintLiteSaveDir(String vprintLiteSaveDir) {
            this.mVprintLiteSaveDir = vprintLiteSaveDir;
            return this;
        }

        public VprintLiteIntent create() throws IllegalArgumentException {
            return new VprintLiteIntent(this);
        }
    }
}
