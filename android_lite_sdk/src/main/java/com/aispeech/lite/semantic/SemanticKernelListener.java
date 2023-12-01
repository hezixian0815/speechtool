package com.aispeech.lite.semantic;

import com.aispeech.AIResult;
import com.aispeech.lite.BaseListener;

public interface SemanticKernelListener extends BaseListener {


    /**
     * 语义处理结果
     * @param result 语义结果
     */
    void onResults(AIResult result);

    /**
     * 更新结果回调
     *
     * @param ret 0 成功 -1 失败
     */
    void onUpdateResult(int ret);
}
