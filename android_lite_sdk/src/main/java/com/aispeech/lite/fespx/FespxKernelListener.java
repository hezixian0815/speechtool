package com.aispeech.lite.fespx;


import com.aispeech.AIResult;
import com.aispeech.lite.BaseListener;

public interface FespxKernelListener extends BaseListener {

    /**
     * /**
     * 经过信号出路模块处理后的音频数据返回，1声道pcm数据
     *
     * @param buffer      数据
     * @param size        数据大小
     * @param wakeup_type 　唤醒类型
     */
    void onResultDataReceived(byte[] buffer, int size, int wakeup_type);

    /**
     * 经过信号出路模块处理后的音频数据返回，1声道pcm数据
     *
     * @param bufferVad vad数据流
     * @param bufferAsr 识别数据
     */
    void onResultDataReceived(byte[] bufferVad, byte[] bufferAsr);

    /**
     * 经过信号出路模块处理后的音频数据返回
     *
     * @param buffer       数据
     * @param useDoubleVad 是否使用双VAD
     */
    void onResultDataReceived(byte[] buffer, boolean useDoubleVad);

    /**
     * 音频裁剪用于声纹的音频或者字符串
     *
     * @param dataType 数据类型
     * @param data     数据
     * @param size     数据大小
     */
    void onVprintCutDataReceived(int dataType, byte[] data, int size);

    /**
     * 输送给算法内核的音频数据
     *
     * @param data 数据
     * @param size 数据大小
     */
    void onInputDataReceived(byte[] data, int size);

    /**
     * 特殊资源输出的音频数据
     *
     * @param data 数据
     * @param size 数据大小
     */
    void onOutputDataReceived(byte[] data, int size);

    /**
     * 带回路的音频消除回路之后的数据
     *
     * @param data 数据
     * @param size 数据大小
     */
    void onEchoDataReceived(byte[] data, int size);

    /**
     * 唤醒信息
     *
     * @param aiResult 　唤醒信息
     */
    void onResult(AIResult aiResult);


    /**
     * 唤醒的doa角度
     *
     * @param doa doa
     */
    void onDoaResult(int doa);

    /**
     * 唤醒的doa角度
     *
     * @param doa  doa
     * @param type 角度类型
     */
    void onDoaResult(int doa, int type);

    void onInterceptWakeup(int doa, double confidence, String wakeupWord);

    void onNearInformation(String info);

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
    void onSevcDoaResult(Object doa);

    /**
     * 输出多路beam音频数据以及对应的通道index信息
     * @param index 通道标识
     * @param bufferData 返回的数据信息
     * @param size 数据大小
     */
    void onMultibfDataReceived(int index, byte[] bufferData, int size);

    /**
     * 输出经过回声消除的送给VoIP使用的音频数据
     * @param type 数据类型
     * @param bufferData 返回的数据信息
     * @param size 数据大小
     */
    void onEchoVoipDataReceived(int type, byte[] bufferData, int size);

    /**
     * 输出经过前端信号处理放大后的agc音频数据
     * @param bufferData 返回的数据信息
     * @param size 数据大小
     */
    void onAgcDataReceived(byte[] bufferData, int size);
}
