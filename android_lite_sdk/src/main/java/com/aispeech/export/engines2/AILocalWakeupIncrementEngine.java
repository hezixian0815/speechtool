package com.aispeech.export.engines2;

import android.text.TextUtils;

import com.aispeech.AIResult;
import com.aispeech.base.BaseInnerEngine;
import com.aispeech.common.DynamicDecodeUtil;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.config.AILocalHotWordConfig;
import com.aispeech.export.config.AILocalWakeupIncrementConfig;
import com.aispeech.export.intent.AILocalWakeupIncrementIntent;
import com.aispeech.export.listeners.AILocalWakeupIncrementListener;
import com.aispeech.export.widget.Scene;
import com.aispeech.kernel.Asr;
import com.aispeech.kernel.Gram;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalAsrConfig;
import com.aispeech.lite.config.LocalGrammarConfig;
import com.aispeech.lite.config.SSLConfig;
import com.aispeech.lite.param.LocalAsrParams;
import com.aispeech.lite.param.LocalGrammarParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.wakeupincrement.WakeupIncrementProcessor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class AILocalWakeupIncrementEngine extends BaseEngine {


    private SSLConfig mVadConfig;
    private LocalAsrConfig mAsrConfig;
    private VadParams mVadParams;
    private LocalAsrParams mAsrParams;
    private WakeupIncrementProcessor mWakeupIncrementProcessor;

    private LocalGrammarConfig mGrammarConfig;

    private AILocalWakeupIncrementConfig mConfig;

    private InnerEngine mInnerEngine;
    private ArrayList<LocalGrammarParams> mGrammerParamsList = new ArrayList<>();
    private Scene mCurrentScene;
    private String mSceneName = "";
    private String mGrammarContent = "";

    private AILocalWakeupIncrementEngine() {
        mVadConfig = new SSLConfig();
        mAsrConfig = new LocalAsrConfig();
        mVadParams = new VadParams();
        mAsrParams = new LocalAsrParams();
        mGrammarConfig = new LocalGrammarConfig();
        mWakeupIncrementProcessor = new WakeupIncrementProcessor();
        mWakeupIncrementProcessor.setUseSingleMessageProcess(false);
        mInnerEngine = new InnerEngine();
        mBaseProcessor = mWakeupIncrementProcessor;
    }

    @Override
    public String getTag() {
        return "local_wake_incre";
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
        if (mWakeupIncrementProcessor != null) {
            mWakeupIncrementProcessor.feedData(data, data.length);
        }
    }

    /**
     * 初始化引擎
     *
     * @param config   {@link AILocalWakeupIncrementConfig}
     * @param listener {@link AILocalWakeupIncrementListener}
     */
    public void init(AILocalWakeupIncrementConfig config, AILocalWakeupIncrementListener listener) {
        super.init();
        mConfig = config;
        parseConfig(config);
        mInnerEngine.init(listener);
        mWakeupIncrementProcessor.init(mInnerEngine, mAsrConfig, mVadConfig, mGrammarConfig);
    }

    /**
     * 解析初始化参数
     *
     * @param config {@link AILocalHotWordConfig}
     */
    private void parseConfig(AILocalWakeupIncrementConfig config) {
        mVadParams.setPauseTime(config.pauseTime);//热词引擎vad pauseTime 0
        mAsrConfig.setScope(config.languages.getLanguage());
        mVadConfig.setVadEnable(config.useVad);
        mVadConfig.setUseSSL(config.useSSL);

        /**
         * VAD资源加载
         */
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

        /***
         * 增强唤醒资源加载
         */
        List<String> asrAssetList = new ArrayList<>();
        if (config.asrRes.startsWith("/")) {
            mAsrConfig.setResBinPath(config.asrRes);
        } else {
            asrAssetList.add(config.asrRes);
            mAsrConfig.setResBinPath(Util.getResourceDir(mAsrConfig.getContext()) + File.separator + config.asrRes);
        }

        /***
         * asr net.bin加载
         */
        for (Scene scene : config.scenes) {
            if (!(scene.getNetBinPath().startsWith("/"))) {
                asrAssetList.add(scene.getNetBinPath());
                scene.setNetBinPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + scene.getNetBinPath());
            }
            Log.d(TAG, "net bin path " + scene.getNetBinPath());

            if (scene.isDefaultScene()) {
                mAsrConfig.setNetBinPath(scene.getNetBinPath());
            }
        }
        mAsrConfig.setAssetsResNames(asrAssetList.toArray(new String[asrAssetList.size()]));
        mAsrConfig.setScene(mConfig.scenes);

        /***
         * Grammer编译资源加载
         */
        Log.d(TAG, "grammar res = " + config.grammerRes);
        if (TextUtils.isEmpty(config.grammerRes)) {
            Log.e(TAG, "Grammar res not found int asserts!!");
            mGrammarConfig.setResBinPath("OFF");
        } else if (config.grammerRes.startsWith("/")) {
            mGrammarConfig.setResBinPath(config.grammerRes);
        } else {
            mGrammarConfig.setAssetsResNames(new String[]{config.grammerRes});
            mGrammarConfig.setResBinPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.grammerRes);
        }
    }

    public void setCustomThreshold(String[] words, Double[] threshold) {
        if (words == null || threshold == null || words.length != threshold.length)
            throw new IllegalArgumentException("set custom threshold data inconsistent!");
        Map<String, Double> customThreshold = new HashMap<>();
        for (int i = 0; i < words.length; i++) {
            customThreshold.put(words[i], threshold[i]);
        }
        mAsrParams.setCustomThresholdMap(customThreshold);
    }

    public void setBlackWords(String[] blackWords) {
        if (blackWords == null || blackWords.length == 0)
            throw new IllegalArgumentException("empty hot words");
        StringBuilder buffer = new StringBuilder();
        buffer.append("\"");
        for (String d : blackWords) {
            if (!TextUtils.isEmpty(d)) {
                buffer.append(d + ",");
            }
        }
        buffer.delete(buffer.lastIndexOf(","), buffer.length());
        buffer.append("\"");
        mAsrParams.setUseDynamicBackList(buffer.toString());
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
        Log.i(TAG, "setScene sceneName = " + sceneName + " , grammarContent = " + grammarContent);
        Scene buildScene = chooseScene(sceneName, grammarContent);
        //去重：同一个scene，同一个内容，不重复build
        if (mSceneName != null && mGrammarContent != null && mSceneName.equals(sceneName) && mGrammarContent.equals(grammarContent)) {
            Log.i(TAG, "drop setScene sceneName = " + sceneName);
        } else {
            mSceneName = sceneName;
            mGrammarContent = grammarContent;
            build(buildScene, grammarContent);
        }
    }

    private void build(Scene buildScene, String grammarContent) {
        try {
            if (mWakeupIncrementProcessor != null && buildScene != null) {
                mGrammerParamsList.clear();
                JSONArray jsonArray = new JSONArray(grammarContent);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.optJSONObject(i);
                    Iterator iterator = jsonObject.keys();
                    while (iterator.hasNext()) {
                        String key = (String) iterator.next();//词槽
                        String xbnf = jsonObject.getString(key);//xbnf文本

                        if (!buildScene.getSlots().contains(key)) {
                            throw new IllegalArgumentException("Scene场景中不存在此词库，请在Scene的setSlots中设置该词库" + key + " ,Scene : " + buildScene.getName());
                        }

                        LocalGrammarParams mGrammarParam = new LocalGrammarParams();
                        String binName = System.currentTimeMillis() + "_" + key;//加时间戳，每次生成的bin都不一样
                        mGrammarParam.setSlotName(binName);
                        mGrammarParam.setOutputPath(buildScene.getGramSlotPath() + binName + Scene.BIN_SUFFIX);
                        String ebnf = DynamicDecodeUtil.jsonToXbnfStr(xbnf);
                        Log.d(TAG, "build key = " + key + " , ebnf = " + ebnf);
                        mGrammarParam.setEbnf(ebnf);
                        mGrammarParam.setScene(buildScene);
                        mGrammerParamsList.add(mGrammarParam);
                    }
                }
                mWakeupIncrementProcessor.build(new ArrayList<LocalGrammarParams>(mGrammerParamsList));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Scene chooseScene(String sceneName, String grammarContent) {
        if (TextUtils.isEmpty(grammarContent)) {
            if (mWakeupIncrementProcessor != null) {
                Log.d(TAG, "grammarContent is null , will reStart asr with null expandFnPath!");
                mAsrParams.setExpandFnPath("");
                mWakeupIncrementProcessor.start(mAsrParams, mVadParams);
            }
            return null;
//            throw new IllegalArgumentException("grammarContent 内容不能为空!");
        }
        Scene backScene = null;
        if (!TextUtils.isEmpty(sceneName)) {
            for (Scene scene : mConfig.scenes) {
                if (scene.getName().equals(sceneName)) {
                    backScene = scene;
                    mCurrentScene = scene;
                }
            }
            if (backScene == null) {
                throw new IllegalArgumentException("当前更新的Scene场景不存在，请先初始化该Scene场景!");
            }
        } else {
            if (mCurrentScene == null) {
                for (Scene scene : mConfig.scenes) {
                    if (scene.isDefaultScene()) {
                        backScene = scene;
                        mCurrentScene = scene;
                    }
                }
                if (backScene == null) {
                    throw new IllegalArgumentException("需要在初始化的时候设置一个默认的Scene场景!");
                }
            } else {
                backScene = mCurrentScene;
            }
        }
        Log.i(TAG, "chooseScene,backScene = " + backScene != null ? backScene.toString() : "null");
        return backScene;
    }

    /**
     * 启动热词引擎
     * n
     *
     * @param intent 启动参数
     */
    public void start(AILocalWakeupIncrementIntent intent) {
        super.start();
        parseIntent(intent);
        if (mWakeupIncrementProcessor != null) {
            mWakeupIncrementProcessor.start(mAsrParams, mVadParams);
        }
    }

    /**
     * 解析启动参数
     *
     * @param intent {@link AILocalWakeupIncrementIntent}
     */
    private void parseIntent(AILocalWakeupIncrementIntent intent) {
        super.parseIntent(intent, mAsrParams, mVadParams);
        mAsrParams.setUseContinuousRecognition(intent.isUseContinuousRecognition());
        mAsrParams.setMaxSpeechTimeS(intent.getMaxSpeechTime());
        mVadParams.setSaveAudioPath(intent.getSaveAudioPath());
        mAsrParams.setSaveAudioPath(intent.getSaveAudioPath());
        mAsrParams.setCustomThresholdMap(intent.getCustomThreshold());
        mAsrParams.setUseDynamicBackList(intent.getBlackWords());
        mAsrParams.setUseThreshold(intent.getThreshold());
        mAsrParams.setUseEnglishThreshold(intent.getEnglishThreshold());
        mAsrParams.setNoSpeechTimeout(intent.getNoSpeechTime());
        mAsrParams.setIsIgnoreThreshold(intent.getIsIgnoreThreshold());
        mAsrParams.setUseCustomFeed(intent.isUseCustomFeed());
        if (mVadConfig.isUseSSL() && !intent.isUseCustomFeed()) {
            throw new IllegalArgumentException("use ssl must custom feed!");
        }
        mAsrParams.setFillerPenaltyScore((float) intent.getFillerPenaltyScore());
        mAsrParams.setUseFiller(intent.isUseFiller());
    }

    /**
     * 停止热词引擎
     */
    public void cancel() {
        super.cancel();
        if (mWakeupIncrementProcessor != null) {
            mWakeupIncrementProcessor.cancel();
        }

        if (mInnerEngine != null) {
            mInnerEngine.sendMsgToCallbackMsgQueue(BaseInnerEngine.CallbackMsg.MSG_CANCEL, null);
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

        if (mWakeupIncrementProcessor != null) {
            mWakeupIncrementProcessor.stop();
        }
    }

    /**
     * 销毁引擎
     */
    public void destroy() {
        super.destroy();
        if (mWakeupIncrementProcessor != null) {
            mWakeupIncrementProcessor.release();
        }
        if (mInnerEngine != null) {
            mInnerEngine.release();
            mInnerEngine = null;
        }

    }

    private static class InnerEngine extends BaseInnerEngine {

        private AILocalWakeupIncrementListener mListener;

        @Override
        public void release() {
            super.release();
            if (mListener != null)
                mListener = null;
        }

        void init(AILocalWakeupIncrementListener listener) {
            super.init(listener);
            mListener = listener;
        }

        @Override
        protected void callbackInMainLooper(CallbackMsg msg, Object obj) {

            switch (msg) {
                case MSG_GRAMMAR_SUCCESS:
                    mListener.onGramResults((String) obj);
                    break;
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
                case MSG_UPDATE_RESULT:
                    mListener.onSetScene((Integer) obj);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onGramResults(String path) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_GRAMMAR_SUCCESS, path);
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

        @Override
        public void onSetScene(int status) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, status);
        }
    }
}
