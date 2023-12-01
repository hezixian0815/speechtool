package com.aispeech.lite.param;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Dmasp 唤醒参数类
 */
public class DmaspParams extends SpeechParams {

    public static final String TAG = "LocalWakeupDnnParams";

    static final String KEY_THRESHOLD = "thresh";
    static final String KEY_WORDS = "words";
    static final String KEY_ENV = "env";
    static final String KEY_DCHECK = "dcheck";
    public static final String KEY_MAJOR = "major";

    private float[] threshold;
    private String[] pinyin;
    private int[] dcheck;
    private String path;
    private boolean inputContinuousAudio = true;
    private boolean preWakeupOn = false;
    private int[] majors;

    //  wakeup E2E config
    private String[] mCustomNet;
    private String[] mEnableNet;
    private String[] mThreshLow;
    private String[] mThreshHigh;
    // 设置多音区是否需要进行动态对齐
    private boolean isDynamicAlignment = true;

    private String env = "words=ni hao xiao chi;thresh=0.100;major=1;";

    public DmaspParams() {
        super();
//        setType(TYPE_NATIVE);
        setCoreType(CN_LOCAL_WAKEUP);
        // 唤醒不需要开启vad
//        setVadEnable(false);
        // 唤醒默认无录音时长的限制
        setMaxSpeechTimeS(0);
        setNoSpeechTimeout(0);
        setTag(TAG);
    }

    public void setThreshold(float[] threshold) {
        this.threshold = threshold;
    }

    public void setWords(String[] words) {
        this.pinyin = words;
    }

    public String[] getWords() {
        return pinyin;
    }

    public void setDchecks(int[] dchecks) {
        this.dcheck = dchecks;
    }

    public int[] getDcheck() {
        return dcheck;
    }

    public int[] getMajors() {
        return majors;
    }

    public void setWakeupSavedPath(String dirPath) {
        this.path = dirPath;
    }

    public String getWakeupSavedPath() {
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

    public String[] getCustomNet() {
        return mCustomNet;
    }

    public void setCustomNet(String[] mCustomNet) {
        this.mCustomNet = mCustomNet;
    }

    public String[] getEnableNet() {
        return mEnableNet;
    }

    public void setEnableNet(String[] mEnableNet) {
        this.mEnableNet = mEnableNet;
    }

    public String[] getThreshLow() {
        return mThreshLow;
    }

    public void setThreshLow(String[] mThreshLow) {
        this.mThreshLow = mThreshLow;
    }

    public String[] getThreshHigh() {
        return mThreshHigh;
    }

    public void setThreshHigh(String[] mThreshHigh) {
        this.mThreshHigh = mThreshHigh;
    }

    public float[] getThreshold() {
        return threshold;
    }

    public String toEnv() {
        StringBuilder envSb = new StringBuilder();

        appendWakeupWords(envSb);
        appendThreshold(envSb);
        appendMajors(envSb);
        appendDchecks(envSb);
        appendWakeupE2E(envSb);

        return envSb.toString();
    }

    private void appendDchecks(StringBuilder envSb) {
        if (dcheck != null) {
            StringBuilder dchecks = new StringBuilder();
            for (int i = 0; i < dcheck.length; i++) {
                dchecks.append(dcheck[i]);
                if (i != dcheck.length - 1) dchecks.append(",");
            }
            envSb.append(KEY_DCHECK + "=").append(dchecks).append(";");
        }
    }

    private void appendMajors(StringBuilder envSb) {
        StringBuilder majorSb = new StringBuilder();
        if (this.majors != null) {
            for (int i = 0; i < this.majors.length; i++) {
                majorSb.append(this.majors[i]);
                if (i != this.majors.length - 1) {
                    majorSb.append(",");
                }
            }
        } else {
            // majors为必须参数，自动补齐 如：{1,0}
            for (int i = 0; i < pinyin.length; i++) {
                majorSb.append(i == 0 ? 1 : 0);
                if (i != pinyin.length - 1) majorSb.append(",");
            }
        }

        envSb.append(KEY_MAJOR + "=").append(majorSb).append(";");
    }

    private void appendThreshold(StringBuilder envSb) {
        if (threshold != null && threshold.length > 0) {
            StringBuilder threshs = new StringBuilder();
            for (int i = 0; i < threshold.length; i++) {
                threshs.append(threshold[i]);
                if (i != threshold.length - 1) threshs.append(",");
            }

            envSb.append(KEY_THRESHOLD + "=").append(threshs).append(";");
        }
    }

    private void appendWakeupWords(StringBuilder envSb) {
        if (pinyin != null && pinyin.length > 0) {
            StringBuilder words = new StringBuilder();
            for (int i = 0; i < pinyin.length; i++) {
                words.append(pinyin[i]);
                if (i != pinyin.length - 1) words.append(",");
            }
            envSb.append(KEY_WORDS + "=").append(words).append(";");
        }
    }

    private void appendWakeupE2E(StringBuilder envSb) {
        // build cutsomNet
        if (mCustomNet != null) {
            StringBuilder customsSb = new StringBuilder();
            for (int i = 0; i < mCustomNet.length; i++) {
                customsSb.append(mCustomNet[i]);
                if (i != mCustomNet.length - 1) customsSb.append(",");
            }
            envSb.append("custom=").append(customsSb).append(";");
        }

        // build enableNet
        if (mEnableNet != null) {
            StringBuilder netsSb = new StringBuilder();
            for (int i = 0; i < mEnableNet.length; i++) {
                netsSb.append(mEnableNet[i]);
                if (i != mEnableNet.length - 1) netsSb.append(",");
            }
            envSb.append("net=").append(netsSb).append(";");
        }

        // build threshHigh
        if (mThreshHigh != null) {
            StringBuilder threshHighSb = new StringBuilder();
            for (int i = 0; i < mThreshHigh.length; i++) {
                threshHighSb.append(mThreshHigh[i]);
                if (i != mThreshHigh.length - 1) threshHighSb.append(",");
            }
            envSb.append("thresh_high=").append(threshHighSb).append(";");
        }

        // build threshLow
        if (mThreshLow != null) {
            StringBuilder threshLowSb = new StringBuilder();
            for (int i = 0; i < mThreshLow.length; i++) {
                threshLowSb.append(mThreshLow[i]);
                if (i != mThreshLow.length - 1) threshLowSb.append(",");
            }
            envSb.append("thresh_low=").append(threshLowSb).append(";");
        }
    }

    /**
     * 设置是否需要动态对齐，即：根据 sslIndex （送给识别的音频通道id） 来调整多音区输出的音频顺序
     *
     * @param dynamicAlignment true:表示在 sdk 中动态对齐音频通道，false: 不用动态对齐
     */
    public void setDynamicAlignment(boolean dynamicAlignment) {
        isDynamicAlignment = dynamicAlignment;
    }

    /**
     * 是否进行动态对齐，默认：true
     *
     * @return true:表示在 sdk 中动态对齐音频通道，false: 不用动态对齐
     */
    public boolean isDynamicAlignment() {
        return isDynamicAlignment;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject request = new JSONObject();
        try {
            if (pinyin != null) {
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
    public String toString() {
        return toJSON().toString();
    }

    @Override
    public DmaspParams clone() throws CloneNotSupportedException {
        return (DmaspParams) super.clone();
    }
}
