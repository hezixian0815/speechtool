/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.speech;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.export.Command;
import com.aispeech.export.NativeApi;
import com.aispeech.export.Speaker;
import com.aispeech.export.listeners.ICloudListener;
import com.aispeech.export.widget.callback.CallbackWidget;
import com.aispeech.export.widget.callback.CallbackWidgetType;
import com.aispeech.lite.oneshot.OneshotCache;

import java.util.Map;

/**
 * 思必驰语音引擎回调接口
 * 用来接收来自AISpeechEngine相关的事件通知
 * Usage:
 * <pre>
 *      AISpeechEngine engine = new AISpeechEngine(...);
 *      engine.setSpeechListener(new SpeechListenerImpl());          //SpeechListenerImpl是SpeechListener接口的实现
 *      ...
 *      engine.start(...);
 * </pre>
 * 改成 abstract class，暂不改类名,拆分各功能接口
 */
public abstract class SpeechListener implements IBaseListener, ICloudListener {

    @Override
    public void onInit(int status) {
    }

    @Override
    public void onError(AIError error) {

    }

    @Override
    public void onCancel() {

    }

    @Override
    public void onConnect(boolean isConnected) {

    }

    /**
     * 当有结果返回时调用
     *
     * @param result 结果内容
     * @see AIResult
     */
    public void onResults(AIResult result) {
    }


    /**
     * doa 输出唤醒角度
     *
     * @param doa 　输出唤醒角度
     */
    public void onDoaResult(int doa) {
    }

    /**
     * 返回唤醒角度
     *
     * @param sslDoa 　ssl and 唤醒角度
     */
    public void onDoaResult(String sslDoa) {

    }

    /**
     * 返回角度信息
     *
     * @param doa  角度
     * @param type 角度类型 ,type = 0 主动获取doa ; type =1 唤醒主动抛出
     */
    public void onDoaResult(int doa, int type) {
    }


    /**
     * 当语音引擎就绪，用户可以说话时调用
     */
    public void onReadyForSpeech() {
    }

    /**
     * 检测到用户开始说话
     */
    public void onBeginningOfSpeech() {
    }

    /**
     * 音频音量发生改变时调用
     *
     * @param rmsdB 音量标量 0-100
     */
    public void onRmsChanged(float rmsdB) {
    }

    /**
     * 语音引擎接收到音频数据时调用，使用该方法可以加入用户对音频数据的处理，不保证这个方法将调用。
     *
     * @param buffer big-endian 16-bit编码,单声道的音频数据缓冲区
     * @param size   数据大小
     */
    public void onRawDataReceived(byte[] buffer, int size) {
    }

    /**
     * 用户停止说话时调用
     */
    public void onEndOfSpeech() {
    }

    /**
     * 录音机停止时调用
     *
     * @deprecated
     */
    public void onRecorderStopped() {
    }


    /**
     * 唤醒引擎结束的回调接口，只会在唤醒后（onWakeup）500ms后被调用，
     * 只有这个接口被调用后，才能开启下一次的唤醒
     * 其他引擎的开启和此接口没关系
     */
//	void onWakeupEngineStopped(){}


    /**
     * 其他事件方法，用于扩展
     *
     * @param eventType 事件类型
     * @param params    具体参数内容
     */
    public void onEvent(int eventType, @SuppressWarnings("rawtypes") Map params) {
    }

    /**
     * 经过信号出路模块处理后的音频数据返回，1声道pcm数据
     *
     * @param buffer 数据
     */
    public void onResultDataReceived(byte[] buffer, int size, int wakeup_type) {
    }

    /**
     * 经过信号出路模块处理后的音频数据返回
     *
     * @param buffer       数据
     * @param useDoubleVad 是否使用双VAD
     */
    public void onResultDataReceived(byte[] buffer, boolean useDoubleVad) {
    }

    /**
     * 经过信号出路模块处理后的单路音频数据返回，1声道pcm数据
     *
     * @param buffer 数据
     * @param size   数据长度
     */
    public void onResultDataReceived(byte[] buffer, int size) {

    }

    /**
     * 经过信号出路模块处理后的音频数据返回，2声道数据
     *
     * @param vad 数据
     * @param asr 数据
     */
    public void onResultDataReceived(byte[] vad, byte[] asr) {
    }



    /**
     * 音频裁剪用于声纹的音频或者字符串
     *
     * @param dataType 数据类型
     * @param data     数据
     * @param size     数据大小
     */
    public void onVprintCutDataReceived(int dataType, byte[] data, int size) {
    }

    /**
     * 算法内核的原始输入音频
     *
     * @param data     数据
     * @param size     数据大小
     */
    public void onInputDataReceived(byte[] data, int size) {
    }

    /**
     * 特定资源下抛出的处理后的音频，区别于beamforming音频
     *
     * @param data     数据
     * @param size     数据大小
     */
    public void onOutputDataReceived(byte[] data, int size) {
    }



    /**
     * 送agc模块后的音频
     *
     * @param data 数据
     */
    public void onAgcDataReceived(byte[] data) {}


    /**
     * 使用就近唤醒时，就近唤醒会回传一些中间信息
     *
     * @param json json的字符串信息
     */
    public void onNearInformation(String json) {
    }

    public void onUpdateResult(int ret) {
    }

    /**
     * 唤醒点前特定时间长度的音频
     * @param wkpData 音频信息
     * @param length 音频长度
     */
    public void onRawWakeupDataReceived(byte[] wkpData, int length) {
    }

    /**
     * 输出信号处理估计噪声最大的beam index 信息和该方向的音量信息，为 json 字符串
     * {@code
     * {"chans": 0,"db":56.625889}
     * }
     *
     * @param retString 返回信息
     */
    public void onSevcNoiseResult(String retString) {
    }

    /**
     * 输出信号处理后语音通信的beam index信息
     * @param doa beam信息
     */
    public void onSevcDoaResult(int doa) {
    }

    /**
     * 经信号处理后的多路beam以及对应的index信息
     *
     * @param data   数据信息
     * @param length 数据长度
     * @param index  数据对应的通道信息
     */
    public void onMultibfDataReceived(byte[] data, int length, int index) {
    }

    /**
     * 带回路的资源消除回路后的音频数据
     *
     * @param data 数据
     * @param size 数据大小
     */
    public void onEchoDataReceived(byte[] data, int size) {
    }

    /**
     * 输出经过回声消除的送给VoIP使用的音频数据
     *
     * @param data   数据信息
     * @param length 数据长度
     */
    public void onEchoVoipDataReceived(byte[] data, int length) {

    }

    // CloudDmProcessorListener 相关接口 start

    /**
     * NativeApi 回调
     *
     * @param api {@link NativeApi}
     */
    public void onQuery(NativeApi api) {

    }

    /**
     * command 执行回调
     *
     * @param command {@link Command}
     */
    public void onCall(Command command) {

    }

    /**
     * 播报
     *
     * @param speaker {@link Speaker}
     */
    public void onPlay(Speaker speaker) {

    }

    /**
     * 展示内容
     *
     * @param type           控件类型,取值参见 {@link CallbackWidgetType}
     * @param callbackWidget {@link CallbackWidget }
     */
    public void onDisplay(int type, CallbackWidget callbackWidget) {
    }

    /**
     * 显示识别文本
     *
     * @param text   识别文本
     * @param isLast 是否最终识别结果
     */
    public void onAsr(boolean isLast, String text) {

    }

    /**
     * 对话结束
     *
     * @param sessionId 一轮对话表识
     */
    public void onEnd(String sessionId) {

    }

    /**
     * 更新产品、技能配置回调
     *
     * @param isSuccess 是否更新成功
     */
    public void onUpdateContext(boolean isSuccess) {

    }

    /**
     * 对话结果回调
     *
     * @param result 含 dm 的响应结果
     */
    public void onHasDmResult(String result) {

    }


    // OneShot相关接口 start

    /**
     * 在识别引擎开启oneshot功能时，来判断用户说的话不是连说（比如你好小乐来首歌）的回调
     * 没开启oneshot功能时，这个回调方法不会被调用
     * 从oneshotkernel回调
     */
    public void onNotOneShot(String word) {

    }

    /**
     * asr模块等回调
     */
    public void onNotOneShot() {

    }

    /**
     * 在识别引擎开启oneshot功能时，来判断用户说的话不是连说（比如你好小乐来首歌）的回调
     * 没开启oneshot功能时，这个回调方法不会被调用
     */
    public void onOneShot(String word, OneshotCache<byte[]> buffer) {

    }

//    /**
//     * oneshot 回调
//     *
//     * @param words  oneshot 唤醒词
//     * @param buffer 回溯音频
//     */
//    public void onOneshot(String words, OneshotCache<byte[]> buffer) {
//
//    }

    // wakeup

    /**
     * 一次唤醒检测完毕后执行，在主UI线程
     *
     * @param recordId   recordId
     * @param confidence 唤醒置信度
     * @param wakeupWord 唤醒词, 如唤醒失败，则返回null
     */
    public void onWakeup(String recordId, double confidence, String wakeupWord) {

    }

    /**
     * 被外部设置拦截的唤醒事件，详情可参考各驾驶模式逻辑
     *
     * @param doa        唤醒doa
     * @param confidence 唤醒置信度
     * @param wakeupWord 唤醒词, 如唤醒失败，则返回null
     */
    public void onInterceptWakeup(int doa, double confidence, String wakeupWord) {

    }

    /**
     * 低阈值唤醒，低阈值时会回调。低阈值会先于onWakeup回调，但onWakeup不一定会回调
     *
     * @param recordId   recordId
     * @param confidence 唤醒置信度
     * @param wakeupWord 唤醒词, 如唤醒失败，则返回null
     */
    public void onPreWakeup(String recordId, double confidence, String wakeupWord) {

    }


    // WakeupIncrement

    public void onGramResults(String path) {

    }

    /**
     * 人声定位信息
     *
     * @param index 通信信息
     */
    public void onSSL(int index) {
    }

    /***
     * update net.bin 动态跟新成功后调用
     * @param status 状态信息
     */
    public void onSetScene(int status) {

    }

    // grammer

    /**
     * 语法构建结束后执行，在主UI线程
     *
     * @param path 生成资源的绝对路径
     */
    public void onBuildCompleted(AIResult path) {

    }
}
