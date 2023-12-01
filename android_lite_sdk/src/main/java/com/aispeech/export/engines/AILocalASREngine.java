package com.aispeech.export.engines;

import android.text.TextUtils;

import com.aispeech.common.FileUtil;
import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.common.Transformer;
import com.aispeech.export.config.AILocalASRConfig;
import com.aispeech.export.intent.AILocalASRIntent;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AIUpdateListener;
import com.aispeech.kernel.Asr;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Vad;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.Languages;
import com.aispeech.lite.config.LocalAsrConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.param.LocalAsrParams;
import com.aispeech.lite.param.VadParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

@Deprecated
public class AILocalASREngine {
    public static final String TAG = "AILocalASREngine";
    private final LocalVadConfig mVadConfig;
    private final LocalAsrConfig mAsrConfig;
    private final VadParams mVadParams;
    private final LocalAsrParams mAsrParams;

    private Languages languages = Languages.CHINESE;
    private String mVadRes;
    private String mVadResPath;
    private String mNetBin;
    private String mNetBinPath;
    private String mResBin;
    private String mResBinPath;
    private com.aispeech.export.engines2.AILocalASREngine aiLocalASREngine2;

    /**
     * 是否启用语义输出格式归一化,默认 false
     */
    private static boolean useFormat = false;


    private AILocalASREngine() {
        aiLocalASREngine2 = com.aispeech.export.engines2.AILocalASREngine.createInstance();

        mVadConfig = new LocalVadConfig();
        mAsrConfig = new LocalAsrConfig();
        mVadParams = new VadParams();
        mAsrParams = new LocalAsrParams();
    }

    @Deprecated
    public static AILocalASREngine createInstance() {
        return new AILocalASREngine();
    }


    @Deprecated
    public static boolean checkLibValid() {
        return Asr.isAsrSoValid() && Vad.isSoValid() && Utils.isUtilsSoValid();
    }


    /**
     * 告知识别引擎已经唤醒，该接口在oneshot功能中使用，内部会记录唤醒的时间点，
     * 之后在vad end的时候来判断到底用户说的是不是唤醒词+指令，还是只有唤醒词
     */
    @Deprecated
    public void notifyWakeup() {
        mAsrParams.setWakeupTime(System.currentTimeMillis());
    }


    /**
     * 设置网络资源名, 适用于网络资源放在assets目录下
     * 须在init之前设置才生效
     *
     * @param netBinName 资源名
     * @deprecated 已过时 使用AILocalASRConfig统一配置 {@link #init(AILocalASRConfig, AIASRListener)}
     */
    @Deprecated
    public void setNetBin(String netBinName) {
        this.mNetBin = netBinName;
    }

    /**
     * 设置声学资源名
     * 须在init之前设置才生效, 适用于声学资源放在assets目录下
     *
     * @param resBinName 资源名
     * @deprecated 已过时 使用AILocalASRConfig统一配置 {@link #init(AILocalASRConfig, AIASRListener)}
     */
    @Deprecated
    public void setResBin(String resBinName) {
        this.mResBin = resBinName;
    }

    /**
     * 设置网络资源自定义路径, 适用于通过语法构建引擎动态生成网络资源或者网络资源放在自定义路径
     * 须在init之前设置才生效
     *
     * @param path 资源名全路径
     * @deprecated 已过时 使用AILocalASRConfig统一配置 {@link #init(AILocalASRConfig, AIASRListener)}
     */
    @Deprecated
    public void setNetBinPath(String path) {
        this.mNetBinPath = path;
    }

    /**
     * 设置声学资源名
     * 须在init之前设置才生效, 适用于声学资源放在自定义目录下
     *
     * @param path 资源名全路径
     * @deprecated 已过时 使用AILocalASRConfig统一配置 {@link #init(AILocalASRConfig, AIASRListener)}
     */
    @Deprecated
    public void setResBinPath(String path) {
        this.mResBinPath = path;
    }

    /**
     * 设置是否启用vad
     * 须在init之前设置才生效
     *
     * @param vadEnable true:使用Vad；false:禁止Vad，默认为true
     * @deprecated 已过时 使用AILocalASRConfig统一配置 {@link #init(AILocalASRConfig, AIASRListener)}
     */
    @Deprecated
    public void setVadEnable(boolean vadEnable) {
        mAsrConfig.setVadEnable(vadEnable);
        mVadConfig.setVadEnable(vadEnable);
        mVadParams.setVadEnable(vadEnable);
    }

    /**
     * 设置VAD资源名,适用于VAD资源放置在assets目录
     * 须在init之前设置才生效
     *
     * @param vadName vadName
     */
    @Deprecated
    public void setVadRes(String vadName) {
        this.mVadRes = vadName;
    }

    /**
     * 设置VAD资源名，适用于VAD资源放置在自定义目录下
     * 须在init之前设置才生效
     *
     * @param path vad资源名全路径
     */
    @Deprecated
    public void setVadResPath(String path) {
        this.mVadResPath = path;
    }

    /**
     * 设置VAD右边界
     *
     * @param pauseTime pauseTime 单位：ms,默认300
     * @deprecated 已过时 使用AILocalASRConfig统一配置 {@link #init(AILocalASRConfig, AIASRListener)}
     */
    @Deprecated
    public void setPauseTime(int pauseTime) {
        mVadConfig.setPauseTime(pauseTime);
        mVadParams.setPauseTime(pauseTime);
    }

    /**
     * 设置是否开启识别中间结果
     *
     * @param useFrameSplit true 启用,默认为false
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setUseFrameSplit(boolean useFrameSplit) {
        mAsrParams.setUseFrameSplit(useFrameSplit);
    }

    /**
     * 添加识别结果分割符,如设置 "," 识别结果显示 ： "打,开,天,窗"
     * start 之前设置生效
     *
     * @param delimiter 分割符
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setUseDelimiter(String delimiter) {
        mAsrParams.setUseWrdSep(delimiter);
    }


    /**
     * 设置是否开启置信度
     * 须在start之前设置才生效
     *
     * @param useConf true 启用,默认为false
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setUseConf(boolean useConf) {
        mAsrParams.setUseConf(useConf);
    }

    /**
     * ExpandFn 文件路径,用于动态拆分net.bin文件
     * start 之前传入有效
     *
     * @param path ExpandFn 文件的绝对路径
     *             slots 文件示例{"slot": [{"name": "DEVICE","path": "device.slot.bin" }, {"name": "WAKEUP_WORD","path": "wakeup_word.slot.bin" }]}
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setExpandFnPath(String path) {
        mAsrParams.setExpandFnPath(path);
    }

    /**
     * 设置是否启用基于语法的语义识别
     * 须在start之前设置才生效
     *
     * @param useXbnfRec true 启用，默认为false
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setUseXbnfRec(boolean useXbnfRec) {
        mAsrParams.setUseXbnfRec(useXbnfRec);
    }

    /**
     * 设置是否开启拼音输出
     * 须在start之前设置才生效
     *
     * @param usePinyin usePinyin,默认为false
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setUsePinyin(boolean usePinyin) {
        mAsrParams.setUsePinyin(usePinyin);
    }

    /**
     * 设置语种，init 之前设置生效 , 默认 {@link Languages#CHINESE}
     *
     * @param languages {@link Languages}
     * @deprecated 已过时 使用AILocalASRConfig统一配置 {@link #init(AILocalASRConfig, AIASRListener)}
     */
    @Deprecated
    public void setLanguages(Languages languages) {
        this.languages = languages;
    }

    /**
     * 设置是否使用解码网络里增加的filler路径
     *
     * @param userFiller 默认不开启
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setUseFiller(boolean userFiller) {
        mAsrParams.setUseFiller(userFiller);
    }

    /**
     * 根据误唤醒集合设置的值
     *
     * @param penaltyScore 默认 1.5，可根据实际情况再调整
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setFillerPenaltyScore(double penaltyScore) {
        mAsrParams.setFillerPenaltyScore((float) penaltyScore);
    }

    /**
     * 设置是否将本地识别结果格式化为与云端结果相同
     *
     * @param useFormat true:格式化,false 不格式化
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setUseFormat(boolean useFormat) {
        AILocalASREngine.useFormat = useFormat;
    }

    /**
     * 更新编译 xbnf 后的 netBin
     *
     * @param listener   更新接口回调 {@link AIUpdateListener}
     * @param netBinPath net.bin 资源自定义路径
     */
    @Deprecated
    public void updateNetBinPath(String netBinPath, AIUpdateListener listener) {
        if (TextUtils.isEmpty(netBinPath)) {
            Log.e(TAG, "illegal net.bin path");
            listener.failed();
        } else {
            if (aiLocalASREngine2 != null) {
                aiLocalASREngine2.updateNetBinPath(netBinPath, listener);
            }
        }
    }

    /**
     * 初始化本地识别引擎
     *
     * @param listener 本地识别回调接口
     * @see #init(AILocalASRConfig, AIASRListener)
     * @deprecated 已过时 {@link #init(AILocalASRConfig, AIASRListener)}
     */
    @Deprecated
    public void init(AIASRListener listener) {
        AILocalASRConfig config = new AILocalASRConfig.Builder()
                .setNetbinResource(!TextUtils.isEmpty(mNetBin) ? mNetBin : mNetBinPath)
                .setAcousticResources(!TextUtils.isEmpty(mResBin) ? mResBin : mResBinPath)
                .setVadResource(!TextUtils.isEmpty(mVadRes) ? mVadRes : mVadResPath)
                .setVadEnable(mVadConfig.isVadEnable())
                .setVadPauseTime(mVadParams.getPauseTime())
                .setLanguages(languages)
                .build();
        //   init(config, listener);
        aiLocalASREngine2.init(config, listener);
    }
    /**
     * 初始化本地识别引擎
     *
     * @param listener       本地识别回调接口
     * @param localASRConfig 初始化配置选项
     */
    public void init(AILocalASRConfig localASRConfig, AIASRListener listener) {
        mVadParams.setPauseTime(localASRConfig.getVadPauseTime());
        aiLocalASREngine2.init(localASRConfig, listener);
    }

    /**
     * 设置oneshot cache音频,在start之前生效
     *
     * @param cache {@link OneshotCache}
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setOneshotCache(OneshotCache<byte[]> cache) {
        if (cache != null && cache.isValid()) {
            mAsrParams.setOneshotCache(cache);
        }
    }

    /**
     * 设置无语音超时时长，单位毫秒，默认值为5000ms ；如果达到该设置值时，自动停止录音并放弃请求识别内核
     * 须在start之前设置才生效
     *
     * @param milliSecond 超时时长，单位毫秒
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
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
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setMaxSpeechTimeS(int seconds) {
        mAsrParams.setMaxSpeechTimeS(seconds);
        mVadParams.setMaxSpeechTimeS(seconds);
    }

    /**
     * 设置是否自行feed数据,不使用内部录音机(包括MockRecord和AIAudioRecord),
     * 需要在init之前调用, 默认为false
     *
     * @param useCustomFeed the useCustomFeed to set
     * @deprecated 已过时 使用AILocalASRConfig统一配置 {@link #init(AILocalASRConfig, AIASRListener)}
     */
    @Deprecated
    public void setUseCustomFeed(boolean useCustomFeed) {
        mAsrConfig.setUseCustomFeed(useCustomFeed);
        mVadConfig.setUseCustomFeed(useCustomFeed);
        mAsrParams.setUseCustomFeed(useCustomFeed);
    }

    /**
     * 设置保存的音频路径，最终的音频路径为path + local_asr_+ recordId + ".pcm"
     * 需要在start之前调用
     *
     * @param path 路径
     * @deprecated 已过时, 使用AILocalASRIntent统一配置 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void setSaveAudioPath(String path) {
        mVadParams.setSaveAudioPath(path);
        mAsrParams.setSaveAudioPath(path);
    }

    /**
     * 启动录音，开始语音识别
     *
     * @deprecated 已过时 {@link #start(AILocalASRIntent)}
     */
    @Deprecated
    public void start() {
        //转化参数使用engine2
        AILocalASRIntent aiLocalASRIntent = new AILocalASRIntent();
        aiLocalASRIntent.setUseHoldConf(mAsrParams.isUseHoldConf());
        //aiLocalASRIntent.setMode(mAsrParams.getModeTrans());

        aiLocalASRIntent.setUseCustomFeed(mAsrParams.isUseCustomFeed());
        // aiLocalASRIntent.setDynamicList(mAsrParams.getDynamicList());
        aiLocalASRIntent.setUsePinyin(mAsrParams.isUsePinyin());
        aiLocalASRIntent.setSaveAudioPath(mAsrParams.getSaveAudioPath());
        aiLocalASRIntent.setPauseTime(mVadParams.getPauseTime());
        aiLocalASRIntent.setVadEnable(mVadConfig.isVadEnable());
        aiLocalASRIntent.setUseConf(mAsrParams.isUseConf());
        aiLocalASRIntent.setExpandFnPath(mAsrParams.getExpandFnPath());
        aiLocalASRIntent.setMaxSpeechTimeS(mAsrParams.getMaxSpeechTimeS());
        aiLocalASRIntent.setNoSpeechTimeOut(mAsrParams.getNoSpeechTimeout());
        start(aiLocalASRIntent);
    }

    /**
     * 启动录音，开始语音识别
     *
     * @param localASRIntent 识别启动参数
     */
    @Deprecated
    public void start(AILocalASRIntent localASRIntent) {
        aiLocalASREngine2.start(localASRIntent);
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
                wakeupWordsJo.put("one_shot_words", jsonArray);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        fileUtil.write(wakeupWordsJo.toString().getBytes());
    }

    /**
     * 设置唤醒词列表，用于oneshot过滤唤醒词
     *
     * @param wakeupWords 唤醒词列表
     */
    @Deprecated
    public void setWakeupWords(String[] wakeupWords) {
        if (wakeupWords != null && wakeupWords.length > 0) {

            mAsrParams.setWakeupWords(wakeupWords);
            //内核接口要求传入文件路径，此处将唤醒词信息写入文件并转换成文件路径传入
            String wakeupFilePath = AISpeech.getContext().getExternalCacheDir() + File.separator +
                    "wakeupWords.json";
            writeWakeupWordsToFile(wakeupWords, wakeupFilePath);
            mAsrParams.setWakeupWordsFilePath(wakeupFilePath);
        }
    }

    /**
     * 传入数据,在不使用SDK内部录音机时调用
     *
     * @param data 音频数据流
     * @deprecated 已过时, 请使用{@link #feed(byte[])}
     */
    @Deprecated
    public void feedData(byte[] data) {
        if (data == null) {
            Log.e(TAG, "custom feed data is null !");
            return;
        }
        aiLocalASREngine2.feedData(data, data.length);
    }

    /**
     * 传入数据,在不使用SDK内部录音机时调用
     *
     * @param data 音频数据流
     */

    public void feed(byte[] data) {
        if (data == null) {
            Log.e(TAG, "custom feed data is null !");
            return;
        }
        aiLocalASREngine2.feedData(data, data.length);
    }

    /**
     * 停止录音，等待识别结果
     *
     * @deprecated 已过时, 请使用{@link #stop()}
     */
    @Deprecated
    public void stopRecording() {
        stop();
    }


    /**
     * 停止录音，等待识别结果。一般在vad.end时或送完音频时调用
     */
    public void stop() {
        if (aiLocalASREngine2 != null) {
            aiLocalASREngine2.stop();
        }
    }

    /**
     * 取消本次识别操作
     */
    public void cancel() {
        if (aiLocalASREngine2 != null) {
            aiLocalASREngine2.cancel();
        }
    }

    /**
     * 销毁本地识别引擎
     */
    public void destroy() {
        if (aiLocalASREngine2 != null) {
            aiLocalASREngine2.destroy();
        }
        mVadRes = null;
        mVadResPath = null;
        mNetBin = null;
        mNetBinPath = null;
        mResBin = null;
        mResBinPath = null;
    }


    /**
     * 语义内容归一化
     *
     * @param json 输入数据结构
     * @return json
     * @throws JSONException
     */
    private static JSONObject format(JSONObject json) throws JSONException {

        if (json.has("nlu")) {
            JSONObject formatGammar = Transformer.transGrammmer(json.optJSONObject("nlu"));
            json.put("nlu", formatGammar);
        }
        return json;
    }
}
