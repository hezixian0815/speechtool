package com.aispeech.export.engines;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.base.IFespxEngine;
import com.aispeech.common.Util;
import com.aispeech.export.listeners.AILocalAsrppListener;
import com.aispeech.kernel.Asrpp;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Vad;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.asrpp.LocalAsrppProcessor;
import com.aispeech.lite.config.LocalAsrppConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;

import java.util.Map;

/**
 * 本地 性别、年龄、情绪 识别
 *
 * @deprecated replaced by {@link com.aispeech.export.engines2.AILocalAsrppEngine}
 */
@Deprecated
public class AILocalAsrppEngine {
    public static final String TAG = "AILocalAsrppEngine";
    private LocalAsrppProcessor mLocalAsrppProcessor;
    private LocalAsrppConfig mAsrppConfig;
    private SpeechParams mAsrppParams;
    private LocalVadConfig mVadConfig;
    private VadParams mVadParams;
    private SpeechListenerImpl mSpeechListener;

    private String mVadResName = "";
    private String mVadResPath = "";
    private String mAsrppResName = "";
    private String mAsrppResPath = "";


    private AILocalAsrppEngine() {
        mLocalAsrppProcessor = new LocalAsrppProcessor();
        mAsrppConfig = new LocalAsrppConfig();
        mAsrppParams = new SpeechParams();
        mVadConfig = new LocalVadConfig();
        mVadParams = new VadParams();
        mSpeechListener = new SpeechListenerImpl(null);
    }

    public static AILocalAsrppEngine createInstance() {
        return new AILocalAsrppEngine();
    }


    public static boolean checkLibValid() {
        return Asrpp.isAsrppSoValid() && Vad.isSoValid() && Utils.isUtilsSoValid();
    }


    /**
     * 设置性别识别资源名, 适用于资源放在assets目录下
     * 须在init之前设置才生效
     *
     * @param asrppResBin 性别识别资源名
     */
    public void setAsrppResBin(String asrppResBin) {
        this.mAsrppResName = asrppResBin;
    }

    /**
     * 设置性别识别资源绝对路径，包含资源名，如/sdcard/speech/***.bin
     * 须在init之前设置才生效
     *
     * @param asrppResPath 性别识别资源绝对路径
     */
    public void setAsrppResPath(String asrppResPath) {
        this.mAsrppResPath = asrppResPath;
    }

    /**
     * 设置本地vad资源名，适用于资源放在assets目录下
     * 须在init之前设置才生效
     *
     * @param vadRes 本地vad资源名
     */
    public void setVadRes(String vadRes) {
        this.mVadResName = vadRes;
    }

    /**
     * 设置本地vad资源绝对路径，包含资源名，如/sdcard/speech/***.bin
     * 须在init之前设置才生效
     *
     * @param vadResPath 本地vad资源绝对路径
     */
    public void setVadResPath(String vadResPath) {
        this.mVadResPath = vadResPath;
    }


    /**
     * 设置是否启用本地vad
     * 须在init之前设置才生效
     *
     * @param vadEnable true:使用Vad；false:禁止Vad，默认为true
     */
    public void setVadEnable(boolean vadEnable) {
        mAsrppConfig.setVadEnable(vadEnable);
        mVadConfig.setVadEnable(vadEnable);
        mVadParams.setVadEnable(vadEnable);
    }

    /**
     * 设置VAD右边界
     *
     * @param pauseTime pauseTime 单位：ms,默认300
     */
    public void setPauseTime(int pauseTime) {
        mVadConfig.setPauseTime(pauseTime);
        mVadParams.setPauseTime(pauseTime);
    }

    /**
     * 设置关联的信号处理引擎AILocalSignalAndWakeupEngine实例，只在使用内部录音机且多麦模式下才需要设置
     *
     * @param engine 引擎实例
     * @throws RuntimeException 内部录音机且多麦模式下没设置
     */
    @Deprecated
    public void setFespxEngine(IFespxEngine engine) {
        mAsrppParams.setFespxEngine(engine);
    }


    /**
     * 设置是否自行feed数据,不使用内部录音机(包括MockRecord和AIAudioRecord),
     * 需要在init之前调用, 默认为false
     *
     * @param useCustomFeed the useCustomFeed to set
     */
    public void setUseCustomFeed(boolean useCustomFeed) {
        mAsrppParams.setUseCustomFeed(useCustomFeed);
        mVadConfig.setUseCustomFeed(useCustomFeed);
    }

    /**
     * 设置无语音超时时长，单位毫秒，默认值为5000ms ；如果达到该设置值时，自动停止录音并放弃请求识别内核
     * 须在start之前设置才生效
     *
     * @param milliSecond 超时时长，单位毫秒
     */
    public void setNoSpeechTimeOut(int milliSecond) {
        mAsrppParams.setNoSpeechTimeout(milliSecond);
        mVadParams.setNoSpeechTimeout(milliSecond);
    }


    /**
     * 设置音频最大录音时长，达到该值将取消语音引擎并抛出异常<br>
     * 允许的最大录音时长 单位秒
     * <ul>
     * <li>0 表示无最大录音时长限制</li>
     * <li>默认大小为60S</li>
     * </ul>
     * 须在start之前设置才生效
     *
     * @param seconds seconds
     */
    public void setMaxSpeechTimeS(int seconds) {
        mAsrppParams.setMaxSpeechTimeS(seconds);
        mVadParams.setMaxSpeechTimeS(seconds);
    }

    public void init(AILocalAsrppListener localAsrppListener) {
        mSpeechListener.setListener(localAsrppListener);
        if (TextUtils.isEmpty(mVadResPath)) {
            mVadConfig.setAssetsResNames(new String[]{mVadResName});
            mVadConfig.setResBinPath(Util.getResPath(AISpeech.getContext(), mVadResName));
        } else {
            mVadConfig.setResBinPath(mVadResPath);
        }
        if (TextUtils.isEmpty(mAsrppResPath)) {
            mAsrppConfig.setAssetsResNames(new String[]{mAsrppResName});
            mAsrppConfig.setResBinPath(Util.getResPath(AISpeech.getContext(), mAsrppResName));
        } else {
            mAsrppConfig.setResBinPath(mAsrppResPath);
        }
        mLocalAsrppProcessor.init(mSpeechListener, mAsrppConfig, mVadConfig);
    }


    /**
     * 启动录音，开始语音识别
     */
    public void start() {
        if (mLocalAsrppProcessor != null) {
            mLocalAsrppProcessor.start(mAsrppParams, mVadParams);
        }
    }


    /**
     * 传入数据,在不使用SDK内部录音机时调用
     *
     * @param data 音频数据流
     * @param size 数据大小
     * @see #setUseCustomFeed(boolean)
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
    public void stopRecording() {
        if (mLocalAsrppProcessor != null) {
            mLocalAsrppProcessor.stop();
        }
    }


    /**
     * 销毁本地识别引擎
     */
    public void destroy() {
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
        mVadResName = null;
        mVadResPath = null;
        mAsrppResName = null;
        mAsrppResPath = null;
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
        public void onResultDataReceived(byte[] buffer, int size, int wakeup_type) {
            if (mListener != null) {
                mListener.onResultDataReceived(buffer, size);
            }
        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] data, int size) {

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

        @Override
        public void onRecorderStopped() {
        }


        @Override
        public void onEvent(int eventType, Map params) {

        }

        @Override
        public void onNotOneShot() {

        }
    }
}
