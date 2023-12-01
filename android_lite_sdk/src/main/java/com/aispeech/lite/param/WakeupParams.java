/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.lite.AISpeechSDK;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 本地唤醒参数类
 */
public class WakeupParams extends SpeechParams {

    public static final String TAG = "LocalWakeupDnnParams";

    static final String KEY_THRESHOLD = "thresh";
    static final String KEY_WORDS = "words";
    static final String KEY_ENV = "env";
    static final String KEY_DCHECK = "dcheck";

    private String[] threshold;
    private String[] pinyin;
    private String[] dcheck;
    private int vad = 1;
    private String path;
    private boolean inputContinuousAudio = true;

    private boolean preWakeupOn = false;
    private int[] majors;


    /**
     * feed段音频时是否增加额外的音频，目的是为了使本应唤醒的音频更容易唤醒
     */
    private boolean addExtraAudioWhenFeedNotContinuousAudio = false;
    private String[] mNet;
    private String[] mCustom;
    private String[] mThreshLow;
    private String[] mThreshHigh;

    public boolean isAddExtraAudioWhenFeedNotContinuousAudio() {
        return addExtraAudioWhenFeedNotContinuousAudio;
    }

    public void setAddExtraAudioWhenFeedNotContinuousAudio(boolean addExtraAudioWhenFeedNotContinuousAudio) {
        this.addExtraAudioWhenFeedNotContinuousAudio = addExtraAudioWhenFeedNotContinuousAudio;
    }

    public boolean inputContinuousAudio() {
        return inputContinuousAudio;
    }

    /**
     * 设置是否输入实时的长音频，默认输入实时的长音频
     *
     * @param continuousAudio
     */
    public void setContinuousAudio(boolean continuousAudio) {
        inputContinuousAudio = continuousAudio;
    }

    private String env;

    public int getVad() {
        return vad;
    }

    /**
     * 设置是否启动vad功能，需要资源配合。在资源支持vad功能的情况下，默认启动vad。<br></br>
     * vad 需引擎 stop 后重新 start 才能生效
     *
     * @param vad 1 启动(default)，0 不启动
     */
    public void setVad(int vad) {
        this.vad = vad;
    }

    public WakeupParams() {
        super();
        setCoreType(CN_LOCAL_WAKEUP);
        setMaxSpeechTimeS(0);
        setNoSpeechTimeout(0);
        setTag(TAG);
    }

    public void setThreshold(String[] threshold) {
        this.threshold = threshold == null ? new String[]{} : threshold;
    }

    public void setThresholds(float[] threshold) {
        if (threshold != null && threshold.length > 0) {
            String[] thresholdArray = new String[threshold.length];
            for(int i=0;i<threshold.length;i++){
                thresholdArray[i] = String.valueOf(threshold[i]);
            }
            this.threshold = thresholdArray;
        }
    }

    public void setWords(String[] words) {
        this.pinyin = words;
    }

    public String[] getWords() {
        return this.pinyin;
    }

    public void setDchecks(String[] dchecks) {
        this.dcheck = dchecks;
    }

    public void setDchecks(int[] dcheck){
        if (dcheck != null && dcheck.length > 0) {
            String[] dCheckArray = new String[dcheck.length];
            for(int i=0;i<dcheck.length;i++){
                dCheckArray[i] = String.valueOf(dcheck[i]);
            }
            this.dcheck = dCheckArray;
        }
    }

    public void setWakeupSavedPath(String dirPath) {
        this.path = dirPath;
    }

    public String getWakeupSavedPath() {
        if (TextUtils.isEmpty(path) && !TextUtils.isEmpty(AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH)) {
                return AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH + "/wakeup";
        }
        return this.path;
    }
    public void setPreWakeupOn(boolean preWakeupOn) {
        this.preWakeupOn = preWakeupOn;
    }

    public boolean isPreWakeupOn() {
        return preWakeupOn;
    }


    public void setMajors(int[] majors) {
        this.majors = majors;
    }

    private String toEnv() {
        StringBuilder words = new StringBuilder();
        StringBuilder majorSB = new StringBuilder();
        StringBuilder results = new StringBuilder();
        for (int i = 0; i < pinyin.length; i++) {
            words.append(pinyin[i]);
            if (i == pinyin.length - 1) {
                words.append(";");
            } else {
                words.append(",");
            }
        }

        if (this.majors != null) {
            for (int i = 0; i < this.majors.length; i++) {
                majorSB.append(this.majors[i]);
                if (i == this.majors.length - 1) {
                    majorSB.append(";");
                } else {
                    majorSB.append(",");
                }
            }
        } else {
            // majors为必须参数，自动补齐 如：{1,0}
            for (int i = 0; i < pinyin.length; i++) {
                majorSB.append(i == 0 ? 1 : 0);
                if (i == pinyin.length - 1) {
                    majorSB.append(";");
                } else {
                    majorSB.append(",");
                }
            }
        }
        results.append("words=");
        results.append(words);
        results.append("major=");
        results.append(majorSB);
        StringBuilder threshs = new StringBuilder();
        for (int i = 0; i < threshold.length; i++) {
            threshs.append(threshold[i]);
            if (i == threshold.length - 1) {
                threshs.append(";");
            } else {
                threshs.append(",");
            }
        }
        results.append("thresh=");
        results.append(threshs);
        results.append("vad=");
        results.append(vad);
        results.append(";");
        if (dcheck != null) {
            StringBuilder dchecks = new StringBuilder();
            for (int i = 0; i < dcheck.length; i++) {
                dchecks.append(dcheck[i]);
                if (i == dcheck.length - 1) {
                    dchecks.append(";");
                } else {
                    dchecks.append(",");
                }
            }
            results.append("dcheck=");
            results.append(dchecks);
        }
        if (mCustom != null) {
            StringBuilder customs = new StringBuilder();
            for (int i = 0; i < mCustom.length; i++) {
                customs.append(mCustom[i]);
                if (i == mCustom.length - 1) {
                    customs.append(";");
                } else {
                    customs.append(",");
                }
            }
            results.append("custom=");
            results.append(customs);
        }
        if (mNet != null) {
            StringBuilder nets = new StringBuilder();
            for (int i = 0; i < mNet.length; i++) {
                nets.append(mNet[i]);
                if (i == mNet.length - 1) {
                    nets.append(";");
                } else {
                    nets.append(",");
                }
            }
            results.append("net=");
            results.append(nets);
        }
        if (mThreshHigh != null) {
            StringBuilder threshHigh = new StringBuilder();
            for (int i = 0; i < mThreshHigh.length; i++) {
                threshHigh.append(mThreshHigh[i]);
                if (i == mThreshHigh.length - 1) {
                    threshHigh.append(";");
                } else {
                    threshHigh.append(",");
                }
            }
            results.append("thresh_high=");
            results.append(threshHigh);
        }
        if (mThreshLow != null) {
            StringBuilder threshLow = new StringBuilder();
            for (int i = 0; i < mThreshLow.length; i++) {
                threshLow.append(mThreshLow[i]);
                if (i == mThreshLow.length - 1) {
                    threshLow.append(";");
                } else {
                    threshLow.append(",");
                }
            }
            results.append("thresh_low=");
            results.append(threshLow);
        }
        return results.toString();
    }

    @Override
    public JSONObject toJSON() {
        JSONObject request = new JSONObject();
        try {
            if (pinyin != null) {
                request.put(KEY_ENV, toEnv());
            } else {
                Log.e(TAG,"wakeup words can not be null ");
//                request.put(KEY_ENV, env);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return request;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    @Override
    public WakeupParams clone() throws CloneNotSupportedException {
        return (WakeupParams) super.clone();
    }

    public void setNet(String[] net) {
        mNet = net;
    }

    public String[] getNet() {
        return mNet;
    }

    public void setCustom(String[] custom) {
        mCustom = custom;
    }

    public String[] getCustom() {
        return mCustom;
    }

    public void setThreshLow(String[] threshLow) {
        mThreshLow = threshLow;
    }

    public String[] getThreshLow() {
        return mThreshLow;
    }

    public void setThreshHigh(String[] threshHigh) {
        mThreshHigh = threshHigh;
    }

    public String[] getThreshHigh() {
        return mThreshHigh;
    }

    public String[] getThreshold() {
        return threshold;
    }

    public int[] getMajors() {
        return majors;
    }

    public String[] getDcheck() {
        return dcheck;
    }
}
