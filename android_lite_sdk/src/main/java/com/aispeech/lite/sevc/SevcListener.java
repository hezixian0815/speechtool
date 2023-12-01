package com.aispeech.lite.sevc;

import com.aispeech.lite.BaseListener;

public interface SevcListener extends BaseListener {

    /**
     * 输送给算法内核的音频数据
     *
     * @param data 数据
     * @param size 数据大小
     */
    void onInputDataReceived(byte[] data, int size);

    /**
     * Sevc之后返回的数据
     *
     * @param data 数据
     */
    void onSevcBufferReceived(byte[] data);

    /**
     * 输出信号处理估计噪声最大的beam index 信息和该方向的音量信息，为 json 字符串
     * {@code
     * {"chans": 0,"db":56.625889}
     * }
     *
     * @param retString 返回信息
     */
    void onSevcNoiseResult(String retString);

    /**
     * 输出信号处理后语音通信的beam index信息
     * @param doa beam信息
     */
    void onSevcDoaResult(int doa);
}
