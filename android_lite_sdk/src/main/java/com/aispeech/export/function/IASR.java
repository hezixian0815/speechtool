package com.aispeech.export.function;

import com.aispeech.export.Vocab;
import com.aispeech.export.config.AILocalASRConfig;
import com.aispeech.export.engines2.bean.Decoder;
import com.aispeech.export.intent.AILocalASRIntent;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AIUpdateListener;

public interface IASR extends IEngine<AILocalASRConfig, AILocalASRIntent, AIASRListener> {

    /**
     * feed 音频
     *
     * @param data pcm数据
     */
    void feed(byte[] data);

    /**
     * 停止引擎并取消当前结果上抛，区别与stop
     */
    void cancel();

    /**
     * 更新词库
     *
     * @param vocab          词库内容
     * @param updateListener 更新结果回调
     */
    void updateVocab(Vocab vocab, AIUpdateListener updateListener);

    /**
     * 批量更新更新词库
     *
     * @param vocabs         词库内容
     * @param updateListener 更新结果回调
     */
    void updateVocabs(AIUpdateListener updateListener, Vocab... vocabs);

    /**
     * 更新解码网络
     *
     * @param updateListener 更新结果回调
     * @param decoders       解码网络
     */
    void updateDecoder(AIUpdateListener updateListener, Decoder... decoders);

}
