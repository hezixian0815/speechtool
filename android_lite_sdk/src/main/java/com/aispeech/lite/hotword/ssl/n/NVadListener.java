package com.aispeech.lite.hotword.ssl.n;

import com.aispeech.AIError;

/**
 * @author hehr
 */
public interface NVadListener {
    /**
     * 初始化回调
     *
     * @param status 初始化状态
     */
    void onInit(int status);

    /**
     * vad begin
     *
     * @param index 通道数据
     */
    void onBegin(int index, String recordid);

    void onEnd(String recordid);

    /**
     * 经过vad模块处理后的pcm音频，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     *
     * @param data 音频数据
     */
    void onBufferReceived(byte[] data);

    /**
     * vad模块内部错误信息，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     *
     * @param error 错误信息
     */
    void onError(AIError error);
}
