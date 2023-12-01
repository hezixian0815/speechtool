package com.aispeech.jnihelper;


public interface IDUIJni {

    /******授权*****/
    int AuthDecryptProfile(String profile, byte[] decProfile);

    boolean AuthCheckApikey(String apiKey, String secretCode);
    /******授权 END*****/

    /******DNS*****/
    String DnsDdsGetHostByName(String hostname, int udpTimeOutSec, int httpTimeOutSec);
}
