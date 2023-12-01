package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.common.Log;
import com.aispeech.export.widget.Scene;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Created by wuwei on 18-5-11.
 */

public class LocalGrammarParams extends SpeechParams implements Cloneable {
    private String outputPath = "";
    //private String ebnfFile = ""
    private String ebnfFilePath = "";
    private String ebnf = "";
    private String slotName;
    private boolean isBuildSuccess;
    private Scene scene;
    private AIError aiError;

    public AIError getAIError() {
        return aiError;
    }

    public void setAIError(AIError aiError) {
        this.aiError = aiError;
    }

    public Scene getScene() {
        return scene;
    }

    public void setScene(Scene scene) {
        this.scene = scene;
    }

    public boolean isBuildSuccess() {
        return isBuildSuccess;
    }

    public void setBuildSuccess(boolean buildSuccess) {
        isBuildSuccess = buildSuccess;
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }


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
        File file = new File(outputPath);
        if (!file.getParentFile().exists()) {
            Log.e(TAG, "Gram outputPath is not exist!! should create the file:" + file.getParentFile());
        }

        this.outputPath = outputPath;
    }

    public String getEbnfFilePath() {
        return ebnfFilePath;
    }

    /**
     * 设置输入的 ebnf 语法文件路径
     *
     * @param path ebnf 文件路径
     */
    public void setEbnfFilePath(String path) {
        if (TextUtils.isEmpty(path)) {
            Log.e(TAG, "Invalid ebnfFilePath!");
            return;
        }
        this.ebnfFilePath = path;
    }

    public String getEbnf() {
        if (TextUtils.isEmpty(ebnf)) {
            if (!TextUtils.isEmpty(ebnfFilePath))
                ebnf = readEbnf(ebnfFilePath);
            else
                throw new IllegalArgumentException("请指定 xbnf 模板路径");
        }
        return ebnf;
    }

    /**
     * 设置输入的ebnf语法文本
     *
     * @param ebnf 输入的ebnf语法
     */
    public void setEbnf(String ebnf) {
        if (TextUtils.isEmpty(ebnf)) {
            Log.e(TAG, "Invalid ebnf");
            return;
        }
        this.ebnf = ebnf;
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("outputPath", outputPath);
            //jsonObject.put("ebnfFile", ebnfFile)
            jsonObject.put("ebnf", getEbnf());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * 读取 xbnf 内容
     *
     * @param filePath xbnf 路径
     */
    private String readEbnf(String filePath) {
        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), Charset.forName("UTF-8")));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    @Override
    public LocalGrammarParams clone() throws CloneNotSupportedException {
        return (LocalGrammarParams) super.clone();
    }
}
