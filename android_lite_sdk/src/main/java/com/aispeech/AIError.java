/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech;

import android.os.Parcel;
import android.os.Parcelable;

import com.aispeech.common.JSONUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

/**
 * 错误信息类<br>
 */
public class AIError extends Exception implements Parcelable {

    public static final String KEY_CODE = "errId";
    public static final String KEY_TEXT = "error";
    public static final String KEY_RECORD_ID = "recordId";
    public static final String KEY_TIME = "timestamp";
    public static final String KEY_EXT = "ext";
    /**
     * SDK未初始化
     */
    public static final int ERR_SDK_NOT_INIT = 70900;
    /**
     * 无法获取录音设备
     */
    public static final int ERR_DEVICE = 70901;
    /**
     * 无法启动引擎
     */
    public static final int ERR_AI_ENGINE = 70902;
    /**
     * 录音失败
     */
    public static final int ERR_RECORDING = 70903;
    /**
     * 没有检测到语音
     */
    public static final int ERR_NO_SPEECH = 70904;
    /**
     * 音频时长超出阈值,对应着引擎的setMaxSpeechTimeS方法
     */
    public static final int ERR_MAX_SPEECH = 70905;
    /**
     * FDM kernel没有初始化
     */
    public static final int ERR_FDM_NO_INIT = 70906;
    /**
     * 网络错误
     */
    public static final int ERR_NETWORK = 70911;
    /**
     * DNS解析失败
     */
    public static final int ERR_DNS = 70912;
    public static final int ERR_INVALID_RECORDER_TYPE = 70913;
    /**
     * 网络li连接超时
     */
    public static final int ERR_CONNECT_TIMEOUT = 70914;
    /**
     * TTS缓存失败
     */
    public static final int ERR_TTS_CACHE = 70915;
    /**
     * TTS播放队列已满
     */
    public static final int ERR_QUEUE_FULL = 70918;
    /**
     * 资源准备失败，请检查是否存放在assets目录下
     */
    public static final int ERR_RES_PREPARE_FAILED = 70920;
    /**
     * 设置识别服务器类型为custom须同时设置lmId
     */
    public static final int ERR_SERVICE_PARAMETERS = 70926;
    /**
     * 本地语法文件编译失败
     */
    public static final int ERR_GRAMMAR_FAILED = 70927;
    /**
     * 信号处理引擎还没有启动，请先启动信号处理引擎，再启动识别引擎
     */
    public static final int ERR_SIGNAL_PROCESSING_NOT_STARTED = 70928;
    /**
     * 语义输入文本空
     */
    public static final int ERR_NULL_SEMANTIC_INPUT = 70929;

    /**
     * 语义为空
     */
    public static final int ERR_NULL_SEMANTIC = 70931;
    /**
     * 接口调用顺序错误
     */
    public static final int ERR_INTERFACE_CALL_SEQUENCE = 70932;
    /**
     * 接口参数错误
     */
    public static final int ERR_INTERFACE_PARAM_ERROR = 70933;
    /**
     * 云端流量管控
     */
    public static final int ERR_403_FORBIDDEN = 70907;
    /**
     * CloudTTS 和 CloudASR 等所有云端功能都有可能返回401，原因是deviceId 冲突导致认证失败，需设置唯一的 deviceId
     */
    public static final int ERR_DEVICE_ID_CONFLICT_ASR = 72150;
    public static final int ERR_DEVICE_ID_CONFLICT_TTS = 73101;

    /**
     * CloudTTS 云端功能都有可能返回400，原因是请求参数错误
     */
    public static final int ERR_VOICE_SERVER_TTS = 73102;

    /**
     * tts错误，产品ID和voiceId无法匹配
     */
    public static final int ERR_TTS_PRODUCT_ID = 73103;

    /**
     * tts错误，请求参数错误
     */
    public static final int ERR_TTS_PARAMETERS = 73104;
    /**
     * asr plus 服务器返回的错误码，当不是 0 2 7 8 9 99时，统一认为是错误，走 onError,具体错误原因由用户再解析服务器的信息
     */
    public static final int ERR_ASR_PLUS_SERVER_ERR = 72151;
    /**
     * asr 服务器主动关闭 webSocket，code 1000
     */
    public static final int ERR_SERVER_CLOSE_WEBSOCKET_1000 = 72152;
    public static final String ERR_DESCRIPTION_SERVER_CLOSE_WEBSOCKET_1000 = "服务器主动关闭了WebSocket";
    /******************声纹错误码*********************/
    public static final int ERR_VOICE_LOW = 70939;//规定时间没说话/声音较低
    public static final int ERR_DEFAULT = 70940;//未知失败
    public static final int ERR_NO_SPKEAKER = 70941;//用户尚未注册
    public static final int ERR_NO_REGISTERED_WORD = 70942;//用户未注册过该文本
    public static final int ERR_SPK_REGISTERED_WORD = 70943;//用户已经注册过该文本
    public static final int ERR_REGISTER_SPK_FULL = 70944;//注册用户数量已满
    public static final int ERR_UNSUPPORT_GENDER = 70945;//不支持该性别
    public static final int ERR_UNSUPPORT_WORD = 70946;//不支持该词语
    public static final int ERR_SPEECH_SPEED_SLOW = 70947;//语速过慢
    public static final int ERR_SPEECH_SPEED_FAST = 70948;//语速过快
    public static final int ERR_SNR_LOW = 70949;//信噪比过低
    public static final int ERR_SPEECH_CLIPPING = 70950;//音频截幅
    public static final int ERR_KEYWORD_UNMATCH = 70951;//未说唤醒词
    public static final String ERR_DESCRIPTION_TTS_VOICE_SERVER = "声纹服务运行出错";
    public static final String ERR_DESCRIPTION_TTS_PARAMETERS = "请求参数错误";
    public static final String ERR_DESCRIPTION_TTS_PRODUCT_ID = "产品ID和voiceId无法匹配";
    /**
     * asr 超时
     */
    public static final int ERR_TIMEOUT_ASR = 70961;

    //    public static final int ERR_SPK_REGISTER_SPEECH_LACK = 70951;//用户的注册的音频条数不够

//    public static final int ERR_REGISTER_MODEL_EXPIRED = 70952;//模型不兼容需要重新注册

    //    public static final int ERR_REGISTER_UPDATE_MODEL_FINISHED = 70953;//升级模型结束
    public static final String ERR_DESCRIPTION_TIMEOUT_ASR = "等待识别结果超时";
    /**
     * 无效的合成文本
     */
    public static final int ERR_TTS_INVALID_REFTEXT = 72203;
    /**
     * 合成MediaPlayer播放器错误
     */
    public static final int ERR_TTS_MEDIAPLAYER = 72204;
    /**
     * 当前音色不支持音素
     */
    public static final int ERR_TTS_NO_SUPPORT_PHONEME = 72205;
    /**
     * TTS 用 AudioTrack 播放时出错
     */
    public static final int ERR_TTS_AUDIO_TRACK = 73105;
    public static final String ERR_DESCRIPTION_TTS_AUDIO_TRACK = "合成后AudioTrack播放错误";
    /**
     * 动态库加载失败
     */
    public static final int ERR_SO_INVALID = 70991;
    /**
     * 识别引擎内容更新失败
     */
    public static final int ERR_NET_BIN_INVALID = 70992;

    /**
     * 内置语义更新失败
     */
    public static final int ERR_SEMANTIC_UPDAYE_NAVI= 70993;

    /**
     * 请求401,请重新尝试初始化
     */
    public static final int ERR_RETRY_INIT = 70930;

    /**
     * native api 响应结果超时
     */
    public static final int ERR_NATIVE_API_TIMEOUT = 709501;
    /**
     * 对话服务返回的结果无效
     */
    public static final int ERR_INVALID_DM_RESULT = 709502;
    /**
     * 对话服务返回recorder id 结果不一致
     */
    public static final int ERR_INVALID_DM_RECORDER_ID = 709503;

    /******************声纹错误描述*********************/
    public static final String ERR_DESCRIPTION_VOICE_LOW = "没听清，请再大声一点";
    public static final String ERR_DESCRIPTION_DEFAULT = "未知错误";
    public static final String ERR_DESCRIPTION_NO_SPEAKER = "该用户尚未注册";
    public static final String ERR_DESCRIPTION_NO_REGISTERED_WORD = "该用户未注册过该文本";
    public static final String ERR_DESCRIPTION_SPK_REGISTERED_WORD = "该用户已经注册过该文本";
    public static final String ERR_DESCRIPTION_REGISTER_SPK_FULL = "注册用户数量已满";
    public static final String ERR_DESCRIPTION_UNSUPPORT_GENDER = "不支持该性别";
    public static final String ERR_DESCRIPTION_UNSUPPORT_WORD = "不支持该词语";
    public static final String ERR_DESCRIPTION_SPK_REGISTER_SPEECH_LACK = "用户的注册的音频条数不够";
    public static final String ERR_DESCRIPTION_REGISTER_MODEL_EXPIRED = "模型不兼容需要重新注册";
    public static final String ERR_DESCRIPTION_REGISTER_UPDATE_MODEL_FINISHED = "升级模型结束";
    public static final String ERR_DESCRIPTION_SPEECH_SPEED_SLOW = "请加快语速再说一次";
    public static final String ERR_DESCRIPTION_SPEECH_SPEED_FAST = "请慢点清晰的再说一次";
    public static final String ERR_DESCRIPTION_SNR_LOW = "信噪比过低，请安静场景下再试一次";
    public static final String ERR_DESCRIPTION_SPEECH_CLIPPING = "音频已截幅，请距离远一点再试一下";
    public static final String ERR_DESCRIPTION_KEYWORD_UNMATCH = "请到安静环境下再说一遍唤醒词";


    /********************云端对话错误描述****************/
    public static final String ERR_DESCRIPTION_NATIVE_API_TIMEOUT = "NativeApi响应超时";

    public static final String ERR_DESCRIPTION_ERR_INVALID_DM_RESULT = "无效的对话结果";

    public static final String ERR_DESCRIPTION_INVALID_DM_RECORDER_ID = "对话服务返回recorderId不一致";

    public static final String ERR_DESCRIPTION_CALL_SEQUENCE = "接口调用顺序错误或者重复调用接口";
    public static final String ERR_DESCRIPTION_PARM_ERROR = "参数错误";
    /*******************长语音离线文件识别**********************/
    public static final int ERR_LASR_SERVER_INNER_ERR = 74101;
    /******************声纹错误描述*********************/
    public static final int ERR_LASR_SERVER_METHOD_ERR = 74102;
    public static final int ERR_LASR_SERVER_PARAM_LOST = 74103;
    public static final int ERR_LASR_SERVER_SIGNA_ERR = 74104;
    public static final int ERR_LASR_SERVER_URL_PARAM_ERR = 74105;
    public static final int ERR_LASR_SERVER_UNKONW_PATH = 74106;
    public static final int ERR_LASR_SERVER_QUERY_TASK_INFO = 74107;
    public static final int ERR_LASR_SERVER_CREATE_TASK_FAIL = 74108;
    public static final int ERR_LASR_SERVER_UPDATE_INFO_FAILE = 74109;
    public static final int ERR_LASR_SERVER_AUDIO_DOWNLOAD_FIAL = 74110;
    public static final int ERR_LASR_SERVER_AUDIO_CUT_FAILED = 74111;
    public static final int ERR_LASR_SERVER_AUDIO_SAVE_ERR = 74112;
    public static final int ERR_LASR_SERVER_POSTPROCESSING_FAIL = 74113;
    public static final int ERR_LASR_SERVER_POSTPROCESSING_TIMEOUT = 74114;
    public static final int ERR_LASR_SERVER_FILE_NO_EXIST = 74115;
    public static final int ERR_LASR_SERVER_AUTH_FAILED = 74116;
    public static final int ERR_LASR_SERVER_USE_UP = 74117;
    public static final int ERR_LASR_SERVER_FLOW_CONTROL = 74118;
    public static final int ERR_LASR_SERVER_SAVE_AUDIO = 74119;
    public static final int ERR_LASR_AUDIO_PARAM_ERR = 74140;
    public static final int ERR_LASR_FILE_NOT_EXIST_OR_FILELENGTH_0 = 74141;
    public static final int ERR_LASR_FILE_OVER_SIZE = 74142;
    public static final int ERR_LASR_ONLY_ONE_TASK = 74143;
    public static final int ERR_LASR_FILE_DIFFERENT = 74144;
    public static final int ERR_LASR_JSON_ERR = 74145;
    public static final int ERR_LASR_CALL_CANCEL = 74146;
    public static final String ERR_DESCRIPTION_LASR_SERVER_INNER_ERR = "服务器内部错误";
    public static final String ERR_DESCRIPTION_LASR_SERVER_METHOD_ERR = "请求方法错误";
    public static final String ERR_DESCRIPTION_LASR_SERVER_PARAM_LOST = "客户端参数缺失";
    public static final String ERR_DESCRIPTION_LASR_SERVER_SIGNA_ERR = "signa验证失败";
    public static final String ERR_DESCRIPTION_LASR_SERVER_URL_PARAM_ERR = "客户端参数里的url格式不对, 如file_path, callback";
    public static final String ERR_DESCRIPTION_LASR_SERVER_UNKONW_PATH = "未知的请求路径";
    public static final String ERR_DESCRIPTION_LASR_SERVER_QUERY_TASK_INFO = "查询任务信息失败";
    public static final String ERR_DESCRIPTION_LASR_SERVER_CREATE_TASK_FAIL = "创建任务失败";
    public static final String ERR_DESCRIPTION_LASR_SERVER_UPDATE_INFO_FAILE = "更新任务信息失败";
    public static final String ERR_DESCRIPTION_LASR_SERVER_AUDIO_DOWNLOAD_FIAL = "音频下载失败";
    public static final String ERR_DESCRIPTION_LASR_SERVER_AUDIO_CUT_FAILED = "音频切割失败";
    public static final String ERR_DESCRIPTION_LASR_SERVER_AUDIO_SAVE_ERR = "音频保存失败或路径有异常";
    public static final String ERR_DESCRIPTION_LASR_SERVER_POSTPROCESSING_FAIL = "音频后处理失败";
    public static final String ERR_DESCRIPTION_LASR_SERVER_POSTPROCESSING_TIMEOUT = "音频后处理超时";
    public static final String ERR_DESCRIPTION_LASR_SERVER_FILE_NO_EXIST = "要加载的音频文件不存在";
    public static final String ERR_DESCRIPTION_LASR_SERVER_AUTH_FAILED = "apikey方式的鉴权失败";
    public static final String ERR_DESCRIPTION_LASR_SERVER_USE_UP = "产品触发达到使用量上限，用量管控";
    public static final String ERR_DESCRIPTION_LASR_SERVER_FLOW_CONTROL = "产品触发流控";
    public static final String ERR_DESCRIPTION_LASR_SERVER_SAVE_AUDIO = "服务器保存文件出错";
    public static final String ERR_DESCRIPTION_LASR_AUDIO_PARAM_ERR = "AudioParam 参数不正确";
    public static final String ERR_DESCRIPTION_LASR_FILE_NOT_EXIST_OR_FILELENGTH_0 = "文件不存在或文件大小为0";
    public static final String ERR_DESCRIPTION_LASR_FILE_OVER_SIZE = "音频大小超过限制，不能上传";
    public static final String ERR_DESCRIPTION_LASR_ONLY_ONE_TASK = "正在上传文件，只能进行一个文件上传任务";
    public static final String ERR_DESCRIPTION_LASR_FILE_DIFFERENT = "上传的文件大小与预期的不符，文件可能被改动！";
    public static final String ERR_DESCRIPTION_LASR_JSON_ERR = "Json解析异常，返回结果不是一个json";
    /*******************长语音实时识别**********************/
    public static final int ERR_LASR_403_FORBIDDEN = 74201;

    /*******************长语音离线文件识别**********************/
    public static final String ERR_DESCRIPTION_LASR_403_FORBIDDEN = "Forbidden，产品时长用尽或触发流控";
    /*******************一句话识别**********************/
    public static final int ERR_ASR_SENTENCE_SERVER_INNER_ERR = 74301;
    /*******************长语音实时识别**********************/
    public static final int ERR_ASR_SENTENCE_SERVER_METHOD_ERR = 74302;
    public static final int ERR_ASR_SENTENCE_SERVER_PARAM_LOST = 74303;
    public static final int ERR_ASR_SENTENCE_SERVER_UNKONW_PATH = 74306;
    public static final int ERR_ASR_SENTENCE_SERVER_AUTH_FAILED = 74316;
    public static final int ERR_ASR_SENTENCE_SERVER_USE_UP = 74317;
    public static final int ERR_ASR_SENTENCE_SERVER_FLOW_CONTROL = 74318;
    public static final int ERR_ASR_SENTENCE_SERVER_SAVE_AUDIO = 74319;
    public static final int ERR_ASR_SENTENCE_SERVER_ASR_ERROR = 74320;
    public static final int ERR_ASR_SENTENCE_AUDIO_PARAM_ERR = 74340;
    public static final int ERR_ASR_SENTENCE_FILE_NOT_EXIST_OR_FILELENGTH_0 = 74341;
    public static final int ERR_ASR_SENTENCE_FILE_OVER_SIZE = 74342;
    public static final int ERR_ASR_SENTENCE_JSON_ERR = 74345;
    // 一句话识别的错误提示复用 长语音文件转写的。
    public static final String ERR_DESCRIPTION_ASR_SENTENCE_ASR_ERROR = "转写失败，%s";
    /*******************一句话识别**********************/

    // 唤醒
    public static final int ERR_WAKEUP_NOT_SUPPORT_IN_RES = 74401;
    public static final String ERR_DESCRIPTION_WAKEUP_NOT_SUPPORT_IN_RES = "唤醒资源不支持设置的唤醒词";
    /******************通用错误描述*********************/
    public static final String ERR_DESCRIPTION_ERR_SDK_NOT_INIT = "SDK尚未初始化，请初始化并授权成功后使用";
    public static final String ERR_DESCRIPTION_DEVICE = "无法获取录音设备!";
    public static final String ERR_DESCRIPTION_AI_ENGINE = "无法启动引擎!";
    public static final String ERR_DESCRIPTION_RECORDING = "录音失败!";
    public static final String ERR_DESCRIPTION_NO_SPEECH = "没有检测到语音";
    public static final String ERR_DESCRIPTION_MAX_SPEECH = "音频时长超出阈值";
    public static final String ERR_DESCRIPTION_ERR_NETWORK = "网络错误";
    public static final String ERR_DESCRIPTION_CONNECT_TIMEOUT = "连接服务器超时";
    public static final String ERR_DESCRIPTION_RES_PREPARE_FAILED = "资源准备失败，请检查是否存放在assets目录下";
    public static final String ERR_DESCRIPTION_ERR_DNS = "没有网络或者dns解析失败";
    public static final String ERR_DESCRIPTION_ERR_TTS_CACHE = "音频缓存失败";
    public static final String ERR_DESCRIPTION_ERR_QUEUE_FULL = "播放队列已满";
    public static final String ERR_DESCRIPTION_ERR_GRAMMAR_FAILED = "本地语法文件编译失败，请检查xbnf文件路径或文本是否合法";
    public static final String ERR_DESCRIPTION_SIGNAL_PROCESSING_NOT_STARTED = "信号处理引擎还没有启动，请先启动信号处理引擎，再启动识别引擎";
    public static final String ERR_DESCRIPTION_ERR_FDM_NOT_INIT = "未init成功fdm或语音引擎模块";
    public static final String ERR_DESCRIPTION_TTS_MEDIAPLAYER = "合成MediaPlayer播放器错误:";
    public static final String ERR_DESCRIPTION_TTS_INVALID_REFTEXT = "无效的合成文本";
    public static final String ERR_DESCRIPTION_TTS_NO_SUPPORT_PHONEME = "当前音色不支持音素信息，请关闭音素功能";
    public static final String ERR_DESCRIPTION_ERR_SERVICE_PARAMETERS = "设置识别服务器类型为custom须同时设置lmId";
    public static final String ERR_DESCRIPTION_INVALID_RECORDER_TYPE = "无效的麦克风类型";
    public static final String ERR_DESCRIPTION_SO_INVALID = "so动态库加载失败";
    public static final String ERR_DESCRIPTION_DEVICE_ID_CONFLICT = "deviceId 冲突导致认证失败";
    public static final String ERR_DESCRIPTION_403_FORBIDDEN = "Forbidden，产品时长用尽或触发流控";
    /**************云端声纹错误码*********************/
    public static final int ERR_CODE_CLOUD_VPRINT_VIDEO_LEAK = 74501;
    public static final int ERR_CODE_CLOUD_VPRINT_CONTEXT_NOT_MATCHED = 74502;
    public static final int ERR_CODE_CLOUD_VPRINT_MODEL_SERVER_FAILED = 74503;
    public static final int ERR_CODE_CLOUD_VPRINT_PLAY_RECORDED_VIDEO = 74504;
    public static final int ERR_CODE_CLOUD_VPRINT_MODEL_LOADED_FAILED = 74505;
    public static final int ERR_CODE_CLOUD_VPRINT_CANT_ENHANCE = 74506;
    public static final int ERR_CODE_CLOUD_VPRINT_CACHE_FAILED = 74507;
    public static final int ERR_CODE_CLOUD_VPRINT_QUEST_ERROR = 74508;
    public static final int ERR_CODE_CLOUD_VPRINT_QUEST_NOT_SUPPORT = 74509;
    public static final int ERR_CODE_CLOUD_VPRINT_VIDEO_PARAMS = 74510;
    public static final int ERR_CODE_CLOUD_VPRINT_NO_USERS = 74511;
    public static final int ERR_CODE_CLOUD_VPRINT_NO_TASKS = 74512;
    public static final int ERR_CODE_CLOUD_VPRINT_TASKS_OUT_OF_TIME = 74513;
    public static final int ERR_CODE_CLOUD_VPRINT_MONGO_SAVE_FAILED = 74514;
    public static final int ERR_CODE_CLOUD_VPRINT_GET_MODLE_FAILED = 74515;
    public static final int ERR_CODE_CLOUD_VPRINT_DELETE_USER_FAILED = 74516;
    public static final int ERR_CODE_CLOUD_VPRINT_NO_DELETEDUSER = 74517;
    public static final int ERR_CODE_CLOUD_VPRINT_DELETE_PARAMS_ERR = 74518;
    public static final int ERR_CODE_CLOUD_VPRINT_NO_SESSION = 74519;
    public static final int ERR_CODE_CLOUD_VPRINT_REQUEST_TYPE_NOT_SUPPORT = 74520;
    public static final int ERR_CODE_CLOUD_VPRINT_PRE_REGISTER_ERR = 74521;
    public static final int ERR_CODE_CLOUD_VPRINT_NO_KERNEL_LINKED = 74522;
    public static final int ERR_CODE_CLOUD_VPRINT_NO_CONTEXT_FROM_KERNEL = 74523;
    public static final int ERR_CODE_CLOUD_VPRINT_ASR_ERR = 74524;
    public static final int ERR_CODE_CLOUD_VPRINT_ASR_EXCEPTION = 74525;
    public static final int ERR_CODE_CLOUD_VPRINT_ASR_NOT_MATCH = 74526;
    public static final int ERR_CODE_CLOUD_VPRINT_MUL_START = 74527;
    public static final int ERR_CODE_CLOUD_VPRINT_HTTP_VIDEO_ERR = 74528;
    public static final int ERR_CODE_CLOUD_VPRINT_HTTP_PARAMS_ERR = 74529;
    public static final int ERR_CODE_CLOUD_VPRINT_VERIFY_PARAMS_ERR = 74530;
    public static final int ERR_CODE_CLOUD_VPRINT_PATH_NOT_SUPPORT = 74531;
    public static final int ERR_CODE_CLOUD_VPRINT_PLS_AUTH_FAILED = 74532;
    public static final int ERR_CODE_CLOUD_VPRINT_OVER_PLS_COUNT = 74533;
    public static final int ERR_CODE_CLOUD_VPRINT_SPEAKER_BACK_FAILED = 74534;
    public static final int ERR_CODE_CLOUD_VPRINT_JSON_ERR = 74535;
    public static final int ERR_CODE_CLOUD_VPRINT_MODE_NOT_SUPPORT = 74536;
    public static final int ERR_CODE_CLOUD_VPRINT_NO_FILE = 74537;
    public static final int ERR_CODE_CLOUD_VPRINT_RESULT_NOT_MATCHED = 74538;
    /**************云端声纹错误描述*********************/
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_VIDEO_LEAK = "有效音频不足，任务无法完成";
    /**************云端声纹错误码*********************/
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_CONTEXT_NOT_MATCHED = "音频文本内容不匹配";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_MODEL_SERVER_FAILED = "模型推理服务连接失败";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_PLAY_RECORDED_VIDEO = "回放转录的音频";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_MODEL_LOADED_FAILED = "注册的模型加载失败";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_CANT_ENHANCE = "constantContent（唤醒词）尚未注册，无法对增强内容进行声纹识别";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_CACHE_FAILED = "cache 信息解析失败";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_QUEST_ERROR = "请求格式错误";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_QUEST_NOT_SUPPORT = "不支持的请求操作（非验证注册）";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_VIDEO_PARAMS = "音频参数错误";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_NO_USERS = "验证时没有传用户或者用户如果没有注册";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_NO_TASKS = "任务不存在";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_TASKS_OUT_OF_TIME = "任务过期";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_MONGO_SAVE_FAILED = "数据库写失败";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_GET_MODLE_FAILED = "验证时取传用户模型失败";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_DELETE_USER_FAILED = "删除用户失败";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_NO_DELETEDUSER = "当前删除用户不存在";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_DELETE_PARAMS_ERR = "删除时参数错误";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_NO_SESSION = "当前请求session不存在";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_REQUEST_TYPE_NOT_SUPPORT = "不支持当前请求类型";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_PRE_REGISTER_ERR = "预注册时格式错误";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_NO_KERNEL_LINKED = "没有可用的kernel链接";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_NO_CONTEXT_FROM_KERNEL = "内核没有返回用户需要输入的文本";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_ASR_ERR = "识别服务错误";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_ASR_EXCEPTION = "识别结果异常";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_ASR_NOT_MATCH = "识别结果与预期字符不匹配";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_MUL_START = "已经发送过start，请重新开始";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_HTTP_VIDEO_ERR = "http获取音频异常";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_HTTP_PARAMS_ERR = "http缺少参数";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_VERIFY_PARAMS_ERR = "url中验证参数失败";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_PATH_NOT_SUPPORT = "不支持的path";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_PLS_AUTH_FAILED = "pls授权失败";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_OVER_PLS_COUNT = "超过pls最大授权个数";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_SPEAKER_BACK_FAILED = "话者分离返回失败";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_JSON_ERR = "JSON解析错误";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_MODE_NOT_SUPPORT = "当前mode不支持当前的网络验证方式";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_NO_FILE = "音频文件不存在或者数据为空";
    public static final String ERR_DESCRIPTION_CLOUD_VPRINT_RESULT_NOT_MATCHED = "声纹结果和用户id不匹配";
    public static final Creator<AIError> CREATOR = new Creator<AIError>() {
        public AIError createFromParcel(Parcel in) {
            return new AIError(in);
        }

        public AIError[] newArray(int size) {
            return new AIError[size];
        }
    };
    private static final long serialVersionUID = 1L;
    /**************云端声纹错误描述*********************/
    /**************语义错误描述*****************/
    public static final String ERR_DESCRIPTION_NULL_SEMANTIC_INPUT = "语义输入文本空";
    public static final String ERR_DESCRIPTION_NULL_SEMANTIC = "语义为空";
    /**************语义错误描述*****************/
    public static final String ERR_DESCRIPTION_INVALID_NET_BIN = "netbin文件异常";
    /**
     * 内置语义更新失败
     */
    public static final String ERR_DESCRIPTION_SEMANTIC_UPDAYE_NAVI= "更新内置语义词库失败";

    public static final String ERR_RETRY_INIT_MSG = "服务401 Unauthorized，请重新尝试init授权...";
    /*********vprintlite 错误码 ******************************************/
    public static final int ERR_CODE_VPRINT_LITE_VIDEO_LEAK = 74601;
    public static final int ERR_CODE_VPRINT_LITE_CONTEXT_NOT_MATCHED = 74602;
    public static final int ERR_CODE_VPRINT_LITE_MODEL_SERVER_FAILED = 74603;
    public static final int ERR_CODE_VPRINT_LITE_MODEL_LOADED_FAILED = 74605;
    public static final int ERR_CODE_VPRINT_LITE_CANT_ENHANCE = 74606;
    public static final int ERR_CODE_VPRINT_LITE_CACHE_FAILED = 74607;
    public static final int ERR_CODE_VPRINT_LITE_NO_REGISTER_SPEAKER = 74608;
    /*********vprintlite 错误码 ******************************************/
    /*********vprintlite 错误码描述 ******************************************/
    public static final String ERR_DESCRIPTION_VPRINT_LITE_VIDEO_LEAK = "有效音频不足，任务无法完成";
    public static final String ERR_DESCRIPTION_VPRINT_LITE_CONTEXT_NOT_MATCHED = "音频文本内容不匹配";
    public static final String ERR_DESCRIPTION_VPRINT_LITE_MODEL_SERVER_FAILED = "模型推理服务连接失败";
    public static final String ERR_DESCRIPTION_VPRINT_LITE_MODEL_LOADED_FAILED = "注册的模型加载失败";
    public static final String ERR_DESCRIPTION_VPRINT_LITE_CANT_ENHANCE = "constantContent（唤醒词）尚未注册，无法对增强内容进行声纹识别";
    public static final String ERR_DESCRIPTION_VPRINT_LITE_CACHE_FAILED = "cache 信息解析失败";
    public static final String ERR_DESCRIPTION_VPRINT_LITE_NO_REGISTER_SPEAKER = "当前用户未注册";
    /*********vprintlite 错误码描述 ******************************************/
    private int errId = ERR_SDK_NOT_INIT;
    private String error;
    private String recordId;
    private long timestamp = -1;
    private String ext = null;
    private Map<Object, Object> eventMap;
    private JSONObject inputJson;

    /**
     * 构造函数
     *
     * @param jsonString jsonString
     */
    public AIError(String jsonString) {
        this(jsonString, null);
    }

    /**
     * 构造函数
     *
     * @param jsonString jsonString
     * @param recordId   recordId
     */
    public AIError(String jsonString, String recordId) {
        stringToJSON(jsonString);
        setRecordId(recordId);
        setTimestamp(System.currentTimeMillis());
    }

    /**
     * 构造函数
     *
     * @param errId errId
     */
    public AIError(int errId) {
        this(errId, null);
    }

    /**
     * 构造函数
     *
     * @param errId errId
     * @param error error
     */
    public AIError(int errId, String error) {
        this(errId, error, null);
    }

    /**
     * 构造函数
     *
     * @param errId    errId
     * @param error    error
     * @param recordId recordId
     */
    public AIError(int errId, String error, String recordId) {
        this(errId, error, recordId, System.currentTimeMillis());
    }

    /**
     * 构造函数
     *
     * @param errId     errId
     * @param error     error
     * @param recordId  recordId
     * @param timestamp timestamp
     */
    public AIError(int errId, String error, String recordId, long timestamp) {
        super(error);
        this.errId = errId;
        this.error = error;
        this.recordId = recordId;
        this.timestamp = timestamp;
    }

    private AIError(Parcel in) {
        errId = in.readInt();
        error = in.readString();
        ext = in.readString();
        recordId = in.readString();
        timestamp = in.readLong();
    }

    public AIError() {
    }

    public Map<Object, Object> getEventMap() {
        return eventMap;
    }

    public AIError setEventMap(Map<Object, Object> event) {
        this.eventMap = event;
        return this;
    }

    /**
     * 是否是网络错误
     *
     * @return 是否是网络错误
     */
    public boolean isNetWorkError() {
        return Integer.toString(errId).startsWith("706");
    }

    /**
     * 获取错误简述
     *
     * @return 错误简述
     */
    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    /**
     * 获取错误码
     *
     * @return 错误码
     */
    public int getErrId() {
        return errId;
    }

    public void setErrId(int errId) {
        this.errId = errId;
    }

    public void setErrId(String errId) {
        try {
            this.errId = Integer.parseInt(errId);
        } catch (Exception e) {
            //do nothing
        }
    }

    public void stringToJSON(String jsonString) {
        if (jsonString != null) {
            try {
                JSONObject json = new JSONObject(jsonString);
                if (json.has(KEY_CODE)) {
                    errId = json.getInt(KEY_CODE);
                    if (json.has(KEY_TEXT)) {
                        error = json.getString(KEY_TEXT);
                    }
                } else {
                    JSONObject result = json.optJSONObject("result");
                    if (result != null && result.has(KEY_CODE)) {
                        errId = result.optInt(KEY_CODE);
                    }
                    if (result != null && result.has(KEY_TEXT)) {
                        error = result.getString(KEY_TEXT);
                    }
                }

            } catch (JSONException e) {
                //do nothing
            }
        }
    }

    /**
     * 将AIError信息转换为JSON格式内容
     *
     * @return JSONObject
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        JSONUtil.putQuietly(json, KEY_CODE, errId);
        JSONUtil.putQuietly(json, KEY_TEXT, error);
        if (recordId != null)
            JSONUtil.putQuietly(json, KEY_RECORD_ID, recordId);
        if (timestamp > 0)
            JSONUtil.putQuietly(json, KEY_TIME, timestamp);
        if (ext != null) {
            JSONUtil.putQuietly(json, KEY_EXT, ext);
        }
        return json;
    }

    public String toString() {
        return toJSON().toString();
    }

    public JSONObject getOutputJSON() {
        JSONObject jsonObject = new JSONObject();
        JSONUtil.putQuietly(jsonObject, KEY_CODE, errId);
        JSONUtil.putQuietly(jsonObject, KEY_TEXT, error);
        return jsonObject;
    }

    public void setInputJson(JSONObject inputJson) {
        this.inputJson = inputJson;
    }

    public JSONObject getInputJSON() {
        return inputJson;
    }

    /**
     * 获取错误额外信息
     *
     * @return 错误额外信息
     */
    public Object getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    /**
     * 获取recordId
     *
     * @return start操作对应的recordId
     */
    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    /**
     * 错误发生时间戳
     *
     * @return 时间戳
     */
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int describeContents() {
        // nothing to do
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(errId);
        dest.writeString(error);
        dest.writeString(ext);
        dest.writeString(recordId);
        dest.writeLong(timestamp);
    }

}
