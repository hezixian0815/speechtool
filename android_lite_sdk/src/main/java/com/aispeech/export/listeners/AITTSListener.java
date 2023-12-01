package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;

/**
 * AITTSListener 接口用以接收 AILocalTTSEngine 和 AICloudTTSEngine 中发生的事件。
 * 关注和需要处理相关事件的类须实现该接口，当相关事件发生时，有关方法将会被回调。 所有这些回调方法的触发都是在UI线程中执行的，请不要执行任何阻塞操作。
 */
public interface AITTSListener {

    /**
     * 合成引擎初始化结束后执行
     *
     * @param status
     *            {@link AIConstant#OPT_SUCCESS}:初始化成功；
     *            {@link AIConstant#OPT_FAILED}:初始化失败,
     */
    void onInit(int status);

    /**
     * 发生错误时执行
     * @param utteranceId 本次合成对应的ID
     *
     * @param error
     *            错误信息
     */
    void onError(String utteranceId, AIError error);

    /**
     * 数据准备就绪，可以播放时执行
     * @param utteranceId 本次合成对应的ID
     */
    void onReady(String utteranceId);

    /**
     * 播放完毕后执行
     * @param utteranceId 本次合成对应的ID
     */
    void onCompletion(String utteranceId);

    /**
     * 播放进度
     *
     * @param currentTime
     *            当前播放时间 (单位:100ms)
     * @param totalTime
     *            已经送入内核的文本合成的总时长 (单位:100ms)
     *            云端合成没有此项
     * @param isRefTextTTSFinished
     *            是否所有文本合成完成
     */
    void onProgress(int currentTime, int totalTime, boolean isRefTextTTSFinished);

    /**
     * 合成开始的回调,在子线程，若需要更新UI控件需要做线程转换
     *
     * @param utteranceId utteranceId
     */
    void onSynthesizeStart(String utteranceId);

    /**
     * 合成数据 ,在子线程，若需要更新UI控件需要做线程转换
     *
     * @param utteranceId utteranceId
     * @param audioData   合成的音频数据
     */
    void onSynthesizeDataArrived(String utteranceId, byte[] audioData);

    /**
     * 合成完成的回调 ,在子线程，若需要更新UI控件需要做线程转换
     *
     * @param utteranceId utteranceId
     */
    void onSynthesizeFinish(String utteranceId);



    /**
     * 用户设置音子之后会在这回调
     * @param timeStampJson
     */
    void onTimestampReceived(byte[] timeStampJson,int size);

    /**
     * 合成完成的音素信息回调
     * @param utteranceId utteranceId
     * @param phonemes 返回的音素信息，String类型
     */
    void onPhonemesDataArrived(String utteranceId, String phonemes);
}
