package com.aispeech.lite;

public enum AIType {

    /**
     * DM 模式
     */
    DM("dm"),
    /**
     * nlu 模式
     */
    NLU("nlu"),
    /**
     * asr 模式
     */
    ASR("asr"),

    ;

    public String value;

    AIType(String value) {
        this.value = value;
    }

}
