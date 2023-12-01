package com.aispeech;

import android.text.TextUtils;

import com.aispeech.export.bean.VoiceQueueStrategy;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.Engines;
import com.aispeech.lite.base.BaseConfig;
import com.aispeech.lite.base.IVoiceRestrictive;

public class AIEchoConfig extends BaseConfig implements IVoiceRestrictive {

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
    private String savedDirPath = null;

    /**
     * 是否开启AEC健康监控
     */
    private boolean monitorEnable;
    /**
     * 健康监控执行周期
     */
    private int monitorPeriod;
    private int micType = -1;

    public boolean isMonitorEnable() {
        return monitorEnable;
    }

    public int getMonitorPeriod() {
        return monitorPeriod;
    }

    public void setMonitorEnable(boolean monitorEnable) {
        this.monitorEnable = monitorEnable;
    }

    /**
     * 设置启用麦克风阵列类型
     *
     * @param micType 麦克风阵列类型
     */
    public void setMicType(int micType) {
        this.micType = micType;
    }

    public int getMicType() {
        return micType;
    }

    public void setMonitorPeriod(int monitorPeriod) {
        this.monitorPeriod = monitorPeriod;
    }

    public String getAecResource() {
        return aecResource;
    }

    /**
     * 设置 ECHO 模式的 AEC 资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param aecResource AEC资源
     */
    public void setAecResource(String aecResource) {
        this.aecResource = aecResource;
    }

    public int getChannels() {
        return channels;
    }

    /**
     * 音频总的通道数，1+1，默认为2
     *
     * @param channels 音频总的通道数
     */
    public void setChannels(int channels) {
        this.channels = channels;
    }

    public int getMicNumber() {
        return micNumber;
    }

    /**
     * mic数，默认为1
     *
     * @param micNumber mic数
     */
    public void setMicNumber(int micNumber) {
        this.micNumber = micNumber;
    }

    public int getRecChannel() {
        return recChannel;
    }

    /**
     * 默认为1,即左通道为rec录音音频,右通道为play参考音频（播放音频）
     * 若设置为2, 通道会互换，即左通道为play参考音频（播放音频）,右通道为rec录音音频
     *
     * @param recChannel recChannel
     */
    public void setRecChannel(int recChannel) {
        this.recChannel = recChannel;
        if (this.recChannel != 1 && this.recChannel != 2)
            throw new IllegalArgumentException("recChannel can only set 1 or 2");
    }

    private static final String ECHO_AUDIO_DIR = "echo";

    public String getSavedDirPath() {
        // 音频保存是否开启
        if (!AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE) {
            return null;
        }

        if (!Engines.isSavingEngineAudioEnable(Engines.ECHO)) {
            return null;
        }
        if (!TextUtils.isEmpty(AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH)) {
            savedDirPath = AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH;
            if (!savedDirPath.endsWith("/")) {
                savedDirPath += "/";
            }
            savedDirPath += ECHO_AUDIO_DIR;
        }
        return savedDirPath;
    }

    /**
     * AEC保存的音频文件目录，
     * aec之前的原始音频文件格式：echoSavedPath/echo_in_时间戳.pcm，
     * aec之后的一路音频文件格式：echoSavedPath/echo_out_时间戳.pcm
     *
     * @param savedDirPath AEC保存的音频文件目录
     */
    public void setSavedDirPath(String savedDirPath) {
        this.savedDirPath = savedDirPath;
    }

    @Override
    public String toString() {
        return "AIEchoConfig{" +
                "aecResource='" + aecResource + '\'' +
                ", channels=" + channels +
                ", micNumber=" + micNumber +
                ", micType=" + micType +
                ", recChannel=" + recChannel +
                ", savedDirPath='" + savedDirPath + '\'' +
                '}';
    }

    public AIEchoConfig() {
    }

    /**
     * 设置 ECHO 模式的配置信息
     *
     * @param aecResource  AEC资源
     * @param channels     音频总的通道数，1+1，默认为2
     * @param micNumber    mic数，默认为1
     * @param recChannel   默认为1,即左通道为rec录音音频,右通道为play参考音频（播放音频）
     *                     若设置为2, 通道会互换，即左通道为play参考音频（播放音频）,右通道为rec录音音频
     * @param savedDirPath AEC保存的音频文件目录，
     *                     aec之前的原始音频文件格式：echoSavedPath/echo_in_时间戳.pcm，
     *                     aec之后的一路音频文件格式：echoSavedPath/echo_out_时间戳.pcm
     */
    public AIEchoConfig(String aecResource, int channels, int micNumber, int recChannel, String savedDirPath) {
        this.aecResource = aecResource;
        this.channels = channels;
        this.micNumber = micNumber;
        this.recChannel = recChannel;
        this.savedDirPath = savedDirPath;
    }

    /**
     * 设置 ECHO 模式的配置信息
     *
     * @param aecResource  AEC资源
     * @param channels     音频总的通道数，1+1，默认为2
     * @param micNumber    mic数，默认为1
     * @param micType      麦克风阵列类型
     * @param recChannel   默认为1,即左通道为rec录音音频,右通道为play参考音频（播放音频）
     *                     若设置为2, 通道会互换，即左通道为play参考音频（播放音频）,右通道为rec录音音频
     * @param savedDirPath AEC保存的音频文件目录，
     *                     aec之前的原始音频文件格式：echoSavedPath/echo_in_时间戳.pcm，
     *                     aec之后的一路音频文件格式：echoSavedPath/echo_out_时间戳.pcm
     */
    public AIEchoConfig(String aecResource, int channels, int micNumber, int micType, int recChannel, String savedDirPath) {
        this.aecResource = aecResource;
        this.channels = channels;
        this.micNumber = micNumber;
        this.recChannel = recChannel;
        this.savedDirPath = savedDirPath;
        this.micType = micType;
    }

    public void setAIEchoConfig(AIEchoConfig config) {
        if (config == null)
            return;
        this.aecResource = config.aecResource;
        this.channels = config.channels;
        this.micNumber = config.micNumber;
        this.recChannel = config.recChannel;
        this.savedDirPath = config.savedDirPath;
        this.micType = config.micType;
    }

    @Override
    public void setVoiceStrategy(VoiceQueueStrategy voiceStrategy) {
        this.voiceQueueStrategy = voiceStrategy;
    }

    @Override
    public VoiceQueueStrategy getMaxVoiceQueueSize() {
        return voiceQueueStrategy;
    }
}
