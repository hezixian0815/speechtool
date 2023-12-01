package com.aispeech.lite.base;

import com.aispeech.export.bean.VoiceQueueStrategy;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/8/1
 */
public interface IVoiceRestrictive {

    void setVoiceStrategy(VoiceQueueStrategy voiceStrategy);

    VoiceQueueStrategy getMaxVoiceQueueSize();

}
