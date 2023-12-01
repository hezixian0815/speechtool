package com.aispeech.lite.tts;

import com.aispeech.lite.BaseListener;

/**
 * Created by yu on 2018/5/7.
 */

public interface TtsKernelListener extends BaseListener {
    void onBufferReceived(byte[] data, int size, int dataType);

    /**
     * 向外传递信息，目前有 url，即key为url
     *
     * @param key
     * @param object
     */
    void onMessage(String key, Object object);

    /**
     * 用户设置音子之后会在这个借口回调
     * @param timeStampJson
     * 返回是byte[] json
     */
    void onTimestampReceived(byte[]  timeStampJson,int size);

    void onStartFeedData();

    void onEmotion(String emotion, String emotionOrigin);

}
