package com.aispeech.export.config;

import com.aispeech.DUILiteConfig;
import com.aispeech.lite.base.BaseConfig;

public class AICloudASRConfig extends BaseConfig {

    /**
     * 识别时是否需要声纹信息，默认false。这里打开后需要在 {@linkplain com.aispeech.export.intent.AICloudASRIntent AICloudASRIntent} 加上需要验证的声纹信息
     */
    private boolean useVprint;

    /**
     * 是否启用本地vad, default is true
     */
    private boolean localVadEnable = true;

    /**
     * 本地vad资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     */
    private String vadResource;

    /**
     * 队列大小
     */
    private int maxMessageQueueSize = -1;
    /**
     * 是否使用双VAD
     */
    private boolean enableDoubleVad;
    /**
     * 设置本地vad资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param vadResource vad资源
     */
    public void setVadResource(String vadResource) {
        this.vadResource = vadResource;
    }

    public String getVadResource() {
        return vadResource;
    }

    /**
     * 设置是否启用本地vad，一般都会打开,
     *
     * @param localVadEnable true (default) 使用Vad, false 禁止Vad
     */
    public void setLocalVadEnable(boolean localVadEnable) {
        this.localVadEnable = localVadEnable;
    }

    public boolean isLocalVadEnable() {
        return localVadEnable;
    }

    public boolean isUseVprint() {
        return useVprint;
    }

    /**
     * 识别时是否需要声纹信息，默认false。这里打开后需要在 {@linkplain com.aispeech.export.intent.AICloudASRIntent AICloudASRIntent} 加上需要验证的声纹信息
     *
     * @param useVprint 是否需要声纹信息
     */
    public void setUseVprint(boolean useVprint) {
        this.useVprint = useVprint;
    }

    /**
     * 设置消息队列最大长度
     * <ul>
     *     <li>默认-1 使用 {@linkplain DUILiteConfig#getMaxMessageQueueSize() DUILiteConfig#getMaxMessageQueueSize()} 的配置</li>
     *     <li>0表示不限制长度, 建议大于100</li>
     * </ul>
     * <p>动态库方法运行在一个单独的线程里，通过消息队列依次调用。
     * 在设备性能不好的设备上可以设置消息队列最大长度，防止算力不够导致内核无法及时处理完音频数据而导致内存过大的问题</p>
     *
     * @param maxMessageQueueSize 消息队列最大长度
     */
    public void setMaxMessageQueueSize(int maxMessageQueueSize) {
        this.maxMessageQueueSize = maxMessageQueueSize;
    }

    public int getMaxMessageQueueSize() {
        return maxMessageQueueSize;
    }

    /**
     * 设置识别引擎是否使用双VAD，使用双VAD 需要 feed 2通道音频，而
     * 使用非双VAD 则 feed 1 通道的音频。
     *
     * @param enableDoubleVad true 使用双VAD , false 非双VAD
     */
    public void setEnableDoubleVad(boolean enableDoubleVad) {
        this.enableDoubleVad = enableDoubleVad;
    }

    public boolean isEnableDoubleVad() {
        return enableDoubleVad;
    }

    @Override
    public String toString() {
        return "AICloudASRConfig{" +
                "useVprint=" + useVprint +
                ", localVadEnable=" + localVadEnable +
                ", vadResource='" + vadResource + '\'' +
                ", enableDoubleVad='" + enableDoubleVad + '\'' +
                ", maxMessageQueueSize=" + maxMessageQueueSize +
                '}';
    }

    public static final class Builder extends BaseConfig.Builder {
        private boolean localVadEnable = true;
        private boolean enableAsrPlus;
        /**
         * 队列大小
         */
        private int maxMessageQueueSize = -1;
        private String vadResource;
        /**
         * 是否使用双VAD
         */
        private boolean enableDoubleVad;

        /**
         * 设置识别引擎是否使用双VAD，使用双VAD 需要 feed 2通道音频，而
         * 使用非双VAD 则 feed 1 通道的音频。
         *
         * @param enableDoubleVad true 使用双VAD , false 非双VAD
         * @return Builder
         */
        public Builder setEnableDoubleVad(boolean enableDoubleVad) {
            this.enableDoubleVad = enableDoubleVad;
            return this;
        }

        /**
         * 设置VAD资源名字
         * 需要在init之前调用
         *
         * @param vadResource vad资源名
         * @return Builder
         */
        public Builder setVadResource(String vadResource) {
            this.vadResource = vadResource;
            return this;
        }
        /**
         * 设置是否使用云端声纹参数
         *
         * @param enableAsrPlus 是否使用云端声纹
         * @return builder
         * 使用serverName进行判断，如果设置serverName，则认为是使用了asrplus
         * 识别时是否需要声纹信息，默认false。这里打开后需要在 {@linkplain com.aispeech.export.intent.AICloudASRIntent AICloudASRIntent} 加上需要验证的声纹信息
         */
        public Builder setEnableAsrPlus(boolean enableAsrPlus) {
            this.enableAsrPlus = enableAsrPlus;
            return this;
        }


        /**
         * 设置消息队列最大长度
         * <ul>
         *     <li>默认-1 使用 {@linkplain DUILiteConfig#getMaxMessageQueueSize() DUILiteConfig#getMaxMessageQueueSize()} 的配置</li>
         *     <li>0表示不限制长度, 建议大于100</li>
         * </ul>
         * <p>动态库方法运行在一个单独的线程里，通过消息队列依次调用。
         * 在设备性能不好的设备上可以设置消息队列最大长度，防止算力不够导致内核无法及时处理完音频数据而导致内存过大的问题</p>
         *
         * @param maxMessageQueueSize 消息队列最大长度
         */
        public Builder setMaxMessageQueueSize(int maxMessageQueueSize) {
            this.maxMessageQueueSize = maxMessageQueueSize;
            return this;
        }

        /**
         * 设置是否启用本地vad，一般都会打开,需要在init之前调用
         *
         * @param localVadEnable 默认为true
         *                       true:使用Vad；false:禁止Vad
         * @return Builder
         */
        public Builder setLocalVadEnable(boolean localVadEnable) {
            this.localVadEnable = localVadEnable;
            return this;
        }


        @Override
        public Builder setTagSuffix(String tagSuffix) {
            return (Builder) super.setTagSuffix(tagSuffix);
        }

        public AICloudASRConfig build() {
            AICloudASRConfig aICloudAsrConfig = new AICloudASRConfig();
            aICloudAsrConfig.vadResource = this.vadResource;
            aICloudAsrConfig.localVadEnable = this.localVadEnable;
            aICloudAsrConfig.useVprint = this.enableAsrPlus;
            aICloudAsrConfig.enableDoubleVad = this.enableDoubleVad;
            aICloudAsrConfig.maxMessageQueueSize = this.maxMessageQueueSize;
            return super.build(aICloudAsrConfig);
        }
    }
}
