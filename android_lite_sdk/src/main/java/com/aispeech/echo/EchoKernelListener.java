package com.aispeech.echo;

import com.aispeech.lite.BaseListener;

/**
 * Created by wuwei on 18-6-19.
 */

public abstract class EchoKernelListener implements BaseListener {
    /**
     * aec之后的音频回调 在SDK子线程
     *
     * @param data pcm数据
     */
    public void onResultBufferReceived(byte[] data) {

    }

    /**
     * echo 内核的 VOIP 回调
     *
     * @param data pcm数据
     */
    public void onAgcDataReceived(byte[] data) {
        onVoipBufferReceived(data);
    }

    /**
     * 开启viop配置后音频回调
     *
     * @param data pcm数据
     * @deprecated
     */
    public void onVoipBufferReceived(byte[] data) {

    }

}
