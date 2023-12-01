/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.audio;

import com.aispeech.AIError;

/**
 * 播放器监听接口
 */
public interface AIPlayerListener {

    /**
     * 注册回调方法，当要播放的音频数据已经就绪时调用
     */
    void onReady();

    /**
     * 注册回调方法，当播放暂停时回调
     */
    void onPaused();

    /**
     * 注册回调方法，当停止播放后回调
     */
    void onStopped();

    /**
     * 注册回调方法，当播放得以恢复时回调
     */
    void onResumed();

    /**
     * 注册回调方法，当播放完成时回调
     */
    void onCompletion(long sessionId);

    /**
     * 注册回调方法，当播放发生错误时回调
     *
     * @param error 错误描述
     */
    void onError(AIError error);

    /**
     * 播放进度
     *
     * @param currentTime          当前播放时间
     * @param totalTime            已经送入内核的文本合成的总时长
     * @param isRefTextTTSFinished 是否所有文本合成完成
     */
    void onProgress(int currentTime, int totalTime, boolean isRefTextTTSFinished);
}
