package com.aispeech.lite.hotword.ssl;

import com.aispeech.lite.BaseListener;

public interface SSLKernelListener extends BaseListener {


    /**
     * vad begin 标志，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     */
    void onVadStart(String recordID);

    /**
     * vad end 标志，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     */
    void onVadEnd(String recordID);

    /**
     * 声源定位结果
     *
     * @param index 通信信息
     */
    void onSsl(int index);

    /**
     * 音量大小检查，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     *
     * @param rmsDb rmsDb
     */
    void onRmsChanged(float rmsDb);

    /**
     * 经过vad模块处理后的pcm音频，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     *
     * @param data 音频数据
     */
    void onBufferReceived(byte[] data);
}
