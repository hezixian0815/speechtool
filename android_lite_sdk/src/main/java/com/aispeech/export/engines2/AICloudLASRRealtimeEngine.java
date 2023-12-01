package com.aispeech.export.engines2;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.auth.AIAuthEngine;
import com.aispeech.common.Log;
import com.aispeech.export.config.AICloudASRConfig;
import com.aispeech.export.intent.AICloudASRIntent;
import com.aispeech.export.intent.AICloudLASRRealtimeIntent;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AILASRRealtimeListener;
import com.aispeech.lite.base.BaseEngine;

/**
 * 长语音实时识别
 */
public class AICloudLASRRealtimeEngine extends BaseEngine {

    private AICloudASREngine mAICloudASREngine;
    private AIASRListenerImpl listener;

    public static AICloudLASRRealtimeEngine createInstance() {
        return new AICloudLASRRealtimeEngine();
    }

    private AICloudLASRRealtimeEngine() {
        mAICloudASREngine = AICloudASREngine.createInstance();
    }

    @Override
    public String getTag() {
        return "cloud_lasr_real";
    }

    public void init(final AILASRRealtimeListener lasrListener) {
        AICloudASRConfig config = new AICloudASRConfig();
        config.setLocalVadEnable(false);
        this.init(config, lasrListener);
    }

    public void init(AICloudASRConfig config, final AILASRRealtimeListener lasrListener) {
        listener = new AIASRListenerImpl();
        listener.setLasrListener(lasrListener);
        mAICloudASREngine.init(config, listener);
    }

    public void start(AICloudLASRRealtimeIntent mAICloudLASRRealtimeIntent) {
        if (mAICloudLASRRealtimeIntent == null || !mAICloudLASRRealtimeIntent.isValid())
            return;
        Log.d(TAG, "AICloudLASRRealtimeIntent " + mAICloudLASRRealtimeIntent);
        String productId = "";
        if (AIAuthEngine.getInstance().getProfile() != null)
            productId = AIAuthEngine.getInstance().getProfile().getProductId();
        Log.d(TAG, "productId " + productId);
        mAICloudASREngine.setLasrInfo(mAICloudLASRRealtimeIntent.getJsonLSR().toString(),
                mAICloudLASRRealtimeIntent.getRes(), mAICloudLASRRealtimeIntent.getForwardAddresses());
        // 还可以增加的参数 setUseCustomFeed setSaveAudioPath setAudioType
        AICloudASRIntent aiCloudASRIntent = new AICloudASRIntent();
        aiCloudASRIntent.setServer(mAICloudLASRRealtimeIntent.getServer());
        aiCloudASRIntent.setMaxSpeechTimeS(0);
        aiCloudASRIntent.setNoSpeechTimeOut(0);
        aiCloudASRIntent.setExtraParam(mAICloudLASRRealtimeIntent.getExtraParam());
        aiCloudASRIntent.setFespxEngine(mAICloudLASRRealtimeIntent.getFespxEngine());
        aiCloudASRIntent.setLanguage(mAICloudLASRRealtimeIntent.getLang());

        AICloudASRIntent.PCM_ENCODE_TYPE audioType;
        switch (mAICloudLASRRealtimeIntent.getAudioType()) {
            case WAV:
                audioType = AICloudASRIntent.PCM_ENCODE_TYPE.WAV;
                break;
            case MP3:
                audioType = AICloudASRIntent.PCM_ENCODE_TYPE.MP3;
                break;
            /*case OPUS:
                audioType = AICloudASRIntent.PCM_ENCODE_TYPE.OPUS;
                break;*/
            case OGG_OPUS:
                audioType = AICloudASRIntent.PCM_ENCODE_TYPE.OGG_OPUS;
                break;
            default:
                audioType = AICloudASRIntent.PCM_ENCODE_TYPE.OGG;
                break;
        }
        aiCloudASRIntent.setAudioType(audioType);
        // aiCloudASRIntent.setSaveAudioPath("/sdcard/aispeech");
        aiCloudASRIntent.setUseCustomFeed(mAICloudLASRRealtimeIntent.isUseCustomFeed(), mAICloudLASRRealtimeIntent.isEncodedAudio());
        mAICloudASREngine.start(aiCloudASRIntent);
    }

    public void stop() {
        mAICloudASREngine.stop();
    }

    public void feedData(byte[] data, int size) {
        mAICloudASREngine.feedData(data, size);
    }

    public void cancel() {
        mAICloudASREngine.cancel();
    }

    public void destroy() {
        mAICloudASREngine.destroy();
        if (listener != null) {
            listener.setLasrListener(null);
            listener = null;
        }
    }

    private static class AIASRListenerImpl implements AIASRListener {

        private AILASRRealtimeListener lasrListener;

        public AILASRRealtimeListener getLasrListener() {
            return lasrListener;
        }

        public void setLasrListener(AILASRRealtimeListener lasrListener) {
            this.lasrListener = lasrListener;
        }

        @Override
        public void onInit(int status) {
            if (lasrListener != null)
                lasrListener.onInit(status);
        }

        @Override
        public void onError(AIError error) {
            if (lasrListener != null)
                lasrListener.onError(error);
        }

        @Override
        public void onResults(AIResult result) {
            if (lasrListener != null)
                lasrListener.onResults(result);
        }

        @Override
        public void onRmsChanged(float rmsdB) {

        }

        @Override
        public void onReadyForSpeech() {
            if (lasrListener != null)
                lasrListener.onReadyForSpeech();
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onEndOfSpeech() {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {
            if (lasrListener != null)
                lasrListener.onRawDataReceived(buffer, size);
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size) {
            if (lasrListener != null)
                lasrListener.onResultDataReceived(buffer, size);
        }

        @Override
        public void onNotOneShot() {
        }
    }
}
