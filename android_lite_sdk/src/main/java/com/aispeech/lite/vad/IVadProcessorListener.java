package com.aispeech.lite.vad;


import com.aispeech.lite.speech.SpeechListener;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/3/27
 */
public abstract class IVadProcessorListener extends SpeechListener {

    /**
     * vad start 标志，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     */
    public abstract void onVadStart(String recordid);

    /**
     * vad end 标志，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     */
    public abstract void onVadEnd(String recordid);

    /**
     * 音量大小检查，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     *
     * @param rmsDb rmsDb
     */
    public abstract void onRmsChanged(float rmsDb);
    /**
     * vad抛出的状态以及其他的结果信息，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     * @param result 返回的vad状态信息
     */
    public abstract void onResults(String result);

    /**
     * 经过vad模块处理后的pcm音频，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     *
     * @param data 音频数据
     */
    public abstract void onBufferReceived(byte[] data);
}
