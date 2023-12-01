package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;

public interface AIASRSentenceListener {

    /**
     * 初始化
     *
     * @param status 初始化状态，{@link AIConstant#OPT_SUCCESS}:初始化成功；{@link AIConstant#OPT_FAILED}:初始化失败,
     * @param errMsg 错误描述, 失败时才有
     */
    void onInit(int status, String errMsg);

    /**
     * 上传文件的进度
     *
     * @param process 进度 0-100
     */
    void onProgress(int process);

    /**
     * 识别音频的回调
     *
     * @param audioFilePath 上传的音频
     * @param result        识别结果，error 为 null 才会有
     * @param error         错误信息
     */
    void onAsrSentenceResult(String audioFilePath, String result, AIError error);

}
