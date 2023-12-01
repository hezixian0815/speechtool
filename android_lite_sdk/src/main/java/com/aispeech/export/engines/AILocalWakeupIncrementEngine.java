package com.aispeech.export.engines;

import com.aispeech.export.config.AILocalWakeupIncrementConfig;
import com.aispeech.export.intent.AILocalWakeupIncrementIntent;
import com.aispeech.export.listeners.AILocalWakeupIncrementListener;
import com.aispeech.kernel.Asr;
import com.aispeech.kernel.Gram;

/**
 * @deprecated 废弃，参考{@link com.aispeech.export.engines2.AILocalWakeupIncrementEngine}
 */
@Deprecated
public class AILocalWakeupIncrementEngine {

    public static final String TAG = "AILocalWakeupIncrementEngine";

    com.aispeech.export.engines2.AILocalWakeupIncrementEngine mEngine;

    private AILocalWakeupIncrementEngine() {
        mEngine = com.aispeech.export.engines2.AILocalWakeupIncrementEngine.createInstance();
    }

    /**
     * 创建实例引擎
     *
     * @return AILocalWakeupIncrementEngine
     */
    public static AILocalWakeupIncrementEngine createInstance() {
        return new AILocalWakeupIncrementEngine();
    }

    /**
     * 检查so是否加载成功
     *
     * @return boolean
     */
    public static boolean checkLibValid() {
        return Asr.isAsrSoValid() && Gram.isGramSoValid();
    }

    /**
     * 传入数据,在不使用SDK内部录音机时调用
     *
     * @param data 音频数据流
     */
    public void feedData(byte[] data) {
        if (mEngine != null) {
            mEngine.feedData(data);
        }
    }

    /**
     * 初始化引擎
     *
     * @param config   {@link AILocalWakeupIncrementConfig}
     * @param listener {@link AILocalWakeupIncrementListener}
     */
    public void init(AILocalWakeupIncrementConfig config, AILocalWakeupIncrementListener listener) {
        if (mEngine != null) {
            mEngine.init(config, listener);
        }
    }

    public void setCustomThreshold(String[] words, Double[] threshold) {
        if (mEngine != null) {
            mEngine.setCustomThreshold(words, threshold);
        }
    }

    public void setBlackWords(String[] blackWords) {
        if (mEngine != null) {
            mEngine.setBlackWords(blackWords);
        }
    }

    /**
     * 切换Scene场景，编译传入的xbnf后，把该xbnf插入Scene场景所代表的主xbnf中
     *
     * @param sceneName      场景名称
     * @param grammarContent json字符串，编译场景slot.bin的内容
     *                       [{"slot_disconnect":[{"origin":"抖音最全中文歌","id":"123123","name":"music","segment":["抖音最全","中文歌"]},{"origin":"白天模式","id":"123123"}]},
     *                       {"slot_connect":[{"origin":"风扇电视机","id":"123124","name":"music","segment":["风扇","电视机"]},{"origin":"白天模式","id":"123124"}]}]
     */
    public void setScene(String sceneName, String grammarContent) {
        if (mEngine != null) {
            mEngine.setScene(sceneName, grammarContent);
        }
    }

    /**
     * 启动热词引擎
     * n
     *
     * @param intent 启动参数
     */
    public void start(AILocalWakeupIncrementIntent intent) {
        if (mEngine != null) {
            mEngine.start(intent);
        }
    }

    /**
     * 停止热词引擎
     */
    public void cancel() {
        if (mEngine != null) {
            mEngine.cancel();
        }
    }

    /**
     * 识别结束等待识别解码结果
     *
     * @deprecated 不推荐外部直接调用，仅供外置vad方案使用。
     */
    @Deprecated
    public void stop() {
        if (mEngine != null) {
            mEngine.stop();
        }
    }

    /**
     * 销毁引擎
     */
    public void destroy() {
        if (mEngine != null) {
            mEngine.destroy();
        }

    }


}
