package com.aispeech.export.engines;

import static com.aispeech.lite.SemanticType.DUI;
import static com.aispeech.lite.SemanticType.NAVI;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.base.BaseInnerEngine;
import com.aispeech.common.AIConstant;
import com.aispeech.common.FileUtil;
import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.ASRMode;
import com.aispeech.export.Vocab;
import com.aispeech.export.config.AILocalSemanticConfig;
import com.aispeech.export.intent.AILocalASRIntent;
import com.aispeech.export.intent.AILocalSemanticIntent;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AIUpdateListener;
import com.aispeech.kernel.Asr;
import com.aispeech.kernel.SemanticNAVI;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Vad;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.Languages;
import com.aispeech.lite.config.LocalAsrConfig;
import com.aispeech.lite.config.LocalSemanticConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.param.LocalAsrParams;
import com.aispeech.lite.param.LocalGrammarParams;
import com.aispeech.lite.param.LocalSemanticParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.semantic.SemanticProcessor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 离线语义引擎
 */
@Deprecated
public class AILocalSemanticEngine {

    public static final String TAG = "AILocalSemanticEngine";
    private final SemanticProcessor mSemanticProcessor;
    private LocalVadConfig mVadConfig;
    private LocalAsrConfig mAsrConfig;
    private LocalSemanticConfig mSemanticConfig;
    private LocalSemanticParams mSemanticParams;
    private VadParams mVadParams;
    private LocalAsrParams mAsrParams;
    private final LocalGrammarParams mGrammarParams;
    private InnerEngine mInnerEngine;

    private Languages languages = Languages.CHINESE;

    private String mVadResName = "";
    private String mVadResPath = "";
    private String mAsrNetName = "";
    private String mAsrNetPath = "";
    private String mAsrResName = "";
    private String mAsrResPath = "";

    private String mSemanticResFolderName = "";
    private String mSemanticResFolderPath = "";
    private String mSemanticLuaFolderName = "";
    private String mSemanticLuaFolderPath = "";

    private AILocalSemanticEngine() {
        mSemanticProcessor = new SemanticProcessor();
        mVadConfig = new LocalVadConfig();
        mAsrConfig = new LocalAsrConfig();
        mSemanticConfig = new LocalSemanticConfig();
        mSemanticParams = new LocalSemanticParams();
        mVadParams = new VadParams();
        mAsrParams = new LocalAsrParams();
        mGrammarParams = new LocalGrammarParams();
        mInnerEngine = new InnerEngine();
    }

    public static AILocalSemanticEngine createInstance() {
        return new AILocalSemanticEngine();
    }

    public static boolean checkLibValid() {
        return Asr.isAsrSoValid() && Vad.isSoValid() && Utils.isUtilsSoValid() && SemanticNAVI.isSemanticSoValid();
    }

    /**
     * 设置识别声学资源名
     * 须在init之前设置才生效, 适用于识别声学资源放在assets目录下
     *
     * @param asrResBin 资源名
     */
    public void setAsrResBin(String asrResBin) {
        this.mAsrResName = asrResBin;
    }


    /**
     * 设置识别声学资源名
     * 须在init之前设置才生效, 适用于识别声学资源放在自定义目录下
     *
     * @param path 资源名全路径
     */
    public void setAsrResBinPath(String path) {
        this.mAsrResPath = path;
    }

    /**
     * 设置识别网络资源名, 适用于识别网络资源放在assets目录下
     * 须在init之前设置才生效
     *
     * @param asrNetBin 资源名
     */
    public void setAsrNetBin(String asrNetBin) {
        this.mAsrNetName = asrNetBin;
    }

    /**
     * 设置识别网络资源自定义路径, 适用于通过语法构建引擎动态生成网络资源或者网络资源放在自定义路径
     * 须在init之前设置才生效
     *
     * @param path 资源名全路径
     */
    public void setAsrNetBinPath(String path) {
        this.mAsrNetPath = path;
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
     * 告知识别引擎已经唤醒，该接口在oneshot功能中使用，内部会记录唤醒的时间点，
     * 之后在vad end的时候来判断到底用户说的是不是唤醒词+指令，还是只有唤醒词
     */
    public void notifyWakeup() {
        mAsrParams.setWakeupTime(System.currentTimeMillis());
    }


    /**
     * 允许引擎初始化后动态更新识别网络资源, 适用于通过语法构建引擎动态生成网络资源或者网络资源放在自定义路径
     * 引擎处于识别中调用无效,如资源更新失败抛出 {@link AIError#ERR_NET_BIN_INVALID} 错误
     *
     * @param asrNetBin 资源完整路径,如：sdcard/aispeech/net.bin
     * @deprecated 该接口不支持结果回调，请使用新接口 {@link #updateNetBinPath(AIUpdateListener, String)}
     */
    public void updateNetBinPath(String asrNetBin) {
        if (TextUtils.isEmpty(asrNetBin)) {
            throw new IllegalArgumentException("illegal net.bin path");
        } else {
            mAsrConfig.setNetBinPath(asrNetBin);//update net.bin path
            if (mSemanticProcessor != null) {
                mSemanticProcessor.update(mAsrConfig.toJson().toString());
            }
        }
    }

    /**
     * 更新 netBin
     *
     * @param listener  更新接口回调 {@link AIUpdateListener}
     * @param asrNetBin asr 加载使用 net.bin 资源
     */
    public void updateNetBinPath(AIUpdateListener listener, String asrNetBin) {
        if (TextUtils.isEmpty(asrNetBin)) {
            Log.e(TAG, "illegal net.bin path");
            listener.failed();
        } else {
            if (mInnerEngine == null)
                throw new IllegalStateException(" init engine first ");
            mInnerEngine.setUpdateListener(listener);
            mAsrConfig.setNetBinPath(asrNetBin);//update net.bin path
            if (mSemanticProcessor != null) {
                mSemanticProcessor.update(mAsrConfig.toJson().toString());
            }
        }
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
     * 设置是否启用vad
     * 须在init之前设置才生效
     *
     * @param vadEnable true:使用Vad；false:禁止Vad，默认为true
     */
    public void setVadEnable(boolean vadEnable) {
        mAsrConfig.setVadEnable(vadEnable);
        mVadConfig.setVadEnable(vadEnable);
    }

    /**
     * 设置VAD资源名,适用于VAD资源放置在assets目录
     * 须在init之前设置才生效
     *
     * @param vadRes vadResName
     */
    public void setVadRes(String vadRes) {
        this.mVadResName = vadRes;
    }

    /**
     * 设置VAD资源名绝对路径，适用于VAD资源放置在自定义目录下
     * 须在init之前设置才生效
     *
     * @param path vad资源名全路径
     */
    public void setVadResPath(String path) {
        this.mVadResPath = path;
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
     * 添加识别结果分割符,如设置 "," 识别结果显示 ： "打,开,天,窗"
     * start 之前设置生效
     *
     * @param delimiter 分割符 , 离线语义引擎不推荐打开,会导致识别结果送入语义引擎无法出结果
     */
    public void setUseDelimiter(String delimiter) {
        mAsrParams.setUseWrdSep(delimiter);
    }

    /**
     * 设置是否开启置信度
     * 须在start之前设置才生效
     *
     * @param useConf true 启用,默认为false
     */
    public void setUseConf(boolean useConf) {
        mAsrParams.setUseConf(useConf);
    }


    /**
     * 设置是否启用基于语法的语义识别
     * 须在start之前设置才生效
     *
     * @param useXbnfRec true 启用，默认为false
     */
    public void setUseXbnfRec(boolean useXbnfRec) {
        mAsrParams.setUseXbnfRec(useXbnfRec);
    }

    /**
     * ExpandFn 文件路径,用于动态拆分net.bin文件
     * start 之前传入有效
     *
     * @param path ExpandFn 文件的绝对路径
     *             slots 文件示例{"slot": [{"name": "DEVICE","path": "device.slot.bin" }, {"name": "WAKEUP_WORD","path": "wakeup_word.slot.bin" }]}
     */
    public void setExpandFnPath(String path) {
        if (TextUtils.isEmpty(path)) {
            throw new IllegalArgumentException("illegal ExpandFn path .");
        }
        File f = new File(path);
        if (!f.exists() || f.isDirectory()) {
            throw new IllegalArgumentException("illegal ExpandFn path .");
        }
        Log.d(TAG, "Semantic setExpandFnPath " + path);
        mAsrParams.setExpandFnPath(path);
    }

    /**
     * 设置是否开启拼音输出
     * 须在start之前设置才生效
     *
     * @param usePinyin usePinyin,默认为false
     */
    public void setUsePinyin(boolean usePinyin) {
        mAsrParams.setUsePinyin(usePinyin);
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
     * 设置无语音超时时长，单位毫秒，默认值为5000ms ；如果达到该设置值时，自动停止录音并放弃请求识别内核
     * 须在start之前设置才生效
     *
     * @param milliSecond 超时时长，单位毫秒
     */
    public void setNoSpeechTimeOut(int milliSecond) {
        mAsrParams.setNoSpeechTimeout(milliSecond);
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
        mAsrParams.setMaxSpeechTimeS(seconds);
        mVadParams.setMaxSpeechTimeS(seconds);
    }


    /**
     * 设置是否使用录音超时之后的识别结果，默认返 {@link AIError#ERR_MAX_SPEECH}
     * 开启后则尝试将录音识别结果返回
     * 须在start之前设置才生效
     *
     * @param useMaxSpeechResult useMaxSpeechResult
     */
    public void setUseMaxSpeechResult(boolean useMaxSpeechResult) {
        mAsrParams.setUseMaxSpeechResult(useMaxSpeechResult);
    }

    /**
     * 设置是否开启识别中间结果
     *
     * @param useFrameSplit true 启用,默认为false
     */
    public void setUseFrameSplit(boolean useFrameSplit) {
        mAsrParams.setUseFrameSplit(useFrameSplit);
    }

    /**
     * 设置是否自行feed数据,不使用内部录音机(包括MockRecord和AIAudioRecord),
     * 需要在init之前调用, 默认为false
     *
     * @param useCustomFeed the useCustomFeed to set
     */
    public void setUseCustomFeed(boolean useCustomFeed) {
        mAsrConfig.setUseCustomFeed(useCustomFeed);
        mVadConfig.setUseCustomFeed(useCustomFeed);
        mSemanticConfig.setUseCustomFeed(useCustomFeed);
    }

    /**
     * 设置是否关闭识别并只传入语义文本功能，
     *
     * @param useRefText 默认为false，开启识别功能
     */
    public void setUseRefText(boolean useRefText) {
        mSemanticConfig.setUseRefText(useRefText);
        mVadConfig.setVadEnable(!useRefText);
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
     * 设置oneshot cache音频,在start之前生效
     *
     * @param cache {@link OneshotCache}
     */
    public void setOneshotCache(OneshotCache<byte[]> cache) {
        if (cache != null && cache.isValid()) {
            mAsrParams.setOneshotCache(cache);
        }
    }

    /**
     * 设置保存的音频路径，最终的音频路径为path + local_semantic_+ recordId + ".pcm"
     * 需要在start之前调用
     *
     * @param path 路径
     */
    public void setSaveAudioPath(String path) {
        mVadParams.setSaveAudioPath(path);
        mAsrParams.setSaveAudioPath(path);
    }

    /**
     * 初始化本地识别引擎
     *
     * @param listener 本地识别回调接口
     * @deprecated 已过时, see {@link #init(AILocalSemanticConfig, AIASRListener)}
     */
    public void init(AIASRListener listener) {
        AILocalSemanticConfig semanticConfig = new AILocalSemanticConfig.Builder()
                .setNetBin(!TextUtils.isEmpty(mAsrNetName) ? mAsrNetName : mAsrNetPath)
                .setNgramBin(!TextUtils.isEmpty(mAsrResName) ? mAsrResName : mAsrResPath)
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
        parseConfig(config);
        mInnerEngine.init(listener);
        if (mSemanticProcessor != null) {
            mSemanticProcessor.init(mInnerEngine, mAsrConfig, mVadConfig, mSemanticConfig);
        }
    }

    /**
     * 解析初始化参数
     *
     * @param config {@link AILocalSemanticConfig}
     */
    private void parseConfig(AILocalSemanticConfig config) {
        //是否启用语义模式
        mSemanticConfig.setUseRefText(config.useRefText);
        mSemanticConfig.setUseFormat(config.useFormat);
        mSemanticConfig.setSemanticType(config.semanticType);

        setUseCustomFeed(config.useCustomFeed);

        mSemanticConfig.setEnableNluFormatV2(config.enableNluFormatV2);
        mSemanticConfig.setUseSelectRule(config.useSelectRule);
        mSemanticConfig.setSelectRuleThreshold(config.selectRuleThreshold);
        if (!config.useRefText) {
            mAsrConfig.setScope(config.languages.getLanguage());
            List<String> asrAssetsResList = new ArrayList();
            mAsrConfig.setUseCustomFeed(config.useCustomFeed);
            mVadConfig.setUseCustomFeed(config.useCustomFeed);
            mSemanticConfig.setUseCustomFeed(config.useCustomFeed);
            mSemanticConfig.setThrowEmptySemantic(config.throwEmptySemantic);

            if (!TextUtils.isEmpty(config.ngramBin)) {
                if (!config.ngramBin.startsWith("/")) {
                    asrAssetsResList.add(config.ngramBin);
                    mAsrConfig.setResBinPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.ngramBin);
                } else {
                    mAsrConfig.setResBinPath(config.ngramBin);
                }
            }

            if (!TextUtils.isEmpty(config.netBin)) {
                if (!config.netBin.startsWith("/")) {
                    asrAssetsResList.add(config.netBin);
                    String netBin = Util.getResourceDir(AISpeech.getContext()) + File.separator + config.netBin;
                    mAsrConfig.setNetBinPath(netBin);
                    mGrammarParams.setOutputPath(netBin);
                } else {
                    mAsrConfig.setNetBinPath(config.netBin);
                    mGrammarParams.setOutputPath(config.netBin);
                }
            }

            mAsrConfig.setAssetsResNames(asrAssetsResList.toArray(new String[asrAssetsResList.size()]));

            mVadConfig.setVadEnable(config.useVad);
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

            if (config.asrPolicy != null && mSemanticProcessor != null) {
                mSemanticProcessor.setAsrPolicy(config.asrPolicy);
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

        mSemanticConfig.setAssetsResNames(semAssetsResList.toArray(new String[semAssetsResList.size()]));

        if (config.useFormat) {
            mSemanticParams.setOutputFomat("DUI");
        }

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
        AILocalASRIntent asrIntent = intent.getAsrIntent();
        if (asrIntent != null) {
            if (asrIntent.getExpandFnPath() != null) {
                File f = new File(asrIntent.getExpandFnPath());
                if (!f.exists() || f.isDirectory()) {
                    throw new IllegalArgumentException("illegal ExpandFn path .");
                }
                mAsrParams.setExpandFnPath(asrIntent.getExpandFnPath());
            }
            mAsrParams.setMaxSpeechTimeS(asrIntent.getMaxSpeechTimeS());
            mAsrParams.setNoSpeechTimeout(asrIntent.getNoSpeechTimeOut());
            mAsrParams.setSaveAudioPath(asrIntent.getSaveAudioPath());
            mAsrParams.setUseFrameSplit(asrIntent.isUseFrameSplit());
            mAsrParams.setMode(asrIntent.getMode().getValue());
            mAsrParams.setUseConf(true);//置信度必选
            mAsrParams.setUsePinyin(languages == Languages.CHINESE);//中文拼音必选
            if (ASRMode.MODE_ASR != asrIntent.getMode() && TextUtils.isEmpty(asrIntent.getWords())) {
                asrIntent.setWords(new String[]{"FILLER"});// 内核默认必须传递热词列表
                Log.e(TAG, "please register dynamic list !");
            }
            mAsrParams.setUseDynamicList(asrIntent.getWords());
            mAsrParams.setUseDynamicBackList(asrIntent.getBlackWords());
            mAsrParams.setCustomThresholdMap(asrIntent.getCustomThreshold());
            mAsrParams.setMode(asrIntent.getMode().getValue());
            mAsrParams.setUseThreshold(asrIntent.getThreshold());
            mAsrParams.setUseEnglishThreshold(asrIntent.getEnglishThreshold());

            if (asrIntent.getWakeupWords() != null && asrIntent.getWakeupWords().length > 0) {

                mAsrParams.setWakeupWords(asrIntent.getWakeupWords());
                //内核接口要求传入文件路径，此处将唤醒词信息写入文件并转换成文件路径传入
                String wakeupFilePath = AISpeech.getContext().getExternalCacheDir() + File.separator +
                        "wakeupWords.json";
                writeWakeupWordsToFile(asrIntent.getWakeupWords(), wakeupFilePath);
                mAsrParams.setWakeupWordsFilePath(wakeupFilePath);
            }
        }

        mSemanticParams.setRefText(intent.getRefText());
        mSemanticParams.setPinyin(intent.getPinyin());
        mSemanticParams.setDomain(intent.getDomain());
        mSemanticParams.setTask(intent.getTask());
        mSemanticParams.setSkillID(intent.getSkillID());
    }

    private void writeWakeupWordsToFile(String[] wakeupWords, String wakeupFilePath) {
        FileUtil fileUtil = new FileUtil(AISpeech.getContext());
        FileUtils.deleteFile(wakeupFilePath);
        fileUtil.createFile(wakeupFilePath);
        JSONObject wakeupWordsJo = new JSONObject();
        try {
            JSONArray jsonArray = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                jsonArray = new JSONArray(wakeupWords);
            } else {
                jsonArray = new JSONArray(Arrays.asList(wakeupWords));
            }
            wakeupWordsJo.put("one_shot_words", jsonArray);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        fileUtil.write(wakeupWordsJo.toString().getBytes());
    }

    /**
     * 更新 ldm 词库
     *
     * @param updateListener 状态回调
     * @param vocabs         词库
     */
    public void updateVocabs(AIUpdateListener updateListener, Vocab... vocabs) {
        if (TextUtils.isEmpty(mSemanticConfig.getDUIResPath())) {
            throw new IllegalArgumentException("请先设置DUI Res路径");
        }

        if (vocabs == null) {
            throw new IllegalArgumentException("illegal vocabs!");
        } else {
            if (mInnerEngine == null)
                throw new IllegalStateException(" init engine first ");
            mInnerEngine.setUpdateListener(updateListener);

            if (mSemanticProcessor != null) {
                mSemanticProcessor.updateVocabs(vocabs);
            }
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
        parseIntent(intent);
        if (mSemanticProcessor != null) {
            mSemanticProcessor.start(mAsrParams, mVadParams, mSemanticParams);
        }
    }

    /**
     * 启动录音，开始语音识别
     */
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
            mSemanticProcessor.start(mAsrParams, mVadParams, mSemanticParams);
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
            mSemanticProcessor.start(mAsrParams, mVadParams, mSemanticParams);
        }
    }

    /**
     * 传入数据,在不使用SDK内部录音机时调用
     *
     * @param data 音频数据流
     * @see #setUseCustomFeed(boolean)
     */
    @Deprecated
    public void feedData(byte[] data) {
        if (mAsrConfig != null && !mAsrConfig.isUseCustomFeed()) {
            Log.e(TAG, "useCustomFeed is not enabled，ignore data");
            return;
        }
        if (mSemanticProcessor != null) {
            mSemanticProcessor.feedData(data, data.length);
        }
    }

    /**
     * 传入数据,在不使用SDK内部录音机时调用
     *
     * @param data 音频数据流
     * @param size 音频数据大小
     * @see #setUseCustomFeed(boolean)
     */
    public void feedData(byte[] data, int size) {
        if (mAsrConfig != null && !mAsrConfig.isUseCustomFeed()) {
            Log.e(TAG, "useCustomFeed is not enabled，ignore data");
            return;
        }
        if (mSemanticProcessor != null) {
            mSemanticProcessor.feedData(data, size);
        }
    }

    /**
     * 停止录音，等待识别结果
     */
    public void stopRecording() {
        if (mSemanticProcessor != null) {
            mSemanticProcessor.stop();
        }
    }

    /**
     * 取消本次识别操作
     */
    public void cancel() {
        if (mSemanticProcessor != null) {
            mSemanticProcessor.cancel();
        }

        if (mInnerEngine != null) {
            mInnerEngine.removeCallbackMsg();
        }
    }

    /**
     * 销毁本地识别引擎
     */
    public void destroy() {
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
        if (mVadConfig != null)
            mVadConfig = null;
        if (mVadParams != null)
            mVadParams = null;
        if (mAsrConfig != null)
            mAsrConfig = null;
        if (mAsrParams != null)
            mAsrParams = null;
        mVadResName = null;
        mVadResPath = null;
        mAsrNetName = null;
        mAsrNetPath = null;
        mAsrResName = null;
        mAsrResPath = null;
        mSemanticLuaFolderName = null;
        mSemanticLuaFolderPath = null;
        mSemanticResFolderName = null;
        mSemanticResFolderPath = null;
    }

    private static class InnerEngine extends BaseInnerEngine {

        private AIASRListener mListener;

        private AIUpdateListener mUpdateListener;

        public void setUpdateListener(AIUpdateListener mUpdateListener) {
            this.mUpdateListener = mUpdateListener;
        }

        @Override
        public void release() {
            super.release();
            if (mListener != null)
                mListener = null;
        }

        void init(AIASRListener listener) {
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
                case MSG_UPDATE_RESULT:
                    int ret = (int) obj;
                    Log.d(TAG, "ret = " + ret);
                    if (ret == AIConstant.OPT_SUCCESS) {
                        mUpdateListener.success();
                    } else {
                        mUpdateListener.failed();
                    }
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
        public void onUpdateResult(int ret) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, ret);
        }

    }


}
