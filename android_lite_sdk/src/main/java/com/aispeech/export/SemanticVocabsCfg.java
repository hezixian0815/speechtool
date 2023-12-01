package com.aispeech.export;

import com.aispeech.common.JSONUtil;

import org.json.JSONObject;

public class SemanticVocabsCfg {

    public static final String  KEY_TEXT = "vocabsTextPath";
    public static final String  KEY_CFG = "vocabsCfgPath";
    private String vocabsTextPath;
    private String vocabsCfgPath;

    /**
     * 设置需要更新的词库文本路径
     *
     * @param vocabsTextPath 文本路径
     */
    public void setVocabsTextPath(String vocabsTextPath) {
        this.vocabsTextPath = vocabsTextPath;
    }

    /**
     * 设置项目product.cfg 配置文件路径
     *
     * @param vocabsCfgPath product.cfg路径
     */
    public void setVocabsCfgPath(String vocabsCfgPath) {
        this.vocabsCfgPath = vocabsCfgPath;
    }

    public String getVocabsTextPath() {
        return vocabsTextPath;
    }


    public String getVocabsCfgPath() {
        return vocabsCfgPath;
    }

    protected JSONObject jsonObject;
    /**
     * 参数JSON化
     *
     * @return JSONObject
     */
    public JSONObject toJSON() {
        jsonObject = new JSONObject();
        JSONUtil.putQuietly(jsonObject, KEY_TEXT, vocabsTextPath);
        JSONUtil.putQuietly(jsonObject, KEY_CFG, vocabsCfgPath);

        return jsonObject;
    }


}
