package com.aispeech.export.engines;

import com.aispeech.export.config.AILocalHotWordConfig;
import com.aispeech.export.intent.AILocalHotWordIntent;
import com.aispeech.export.listeners.AILocalHotWordsListener;
import com.aispeech.kernel.Asr;


/**
 * @deprecated 废弃，参考{@link com.aispeech.export.engines2.AILocalHotWordsEngine}
 */
@Deprecated
public class AILocalHotWordsEngine {

    private static final String TAG = "AILocalHotWordsEngine";

    private com.aispeech.export.engines2.AILocalHotWordsEngine hotWordsEngine;

    private AILocalHotWordsEngine() {
        hotWordsEngine = com.aispeech.export.engines2.AILocalHotWordsEngine.createInstance();
    }

    /**
     * 创建实例引擎
     *
     * @return AILocalHotWordsEngine
     */
    public static AILocalHotWordsEngine createInstance() {
        return new AILocalHotWordsEngine();
    }

    /**
     * 检查so是否加载成功
     *
     * @return boolean
     */
    public static boolean checkLibValid() {
        return Asr.isAsrSoValid();
    }

    /**
     * 传入数据,在不使用SDK内部录音机时调用
     *
     * @param data 音频数据流
     *             //     * @see #setUseCustomFeed(boolean)
     */
    public void feedData(byte[] data) {
        if (hotWordsEngine != null) {
            hotWordsEngine.feedData(data);
        }
    }


    /**
     * 初始化引擎
     *
     * @param config   {@link AILocalHotWordConfig}
     * @param listener {@link AILocalHotWordsListener}
     */
    public void init(AILocalHotWordConfig config, AILocalHotWordsListener listener) {
        if (hotWordsEngine != null) {
            hotWordsEngine.init(config, listener);
        }
    }

    /**
     * 启动本地热词引擎
     *
     * @param intent 启动参数 {@link AILocalHotWordIntent}
     */
    public void start(AILocalHotWordIntent intent) {
        if (hotWordsEngine != null) {
            hotWordsEngine.start(intent);
        }
    }


    /**
     * 停止热词引擎
     */
    public void cancel() {
        if (hotWordsEngine != null) {
            hotWordsEngine.cancel();
        }
    }

    /**
     * 识别结束等待识别解码结果
     *
     * @deprecated 不推荐外部直接调用，仅供外置vad方案使用。
     */
    @Deprecated
    public void stop() {
        if (hotWordsEngine != null) {
            hotWordsEngine.stop();
        }
    }

    /**
     * 销毁引擎
     */
    public void destroy() {
        if (hotWordsEngine != null) {
            hotWordsEngine.destroy();
        }
    }

}
