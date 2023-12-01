package com.aispeech.export.config;

import com.aispeech.common.Log;
import com.aispeech.lite.base.BaseConfig;

import org.json.JSONException;
import org.json.JSONObject;

public class AICloudASRSentenceConfig extends BaseConfig {

    public static class AudioParam extends AICloudLASRConfig.AudioParam {

        public AudioParam(String audioType) {
            super(audioType);
        }
    }

    private AudioParam audioParam;

    /**
     * 可选值： cn（默认值， 中文）/ en（英文）/ ce（中英文混合）/ sichuantone-mix（四川话+普通话）/ cantonese-mix（粤语+普通话）
     */
    private String lang = "cn";
    /**
     * 是否启用VAD功能。 默认：true。如果关闭，会跳过音频分割，直接进入识别流程。
     */
    private boolean useVad = true;
    /**
     * 切割间隔时间，默认值：500，单位：毫秒。
     */
    private int vadPause = 500;

    /**
     * 是否启用所有后处理功能。 默认：true。如果关闭，会以原始的asr结果返回。
     */
    private boolean usePost = true;

    /**
     * 开启后处理时，是否使用中文句号来进行分句。 默认：false。
     */
    private boolean useFullstop = false;
    /**
     * 是否返回详细的分词结果。默认：false。如果开启，返回的result会以词为单位返回。
     */
    private boolean useSegment = false;

    private String lmId = "";

    /**
     * 开启后处理时，是否启用逆文本转换，默认：false。
     */
    private boolean useItn = false;

    /**
     * 开启后处理时，是否启用口语顺滑，默认：false。
     */
    private boolean useTxtSmooth = false;

    /**
     * 开启后处理时，是否启用标点符号，默认：true。 只对中文生效
     */
    private boolean usePuctuation = true;

    /**
     * 开启后处理时，是否返回每个句子和词的拼音，默认：false。只对中文生效
     */
    private boolean usePinyin = false;


    public AudioParam getAudioParam() {
        return audioParam;
    }

    /**
     * 设置音频参数，包含 类型，采样率，取样字节数，通道数 等
     *
     * @param audioParam 音频参数
     */
    public void setAudioParam(AudioParam audioParam) {
        this.audioParam = audioParam;
    }

    public String getLang() {
        return lang;
    }

    /**
     * 识别的语言
     *
     * @param lang 语言，可选值： cn（默认值， 中文）
     */
    public void setLang(String lang) {
        this.lang = lang;
    }

    public boolean isUseVad() {
        return useVad;
    }

    /**
     * 是否启用VAD功能。默认：true。如果关闭，会跳过音频分割，直接进入识别流程。
     *
     * @param useVad 是否启用VAD功能
     */
    public void setUseVad(boolean useVad) {
        this.useVad = useVad;
    }

    public int getVadPause() {
        return vadPause;
    }

    /**
     * 切割间隔时间 默认值：500，单位：毫秒。
     *
     * @param vadPause 切割间隔时间
     */
    public void setVadPause(int vadPause) {
        this.vadPause = vadPause;
    }

    public boolean isUsePost() {
        return usePost;
    }

    /**
     * 是否启用所有后处理功能。 默认：true。如果关闭，会以原始的asr结果返回。
     *
     * @param usePost 是否启用所有后处理功能，默认 true 启用
     */
    public void setUsePost(boolean usePost) {
        this.usePost = usePost;
    }

    public boolean isUseFullstop() {
        return useFullstop;
    }

    /**
     * 是否使用中文句号来进行分句。开启后处理时才有效，后处理默认开启。
     *
     * @param useFullstop 是否使用中文句号来进行分句 默认：false。
     */
    public void setUseFullstop(boolean useFullstop) {
        this.useFullstop = useFullstop;
    }

    public boolean isUseSegment() {
        return useSegment;
    }

    /**
     * 是否返回详细的分词结果。如果开启，返回的result会以词为单位返回。
     *
     * @param useSegment 是否返回详细的分词结果。默认：false。
     */
    public void setUseSegment(boolean useSegment) {
        this.useSegment = useSegment;
    }

    public String getLmId() {
        return lmId;
    }

    /**
     * 要使用的二路模型LMID。默认：""， 表示不使用二路资源。如需训练二路模型，请联系商务或项目经理获取服务。
     *
     * @param lmId 二路模型LMID
     */
    public void setLmId(String lmId) {
        this.lmId = lmId;
    }

    public boolean isUseItn() {
        return useItn;
    }

    /**
     * 开启后处理时，是否启用逆文本转换，即中文转阿拉伯数字，默认：false。
     *
     * @param useItn 逆文本转换
     */
    public void setUseItn(boolean useItn) {
        this.useItn = useItn;
    }

    public boolean isUseTxtSmooth() {
        return useTxtSmooth;
    }

    /**
     * 开启后处理时，是否启用口语顺滑，默认：false。
     *
     * @param useTxtSmooth 是否启用口语顺滑
     */
    public void setUseTxtSmooth(boolean useTxtSmooth) {
        this.useTxtSmooth = useTxtSmooth;
    }

    public boolean isUsePuctuation() {
        return usePuctuation;
    }

    /**
     * 开启后处理时，是否启用标点符号，默认：true。 只对中文生效
     *
     * @param usePuctuation 是否启用标点符号
     */
    public void setUsePuctuation(boolean usePuctuation) {
        this.usePuctuation = usePuctuation;
    }

    public boolean isUsePinyin() {
        return usePinyin;
    }

    /**
     * 开启后处理时，是否返回每个句子和词的拼音，默认：false。只对中文生效
     *
     * @param usePinyin 是否返回每个句子和词的拼音
     */
    public void setUsePinyin(boolean usePinyin) {
        this.usePinyin = usePinyin;
    }

    public String toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONObject asrObj = new JSONObject();
            asrObj.put("lang", lang);
            asrObj.put("use_vad", useVad);
            asrObj.put("vad_pause", vadPause);
            asrObj.put("use_post", usePost);
            asrObj.put("use_fullstop", useFullstop);
            asrObj.put("use_segment", useSegment);
            asrObj.put("lm_id", lmId);
            asrObj.put("use_itn", useItn);
            asrObj.put("use_txt_smooth", useTxtSmooth);
            asrObj.put("use_puctuation", usePuctuation);
            asrObj.put("use_pinyin", usePinyin);

            JSONObject audioObj = new JSONObject();
            audioObj.put("audio_type", audioParam.getAudioType());
            audioObj.put("sample_rate", audioParam.getSampleRate());
            audioObj.put("channel", audioParam.getChannel());
            audioObj.put("sample_bytes", audioParam.getSampleBytes());

            jsonObject.put("audio", audioObj);
            jsonObject.put("asr", asrObj);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (NullPointerException e1) {
            e1.printStackTrace();
            Log.d("ASRSentenceConfig", "audioParam: " + audioParam);
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return jsonObject.toString();
    }

    public boolean isValid() {
        return audioParam != null && audioParam.isValid();
    }

}
