package com.aispeech.lite.dm.dispather;

import com.aispeech.export.Command;
import com.aispeech.export.NativeApi;
import com.aispeech.export.Speaker;
import com.aispeech.export.widget.callback.CallbackWidget;
import com.aispeech.lite.dm.Error;

/**
 * @author hehr
 * Dispatcher 回调监听器
 */
public interface DispatcherListener {

//    /**
//     * recorderId 回调
//     *
//     * @param recorderId recorder id
//     */
//    void onRecorderId(String recorderId);

    /**
     * 识别结果回调
     *
     * @param asr 识别文本
     * @param eof eof状态标识
     */
    void onAsr(int eof, String asr);

    /**
     * sessionId 回调
     *
     * @param id sessionId
     */
    void onSessionId(String id);

    /**
     * 对话控件展示回调
     *
     * @param callbackWidget {@link CallbackWidget}
     */
    void onWidget(CallbackWidget callbackWidget);

    /**
     * NLG 播报文本回调
     *
     * @param speaker {@link Speaker}
     */
    void onSpeak(Speaker speaker);

    /**
     * 对话命令回调
     *
     * @param command {@link Command}
     */
    void onCommand(Command command);

    /**
     * native api 回调
     *
     * @param api {@link NativeApi}
     */
    void onNativeApi(NativeApi api);

    /**
     * waite 回调,需要等待前序对话动作执行完毕
     */
    void onWait();

    /**
     * close 回调
     */
    void onClose();

    /**
     * error 回调
     *
     * @param error {@link Error}
     */
    void onError(Error error);

    /**
     * 重新进入倾听 回调
     */
    void onListen();


}
