package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.JSONResultParser;

public interface AILASRRealtimeListener {

    /**
     * 识别引擎初始化结束后执行，在主UI线程
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

    /**
     * 收到结果时执行，请使用{@link JSONResultParser}解析，在主UI线程
     *
     * <ul>
     * <li>errno = 7，start响应成功</li>
     * <li>errno = 8， 表示本次返回为识别中间var结果</li>
     * <li>errno = 0，表示本次返回为识别中间rec结果</li>
     * <li>errno = 9，表示为客户端发完空帧后的最后一个rec，客户端可以断开链接</li>
     * <li>errno=10，客户端发送的数据错误</li>
     * <li>errno=11，服务异常，比如live模块初始化失败，没有可用的计算进程，计算进程退出，计算进程计算超时</li>
     * </ul>
     * <p>
     * errno 除了 0，7，8 外，收到其余code后 WebSocket 会断开
     *
     * @param result 服务器返回的结果
     */
    void onResults(AIResult result);


    /**
     * 当语音引擎就绪，用户可以说话时调用，在主UI线程
     */
    void onReadyForSpeech();


    /**
     * 录音机数据返回，在SDK内部子线程返回
     *
     * @param buffer 录音机数据
     * @param size   数据大小
     */
    void onRawDataReceived(byte[] buffer, int size);

    /**
     * 经过信号出路模块处理后的音频数据返回，1声道pcm数据
     *
     * @param buffer 数据
     * @param size   数据大小
     */
    void onResultDataReceived(byte[] buffer, int size);

}
