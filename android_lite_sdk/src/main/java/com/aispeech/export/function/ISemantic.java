package com.aispeech.export.function;

import com.aispeech.export.Vocab;
import com.aispeech.export.config.AILocalSemanticConfig;
import com.aispeech.export.intent.AILocalSemanticIntent;
import com.aispeech.export.listeners.AILocalSemanticListener;
import com.aispeech.export.listeners.AIUpdateListener;

public interface ISemantic extends IEngine<AILocalSemanticConfig, AILocalSemanticIntent, AILocalSemanticListener> {

    /**
     * 更新词库
     *
     * @param vocab          词库内容
     * @param updateListener 更新结果回调
     */
    void updateVocab(Vocab vocab, AIUpdateListener updateListener);

    /**
     * 批量更新单个或者多个词库
     *
     * @param vocabs         词库列表
     * @param updateListener 更新结果回调
     */
    void updateVocabs(AIUpdateListener updateListener, Vocab... vocabs);


}
