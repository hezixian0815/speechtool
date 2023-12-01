package com.aispeech.export;

/**
 * 识别引擎模式
 */
public enum ASRMode {
    /**
     * 识别模式，抛出 1|2 路结果(视具体初始化配置的识别资源)
     */
    MODE_ASR(1),
    /**
     * 热词模式，热词 1 路结果
     */
    MODE_HOTWORD(2),
    /**
     * 增强模式，识别+热词 3 路结果（资源必须为 ngram+grammar 的资源，并开启 use_dymc = 1 ）
     */
    MODE_ASR_X(3);

    private final int mode;

    ASRMode(int mode) {
        this.mode = mode;
    }

    public int getValue() {
        return mode;
    }
}
