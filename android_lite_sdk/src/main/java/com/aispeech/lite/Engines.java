package com.aispeech.lite;

/**
 * Description: 引擎标识类，按位标识，使用int方便配置多模块，& 和 | 操作简单
 * Author: junlong.huang
 * CreateTime: 2022/8/22
 */
public class Engines {

    public static final int VAD = 0x01;

    public static final int ECHO = 0x01 << 1;

    public static final int LOCAL_ASR = 0x01 << 2;

    public static final int CLOUD_ASR = 0x01 << 3;

    public static final int CLOUD_TTS = 0x01 << 4;

    public static final int CLOUD_SEMANTIC = 0x01 << 5;

    public static final int VPRINT = 0x01 << 6;

    public static final int FESP = 0x01 << 7;

    public static final int DMASP = 0x01 << 8;

    public static final int WAKEUP = 0x01 << 9; // 512  第10位，2^9

    public static final int DDS_DMS = 0x01 << 10;

    // 11~15位 预留给Vad场景 见VadScenes

    // 20 - 25位 预留给文件保存 见FileSaveScenes

    public static final int HOT_WORDS = 0x01 << 26;

    /**
     * 判断改引擎是否开启了音频保存  {@link Engines}
     *
     * @param engine
     */
    public static boolean isSavingEngineAudioEnable(int engine) {
        return AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE && (AISpeechSDK.GLOBAL_AUDIO_SAVE_ENGINES.get() & engine) > 0;
    }
}
