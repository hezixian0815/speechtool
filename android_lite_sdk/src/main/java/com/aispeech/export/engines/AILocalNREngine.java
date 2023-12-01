package com.aispeech.export.engines;

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

/**
 * 本地降噪
 *
 * @deprecated replaced by {@link com.aispeech.export.engines2.AILocalNREngine}
 */
@Deprecated
public class AILocalNREngine extends BaseInnerEngine {

    public static final String TAG = "AILocalNREngine";
    private NRKernel mNrKernel;
    private LocalNRConfig mLocalNrConfig;

    private String mNrResPath = "";
    private String mNrRes = "";
    private AILocalNRListener mListener;

    private AIProfile mProfile;
    private ProfileState mProfileState;


    private AILocalNREngine() {
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
    public static boolean checkLibValid() {
        return NR.isNrSoValid();
    }


    /**
     * 初始化本地引擎。
     *
     * @param listener 回调接口
     */
    public void init(AILocalNRListener listener) {
        mListener = listener;
        mProfile = AIAuthEngine.getInstance().getProfile();
        mProfileState = mProfile.isProfileValid(Scope.LOCAL_NR);
        Log.d(TAG, "authstate: " + mProfileState.toString());
        super.init(mListener);
        if (mProfileState.isValid()) {
            mNrKernel = new NRKernel(mListener);
            new Thread(ThreadNameUtil.getFixedThreadName(TAG)) {
                public void run() {
                    if (TextUtils.isEmpty(mNrResPath) && !TextUtils.isEmpty(mNrRes)) {
                        int ret = Util.copyResource(AISpeech.getContext(), mNrRes);
                        if (ret == -1) {//拷贝失败
                            if (mListener != null) {
                                mListener.onInit(AIConstant.OPT_FAILED);
                                mListener.onError(new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                            }
                            return;
                        }
                        mLocalNrConfig.setResBinPath(Util.getResPath(AISpeech.getContext(), mNrRes));
                    } else if (!TextUtils.isEmpty(mNrResPath)) {
                        mLocalNrConfig.setResBinPath(mNrResPath);
                    } else {
                        if (mListener != null) {
                            mListener.onError(new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                        }
                    }
                    mNrKernel.newKernel(mLocalNrConfig);
                }
            }.start();
        } else {
            showErrorMessage(mProfileState);
        }
    }

    /**
     * 启动本地nr引擎
     */
    public void start() {
        if (mProfileState != null && mProfileState.isValid()) {
            if (!updateTrails(mProfileState, Scope.LOCAL_NR)) {
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


    /**
     * 设置NR资源的绝对路径,包括文件名,
     * 需要在init之前调用
     *
     * @param nrResBinPath NR资源的绝对路径
     */
    public void setNrResBinPath(String nrResBinPath) {
        mNrResPath = nrResBinPath;
    }


    /**
     * 设置NR资源名字
     * 需要在init之前调用
     *
     * @param nrRes nr资源名
     */
    public void setNrResource(String nrRes) {
        this.mNrRes = nrRes;
    }


    @Override
    protected void callbackInMainLooper(CallbackMsg callback, Object obj) {

    }
}
