package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.common.AIConstant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wuwei on 2018/7/11.
 */

public class VprintLiteParams extends SpeechParams {

    /**
     * #init
     * {"configPath":"model/yadi.bin","modelPath":"models/a","type":"textDependent"}
     * # register
     * {"activity":"register","env":{"asrErrorRate":0,"constantContent":"","customContent":"你好小迪9527","enhanceRegister":true,"uId":"3"},"operator":"start"}
     * {"activity":"register","asrInfo":{"alignment":[{"rec":"你","start":0},{"end":45972,"rec":"7"}],"rec":"你好小迪9527"},"operator":"calc"}
     * <p>
     * # verify
     * {"activity":"verify","env":{"asrErrorRate":0,"constantContent":"","customContent":"你好小迪9527","topN":1},"operator":"start"}
     * {"activity":"verify","asrInfo":{"alignment":[{"rec":"你","start":0},{"end":45972,"rec":"7"}],"rec":"你好小迪9527"},"operator":"calc"}
     * <p>
     * # unregister
     * {"activity":"unregister","env":{"uId":"3"},"operator":"start"}
     */
    private static final String KEY_ENV = "env";
    private static final String KEY_ACTIVITY = "activity";
    private static final String KEY_OPERATOR = "operator";
    private static final String KEY_MODE_REGISTER = "register";
    private static final String KEY_MODE_VERIFY = "verify";
    private static final String KEY_MODE_UNREGISTER = "unregister";
    private static final String KEY_ASR_ERROR_RATE = "asrErrorRate";
    private static final String KEY_CONTAN_CONTENT = "constantContent";
    private static final String KEY_CUSTOM_CONTENT = "customContent";
    private static final String KEY_ENHANCE_REGISTER = "enhanceRegister";
    private static final String KEY_UID = "uId";
    private static final String KEY_TOPN = "topN";
    private static final String KEY_ALIGN = "alignment";
    private static final String KEY_ASR_INFO = "asrInfo";
    private static final String KEY_REC = "rec";
    private static final String KEY_REC_START = "start";
    private static final String KEY_REC_END = "end";
    private static final String VALUE_START = "start";
    private static final String VALUE_CAL = "calc";
    private static final String KEY_STATE = "state";
    private static final String KEY_AUDIO_INFO = "audioInfo";

    private String action;
    private int asrErrorRate;
    private String constantContent;
    private String customContent;
    private boolean enhanceRegister;
    private int topN;
    private String recWords;
    private long resStart;
    private long resEnd;
    private String uId;
    private String speechState = "speech";
    /**
     * 敏感度等级，默认0等级的灵敏度。对应cfg中的第一组阈值，env中可以不设置。
     * sensitivity_level从0开始，小于toal_sensitivity_level.，传入不合法会返回 VPRINT_ERROR_SENSITIVITY_INVALID
     */
    private int sensitivityLevel = 0;
    // 保存唤醒引擎吐给声纹的音频数据，从tlv数据里获取
    private String vprintLiteSaveDir = null;

    public String getRecWords() {
        return recWords;
    }

    public void setRecWords(String recWords) {
        this.recWords = recWords;
    }

    public long getResStart() {
        return resStart;
    }

    public void setResStart(long resStart) {
        this.resStart = resStart;
    }

    public long getResEnd() {
        return resEnd;
    }

    public void setResEnd(long resEnd) {
        this.resEnd = resEnd;
    }

    public int getTopN() {
        return topN;
    }

    public void setTopN(int topN) {
        this.topN = topN;
    }

    public String getConstantContent() {
        return constantContent;
    }

    public void setConstantContent(String constantContent) {
        this.constantContent = constantContent;
    }

    public String getCustomContent() {
        return customContent;
    }

    public void setCustomContent(String customContent) {
        this.customContent = customContent;
    }

    public boolean isEnhanceRegister() {
        return enhanceRegister;
    }

    public void setEnhanceRegister(boolean enhanceRegister) {
        this.enhanceRegister = enhanceRegister;
    }

    public String getuId() {
        return uId;
    }

    public void setuId(String uId) {
        this.uId = uId;
    }

    public int getSensitivityLevel() {
        return sensitivityLevel;
    }

    public void setSensitivityLevel(int sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    public String getVprintLiteSaveDir() {
        return vprintLiteSaveDir;
    }

    public void setVprintLiteSaveDir(String vprintLiteSaveDir) {
        this.vprintLiteSaveDir = vprintLiteSaveDir;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSpeechState() {
        return speechState;
    }

    public void setSpeechState(String speechState) {
        this.speechState = speechState;
    }

    public JSONObject toStartJSON(String type) {
        JSONObject request = new JSONObject();
        try {
            request.put(KEY_ACTIVITY, this.action);
            JSONObject object = new JSONObject();
            if (action.equals(KEY_MODE_REGISTER)) {
                if (type.equals(AIConstant.VPRINTLITE_TYPE_TD)) {
                    object.put(KEY_ASR_ERROR_RATE, getAsrErrorRate());
                    object.put(KEY_CONTAN_CONTENT, getConstantContent());
                    object.put(KEY_CUSTOM_CONTENT, getCustomContent());
                }
                object.put(KEY_ENHANCE_REGISTER, isEnhanceRegister());
                object.put(KEY_UID, getuId());
            } else if (action.equals(KEY_MODE_VERIFY)) {
                if (type.equals(AIConstant.VPRINTLITE_TYPE_TD)) {
                    object.put(KEY_ASR_ERROR_RATE, getAsrErrorRate());
                    object.put(KEY_CONTAN_CONTENT, getConstantContent());
                    object.put(KEY_CUSTOM_CONTENT, getCustomContent());
                }
                object.put(KEY_TOPN, getTopN());
            } else if (action.equals(KEY_MODE_UNREGISTER)) {
                object.put(KEY_UID, getuId());
            }
            request.put(KEY_ENV, object);
            request.put(KEY_OPERATOR, VALUE_START);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return request;
    }

    public JSONObject toCalJSON(String type) {
        JSONObject request = new JSONObject();
        try {
            request.put(KEY_ACTIVITY, this.action);
            request.put(KEY_OPERATOR, VALUE_CAL);
            JSONObject object = new JSONObject();
            if (type.equals(AIConstant.VPRINTLITE_TYPE_TD)) {
                if (action.equals(KEY_MODE_REGISTER) || action.equals(KEY_MODE_VERIFY)) {
                    String resWords = getRecWords();
                    object.put(KEY_REC, resWords);
                    JSONArray array = new JSONArray();
                    JSONObject objectStart = new JSONObject();
                    objectStart.put(KEY_REC, TextUtils.isEmpty(resWords) ? "" : resWords.substring(0, 1));
                    objectStart.put(KEY_REC_START, getResStart());
                    array.put(objectStart);
                    JSONObject objectEnd = new JSONObject();
                    objectEnd.put(KEY_REC, TextUtils.isEmpty(resWords) ? "" : resWords.substring(resWords.length() - 1));
                    objectEnd.put(KEY_REC_END, getResEnd());
                    array.put(objectEnd);
                    object.put(KEY_ALIGN, array);
                }
                request.put(KEY_ASR_INFO, object);
            } else if (type.equals(AIConstant.VPRINTLITE_TYPE_SR) || type.equals(AIConstant.VPRINTLITE_TYPE_ANTI_SPOOFING)) {
                if (action.equals(KEY_MODE_REGISTER) || action.equals(KEY_MODE_VERIFY)) {
                    object.put(KEY_STATE, getSpeechState());
                    object.put(KEY_REC_START, getResStart());
                    object.put(KEY_REC_END, getResEnd());
                }
                request.put(KEY_AUDIO_INFO, object);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return request;
    }

    private int getAsrErrorRate() {
        return asrErrorRate;
    }

    public void setAsrErrorRate(int asrErrorRate) {
        this.asrErrorRate = asrErrorRate;
    }

    @Override
    public VprintLiteParams clone() throws CloneNotSupportedException {
        return (VprintLiteParams) super.clone();
    }
}
