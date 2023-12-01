package com.aispeech.export.listeners;

/**
 * AITTSListener 接口用以接收 AILocalTTSEngine 和 AICloudTTSEngine 中发生的事件。
 * 关注和需要处理相关事件的类须实现该接口，当相关事件发生时，有关方法将会被回调。 所有这些回调方法的触发都是在UI线程中执行的，请不要执行任何阻塞操作。
 */
public interface AIEmotionTTSListener extends AITTSListener {

    /**
     * 合成文本情感标签回调
     *
     * @param emotion       情感标签
     * @param emotionOrigin 情感类型描述（中文）
     * @param utteranceId   本次合成对应的唯一标识
     */
    void onEmotion(String emotion, String emotionOrigin, String utteranceId);
}
