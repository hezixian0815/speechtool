package com.aispeech.export.engines2;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.JNIFlag;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.config.AILocalSevcConfig;
import com.aispeech.export.intent.AILocalSevcIntent;
import com.aispeech.export.listeners.AILocalSevcListener;
import com.aispeech.kernel.Sspe;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalSevcConfig;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.sevc.SevcProcessor;
import com.aispeech.lite.speech.SpeechListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AILocalSevcEngine extends BaseEngine {

    private final LocalSevcConfig innerConfig;
    private final SpeechParams innerParam;
    private final SpeechListenerImpl innerSpeechListener;
    private SevcProcessor innerProcessor;

    private AILocalSevcEngine() {
        innerConfig = new LocalSevcConfig();
        innerParam = new SpeechParams();
        innerSpeechListener = new SpeechListenerImpl();
    }

    @Override
    public String getTag() {
        return "local_sevc";
    }


    public static AILocalSevcEngine createInstance() { //懒汉式单例
        return new AILocalSevcEngine();
    }

    private boolean checkLibValid() {
        return Sspe.isSoValid();
    }

    public synchronized void init(AILocalSevcConfig config, AILocalSevcListener listener) {
        super.init();
        JNIFlag.isLoadCarSspe = false;
        if (!checkLibValid()) {
            if (listener != null) {
                listener.onInit(AIConstant.OPT_FAILED);
                listener.onError(new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }
        parseConfig(config);
        innerSpeechListener.setListener(listener);
        if (innerProcessor == null)
            innerProcessor = new SevcProcessor();
        innerProcessor.init(innerSpeechListener, innerConfig);
        mBaseProcessor = innerProcessor;
    }

    private void parseConfig(AILocalSevcConfig config) {
        if (config == null) {
            Log.e(TAG, "AILocalSevcConfig is null !");
            return;
        }
        Log.d(TAG, "AILocalSevcConfig " + config);

        // resource in assets
        final List<String> resourceInAssetsList = new ArrayList<>();

        // sspeBinResource
        String sspeBinResource = config.getSspeBinResource();
        if (TextUtils.isEmpty(sspeBinResource)) {
            Log.e(TAG, "sspeBinResource not found !!");
        } else if (sspeBinResource.startsWith("/")) {
            innerConfig.setSspeBinPath(sspeBinResource);
        } else {
            resourceInAssetsList.add(sspeBinResource);
            innerConfig.setSspeBinPath(Util.getResourceDir(innerConfig.getContext()) + File.separator + sspeBinResource);
        }

        innerConfig.setAssetsResNames(resourceInAssetsList.toArray(new String[resourceInAssetsList.size()]));
    }

    /**
     * 启动引擎
     *
     * @param aiLocalSevcIntent 参数
     */
    public synchronized void start(AILocalSevcIntent aiLocalSevcIntent) {
        super.start();
        parseIntent(aiLocalSevcIntent);
        if (innerProcessor != null) {
            innerProcessor.start(innerParam);
        }
    }

    private void parseIntent(AILocalSevcIntent aiLocalSevcIntent) {
        if (aiLocalSevcIntent == null) {
            Log.e(TAG, "AILocalSevcIntent is null !");
            return;
        }
        Log.d(TAG, "AILocalSevcIntent " + aiLocalSevcIntent);
        innerParam.setUseCustomFeed(aiLocalSevcIntent.isUseCustomFeed());
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     * @param size 数据大小
     */
    public synchronized void feedData(byte[] data, int size) {
        if (!innerParam.isUseCustomFeed()) {
            Log.d(TAG, "feedData, but not UseCustomFeed");
            return;
        }
        if (innerProcessor != null) {
            innerProcessor.feedData(data, size);
        }
    }

    /**
     * 动态设置参数
     *
     * @param param 参数
     */
    public synchronized void setDynamicParam(String param) {
        if (innerProcessor != null) {
            innerProcessor.set(param);
        }
    }

    /**
     * 不直接提供 get 通用方法，提供 get 具体信息的方法
     *
     * @param param 参数
     */
    private synchronized int getInfo(String param) {
        if (innerProcessor != null) {
            return innerProcessor.get(param);
        }
        return -1;
    }

    /**
     * 停止引擎
     * <p>
     * 该方法会停止接收录音数据和停止引擎，程序退出时可以调用
     * </p>
     */
    public synchronized void stop() {
        super.stop();
        if (innerProcessor != null) {
            innerProcessor.stop();
        }
    }

    /**
     * 销毁引擎
     * <p>
     * 该方法会停止录音机和销毁录音机
     * </p>
     */
    public synchronized void destroy() {
        super.destroy();
        if (innerProcessor != null) {
            innerProcessor.release();
            innerProcessor = null;
        }
        if (innerSpeechListener != null) {
            innerSpeechListener.setListener(null);
        }
    }


    private class SpeechListenerImpl extends SpeechListener {

        private AILocalSevcListener listener;
        private Handler mainHandler = new Handler(Looper.getMainLooper());

        public void setListener(AILocalSevcListener listener) {
            this.listener = listener;
        }

        @Override
        public void onInit(final int status) {
            if (listener != null)
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null)
                            listener.onInit(status);
                    }
                });

        }

        @Override
        public void onError(final AIError error) {
            if (listener != null)
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null)
                            listener.onError(error);
                    }
                });
        }

        @Override
        public void onInputDataReceived(byte[] data, int size) {
            if (listener != null)
                listener.onInputDataReceived(data, size);
        }

        @Override
        public void onAgcDataReceived(byte[] buffer) {
            if (listener != null)
                listener.onAgcDataReceived(buffer);
        }

        @Override
        public void onSevcNoiseResult(String retString) {
            if (listener != null)
                listener.onSevcNoiseResult(retString);
        }

        @Override
        public void onSevcDoaResult(int doa) {
            if (listener != null)
                listener.onSevcDoaResult(doa);
        }
    }
}
