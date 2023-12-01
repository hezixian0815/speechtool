package com.aispeech.export.engines2;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.OneshotWordsUtils;
import com.aispeech.common.Util;
import com.aispeech.export.Vocab;
import com.aispeech.export.config.AILocalASRConfig;
import com.aispeech.export.engines2.bean.Decoder;
import com.aispeech.export.intent.AILocalASRIntent;
import com.aispeech.export.interceptor.AsrInterceptor;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AIUpdateListener;
import com.aispeech.kernel.Asr;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Vad;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.asr.LocalAsrProcessor;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalAsrConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.LocalAsrParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地识别
 */
public class AILocalASREngine extends BaseEngine {
    public static final String TAG = "AILocalASREngine";
    private LocalAsrProcessor mLocalAsrProcessor;
    private LocalVadConfig mVadConfig;
    private LocalAsrConfig mAsrConfig;
    private VadParams mVadParams;
    private LocalAsrParams mAsrParams;
    SpeechListenerImpl mSpeechListener;

    private AILocalASREngine() {
        mLocalAsrProcessor = new LocalAsrProcessor();
        mBaseProcessor = mLocalAsrProcessor;
        mVadConfig = new LocalVadConfig();
        mAsrConfig = new LocalAsrConfig();
        mVadParams = new VadParams();
        mAsrParams = new LocalAsrParams();
        mSpeechListener = new SpeechListenerImpl(null);
        // 词库更新 ngram会耗时阻塞，使用单独线程处理
        mLocalAsrProcessor.setUseSingleMessageProcess(false);
    }

    @Override
    public String getTag() {
        return "local_asr";
    }

    public static AILocalASREngine createInstance() {
        return new AILocalASREngine();
    }

    private static boolean checkLibValid() {
        return Asr.isAsrSoValid() && Vad.isSoValid() && Utils.isUtilsSoValid();
    }

    /**
     * 告知识别引擎已经唤醒，该接口在oneshot功能中使用，内部会记录唤醒的时间点，
     * 之后在vad end的时候来判断到底用户说的是不是唤醒词+指令，还是只有唤醒词
     * <p> 请参考 oneshot demo 中的使用方法 </p>
     */
    public void notifyWakeup() {
        mAsrParams.setWakeupTime(System.currentTimeMillis());
    }

    /**
     * 更新编译 xbnf 后的 netBin
     *
     * @param listener   更新接口回调 {@link AIUpdateListener}
     * @param netBinPath net.bin 资源自定义路径
     */
    public void updateNetBinPath(String netBinPath, AIUpdateListener listener) {
        if (TextUtils.isEmpty(netBinPath)) {
            Log.e(TAG, "illegal net.bin path");
            listener.failed();
        } else {
            if (mSpeechListener == null)
                throw new IllegalStateException(" init engine first ");
            // resource in assets
            final List<String> resourceInAssetsList = new ArrayList<>();
            if (netBinPath.startsWith("/")) {
                mAsrConfig.setNetBinPath(netBinPath);
            } else {
                resourceInAssetsList.add(netBinPath);
                mAsrConfig.setNetBinPath(Util.getResourceDir(mAsrConfig.getContext()) + File.separator + netBinPath);
            }
            mAsrConfig.setAssetsResNames(resourceInAssetsList.toArray(new String[resourceInAssetsList.size()]));
            mSpeechListener.setUpdateListener(listener);
            if (mLocalAsrProcessor != null) {
                mLocalAsrProcessor.update(mAsrConfig);
            }
        }
    }

    /**
     * 初始化本地识别引擎
     *
     * @param config   配置信息
     * @param listener 回调接口
     */
    public void init(AILocalASRConfig config, AIASRListener listener) {
        if (!checkLibValid()) {
            if (listener != null) {
                listener.onInit(AIConstant.OPT_FAILED);
                listener.onError(new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }
        super.init();
        parseConfig(config);
        mSpeechListener.setListener(listener);
        mLocalAsrProcessor.init(mSpeechListener, mAsrConfig, mVadConfig);
    }



    private void parseConfig(AILocalASRConfig config) {
        if (config == null) {
            Log.e(TAG, "AILocalASRConfig is null !");
            return;
        }
        Log.d(TAG, "AILocalASRConfig " + config);

        super.parseConfig(config, mAsrConfig, mVadConfig);
        mAsrConfig.setVadEnable(config.isVadEnable());
        mVadConfig.setVadEnable(config.isVadEnable());
        mVadParams.setVadEnable(config.isVadEnable());

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

        // resource in assets
        final List<String> resourceInAssetsList = new ArrayList<>();

        // acousticResources 声学资源
        final String acousticResources = config.getAcousticResources();
        if (TextUtils.isEmpty(acousticResources)) {
            Log.e(TAG, "acousticResources not found !!");
        } else if (acousticResources.startsWith("/")) {
            mAsrConfig.setResBinPath(acousticResources);
        } else {
            resourceInAssetsList.add(acousticResources);
            mAsrConfig.setResBinPath(Util.getResourceDir(mAsrConfig.getContext()) + File.separator + acousticResources);
        }

        // netbinResource
        final String netbinResource = config.getNetbinResource();
        if (TextUtils.isEmpty(netbinResource)) {
            Log.e(TAG, "netbinResource not found !!");
        } else if (netbinResource.startsWith("/")) {
            mAsrConfig.setNetBinPath(netbinResource);
        } else {
            resourceInAssetsList.add(netbinResource);
            mAsrConfig.setNetBinPath(Util.getResourceDir(mAsrConfig.getContext()) + File.separator + netbinResource);
        }

        final String ngramSlotRes = config.getNgramSlotRes();
        if (TextUtils.isEmpty(ngramSlotRes)) {
            Log.e(TAG, "ngramSlotRes not found !!");
        } else if (ngramSlotRes.startsWith("/")) {
            mAsrConfig.setNgramSlotBinPath(ngramSlotRes);
        } else {
            resourceInAssetsList.add(ngramSlotRes);
            mAsrConfig.setNgramSlotBinPath(Util.getResourceDir(mAsrConfig.getContext()) + File.separator + ngramSlotRes);
        }

        mAsrConfig.setUseAggregateMate(config.isUseAggregateMate());
        mAsrConfig.setUseItn(config.isUseItn());


        if (config.isUseItn()) {
            // 确认itn下numlex/lua资源目录
            if (config.getItnLuaResFolderName().startsWith("/")) {
                mAsrConfig.setItnLuaResFolderName(config.getItnLuaResFolderName());
            } else {
                mAsrConfig.setItnLuaResFolderName(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.getItnLuaResFolderName());
                mAsrConfig.setResFolderName(config.getItnLuaResFolderName());
            }
            // 确认num.bin资源路径，代表开启文本转数字功能
            if (!TextUtils.isEmpty(config.getNumBinPath())) {
                if (config.getNumBinPath().startsWith("/")) {
                    mAsrConfig.setNumBinPath(config.getNumBinPath());
                } else {
                    mAsrConfig.setNumBinPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.getNumBinPath());
                    // 如num.bin在getItnResFolderName()指定文件夹内，不需要重复copy
                    if (!config.getNumBinPath().contains(config.getItnLuaResFolderName())) {
                        resourceInAssetsList.add(config.getNumBinPath());
                    }
                }
            }
            // 是否开启首字母大写
            mAsrConfig.setItnUpperCase(config.isItnUpperCase());
        }

        if (config.isUseAggregateMate()) {
            // 确认itn下numlex/lua资源目录
            if (config.getItnLuaResFolderName().startsWith("/")) {
                mAsrConfig.setItnLuaResFolderName(config.getItnLuaResFolderName());
            } else {
                mAsrConfig.setItnLuaResFolderName(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.getItnLuaResFolderName());
                mAsrConfig.setResFolderName(config.getItnLuaResFolderName());
            }
            if (!TextUtils.isEmpty(config.getAggregateMateBinPath())) {
                if (config.getAggregateMateBinPath().startsWith("/")) {
                    mAsrConfig.setAggregateMateBinPath(config.getAggregateMateBinPath());
                } else {
                    mAsrConfig.setAggregateMateBinPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.getAggregateMateBinPath());
                    resourceInAssetsList.add(config.getAggregateMateBinPath());
                }
            }
            if (!TextUtils.isEmpty(config.getAggregateMateCommandBinPath())) {
                if (config.getAggregateMateCommandBinPath().startsWith("/")) {
                    mAsrConfig.setAggregateMateCommandBinPath(config.getAggregateMateCommandBinPath());
                } else {
                    mAsrConfig.setAggregateMateCommandBinPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.getAggregateMateCommandBinPath());
                    resourceInAssetsList.add(config.getAggregateMateCommandBinPath());
                }
            }
            // 是否开启首字母大写
            mAsrConfig.setItnUpperCase(config.isItnUpperCase());
        }

        mAsrConfig.setEnableDoubleVad(config.isEnableDoubleVad());
        mVadConfig.setUseDoubleVad(config.isEnableDoubleVad());
        mAsrConfig.setAssetsResNames(resourceInAssetsList.toArray(new String[resourceInAssetsList.size()]));
        if (config.getLanguages() != null) {
            mAsrConfig.setScope(config.getLanguages().getLanguage());
        }
    }

    private void parseIntent(AILocalASRIntent aiLocalASRIntent) {
        if (aiLocalASRIntent == null) {
            Log.e(TAG, "AILocalASRIntent is null !");
            return;
        }
        super.parseIntent(aiLocalASRIntent, mAsrParams, mVadParams);
        Log.d(TAG, "AILocalASRIntent " + aiLocalASRIntent);
        if (aiLocalASRIntent.getUseDelimiter() != null) {
            mAsrParams.setUseWrdSep(aiLocalASRIntent.getUseDelimiter());
        }
        // fix https://jira.aispeech.com.cn/browse/VEHICLEDUILFC-191
        // 兼容assets路径文件，将文件判断及复制动作放到progress中操作
//        if (aiLocalASRIntent.getExpandFnPath() != null) {
//            File f = new File(aiLocalASRIntent.getExpandFnPath());
//            if (!f.exists() || f.isDirectory()) {
//                throw new IllegalArgumentException("illegal ExpandFn path .");
//            }
        mAsrParams.setExpandFnPath(aiLocalASRIntent.getExpandFnPath());
//        }

        mAsrParams.setUseThreshold(aiLocalASRIntent.getThreshold());
        mAsrParams.setIsIgnoreThreshold(aiLocalASRIntent.getIsIgnoreThreshold());
        mAsrParams.setNoSpeechTimeout(aiLocalASRIntent.getNoSpeechTimeOut());
        mVadParams.setNoSpeechTimeout(aiLocalASRIntent.getNoSpeechTimeOut());

        mAsrParams.setMaxSpeechTimeS(aiLocalASRIntent.getMaxSpeechTimeS());
        mVadParams.setMaxSpeechTimeS(aiLocalASRIntent.getMaxSpeechTimeS());

        mAsrParams.setSaveAudioPath(aiLocalASRIntent.getSaveAudioPath());
        mAsrParams.setOneShotIntervalTime(aiLocalASRIntent.getIntervalTimeThresh());
        mAsrParams.setUseOneShotFunction(aiLocalASRIntent.isUseOneShot());
        mAsrParams.setUseConf(aiLocalASRIntent.isUseConf());

        mAsrParams.setUseXbnfRec(aiLocalASRIntent.isUseXbnfRec());
        mAsrParams.setUseRealBack(aiLocalASRIntent.isUseRealBack());
        mAsrParams.setUseHoldConf(aiLocalASRIntent.isUseHoldConf());
        mAsrParams.setUsePinyin(aiLocalASRIntent.isUsePinyin());
        mAsrParams.setDynamicList(aiLocalASRIntent.getDynamicList());
        mAsrParams.setFespxEngine(aiLocalASRIntent.getFespxEngine());

        mVadConfig.setPauseTime(aiLocalASRIntent.getPauseTime());
        mVadParams.setPauseTime(aiLocalASRIntent.getPauseTime());

        mAsrParams.setUseCustomFeed(aiLocalASRIntent.isUseCustomFeed());
        mVadConfig.setUseCustomFeed(aiLocalASRIntent.isUseCustomFeed());

        mAsrParams.setUseFiller(aiLocalASRIntent.isUseFiller());
        mAsrParams.setFillerPenaltyScore(aiLocalASRIntent.getFillerPenaltyScore());
        mAsrParams.setCustomThresholdMap(aiLocalASRIntent.getCustomThreshold());
        mAsrParams.setUseDynamicBackList(aiLocalASRIntent.getBlackWords());
        mAsrParams.setMode(aiLocalASRIntent.getMode().getValue());
        mAsrParams.setUseRawRec(aiLocalASRIntent.isUseRawRec());
        mAsrParams.setUseOneshotJson(aiLocalASRIntent.isUseOneshotJson());
        mAsrParams.setUseE2EFusion(aiLocalASRIntent.isUseE2EFusion());
        mAsrParams.setNgramConf(aiLocalASRIntent.getNgramConf());
        mAsrParams.setEnv(aiLocalASRIntent.getEnv());
        // 设置为 null 就用 config 的设置
        if (mVadConfig.isVadEnable())
            mVadParams.setVadEnable(aiLocalASRIntent.getVadEnable() == null ? mVadConfig.isVadEnable() : aiLocalASRIntent.getVadEnable());
        else
            Log.d(TAG, "mVadConfig.isVadEnable() is false, ignore set aiLocalASRIntent.getVadEnable()");

        if (!TextUtils.isEmpty(aiLocalASRIntent.getActiveDecoders())) {
            mAsrParams.setActiveDecoderList(aiLocalASRIntent.getActiveDecoders());
        }
        setWakeupWords(aiLocalASRIntent.getWakeupWords());
    }


    /**
     * 设置唤醒词列表，用于oneshot过滤唤醒词
     *
     * @param wakeupWords 唤醒词列表
     */
    public void setWakeupWords(String[] wakeupWords) {
        Log.i(TAG, "setWakeupWords:" + wakeupWords);
        if (wakeupWords != null && wakeupWords.length > 0) {
            mAsrParams.setWakeupWords(wakeupWords);
            //内核接口要求传入文件路径，此处将唤醒词信息写入文件并转换成文件路径传入
            String wakeupFilePath = AISpeech.getContext().getExternalCacheDir() + File.separator +
                    "wakeupWords.json";
            OneshotWordsUtils.writeWakeupWordsToFile(wakeupWords, wakeupFilePath);
            mAsrParams.setWakeupWordsFilePath(wakeupFilePath);
        }
    }

    /**
     * 启动录音，开始语音识别
     *
     * @param aiLocalASRIntent 参数
     */
    public void start(AILocalASRIntent aiLocalASRIntent) {
        super.start();
        parseIntent(aiLocalASRIntent);
        if (mLocalAsrProcessor != null) {
            mLocalAsrProcessor.start(mAsrParams, mVadParams);
        }
    }

    /**
     * 传入数据,在不使用SDK内部录音机时调用
     *
     * @param data 音频数据流
     * @see com.aispeech.export.intent.AILocalASRIntent#setUseCustomFeed(boolean)
     */
    public void feed(byte[] data) {
        if (mAsrParams != null && !mAsrParams.isUseCustomFeed())
            return;
        if (mLocalAsrProcessor != null) {
            if (data == null) {
                Log.e(TAG, "custom feed data is null !");
                return;
            }
            mLocalAsrProcessor.feedData(data, data.length);
        }
    }

    /**
     * 传入数据,在不使用SDK内部录音机时调用
     *
     * @param data 音频数据流
     * @param size 数据大小
     * @see com.aispeech.export.intent.AILocalASRIntent#setUseCustomFeed(boolean)
     */
    public void feedData(byte[] data, int size) {
        if (mAsrParams != null && !mAsrParams.isUseCustomFeed())
            return;
        if (mLocalAsrProcessor != null) {
            mLocalAsrProcessor.feedData(data, size);
        }
    }

    /**
     * 停止录音，等待识别结果
     */
    public void stop() {
        super.stop();
        if (mLocalAsrProcessor != null) {
            mLocalAsrProcessor.stop();
        }
    }

    /**
     * 取消本次识别操作
     */
    public void cancel() {
        super.cancel();
        if (mLocalAsrProcessor != null) {
            mLocalAsrProcessor.cancel();
        }
    }

    /**
     * 销毁本地识别引擎
     */
    public void destroy() {
        super.destroy();
        if (mLocalAsrProcessor != null) {
            mLocalAsrProcessor.release();
        }
        if (mSpeechListener != null) {
            mSpeechListener.setListener(null);
            mSpeechListener = null;
        }
    }


    public void updateVocab(Vocab vocab, AIUpdateListener updateListener) {
        Log.in(TAG, "updateVocab");
        updateVocabs(updateListener, vocab);
    }

    /**
     * 更新单个或者多个词库
     *
     * @param updateListener 更新结果回调
     * @param vocabs         词库内容
     */

    public void updateVocabs(AIUpdateListener updateListener, Vocab... vocabs) {
        Log.in(TAG, "updateVocabs");
        mSpeechListener.setUpdateListener(updateListener);
        if (mLocalAsrProcessor != null) {
            mLocalAsrProcessor.updateVocab(vocabs);
        }
    }

    public void updateDecoder(AIUpdateListener updateListener, Decoder... decoders) {
        Log.in(TAG, "updateDecoder");
        mSpeechListener.setUpdateListener(updateListener);
        if (mLocalAsrProcessor != null) {
            mLocalAsrProcessor.decoder(decoders);
        }
    }

    /**
     * The adapter for convert SpeechListener to AIASRListener.
     */
    private class SpeechListenerImpl extends SpeechListener {
        AIASRListener mListener;
        private AIUpdateListener mUpdateListener;

        public void setUpdateListener(AIUpdateListener mUpdateListener) {
            this.mUpdateListener = mUpdateListener;
        }

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
        public void onResults(AIResult result) {
            try {
                JSONObject customObj = new JSONObject().put(IInterceptor.Name.LOCAL_ASR_RESULT, result);
                JSONObject inputObj = AsrInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, customObj);
                SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.LOCAL_ASR_RESULT, inputObj);
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
        public void onVprintCutDataReceived(int dataType, byte[] data, int size) {
            //do nothing
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

        @Override
        public void onUpdateResult(int ret) {
            if (mUpdateListener == null) {
                return;
            }
            if (ret == AIConstant.OPT_SUCCESS) {
                mUpdateListener.success();
            } else {
                mUpdateListener.failed();
            }
        }
    }
}
