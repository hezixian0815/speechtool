package com.aispeech.lite.asrpp;

import com.aispeech.AIResult;
import com.aispeech.lite.BaseListener;

/**
 * Created by wuwei on 2018/7/11.
 */

public interface AsrppKernelListener extends BaseListener {


    /**
     * 当有结果返回时调用
     *
     * @param result
     *                结果内容
     * @see AIResult
     */
    void onResults(AIResult result);
}
