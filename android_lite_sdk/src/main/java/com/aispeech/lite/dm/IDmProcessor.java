package com.aispeech.lite.dm;

import com.aispeech.export.MultiModal;
import com.aispeech.export.SkillIntent;
import com.aispeech.export.widget.feedback.FeedbackWidget;
import com.aispeech.lite.config.CloudDMConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;

/**
 * dm processor 实现接口
 *
 * @author hehr
 */
public interface IDmProcessor {

    /**
     * 初始化
     *
     * @param listener  {@link SpeechListener}
     * @param config    {@link  CloudDMConfig}
     * @param vadConfig {@link LocalVadConfig}
     */
    void init(SpeechListener listener, CloudDMConfig config, LocalVadConfig vadConfig);

    /**
     * 启动对话
     *
     * @param asrParams {@link SpeechParams}
     * @param vadParams {@link VadParams}
     */
    void start(SpeechParams asrParams, VadParams vadParams);

    /**
     * 通过纯语义模式启动对话
     *
     * @param asrParams {@link SpeechParams}
     * @param vadParams {@link VadParams}
     */
    void startWithText(SpeechParams asrParams, VadParams vadParams);

    /**
     * 回复对话数据
     *
     * @param widget {@link FeedbackWidget}
     */
    void feedback(FeedbackWidget widget);

    /**
     * 终端回复对话结果（注意：该接口仅供私有云来调用）
     *
     * @param topic 回复主题
     * @param data  回复结果，为 JSON 字符串
     */
    void feedback2PRIVCloud(String topic, String data);

    /**
     * 主动触发意图
     *
     * @param intent    {@link SkillIntent}
     * @param asrParams {@link SpeechParams}
     * @param vadParams {@link VadParams}
     */
    void triggerIntent(SkillIntent intent, SpeechParams asrParams, VadParams vadParams);

    /**
     * 上传多模态数据
     *
     * @param multiModal {@link MultiModal}
     */
    void async(MultiModal multiModal);

    /**
     * 关闭对话
     */
    void close();


    /**
     * 停止录音，等待识别结果
     */
    void stop();

    void startRecording();

    /**
     * 自定义feed音频
     *
     * @param data 音频
     * @param size 长度
     */
    void feedData(byte[] data, int size);

    /**
     * nlg end notify
     */
    void notifyNlgEnd();

    /**
     * release
     */
    void release();

    /**
     * 返回当前连接状态
     * */
    boolean isConnected();

}
