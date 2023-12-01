package com.aispeech.echo;

import android.text.TextUtils;

import com.aispeech.AIEchoConfig;
import com.aispeech.AIError;
import com.aispeech.DUILiteConfig;
import com.aispeech.auth.AIAuthEngine;
import com.aispeech.auth.AIProfile;
import com.aispeech.auth.ProfileState;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AssetsHelper;
import com.aispeech.common.FileSaveUtil;
import com.aispeech.common.JNIFlag;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.bean.VoiceQueueStrategy;
import com.aispeech.kernel.Sspe;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.LocalEchoConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.EchoParams;

import java.io.File;
import java.util.Map;


/**
 * Created by wuwei on 18-6-19.
 */

public class EchoKernel extends BaseKernel {
    private static final String TAG = "EchoKernel";
    private static final AIEchoConfig aiEchoConfig = new AIEchoConfig();
    private EchoKernelListener mListener;
    private Sspe mEngine;
    private volatile boolean isStopped = true;
    private android.content.Context mContext;
    FileSaveUtil echoFileUtil = null;
    private MyEchoCallback myEchoCallback;
    private MyEchoVoipCallback voipCallback;
    private LocalEchoConfig mEchoConfig;
    private ProfileState mProfileState;
    private EchoParams echoParams;

    public EchoKernel(EchoKernelListener listener) {
        this(AIAuthEngine.getInstance().getProfile(), listener);
    }

    public EchoKernel(AIProfile profile, EchoKernelListener listener) {
        super(TAG, listener);
        this.mListener = listener;
        mContext = AISpeech.getContext();
        this.profile = profile;
        mProfileState = this.profile.isProfileValid(Scope.LOCAL_ECHO);
        Log.d(TAG, "authstate: " + mProfileState.toString());
        if (mProfileState.isValid()) {
            myEchoCallback = new MyEchoCallback();
            voipCallback = new MyEchoVoipCallback();
            mEchoConfig = new LocalEchoConfig();

            // echoAecResource
            final String echoAecResource = aiEchoConfig.getAecResource();
            if (TextUtils.isEmpty(echoAecResource)) {
                Log.e(TAG, "aec res not found !!");
            } else if (echoAecResource.startsWith("/")) {
                mEchoConfig.setResBinPath(echoAecResource);
            } else {
                mEchoConfig.setAssetsResNames(new String[]{echoAecResource});
                mEchoConfig.setResBinPath(Util.getResourceDir(mEchoConfig.getContext()) + File.separator + echoAecResource);
            }

            mEchoConfig.setChannels(aiEchoConfig.getChannels());
            mEchoConfig.setMicNum(aiEchoConfig.getMicNumber());
        } else {
            showErrorMessage(mProfileState);
        }
    }

    public static void setAiEchoConfig(AIEchoConfig aiEchoConfig) {
        EchoKernel.aiEchoConfig.setAIEchoConfig(aiEchoConfig);
        int micType = EchoKernel.aiEchoConfig.getMicType();
        if (micType == DUILiteConfig.TYPE_COMMON_FESPCAR || micType == DUILiteConfig.TYPE_COMMON_FESPCAR4) {
            JNIFlag.isLoadCarSspe = true;
        } else {
            JNIFlag.isLoadCarSspe = false;
        }
    }

    /**
     * 初始化echo内核
     */
    public void newKernel() {
        if (mProfileState != null && mProfileState.isValid()) {
            Log.d(TAG, "newKernel");
            sendMessage(new Message(Message.MSG_NEW));
        } else {
            showErrorMessage(mProfileState);
        }
    }

    public void startKernel() {
        startKernel(null);
    }

    /**
     * 启动echo内核
     */
    public void startKernel(EchoParams params) {
        if (mProfileState != null && mProfileState.isValid()) {
            Log.d(TAG, "startKernel");
            sendMessage(new Message(Message.MSG_START, params));
        } else {
            showErrorMessage(mProfileState);
        }
    }

    /**
     * feed 音频数据给内核，只能是 pcm 音频数据
     *
     * @param data pcm 数据
     */
    @Override
    public void feed(byte[] data) {
        if (mProfileState != null && mProfileState.isValid()) {
            super.feed(data);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    @Override
    public void cancelKernel() {
        if (mProfileState != null && mProfileState.isValid()) {
            super.cancelKernel();
        } else {
            showErrorMessage(mProfileState);
        }
    }

    @Override
    public void stopKernel() {
        if (mProfileState != null && mProfileState.isValid()) {
            super.stopKernel();
        } else {
            showErrorMessage(mProfileState);
        }
    }

    @Override
    public void releaseKernel() {
        if (mProfileState != null && mProfileState.isValid()) {
            super.releaseKernel();
        } else {
            showErrorMessage(mProfileState);
        }
    }

    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    handleMshNew();
                    break;
                case Message.MSG_START:
                    handleMsgStart(message);
                    break;
                case Message.MSG_STOP:
                    handleMsgStop();
                    break;
                case Message.MSG_RELEASE:
                    handleMsgRelease();
                    isReleased = true;
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    handleMsgFeedData(message);
                    break;
                case Message.MSG_ERROR:
                    mListener.onError((AIError) message.mObject);
                    break;
                default:
                    break;
            }
            if (isReleased) {
                innerRelease();
                break;//release后跳出while循环
            }
        }
    }

    private void handleMsgFeedData(Message message) {
        byte[] data = (byte[]) message.mObject;
        //保存原始录音的音频数据aec_in
        if (echoFileUtil != null && !isStopped) {
            echoFileUtil.feedTypeIn(data);
        }
        if (aiEchoConfig.getRecChannel() == 1) {//无需通道互换的音频
            if (mEngine != null && !isStopped) {
                mEngine.feed(data, data.length);
            }
        } else if (aiEchoConfig.getRecChannel() == 2) {//需要通道互换,保存互换之后的aec_in
            byte[] recChannelData = Util.getRecChannelData(data);
            //处理互换之后的原始数据
            if (mEngine != null && !isStopped) {
                mEngine.feed(recChannelData, data.length);
            }
        }
    }

    private void handleMsgRelease() {
        closeFileWriter();
        if (mEngine != null) {
            mEngine.destroy();
            mEngine = null;
        }
        if (mEchoConfig != null) {
            mEchoConfig = null;
        }
        if (myEchoCallback != null) {
            myEchoCallback = null;
        }
        if (voipCallback != null) voipCallback = null;
        isStopped = true;
    }

    private void handleMsgStop() {
        closeFileWriter();
        if (mEngine != null) {
            mEngine.stop();
        }
        isStopped = true;
    }

    private void handleMsgStart(Message message) {
        if (mProfileState.getAuthType() == ProfileState.AUTH_TYPE.TRIAL && mProfileState.getTimesLimit() != -1 && !updateTrails(profile, mProfileState, Scope.LOCAL_ECHO)) {
            return;
        }
        if (message.mObject instanceof EchoParams) {
            echoParams = (EchoParams) message.mObject;
        }
        createFileWriter();
        if (mEngine != null) {
            mEngine.start("");
            isStopped = false;
        }
    }

    private void handleMshNew() {
        mEngine = new Sspe();
        VoiceQueueStrategy voiceQueueStrategy = aiEchoConfig.getMaxVoiceQueueSize();
        if (voiceQueueStrategy != null) {
            setMaxVoiceQueueSize(voiceQueueStrategy.getMaxVoiceQueueSize(), voiceQueueStrategy.getIgnoreSize());
        }
        int flag = initEcho(mEchoConfig, mEngine);
        mListener.onInit(flag);
    }

    private int initEcho(LocalEchoConfig config, Sspe engine) {
        if (config == null) return AIConstant.OPT_FAILED;

        int status = copyAssetsRes(config);
        if (status == AIConstant.OPT_FAILED) {
            return status;
        }
        // 创建引擎
        String cfg = config.toSspeJSON().toString();
        Log.d(TAG, "config" + cfg);
        long engineId = engine.init(cfg);
        Log.d(TAG, "echo create return " + engineId + ".");
        if (engineId == 0) {
            Log.d(TAG, "引擎初始化失败");
            return AIConstant.OPT_FAILED;
        }
        Log.d(TAG, "引擎初始化成功");
        int ret = engine.setCallback(myEchoCallback, voipCallback);
        if (ret != 0 && ret != -9892) {
            // 0 成功  -9892 表示内核不支持该功能
            Log.e(TAG, "setCallback failed");
            return AIConstant.OPT_FAILED;
        } else return AIConstant.OPT_SUCCESS;
    }

    /**
     * 拷贝assets目录下的制定资源
     *
     * @param config
     * @return
     */
    private int copyAssetsRes(AIEngineConfig config) {
        int status = AIConstant.OPT_SUCCESS;
        String[] assetsResNames = config.getAssetsResNames();
        Map<String, String> assetsResMd5sumMap = config.getAssetsResMd5sum();
        if (assetsResNames != null && assetsResNames.length > 0) {
            for (String resName : assetsResNames) {
                String resMd5sumName = null;
                if (assetsResMd5sumMap != null) {
                    resMd5sumName = assetsResMd5sumMap.get(resName);
                }
                int ret = AssetsHelper.copyResource(mContext, resName, resMd5sumName);
                if (ret == -1) {
                    Log.e(TAG, "file " + resName + " not found in assest folder, Did you forget add it?");
                    return ret;
                }
            }
            AssetsHelper.updateMapFile(mContext);
        }
        return status;
    }

    private void createFileWriter() {
        if (!TextUtils.isEmpty(aiEchoConfig.getSavedDirPath())) {
            echoFileUtil = new FileSaveUtil();
            echoFileUtil.init(aiEchoConfig.getSavedDirPath());
            echoFileUtil.prepare("echo");
        }
    }

    private void closeFileWriter() {
        if (echoFileUtil != null) {
            echoFileUtil.close();
            echoFileUtil = null;
        }
    }

    private class MyEchoCallback extends Sspe.echo_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bufferData = checkNeedCopyResultData(echoParams, data, size);
                if (mListener != null) {
                    mListener.onResultBufferReceived(bufferData);
                    //本地保存处理后的音频文件
                    if (echoFileUtil != null) {
                        echoFileUtil.feedTypeOut(bufferData);
                    }
                }
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }

    private class MyEchoVoipCallback implements Sspe.echo_voip_callback {

        @Override
        public int run(int type, byte[] data, int size) {
            if (type == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bufferData = checkNeedCopyResultData(echoParams, data, size);
                if (mListener != null) mListener.onAgcDataReceived(bufferData);
            }
            return 0;
        }
    }


}
