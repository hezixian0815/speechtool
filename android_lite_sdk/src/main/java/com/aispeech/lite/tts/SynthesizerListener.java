package com.aispeech.lite.tts;

import com.aispeech.AIError;

/**
 * 接口说明： 合成监听器
 * 
 */
public interface SynthesizerListener {
    /**
     * 注册回调方法，当合成引擎加载完毕后回调
     * 
     * @param status 初始化状态
     */
    void onInit(int status);

    /**
     * 注册回调方法，当开始播放时回调
     */
    void onSpeechStart();

    /**
     * 注册回调方法，当播放完成时回调
     */
    void onSpeechFinish();

    /**
     * 注册回调方法，当发生错误时回调
     * 
     * @param error 错误信息
     */
    void onError(AIError error);


    /**
     * 注册回调方法，当播放器播放进度变更时回调
     * 
     * @param currentTime
     *            当前播放的时间进度
     * @param totalTime
     *            总共的播放时间
     * @param isDataReady
     *            数据是否已经缓冲完毕
     */
    void onSpeechProgress(int currentTime, int totalTime, boolean isDataReady);
    
    /**
     * 注册回调方法，当外部调用stop时回调
     */
    //void onStopped();


    /**
     * 合成开始的回调
     */
    void onSynthesizeStart();

    /**
     * 合成数据
     * @param audioData 合成数据
     */
    void onSynthesizeDataArrived(byte[] audioData);

    /**
     * 合成完成的回调
     */
    void onSynthesizeFinish();


    /**
     * 用户设置音子之后会在这个借口回调
     * @param timeStampJson
     * @param size
     */
    void onTimestampReceived(byte[] timeStampJson,int size);
    /**
     * 音素数据返回
     */
    void onPhonemesDataArrived(String phonemes);

    void onEmotion(String emotion, String emotionOrigin);
}
