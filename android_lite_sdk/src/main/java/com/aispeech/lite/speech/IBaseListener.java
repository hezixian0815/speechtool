package com.aispeech.lite.speech;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;

/**
 * Description: 基础的初始化和错误接口
 * Author: junlong.huang
 * CreateTime: 2022/10/25
 */
public interface IBaseListener {

    /**
     * 初始化完成后的回调函数
     *
     * @param status {@link AIConstant#OPT_SUCCESS}:初始化成功；
     *               {@link AIConstant#OPT_FAILED}:初始化失败,
     *               不应该继续使用该AIEngine对象；
     */
    void onInit(int status);

    /**
     * 当错误发生时调用
     *
     * @param error 错误信息类
     * @see AIError
     */
    void onError(AIError error);

    /**
     * cancel 接口调用,引擎清除对外回调消息队列
     */
    void onCancel();
}
