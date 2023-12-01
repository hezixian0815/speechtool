package com.aispeech.export.listeners;

import com.aispeech.AIError;

/**
 * Created by yu on 2018/5/4.
 */

public interface AILocalTTSListener {
    /**
     * 初始化的回调，在主UI线程
     * @param status 初始化状态
     */
    void onInit(int status);

    /**
     * 错误回调 在主UI线程
     * @param utteranceId utteranceId
     * @param error 错误信息
     */
    void onError(String utteranceId, AIError error);

    /**
     * 合成开始的回调,在子线程，若需要更新UI控件需要做线程转换
     * @param utteranceId utteranceId
     */
    void onSynthesizeStart (String utteranceId);

    /**
     * 合成数据 ,在子线程，若需要更新UI控件需要做线程转换
     * @param utteranceId utteranceId
     * @param audioData 合成的音频数据
     */
    void onSynthesizeDataArrived(String utteranceId, byte[] audioData);

    /**
     * 合成完成的回调 ,在子线程，若需要更新UI控件需要做线程转换
     * @param utteranceId utteranceId
     */
    void onSynthesizeFinish (String utteranceId);

    /**
     * 开始播放的回调 在主UI线程
     * @param utteranceId utteranceId
     */
    void onSpeechStart (String utteranceId) ;

    /**
     * 播放的进度  在主UI线程
     * @param currentTime
     *            当前播放的时间进度
     * @param totalTime
     *            总共的播放时间
     * @param isRefTextTTSFinished
     *            数据是否已经缓冲完毕
     */
    void onSpeechProgress(int currentTime, int totalTime, boolean isRefTextTTSFinished);

    /**
     * 播放结束的回调 在主UI线程
     * @param utteranceId utteranceId
     */
    void onSpeechFinish (String utteranceId);


    /**
          * 用户设置音子之后会在这个接口回调
          * @param timeStampJson  返回的口型字节数组
          */
    void onTimestampReceived(byte[] timeStampJson,int size);

}
