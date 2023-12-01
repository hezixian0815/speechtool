package com.aispeech.lite.hotword.ssl.n;

import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.SpeechParams;

/**
 * NVadEngine
 *
 * @author hehr
 */
public interface INVad {
    /**
     * 初始化
     *
     * @param config   vad 初始化参数
     * @param listener 回调
     */
    void init(LocalVadConfig config, NVadListener listener);

    /**
     * 启动
     *
     * @param params 启动参数
     */
    void start(SpeechParams params);

    /**
     * feed 音频,4通道音频
     *
     * @param chanel0 0通信音频数据
     * @param chanel1 1通信音频数据
     * @param chanel2 2通信音频数据
     * @param chanel3 3通信音频数据
     */
    void feed(byte[] chanel0, byte[] chanel1, byte[] chanel2, byte[] chanel3);

    /**
     * 通知引擎
     */
    void stop();

    /**
     * 释放引擎
     */
    void release();

    /**
     * 外部通知SSL定位结果，此时vad引擎需要开始外抛过了vad的音频
     *
     * @param index 音频通道
     */
    void notifySSL(int index);
}
