package com.aispeech.lite.hotword;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.common.AIConstant;
import com.aispeech.common.FileSaveUtil;
import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;
import com.aispeech.common.NetworkUtil;
import com.aispeech.export.config.RecorderConfig;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Engines;
import com.aispeech.lite.Scope;
import com.aispeech.lite.asr.AsrKernelListener;
import com.aispeech.lite.asr.LocalAsrKernel;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.LocalAsrConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.config.SSLConfig;
import com.aispeech.lite.hotword.ssl.SSLKernel;
import com.aispeech.lite.hotword.ssl.SSLKernelListener;
import com.aispeech.lite.param.LocalAsrParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.vad.VadKernel;
import com.aispeech.lite.vad.VadKernelListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * 热词引擎processor
 *
 * @author hehr
 */
public class HotWordsProcessor extends BaseProcessor {

    private static final String TAG = "HotWordsProcessor";

    private SpeechListener mListener;

    private AIEngineConfig mAsrConfig;

    private SSLConfig mVadConfig;

    private LocalAsrKernel mAsrKernel;

    private VadKernel mVadKernel;

    private SSLKernel mSslKernel;

    private VadParams mVadParams;

    private LocalAsrParams mAsrParams;

    private long mVadBeginTime;
    private long mVadEndTime;
    private long mAsrResultTime;

    /***
     * 内存泄漏问题修复后删除
     *
     * 防止内存泄漏,当热词引擎启动超过100次以后，release --init -- start.
     */
    private long mCount = 0;

    /**
     * 保存触发的音频
     */
    private LinkedList<byte[]> mAudioQueue;

    /**
     * 初始化
     *
     * @param listener  {@link SpeechListener}
     * @param asrConfig {@link LocalAsrConfig}
     * @param vadConfig {@link LocalVadConfig}
     */
    public void init(SpeechListener listener, LocalAsrConfig asrConfig, SSLConfig vadConfig) {

        mListener = listener;
        mAsrConfig = asrConfig;
        mVadConfig = vadConfig;
        mScope = Scope.LOCAL_ASR;

        if (vadConfig.isVadEnable() || vadConfig.isUseSSL())
            threadCount++;

        //init(listener, asrConfig.getContext(), TAG, "asr-" + asrConfig.getScope());
        init(listener, asrConfig.getContext(), TAG);

        mAsrKernel = new LocalAsrKernel("hotword", new AsrListenerImpl());
        mAsrKernel.setProfile(mProfile);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }

    /**
     * 启动引擎
     *
     * @param asrParams {@link LocalAsrParams}
     * @param vadParams {@link VadParams}
     */
    public void start(LocalAsrParams asrParams, VadParams vadParams) {
        if (isAuthorized()) {
            this.mAsrParams = asrParams;
            this.mVadParams = vadParams;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage();
        }
    }

    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                if (mState == EngineState.STATE_IDLE) {
                    int status = copyAssetsFolders(mAsrConfig);
                    if (status == AIConstant.OPT_FAILED) {
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                        break;
                    }
                    if (mVadConfig.isUseSSL()) {
                        status = copyAssetsRes(mVadConfig);
                        if (status == AIConstant.OPT_FAILED) {
                            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                            break;
                        }

                        mSslKernel = new SSLKernel(new VadListenerImpl());
                        mSslKernel.newKernel(mVadConfig);

                    } else if (mVadConfig.isVadEnable()) {
                        status = copyAssetsRes(mVadConfig);
                        if (status == AIConstant.OPT_FAILED) {
                            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                            break;
                        }
                        mVadKernel = new VadKernel("hotWord", new VadListenerImpl());
//                        新增的监测kernel层阻塞的watchdog机制，调试可以打开，后续可以考虑开放配置
//                        mVadKernel.enableWatchDog();
                        mVadKernel.newKernel(mVadConfig);
                    }
                    checkSaveAudio();
                    mAsrKernel.newKernel(mAsrConfig);
//                    mAsrKernel.enableWatchDog();
                } else {
                    trackInvalidState("new");
                }
                break;
            case MSG_START:
                if (mState == EngineState.STATE_NEWED) {
                    syncRecorderId(mAsrParams, mVadParams);
                    checkSaveAudio();
                    if (mAudioQueue != null) mAudioQueue.clear();

                    if (mAsrParams.isUseCustomFeed()) {
                        Log.i(TAG, "isUseCustomFeed");
                        if (mCount > AISpeech.asrResetMaxSize) {
                            Log.w(TAG, "native count max size ,reset kernel !");
                            mCount = 0;
                            mAsrKernel.resetKernel();
                        } else {
                            mAsrKernel.startKernel(mAsrParams);
                        }
                        mCount++;
                        Log.d(TAG, "count " + mCount);

                        if (mVadConfig.isUseSSL()) {
                            startNoSpeechTimer(mAsrParams);
                            mSslKernel.startKernel(mVadParams);
                        } else if (mVadConfig.isVadEnable()) {
                            startNoSpeechTimer(mAsrParams);
                            mVadKernel.startKernel(mVadParams);
                        }

                        transferState(EngineState.STATE_RUNNING);
                    } else {
                        if (mAIRecorder == null) {
                            if (AISpeech.getRecoderType() == RecorderConfig.TYPE_COMMON_MIC ||//音频来源于AudioRecorder节点
                                    AISpeech.getRecoderType() == RecorderConfig.TYPE_COMMON_ECHO) {
                                mAIRecorder = createRecorder(this);
                                if (mAIRecorder == null) {
                                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                                            AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE));
                                    return;
                                }
                            }
                        }
                        // 启动SDK内部录音机
                        startRecorder(mAsrParams, this);
                    }
                } else {
                    trackInvalidState("start");
                }
                break;

            case MSG_RECORDER_START:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
                    if (mCount > 2000) {
                        Log.d(TAG, "native count max size ,release !");
                        mCount = 0;
                        mAsrKernel.resetKernel();
                    } else {
                        mAsrKernel.startKernel(mAsrParams);
                    }
                    mCount++;
                    Log.d(TAG, "count " + mCount);
                    if (mVadConfig.isUseSSL()) {
                        startNoSpeechTimer(mAsrParams);
                        mSslKernel.startKernel(mVadParams);
                    } else if (mVadConfig.isVadEnable()) {
                        startNoSpeechTimer(mAsrParams);
                        mVadKernel.startKernel(mVadParams);
                    }
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("recorder start");
                }
                break;

            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(this);
                    mAsrKernel.stopKernel();

                    if (mVadConfig.isUseSSL()) {
                        mSslKernel.stopKernel();
                    } else if (mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }

                    if (mAudioQueue != null) mAudioQueue.clear();
                    transferState(EngineState.STATE_WAITING);
                } else {
                    trackInvalidState("stop");
                }
                break;
            case MSG_CANCEL:
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    if (!mAsrConfig.isUseCustomFeed()) {
                        unRegisterRecorderIfIsRecording(this);
                    }
                    mAsrKernel.cancelKernel();

                    if (mVadConfig.isUseSSL()) {
                        mSslKernel.stopKernel();
                    } else if (mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("cancel");
                }
                break;
            case MSG_RAW_RECEIVE_DATA:
                byte[] rawBufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mListener != null) {
                        mListener.onRawDataReceived(rawBufferData, rawBufferData.length);
                    }
                }
                break;
            case MSG_RESULT_RECEIVE_DATA:
                //此MSG包含录音机/自定义feed音频来源
                byte[] bufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    if (mVadConfig.isUseSSL()) {
                        mSslKernel.feed(bufferData);
                    } else if (mVadConfig.isVadEnable()) {//送vad模块，vad处理后再送asr
                        mVadKernel.feed(bufferData);
                    } else {
                        mAsrKernel.feed(bufferData);
                    }
                    if (mListener != null) {
                        mListener.onResultDataReceived(bufferData, bufferData.length, 0);
                    }
                }
                break;
            case MSG_VAD_RECEIVE_DATA:
                byte[] vadData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    mAsrKernel.feed(vadData);
                }
                if (mAudioQueue != null) {
                    mAudioQueue.offer(vadData);
                }
                break;
            case MSG_VAD_START:
                if (mState == EngineState.STATE_RUNNING) {
                    Log.d(TAG, "VAD.BEGIN");
                    mVadBeginTime = System.currentTimeMillis();
                    cancelNoSpeechTimer();
                    startMaxSpeechTimerTask(mAsrParams);
                    if (mListener != null) {
                        mListener.onBeginningOfSpeech();
                    }
                } else {
                    trackInvalidState("VAD.BEGIN");
                }
                break;
            case MSG_VAD_END:
                if (mState == EngineState.STATE_RUNNING) {
                    Log.d(TAG, "VAD.END");
                    transferState(EngineState.STATE_WAITING);
                    mVadEndTime = System.currentTimeMillis();
                    unRegisterRecorderIfIsRecording(this);
                    mAsrKernel.stopKernel();
                    if (mVadConfig.isUseSSL()) {
                        mSslKernel.stopKernel();
                    } else if (mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }

                    if (mListener != null) {
                        mListener.onEndOfSpeech();
                    }
                } else {
                    trackInvalidState("VAD.END");
                }
                break;
            case MSG_VOLUME_CHANGED:
                float rmsDb = (float) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mListener != null) {
                        mListener.onRmsChanged(rmsDb);
                    }
                } else {
                    trackInvalidState("volume changed");
                }
                break;
            case MSG_DOA:
                int index = (int) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mListener != null) {
                        mListener.onSSL(index);
                    }
                }
                break;
            case MSG_RESULT:
                AIResult result = (AIResult) msg.obj;
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    mAsrResultTime = System.currentTimeMillis();
                    Log.d(TAG, "VAD.END.HOTWORDS.RESULT : " + (mAsrResultTime - mVadEndTime) + "ms");
                    Log.d(TAG, "VAD.BEGIN.HOTWORDS.RESULT : " + (mAsrResultTime - mVadBeginTime) + "ms");
                    if (mListener != null) {
                        mListener.onResults(result);
                    }
                    final String rec = JSONUtil.getQuietly(result.getResultJSONObject(), "rec").toString();
                    saveHotWordsAudio(rec);
                    if (result.isLast()) {
                        transferState(EngineState.STATE_NEWED);
                        unRegisterRecorderIfIsRecording(this);
                    }

                    if (mAsrParams.isUseContinuousRecognition())
                        reStart();//重新启动

                } else {
                    trackInvalidState("result");
                }
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(this);
                        mCount = 0;
                        if (mAsrKernel != null) {
                            mAsrKernel.stopKernel();
                        }
                        if (mVadConfig.isUseSSL()) {
                            mSslKernel.stopKernel();
                        } else if (mVadConfig.isVadEnable()) {
                            mVadKernel.stopKernel();
                        }
                    }
                    releaseRecorder();
                    cancelNoSpeechTimer();
                    mAsrKernel.releaseKernel();
                    mAsrKernel = null;
                    if (mVadKernel != null) {
                        mVadKernel.releaseKernel();
                        mVadKernel = null;
                    }
                    if (mSslKernel != null) {
                        mSslKernel.releaseKernel();
                        mSslKernel = null;
                    }
                    clearObject();//清除实例
                    transferState(EngineState.STATE_IDLE);
                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_ERROR:
                AIError error = (AIError) msg.obj;
                if (TextUtils.isEmpty(error.getRecordId())) {
                    error.setRecordId(Utils.get_recordid());
                }
                if (error.getErrId() == AIError.ERR_RES_PREPARE_FAILED) {
                    Log.w(TAG, error.toString());
                    if (mListener != null) {
                        mListener.onError(error);
                    }
                    uploadError(error);
                    return;
                }
                if (mState == EngineState.STATE_IDLE) {
                    if (mListener != null) {
                        mListener.onError(error);
                    }
                    uploadError(error);
                    return;
                }
                if (mState != EngineState.STATE_NEWED && mState != EngineState.STATE_IDLE) {

                    unRegisterRecorderIfIsRecording(this);

                    if (error.getErrId() == AIError.ERR_TIMEOUT_ASR)
                        mAsrKernel.cancelKernel(); // cloud asr 才处理 cancelKernel
                    else
                        mAsrKernel.stopKernel();

                    if (mVadConfig.isUseSSL()) {
                        mSslKernel.stopKernel();
                    } else if (mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                    Log.w(TAG, error.toString());
                    uploadError(error);
                    if (error.getErrId() == AIError.ERR_DNS) {
                        error.setErrId(AIError.ERR_NETWORK);
                        error.setError(AIError.ERR_DESCRIPTION_ERR_NETWORK);
                    }
                    if (mListener != null) {
                        mListener.onError(error);
                    }
                } else {
                    trackInvalidState("error");
                }
                break;
            default:
                break;
        }
    }

    private void checkSaveAudio() {
        if (mAudioQueue == null && Engines.isSavingEngineAudioEnable(Engines.HOT_WORDS)) {
            mAudioQueue = new LinkedList<>();
            Log.i(TAG, "init VoiceQueue");
        }
    }

    @Override
    public void processNoSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_NO_SPEECH,
                AIError.ERR_DESCRIPTION_NO_SPEECH));
    }

    @Override
    public void processMaxSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                AIError.ERR_MAX_SPEECH, AIError.ERR_DESCRIPTION_MAX_SPEECH));
    }

    @Override
    public void clearObject() {
        super.clearObject();
        if (mAudioQueue != null) {
            mAudioQueue.clear();
            mAudioQueue = null;
        }
    }

    /**
     * 重启本地识别内核
     */
    private void reStart() {
        transferState(EngineState.STATE_NEWED);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
    }

    private void uploadError(AIError aiError) {
        if ((aiError.getErrId() == AIError.ERR_NETWORK || aiError.getErrId() == AIError.ERR_DNS)
                && !NetworkUtil.isNetworkConnected(AISpeech.getContext())) {
            Log.d(TAG, "network is not connected, ignore upload error");
            return;
        }
        String recordId = aiError.getRecordId();
        if (TextUtils.isEmpty(recordId)) {
            recordId = Utils.get_recordid();
        }
        //添加message外面的字段
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("recordId", recordId);
        entryMap.put("mode", "lite");
        entryMap.put("module", "local_exception");
        JSONObject input = new JSONObject();

        try {
            if (mAsrConfig != null) {
                input.put("config", mAsrConfig.toJson());
            }
            if (mAsrParams != null) {
                input.put("param", mAsrParams.toJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("local_asr_exception", "info", "local_exception",
                recordId, input, aiError.getOutputJSON(), entryMap);

    }

    private class VadListenerImpl implements VadKernelListener, SSLKernelListener {

        @Override
        public void onInit(int status) {
            Log.i(TAG, "VadListenerImpl onInit : " + status);
            processInit(status);
        }

        @Override
        public void onVadStart(String recordID) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_START, null);
        }

        @Override
        public void onVadEnd(String recordID) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_END, null);
        }

        @Override
        public void onSsl(int index) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_DOA, index);
        }

        @Override
        public void onRmsChanged(float rmsDb) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VOLUME_CHANGED, rmsDb);
        }

        @Override
        public void onBufferReceived(byte[] data) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_RECEIVE_DATA, data);
        }

        @Override
        public void onResults(String result) {

        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onReadyForSpeech() {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }
    }

    private void saveHotWordsAudio(String asr) {
        if (mAudioQueue == null) return;
        FileSaveUtil fileSaveUtil = new FileSaveUtil();

        if (TextUtils.isEmpty(AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH)) {
            fileSaveUtil.init(new File(AISpeech.getContext().getCacheDir(), "hotwords").getAbsolutePath());
        } else {
            fileSaveUtil.init(new File(AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH, "hotwords").getAbsolutePath());
        }

        String realPath = fileSaveUtil.prepare(asr);
        Log.i(TAG, "saveHotWordsAudio: " + asr + " in :" + realPath);
        while (!mAudioQueue.isEmpty()) {
            byte[] data = mAudioQueue.poll();
            fileSaveUtil.feedTypeCustom(data);
        }
        fileSaveUtil.close();
    }

    private class AsrListenerImpl implements AsrKernelListener {


        /**
         * 本地识别结果置信度字段
         */
        private final String KEY_LOCAL_ASR_RESULT_CONF = "conf";

        /**
         * 本地识别结果热词字段
         */
        private final String KEY_LOCAL_ASR_RESULT_REC = "rec";

        /**
         * 识别非正常解码结束，此中场景下识别结果可直接丢弃
         */
        private final String KEY_LOCAL_ASR_RESULT_FORCE_OUT = "forceout";

        @Override
        public void onInit(int status) {
            Log.i(TAG, "AsrKernelListener onInit : " + status);
            processInit(status);
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onResults(AIResult result) {

            try {
                JSONObject resultObj = JSONUtil.build(result.getResultObject().toString());
                //识别非正常解码结束标记
                Object forceOutObj = JSONUtil.optQuietly(resultObj, KEY_LOCAL_ASR_RESULT_FORCE_OUT);
                //识别返回的阈值
                Object confObj = JSONUtil.getQuietly(resultObj, KEY_LOCAL_ASR_RESULT_CONF);
                //识别返回的热词
                Object recObj = JSONUtil.getQuietly(resultObj, KEY_LOCAL_ASR_RESULT_REC);
                String rec = recObj != null ? recObj.toString() : "";
                double conf = confObj == null ? 0 : Double.parseDouble(confObj.toString());

                resultObj.put("isReachThreshold", conf > getThreshold(rec, mAsrParams));
                result.setResultObject(resultObj);
                Log.d(TAG, "confObj" + conf + "Threshold" + getThreshold(rec, mAsrParams) + "IsIgnoreThreshold" + mAsrParams.getIsIgnoreThreshold() + "resultObj" + resultObj);

                if (mAsrParams.getIsIgnoreThreshold()) {
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, result);
                } else {
                    if (forceOutObj != null && Integer.parseInt(forceOutObj.toString()) == 1) {
                        Log.d(TAG, "DROP FORCE OUT HOT WORD " + result.getResultObject());
                        reStart();
                    } else if (conf < getThreshold(rec, mAsrParams)) {
                        Log.d(TAG, "DROP CONF NOT QUALIFIED HOT WORD " + result.getResultObject());
                        reStart();
                    } else {
                        if (!TextUtils.isEmpty(rec)) {
                            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, result);
                        } else {
                            Log.d(TAG, "DROP REC NOT QUALIFIED HOT WORD " + result.getResultObject());
                            reStart();
                        }
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "DROP NO CONF HOT WORD " + result.getResultObject());
                reStart();
            }
        }

        @Override
        public void onStarted(String recordId) {

        }

        @Override
        public void onUpdateResult(int ret) {

        }
    }


}
