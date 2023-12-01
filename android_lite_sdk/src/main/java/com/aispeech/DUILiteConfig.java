package com.aispeech;


import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.text.TextUtils;

import com.aispeech.common.AISpeechBridge;
import com.aispeech.common.Log;
import com.aispeech.export.config.AuthConfig;
import com.aispeech.export.config.RecorderConfig;
import com.aispeech.export.config.UploadConfig;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;

import java.util.HashMap;
import java.util.Map;

import okhttp3.OkHttpClient;

/**
 * sdk 配置类，包括设置产品的相关信息 apiKey, productId, productKey, productSecret，以及其它的全局配置
 */
public class DUILiteConfig implements Cloneable {

    /**
     * 音频都不上传
     */
    public static final int UPLOAD_AUDIO_LEVEL_NONE = 0x00;
    /**
     * 只上传预唤醒音频
     */
    public static final byte UPLOAD_AUDIO_LEVEL_PREWAKEUP = 0x01;
    /**
     * 只上传唤醒音频
     */
    public static final int UPLOAD_AUDIO_LEVEL_WAKEUP = 0x10;
    /**
     * 上传唤醒和预唤醒音频
     */
    public static final int UPLOAD_AUDIO_LEVEL_ALL = 0x11;
    /**
     * 普通单麦模式SDK,获取单通道音频
     */
    public static final int TYPE_COMMON_MIC = 0;
    /**
     * 家居双麦模式SDK,获取双通道音频
     */
    public static final int TYPE_COMMON_DUAL = 1;
    /**
     * 线性4麦模式SDK,获取四通道音频
     */
    public static final int TYPE_COMMON_LINE4 = 2;
    /**
     * 环形6麦模式SDK,获取六通道音频
     */
    public static final int TYPE_COMMON_CIRCLE6 = 3;
    /**
     * echo模式SDK,获取双通道音频(包含一路参考音)
     */
    public static final int TYPE_COMMON_ECHO = 4;
    /**
     * 车载双麦模式SDK,获取双通道音频
     */
    public static final int TYPE_COMMON_FESPCAR = 5;
    /**
     * 环形4麦模式SDK,获取四通道音频
     */
    public static final int TYPE_COMMON_CIRCLE4 = 6;
    /**
     * 家居双麦通过TinyCap方式录音，获取2+2音频
     */
    public static final int TYPE_TINYCAP_DUAL = 7;
    /**
     * 线性四麦通过TinyCap方式录音，获取4+2音频
     */
    public static final int TYPE_TINYCAP_LINE4 = 8;
    /**
     * 线性四麦通过TinyCap方式录音，获取4+2音频
     */
    public static final int TYPE_TINYCAP_CIRCLE4 = 9;
    /**
     * 线性6麦模式SDK,获取六通道音频
     */
    public static final int TYPE_COMMON_LINE6 = 10;
    /**
     * L形状4麦模式SDK,获取4路音频
     */
    public static final int TYPE_COMMON_SHAPE_L4 = 11;
    /**
     * 线性六麦通过TinyCap方式录音，获取6+2音频
     */
    public static final int TYPE_TINYCAP_LINE6 = 12;
    /**
     * 环形六麦通过TinyCap方式录音，获取6+2音频
     */
    public static final int TYPE_TINYCAP_CIRCLE6 = 13;
    /**
     * 线性8麦模式SDK,获取八通道音频
     */
    public static final int TYPE_COMMON_LINE8 = 14;

    /**
     * 车载四麦模式SDK，使用SSPE四音区，获取四通道音频
     */
    public static final int TYPE_COMMON_FESPCAR4 = 15;

    /**
     * assets目录下的离线授权文件名<br>
     * <p>
     * 取值：授权文件名
     * 是否必需：否<br>
     */
    public static String offlineProfileName = null;
    /**
     * MediaRecorder.AudioSource.VOICE_RECOGNITION声音源
     */
    public static int audioSourceType_VOICE_RECOGNITION = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    /**
     * MediaRecorder.AudioSource.MIC声音源
     */
    public static int audioSourceType_MIC = MediaRecorder.AudioSource.MIC;
    /**
     * AudioRecord Provider
     * 如果用户想让我们内部控制录音机，但是外部实现，那就设置这个。
     */
    private static ExternalAudioRecordProvider ExternalAudioRecordProvider;
    /**
     * 授权网络连接超时时间, 单位：毫秒，default is 5000ms
     */
    private int authTimeout = AISpeech.authTimeout;
    /**
     * 当试用授权文件认证检查时，是否尝试更新为在线授权文件。default is true
     * <p>不进行联网更新授权文件才需要设置<br></p>
     */
    private boolean updateTrailProfileToOnlineProfile = true;

    /**
     * 是否开启唤醒词的非法拼音检测
     */
    private boolean illegalPingyinCheck = true;

    /**
     * 授权文件保存目录的绝对路径<br>
     * <p>
     * 取值：绝对路径字符串, e.g./sdard/speech<br>
     * 默认存放目录 /data/data/包名/files/
     */
    private String deviceProfileDirPath = null;
    /**
     * 音频文件的保存目录的绝对路径
     * 取值：绝对路径字符串
     * 设置则全局保存音频文件，单项功能如果设置文件保存路径，则以单项功能路径为准；如果单项功能没有设置，则使用全局变量操作
     */
    private String localSaveAudioPath = null;
    /**
     * 离线状态下最大缓存文件数据个数, default is 100<br>
     */
    private int cacheUploadMaxNumber = AISpeech.cacheUploadMaxNumber;
    /**
     * 设置算法绑定线程开关，降低CPU占用，默认值：0 <br>
     * 取值：具体的核id
     */
    private int threadAffinity = AISpeech.threadAffinity;


    /**
     * 大数据上传跟更新配置的时间间隔
     * 单位：ms
     */
    private   int uploadConfigInterval = 24 * 60 * 60 * 1000;//默认24小时



    /**
     * 设置TTS cache 目录
     * <p>
     * 取值：绝对路径字符串,e.g./sdcard/speech<br>
     * 是否必需：否 <br>
     * 默认值：/sdcard/Android/data/包名/cache
     */
    private String ttsCacheDir;

    /**
     * 上传音频等级, default is {@link #UPLOAD_AUDIO_LEVEL_NONE}<br>
     * 音频都不上传{@link #UPLOAD_AUDIO_LEVEL_NONE}<br>
     * 只上传唤醒音频{@link #UPLOAD_AUDIO_LEVEL_WAKEUP}<br>
     * 只上传预唤醒音频{@link #UPLOAD_AUDIO_LEVEL_PREWAKEUP}<br>
     * 上传唤醒和预唤醒音频{@link #UPLOAD_AUDIO_LEVEL_ALL}<br>
     */
    private int uploadAudioLevel = UPLOAD_AUDIO_LEVEL_NONE;
    /**
     * 上传大数据的唤醒音频保存的路径
     */
    private String uploadAudioPath = null;
    /**
     * 上传唤醒音频的延迟时间，单位:毫秒，default is {@value}毫秒 <br>
     * 0表示不延迟上传，即收到唤醒音频后立即上传。&gt;0 时延迟有效，建议在1分钟到10分钟内<br>
     * 前置条件：{@link #uploadAudioLevel} <strong>不能为</strong> {@link #UPLOAD_AUDIO_LEVEL_NONE}
     */
    private int uploadAudioDelayTime = 5 * 60 * 1000;
    /**
     * 设置大数据上传url地址 <br>
     * <p>
     * 取值：字符串<br>
     * 是否必需：否<br>
     * 默认值：https://log.aispeech.com<br>
     */
    private String uploadUrl = "https://log.aispeech.com";
    private String authServer = "https://auth.duiopen.com";
    /**
     * 录音机采集数据间隔
     */
    private int intervalTime = 100;
    /**
     * 音频源默认为{@link MediaRecorder.AudioSource#VOICE_RECOGNITION}
     * AudioSource.MIC  麦克风
     * AudioSource.VOICE_RECOGNITION 语音识别
     */
    private int audioSourceType = 0;//MediaRecorder.AudioSource.VOICE_RECOGNITION
    /**
     * 默认为普通单麦模式
     */
    private int audioRecorderType = TYPE_COMMON_MIC;
    private boolean maxVolumeMode = false;
    private Map<String, String> extraParameter = new HashMap<>();

    /**
     * 支持外部配置是否缩放声音能量幅值
     * 取值： [0,5.0]
     */
    public static final String CONFIG_KEY_ZOOM_AUDIO_RATE = "ZOOM_AUDIO_RATE";
    /**
     * 支持指定 asr 或者 vad 外部配置是否缩放声音能量幅值
     * 默认都放大
     */
    public static final String CONFIG_KEY_ZOOM_AUDIO_FLAG = "ZOOM_AUDIO_FLAG";
    public static final int CONFIG_VAL_ZOOM_AUDIO_FLAG_VAD = 0x1;
    public static final int CONFIG_VAL_ZOOM_AUDIO_FLAG_ASR = 0x1 << 1;

    /**
     * 支持指定 vad out音频 外部配置是否缩放声音能量幅值
     * 默认不放大
     */
    public static final int CONFIG_VAL_ZOOM_AUDIO_FLAG_VAD_OUT = 0x1 << 2;

    /**
     * 从DUI平台产品里获取的APPKEY
     */
    private String apiKey;

    /**
     * API接入KEY
     */
    private String serverApiKey;

    /**
     * 产品ID
     */
    private String productId;
    /**
     * 产品KEY
     */
    private String productKey;
    /**
     * 产品SECRET
     */
    private String productSecret;
    /**
     * buildModel，将原样转递给post调用
     */
    private String buildModel;
    /**
     * 设置 SDK信息（异常监控信息/音频）是否上传，默认上传
     */
    private boolean monitorUploadEnable = true;
    private boolean logEnable = false;
    private String logFilePath = null;
    /**
     * 授权在网络错误时重试的次数，默认为0，不重试
     */
    private int authRetryTimesForNetworkErr = 0;
    private boolean callbackInThread;
    private AIEchoConfig aiEchoConfig;

    /**
     * 录音机配置
     */
    private RecorderConfig recorderConfig;
    /**
     * 队列大小
     */
    private int maxMessageQueueSize = 0;

    /**
     * 是否使用思必驰提供的DNS
     */
    private boolean useSpeechDns = true;

    /**
     * 是否开启双VAD模式
     */
    private boolean useDoubleVad = false;

    /**
     * 设置是否支持sha1加密
     *
     * @return
     */
    private boolean encryptCustomDeviceName = false;

    public DUILiteConfig(String apiKey, String productId, String productKey, String productSecret) {
        this(apiKey, productId, productKey, productSecret, "", false);
    }

    public DUILiteConfig(String apiKey, String productId, String productKey, String productSecret, String ttsCacheDir, boolean useDoubleVad) {
        this.productId = productId;
        this.productKey = productKey;
        this.productSecret = productSecret;
        this.apiKey = apiKey;
        this.ttsCacheDir = ttsCacheDir;
        this.useDoubleVad = useDoubleVad;
        if (!isProductInfoValid())
            throw new IllegalArgumentException("ProductInfo set invalid, at least one in productId|productKey|productSecret|apiKey is empty");
    }

    /**
     * 是否提前返回授权成功逻辑，用以优化授权速度
     */
    private boolean ignoreLogin = false;
    /**
     * 队列大小
     */
    private boolean localSaveEnabled = true;
    /**
     * 设置 sdk 缓存唤醒、预唤醒音频文件，默认不缓存
     */
    private boolean cacheUploadEnable;

    public boolean isEncryptCustomDeviceName() {
        return encryptCustomDeviceName;
    }

    public void setEncryptCustomDeviceName(boolean encryptCustomDeviceName) {
        this.encryptCustomDeviceName = encryptCustomDeviceName;
    }

    public static ExternalAudioRecordProvider getExternalAudioRecordProvider() {
        return ExternalAudioRecordProvider;
    }

    public static void setExternalAudioRecordProvider(ExternalAudioRecordProvider provider) {
        ExternalAudioRecordProvider = provider;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }

    public boolean isIgnoreLogin() {
        return ignoreLogin;
    }

    /**
     * 设置是否优化授权速度
     *
     * @param ignoreLogin 默认false
     */
    public void setIgnoreLogin(boolean ignoreLogin) {
        this.ignoreLogin = ignoreLogin;
    }

    /**
     * 设置大数据上传url地址 <br>
     * <p>
     * 取值：字符串<br>
     * 是否必需：否<br>
     * 默认值：https://log.aispeech.com<br>
     *
     * @param uploadUrl 大数据上传url地址
     */
    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getAuthServer() {
        return authServer;
    }

    /**
     * 设置DUI授权url地址 <br>
     * <p>
     * 取值：字符串<br>
     * 是否必需：否<br>
     * 默认值：https://auth.duiopen.com<br>
     * 对应ip:
     * 47.111.106.49
     * 47.110.225.67
     * 47.111.81.103
     * 47.110.248.171
     *
     * @param authServer DUI授权url地址
     */
    public void setAuthServer(String authServer) {
        this.authServer = authServer;
    }

    public int getAuthTimeout() {
        return authTimeout;
    }

    /**
     * 授权网络连接超时时间<br>
     * <p>
     * 单位：毫秒 默认值：5000ms<br>
     *
     * @param authTimeout 网络连接超时时间
     */
    public void setAuthTimeout(int authTimeout) {
        this.authTimeout = authTimeout;
    }

    public boolean isUpdateTrailProfileToOnlineProfile() {
        return updateTrailProfileToOnlineProfile;
    }


    /**
     * 支持外部配置是否启用信号处理双VAD
     * 设置sspe信号处理返回多路音频，以及VAD加载多路音频
     * 取值： true or false 是否使用双VAD
     */
    public boolean isUseDoubleVad() {
        return useDoubleVad;
    }

    public void setUseDoubleVad(boolean useDoubleVad) {
        this.useDoubleVad = useDoubleVad;
    }

    /**
     * 当试用授权文件认证检查时，是否尝试更新为在线授权文件。default is true
     * <p>不进行联网更新授权文件才需要设置<br></p>
     *
     * @param updateTrailProfileToOnlineProfile 是否尝试更新为在线授权文件
     */
    public void setUpdateTrailProfileToOnlineProfile(boolean updateTrailProfileToOnlineProfile) {
        this.updateTrailProfileToOnlineProfile = updateTrailProfileToOnlineProfile;
    }

    public String getDeviceProfileDirPath() {
        return deviceProfileDirPath;
    }

    /**
     * 授权文件保存目录的绝对路径 <br>
     * <p>
     * 取值：绝对路径字符串, e.g./sdard/speech<br>
     * 默认存放目录 /data/data/包名/files/
     *
     * @param deviceProfileDirPath 授权文件保存目录的绝对路径
     */
    public void setDeviceProfileDirPath(String deviceProfileDirPath) {
        this.deviceProfileDirPath = deviceProfileDirPath;
    }

    public String getOfflineProfileName() {
        return offlineProfileName;
    }

    /**
     * assets目录下的离线授权文件名<br>
     * <p>
     * 取值：授权文件名
     * 是否必需：否<br>
     *
     * @param offlineProfileName 离线授权文件名
     */
    public void setOfflineProfileName(String offlineProfileName) {
        DUILiteConfig.offlineProfileName = offlineProfileName;
    }

    /**
     * @return 获取数据上传的数量
     * @deprecated 废弃
     */
    @Deprecated
    public int getCacheUploadMaxNumber() {
        return cacheUploadMaxNumber;
    }

    /**
     * 离线状态下最大缓存文件数据个数, default is 100 <br>
     *
     * @param cacheUploadMaxNumber 最大缓存文件数据个数
     * @deprecated 废弃
     */
    @Deprecated
    public void setCacheUploadMaxNumber(int cacheUploadMaxNumber) {
        this.cacheUploadMaxNumber = cacheUploadMaxNumber;
    }

    public int getThreadAffinity() {
        return threadAffinity;
    }

    public String getTtsCacheDir() {
        return ttsCacheDir;
    }

    /**
     * 设置算法绑定线程开关，降低CPU占用，默认值：0 <br>
     * 取值：具体的核id
     *
     * @param threadAffinity 具体的核id
     */
    public void setThreadAffinity(int threadAffinity) {
        this.threadAffinity = threadAffinity;
    }

    public String getLocalSaveAudioPath() {
        return localSaveAudioPath;
    }

    /**
     * 设置全局音频文件的保存目录的绝对路径
     * 设置则全局保存音频文件，单项功能如果设置文件保存路径，则以单项功能路径为准；如果单项功能没有设置，则使用全局变量操作
     *
     * @param localSaveAudioPath 绝对路径字符串
     * @deprecated use {@link DUILiteSDK#openGlobalAudioSave(String)}
     */
    public void setLocalSaveAudioPath(String localSaveAudioPath) {
        this.localSaveAudioPath = localSaveAudioPath;
    }

    /**
     * 设置全局音频文件的保存目录的绝对路径
     * 设置则全局保存音频文件，单项功能如果设置文件保存路径，则以单项功能路径为准；如果单项功能没有设置，则使用全局变量操作
     *
     * @param localSaveAudioPath 绝对路径字符串
     * @param localSaveEnabled   动态设置音频保存
     * @deprecated use {@link DUILiteSDK#openGlobalAudioSave(String)}
     */
    public void setLocalSaveAudioPath(String localSaveAudioPath, boolean localSaveEnabled) {
        this.localSaveAudioPath = localSaveAudioPath;
        this.localSaveEnabled = localSaveEnabled;
    }


    public boolean isCacheUploadEnable() {
        return cacheUploadEnable;
    }

    /**
     * 设置 sdk 缓存唤醒、预唤醒音频文件，默认不缓存
     */
    public void setCacheUploadEnable(boolean cacheUploadEnable) {
        this.cacheUploadEnable = cacheUploadEnable;
    }

    /**
     * 设置大数据上传配置的更新时间间隔
     * 默认24小时
     *
     * @param uploadConfigInterval 单位：ms
     */
    private void setUploadConfigInterval(int uploadConfigInterval) {
        this.uploadConfigInterval = uploadConfigInterval;
    }

    public int getUploadAudioLevel() {
        return uploadAudioLevel;
    }

    /**
     * 上传音频等级, default is {@link #UPLOAD_AUDIO_LEVEL_NONE}<br>
     * 音频都不上传{@link #UPLOAD_AUDIO_LEVEL_NONE}<br>
     * 只上传唤醒音频{@link #UPLOAD_AUDIO_LEVEL_WAKEUP}<br>
     * 只上传预唤醒音频{@link #UPLOAD_AUDIO_LEVEL_PREWAKEUP}<br>
     * 上传唤醒和预唤醒音频{@link #UPLOAD_AUDIO_LEVEL_ALL}<br>
     *
     * @param uploadAudioLevel 上传音频等级
     * @deprecated 废弃，当前由服务端控制
     */
    @Deprecated
    public void setUploadAudioLevel(int uploadAudioLevel) {
        this.uploadAudioLevel = uploadAudioLevel;
    }

    public String getUploadAudioPath() {
        return uploadAudioPath;
    }

    /**
     * 设置大数据上传唤醒音频的本地存储路径，如果不设置，默认存储在sd卡的cache目录
     *
     * @param uploadAudioPath 唤醒音频的本地存储路径
     */
    public void setUploadAudioPath(String uploadAudioPath) {
        this.uploadAudioPath = uploadAudioPath;
    }

    /**
     * 音频上传延时时间
     *
     * @return 延时
     * @deprecated 废弃
     */
    @Deprecated
    public int getUploadAudioDelayTime() {
        return uploadAudioDelayTime;
    }

    /**
     * 设置 上传音频等级 和 上传唤醒音频的延迟时间
     *
     * @param uploadAudioLevel     上传音频等级, default is {@link #UPLOAD_AUDIO_LEVEL_NONE}<br>
     *                             音频都不上传{@link #UPLOAD_AUDIO_LEVEL_NONE}<br>
     *                             只上传唤醒音频{@link #UPLOAD_AUDIO_LEVEL_WAKEUP}<br>
     *                             只上传预唤醒音频{@link #UPLOAD_AUDIO_LEVEL_PREWAKEUP}<br>
     *                             上传唤醒和预唤醒音频{@link #UPLOAD_AUDIO_LEVEL_ALL}<br>
     * @param uploadAudioDelayTime 上传唤醒音频的延迟时间，单位:毫秒，default is 300000毫秒 <br>
     *                             0表示不延迟上传，即收到唤醒音频后立即上传。&gt;0 时延迟有效，建议在1分钟到10分钟内<br>
     *                             前置条件：{@link #uploadAudioLevel} <strong>不能为</strong> {@link #UPLOAD_AUDIO_LEVEL_NONE}
     * @deprecated 废弃，当前由服务端控制上传等级
     */
    @Deprecated
    public void setUploadAudioLevel(int uploadAudioLevel, int uploadAudioDelayTime) {
        this.uploadAudioLevel = uploadAudioLevel;
        this.uploadAudioDelayTime = uploadAudioDelayTime;
    }

    public int getIntervalTime() {
        return intervalTime;
    }

    /**
     * 设置录音机录音间隔，单位ms
     *
     * @param recordInterValTime 音频间隔
     */
    public void setIntervalTime(int recordInterValTime) {
        if (recordInterValTime > 0)
            intervalTime = recordInterValTime;
        else
            intervalTime = 100;
    }

    public int getAudioSourceType() {
        return audioSourceType;
    }

    /**
     * 设置audioRecorder的声音源
     *
     * @param audioSourceType 默认为VOICE_RECOGNITION
     *                        可选值：{@link #audioSourceType_VOICE_RECOGNITION}和
     *                        {@link #audioSourceType_MIC}
     */
    public void setAudioSourceType(int audioSourceType) {
        this.audioSourceType = audioSourceType;
    }

    public int getAudioRecorderType() {
        return audioRecorderType;
    }

    /**
     * 设置录音机采集数据的方式
     *
     * @param type 默认为common_dual
     */
    public void setAudioRecorderType(int type) {
        audioRecorderType = type;
    }

    /**
     * 获取当前大音量模式
     *
     * @return maxVolumeMode
     */
    public boolean isMaxVolumeMode() {
        return maxVolumeMode;
    }

    /**
     * 设置开启线性四麦大音量检测模式
     *
     * @param isMaxVolumeMode 大音量模式开关，默认关闭为false
     */
    public void setMaxVolumeMode(boolean isMaxVolumeMode) {
        maxVolumeMode = isMaxVolumeMode;
    }

    /**
     * 参数设置，主要用于设置devicename和deviceID，其中，devicename是设备计量的标识，可以根据需要自行设置
     * 如果不设置，服务器会生成一个devicename。
     * {@code
     * //设置deviceName
     * setExtraParameter("DEVICE_NAME","deviceName");
     * //也可以使用deviceID作为设备的唯一标识符
     * setExtraParameter("DEVICE_ID","deviceId");
     * setExtraParameter("DEVICE_NAME_TYPE", "deviceId");
     * //使用预分配方案
     * setExtraParameter("LICENSE_ID", "licenseId");
     * }
     * 需在init前调用生效
     *
     * @param key   key
     * @param value value
     */
    public void setExtraParameter(String key, String value) {
        extraParameter.put(key, value);
    }

    /**
     * 参数设置
     * 需在init前调用生效
     *
     * @param key   key
     * @param value value
     */
    public void setExtraParameter(String key, int value) {
        extraParameter.put(key, String.valueOf(value));
    }

    public boolean isUseSpeechDns() {
        return useSpeechDns;
    }

    /**
     * 设置是否使用思必驰的DNS
     *
     * @param useSpeechDns 是否使用思必驰的DNS，默认为true
     */
    public void setUseSpeechDns(boolean useSpeechDns) {
        this.useSpeechDns = useSpeechDns;
    }

    public Map<String, String> getExtraParameter() {
        return extraParameter;
    }

    public boolean isProductInfoValid() {
        if ("4ff3171a1ef1f122381692fa5a2f52ea".equals(apiKey)) {
            return true;
        }
        return (!TextUtils.isEmpty(this.apiKey) && !TextUtils.isEmpty(this.productId)
                && !TextUtils.isEmpty(this.productKey) && !TextUtils.isEmpty(this.productSecret));
    }

    public String getApiKey() {
        return apiKey;
    }


    public void setBuildModel(String buildModel) {
        this.buildModel = buildModel;
    }

    public String getBuildModel() {
        return buildModel;
    }

    public String getProductId() {
        return productId;
    }

    public String getProductKey() {
        return productKey;
    }

    public String getProductSecret() {
        return productSecret;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        DUILiteConfig config = (DUILiteConfig) super.clone();
        config.extraParameter = this.extraParameter;
        return config;
    }

    public boolean isMonitorUploadEnable() {
        return monitorUploadEnable;
    }

    /**
     * 设置 SDK信息（异常监控信息）是否上传，默认上传
     *
     * @param monitorUploadEnable true 上传，false 不上传
     */
    public void setMonitorUploadEnable(boolean monitorUploadEnable) {
        this.monitorUploadEnable = monitorUploadEnable;
    }

    public void setIllegalPingyinCheck(boolean illegalPingyinCheck) {
        this.illegalPingyinCheck = illegalPingyinCheck;
    }

    public boolean isLogEnable() {
        return logEnable;
    }

    public String getLogFilePath() {
        return logFilePath;
    }

    public boolean getIllegalPingyinCheck() {
        return illegalPingyinCheck;
    }

    /**
     * 打开sdk日志
     * 日志打开，性能会受影响，调试时可打开日志, 默认日志关闭
     *
     * @see DUILiteSDK#setDebugMode(int)
     * @deprecated 废弃
     */
    @Deprecated
    public void openLog() {
        openLog(null);
    }

    /**
     * 打开sdk日志
     * 日志打开，性能会受影响，调试时可打开日志, 默认日志关闭
     *
     * @param logFilePath 保存的日志文件路径，包含文件名，比如"/sdcard/duilite/DUILite_SDK.log"
     * @see DUILiteSDK#setDebugMode(int, String)
     * @deprecated
     */
    @Deprecated
    public void openLog(String logFilePath) {
        logEnable = true;
        this.logFilePath = logFilePath;
    }

    public int getAuthRetryTimesForNetworkErr() {
        return authRetryTimesForNetworkErr;
    }

    /**
     * 支持外部配置OkHttpClient.Builder对象
     * 用于CloudAsr、CloudDM、CloudSemantic等功能创建WebSocket长连接
     */
    private OkHttpClient.Builder webSocketBuilder;

    /**
     * 日志上传配置
     */
    private UploadConfig uploadConfig;

    /**
     * 授权配置
     */
    private AuthConfig authConfig;

    /**
     * 授权在网络错误时重试的次数
     *
     * @param authRetryTimesForNetworkErr 授权重试次数，默认为0，不重试
     */
    public void setAuthRetryTimesForNetworkErr(int authRetryTimesForNetworkErr) {
        this.authRetryTimesForNetworkErr = authRetryTimesForNetworkErr;
    }

    public boolean isCallbackInThread() {
        return callbackInThread;
    }

    public void setUploadConfig(UploadConfig uploadConfig) {
        this.uploadConfig = uploadConfig;
    }

    /**
     * 设置 sdk 的回调是否都在子线程里，默认 false
     *
     * @param callbackInThread false 文本结果在主线程回调，音频在子线程回调，true 全部在子线程里回调
     */
    public void setCallbackInThread(boolean callbackInThread) {
        this.callbackInThread = callbackInThread;
    }

    public AIEchoConfig getEchoConfig() {
        return aiEchoConfig;
    }

    /**
     * 设置 ECHO 模式的配置，其它模式下无影响
     *
     * @param echoConfig ECHO 模式的配置
     */
    public void setEchoConfig(AIEchoConfig echoConfig) {
        this.aiEchoConfig = echoConfig;
    }

    public int getMaxMessageQueueSize() {
        return maxMessageQueueSize;
    }

    public AuthConfig getAuthConfig() {
        return authConfig;
    }

    public RecorderConfig getRecorderConfig() {
        return recorderConfig;
    }

    public UploadConfig getUploadConfig() {
        return uploadConfig;
    }

    /**
     * 设置消息队列最大长度，
     * <p>默认0, 0表示不限制长度, 建议大于100</p>
     * <p>动态库方法运行在一个单独的线程里，通过消息队列依次调用。
     * 在设备性能不好的设备上可以设置消息队列最大长度，防止算力不够导致内核无法及时处理完音频数据而导致内存过大的问题</p>
     *
     * @param maxMessageQueueSize 消息队列最大长度
     */
    public void setMaxMessageQueueSize(int maxMessageQueueSize) {
        this.maxMessageQueueSize = maxMessageQueueSize;
    }

    private DUILiteConfig(String apiKey, String serverApiKey, String productId, String productKey,
                          String productSecret, boolean callbackInThread, boolean useSystemDns,
                          AIEchoConfig echoConfig, RecorderConfig recorderConfig,
                          UploadConfig uploadConfig, AuthConfig authConfig,
                          String ttsCacheDir, int threadAffinity,
                          OkHttpClient.Builder webSocketBuilder,
                          boolean illegalPingyinCheck, boolean useDoubleVad,
                          int maxMessageQueueSize,
                          String buildModel) {
        this.apiKey = apiKey;
        this.serverApiKey = serverApiKey;
        this.productId = productId;
        this.productKey = productKey;
        this.productSecret = productSecret;
        this.callbackInThread = callbackInThread;
        this.useSpeechDns = useSystemDns;
        this.aiEchoConfig = echoConfig;
        this.recorderConfig = recorderConfig;
        this.uploadConfig = uploadConfig;
        this.authConfig = authConfig;
        this.ttsCacheDir = ttsCacheDir;
        this.threadAffinity = threadAffinity;
        this.webSocketBuilder = webSocketBuilder;
        this.illegalPingyinCheck = illegalPingyinCheck;
        this.useDoubleVad = useDoubleVad;
        this.maxMessageQueueSize = maxMessageQueueSize;
        this.buildModel = buildModel;
    }

    private DUILiteConfig(Builder builder) {
        this(builder.apiKey,
                builder.serverApiKey,
                builder.productId,
                builder.productKey,
                builder.productSecret,
                builder.callbackInThread,
                builder.useSystemDns,
                builder.echoConfig,
                builder.recorderConfig,
                builder.uploadConfig,
                builder.authConfig,
                builder.ttsCacheDir,
                builder.threadAffinity,
                builder.webSocketBuilder,
                builder.illegalPingyinCheck,
                builder.useDoubleVad,
                builder.maxMessageQueueSize,
                builder.buildModel
        );
    }

    public static class Builder {

        /**
         * api key
         */
        private String apiKey;

        /**
         * API接入KEY
         */
        private String serverApiKey;

        /**
         * 产品ID
         */
        private String productId;

        /**
         * 产品KEY
         */
        private String productKey;

        /**
         * 产品SECRET
         */
        private String productSecret;

        private String buildModel;

        /***
         * 回调线程配置
         */
        private boolean callbackInThread;

        /**
         * 回声消除配置
         */
        private AIEchoConfig echoConfig;

        /**
         * 录音机配置
         */
        private RecorderConfig recorderConfig;

        /**
         * 日志上传配置
         */
        private UploadConfig uploadConfig;

        /**
         * 授权配置
         */
        private AuthConfig authConfig;

        /**
         * 设置TTS cache 目录
         * <p>
         * 取值：绝对路径字符串,e.g./sdcard/speech<br>
         * 是否必需：否 <br>
         * 默认值：/sdcard/Android/data/包名/cache
         */
        private String ttsCacheDir;

        /**
         * 设置算法绑定线程开关，降低CPU占用，默认值：0 <br>
         * 取值：具体的核id
         */
        private int threadAffinity = 0;

        /**
         * 是否开启唤醒词的非法拼音检测
         */
        private boolean illegalPingyinCheck = true;

        /**
         * 设置sdk是否使用Speech dns解析
         * true 使用思必驰系统解析  false 使用系统解析  默认为false
         */
        private boolean useSystemDns = false;

        /**
         * 增加setWebSocketBuilder接口，支持外部配置OkHttpClient.Builder对象
         * 用于CloudAsr、CloudDM、CloudSemantic等功能与服务器的交互
         */
        private OkHttpClient.Builder webSocketBuilder;
        /**
         * 队列大小
         */
        private int maxMessageQueueSize = 0;
        /**
         * 支持外部配置是否启用信号处理双VAD
         * 设置sspe信号处理返回多路音频，以及VAD加载多路音频
         * 取值： true or false 是否使用双VAD
         */
        private boolean useDoubleVad = false;

        public Builder setApiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder setServerApiKey(String serverApiKey) {
            this.serverApiKey = serverApiKey;
            return this;
        }

        public Builder setBuildModel(String buildModel) {
            this.buildModel = buildModel;
            return this;
        }

        public Builder setProductId(String productId) {
            this.productId = productId;
            return this;
        }

        public Builder setProductKey(String productKey) {
            this.productKey = productKey;
            return this;
        }

        public Builder setProductSecret(String productSecret) {
            this.productSecret = productSecret;
            return this;
        }

        public Builder setCallBackInThread(boolean callbackInThread) {
            this.callbackInThread = callbackInThread;
            return this;
        }

        public Builder setWebSocketBuilder(OkHttpClient.Builder webSocketBuilder) {
            this.webSocketBuilder = webSocketBuilder;
            return this;
        }

        public Builder setUseSystemDns(boolean useSystemDns) {
            this.useSystemDns = useSystemDns;
            return this;
        }

        public Builder setUseDoubleVad(boolean useDoubleVad) {
            this.useDoubleVad = useDoubleVad;
            return this;
        }

        /**
         * 设置回声消除参数
         *
         * @param echoConfig 回声消除参数
         * @return builder
         */
        public Builder setEchoConfig(AIEchoConfig echoConfig) {
            this.echoConfig = echoConfig;
            return this;
        }

        public Builder setRecorderConfig(RecorderConfig recorderConfig) {
            this.recorderConfig = recorderConfig;
            return this;
        }

        public Builder setUploadConfig(UploadConfig uploadConfig) {
            this.uploadConfig = uploadConfig;
            return this;
        }

        public Builder setAuthConfig(AuthConfig authConfig) {
            this.authConfig = authConfig;
            return this;
        }

        public Builder setTtsCacheDir(String ttsCacheDir) {
            this.ttsCacheDir = ttsCacheDir;
            return this;
        }

        public Builder setThreadAffinity(int threadAffinity) {
            this.threadAffinity = threadAffinity;
            return this;
        }

        public Builder setIllegalPingyinCheck(boolean illegalPingyinCheck) {
            this.illegalPingyinCheck = illegalPingyinCheck;
            return this;
        }

        public Builder setMaxMessageQueueSize(int maxMessageQueueSize) {
            this.maxMessageQueueSize = maxMessageQueueSize;
            return this;
        }

        /**
         * 设置全局总音频缓存总大小
         *
         * @param globalAudioFileAllSize 总音频缓存大小，默认为0，不做限制
         */
        public Builder setAudioFileTotalSize(int globalAudioFileAllSize) {
            AISpeechSDK.GLOBAL_AUDIO_FILE_ALL_SIZE = globalAudioFileAllSize;
            return this;
        }

        /**
         * 设置全局单音频缓存策略
         *
         * @param singleSize  单音频缓存大小
         * @param splitNumber 拆分数量
         */
        public Builder setAudioFileCacheStrategy(int singleSize, int splitNumber) {
            AISpeechSDK.GLOBAL_AUDIO_FILE_SINGLE_SIZE = singleSize;
            AISpeechSDK.GLOBAL_AUDIO_FILE_SPLIT_NUMBER = splitNumber;
            return this;
        }

        public Builder addLiteConfig(String key, String value) {
            Log.i("addLiteConfig", key + " : " + value);
            if (!TextUtils.isEmpty(value)) {
                if (TextUtils.equals(key, "packageName")) {
                    AISpeech.sharePkgName = value;
                    AISpeechBridge.authPackageName = value;
                } else if (TextUtils.equals(key, "SHA256")) {
                    AISpeech.shareSHA256 = value;
                    AISpeechBridge.authSHA256 = value;
                } else if (TextUtils.equals(key, CONFIG_KEY_ZOOM_AUDIO_RATE)) {
                    try {
                        AISpeech.zoomAudioRate = Float.parseFloat(value);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else if (TextUtils.equals(key, CONFIG_KEY_ZOOM_AUDIO_FLAG)) {
                    try {
                        AISpeech.zoomAudioFlag = Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                } else if (TextUtils.equals(key, "OFFLINE_ENGINES_SCOPE")) {
                    AISpeech.offlineEngineAuth = value;
                    AISpeechBridge.offlineEngineAuth = value;
                }
            }
            return this;
        }

        /**
         * 检查授权信息是否有效
         *
         * @param apiKey        从DUI平台产品里获取的APPKEY
         * @param productId     产品ID
         * @param productKey    产品KEY
         * @param productSecret 产品SECRET
         * @return
         */
        private boolean isProductInfoValid(String productId, String apiKey, String productKey, String productSecret) {
            if (isNewAuthValid(productId, apiKey, productKey, productSecret) || isOldAuthValid(productId, apiKey)) {
                return true;
            }
            return false;
        }

        /**
         * 录音类型为echo模式，判断是否有设置echo资源
         *
         * @param echoConfig     {@link AIEchoConfig}
         * @param recorderConfig {@link RecorderConfig}
         * @return boolean
         */
        private boolean isEchoInfoValid(AIEchoConfig echoConfig, RecorderConfig recorderConfig) {

            if (recorderConfig != null && recorderConfig.recorderType == RecorderConfig.TYPE_COMMON_ECHO) {
                if (echoConfig == null || TextUtils.isEmpty(echoConfig.getAecResource())) {
                    return false;
                }
            }

            return true;
        }

        /**
         * 老的授权方案,只需要设置 apiKey & productId
         *
         * @param apiKey    从DUI平台产品里获取的APPKEY
         * @param productId 产品ID
         * @return boolean
         */
        private boolean isOldAuthValid(String productId, String apiKey) {

            if (!TextUtils.isEmpty(productId) && !TextUtils.isEmpty(apiKey)) {
                return true;
            }

            return false;

        }

        /**
         * 新的授权方案,需要同时设置 apiKey & productId & productKey & productSecret
         *
         * @param apiKey        从DUI平台产品里获取的APPKEY
         * @param productId     产品ID
         * @param productKey    产品KEY
         * @param productSecret 产品SECRET
         * @return
         */
        private boolean isNewAuthValid(String productId, String apiKey, String productKey, String productSecret) {
            if (!TextUtils.isEmpty(productId)
                    && !TextUtils.isEmpty(apiKey)
                    && !TextUtils.isEmpty(productKey)
                    && !TextUtils.isEmpty(productSecret)) {
                return true;
            }
            return false;
        }

        public DUILiteConfig create() {

            if (!isProductInfoValid(productId, apiKey, productKey, productSecret)) {
                throw new IllegalArgumentException("ProductInfo set invalid, at least one in productId|productKey|productSecret|apiKey is empty");
            }

            if (!isEchoInfoValid(echoConfig, recorderConfig)) {
                throw new IllegalArgumentException("AIEchoConfig cannot be null, pls use DUILiteConfig.setEchoConfig() to set");
            }

            return new DUILiteConfig(this);
        }
    }

    @Override
    public String toString() {
        return "DUILiteConfig{" +
                "authTimeout=" + authTimeout +
                ", updateTrailProfileToOnlineProfile=" + updateTrailProfileToOnlineProfile +
                ", deviceProfileDirPath='" + deviceProfileDirPath + '\'' +
                ", cacheUploadMaxNumber=" + cacheUploadMaxNumber +
                ", threadAffinity=" + threadAffinity +
                ", uploadAudioLevel=" + uploadAudioLevel +
                ", uploadAudioPath='" + uploadAudioPath + '\'' +
                ", uploadAudioDelayTime=" + uploadAudioDelayTime +
                ", localSaveAudioPath=" + localSaveAudioPath +
                ", uploadUrl='" + uploadUrl + '\'' +
                ", authServer='" + authServer + '\'' +
                ", intervalTime=" + intervalTime +
                ", audioSourceType=" + audioSourceType +
                ", audioRecorderType=" + audioRecorderType +
                ", maxVolumeMode=" + maxVolumeMode +
                ", extraParameter=" + extraParameter +
                ", apiKey='" + apiKey + '\'' +
                ", productId='" + productId + '\'' +
                ", productKey='" + productKey + '\'' +
                ", productSecret='" + productSecret + '\'' +
                ", monitorUploadEnable=" + monitorUploadEnable +
                ", logEnable=" + logEnable +
                ", logFilePath='" + logFilePath + '\'' +
                ", authRetryTimesForNetworkErr=" + authRetryTimesForNetworkErr +
                ", callbackInThread=" + callbackInThread +
                ", echoConfig=" + aiEchoConfig +
                ", maxMessageQueueSize=" + maxMessageQueueSize +
                ", useSpeechDns=" + useSpeechDns +
                ", buildModel=" + buildModel +
                '}';
    }

    /**
     * 外部传入录音机的provider
     */
    public interface ExternalAudioRecordProvider {
        /**
         * 相关参数已经给出，参考AudioRecord的构造函数，外部只需要提供生成一个AudioRecord实例的方法，不需要引用这个实例
         *
         * @param audioFormat       audioFormat
         * @param audioSource       audioSource
         * @param bufferSizeInBytes bufferSizeInBytes
         * @param channelConfig     channelConfig
         * @param sampleRateInHz    sampleRateInHz
         * @return 返回一个录音机实例
         */
        AudioRecord provideAudioRecord(int audioSource, int sampleRateInHz,
                                       int channelConfig, int audioFormat, int bufferSizeInBytes);
    }

    public boolean getLocalSaveEnabled() {
        return localSaveEnabled;
    }
}
