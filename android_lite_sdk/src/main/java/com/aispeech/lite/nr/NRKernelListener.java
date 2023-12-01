package com.aispeech.lite.nr;

import com.aispeech.lite.speech.EngineListener;

public interface NRKernelListener extends EngineListener {


    /**
     * nr之后返回的数据
     * @param data 数据
     * @param size 数据大小
     */
    void onBufferReceived(byte[] data, int size);
}
