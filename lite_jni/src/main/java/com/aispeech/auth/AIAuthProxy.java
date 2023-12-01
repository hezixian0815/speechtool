package com.aispeech.auth;

import android.content.Context;

import com.aispeech.auth.config.AIAuthConfig;
import com.aispeech.common.Log;
import com.aispeech.jnihelper.AuthJniHelper;
import com.aispeech.jnihelper.DDSDnsClientJniHelper;
import com.aispeech.jnihelper.IDUIJni;

/**
 * @author wuwei
 * @date 2019-09-25 10:55
 * @email wei.wu@aispeech.com
 */
public class AIAuthProxy implements IAuth {

    private AuthImpl mAuthImpl;

    public AIAuthProxy(IDUIJni duiJni) {
        AuthJniHelper.setDuiJni(duiJni);
        DDSDnsClientJniHelper.setDuiJni(duiJni);
        mAuthImpl = new AuthImpl();
    }

    /**
     * 提供给dds调用，开启 Log
     *
     * @param logLevel {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     * @param logFilepath 日志的绝对路径，如果为空则不写入文件，只在控制台打印
     */
    public void setDebugMode(int logLevel, String logFilepath) {
        Log.setDebugMode(logLevel, logFilepath);
    }

    @Override
    public void init(Context context, AIAuthConfig config, AIAuthListener listener) {
        mAuthImpl.init(context, config, listener);
    }

    @Override
    public void doAuth() {
        mAuthImpl.doAuth();
    }

    @Override
    public void login() {
        mAuthImpl.login();
    }

    @Override
    public boolean isAuthorized() {
        return mAuthImpl.isAuthorized();
    }

    @Override
    public AIProfile getProfile() {
        return mAuthImpl.getProfile();
    }

    @Override
    public void release() {
        mAuthImpl.release();
        mAuthImpl = null;
    }


}
