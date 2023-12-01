package com.aispeech.export.config;

public class AILocalSevcConfig {

    private String sspeBinResource;

    public String getSspeBinResource() {
        return sspeBinResource;
    }

    public void setSspeBinResource(String sspeBinResource) {
        this.sspeBinResource = sspeBinResource;
    }

    @Override
    public String toString() {
        return "AILocalSevcConfig{" +
                "sspeBinResource='" + sspeBinResource + '\'' +
                '}';
    }
}
