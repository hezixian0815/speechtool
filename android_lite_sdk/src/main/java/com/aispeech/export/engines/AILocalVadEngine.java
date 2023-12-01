package com.aispeech.export.engines;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.auth.ProfileState;
import com.aispeech.export.config.AILocalVadConfig;
import com.aispeech.export.listeners.AILocalVadListener;
import com.aispeech.kernel.Vad;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.VadParams;


/**
 * 本地vad引擎
 *
 * @deprecated replaced by {@link com.aispeech.export.engines2.AILocalVadEngine}
 */
public class AILocalVadEngine {
    public static final String TAG = "AILocalVadEngine";
    private LocalVadConfig mLocalVadConfig;
    private VadParams mVadParams;
    private String mVadResPath = "";
    private String mVadRes = "";
    private com.aispeech.export.engines2.AILocalVadEngine vadEngine2;
    private AILocalVadListener mListener;

    private AILocalVadEngine() {
        mLocalVadConfig = new LocalVadConfig();
        mVadParams = new VadParams();
        vadEngine2 = com.aispeech.export.engines2.AILocalVadEngine.createInstance();
    }

    /**
     * 创建实例
     *
     * @return AICloudASREngine实例
     */
    public static AILocalVadEngine createInstance() {
        return new AILocalVadEngine();
    }


    public static boolean checkLibValid() {
        return Vad.isSoValid();
    }


    /**
     * 初始化本地vad引擎。
     *
     * @param listener 本地vad回调接口
     * @deprecated 已过时 使用带AILocalVadConfig参数的接口 {@link #init(AILocalVadConfig, AILocalVadListener)}
     */
    public void init(AILocalVadListener listener) {
        AILocalVadConfig config = new AILocalVadConfig.Builder()
                .setVadResource(TextUtils.isEmpty(mVadRes) ? mVadResPath : mVadRes)
                .setPauseTime(mVadParams.getPauseTime())
                .setUseFullMode(mLocalVadConfig.isFullMode())
                .build();
        init(config, listener);
    }

    /**
     * 初始化本地vad引擎。
     *
     * @param listener 本地vad回调接口
     * @param config   初始化配置参数实体类
     */
    public void init(final AILocalVadConfig config, AILocalVadListener listener) {
        mListener = listener;
        if (vadEngine2 != null) {
            vadEngine2.init(config, listener);
        }
    }

    /**
     * 启动本地vad引擎
     */
    public void start() {
        if (vadEngine2 != null) {
            vadEngine2.start();
        }
    }

    /**
     * 往本地vad引擎feed数据
     *
     * @param data 数据
     * @param size 数据大小
     */
    public void feedData(byte[] data, int size) {
        if (vadEngine2 != null) {
            vadEngine2.feedData(data, size);
        }
    }


    /**
     * 往本地vad引擎feed数据,支持feed双路数据
     *
     * @param dataVad vad检测数据流
     * @param dataAsr 识别数据流
     */
    public void feedData(byte[] dataVad, byte[] dataAsr) {
        if (vadEngine2 != null) {
            vadEngine2.feedData(dataVad, dataAsr);
        }
    }


    /**
     * 停止本地vad引擎
     */
    public void stop() {
        if (vadEngine2 != null) {
            vadEngine2.stop();
        }
    }


    /**
     * 销毁本地vad引擎
     */
    public void destroy() {
        if (vadEngine2 != null) {
            vadEngine2.destroy();
        }
    }


    /**
     * 设置VAD资源的绝对路径,包括文件名,
     * 需要在init之前调用
     *
     * @param vadResBinPath vadResBinPath
     * @deprecated 已过时, 使用AILocalVadConfig统一配置 {@link #init(AILocalVadConfig, AILocalVadListener)}
     */
    public void setVadResBinPath(String vadResBinPath) {
        mVadResPath = vadResBinPath;
    }


    /**
     * 设置VAD资源名字
     * 需要在init之前调用
     *
     * @param vadRes vad资源名
     * @deprecated 已过时, 使用AILocalVadConfig统一配置 {@link #init(AILocalVadConfig, AILocalVadListener)}
     */
    public void setVadResource(String vadRes) {
        this.mVadRes = vadRes;
    }


    /**
     * 设置VAD右边界
     * 需要在init之前调用
     *
     * @param pauseTime pauseTime 单位为ms,默认为300ms
     * @deprecated 已过时, 使用AILocalVadConfig统一配置 {@link #init(AILocalVadConfig, AILocalVadListener)}
     *
     */
    public void setPauseTime(int pauseTime) {
        mLocalVadConfig.setPauseTime(pauseTime);
        mVadParams.setPauseTime(pauseTime);
    }

    /**
     * 设置是否启用vad常开模式
     * 初始化参数，init之前设置生效
     *
     * @param useFullMode boolean
     * @deprecated 已过时, 使用AILocalVadConfig统一配置 {@link #init(AILocalVadConfig, AILocalVadListener)}
     */
    public void setUseFullMode(boolean useFullMode) {
        mLocalVadConfig.setFullMode(useFullMode);
    }

    protected void showErrorMessage(ProfileState state) {
        AIError error = new AIError();
        if (state == null) {
            error.setErrId(AIError.ERR_SDK_NOT_INIT);
            error.setError(AIError.ERR_DESCRIPTION_ERR_SDK_NOT_INIT);
        } else {
            error.setErrId(state.getAuthErrMsg().getId());
            error.setError(state.getAuthErrMsg().getValue());
        }
        if (mListener != null) {
            mListener.onError(error);
        }
    }

}
