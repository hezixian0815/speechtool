package com.aispeech.export.engines;


import android.text.TextUtils;

import com.aispeech.common.ArrayUtils;
import com.aispeech.common.PinYinUtils;
import com.aispeech.common.Util;
import com.aispeech.export.config.AIOneshotConfig;
import com.aispeech.export.config.AIWakeupConfig;
import com.aispeech.export.exception.IllegalPinyinException;
import com.aispeech.export.intent.AIWakeupIntent;
import com.aispeech.export.listeners.AIWakeupListener;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Wakeup;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.Languages;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.config.OneshotConfig;
import com.aispeech.lite.config.WakeupConfig;
import com.aispeech.lite.param.WakeupParams;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 * 本地唤醒引擎
 */
@Deprecated
public class AIWakeupEngine {
    private final String TAG = "AIWakeupEngine";

    private com.aispeech.export.engines2.AIWakeupEngine wakeupEngine2;

    private WakeupConfig mConfig;
    private WakeupParams mParam;
    private String mResBinName;
    private String mResBinPath = "";

    private AIWakeupConfig aiWakeupConfig;
    private AIOneshotConfig aiOneshotConfig;

    // resource in assets
    final List<String> resourceInAssetsList = new ArrayList<>();

    private AIWakeupEngine() {
        wakeupEngine2 = com.aispeech.export.engines2.AIWakeupEngine.createInstance();
        mConfig = new WakeupConfig();
        mParam = new WakeupParams();
    }

    /**
     * 唤醒引擎
     *
     * @return 唤醒引擎实例
     */
    public static AIWakeupEngine createInstance() {
        return new AIWakeupEngine();
    }

    /**
     * 唤醒引擎初始化
     *
     * @param listener 唤醒相关时间的回调
     * @see #init(AIWakeupConfig, AIWakeupListener)
     * @deprecated 使用AIWakeupConfig统一配置
     */
    public void init(AIWakeupListener listener) {
        AIWakeupConfig config = new AIWakeupConfig.Builder()
                .setResBinName(TextUtils.isEmpty(mResBinName) ? mResBinPath : mResBinName)
                .setPreWakeupOn(mParam.isPreWakeupOn())
                .setLanguages(language)
                .setOneshotConfig(aiOneshotConfig)
                .build();
        init(config, listener);
    }

    public void init(AIWakeupConfig aiWakeupConfig, AIWakeupListener listener) {
        if (wakeupEngine2 != null) {
            wakeupEngine2.init(aiWakeupConfig, listener);
        }
    }


    /**
     * 设置是否使用oneshot功能,默认为false
     * 须在start之前设置才生效,默认为false
     *
     * @param config {@link AIOneshotConfig}
     * @see #init(AIWakeupConfig, AIWakeupListener) 配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setOneshot(AIOneshotConfig config) {
        this.aiOneshotConfig = config;
        mConfig.setOneshotConfig(parseOneshotConfig(config));
    }

    /**
     * 动态调整参数，具体请参照 demo
     *
     * @param wakeupWord 唤醒词，参数示例：["ni hao xiao chi","xiao bu xiao bu"]
     * @param threshold  唤醒词对应的阈值，参数示例：[0.2, 0.3]
     * @param majors     是否主唤醒词，主唤醒词为1，副唤醒词为0，如 [1,0]
     *                   设置主唤醒词后，内核会对唤醒词部分音频进行回溯
     * @throws IllegalPinyinException {@link IllegalPinyinException} 非法拼音异常
     */
    public void setWakeupWords(String[] wakeupWord, float[] threshold, int[] majors) throws IllegalPinyinException {
        if (wakeupEngine2 != null) {
            wakeupEngine2.setWakeupWords(wakeupWord, threshold, majors);
        }
    }

    /**
     * 解析oneshot config 配置
     *
     * @param config {@link AIOneshotConfig}
     * @return {@link  OneshotConfig}
     */
    private OneshotConfig parseOneshotConfig(AIOneshotConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("AIOneshotConfig can not be null!");
        }

        OneshotConfig oneshotConfig = new OneshotConfig();

        LocalVadConfig vadConfig = new LocalVadConfig();

        vadConfig.setPauseTime(0);//oneshot set vad pauseTime 0

        if (!config.getResBin().startsWith("/")) {
            vadConfig.setAssetsResNames(new String[]{config.getResBin()});
            vadConfig.setResBinPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.getResBin());
        } else {
            vadConfig.setResBinPath(config.getResBin());
        }

        oneshotConfig.setVadConfig(vadConfig);
        oneshotConfig.setCacheAudioTime(config.getCacheAudioTime());
        oneshotConfig.setMiddleTime(config.getMiddleTime());
        oneshotConfig.setWords(config.getWords());

        return oneshotConfig;

    }


    /**
     * 设置是否输入实时的长音频，默认接受长音频为true(如果是一二级唤醒，即每个唤醒词独立且非实时，则需要设置为false，如果不设置会影响性能)
     * 须在start之前设置生效
     * 当设置为false时,每次送一段音频段都会给予是否唤醒的反馈，如果没有被唤醒，则抛出wakeupWord:null, confidence:0的信息
     *
     * @param inputContinuousAudio 是否输入实时的长音频
     * @see #start(AIWakeupIntent) 在AIWakeupIntent中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setInputContinuousAudio(boolean inputContinuousAudio) {
        mParam.setContinuousAudio(inputContinuousAudio);
    }

    /**
     * 设置唤醒词列表
     *
     * @param pinyin 唤醒词的拼音，建议三到五字
     * @param thresh 阈值，0-1，可根据需求自行调整
     * @throws IllegalPinyinException 非法拼音异常,中文唤醒开启
     * @throws NumberFormatException  阈值转数字错误
     * @see #start(AIWakeupIntent) 在AIWakeupIntent中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setWakeupWord(String[] pinyin, String[] thresh) {
        setWakeupWord(pinyin, ArrayUtils.string2Float(thresh));
    }

    /**
     * 设置唤醒词列表
     *
     * @param pinyin 唤醒词的拼音，建议三到五字
     * @param thresh 阈值，0-1，可根据需求自行调整
     * @throws IllegalPinyinException 非法拼音异常,中文唤醒开启
     * @see #start(AIWakeupIntent) 在AIWakeupIntent中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setWakeupWord(String[] pinyin, float[] thresh) {
        setWakeupWord(pinyin, thresh, null);
    }

    /**
     * 设置唤醒词列表，主要跟低阈值唤醒配合使用
     *
     * @param pinyin 唤醒词的拼音，建议三到五字
     * @param thresh 阈值，0-1，可根据需求自行调整
     * @param major  设置为0，低阈值唤醒不生效；设置为1，低阈值唤醒生效
     * @throws IllegalPinyinException 非法拼音异常,中文唤醒开启
     * @throws NumberFormatException  阈值转数字错误
     * @see #start(AIWakeupIntent) 在AIWakeupIntent中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setWakeupWord(String[] pinyin, String[] thresh, String[] major) {
        setWakeupWord(pinyin, ArrayUtils.string2Float(thresh), ArrayUtils.string2Int(major));
    }

    /**
     * 设置唤醒词列表，主要跟低阈值唤醒配合使用
     *
     * @param pinyin 唤醒词的拼音，建议三到五字
     * @param thresh 阈值，0-1，可根据需求自行调整
     * @param major  设置为0，低阈值唤醒不生效；设置为1，低阈值唤醒生效
     * @throws IllegalPinyinException 非法拼音异常,中文唤醒开启
     * @see #start(AIWakeupIntent) 在AIWakeupIntent中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setWakeupWord(String[] pinyin, float[] thresh, int[] major) {

        if (language == Languages.CHINESE) {// illegal pinyin check only use for chinese
            PinYinUtils.checkPinyin(pinyin);
        }

        if (pinyin != null && pinyin.length != 0)
            mParam.setWords(pinyin);

        if (thresh != null && thresh.length != 0)
            mParam.setThresholds(thresh);

        if (major != null && major.length != 0)
            mParam.setMajors(major);


    }

    private Languages language = Languages.CHINESE;

    /**
     * 设置唤醒词语种
     *
     * @param languages {@link Languages}
     * @see #init(AIWakeupConfig, AIWakeupListener)  在AIWakeupConfig中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setLanguages(Languages languages) {
        language = languages;
    }


    /**
     * 设置唤醒env热更新，可以在引擎初始化成功后动态设置,当前只支持更新thresh
     *
     * @param setJson setJson
     *                须在start启动成功后可以调用
     */
    public void set(JSONObject setJson) {
        if (wakeupEngine2 != null) {
            wakeupEngine2.setDynamicParam(setJson);
        }
    }

    /**
     * 设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验
     * 须在start之前设置生效
     *
     * @param dcheck 校验值
     * @see #start(AIWakeupIntent) 在AIWakeupIntent中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setDcheck(String[] dcheck) {
        setDcheck(ArrayUtils.string2Int(dcheck));
    }

    /**
     * 设置唤醒是否开启校验，1表示开启校验，0表示不开启校验
     * 须在start之前设置生效
     *
     * @param dcheck 校验值
     * @see #start(AIWakeupIntent) 在AIWakeupIntent中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setDcheck(int[] dcheck) {
        mParam.setDchecks(dcheck);
    }

    /**
     * 设置是否自行feed数据,不使用内部录音机(包括MockRecord和AIAudioRecord),
     * 需要在init之前调用 默认使用内部录音机为false
     *
     * @param useCustomFeed the useCustomFeed to set
     * @see #init(AIWakeupConfig, AIWakeupListener)  在AIWakeupConfig中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setUseCustomFeed(boolean useCustomFeed) {
        mConfig.setUseCustomFeed(useCustomFeed);
    }

    /**
     * 设置是否自行feed数据,不使用内部录音机(包括MockRecord和AIAudioRecord),
     * 需要在init之前调用
     * 默认使用内部录音机为true
     *
     * @param useRecord the useRecord to set
     * @see #setUseCustomFeed(boolean)
     */
    @Deprecated
    public void setUseRecord(boolean useRecord) {
        mConfig.setUseCustomFeed(!useRecord);
    }

    /**
     * 设置不使用内部录音机时可用，自行feed音频数据
     *
     * @param data 音频数据
     * @see #setUseCustomFeed(boolean)
     */
    @Deprecated
    public void feed(byte[] data) {
        feedData(data, data.length);
    }

    /**
     * 设置不使用内部录音机时可用，自行feed音频数据
     *
     * @param data 音频数据
     * @param size 音频数据大小
     * @see #setUseCustomFeed(boolean)
     */
    public void feedData(byte[] data, int size) {
        if (wakeupEngine2 != null) {
            wakeupEngine2.feedData(data, size);
        }
    }

    /**
     * 开启唤醒，如果使用内部录音机的话一并开启
     *
     * @see #start(AIWakeupIntent)
     * @deprecated 已过时, 不推荐使用
     */
    public void start() {
        start(parseParams());
    }

    public void start(AIWakeupIntent wakeupIntent) {
        if (wakeupEngine2 != null) {
            wakeupEngine2.start(wakeupIntent);
        }
    }

    /**
     * 关闭唤醒，如果使用内部录音机的话一并关闭
     */
    public void stop() {
        if (wakeupEngine2 != null) {
            wakeupEngine2.stop();
        }
    }


    /**
     * 销毁唤醒内核和录音机
     */
    public void destroy() {
        if (wakeupEngine2 != null) {
            wakeupEngine2.destroy();
        }
        if (mConfig != null) {
            mConfig = null;
        }
        if (mParam != null) {
            mParam = null;
        }
        if (aiWakeupConfig != null) {
            aiWakeupConfig = null;
        }
    }

    private AIWakeupIntent parseParams() {
        AIWakeupIntent intent = new AIWakeupIntent();
        intent.setInputContinuousAudio(mParam.inputContinuousAudio());
        intent.setWakeupWord(mParam.getWords(), ArrayUtils.string2Float(mParam.getThreshold()));

        if (mParam.getMajors() != null) intent.setMajors(mParam.getMajors());
        if (mParam.getDcheck() != null) intent.setDcheck(mParam.getDcheck());


        return intent;
    }

    /**
     * 设置唤醒资源的名字，适用于把唤醒资源放在assets目录，默认名字为wakeup.bin
     *
     * @param resBin 唤醒资源的名字
     * @see #init(AIWakeupConfig, AIWakeupListener)  在AIWakeupConfig中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setResBin(String resBin) {
        mResBinName = resBin;
    }

    /**
     * 设置唤醒资源的绝对路径
     *
     * @param resBinPath 路径包括文件名
     * @see #init(AIWakeupConfig, AIWakeupListener)  在AIWakeupConfig中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setResBinPath(String resBinPath) {
        this.mResBinPath = resBinPath;
    }


    /**
     * 设置低阈值唤醒回调开关
     * 打开后：1.onPreWakeup回调   2.只要触发低阈值，onPreWakeup一定会回调，但onWakeup不一定会回调
     *
     * @param preWakeupOn 布尔值
     * @see #init(AIWakeupConfig, AIWakeupListener)  在AIWakeupConfig中配置此参数
     * @deprecated 已过时, 不推荐使用
     */
    public void setPreWakeupOn(boolean preWakeupOn) {
        this.mParam.setPreWakeupOn(preWakeupOn);
    }

    /**
     * 检查唤醒内核是否准备好，外部可以不关注
     *
     * @return true or false
     */
    public static boolean checkLibValid() {
        return Wakeup.isWakeupSoValid() && Utils.isUtilsSoValid();
    }

}
