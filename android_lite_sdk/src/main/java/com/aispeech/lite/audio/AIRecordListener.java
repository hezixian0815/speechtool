/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.audio;

import com.aispeech.AIError;

/**
 * AIAudioRecord回调接口
 */
public interface AIRecordListener {

    /**
     * 注册录音回调方法，在{@link AIAudioRecorder}已经启动，但是尚未读取录音数据时调用;
     */
    void onRecordStarted(long sessionId);

    /**
     * 注册录音回调方法，在录音过程中，读取到部分音频数据时调用
     *
     * @param buffer
     * @param size
     */
    void onRawDataReceived(long sessionId,final byte[] buffer,final int size);


    /**
     * 经过信号处理引擎后的单路数据
     * @param buffer
     * @param size
     */
    void onResultDataReceived(byte[] buffer, int size);

    /**
     * 注册录音回调方法，在录音停止后调用
     */
    void onRecordStopped(long sessionId);
    
    /**
     * 注册录音回调方法，在录音机资源释放后调用
     */
    void onRecordReleased();

    /**
     * 注册录音回调方法，在异常发生时调用
     *
     * @param e AIError Exception
     */
    void onException(AIError e);
    
    String getTag();

    /**
     * 大音量状态
     * @param isHighPower 大音量状态
     */
    void onPowerChanged(boolean isHighPower);
}
