package com.aispeech.export.engines2;

import com.aispeech.base.BaseInnerEngine;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.Command;
import com.aispeech.export.DmInfo;
import com.aispeech.export.MultiModal;
import com.aispeech.export.NativeApi;
import com.aispeech.export.ProductContext;
import com.aispeech.export.Setting;
import com.aispeech.export.SkillContext;
import com.aispeech.export.SkillIntent;
import com.aispeech.export.Speaker;
import com.aispeech.export.Vocab;
import com.aispeech.export.config.AICloudDMConfig;
import com.aispeech.export.intent.AICloudDMIntent;
import com.aispeech.export.itn.Convert;
import com.aispeech.export.listeners.AICloudDMListener;
import com.aispeech.export.listeners.AIUpdateListener;
import com.aispeech.export.widget.callback.CallbackWidget;
import com.aispeech.export.widget.callback.CallbackWidgetType;
import com.aispeech.export.widget.feedback.FeedbackWidget;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.CloudDMConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.dm.CloudDmFullDuplexProcessor;
import com.aispeech.lite.dm.CloudDmProcessor;
import com.aispeech.lite.dm.IDmProcessor;
import com.aispeech.lite.dm.Protocol;
import com.aispeech.lite.dm.update.UpdaterImpl;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.param.CloudSemanticParams;
import com.aispeech.lite.param.VadParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 云端对话引擎
 *
 * @author hehr
 */
public class AICloudDMEngine extends BaseEngine {

    private IDmProcessor mProcessor;

    private CloudDMConfig mCloudAsrConfig;

    private CloudSemanticParams mParams;

    private LocalVadConfig mLocalVadConfig;

    private VadParams mVadParams;

    private UpdaterImpl mUpdaterImpl;

    private InnerEngine mInnerEngine;

    private AICloudDMEngine() {
        mCloudAsrConfig = new CloudDMConfig();
        mParams = new CloudSemanticParams();
        mLocalVadConfig = new LocalVadConfig();
        mVadParams = new VadParams();
        mInnerEngine = new InnerEngine();
    }

    @Override
    public String getTag() {
        return "cloud_dm";
    }

    /**
     * 创建引擎实例，支持多实例
     *
     * @return {@link AICloudDMEngine}
     */
    public static AICloudDMEngine createInstance() {
        return new AICloudDMEngine();
    }


    /**
     * 初始化云端识别引擎。
     *
     * @param config   {@link AICloudDMConfig}
     * @param listener 语音识别回调接口
     */
    public void init(AICloudDMConfig config, AICloudDMListener listener) {
        super.init();
        mInnerEngine.init(listener);
        parseConfig(config);//解析初始化参数
        mUpdaterImpl = new UpdaterImpl.Builder()
                //.setV1Host(CloudDMConfig.CINFO_SERVER)
                //.setWebSocketHost(CloudDMConfig.DDS_SERVER)
                .setV1Host(mCloudAsrConfig.getCInfoServerAddress())
                .setWebSocketHost(mCloudAsrConfig.getServerAddress())
                .setAliasKey(mCloudAsrConfig.getAliasKey())
                .setFullDuplex(config.isUseFullDuplex())//是否开启全双工
                .build();
        mProcessor = config.isUseFullDuplex() ? (new CloudDmFullDuplexProcessor()) : (new CloudDmProcessor());
        mProcessor.init(mInnerEngine, mCloudAsrConfig, mLocalVadConfig);
        if (mProcessor instanceof BaseProcessor) {
            mBaseProcessor = (BaseProcessor) mProcessor;
        }
    }

    /**
     * 创建对话
     *
     * @param intent {@link AICloudDMIntent}
     */
    public void start(AICloudDMIntent intent) {
        super.start();
        parseIntent(intent);//解析启动参数
        if (mProcessor != null) {
            if (!mCloudAsrConfig.isUseRefText()) {
                mProcessor.start(mParams, mVadParams);
            } else {
                mProcessor.startWithText(mParams, mVadParams);
            }
        }
    }

    /**
     * 创建对话,使用默认的对话配置
     */
    public void start() {
        this.start(new AICloudDMIntent());
    }

    /**
     * 解析启动参数
     *
     * @param intent {@link AICloudDMIntent}
     */
    private void parseIntent(AICloudDMIntent intent) {
        super.parseIntent(intent, mParams, mVadParams);
        mParams.setMaxSpeechTimeS(intent.getMaxSpeechTimeS());
        mParams.setNoSpeechTimeout(intent.getNoSpeechTimeOut());
        mParams.setOneshotCache(intent.getOneshotCache());
        mParams.setVadPauseTime(intent.getCloudVadPauseTime());
        mVadParams.setPauseTime(intent.getPauseTime());
        mVadParams.setSaveAudioPath(intent.getSaveAudioPath());
        mParams.setSaveAudioPath(intent.getSaveAudioPath());
        mParams.setWakeupWord(intent.getStrWakeupWords());
        mParams.setSessionId(intent.getSessionId());
        mParams.setEnableAlignment(intent.isEnableAlignment());
        mParams.setEnableVAD(intent.isEnableVAD());
        mParams.setEnableEmotion(intent.isEnableEmotion());
        mParams.setEnableAudioDetection(intent.isEnableAudioDetection());
        mParams.setEnablePunctuation(intent.isEnablePunctuation());
        mParams.setRealBack(intent.isUseRealback());
        mParams.setEnableNumberConvert(intent.isEnableNumberConvert());
        mParams.setEnableRecUppercase(intent.isEnableRecUppercase());
        mParams.setEnableVocabsConvert(intent.isEnableVocabsConvert());
        mParams.setRefText(intent.getRefText());
        mParams.setUserId(intent.getUserId());
        mParams.setEnableTone(intent.isEnableTone());
        mParams.setCustomWakeupScore(intent.getCustomWakeupScore());
        mParams.setAIType(intent.getAIType());
        mParams.setEnableCloudVAD(intent.isEnableCloudVAD());
        mParams.setEnableVAD(intent.isEnableVAD());
        mParams.setEnableShowText(intent.isEnableShowText());
        mParams.setUseCustomFeed(intent.isUseCustomFeed());
        mParams.setRes(intent.getRes());
    }

    /**
     * 解析初始化参数
     *
     * @param config {@link AICloudDMConfig}
     */
    private void parseConfig(AICloudDMConfig config) {
        super.parseConfig(config, mCloudAsrConfig);
        mCloudAsrConfig.setDMRoute(config.isRoute());
        mCloudAsrConfig.setAliasKey(config.getAliasKey());
        mCloudAsrConfig.setNativeApiTimeout(config.getNativeApiTimeout());
        mCloudAsrConfig.setServerAddress(config.getServerAddress());
        mCloudAsrConfig.setCInfoServerAddress(config.getCInfoServerAddress());
        mLocalVadConfig.setVadEnable(config.isUseVad());
        mCloudAsrConfig.setUseRefText(config.isUseRefText());//纯语义模式
        mCloudAsrConfig.setUseFullDuplex(config.isUseFullDuplex());//全双工模式
        mCloudAsrConfig.setUseFullDuplexNoSpeechTimeOut(config.isUseFullDuplexNoSpeechTimeOut());
        mCloudAsrConfig.setKeys(config.getKeys());
        mCloudAsrConfig.setValues(config.getValues());
        mCloudAsrConfig.setConnectTimeout(config.getConnectTimeout());
        mLocalVadConfig.setVadEnable(config.isUseVad());

        if (config.isUseFullDuplex()) {
            mLocalVadConfig.setFullMode(true);//全双工模式下，启用vad常开模式
        }
        if (!config.getVadRes().startsWith("/")) {
            mLocalVadConfig.setAssetsResNames(new String[]{config.getVadRes()});
            mLocalVadConfig.setResBinPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + config.getVadRes());
        } else {
            mLocalVadConfig.setResBinPath(config.getVadRes());
        }
    }

    /**
     * 主动结束对话
     */
    public void stop() {
        super.stop();
        if (mProcessor != null) {
            mProcessor.close();
        }

        if (mInnerEngine != null) {
            mInnerEngine.removeCallbackMsg();
        }

    }

    /**
     * 开始录音, 使用外部 VAD 场景，在 VAD_START 时调用
     */
    public void notifyVadStart() {
        Log.i(TAG, "notifyVadStart");
        if (mProcessor != null) {
            mProcessor.startRecording();
        }
    }

    /**
     * 停止录音，等待对话结果 , 使用外部 VAD 场景，在 VAD_END 时调用
     */
    public void notifyVadEnd() {
        Log.i(TAG, "notifyVadEnd");
        if (mProcessor != null) {
            mProcessor.stop();
        }
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     * @see AICloudDMIntent#setUseCustomFeed(boolean)
     */
    public void feedData(byte[] data) {
        if (mParams != null && !mParams.isUseCustomFeed()) {
            Log.df(TAG, "useCustomFeed is not enabled，ignore data");
            return;
        }
        if (mProcessor != null) {
            mProcessor.feedData(data, data.length);
        }
    }

    /**
     * 外部通知引擎NLG播报已完成
     */
    public void notifyNlgEnd() {
        Log.i(TAG, "notifyNlgEnd");
        if (mProcessor != null) {
            mProcessor.notifyNlgEnd();
        }
    }

    /**
     * 销毁云端识别引擎
     */
    public void destroy() {
        super.destroy();
        if (mProcessor != null) {
            mProcessor.release();
        }
        if (mInnerEngine != null) {
            mInnerEngine.release();
            mInnerEngine = null;
        }
        if (mUpdaterImpl != null) {
            mUpdaterImpl = null;
        }
        if (mCloudAsrConfig != null) {
            mCloudAsrConfig = null;
        }
        if (mParams != null) {
            mParams = null;
        }
        if (mLocalVadConfig != null) {
            mLocalVadConfig = null;
        }
        if (mVadParams != null) {
            mVadParams = null;
        }
    }

    /**
     * 回复对话结果
     *
     * @param widget {@link FeedbackWidget}
     */
    public void feedback(FeedbackWidget widget) {
        if (widget != null) {
            mProcessor.feedback(widget);
        }
    }

    /**
     * 终端回复对话结果（注意：该接口仅供私有云来调用）
     *
     * @param topic 回复主题
     * @param data  回复结果，为 JSON 字符串
     */
    public void feedback2PRIVCloud(String topic, String data) {
        if (mProcessor != null) {
            mProcessor.feedback2PRIVCloud(topic, data);
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
        if (mUpdaterImpl != null && vocabs != null) {
            mUpdaterImpl.updateVocabs(listener, vocabs);
        }
    }

    /**
     * 更新设备状态，产品级的配置。比如：定位信息，设备硬件状态等
     *
     * @param listener {@link AIUpdateListener}
     * @param context  {@link ProductContext}
     */
    public void updateProductContext(AIUpdateListener listener, ProductContext context) {
        mInnerEngine.setUpdateListener(listener);
        if (mCloudAsrConfig.isUseFullDuplex()) {
            ((CloudDmFullDuplexProcessor) mProcessor).uploadProductContext(context);
        } else {
            ((CloudDmProcessor) mProcessor).uploadProductContext(context);
        }
    }

    /**
     * 更新技能配置
     *
     * @param listener {@link AIUpdateListener}
     * @param context  {@link SkillContext}
     */
    public void updateSkillContext(AIUpdateListener listener, SkillContext context) {
        mInnerEngine.setUpdateListener(listener);
        if (mCloudAsrConfig.isUseFullDuplex()) {
            ((CloudDmFullDuplexProcessor) mProcessor).uploadSkillContext(context);
        } else {
            ((CloudDmProcessor) mProcessor).uploadSkillContext(context);
        }
    }

    /**
     * 主动触发意图
     *
     * @param intent {@link SkillIntent}
     */
    public void triggerIntent(SkillIntent intent) {
        Log.i(TAG, "triggerIntent");
        if (intent != null) {
            mProcessor.triggerIntent(intent, mParams, mVadParams);
        }
    }


    /**
     * 主动触发意图
     *
     * @param skill  技能名称， 必填
     * @param task   任务名称， 必填
     * @param intent 意图名称， 必填
     * @param slots  语义槽， key-value Json， 可选
     * @deprecated Use {@link AICloudDMEngine#triggerIntent(SkillIntent)} instead.
     */
    public void triggerIntent(String skill, String task, String intent, String slots) {
        triggerIntent(new SkillIntent(skill, task, intent, slots));
    }

    /**
     * 同步多模态数据
     *
     * @param multiModal {@link MultiModal}
     */
    public void async(MultiModal multiModal) {
        Log.i(TAG, "async");
        if (multiModal != null) {
            mProcessor.async(multiModal);
        }
    }

    /**
     * 指定技能调度黑名单列表，DM中控调度时对于指定的技能不参与调度
     *
     * @param listener  {@link AIUpdateListener}
     * @param skillList 过滤技能列表
     * @param option    操作类型
     * @see ProductContext#OPTION_SET
     * @see ProductContext#OPTION_DELETE
     */
    public void setExcludeDispatch(AIUpdateListener listener, String[] skillList, String option) {
        if (!mCloudAsrConfig.isUseFullDuplex()) {
            Log.e(TAG, "---setExcludeDispatch() Valid only in full duplex mode!");
            return;
        }
        mInnerEngine.setUpdateListener(listener);
        JSONArray skillArray = new JSONArray();
        if (skillList != null) {
            skillArray = new JSONArray(Arrays.asList(skillList));
        }
        ((CloudDmFullDuplexProcessor) mProcessor).uploadProductContext(new ProductContext.Builder()
                .setOption(option)
                .setSetting(new Setting(Setting.EXCLUDE_DISPATCH_SKILL_LIST, skillArray))
                .build());
    }


    /**
     * 开启调度后过滤；技能都参与调度，命中技能在该名单中，则过滤该 skillId，如：闲聊
     *
     * @param listener  {@link AIUpdateListener}
     * @param skillList 过滤技能列表
     * @param enable    是否开启拒识过滤开关 true 开启，false 关闭
     */
    public void setRejectAfterDispatch(AIUpdateListener listener, String[] skillList, boolean enable) {
        if (!mCloudAsrConfig.isUseFullDuplex()) {
            Log.e(TAG, "---enableRejectAfterDispatch() Valid only in full duplex mode!");
            return;
        }
        Log.i(TAG, "setRejectAfterDispatch");
        mInnerEngine.setUpdateListener(listener);

        JSONArray skillArray = new JSONArray();
        if (skillList != null) {
            skillArray = new JSONArray(Arrays.asList(skillList));
        }
        List<Setting> settings = new ArrayList<>();
        settings.add(new Setting(Setting.FULL_DUPLEX_FILTER_SWITCH, enable ? "on" : "off"));
        settings.add(new Setting(Setting.FILTER_SKILL_LIST, skillArray));

        ((CloudDmFullDuplexProcessor) mProcessor).uploadProductContext(new ProductContext.Builder()
                .setOption(ProductContext.OPTION_SET)
                .setSettings(settings)
                .build());
    }


    /**
     * 告知识别引擎已经唤醒，该接口在oneshot功能中使用，内部会记录唤醒的时间点，
     * 之后在vad end的时候来判断到底用户说的是不是唤醒词+指令，还是只有唤醒词
     *
     * @see AICloudDMIntent#setOneshotCache(OneshotCache)
     */
    public void notifyWakeup() {
        Log.i(TAG, "notifyWakeup");
        mParams.setWakeupTime(System.currentTimeMillis());
    }

    /**
     * 返回当前连接状态
     *
     * @return true isConnected
     */
    public boolean isConnected() {
        if (mProcessor != null) {
            return mProcessor.isConnected();
        }
        return false;
    }

    private static class InnerEngine extends BaseInnerEngine {

        private AICloudDMListener mListener;
        private AIUpdateListener mUpdateListener;//更新配置的回调

        @Override
        public void release() {
            super.release();
            if (mListener != null)
                mListener = null;
        }

        void init(AICloudDMListener listener) {
            super.init(listener);
            mListener = listener;
        }

        void setUpdateListener(AIUpdateListener updateListener) {
            this.mUpdateListener = updateListener;
        }

        @Override
        protected void callbackInMainLooper(CallbackMsg msg, Object obj) {
            switch (msg) {
                case MSG_DM_ASR:
                    Map asr = (Map) obj;
                    if (mListener != null) {
                        mListener.onAsr((boolean) asr.get("isLast"), (String) asr.get("text"));
                    }
                    break;
                case MSG_DM_END:
                    if (mListener != null) {
                        mListener.onEnd((String) obj);
                    }
                    break;
                case MSG_DM_DISPLAY:
                    Map display = (Map) obj;
                    if (mListener != null) {
                        mListener.onDisplay(CallbackWidgetType.getWidgetTypeByInt((int) display.get("type")), (CallbackWidget) display.get("callbackWidget"));
                    }
                    break;
                case MSG_DM_CALL:
                    if (mListener != null) {
                        mListener.onCall((Command) obj);
                    }
                    break;
                case MSG_DM_QUERY:
                    if (mListener != null) {
                        mListener.onQuery((NativeApi) obj);
                    }
                    break;
                case MSG_DM_PLAY:
                    if (mListener != null) {
                        mListener.onPlay((Speaker) obj);
                    }
                    break;
                case MSG_BEGINNING_OF_SPEECH:
                    if (mListener != null) {
                        mListener.onBeginningOfSpeech();
                    }
                    break;
                case MSG_RMS_CHANGED:
                    if (mListener != null) {
                        mListener.onRmsChanged((Float) obj);
                    }
                    break;
                case MSG_END_OF_SPEECH:
                    if (mListener != null) {
                        mListener.onEndOfSpeech();
                    }
                    break;
                case MSG_UPDATE_RESULT:
                    if (mUpdateListener != null) {
                        if ((Boolean) obj) {
                            mUpdateListener.success();
                        } else {
                            mUpdateListener.failed();
                        }
                    }
                    break;
                case MSG_DM_RESULT:
                    String dmResult = (String) obj;
                    try {
                        JSONObject retObj = new JSONObject(dmResult);
                        if (mListener != null) {
                            // 部分许可字段通过接口形式对外释放
                            DmInfo dmInfo = new DmInfo();
                            dmInfo.setSessionId(retObj.optString(Protocol.SESSION_ID));
                            dmInfo.setRecordId(retObj.optString(Protocol.RECORDER_ID));
                            dmInfo.setSkillId(retObj.optString(Protocol.SKILL_ID));
                            dmInfo.setSkill(retObj.optString(Protocol.SKILL));

                            JSONObject dmObj = retObj.optJSONObject(Protocol.DM);
                            if (dmObj.has(Protocol.DM_END_SKILL_DM)) {
                                boolean endSkillDm = dmObj.optBoolean(Protocol.DM_END_SKILL_DM);
                                dmInfo.setEndSkillDm(endSkillDm);
                            }
                            if (retObj.has(Protocol.ERROR)) {
                                JSONObject errObj = retObj.optJSONObject(Protocol.ERROR);
                                dmInfo.setErrId(errObj.optString(Protocol.ERROR_ID));
                            }
                            mListener.onDmResult(dmInfo);
                        }
                    } catch (JSONException exception) {
                        exception.printStackTrace();
                    }
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onQuery(NativeApi api) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_DM_QUERY, api);
        }

        @Override
        public void onCall(Command command) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_DM_CALL, command);
        }

        @Override
        public void onPlay(Speaker speaker) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_DM_PLAY, speaker);
        }

        @Override
        public void onDisplay(int type, CallbackWidget callbackWidget) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_DM_DISPLAY, optExtra("type", type, "callbackWidget", callbackWidget));
        }

        @Override
        public void onAsr(boolean isLast, String text) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_DM_ASR, optExtra("isLast", isLast, "text", text));
        }

        @Override
        public void onEnd(String sessionId) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_DM_END, sessionId);
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
        public void onUpdateContext(boolean isSuccess) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, isSuccess);
        }

        @Override
        public void onHasDmResult(String result) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_DM_RESULT, result);
        }

        @Override
        public void onConnect(boolean isConnected) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_CONNECT_STATE, isConnected);
        }
    }
}
