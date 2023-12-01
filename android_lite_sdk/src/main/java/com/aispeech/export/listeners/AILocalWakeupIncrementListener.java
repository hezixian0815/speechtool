package com.aispeech.export.listeners;

/**
 * 本地热词引擎回调
 *
 * @author hehr
 */
public interface AILocalWakeupIncrementListener extends AILocalHotWordsListener {

    /***
     * 动态跟新场景
     * @param status 状态
     */
    void onSetScene(int status);

    void onGramResults(String result);


}
