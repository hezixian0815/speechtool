package com.aispeech.lite;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;

public interface BaseListener {
    /**
     * 本地唤醒引擎初始化结束后执行，在主UI线程
     *
     * @param status {@link AIConstant#OPT_SUCCESS}:初始化成功；
     *               {@link AIConstant#OPT_FAILED}:初始化失败,
     */
    void onInit(int status);

    /**
     * 发生错误时执行，在主UI线程
     *
     * @param error 错误信息
     */
    void onError(AIError error);
}
