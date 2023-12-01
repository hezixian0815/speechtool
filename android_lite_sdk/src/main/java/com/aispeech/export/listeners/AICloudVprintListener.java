package com.aispeech.export.listeners;

import com.aispeech.AIError;

public interface AICloudVprintListener {

    /**
     * 初始化回调
     *
     * @param status 0 成功，其它 异常
     * @param errMsg 错误信息
     */
    void onInit(int status, String errMsg);

    /**
     * 注册回调
     *
     * @param state 状态码
     * @param json  服务器回调的消息
     */
    void onRegister(int state, String json);

    /**
     * Http 方式验证的回调
     *
     * @param state 状态码
     * @param json  服务器回调的消息
     */
    void onVerifyHttp(int state, String json);

    /**
     * WebSocket 方式验证的回调
     *
     * @param message 验证信息
     */
    void onVerifyWS(String message);

    /**
     * 注销声纹的回调
     *
     * @param state 状态码
     * @param json  服务器回调的消息
     */
    void onUnregister(int state, String json);

    /**
     * 返回云端声纹错误码
     *
     * @param aiError 错误码
     */
    void onError(AIError aiError);

    /**
     * 查询结果
     * @param result
     */
    void onQueryResult(String result);

    interface AudioToolListener {

        /**
         * 录音异常的回调
         *
         * @param state 状态码
         * @param err   错误信息
         */
        void onError(int state, String err);

        /**
         * 录音开始的回调
         */
        void onRecordStart();

        /**
         * 录音结束的回调
         */
        void onRecordStop();
    }
}
