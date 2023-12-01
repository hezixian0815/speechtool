package com.aispeech.export.engines2;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.auth.AIAuthEngine;
import com.aispeech.auth.AIProfile;
import com.aispeech.auth.ProfileState;
import com.aispeech.base.BaseInnerEngine;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.ThreadNameUtil;
import com.aispeech.common.Util;
import com.aispeech.export.listeners.AILocalNRListener;
import com.aispeech.kernel.NR;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.LocalNRConfig;
import com.aispeech.lite.nr.NRKernel;
import com.aispeech.lite.param.SpeechParams;

import java.io.File;

/**
 * 本地降噪
 */
public class AILocalNREngine extends BaseInnerEngine {

    public static final String TAG = "AILocalNREngine";
    private NRKernel mNrKernel;
    private LocalNRConfig mLocalNrConfig;

    private AILocalNRListener mListener;

    private AIProfile mProfile;
    private ProfileState mProfileState;


    private AILocalNREngine() {
        super();
        mLocalNrConfig = new LocalNRConfig();
    }

    /**
     * 创建实例
     *
     * @return AICloudASREngine实例
     */
    public static AILocalNREngine createInstance() {
        return new AILocalNREngine();
    }


    /**
     * 检查so库是否加载成功
     *
     * @return true 加载成功，false 加载失败
     */
    private static boolean checkLibValid() {
        return NR.isNrSoValid();
    }

    /**
     * 初始化引擎,需设置 NR 资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param nrResource NR资源
     * @param listener   回调接口
     */
    public void init(final String nrResource, AILocalNRListener listener) {
        if (!checkLibValid()) {
            if (listener != null) {
                listener.onInit(AIConstant.OPT_FAILED);
                listener.onError(new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }
        mListener = listener;
        super.init(mListener);
        mProfile = AIAuthEngine.getInstance().getProfile();
        mProfileState = mProfile.isProfileValid(Scope.LOCAL_NR);
        Log.d(TAG, "authstate: " + mProfileState.toString());
        if (mProfileState.isValid()) {
            mNrKernel = new NRKernel(mListener);
            new Thread(ThreadNameUtil.getFixedThreadName(TAG)) {
                public void run() {
                    // nrResource
                    if (TextUtils.isEmpty(nrResource)) {
                        Log.e(TAG, "nrResource not found !!");
                        if (mListener != null) {
                            mListener.onError(new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                        }
                        return;
                    } else if (nrResource.startsWith("/")) {
                        mLocalNrConfig.setResBinPath(nrResource);
                    } else {
                        int ret = Util.copyResource(AISpeech.getContext(), nrResource);
                        if (ret == -1) {//拷贝失败
                            if (mListener != null) {
                                mListener.onInit(AIConstant.OPT_FAILED);
                                mListener.onError(new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                            }
                            return;
                        }
                        mLocalNrConfig.setResBinPath(Util.getResourceDir(mLocalNrConfig.getContext()) + File.separator + nrResource);
                    }

                    mNrKernel.newKernel(mLocalNrConfig);
                }
            }.start();
        } else {
            showErrorMessage(mProfileState);
        }
    }

    private boolean updateTrails(String scope) {
        if (mProfileState.getAuthType() == ProfileState.AUTH_TYPE.TRIAL
                && mProfileState.getTimesLimit() != -1) {
            ProfileState state = AIAuthEngine.getInstance().getProfile().isProfileValid(scope);
            if (!state.isValid()) {
                showErrorMessage(state);
                return false;
            } else {
                mProfile.updateUsedTimes(scope);
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * 启动本地nr引擎
     */
    public void start() {
        if (mProfileState != null && mProfileState.isValid()) {
            if (!updateTrails(Scope.LOCAL_NR)) {
                return;
            }
            if (mNrKernel != null) {
                mNrKernel.startKernel(new SpeechParams());
            }
        } else {
            showErrorMessage(mProfileState);
        }
    }

    /**
     * 向引擎feed数据
     *
     * @param data 数据
     * @param size 数据大小
     */
    public void feedData(byte[] data, int size) {
        if (mProfileState != null && mProfileState.isValid()) {
            if (mNrKernel != null) {
                byte[] bufferData = new byte[size];
                System.arraycopy(data, 0, bufferData, 0, size);
                mNrKernel.feed(bufferData);
            }
        } else {
            showErrorMessage(mProfileState);
        }
    }


    /**
     * 停止引擎
     */
    public void stop() {
        if (mProfileState != null && mProfileState.isValid()) {
            if (mNrKernel != null) {
                mNrKernel.stopKernel();
            }
        } else {
            showErrorMessage(mProfileState);
        }
    }


    /**
     * 销毁引擎
     */
    public void destroy() {
        if (mProfileState != null && mProfileState.isValid()) {
            if (mNrKernel != null) {
                mNrKernel.releaseKernel();
            }
            mListener = null;
        } else {
            showErrorMessage(mProfileState);
        }
    }


    @Override
    protected void callbackInMainLooper(CallbackMsg callback, Object obj) {

    }
}
