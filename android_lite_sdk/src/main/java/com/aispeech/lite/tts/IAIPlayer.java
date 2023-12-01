package com.aispeech.lite.tts;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.os.Build;

import com.aispeech.lite.audio.AIPlayerListener;


/**
 * 接口说明： 播放器接口
 *
 * @author Everett Li
 * @version 1.0
 * @date Nov 10, 2014
 */
public interface IAIPlayer {
    /**
     * 初始化
     */
    void init(final Context context, final int streamType, final int sampleRate);

    /**
     * 初始化
     */
    void init(final Context context, final AudioAttributes audioAttributes, final int sampleRate);

    /**
     * 播放
     */
    long play();

    /**
     * 停止
     */
    void stop();

    /**
     * 恢复
     */
    void resume();

    /**
     * 暂停
     */
    void pause();

    /**
     * 释放资源
     */
    void release();

    /**
     * 设置数据源
     *
     * @param queue
     */
    void setDataQueue(SynthesizedBlockQueue queue);

    /**
     * 设置播放器回调接口
     *
     * @param listener
     */
    void setPlayerListener(AIPlayerListener listener);

    /**
     * 设置音频播放流通道
     *
     * @param streamType
     */
    void setStreamType(int streamType);

    /**
     * 设置音频属性
     **/
    @TargetApi(Build.VERSION_CODES.M)
    void setAudioAttributes(AudioAttributes audioAttributes);

    /**
     * 设置音频采样率
     *
     * @param sampleRate
     */
    void setSampleRate(int sampleRate);

    /**
     * 设置音量及均衡
     *
     * @param volume 音量值 0～1.0之间
     * @param pan    平衡值 －1.0～1.0之间
     */
    void setupVolume(float volume, float pan);

    /**
     * 告知播放器数据已经准备好
     */
    void notifyDataIsReady(boolean isFinish);

}
