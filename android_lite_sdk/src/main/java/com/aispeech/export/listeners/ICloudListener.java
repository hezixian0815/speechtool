package com.aispeech.export.listeners;

public interface ICloudListener {
    /**
     * 连接状态回调
     *
     * @param isConnected 是否连接
     */
    void onConnect(boolean isConnected);
}
