package com.aispeech.export.engines2;

import com.aispeech.AIResult;
import com.aispeech.base.BaseInnerEngine;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.ProductContext;
import com.aispeech.export.SkillIntent;
import com.aispeech.export.Vocab;
import com.aispeech.export.config.AICloudSemanticConfig;
import com.aispeech.export.intent.AICloudSemanticIntent;
import com.aispeech.export.itn.Convert;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AIUpdateListener;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.config.CloudDMConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.dm.update.UpdaterImpl;
import com.aispeech.lite.param.CInfoParams;
import com.aispeech.lite.param.CloudSemanticParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.semantic.CloudSemanticProcessor;

import org.json.JSONArray;

import java.io.File;

/**
 * 云端识别引擎(含语义)
 *
 * @deprecated 不再维护, 推荐使用 AICloudDMEngine
 */
public class AICloudSemanticEngine {

    private static final String TAG = "AICloudSemanticEngine";

    private final CloudSemanticProcessor mProcessor;

    private CloudDMConfig mCloudAsrConfig;

    private CloudSemanticParams mParams;

    private LocalVadConfig mLocalVadConfig;

    private VadParams mVadParams;

    private CInfoParams mCInfoParams;

    private InnerEngine mInnerEngine;

    private UpdaterImpl mUpdaterImpl;

    private AICloudSemanticEngine() {
        this.mProcessor = new CloudSemanticProcessor();
        mCloudAsrConfig = new CloudDMConfig();
        mParams = new CloudSemanticParams();
        mLocalVadConfig = new LocalVadConfig();
        mVadParams = new VadParams();
        mCInfoParams = new CInfoParams();
        mInnerEngine = new InnerEngine();
    }

    /**
     * 创建实例
     *
     * @return AICloudSemanticEngine 实例
     */
    public static AICloudSemanticEngine createInstance() {
        return new AICloudSemanticEngine();
    }

    /**
     * 初始化云端识别引擎。
     *
     * @param config   初始化参数 {@link AICloudSemanticConfig}
     * @param listener 引擎回调 {@link AIASRListener}
     */
    public void init(AICloudSemanticConfig config, AIASRListener listener) {
        mInnerEngine.init(listener);
        parseConfig(config);
        mUpdaterImpl = new UpdaterImpl.Builder()
                .setV1Host(CloudDMConfig.CINFO_SERVER)
                .setWebSocketHost(CloudDMConfig.DDS_SERVER)
                .setAliasKey(mCloudAsrConfig.getAliasKey())
                .build();
        mProcessor.init(mInnerEngine, mCloudAsrConfig, mLocalVadConfig);
    }

    /**
     * 解析初始化参数
     *
     * @param config {@link AICloudSemanticConfig }
     */
    private void parseConfig(AICloudSemanticConfig config) {
        mCloudAsrConfig.setUseCustomFeed(config.isUseCustomFeed());
        mLocalVadConfig.setVadEnable(config.isUseVad());
        if (!config.getVadRes().startsWith("/")) {
            mLocalVadConfig.setAssetsResNames(new String[]{config.getVadRes()});
            mLocalVadConfig.setResBinPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.getVadRes());
        } else {
            mLocalVadConfig.setResBinPath(config.getVadRes());
        }
        mCloudAsrConfig.setAliasKey(config.getAliasKey());//设置产品分支
        mCloudAsrConfig.setServerAddress(config.getServerAddress());//设置服务地址
        mCloudAsrConfig.setUseRefText(config.isUseRefText());//纯语义模式
    }

    /**
     * 启动引擎
     *
     * @param intent 启动参数
     */
    public void start(AICloudSemanticIntent intent) {
        if (mProcessor != null) {
            parseIntent(intent);
            mProcessor.start(mParams, mVadParams);
        }
    }

    /**
     * 解析启动参数
     *
     * @param intent {@link AICloudSemanticIntent}
     */
    private void parseIntent(AICloudSemanticIntent intent) {
        mParams.setEnableNluNbest(intent.isEnableNBest());
        mParams.setEnablePunctuation(intent.isEnablePunctuation());
        mParams.setRealBack(intent.isUseRealBack());
        mParams.setMaxSpeechTimeS(intent.getMaxSpeechTimeS());
        mParams.setNoSpeechTimeout(intent.getNoSpeechTimeOut());
        mParams.setSessionId(intent.getSessionId());
        mParams.setWakeupWord(intent.getWakeupWords());
        mParams.setSaveAudioPath(intent.getSaveAudioPath());
        mVadParams.setSaveAudioPath(intent.getSaveAudioPath());
        mVadParams.setPauseTime(intent.getPauseTime());
        mParams.setAIType(intent.getType());
        mParams.setRefText(intent.getRefText());
        mParams.setSkillId(intent.getSkillId());
        mParams.setTask(intent.getTask());
        mParams.setEnableNumberConvert(intent.isEnableNumberConvert());
        mParams.setUserId(intent.getUserId());
        mParams.setEnableRecUppercase(intent.isEnableRecUppercase());
        mParams.setEnableVocabsConvert(intent.isEnableVocabsConvert());
        if (intent.getResourceType() != null) {
            mParams.setRes(intent.getResourceType().value);
        }
//        mParams.setEnableAudioDetection(intent.isEnableAudioDetection());
//        mParams.setEnableEmotion(intent.isEnableEmotion());
//        mParams.setEnableAlignment(intent.isEnableAlignment());
    }


    /**
     * 主动结束语义
     */
    public void stop() {
        if (mProcessor != null) {
            mProcessor.close();
        }
    }


    /**
     * 停止录音，等待识别结果
     */
    public void stopRecording() {
        if (mProcessor != null) {
            mProcessor.stop();
        }
    }


    /**
     * 取消本次识别操作
     */
    public void cancel() {
        if (mProcessor != null) {
            mProcessor.cancel();
        }
        if (mInnerEngine != null) {
            mInnerEngine.removeCallbackMsg();
        }
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     */
    public void feedData(byte[] data) {
        if (mCloudAsrConfig != null && !mCloudAsrConfig.isUseCustomFeed()) {
            Log.df(TAG, "useCustomFeed is not enabled，ignore data");
            return;
        }
        if (mProcessor != null) {
            mProcessor.feedData(data, data.length);
        }
    }

    /**
     * 销毁云端识别引擎
     */
    public void destroy() {
        if (mProcessor != null) {
            mProcessor.release();
        }
        if (mInnerEngine != null) {
            mInnerEngine.release();
            mInnerEngine = null;
        }
        if (mVadParams != null) {
            mVadParams = null;
        }
        if (mParams != null) {
            mParams = null;
        }
        if (mCInfoParams != null) {
            mCInfoParams = null;
        }
        if (mCloudAsrConfig != null) {
            mCloudAsrConfig = null;
        }
        if (mLocalVadConfig != null) {
            mLocalVadConfig = null;
        }
        if (mUpdaterImpl != null) {
            mUpdaterImpl = null;
        }
    }


    /**
     * 更新技能配置，调整技能优先级
     * 须在start启动成功后可以调用
     *
     * @param skills 技能排序列表 ["skillId1" , ""skillId2"","skillId3"]
     * @see #updateProductContext(AIUpdateListener, ProductContext)
     * @deprecated 已废弃, 使用下面的接口
     */
    @Deprecated
    public void updateSkillPriority(JSONArray skills) {
        if (mProcessor != null) {
            String params = mCInfoParams.toSkillPriorityJSON(skills).toString();
            mProcessor.update(params);
        }
    }


    /**
     * 更新产品配置
     *
     * @param listener {@link AIUpdateListener}
     * @param context  {@link ProductContext}
     *                 usage: http://car.aispeech.com/duilite/docs/duilite/yu-yin-shi-bie/56-yun-duan-dui-hua.html
     */
    public void updateProductContext(AIUpdateListener listener, ProductContext context) {
        if (mUpdaterImpl != null) {
            mUpdaterImpl.updateProductContext(listener, context);
        }
    }


    /**
     * 上传联系人接口
     *
     * @param addOrDelete 删除还是增加
     * @param data        联系人数据 ["张三","李四"]
     * @see #updateVocabs(AIUpdateListener, Vocab...)
     * @deprecated 已废弃, 使用下面的新接口替换
     */
    @Deprecated
    public void updateContact(boolean addOrDelete, JSONArray data) {
        if (mProcessor != null) {
            String params = mCInfoParams.toVocabsContactJSON(addOrDelete, data).toString();
            mProcessor.updateVocab(params);
        }
    }

    /**
     * 更新词库接口
     * <p>
     * 更新指定词库的词条。
     *
     * @param listener {@link AIUpdateListener} 上传词库结果回调监听
     * @param vocabs   {@link Vocab} 需要更新的词库列表
     */
    public void updateVocabs(AIUpdateListener listener, Vocab... vocabs) {
        if (mParams.isEnableVocabsConvert()) {
            vocabs = Convert.getInstance().encodeVocabs(vocabs);
        }
        if (mUpdaterImpl != null) {
            mUpdaterImpl.updateVocabs(listener, vocabs);
        }
    }

    /**
     * 主动触发意图
     *
     * @param intent {@link SkillIntent}
     */
    public void triggerIntent(SkillIntent intent) {
        if (intent != null) {
            mProcessor.triggerIntent(intent, mParams, mVadParams);
        }
    }


    private static class InnerEngine extends BaseInnerEngine {

        private AIASRListener mListener;

        @Override
        public void release() {
            super.release();
            if (mListener != null)
                mListener = null;
        }

        void init(AIASRListener listener) {
            super.init(listener);
            mListener = listener;
        }

        @Override
        protected void callbackInMainLooper(CallbackMsg msg, Object obj) {

            switch (msg) {
                case MSG_RESULTS:
                    mListener.onResults((AIResult) obj);
                    break;
                case MSG_BEGINNING_OF_SPEECH:
                    mListener.onBeginningOfSpeech();
                    break;
                case MSG_END_OF_SPEECH:
                    mListener.onEndOfSpeech();
                    break;
                case MSG_RMS_CHANGED:
                    mListener.onRmsChanged((Float) obj);
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onResults(AIResult result) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RMS_CHANGED, rmsdB);
        }

        @Override
        public void onBeginningOfSpeech() {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_BEGINNING_OF_SPEECH, null);
        }

        @Override
        public void onEndOfSpeech() {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_END_OF_SPEECH, null);
        }


        @Override
        public void onConnect(boolean isConnected) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_CONNECT_STATE, isConnected);
        }
    }


}
