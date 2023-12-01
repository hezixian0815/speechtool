package com.aispeech.lite.aecagc;

import com.aispeech.lite.BaseListener;

public interface AecAgcKernelListener extends BaseListener {

    /**
     * aec之后的音频回调 在SDK子线程
     *
     * @param data pcm数据
     */
    void onAecBufferReceived(byte[] data);

    /**
     * agc之后的1路音频回调 在SDK子线程
     *
     * @param data pcm数据
     */
    void onVoipBufferReceived(byte[] data);

}
