package com.aispeech.export.engines2;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.intent.AILocalLASRIntent;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.kernel.LAsr;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.asr.LocalLAsrProcessor;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalLAsrConfig;
import com.aispeech.lite.param.LocalLAsrParams;
import com.aispeech.lite.speech.SpeechListener;

import java.io.File;
import java.util.Map;

public class AILocalLASREngine extends BaseEngine {


    private LocalLAsrParams localLAsrParams;
    private SpeechListenerImpl innerSpeechListener;
    private LocalLAsrProcessor mLocalLAsrProcessor;
    private LocalLAsrConfig mConfig;

    private AILocalLASREngine() {
        mLocalLAsrProcessor = new LocalLAsrProcessor();
        mConfig = new LocalLAsrConfig();
        localLAsrParams = new LocalLAsrParams();
        innerSpeechListener = new SpeechListenerImpl(null);
        mBaseProcessor = mLocalLAsrProcessor;
    }

    @Override
    public String getTag() {
        return "local_lasr";
    }


    public static AILocalLASREngine createInstance() { //懒汉式单例
        return new AILocalLASREngine();
    }

    private boolean checkLibValid() {
        return LAsr.isEngineSoValid() && Utils.isUtilsSoValid();
    }

    public synchronized void init(String lAsrResource, AIASRListener listener) {
        if (!checkLibValid()) {
            if (listener != null) {
                listener.onInit(AIConstant.OPT_FAILED);
                listener.onError(new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }
        super.init();
        // grammarResource
        if (TextUtils.isEmpty(lAsrResource)) {
            Log.e(TAG, "lAsrResourceResource not found !!");
        } else if (lAsrResource.startsWith("/")) {
            mConfig.setResourcePath(lAsrResource);
        } else {
            mConfig.setResFolderName(lAsrResource);
            String path = Util.getResourceDir(AISpeech.getContext()) + File.separator + lAsrResource;
            Log.i(TAG, "lAsrResourceResource path : "+path);
            mConfig.setResourcePath(path);
        }

        innerSpeechListener.setListener(listener);
        if (mLocalLAsrProcessor == null)
            mLocalLAsrProcessor = new LocalLAsrProcessor();
        mLocalLAsrProcessor.init(innerSpeechListener, mConfig);
    }


    /**
     * 启动引擎
     *
     * @param aiLocalLASRIntent 参数
     */
    public synchronized void start(AILocalLASRIntent aiLocalLASRIntent) {
        super.start();
        parseIntent(aiLocalLASRIntent);
        if (mLocalLAsrProcessor != null) {
            mLocalLAsrProcessor.start(localLAsrParams);
        }
    }

    private void parseIntent(AILocalLASRIntent aiLocalLASRIntent) {
        if (aiLocalLASRIntent == null) {
            Log.e(TAG, "AILocalLASRIntent is null !");
            return;
        }
        super.parseIntent(aiLocalLASRIntent, localLAsrParams);
        Log.d(TAG, "AILocalLASRIntent " + aiLocalLASRIntent);
        localLAsrParams.setUseCustomFeed(aiLocalLASRIntent.isUseCustomFeed());
        localLAsrParams.setLAsrParamJsonString(aiLocalLASRIntent.getLAsrParamJson());
        localLAsrParams.setFespxEngine(aiLocalLASRIntent.getFespxEngine());
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     * @param size 数据大小
     */
    public synchronized void feedData(byte[] data, int size) {
        if (!localLAsrParams.isUseCustomFeed()) {
            Log.d(TAG, "feedData, but not UseCustomFeed");
            return;
        }
        if (mLocalLAsrProcessor != null) {
            mLocalLAsrProcessor.feedData(data, size);
        }
    }

    /**
     * 停止引擎
     * <p>
     * 该方法会停止接收录音数据和停止引擎，程序退出时可以调用
     * </p>
     */
    public synchronized void stop() {
        super.stop();
        if (mLocalLAsrProcessor != null) {
            mLocalLAsrProcessor.stop();
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
        if (mLocalLAsrProcessor != null) {
            mLocalLAsrProcessor.release();
            mLocalLAsrProcessor = null;
        }
        if (innerSpeechListener != null) {
            innerSpeechListener.setListener(null);
            innerSpeechListener = null;
        }
    }


    private class SpeechListenerImpl extends SpeechListener {
        AIASRListener mListener;

        public SpeechListenerImpl(AIASRListener listener) {
            mListener = listener;
        }

        public void setListener(AIASRListener listener) {
            mListener = listener;
        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(error);
            }
        }

        @Override
        public void onInit(int status) {
            if (mListener != null) {
                mListener.onInit(status);
            }
        }

        @Override
        public void onResults(AIResult result) {
            if (mListener != null) {
                mListener.onResults(result);
            }
        }

        @Override
        public void onBeginningOfSpeech() {
            if (mListener != null) {
                mListener.onBeginningOfSpeech();
            }
        }

        @Override
        public void onDoaResult(int doa) {
            //do nothing
        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] data, int size) {
            //do nothing
        }


        @Override
        public void onRawDataReceived(byte[] buffer, int size) {
            if (mListener != null) {
                mListener.onRawDataReceived(buffer, size);
            }
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {
            if (mListener != null) {
                mListener.onResultDataReceived(buffer, size);
            }
        }

        @Override
        public void onEndOfSpeech() {
            if (mListener != null) {
                mListener.onEndOfSpeech();
            }
        }


        @Override
        public void onReadyForSpeech() {
            if (mListener != null) {
                mListener.onReadyForSpeech();
            }

        }

        @Override
        public void onRmsChanged(float rmsdB) {
            if (mListener != null) {
                mListener.onRmsChanged(rmsdB);
            }
        }

        @Override
        public void onEvent(int eventType, Map params) {
            //do nothing
        }

        @Override
        public void onNotOneShot() {
            if (mListener != null) {
                mListener.onNotOneShot();
            }
        }

        @Override
        public void onUpdateResult(int ret) {

        }
    }
}
