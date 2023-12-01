package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.lite.AISpeechSDK;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by wuwei on 2018/7/11.
 */

public class VprintParams extends SpeechParams {

    /**
     * vprint env params
     * op=register;name=testname;audionum=6;word=ni hao xiao le;thresh=0.5;channelnum=2; (TIPS:thresh ,channelnum , )
     * op=test;channelnum=2; (TIPS:channelnum , )
     * op=unregister;name=strname;word=ni hao xiao le;
     * op=update;name=testname;audionum=6;word=ni hao xiao le;thresh=0.5;channelnum=2;(TIPS:thresh ,channelnum , )
     * op=query;
     * op=append;name=testname;word=ni hao xiao le;channelnum=2; (TIPS:channelnum , )
     */

    private static final String KEY_THRESH = "thresh";
    private static final String KEY_SNR = "snr";
    private static final String KEY_ENV = "env";
    private static final String KEY_NAME = "name";
    private static final String KEY_AUDIONUM = "audionum";
    private static final String KEY_BF_CHANNEL = "bfchannel";
    private static final String KEY_AEC_CHANNEL = "aecchannel";
    private static final String KEY_OUT_CHANNEL = "outchannel";
    private static final String KEY_WORD = "word";
    private static final String KEY_OP = "op";
    private static final String KEY_MODE_REGISTER = "register";
    private static final String KEY_MODE_UNREGISTER = "unregister";
    private static final String KEY_MODE_UPDATE = "update";
    private static final String KEY_MODE_TEST = "test";
    private static final String KEY_MODE_APPEND = "append";
    private static final String KEY_LOG_PATH = "logpath";//保存音频


    private float thresh = Float.MAX_VALUE;
    private float snrThresh = 8.69f;
    private String userId;
    private int trainNum = 4;
    private int bfChannelNum;
    private int aecChannelNum;
    private int outChannelNum;
    private String[] vprintWord;
    private String action;
    /**
     * 敏感度等级，默认0等级的灵敏度。对应cfg中的第一组阈值，env中可以不设置。
     * sensitivity_level从0开始，小于toal_sensitivity_level.，传入不合法会返回 VPRINT_ERROR_SENSITIVITY_INVALID
     */
    private int sensitivityLevel = 0;
    // 保存唤醒引擎吐给声纹的音频数据，从tlv数据里获取
    private String vprintCutSaveDir = null;

    public int getSensitivityLevel() {
        return sensitivityLevel;
    }

    public void setSensitivityLevel(int sensitivityLevel) {
        this.sensitivityLevel = sensitivityLevel;
    }

    public String getVprintCutSaveDir() {
        if (TextUtils.isEmpty(vprintCutSaveDir) &&
                !TextUtils.isEmpty(AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH)) {
            return AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH;
        }
        return vprintCutSaveDir;
    }

    public void setVprintCutSaveDir(String vprintCutSaveDir) {
        this.vprintCutSaveDir = vprintCutSaveDir;
    }

    public int getBfChannelNum() {
        return bfChannelNum;
    }

    public void setBfChannelNum(int bfChannelNum) {
        this.bfChannelNum = bfChannelNum;
    }

    public int getAecChannelNum() {
        return aecChannelNum;
    }

    public void setAecChannelNum(int aecChannelNum) {
        this.aecChannelNum = aecChannelNum;
    }

    public int getOutChannelNum() {
        return outChannelNum;
    }

    public void setOutChannelNum(int outChannelNum) {
        this.outChannelNum = outChannelNum;
    }

    public float getSnrThresh() {
        return snrThresh;
    }

    public void setSnrThresh(float snrThresh) {
        this.snrThresh = snrThresh;
    }

    public float getThresh() {
        return thresh;
    }

    public void setThresh(float thresh) {
        this.thresh = thresh;
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public int getTrainNum() {
        return trainNum;
    }

    public void setTrainNum(int trainNum) {
        this.trainNum = trainNum;
    }


    public String[] getVprintWord() {
        return vprintWord;
    }

    public void setVprintWord(String[] vprintWord) {
        this.vprintWord = vprintWord;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }


    private String toEnv() {
        StringBuilder envSb = new StringBuilder();
        envSb.append(KEY_OP + "=" + this.action + ";");
        if (!TextUtils.isEmpty(getSaveAudioPath())) {
            envSb.append(KEY_LOG_PATH + "=" + getSaveAudioPath() + File.separator + "vprintMic" + ";");
        }
        if (TextUtils.equals(action, KEY_MODE_REGISTER) ||
                TextUtils.equals(action, KEY_MODE_UPDATE) ||
                TextUtils.equals(action, KEY_MODE_APPEND)) {
            handleActionRegisterUpdateAppend(envSb);
        } else if (TextUtils.equals(action, KEY_MODE_UNREGISTER)) {
            handleActionModeUnregister(envSb);
        } else if (TextUtils.equals(action, KEY_MODE_TEST)) {
            handleActionModeTest(envSb);
        }
        return envSb.toString();
    }

    private void handleActionRegisterUpdateAppend(StringBuilder envSb) {
        if (this.thresh != Float.MAX_VALUE) {
            envSb.append(KEY_THRESH + "=" + this.thresh + ";");
        }
        StringBuilder words = new StringBuilder();
        if (vprintWord != null)
            for (int i = 0; i < vprintWord.length; i++) {
                words.append(vprintWord[i]);
                if (i < vprintWord.length - 1)
                    words.append(",");
            }
        envSb.append(KEY_WORD + "=" + words.toString() + ";");
        envSb.append(KEY_NAME + "=" + this.userId + ";");
        if (this.bfChannelNum != 0) {
            envSb.append(KEY_BF_CHANNEL + "=" + this.bfChannelNum + ";");
        }
        if (this.aecChannelNum != 0) {
            envSb.append(KEY_AEC_CHANNEL + "=" + this.aecChannelNum + ";");
        }
        if (this.outChannelNum != 0) {
            envSb.append(KEY_OUT_CHANNEL + "=" + this.outChannelNum + ";");
        }
        envSb.append(KEY_AUDIONUM + "=" + this.trainNum + ";");
        envSb.append(KEY_SNR + "=" + this.snrThresh + ";");
    }

    private void handleActionModeUnregister(StringBuilder envSb) {
        StringBuilder words = new StringBuilder();
        if (vprintWord != null)
            for (int i = 0; i < vprintWord.length; i++) {
                words.append(vprintWord[i]);
                if (i < vprintWord.length - 1)
                    words.append(",");
            }
        envSb.append(KEY_WORD + "=" + words + ";");
        envSb.append(KEY_NAME + "=" + this.userId + ";");
    }

    private void handleActionModeTest(StringBuilder envSb) {
        if (this.bfChannelNum != 0) {
            envSb.append(KEY_BF_CHANNEL + "=" + this.bfChannelNum + ";");
        }
        if (this.aecChannelNum != 0) {
            envSb.append(KEY_AEC_CHANNEL + "=" + this.aecChannelNum + ";");
        }
        if (this.outChannelNum != 0) {
            envSb.append(KEY_OUT_CHANNEL + "=" + this.outChannelNum + ";");
        }
        if (this.thresh != Float.MAX_VALUE) {
            envSb.append(KEY_THRESH + "=" + this.thresh + ";");
        }
        envSb.append("sensitivity_level=" + sensitivityLevel + ";");
    }

    @Override
    public JSONObject toJSON() {
        JSONObject request = new JSONObject();
        try {
            request.put(KEY_ENV, toEnv());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return request;
    }

    @Override
    public VprintParams clone() throws CloneNotSupportedException {
        return (VprintParams) super.clone();
    }
}
