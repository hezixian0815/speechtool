package com.aispeech.export.engines2;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.ISyncEngine;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.config.AILocalVadConfig;
import com.aispeech.export.intent.AILocalVadIntent;
import com.aispeech.export.listeners.AILocalVadListener;
import com.aispeech.kernel.Vad;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.base.SyncRequest;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.vad.IVadProcessorListener;
import com.aispeech.lite.vad.LocalVadProcessor;


/**
 * 本地vad引擎
 */
public class AILocalVadEngine extends BaseEngine implements ISyncEngine<AILocalVadConfig, AILocalVadListener> {

    private LocalVadConfig mLocalVadConfig;
    private VadParams mVadParams;
    private AILocalVadListener mListener;

    LocalVadProcessor mVadProcessor;
    private SyncRequest mSyncRequestDelegate = new VadSyncRequest();


    private AILocalVadEngine() {
        mLocalVadConfig = new LocalVadConfig();
        mVadParams = new VadParams();
        mVadProcessor = new LocalVadProcessor();
        mBaseProcessor = mVadProcessor;
    }

    @Override
    public String getTag() {
        return "local_vad";
    }

    /**
     * 创建实例
     *
     * @return AICloudASREngine实例
     */
    public static AILocalVadEngine createInstance() {
        return new AILocalVadEngine();
    }


    private static boolean checkLibValid() {
        return Vad.isSoValid();
    }

    public void init(final AILocalVadConfig config, AILocalVadListener listener) {
        if (config == null) {
            throw new IllegalArgumentException("AILocalVadConfig can not be null");
        }
        super.init();

        parseConfig(config);
        mListener = listener;

        mVadProcessor.init(mLocalVadConfig, new VadProcessorListenerImpl());
    }

    /**
     * 初始化本地vad引擎，本地vad资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param vadResource 本地vad资源
     * @param listener    回调接口
     */
    public void init(final String vadResource, AILocalVadListener listener) {
        init(vadResource, 300, listener);
    }

    /**
     * 初始化本地vad引擎，本地vad资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param vadResource 本地vad资源
     * @param pauseTime   设置VAD右边界，单位为ms,默认为300ms
     * @param listener    回调接口
     */
    public void init(final String vadResource, int pauseTime, AILocalVadListener listener) {
        init(vadResource, pauseTime, false, false, listener);
    }

    /**
     * 初始化本地vad引擎，本地vad资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param vadResource 本地vad资源
     * @param pauseTime   设置VAD右边界，单位为ms,默认为300ms
     * @param fullMode    全双工输出模式，一次`start`操作后能输出多次状态跳变。default is false
     * @param listener    回调接口
     */
    public void init(final String vadResource, int pauseTime, boolean fullMode, AILocalVadListener listener) {
        init(vadResource, pauseTime, fullMode, false, listener);
    }


    /**
     * 初始化本地vad引擎，本地vad资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param vadResource 本地vad资源
     * @param pauseTime   设置VAD右边界，单位为ms,默认为300ms
     * @param fullMode    全双工输出模式，一次`start`操作后能输出多次状态跳变。default is false
     * @param listener    回调接口
     */
    public void init(final String vadResource, int pauseTime, boolean fullMode, boolean isUseDoubleVad, AILocalVadListener listener) {
        if (!checkLibValid()) {
            if (listener != null) {
                listener.onInit(AIConstant.OPT_FAILED);
                listener.onError(new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }

        AILocalVadConfig vadConfig = new AILocalVadConfig();
        vadConfig.setVadResource(vadResource);
        vadConfig.setPauseTime(pauseTime);
        vadConfig.setUseFullMode(fullMode);
        vadConfig.setUseDoubleVad(isUseDoubleVad);


        init(vadConfig, listener);
    }


    private void parseConfig(AILocalVadConfig config) {
        String vadRes = config.getVadResource();
        if (TextUtils.isEmpty(vadRes)) {
            if (mListener != null) {
                mListener.onError(new AIError(AIError.ERR_RES_PREPARE_FAILED,
                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
            }
            return;
        }
        super.parseConfig(config, mLocalVadConfig);

        if (vadRes.startsWith("/")) {
            mLocalVadConfig.setResBinPath(vadRes);
        } else {
            mLocalVadConfig.setResBinPath(Util.getResPath(AISpeech.getContext(), vadRes));
            mLocalVadConfig.setAssetsResNames(new String[]{vadRes});
        }
        mLocalVadConfig.setFullMode(config.isUseFullMode());
        mLocalVadConfig.setPauseTime(config.getPauseTime());
        mLocalVadConfig.setUseDoubleVad(config.isUseDoubleVad());
        mLocalVadConfig.setPauseTimeArray(config.getPauseTimeArray());
        mLocalVadConfig.setMultiMode(config.getMultiMode());

        if (null != mVadParams) {
            mVadParams.setPauseTime(config.getPauseTime());
            mVadParams.setMultiMode(config.getMultiMode());
            if (config.getMultiMode() == 1) {
                mVadParams.setPauseTimeArray(config.getPauseTimeArray());
            }
        }
    }

    /**
     * 启动本地vad引擎
     */
    public void start() {
        super.start();
        if (mVadProcessor != null) {
            mVadProcessor.start(mVadParams);
        }
    }

    /**
     * 启动本地vad
     *
     * @param intent
     */
    public void start(AILocalVadIntent intent) {
        parseIntent(intent);
        start();
    }

    private void parseIntent(AILocalVadIntent intent) {
        if (intent == null) {
            Log.e(TAG, "AILocalVadIntent is null !");
            return;
        }
        super.parseIntent(intent, mVadParams);
        Log.e(TAG, "parseIntent: " + intent.toString());
        mVadParams.setPauseTime(intent.getPauseTime());
        mVadParams.setVadEnable(intent.isVadEnable());
    }

    /**
     * 往本地vad引擎feed数据
     *
     * @param data 数据
     * @param size 数据大小
     */
    public void feedData(byte[] data, int size) {
        Log.df(TAG, "feedData: ");
        if (mVadProcessor != null) {
            mVadProcessor.feedData(data, size);
        }
    }


    /**
     * 往本地vad引擎feed数据,支持feed双路数据
     *
     * @param dataVad vad检测数据流
     * @param dataAsr 识别数据流
     */
    public void feedData(byte[] dataVad, byte[] dataAsr) {
        Log.df(TAG, "feed double data");
        if (mVadProcessor != null) {
            mVadProcessor.feedData(dataVad, dataAsr);
        }
    }

    /**
     * 执行vadPauseTime，即动态设置vadPauseTime并生效，设置的vadPauseTime必须是init时传进去的setPauseTimeArray列表中的一个
     *
     * @param pauseTime
     */
    public void executeVadPauseTime(int pauseTime) {
        mVadParams.setPauseTime(pauseTime);
        if (mVadProcessor != null) {
            mVadProcessor.executeVadPauseTime(pauseTime);
        }
    }


    /**
     * 停止本地vad引擎
     */
    public void stop() {
        super.stop();
        if (mVadProcessor != null) {
            mVadProcessor.stop();
        }
    }


    /**
     * 销毁本地vad引擎
     */
    public void destroy() {
        super.destroy();
        if (mVadProcessor != null) {
            mVadProcessor.release();
        }
    }

    /**
     * 销毁本地引擎 -- 同步方法
     *
     * @return code 操作结果 {@link AIConstant#OPT_SUCCESS}
     */
    public int destroySync() {
        Log.i(TAG, "destroySynchronize");
        if (mVadProcessor == null) return AIConstant.OPT_FAILED;
        return mVadProcessor.releaseSynchronize();
    }

    @Override
    public int initSync(AILocalVadConfig config, long time, AILocalVadListener listener) {
        super.initSync();

        parseConfig(config);
        mListener = listener;

        return mSyncRequestDelegate.initSync(time);
    }

    private class VadSyncRequest extends SyncRequest {

        @Override
        public void doInit() {
            mVadProcessor.init(mLocalVadConfig, new VadProcessorListenerImpl());
        }
    }

    private class VadProcessorListenerImpl extends IVadProcessorListener {

        @Override
        public void onInit(int status) {
            if (isInitSync && mSyncRequestDelegate.isInitRequesting()) {
                mSyncRequestDelegate.notifyInitResult(status);
            } else if (!isInitSync && mListener != null) {
                mListener.onInit(status);
            }
        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) mListener.onError(error);
        }

        @Override
        public void onVadStart(String recordID) {
            if (mListener != null) {
                mListener.onVadStart(recordID);
            }
        }

        @Override
        public void onVadEnd(String recordID) {
            if (mListener != null) {
                mListener.onVadEnd(recordID);
            }
        }

        @Override
        public void onRmsChanged(float rmsDb) {
            if (mListener != null) {
                mListener.onRmsChanged(rmsDb);
            }
        }

        @Override
        public void onResults(String result) {
            if (mListener != null) {
                mListener.onResults(result);
            }
        }

        @Override
        public void onBufferReceived(byte[] data) {
            if (mListener != null) {
                mListener.onBufferReceived(data);
            }
        }
    }
}
