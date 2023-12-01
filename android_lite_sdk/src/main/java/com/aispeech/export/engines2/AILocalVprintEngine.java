package com.aispeech.export.engines2;


import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.listeners.AILocalVprintListener;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Vprint;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalVprintConfig;
import com.aispeech.lite.param.VprintParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.vprint.VprintConfig;
import com.aispeech.lite.vprint.VprintIntent;
import com.aispeech.lite.vprint.VprintProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地声纹
 */
public class AILocalVprintEngine extends BaseEngine {

    private static AILocalVprintEngine mInstance = null;
    private VprintProcessor mVprintProcessor;
    private VprintParams mVprintParams;
    private LocalVprintConfig mVprintConfig;
    private SpeechListenerImpl mSpeechListener;
    private VprintIntent.Action mAction;
    /**
     * 是否处于初始化中
     */
    private boolean initStatus = false;


    private AILocalVprintEngine() {
        mVprintProcessor = VprintProcessor.getInstance();
        mVprintParams = new VprintParams();
        mVprintConfig = new LocalVprintConfig();
        mSpeechListener = new SpeechListenerImpl(null);
        mBaseProcessor = mVprintProcessor;
    }

    @Override
    public String getTag() {
        return "local_vprint";
    }


    public static boolean checkLibValid() {
        return Vprint.isVprintSoValid() &&
                Utils.isUtilsSoValid();
    }


    public static synchronized AILocalVprintEngine getInstance() { //懒汉式单例
        if (mInstance == null) {
            mInstance = new AILocalVprintEngine();
        }
        return mInstance;
    }

    /**
     * 返回的json信息是否是查询注册音频接口回调的json
     *
     * @param json json字符串
     * @return true 是，false 不是
     */
    public static boolean isQueryRegisterAudioJson(String json) {
        return json != null && json.contains("QueryRegisterAudio");
    }

    /**
     * 获取当前声纹模型中的注册信息，需要在init成功后调用生效。
     */
    public void queryModel() {
        if (mVprintProcessor != null) {
            mVprintProcessor.set("{\"env\":\"op=query;\"}");
        }
    }

    /**
     * 查询已经注册的音频
     *
     * @param name 注册的用户名
     * @param word 注册的唤醒词
     */
    public void queryRegisterAudio(String name, String word) {
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(word)) {
            Log.d(TAG, "queryRegisterAudio() name or word is empty ");
            return;
        }
        if (mVprintProcessor != null) {
            mVprintProcessor.set(String.format("{\"env\":\"op=query_register_audio;name=%s;word=%s\"}", name, word));
        }
    }

    /**
     * 初始化声纹引擎
     *
     * @param config              声纹配置
     * @param localVprintListener 声纹回调
     */
    public synchronized void init(VprintConfig config, AILocalVprintListener localVprintListener) {
        super.init();
        initStatus = true;
        List<String> assetsResList = new ArrayList<>();
        mSpeechListener.setListener(localVprintListener);
        if (!config.getAsrppResBin().startsWith("/")) {
            assetsResList.add(config.getAsrppResBin());
            mVprintConfig.setAsrppResBin(Util.getResPath(AISpeech.getContext(), config.getAsrppResBin()));
        } else {
            mVprintConfig.setAsrppResBin(config.getAsrppResBin());
        }
        if (!config.getVprintResBin().startsWith("/")) {
            assetsResList.add(config.getVprintResBin());
            mVprintConfig.setVprintResBin(Util.getResPath(AISpeech.getContext(), config.getVprintResBin()));
        } else {
            mVprintConfig.setVprintResBin(config.getVprintResBin());
        }
        mVprintConfig.setAssetsResNames(assetsResList.toArray(new String[assetsResList.size()]));
        mVprintConfig.setVprintModelFile(config.getVprintModelPath());
        mVprintConfig.setUseDatabaseStorage(config.isUseDatabaseStorage());
        mVprintConfig.setVprintDatabasePath(config.getVprintDatabasePath());
        mVprintProcessor.init(mSpeechListener, mVprintConfig);
    }


    /**
     * 启动声纹引擎
     *
     * @param intent 声纹Intent
     */
    public void start(VprintIntent intent) {
        super.start();
        super.parseIntent(intent, mVprintParams);
        if (mVprintProcessor != null) {
            mAction = intent.getAction();
            mVprintParams.setBfChannelNum(intent.getBfChannelNum());
            mVprintParams.setAecChannelNum(intent.getAecChannelNum());
            mVprintParams.setOutChannelNum(intent.getOutChannelNum());
            mVprintParams.setAction(intent.getAction().getValue());
            mVprintParams.setTrainNum(intent.getTrainNum());
            mVprintParams.setUserId(intent.getUserId());
            mVprintParams.setVprintWord(intent.getVprintWord());
            mVprintParams.setThresh(intent.getThresh());
            mVprintParams.setSnrThresh(intent.getSnrThresh());
            mVprintParams.setSaveAudioPath(intent.getSaveAudioPath());
            mVprintParams.setVprintCutSaveDir(intent.getVprintCutSaveDir());
            mVprintParams.setSensitivityLevel(intent.getSensitivityLevel());
            mVprintProcessor.start(mVprintParams);
        }
    }


    /**
     * 传入事件信息，比如唤醒JSON字符串
     *
     * @param event 事件信息
     */
    public void notifyEvent(String event) {
        if (mVprintProcessor != null) {
            mVprintProcessor.notifyEvent(event);
        }
    }

    /**
     * 获取当前声纹模式
     *
     * @return 当前声纹模式
     */
    public VprintIntent.Action getAction() {
        return mAction;
    }

    /**
     * 传入数据
     *
     * @param data 音频数据流
     * @param size 数据大小
     */
    public void feedData(byte[] data, int size) {
        if (mVprintProcessor != null) {
            mVprintProcessor.feedData(data, size);
        }
    }

    /**
     * 传入数据
     *
     * @param dataType 数据类型
     * @param data     数据流
     * @param size     数据流大小
     */
    public void feedData(int dataType, byte[] data, int size) {
        if (mVprintProcessor != null) {
            if (AIConstant.AIENGINE_MESSAGE_TYPE_JSON == dataType) {
                mVprintProcessor.notifyEvent(Util.newUTF8String(data));
            } else if (AIConstant.AIENGINE_MESSAGE_TYPE_BIN == dataType) {
                mVprintProcessor.feedData(data, size);
            } else if (AIConstant.DUILITE_MSG_TYPE_TLV == dataType) {
                mVprintProcessor.feedTLV(data, size);
            } else
                Log.d(TAG, "no support dataType " + dataType);
        }
    }

    /**
     * 停止声纹引擎，该接口只在通用声纹模式下需要调用，唤醒+声纹不需要调用
     */
    public void stop() {
        super.stop();
        if (mVprintProcessor != null) {
            mVprintProcessor.stop();
        }
    }


    /**
     * 取消声纹引擎，当不再接受内部消息时或切换模式前调用
     */
    public void cancel() {
        super.cancel();
        if (mVprintProcessor != null) {
            mVprintProcessor.cancel();
        }
    }

    /**
     * 销毁声纹引擎
     */
    public synchronized void destroy() {
        super.destroy();
        if (initStatus) {
            Log.e(TAG, "never happened: init ing");
            return;
        }
        if (mVprintProcessor != null) {
            mVprintProcessor.release();
            mVprintProcessor = null;
        }
        if (mVprintParams != null)
            mVprintParams = null;
        if (mVprintConfig != null)
            mVprintConfig = null;
        if (mSpeechListener != null)
            mSpeechListener = null;
        if (mInstance != null)
            mInstance = null;
    }


    /**
     * The adapter for convert SpeechListener to AIASRListener.
     */
    private class SpeechListenerImpl extends SpeechListener {
        AILocalVprintListener mListener;

        public SpeechListenerImpl(AILocalVprintListener listener) {
            mListener = listener;
        }

        public void setListener(AILocalVprintListener listener) {
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
            initStatus = false;
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
            //nothing
        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {
            //nothing
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {
            //nothing
        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] buffer, int size) {
            //nothing
        }

        @Override
        public void onEndOfSpeech() {
            //nothing
        }


        @Override
        public void onReadyForSpeech() {
            //nothing
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            //nothing
        }

        /**
         * @deprecated 废弃，请勿继续使用
         */
        @Override
        @Deprecated
        public void onRecorderStopped() {
            //nothing
        }


        @Override
        public void onEvent(int eventType, Map params) {
            //nothing
        }

        @Override
        public void onNotOneShot() {
            //nothing
        }
    }
}
