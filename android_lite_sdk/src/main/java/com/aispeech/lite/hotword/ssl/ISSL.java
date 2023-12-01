package com.aispeech.lite.hotword.ssl;

import com.aispeech.lite.config.SSLConfig;
import com.aispeech.lite.param.SpeechParams;

/**
 * SSL means sound source locate
 *
 * @author hehr
 */
public interface ISSL {

    /**
     * 初始化
     *
     * @param listener 回调
     * @param config   初始化参数
     */
    void init(SSLConfig config, SSLListener listener);

    /**
     * 启动
     *
     * @param params 启动参数
     */
    void start(SpeechParams params);

    /**
     * 停止
     */
    void stop();

    /**
     * feed 4 通道音频
     *
     * @param bytes 4通道音频
     */
    void feed(byte[] bytes);

    /**
     * 引擎销毁
     */
    void release();
}
