package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.common.JSONUtil;

import org.json.JSONObject;

/**
 * @decription TODO
 * @auther wuwei
 * @date 2018/10/23 下午5:32
 * @email wei.wu@aispeech.com
 */
public class LocalNRConfig extends AIEngineConfig {
    public static final String KEY_RES_BIN_PATH = "resBinPath";

    private String resBinFile;
    private JSONObject jsonObject;

    /**
     * 设置资源文件名
     * @param resBinPath
     * 资源绝对路径
     */
    public void setResBinPath(String resBinPath) {
        this.resBinFile = resBinPath;
    }


    /**
     * 获得资源绝对路径
     * @return
     */
    public String getResBinPath(){
        return resBinFile;
    }


    /**
     * 参数JSON化
     *
     * @return JSONObject
     */
    public JSONObject toJSON() {
        jsonObject = new JSONObject();
        if(!TextUtils.isEmpty(resBinFile)){
            JSONUtil.putQuietly(jsonObject, KEY_RES_BIN_PATH, resBinFile);
        }
        return jsonObject;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }
}
