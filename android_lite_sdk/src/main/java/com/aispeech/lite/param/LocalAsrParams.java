package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.export.ASRMode;
import com.aispeech.export.engines2.bean.Decoder;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

/**
 * Created by wuwei on 18-5-14.
 */

public class LocalAsrParams extends SpeechParams {
    private static final String KEY_ENV = "env";
    private static final String KEY_XBNF = "use_xbnf_rec";//是否输出xbnf语义标签
    private static final String KEY_CONF = "use_conf_dnn";
    private static final String KEY_PINYIN = "use_pinyin";
    private static final String KEY_FRAME_SPLIT = "use_frame_split";
    private static final String KEY_DYNAMIC_LIST = "dynamic_list";//识别增量列表
    private static final String KEY_BLACK_LIST = "blacklist";//识别增量黑名单
    private static final String KEY_EXPAND_FN = "expand_fn";//动态netbin配置文件
    private static final String KEY_REC_WRD_SEP = "rec_wrd_sep";//离线识别结果分割字符，默认不设置
    private static final String KEY_USE_FILLER = "use_filler";//在解码网络里增加了filler路径，filler_penalty_score 开关，默认 1
    private static final String KEY_FILLER_PENALTY_SCORE = "filler_penalty_score";//根据误唤醒集合设置的值,可根据实际情况再调整，默认 1.5
    private static final String KEY_ONE_SHOT_WAKEUP_WORDS = "one_shot_json";//根据误唤醒集合设置的值,可根据实际情况再调整，默认 1.5
    private static final String KEY_HOLD_CONF = "hold_conf";//ngram输出置信度
    private static final String KEY_FILTER_SLOT_ONESHOT = "filter_slot_oneshot"; //是否过滤"sys.ONESHOT"槽位的环境变量
    private static final String KEY_USE_RAWREC = "use_rawrec";
    private static final String KEY_USE_E2E_FUSION = "use_e2e_fusion"; //开启融合，并且active_decoder_list需要激活e2e和ngram；
    private static final String KEY_NGRAM_CONF = "ngram_conf"; //ngram默认置信度判断为0.7，通过env中ngram_conf来更改这个置信度

    /**
     * 用于设置本次解码需要激活的解码网络。所有注册过，但不在 active_decoder_list 中的解码网络，将处于停用状态（不做解码，不出结果）。
     * 样例：active_decoder_list="ngram,phone_fst";
     */
    private static final String KEY_ACTIVE_DECODER_LIST = "active_decoder_list";

    /**
     * 引擎模式 {@link com.aispeech.export.ASRMode}
     */
    public int mode;
    private boolean useXbnfRec = true; // 默认输出扩展语法结果
    private boolean useConf = true; // 默认开启置信度
    private boolean usePinyin = false; // 默认关闭拼音
    private boolean useFrameSplit = false;//默认不对外输出识别结果中间值
    private boolean useRealBack = false; //默认关闭实时反馈
    private boolean useHoldConf = true;//默认开启ngram置信度
    private String dynamicList = "";//本地热词
    private boolean useFiller = false;
    private float fillerPenaltyScore = 2.0f;
    private double useThreshold = 0.60;//本地热词引擎默认采信置信度阈值
    private Map<String, Double> customThresholdMap; //存放自定义热词及对应的阈值
    private String expandFnPath = "";//expandFnPath 文件路径
    private boolean useMaxSpeechResult = false;//默认不使用超时之后的识别结果，直接对外回调录音超时错误，开启后则继续返回超时后的识别结果
    private String useDynamicBackList = "";//本地热词黑名单
    private boolean useContinuousRecognition = true;//本地热词连续识别
    private String useWrdSep = "";//识别结果分割符,默认传入"" , 祛除内核默认空格

    private boolean useNgram = false;//表示当前使用的资源是NGRAM的资源
    private double useEnglishThreshold = 0;//存在英文情况下设置英文置信度(中英文下识别率低，需另外设置置信度)

    /**
     * 开启融合，
     * 并且active_decoder_list需要激活e2e和ngram
     */
    private boolean useE2EFusion;
    /**
     * 置信度
     */
    private double ngramConf;
    /**
     * env
     */
    private String env;

    public boolean isUseE2EFusion() {
        return useE2EFusion;
    }

    /**
     * 开启融合，
     * 并且active_decoder_list需要激活e2e和ngram
     *
     * @param useE2EFusion 是否开启融合默认是关闭
     */
    public void setUseE2EFusion(boolean useE2EFusion) {
        this.useE2EFusion = useE2EFusion;
    }

    public double getNgramConf() {
        return ngramConf;
    }

    /**
     * 设置置信度
     *
     * @param ngramConf 置信度
     */
    public void setNgramConf(double ngramConf) {
        this.ngramConf = ngramConf;
    }

    public String getEnv() {
        return env;
    }

    /**
     * env
     *
     * @param env env json串
     */
    public void setEnv(String env) {
        this.env = env;
    }

    /**
     * 唤醒词配置文件，用于oneshot场景过滤开头的唤醒词
     */
    public String wakeupWordsFilePath;

    /**
     * 唤醒词列表，用于oneshot场景过滤开头的唤醒词
     */
    public String[] wakeupWords;

    /**
     * 用于设置本次解码需要激活的解码网络
     */
    private String activeDecoderList;

    /**
     * 多路解码网络
     */
    private List<Decoder> decoders;

    /**
     * 是否忽略阈值，直接将结果透传
     */
    private boolean isIgnoreThreshold = false;
    private boolean userRawRec = false;
    private boolean useOneshotJson = true;

    public boolean isUseOneshotJson() {
        return useOneshotJson;
    }

    public void setUseOneshotJson(boolean useOneshotJson) {
        this.useOneshotJson = useOneshotJson;
    }
    private static final String KEY_BACKFILLER_OMIT_CONF = "backfiller_omit_conf";// 后置filler缺少惩罚的置信度（float 默认 0.1）
    private static final String KEY_BACKFILLER_OMIT_TIME = "backfiller_omit_time";// 后置filler缺少的时间阈值（int 默认 550）
    private static final String KEY_USE_BACKFILLE_OMIT_CHECK = "use_backfiller_omit_check";// 是否开启后置filler缺少检查（（0，1） 默认 0关闭
    private float useOmitConf = 0.1f;//后置filler缺少惩罚的置信度（float 默认 0.1）
    private int useOmitTime = 550;// 后置filler缺少的时间阈值（int 默认 550）
    private boolean useOmitCheck = false;// 是否开启后置filler缺少检查（（0，1） 默认 0关闭。

    public List<Decoder> getDecoders() {
        return decoders;
    }

    /**
     * 设置是否忽略阈值，不管kernel返回的结果是大于还是小于阈值，都将结果返回给上层
     */
    public void setIsIgnoreThreshold(boolean isIgnoreThreshold) {
        this.isIgnoreThreshold = isIgnoreThreshold;
    }

    public boolean getIsIgnoreThreshold() {
        return isIgnoreThreshold;
    }

    /**
     * 添加解码网络
     *
     * @param decoder 解码网络信息
     */
    public void addDecoder(Decoder decoder) {
        decoders.add(decoder);
    }

    public String getActiveDecoderList() {
        return activeDecoderList;
    }

    public void setActiveDecoderList(String activeDecoderList) {
        this.activeDecoderList = activeDecoderList;
    }

    public boolean isUseMaxSpeechResult() {
        return useMaxSpeechResult;
    }

    public void setUseMaxSpeechResult(boolean useMaxSpeechResult) {
        this.useMaxSpeechResult = useMaxSpeechResult;
    }

    public String getUseDynamicBackList() {
        return useDynamicBackList;
    }

    public void setUseDynamicBackList(String useDynamicBackList) {
        this.useDynamicBackList = useDynamicBackList;
    }

    public boolean isUseContinuousRecognition() {
        return useContinuousRecognition;
    }

    public void setUseContinuousRecognition(boolean useContinuousRecognition) {
        this.useContinuousRecognition = useContinuousRecognition;
    }

    public void setUseRawRec(boolean useRawRec) {
        this.userRawRec = useRawRec;
    }

    public boolean isUseRawRec() {
        return userRawRec;
    }

    public String getUseWrdSep() {
        return useWrdSep;
    }

    public void setUseWrdSep(String useWrdSep) {
        this.useWrdSep = useWrdSep;
    }

    public String getExpandFnPath() {
        return expandFnPath;
    }

    public boolean isUseNgram() {
        return useNgram;
    }

    public void setUseNgram(boolean useNgram) {
        this.useNgram = useNgram;
    }

    public void setExpandFnPath(String expandFnPath) {
        this.expandFnPath = expandFnPath;
    }

    public boolean isUseXbnfRec() {
        return useXbnfRec;
    }

    public void setUseXbnfRec(boolean useXbnfRec) {
        this.useXbnfRec = useXbnfRec;
    }

    public boolean isUseConf() {
        return useConf;
    }

    public void setUseConf(boolean useConf) {
        this.useConf = useConf;
    }

    public boolean isUsePinyin() {
        return usePinyin;
    }

    public void setUsePinyin(boolean usePinyin) {
        this.usePinyin = usePinyin;
    }

    public boolean isUseRealBack() {
        return useRealBack;
    }

    public void setUseRealBack(boolean useRealBack) {
        this.useRealBack = useRealBack;
    }

    public boolean isUseHoldConf() {
        return useHoldConf;
    }

    public void setUseHoldConf(boolean useHoldConf) {
        this.useHoldConf = useHoldConf;
    }

    public boolean isUseFrameSplit() {
        return useFrameSplit;
    }

    public void setUseFrameSplit(boolean useFrameSplit) {
        this.useFrameSplit = useFrameSplit;
    }

    public String getDynamicList() {
        return dynamicList;
    }
    public void setUseDynamicList(String dynamicList) {
        this.dynamicList = dynamicList;
    }

    public void setDynamicList(String dynamicList) {
        this.dynamicList = dynamicList;
    }

    public boolean isUseFiller() {
        return useFiller;
    }

    public void setUseFiller(boolean useFiller) {
        this.useFiller = useFiller;
    }

    public float getFillerPenaltyScore() {
        return fillerPenaltyScore;
    }

    public void setFillerPenaltyScore(float fillerPenaltyScore) {
        this.fillerPenaltyScore = fillerPenaltyScore;
    }

    public int getMode() {
        return mode;
    }

    public ASRMode getModeTrans() {
        ASRMode asrMode = ASRMode.MODE_ASR;
        switch (mode){
            case 2:
                asrMode = ASRMode.MODE_HOTWORD;
                break;
            case 3:
                asrMode = ASRMode.MODE_ASR_X;
                break;

        }
        return asrMode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public Map<String, Double> getCustomThresholdMap() {
        return customThresholdMap;
    }

    public void setCustomThresholdMap(Map<String, Double> customThresholdMap) {
        this.customThresholdMap = customThresholdMap;
    }

    public double getUseThreshold() {
        return useThreshold;
    }

    public void setUseThreshold(double useThreshold) {
        this.useThreshold = useThreshold;
    }

    public double getUseEnglishThreshold() {
        return useEnglishThreshold;
    }

    public void setUseEnglishThreshold(double useEnglishThreshold) {
        this.useEnglishThreshold = useEnglishThreshold;
    }

    public String getWakeupWordsFilePath() {
        return wakeupWordsFilePath;
    }

    public void setWakeupWordsFilePath(String wakeupWords) {
        this.wakeupWordsFilePath = wakeupWords;
    }

    public void setWakeupWords(String[] wakeupWords) {
        this.wakeupWords = wakeupWords;
    }

    public String[] getWakeupWords() {
        return wakeupWords;
    }
    public float getUseOmitConf() {
        return useOmitConf;
    }

    public void setUseOmitConf(float useOmitConf) {
        this.useOmitConf = useOmitConf;
    }

    public int getUseOmitTime() {
        return useOmitTime;
    }

    public void setUseOmitTime(int useOmitTime) {
        this.useOmitTime = useOmitTime;
    }

    public boolean isUseOmitCheck() {
        return useOmitCheck;
    }

    public void setUseOmitCheck(boolean useOmitCheck) {
        this.useOmitCheck = useOmitCheck;
    }
    private String toEnv() {
        StringBuilder envSb = new StringBuilder();
        envSb.append(KEY_XBNF + "=" + (useXbnfRec ? 1 : 0) + ";");
        envSb.append(KEY_CONF + "=" + (useConf ? 1 : 0) + ";");
        envSb.append(KEY_PINYIN + "=" + (usePinyin ? 1 : 0) + ";");
        envSb.append(KEY_FRAME_SPLIT + "=" + (useRealBack ? 1 : 0) + ";");
        envSb.append(KEY_HOLD_CONF + "=" + (useHoldConf ? 1 : 0) + ";");
        envSb.append(KEY_USE_RAWREC + "=" + (userRawRec ? 1 : 0) + ";");
        envSb.append(KEY_FILTER_SLOT_ONESHOT + "=" + (getWakeupWords() != null && getWakeupWords().length > 0 ? 1 : 0) + ";");
        envSb.append(KEY_BACKFILLER_OMIT_CONF + "=" + getUseOmitConf() + ";");
        envSb.append(KEY_BACKFILLER_OMIT_TIME + "=" + getUseOmitTime() + ";");
        envSb.append(KEY_USE_BACKFILLE_OMIT_CHECK + "=" + (useOmitCheck ? 1 : 0) + ";");
        envSb.append(KEY_USE_E2E_FUSION + "=" + (useE2EFusion ? 1 : 0) + ";");
        envSb.append(KEY_NGRAM_CONF + "=" + ngramConf + ";");
        if (useFiller) {
            envSb.append(KEY_USE_FILLER + "=1;");
            envSb.append(KEY_FILLER_PENALTY_SCORE + "=" + getFillerPenaltyScore() + ";");
        }
        if (!TextUtils.isEmpty(dynamicList)) {
            /**
             * 车载的代码会在对应的intent中设置转义符，公版的是在params中设置
             * 需要做兼容判断
             */
            if (!(dynamicList.startsWith("\"") && dynamicList.endsWith("\""))) {
                envSb.append(KEY_DYNAMIC_LIST + "=" + "\"" + dynamicList + "\"" + ";");
            } else {
                envSb.append(KEY_DYNAMIC_LIST + "=" + dynamicList + ";");
            }
        }

        if (!TextUtils.isEmpty(getUseWrdSep())) {
            envSb.append(KEY_REC_WRD_SEP + "=" + ("\"" + getUseWrdSep() + "\"") + ";");
        }

        if (!TextUtils.isEmpty(getUseDynamicBackList())) {
            /**
             * 车载的代码会在对应的intent中设置转义符，公版的是在params中设置
             * 需要做兼容判断
             */
            String usedynamicbacklist = getUseDynamicBackList();
            if (!(usedynamicbacklist.startsWith("\"") && usedynamicbacklist.endsWith("\""))) {
                envSb.append(KEY_BLACK_LIST + "=" + "\"" + usedynamicbacklist + "\"" + ";");
            } else {
                envSb.append(KEY_BLACK_LIST + "=" + usedynamicbacklist + ";");
            }
        }
        if (!TextUtils.isEmpty(getExpandFnPath())) {
            envSb.append(KEY_EXPAND_FN + "=" + getExpandFnPath() + ";");
        }
        if (!TextUtils.isEmpty(getWakeupWordsFilePath())) {
            envSb.append(KEY_ONE_SHOT_WAKEUP_WORDS + "=" + getWakeupWordsFilePath() + ";");
        }
        if (!TextUtils.isEmpty(getActiveDecoderList())) {
            envSb.append(KEY_ACTIVE_DECODER_LIST + "=" + getActiveDecoderList() + ";");
        }
        return envSb.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject request = new JSONObject();
        try {
            if (TextUtils.isEmpty(env)) {
                request.put(KEY_ENV, toEnv());
            } else {
                request.put(KEY_ENV, env);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return request;
    }

    @Override
    public LocalAsrParams clone() throws CloneNotSupportedException {
        return (LocalAsrParams) super.clone();
    }

}
