package com.aispeech.lite;

/**
 * 云端引擎资源类型
 */
public enum ResourceType {

    /**
     * 默认通用识别模型
     */
    COMM("comm"),
    /**
     * 车载识别模型
     */
    AICAR("aicar"),

    /**
     * 智能家居识别模型
     */
    AIHOME("aihome"),

    /**
     * 机器人识别模型
     */
    AIROBOT("airobot"),
    /**
     * 其他定制识别模型
     */
    CUSTOM("custom");

    public String value;

    ResourceType(String value) {
        this.value = value;
    }

}
