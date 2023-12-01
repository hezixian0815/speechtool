package com.aispeech.auth;

import android.content.Context;

import com.aispeech.DUILiteSDK;
import com.aispeech.auth.config.AIAuthConfig;
import com.aispeech.common.AuthError;

/**
 * @author wuwei
 * @decription TODO
 * @date 2019-09-30 11:26
 * @email wei.wu@aispeech.com
 */
public class AIAuthEngine {
    private static volatile AIAuthEngine mInstance = null;
    private AIAuthProxy mAIAuthProxy;
    private AIAuthListenerImpl mAIAuthListenerImpl;
    private DUILiteSDK.InitListener mOutListener;

    private AIAuthEngine() {
        mAIAuthProxy = new AIAuthProxy(null);
        mAIAuthListenerImpl = new AIAuthListenerImpl();
    }

    public static AIAuthEngine getInstance() {
        if (mInstance == null) {
            synchronized (AIAuthEngine.class) {
                if (mInstance == null) {
                    mInstance = new AIAuthEngine();
                }
            }
        }
        return mInstance;
    }

    public void init(Context context, AIAuthConfig config, DUILiteSDK.InitListener listener) {
        this.mOutListener = listener;
        this.mAIAuthProxy.init(context, config, mAIAuthListenerImpl);
    }

    public void doAuth() {
        this.mAIAuthProxy.doAuth();
    }

    public boolean isAuthorized() {
        return this.mAIAuthProxy.isAuthorized();
    }

    public AIProfile getProfile() {
        AIProfile mAIProfile = mAIAuthProxy.getProfile();
        if (mAIProfile == null) {
            throw new IllegalArgumentException("SDK Should Auth First !!!");
        } else
            return mAIProfile;
    }


    private class AIAuthListenerImpl implements AIAuthListener {

        @Override
        public void onFailure(AuthError.AUTH_ERR_MSG authErrMsg) {
            if (mOutListener != null) {
                mOutListener.error(authErrMsg.getId(), authErrMsg.getValue());
            }
        }

        @Override
        public void onSuccess() {
            if (mOutListener != null) {
                mOutListener.success();
            }
        }
    }


}
