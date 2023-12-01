package com.aispeech.export.intent;

import android.media.AudioAttributes;
import android.media.AudioManager;

import com.aispeech.export.config.AILocalTTSConfig;
import com.aispeech.lite.AISampleRate;

public class AILocalTTSIntent {

    /**
     * 语音合成的速度, default is {@value}
     */
    private float speed = 1.0f;
    /**
     * 语音合成的音量
     */
    private int volume = 80;

    /**
     * 是否使用 ssml, default is {@value}
     */
    private boolean useSSML = false;

    /**
     * 播放器的stream type,默认为{@link AudioManager#STREAM_MUSIC}
     */
    private int streamType = AudioManager.STREAM_MUSIC;

    private int audioAttributesUsage = 0; // AudioAttributes.USAGE_UNKNOWN
    private int audioAttributesContentType = 0; // AudioAttributes.CONTENT_TYPE_UNKNOWN

    /**
     * 合成音的保存路径
     */
    private String saveAudioFilePath = "";

    /**
     * 切换 发音人 资源
     */
    private String speakerResource = null;

    /**
     * 音频头部静音时间，范围5-20，默认是5
     */
    private int lmargin = 5;

    /**
     * 音频尾部静音时间，范围5-20，默认是10
     */
    private int rmargin = 10;


    /**
     * 时间音子
     */
    public boolean useTimeStamp;
    /**
     * 表示 CPU 睡眠时间
     * 配置了 optimization 为 true 时，此选项才生效。取值范围为 0-500，
     * 默认为 0，可选。
     */
    private int sleepTime = 0;

    /**
     * 方言选项，用于支持粤语、上海话、四川话等音色，默认为 0，可选。
     * 4-粤语；5-英语；6-法语；7-泰语；8-四川话；9-东北话；10-闽南语；11-德语
     */
    private int language = 0;

    private AudioAttributes audioAttributes;
    private boolean useStreamType;
    private AISampleRate mPlaySampleRate = AISampleRate.SAMPLE_RATE_16K;

    public int getLanguage() {
        return language;
    }

    /**
     * 方言选项，用于支持粤语、上海话、四川话等音色，默认为 0，可选。
     *
     * @param language 4-粤语；5-英语；6-法语；7-泰语；8-四川话；9-东北话；10-闽南语；11-德语
     */
    public void setLanguage(int language) {
        this.language = language;
    }

    public int getSleepTime() {
        return sleepTime;
    }

    /**
     * 表示 CPU 睡眠时间
     * 配置了 optimization 为 true 时，此选项才生效。取值范围为 0-500，
     * 默认为 0，可选。
     */
    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    /**
     * 设置语音合成的速度
     *
     * @param speed 合成语速 范围为0.5～2.0
     */
    public void setSpeed(float speed) {
        this.speed = speed;
    }

    /**
     * 设置语音合成的音量
     *
     * @param volume 合成音量 范围为1～500
     */
    public void setVolume(int volume) {
        this.volume = volume;
    }

    /**
     * 设置是否使用ssml 默认不使用为false
     *
     * @param useSSML 是否配置ssml
     */
    public void setUseSSML(boolean useSSML) {
        this.useSSML = useSSML;
    }


    /**
     * 设置播放器的stream type,默认为{@link AudioManager#STREAM_MUSIC}
     * 强制使用需设置{@link #setUseStreamType(boolean)}
     * 高版本API 23及以上使用 {@link #setAudioAttributes(AudioAttributes)}
     *
     * @param streamType audioTrack播放stream type
     */
    public void setStreamType(int streamType) {
        this.streamType = streamType;
    }

    /**
     * 设置音子时间戳
     *
     * @param useTimeStamp 是否开启音子时间戳
     */
    public void setUseTimeStamp(boolean useTimeStamp) {
        this.useTimeStamp = useTimeStamp;
    }

    /**
     * 设置音频属性，Android O 及以上系统使用，Android O 以前的系统请使用 {@linkplain #setStreamType} 方法
     *
     * @param audioAttributesUsage       类似 AudioAttributes.USAGE_MEDIA 的设置
     * @param audioAttributesContentType 类似 AudioAttributes.CONTENT_TYPE_MUSIC 的设置
     * @deprecated use {{@link #setAudioAttributes(AudioAttributes)}}
     */
    public void setAudioAttributes(int audioAttributesUsage, int audioAttributesContentType) {
        this.audioAttributesUsage = audioAttributesUsage;
        this.audioAttributesContentType = audioAttributesContentType;
    }

    public int getAudioAttributesUsage() {
        return audioAttributesUsage;
    }

    public int getAudioAttributesContentType() {
        return audioAttributesContentType;
    }

    /**
     * 设置合成音的保存路径
     *
     * @param saveAudioFilePath 文件路径
     */
    public void setSaveAudioFilePath(String saveAudioFilePath) {
        this.saveAudioFilePath = saveAudioFilePath;
    }

    /**
     * 切换发音人，需在 {@link AILocalTTSConfig#addSpeakerResource(String[]) addSpeakerResource} 方法设置过
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param speakerResource 发音人资源
     */
    public void switchToSpeaker(String speakerResource) {
        this.speakerResource = speakerResource;
    }

    /**
     * 切换发音人，需在 {@link AILocalTTSConfig#addSpeakerResource(String[]) addSpeakerResource} 方法设置过
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param speakerResource 发音人资源
     * @param language        语言
     */
    public void switchToSpeaker(String speakerResource, int language) {
        this.speakerResource = speakerResource;
        this.language = language;
    }

    public float getSpeed() {
        return speed;
    }


    public int getVolume() {
        return volume;
    }


    public boolean isUseSSML() {
        return useSSML;
    }

    public int getStreamType() {
        return streamType;
    }

    public String getSaveAudioFilePath() {
        return saveAudioFilePath;
    }

    public String getSpeakerResource() {
        return speakerResource;
    }

    public boolean getUseTimeStamp() {
        return useTimeStamp;
    }


    public void setAudioAttributes(AudioAttributes audioAttributes) {
        this.audioAttributes = audioAttributes;
    }

    public AudioAttributes getAudioAttributes() {
        return audioAttributes;
    }

    public boolean isUseStreamType() {
        return useStreamType;
    }

    public void setUseStreamType(boolean useStreamType) {
        this.useStreamType = useStreamType;
    }

    public void setPlaySampleRate(AISampleRate mPlaySampleRate) {
        this.mPlaySampleRate = mPlaySampleRate;
    }

    public AISampleRate getPlaySampleRate() {
        return mPlaySampleRate;
    }

    @Override
    public String toString() {
        return "AILocalTTSIntent{" +
                "speed=" + speed +
                ", volume=" + volume +
                ", useSSML=" + useSSML +
                ", streamType=" + streamType +
                ", audioAttributesUsage=" + audioAttributesUsage +
                ", audioAttributesContentType=" + audioAttributesContentType +
                ", saveAudioFilePath='" + saveAudioFilePath + '\'' +
                ", speakerResource='" + speakerResource + '\'' +
                ", lmargin='" + lmargin + '\'' +
                ", rmargin='" + rmargin + '\'' +
                ", sleepTime='" + sleepTime + '\'' +
                ", useTimeStamp=" + useTimeStamp +
                ", language=" + language +
                ", mPlaySampleRate=" + mPlaySampleRate +
                '}';
    }

    public int getLmargin() {
        return this.lmargin;
    }

    /**
     * 设置合成音频头部静音时间，范围5-20，默认是5，单位ms
     *
     * @param lmargin 合成音频头部静音时间
     */

    public void setLmargin(int lmargin) {
        this.lmargin = lmargin;
    }

    public int getRmargin() {
        return rmargin;
    }

    /**
     * 设置合成音频尾部静音时间，范围5-20，默认是10，单位ms
     *
     * @param rmargin 合成音频尾部静音时间
     */
    public void setRmargin(int rmargin) {
        this.rmargin = rmargin;
    }

}
