package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.JSONResultParser;


public interface AILocalAsrppListener {
    /**
     * 本地性别识别引擎初始化结束后执行，在主UI线程
     *
     * @param status
     *            {@link AIConstant#OPT_SUCCESS}:初始化成功；
     *            {@link AIConstant#OPT_FAILED}:初始化失败,
     */
    void onInit(int status);

    /**
     * 发生错误时执行，在主UI线程
     *
     * @param error
     *            错误信息
     */
    void onError(AIError error);


    /**
     * 录音机启动时调用，在主UI线程
     */
    void onReadyForSpeech();

    /**
     * 录音机数据返回，在SDK内部子线程返回
     * @param buffer 录音机数据
     * @param size  数据大小
     */
    void onRawDataReceived(byte[] buffer, int size);


    /**
     * 经过信号出路模块处理后的音频数据返回，1声道pcm数据，在SDK内部子线程返回
     * @param buffer 数据
     * @param size  数据大小
     */
    void onResultDataReceived(byte[] buffer, int size);

    /**
     * 收到结果时执行，请使用{@link JSONResultParser}解析，在主UI线程
     *
     * @param result 结果
     */
    void onResults(AIResult result);

    /**
     * 音频音量发生改变时调用，在主UI线程
     *
     * @param rmsdB
     *                音量标量 0-100
     */
    void onRmsChanged(float rmsdB);


    /**
     * 检测到用户开始说话，在主UI线程
     */
    void onBeginningOfSpeech();

    /**
     * 用户停止说话时调用，在主UI线程
     */
    void onEndOfSpeech();
}
