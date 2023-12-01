package com.aispeech.lite.net;

import com.aispeech.AIError;

/**
 * @auther wuwei
 */
public interface CloudTTSHttpClientListener {
    void onBufferReceived(byte[] data, int size, int dataType);
    void onError(AIError error);

    void onEmotion(String emotion, String emotionOrigin);
}
