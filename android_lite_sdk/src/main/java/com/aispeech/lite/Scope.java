package com.aispeech.lite;

/**
 * DUILite scope，用于授权文件本地模块校验，云端功能不进行鉴权拦截，由服务器判断。
 */
public interface Scope {

    /**
     * 单麦唤醒
     */
    String WAKEUP = "wakeup";

    /**
     * 前端信号处理（sspe），支持各种信号处理算法的组合应用，与线性麦克风阵列、环形麦克风阵列等模块类似。
     * sspe目前包括的功能有回声消除（echo）、盲源分离（blind Source Separation）、唤醒（wakeup）等功能模块。
     */
    String SSPE = "sspe";

    /**
     * 家居双麦信号处理+唤醒
     */
    String FESPD = "fespd";

    /**
     * 车载双麦信号处理+唤醒
     */
    String FESPCAR = "fespCar";

    /**
     * 线性4麦信号处理+唤醒
     */
    String FESPL_4 = "fespl-4";

    /**
     * 线性6麦信号处理+唤醒
     */
    String FESPL_6 = "fespl-6";

    /**
     * 线性6麦信号处理+唤醒
     */
    String FESPL_8 = "fespl-8";
    /**
     * 环形4麦信号处理+唤醒
     */
    String FESPA_4 = "fespa-4";

    /**
     * 环形6麦信号处理+唤醒
     */
    String FESPA_6 = "fespa-6";


    /**
     * L形状4麦信号处理+唤醒
     */
    String FESP_SHAPE_L4 = "fesp-L4";

    /**
     * 本地识别
     */
    String LOCAL_ASR = "asr";
    /**
     * 本地长语音
     */
    String LOCAL_STREAM_ASR = "streamasr";
    /**
     * 本地合成
     */
    String LOCAL_TTS = "tts";

    /**
     * 本地声纹
     */
    String LOCAL_VPRINT = "vprint";

    /**
     * 本地asr++识别(性别，情绪，年龄)
     */
    String LOCAL_ASRPP = "asrpp";

    /**
     * 本地语法编译
     */
    String LOCAL_GRAMMAR = "grammar";

    /**
     * 本地语义
     */
    String LOCAL_SEMANTIC = "semantic";

    /**
     * 本地单通道降噪
     */
    String LOCAL_NR = "nr";

    /**
     * 本地人声检测
     */
    String LOCAL_VAD = "vad";

    /**
     * 本地回消
     */
    String LOCAL_ECHO = "echo";

    String LOCAL_ONESHOT = "asr";

    /**
     * 线性4麦信号处理+唤醒
     */
    String DMASP = "dmasp-4";

    /**
     * sevc 多路音频经过 aec_bss_nr_agc 后出一路音频用于voip
     */
    String LOCAL_SEVC = "sevc";

    /**
     * mds, 多设备选择（mds，multiple device selection），用于在存在多个语音采集设备的场景中，选择最优输入设备
     */
    String LOCAL_MDS = "mds";

    /**
     * 无效的 scope，invalid+时间戳 保证离线授权文件肯定没有这个 scope
     */
    String INVALID = "invalid_1577088834000";

    /**
     * 云端模块的鉴权由服务器来做，包括 cloudASR cloudTTS
     */
    String CLOUD_MODEL = null;

}
