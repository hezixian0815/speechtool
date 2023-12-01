package com.aispeech;

import static com.aispeech.lite.AISpeechSDK.LOG_WK_TYPE_PREWAKEUP;
import static com.aispeech.lite.AISpeechSDK.LOG_WK_TYPE_WAKEUP;

import android.content.Context;
import android.text.TextUtils;

import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.auth.AIAuthEngine;
import com.aispeech.auth.AIProfile;
import com.aispeech.auth.ProfileState;
import com.aispeech.auth.config.AIAuthConfig;
import com.aispeech.common.AISpeechBridge;
import com.aispeech.common.AITimer;
import com.aispeech.common.AuthError;
import com.aispeech.common.AuthUtil;
import com.aispeech.common.CustomLog;
import com.aispeech.common.DiskLogAdapter;
import com.aispeech.common.FileUtils;
import com.aispeech.common.JNIFlag;
import com.aispeech.common.Log;
import com.aispeech.common.SharedPreferencesUtil;
import com.aispeech.common.Util;
import com.aispeech.echo.EchoKernel;
import com.aispeech.export.config.AuthConfig;
import com.aispeech.export.config.RecorderConfig;
import com.aispeech.export.config.UploadConfig;
import com.aispeech.export.listeners.AIVersionInfoListener;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.AuthType;
import com.aispeech.util.LiteSdkCoreVersionUtil;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.UUID;

/**
 * DUILiteSDK 是整个sdk的入口，包括以下功能：
 * <ol>
 * <li>
 * 初始化配置信息
 * </li>
 * <li>
 * 提供获取 sdk 信息的方法
 * </li>
 * <li>
 * auth 认证，只有认证成功后才能使用各个模块的功能
 * </li>
 * </ol>
 */
public class DUILiteSDK {

    private static final String TAG = "DUILiteSDK";

    public static String getSdkVersion() {
        return AISpeechSDK.SDK_VERSION;
    }

    /**
     * 获取内核版本号
     *
     * @return core_version
     */
    public static String getCoreVersion() {
        if (Utils.isUtilsSoValid()) {
            return Utils.get_version();
        } else {
            return "get error because of unload libduiutils.so";
        }
    }


    /**
     * 获取SDK使用的deviceId
     *
     * @param context context
     * @return deviceId
     */
    public static String getDeviceId(Context context) {
        return AIAuthEngine.getInstance().getProfile().getDeviceId();
    }


    /**
     * 获取SDK生成的deviceName
     *
     * @return deviceName
     */
    public static String getDeviceName() {
        return AIAuthEngine.getInstance().getProfile().getDeviceName();
    }

    /**
     * 只试用于试用授权和在线授权切换
     * 当试用授权授权成功之后,在试用授权状态,链接网络之后要主动授权一次,如果授权失败,还在试用授权类型
     * 获取当前授权后类型,用于判断是否提示网络问题还是授权问题
     *
     * @return AuthType
     */
    public static AuthType getAuthType() {
        Log.d(TAG, "getAuthType " + AIAuthEngine.getInstance().getProfile().getProfilePath());
        if (TextUtils.isEmpty(AIAuthEngine.getInstance().getProfile().getProfilePath())) {
            return AuthType.TRIAL;
        }
        return AuthType.ONLINE;
    }

    /**
     * 获取授权参数信息，用于请求云端服务
     *
     * @param productId 产品id
     * @return 授权参数信息
     */
    public static String getAuthParams(String productId) {
        String timestamp = System.currentTimeMillis() + "";
        String nonce = UUID.randomUUID() + "";
        String sig = AuthUtil.getSignature(getDeviceName() + nonce + productId + timestamp, AIAuthEngine.getInstance().getProfile().getDeviceSecret());
        return "timestamp=" + timestamp + "&nonce=" + nonce + "&sig=" + sig;
    }

    /**
     * 用户自定义log，用于客户自行实现自己的log逻辑，将SDK的log使用客户的log机制实现，将所有的log统一归类，调用此接口之后
     * 不需要在调用{@link DUILiteSDK#openLog(Context, String)}
     * 和{@link DUILiteSDK#openLog(Context, String, int)}
     *
     * @param customLog 客户自定义的log类
     */
    public static void setCustomLog(CustomLog customLog) {
        Log.setCustomLog(customLog);
    }

    /**
     * 设置 LiteSDK 全局日志等级 默认 android.util.Log.WARN
     * {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     *
     * @param globalLogLevel 日志级别
     */
    public static void setGlobalLogLevel(int globalLogLevel) {
        // 设置Java lite层日志等级
        setJavaLiteLogLevel(globalLogLevel);
        // 设置Native层日志等级
        setNativeLogLevel(globalLogLevel);
    }

    /**
     * 设置 Java lite层日志等级 默认 android.util.Log.WARN
     * {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     *
     * @param logLevel 日志级别
     */
    public static void setJavaLiteLogLevel(int logLevel) {
        AISpeech.setDebugMode(logLevel);
    }

    /**
     * 设置 SDK 内核层日志等级 默认 android.util.Log.WARN
     * {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     *
     * @param nativeLogLevel 日志级别
     */
    public static void setNativeLogLevel(int nativeLogLevel) {
        AISpeech.setNativeDebugMode(nativeLogLevel);
    }

    /**
     * 打开sdk日志
     * 日志打开，性能会受影响，调试时可打开日志, 默认日志关闭
     *
     * @param logFilePath 保存的日志文件路径，包含文件名，比如"/sdcard/duilite/DUILite_SDK.log"
     * @see DUILiteSDK#openLog(Context, String)
     * @deprecated
     */
    private static void openLog(String logFilePath) {
        if (!TextUtils.isEmpty(logFilePath)) {
            AISpeechSDK.LOGFILE_PATH = logFilePath;
        }
        AISpeech.openLog(logFilePath);
    }

    /**
     * 打开sdk日志
     * 日志打开，性能会受影响，调试时可打开日志, 默认日志关闭
     *
     * @param context     上下文
     * @param logFilePath 保存的日志文件路径，包含文件名，比如"/sdcard/duilite/DUILite_SDK.log"
     */
    public static void openLog(Context context, String logFilePath) {
        openLog(context, logFilePath, 1);
    }

    /**
     * 打开sdk日志
     * 日志打开，性能会受影响，调试时可打开日志, 默认日志关闭
     *
     * @param context     上下文
     * @param logFilePath 保存的日志文件路径，包含文件名，比如"/sdcard/duilite/DUILite_SDK.log"
     * @param cachedDays  日志的清理周期，单位天
     */
    public static void openLog(Context context, String logFilePath, int cachedDays) {
        openLog(context, logFilePath, cachedDays, "speechLite", "speechLite", "speechLite", false);
    }

    /**
     * 打开sdk日志
     * 日志打开，性能会受影响，调试时可打开日志, 默认日志关闭
     *
     * @param context     上下文
     * @param logFilePath 保存的日志文件路径，包含文件名，比如"/sdcard/duilite/DUILite_SDK.log"
     * @param cachedDays  日志的清理周期，单位天
     * @param maxLength  日志每行最多输出的字数
     */
    public static void openLog(Context context, String logFilePath, int cachedDays,int maxLength) {
        openLog(context, logFilePath, cachedDays, "speechLite", "speechLite", "speechLite", false,maxLength);
    }

    /**
     * 打开sdk日志
     * 日志打开，性能会受影响，调试时可打开日志, 默认日志关闭
     *
     * @param context        上下文
     * @param logFilePath    保存的日志文件路径，包含文件名，比如"/sdcard/duilite/DUILite_SDK.log"
     * @param cachedDays     日志的清理周期，单位天
     * @param apiKey         apikey，日志回捞使用
     * @param apiSecret      APISecret，日志回捞使用
     * @param deviceId       deviceId，日志回捞使用
     * @param salvageEnabled 是否使用回捞功能
     */
    public static void openLog(Context context,
                               String logFilePath,
                               int cachedDays,
                               String deviceId,
                               String apiKey,
                               String apiSecret,
                               boolean salvageEnabled) {
        if (!TextUtils.isEmpty(logFilePath)) {
            AISpeechSDK.LOGFILE_PATH = logFilePath;
        }
        AISpeech.openLog(context, logFilePath, cachedDays, deviceId, apiKey, apiSecret, salvageEnabled,0);
    }

    /**
     * 打开sdk日志
     * 日志打开，性能会受影响，调试时可打开日志, 默认日志关闭
     *
     * @param context        上下文
     * @param logFilePath    保存的日志文件路径，包含文件名，比如"/sdcard/duilite/DUILite_SDK.log"
     * @param cachedDays     日志的清理周期，单位天
     * @param apiKey         apikey，日志回捞使用
     * @param apiSecret      APISecret，日志回捞使用
     * @param deviceId       deviceId，日志回捞使用
     * @param salvageEnabled 是否使用回捞功能
     * @param maxLength  日志每行最多输出的字数
     */
    public static void openLog(Context context,
                               String logFilePath,
                               int cachedDays,
                               String deviceId,
                               String apiKey,
                               String apiSecret,
                               boolean salvageEnabled,int maxLength) {
        if (!TextUtils.isEmpty(logFilePath)) {
            AISpeechSDK.LOGFILE_PATH = logFilePath;
        }
        AISpeech.openLog(context, logFilePath, cachedDays, deviceId, apiKey, apiSecret, salvageEnabled,maxLength);
    }

    /**
     * 设置 SDK信息（异常监控信息/音频）是否上传，默认上传
     */
    private static void setMonitorUploadEnable() {
        if (AISpeech.isMonitorUploadEnable)
            AnalysisProxy.getInstance().getAnalysisMonitor().enableUpload();
        else
            AnalysisProxy.getInstance().getAnalysisMonitor().disableUpload();
    }

    /**
     * 检查是否授权成功(本地授权文件有效)
     *
     * @param context 上下文
     * @return true:成功; false:失败
     */
    public static boolean isAuthorized(Context context) {
        AISpeech.init(context.getApplicationContext());
        return AIAuthEngine.getInstance().isAuthorized();
    }

    /**
     * 获取授权文件状态
     *
     * @param scopeType 模块类型，null表示不检查模块
     * @return 授权文件状态值
     */
    public static ProfileState getAuthState(String scopeType) {
        return AIAuthEngine.getInstance().getProfile().isProfileValid(scopeType);
    }


    /**
     * 设置SDK日志级别，默认W-3
     * {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     *
     * @param logLevel    日志级别
     * @param logFilePath 日志保存绝对路径
     * @deprecated
     */
    public static void setDebugMode(int logLevel, String logFilePath) {
        AISpeech.setDebugMode(logLevel, logFilePath);
    }

    /**
     * 设置SDK日志级别，默认W-3
     * {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     *
     * @param logLevel 日志级别
     */
    public static void setDebugMode(int logLevel) {
        AISpeech.setDebugMode(logLevel);
    }

    /**
     * 设置SDK日志级别，默认W-3
     * {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     *M
     * @param logLevel 日志级别
     * @param maxLength 设置输出的日志一行最多显示多少字符，超过之后，自动换行
     */
    public static void setDebugMode(int logLevel,int maxLength) {
        AISpeech.setDebugMode(logLevel,maxLength);
    }

    private static void checkWhetherNeedOkHttpLibrary() {
        int i = 0;
        try {
            // 0 个类有，无网络库
            Class c1 = com.aispeech.analysis.AnalysisAudioImpl.class;
            i++; // 1 个类有
            Class c2 = okhttp3.OkHttpClient.class;
            i++; // 2 个类都有
            if (c1 == c2) {
                // 不会执行到，为了这段代码不被优化
                Log.d(TAG, "c1 == c2");
            }
        } catch (NoClassDefFoundError e) {
            //do nothing
        }
        if (i == 1)
            throw new NoClassDefFoundError("DUI Lite sdk 依赖 OkHttp 网络开源库，请在项目里添加 OkHttp 依赖，请参考 Demo");
    }

    /**
     * 授权失败次数
     * 授权失败一次+1，最大是4
     * 授权成功置0
     */
    public static int authFailTimes = 0;


    /**
     * 初始化授权信息
     *
     * @param context  上下文
     * @param config   配置参数，包括产品的 APPKEY，产品ID，产品KEY，产品SECRET，以及其它的全局配置参数
     * @param listener 授权回调
     */
    public synchronized static void init(Context context, DUILiteConfig config, final InitListener listener) {
        checkWhetherNeedOkHttpLibrary();
        if (config == null) {
            throw new IllegalArgumentException("config cannot be null");
        }
        final Context appContext = context.getApplicationContext();
        Log.d(TAG, "user set config: " + config);
        parseConfig(config);
        AISpeech.init(appContext);
        Log.setDiskLogAdapter(DiskLogAdapter.getInstance());
        Map<String, Object> map = new HashMap<>();
        //万能key 对SDK版本有限制，要求在1.2.0之下，故而设置当使用万能key（只用productid和apikey）不上传sdkversion(万能key只针对dds有效)
        if (!TextUtils.isEmpty(config.getProductKey()) || !TextUtils.isEmpty(config.getProductSecret())) {
            map.put("sdkName", "duilite_android");
            map.put("sdkVersion", getSdkVersion());
        }


        AIAuthConfig aiAuthConfig = new AIAuthConfig.Builder()
                .setProductId(config.getProductId())
                .setProductKey(config.getProductKey())
                .setProductSecret(config.getProductSecret())
                .setApiKey(config.getApiKey())
                .setProfilePath(AISpeech.deviceProfileDirPath)
                .setAuthTimeout(AISpeech.authTimeout)
                .setNeedReplaceProfile(AISpeech.updateTrailProfileToOnlineProfile)
                .setIgnoreLogin(config.isIgnoreLogin())
                .setOfflineProfileName(AISpeech.offlineProfileName)
                .setAuthServer(AISpeech.authServer)
                .setCustomDeviceId(AISpeech.getConfig(AISpeech.KEY_DEVICE_ID))
                .setCustomDeviceName(AISpeech.getConfig(AISpeech.KEY_DEVICE_NAME))
                .setDeviceNameType(AISpeech.getConfig(AISpeech.KEY_DEVICE_NAME_TYPE))
                .setCustomSHA256(AISpeech.getConfig(AISpeech.KEY_CUSTOM_SHA256))
                .setLicenseId(AISpeech.getConfig(AISpeech.KEY_LICENSE_ID))
                .setBuildModel(config.getBuildModel())
                .setDeviceInfoMap(map)
                .setEncryptCustomDeviceName(AISpeech.encryptCustomDeviceName)
                .setSharePkgName(AISpeech.sharePkgName)
                .setShareSHA256(AISpeech.shareSHA256)
                .build();
        MyInitListenerImpl myMyInitListenerImpl = new MyInitListenerImpl(listener);
        myMyInitListenerImpl.setContextAndConfig(appContext, config);
        AIAuthEngine.getInstance().init(appContext, aiAuthConfig, myMyInitListenerImpl);


        //根据失败次数，设置延时授权时间 1000*失败次数^2,不失败则不延时
        long delayTime = (long) (authFailTimes == 0 ? 0 : 1000 * Math.pow(2, authFailTimes));
        // 授权最多延迟10s
        if (delayTime > 10 * 1000)
            delayTime = 10 * 1000;
        if (authFailTimes > 0)
            Log.d(TAG, "auth delay " + delayTime + "ms  when auth failed last time!");
        //如果有正在执行的任务，则不做处理，将等待上次任务执行完毕之后再执行
        if (mAuthTaskIsWaitting) {
            Log.d(TAG, "last auth task is waitting,please wait!");
        } else {
            if (mAuthTask != null) {
                mAuthTask.cancel();
                mAuthTask = null;
            }
            mAuthTaskIsWaitting = true;
            mAuthTask = new AuthTask();
            AITimer.getInstance().schedule(mAuthTask, delayTime);
        }
        Log.d(TAG, "SdkVersion " + getSdkVersion() + " CoreVersion " + getCoreVersion());
    }

    private static void parseConfig(DUILiteConfig config) {
        if (!config.isProductInfoValid()) {
            throw new IllegalArgumentException("ProductInfo set invalid, at least one in productId|productKey|productSecret|apiKey is empty");
        }

        // 设置 ECHO 模式的配置，其它模式下无影响
        AIEchoConfig aiEchoConfig = config.getEchoConfig();
        if (aiEchoConfig == null) {
            Log.d(TAG, "AIEchoConfig is null");
            if (config.getAudioRecorderType() == DUILiteConfig.TYPE_COMMON_ECHO)
                throw new IllegalArgumentException("AIEchoConfig cannot be null, pls use DUILiteConfig.setEchoConfig() to set");
        } else {
            Log.d(TAG, "AIEchoConfig " + aiEchoConfig.toString());
            // echo 模式的配置
            EchoKernel.setAiEchoConfig(aiEchoConfig);
        }

        RecorderConfig recorderConfig = config.getRecorderConfig();
        AuthConfig authConfig = config.getAuthConfig();
        UploadConfig uploadConfig = config.getUploadConfig();

        AISpeech.echoResources = aiEchoConfig != null ? aiEchoConfig.getAecResource() : null;
        AISpeech.micNumber = aiEchoConfig != null ? aiEchoConfig.getMicNumber() : 1;
        AISpeech.channels = aiEchoConfig != null ? aiEchoConfig.getChannels() : 2;
        AISpeech.recChannel = aiEchoConfig != null ? aiEchoConfig.getRecChannel() : 1;
        AISpeech.setEchoSavedDirPath(aiEchoConfig != null ? aiEchoConfig.getSavedDirPath() : null);
        AISpeech.echoMonitorEnable = aiEchoConfig != null && aiEchoConfig.isMonitorEnable();
        AISpeech.echoMonitorPeriod = aiEchoConfig != null ? aiEchoConfig.getMonitorPeriod() : 200;

        if (recorderConfig != null) {
            AISpeech.audioSource = recorderConfig.getAudioSource();
            AISpeech.setRecoderType(recorderConfig.recorderType);
            AISpeech.intervalTime = recorderConfig.intervalTime;
        } else {
            AISpeech.audioSource = config.getAudioSourceType();
            AISpeech.setRecoderType(config.getAudioRecorderType());
            AISpeech.intervalTime = config.getIntervalTime();
        }
        // auth
        Map map = config.getExtraParameter();
        if (authConfig != null) {
            AISpeech.authTimeout = authConfig.getAuthTimeout();
            AISpeech.deviceProfileDirPath = authConfig.getDeviceProfileDirPath();
            AISpeech.updateTrailProfileToOnlineProfile = authConfig.isUpdateTrailProfileToOnlineProfile();
            AISpeech.offlineProfileName = authConfig.getOfflineProfileName();
            AISpeech.encryptCustomDeviceName = authConfig.encryptCustomDeviceName();
            AISpeechBridge.encryptCustomDeviceName = authConfig.encryptCustomDeviceName();
            AISpeech.authServer = authConfig.getAuthServer();
            AISpeech.customDeviceId = authConfig.getCustomDeviceId();
            AISpeech.customDeviceName = authConfig.getCustomDeviceName();
            AISpeech.licenceId = authConfig.getLicenceId();
            AISpeech.authType = authConfig.getType();
            if (!map.containsKey(AISpeech.KEY_DEVICE_ID) && !TextUtils.isEmpty(authConfig.getCustomDeviceId())) {
                map.put(AISpeech.KEY_DEVICE_ID, authConfig.getCustomDeviceId());
            }
            if (!map.containsKey(AISpeech.KEY_DEVICE_NAME) && !TextUtils.isEmpty(authConfig.getCustomDeviceName())) {
                map.put(AISpeech.KEY_DEVICE_NAME, authConfig.getCustomDeviceName());
            }
            if (!map.containsKey(AISpeech.KEY_LICENSE_ID) && !TextUtils.isEmpty(authConfig.getLicenceId())) {
                map.put(AISpeech.KEY_LICENSE_ID, authConfig.getLicenceId());
            }
        } else {
            AISpeech.authServer = config.getAuthServer();
            AISpeech.authTimeout = config.getAuthTimeout();
            AISpeech.deviceProfileDirPath = config.getDeviceProfileDirPath();
            AISpeech.updateTrailProfileToOnlineProfile = config.isUpdateTrailProfileToOnlineProfile();
            AISpeech.offlineProfileName = config.getOfflineProfileName();
            AISpeech.encryptCustomDeviceName = config.isEncryptCustomDeviceName();
            AISpeechBridge.encryptCustomDeviceName = config.isEncryptCustomDeviceName();
        }

        AISpeech.addConfig(map);
        if (uploadConfig != null) {
            // update
            AISpeech.uploadEnable = uploadConfig != null && uploadConfig.isUploadEnable();
            AISpeech.cacheUploadEnable = uploadConfig != null && uploadConfig.isCacheUploadEnable();
            AISpeech.uploadUrl = uploadConfig != null ? uploadConfig.getUploadUrl() : "https://log.aispeech.com";
            AISpeech.cacheUploadMaxNumber = uploadConfig != null ? uploadConfig.getCacheUploadMaxNumber() : 100;
            AISpeech.uploadAudioLevel = uploadConfig != null ? uploadConfig.getUploadAudioLevel() : UploadConfig.UPLOAD_AUDIO_LEVEL_NONE;
            AISpeech.uploadAudioDelayTime = uploadConfig != null ? uploadConfig.getUploadAudioDelayTime() : 5 * 60 * 1000;
            AISpeech.uploadAudioPath = uploadConfig != null ? uploadConfig.getUploadAudioPath() : null;
            AISpeech.uploadAudioTime = uploadConfig != null ? uploadConfig.getUploadAudioTime() : 6 * 1000;
            AISpeech.uploadAudioMaxLength = uploadConfig != null ? uploadConfig.getUploadAudioMaxLength() : 10 * 1024 * 1024;
            AISpeech.isMonitorUploadEnable = uploadConfig != null ? uploadConfig.isUploadLogEnable() : false;
        } else {
            if (!TextUtils.isEmpty(config.getUploadAudioPath())) {
                AISpeech.uploadAudioPath = config.getUploadAudioPath();
            }
            AISpeech.cacheUploadEnable = config.isCacheUploadEnable();
            AISpeech.isMonitorUploadEnable = config.isMonitorUploadEnable();
        }
        AISpeech.cacheUploadEnable |= Util.isDebugSystem();
        AISpeech.callbackInThread = config.isCallbackInThread();
        AISpeech.maxMessageQueueSize = config.getMaxMessageQueueSize();
        AISpeech.maxVolumeMode = config.isMaxVolumeMode();

        if (!TextUtils.isEmpty(config.getLocalSaveAudioPath())) {
            AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH = config.getLocalSaveAudioPath();
        }
        AISpeech.setLocalSaveEnabled(config.getLocalSaveEnabled());

        if (config.getThreadAffinity() > 0) {
            AISpeech.threadAffinity = config.getThreadAffinity();
        }

        AISpeech.illegalPingyinCheck = config.getIllegalPingyinCheck();
        AISpeech.useDoubleVad = config.isUseDoubleVad();
        JNIFlag.useDoubleVad = config.isUseDoubleVad();
        JNIFlag.isLoadCarSspe = false;
        AISpeech.setUseSpeechDns(config.isUseSpeechDns());

        if (config.isLogEnable()) {
            openLog(config.getLogFilePath());
        }

    }

    /**
     * 关闭vad音量计算反馈 所有引擎中Listener.onRmsChanged将不在被回调
     * 默认开启
     */
    public static void closeVadCalcVolume() {
        AISpeech.useCalcVolume = false;
        Log.d(TAG, "closeVadCalcVolume");
    }

    /**
     * @deprecated use {{@link #openGlobalAudioSave(String)}}
     */
    public static void setLocalSavedEnabled(boolean localSavedEnabled) {
        AISpeech.setLocalSaveEnabled(localSavedEnabled);
    }

    /**
     * 开启车载特性 请咨询对接人员开启
     */
    public static void enableCarFlavor() {
        Log.i(TAG, "improveCarPerformance");
        AISpeech.threadAffinity = 0b00111000;
    }

    /**
     * 设置全局音频存储路径 默认开启
     * 比引擎单独设置的优先级更高，如需单独配置各引擎保存地址，可传入null
     *
     * @param audioSavePath /sdcard/aispeech/
     */
    public static void openGlobalAudioSave(String audioSavePath) {
        setGlobalAudioSaveEnable(true);
        if (!TextUtils.isEmpty(audioSavePath))
            setGlobalAudioSavePath(audioSavePath);
    }

    /**
     * 是否开启全局音频保存
     *
     * @return 是否开启全局音频保存
     */
    public static boolean isGlobalAudioSave() {
        return AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE;
    }

    /**
     * 关闭全局音频存储
     */
    public static void closeGlobalAudioSave() {
        setGlobalAudioSaveEnable(false);
    }

    @Deprecated
    public static void setGlobalAudioSavePath(String globalAudioSavePath) {
        if (TextUtils.isEmpty(globalAudioSavePath)) {
            Log.e(TAG, "setGlobalAudioSavePath error,path is empty");
            return;
        }
        if (!globalAudioSavePath.endsWith("/")) {
            globalAudioSavePath += "/";
        }
        FileUtils.createOrExistsDir(globalAudioSavePath);
        AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH = globalAudioSavePath;
    }

    @Deprecated
    public static void setGlobalAudioSaveEnable(boolean globalAudioSaveEnable) {
        AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE = globalAudioSaveEnable;
        if (!globalAudioSaveEnable) {
            AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH = "";
        }
    }

    /**
     * 设置要保存音频的模块，其他模块则不会保存音频
     * 支持多个模块配置  例如  setGlobalAudioSaveEngines(SpeechParams.Engine.ECHO | SpeechParams.Engine.CLOUD_ASR);
     * 默认是全开,多次设置或配置setGlobalDisableAudioSaveEngines会覆盖
     *
     * @param engine {@link com.aispeech.lite.Engines}
     */
    public static void setGlobalAudioSaveEngines(int engine) {
        AISpeechSDK.GLOBAL_AUDIO_SAVE_ENGINES.set(engine);
    }

    /**
     * 设置不保存音频的模块,其他模块则会保存音频
     * 支持多个模块配置
     * 多次设置会覆盖，注意同时设置setGlobalAudioSaveEngine会覆盖
     *
     * @param engine
     */
    public static void setGlobalDisableAudioSaveEngines(int engine) {
        AISpeechSDK.GLOBAL_AUDIO_SAVE_ENGINES.set(~engine & Integer.MAX_VALUE);
    }

    /**
     * 该引擎的是否保存音频
     *
     * @param engine {@link com.aispeech.lite.Engines}
     */
    public static boolean isSavingEngineAudioEnable(int engine) {
        return AISpeech.isSavingEngineAudioEnable(engine);
    }

    /**
     * @deprecated use {{@link #isGlobalAudioSave()}} instead
     */
    public static boolean getLocalSavedEnabled() {
        return AISpeech.isLocalSaveEnabled();
    }

    public interface InitListener {
        void success();

        void error(String errorCode, String errorInfo);
    }

    public static void getSdkInfo(AIVersionInfoListener aiVersionInfoListener) {
        if (AISpeech.getContext() != null && isAuthorized(AISpeech.getContext())) {
            LiteSdkCoreVersionUtil.getInstance().getSdkInfo(aiVersionInfoListener);
        } else {
            Log.i(TAG, "getSdkInfo , please first authorized");
        }
    }

    private static class MyInitListenerImpl implements InitListener {
        private InitListener mListener;
        private Context context;
        private DUILiteConfig config;
        private UploadDeviceInfoTask mUpdateConfigTask = null;

        /**
         * 上传设备信息
         */
        private void uploadDeviceInfo() {
            if (mUpdateConfigTask != null) {
                mUpdateConfigTask.cancel();
                mUpdateConfigTask = null;
            }
            mUpdateConfigTask = new UploadDeviceInfoTask();
            AITimer.getInstance().schedule(mUpdateConfigTask, 10000);
        }

        /**
         * 获取远程数据上传配置信息
         */
        private void updateConfig(Context context) {
            //获取上次的更新时间
            long lastUpdateTime = SharedPreferencesUtil.getLong(context, SharedPreferencesUtil.SDK_INIT_UPDATE_TIME, 0);
            Log.d(TAG, "update config last time : " + lastUpdateTime);
            long diff = System.currentTimeMillis() - lastUpdateTime;
            Log.d(TAG, "update interval time : " + diff);
            Log.d(TAG, "server interval time : " + AISpeech.uploadConfigInterval);
            //如果时间大于设定的更新间隔时间
            if (diff > AISpeech.uploadConfigInterval) {
                Log.d(TAG, "over interval time，begin update config！");
                //更新配置
                AnalysisProxy.getInstance().updateConfig(true, context);
                //上传设备信息
                uploadDeviceInfo();
                //更新上传时间
                SharedPreferencesUtil.putLong(context, SharedPreferencesUtil.SDK_INIT_UPDATE_TIME, System.currentTimeMillis());
            } else {
                Log.w(TAG, "in interval time，will not update config！");
            }
        }

        private MyInitListenerImpl(InitListener listener) {
            this.mListener = listener;
        }

        public void setContextAndConfig(Context context, DUILiteConfig config) {
            this.context = context;
            this.config = config;
        }

        @Override
        public void success() {
            AISpeech.setProfile(AIAuthEngine.getInstance().getProfile());// sync profile
            //授权成功之后，授权失败次数置0
            authFailTimes = 0;
            if (mListener != null) {
                Log.d(TAG, "auth success，set authFailTimes=0 ");
                if (getAuthState("") != null) {
                    Log.d(TAG, "auth type :  " + getAuthState("").getAuthType());
                }
                if (getAuthState("") != null && getAuthState("").getAuthType() != ProfileState.AUTH_TYPE.OFFLINE) {
                    int uploadConfigInterval = SharedPreferencesUtil.getInt(context, SharedPreferencesUtil.SDK_INIT_UPDATE_INTERVAL, 0);
                    Log.d(TAG, " local saved uploadConfigInterval : " + uploadConfigInterval);
                    if (uploadConfigInterval > 0)
                        AISpeech.uploadConfigInterval = uploadConfigInterval;
                    updateConfig(context);
                }
                this.mListener.success();
                this.mListener = null;

            }

            context = null;
            config = null;
        }

        @Override
        public void error(String errorCode, String errorInfo) {
            AISpeech.setProfile(AIAuthEngine.getInstance().getProfile());// sync profile
            //只针对070648  070649  070655 错误码 进行指数延迟授权策略
            if (AuthError.AUTH_ERR_MSG.ERR_SERVER_070648.getId().equals(errorCode)
                    || AuthError.AUTH_ERR_MSG.ERR_SERVER_070649.getId().equals(errorCode)
                    || AuthError.AUTH_ERR_MSG.ERR_SERVER_070655.getId().equals(errorCode)) {
                if (authFailTimes < 4) {
                    if (!mAuthTaskIsWaitting) {
                        authFailTimes++;
                    } else {
                        Log.w(TAG, "last auth task is waitting,will not increase auth fail times!");
                    }
                } else {
                    authFailTimes = 0;
                }
            } else {
                authFailTimes = 0;
            }
            saveAuthFailInfo(errorCode, errorInfo);
            Log.e(TAG, "auth fail， authFailTimes= " + authFailTimes);
            AuthUtil.logAuthFailureInfo(errorCode, errorInfo, AISpeech.getContext());
            if (config != null && config.getAuthRetryTimesForNetworkErr() > 0 && AuthError.NETWORK_ERROR.equals(errorCode)) {
                int retryTimes = config.getAuthRetryTimesForNetworkErr() - 1;
                Log.d(TAG, "auth retryTimes " + retryTimes);
                config.setAuthRetryTimesForNetworkErr(retryTimes);
                init(context, config, mListener);
            } else if (mListener != null) {
                this.mListener.error(errorCode, errorInfo);
            }
            context = null;
            config = null;
            mListener = null;
        }
    }


    private static void saveAuthFailInfo(String errorCode, String errorInfo) {
        Log.write(TAG, "auth fail,errorCode:" + errorCode + ",errorInfo:" + errorInfo);
        AIProfile profile = AIAuthEngine.getInstance().getProfile();
        if (profile != null) {
            Log.write(TAG, "getProfilePath:" + profile.getProfilePath());
            Log.write(TAG, "getProfileDeviceId:" + profile.getDeviceId());
            Log.write(TAG, "Profile exists:" + FileUtils.isFileExists(profile.getProfilePath()));
        }

        Log.write(TAG, "getAuthKeyHash:" + AuthUtil.getAuthKeyHash(AISpeech.getContext()));
        Log.write(TAG, "authType:" + AISpeech.authType);
    }

    private static synchronized void releaseAnalysis() {
        AnalysisProxy.getInstance().getAnalysisMonitor().disableUpload();
        AnalysisProxy.getInstance().getAnalysisAudio(LOG_WK_TYPE_PREWAKEUP).release();
        AnalysisProxy.getInstance().getAnalysisAudio(LOG_WK_TYPE_WAKEUP).release();
        AnalysisProxy.getInstance().getAnalysisAudioLocalASR().release();
    }

    public static int getAudioRecorderType() {
        return AISpeech.getRecoderType();
    }


    public static class UploadDeviceInfoTask extends TimerTask {

        @Override
        public void run() {
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("mode", "lite");
            entryMap.put("module", "deviceInfo");
            entryMap.put("recordId", System.currentTimeMillis());
            AnalysisProxy.getInstance().getAnalysisMonitor().cacheData(
                    "duilite_deviceInfo",
                    "info",
                    "deviceInfo",
                    null,
                    AuthUtil.getDeviceInfo(),
                    null,
                    entryMap);
            AnalysisProxy.getInstance().getAnalysisMonitor().start();
        }
    }


    private static AuthTask mAuthTask = null;//授权任务
    private static boolean mAuthTaskIsWaitting = false;//授权任务运行标记

    public static class AuthTask extends TimerTask {

        @Override
        public void run() {
            com.aispeech.util.Utils.checkThreadAffinity();
            mAuthTaskIsWaitting = false;
            if (authFailTimes > 0) {
                Log.d(TAG, "do auth again ！fail times:" + authFailTimes);
            } else {
                Log.d(TAG, "do auth ");
            }
            AIAuthEngine.getInstance().doAuth();
        }
    }
}
