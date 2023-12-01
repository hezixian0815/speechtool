package com.aispeech.export.engines2;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.export.config.AntiSpoofConfig;
import com.aispeech.export.intent.AntiSpoofIntent;
import com.aispeech.export.listeners.AILocalAntiSpoofListener;
import com.aispeech.export.listeners.AILocalVprintLiteListener;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.vprintlite.VprintLiteConfig;
import com.aispeech.lite.vprintlite.VprintLiteIntent;

public class AILocalAntiSpoofingEngine extends BaseEngine {
    private AILocalVprintLiteEngine mAILocalVprintLiteEngine;
    private VprintliteListenerImpl mAILocalVprintLiteListener;

    private AILocalAntiSpoofingEngine() {
        mAILocalVprintLiteEngine = AILocalVprintLiteEngine.createInstance();
    }

    @Override
    public String getTag() {
        return "local_anti_spoof";
    }

    public static AILocalAntiSpoofingEngine createInstance() {
        return new AILocalAntiSpoofingEngine();
    }

    public void init(AntiSpoofConfig config, final AILocalAntiSpoofListener antiSpoofListener) {
        super.init();
        mAILocalVprintLiteListener = new VprintliteListenerImpl();
        mAILocalVprintLiteListener.setListener(antiSpoofListener);
        VprintLiteConfig.Builder builder = new VprintLiteConfig.Builder();
        builder.setVprintType(AIConstant.VPRINTLITE_TYPE_ANTI_SPOOFING);
        builder.setVprintResBin(config.getAntiSpoofResBin());
        VprintLiteConfig vprintLiteConfig = builder.create();
        mAILocalVprintLiteEngine.init(vprintLiteConfig, mAILocalVprintLiteListener);
    }

    /**
     * 启动仿冒攻击引擎
     * @param intent 仿冒攻击intent
     */
    public void start(AntiSpoofIntent intent) {
        super.start();
        if (mAILocalVprintLiteEngine != null) {
            VprintLiteIntent.Builder builder = new VprintLiteIntent.Builder();
            builder.setAction(VprintLiteIntent.Action.VERIFY);
            builder.setVprintLiteSaveDir(intent.getAntiSpoofSaveDir());
            mAILocalVprintLiteEngine.start(builder.create());
        }
    }
    /**
     * 传入数据
     *
     * @param data 音频数据流
     * @param size 数据大小
     */
    public void feedData(byte[] data, int size, AntiSpoofIntent intent) {
        VprintLiteIntent.Builder builder = new VprintLiteIntent.Builder();
        builder.setResStart(intent.getResStart());
        builder.setResEnd(intent.getResEnd());
        builder.setAction(VprintLiteIntent.Action.VERIFY);
        builder.setSpeechState(intent.getSpeechState());
        if (mAILocalVprintLiteEngine != null) {
            mAILocalVprintLiteEngine.feedData(data, size, builder.create());
        }
    }

    /**
     * 停止引擎
     */
    public void stop() {
        super.stop();
        if (mAILocalVprintLiteEngine != null) {
            mAILocalVprintLiteEngine.stop();
        }
    }

    /**
     * 取消引擎
     */
    public void cancel() {
        super.cancel();
        if (mAILocalVprintLiteEngine != null) {
            mAILocalVprintLiteEngine.cancel();
        }
    }

    /**
     * 销毁引擎
     */
    public synchronized void destroy() {
        super.destroy();
        if (mAILocalVprintLiteEngine != null) {
            mAILocalVprintLiteEngine.destroy();
            mAILocalVprintLiteEngine = null;
        }
        if (mAILocalVprintLiteListener != null) {
            mAILocalVprintLiteListener.setListener(null);
            mAILocalVprintLiteListener = null;
        }
    }

    private class VprintliteListenerImpl implements AILocalVprintLiteListener {
        private AILocalAntiSpoofListener listener;

        public void setListener(AILocalAntiSpoofListener listener) {
            this.listener = listener;
        }

        @Override
        public void onInit(int status) {
            if (listener != null) {
                listener.onInit(status);
            }
        }

        @Override
        public void onError(AIError error) {
            if (listener != null) {
                listener.onError(error);
            }
        }

        @Override
        public void onResults(AIResult result) {
            if (listener != null) {
                listener.onResults(result);
            }
        }
    }
}
