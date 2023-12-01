package com.aispeech.export.listeners;

import com.aispeech.lite.speech.EngineListener;

/**
 * Created by nemo on 17-12-15.
 */

public interface AIDmaspListener extends EngineListener {

    /**
     * 一次唤醒检测完毕后执行，在主UI线程
     *
     * @param recordId   recordId
     * @param confidence 唤醒置信度
     * @param wakeupWord 唤醒词, 如唤醒失败，则返回null
     */
    void onWakeup(String recordId, double confidence, String wakeupWord);

    /**
     * 经过信号处理模块处理后的音频数据返回，1声道pcm数据
     *
     * @param buffer 数据
     * @param size   数据大小
     */
    void onResultDataReceived(byte[] buffer, int size);

    /**
     * 音频裁剪用于声纹的音频或者字符串
     *
     * @param dataType 数据类型
     * @param data     数据
     * @param size     数据大小
     */
    void onVprintCutDataReceived(int dataType, byte[] data, int size);

    /**
     * 返回唤醒角度
     *
     * @param ssl 　ssl
     * @param doa doa
     */
    void onDoaResult(int ssl, int doa);


}
