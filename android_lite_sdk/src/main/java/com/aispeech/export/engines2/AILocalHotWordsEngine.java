package com.aispeech.export.engines2;

import com.aispeech.AIResult;
import com.aispeech.base.BaseInnerEngine;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.config.AILocalHotWordConfig;
import com.aispeech.export.intent.AILocalHotWordIntent;
import com.aispeech.export.listeners.AILocalHotWordsListener;
import com.aispeech.kernel.Asr;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalAsrConfig;
import com.aispeech.lite.config.SSLConfig;
import com.aispeech.lite.hotword.HotWordsProcessor;
import com.aispeech.lite.param.LocalAsrParams;
import com.aispeech.lite.param.VadParams;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * 本地热词引擎
 */
public class AILocalHotWordsEngine extends BaseEngine {

    private SSLConfig mVadConfig;
    private LocalAsrConfig mAsrConfig;
    private VadParams mVadParams;
    private LocalAsrParams mAsrParams;
    private HotWordsProcessor mLocalHotWordsProcessor;

    private InnerEngine mInnerEngine;

    private AILocalHotWordsEngine() {
        mVadConfig = new SSLConfig();
        mAsrConfig = new LocalAsrConfig();
        mVadParams = new VadParams();
        mAsrParams = new LocalAsrParams();
        mLocalHotWordsProcessor = new HotWordsProcessor();
        mInnerEngine = new InnerEngine();
        mBaseProcessor = mLocalHotWordsProcessor;
    }

    @Override
    public String getTag() {
        return "hotwords";
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
        if (mAsrParams != null && !mAsrParams.isUseCustomFeed()) {
            Log.df(TAG, "useCustomFeed is not enabled,ignore data");
            return;
        }
        if (mLocalHotWordsProcessor != null) {
            mLocalHotWordsProcessor.feedData(data, data.length);
        }
    }


    /**
     * 初始化引擎
     *
     * @param config   {@link AILocalHotWordConfig}
     * @param listener {@link AILocalHotWordsListener}
     */
    public void init(AILocalHotWordConfig config, AILocalHotWordsListener listener) {
        super.init();
        parseConfig(config);
        mInnerEngine.init(listener);
        mLocalHotWordsProcessor.init(mInnerEngine, mAsrConfig, mVadConfig);
    }

    /**
     * 解析初始化参数
     *
     * @param config {@link AILocalHotWordConfig}
     */
    private void parseConfig(AILocalHotWordConfig config) {
        super.parseConfig(config, mVadConfig, mAsrConfig);
        mVadParams.setPauseTime(0);//热词引擎vad pauseTime 0
        mAsrConfig.setScope(config.languages.getLanguage());
        mVadConfig.setVadEnable(config.useVad);
        mVadConfig.setUseSSL(config.useSSL);

        if (config.useVad) {
            List<String> vadAssetList = new ArrayList<>();
            if (config.vadRes.startsWith("/")) {
                mVadConfig.setResBinPath(config.vadRes);
            } else {
                vadAssetList.add(config.vadRes);
                mVadConfig.setResBinPath(Util.getResourceDir(mAsrConfig.getContext()) + File.separator + config.vadRes);
            }
            mVadConfig.setAssetsResNames(vadAssetList.toArray(new String[vadAssetList.size()]));
        }
        List<String> asrAssetList = new ArrayList<>();
        if (config.asrRes.startsWith("/")) {
            mAsrConfig.setResBinPath(config.asrRes);
        } else {
            asrAssetList.add(config.asrRes);
            mAsrConfig.setResBinPath(Util.getResourceDir(mAsrConfig.getContext()) + File.separator + config.asrRes);
        }
        mAsrConfig.setAssetsResNames(asrAssetList.toArray(new String[asrAssetList.size()]));
    }


    /**
     * 启动本地热词引擎
     *
     * @param intent 启动参数 {@link AILocalHotWordIntent}
     */
    public void start(AILocalHotWordIntent intent) {
        super.start();
        parseIntent(intent);
        if (mLocalHotWordsProcessor != null) {
            mLocalHotWordsProcessor.start(mAsrParams, mVadParams);
        }
    }

    /**
     * 解析启动次参数
     *
     * @param intent {@link AILocalHotWordIntent}
     */
    private void parseIntent(AILocalHotWordIntent intent) {
        super.parseIntent(intent, mAsrParams, mVadParams);
        mAsrParams.setUseContinuousRecognition(intent.isUseContinuousRecognition());
        mAsrParams.setMaxSpeechTimeS(intent.getMaxSpeechTime());
        mVadParams.setSaveAudioPath(intent.getSaveAudioPath());
        mAsrParams.setSaveAudioPath(intent.getSaveAudioPath());
        mAsrParams.setCustomThresholdMap(intent.getCustomThreshold());
        mAsrParams.setDynamicList(intent.getWords());
        mAsrParams.setUseDynamicBackList(intent.getBlackWords());
        mAsrParams.setUseThreshold(intent.getThreshold());
        mAsrParams.setUseEnglishThreshold(intent.getEnglishThreshold());
        mAsrParams.setNoSpeechTimeout(intent.getNoSpeechTime());
        mAsrParams.setIsIgnoreThreshold(intent.getIsIgnoreThreshold());
        mAsrParams.setUseCustomFeed(intent.isUseCustomFeed());
        mAsrParams.setUseOmitConf(intent.getOmitConf());
        mAsrParams.setUseOmitTime(intent.getOmitTime());
        mAsrParams.setUseOmitCheck(intent.isOmitCheck());

    }


    /**
     * 停止热词引擎
     */
    public void cancel() {
        super.cancel();
        if (mLocalHotWordsProcessor != null) {
            mLocalHotWordsProcessor.cancel();
        }

        if (mInnerEngine != null) {
            mInnerEngine.removeCallbackMsg();
        }
    }

    /**
     * 识别结束等待识别解码结果
     *
     * @deprecated 不推荐外部直接调用，仅供外置vad方案使用。
     */
    @Deprecated
    public void stop() {
        super.stop();
        if (mVadConfig.isVadEnable())
            throw new IllegalArgumentException("not allowed method when vad enable");

        if (mLocalHotWordsProcessor != null) {
            mLocalHotWordsProcessor.stop();
        }
    }


    /**
     * 销毁引擎
     */
    public void destroy() {
        super.destroy();
        if (mLocalHotWordsProcessor != null) {
            mLocalHotWordsProcessor.release();
        }
        if (mInnerEngine != null) {
            mInnerEngine.release();
            mInnerEngine = null;
        }
    }

    private static class InnerEngine extends BaseInnerEngine {

        private AILocalHotWordsListener mListener;

        @Override
        public void release() {
            super.release();
            if (mListener != null)
                mListener = null;
        }

        void init(AILocalHotWordsListener listener) {
            super.init(listener);
            mListener = listener;
        }

        @Override
        protected void callbackInMainLooper(CallbackMsg msg, Object obj) {

            switch (msg) {
                case MSG_RESULTS:
                    mListener.onResults((AIResult) obj);
                    break;
                case MSG_BEGINNING_OF_SPEECH:
                    mListener.onBeginningOfSpeech();
                    break;
                case MSG_END_OF_SPEECH:
                    mListener.onEndOfSpeech();
                    break;
                case MSG_RMS_CHANGED:
                    mListener.onRmsChanged((Float) obj);
                    break;
                case MSG_DOA_RESULT:
                    mListener.onDoa((Integer) obj);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onResults(AIResult result) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RMS_CHANGED, rmsdB);
        }

        @Override
        public void onBeginningOfSpeech() {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_BEGINNING_OF_SPEECH, null);
        }

        @Override
        public void onEndOfSpeech() {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_END_OF_SPEECH, null);
        }

        @Override
        public void onSSL(int index) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_DOA_RESULT, index);
        }

    }

}
