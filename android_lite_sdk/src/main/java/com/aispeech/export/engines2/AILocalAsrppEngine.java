package com.aispeech.export.engines2;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.config.AILocalAsrppConfig;
import com.aispeech.export.intent.AILocalAsrppIntent;
import com.aispeech.export.listeners.AILocalAsrppListener;
import com.aispeech.kernel.Asrpp;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Vad;
import com.aispeech.lite.asrpp.LocalAsrppProcessor;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalAsrppConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.LocalAsrppParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;

import java.io.File;
import java.util.Map;

/**
 * 本地 性别、年龄、情绪 识别
 */
public class AILocalAsrppEngine extends BaseEngine {

    private LocalAsrppProcessor mLocalAsrppProcessor;
    private LocalAsrppConfig mAsrppConfig;
    private LocalAsrppParams mAsrppParams;
    private LocalVadConfig mVadConfig;
    private VadParams mVadParams;
    private SpeechListenerImpl mSpeechListener;

    private AILocalAsrppEngine() {
        mLocalAsrppProcessor = new LocalAsrppProcessor();
        mAsrppConfig = new LocalAsrppConfig();
        mAsrppParams = new LocalAsrppParams();
        mVadConfig = new LocalVadConfig();
        mVadParams = new VadParams();
        mSpeechListener = new SpeechListenerImpl(null);
        mBaseProcessor = mLocalAsrppProcessor;
    }

    @Override
    public String getTag() {
        return "local_asrpp";
    }

    public static AILocalAsrppEngine createInstance() {
        return new AILocalAsrppEngine();
    }


    private static boolean checkLibValid() {
        return Asrpp.isAsrppSoValid() && Vad.isSoValid() && Utils.isUtilsSoValid();
    }

    public void init(final AILocalAsrppConfig config, AILocalAsrppListener localAsrppListener) {
        if (!checkLibValid()) {
            if (localAsrppListener != null) {
                localAsrppListener.onInit(AIConstant.OPT_FAILED);
                localAsrppListener.onError(new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }
        super.init();
        parseConfig(config);
        mSpeechListener.setListener(localAsrppListener);
        mLocalAsrppProcessor.init(mSpeechListener, mAsrppConfig, mVadConfig);
    }

    private void parseConfig(final AILocalAsrppConfig config) {
        if (config == null) {
            Log.e(TAG, "AILocalAsrppConfig is null !");
            return;
        }
        Log.d(TAG, "AILocalAsrppConfig " + config);

        // asrppResource
        final String asrppResource = config.getAsrppResource();
        if (TextUtils.isEmpty(asrppResource)) {
            Log.e(TAG, "Asrpp res not found !!");
        } else if (asrppResource.startsWith("/")) {
            mAsrppConfig.setResBinPath(asrppResource);
        } else {
            mAsrppConfig.setAssetsResNames(new String[]{asrppResource});
            mAsrppConfig.setResBinPath(Util.getResourceDir(mAsrppConfig.getContext()) + File.separator + asrppResource);
        }
        // vadResource
        final String vadResource = config.getVadResource();
        if (TextUtils.isEmpty(vadResource)) {
            Log.e(TAG, "vad res not found !!");
        } else if (vadResource.startsWith("/")) {
            mVadConfig.setResBinPath(vadResource);
        } else {
            mVadConfig.setAssetsResNames(new String[]{vadResource});
            mVadConfig.setResBinPath(Util.getResourceDir(mVadConfig.getContext()) + File.separator + vadResource);
        }

        // vadEnable
        mAsrppConfig.setVadEnable(config.isVadEnable());
        mVadConfig.setVadEnable(config.isVadEnable());
        mVadParams.setVadEnable(config.isVadEnable());

        // vadPauseTime
        mVadConfig.setPauseTime(config.getVadPauseTime());
        mVadParams.setPauseTime(config.getVadPauseTime());
    }

    private void parseIntent(final AILocalAsrppIntent aiLocalAsrppIntent) {
        if (aiLocalAsrppIntent == null) {
            Log.d(TAG, "AILocalAsrppIntent is null !");
            return;
        }
        super.parseIntent(aiLocalAsrppIntent, mAsrppParams, mVadParams);
        Log.d(TAG, "AILocalAsrppIntent " + aiLocalAsrppIntent);
        // useCustomFeed
        mAsrppParams.setUseCustomFeed(aiLocalAsrppIntent.isUseCustomFeed());
        mVadConfig.setUseCustomFeed(aiLocalAsrppIntent.isUseCustomFeed());
        // NoSpeechTimeout
        mAsrppParams.setNoSpeechTimeout(aiLocalAsrppIntent.getNoSpeechTimeOut());
        mVadParams.setNoSpeechTimeout(aiLocalAsrppIntent.getNoSpeechTimeOut());
        // MaxSpeechTimeS
        mAsrppParams.setMaxSpeechTimeS(aiLocalAsrppIntent.getMaxSpeechTimeS());
        mVadParams.setMaxSpeechTimeS(aiLocalAsrppIntent.getMaxSpeechTimeS());
        mAsrppParams.setFespxEngine(aiLocalAsrppIntent.getFespxEngine());
        mAsrppParams.setVolumeCheck(aiLocalAsrppIntent.isVolumeCheck());
        mAsrppParams.setEnv(aiLocalAsrppIntent.getEnv());
    }

    /**
     * 启动录音，开始语音识别
     *
     * @param aiLocalAsrppIntent 参数
     */
    public void start(final AILocalAsrppIntent aiLocalAsrppIntent) {
        super.start();
        parseIntent(aiLocalAsrppIntent);
        if (mLocalAsrppProcessor != null) {
            mLocalAsrppProcessor.start(mAsrppParams, mVadParams);
        }
    }


    /**
     * 传入数据,在不使用SDK内部录音机时调用
     *
     * @param data 音频数据流
     * @param size 数据大小
     * @see AILocalAsrppIntent#setUseCustomFeed(boolean)
     */
    public void feedData(byte[] data, int size) {
        if (mAsrppParams != null && !mAsrppParams.isUseCustomFeed())
            return;
        if (mLocalAsrppProcessor != null) {
            mLocalAsrppProcessor.feedData(data, size);
        }
    }

    /**
     * 停止录音，等待识别结果
     */
    public void stop() {
        super.stop();
        if (mLocalAsrppProcessor != null) {
            mLocalAsrppProcessor.stop();
        }
    }


    /**
     * 销毁本地识别引擎
     */
    public void destroy() {
        super.destroy();
        if (mLocalAsrppProcessor != null) {
            mLocalAsrppProcessor.release();
        }
        if (mAsrppConfig != null) {
            mAsrppConfig = null;
        }
        if (mAsrppParams != null) {
            mAsrppParams = null;
        }
        if (mVadConfig != null) {
            mVadConfig = null;
        }
        if (mVadParams != null) {
            mVadParams = null;
        }
        if (mSpeechListener != null) {
            mSpeechListener.setListener(null);
            mSpeechListener = null;
        }
    }

    private class SpeechListenerImpl extends SpeechListener {
        AILocalAsrppListener mListener;

        public SpeechListenerImpl(AILocalAsrppListener listener) {
            mListener = listener;
        }

        public void setListener(AILocalAsrppListener listener) {
            mListener = listener;
        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(error);
            }
        }

        @Override
        public void onInit(int status) {
            if (mListener != null) {
                mListener.onInit(status);
            }
        }

        @Override
        public void onResults(AIResult result) {
            if (mListener != null) {
                mListener.onResults(result);
            }
        }

        @Override
        public void onBeginningOfSpeech() {
            if (mListener != null) {
                mListener.onBeginningOfSpeech();
            }
        }


        @Override
        public void onRawDataReceived(byte[] buffer, int size) {
            if (mListener != null) {
                mListener.onRawDataReceived(buffer, size);
            }
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {
            if (mListener != null) {
                mListener.onResultDataReceived(buffer, size);
            }
        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] data, int size) {
            //do nothing
        }


        @Override
        public void onEndOfSpeech() {
            if (mListener != null) {
                mListener.onEndOfSpeech();
            }
        }


        @Override
        public void onReadyForSpeech() {
            if (mListener != null) {
                mListener.onReadyForSpeech();
            }

        }

        @Override
        public void onRmsChanged(float rmsdB) {
            if (mListener != null) {
                mListener.onRmsChanged(rmsdB);
            }
        }

        /**
         * 废弃
         * @deprecated 废弃
         */
        @Override
        @Deprecated
        public void onRecorderStopped() {
            //do nothing
        }


        @Override
        public void onEvent(int eventType, Map params) {
            //do nothing
        }

        @Override
        public void onNotOneShot() {
            //do nothing
        }
    }
}
