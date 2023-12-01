package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;

/**
 * 音频文件识别的回调，1-5对应音频识别的过程
 */
public interface AILASRListener {

    /**
     * 0. 初始化
     *
     * @param status 初始化状态，{@link AIConstant#OPT_SUCCESS}:初始化成功；{@link AIConstant#OPT_FAILED}:初始化失败,
     * @param errMsg 错误描述, 失败时才有
     */
    void onInit(int status, String errMsg);

    /**
     * 1. 上传文件的进度
     *
     * @param audioFilePath 上传文件的本地路径
     * @param process       0-100
     */
    void onUploadFileProcess(String audioFilePath, int process);

    /**
     * 2.成功上传文件，回调 audioId
     *
     * @param audioFilePath 上传文件的本地路径
     * @param audioId       文件唯一表示
     * @param error         错误信息，没有错误时为 null
     */
    void onUploadFileResult(String audioFilePath, String audioId, AIError error);

    /**
     * 3. 成功创建识别任务，得到 taskId
     *
     * @param audioIdOrUri 上传本地文件成功后得到的 audioId 或者 http开头的url
     * @param taskId       taskId
     * @param error        错误信息，没有错误时为 null
     */
    void onTaskCreate(String audioIdOrUri, String taskId, AIError error);

    /**
     * 4. 查询服务器识别进度
     *
     * @param taskId  创建识别任务成功后得到的taskId
     * @param process 服务器处理进度
     * @param error   错误信息，没有错误时为 null
     */
    void onTaskProcess(String taskId, int process, AIError error);

    /**
     * 5. 服务器识别结束后，可以查询结果
     *
     * @param taskId  创建识别任务成功后得到的taskId
     * @param results 识别结果
     * @param error   错误信息，没有错误时为 null
     */
    void onTaskResult(String taskId, String results, AIError error);

}
