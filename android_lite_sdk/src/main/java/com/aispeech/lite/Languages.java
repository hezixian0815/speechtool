package com.aispeech.lite;

/**
 * 当前支持的语种定义，需参考内核标准对齐
 */
public enum Languages {
    /**
     *
     */
    CHINESE("cn", "汉语"),
    ENGLISH("en", "英语"),
    PORTUGUESE("pt", "葡语"),
    SPANISH("es", "西班牙语"),
    GERMAN("de", "德语"),
    RUSSIAN("ru", "俄语"),
    JAPANESE("jp", "日语"),
    THAI("th", "泰语"),
    VIETNAMESE("vn", "越南语"),
    ARABIC("arb", "阿拉伯语"),
    INDIA("in","印度语"),
    FRENCH("fr","法语");


    private String language;
    private String tips;

    Languages(String language, String tips) {
        this.language = language;
        this.tips = tips;
    }

    public String getLanguage() {
        return language;
    }

    public String getTips() {
        return tips;
    }

}
