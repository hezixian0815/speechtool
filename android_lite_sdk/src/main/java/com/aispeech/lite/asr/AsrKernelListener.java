package com.aispeech.lite.asr;

import com.aispeech.AIResult;
import com.aispeech.lite.BaseListener;

/**
 * Created by yuruilong on 2017/5/19.
 */

public interface AsrKernelListener extends BaseListener {

    /**
     * 当有结果返回时调用
     *
     * @param result 结果内容
     * @see AIResult
     */
    void onResults(AIResult result);

    /**
     * 启动成功后会调用
     *
     * @param recordId recordId
     */
    void onStarted(String recordId);

    /**
     * 更新结果回调
     *
     * @param ret 0 成功 -1 失败
     */
    void onUpdateResult(int ret);
}
