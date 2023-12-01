package com.aispeech.export.intent;

import com.aispeech.base.IFespxEngine;
import com.aispeech.lite.base.BaseIntent;
import com.aispeech.lite.param.PhraseParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

public class AILocalLASRIntent extends BaseIntent {
    private IFespxEngine fespxEngine;
    //和资源词表的大小写相同
    public static final int USE_REC_NATIVE = -1;
    //rec结果为小写
    public static final int USE_REC_LOW = 0;
    //rec结果为大写
    public static final int USE_REC_UPPERCASE = 1;
    /**
     * 功能描述：数字、日期、时间、百分号等符号转换 default is {@value}
     */
    private boolean useTprocess = true;
    /**
     * 口语顺滑
     * 将识别文本中冗余部分过滤，如无意义的语气词、重复词等
     * default is {@value}
     */
    private boolean useTxtSmooth = true;
    /**
     * 英文大小写
     * use_rec_uppercase=-1，和资源词表的大小写相同
     * use_rec_uppercase=0，rec结果为小写；
     * use_rec_uppercase=1, rec结果为大写。
     * default is {@value}
     */
    private int useRecUppercase = USE_REC_LOW;
    /**
     * 热词
     */
    private boolean usePhrase = false;
    /**
     * 热词词表
     */
    private PhraseParams[] phraseParams;
//    /**
//     * 识别置信度
//     */
//    private boolean useConfidence = false;

    public boolean isUseTprocess() {
        return useTprocess;
    }

    /**
     * 逆文本，将识别的中文数字改成阿拉伯数字
     *
     * @param useTprocess 是否启用，默认 true
     */
    public void setUseTprocess(boolean useTprocess) {
        this.useTprocess = useTprocess;
    }


    public boolean isUseTxtSmooth() {
        return useTxtSmooth;
    }

    /**
     * 是否启用口语顺滑
     *
     * @param useTxtSmooth 是否启用，默认 true
     */
    public void setUseTxtSmooth(boolean useTxtSmooth) {
        this.useTxtSmooth = useTxtSmooth;
    }

    public int getUseRecUppercase() {
        return useRecUppercase;
    }

    /**
     * 英文大小写
     * AILocalLASRIntent.USE_REC_NATIVE 和资源词表的大小写相同
     * AILocalLASRIntent.USE_REC_LOW rec结果为小写
     * AILocalLASRIntent.USE_REC_UPPERCASE rec结果为大写
     *
     * @param useRecUppercase 默认 AILocalLASRIntent.USE_REC_LOW
     */
    public void setUseRecUppercase(int useRecUppercase) {
        this.useRecUppercase = useRecUppercase;
    }

    public boolean isUsePhrase() {
        return usePhrase;
    }

    /**
     * 是否启用热词
     *
     * @param usePhrase 是否启用，默认 false
     */
    public void setUsePhrase(boolean usePhrase) {
        this.usePhrase = usePhrase;
    }

    public PhraseParams[] getPhraseParams() {
        return phraseParams;
    }

    /**
     * 热词词表
     *
     * @param phraseParams 热词数组
     */
    public void setPhraseParams(PhraseParams[] phraseParams) {
        this.phraseParams = phraseParams;
    }

//    /**
//     * 设置识别置信度
//     *
//     * @param useConfidence 是否启用，默认 false
//     */
//    public void setUseConfidence(boolean useConfidence) {
//        this.useConfidence = useConfidence;
//    }
//
//    public boolean isUseConfidence() {
//        return useConfidence;
//    }

    public IFespxEngine getFespxEngine() {
        return fespxEngine;
    }

    /**
     * 设置关联的信号处理引擎AILocalSignalAndWakeupEngine实例，只在使用内部录音机且多麦模式下才需要设置
     *
     * @param fespxEngine 引擎实例
     * @throws RuntimeException 内部录音机且多麦模式下没设置
     */
    public void setFespxEngine(IFespxEngine fespxEngine) {
        this.fespxEngine = fespxEngine;
    }


    public String getLAsrParamJson() {
        JSONObject jsonObject = new JSONObject();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("use_txt_smooth=" + (useTxtSmooth ? 1 : 0));
        stringBuilder.append(";use_rec_uppercase=" + useRecUppercase);
        stringBuilder.append(";use_phrase=" + (usePhrase ? 1 : 0));
        stringBuilder.append(";use_tprocess=" + (useTprocess ? 1 : 0));
//        stringBuilder.append(";use_confidence=" + (useConfidence ? 1 : 0));
        try {
            jsonObject.put("env", stringBuilder.toString());
            if (phraseParams != null) {
                JSONArray jsonArray = new JSONArray();
                for (PhraseParams phraseParams : phraseParams) {
                    JSONObject phrase = new JSONObject();
                    phrase.put("name", phraseParams.getName());
                    phrase.put("boost", phraseParams.getBoost());
                    JSONArray words = new JSONArray();
                    for (String word : phraseParams.getWords()) {
                        words.put(word);
                    }
                    phrase.put("words", words);
                    jsonArray.put(phrase);
                }
                jsonObject.put("phraseList", jsonArray);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }

    @Override
    public String toString() {
        return "AILocalLASRIntent{" +
                "useCustomFeed=" + useCustomFeed +
                ", useTprocess=" + useTprocess +
                ", useTxtSmooth=" + useTxtSmooth +
                ", useRecUppercase=" + useRecUppercase +
                ", usePhrase=" + usePhrase +
                ", phraseParams=" + Arrays.toString(phraseParams) +
                '}';
    }
}
