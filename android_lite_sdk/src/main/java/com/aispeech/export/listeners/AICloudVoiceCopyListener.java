package com.aispeech.export.listeners;

import java.io.IOException;

/**
 * 云端声音复刻监听接口
 */
public interface AICloudVoiceCopyListener {


    /**
     * 初始化结果回调
     *
     * @param status 0 表示成功，1 表示失败
     * @param msg    详细描述
     */
    void onInit(int status, String msg);

    /**
     * 录音文本回调
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    void onRecordText(int state, String data, IOException e);

    /**
     * 上传音频回调
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    void onUpload(int state, String data, IOException e);

    /**
     * 提交训练回调
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    void onTraining(int state, String data, IOException e);

    /**
     * 查询任务状态，查询全部回调
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    void onQuery(int state, String data, IOException e);

    /**
     * 删除音色资源回调
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    void onDelete(int state, String data, IOException e);

    /**
     * 更新任务相关的自定义信息回调
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    void onCustomize(int state, String data, IOException e);
}
