package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;

import org.json.JSONObject;

/**
 * Created by wuwei on 18-6-19.
 */

public class LocalEchoConfig extends AIEngineConfig {
    /*{
         "resBinPath": "./third/res/echo/AEC_ch2-2-ch1_1ref_com_20171227_v0.8.5.bin",
         "channels": 2,
         "micNum": 1,
         "sampleFormat": 16
    }*/

    public static final String KEY_RES_BIN_PATH = "resBinPath";
    public static final String KEY_CHANNELS = "channels";
    public static final String KEY_MIC_NUM = "micNum";
    public static final String KEY_SAMPLEFORMAT = "sampleFormat";
    protected JSONObject jsonObject;

    private String resBinPath = "";
    private int channels = 2;
    private int micNum = 1;
    private int sampleFormat = 16;


    /**
     * 设置音频总的通道数，2
     * @param channels 音频总的通道数
     */
    public void setChannels(int channels) {
        this.channels = channels;
    }

    /**
     * 设置mic数,1
     * @param micNum mic数
     */
    public void setMicNum(int micNum) {
        this.micNum = micNum;
    }

    /**
     * 设置资源文件名
     *
     * @param resBinPath
     *                资源绝对路径
     */
    public void setResBinPath(String resBinPath) {
        this.resBinPath = resBinPath;
    }

    /**
     * 获得资源绝对路径
     * @return
     */
    public String getResBinPath(){
        return resBinPath;
    }

    /**
     * 参数JSON化
     *
     * @return JSONObject
     */
    public JSONObject toJSON() {
        jsonObject = new JSONObject();
        JSONUtil.putQuietly(jsonObject, "prof", Log.parseDuiliteLog());
        if(!TextUtils.isEmpty(resBinPath)){
            JSONUtil.putQuietly(jsonObject, KEY_RES_BIN_PATH, resBinPath);
        }
        JSONUtil.putQuietly(jsonObject, KEY_CHANNELS, channels);
        JSONUtil.putQuietly(jsonObject, KEY_MIC_NUM, micNum);
        JSONUtil.putQuietly(jsonObject, KEY_SAMPLEFORMAT, sampleFormat);
        return jsonObject;
    }
    /**
     * 参数JSON化
     *
     * @return JSONObject
     */
    public JSONObject toSspeJSON() {
        jsonObject = new JSONObject();
        JSONUtil.putQuietly(jsonObject, "prof", Log.parseDuiliteLog());
        if(!TextUtils.isEmpty(resBinPath)) {
            JSONUtil.putQuietly(jsonObject, "sspeBinPath", resBinPath);
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}
