package com.aispeech.lite.speech;

import com.aispeech.lite.BaseListener;


/**
 * 引擎对外回调集类
 */
public interface EngineListener extends BaseListener {

    /**
     * 录音机启动时调用，在主UI线程
     */
    void onReadyForSpeech();

    /**
     * 经过信号出路模块处理后的音频数据返回，1声道pcm数据，在SDK内部子线程返回
     *
     * @param buffer     数据
     * @param size       数据大小
     * @param wakeupType 唤醒类型　0:非唤醒状态;　1:主唤醒词被唤醒; 2副唤醒词被唤醒
     */
    void onResultDataReceived(byte[] buffer, int size, int wakeupType);

    /**
     * 录音机数据返回，在SDK内部子线程返回
     *
     * @param buffer 录音机数据
     * @param size   数据大小
     */
    void onRawDataReceived(byte[] buffer, int size);
}
