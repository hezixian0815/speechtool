package com.aispeech.export.intent;

import android.media.AudioAttributes;
import android.media.AudioManager;

import com.aispeech.common.AIConstant;

public class AICloudTTSIntent {

    private AudioAttributes audioAttributes;
    private boolean useStreamType;
    private int pitchChange;
    /**
     * 合成的文本类型, text or ssml, default is {@value}
     */
    private String textType = "text";

    /**
     * 云端合成请求地址, default is {@value}
     */
    private String server = "https://tts.duiopen.com/runtime/aggregation/synthesize";

    /**
     * 音频流通道, default is AudioManager.STREAM_MUSIC
     */
    private int streamType = AudioManager.STREAM_MUSIC;

    private int audioAttributesUsage = 0; // AudioAttributes.USAGE_UNKNOWN
    private int audioAttributesContentType = 0; // AudioAttributes.CONTENT_TYPE_UNKNOWN

    /**
     * 是否开启实时反馈，default is {@value}
     */
    private boolean realBack = true;

    /**
     * 云端TTS合成的采样率，默认16000，芊芊音色需要配置24000
     */
    private int sampleRate = 16000;

    /**
     * 特定的发音人ID，用于设置声音复刻的声音ID
     */
    private String userId = "1001";

    /**
     * 合成音频格式，支持mp3，default is {@value}
     */
    private String audioType = AIConstant.TTS_AUDIO_TYPE_MP3;

    /**
     * 云端合成mp3码率，支持low{@link AIConstant#TTS_MP3_QUALITY_LOW}
     * 和high{@link AIConstant#TTS_MP3_QUALITY_HIGH}，默认为{@link AIConstant#TTS_MP3_QUALITY_LOW}
     */
    private String mp3Quality = AIConstant.TTS_MP3_QUALITY_LOW;

    /**
     * 合成音的保存路径
     */
    private String saveAudioPath = null;

    /**
     * 音量大小,1-100, 100声音最响, default is {@value}
     */
    private String volume = "50";

    /**
     * 合成音语速,0.5-2, 0.5语速最快, default is {@value}
     */
    private String speed = "1";

    /**
     * 发音人、合成音类型
     */
    private String speaker = AIConstant.CLOUD_TTS_SPEAK_DEFAULT;

    /**
     * 情感，有三种参数类型{"happy","default","sad"},如果不传或者传递错误的类型默认为default
     */
    private String speakingStyle = "default";

    /**
     * 合成语言，部分音色支持普通话，粤语，四川话
     */
    private String mLanguage;

    /**
     * 音素，多用于多模态下口型对应信息
     */
    private boolean returnPhone = false;
    /**
     * 等待合成结果超时时间 单位毫秒，默认是3000，default is {@value}ms
     * <p> 从调用speak|synthesize方法开始计时 </p>
     */
    private int waitingTimeout = 3000;

    /**
     * 设置合成的文本类型
     *
     * @param type text or ssml
     */
    public void setTextType(String type) {
        this.textType = type;
    }

    /**
     * 设置云端tts合成请求地址
     *
     * @param server 云端tts合成请求地址
     */
    public void setServer(String server) {
        this.server = server;
    }


    /**
     * 设置音频流通道
     *
     * @param streamType streamType,默认为{@link AudioManager#STREAM_MUSIC}
     */
    public void setStreamType(int streamType) {
        this.streamType = streamType;
    }

    /**
     * 设置音频属性，Android O 及以上系统使用，Android O 以前的系统请使用 {@linkplain #setStreamType} 方法
     *
     * @param audioAttributesUsage       类似 AudioAttributes.USAGE_MEDIA 的设置
     * @param audioAttributesContentType 类似 AudioAttributes.CONTENT_TYPE_MUSIC 的设置
     * @deprecated use {{@link #setAudioAttributes(AudioAttributes)}}
     */
    @Deprecated
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
     * 设置是否开启实时反馈，默认开启为true
     *
     * @param realBack true 开启，false 不开启
     */
    public void setRealBack(boolean realBack) {
        this.realBack = realBack;
    }


    /**
     * 设置合成音频格式，支持mp3{@link AIConstant#TTS_AUDIO_TYPE_MP3}
     *
     * @param audioType 合成音频格式
     */
    public void setAudioType(String audioType) {
        this.audioType = audioType;
    }

    /**
     * 设置云端合成mp3码率，支持low{@link AIConstant#TTS_MP3_QUALITY_LOW}
     * 和high{@link AIConstant#TTS_MP3_QUALITY_HIGH}，默认为low码率
     *
     * @param mp3Quality 云端合成mp3码率
     *                   ，且只在合成音频格式为mp3前提下才有效,详见{@link #setAudioType(String)}
     */
    public void setMp3Quality(String mp3Quality) {
        this.mp3Quality = mp3Quality;
    }

    /**
     * 设置合成音的保存路径
     *
     * @param filePath 文件路径
     */
    public void setSaveAudioPath(String filePath) {
        this.saveAudioPath = filePath;
    }

    /**
     * 设置音量大小
     *
     * @param volume 1-100, 100声音最响
     */
    public void setVolume(String volume) {
        this.volume = volume;
    }

    public void setVolume(int volume) {
        this.volume = volume + "";
    }

    /**
     * 设置合成音语速
     *
     * @param speed 0.5-2, 0.5语速最快
     */
    public void setSpeed(String speed) {
        this.speed = speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed + "";
    }

    /**
     * 情感，有三种参数类型{"happy","default","sad"},如果不传或者传递错误的类型默认为default
     * 目前仅有两种音色支持情感参数
     * ryzenm : night
     * kuayuf : angry/sad/happy
     *
     * @param speakingStyle 情感
     */
    public void setSpeakingStyle(String speakingStyle) {
        this.speakingStyle = speakingStyle;
    }

    /**
     * 情感，有三种参数类型{"happy","default","sad"},如果不传或者传递错误的类型默认为default
     * 目前仅有两种音色支持情感参数
     * ryzenm : night
     * kuayuf : angry/sad/happy
     *
     * @param speakingStyle 情感
     */
    public void setEmotion(String speakingStyle) {
        this.speakingStyle = speakingStyle;
    }

    /**
     * 设置合成音类型
     *
     * @param speaker 例如：zhilingfa
     */
    public void setSpeaker(String speaker) {
        this.speaker = speaker;
    }

    public String getTextType() {
        return textType;
    }

    public String getServer() {
        return server;
    }

    public int getStreamType() {
        return streamType;
    }

    public boolean isRealBack() {
        return realBack;
    }

    public String getAudioType() {
        return audioType;
    }

    public String getMp3Quality() {
        return mp3Quality;
    }


    public String getSaveAudioPath() {
        return saveAudioPath;
    }


    public String getVolume() {
        return volume;
    }

    public String getSpeed() {
        return speed;
    }

    public String getSpeakingStyle() {
        return speakingStyle;
    }

    public String getSpeaker() {
        return speaker;
    }

    public String getUserId() {
        return userId;
    }

    /**
     * 设置发音人ID，用于声音复刻生成的自定义声音ID
     *
     * @param userId 自定义的声音ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * 设置云端tts合成音的采样率
     *
     * @param sampleRate 采样率，默认16000，芊芊音色需要配置24000
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    /**
     * 当前是否开启了音素返回，需注意，部分音色不支持音素，所以虽然打开了音素，但是不会有音素信息返回
     *
     * @return 是否开启了音素
     */
    public boolean isReturnPhone() {
        return returnPhone;
    }

    public int getPitchChange() {
        return pitchChange;
    }

    /**
     * 设置语调
     *
     * @param pitchChange 取值范围(-60，60)，默认是0
     */
    public void setPitchChange(int pitchChange) {
        this.pitchChange = pitchChange;
    }

    /**
     * 设置是否返回音素信息
     * 当前只有以下音色支持音素功能，除此之外的音色禁止使用音素：
     * cyangfp dyb gdfanfp gqlanfp hthy jjingfp jlshim lanyuf lchuam lili1f_yubo lucyfa
     * lzliafp madoufp_wenrou madoufp_yubo xbekef xijunma xjingfp xyb xynmamp ychanmp yhchu
     * zhilingfp zhilingfp_huankuai zsmeif dksjif ybyuaf sqksaf zxiyum aningfp lmyanm
     * wqingf_csn ppangf_csn hchunf_ctn mamif xmguof
     *
     * @param returnPhone 是否返回音素信息，默认false
     */
    public void setReturnPhone(boolean returnPhone) {
        this.returnPhone = returnPhone;
    }

    /**
     * 等待合成结果超时时间 单位毫秒，默认是3000，default is {@value}ms
     * <p> 从调用speak|synthesize方法开始计时 </p>
     */
    public void setWaitingTimeout(int waitingTimeout) {
        this.waitingTimeout = waitingTimeout;
    }

    public int getWaitingTimeout() {
        return waitingTimeout;
    }

    @Override
    public String toString() {
        return "AICloudTTSIntent{" +
                "textType='" + textType + '\'' +
                ", server='" + server + '\'' +
                ", streamType=" + streamType +
                ", audioAttributesUsage=" + audioAttributesUsage +
                ", audioAttributesContentType=" + audioAttributesContentType +
                ", realBack=" + realBack +
                ", audioType='" + audioType + '\'' +
                ", mp3Quality='" + mp3Quality + '\'' +
                ", saveAudioPath='" + saveAudioPath + '\'' +
                ", volume='" + volume + '\'' +
                ", speed='" + speed + '\'' +
                ", speaker='" + speaker + '\'' +
                ", speakingStyle='" + speakingStyle + '\'' +
                ", sampleRate='" + sampleRate + '\'' +
                ", returnPhone='" + returnPhone + '\'' +
                ", waitingTimeout='" + waitingTimeout + '\'' +
                '}';
    }

    public String getLanguage() {
        return mLanguage;
    }

    /**
     * 设置合成方言，当前 chuxif音色支持
     *
     * @param language 方言类型，支持粤语 cantonese 四川话 sichuanese
     */
    public void setLanguage(String language) {
        mLanguage = language;
    }

    public AudioAttributes getAudioAttributes() {
        return audioAttributes;
    }

    /**
     * 设置音频属性
     * use {{@link (AudioAttributes)}}
     */
    public void setAudioAttributes(AudioAttributes audioAttributes) {
        this.audioAttributes = audioAttributes;
    }

    public boolean isUseStreamType() {
        return useStreamType;
    }

    public void setUseStreamType(boolean useStreamType) {
        this.useStreamType = useStreamType;
    }

}
