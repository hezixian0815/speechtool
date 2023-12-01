package com.aispeech.export.engines;

import com.aispeech.export.config.AICloudASRConfig;
import com.aispeech.export.intent.AICloudASRIntent;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Vad;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.param.CloudASRParams;

/**
 * 云端识别引擎
 *
 * @deprecated {@link com.aispeech.export.engines2.AICloudASREngine}
 */
@Deprecated
public class AICloudASREngine {

    public static final String TAG = "AICloudASREngine";

    private com.aispeech.export.engines2.AICloudASREngine mAICloudASREngine;
    private AICloudASRIntent mAICloudASRIntent;
    private AICloudASRConfig.Builder mAICloudASRConfigBuilder;

    private AICloudASREngine() {
        mAICloudASREngine = com.aispeech.export.engines2.AICloudASREngine.createInstance();
        mAICloudASRIntent = new AICloudASRIntent();
        mAICloudASRConfigBuilder = new AICloudASRConfig.Builder();
    }

    /**
     * 创建实例
     *
     * @return AICloudASREngine实例
     */
    public static AICloudASREngine createInstance() {
        return new AICloudASREngine();
    }


    public static boolean checkLibValid() {
        return Vad.isSoValid() && Utils.isUtilsSoValid();
    }

    /**
     * 初始化云端识别引擎。
     *
     * @param listener 语音识别回调接口
     * @deprecated 已过时
     */
    public void init(AIASRListener listener) {
        init(mAICloudASRConfigBuilder.build(), listener);
    }

    public void init(AICloudASRConfig cloudASRConfig, AIASRListener listener) {
        if (mAICloudASREngine != null) {
            mAICloudASREngine.init(cloudASRConfig, listener);
        }
    }

    /**
     * 启动录音，开始语音识别
     *
     * @deprecated 已过时 {@link #start(AICloudASRIntent)}
     */
    public void start() {
        if (mAICloudASREngine != null) {
            mAICloudASREngine.start(mAICloudASRIntent);
        }
    }

    public void start(AICloudASRIntent intent) {
        if (mAICloudASREngine != null) {
            mAICloudASREngine.start(intent);
        }
    }

    /**
     * 停止录音，等待识别结果
     */
    public void stopRecording() {
        if (mAICloudASREngine != null) {
            mAICloudASREngine.stop();
        }
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     */
    @Deprecated
    public void feedData(byte[] data) {
        if (mAICloudASREngine != null) {
            mAICloudASREngine.feedData(data, data.length);
        }
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     * @param size 音频数据大小
     */
    public void feedData(byte[] data, int size) {
        if (mAICloudASREngine != null) {
            mAICloudASREngine.feedData(data, size);
        }
    }

    /**
     * 取消本次识别操作
     */
    public void cancel() {
        if (mAICloudASREngine != null) {
            mAICloudASREngine.cancel();
        }
    }

    /**
     * 销毁云端识别引擎
     */
    public void destroy() {
        if (mAICloudASREngine != null) {
            mAICloudASREngine.destroy();
        }
        mAICloudASRIntent = null;
        mAICloudASRConfigBuilder = null;
    }

    /**
     * 设置等待识别结果超时时间 单位毫秒，小于或等于0则不设置超时，默认5000ms.
     * 从vad结束或者用户主动调用stop方法开始计时
     *
     * @param time 超时时长
     *             须在start之前设置才生效
     * @see #init(AICloudASRConfig, AIASRListener)
     * @deprecated 已过时, 使用AICloudASRConfig统一配置
     */
    public void setWaitingTimeout(int time) {
        mAICloudASRIntent.setWaitingTimeout(time);
    }


    /**
     * 告知识别引擎已经唤醒，该接口在oneshot功能中使用，内部会记录唤醒的时间点，
     * 之后在vad end的时候来判断到底用户说的是不是唤醒词+指令，还是只有唤醒词
     */
    public void notifyWakeup() {
        if (mAICloudASREngine != null) {
            mAICloudASREngine.notifyWakeup();
        }
    }
//
//    /**
//     * 设置服务器地址，默认不用设置
//     * 需要在init之前调用
//     *
//     * @param server 服务器地址，包含ws://
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setServer(String server) {
//        mAICloudASRConfigBuilder.setServer(server);
//    }
//
//    /**
//     * 设置用户标示
//     * 需要在init之前调用
//     *
//     * @param userId 用户标示
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setUserId(String userId) {
//        mAICloudASRConfigBuilder.setUserId(userId);
//    }
//
//
//    /**
//     * 设置识别lmid
//     * 需要在init之前调用
//     *
//     * @param lmId 　custom　lmid
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setLmId(String lmId) {
//        mAICloudASRConfigBuilder.setLmId(lmId);
//    }
//    /**
//     * 设置设备Id
//     * 需要在start之前调用
//     *
//     * @param deviceId deviceId
//     */
//    @Deprecated
//    public void setDeviceId(String deviceId) {
//        mAICloudASRIntent.setDeviceId(deviceId);
//    }


//    /**
//     * 设置是否启用本地vad，一般都会打开,需要在init之前调用
//     *
//     * @param vadEnable 默认为true
//     *                  true:使用Vad；false:禁止Vad
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setLocalVadEnable(boolean vadEnable) {
//        mAICloudASRConfigBuilder.setLocalVadEnable(vadEnable);
//    }
//
//    /**
//     * 设置云端唤醒词阈值，需要在init之前调用
//     *
//     * @param customWakeupScore 默认为-6
//     * @deprecated 已过时
//     */
//    public void setCustomWakeupScore(int customWakeupScore) {
//        mAICloudASRConfigBuilder.setCustomWakeupScore(customWakeupScore);
//    }
//
//
//    /**
//     * 设置是否启用标点符号识别，需要在init之前调用
//     *
//     * @param enablePunctuation 默认为false
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setEnablePunctuation(boolean enablePunctuation) {
//        mAICloudASRConfigBuilder.setEnablePunctuation(enablePunctuation);
//    }
//
//    /**
//     * 设置是否启用识别结果汉字数字转阿拉伯数字功能，需要在init之前调用
//     *
//     * @param enableNumberConvert 默认为false
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setEnableNumberConvert(boolean enableNumberConvert) {
//        mAICloudASRConfigBuilder.setEnableNumberConvert(enableNumberConvert);
//    }
//
//    /**
//     * 设置自定义唤醒词得分
//     * 需要在init之前调用,默认为0
//     *
//     * @param selfCustomWakeupScore 自定义唤醒词得分
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setSelfCustomWakeupScore(int selfCustomWakeupScore) {
//        mAICloudASRConfigBuilder.setSelfCustomWakeupScore(selfCustomWakeupScore);
//    }
//
//
//    /**
//     * 设置自定义唤醒词，可用于过滤和指定唤醒词识别比如 ["你好小乐","你好小白"]
//     * 需要和{@link AICloudASREngine#setWakeupWordVisible(boolean)}结合使用
//     * 需要在init之前调用
//     *
//     * @param customWakeupWord customWakeupWord
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setCustomWakeupWord(JSONArray customWakeupWord) {
//        mAICloudASRConfigBuilder.setCustomWakeupWord(customWakeupWord);
//    }
//
//
//    /**
//     * 设置是oneshot是否过滤句首唤醒词，比如音频输入为"你好小驰，今天天气怎么样"
//     * setWakeupWord传入"你好小驰"后，setWakeupWordVisible设置为true后识别结果即为"今天天气怎么样"
//     * 需要和{@link AICloudASREngine#setCustomWakeupWord(JSONArray)}结合使用
//     * 需要在init之前调用
//     *
//     * @param wakeupWordVisible 是否要过滤唤醒词，默认为false，不过滤
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setWakeupWordVisible(boolean wakeupWordVisible) {
//        mAICloudASRConfigBuilder.setWakeupWordVisible(wakeupWordVisible);
//    }
//
//    /**
//     * 设置音调功能，默认为false,关闭
//     * 需要在init之前调用
//     *
//     * @param enableTone enableTone
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setEnableTone(boolean enableTone) {
//        mAICloudASRConfigBuilder.setEnableTone(enableTone);
//    }
//
//    /**
//     * 设置识别Alignment功能，默认false,关闭
//     * 需要在init之前打开
//     *
//     * @param enableAlignment enableAlignment
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setEnableAlignment(boolean enableAlignment) {
//        mAICloudASRConfigBuilder.setEnableAlignment(enableAlignment);
//    }
//
//    /**
//     * 设置打开识别结果情绪识别功能，默认false，关闭
//     * 需要在init之前打开
//     * emotion 取值范围 ： [sad, happy, angry,neutral]
//     * gender 取值范围 ： [female, male]
//     * age 取值范围 ：  [child，adult, elder]
//     *
//     * @param enableEmotion enableEmotion
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setEnableEmotion(boolean enableEmotion) {
//        mAICloudASRConfigBuilder.setEnableEmotion(enableEmotion);
//    }
//
//    /**
//     * 设置打开云端音频检测功能，默认false 关闭
//     * 需要在init之前打开
//     *
//     * @param enableAudioDetection enableAudioDetection
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setEnableAudioDetection(boolean enableAudioDetection) {
//        mAICloudASRConfigBuilder.setEnableAudioDetection(enableAudioDetection);
//    }
//
//    /**
//     * 设置语言分类功能，默认为false，关闭
//     * 需要在init之前调用
//     *
//     * @param enableLanguageClassifier enableLanguageClassifier
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setEnableLanguageClassifier(boolean enableLanguageClassifier) {
//        mAICloudASRConfigBuilder.setEnableLanguageClassifier(enableLanguageClassifier);
//    }
//
//    /**
//     * 设置rec结果增加对齐信息，默认为false,关闭
//     * 需要在init之前调用
//     *
//     * @param enableSNTime enableSNTime
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setEnableSNTime(boolean enableSNTime) {
//        mAICloudASRConfigBuilder.setEnableSNTime(enableSNTime);
//    }
//
//
//    /**
//     * 设置识别引擎的资源类型,默认为comm
//     * 需要在init之前调用
//     *
//     * @param type 取值如：comm, airobot, aihome, custom
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setResourceType(String type) {
//        mAICloudASRConfigBuilder.setResourceType(type);
//    }

    /**
     * 设置无语音超时时长，单位毫秒，默认值为5000ms ；如果达到该设置值时，自动停止录音
     * 设置为0表示不进行语音超时判断
     * 需要在start之前调用
     *
     * @param milliSecond 超时时长，单位毫秒
     * @see CloudASRParams#setNoSpeechTimeout(int)
     * @see #start(AICloudASRIntent)
     * @deprecated 已过时, 使用AICloudASRIntent统一配置
     */
    public void setNoSpeechTimeOut(int milliSecond) {
        mAICloudASRIntent.setNoSpeechTimeOut(milliSecond);
    }

    /**
     * 设置音频最大录音时长，达到该值将取消语音引擎并抛出异常<br>
     * 允许的最大录音时长 单位秒
     * 需要在start之前调用
     * <ul>
     * <li>0 表示无最大录音时长限制</li>
     * <li>默认大小为60S</li>
     * </ul>
     *
     * @param seconds seconds
     * @see CloudASRParams#setMaxSpeechTimeS(int)
     * @see #start(AICloudASRIntent)
     * @deprecated 已过时, 使用AICloudASRIntent统一配置
     */
    public void setMaxSpeechTimeS(int seconds) {
        mAICloudASRIntent.setMaxSpeechTimeS(seconds);
    }


    /**
     * 设置VAD资源的绝对路径,包括文件名,
     * 需要在init之前调用
     *
     * @param vadResBinPath vadResBinPath
     * @see #init(AICloudASRConfig, AIASRListener)
     * @deprecated 已过时, 使用AICloudASRConfig统一配置
     */
    public void setVadResBinPath(String vadResBinPath) {
        mAICloudASRConfigBuilder.setVadResource(vadResBinPath);
    }


    /**
     * 设置VAD资源名字
     * 需要在init之前调用
     *
     * @param vadRes vad资源名
     * @see #init(AICloudASRConfig, AIASRListener)
     * @deprecated 已过时, 使用AICloudASRConfig统一配置
     */
    public void setVadResource(String vadRes) {
        mAICloudASRConfigBuilder.setVadResource(vadRes);
    }

    /**
     * 设置oneshot cache音频,在start之前生效
     *
     * @param cache {@link OneshotCache}
     * @see #start(AICloudASRIntent)
     * @deprecated 已过时, 使用AICloudASRIntent统一配置
     */
    public void setOneshotCache(OneshotCache<byte[]> cache) {
        mAICloudASRIntent.setOneshotCache(cache);
    }

//    /**
//     * 设置是否使用实时反馈功能
//     * 需要在init之前调用
//     *
//     * @param realback realback 默认为false
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setRealback(boolean realback) {
//        mAICloudASRConfigBuilder.setRealBack(realback);
//    }
//
//    /**
//     * 设置是否开启服务端的vad功能，一般近场关闭，远场打开
//     * 需要在init之前调用
//     *
//     * @param cloudVadEnable cloudVadEnable 默认为true
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setCloudVadEnable(boolean cloudVadEnable) {
//        mAICloudASRConfigBuilder.setCloudVadEnable(cloudVadEnable);
//    }
//
//
//    /**
//     * 设置nbest
//     *
//     * @param nbest nbest，默认为0
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setNbest(int nbest) {
//        mAICloudASRConfigBuilder.setNBest(nbest);
//    }
//
//    /**
//     * 设置保存的音频路径，最终的音频路径为path + recordId + ".ogg"
//     * 需要在start之前调用
//     *
//     * @param path 路径
//     * @see #start(AICloudASRIntent)
//     * @deprecated 已过时, 使用AICloudASRIntent统一配置
//     */
//    public void setSaveAudioPath(String path) {
//        mAICloudASRIntent.setSaveAudioPath(path);
//    }
//
//    /**
//     * 设置VAD右边界
//     * 需要在init之前调用
//     *
//     * @param pauseTime pauseTime 单位为ms,默认为300ms
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setPauseTime(int pauseTime) {
//        mAICloudASRConfigBuilder.setVadPauseTime(pauseTime);
//    }
//
//
//    /**
//     * 设置是否自行feed数据,不使用内部录音机(包括MockRecord和AIAudioRecord),
//     * 需要在init之前调用
//     *
//     * @param useCustomFeed the useCustomFeed to set
//     * @see #init(AICloudASRConfig, AIASRListener)
//     * @deprecated 已过时, 使用AICloudASRConfig统一配置
//     */
//    public void setUseCustomFeed(boolean useCustomFeed) {
//        mAICloudASRConfigBuilder.setUseCustomFeed(useCustomFeed);
//        mAICloudASRIntent.setUseCustomFeed(useCustomFeed);
//    }
//
//    /**
//     * 设置产品号
//     * 需要在start之前调用
//     *
//     * @param productId productId dui平台网页中的productId
//     */
//    @Deprecated
//    public void setProductId(String productId) {
//        mAICloudASRIntent.setProductId(productId);
//    }

}
