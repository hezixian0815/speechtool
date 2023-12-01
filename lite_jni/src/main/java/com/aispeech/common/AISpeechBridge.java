package com.aispeech.common;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/3/29
 */
public class AISpeechBridge {


    public static String offlineEngineAuth;
    public static boolean encryptCustomDeviceName = false;

    /**
     * 鉴权应用包名
     * 用于多应用授权
     */
    public static String authPackageName;

    /***
     * 授权应用签名
     * 用于多应用授权
     */
    public static String authSHA256;

}
