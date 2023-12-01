package com.aispeech.net;

public class NetConfig {
    private static boolean useSpeechDns = true;

    public static boolean isUseSpeechDns() {
        return useSpeechDns;
    }

    /**
     * 设置是否使用思必驰提供的DNS解析库
     * @param useSpeechDns 是否使用思必驰的DNS解析库，默认为true
     */
    public static void setUseSpeechDns(boolean useSpeechDns) {
        NetConfig.useSpeechDns = useSpeechDns;
    }
}
