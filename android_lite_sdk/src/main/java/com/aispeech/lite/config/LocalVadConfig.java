package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;

import org.json.JSONObject;

/**
 * Created by yuruilong on 2017/5/22.
 */

public class LocalVadConfig extends AIEngineConfig {

    public static final String KEY_RES_BIN_PATH = "resBinPath";
    public static final String KEY_PAUSETIME = "pauseTime";
    public static final String KEY_FULL_MODE = "fullmode";//vad 常开模式
    public static final String KEY_MULTI_MODE = "multiMode";//vad 回调模式
    public static final String KEY_PAUSETIME_ARRAY = "pauseTimeArray";


    private String resBinFile;

    protected JSONObject jsonObject;


    private int pauseTime = 300;
    private int[] pauseTimeArray = new int[]{300, 500, 800};
    /**
     * 全双工输出模式，一次`start`操作后能输出多次状态跳变。
     * <p>
     * 0或不配置为关闭，1为打开。
     * </p>
     * default is false
     */
    private boolean fullMode = false;
    private int multiMode = 0;//vad 回调模式

    private boolean useSSL = false;

    private boolean useDoubleVad = false; //是否开启双vad

    public boolean isUseDoubleVad() {
        return useDoubleVad;
    }

    public void setUseDoubleVad(boolean useDoubleVad) {
        this.useDoubleVad = useDoubleVad;
    }

    public LocalVadConfig() {
    }
    public int isMultiMode() {
        return multiMode;
    }

    public void setMultiMode(int multiMode) {
        this.multiMode = multiMode;
    }

    public boolean isUseSSL() {
        return useSSL;
    }

    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }

    public boolean isFullMode() {
        return fullMode;
    }

    public void setFullMode(boolean fullMode) {
        this.fullMode = fullMode;
    }

    public void setPauseTime(int pauseTime) {
        this.pauseTime = pauseTime;
    }

    /**
     * 设置资源文件名
     *
     * @param resBinPath 资源绝对路径
     */
    public void setResBinPath(String resBinPath) {
        this.resBinFile = resBinPath;
    }

    /**
     * 获得资源绝对路径
     *
     * @return
     */
    public String getResBinPath() {
        return resBinFile;
    }
    public int[] getPauseTimeArray() {
        return pauseTimeArray;
    }

    public void setPauseTimeArray(int[] pauseTimeArray) {
        this.pauseTimeArray = pauseTimeArray;
    }

    /**
     * 参数JSON化
     *
     * @return JSONObject
     */
    public JSONObject toJSON() {
        jsonObject = new JSONObject();
        JSONUtil.putQuietly(jsonObject, "prof", Log.parseDuiliteLog());
        if (!TextUtils.isEmpty(resBinFile)) {
            JSONUtil.putQuietly(jsonObject, KEY_RES_BIN_PATH, resBinFile);
        }
        JSONUtil.putQuietly(jsonObject, KEY_PAUSETIME, pauseTime);
        JSONUtil.putQuietly(jsonObject, KEY_FULL_MODE, fullMode ? 1 : 0);
        if (multiMode == 1) {
            JSONUtil.putQuietly(jsonObject, KEY_PAUSETIME_ARRAY, pauseTimeArray);
            JSONUtil.putQuietly(jsonObject, KEY_MULTI_MODE, multiMode);
        }

        return jsonObject;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

}
