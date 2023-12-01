package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.common.JSONUtil;

import org.json.JSONObject;

/**
 * Created by wuwei on 18-6-19.
 */

public class LocalDmaspConfig extends AIEngineConfig {

    static final String KEY_RES_BIN_PATH = "resBinPath";//cfg

    static final String KEY_WAKEUP_BIN_PATH = "wakeupBinPath";//wakeup bin

    static final String KEY_PARALLEL_MODE = "parallel_mode";//暂不开放

    protected JSONObject jsonObject;

    /**
     * 唤醒资源
     */
    private String wakeupBinPath = "";
    /**
     * cfg资源
     */
    private String resBinPath = "";

    private int parallelMode = 1;//暂不开放

    /**
     * 设置 Dmasp 输出的通道数 支持 4Mic 2Mic
     * 该数据将影响驾驶模式抛出的单路音频
     * input: int 2、4、6, 默认4路
     */
    private int dmaspChannelCount = 4;

    /**
     * 设置资源文件名
     *
     * @param resBinPath 资源绝对路径
     */
    public void setResBinPath(String resBinPath) {
        this.resBinPath = resBinPath;
    }

    /**
     * 设置唤醒资源
     *
     * @param wakeupBinPath
     */
    public void setWakeupBinPath(String wakeupBinPath) {
        this.wakeupBinPath = wakeupBinPath;
    }

    /**
     * 设置  Parallel mode 暂不开放
     *
     * @param parallelMode
     * @return
     */
    public void setParallelMode(int parallelMode) {
        this.parallelMode = parallelMode;
    }

    /**
     * Dmasp 输出的通道数 支持 4Mic 2Mic
     * 该数据将影响驾驶模式抛出的单路音频
     *
     * @param channelCount int 2、4、6, 默认4路
     */
    public void setDmaspChannelCount(int channelCount) {
        this.dmaspChannelCount = channelCount;
    }

    public int getDmaspChannelCount() {
        return dmaspChannelCount;
    }

    /**
     * 参数JSON化
     *
     * @return JSONObject
     */
    public JSONObject toJSON() {
        jsonObject = super.toJson();

        if (!TextUtils.isEmpty(resBinPath)) {
            JSONUtil.putQuietly(jsonObject, KEY_RES_BIN_PATH, resBinPath);
        }

        if (!TextUtils.isEmpty(wakeupBinPath)) {
            JSONUtil.putQuietly(jsonObject, KEY_WAKEUP_BIN_PATH, wakeupBinPath);
        }

        JSONUtil.putQuietly(jsonObject, KEY_PARALLEL_MODE, parallelMode);

        return jsonObject;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}
