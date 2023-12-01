package com.aispeech.lite.vad;

import com.aispeech.lite.speech.EngineListener;

/**
 * Created by yuruilong on 2017/5/16.
 */

public interface VadKernelListener extends EngineListener {

    /**
     * vad start 标志，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     */
    void onVadStart(String recordid);

    /**
     * vad end 标志，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     */
    void onVadEnd(String recordid);

    /**
     * 音量大小检查，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     * @param rmsDb rmsDb
     */
    void onRmsChanged(float rmsDb);

    /**
     * 经过vad模块处理后的pcm音频，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     * @param data 音频数据
     */
    void onBufferReceived(byte[] data);

    /**
     * vad抛出的状态以及其他的结果信息，在内部子线程，请勿在该回调做阻塞操作，否则会导致线性卡死
     * @param result 返回的vad状态信息
     */
    void onResults(String result);
}
