package com.aispeech.export.config;


import com.aispeech.AIEchoConfig;
import com.aispeech.export.function.IConfig;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.base.BaseConfig;

/**
 * 回声消除初始化配置
 *
 * @author hehr
 * @deprecated 废弃，使用{@link AIEchoConfig}
 */
public class EchoConfig extends BaseConfig implements IConfig {

    /**
     * 设置 ECHO 模式的 AEC 资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     */
    private String aecResource = null;

    /**
     * 音频总的通道数，1+1，默认为2
     */
    private int channels = 2;

    /**
     * mic数，默认为1
     */
    private int micNumber = 1;

    /**
     * 默认为1,即左通道为rec录音音频,右通道为play参考音频（播放音频）
     * 若设置为2, 通道会互换，即左通道为play参考音频（播放音频）,右通道为rec录音音频
     */
    private int recChannel = 1;

    /**
     * AEC保存的音频文件目录，
     * aec之前的原始音频文件格式：echoSavedPath/echo_in_时间戳.pcm，
     * aec之后的一路音频文件格式：echoSavedPath/echo_out_时间戳.pcm
     */
    private String savedDirPath;

    /**
     * 是否开启AEC健康监控
     */
    private boolean monitorEnable;
    /**
     * 健康监控执行周期
     */
    private int monitorPeriod;

    private int micType = -1;

    public int getMicType() {
        return micType;
    }

    /**
     * 设置启用麦克风阵列类型
     *
     * @param micType 麦克风阵列类型
     */
    public void setMicType(int micType) {
        this.micType = micType;
    }

    public boolean isMonitorEnable() {
        return monitorEnable;
    }

    public int getMonitorPeriod() {
        return monitorPeriod;
    }

    public String getAecResource() {
        return aecResource;
    }

    public int getChannels() {
        return channels;
    }

    public int getMicNumber() {
        return micNumber;
    }

    public int getRecChannel() {
        return recChannel;
    }

    public String getSavedDirPath() {
        return savedDirPath;
    }

    private EchoConfig(Builder builder) {
        this.aecResource = builder.aecResource;
        this.channels = builder.channels;
        this.micNumber = builder.micNumber;
        this.recChannel = builder.recChannel;
        this.savedDirPath = builder.savedDirPath;
        this.monitorEnable = builder.monitorEnable;
        this.monitorPeriod = builder.monitorPeriod;
        this.micType = builder.micType;

        if (builder.echoCallbackBufferSize <= 0) {
            if (this.micNumber == 2) {
                //32ms的数据大小=16000*2*16/8/1000*32=2048
                AISpeech.echoCallbackBufferSize = 16000 * 2 * 16 / 8 / 1000 * 32;
            } else if (this.micNumber == 4) {
                AISpeech.echoCallbackBufferSize = 16000 * 4 * 16 / 8 / 1000 * 32;
            }
        } else {
            AISpeech.echoCallbackBufferSize = builder.echoCallbackBufferSize;
        }
    }

    public static class Builder extends BaseConfig.Builder {


        private String aecResource = null;

        private int channels = 2;

        private int micNumber = 1;
        private int micType = -1;

        private int recChannel = 1;

        private String savedDirPath = null;

        /**
         * 是否开启AEC健康监控
         */
        private boolean monitorEnable = false;
        /**
         * 健康监控执行周期 ,默认200 ms
         */
        private int monitorPeriod = 200;

        private int echoCallbackBufferSize;

        /**
         * 是否开启AEC健康检查，默认关闭
         *
         * @param enableMonitor boolean
         * @return {@link Builder}
         */
        public Builder setMonitorEnable(boolean enableMonitor) {
            this.monitorEnable = enableMonitor;
            return this;
        }

        /**
         * 设置健康检查运行周期,默认200ms
         *
         * @param monitorPeriod 运行周期
         * @return {@link Builder}
         */
        public Builder setMonitorPeriod(int monitorPeriod) {
            this.monitorPeriod = monitorPeriod;
            return this;
        }

        /**
         * 设置 ECHO 模式的 AEC 资源
         * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
         * <p>2. 如在 assets 里设置为名称</p>
         *
         * @param aecResource echo资源绝对路径
         * @return {@link Builder}
         */
        public Builder setAecResource(String aecResource) {
            this.aecResource = aecResource;
            return this;
        }

        /**
         * 音频总的通道数，1+1，默认为2
         *
         * @param channels 通道数
         * @return {@link Builder}
         */
        public Builder setChannels(int channels) {
            this.channels = channels;
            return this;
        }

        /**
         * mic数，默认为1
         *
         * @param micNumber mic数量
         * @return {@link Builder}
         */
        public Builder setMicNumber(int micNumber) {
            this.micNumber = micNumber;
            return this;
        }

        /**
         * 设置启用麦克风阵列类型
         *
         * @param type 麦克风阵列类型
         */
        public Builder setMicType(int type) {
            this.micType = type;
            return this;
        }

        /**
         * 默认为1,即左通道为rec录音音频,右通道为play参考音频（播放音频）
         * 若设置为2, 通道会互换，即左通道为play参考音频（播放音频）,右通道为rec录音音频
         *
         * @param recChannel 通道互换 ,默认 1
         * @return {@link Builder}
         */
        public Builder setRecChannel(int recChannel) {
            this.recChannel = recChannel;
            return this;
        }

        /**
         * AEC保存的音频文件目录，
         * aec之前的原始音频文件格式：echoSavedPath/echo_in_时间戳.pcm，
         * aec之后的一路音频文件格式：echoSavedPath/echo_out_时间戳.pcm
         *
         * @param savedDirPath 音频保存目录，如： /sdcar/aispeech/echo/
         * @return {@link Builder}
         */
        public Builder setSavedDirPath(String savedDirPath) {
            this.savedDirPath = savedDirPath;
            return this;
        }

        /**
         * 设置echo回调的buffer大小
         *
         * @param bufferSize echo后的数据大小
         * @return this
         * @deprecated 使用sspe代替原有的echo
         */
        @Deprecated
        public Builder setEchoCallbackBufferSize(int bufferSize) {
            this.echoCallbackBufferSize = bufferSize;
            return this;
        }

        @Override
        public Builder setTagSuffix(String tagSuffix) {
            return (Builder) super.setTagSuffix(tagSuffix);
        }

        public EchoConfig create() {
            return super.build(new EchoConfig(this));
        }
    }

    public AIEchoConfig transferAIEchoConfig() {
        AIEchoConfig config = new AIEchoConfig();
        config.setSavedDirPath(this.getSavedDirPath());
        config.setAecResource(this.getAecResource());
        config.setChannels(this.getChannels());
        config.setMicNumber(this.getMicNumber());
        config.setRecChannel(this.getRecChannel());
        config.setMonitorPeriod(this.getMonitorPeriod());
        config.setMonitorEnable(this.isMonitorEnable());
        config.setMicType(this.getMicType());
        return config;
    }
}
