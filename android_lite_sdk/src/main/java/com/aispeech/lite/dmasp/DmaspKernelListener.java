package com.aispeech.lite.dmasp;

import com.aispeech.AIResult;
import com.aispeech.lite.BaseListener;

/**
 * Created by wuwei on 18-6-19.
 */

public interface DmaspKernelListener extends BaseListener {

    /**
     * dmasp处理之后的音频回调 在SDK子线程
     *
     * @param data pcm数据
     */
    void onResultBufferReceived(byte[] data);

    /**
     * dmasp处理之后的音频回调 在SDK子线程
     *
     * @param doa  doa信息
     * @param data pcm数据
     */
    void onResultBufferReceived(int doa, byte[] data);


    /**
     * nwakeup唤醒的回调
     *
     * @param result 回调结果
     */
    void onResults(AIResult result);

    /**
     * 唤醒的doa角度
     *
     * @param doa doa
     */
    void onDoa(int ssl, int doa);

    /**
     * 音频裁剪用于声纹的音频或者字符串
     *
     * @param dataType 数据类型
     * @param data     数据
     * @param size     数据大小
     */
    void onVprintCutDataReceived(int dataType, byte[] data, int size);
}
