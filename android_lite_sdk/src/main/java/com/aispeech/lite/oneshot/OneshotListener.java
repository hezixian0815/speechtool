package com.aispeech.lite.oneshot;


import com.aispeech.AIError;

public interface OneshotListener {

    /**
     * 初始化回调 在SDK子线程
     *
     * @param status 初始化状态
     */
    void onInit(int status);

    /**
     * error回调信息 在SDK子线程
     *
     * @param error error回调信息
     */
    void onError(AIError error);

    /**
     * oneshot 回调
     *
     * @param words  oneshot 唤醒词
     * @param buffer 回溯音频
     */
    void onOneshot(String words, OneshotCache<byte[]> buffer);

    /**
     * 非oneshot 回调
     *
     * @param word oneshot 唤醒词
     */
    void onNotOneshot(String word);


}
