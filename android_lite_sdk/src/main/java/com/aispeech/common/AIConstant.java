package com.aispeech.common;

import com.aispeech.lite.AISpeechSDK;

/**
 * 对外常量类
 */
public class AIConstant {
    private AIConstant() {
    }

    /**
     * 内置唤醒参数 双唤醒阈值
     */
    public static final String BUILTIN_WAKEUPWORD_THRESH2 = "builtinWakeupwordThresh2";

    /**
     * 内置唤醒参数 E2E高低唤醒阈值
     */
    public static final String BUILTIN_WAKEUPWORD_E2E = "builtinWakeupwordE2E";


    /**
     * SDK版本
     */
    public static final String SDK_VERSION = AISpeechSDK.SDK_VERSION;

    /**
     * 表示操作成功
     */
    public static final int OPT_SUCCESS = 0;
    /**
     * 表示操作失败
     */
    public static final int OPT_FAILED = -1;
    /**
     * 结果类型JSON
     */
    public static final int AIENGINE_MESSAGE_TYPE_JSON = 0;
    /**
     * 结果类型BINARY
     */
    public static final int AIENGINE_MESSAGE_TYPE_BIN = 1;

    /**
     * 声纹的数据类型，结构体，包含音频和其它信息
     */
    public static final int DUILITE_MSG_TYPE_TLV = 2;

    /**
     * 结果类型KWS_BINARY，文本相关
     */
    public static final int DUILITE_MSG_TYPE_KWS_BINARY = 3;

    /**
     * 驾驶模式：定位模式，按照声源定位
     */
    public static final int DRIVE_MODE_LOCATE = 0;

    /**
     * 驾驶模式：主驾模式
     */
    public static final int DRIVE_MODE_MAIN = 1;

    /**
     * 驾驶模式：副驾模式
     */
    public static final int DRIVE_MODE_DEPUTY = 2;

    /**
     * 驾驶模式：全模式
     */
    public static final int DRIVE_MODE_ALL = 3;

    /**
     * 大音量状态
     */
    public static final String MAX_VOLUME_ON = "{\"maxVolumeState\": 1}";

    /**
     * 非大音量状态
     */
    public static final String MAX_VOLUME_OFF = "{\"maxVolumeState\": 0}";


    /**
     * 声纹功能模式：注册模型
     */
    public static final String VP_REGISTER = "register";

    /**
     * 云端合成默认音色
     */
    public static final String CLOUD_TTS_SPEAK_DEFAULT = "zhilingf";

    /**
     * 声纹功能模式：删除模型
     */
    public static final String VP_UNREGISTER = "unregister";

    /**
     * 声纹功能模式：删除模型中全部记录
     */
    public static final String VP_UNREGISTER_ALL = "unregisterall";

    /**
     * 声纹功能模式：追加模型
     */
    public static final String VP_APPEND = "append";


    /**
     * 声纹功能模式：测试/签到
     */
    public static final String VP_TEST = "test";


    /**
     * 声纹功能模式：覆盖更新已有的模型
     */
    public static final String VP_UPDATE = "update";

    /**
     * 云端合成音频类型：mp3格式
     */
    public static final String TTS_AUDIO_TYPE_MP3 = "mp3";

    /**
     * 云端合成mp3码率：低质量
     */
    public static final String TTS_MP3_QUALITY_LOW = "low";

    /**
     * 云端合成mp3码率：高质量
     */
    public static final String TTS_MP3_QUALITY_HIGH = "high";

    /**
     * 获取aecChannelNum时传入的key
     */
    public static final String KEY_GET_DOA = "getDoa";


    /**
     * 获取bfChannelNum时传入的key
     */
    public static final String KEY_FESPX_BF_CHANNEL = "bfWavChan";

    /**
     * 获取aecChannelNum时传入的key
     */
    public static final String KEY_FESPX_AEC_CHANNEL = "aecWavChan";

    /**
     * vprintlite 声纹类型textDependent
     */
    public static final String VPRINTLITE_TYPE_TD = "textDependent";

    /**
     * vprintlite 声纹类型speakerRecognition
     */
    public static final String VPRINTLITE_TYPE_SR = "speakerRecognition";

    /**
     * vprintlite 声纹类型speakerAntiSpoofing
     */
    public static final String VPRINTLITE_TYPE_ANTI_SPOOFING = "speakerAntiSpoofing";

    /**
     * SDK初始化防抖时间间隔
     */

    public static final long INIT_INTERVAL = 1000L;

    /**
     * 词库更新时长
     */

    public static final int VOCAB_UPDATE_TIMEOUT = 10000;

    public interface Nlu {
        String KEY_SOURCE = "source";

        /**
         * 离线dui语义
         */
        String SOURCE_DUI = "dui";

        /**
         * 离线导航语义
         */
        String SOURCE_NAVI = "navi";

        /**
         * 离线内置语义
         */
        String SOURCE_AIDUI = "aidui";

        /**
         * 离线bcdv2语义
         */
        String SOURCE_BCDV2 = "bcdv2";
    }

    public interface DOA {
        int TYPE_WAKEUP = 1;
        int TYPE_QUERY = 0;
    }
}
