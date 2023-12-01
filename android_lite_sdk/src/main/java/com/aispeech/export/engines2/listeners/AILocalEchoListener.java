package com.aispeech.export.engines2.listeners;

import com.aispeech.lite.speech.EngineListener;

/**
 * @Description:
 * @Author: junlong.huang
 * @CreateTime: 2022/8/15
 */
public interface AILocalEchoListener extends EngineListener {

    /**
     * aec之后的音频回调 在SDK子线程
     *
     * @param data pcm数据
     */
    void onResultBufferReceived(byte[] data);

    /**
     * 开启viop配置后音频回调
     *
     * @param data pcm数据
     */
    void onVoipBufferReceived(byte[] data);

}
