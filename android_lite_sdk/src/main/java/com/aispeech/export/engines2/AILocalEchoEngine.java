package com.aispeech.export.engines2;


import com.aispeech.AIEchoConfig;
import com.aispeech.AIError;
import com.aispeech.common.ISyncEngine;
import com.aispeech.common.Log;
import com.aispeech.echo.EchoKernel;
import com.aispeech.export.config.EchoConfig;
import com.aispeech.export.engines2.listeners.AILocalEchoListener;
import com.aispeech.export.function.IEcho;
import com.aispeech.export.intent.AILocalEchoIntent;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.base.SyncRequest;
import com.aispeech.lite.echo.LocalEchoProcessor;
import com.aispeech.lite.param.EchoParams;
import com.aispeech.lite.speech.SpeechListener;

/**
 * @Description: Echo 对外引擎封装; 底层使用sspe为kernel {@link com.aispeech.echo.EchoKernel}
 * @Author: junlong.huang
 * @CreateTime: 2022/8/15
 */
public class AILocalEchoEngine extends BaseEngine implements IEcho, ISyncEngine<EchoConfig, AILocalEchoListener> {

    private final LocalEchoProcessor mLocalEchoProcessor;
    private final EchoProcessorListenerImpl mProcessorListener;
    private EchoSyncRequest mSyncRequestDelegate;

    private EchoParams echoParams;


    public AILocalEchoEngine() {
        mLocalEchoProcessor = new LocalEchoProcessor();
        mProcessorListener = new EchoProcessorListenerImpl();
        echoParams = new EchoParams();
        mBaseProcessor = mLocalEchoProcessor;
        mSyncRequestDelegate = new EchoSyncRequest();
    }

    public static boolean checkLibValid() {
        //车载和公版sspe 目前分开的库，需要根据麦序阵列判断加载不同的sspe
        //后续合并后。将通过Sspe.isSoValid()判断是否加载成功
//        return Sspe.isSoValid();
        return true;
    }

    public static AILocalEchoEngine newInstance() {
        return new AILocalEchoEngine();
    }

    @Override
    public String getTag() {
        return "local_echo";
    }

    @Override
    public void init(EchoConfig config, AILocalEchoListener listener) {
        super.init();
        parseConfig(config);
        mProcessorListener.init(listener);
        mLocalEchoProcessor.init(mProcessorListener);

    }

    public void init(AIEchoConfig config, AILocalEchoListener listener) {
        super.init();
        if (config != null) {
            EchoKernel.setAiEchoConfig(config);
        }
        mProcessorListener.init(listener);
        mLocalEchoProcessor.init(mProcessorListener);

    }

    /**
     * 全局配置，不做单独解析
     *
     * @param echoConfig
     */
    private void parseConfig(EchoConfig echoConfig) {
        if (echoConfig != null) {
            Log.d(TAG, "setAIEchoConfig" + echoConfig.getAecResource());
            AIEchoConfig aiEchoConfig = new AIEchoConfig();
            aiEchoConfig.setAecResource(echoConfig.getAecResource()); // 设置echo的AEC资源文件
            aiEchoConfig.setChannels(echoConfig.getChannels()); //音频总的通道数
            aiEchoConfig.setMicNumber(echoConfig.getMicNumber()); //真实mic数
            aiEchoConfig.setRecChannel(echoConfig.getRecChannel());
            aiEchoConfig.setSavedDirPath(echoConfig.getSavedDirPath());//设置保存的aec原始输入和aec之后的音频文件路径
            aiEchoConfig.setMicType(echoConfig.getMicType());
            EchoKernel.setAiEchoConfig(aiEchoConfig);
        } else {
            Log.w(TAG, "parseConfig: echo config is null,use last config ");
        }

    }

    public void start() {
        start(new AILocalEchoIntent());
    }

    @Override
    public void start(AILocalEchoIntent intent) {
        super.parseIntent(intent, echoParams);
        super.start();
        if (mLocalEchoProcessor != null) mLocalEchoProcessor.start(echoParams);
    }

    @Override
    public void stop() {
        super.stop();
        if (mLocalEchoProcessor != null) mLocalEchoProcessor.stop();
    }

    @Override
    public void destroy() {
        super.destroy();
        if (mLocalEchoProcessor != null) mLocalEchoProcessor.release();
    }

    @Override
    public void feed(byte[] data) {
        if (mLocalEchoProcessor != null) mLocalEchoProcessor.feedData(data, data.length);
    }

    @Override
    public int initSync(EchoConfig config, long time, AILocalEchoListener listener) {
        super.initSync();
        parseConfig(config);
        mProcessorListener.init(listener);

        return mSyncRequestDelegate.initSync(time);
    }

    private class EchoSyncRequest extends SyncRequest {

        @Override
        public void doInit() {
            mLocalEchoProcessor.init(mProcessorListener);
        }
    }


    private class EchoProcessorListenerImpl extends SpeechListener {

        private AILocalEchoListener listener;

        public void init(AILocalEchoListener localEchoListener) {
            this.listener = localEchoListener;
        }

        @Override
        public void onEchoDataReceived(byte[] data, int size) {
            if (listener != null) listener.onResultBufferReceived(data);
        }

        @Override
        public void onEchoVoipDataReceived(byte[] data, int length) {
            if (listener != null) listener.onVoipBufferReceived(data);
        }

        @Override
        public void onInit(int status) {
            if (isInitSync && mSyncRequestDelegate.isInitRequesting()) {
                mSyncRequestDelegate.notifyInitResult(status);
            } else if (!isInitSync && listener != null) {
                listener.onInit(status);
            }
        }

        @Override
        public void onError(AIError error) {
            if (listener != null) listener.onError(error);
        }

        @Override
        public void onReadyForSpeech() {
            if (listener != null) listener.onReadyForSpeech();
        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {
            if (listener != null) listener.onRawDataReceived(buffer, size);
        }

        @Override
        public void onRecorderStopped() {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {
            if (listener != null) {
                listener.onResultDataReceived(buffer, size, wakeupType);
            }
        }

        @Override
        public void onCancel() {

        }
    }
}
