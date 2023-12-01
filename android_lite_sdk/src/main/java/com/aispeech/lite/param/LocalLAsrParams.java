package com.aispeech.lite.param;

/**
 * Created by wuwei on 18-5-14.
 */

public class LocalLAsrParams extends SpeechParams {
    private boolean useCustomFeed = false;
    private String lAsrParamJsonString;

    public String getLAsrParamJsonString() {
        return this.lAsrParamJsonString;
    }

    public void setLAsrParamJsonString(String var1) {
        this.lAsrParamJsonString = var1;
    }

    public boolean isUseCustomFeed() {
        return this.useCustomFeed;
    }

    public void setUseCustomFeed(boolean var1) {
        this.useCustomFeed = var1;
    }

}
