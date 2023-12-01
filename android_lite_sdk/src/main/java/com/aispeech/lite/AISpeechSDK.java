/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite;

import android.media.AudioFormat;

import com.hzx.aispeech.BuildConfig;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * SDK内部常量
 */
public class AISpeechSDK {

    /**
     * @hide logcat使能开关，需要在发布前关闭，以防关键日志输出到开发者logcat中 TODO before release
     */
    public static boolean LOGCAT_DEBUGABLE = false;

    /**
     * @hide 日志文件使能开关，需要在发布前关闭 TODO before release
     */
    public static boolean LOGFILE_DEBUGABLE = false;


    public static String LOGFILE_PATH = "";

    /**
     * 是否需要授权
     */
    public static final boolean PROVISION_ENABLE = true;

    /**
     * 是否需要认证
     */
    public static final boolean AUTH_ENABLE = true;
    /**
     * serialNum文件名
     */
    public static final String SERIALNUM_FILE = ".serialNum";


    // 回声对消建议采用以下配置来完成，需要配置以下三个配置项
    /**
     * 回声消除类型: 0 不做aec, 1 需要先对齐，后做aec, 2 不要对齐，直接做aec
     */
    public static int ECHO_TYPE = (AISpeech.ECHO_ENABLE) ? 2 : 0;

    /**
     * 是否音频数据做通道分离
     */
    public static final boolean ECHO_DETACH = true;

    /**
     * 默认录音机声道
     */
    public static int DEFAULT_AUDIO_CHANNEL = (AISpeech.ECHO_ENABLE) ? AudioFormat.CHANNEL_IN_STEREO
            : AudioFormat.CHANNEL_IN_MONO;

    /**
     * @hide sdk版本号，发布前需要更新版本 done before release; 这里已经修改为直接根据Build.gradle 里面的 sdkVersionNum
     */
    public static final String SDK_VERSION = BuildConfig.SDK_VERSION_NUM;

    /**
     * @hide sdk平台
     */
    public static final String LOG_SOURCE = "SDK_ANDROID";

    public static final String AISPEECH = "aispeech";

    public static final String COM_AISPEECH = "com.aispeech";

    /**
     * SDK内部USERID字段名
     */
    public static final String PREFKEY_UNIQUE_ID = "AISPEECH_PREFKEY_UNIQUE_ID";
    /**
     * SDK内部USERKEY字段名
     */
    public static final String PREFKEY_GRAMMAR_HASH_ID = "AISPEECH_PREFKEY_USERKEY_ID";
    /**
     * SDK内部配置文件名
     */
    public static final String PREF_AISPEECH = "AISPEECH_PREF";
    /**
     * 云端识别服务器地址
     */
    public static final String CLOUD_SERVER = "ws://asr.dui.ai/service/v2";
    /**
     * 云端识别服务器地址
     */
    public static final String ALIAS_KEY = "prod";
    /**
     * 本地自适应服务器地址
     */
    public static final String UPDATE_SERVER = "https://update.aispeech.com/update";
    /**
     * 数据上传服务器地址
     */
    public static final String UPLOAD_SERVER = "https://log.aispeech.com/bus";
    /**
     * 认证服务器地址
     */
    public static final String AUTH_SERVER = "https://auth.api.aispeech.com/device";
    /**
     * 服务安装程序下载地址
     */
    public static final String APK_URL = "https://open.aispeech.com/app/aiserver/aispeech_aiservice.apk";
    /**
     * 服务安装包名
     */
    public static final String APK_NAME = "aispeech_aiservice.apk";
    //默认合成地址
    public static final String DEFAULT_CLOUD_TTS_SERVER = "https://tts.dui.ai/runtime/v2/synthesize";
    /**
     * 调试密码
     */
    public static final String DEBUG_SCRECT = "i am a partner of aispeech";
    /**
     * 识别分词密码
     */
    public static final String WRD_SEP_SCRECT = "divied by space";
    /**
     * 查询授权状态
     */
    public static final int AIENGINE_OPT_QUERY_AUTH = 0;
    /**
     * 执行授权操作
     */
    public static final int AIENGINE_OPT_DO_AUTH = 1;
    /**
     * 查询授权类型
     */
    public static final int AIENGINE_OPT_QUERY_AUTH_TYPE = 2;
    /**
     * 录音机释放超时时间(ms)
     */
    public static final long RECORDER_RELEASE_TIMEOUT = 10000;
    /**
     * 等待云端识别结果超时时间(ms)
     */
    public static final long WAIT_CLOUD_ASR_TIMEOUT_VALUE = 10000;
    /**
     * 合成进度粒度值(ms)
     */
    public static final int TTS_PROGRESS_INTERVAL_VALUE = 50;
    /**
     * 网络连接超时2S
     */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 2;
    /**
     * 网络传输超时60S
     */
    public static final int DEFAULT_SERVER_TIMEOUT = 60;
    /**
     * 默认发送超时
     */
    public static final int DEFAULT_SEND_TIMEOUT = 20;
    /**
     * 默认接收超时
     */
    public static final int DEFAULT_RECV_TIMEOUT = 20;

    public static final String LUA_RES_NAME = "aiengine.lub";

    public static final String LUA_DIR_NAME = "lub";

    public static final int AIENGINE_CALLBACK_COMPLETE = 0;

    public static final int AIENGINE_CALLBACK_CANCEL = 1;

    public static final String AUTH_TYPE_IMEI = "imei";
    public static final String AUTH_TYPE_MAC = "mac";
    public static final String AUTH_TYPE_ANDROID_ID = "android_id";
    public static final String AUTH_TYPE_ANDROID_OS_BUILD_SERIAL = "android_os_build_serial";

    /**
     * 预唤醒日志类型
     */
    public static final int LOG_WK_TYPE_PREWAKEUP = 1;

    /**
     * 唤醒日志类型
     */
    public static final int LOG_WK_TYPE_WAKEUP = 2;

    /**
     * 本地asr日志类型
     */
    public static final int LOG_LOCAL_ASR = 3;

    /**
     * 本地Vad 超时日志类型
     */
    public static final int LOG_VAD_TIMEOUT = 4;

    /**
     * 本地monitor日志类型
     */
    public static final int LOG_MONITOR = 5;

    /**
     * 家居场景
     */
    public static final String LOG_SCENE_TYPE_AIHOME = "aihome";

    /**
     * 车载场景
     */
    public static final String LOG_SCENE_TYPE_AICAR = "aicar";

    /**
     * 单麦
     */
    public static final String LOG_MIC_MATRIX_TYPE_ONE = "1";

    /**
     * 线性双麦
     */
    public static final String LOG_MIC_MATRIX_TYPE_TWO_LINE = "2_line";

    /**
     * 车载双麦
     */
    public static final String LOG_MIC_MATRIX_TYPE_TWO_CAR = "2_car";

    /**
     * 环形四麦
     */
    public static final String LOG_MIC_MATRIX_TYPE_FOUR_CIRCLE = "4_circle";

    /**
     * L型四麦
     */
    public static final String LOG_MIC_MATRIX_TYPE_FOUR_L = "4_L";

    /**
     * 线性四麦
     */
    public static final String LOG_MIC_MATRIX_TYPE_FOUR_LINE = "4_line";

    /**
     * 车载四麦
     */
    public static final String LOG_MIC_MATRIX_TYPE_FOUR_CAR = "4_car";

    /**
     * 环形六麦
     */
    public static final String LOG_MIC_MATRIX_TYPE_SIX_CIRCLE = "6_circle";

    /**
     * 线性六麦
     */
    public static final String LOG_MIC_MATRIX_TYPE_SIX_LINE = "6_line";

    /**
     * 线性八麦
     */
    public static final String LOG_MIC_MATRIX_TYPE_EIGHT_LINE = "8_line";
    public static final String KEY_MIC_MATRIX = "mic_matrix";

    public static final String KEY_SCENE = "scene";

    public static final String KEY_LOG_WK_TYPE = "wakeup_log_type";

    public static final String KEY_UPLOAD_ENTRY = "upload_entry";

    /**
     * 全局音频保存路径
     */
    public static String GLOBAL_AUDIO_SAVE_PATH = "";

    /**
     * 全局音频保存开关
     */
    public static boolean GLOBAL_AUDIO_SAVE_ENABLE = false;

    /**
     * 设置全局多个音频保存的集合(需要先开启GLOBAL_AUDIO_SAVE_ENABLE才会生效)
     * 默认是全引擎保存
     * {@link com.aispeech.DUILiteSDK#setGlobalAudioSaveEngines(int)}
     */

    public static AtomicInteger GLOBAL_AUDIO_SAVE_ENGINES = new AtomicInteger(Integer.MAX_VALUE);

    /**
     * 全量上传模式(超时下次继续重复上传)
     */
    public static final String UPLOAD_MODE_FULL_RETRY = "full-retry";

    /**
     * 全量容错上传模式(超时删除文件，下次不重复上传)
     */
    public static final String UPLOAD_MODE_FULL = "full";

    /**
     * 抽样容错上传模式(超时删除文件，下次不重复上传)
     */
    public static final String UPLOAD_MODE_SAMPLE = "sample";

    /**
     * 禁止上传模式
     */
    public static final String UPLOAD_MODE_FORBIDDEN = "forbidden";
    /**
     * 允许上传模式
     */
    public static final String UPLOAD_MODE_ALLOW = "allow";



    /**
     * 全局音频文件总大小，默认为0，不删除超过大小文件
     */
    public static int GLOBAL_AUDIO_FILE_ALL_SIZE = 0;
    /**
     * 全局单音频大小，超过大小会另写一文件。默认不为0，不做限制
     */
    public static int GLOBAL_AUDIO_FILE_SINGLE_SIZE = 0;
    /**
     * 全局单音频拆分个数，超过后会删除之前拆分的音频。默认为0，不做限制
     */
    public static int GLOBAL_AUDIO_FILE_SPLIT_NUMBER = 0;

}
