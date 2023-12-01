package com.aispeech.lite.config;
/**
 * Created by wuwei on 18-5-14.
 */

public class LocalLAsrConfig extends AIEngineConfig {
    private String resourcePath;

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public LocalLAsrConfig clone() throws CloneNotSupportedException {
        return (LocalLAsrConfig) super.clone();
    }

    @Override
    public String toString() {
        return "LocalLAsrConfig{" +
                "resourcePath='" + resourcePath + '\'' +
                '}';
    }
}
