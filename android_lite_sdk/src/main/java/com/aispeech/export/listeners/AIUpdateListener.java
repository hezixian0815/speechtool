package com.aispeech.export.listeners;

/**
 * @author hehr
 * 上传结果回调
 */
public interface AIUpdateListener {
    /**
     * 上传成功回调
     */
    void success();

    /**
     * 上传失败回调
     */
    void failed();
}
