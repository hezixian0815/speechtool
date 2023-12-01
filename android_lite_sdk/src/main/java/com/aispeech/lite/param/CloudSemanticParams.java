package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.common.JSONUtil;
import com.aispeech.lite.AISampleRate;
import com.aispeech.lite.AIType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * dds云端识别接口
 * api:https://wiki.aispeech.com.cn/pages/viewpage.action?pageId=33241282
 */
public class CloudSemanticParams extends SpeechParams {
    private class audio {
        public static final int SAMPLE_RATE = 16000;
        public static final int CHANNEL = 1;
        public static final int SAMPLE_BYTES = 2;
    }

    /**
     * 音频
     */
    public static final String TOPIC_RECORDER_STREAM = "recorder.stream.start";
    /**
     * 输出文本
     */
    public static final String TOPIC_NLU_INPUT_TEXT = "nlu.input.text";

    public CloudSemanticParams() {
        setTopic(TOPIC_RECORDER_STREAM);
        setSampleRate(AISampleRate.toAISampleRate(AISampleRate.SAMPLE_RATE_16K.getValue()));
        setAudioType(CloudASRParams.OGG);
        setEncodedAudio(true);
        setChannel(audio.CHANNEL);
        setSampleBytes(audio.SAMPLE_BYTES);
    }

    private static final String KEYS_TOPIC = "topic";
    private static final String KEYS_RECORD_ID = "recordId";
    private static final String KEYS_SESSION_ID = "sessionId";
    private static final String KEYS_AI_TYPE = "aiType"; //指定云端服务类型，asr表示识别结果，nlu表示语义结果 默认表示对话结果
    private static final String KEYS_WAKEUP_WORD = "wakeupWord";
    private static final String KEYS_FER_TEST = "refText";//请求文本

    /**
     * audio 请求参数
     */
    private static final String KEYS_AUDIO = "audio";
    private static final String KEYS_AUDIO_TYPE = "audioType";
    private static final String KEYS_AUDIO_SAMPLE_RATE = "sampleRate";
    private static final String KEYS_AUDIO_CHANNEL = "channel";
    private static final String KEYS_AUDIO_SAMPLE_BYTES = "sampleBytes";
    /**
     * asrParams请求参数
     */
    private static final String KEYS_ASR = "asrParams";
    private static final String KEYS_ASR_VAD_PAUSE_TIME = "vadPauseTime";
    private static final String KEYS_ASR_ENABLE_VAD = "enableVAD";
    private static final String KEYS_ASR_ENABLE_CLOUD_VAD = "enableCloudVAD";
    private static final String KEYS_ASR_REAL_BACK = "realBack";
    private static final String KEYS_ASR_ENABLE_PUNCTUATION = "enablePunctuation";
    private static final String KEYS_ASR_ENABLE_TONE = "enableTone";
    private static final String KEYS_ASR_CUSTOM_WAKEUP_SCORE = "customWakeupScore";
    private static final String KEYS_ASR_ENABLE_CONFIDENCE = "enableConfidence";
    private static final String KEYS_ASR_ENABLE_NUMBER_CONVERT = "enableNumberConvert";
    private static final String KEYS_ASR_ENABLE_REC_UPPERCASE = "enableRecUppercase";
    private static final String KEYS_ASR_PHRASEHINTS = "phraseHints";
    private static final String KEYS_ASR_ENABLE_AUDIO_DETECTION = "enableAudioDetection";
    private static final String KEYS_ASR_ENABLE_EMOTION = "enableEmotion";
    private static final String KEYS_ASR_ENABLE_ALIGNMENT = "enableAlignment";
    /**
     * nluParams请求参数
     */
    private static final String KEYS_NLU = "nluParams";
    private static final String KEYS_NLU_ENABLE_CENSOR = "enableCensor";
    private static final String KEYS_NLU_ENABLE_NBEST = "enableNBest";
    /**
     * 开启nlu模式下的实时识别结果
     */
    private static final String KEYS_NLU_ENABLE_ASR_RESULT = "enableASRResult";
    /**
     * context请求参数
     */
    private static final String KEYS_CONTEXT = "context";
    /**
     * skill请求参数
     */
    private static final String KEYS_CONTEXT_SKILL = "skill";
    private static final String KEYS_CONTEXT_SKILL_SKILL_ID = "skillId";
    private static final String KEYS_CONTEXT_SKILL_TASK = "task";
    private static final String KEYS_ASR_ENABLE_SHOW_TEXT = "enableShowText";
    /**
     * 生成audio请求参数,必选
     *
     * @return
     */
    private JSONObject getRequestAudioJson() {
        JSONObject audio = new JSONObject();
        String audioType;
        if (getAudioType() == CloudASRParams.OGG) {
            audioType = "ogg";
        } else if (getAudioType() == CloudASRParams.OGG_OPUS) {
            audioType = "ogg_opus";
        } else {
            audioType = "wav";
        }
        JSONUtil.putQuietly(audio, KEYS_AUDIO_TYPE, audioType);
        JSONUtil.putQuietly(audio, KEYS_AUDIO_SAMPLE_RATE, getSampleRate().getValue());
        JSONUtil.putQuietly(audio, KEYS_AUDIO_CHANNEL, getChannel());
        JSONUtil.putQuietly(audio, KEYS_AUDIO_SAMPLE_BYTES, getSampleBytes());
        return audio;
    }
    /**
     * 生成asrParams请求参数，可选
     *
     * @return
     */
    private JSONObject getRequestAsrParamsJson() {
        JSONObject asr = new JSONObject();
        if (getVadPauseTime() > 0) {
            JSONUtil.putQuietly(asr, KEYS_ASR_VAD_PAUSE_TIME, getVadPauseTime());
        }
        JSONUtil.putQuietly(asr, KEYS_ASR_REAL_BACK, isRealBack());
        JSONUtil.putQuietly(asr, KEYS_ASR_ENABLE_PUNCTUATION, isEnablePunctuation());
        JSONUtil.putQuietly(asr, KEYS_ASR_ENABLE_TONE, isEnableTone());
        JSONUtil.putQuietly(asr, KEYS_ASR_CUSTOM_WAKEUP_SCORE, getCustomWakeupScore());
        JSONUtil.putQuietly(asr, KEYS_ASR_ENABLE_CONFIDENCE, isEnableConfidence());
        JSONUtil.putQuietly(asr, KEYS_ASR_ENABLE_NUMBER_CONVERT, isEnableNumberConvert());
        JSONUtil.putQuietly(asr, KEYS_ASR_ENABLE_REC_UPPERCASE, isEnableRecUppercase());
        JSONUtil.putQuietly(asr, KEYS_ASR_ENABLE_ALIGNMENT, isEnableAlignment());
        JSONUtil.putQuietly(asr, KEYS_ASR_ENABLE_EMOTION, isEnableEmotion());
        JSONUtil.putQuietly(asr, KEYS_ASR_ENABLE_AUDIO_DETECTION, isEnableAudioDetection());
        JSONUtil.putQuietly(asr, KEYS_ASR_ENABLE_VAD, isEnableVAD());
        JSONUtil.putQuietly(asr, KEYS_ASR_ENABLE_CLOUD_VAD, isEnableCloudVAD());
        JSONUtil.putQuietly(asr, KEYS_ASR_ENABLE_SHOW_TEXT, isEnableShowText());
        if (!TextUtils.isEmpty(getRes())) {
            JSONUtil.putQuietly(asr, KEY_RES, getRes());
        }

        if (!TextUtils.isEmpty(getPhraseHints())) {
            try {
                JSONUtil.putQuietly(asr, KEYS_ASR_PHRASEHINTS, new JSONArray(getPhraseHints()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if (!TextUtils.isEmpty(getPhraseHints())) {
            try {
                JSONUtil.putQuietly(asr, KEYS_ASR_PHRASEHINTS, new JSONArray(getPhraseHints()));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return asr;
    }
    /**
     * 生成nluParams相关参数
     *
     * @return
     */
    private JSONObject getRequestNluParams() {
        JSONObject nlu = new JSONObject();
        if (isEnableNluCensor())
            JSONUtil.putQuietly(nlu, KEYS_NLU_ENABLE_CENSOR, isEnableNluCensor());
        if (isEnableNluNbest()) JSONUtil.putQuietly(nlu, KEYS_NLU_ENABLE_NBEST, isEnableNluNbest());
        //nlu 模式下开启实时别结果，需要新增额外参数  enableASRResult
        if("nlu".equals(getAiType()) && isRealBack())
            JSONUtil.putQuietly(nlu, KEYS_NLU_ENABLE_ASR_RESULT, true);
        return nlu;
    }
    /**
     * 生成context参数
     *
     * @return
     */
    private JSONObject getRequestContext() {
        JSONObject context = new JSONObject();
        JSONUtil.putQuietly(context, KEYS_CONTEXT_SKILL, getRequestSkill());
        return context;
    }
    /**
     * 生成skill参数
     *
     * @return
     */
    private JSONObject getRequestSkill() {
        JSONObject skill = new JSONObject();
        if (!TextUtils.isEmpty(getSkillId()))
            JSONUtil.putQuietly(skill, KEYS_CONTEXT_SKILL_SKILL_ID, getSkillId());
        if (!TextUtils.isEmpty(getTask()))
            JSONUtil.putQuietly(skill, KEYS_CONTEXT_SKILL_TASK, getTask());
        return skill;
    }
    @Override
    public JSONObject toJSON() {
        JSONObject asrParams = new JSONObject();
        if (getAIType() != AIType.DM) {//dm 模式下不传入aitype ,否则没有实时识别结果
            JSONUtil.putQuietly(asrParams, KEYS_AI_TYPE, getAIType().value);
        }
        if (!TextUtils.isEmpty(getRecordId()))
            JSONUtil.putQuietly(asrParams, KEYS_RECORD_ID, getRecordId());
        if (!TextUtils.isEmpty(getSessionId()))
            JSONUtil.putQuietly(asrParams, KEYS_SESSION_ID, getSessionId());
        if (!TextUtils.isEmpty(getWakeupWord()))
            JSONUtil.putQuietly(asrParams, KEYS_WAKEUP_WORD, getWakeupWord());
        if (!TextUtils.isEmpty(getRefText()))
            JSONUtil.putQuietly(asrParams, KEY_REF_TEXT, getRefText());
        JSONUtil.putQuietly(asrParams, KEYS_TOPIC, getTopic());
        // todo：加入判断是否匹配差异
        if (TextUtils.equals(getTopic(), TOPIC_RECORDER_STREAM)) {
            JSONUtil.putQuietly(asrParams, KEYS_AUDIO, getRequestAudioJson());
            JSONUtil.putQuietly(asrParams, KEYS_ASR, getRequestAsrParamsJson());
        }
        JSONUtil.putQuietly(asrParams, KEYS_NLU, getRequestNluParams());
        JSONUtil.putQuietly(asrParams, KEYS_CONTEXT, getRequestContext());
        return asrParams;
    }
    @Override
    public String toString() {
        return "CloudSemanticParams{" +
                "topic='" + topic + '\'' +
                ", recordId='" + recordId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", wakeupWord='" + wakeupWord + '\'' +
                ", audioType='" + audioType + '\'' +
                ", sampleRate=" + sampleRate +
                ", channel=" + channel +
                ", sampleBytes=" + sampleBytes +
                ", enableVAD=" + enableVAD +
                ", realBack=" + realBack +
                ", enablePunctuation=" + enablePunctuation +
                ", enableTone=" + enableTone +
                ", customWakeupScore=" + customWakeupScore +
                ", enableConfidence=" + enableConfidence +
                ", enableNumberConvert=" + enableNumberConvert +
                ", phraseHints='" + phraseHints + '\'' +
                ", enableNluCensor=" + enableNluCensor +
                ", enableNluNbese=" + enableNluNbest +
                ", skillId=" + skillId +
                ", task='" + task + '\'' +
                ", aiType='" + aiType + '\'' +
                '}';
    }

    private String topic;
    private String recordId;
    private String sessionId;
    private String wakeupWord;
    private int audioType;
    private AISampleRate sampleRate;
    private int channel;
    private int sampleBytes;

    /**
     * 用户 feed 的音频是否是编码后的音频。
     * 默认 false 即 feed 的是 pcm 音频，true 表示 feed 的是编码后的音频，如 MP3 OGG OPUS OGG_OPUS
     * 使用前提 用户feed，并且不使用本地vad
     */
    private boolean encodedAudio;

    /**
     * 设置云端 vad 静音检查时长，单位 ms
     * <0 不设置，云端默认 500 ms
     */
    private int vadPauseTime = -1;

    /**
     * 是否开启云端vad
     * 默认关闭
     */
    private boolean enableVAD = false;
    /**
     * 是否开启实时识别
     */
    private boolean realBack = true;
    /**
     * 是否开启标点符号
     */
    private boolean enablePunctuation = true;
    /**
     * 是否开启拼音音调
     */
    private boolean enableTone = true;
    /**
     * 自定义唤醒词阈值
     */
    private int customWakeupScore;
    /**
     * 设置是否返回置信度
     */
    private boolean enableConfidence = true;
    /**
     * 识别结果中文转阿拉伯数字
     */
    private boolean enableNumberConvert = false;

    /**
     * 识别结果开启大写
     */
    private boolean enableRecUppercase = false;

    /**
     * 词库识别、语义是否开启转换，开启后，会对含英文等词库统一加工后送识别，得到结果后还原原始词库
     */
    private boolean enableVocabsConvert = true;

    /**
     * 使用热词识别
     * 需要pattern 在第二路或者第三路im中存在
     */
    private String phraseHints;
    /**
     * 语义解析输出做敏感词检查
     */
    private boolean enableNluCensor = false;
    /**
     * 语义解析结果是否带出nbest结果
     */
    private boolean enableNluNbest = false;
    /**
     * 非首轮识别，用于调用端指定上一轮使用了上一轮nbest结果中对哪一个skill
     */
    private String skillId;
    /**
     * 非首轮识别，用于指定调用端使用了上一轮nbest结果中对哪一个task
     */
    private String task;
    /**
     * 指定语段服务类别，默认对话结果，可单独指定asr、nlu
     * nlu 字段默认
     */
    private AIType aiType = AIType.DM;

    /**
     * 指定识别文本
     */
    private String refText = "";

    private boolean enableAlignment = false;// alignment

    private boolean enableEmotion = false;//情绪识别

    private boolean enableAudioDetection = false;//音频检测

    /**
     * vadEnable是老字段，不提倡使用，等价于enableVAD，用于控制env:use_vad=x；
     * 通常情况下，对应关系是true对应1， false对应0；
     * 但是有一个特殊场景:产品配置中使用了远场拾音方式，此时由dds统一设置为use_vad=0;
     */
    private boolean enableCloudVAD = true;//全双工下，单独vad服务，全双工下， 云端一定为true
    private boolean enableShowText = false; // 开启后 会返 showText 字段，用于上屏，例如展示阿拉伯数字

    public boolean isEnableShowText() {
        return enableShowText;
    }

    public void setEnableShowText(boolean enableShowText) {
        this.enableShowText = enableShowText;
    }

    public String getRefText() {
        return refText;
    }

    public void setRefText(String refText) {
        this.refText = refText;
    }

    public void setAudioType(int type) {
        this.audioType = type;
    }

    public int getAudioType() {
        return audioType;
    }


    public void setEncodedAudio(boolean encodedAudio) {
        this.encodedAudio = encodedAudio;
    }

    public boolean isEncodedAudio() {
        return encodedAudio;
    }


    public int getVadPauseTime() {
        return vadPauseTime;
    }

    public void setVadPauseTime(int pauseTime) {
        this.vadPauseTime = pauseTime;
    }

    @Override
    public AISampleRate getSampleRate() {
        return sampleRate;
    }

    @Override
    public void setSampleRate(AISampleRate sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getChannel() {
        return channel;
    }
    public void setChannel(int channel) {
        this.channel = channel;
    }
    public int getSampleBytes() {
        return sampleBytes;
    }
    public void setSampleBytes(int sampleBytes) {
        this.sampleBytes = sampleBytes;
    }
    public void setEnableDmEnable(boolean enableDmEnable) {
        if (!enableDmEnable)
            setAiType("nlu");
        else
            setAiType("");//dm 模式，不设置aiType
    }
    @Override
    public String getTopic() {
        return topic;
    }
    @Override
    public void setTopic(String topic) {
        this.topic = topic;
    }
    @Override
    public String getRecordId() {
        return recordId;
    }
    @Override
    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }
    @Override
    public String getSessionId() {
        return sessionId;
    }
    @Override
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    public String getWakeupWord() {
        return wakeupWord;
    }
    public void setWakeupWord(String wakeupWord) {
        this.wakeupWord = wakeupWord;
    }
    public boolean isEnableVAD() {
        return enableVAD;
    }
    public void setEnableVAD(boolean enableVAD) {
        this.enableVAD = enableVAD;
    }
    public boolean isRealBack() {
        return realBack;
    }
    public void setRealBack(boolean realBack) {
        this.realBack = realBack;
    }
    public boolean isEnablePunctuation() {
        return enablePunctuation;
    }
    public void setEnablePunctuation(boolean enablePunctuation) {
        this.enablePunctuation = enablePunctuation;
    }
    public boolean isEnableTone() {
        return enableTone;
    }
    public void setEnableTone(boolean enableTone) {
        this.enableTone = enableTone;
    }
    public int getCustomWakeupScore() {
        return customWakeupScore;
    }
    public void setCustomWakeupScore(int customWakeupScore) {
        this.customWakeupScore = customWakeupScore;
    }
    public boolean isEnableConfidence() {
        return enableConfidence;
    }

    public void setEnableConfidence(boolean enableConfidence) {
        this.enableConfidence = enableConfidence;
    }

    public boolean isEnableNumberConvert() {
        return enableNumberConvert;
    }

    public void setEnableNumberConvert(boolean enableNumberConvert) {
        this.enableNumberConvert = enableNumberConvert;
    }

    public String getPhraseHints() {
        return phraseHints;
    }

    public void setPhraseHints(String phraseHints) {
        this.phraseHints = phraseHints;
    }

    public boolean isEnableNluCensor() {
        return enableNluCensor;
    }

    public void setEnableNluCensor(boolean enableNluCensor) {
        this.enableNluCensor = enableNluCensor;
    }

    public boolean isEnableNluNbest() {
        return enableNluNbest;
    }

    public void setEnableNluNbest(boolean enableNluNbest) {
        this.enableNluNbest = enableNluNbest;
    }

    public String getSkillId() {
        return skillId;
    }

    public void setSkillId(int skillId) {
        this.skillId = skillId + "";
    }

    public void setSkillId(String skillId) {
        this.skillId = skillId;
    }

    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public boolean isEnableAlignment() {
        return enableAlignment;
    }

    public CloudSemanticParams setEnableAlignment(boolean enableAlignment) {
        this.enableAlignment = enableAlignment;
        return this;
    }

    public boolean isEnableEmotion() {
        return enableEmotion;
    }

    public CloudSemanticParams setEnableEmotion(boolean enableEmotion) {
        this.enableEmotion = enableEmotion;
        return this;
    }

    public boolean isEnableAudioDetection() {
        return enableAudioDetection;
    }

    public CloudSemanticParams setEnableAudioDetection(boolean enableAudioDetection) {
        this.enableAudioDetection = enableAudioDetection;
        return this;
    }

    public boolean isEnableRecUppercase() {
        return enableRecUppercase;
    }

    public void setEnableRecUppercase(boolean enableRecUppercase) {
        this.enableRecUppercase = enableRecUppercase;
    }

    public boolean isEnableVocabsConvert() {
        return enableVocabsConvert;
    }

    public void setEnableVocabsConvert(boolean enableVocabsConvert) {
        this.enableVocabsConvert = enableVocabsConvert;
    }

    public void setEnableCloudVAD(boolean enableCloudVAD) {
        this.enableCloudVAD = enableCloudVAD;
    }

    public boolean isEnableCloudVAD() {
        return enableCloudVAD;
    }


    public AIType getAIType() {
        return aiType;
    }

    public void setAIType(AIType aiType) {
        this.aiType = aiType;
    }
}
