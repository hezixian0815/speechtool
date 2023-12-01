package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;

public interface IListener {
    /**
     * 初始化完成后的回调函数,在主UI线程
     *
     * @param status {@link AIConstant#OPT_SUCCESS}:初始化成功；
     *               {@link AIConstant#OPT_FAILED}:初始化失败,
     *               不应该继续使用该AIEngine对象；
     */
    void onInit(int status);

    /**
     * 结果回调
     *
     * @param result 结果
     */
    void onResults(AIResult result);

    /**
     * 当错误发生时调用,在主UI线程
     *
     * @param error 错误信息类
     * @see AIError
     */
    void onError(AIError error);
}
