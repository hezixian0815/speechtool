package com.aispeech.export.intent;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.lite.AIType;
import com.aispeech.lite.ResourceType;
import com.aispeech.lite.base.BaseIntent;
import com.aispeech.lite.oneshot.OneshotCache;

/**
 * 云端对话引擎启动参数
 *
 * @author hehr
 */
public class AICloudDMIntent extends BaseIntent {

    private static final String TAG = "AICloudDMIntent";

    /**
     * 指定语段服务类别，默认对话结果，可单独指定asr、nlu
     * nlu 字段默认
     */
    private AIType aiType = AIType.DM;

    /**
     * 登录用户id
     */
    private String userId;

    /**
     * oneshot cache 音频
     */
    OneshotCache oneshotCache = null;
    /**
     * 是否启用标点符号
     */
    boolean enablePunctuation = false;

    /**
     * 设置主唤醒词,用于oneshot场景识别过滤唤醒词。如："你好小弛,你好小乐"
     */
    String[] wakeupWords = {};

    /**
     * 设置对话的上下文id
     */
    private String sessionId = "";

    /**
     * 设置无语音超时时长，单位毫秒，默认值为5000ms ；如果达到该设置值时，自动停止录音
     */
    int noSpeechTimeOut = 5000;

    /**
     * 设置音频最大录音时长，达到该值将取消语音引擎并抛出异常<br>
     * 允许的最大录音时长 单位秒
     */
    int MaxSpeechTimeS = 60;

    /**
     * 设置云端 vad 静音检查时长，单位 ms
     * 云端默认 500 ms
     */
    int cloudVadPauseTime = -1;

    /**
     * 设置VAD右边界
     */
    int pauseTime = 300;
    /**
     * 是否打开实时识别
     */
    boolean useRealback = true;

    /**
     * 设置识别Alignment功能，默认false
     */
    boolean enableAlignment = false;
    /**
     * 设置打开识别结果情绪识别功能，默认false，关闭
     * 需要在init之前打开
     * emotion 取值范围 ： [sad, happy, angry,neutral]
     * gender 取值范围 ： [female, male]
     * age 取值范围 ：  [child，adult, elder]
     */
    boolean enableEmotion = false;

    /**
     * 设置打开云端音频检测功能，默认false 关闭
     */
    boolean enableAudioDetection = false;
    /**
     * 设置保存的音频路径，最终的音频路径为path + recordId + ".pcm"
     */
    String saveAudioPath;

    /**
     * 识别结果中文转阿拉伯数字
     */
    private boolean enableNumberConvert = true;

    /**
     * 识别结果开启大写
     */
    private boolean enableRecUppercase = false;

    /**
     * 词库识别、语义是否开启转换，开启后，会对含英文等词库统一加工后送识别，得到结果后还原原始词库
     */
    private boolean enableVocabsConvert = true;

    /**
     * 识别引擎的资源类型,默认为aicar
     */
    private String res = "aicar";

    /**
     * 请求语义文本
     */
    private String refText = "";

    /**
     * 是否使用识别服务内置的vad模块检测，默认是 true
     */
    private boolean enableVAD = true;

    /**
     * 是否开启音标显示
     */
    private boolean enableTone;


    /**
     * 自定义唤醒词阈值
     */
    private int customWakeupScore = -6;

    private boolean vadEnable = false; //识别服务内部的vad开关；全双工下，务必关闭此vad，不然可能引起缺字问题
    private boolean enableCloudVAD = true;//全双工下，单独vad服务，全双工下， 云端一定为true
    private boolean enableShowText = false; // 开启后 会返 showText 字段，用于上屏，例如展示阿拉伯数字

    public boolean isEnableShowText() {
        return enableShowText;
    }

    public void setEnableShowText(boolean enableShowText) {
        this.enableShowText = enableShowText;
    }

    public int getCustomWakeupScore() {
        return customWakeupScore;
    }

    public void setCustomWakeupScore(int customWakeupScore) {
        this.customWakeupScore = customWakeupScore;
    }


    public String getRefText() {
        return refText;
    }

    public void setRefText(String refText) {
        this.refText = refText;
    }

    public boolean isEnableRecUppercase() {
        return enableRecUppercase;
    }

    /**
     * 识别结果英文转大写
     *
     * @param enableRecUppercase boolean
     */
    public void setEnableRecUppercase(boolean enableRecUppercase) {
        this.enableRecUppercase = enableRecUppercase;
    }

    public boolean isEnableVocabsConvert() {
        return enableVocabsConvert;
    }

    /**
     * 词库识别、语义是否开启转换
     * 开启后，对含英文、数字、特殊字符等词库统一加工后送识别，得到识别结果后还原原始词库
     *
     * @param enableVocabsConvert boolean
     */
    public void setEnableVocabsConvert(boolean enableVocabsConvert) {
        this.enableVocabsConvert = enableVocabsConvert;
    }

    /**
     * 设置识别引擎的资源类型,默认为aicar
     *
     * @param resourceType 取值如：comm, aicar,airobot, aihome, custom
     * @deprecated use {@link #setRes(String)}
     */
    public void setResourceType(ResourceType resourceType) {
        this.res = resourceType.value;
    }

    /**
     * 设置识别引擎的资源类型,默认为aicar
     *
     * @param res 取值如：comm, aicar,airobot, aihome, custom
     */
    public void setRes(String res) {
        this.res = res;
    }

    public String getRes() {
        return res;
    }

    /**
     * 识别结果中文转阿拉伯数字，如：三点半---3点半
     *
     * @param enableNumberConvert boolean
     */
    public void setEnableNumberConvert(boolean enableNumberConvert) {
        this.enableNumberConvert = enableNumberConvert;
    }

    /**
     * 设置oneshot cache 音频数据
     *
     * @param cache {@link OneshotCache}
     */
    public void setOneshotCache(OneshotCache cache) {
        if (cache != null && cache.isValid()) {
            this.oneshotCache = cache;
        } else {
            Log.e(TAG, " drop invalid oneshot cache ");
        }
    }

    /**
     * 设置是否启用标点符号
     *
     * @param enablePunctuation boolean 默认 true
     */
    public void setEnablePunctuation(boolean enablePunctuation) {
        this.enablePunctuation = enablePunctuation;
    }

    /**
     * 设置唤醒词,用于oneshot场景识别过滤唤醒词。如："你好小弛,你好小乐"
     *
     * @param wakeupWords 唤醒词列表
     */
    public void setWakeupWords(String[] wakeupWords) {
        this.wakeupWords = wakeupWords;
    }

    /**
     * 设置对话sessionId,服务端通过相同的sessionId关联多轮请求的上下文,首轮对话请求不需要携带；
     * 非首轮对话请求取值是上一轮服务端返回结果中的sessionId
     *
     * @param sessionId 上下文Id
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 设置无语音超时时长
     *
     * @param noSpeechTimeOut 默认值为5000ms,单位毫秒
     */
    public void setNoSpeechTimeOut(int noSpeechTimeOut) {
        this.noSpeechTimeOut = noSpeechTimeOut;
    }

    /**
     * 设置单次允许的最大录音时长
     *
     * @param maxSpeechTimeS 默认60s,单位秒
     */
    public void setMaxSpeechTimeS(int maxSpeechTimeS) {
        MaxSpeechTimeS = maxSpeechTimeS;
    }

    /**
     * vad右边界
     *
     * @param pauseTime 默认300ms
     */
    public void setPauseTime(int pauseTime) {
        this.pauseTime = pauseTime;
    }

    /**
     * 设置是否启用实时识别结果
     *
     * @param useRealback boolean , 默认 true
     */
    public void setUseRealback(boolean useRealback) {
        this.useRealback = useRealback;
    }

    /**
     * 设置识别Alignment功能
     *
     * @param enableAlignment boolean ,默认 false
     */
    public void setEnableAlignment(boolean enableAlignment) {
        this.enableAlignment = enableAlignment;
    }

    /**
     * 设置打开识别结果情绪识别功能,同时抛出情绪，年龄，性别结果
     * emotion 取值范围 ： [sad, happy, angry,neutral]
     * gender 取值范围 ： [female, male]
     * age 取值范围 ：  [child，adult, elder]
     *
     * @param enableEmotion boolean , 默认false
     */
    public void setEnableEmotion(boolean enableEmotion) {
        this.enableEmotion = enableEmotion;
    }

    /**
     * 设置打开云端音频检测功能
     *
     * @param enableAudioDetection boolean 默认 false
     */
    public void setEnableAudioDetection(boolean enableAudioDetection) {
        this.enableAudioDetection = enableAudioDetection;
    }

    /**
     * 设置音频存储路径，用于DEBUG版本，release版本不建议打开此配置
     *
     * @param saveAudioPath 音频路径
     */
    public void setSaveAudioPath(String saveAudioPath) {
        this.saveAudioPath = saveAudioPath;
    }

    /**
     * 设置AIType 参数
     *
     * @param type {@link AIType}
     */
    public void setAIType(AIType type) {
        this.aiType = type;
    }

    public AIType getAIType() {
        return aiType;
    }


    public OneshotCache getOneshotCache() {
        return oneshotCache;
    }

    public boolean isEnablePunctuation() {
        return enablePunctuation;
    }

    public String[] getWakeupWords() {
        return wakeupWords;
    }

    public String getStrWakeupWords() {
        return arrayToStr(getWakeupWords());
    }

    public String getSessionId() {
        if (TextUtils.isEmpty(sessionId)) {
            sessionId = Util.uuid();
        }
        return sessionId;
    }

    public int getNoSpeechTimeOut() {
        return noSpeechTimeOut;
    }

    public int getMaxSpeechTimeS() {
        return MaxSpeechTimeS;
    }

    public int getPauseTime() {
        return pauseTime;
    }

    public boolean isUseRealback() {
        return useRealback;
    }

    public boolean isEnableAlignment() {
        return enableAlignment;
    }

    public boolean isEnableEmotion() {
        return enableEmotion;
    }

    public boolean isEnableAudioDetection() {
        return enableAudioDetection;
    }

    public String getSaveAudioPath() {
        return saveAudioPath;
    }

    public boolean isEnableNumberConvert() {
        return enableNumberConvert;
    }

    public int getCloudVadPauseTime() {
        return cloudVadPauseTime;
    }

    public void setCloudVadPauseTime(int cloudVadPauseTime) {
        this.cloudVadPauseTime = cloudVadPauseTime;
    }

    /**
     * 设置是否使用识别服务内置的vad模块检测
     *
     * @param enableVAD true 使用内置的vad模块检测 ,false 不使用
     */
    public void setEnableVAD(boolean enableVAD) {
        this.enableVAD = enableVAD;
    }

    /**
     * 返回是否使用识别服务内置的vad模块检测
     *
     * @return true 使用内置的vad模块检测 ,false 不使用
     */
    public boolean isEnableVAD() {
        return enableVAD;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isEnableTone() {
        return enableTone;
    }

    public void setEnableTone(boolean enableTone) {
        this.enableTone = enableTone;
    }

    public boolean isVadEnable() {
        return vadEnable;
    }

    public void setVadEnable(boolean vadEnable) {
        this.vadEnable = vadEnable;
    }

    public boolean isEnableCloudVAD() {
        return enableCloudVAD;
    }

    public void setEnableCloudVAD(boolean enableCloudVAD) {
        this.enableCloudVAD = enableCloudVAD;
    }

    /**
     * array to str
     *
     * @param array
     * @return str
     */
    private String arrayToStr(String[] array) {
        StringBuilder buffer = new StringBuilder();
        for (String s : array) {
            buffer.append(s + ",");
        }
        if (buffer.length() > 0) {
            buffer.deleteCharAt(buffer.length() - 1);//祛除尾部字符 ,
        }
        return buffer.toString();
    }

}