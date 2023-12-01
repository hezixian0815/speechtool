package com.aispeech.export.bean;

import com.aispeech.AIError;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/3/22
 */
public class BuildGrammarResult {

    private String outputPath = "";
    private boolean isBuildSuccess = false;
    private String ebnf = "";
    private AIError aiError;


    public BuildGrammarResult(String outputPath, boolean isBuildSuccess, String ebnf, AIError aiError) {
        this.outputPath = outputPath;
        this.isBuildSuccess = isBuildSuccess;
        this.ebnf = ebnf;
        this.aiError = aiError;
    }

    public AIError getAIError() {
        return aiError;
    }

    public String getEbnf() {
        return ebnf;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public boolean isBuildSuccess() {
        return isBuildSuccess;
    }
}
