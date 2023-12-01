package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.speech.EngineListener;

/**
 * Created by wanbing on 2021/9/6 10:52
 * AIFespxCarListener 接口用以接收 AIFespxCarEngine中发生的事件。
 * 关注和需要处理相关事件的类须实现该接口，当相关事件发生时，有关方法将会被回调。 所有这些回调方法的触发都是在UI线程中执行的，请不要执行任何阻塞操作。
 */
public interface AIFespCarListener extends EngineListener {

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
     * @param recordId
     * @param confidence 唤醒置信度
     * @param wakeupWord 唤醒词
     */
    void onWakeup(String recordId, double confidence, String wakeupWord);

    /**
     * 被外部设置拦截的唤醒事件，详情可参考各驾驶模式逻辑
     *
     * @param doa        唤醒doa
     * @param confidence 唤醒置信度
     * @param wakeupWord 唤醒词, 如唤醒失败，则返回null
     */
    void onInterceptWakeup(int doa, double confidence, String wakeupWord);

    /**
     * 半字唤醒
     *
     * @param confidence 唤醒置信度
     * @param wakeupWord 唤醒词
     */
    void onSubWordWakeup(double confidence, String wakeupWord);

    /**
     * 返回唤醒角度
     *
     * @param doa 　唤醒角度
     */
    void onDoaResult(int doa);

    /**
     * 返回角度信息
     *
     * @param doa  角度
     * @param type 角度类型 ,type = 0 主动获取doa ; type =1 唤醒主动抛出
     */
    void onDoaResult(int doa, int type);

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
     * 经过信号处理模块处理后的音频数据返回，asr数据 vad数据
     *
     * @param vad vad数据
     * @param asr asr数据
     */
    void onResultDataReceived(byte[] vad, byte[] asr);

    /**
     * 经过信号出路模块处理后的音频数据返回
     *
     * @param buffer         主驾-vad数据流
     * @param isUseDoubleVad 是否使用双VAD
     */
    void onResultDataReceived(byte[] buffer, boolean isUseDoubleVad);

    /**
     * 音频裁剪用于声纹的音频或者字符串
     *
     * @param dataType 数据类型
     * @param data     数据
     * @param size     数据大小
     */
    void onVprintCutDataReceived(int dataType, byte[] data, int size);


    /**
     * oneshot 回调
     *
     * @param word   oneshot 唤醒词
     * @param buffer 回溯音频
     */
    void onOneshot(String word, OneshotCache<byte[]> buffer);

    /**
     * 非oneshot 回调
     *
     * @param word oneshot 唤醒词
     */
    void onNotOneshot(String word);
}
