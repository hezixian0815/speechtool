package com.aispeech.lite.hotword.ssl;

import com.aispeech.AIError;

/**
 * doa selector 回调
 *
 * @author hehr
 */
public interface SSLListener {

    /**
     * 初始化结果
     *
     * @param status 0 success ,-1 failed
     */
    void init(int status);

    /**
     * vad模块内部错误信息，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     *
     * @param error 错误信息
     */
    void onError(AIError error);

    /**
     * 经过vad模块处理后的pcm音频
     *
     * @param data 音频数据
     */
    void onBufferReceived(byte[] data);

    /**
     * 首帧vad begin 标识
     */
    void onStart(String recordID);

    /**
     * ssl 定位数据
     *
     * @param index 0-3
     */
    void onSsl(int index);

    /**
     * vad end 标志，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     */
    void onEnd(String recordID);
}
