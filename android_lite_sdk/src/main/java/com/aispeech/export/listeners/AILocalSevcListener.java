package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;

public interface AILocalSevcListener {

    /**
     * 引擎初始化结束后执行　主UI线程
     *
     * @param status {@link AIConstant#OPT_SUCCESS}:初始化成功；
     *               {@link AIConstant#OPT_FAILED}:初始化失败,
     */
    void onInit(int status);

    /**
     * 发生错误时执行　主UI线程
     *
     * @param error 错误信息
     */
    void onError(AIError error);

    /**
     * 送agc模块后的音频 非主线程
     *
     * @param data 数据
     */
    void onAgcDataReceived(byte[] data);

    /**
     * 算法内核的原始输入音频
     *
     * @param data     数据
     * @param size     数据大小
     */
    void onInputDataReceived(byte[] data, int size);

    /**
     * 输出信号处理估计噪声最大的beam index 信息和该方向的音量信息，为 json 字符串
     * {@code
     * {"chans": 0,"db":56.625889}
     * }
     *
     * @param retString 返回信息
     */
    void onSevcNoiseResult(String retString);

    /**
     * 输出信号处理后语音通信的beam index信息
     * @param doa beam信息
     */
    void onSevcDoaResult(int doa);
}
