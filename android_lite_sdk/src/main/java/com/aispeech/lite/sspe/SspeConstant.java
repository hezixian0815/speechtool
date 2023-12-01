package com.aispeech.lite.sspe;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/1/30
 */
public interface SspeConstant {

    //    0-asr0  , 1-asr1 , 2-vad0 , 3-vad1 , 4-asr2 , 5-asr3
    int AUDIO_TYPE_ASR0 = 0;
    int AUDIO_TYPE_ASR1 = 1;
    int AUDIO_TYPE_VAD0 = 2;
    int AUDIO_TYPE_VAD1 = 3;
    int AUDIO_TYPE_ASR2 = 4;
    int AUDIO_TYPE_ASR3 = 5;

    int SSPE_TYPE_CAR_TWO = 0x1001;
    int SSPE_TYPE_CAR_FOUR = 0x1002;
}
