package com.aispeech.lite.mds;

import com.aispeech.AIResult;
import com.aispeech.lite.BaseListener;

public interface MdsKernelListener extends BaseListener {

    /**
     * 当有结果返回时调用
     *
     * @param result
     *                结果内容
     * @see AIResult
     */
    void onResults(AIResult result);
}
