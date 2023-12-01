package com.aispeech.lite.dm.update;

/**
 * cinfo 服务监听器
 */
public interface CInfoListener {

    /**
     * 上传成功
     */
    void onUploadSuccess();

    /**
     * 上传失败
     */
    void onUploadFailed();

}
