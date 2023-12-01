package com.aispeech.lite.wakeup;

import com.aispeech.AIResult;
import com.aispeech.lite.BaseListener;

/**
 * Created by yuruilong on 2017/5/16.
 */

public interface WakeupThreadListener extends BaseListener {

    /**
     * 当有结果返回时调用
     *
     * @param result
     *                结果内容
     * @see AIResult
     */
    void onResults(AIResult result);

    /**
     * 音频裁剪用于声纹的音频或者字符串
     * @param dataType 数据类型
     * @param data 数据
     * @param size 数据大小
     */
    void onVprintCutDataReceived(int dataType, byte[] data, int size);
}
