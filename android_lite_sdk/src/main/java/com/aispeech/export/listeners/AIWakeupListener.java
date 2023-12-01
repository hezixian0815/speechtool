package com.aispeech.export.listeners;

import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.speech.EngineListener;

/**
 * Created by nemo on 17-12-15.
 */

public interface AIWakeupListener extends EngineListener {

    /**
     * 一次唤醒检测完毕后执行，在主UI线程
     *
     * @param recordId   recordId
     * @param confidence 唤醒置信度
     * @param wakeupWord 唤醒词, 如唤醒失败，则返回null
     */
    void onWakeup(String recordId, double confidence, String wakeupWord);

    /**
     * 低阈值唤醒，低阈值时会回调。低阈值会先于onWakeup回调，但onWakeup不一定会回调
     *
     * @param recordId   recordId
     * @param confidence 唤醒置信度
     * @param wakeupWord 唤醒词, 如唤醒失败，则返回null
     */
    void onPreWakeup(String recordId, double confidence, String wakeupWord);


    /**
     * 音频裁剪用于声纹的音频或者字符串
     *
     * @param dataType 数据类型
     * @param data     数据
     * @param size     数据大小
     */
    void onVprintCutDataReceived(int dataType, byte[] data, int size);


    /**
     * 经过信号出路模块处理后的音频数据返回，1声道pcm数据
     *
     * @param buffer 数据
     * @param size   数据大小
     */
    void onResultDataReceived(byte[] buffer, int size);


    /**
     * 唤醒点前特定时间长度的音频
     *
     * @param wkpData 音频信息
     * @param length  音频长度
     */
    void onRawWakeupDataReceived(byte[] wkpData, int length);

    /**
     * oneshot 回调
     *
     * @param word   oneshot 唤醒词
     * @param buffer 回溯音频
     */
    void onOneshot(String word, OneshotCache<byte[]> buffer);

    /**
     * 非oneshot 回调
     *
     * @param word oneshot 唤醒词
     */
    void onNotOneshot(String word);
}
