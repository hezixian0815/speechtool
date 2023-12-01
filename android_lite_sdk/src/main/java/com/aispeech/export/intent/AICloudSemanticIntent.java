package com.aispeech.export.intent;


import android.text.TextUtils;

import com.aispeech.common.Util;
import com.aispeech.lite.AIType;
import com.aispeech.lite.ResourceType;


/**
 * 云端语义引擎启动参数
 *
 * @author hehr
 */
public class AICloudSemanticIntent {

    /**
     * 用户id
     */
    private String userId;

    /**
     * 设置是否启用标点符号识别
     */
    private boolean enablePunctuation = false;
    /**
     * 设置是否开始语义nbest 结果
     */
    private boolean enableNBest = false;
    /**
     * 设置唤醒词，
     */
    private String wakeupWords = "";
    /**
     * 设置对话的上下文id
     */
    private String sessionId = "";
    /**
     * 设置无语音超时时长,单位毫秒,默认 5000 ms
     */
    private int noSpeechTimeOut = 5000;
    /**
     * 设置音频最大录音时长，达到该值将取消语音引擎并抛出异常 ,默认 60s
     */
    private int MaxSpeechTimeS = 60;
    /**
     * vad pauseTime ,默认 300 ms
     */
    private int pauseTime = 300;
    /**
     * 设置是否启用实时反馈,默认true
     */
    private boolean useRealBack = true;
    /**
     * 设置音频文件存储路径
     */
    private String saveAudioPath = "";
    /**
     * 请求语义文本
     */
    private String refText = "";

    private AIType type = AIType.NLU; //默认nlu 模式

    /**
     * 在非首轮中可能用到，用于指定调用端使用了上一轮nbest结果中的哪一个skill
     */
    private String SkillId = "";
    /**
     * 在非首轮中可能用到，用于指定调用端使用了上一轮nbest结果中的哪一个task
     */
    private String task = "";

//    /**
//     * 情绪识别
//     */
//    private boolean enableEmotion = false;
//    /**
//     * alignment
//     */
//    private boolean enableAlignment = false;
//    /**
//     * 识别音频检测
//     */
//    private boolean enableAudioDetection = false;

    /**
     * 识别结果中文转阿拉伯数字
     */
    private boolean enableNumberConvert = true;

    /**
     * 识别引擎的资源类型,默认为aicar
     */
    private ResourceType resourceType = ResourceType.AICAR;

    /**
     * 识别结果开启大写
     */
    private boolean enableRecUppercase = false;

    /**
     * 词库识别、语义是否开启转换，开启后，会对含英文等词库统一加工后送识别，得到结果后还原原始词库
     */
    private boolean enableVocabsConvert = false;


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

    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * 设置识别引擎的资源类型,默认为aicar
     *
     * @param resourceType 取值如：comm, aicar,airobot, aihome, custom
     */
    public void setResourceType(ResourceType resourceType) {
        this.resourceType = resourceType;
    }

    /**
     * 设置是否启动标点符号,默认true
     *
     * @param enablePunctuation boolean
     */
    public void setEnablePunctuation(boolean enablePunctuation) {
        this.enablePunctuation = enablePunctuation;
    }


    /**
     * 识别结果中文转阿拉伯数字，如：三点半---3点半
     *
     * @param enableNumberConvert boolean
     */
    public void setEnableNumberConvert(boolean enableNumberConvert) {
        this.enableNumberConvert = enableNumberConvert;
    }

    //    /**
//     * 设置识别Alignment功能，默认false,关闭
//     * 需要在init之前打开
//     *
//     * @param enableAlignment enableAlignment
//     */
//    public void setEnableAlignment(boolean enableAlignment) {
//        this.enableAlignment = enableAlignment;
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
//     */
//    public void setEnableEmotion(boolean enableEmotion) {
//        this.enableEmotion = enableEmotion;
//    }
//
//    /**
//     * 设置打开云端音频检测功能，默认false 关闭
//     * 需要在init之前打开
//     *
//     * @param enableAudioDetection enableAudioDetection
//     */
//    public void setEnableAudioDetection(boolean enableAudioDetection) {
//        this.enableAudioDetection = enableAudioDetection;
//    }

    /**
     * 设置是否返回语义 Nbest 结果 , 默认 false
     *
     * @param enableNBest boolean
     */
    public void setEnableNBest(boolean enableNBest) {
        this.enableNBest = enableNBest;
    }

    /**
     * 设置唤醒词,用于oneshot场景使用。如："你好小弛,你好小乐"
     *
     * @param wakeupWords 唤醒词
     */
    public void setWakeupWords(String wakeupWords) {
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
     * 设置无语音超时时长，单位毫秒，默认值为 5000 ms ，如达到该设置值时，自动停止录音
     * 设置为0表示不进行语音超时判断
     *
     * @param noSpeechTimeOut 超时时间
     */
    public void setNoSpeechTimeOut(int noSpeechTimeOut) {
        this.noSpeechTimeOut = noSpeechTimeOut;
    }

    /**
     * 设置音频最大录音时长，默认大小为60S ， 达到该值将取消语音引擎并抛出异常
     * 0 表示无最大录音时长限制
     *
     * @param maxSpeechTimeS seconds
     */
    public void setMaxSpeechTimeS(int maxSpeechTimeS) {
        MaxSpeechTimeS = maxSpeechTimeS;
    }

    /**
     * 设置内置vad的右边界时常，默认 300 ms
     *
     * @param pauseTime 时常
     */
    public void setPauseTime(int pauseTime) {
        this.pauseTime = pauseTime;
    }

    /**
     * 设置是否启用实时反馈,默认true
     *
     * @param useRealBack boolean
     */
    public void setUseRealBack(boolean useRealBack) {
        this.useRealBack = useRealBack;
    }

    /**
     * 设置引擎保存音频路径，默认不保存，不推荐在release版本打开上述配置
     *
     * @param path 音频存储路径
     */
    public void setSaveAudioPath(String path) {
        this.saveAudioPath = path;
    }

    /**
     * 设置识别文本
     *
     * @param refText 识别文本
     */
    public void setRefText(String refText) {
        this.refText = refText;
    }

    /**
     * 设置AIType 参数
     *
     * @param type {@link AIType}
     */
    public void setType(AIType type) {
        this.type = type;
    }

    /**
     * 在非首轮中可能用到，用于指定调用端使用了上一轮nbest结果中的哪一个skill
     *
     * @param skillId String
     */
    public void setSkillId(String skillId) {
        SkillId = skillId;
    }

    /**
     * 在非首轮中可能用到，用于指定调用端使用了上一轮nbest结果中的哪一个task
     *
     * @param task String
     */
    public void setTask(String task) {
        this.task = task;
    }

    /*************************************************   分割线  *************************************/

    /**
     * 是否启用标点符号
     *
     * @return boolean
     */
    public boolean isEnablePunctuation() {
        return enablePunctuation;
    }

    /**
     * 是否启用语义的nbest 结果
     *
     * @return boolean
     */
    public boolean isEnableNBest() {
        return enableNBest;
    }

    /**
     * 唤醒词信息
     *
     * @return String
     */
    public String getWakeupWords() {
        return wakeupWords;
    }

    /**
     * session Id
     *
     * @return String
     */
    public String getSessionId() {
        if (TextUtils.isEmpty(sessionId)) {
            sessionId = Util.uuid();
        }
        return sessionId;
    }

    /**
     * 设置无语音超时时长
     *
     * @return int
     */
    public int getNoSpeechTimeOut() {
        return noSpeechTimeOut;
    }

    /**
     * 设置音频最大录音时长
     *
     * @return int
     */
    public int getMaxSpeechTimeS() {
        return MaxSpeechTimeS;
    }

    /**
     * vad pause time
     *
     * @return int
     */
    public int getPauseTime() {
        return pauseTime;
    }

    /**
     * 是否启用实时识别
     *
     * @return boolean
     */
    public boolean isUseRealBack() {
        return useRealBack;
    }

    /**
     * 音频保存路径
     *
     * @return String
     */
    public String getSaveAudioPath() {
        return saveAudioPath;
    }

    /**
     * 识别文本
     *
     * @return String
     */
    public String getRefText() {
        return refText;
    }

    /**
     * AI type
     *
     * @return {@link AIType}
     */
    public AIType getType() {
        return type;
    }

    /**
     * skill id
     *
     * @return String
     */
    public String getSkillId() {
        return SkillId;
    }

    /**
     * task name
     *
     * @return String
     */
    public String getTask() {
        return task;
    }


    /**
     * 是否开启识别结果中文转阿拉伯数字
     *
     * @return Boolean
     */
    public boolean isEnableNumberConvert() {
        return enableNumberConvert;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    //    public boolean isEnableEmotion() {
//        return enableEmotion;
//    }
//
//    public boolean isEnableAlignment() {
//        return enableAlignment;
//    }
//
//    public boolean isEnableAudioDetection() {
//        return enableAudioDetection;
//    }
}
