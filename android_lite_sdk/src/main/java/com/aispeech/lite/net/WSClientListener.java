package com.aispeech.lite.net;

/**
 * Created by yrl on 17-10-16.
 */

public interface WSClientListener {
    /**
     * 报文回调
     *
     * @param text
     */
    void onMessage(String text);

    /**
     * 错误回调
     *
     * @param text 错误信息
     */
    void onError(String text);

    /**
     * 链接创建成功
     */
    void onOpen();

    /**
     * 链接已关闭
     */
    void onClose();
}
