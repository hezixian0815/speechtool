package com.aispeech.lite.config;

import android.content.Context;

import com.aispeech.lite.AISpeech;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nemo on 17-12-15.
 */

public class WakeupConfig extends AIEngineConfig {
    /**
     * new config
     * native->cn.wakeup
     */
    private String resBinPath;
    private int useOutputBoundary = 0;
    private int continusWakeupEnable = 0;
    private int intervalTime = 100;
    private int sampleRate = 16000;
    private int maxRecorderSec = 60;
    private OneshotConfig oneshotConfig;

    /**
     * start config
     * request
     */
    private String env ;

    public int getSampleRate() {
        return sampleRate;
    }

    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getMaxRecorderSec() {
        return maxRecorderSec;
    }

    public void setMaxRecorderSec(int maxRecorderSec) {
        this.maxRecorderSec = maxRecorderSec;
    }

    public void setWakeupWord(String[] pinyin, float[] thresh) {
        StringBuilder words = new StringBuilder();
        for (int i = 0; i < pinyin.length; i++) {
            words.append(pinyin[i]);
            if (i == pinyin.length - 1) {
                words.append(";");
            } else {
                words.append(",");
            }
        }
        StringBuilder threshs = new StringBuilder();
        for (int i = 0; i < thresh.length; i++) {
            threshs.append(thresh[i]);
            if (i == thresh.length - 1) {
                threshs.append(";");
            } else {
                threshs.append(",");
            }
        }
        env = "words=" + words.toString() + "thresh=" + threshs.toString() + "major=1;";
    }


    public void setWakeupWord(String[] pinyin, String[] thresh) {
        StringBuilder words = new StringBuilder();
        for (int i = 0; i < pinyin.length; i++) {
            words.append(pinyin[i]);
            if (i == pinyin.length - 1) {
                words.append(";");
            } else {
                words.append(",");
            }
        }
        StringBuilder threshs = new StringBuilder();
        for (int i = 0; i < thresh.length; i++) {
            threshs.append(thresh[i]);
            if (i == thresh.length - 1) {
                threshs.append(";");
            } else {
                threshs.append(",");
            }
        }
        env = "words=" + words.toString() + "thresh=" + threshs.toString() + "major=1;";
    }

    public String getStartConfigJSON() {
        JSONObject request = new JSONObject();
        try {
            request.put("env", env);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return request.toString();
    }

    public String getResBinPath() {
        return resBinPath;
    }

    public void setResBinPath(String resBinPath) {
        this.resBinPath = resBinPath;
    }

    public int getUseOutputBoundary() {
        return useOutputBoundary;
    }

    public void setUseOutputBoundary(int useOutputBoundary) {
        this.useOutputBoundary = useOutputBoundary;
    }

    @Override
    public Context getContext() {
        return AISpeech.getContext();
    }

    public int getContinusWakeupEnable() {
        return continusWakeupEnable;
    }

    public void setContinusWakeupEnable(int continusWakeupEnable) {
        this.continusWakeupEnable = continusWakeupEnable;
    }

    public int getIntervalTime() {
        return intervalTime;
    }

    public void setIntervalTime(int intervalTime) {
        this.intervalTime = intervalTime;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public OneshotConfig getOneshotConfig() {
        return oneshotConfig;
    }

    public void setOneshotConfig(OneshotConfig oneshotConfig) {
        this.oneshotConfig = oneshotConfig;
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public JSONObject toJson() {
        JSONObject cnwakeup = super.toJson();
        try {
            cnwakeup.put("resBinPath", resBinPath);
            cnwakeup.put("useOutputBoundary", useOutputBoundary);
            cnwakeup.put("continusWakeupEnable", continusWakeupEnable);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return cnwakeup;
    }

}
