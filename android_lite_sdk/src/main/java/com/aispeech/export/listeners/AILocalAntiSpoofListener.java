package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.JSONResultParser;

/**
 * Created by wuwei on 2018/7/12.
 */

public interface AILocalAntiSpoofListener {
    /**
     * 引擎初始化结束后执行，在主UI线程
     *
     * @param status
     *                {@link AIConstant#OPT_SUCCESS}:初始化成功；
     *                {@link AIConstant#OPT_FAILED}:初始化失败,
     */
    void onInit(int status);

    /**
     * 发生错误时执行，在主UI线程
     *
     * @param error
     *                错误信息
     */
    void onError(AIError error);

    /**
     * 收到结果时执行，请使用{@link JSONResultParser}解析，在主UI线程
     *
     * @param result 结果
     */
    void onResults(AIResult result);

}
