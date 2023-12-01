package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by wuwei on 18-5-11.
 */

public class LocalNgramParams extends SpeechParams implements Cloneable {
    private String outputPath = "";
    private String inputPath = "";
    private String slotData = "";


    public String getOutputPath() {
        return outputPath;
    }

    /**
     * 设置输出文件路径<br>
     *
     * @param outputPath 输出文件路径
     */
    public void setOutputPath(String outputPath) {
        if (TextUtils.isEmpty(outputPath)) {
            Log.e(TAG, "Invalid outputPath");
            return;
        }
        this.outputPath = outputPath;
    }

    public String getInputPath() {
        return inputPath;
    }

    /**
     * 按文件路径指定词库
     *
     * @param inputPath 词库文件路径，每行一个词条
     */
    public void setInputPath(String inputPath) {
        if (TextUtils.isEmpty(inputPath)) {
            Log.e(TAG, "Invalid inputPath!");
            return;
        }
        this.inputPath = inputPath;
    }


    /**
     * 设置词库内容，每个词条一行
     *
     * @param slotData 词库内容
     */
    public void setSlotData(String slotData) {
        if (TextUtils.isEmpty(slotData)) {
            Log.e(TAG, "Invalid slotData！！！");
            return;
        }
        this.slotData = slotData;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("outputPath", outputPath);
            if (!TextUtils.isEmpty(slotData)) {
                jsonObject.put("slotData", slotData);
            }
            if (!TextUtils.isEmpty(inputPath)) {
                jsonObject.put("inputPath", inputPath);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    @Override
    public LocalNgramParams clone() throws CloneNotSupportedException {
        return (LocalNgramParams) super.clone();
    }

}
