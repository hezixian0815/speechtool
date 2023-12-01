package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;

/**
 * AILocalWakeupListener 接口用以接收 AILocalSignalAndWakeupEngine 中发生的事件。
 * 关注和需要处理相关事件的类须实现该接口，当相关事件发生时，有关方法将会被回调。 所有这些回调方法的触发都是在UI线程中执行的，请不要执行任何阻塞操作。
 */
public interface AILocalSignalAndWakeupListener {
    /**
     * 本地信号处理和唤醒引擎初始化结束后执行　主UI线程
     *
     * @param status {@link AIConstant#OPT_SUCCESS}:初始化成功；
     *               {@link AIConstant#OPT_FAILED}:初始化失败,
     */
    void onInit(int status);


    /**
     * 发生错误时执行　主UI线程
     *
     * @param error 错误信息
     */
    void onError(AIError error);

    /**
     * 一次唤醒检测完毕后执行　主UI线程
     *
     * @param confidence 唤醒置信度
     * @param wakeupWord 唤醒词
     */
    void onWakeup(double confidence, String wakeupWord);

    /**
     * 一次唤醒检测完毕后执行　主UI线程
     *
     * @param json 内核吐出的唤醒信息
     */
    void onWakeup(String json);

    /**
     * 使用就近唤醒时，就近唤醒会回传一些中间信息
     *
     * @param json json的字符串信息
     */
    void onNearInformation(String json);

    /**
     * 返回唤醒角度
     *
     * @param doa 　唤醒角度
     */
    void onDoaResult(int doa);

    /**
     * 录音机启动时调用　主UI线程
     */
    void onReadyForSpeech();

    /**
     * 原始音频数据返回，多声道pcm数据　该回调在SDK内部子线程
     *
     * @param buffer 数据
     * @param size   数据大小
     */
    void onRawDataReceived(byte[] buffer, int size);

    /**
     * 经过信号处理模块处理后的音频数据返回，1声道pcm数据　该回调在SDK内部子线程
     *
     * @param buffer      数据
     * @param size        数据大小
     * @param wakeup_type 唤醒类型　0:非唤醒状态;　1:主唤醒词被唤醒; 2副唤醒词被唤醒
     */
    void onResultDataReceived(byte[] buffer, int size, int wakeup_type);


    /**
     * 音频裁剪用于声纹的音频或者字符串
     *
     * @param dataType 数据类型
     * @param data     数据
     * @param size     数据大小
     */
    void onVprintCutDataReceived(int dataType, byte[] data, int size);

    /**
     * 送agc模块后的音频
     *
     * @param buffer 数据
     * @param size   数据大小
     */
    void onAgcDataReceived(byte[] buffer, int size);

    /**
     * 算法内核的原始输入音频
     *
     * @param data     数据
     * @param size     数据大小
     */
    void onInputDataReceived(byte[] data, int size);

    /**
     * 特定资源下抛出的处理后的音频，区别于beamforming音频
     *
     * @param data     数据
     * @param size     数据大小
     */
    void onOutputDataReceived(byte[] data, int size);

    /**
     * 带回路的资源消除回路后的音频数据
     *
     * @param data     数据
     * @param size     数据大小
     */
    void onEchoDataReceived(byte[] data, int size);

    /**
     * 输出信号处理后语音通信的beam index信息
     * @param doa beam信息
     */
    void onSevcDoaResult(int doa);

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
     * 输出多路beam音频数据以及对应的通道index信息
     * @param data 返回的数据信息
     * @param length 数据大小
     * @param index 通道标识
     */
    void onMultibfDataReceived(byte[] data, int length, int index);

    /**
     * 输出经过回声消除的送给VoIP使用的音频数据
     * @param data 数据信息
     * @param length 数据长度
     */
    void onEchoVoipDataReceived(byte[] data, int length);
}
