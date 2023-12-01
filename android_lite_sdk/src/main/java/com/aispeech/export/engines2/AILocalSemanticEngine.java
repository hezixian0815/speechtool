package com.aispeech.export.engines2;

import static com.aispeech.lite.SemanticType.BCDV2;
import static com.aispeech.lite.SemanticType.BUILDIN;
import static com.aispeech.lite.SemanticType.DUI;
import static com.aispeech.lite.SemanticType.NAVI;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.SemanticVocabsCfg;
import com.aispeech.export.Vocab;
import com.aispeech.export.config.AILocalSemanticConfig;
import com.aispeech.export.function.ISemantic;
import com.aispeech.export.intent.AILocalSemanticIntent;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.NluInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AILocalSemanticListener;
import com.aispeech.export.listeners.AIUpdateListener;
import com.aispeech.kernel.Asr;
import com.aispeech.kernel.SemanticNAVI;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Vad;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.Languages;
import com.aispeech.lite.SemanticType;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalSemanticConfig;
import com.aispeech.lite.param.LocalGrammarParams;
import com.aispeech.lite.param.LocalSemanticParams;
import com.aispeech.lite.semantic.LocalSemanticProcessor;
import com.aispeech.lite.speech.EngineListener;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 离线语义引擎
 */
public class AILocalSemanticEngine extends BaseEngine implements ISemantic {

    private final LocalSemanticProcessor mSemanticProcessor;
    private LocalSemanticConfig mSemanticConfig;
    private LocalSemanticParams mSemanticParams;
    private LocalGrammarParams mGrammarParams;
    private InnerEngine mInnerEngine;

    private Languages languages = Languages.CHINESE;

    private String mSemanticResFolderName = "";
    private String mSemanticResFolderPath = "";
    private String mSemanticLuaFolderName = "";
    private String mSemanticLuaFolderPath = "";

    private AILocalSemanticEngine() {
        mSemanticProcessor = new LocalSemanticProcessor();
        mSemanticConfig = new LocalSemanticConfig();
        mSemanticParams = new LocalSemanticParams();
        mGrammarParams = new LocalGrammarParams();
        mInnerEngine = new InnerEngine();
        mBaseProcessor = mSemanticProcessor;
    }

    @Override
    public String getTag() {
        return "local_semantic";
    }

    public static AILocalSemanticEngine createInstance() {
        return new AILocalSemanticEngine();
    }

    public static boolean checkLibValid() {
        return Asr.isAsrSoValid() && Vad.isSoValid() && Utils.isUtilsSoValid() && SemanticNAVI.isSemanticSoValid();
    }
    /**
     * 设置语义资源文件夹,名 适用于语义资源文件夹放在assets目录下
     * 须在init之前设置才生效
     *
     * @param semResFolder 语义资源文件夹
     */
    public void setSemResFolder(String semResFolder) {
        this.mSemanticResFolderName = semResFolder;
    }

    /**
     * 设置语义资源文件夹自定义路径, 适用于语义资源文件夹放在自定义路径
     * 须在init之前设置才生效
     *
     * @param semResFolderPath 语义资源文件夹全路径
     */
    public void setSemResFolderPath(String semResFolderPath) {
        this.mSemanticResFolderPath = semResFolderPath;
    }

    /**
     * 设置语义lua资源所在文件夹, 适用于语义lua资源所在文件夹放在assets目录下
     * 须在init之前设置才生效
     *
     * @param semLuaFolder 语义lua资源所在文件夹
     */
    public void setSemLuaFolder(String semLuaFolder) {
        this.mSemanticLuaFolderName = semLuaFolder;
    }

    /**
     * 设置语义lua资源所在文件夹自定义路径, 适用于语义lua资源所在文件夹放在自定义路径
     * 须在init之前设置才生效
     *
     * @param semLuaFolderPath 语义lua资源所在文件夹全路径
     */
    public void setSemLuaFolderPath(String semLuaFolderPath) {
        this.mSemanticLuaFolderPath = semLuaFolderPath;
    }

    /**
     * 设置前一轮对话领域信息,start之前设置生效
     *
     * @param domain 领域信息，如 地图
     */
    public void setDomain(String domain) {
        mSemanticParams.setDomain(domain);
    }

    /**
     * 设置SKILLID
     *
     * @param skillID 离线技能ID
     */
    public void setSkillID(String skillID) {
        mSemanticParams.setSkillID(skillID);
    }

    /**
     * 设置前一轮的对话task信息，start之前设置生效
     *
     * @param task task信息，如：导航 、 附件检索 等
     */
    public void setTask(String task) {
        mSemanticParams.setTask(task);
    }

    /**
     * 设置是否开启语义格式归一化,默认 false
     *
     * @param useFormat boolean
     */
    public void setUseFormat(boolean useFormat) {
        mSemanticConfig.setUseFormat(useFormat);
    }


    /**
     * 设置 grammar 与 ngram 最终输出结果的决策阈值
     *
     * @param threshold 决策阈值，默认是 0.63
     */
    public void setSelectRuleThreshold(double threshold) {
        mSemanticConfig.setSelectRuleThreshold(threshold);
    }


    /**
     * 设置是否使用SDK内部的 grammar 与 ngram 决策规则，默认 false
     *
     * @param useSelectRule boolean,自定义决策阈值见
     * @see #setSelectRuleThreshold
     */
    public void setUseSelectRule(boolean useSelectRule) {
        mSemanticConfig.setUseSelectRule(useSelectRule);
    }

    /**
     * 设置是否关闭识别并只传入语义文本功能，
     *
     * @param useRefText 默认为false，开启识别功能
     */
    public void setUseRefText(boolean useRefText) {
        mSemanticConfig.setUseRefText(useRefText);
    }

    /**
     * 设置语种，init 之前设置生效 , 默认 {@link Languages#CHINESE}
     *
     * @param languages {@link Languages}
     * @deprecated 已过时 使用 AILocalASRConfig 统一配置 {@link #init(AILocalSemanticConfig, AIASRListener)}
     */
    public void setLanguages(Languages languages) {
        this.languages = languages;
    }

    /**
     * 初始化本地识别引擎
     *
     * @param listener 本地识别回调接口
     * @deprecated 已过时, see {@link #init(AILocalSemanticConfig, AIASRListener)}
     */
    public void init(AIASRListener listener) {
        AILocalSemanticConfig semanticConfig = new AILocalSemanticConfig.Builder()
                .setSemNaviLuaResFolder(!TextUtils.isEmpty(mSemanticLuaFolderName) ? mSemanticLuaFolderName : mSemanticLuaFolderPath)
                .setSemNaviResFolder(!TextUtils.isEmpty(mSemanticResFolderName) ? mSemanticResFolderName : mSemanticResFolderPath)
                .setLanguages(languages)
                .setUseRefText(mSemanticConfig.isUseRefText())
                .setUseFormat(mSemanticConfig.isUseFormat())
                .build();
        init(semanticConfig, listener);
    }

    /**
     * 初始化本地识别引擎
     *
     * @param config   初始化参数 {@link AILocalSemanticConfig}
     * @param listener 本地识别回调接口{@link AIASRListener}
     */
    public void init(AILocalSemanticConfig config, AIASRListener listener) {
        super.init();
        parseConfig(config);
        mInnerEngine.init(listener);
        if (mSemanticProcessor != null) {
            mSemanticProcessor.init(mInnerEngine, mSemanticConfig);
        }
    }

    @Override
    public void init(AILocalSemanticConfig config, AILocalSemanticListener listener) {
        super.init();
        parseConfig(config);
        mInnerEngine.init(listener);
        if (mSemanticProcessor != null) {
            mSemanticProcessor.init(mInnerEngine, mSemanticConfig);
        }
    }

    /**
     * 解析初始化参数
     *
     * @param config {@link AILocalSemanticConfig}
     */
    private void parseConfig(AILocalSemanticConfig config) {
        super.parseConfig(config, mSemanticConfig);
        //是否启用语义模式
        mSemanticConfig.setUseRefText(config.useRefText);
        mSemanticConfig.setUseFormat(config.useFormat);
        mSemanticConfig.setSemanticType(config.semanticType);

        if (!config.useRefText) {
            if (!config.netBin.startsWith("/")) {
                String netBin = Util.getResourceDir(AISpeech.getContext()) + File.separator + config.netBin;
                mGrammarParams.setOutputPath(netBin);
            } else {
                mGrammarParams.setOutputPath(config.netBin);
            }
        }

        List<String> semAssetsResList = new ArrayList();
        if ((config.semanticType.getType() & NAVI.getType()) == NAVI.getType()) {

            if (!config.semNaviResFolder.startsWith("/")) {
                mSemanticConfig.setNaviResPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.semNaviResFolder);
                mSemanticConfig.setNaviVocabPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.semNaviResFolder);
                if (!semAssetsResList.contains(config.semNaviResFolder))
                    semAssetsResList.add(config.semNaviResFolder);
            } else {
                mSemanticConfig.setNaviResPath(config.semNaviResFolder);
                mSemanticConfig.setNaviVocabPath(config.semNaviResFolder);
            }

            if (!config.semNaviLuaResFolder.startsWith("/")) {
                mSemanticConfig.setNaviLuaPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.semNaviLuaResFolder);
                mSemanticConfig.setNaviSkillConfPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.semNaviLuaResFolder);
                if (!semAssetsResList.contains(config.semNaviLuaResFolder))
                    semAssetsResList.add(config.semNaviLuaResFolder);
            } else {
                mSemanticConfig.setNaviLuaPath(config.semNaviLuaResFolder);
                mSemanticConfig.setNaviSkillConfPath(config.semNaviLuaResFolder);
            }
        }

        if ((config.semanticType.getType() & DUI.getType()) == DUI.getType()) {
            if (!config.semDUIResFloder.startsWith("/")) {
                mSemanticConfig.setDUIResPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.semDUIResFloder);
                if (!semAssetsResList.contains(config.semDUIResFloder))
                    semAssetsResList.add(config.semDUIResFloder);
            } else {
                mSemanticConfig.setDUIResPath(config.semDUIResFloder);
            }

            if (!config.semDUILuaResFloder.startsWith("/")) {
                mSemanticConfig.setDUILuaPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.semDUILuaResFloder);
                if (!semAssetsResList.contains(config.semDUILuaResFloder))
                    semAssetsResList.add(config.semDUILuaResFloder);
            } else {
                mSemanticConfig.setDUILuaPath(config.semDUILuaResFloder);
            }

            if (!config.semDUIResCustomFloder.startsWith("/")) {
                mSemanticConfig.setDUIResCustomPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.semDUIResCustomFloder);
                if (!semAssetsResList.contains(config.semDUIResCustomFloder))
                    semAssetsResList.add(config.semDUIResCustomFloder);
            } else {
                mSemanticConfig.setDUIResCustomPath(config.semDUIResCustomFloder);
            }
        }

        if ((config.semanticType.getType() & BUILDIN.getType()) == BUILDIN.getType() || (config.semanticType.getType() & BCDV2.getType()) == BCDV2.getType()) {
            if (!config.buildinResFolder.startsWith("/")) {
                mSemanticConfig.setBuildinResPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.buildinResFolder);
                if (!semAssetsResList.contains(config.buildinResFolder))
                    semAssetsResList.add(config.buildinResFolder);
            } else {
                mSemanticConfig.setBuildinResPath(config.buildinResFolder);
            }

            if (!config.buildinLuaResFolder.startsWith("/")) {
                mSemanticConfig.setBuildinLuaPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.buildinLuaResFolder);
                mSemanticConfig.setBuildinSkillConfPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.buildinLuaResFolder);
                if (!semAssetsResList.contains(config.buildinLuaResFolder))
                    semAssetsResList.add(config.buildinLuaResFolder);
            } else {
                mSemanticConfig.setBuildinLuaPath(config.buildinLuaResFolder);
                mSemanticConfig.setBuildinSkillConfPath(config.buildinLuaResFolder);
            }

            if (config.vocabsCfgFolder != null && !config.vocabsCfgFolder.startsWith("/")) {
                mSemanticConfig.setVocabCfgPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.vocabsCfgFolder);
                if (!semAssetsResList.contains(config.vocabsCfgFolder))
                    semAssetsResList.add(config.vocabsCfgFolder);
            } else {
                mSemanticConfig.setVocabCfgPath(config.vocabsCfgFolder);
            }

            if (!TextUtils.isEmpty(config.skillMapping)) {
                if (!config.skillMapping.startsWith("/")) {
                    mSemanticConfig.setSkillMappingPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.skillMapping);
                    if (!semAssetsResList.contains(config.skillMapping))
                        semAssetsResList.add(config.skillMapping);
                } else {
                    mSemanticConfig.setSkillMappingPath(config.skillMapping);
                }
            }

        }

        mSemanticConfig.setAssetsResNames(semAssetsResList.toArray(new String[semAssetsResList.size()]));

        if (!TextUtils.isEmpty(config.builtInSemanticSkillID)) {
            mSemanticParams.setBuiltInSemanticSkillID(config.builtInSemanticSkillID);
        }

    }

    /**
     * 解析启动参数
     *
     * @param intent {@link AILocalSemanticIntent}
     */
    private void parseIntent(AILocalSemanticIntent intent) {
        super.parseIntent(intent, mSemanticParams);
        if (!TextUtils.isEmpty(intent.getRefText())) {
            mSemanticParams.setRefText(intent.getRefText());
        }
        mSemanticParams.setOutputFomat("DUI");
        mSemanticParams.setPinyin(intent.getPinyin());
        mSemanticParams.setDomain(intent.getDomain());
        mSemanticParams.setTask(intent.getTask());
        mSemanticParams.setSemanticThreshold(intent.getSemanticThreshold());
        mSemanticParams.setSkillID(intent.getSkillID());
        mSemanticParams.setUseRefTextLength(intent.getUseRefTextLength());
        mSemanticParams.setUsePinyin(intent.isUsePinyin());
        mSemanticParams.setEnableBCDDiscard(intent.isEnableBCDDiscard());
        mSemanticParams.setUseHarshDiscard(intent.isUseHarshDiscard());
        if (!TextUtils.isEmpty(intent.getEnvJson()) && (mSemanticConfig.getSemanticType().getType() & SemanticType.BCDV2.getType()) == SemanticType.BCDV2.getType()) {
            try {
                JSONObject envJson = new JSONObject(intent.getEnvJson());
                mSemanticParams.setEnvJson(envJson);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, e.toString());
            }
        }

        if (intent.getMaxWaitingTimeout() > 0) {
            mSemanticParams.setWaitingTimeout(intent.getMaxWaitingTimeout());
        }
    }

    @Override
    public void updateVocab(Vocab vocab, AIUpdateListener updateListener) {
        Log.in(TAG, "updateVocab");
        if (vocab == null) {
            Log.e(TAG, "illegal vocab!");
        } else {
            updateVocabs(updateListener, vocab);
        }
    }

    /**
     * 更新 ldm 词库
     *
     * @param updateListener 状态回调
     * @param vocabs         词库
     */
    public void updateVocabs(AIUpdateListener updateListener, Vocab... vocabs) {
        if (vocabs == null || vocabs.length <= 0) {
            Log.e(TAG, "illegal vocab!");
        }

        if (mInnerEngine != null) {
            mInnerEngine.setUpdateListener(updateListener);
        }
        if (mSemanticProcessor != null) {
            mSemanticProcessor.updateVocab(vocabs);
        }
    }

    /**
     * 获取 ldm contents 同步方法 读取联系人需要在线程中调用
     *
     * @param vocabName 文件名
     * @return 词库内容
     */
    public List<String> getVocab(String vocabName) {
        if (TextUtils.isEmpty(mSemanticConfig.getDUIResPath())) {
            throw new IllegalArgumentException("请先设置DUI Res路径");
        }
        return mSemanticProcessor.getVocab(vocabName);
    }

    /**
     * 默认配置启动引擎
     *
     * @see #start(AILocalSemanticIntent) 在AILocalSemanticIntent中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void start() {
        start(new AILocalSemanticIntent());
    }


    /**
     * 识别+语义模式启动引擎
     *
     * @param intent 启动参数{@link AILocalSemanticIntent}
     */
    public void start(AILocalSemanticIntent intent) {
        super.start();
        parseIntent(intent);
        if (mSemanticProcessor != null) {
            mSemanticProcessor.start(mSemanticParams);
        }
    }

    @Override
    public void stop() {
        super.stop();
        if (mSemanticProcessor != null) {
            mSemanticProcessor.stop();
        }
    }

    /**
     * 启动录音，开始语音识别
     *
     * @deprecated 不支持录音
     */
    @Deprecated
    public void startWithRecording() {
        start(new AILocalSemanticIntent());
    }

    /**
     * 启动引擎，输入文本
     *
     * @param refText 文本
     * @deprecated 请参考 {@link #startWithText}
     */
    public void startWithText(String refText) {
        mSemanticParams.setRefText(refText);
        if (mSemanticProcessor != null) {
            mSemanticProcessor.start(mSemanticParams);
        }
    }

    /**
     * 启动引擎，输入文本和拼音，拼音可从识别结果中获取
     *
     * @param refText 文本
     * @param pinyin  拼音
     */
    public void startWithText(String refText, String pinyin) {
        mSemanticParams.setRefText(refText);
        mSemanticParams.setPinyin(pinyin);
        if (mSemanticProcessor != null) {
            mSemanticProcessor.start(mSemanticParams);
        }
    }
    /**
     * 取消本次识别操作
     */
    public void cancel() {
        super.cancel();
        if (mSemanticProcessor != null) {
            mSemanticProcessor.cancel();
        }
    }

    /**
     * 销毁本地识别引擎
     */
    public void destroy() {
        super.destroy();
        if (mSemanticProcessor != null) {
            mSemanticProcessor.release();
        }
        if (mInnerEngine != null) {
            mInnerEngine.release();
            mInnerEngine = null;
        }
        if (mSemanticConfig != null)
            mSemanticConfig = null;
        if (mSemanticParams != null)
            mSemanticParams = null;
        mSemanticLuaFolderName = null;
        mSemanticLuaFolderPath = null;
        mSemanticResFolderName = null;
        mSemanticResFolderPath = null;
    }

    private static class InnerEngine extends SpeechListener {

        private EngineListener mListener;

        private AIUpdateListener mUpdateListener;

        public void setUpdateListener(AIUpdateListener mUpdateListener) {
            this.mUpdateListener = mUpdateListener;
        }

        public void release() {
            if (mListener != null)
                mListener = null;
        }

        void init(EngineListener listener) {
            mListener = listener;
        }


        @Override
        public void onInit(int status) {
            if (mListener != null) {
                mListener.onInit(status);
            }
        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(error);
            }
        }

        @Override
        public void onNotOneShot() {
            if (mListener != null) {
                if (mListener instanceof AIASRListener) {
                    ((AIASRListener) mListener).onNotOneShot();
                }
            }
        }

        @Override
        public void onResults(AIResult result) {
            if (mListener instanceof AIASRListener) {
                ((AIASRListener) mListener).onResults(result);
            } else if (mListener instanceof AILocalSemanticListener) {
                ((AILocalSemanticListener) mListener).onResults(result);
            }
            try {
                JSONObject customObj = new JSONObject().put(IInterceptor.Name.LOCAL_NLU_OUTPUT, (AIResult) result);
                JSONObject inputObj = NluInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, customObj);
                SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.LOCAL_NLU_OUTPUT, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            if (mListener instanceof AIASRListener) {
                ((AIASRListener) mListener).onRmsChanged(rmsdB);
            }
        }

        @Override
        public void onBeginningOfSpeech() {
            if (mListener instanceof AIASRListener) {
                ((AIASRListener) mListener).onBeginningOfSpeech();
            }
        }

        @Override
        public void onEndOfSpeech() {
            if (mListener instanceof AIASRListener) {
                ((AIASRListener) mListener).onEndOfSpeech();
            }
        }

        public void onUpdateResult(int ret) {
            if (ret == AIConstant.OPT_SUCCESS) {
                if (mUpdateListener != null) {
                    mUpdateListener.success();
                }
            } else {
                if (mUpdateListener != null) {
                    mUpdateListener.failed();
                }
            }
        }

        @Override
        public void onCancel() {

        }

    }

    /**
     * 更新离线内置语义词库
     *
     * @param updateListener    状态回调
     * @param semanticVocabsCfg 词库配置
     */
    public void updateNaviVocab(AIUpdateListener updateListener, SemanticVocabsCfg semanticVocabsCfg) {
        if (semanticVocabsCfg == null) {
            throw new IllegalArgumentException("illegal vocabcfg!");
        } else {
            if (mInnerEngine == null)
                throw new IllegalStateException(" init engine first ");
            mInnerEngine.setUpdateListener(updateListener);

            if (mSemanticProcessor != null) {
                mSemanticProcessor.updateNaviVocab(semanticVocabsCfg.toJSON().toString());
            }
        }
    }


}
