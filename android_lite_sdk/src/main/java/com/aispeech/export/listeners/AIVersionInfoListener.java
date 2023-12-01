package com.aispeech.export.listeners;

/**
 * 获取sdk版本+内核版本信息回调
 */
public interface AIVersionInfoListener {
    /**
     * 回调解惑
     * @param result
     */
    void onResult(String result);
}
