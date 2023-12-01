package com.aispeech.lite.asr;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.DUILiteConfig;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.auth.AIAuthEngine;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AITimer;
import com.aispeech.common.AudioHelper;
import com.aispeech.common.JSONResultParser;
import com.aispeech.common.LimitQueue;
import com.aispeech.common.Log;
import com.aispeech.common.NetworkUtil;
import com.aispeech.export.interceptor.AsrInterceptor;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.LocalAsrConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.CloudASRParams;
import com.aispeech.lite.param.LocalAsrParams;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.vad.VadKernel;
import com.aispeech.lite.vad.VadKernelListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

/**
 * Created by yuruilong on 2017/5/19.
 */

public class AsrProcessor extends BaseProcessor {
    public static final String MODEL_LOCAL = "LocalAsr";
    public static final String MODEL_CLOUD = "CloudAsr";
    /**
     * cloud asr 带上声纹信息
     */
    public static final String MODEL_CLOUD_PLUS = "CloudAsrPlus";
    // asr cache ===>
    private static final int STATUS_CACHE_ASR_CLOSE = 0;
    private static final int STATUS_CACHE_ASR_OPEN = 1;
    private String tag = "";
    private BaseKernel mAsrKernel;
    private SpeechParams mAsrParams;
    private AIEngineConfig mAsrConfig;
    private VadKernel mVadKernel;
    private VadParams mVadParams;
    private LocalVadConfig mVadConfig;
    private String mModelName;
    /**
     * cloud asr 超时任务
     */
    private AsrTimeoutTimer asrTimeoutTimer = null;
    /**
     * 0 关闭 不能cache, 1 打开 可以cache, 2 send cache to asr, 3 关闭 并清除数据
     */
    private int asrCacheStatus = STATUS_CACHE_ASR_CLOSE;
    /**
     * 每次数据 3200，即 0.1s，长度100可以缓存10s的音频
     */
    private LimitQueue<byte[]> asrCacheQueue = new LimitQueue<>(100);
    /**
     * 识别出空字符串的次数
     */
    private int emptyTimes = 0;
    /**
     * oneshot 功能优化，当用户说 唤醒词+命令词 时，vad在唤醒词后即结束，导致asr识别结果是空，
     * 可打开此功能，此功能会保留唤醒词后vad结束后的音频，即命令词的音频，然后重新asr识别
     */
    private boolean oneshotOptimization = false;
    /**
     * 要使 oneshot 功能优化生效，不仅要打开{@link #oneshotOptimization},还要相关的配置设置正确
     */
    private boolean oneshotOptimizationSettingRight = true;

    /**
     * 识别结果为空的次数，区别于emptyTimes，emptyTimes是用于oneshot优化方案，该参数是用于普通的asr结果，
     * 配合{@link com.aispeech.export.intent.AICloudASRIntent#setIgnoreEmptyResult(boolean)}或者
     * {@link com.aispeech.export.intent.AICloudASRIntent#setIgnoreEmptyResult(boolean, int)}使用
     */
    private int noResultCnt = 0;

    private long mVadBeginTime;
    private long mVadEndTime;
    private long mAsrResultTime;

    public void init(SpeechListener listener, AIEngineConfig asrConfig, LocalVadConfig vadConfig, String modelName) {
        this.mModelName = modelName;
        this.mVadConfig = vadConfig;
        tag = modelName + "Processor";
        if (mVadConfig.isVadEnable()) {
            threadCount++;
        }
        if (MODEL_LOCAL.equals(mModelName)) {
            mScope = Scope.LOCAL_ASR;
        } else {
            mScope = Scope.CLOUD_MODEL;
        }
        init(listener, asrConfig.getContext(), tag);

        // 认证非法的时候已经在init回调回去了，防止授权失败重复init导致的内存溢出
        if (mProfileState == null || !mProfileState.isValid()) return;

        if (modelName.equals(MODEL_LOCAL)) {
            mAsrConfig = asrConfig;
            mAsrKernel = new LocalAsrKernel("lgram", new MyAsrKernelListener());
            mAsrKernel.setMaxMessageQueueSize(mAsrConfig.getMaxMessageQueueSize());
        } else if (modelName.equals(MODEL_CLOUD)) {
            mAsrConfig = asrConfig;
            mAsrKernel = new CloudAsrKernel(new MyAsrKernelListener());
            mAsrKernel.setProfile(AIAuthEngine.getInstance().getProfile());
            mAsrKernel.setMaxMessageQueueSize(mAsrConfig.getMaxMessageQueueSize());
        } else if (MODEL_CLOUD_PLUS.equals(modelName)) {
            mAsrConfig = asrConfig;
            mAsrKernel = new CloudAsrPlusKernel(new MyAsrKernelListener());
            mAsrKernel.setProfile(AIAuthEngine.getInstance().getProfile());
            mAsrKernel.setMaxMessageQueueSize(mAsrConfig.getMaxMessageQueueSize());
        }
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }

    public void start(SpeechParams asrParams, VadParams vadParams) {
        if (mProfileState != null && mProfileState.isValid()) {
            this.mAsrParams = asrParams;
            this.mVadParams = vadParams;
            if (mAsrParams instanceof CloudASRParams && ((CloudASRParams) mAsrParams).getRes().equals("custom") && TextUtils.isEmpty(((CloudASRParams) mAsrParams).getLmId())) {
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, new AIError(AIError.ERR_SERVICE_PARAMETERS,
                        AIError.ERR_DESCRIPTION_ERR_SERVICE_PARAMETERS));
                Log.e(tag, "error: " + AIError.ERR_DESCRIPTION_ERR_SERVICE_PARAMETERS);
                return;
            }
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    @Override
    public void clearObject() {
        super.clearObject();
        if (mAsrKernel != null)
            mAsrKernel = null;
        if (mAsrParams != null)
            mAsrParams = null;
        if (mAsrConfig != null)
            mAsrConfig = null;
        if (mVadKernel != null)
            mVadKernel = null;
        if (mVadParams != null)
            mVadParams = null;
        if (mVadConfig != null)
            mVadConfig = null;
    }

    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                handleMsgNew();
                break;
            case MSG_START:
                handleMsgStart();
                break;
            case MSG_RECORDER_START:
                handleMsgRecorderStart();
                break;
            case MSG_STOP:
                handleMsgStop();
                break;
            case MSG_CANCEL:
                handleMsgCancel();
                break;
            case MSG_RAW_RECEIVE_DATA:
                final byte[] rawBufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING && mOutListener != null) {
                    mOutListener.onRawDataReceived(rawBufferData, rawBufferData.length);
                }
                break;
            case MSG_RESULT_RECEIVE_DATA:
                //此MSG包含录音机/自定义feed音频来源
                handleMsgResultData(msg);
                break;
            case MSG_VAD_RECEIVE_DATA:
                final byte[] vadData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    mAsrKernel.feed(vadData);
                }
                break;
            case MSG_VAD_START:
                handleMsgVadStart();
                break;
            case MSG_VAD_END:
                handleMsgVadEnd();
                break;
            case MSG_VOLUME_CHANGED:
                handleMsgVolumeChanged(msg);
                break;
            case MSG_UPDATE://离线语义引擎update消息仅用来更新net.bin文件
                if (mState != EngineState.STATE_IDLE) {
                    String config = (String) msg.obj;
                    if (mAsrKernel != null) {
                        mAsrKernel.update(config);
                    }
                } else {
                    trackInvalidState("update");
                }
                break;
            case MSG_RESULT:
                handleMsgResult(msg);
                break;
            case MSG_UPDATE_RESULT:
                if (mState != EngineState.STATE_IDLE) {
                    Integer ret = (Integer) msg.obj;
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, ret);
                } else {
                    trackInvalidState("update_result");
                }
                break;
            case MSG_RELEASE:
                handleMsgRelease();
                break;
            case MSG_ERROR:
                handleMsgError(msg);
                break;
            default:
                break;
        }
    }

    private void handleMsgResultData(Message msg) {
        final byte[] bufferData = (byte[]) msg.obj;
        if (mState == EngineState.STATE_RUNNING) {
            if (mVadKernel != null && mVadParams.isVadEnable()) {//送vad模块，vad处理后再送asr
                if (!cacheAudio(bufferData)) {
                    if (mAsrConfig.isEnableDoubleVad()) {
                        byte[] byte0 = AudioHelper.splitOriginalChannel(bufferData, 0, 2);
                        byte[] byte1 = AudioHelper.splitOriginalChannel(bufferData, 1, 2);
                        mVadKernel.feed(new byte[][]{byte0, byte1});
                    } else {
                        mVadKernel.feed(bufferData);
                    }
                }
            } else {
                mAsrKernel.feed(bufferData);
            }
            if (mOutListener != null) {
                mOutListener.onResultDataReceived(bufferData, bufferData.length, 0);
            }
        }
    }

    private void handleMsgError(Message msg) {
        cancelAsrTimeoutTimer();
        AIError error = (AIError) msg.obj;
        if (TextUtils.isEmpty(error.getRecordId())) {
            error.setRecordId(Utils.getRecorderId());
        }
        if (error.getErrId() == AIError.ERR_RES_PREPARE_FAILED) {
            Log.w(tag, error.toString());
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
            uploadError(error);
            return;
        }
        if (mState == EngineState.STATE_IDLE) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
            return;
        }
        if (mState != EngineState.STATE_NEWED && mState != EngineState.STATE_IDLE) {
            unRegisterRecorderIfIsRecording(AsrProcessor.this);
            transferState(EngineState.STATE_NEWED);
            if (error.getErrId() == AIError.ERR_TIMEOUT_ASR || mAsrKernel instanceof CloudAsrPlusKernel)
                mAsrKernel.cancelKernel(); // cloud asr 才处理 cancelKerneld
            else
                mAsrKernel.stopKernel();
            if (mVadKernel != null && mVadParams.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            Log.w(tag, error.toString());
            uploadError(error);
            if (error.getErrId() == AIError.ERR_DNS) {
                error.setErrId(AIError.ERR_NETWORK);
                error.setError(AIError.ERR_DESCRIPTION_ERR_NETWORK);
            }
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, msg.obj);
        } else {
            trackInvalidState("error");
        }
    }

    private void handleMsgRelease() {
        if (mState != EngineState.STATE_IDLE) {
            cancelAsrTimeoutTimer();
            if (mState == EngineState.STATE_RUNNING) {
                unRegisterRecorderIfIsRecording(AsrProcessor.this);
                //修复内存溢出问题
                if (mAsrKernel != null) {
                    mAsrKernel.stopKernel();
                }
                if (mVadConfig.isVadEnable()) {
                    mVadKernel.stopKernel();
                }
            }
            cancelNoSpeechTimer();
            mAsrKernel.releaseKernel();
            mAsrKernel = null;
            if (mVadKernel != null) {
                mVadKernel.releaseKernel();
                mVadKernel = null;
            }
            clearObject();//清除实例
            transferState(EngineState.STATE_IDLE);
        } else {
            trackInvalidState("release");
        }
    }

    private void handleMsgResult(Message msg) {
        cancelAsrTimeoutTimer();
        AIResult result = (AIResult) msg.obj;
        if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
            mAsrResultTime = System.currentTimeMillis();
            Log.d(tag,"VAD.BEGIN.ASR.RESULT.DELAY : "+(mAsrResultTime-mVadBeginTime));
            Log.d(tag,"VAD.END.ASR.RESULT.DELAY : "+(mAsrResultTime-mVadEndTime));
            //和oneshot优化相比，空文本重试的优先级较低，优先处理oneshot优化的逻辑
            if (!stopCacheAudioWhenGotResult(result) && !checkRetryWhenEmptyResult(result)) {
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
                if (result.isLast()) {
                    transferState(EngineState.STATE_NEWED);
                    unRegisterRecorderIfIsRecording(AsrProcessor.this);
                }
            }
        } else {
            trackInvalidState("result");
        }
    }

    private boolean checkRetryWhenEmptyResult(AIResult result) {
        if (mAsrParams instanceof CloudASRParams && ((CloudASRParams) mAsrParams).isIgnoreEmptyResult()
                && result != null && result.isLast()) {
            String json = (result != null && result.getResultObject() != null) ? result.getResultObject().toString() : "";
            JSONResultParser parser = new JSONResultParser(json);
            if ("".equals(parser.getText()) && (noResultCnt < ((CloudASRParams) mAsrParams).getIgnoreEmptyResultCounts())) {
                if (mAsrParams.isUseOneShot()) {
                    //oneshot返回结果为空，则设置为非oneshot返回结果
                    mAsrParams.setUseOneShotFunction(false);
                    Log.d(tag, "oneshot return empty result, set it as not oneshot");
                }
                noResultCnt ++;
                Log.d(tag, "checkRetryWhenEmptyResult:noResultCnt " + noResultCnt);
                if (!mAsrParams.isUseCustomFeed()) {
                    if (initRecorder()) {
                        return true;
                    }
                } else {
                    unRegisterRecorderIfIsRecording(this);
                }
                startKernelOrRecorder();
                return true;
            }
        }
        return false;
    }

    private void handleMsgVolumeChanged(Message msg) {
        float rmsDb = (float) msg.obj;
        if (mState == EngineState.STATE_RUNNING) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RMS_CHANGED, rmsDb);
        } else {
            trackInvalidState("volume changed");
        }
    }

    private void handleMsgVadEnd() {
        if (mState == EngineState.STATE_RUNNING) {
            Log.d(tag, "VAD.END");
            mVadEndTime = System.currentTimeMillis();
            cancelAsrTimeoutTimer();
            if (mAsrParams.isUseOneShot()) {
                handleVadEndOneshot();
            } else {
                startAsrTimeoutTimer();
                unRegisterRecorderIfIsRecording(AsrProcessor.this);
                mAsrKernel.stopKernel();
                if (mVadKernel != null && mVadParams.isVadEnable()) {
                    mVadKernel.stopKernel();
                }
                transferState(EngineState.STATE_WAITING);
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_END_OF_SPEECH, null);
            }
        } else {
            trackInvalidState("VAD.END");
        }
    }

    private void handleVadEndOneshot() {
        Log.d(tag, "use one shot");
        //使用oneshot
        long currentTime = System.currentTimeMillis();
        long intervalTime = currentTime - mAsrParams.getWakeupTime();
        Log.d(tag, "interval time is : " + intervalTime);
        Log.d(tag, "interval thresh time is : " + mAsrParams.getOneShotIntervalTime());
        if (intervalTime < mAsrParams.getOneShotIntervalTime()) {
            //小于阈值，认为不是oneshot, 则取消本次识别
            unRegisterRecorderIfIsRecording(AsrProcessor.this);
            mAsrKernel.cancelKernel();
            if (mVadKernel != null && mVadParams.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_NEWED);
            Log.d(tag, "not one shot");
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_NOT_ONE_SHOT, null);
        } else {
            startAsrTimeoutTimer();
            mAsrKernel.stopKernel();
            if (mVadKernel != null && mVadParams.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            if (!startCacheAudioAfterVadEnd()) {
                //大于阈值，直接用本次识别结果，结果中带唤醒词
                unRegisterRecorderIfIsRecording(AsrProcessor.this);
                transferState(EngineState.STATE_WAITING);
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_END_OF_SPEECH, null);
            }
        }
    }

    private void handleMsgVadStart() {
        if (mState == EngineState.STATE_RUNNING) {
            Log.d(tag, "VAD.BEGIN");
            mVadBeginTime = System.currentTimeMillis();
            cancelNoSpeechTimer();
            startMaxSpeechTimerTask(mAsrParams);
            if (!oneshotOptimization || emptyTimes == 0)
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_BEGINNING_OF_SPEECH, null);
        } else {
            trackInvalidState("VAD.BEGIN");
        }
    }

    private void handleMsgCancel() {
        if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING
                || mState == EngineState.STATE_NEWED) {
            cancelAsrTimeoutTimer();
            unRegisterRecorderIfIsRecording(AsrProcessor.this);
            if (mAsrParams instanceof CloudASRParams) {
                //主动调用cancel,不在重新起start
                ((CloudASRParams) mAsrParams).setIgnoreEmptyResult(false);
            }
            mAsrKernel.cancelKernel();
            if (mVadKernel != null && mVadParams != null && mVadParams.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("cancel");
        }
    }

    private void handleMsgStop() {
        if (mState == EngineState.STATE_RUNNING) {
            cancelAsrTimeoutTimer();
            if (!mVadParams.isVadEnable())
                startAsrTimeoutTimer();

            unRegisterRecorderIfIsRecording(AsrProcessor.this);
            if (mAsrParams instanceof CloudASRParams) {
                //主动调用stop,不在重新起start
                ((CloudASRParams) mAsrParams).setIgnoreEmptyResult(false);
            }
            mAsrKernel.stopKernel();
            if (mVadKernel != null && mVadParams.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_WAITING);
        } else {
            trackInvalidState("stop");
        }
    }

    private void handleMsgRecorderStart() {
        if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
            mAsrKernel.startKernel(mAsrParams);
            if (mVadKernel != null && mVadParams.isVadEnable()) {
                startNoSpeechTimer(mAsrParams);
                mVadKernel.startKernel(mVadParams);
            }
            transferState(EngineState.STATE_RUNNING);
        } else {
            trackInvalidState("recorder start");
        }
    }

    private void handleMsgStart() {
        if (mState == EngineState.STATE_NEWED) {
            //试用检查判断
            if (mModelName.equals(MODEL_LOCAL) && !updateTrails(mProfileState)) {
                return;
            }
            syncRecorderId(mAsrParams, mVadParams);
            mCallbackState = EngineState.STATE_RUNNING;
            if (mAsrParams instanceof CloudASRParams) {
                ((CloudASRParams) mAsrParams).setOneshotOptimizationSecond(false);
                setOneshotOptimization(((CloudASRParams) mAsrParams).isOneshotOptimization());
            }
            if (!mAsrParams.isUseCustomFeed()) {
                if (initRecorder()) return;
            } else {
                unRegisterRecorderIfIsRecording(this);
            }
            noResultCnt = 0;
            resetAsrCacheStatus();
            startKernelOrRecorder();
        } else {
            trackInvalidState("start");
        }
    }

    private void startKernelOrRecorder() {
        if (mAsrParams.isUseCustomFeed()) {
            Log.i(tag, "isUseCustomFeed");
            mAsrKernel.startKernel(mAsrParams);
            if (mVadKernel != null && mVadParams.isVadEnable()) {
                startNoSpeechTimer(mAsrParams);
                mVadKernel.startKernel(mVadParams);
            }
            transferState(EngineState.STATE_RUNNING);
        } else {
            // 启动SDK内部录音机
            startRecorder(mAsrParams, AsrProcessor.this);
        }
    }

    private boolean initRecorder() {
        if (mAIRecorder == null) {
            if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_MIC ||//音频来源于AudioRecorder节点
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
                mAIRecorder = createRecorder(AsrProcessor.this);
                if (mAIRecorder == null) {
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                            AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE));
                    return true;
                }
            } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_DUAL ||//音频来源于信号处理引擎节点
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_FESPCAR ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_FESPCAR4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_DUAL ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_LINE4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_LINE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE4) {
                if (mAsrParams.getFespxEngine() == null) {
                    throw new RuntimeException("need to setFespxEngine before start engine");
                }
                mAIRecorder = createSignalProcessingRecorder(mAsrParams.getFespxEngine().getFespxProcessor());
                if (mAIRecorder == null) {
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                            AIError.ERR_SIGNAL_PROCESSING_NOT_STARTED,
                            AIError.ERR_DESCRIPTION_SIGNAL_PROCESSING_NOT_STARTED));
                    return true;
                }
            }
        }
        return false;
    }

    private void handleMsgNew() {
        if (mState == EngineState.STATE_IDLE) {
            int status = copyAssetsRes(mAsrConfig);
            if (status == AIConstant.OPT_FAILED) {
                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                return;
            }
            if (mVadConfig.isVadEnable()) {
                status = copyAssetsRes(mVadConfig);
                if (status == AIConstant.OPT_FAILED) {
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                            AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                    return;
                }
                mVadKernel = new VadKernel("asr",new MyVadKernelListener());
                mVadKernel.setMaxMessageQueueSize(mAsrConfig.getMaxMessageQueueSize());
                mVadKernel.newKernel(mVadConfig);
            }
            mAsrKernel.newKernel(mAsrConfig);
        } else {
            trackInvalidState("new");
        }
    }
    // asr cache <===

    private void resetAsrCacheStatus() {
        asrCacheStatus = STATUS_CACHE_ASR_CLOSE;
        asrCacheQueue.clear();
        emptyTimes = 0;
        oneshotOptimizationSettingRight = isAsrCacheSettingRight();
        Log.i(tag, "oneshotOptimizationSettingRight " + oneshotOptimizationSettingRight);
    }

    /**
     * 检测相关配置是否正确，start时调用。动态改变 OneShot 未考虑。<br>
     * 需打开以下设置 cloudASR 唤醒词并过滤，使用 OneShot 和 Vad
     *
     * @return true 配置正确，false OTHERS
     */
    private boolean isAsrCacheSettingRight() {
        if (mAsrParams instanceof CloudASRParams) {
            CloudASRParams cloudASRParams = (CloudASRParams) mAsrParams;
            JSONArray jsonArray = cloudASRParams.getCustomWakeupWord();
            return cloudASRParams.isVisibleWakeupWord() && jsonArray != null && jsonArray.length() > 0
                    && mAsrParams != null && mAsrParams.isUseOneShot() && mVadParams != null && mVadParams.isVadEnable();
        } else
            return false;
    }

    /**
     * 缓存音频
     *
     * @param bytes 录音机/自定义feed音频数据
     * @return true 缓存音频，false 不缓存
     */
    private boolean cacheAudio(byte[] bytes) {
        if (!oneshotOptimization || !oneshotOptimizationSettingRight)
            return false;

        if (asrCacheStatus == STATUS_CACHE_ASR_OPEN) {
            asrCacheQueue.offer(bytes);
            Log.i(tag, "cacheAudio " + asrCacheQueue.size());
            return true;
        } else
            return false;
    }

    /**
     * 初始化前设置有效
     *
     * @param oneshotOptimization
     */
    private void setOneshotOptimization(boolean oneshotOptimization) {
        if (mState == EngineState.STATE_IDLE || mState == EngineState.STATE_NEWED) {
            this.oneshotOptimization = oneshotOptimization;
            Log.i(tag, "setOneshotOptimization " + oneshotOptimization);
        } else
            Log.i(tag, "setOneshotOptimization mState " + mState);
    }

    /**
     * 开启音频缓存
     *
     * @return true 开启成功，false 不能开启，开关未打开、唤醒词未过滤等
     */
    private boolean startCacheAudioAfterVadEnd() {
        if (!oneshotOptimization || !oneshotOptimizationSettingRight)
            return false;

        // start cache audio
        boolean restTimes = emptyTimes == 0;
        asrCacheStatus = restTimes ? STATUS_CACHE_ASR_OPEN : STATUS_CACHE_ASR_CLOSE;
        Log.i(tag, "startCacheAudioAfterVadEnd restTimes " + restTimes);
        return restTimes;
    }

    /**
     * 对asr识别结果进行处理，如果语句结束，识别出的结果为空说明音频只含有唤醒词
     *
     * @param result asr 识别结果
     * @return true 对缓存音频及后续录音机/feed音频进行识别，false 不识别缓存音频,走正常流程
     */
    private boolean stopCacheAudioWhenGotResult(AIResult result) {
        if (!oneshotOptimization || !oneshotOptimizationSettingRight)
            return false;

        String json = (result != null && result.getResultObject() != null) ?
                result.getResultObject().toString() : "";
        JSONResultParser parser = new JSONResultParser(json);
        boolean recognitionCache = mAsrKernel instanceof CloudAsrPlusKernel ? parser.getEof() == 2 : parser.getEof() == 1;
        if (!recognitionCache)
            return false;  // 说明是中间结果

        // 只做一次空字符串时识别缓存
        recognitionCache = recognitionCache && emptyTimes == 0;
        Log.i(tag, "stopCacheAudioWhenGotResult recognitionCache:" + recognitionCache + " emptyTimes " + emptyTimes);

        // 识别结果为空说明音频里只有唤醒词，或者只识别出1个字，仍然会认为是没有命令词
        if (recognitionCache) {
            recognitionCache = parser.getText() == null || parser.getText().length() < 2;
        }

        recognitionCache = calRecognitionCache(json, parser, recognitionCache);

        return recognitionCache;
    }

    private boolean calRecognitionCache(String json, JSONResultParser parser, boolean recognitionCache) {
        if (emptyTimes == 0 && parser.getText() != null && !recognitionCache
                && mAsrParams != null && mAsrParams instanceof CloudASRParams) {
            // 第一次识别不为空的情况下，再做一些过滤
            CloudASRParams cloudASRParams = (CloudASRParams) mAsrParams;
            Log.d(tag, "isVisibleWakeupWord " + cloudASRParams.isVisibleWakeupWord());
            if (cloudASRParams.isVisibleWakeupWord() && cloudASRParams.getOneshotOptimizationFilterWords() != null
                    && cloudASRParams.getOneshotOptimizationFilterWords().length > 0) {
                String result1 = parser.getText();
                for (String s : cloudASRParams.getOneshotOptimizationFilterWords()) {
                    if (!TextUtils.isEmpty(s)) {
                        result1 = result1.replace(s, "");
                    }
                }
                recognitionCache = result1.length() < 2;// 肯定不为null
                Log.d(tag, "result1: " + result1);
            }
        }
        Log.i(tag, "recognitionCache " + recognitionCache);
        Log.i(tag, "stopCacheAudioWhenGotResult json " + json);
        // stop cache audio
        asrCacheStatus = STATUS_CACHE_ASR_CLOSE;

        if (recognitionCache) {
            // 相同配置再次启动asr
            emptyTimes++;

            handleKernelStart();
            Log.i(tag, "asrCacheQueue.size " + asrCacheQueue.size());
            byte[] data;
            while ((data = asrCacheQueue.poll()) != null) {
                sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT_RECEIVE_DATA, data);
            }

        } else {
            asrCacheQueue.clear();
            // oneshot优化的情况下，第一次就有识别结果，那么同时回调 onEndOfSpeech()
            if (emptyTimes == 0)
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_END_OF_SPEECH, null);
        }
        return recognitionCache;
    }

    private void handleKernelStart() {
        if (mAsrParams instanceof CloudASRParams) {
            ((CloudASRParams) mAsrParams).setOneshotOptimizationSecond(true);
        }
        mAsrKernel.startKernel(mAsrParams);
        if (mVadKernel != null && mVadParams.isVadEnable()) {
            startNoSpeechTimer(mAsrParams);
            mVadKernel.startKernel(mVadParams);
        }
    }

    private void uploadError(AIError aiError) {
        if ((aiError.getErrId() == AIError.ERR_NETWORK || aiError.getErrId() == AIError.ERR_DNS)
                && !NetworkUtil.isNetworkConnected(AISpeech.getContext())) {
            Log.d(tag, "network is not connected, ignore upload error");
            return;
        }
        String recordId = aiError.getRecordId();
        if (TextUtils.isEmpty(recordId)) {
            recordId = Utils.getRecorderId();
        }
        //添加message外面的字段
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("recordId", recordId);
        entryMap.put("mode", "lite");
        if (mModelName.equals(MODEL_CLOUD)) {
            entryMap.put("module", "cloud_exception");
            AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("cloud_asr_exception", "info", "cloud_exception",
                    recordId, aiError.getInputJSON(), aiError.getOutputJSON(), entryMap);
        } else if (mModelName.equals(MODEL_LOCAL)) {
            entryMap.put("module", "local_exception");
            JSONObject input = new JSONObject();
            try {
                if (mAsrConfig != null) {
                    input.put("config", ((LocalAsrConfig) mAsrConfig).toJson());
                }
                if (mAsrParams != null) {
                    input.put("param", ((LocalAsrParams) mAsrParams).toJSON());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("local_asr_exception", "info", "local_exception",
                    recordId, input, aiError.getOutputJSON(), entryMap);
        }
    }


    @Override
    public void processNoSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_NO_SPEECH,
                AIError.ERR_DESCRIPTION_NO_SPEECH));
        Log.w(tag, "no speech timeout!");
    }

    @Override
    public void processMaxSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                AIError.ERR_MAX_SPEECH, AIError.ERR_DESCRIPTION_MAX_SPEECH));
    }

    /**
     * 只有云端asr并且不是实时反馈才会有超时检测
     */
    private void startAsrTimeoutTimer() {
        if (asrTimeoutTimer != null) {
            asrTimeoutTimer.cancel();
            asrTimeoutTimer = null;
        }
        int timeout = mAsrParams.getWaitingTimeout();
        if (timeout <= 0 || !MODEL_CLOUD.equals(mModelName))
            return;
        if (mAsrParams instanceof CloudASRParams && ((CloudASRParams) mAsrParams).isEnableRealTimeFeedBack()) {
            return;
        }
        asrTimeoutTimer = new AsrTimeoutTimer();
        try {
            AITimer.getInstance().schedule(asrTimeoutTimer, timeout);
            Log.d(tag, "startAsrTimeoutTimer and timeout is : " + timeout + "ms");
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void cancelAsrTimeoutTimer() {
        if (asrTimeoutTimer != null) {
            asrTimeoutTimer.cancel();
            asrTimeoutTimer = null;
            Log.d(tag, "cancelAsrTimeoutTimer");
        }
    }

    /**
     * asr回调
     */
    private class MyAsrKernelListener implements AsrKernelListener {

        @Override
        public void onInit(int status) {
            Log.i(tag, "MyAsrKernelListener onInit : " + status);
            processInit(status);
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onResults(AIResult result) {
            try {
                JSONObject customObj = new JSONObject().put(IInterceptor.Name.LOCAL_ASR_RESULT, result);
                JSONObject inputObj = AsrInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.RECEIVE, customObj);
                SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.LOCAL_ASR_RESULT, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, result);
        }

        @Override
        public void onStarted(String recordId) {
            //do nothing
        }

        @Override
        public void onUpdateResult(int ret) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE_RESULT, Integer.valueOf(ret));
        }
    }

    /**
     * vad模块回调
     */
    private class MyVadKernelListener implements VadKernelListener {

        @Override
        public void onInit(int status) {
            Log.i(tag, "MyVadKernelListener onInit : " + status);
            processInit(status);
        }

        @Override
        public void onVadStart(String recordId) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_START, null);
        }

        @Override
        public void onVadEnd(String recordId) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_END, null);
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

    private class AsrTimeoutTimer extends TimerTask {
        @Override
        public void run() {
            if (mAsrKernel instanceof CloudAsrKernel) {
                String recordId = ((CloudAsrKernel) mAsrKernel).getRecordId();
                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_TIMEOUT_ASR,
                        AIError.ERR_DESCRIPTION_TIMEOUT_ASR, recordId));
                Log.w(tag, "AsrTimeoutTimer ERR_TIMEOUT_ASR");
            } else
                Log.w(tag, "AsrTimeoutTimer do nothing");
        }
    }

}
