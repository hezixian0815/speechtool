package com.aispeech.lite;

import static com.aispeech.DUILiteConfig.TYPE_COMMON_MIC;

import android.content.Context;
import android.media.MediaRecorder;
import android.text.TextUtils;

import com.aispeech.DUILiteConfig;
import com.aispeech.auth.AIProfile;
import com.aispeech.common.Audio2RefSetting;
import com.aispeech.common.Constant;
import com.aispeech.common.Log;
import com.aispeech.net.NetConfig;

import java.util.HashMap;
import java.util.Map;


public class AISpeech {

    private static final String ECHO_AUDIO_DIR = "echo";

    /**
     * 如下定义KEY为隐藏接口使用，不对外暴露
     */
    public static final String KEY_DEVICE_ID = "DEVICE_ID";
    public static final String KEY_DEVICE_NAME = "DEVICE_NAME";
    /**
     * 指定哪个字段为设备的唯一标识
     */
    public static final String KEY_DEVICE_NAME_TYPE = "DEVICE_NAME_TYPE";
    public static final String KEY_CUSTOM_SHA256 = "CUSTOM_SHA256";
    /**
     * licenseID方案授权
     */
    public static final String KEY_LICENSE_ID = "LICENSE_ID";
    /**
     * 多路音频直接获取带2路参考音的音频数据
     */
    public static final String KEY_FESPX_2REFENCE_ENABLE = "KEY_FESPX_2REFENCE_ENABLE";
    public static final String KEY_FESPX_2REFENCE_PATH = "KEY_FESPX_2REFENCE_PATH";
    private static final Map<String, String> map = new HashMap<>();
    public static boolean ECHO_ENABLE = false;
    /**
     * 设置 sdk 的回调是否都在子线程里
     * false 文本结果在主线程回调，音频在子线程回调，true 全部在子线程里回调
     */
    public static boolean callbackInThread;
    /**
     * 消息队列最大长度，
     * <p>默认0, 0表示不限制长度, 建议大于100</p>
     * <p>动态库方法运行在一个单独的线程里，通过消息队列依次调用。
     * 在设备性能不好的设备上可以设置消息队列最大长度，防止算力不够导致内核无法及时处理完音频数据而导致内存过大的问题</p>
     */
    public static int maxMessageQueueSize = 0;
    /**
     * 授权网络连接超时时间, 单位：毫秒，default is {@value}ms
     */
    public static int authTimeout = 5000;
    /**
     * 当试用授权文件认证检查时，是否尝试更新为在线授权文件。default is {@value}
     * <p>不进行联网更新授权文件才需要设置<br></p>
     */
    public static boolean updateTrailProfileToOnlineProfile = true;
    /**
     * 是否开启唤醒词的非法拼音检测
     */
    public static boolean illegalPingyinCheck;
    /**
     * 授权文件保存目录的绝对路径<br>
     * <p>
     * 取值：绝对路径字符串, e.g./sdard/speech<br>
     * 是否必需：否<br>
     * 默认存放目录 /data/data/包名/files/
     */
    public static String deviceProfileDirPath = null;

    /**
     * 授权服务地址
     */
    public static String authServer = "https://auth.duiopen.com";

    /**
     * 自定义设置设备授权id
     * 离线授权方案需要设置该值,需要用户保证deviceId唯一性
     */
    public static String customDeviceId;

    /**
     * 鉴权类型
     */
    public static AuthType authType;

    /**
     * 使用licenceId方案授权
     */
    public static String licenceId;

    /**
     * assets目录下的离线授权文件名<br>
     * <p>
     * 取值：绝对路径字符串, e.g./sdard/speech<br>
     * 是否必需：否<br>
     */
    public static String offlineProfileName = null;

    public static boolean encryptCustomDeviceName = false;

    /**
     * 鉴权应用包名
     * 用于多应用授权
     */
    public static String sharePkgName;


    /***
     * 授权应用签名
     * 用于多应用授权
     */
    public static String shareSHA256;

    /**
     * 支持外部配置是否缩放声音能量幅值
     * 取值： [0,5.0]
     */
    public static float zoomAudioRate = 1.0f;

    /**
     * 音频放大开关标志，1:vad,2:asr,3:vad和asr都放大
     */
    public static int zoomAudioFlag = DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_VAD | DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_ASR;

    /***
     * 热词+可见即可说 start多次以后，reset引擎释放内存
     */
    public static int asrResetMaxSize = 2048;
    /**
     * deviceName only for online auth
     */
    public static String customDeviceName;
//    public static String localSaveAudioPath = null;

    /**
     * 音频数据动态保存开关
     * 默认打开
     */
    private static boolean localSaveEnabled = true;


    /**************  日志上传 *************/
    public static boolean uploadEnable = false;//车载的日志收集，默认关闭

    public static boolean cacheUploadEnable = false;//本地唤醒、预唤醒音频缓存、localAsr音频，默认不缓存
    /**
     * 离线状态下最大缓存文件数据个数, default is {@value} <br>
     */
    public static int cacheUploadMaxNumber = 100;
    /**
     * 设置算法绑定线程开关，降低CPU占用，默认值：0 <br>
     * 取值：具体的核id
     */
    public static int threadAffinity = 0;


    /**
     * echo回调buffer大小，尽量和多音区缓存buffer保持一致或倍数关系，避免2次缓存延时，计算方式：采样率*通道数*16/8/1000*毫秒数
     * 2麦：输入2+2，输出2通道，fespcar期望32ms数据：16000*2*16/8/1000*32=2048
     * 4麦：输入4+2，输出4通道，dmasp期望32ms数据：16000*4*16/8/1000*32=4096
     */
    public static int echoCallbackBufferSize = 2048;

    /**
     * 上传音频等级, default is {@link DUILiteConfig#UPLOAD_AUDIO_LEVEL_NONE}<br>
     * 音频都不上传{@link DUILiteConfig#UPLOAD_AUDIO_LEVEL_NONE}<br>
     * 只上传唤醒音频{@link DUILiteConfig#UPLOAD_AUDIO_LEVEL_WAKEUP}<br>
     * 只上传预唤醒音频{@link DUILiteConfig#UPLOAD_AUDIO_LEVEL_PREWAKEUP}<br>
     * 上传唤醒和预唤醒音频{@link DUILiteConfig#UPLOAD_AUDIO_LEVEL_ALL}<br>
     */
    public static int uploadAudioLevel = DUILiteConfig.UPLOAD_AUDIO_LEVEL_NONE;
    /**
     * 上传唤醒音频的延迟时间，单位:毫秒，default is {@value} 毫秒 <br>
     * 0表示不延迟上传，即收到唤醒音频后立即上传。&gt;0 时延迟有效，建议在1分钟到10分钟内<br>
     * 前置条件：{@link #uploadAudioLevel} <strong>不能为</strong> {@link DUILiteConfig#UPLOAD_AUDIO_LEVEL_NONE}
     */
    public static int uploadAudioDelayTime = 5 * 60 * 1000;

    public static int uploadAudioTime = 6 * 1000;

    public static long uploadAudioMaxLength = 10 * 1024 * 1024;
    /**
     * 录音机采集数据间隔
     */
    public static int intervalTime = 100;

    /**
     * 大数据上传跟更新配置的时间间隔
     * 单位：ms
     */
    public static int uploadConfigInterval = 24 * 60 * 60 * 1000;//默认24小时

    /**
     * 设置大数据上传url地址 <br>
     * <p>
     * 取值：字符串<br>
     * 是否必需：否<br>
     * 默认值：https://log.aispeech.com<br>
     */
    public static String uploadUrl = "https://log.aispeech.com";

    /**
     * 音频源默认为{@link MediaRecorder.AudioSource#VOICE_RECOGNITION}
     * AudioSource.MIC  麦克风
     * AudioSource.VOICE_RECOGNITION 语音识别
     */
    public static int audioSource = 0;
    /**
     * 默认为普通单麦模式
     */
    private static int recoderType = TYPE_COMMON_MIC;
    /**
     * 大音量模式开关，默认关闭为false
     */
    public static boolean maxVolumeMode = false;
    /**
     * 监控数据是否上传
     */
    public static boolean isMonitorUploadEnable = true;

    /**
     * 设置是否启用vad音量计算反馈, 默认为 true
     */
    public static boolean useCalcVolume = true;

    /**
     * 支持外部配置是否启用信号处理双VAD
     * 设置sspe信号处理返回多路音频，以及VAD加载多路音频
     * 取值： true or false 是否使用双VAD
     */
    public static boolean useDoubleVad = false;

    /**
     * 上传大数据的唤醒音频保存的路径
     */
    public static String uploadAudioPath = null;
    private static String TAG = "AISpeech";
    private static Context mContext;
    /**
     * 设置oneshot功能下需要缓存的音频时长,根据具体的硬件性能和唤醒词长度调节
     */
    private static int oneShotCacheTime = 1200;

    /**
     * 设置使用思必驰提供的DNS解析库
     */
    private static boolean useSpeechDns = true;

    public static String offlineEngineAuth;
    /**
     * 初始化sdk
     *
     * @param context context
     * @return 初始化是否成功标志
     */
    public static int init(Context context) {
        mContext = context;
        Log.i(TAG, "mContext " + mContext);
        try {
            if (containsConfig(KEY_FESPX_2REFENCE_ENABLE)) {
                boolean enable = Boolean.parseBoolean(getConfig(KEY_FESPX_2REFENCE_ENABLE));
                Audio2RefSetting.set(context, enable, getConfig(KEY_FESPX_2REFENCE_PATH));
            }
        } catch (Exception e) {
            Log.e(TAG, "init: " + e.toString());
        }

        return 0;
    }

    /**
     * 打开sdk日志
     * 日志打开，性能会受影响，调试时可打开日志, 默认日志关闭
     * @see AISpeech#openLog(Context, String, int, String, String, String, boolean)
     * @deprecated
     */
    public static void openLog(String logFilePath) {
        Log.setDebugMode(Constant.D, logFilePath);
    }

    /**
     * 打开sdk日志
     * 日志打开，性能会受影响，调试时可打开日志, 默认日志关闭
     *
     * @see AISpeech#openLog(Context, String, int, String, String, String, boolean)
     * @deprecated 废弃
     */
    @Deprecated
    public static void openLog(Context context, String logFilePath, int cachedDays) {
        //Log.openLog(context, logFilePath, cachedDays);
    }

    /**
     * 设置SDK内核层日志等级 默认 android.util.Log.WARN
     * {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     *
     * @param nativeLogLevel 日志级别
     */
    public static void setNativeDebugMode(int nativeLogLevel) {
        if (nativeLogLevel <= android.util.Log.INFO) {
            AISpeechSDK.LOGCAT_DEBUGABLE = true;
        }
        Log.LOG_NATIVE_LEVEL = nativeLogLevel;
    }

    public static void openLog(Context context,
                               String logFilePath,
                               int cachedDays,
                               String deviceId,
                               String apiKey,
                               String apiSecret,
                               boolean salvageEnabled,int maxLength) {
        Log.openLog(context, logFilePath, cachedDays, deviceId, apiKey, apiSecret, salvageEnabled,maxLength);
    }


    /**
     * 设置SDK日志级别，默认W-3
     * {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     *
     * @param logLevel    日志级别
     * @param logFilePath 日志保存绝对路径
     * @deprecated 当前log工具需要先初始化，本方法废弃
     */
    @Deprecated
    public static void setDebugMode(int logLevel, String logFilePath) {
        if (!TextUtils.isEmpty(logFilePath)) {
            AISpeechSDK.LOGFILE_PATH = logFilePath;
        }
        if (logLevel <= Constant.D) {
            AISpeechSDK.LOGCAT_DEBUGABLE = true;
        }
        Log.setDebugMode(logLevel, logFilePath);
    }

    /**
     * 设置SDK日志级别，默认W-3
     * {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     *
     * @param logLevel    日志级别
     */
    public static void setDebugMode(int logLevel) {
        if (logLevel <= Constant.D) {
            AISpeechSDK.LOGCAT_DEBUGABLE = true;
        }
        Log.setDebugMode(logLevel);
    }

    /**
     * 设置SDK日志级别，默认W-3
     * {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     *
     * @param logLevel    日志级别
     * @param maxLength 设置输出的日志一行最多显示多少字符，超过之后，自动换行
     */
    public static void setDebugMode(int logLevel,int maxLength) {
        if (logLevel <= Constant.D) {
            AISpeechSDK.LOGCAT_DEBUGABLE = true;
        }
        Log.setDebugMode(logLevel,maxLength);
    }

    /**
     * 该引擎的是否保存音频
     *
     * @param engine {@link Engines}
     */
    public static boolean isSavingEngineAudioEnable(int engine) {
        return Engines.isSavingEngineAudioEnable(engine);
    }


    /**
     * 获取context
     *
     * @return 获取context
     */
    public static Context getContext() {
        return mContext;
    }

    /****************** echo 配置 **********************/


    /**
     * echo资源
     */
    public static String echoResources = null;

    /**
     * 通道数
     */
    public static int channels = 2;
    /**
     * mic数
     */
    public static int micNumber = 1;
    /**
     * 默认为1,即左通道为rec录音音频,右通道为play参考音频（播放音频）
     * 若设置为2, 通道会互换，即左通道为play参考音频（播放音频）,右通道为rec录音音频
     */
    public static int recChannel = 1;
    /**
     * AEC保存的音频文件目录，
     */
    private static String echoSavedDirPath = null;

    public static String getEchoSavedDirPath() {
        // 音频保存是否开启
        if (!AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE) {
            return null;
        }

        if (!isSavingEngineAudioEnable(Engines.ECHO)){
            return null;
        }

        if (!TextUtils.isEmpty(AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH)) {
            echoSavedDirPath = AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH;
            if (!echoSavedDirPath.endsWith("/")) {
                echoSavedDirPath += "/";
            }
            echoSavedDirPath += ECHO_AUDIO_DIR;
        }
        return echoSavedDirPath;
    }

    public static void setEchoSavedDirPath(String echoSavedDirPath) {
        AISpeech.echoSavedDirPath = echoSavedDirPath;
    }

    /**
     * 是否开启AEC健康监控
     */
    public static boolean echoMonitorEnable = false;

    /**
     * 健康监控执行周期 ,默认200 ms
     */
    public static int echoMonitorPeriod = 200;

    /**
     * tts cache 目录
     */
    public static String ttsCacheDir = null;


    @Deprecated
    public static boolean isLocalSaveEnabled() {
        return AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE;
    }

    @Deprecated
    public static void setLocalSaveEnabled(boolean localSaveEnabled) {
        AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE = localSaveEnabled;
    }
    /**
     * 设置oneshot功能下需要缓存的音频时长,根据具体的硬件性能和唤醒词长度调节
     *
     * @return 返回oneshot功能下需要缓存的音频时长
     */
    public static int getOneShotCacheTime() {
        return oneShotCacheTime;
    }

    /**
     * @param oneShotCacheTime oneshot功能下需要缓存的音频时长,默认1200ms
     *                         设置oneshot功能下需要缓存的音频时长,根据具体的硬件性能和唤醒词长度调节
     */
    public static void setOneShotCacheTime(int oneShotCacheTime) {
        AISpeech.oneShotCacheTime = oneShotCacheTime;
    }

    public synchronized static void addConfig(final String key, final String value) {
        map.put(key, value);
    }

    public synchronized static void addConfig(final String key, final int value) {
        map.put(key, String.valueOf(value));
    }

    public synchronized static void addConfig(Map<String, String> mapKV) {
        map.putAll(mapKV);
    }

    /**
     * 获取config中key对应的value
     *
     * @param key key
     * @return String key对应的值
     */
    public synchronized static String getConfig(final String key) {
        return map.get(key);
    }


    /**
     * 获取config中是否包含key
     *
     * @param key key
     * @return boolean true:包含 false:不包含
     */
    public synchronized static boolean containsConfig(String key) {
        return map.containsKey(key);
    }

    public static int getRecoderType() {
        return recoderType;
    }

    public static void setRecoderType(int recoderType) {
        AISpeech.recoderType = recoderType;
    }

    public static boolean isUseSpeechDns() {
        return useSpeechDns;
    }

    /**
     * 设置是否使用思必驰提供的DNS解析库
     * @param useSpeechDns 是否使用思必驰提供的解析库，默认true
     */
    public static void setUseSpeechDns(boolean useSpeechDns) {
        AISpeech.useSpeechDns = useSpeechDns;
        NetConfig.setUseSpeechDns(useSpeechDns);
    }

    /**
     * 授权信息
     */
    public static AIProfile mProfile;

    public static AIProfile getProfile() {

        if (mProfile == null) {
            throw new IllegalStateException("please init sdk first!");
        }

        return mProfile;
    }

    public static void setProfile(AIProfile mProfile) {
        AISpeech.mProfile = mProfile;
    }
}
