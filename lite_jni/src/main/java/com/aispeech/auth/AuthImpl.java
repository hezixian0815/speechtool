package com.aispeech.auth;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import com.aispeech.auth.config.AIAuthConfig;
import com.aispeech.common.AuthError;
import com.aispeech.common.AuthUtil;
import com.aispeech.common.Log;
import com.aispeech.common.ThreadNameUtil;
import com.aispeech.BuildConfig;
import com.aispeech.net.NetProxy;
import com.aispeech.net.http.HttpCallback;
import com.aispeech.net.http.IHttp;
import com.aispeech.net.http.IResponse;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author wuwei
 * @date 2019-09-26 11:07
 * @email wei.wu@aispeech.com
 */
public class AuthImpl implements IAuth {
    private static final String TAG = "AuthProxy";
    //老/新授权方式
    private static final String AUTH_KEY_TYPE_APIKEY = "apikey";
    private static final String AUTH_KEY_TYPE_PRODUCTKEY = "productKey";
    private static final String MODE_VERIFY = "VERIFY";
    private static final String MODE_REGISTER = "REGISTER";
    private static final String MODE_LOGIN = "LOGIN";
    private static final int MSG_AUTH_TIMEOUT = 1;
    private String mAuthMode;
    private AIAuthListener mOutListener;
    private AIAuthConfig mConfig;
    private Context mContext;
    private AIProfile mAIProfile;
    private ProfileState mProfileState;
    private String mAuthKeyType;
    private String mAuthKey;
    private String mAuthCode;
    private int loginRetryTimes = 0;
    //timeout
    private HandlerThread authTimeoutThread = null;
    private Handler authTimeoutHandler = null;
    private volatile boolean mIsTimeout = false;
    private static final String TYPE_TEMP = ".temp";


    @Override
    public void init(Context context, AIAuthConfig config, AIAuthListener listener) {
        this.mContext = context;
        this.mConfig = config;
        this.mOutListener = listener;
        this.mAIProfile = new AIProfile(mContext, mConfig);
        this.mProfileState = mAIProfile.isProfileValid(true, null);
    }

    /**
     * {@code 如果不需要检查传入的 productId apiKey,可以在checkParam();之前使用下面代码进行屏蔽
     * if (AIProfile.NO_NEED_AUTH) {
     * mProfileState = mAIProfile.isProfileValid(true, null);
     * handleRetProfileState(mProfileState);
     * return;
     * }}
     */
    @Override
    public void doAuth() {
        checkParam();

        if (TextUtils.isEmpty(getProfile().getDeviceId())) {
            Log.e(TAG, "auth error ,customDeviceName must not be empty");
            sendAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_PROFILE_NO_MATCH_DEVICE);
            return;
        }

        /**
         * 1.check apiKey locally
         * 2.check profile valid
         * 3.ok : login
         * 4.err : verify; register; login
         *
         */
        String secretCode = AuthUtil.getSecretCode(mContext, mConfig.getSharePkgName(), mConfig.getShareSHA256());
        if (TextUtils.isEmpty(mConfig.getProductKey())) {
            Log.d(TAG, "old auth type");
            mAuthKeyType = AUTH_KEY_TYPE_APIKEY;
            mAuthKey = mConfig.getApiKey();
            mAuthCode = secretCode;
        } else {
            Log.d(TAG, "new auth type");
            mAuthKeyType = AUTH_KEY_TYPE_PRODUCTKEY;
            mAuthKey = mConfig.getProductKey();
            mAuthCode = mConfig.getProductSecret();
        }

        if (BuildConfig.NO_AUTH) {
            Log.d(TAG, "no need check apiKey");
            mProfileState = mAIProfile.isProfileValid(true, null);
            handleRetProfileState(mProfileState);
        } else {
            if (mAIProfile.checkApiKey(mConfig.getApiKey(), secretCode)) {
                Log.d(TAG, "apiKey is ok locally" + mAIProfile);
                mProfileState = mAIProfile.isProfileValid(true, null);
                handleRetProfileState(mProfileState);
            } else {
                sendAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_API_KEY_INVALID);
            }
        }
    }

    /**
     * 处理授权文件状态
     *
     * @param profileState
     */
    private void handleRetProfileState(ProfileState profileState) {
        Log.i(TAG, "handleRetProfileState  state.isValid()" + profileState.isValid());
        if (profileState.isValid()) {
            switch (profileState.getAuthType()) {
                case TRIAL:
                    //TRIAL auth
                    Log.d(TAG, "handleRetProfileState  TRIAL" + mConfig.isNeedReplaceProfile());
                    if (mConfig.isNeedReplaceProfile()) {
                        //try update trial to online profile
                        mAIProfile.copyTrailProfile();
                        loginRetryTimes = 0;
                        _do_auth();
                    } else {
                        sendAuthSuccess();
                    }
                    break;
                case OFFLINE:
                    //OFFLINE auth
                    Log.d(TAG, "handleRetProfileState  OFFLINE" + mConfig.isNeedReplaceProfile());
                    sendAuthSuccess();
                    break;
                default:
                    //ONLINE auth
                    Log.d(TAG, "mConfig.isNeedReplaceProfile() ONLINE" + mConfig.isNeedReplaceProfile());
                    if (mConfig.isNeedReplaceProfile() && !BuildConfig.NO_AUTH) {
                        File profile = new File(mAIProfile.getProfilePath());
                        if (!profile.exists()) {
                            Log.e(TAG, "profile no exist");
                            _do_auth();
                        } else {
                            login();
                        }
                    } else {
                        sendAuthSuccess();
                    }
                    break;
            }
        } else {
            //授权只要失败，都会新发起授权请求
            Log.d(TAG, "delete useless profile ret = " + mAIProfile.deleteProfile());
            loginRetryTimes = 0;
            _do_auth();
        }
    }

    private void checkParam() {
        if (mConfig == null) {
            Log.e(TAG, "AIAuthConfig is null, please check");
            throw new IllegalArgumentException("DUI SDK init AIAuthConfig == null");
        }
        if (mContext == null) {
            Log.e(TAG, "context is null, please check");
            throw new IllegalArgumentException("DUI SDK init Context == null");
        }
        if (mOutListener == null) {
            Log.e(TAG, "AuthListener is null, please check");
            throw new IllegalArgumentException("DUI SDK init AIAuthListener == null");
        }
        if (TextUtils.isEmpty(mConfig.getProductId())) {
            throw new IllegalArgumentException("authConfig is invalid, lost productId");
        }
        if (TextUtils.isEmpty(mConfig.getApiKey())) {
            throw new IllegalArgumentException("authConfig is invalid, lost apiKey");
        }
    }

    private void sendAuthState(ProfileState state) {
        if (state.isValid()) {
            sendAuthSuccess();
        } else {
            sendAuthErrMsg(state.getAuthErrMsg());
        }
    }

    private void sendAuthErrMsg(AuthError.AUTH_ERR_MSG errMsg) {
        if (mOutListener != null) {
            mOutListener.onFailure(errMsg);
        }
    }

    private void sendAuthSuccess() {
        Log.write(TAG, "auth success");
        if (mConfig != null) {
            Log.write(TAG, "auth success , customDeviceName:" + mConfig.getCustomDeviceName() + ",customDeviceId:" + mConfig.getCustomDeviceId());
        }
        if (mAIProfile != null) {
            Log.write(TAG, "mAIProfile,getDeviceId:" + mAIProfile.getDeviceId());
            Log.write(TAG, "mAIProfile,getDeviceName:" + mAIProfile.getDeviceName());
        }
        if (mOutListener != null) {
            mOutListener.onSuccess();
        }
    }

    public void _do_auth() {

        if (TextUtils.isEmpty(mConfig.getProductKey())) {
            Log.d(TAG, "go to old auth");
            handleAuth();
        } else {
            handleAuthNew();
        }
    }

    private void handleAuth() {
        //register + login
        register();
    }

    private void handleAuthNew() {
        //verify + register + login
        verify();
    }

    @Override
    public boolean isAuthorized() {
        return mAIProfile != null && mAIProfile.isProfileValid(false, null).isValid();
    }

    /**
     * 授权verify
     */
    private void verify() {
        startAuthTimeoutTask();
        mAuthMode = MODE_VERIFY;
        NetProxy.getHttp().get(getVerifyUrl(), new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                cancelAuthTimeoutTask();
                if (mProfileState.getAuthType() != ProfileState.AUTH_TYPE.TRIAL) {//非试用授权
                    Log.d(TAG, " verify onFailure ");
                    if (TextUtils.isEmpty(getOfflineEngine())) {
                        handleException(e, false);
                    } else {
                        sendAuthState(mAIProfile.isProfileValid(null));
                    }

                } else {
                    Log.d(TAG, "onFailure TRIAL");
                    //回退试用授权文件
                    mAIProfile.revertTrailProfile(TYPE_TEMP);
                    mConfig.setNeedReplaceProfile(false);
                    sendAuthState(mAIProfile.isProfileValid(true, null));
                }
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse response) throws IOException {
                cancelAuthTimeoutTask();
                if (mProfileState.getAuthType() != ProfileState.AUTH_TYPE.TRIAL) {//非试用授权
                    handleResponse(response, MODE_VERIFY);
                } else {
                    String s = response.string();
                    if (response.code() == 200 && !TextUtils.isEmpty(s)) {
                        Log.d(TAG, "verify response->" + s);
                        Log.d(TAG, "first register url: " + getRegisterUrl());
                        register();
                    } else {
                        mAIProfile.revertTrailProfile(TYPE_TEMP);//回退试用授权文件
                        mConfig.setNeedReplaceProfile(false);
                        sendAuthState(mAIProfile.isProfileValid(true, null));
                    }
                }
            }
        });
    }

    /**
     * 授权register
     */
    private void register() {
        startAuthTimeoutTask();
        mAuthMode = MODE_REGISTER;
        Map<String, Object> map = mConfig.getDeviceInfoMap();
        if (!TextUtils.isEmpty(mConfig.getCustomDeviceId())) {
            map.put("deviceId", AuthUtil.getDeviceId(mContext,mConfig.getCustomDeviceId()));
        }
        if (!TextUtils.isEmpty(mConfig.getCustomDeviceName())) {
            map.put("deviceName", AuthUtil.getDeviceId(mContext,mConfig.getCustomDeviceName()));
        }
        if (!TextUtils.isEmpty(mConfig.getDeviceNameType())) {
            map.put("deviceNameType", mConfig.getDeviceNameType());
        }
        //licence id 预授权方案
        if (!TextUtils.isEmpty(mConfig.getLicenseId())) {
            map.put("licenceId", mConfig.getLicenseId());
        }
        if (!TextUtils.isEmpty(mConfig.getBuildModel())) {
            map.put("buildModel", mConfig.getBuildModel());
        }
        String body = AuthUtil.getDeviceData(mContext, mConfig.getDeviceInfoMap(), mConfig.getCustomDeviceName());
        Log.d(TAG, "register body: " + body);
        NetProxy.getHttp().post(getRegisterUrl(), body, new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                cancelAuthTimeoutTask();
                if (mProfileState.getAuthType() != ProfileState.AUTH_TYPE.TRIAL) {//非试用授权
                    if (TextUtils.isEmpty(getOfflineEngine())) {
                        handleException(e, false);
                    } else {
                        sendAuthState(mAIProfile.isProfileValid(null));
                    }
                } else {
                    //回退试用授权文件
                    mAIProfile.revertTrailProfile(TYPE_TEMP);
                    mConfig.setNeedReplaceProfile(false);
                    sendAuthState(mAIProfile.isProfileValid(true, null));
                }
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse response) throws IOException {
                cancelAuthTimeoutTask();
                Log.d(TAG, "register response code " + response.code());
                if (response.code() != 200 && mProfileState.getAuthType() == ProfileState.AUTH_TYPE.TRIAL) {
                    mAIProfile.revertTrailProfile(TYPE_TEMP);//回退试用授权文件
                    mConfig.setNeedReplaceProfile(false);
                    sendAuthState(mAIProfile.isProfileValid(true, null));
                } else {
                    handleResponse(response, MODE_REGISTER);
                }
            }
        });
    }

    /**
     * 授权login
     */
    @Override
    public void login() {
        startAuthTimeoutTask();
        mAuthMode = MODE_LOGIN;
        if (mConfig.isIgnoreLogin()) {
            sendAuthSuccess();
        }
        NetProxy.getHttp().post(getLoginUrl(), "", new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                cancelAuthTimeoutTask();
                Log.e(TAG, "login onFailure: " + e.getMessage());
                if (!mConfig.isIgnoreLogin()) {
                    Log.e(TAG, "sendAuthSuccess");
                    sendAuthSuccess();
                }
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse response) throws IOException {
                cancelAuthTimeoutTask();
                Log.d(TAG, "login response code is " + response.code());
                //new feature of login
                if (mProfileState.getAuthType() != ProfileState.AUTH_TYPE.TRIAL &&
                        response.code() == 401) {
                    String errContent = response.string();
                    Log.e(TAG, "login failed, errMsg is " + errContent);
                    String detailErrId = null;
                    try {
                        JSONObject errJson = new JSONObject(errContent);
                        detailErrId = errJson.optString("detailErrId");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    Log.e(TAG, "login failed, errMsg is " + errContent);
                    Log.w(TAG, "current profile is invalid, need to update profile");
                    Log.write(TAG, "delete useless profile, ret = " + mAIProfile.deleteProfile());

                    if (loginRetryTimes < 1) {
                        loginRetryTimes++;
                        _do_auth();
                    } else {
                        sendAuthErrMsg(AuthError.AUTH_ERR_MSG.parseServerErrorId(detailErrId));
                    }
                    return;
                }
                if (!mConfig.isIgnoreLogin()) {
                    sendAuthSuccess();
                } else {
                    Log.w(TAG, "mAuthMode is error");
                }
                handleLoginResponse(response);
            }
        });
    }

    private void handleLoginResponse(IResponse response) {
        if (response.code() == 200) {
            final String content = response.string();
            if ("{}".equals(content)) {
                Log.d(TAG, "local profile is right.");
            } else {
                if (!TextUtils.isEmpty(content)) {
                    Log.d(TAG, "local profile have update->" + content);
                    if (!mAIProfile.writeProfile(content)) {
                        sendAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_PROFILE_SAVE);
                    }
                }
            }
        }
    }

    @Override
    public AIProfile getProfile() {
        return mAIProfile;
    }

    @Override
    public void release() {
        if (mConfig != null) {
            mConfig = null;
        }
        if (mAIProfile != null) {
            mAIProfile = null;
        }
        if (mOutListener != null) {
            mOutListener = null;
        }
    }

    private void handleResponse(IResponse response, String mode) {
        Log.d(TAG, mode + " response code is " + response.code());
        switch (response.code()) {
            case 200:
                String s = response.string();
                if (!TextUtils.isEmpty(s)) {
                    Log.d(TAG, "response->" + s);
                    handleResponseByMode(mode, s);
                } else {
                    sendAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_API_KEY_INVALID);
                }
                break;
            case 401:
                String err401 = response.string();
                Log.d(TAG, "err401: " + err401);
                String detailErrId = null;
                try {
                    detailErrId = new JSONObject(err401).getString("detailErrId");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                AuthError.AUTH_ERR_MSG authErrMsg = AuthError.AUTH_ERR_MSG.parseServerErrorId(detailErrId);
                if (authErrMsg == AuthError.AUTH_ERR_MSG.ERR_SERVER_070635) { //licence id used remove licence id register again
                    //reset licence Id
                    mConfig.setLicenseId(null);
                    mConfig.getDeviceInfoMap().remove("licenceId");
                    register();
                } else {
                    sendAuthErrMsg(authErrMsg);
                }
                break;
            case 500:
                sendAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_SHA256_INVALID);
                break;
            default:
                sendAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_NET_CONNECT);
                break;
        }
    }

    private void handleResponseByMode(String mode, String s) {
        switch (mode) {
            case MODE_VERIFY:
                Log.d(TAG, "first register url: " + getRegisterUrl());
                register();
                break;
            case MODE_REGISTER:
                if (mAIProfile.writeProfile(s)) {
                    ProfileState state = mAIProfile.isProfileValid(true, null);
                    if (state.isValid()) {
                        login();
                    } else {
                        sendAuthState(state);
                    }
                } else {
                    sendAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_PROFILE_SAVE);
                }
                break;
            case MODE_LOGIN:
                break;
            default:
                break;
        }
    }

    private void handleException(Exception e, boolean ignoreErr) {
        if (ignoreErr) {
            return;
        }
        String errorInfo = e.getMessage();
        Log.d(TAG, "onFailure: " + errorInfo);
        if (TextUtils.isEmpty(errorInfo)) {
            Log.d(TAG, "onFailure: " + e);
            sendAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_NET_CONNECT);
            return;
        }
        if (mIsTimeout && (errorInfo.contains("Canceled") ||
                errorInfo.contains("closed"))) {
            return;
        }
        if (errorInfo.toLowerCase().contains("certi".toLowerCase()) ||
                errorInfo.toLowerCase().contains("Chain validation".toLowerCase())) {//证书失效
            sendAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_CERTIFICATION_INVALID);
        } else {//网络错误
            sendAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_NET_CONNECT);
        }
    }

    private synchronized void startAuthTimeoutTask() {
        Log.d(TAG, "startMaxSpeechTimerTask");
        if (authTimeoutHandler != null) {
            authTimeoutHandler.removeMessages(MSG_AUTH_TIMEOUT);
        }
        if (mConfig.getAuthTimeout() > 0) {
            mIsTimeout = false;
            initAuthTimeoutHandler();
            if (authTimeoutHandler != null) {
                Log.d(TAG, "auth-connect-timeout: " + mConfig.getAuthTimeout() + "ms");
                authTimeoutHandler.sendEmptyMessageDelayed(MSG_AUTH_TIMEOUT, mConfig.getAuthTimeout());
            }
        }
    }

    private synchronized void initAuthTimeoutHandler() {
        authTimeoutThread = new HandlerThread(ThreadNameUtil.getSimpleThreadName("AuthTimeout"), Thread.MAX_PRIORITY);
        authTimeoutThread.start();
        authTimeoutHandler = new Handler(authTimeoutThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MSG_AUTH_TIMEOUT) {
                    dealAuthTimeout();
                }
            }
        };
    }

    private synchronized void cancelAuthTimeoutTask() {
        Log.d(TAG, "cancelTimeoutTimerTask");
        if (authTimeoutHandler != null) {
            authTimeoutHandler.removeMessages(MSG_AUTH_TIMEOUT);
            authTimeoutHandler = null;
        }
        if (authTimeoutThread != null) {
            authTimeoutThread.quit();
            authTimeoutThread = null;
        }
    }

    private synchronized void dealAuthTimeout() {
        Log.d(TAG, "network connect timeout before");
        mIsTimeout = true;
        // 抛超时错误
        NetProxy.getHttp().cancel();
        Log.d(TAG, "auth mode is " + mAuthMode);
        if (mProfileState.getAuthType() != ProfileState.AUTH_TYPE.TRIAL) {
            //只有register/verify的超时才抛错误
            if ((TextUtils.equals(mAuthMode, MODE_REGISTER) ||
                    TextUtils.equals(mAuthMode, MODE_VERIFY))) {

                if (TextUtils.isEmpty(getOfflineEngine())) {
                    sendAuthErrMsg(AuthError.AUTH_ERR_MSG.ERR_NET_TIMEOUT);
                } else {
                    sendAuthSuccess();
                }
                Log.d(TAG, mAuthMode + " timeout, invoke timeout err");
            } else if (TextUtils.equals(mAuthMode, MODE_LOGIN)) {
                Log.d(TAG, "login timeout, invoke success");
                sendAuthSuccess();
            } else {
                Log.w(TAG, "mAuthMode is error");
            }
        } else {
            mAIProfile.revertTrailProfile(TYPE_TEMP);//回退试用授权文件
            mConfig.setNeedReplaceProfile(false);
            sendAuthState(mAIProfile.isProfileValid(null));
        }
        Log.d(TAG, "network connect timeout after");
    }


    private String getVerifyUrl() {
        String url = mConfig.getAuthServer() + mConfig.getVerifyPath();
        HashMap<String, Object> map = new LinkedHashMap<>();
        map.put(AUTH_KEY_TYPE_APIKEY, mConfig.getApiKey());
        map.put("package", AuthUtil.getAuthPackageName(mContext));
        map.put("signatuerSha256", AuthUtil.getAuthKeyHash(mContext));
        map.put("buildVariant", AuthUtil.getBuildVariant(mContext));
        return AuthUtil.appendUrl(url, map);
    }

    private String getRegisterUrl() {
        String url = mConfig.getAuthServer() + mConfig.getRegisterPath();
        HashMap<String, Object> map = new LinkedHashMap<>();
        map.put(mAuthKeyType, mAuthKey);
        map.put("productId", mConfig.getProductId());
        String timeStamp = System.currentTimeMillis() + "";
        String nonce = UUID.randomUUID() + "";
        map.put("timestamp", timeStamp);
        map.put("nonce", nonce);
        Log.d(TAG, "getRegisterUrl() mAuthCode " + mAuthCode);
        String sig = AuthUtil.getSignature(mAuthKey + nonce + mConfig.getProductId() + timeStamp, mAuthCode);
        map.put("sig", sig);

        return AuthUtil.appendUrl(url, map);
    }

    private String getLoginUrl() {
        String url = mConfig.getAuthServer() + mConfig.getLoginPath();
        HashMap<String, Object> map = new LinkedHashMap<>();
        map.put("productId", mConfig.getProductId());
        map.put("deviceName", mAIProfile.getDeviceName());
        String timeStamp = System.currentTimeMillis() + "";
        String nonce = UUID.randomUUID() + "";
        map.put("timestamp", timeStamp);
        map.put("nonce", nonce);
        String deviceSecret = mAIProfile.getDeviceSecret();
        Log.d(TAG, "getLoginUrl() deviceSecret " + deviceSecret);
        String sig = AuthUtil.getSignature(mAIProfile.getDeviceName() + nonce + mConfig.getProductId() + timeStamp, deviceSecret);
        map.put("sig", sig);
        return AuthUtil.appendUrl(url, map);
    }

    private String getOfflineEngine() {
        String result = "";

        try {
            Class<?> aClass = Class.forName("com.aispeech.lite.AISpeech");
            Field offlineEngineAuth = aClass.getField("offlineEngineAuth");
            result = (String) offlineEngineAuth.get(null);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
