package com.aispeech.export.listeners;

/**
 * 本地热词引擎回调
 *
 * @author hehr
 */
public interface AILocalHotWordsListener extends AIASRListener {
    /**
     * 开启ssl功能后,该接口才会回调
     *
     * @param index 人声定位结果
     */
    void onDoa(int index);
}
