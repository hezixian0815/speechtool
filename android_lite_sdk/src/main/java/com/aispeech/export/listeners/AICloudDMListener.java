package com.aispeech.export.listeners;

import com.aispeech.export.Command;
import com.aispeech.export.DmInfo;
import com.aispeech.export.NativeApi;
import com.aispeech.export.Speaker;
import com.aispeech.export.widget.callback.CallbackWidget;
import com.aispeech.export.widget.callback.CallbackWidgetType;
import com.aispeech.lite.speech.EngineListener;

/**
 * 云端对话回调监听
 *
 * @author hehr
 */
public interface AICloudDMListener extends EngineListener, ICloudListener {

    /**
     * NativeApi 回调
     *
     * @param api {@link NativeApi}
     */
    void onQuery(NativeApi api);

    /**
     * command 执行回调
     *
     * @param command {@link Command}
     */
    void onCall(Command command);

    /**
     * 播报
     *
     * @param speaker {@link Speaker}
     */
    void onPlay(Speaker speaker);

    /**
     * 展示内容
     *
     * @param type           控件类型,取值参见 {@link CallbackWidgetType}
     * @param callbackWidget {@link CallbackWidget }
     */
    void onDisplay(CallbackWidgetType type, CallbackWidget callbackWidget);

    /**
     * 显示识别文本
     *
     * @param text   识别文本
     * @param isLast 是否最终识别结果
     */
    void onAsr(boolean isLast, String text);

    /**
     * 对话结束
     *
     * @param sessionId 一轮对话表识
     */
    void onEnd(String sessionId);

    /**
     * 音频音量发生改变时调用，在主UI线程
     *
     * @param rmsdB 音量标量 0-100
     */
    void onRmsChanged(float rmsdB);


    /**
     * 检测到用户开始说话，在主UI线程
     */
    void onBeginningOfSpeech();

    /**
     * 用户停止说话时调用，在主UI线程
     */
    void onEndOfSpeech();

    /**
     * dm 结果回调
     *
     * @param dmInfo dm 信息
     */
    void onDmResult(DmInfo dmInfo);
}
