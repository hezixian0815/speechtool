package com.aispeech.lite.param;

import android.media.AudioManager;

import com.aispeech.common.Util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by yu on 2018/5/7.
 */

public class LocalTtsParams extends TTSParams {

    private float speed = 1.0f;
    private int volume = 80;
    private int lmargin = 10;
    private int rmargin = 10;
    private String refText;
    public boolean useTimeStamp;
    private int streamType = AudioManager.STREAM_MUSIC;

    /**
     * 后端发音人资源,用作判断是否是相同的合成请求
     */
    private String backBin;

    private boolean useSSML = false;//是否使用SSML，false不启用，默认关闭;　true启用。
    /**
     * 表示 CPU 睡眠时间
     * 配置了 optimization 为 true 时，此选项才生效。取值范围为 0-500，
     * 默认为 0，可选。
     */
    private int sleepTime = 0;

    public int getSleepTime() {
        return sleepTime;
    }

    /**
     * 表示 CPU 睡眠时间
     * 配置了 optimization 为 true 时，此选项才生效。取值范围为 0-500，
     * 默认为 0，可选。
     */
    public void setSleepTime(int sleepTime) {
        this.sleepTime = sleepTime;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public float getSpeed() {
        return speed;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public int getVolume() {
        return volume;
    }

    public void setLmargin(int lmargin) {
        this.lmargin = lmargin;
    }

    public int getLmargin() {
        return lmargin;
    }

    public void setRmargin(int rmargin) {
        this.rmargin = rmargin;
    }

    public int getRmargin() {
        return rmargin;
    }

    public String getRefText() {
        return refText;
    }

    public void setRefText(String refText) {
        this.refText = refText;
    }

    @Override
    public boolean isRealBack() {
        return true;
    }


    @Override
    public String getType() {
        return TYPE_NATIVE;
    }

    public String getBackBin() {
        return backBin;
    }

    public void setBackBin(String backBin) {
        this.backBin = backBin;
    }

    /**
     * 设置是否使用ssml 默认不使用为false
     *
     * @param useSSML 是否配置ssml
     */
    public void setUseSSML(boolean useSSML) {
        this.useSSML = useSSML;
    }

    public boolean isUseSSML() {
        return useSSML;
    }


    public boolean isUseTimeStamp() {
        return useTimeStamp;
    }

    @Override
    public boolean isReturnPhone() {
        return useTimeStamp;
    }

    public void setUseTimeStamp(boolean useTimeStamp) {
        this.useTimeStamp = useTimeStamp;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("speed", speed);
            jsonObject.put("volume", volume);
            jsonObject.put("lmargin", lmargin);
            jsonObject.put("rmargin", rmargin);
            jsonObject.put("useTimeStamp", useTimeStamp ? 1 : 0);
            jsonObject.put("sleepTime",sleepTime);
            if (useSSML) {
                jsonObject.put("useSSML", 1);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public LocalTtsParams clone() throws CloneNotSupportedException {
        return (LocalTtsParams) super.clone();
    }

    public String getSHA1Name() {
        return Util.SHA1(toString());
    }

    @Override
    public String getAudioType() {
        return "wav";
    }

    @Override
    public String toString() {
        return "LocalTtsParams{" +
                "speed=" + speed +
                ", volume=" + volume +
                ", lmargin=" + lmargin +
                ", rmargin=" + rmargin +
                ", refText='" + refText + '\'' +
                ", backBin='" + backBin + '\'' +
                ", useSSML=" + useSSML +
                ", streamType=" + streamType +
                ", useTimeStamp=" + useTimeStamp +
                ", sleepTime=" + sleepTime +
                '}';
    }
}
