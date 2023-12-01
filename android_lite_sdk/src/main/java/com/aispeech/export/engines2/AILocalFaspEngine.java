package com.aispeech.export.engines2;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.config.AILocalFaspConfig;
import com.aispeech.export.listeners.AILocalFaspListener;
import com.aispeech.kernel.Fasp;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.FaspConfig;
import com.aispeech.lite.fasp.FaspProcessor;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.speech.SpeechListener;

import java.io.File;
import java.util.Map;

/**
 * 双麦降噪
 */
public class AILocalFaspEngine extends BaseEngine {

    private FaspProcessor processor;
    private FaspConfig config;
    private SpeechParams params;
    private SpeechListenerImpl mSpeechListenerImpl;

    public static AILocalFaspEngine createInstance() {
        return new AILocalFaspEngine();
    }

    private AILocalFaspEngine() {
        config = new FaspConfig();
        params = new SpeechParams();
        processor = new FaspProcessor();
        mSpeechListenerImpl = new SpeechListenerImpl();
        mBaseProcessor = processor;
    }

    @Override
    public String getTag() {
        return "local_fasp";
    }

    private static boolean checkLibValid() {
        return Fasp.isLoadSoSuc();
    }

    public void init(AILocalFaspConfig faspConfig, AILocalFaspListener listener) {
        if (!checkLibValid()) {
            if (listener != null) {
                listener.onInit(AIConstant.OPT_FAILED);
                listener.onError(new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }
        super.init();
        Log.d(TAG, "AILocalFaspConfig " + faspConfig);
        final String resource = faspConfig.getResource();
        if (TextUtils.isEmpty(resource)) {
            Log.e(TAG, "fasp resource not found !!");
        } else if (resource.startsWith("/")) {
            config.setResBinPath(resource);
        } else {
            config.setAssetsResNames(new String[]{resource});
            config.setResBinPath(Util.getResourceDir(config.getContext()) + File.separator + resource);
        }

        mSpeechListenerImpl.setListener(listener);
        processor.init(mSpeechListenerImpl, config);
    }

    public void start(boolean useCustomFeed) {
        super.start();
        params.setUseCustomFeed(useCustomFeed);
        if (processor != null) {
            processor.start(params);
        }
    }

    /**
     * 自行feed音频数据，不使用内部录音机时可用
     *
     * @param data 音频数据
     * @param size 音频数据大小
     */
    public void feedData(byte[] data, int size) {
        if (params != null && !params.isUseCustomFeed())
            return;
        if (processor != null) {
            processor.feedData(data, size);
        }
    }

    public void setDynamicParam(String setParam) {
        if (processor != null)
            processor.set(setParam);
    }

    public void getInputWavChan() {
        if (processor != null)
            processor.getInputWavChan();
    }

    /**
     * 停止引擎
     */
    public void stop() {
        super.stop();
        if (processor != null)
            processor.stop();
    }

    /**
     * 销毁引擎
     */
    public void destroy() {
        super.destroy();
        if (mSpeechListenerImpl != null)
            mSpeechListenerImpl.setListener(null);
        if (processor != null)
            processor.release();
    }


    private class SpeechListenerImpl extends SpeechListener {

        private AILocalFaspListener listener;

        public void setListener(AILocalFaspListener listener) {
            this.listener = listener;
        }

        @Override
        public void onInit(int status) {
            if (listener != null)
                listener.onInit(status);
        }

        @Override
        public void onError(AIError error) {
            if (listener != null)
                listener.onError(error);
        }

        @Override
        public void onResults(AIResult result) {

        }

        @Override
        public void onReadyForSpeech() {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {
            if (listener != null)
                listener.onRawDataReceived(buffer, size);
        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onRecorderStopped() {

        }

        @Override
        public void onEvent(int eventType, Map params) {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeup_type) {

        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] data, int size) {
            if (listener != null) {
                if (dataType == 1)
                    listener.onChs1DataReceived(size, data);
                else if (dataType == 2)
                    listener.onChs2DataReceived(size, data);
                else if (dataType == 3)
                    listener.onGotInputWavChan(size);
            }

        }

        @Override
        public void onNotOneShot() {

        }
    }
}
