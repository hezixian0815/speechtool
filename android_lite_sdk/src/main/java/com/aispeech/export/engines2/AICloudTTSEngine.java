package com.aispeech.export.engines2;

import static com.aispeech.lite.param.BaseRequestParams.TYPE_CLOUD;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.os.Build;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.common.Log;
import com.aispeech.export.config.AICloudTTSConfig;
import com.aispeech.export.intent.AICloudTTSIntent;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.export.interceptor.TTSInterceptor;
import com.aispeech.export.listeners.AIEmotionTTSListener;
import com.aispeech.export.listeners.AITTSListener;
import com.aispeech.lite.AISampleRate;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.audio.AIPlayerListener;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.CloudTtsConfig;
import com.aispeech.lite.param.CloudTtsParams;
import com.aispeech.lite.tts.SynthesizerListener;
import com.aispeech.lite.tts.TTSCache;
import com.aispeech.lite.tts.TtsProcessor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * 云端TTS
 */
public class AICloudTTSEngine extends BaseEngine {

    private TtsProcessor mTtsProcessor;
    private CloudTtsConfig mConfig;
    private CloudTtsParams mParams;
    private SynthesizerListenerImpl mSpeechListener;
    private boolean useCache = true;
    private String cacheDirectory = null;

    private String mUtteranceId;

    private AICloudTTSEngine() {
        mTtsProcessor = new TtsProcessor();
        mParams = new CloudTtsParams();
        mConfig = new CloudTtsConfig();
        mSpeechListener = new SynthesizerListenerImpl(null);
    }

    @Override
    public String getTag() {
        return "cloud_tts";
    }


    /**
     * 获取云端合成引擎实例
     *
     * @return 云端tts引擎
     */
    public static AICloudTTSEngine createInstance() {
        return new AICloudTTSEngine();
    }

    public synchronized void init(AICloudTTSConfig config, AITTSListener listener) {
        super.init();
        parseConfig(config);
        mSpeechListener.setListener(listener);
        mTtsProcessor.init(mSpeechListener, mConfig, TtsProcessor.MODEL_CLOUD);
        TTSCache.getInstanceCloud().setUseCache(useCache, TextUtils.isEmpty(cacheDirectory) ? null : new File(cacheDirectory));
    }

    private void parseConfig(AICloudTTSConfig config) {
        if (config == null) {
            Log.e(TAG, "AICloudTTSConfig is null");
            return;
        }
        super.parseConfig(config, mConfig);
        Log.e(TAG, "AICloudTTSConfig " + config);
        this.useCache = config.isUseCache();
        TTSCache.getInstanceCloud().setUseCache(config.isUseCache());
        TTSCache.getInstanceCloud().setCacheSize(config.getCacheSize());
        TTSCache.getInstanceLocal().setCacheWordCount(config.getCacheWordCount());
        mConfig.setUseStopCallback(config.isUseStopCallback());

        this.cacheDirectory = config.getCacheDirectory();
    }

    /**
     * 合成并播放
     *
     * @param aICloudTTSIntent 参数
     * @param refText          合成文本
     * @param utteranceId      utteranceId
     */
    public void speak(AICloudTTSIntent aICloudTTSIntent, String refText, String utteranceId) {
        Log.i(TAG, "speak:" + refText);
        parseIntent(aICloudTTSIntent);
        mUtteranceId = utteranceId;
        mParams.setUtteranceId(utteranceId);
        mParams.setRefText(refText);
        mParams.setIsAutoPlay(true);
        mParams.setOutRealData(false);
        mParams.setType(TYPE_CLOUD);
        mParams.setSdkName("DUI-lite-android-sdk-" + AISpeechSDK.SDK_VERSION);
        mTtsProcessor.start(mParams);
    }

    /**
     * 只合成，不播放，同时抛出实时合成音频流
     *
     * @param aICloudTTSIntent 参数
     * @param refText          合成文本
     * @param utteranceId      utteranceId
     */
    public void synthesize(AICloudTTSIntent aICloudTTSIntent, String refText, String utteranceId) {
        Log.i(TAG, "synthesize:" + refText);
        parseIntent(aICloudTTSIntent);
        mUtteranceId = utteranceId;
        mParams.setUtteranceId(utteranceId);
        mParams.setRefText(refText);
        mParams.setIsAutoPlay(false);
        mParams.setOutRealData(true);
        mParams.setType(TYPE_CLOUD);
        mParams.setSdkName("DUI-lite-android-sdk-" + AISpeechSDK.SDK_VERSION);
        mTtsProcessor.start(mParams);
    }

    @SuppressLint("WrongConstant")
    private void parseIntent(AICloudTTSIntent aICloudTTSIntent) {
        if (aICloudTTSIntent == null) {
            Log.e(TAG, "AICloudTTSIntent is null");
            return;
        }
        super.parseIntent(aICloudTTSIntent, mParams);
        Log.e(TAG, "AICloudTTSIntent " + aICloudTTSIntent);


        if (aICloudTTSIntent.getSaveAudioPath() != null) {
            mParams.setSaveAudioPath(aICloudTTSIntent.getSaveAudioPath());
        }
        if (aICloudTTSIntent.getAudioType() != null) {
            mParams.setAudioType(aICloudTTSIntent.getAudioType());
        }
        if (aICloudTTSIntent.getMp3Quality() != null) {
            mParams.setMp3Quality(aICloudTTSIntent.getMp3Quality());
        }
        if (aICloudTTSIntent.getTextType() != null) {
            mParams.setTextType(aICloudTTSIntent.getTextType());
        }

        if (aICloudTTSIntent.getServer() != null) {
            mParams.setServer(aICloudTTSIntent.getServer());
        }
        mParams.setSpeaker(aICloudTTSIntent.getSpeaker());
        mParams.setSpeed(aICloudTTSIntent.getSpeed());
        mParams.setStreamType(aICloudTTSIntent.getStreamType());
        mParams.setPitchChange(aICloudTTSIntent.getPitchChange() + "");

        if (aICloudTTSIntent.getAudioAttributesUsage() > 0 &&
                aICloudTTSIntent.getAudioAttributesContentType() > 0 &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(aICloudTTSIntent.getAudioAttributesUsage())
                    .setContentType(aICloudTTSIntent.getAudioAttributesContentType())
                    .build();
            mParams.setAudioAttributes(audioAttributes);
        }
        if (aICloudTTSIntent.getAudioAttributes() != null) {
            mParams.setAudioAttributes(aICloudTTSIntent.getAudioAttributes());
        }

        mParams.setVolume(aICloudTTSIntent.getVolume());
        mParams.setRealBack(aICloudTTSIntent.isRealBack());
        mParams.setUserId(aICloudTTSIntent.getUserId());
        mParams.setSpeakingStyle(aICloudTTSIntent.getSpeakingStyle());
        mParams.setSampleRate(AISampleRate.toAISampleRate(aICloudTTSIntent.getSampleRate()));
        mParams.setLanguage(aICloudTTSIntent.getLanguage());
        mParams.setReturnPhone(aICloudTTSIntent.isReturnPhone());
        mParams.setWaitingTimeout(aICloudTTSIntent.getWaitingTimeout());
    }

    public void stop() {
        super.stop();
        mTtsProcessor.stop();
    }

    public void pause() {
        Log.i(TAG, "pause");
        mTtsProcessor.pause();
    }

    public void resume() {
        Log.i(TAG, "resume");
        mTtsProcessor.resume();
    }

    /**
     * @deprecated 废弃
     */
    @Deprecated
    public void release() {
        destroy();
    }

    public synchronized void destroy() {
        super.destroy();
        if (mTtsProcessor != null)
            mTtsProcessor.release();

        if (mSpeechListener != null)
            mSpeechListener.setListener(null);
    }


    /**
     * 是否使用了缓存功能
     *
     * @return true 使用，false 未使用
     */
    public boolean isUseCache() {
        return TTSCache.getInstanceCloud().isUseCache();
    }

    /**
     * The adapter for convert SpeechListener to AILocalTTSListener.
     */
    private class SynthesizerListenerImpl implements SynthesizerListener, AIPlayerListener {

        AITTSListener mListener;

        public SynthesizerListenerImpl(AITTSListener listener) {
            mListener = listener;
        }

        public void setListener(AITTSListener listener) {
            mListener = listener;
        }


        @Override
        public void onInit(int status) {
            if (mListener != null) {
                mListener.onInit(status);
            }
        }

        @Override
        public void onSpeechStart() {
            try {
                JSONObject customObj = new JSONObject().put(IInterceptor.Name.CLOUD_TTS_PLAY_FIRST, "");
                JSONObject inputObj = TTSInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, customObj);
                SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.CLOUD_TTS_PLAY_FIRST, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (mListener != null) {
                mListener.onReady(mUtteranceId);
            }
        }

        @Override
        public void onSpeechFinish() {
            if (mListener != null) {
                mListener.onCompletion(mUtteranceId);
            }
        }


        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(mUtteranceId, error);
            }
        }

        @Override
        public void onSpeechProgress(int currentTime, int totalTime, boolean isDataReady) {
            if (mListener != null) {
                mListener.onProgress(currentTime, totalTime, isDataReady);
            }
        }

        @Override
        public void onProgress(int currentTime, int totalTime, boolean isRefTextTTSFinished) {
            //do nothing
        }


        @Override
        public void onSynthesizeStart() {
            try {
                JSONObject customObj = new JSONObject().put(IInterceptor.Name.CLOUD_TTS_SYNTHESIS_FIRST, "");
                JSONObject inputObj = TTSInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, customObj);
                SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.CLOUD_TTS_SYNTHESIS_FIRST, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (mListener != null) {
                mListener.onSynthesizeStart(mUtteranceId);
            }
        }

        @Override
        public void onSynthesizeDataArrived(byte[] audioData) {
            if (mListener != null) {
                mListener.onSynthesizeDataArrived(mUtteranceId, audioData);
            }
        }

        @Override
        public void onSynthesizeFinish() {
            try {
                JSONObject customObj = new JSONObject().put(IInterceptor.Name.CLOUD_TTS_SYNTHESIS_END, "");
                JSONObject inputObj = TTSInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, customObj);
                SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.CLOUD_TTS_SYNTHESIS_END, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (mListener != null) {
                mListener.onSynthesizeFinish(mUtteranceId);
            }
        }

        @Override
        public void onTimestampReceived(byte[] timeStampJson, int size) {
            if (mListener != null) {
                mListener.onTimestampReceived(timeStampJson,size);
            }
        }


        @Override
        public void onPhonemesDataArrived(String phonemes) {
            if (mListener != null) {
                mListener.onPhonemesDataArrived(mUtteranceId, phonemes);
            }
        }

        @Override
        public void onEmotion(String emotion, String emotionOrigin) {
            if (mListener instanceof AIEmotionTTSListener) {
                ((AIEmotionTTSListener) mListener).onEmotion(emotion, emotionOrigin, mUtteranceId);
            }
        }

        @Override
        public void onReady() {
            //do nothing
        }

        @Override
        public void onPaused() {
            //do nothing
        }

        @Override
        public void onStopped() {
            //do nothing
        }

        @Override
        public void onResumed() {
            //do nothing
        }

        @Override
        public void onCompletion(long sessionId) {
            //do nothing
        }
    }

}
