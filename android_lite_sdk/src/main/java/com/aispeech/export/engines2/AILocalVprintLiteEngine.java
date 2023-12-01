package com.aispeech.export.engines2;

import android.text.TextUtils;
import android.util.Log;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.Util;
import com.aispeech.export.listeners.AILocalVprintLiteListener;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.VprintLite;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalVprintLiteConfig;
import com.aispeech.lite.param.VprintLiteParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.vprintlite.VprintLiteConfig;
import com.aispeech.lite.vprintlite.VprintLiteIntent;
import com.aispeech.lite.vprintlite.VprintLiteProcessor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 本地声纹
 */
public class AILocalVprintLiteEngine extends BaseEngine {

    private VprintLiteProcessor mVprintLiteProcessor;
    private VprintLiteParams mVprintLiteParams;
    private LocalVprintLiteConfig mVprintLiteConfig;
    private SpeechListenerImpl mSpeechListener;
    private VprintLiteIntent.Action mAction;
    /**
     * 是否处于初始化中
     */
    private boolean initStatus = false;


    private AILocalVprintLiteEngine() {
        mVprintLiteProcessor = new VprintLiteProcessor();
        mVprintLiteParams = new VprintLiteParams();
        mVprintLiteConfig = new LocalVprintLiteConfig();
        mSpeechListener = new SpeechListenerImpl(null);
        mBaseProcessor = mVprintLiteProcessor;
    }

    @Override
    public String getTag() {
        return "local_vprint_lite";
    }


    public static boolean checkLibValid() {
        return VprintLite.isVprintSoValid() &&
                Utils.isUtilsSoValid();
    }

    /**
     * 多实例对象
     *
     * @return engine对象
     */
    public static synchronized AILocalVprintLiteEngine createInstance() {
        return new AILocalVprintLiteEngine();
    }

    /**
     * 初始化声纹引擎
     *
     * @param config                  声纹配置
     * @param localVprintLiteListener 声纹回调
     */
    public synchronized void init(VprintLiteConfig config, AILocalVprintLiteListener localVprintLiteListener) {
        super.init();
        initStatus = true;
        List<String> assetsResList = new ArrayList<>();
        mSpeechListener.setListener(localVprintLiteListener);
        mVprintLiteConfig.setVprintType(config.getVprintType());
        String vprintRes = config.getVprintResBin();
        if (TextUtils.isEmpty(vprintRes)) {
            Log.e(TAG, "Vprintlite res not found !!");
        } else if (!vprintRes.startsWith("/")) {
            assetsResList.add(vprintRes);
            mVprintLiteConfig.setVprintResBin(Util.getResPath(AISpeech.getContext(), vprintRes));
        } else {
            mVprintLiteConfig.setVprintResBin(vprintRes);
        }

        mVprintLiteConfig.setAssetsResNames(assetsResList.toArray(new String[assetsResList.size()]));
        String vprintModelPath = config.getVprintModelPath();
        if (TextUtils.isEmpty(vprintModelPath)) {
            Log.e(TAG, "model path is null");
        } else {
            File file = new File(vprintModelPath);
            if (file.isDirectory()) {
                Log.e(TAG, "model path is illegal");
            } else {
                //当前内核只支持需要文件父目录存在，故而创建父目录，文件本身由内核创建
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
            }
            mVprintLiteConfig.setVprintModelFile(config.getVprintModelPath());
        }
        mVprintLiteProcessor.init(mSpeechListener, mVprintLiteConfig);
    }


    /**
     * 启动声纹引擎
     *
     * @param intent 声纹Intent
     */
    public void start(VprintLiteIntent intent) {
        super.start();
        if (mVprintLiteProcessor != null) {
            parseIntent(intent);
            mVprintLiteProcessor.start(mVprintLiteParams);
        }
    }

    private void parseIntent(VprintLiteIntent intent) {
        super.parseIntent(intent, mVprintLiteParams);
        mAction = intent.getAction();
        mVprintLiteParams.setAction(intent.getAction().getValue());
        mVprintLiteParams.setVprintLiteSaveDir(intent.getVprintLiteSaveDir());
        mVprintLiteParams.setAsrErrorRate(intent.getAsrErrorRate());
        mVprintLiteParams.setConstantContent(intent.getConstantContent());
        mVprintLiteParams.setCustomContent(intent.getCustomContent());
        mVprintLiteParams.setEnhanceRegister(intent.isEnhanceRegister());
        mVprintLiteParams.setuId(intent.getuId());
        mVprintLiteParams.setTopN(intent.getTopN());
        mVprintLiteParams.setRecWords(intent.getRecWords());
        mVprintLiteParams.setResStart(intent.getResStart());
        mVprintLiteParams.setResEnd(intent.getResEnd());
        mVprintLiteParams.setSpeechState(intent.getSpeechState());
    }

    /**
     * 获取当前声纹模式
     *
     * @return 当前声纹模式
     */
    public VprintLiteIntent.Action getAction() {
        return mAction;
    }

    /**
     * 传入数据
     *
     * @param data 音频数据流
     * @param size 数据大小
     */
    public void feedData(byte[] data, int size, VprintLiteIntent intent) {
        parseIntent(intent);
        if (mVprintLiteProcessor != null) {
            mVprintLiteProcessor.feedKWS(data, size, mVprintLiteParams.toCalJSON(mVprintLiteConfig.getVprintType()).toString());
        }
    }

    /**
     * 停止声纹引擎，该接口只在通用声纹模式下需要调用，唤醒+声纹不需要调用
     */
    public void stop() {
        super.stop();
        if (mVprintLiteProcessor != null) {
            mVprintLiteProcessor.stop();
        }
    }


    /**
     * 取消声纹引擎，当不再接受内部消息时或切换模式前调用
     */
    public void cancel() {
        super.cancel();
        if (mVprintLiteProcessor != null) {
            mVprintLiteProcessor.cancel();
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
        if (mVprintLiteProcessor != null) {
            mVprintLiteProcessor.release();
            mVprintLiteProcessor = null;
        }
        if (mVprintLiteParams != null)
            mVprintLiteParams = null;
        if (mVprintLiteConfig != null)
            mVprintLiteConfig = null;
        if (mSpeechListener != null)
            mSpeechListener = null;
    }


    /**
     * The adapter for convert SpeechListener to AIASRListener.
     */
    private class SpeechListenerImpl extends SpeechListener {
        AILocalVprintLiteListener mListener;

        public SpeechListenerImpl(AILocalVprintLiteListener listener) {
            mListener = listener;
        }

        public void setListener(AILocalVprintLiteListener listener) {
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
            //do nothing
        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {
            //do nothing
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {
            //do nothing
        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] buffer, int size) {
            //do nothing
        }

        @Override
        public void onEndOfSpeech() {
            //do nothing
        }


        @Override
        public void onReadyForSpeech() {
            //do nothing
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            //do nothing
        }

        /**
         * @deprecated 废弃
         */
        @Override
        @Deprecated
        public void onRecorderStopped() {
            //do nothing
        }


        @Override
        public void onEvent(int eventType, Map params) {
            //do nothing
        }

        @Override
        public void onNotOneShot() {
            //do nothing
        }
    }
}
