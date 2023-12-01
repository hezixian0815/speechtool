package com.aispeech.lite.param;

import com.aispeech.common.JSONUtil;

import org.json.JSONObject;

/**
 * Created by yuruilong on 2017/5/22.
 */

public class VadParams extends SpeechParams {
    public static final String KEY_PAUSETIME = "pauseTime";
    public static final String KEY_PAUSETIME_ARRAY = "pauseTimeArray";
    private int pauseTime = 300;
    private int[] pauseTimeArray = new int[]{300, 500, 800};
    private int multiMode;
    private boolean isVadEnable = true;

    protected JSONObject jsonObject;


    public VadParams() {
        super();
        setIsAttachAudioParam(false);
    }
    public int[] getPauseTimeArray() {
        return pauseTimeArray;
    }

    public void setPauseTimeArray(int[] pauseTimeArray) {
        this.pauseTimeArray = pauseTimeArray;
    }

    public int getPauseTime() {
        return pauseTime;
    }

    public void setPauseTime(int pauseTime) {
        this.pauseTime = pauseTime;
    }


    public void setVadEnable(boolean isVadEnable) {
        this.isVadEnable = isVadEnable;
    }

    public boolean isVadEnable() {
        return isVadEnable;
    }
    public int getMultiMode() {
        return multiMode;
    }

    public void setMultiMode(int multiMode) {
        this.multiMode = multiMode;
    }

    public JSONObject toJSON() {
        jsonObject = new JSONObject();
        JSONUtil.putQuietly(jsonObject, KEY_PAUSETIME, pauseTime);
        //TODO：可以和pauseTime共存，start时可以改变value，但pauseTimeArray要跟config中的pauseTimeArray个数要对应，否则出错，研究暂定这么设计
        if (multiMode == 1)
            JSONUtil.putQuietly(jsonObject, KEY_PAUSETIME_ARRAY, pauseTimeArray);
        return jsonObject;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    @Override
    public VadParams clone() throws CloneNotSupportedException {
        return (VadParams) super.clone();
    }

}
