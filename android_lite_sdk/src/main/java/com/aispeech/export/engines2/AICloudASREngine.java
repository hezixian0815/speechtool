package com.aispeech.export.engines2;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.config.AICloudASRConfig;
import com.aispeech.export.intent.AICloudASRIntent;
import com.aispeech.export.interceptor.AsrInterceptor;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.kernel.Opus;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Vad;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.asr.AsrProcessor;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.CloudAsrConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.CloudASRParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;


/**
 * 云端识别引擎
 */
public class AICloudASREngine extends BaseEngine {

    AsrProcessor mCloudAsrProcessor;
    CloudAsrConfig mCloudAsrConfig;
    CloudASRParams mCloudASRParams;

    LocalVadConfig mLocalVadConfig;

    VadParams mVadParams;

    SpeechListenerImpl mSpeechListener;


    private AICloudASREngine() {
        mCloudAsrProcessor = new AsrProcessor();
        mCloudAsrConfig = new CloudAsrConfig();
        mCloudASRParams = new CloudASRParams("", "", "");
        mLocalVadConfig = new LocalVadConfig();
        mVadParams = new VadParams();
        mSpeechListener = new SpeechListenerImpl(null);
        mBaseProcessor = mCloudAsrProcessor;
    }

    @Override
    public String getTag() {
        return "cloud_asr";
    }

    /**
     * 创建实例
     *
     * @return AICloudASREngine实例
     */
    public static AICloudASREngine createInstance() {
        return new AICloudASREngine();
    }


    private static boolean checkLibValid() {
        return Vad.isSoValid() && Utils.isUtilsSoValid() && Opus.isSoValid();
    }

    /**
     * 初始化云端识别引擎
     *
     * @param config   配置
     * @param listener 回调接口
     */
    public void init(AICloudASRConfig config, final AIASRListener listener) {
        super.init();
        if (!checkLibValid()) {
            if (listener != null) {
                listener.onInit(AIConstant.OPT_FAILED);
                listener.onError(new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }
        parseConfig(config);
        mSpeechListener.setListener(listener);
        mCloudAsrProcessor.init(mSpeechListener, mCloudAsrConfig, mLocalVadConfig,
                config.isUseVprint() ? AsrProcessor.MODEL_CLOUD_PLUS : AsrProcessor.MODEL_CLOUD);
    }

    private void parseConfig(AICloudASRConfig config) {
        if (config == null) {
            Log.e(TAG, "AICloudASRConfig is null !");
            return;
        }
        super.parseConfig(config, mCloudAsrConfig);
        Log.d(TAG, "AICloudASRConfig " + config);

        mLocalVadConfig.setVadEnable(config.isLocalVadEnable());
        mVadParams.setVadEnable(config.isLocalVadEnable());
        mCloudAsrConfig.setMaxMessageQueueSize(config.getMaxMessageQueueSize());
        mCloudAsrConfig.setEnableDoubleVad(config.isEnableDoubleVad());

        // vadResource
        final String vadResource = config.getVadResource();
        if (TextUtils.isEmpty(vadResource)) {
            Log.e(TAG, "vad res not found !!");
        } else if (vadResource.startsWith("/")) {
            mLocalVadConfig.setResBinPath(vadResource);
        } else {
            mLocalVadConfig.setAssetsResNames(new String[]{vadResource});
            mLocalVadConfig.setResBinPath(Util.getResourceDir(mLocalVadConfig.getContext()) + File.separator + vadResource);
        }
    }

    private void parseIntent2(AICloudASRIntent mAICloudASRIntent) {
        mCloudASRParams.setServer(mAICloudASRIntent.getServer());

        if (!TextUtils.isEmpty(mAICloudASRIntent.getUserId())) {
            mCloudASRParams.setUserId(mAICloudASRIntent.getUserId());
        }

        if (!TextUtils.isEmpty(mAICloudASRIntent.getLmId()))
            mCloudASRParams.setLmId(mAICloudASRIntent.getLmId());


        mCloudASRParams.setEnablePunctuation(mAICloudASRIntent.isEnablePunctuation());
        mCloudASRParams.setEnableNumberConvert(mAICloudASRIntent.isEnableNumberConvert());
        mCloudASRParams.setSelfCustomWakeupScore(mAICloudASRIntent.getSelfCustomWakeupScore());
        mCloudASRParams.setCustomWakeupWord(mAICloudASRIntent.getCustomWakeupWord());
        mCloudASRParams.setWakeupWord(mAICloudASRIntent.getWakeupWord());
        mCloudASRParams.setCommonWakeupWord(mAICloudASRIntent.getCommonWakeupWord());
        mCloudASRParams.setVisibleWakeupWord(mAICloudASRIntent.isWakeupWordFilter());
        mCloudASRParams.setEnableTone(mAICloudASRIntent.isEnableTone());
        mCloudASRParams.setEnableLanguageClassifier(mAICloudASRIntent.isEnableLanguageClassifier());
        mCloudASRParams.setEnableSNTime(mAICloudASRIntent.isEnableSNTime());
        mCloudASRParams.setLanguage(mAICloudASRIntent.getLanguage());
        mCloudASRParams.setRes(mAICloudASRIntent.getResourceType());

        mCloudASRParams.setEnableRealTimeFeedBack(mAICloudASRIntent.isRealback());

        mCloudASRParams.setEnableVad(mAICloudASRIntent.isCloudVadEnable());

        mCloudASRParams.setUseCustomFeed(mAICloudASRIntent.isUseCustomFeed());
        mCloudASRParams.setEncodedAudio(mAICloudASRIntent.isEncodedAudio());
        mCloudASRParams.setNbest(mAICloudASRIntent.getNbest());

        mCloudASRParams.setOneshotOptimization(mAICloudASRIntent.isOneshotOptimization());
        mCloudASRParams.setOneshotOptimizationFilterWords(mAICloudASRIntent.getOneshotOptimizationFilterWords());
        //oneshot优化带空结果重试，不需要再做识别结果为空的重试
        mCloudASRParams.setIgnoreEmptyResult(!mAICloudASRIntent.isOneshotOptimization() && mAICloudASRIntent.isIgnoreEmptyResult());
        mCloudASRParams.setIgnoreEmptyResultCounts(mAICloudASRIntent.getIgnoreEmptyResultCounts());
        mCloudASRParams.setCustomWakeupScore(mAICloudASRIntent.getCustomWakeupScore());
        mCloudASRParams.setEnableAudioDetection(mAICloudASRIntent.isEnableAudioDetection());
        mCloudASRParams.setEnableAlignment(mAICloudASRIntent.isEnableAlignment());
        mCloudASRParams.setEnableSensitiveWdsNorm(mAICloudASRIntent.isEnableSensitiveWdsNorm());
        mCloudASRParams.setEnableRecUppercase(mAICloudASRIntent.isEnableRecUppercase());

        mCloudASRParams.setEnableFirstDec(mAICloudASRIntent.isEnableFirstDec());
        mCloudASRParams.setEnableFirstDecForce(mAICloudASRIntent.isEnableFirstDecForce());

        mCloudASRParams.setUseStrongWakeupVisible(mAICloudASRIntent.isUseStrongWakeupVisible());

        switch (mAICloudASRIntent.getAudioType()) {
            case OGG:
                mCloudASRParams.setAudioType(CloudASRParams.OGG);
                break;
            case OGG_OPUS:
                mCloudASRParams.setAudioType(CloudASRParams.OGG_OPUS);
                break;
            case WAV:
                mCloudASRParams.setAudioType(CloudASRParams.WAV);
                break;
            case MP3:
                mCloudASRParams.setAudioType(CloudASRParams.MP3);
                break;
            case OPUS:
                mCloudASRParams.setAudioType(CloudASRParams.OPUS);
                break;
        }
    }

    private void parseIntent(AICloudASRIntent aICloudASRIntent) {
        if (aICloudASRIntent == null) {
            Log.d(TAG, "AICloudASRIntent is null !");
            return;
        }
        super.parseIntent(aICloudASRIntent, mCloudASRParams);

        Log.d(TAG, "AICloudASRIntent " + aICloudASRIntent);
        if (aICloudASRIntent.getOneshotCache() != null) {
            mCloudASRParams.setOneshotCache(aICloudASRIntent.getOneshotCache());
        }
        mCloudASRParams.setUseDmAsr(aICloudASRIntent.isUseDmAsr());
        mCloudASRParams.setDmAsrJson(aICloudASRIntent.getDmAsrJson());
        mCloudASRParams.setNoSpeechTimeout(aICloudASRIntent.getNoSpeechTimeOut());
        mCloudASRParams.setMaxSpeechTimeS(aICloudASRIntent.getMaxSpeechTimeS());
        mVadParams.setSaveAudioPath(aICloudASRIntent.getSaveAudioPath());
        mCloudASRParams.setSaveAudioPath(aICloudASRIntent.getSaveAudioPath());
        mCloudASRParams.setSaveOriginalAudio(aICloudASRIntent.isSaveOriginalAudio());
        mCloudASRParams.setWaitingTimeout(aICloudASRIntent.getWaitingTimeout());
        mCloudASRParams.setOneShotIntervalTime(aICloudASRIntent.getIntervalTimeThresh());
        mCloudASRParams.setUseOneShotFunction(aICloudASRIntent.isUseOneShot());
        mCloudASRParams.setFespxEngine(aICloudASRIntent.getFespxEngine());
        mLocalVadConfig.setPauseTime(aICloudASRIntent.getPauseTime());
        mVadParams.setPauseTime(aICloudASRIntent.getPauseTime());

        mCloudASRParams.setEnableConfidence(aICloudASRIntent.isEnableConfidence());
        mCloudASRParams.setHotWords(aICloudASRIntent.getHotWords());
        // 设置为 null 就用 config 的设置
        if (mLocalVadConfig.isVadEnable())
            mVadParams.setVadEnable(aICloudASRIntent.getLocalVadEnable() == null ? mLocalVadConfig.isVadEnable() : aICloudASRIntent.getLocalVadEnable());
        else
            Log.d(TAG, "mLocalVadConfig.isVadEnable() is false, ignore set aICloudASRIntent.isLocalVadEnable()");

        parseIntent2(aICloudASRIntent);

        mCloudASRParams.setServerName(aICloudASRIntent.getServerName());
        mCloudASRParams.setUsers(aICloudASRIntent.getUsers());
        mCloudASRParams.setDomain(aICloudASRIntent.getDomain());
        mCloudASRParams.setContextId(aICloudASRIntent.getContextId());
        mCloudASRParams.setGroupId(aICloudASRIntent.getGroupId());
        mCloudASRParams.setOrganization(aICloudASRIntent.getOrganization());
        mCloudASRParams.setCloudVprintVadEnable(aICloudASRIntent.isCloudVprintVadEnable());
        mCloudASRParams.setMinSpeechLength(aICloudASRIntent.getMinSpeechLength());
        mCloudASRParams.setConstantContent(aICloudASRIntent.getConstantContent());
        mCloudASRParams.setGroupId(aICloudASRIntent.getGroupId());
        mCloudASRParams.setUseCustomFeed(aICloudASRIntent.isUseCustomFeed());
        mCloudASRParams.setPhraseList(aICloudASRIntent.getPhrasesList());

        mCloudASRParams.setEnableRealBackFastend(aICloudASRIntent.getEnableRealBackFastend());
        mCloudASRParams.setEnableRuleCorrect(aICloudASRIntent.getEnableRuleCorrect());
        mCloudASRParams.setEnableWordConfi(aICloudASRIntent.getEnableWordConfi());
        mCloudASRParams.setEnableNbestConfi(aICloudASRIntent.getEnableNbestConfi());
        mCloudASRParams.setEnableTxtSmooth(aICloudASRIntent.getEnableTxtSmooth());
        mCloudASRParams.setEnableSeparateNBest(aICloudASRIntent.getEnableSeparateNBest());

        // asr里新的参数可以直接加载这里
        Map<String, Object> extra = new HashMap<>();
        extra.put("enableEmotion", aICloudASRIntent.isEnableEmotion());
        extra.put("phraseHints", aICloudASRIntent.getPhraseHintsJsonArray());
        extra.put("lmList", aICloudASRIntent.getLmList());
        extra.put("enableDialectProcess", aICloudASRIntent.isEnableDialectProcess());
        extra.put("enableDialectProcessNlurec", aICloudASRIntent.isEnableDialectProcessNlurec());
        if (aICloudASRIntent.getExtraParam() != null)
            extra.putAll(aICloudASRIntent.getExtraParam());
        mCloudASRParams.setExtraParam(extra);
    }

    /**
     * 启动录音，开始语音识别
     *
     * @param aICloudASRIntent 参数
     */
    public void start(AICloudASRIntent aICloudASRIntent) {
        super.start();
        parseIntent(aICloudASRIntent);
        if (mCloudAsrProcessor != null) {
            mCloudAsrProcessor.start(mCloudASRParams, mVadParams);
        }
    }

    /**
     * 提供给实时长语音引擎使用
     *
     * @param lasrRealtimeParam    lasrRealtimeParam
     * @param lasrRes              lasrRes
     * @param lasrForwardAddresses lasrForwardAddresses
     */
    protected void setLasrInfo(String lasrRealtimeParam, String lasrRes, String lasrForwardAddresses) {
        mCloudASRParams.setLasr(true);
        mCloudASRParams.setLasrRealtimeParam(lasrRealtimeParam);
        mCloudASRParams.setLasrRes(lasrRes);
        mCloudASRParams.setLasrForwardAddresses(lasrForwardAddresses);
    }

    /**
     * 停止录音，等待识别结果
     */
    public void stop() {
        super.stop();
        if (mCloudAsrProcessor != null) {
            mCloudAsrProcessor.stop();
        }
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     * @param size 音频数据大小
     * @see AICloudASRIntent#setUseCustomFeed(boolean)
     */
    public void feedData(byte[] data, int size) {
        if (mCloudASRParams != null && !mCloudASRParams.isUseCustomFeed()) {
            Log.df(TAG, "useCustomFeed is not enabled，ignore data");
            return;
        }
        if (mCloudAsrProcessor != null) {
            mCloudAsrProcessor.feedData(data, size);
        }
    }

    /**
     * 取消本次识别操作
     */
    public void cancel() {
        super.cancel();
        if (mCloudAsrProcessor != null) {
            mCloudAsrProcessor.cancel();
        }
    }

    /**
     * 销毁云端识别引擎
     */
    public void destroy() {
        super.destroy();
        if (mCloudAsrProcessor != null) {
            mCloudAsrProcessor.release();
        }
        if (mSpeechListener != null)
            mSpeechListener.setListener(null);
    }


    /**
     * 告知识别引擎已经唤醒，该接口在oneshot功能中使用，内部会记录唤醒的时间点，
     * 之后在vad end的时候来判断到底用户说的是不是唤醒词+指令，还是只有唤醒词
     * <p> 请参考 oneshot demo 中的使用方法 </p>
     */
    public void notifyWakeup() {
        Log.i(TAG, "notifyWakeup");
        mCloudASRParams.setWakeupTime(System.currentTimeMillis());
    }

    /**
     * 获取当前的引擎状态
     *
     * @return EngineState
     */
    public BaseProcessor.EngineState getCurrentState() {
        return mCloudAsrProcessor != null ? mCloudAsrProcessor.getCurrentState() : null;
    }

    /**
     * The adapter for convert SpeechListener to AIASRListener.
     */
    private class SpeechListenerImpl extends SpeechListener {
        AIASRListener mListener;

        public SpeechListenerImpl(AIASRListener listener) {
            mListener = listener;
        }

        public void setListener(AIASRListener listener) {
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
        public void onDoaResult(int doa) {
            //do nothing
        }

        @Override
        public void onResults(AIResult result) {
            try {
                JSONObject customObj = new JSONObject().put(IInterceptor.Name.CLOUD_ASR_RESULT, result);
                JSONObject inputObj = AsrInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, customObj);
                SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.CLOUD_ASR_RESULT, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
         *
         * @deprecated 废弃
         */
        @Deprecated
        @Override
        public void onRecorderStopped() {
            //do nothing
        }


        @Override
        public void onEvent(int eventType, Map params) {
            //do nothing
        }

        @Override
        public void onNotOneShot() {
            if (mListener != null) {
                mListener.onNotOneShot();
            }
        }
    }


}
