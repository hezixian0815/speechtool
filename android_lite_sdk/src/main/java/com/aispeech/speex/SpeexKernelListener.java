package com.aispeech.speex;

import com.aispeech.lite.BaseListener;

public interface SpeexKernelListener extends BaseListener {

    /**
     * 经过speex模块处理后的ogg音频，在内部子线程，请勿在该回调做阻塞操作，否则会导致线程卡死
     * @param data 音频数据
     * @param size 数据大小
     */
    void onResultBufferReceived(byte[] data, int size);


}
