package com.aispeech.auth;

import android.content.Context;

import com.aispeech.auth.config.AIAuthConfig;

/**
 * @author wuwei
 * @date 2019-09-27 09:17
 * @email wei.wu@aispeech.com
 */
public interface IAuth {
    void init(Context context, AIAuthConfig config, AIAuthListener listener);
    void doAuth();
    boolean isAuthorized();
    AIProfile getProfile();
    void release();
    void login();
}
