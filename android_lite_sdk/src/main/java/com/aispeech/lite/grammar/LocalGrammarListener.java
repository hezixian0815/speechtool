package com.aispeech.lite.grammar;

import com.aispeech.AIResult;
import com.aispeech.lite.BaseListener;

public interface LocalGrammarListener extends BaseListener {


    /**
     * 语法构建结束后执行，在SDK子线程
     *
     * @param aiResult xbnf编译成功信息
     */
    void onBuildCompleted(AIResult aiResult);

}
