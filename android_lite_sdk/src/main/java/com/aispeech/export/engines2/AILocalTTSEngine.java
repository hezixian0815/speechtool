package com.aispeech.export.engines2;

import android.annotation.SuppressLint;
import android.media.AudioAttributes;
import android.os.Build;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.TTSDynamicParamUtils;
import com.aispeech.common.Util;
import com.aispeech.export.config.AILocalTTSConfig;
import com.aispeech.export.intent.AILocalTTSIntent;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.export.interceptor.TTSInterceptor;
import com.aispeech.export.listeners.AILocalTTSListener;
import com.aispeech.export.listeners.AITTSListener;
import com.aispeech.kernel.Cntts;
import com.aispeech.lite.audio.AIPlayerListener;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalTtsConfig;
import com.aispeech.lite.param.LocalTtsParams;
import com.aispeech.lite.tts.SynthesizerListener;
import com.aispeech.lite.tts.TTSCache;
import com.aispeech.lite.tts.TtsProcessor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地合成
 */
public class AILocalTTSEngine extends BaseEngine {

    private TtsProcessor mLocalTtsProcessor;
    private LocalTtsParams mParams;
    private LocalTtsConfig mLocalTtsConfig;

    private SynthesizerListenerImpl mListenerImpl;
    private boolean useCache = true;
    private String cacheDirectory = null;

    private String mUtteranceId;

    private AtomicInteger ref;

    private AILocalTTSEngine() {
        ref = new AtomicInteger(0);
    }

    @Override
    public String getTag() {
        return "local_tts";
    }


    private static boolean checkLibValid() {
        return Cntts.isSoValid();
    }

    public static AILocalTTSEngine createInstance() {
        return new AILocalTTSEngine();
    }

    /**
     * 初始化合成引擎
     *
     * @param config   配置参数
     * @param listener 合成回调接口
     */
    public void init(AILocalTTSConfig config, AILocalTTSListener listener) {
        if (!checkLibValid()) {
            if (listener != null) {
                listener.onInit(AIConstant.OPT_FAILED);
                listener.onError("", new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }
        super.init();
        addReference();
        parseConfig(config);
        mListenerImpl.setListener(listener);

        mLocalTtsProcessor.init(mListenerImpl, mLocalTtsConfig, TtsProcessor.MODEL_LOCAL);
        TTSCache.getInstanceLocal().setUseCache(useCache, TextUtils.isEmpty(cacheDirectory) ? null : new File(cacheDirectory));
    }

    /**
     * 初始化合成引擎
     *
     * @param config   配置参数
     * @param listener 合成回调接口
     */
    public void init(AILocalTTSConfig config, AITTSListener listener) {
        if (!checkLibValid()) {
            if (listener != null) {
                listener.onInit(AIConstant.OPT_FAILED);
                listener.onError("", new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }
        super.init();
        addReference();
        parseConfig(config);
        mListenerImpl.setListener(listener);

        mLocalTtsProcessor.init(mListenerImpl, mLocalTtsConfig, TtsProcessor.MODEL_LOCAL);
        TTSCache.getInstanceLocal().setUseCache(useCache, TextUtils.isEmpty(cacheDirectory) ? null : new File(cacheDirectory));
    }

    private synchronized void addReference() {
        int count = ref.incrementAndGet();
        if (count <= 0)
            ref.set(1);
        if (mLocalTtsProcessor == null)
            mLocalTtsProcessor = new TtsProcessor();
        if (mParams == null)
            mParams = new LocalTtsParams();
        if (mLocalTtsConfig == null)
            mLocalTtsConfig = new LocalTtsConfig();
        if (mListenerImpl == null)
            mListenerImpl = new SynthesizerListenerImpl(null);
        else
            mListenerImpl.release();
    }

    private void parseConfig(AILocalTTSConfig config) {
        if (config == null) {
            Log.e(TAG, "AILocalTTSConfig is null");
            return;
        }
        Log.i(TAG, "AILocalTTSConfig " + config);
        super.parseConfig(config, mLocalTtsConfig);
        this.useCache = config.isUseCache();
        this.cacheDirectory = config.getCacheDirectory();
        mLocalTtsConfig.setEnableOptimization(config.isEnableOptimization());
        mLocalTtsConfig.setLanguage(config.getLanguage());
        // resource in assets
        final List<String> resourceInAssetsList = new ArrayList<>();

        // mDictDbName
        final String dictResource = config.getDictResource();
        if (TextUtils.isEmpty(dictResource)) {
            Log.e(TAG, "dictResource not found !!");
        } else if (dictResource.startsWith("/")) {
            mLocalTtsConfig.setDictPath(dictResource);
        } else {
            resourceInAssetsList.add(dictResource);
            mLocalTtsConfig.setDictPath(Util.getResourceDir(mLocalTtsConfig.getContext()) + File.separator + dictResource);
        }
        // userDictResource
        final String userDictResource = config.getUserDictResource();
        if (TextUtils.isEmpty(userDictResource)) {
            Log.e(TAG, "userDict resource not found.");
        } else if (userDictResource.startsWith("/")) {
            mLocalTtsConfig.setUserDict(userDictResource);
        } else {
            resourceInAssetsList.add(userDictResource);
            mLocalTtsConfig.setUserDict(Util.getResourceDir(mLocalTtsConfig.getContext()) + File.separator + userDictResource);
        }

        // mFrontBin
        final String frontBinResource = config.getFrontBinResource();
        if (TextUtils.isEmpty(frontBinResource)) {
            Log.e(TAG, "frontBinResource not found !!");
        } else if (frontBinResource.startsWith("/")) {
            mLocalTtsConfig.setFrontBinPath(frontBinResource);
        } else {
            resourceInAssetsList.add(frontBinResource);
            mLocalTtsConfig.setFrontBinPath(Util.getResourceDir(mLocalTtsConfig.getContext()) + File.separator + frontBinResource);
        }

        // speakerResourceList
        List<String> speakerResourceList = config.getSpeakerResourceList();
        if (speakerResourceList != null && !speakerResourceList.isEmpty()) {
            for (String res : speakerResourceList) {
                if (!res.startsWith("/"))
                    resourceInAssetsList.add(res);
            }

            String speakerRes = speakerResourceList.get(0);
            if (TextUtils.isEmpty(speakerRes)) {
                Log.e(TAG, "backBinResource res not found !!");
            } else if (speakerRes.startsWith("/")) {
                mLocalTtsConfig.setBackBinPath(speakerRes);
            } else {
                mLocalTtsConfig.setBackBinPath(Util.getResourceDir(mLocalTtsConfig.getContext()) + File.separator + speakerRes);
            }
        } else
            Log.e(TAG, "speakerResourceList isEmpty !");

        mLocalTtsConfig.setAssetsResNames(resourceInAssetsList.toArray(new String[resourceInAssetsList.size()]));
        mLocalTtsConfig.setAssetsResMd5sum(config.getSpeakerResourceMD5Map());
        TTSCache.getInstanceLocal().setUseCache(useCache);
        TTSCache.getInstanceLocal().setCacheSize(config.getCacheSize());
        TTSCache.getInstanceLocal().setCacheWordCount(config.getCacheWordCount());

        mLocalTtsConfig.setUseStopCallback(config.isUseStopCallback());
        if (config.getCustomAudioList() != null) {
            TTSCache.getInstanceLocal().setCustomAudioList(config.getCustomAudioList());
        }
    }

    /**
     * 合成并播放
     *
     * @param aiLocalTTSIntent 参数
     * @param refText          合成文本
     * @param utteranceId      utteranceId
     */
    public void speak(AILocalTTSIntent aiLocalTTSIntent, String refText, String utteranceId) {
        Log.i(TAG, "speak:" + refText);
        parseIntent(aiLocalTTSIntent);
        mUtteranceId = utteranceId;
        mParams.setRefText(refText);
        mParams.setIsAutoPlay(true);
        mParams.setOutRealData(false);
        mParams.setSleepTime(aiLocalTTSIntent.getSleepTime());
        mLocalTtsProcessor.start(mParams);
    }


    /**
     * 暂停播放
     */
    public void pause() {
        Log.i(TAG, "pause");
        if (mLocalTtsProcessor != null) {
            mLocalTtsProcessor.pause();
        }
    }

    /**
     * 继续播放
     */
    public void resume() {
        Log.i(TAG, "resume");
        if (mLocalTtsProcessor != null) {
            mLocalTtsProcessor.resume();
        }
    }

    /**
     * 停止合成和播放
     */
    public void stop() {
        super.stop();
        if (mLocalTtsProcessor != null) {
            mLocalTtsProcessor.stop();
        }
    }

    /*
     * 删除TTS本地资源方法 ，可以在onInit回调失败时调用，一般是相关bin资源在assets下时调用，
     * 若是外置目录需注意需要重新copy进去。
     *
     * 1. 若相关bin资源是放置在assets目录下，则删除为：
     *  setFrontResBin
     *  setBackResBinArray
     *  setDictDb
     *  方法设置的资源(路径：data/data/包名/files/)
     *
     * 2. 若相关bin资源放置在外部目录下，则删除为：
     *  setFrontResBinPath
     *  setDictDbPath
     *  setBackResBinPath
     *
     */
    public void deleteLocalResFile() {
        Log.i(TAG, "deleteLocalResFile");
        if (mLocalTtsProcessor != null) {
            mLocalTtsProcessor.deleteFile();
        }
    }


    /**
     * 只合成，不播放，同时抛出实时合成音频流
     *
     * @param aiLocalTTSIntent 参数
     * @param text             合成文本
     * @param utteranceId      utteranceId
     */
    public void synthesize(AILocalTTSIntent aiLocalTTSIntent, String text, String utteranceId) {
        Log.i(TAG, "synthesize:" + text);
        parseIntent(aiLocalTTSIntent);
        mUtteranceId = utteranceId;
        mParams.setRefText(text);
        mParams.setIsAutoPlay(false);
        mParams.setOutRealData(true);
        mParams.setSleepTime(aiLocalTTSIntent.getSleepTime());
        mLocalTtsProcessor.start(mParams);
    }

    @SuppressLint("WrongConstant")
    private void parseIntent(AILocalTTSIntent aiLocalTTSIntent) {
        if (aiLocalTTSIntent == null) {
            Log.e(TAG, "AILocalTTSIntent is null");
            return;
        }
        Log.i(TAG, "AILocalTTSIntent " + aiLocalTTSIntent);

        if (aiLocalTTSIntent.getSpeed() > 0) {
            mParams.setSpeed(aiLocalTTSIntent.getSpeed());
        }
        if (aiLocalTTSIntent.getLmargin() >= 5 && aiLocalTTSIntent.getLmargin() <= 20) {
            mParams.setLmargin(aiLocalTTSIntent.getLmargin());
        } else {
            Log.d(TAG, " Lmargin invalid parameter");
        }

        if (aiLocalTTSIntent.getRmargin() >= 5 && aiLocalTTSIntent.getRmargin() <= 20) {
            mParams.setRmargin(aiLocalTTSIntent.getRmargin());
        } else {
            Log.d(TAG, " Rmargin invalid parameter");
        }
        if (aiLocalTTSIntent.getVolume() > 0) {
            mParams.setVolume(aiLocalTTSIntent.getVolume());
        }
        /**
         * 兼容synthesizeToFile 接口，
         *  saveAudioFilePath 默认值是“”，导致aiLocalTTSIntent.getSaveAudioFilePath()!= null 一直为true
         *  导致如果没有在intet中设置saveAudioFilePath，
         *  但是synthesizeToFile中设置了路径，
         *  就导致音频无法保存在synthesizeToFile设置的文件中
         *  如果aiLocalTTSIntent中设置，则以intent中设置的文件名称为准
         */
        if (!TextUtils.isEmpty(aiLocalTTSIntent.getSaveAudioFilePath())) {
            mParams.setSaveAudioFileName(aiLocalTTSIntent.getSaveAudioFilePath());
        }
        if (aiLocalTTSIntent.getAudioAttributesUsage() > 0 &&
                aiLocalTTSIntent.getAudioAttributesContentType() > 0 &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(aiLocalTTSIntent.getAudioAttributesUsage())
                    .setContentType(aiLocalTTSIntent.getAudioAttributesContentType())
                    .build();
            mParams.setAudioAttributes(audioAttributes);
        }

        if (aiLocalTTSIntent.getAudioAttributes() != null) {
            mParams.setAudioAttributes(aiLocalTTSIntent.getAudioAttributes());
        }
        if (aiLocalTTSIntent.getStreamType() > 0) {
            mParams.setStreamType(aiLocalTTSIntent.getStreamType());
        }

        mParams.setUseStreamType(aiLocalTTSIntent.isUseStreamType());
        mParams.setUseSSML(aiLocalTTSIntent.isUseSSML());
        mParams.setUseTimeStamp(aiLocalTTSIntent.getUseTimeStamp());

        int language = aiLocalTTSIntent.getLanguage();
        // 切换发音人
        String speakerResource = aiLocalTTSIntent.getSpeakerResource();
        if (!TextUtils.isEmpty(speakerResource) && mLocalTtsProcessor != null) {
            String speakerResourcePath;
            boolean isCopy = false;
            if (speakerResource.startsWith("/")) {
                speakerResourcePath = speakerResource;
            } else {
                speakerResourcePath = Util.getResourceDir(mLocalTtsConfig.getContext()) + File.separator + speakerResource;
                isCopy = true;
            }
            Log.d(TAG, "parseIntent:speakerResource " + speakerResource);
            Log.d(TAG, "parseConfig:backBinPath is  " + mLocalTtsConfig.getBackBinPath());
            if (speakerResourcePath.equals(mLocalTtsConfig.getBackBinPath()) && language == mLocalTtsConfig.getLanguage()) {
                Log.d(TAG, "back bin not change return !");
            } else {
                if (isCopy) Util.copyResource(mLocalTtsConfig.getContext(), speakerResource);
                String dynamic = TTSDynamicParamUtils.getTtsDynamicParam(speakerResourcePath, language, mLocalTtsConfig);
                mLocalTtsConfig.setBackBinPath(speakerResourcePath);
                mLocalTtsConfig.setLanguage(language);
                mLocalTtsProcessor.set(dynamic);
            }
        }

        mParams.setSampleRate(aiLocalTTSIntent.getPlaySampleRate());
    }

    /**
     * 合成音频到某个文件
     *
     * @param text        合成文本
     * @param fileName    保存的合成音频文件，包含路径
     * @param utteranceId utteranceId
     * @param intent      合成前可动态配置的参数实体类
     */
    public void synthesizeToFile(String text, String fileName, String utteranceId, AILocalTTSIntent intent) {
        Log.i(TAG, "synthesizeToFile:" + text);
        mParams.setRefText(text);
        mParams.setIsAutoPlay(false);
        mParams.setOutRealData(false);
        mParams.setSaveAudioFileName(fileName);
        mParams.setUtteranceId(utteranceId);
        mUtteranceId = utteranceId;
        parseIntent(intent);
        mLocalTtsProcessor.start(mParams);
    }

    /**
     * 设置本地后端合成音色资源路径
     *
     * @param backBinPath 后端合成音色资源路径
     * @deprecated
     */
    public void setBackResBinPath(String backBinPath) {
        String dynamic = TTSDynamicParamUtils.getTtsDynamicParam(backBinPath, mLocalTtsConfig.getLanguage(), mLocalTtsConfig);
        mLocalTtsProcessor.set(dynamic);
    }

    /**
     * 销毁合成引擎
     */
    public synchronized void destroy() {
        super.destroy();
        if (ref.decrementAndGet() == 0) {
            if (mLocalTtsProcessor != null) {
                mLocalTtsProcessor.release();
                mLocalTtsProcessor = null;
            }
            if (mListenerImpl != null) {
                mListenerImpl.release();
                mListenerImpl = null;
            }
        } else {
            if (mListenerImpl != null) {
                mListenerImpl.release();
            }
        }
    }

    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
        TTSCache.getInstanceLocal().setUseCache(useCache);
    }

    /**
     * 是否使用了缓存功能
     *
     * @return true 使用，false 未使用
     */
    public boolean isUseCache() {
        return TTSCache.getInstanceLocal().isUseCache();
    }

    /**
     * The adapter for convert SpeechListener to AILocalTTSListener.
     */
    private class SynthesizerListenerImpl implements SynthesizerListener, AIPlayerListener {

        AILocalTTSListener localListener;
        AITTSListener ttsListener;

        public SynthesizerListenerImpl(AILocalTTSListener listener) {
            localListener = listener;
        }

        public void setListener(AILocalTTSListener listener) {
            localListener = listener;
        }

        public void setListener(AITTSListener listener) {
            ttsListener = listener;
        }

        @Override
        public void onInit(int status) {
            if (ttsListener != null) {
                ttsListener.onInit(status);
            } else if (localListener != null) {
                localListener.onInit(status);
            }
        }

        @Override
        public void onSpeechStart() {
            try {
                JSONObject customObj = new JSONObject().put(IInterceptor.Name.LOCAL_TTS_PLAY_FIRST, "");
                JSONObject inputObj = TTSInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, customObj);
                SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.LOCAL_TTS_PLAY_FIRST, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (ttsListener != null) {
                ttsListener.onReady(mUtteranceId);
            } else if (localListener != null) {
                localListener.onSpeechStart(mUtteranceId);
            }
        }

        @Override
        public void onSpeechFinish() {
            if (ttsListener != null) {
                ttsListener.onCompletion(mUtteranceId);
            } else if (localListener != null) {
                localListener.onSpeechFinish(mUtteranceId);
            }
        }

        @Override
        public void onError(AIError error) {
            if (ttsListener != null) {
                ttsListener.onError(mUtteranceId, error);
            } else if (localListener != null) {
                localListener.onError(mUtteranceId, error);
            }
        }

        @Override
        public void onSpeechProgress(int currentTime, int totalTime, boolean isDataReady) {
            if (ttsListener != null) {
                ttsListener.onProgress(currentTime, totalTime, isDataReady);
            } else if (localListener != null) {
                localListener.onSpeechProgress(currentTime, totalTime, isDataReady);
            }
        }

        @Override
        public void onProgress(int currentTime, int totalTime, boolean isRefTextTTSFinished) {

        }


        @Override
        public void onSynthesizeStart() {
            try {
                JSONObject customObj = new JSONObject().put(IInterceptor.Name.LOCAL_TTS_SYNTHESIS_FIRST, mUtteranceId);
                JSONObject inputObj = TTSInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, customObj);
                SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.LOCAL_TTS_SYNTHESIS_FIRST, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (ttsListener != null) {
                ttsListener.onSynthesizeStart(mUtteranceId);
            } else if (localListener != null) {
                localListener.onSynthesizeStart(mUtteranceId);
            }
        }

        @Override
        public void onSynthesizeDataArrived(byte[] audioData) {
            if (ttsListener != null) {
                ttsListener.onSynthesizeDataArrived(mUtteranceId, audioData);
            } else if (localListener != null) {
                localListener.onSynthesizeDataArrived(mUtteranceId, audioData);
            }
        }

        @Override
        public void onSynthesizeFinish() {
            try {
                JSONObject customObj = new JSONObject().put(IInterceptor.Name.LOCAL_TTS_SYNTHESIS_END, mUtteranceId);
                JSONObject inputObj = TTSInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, customObj);
                SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.LOCAL_TTS_SYNTHESIS_END, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (ttsListener != null) {
                ttsListener.onSynthesizeFinish(mUtteranceId);
            } else if (localListener != null) {
                localListener.onSynthesizeFinish(mUtteranceId);
            }
        }

        @Override
        public void onTimestampReceived(byte[] timeStampJson, int size) {
            if (localListener != null) {
                localListener.onTimestampReceived(timeStampJson, size);
            }
        }

        @Override
        public void onPhonemesDataArrived(String phonemes) {
        }

        @Override
        public void onEmotion(String emotion, String emotionOrigin) {

        }

        @Override
        public void onReady() {

        }

        @Override
        public void onPaused() {

        }

        @Override
        public void onStopped() {

        }

        @Override
        public void onResumed() {

        }

        @Override
        public void onCompletion(long sessionId) {

        }

        public void release() {
            localListener = null;
            ttsListener = null;
        }
    }

}
