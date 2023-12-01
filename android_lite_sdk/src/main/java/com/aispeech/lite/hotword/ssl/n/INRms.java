package com.aispeech.lite.hotword.ssl.n;

public interface INRms {

    /**
     * 初始化
     *
     * @param frameNum 帧数
     */
    void init(int frameNum);

    /**
     * 启动
     */
    void start();

    /**
     * feed 音频
     *
     * @param chanel0 1通道音频
     * @param chanel1 2通道音频
     * @param chanel2 3通道音频
     * @param chanel3 4通道音频
     */
    void feed(byte[] chanel0, byte[] chanel1, byte[] chanel2, byte[] chanel3);

    /**
     * 停止
     */
    void stop();

    /**
     * 销毁
     */
    void release();

    /**
     * 获取当前音量最大值通道值
     *
     * @param index 通信信息
     * @return int  0 - 3
     */
    float optDms(int index);
}
