package com.aispeech.lite.wakeupincrement;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.common.AIConstant;
import com.aispeech.common.FileIOUtils;
import com.aispeech.common.FileUtils;
import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;
import com.aispeech.common.NetworkUtil;
import com.aispeech.export.config.RecorderConfig;
import com.aispeech.export.widget.Scene;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.asr.AsrKernelListener;
import com.aispeech.lite.asr.LocalAsrKernel;
import com.aispeech.lite.config.LocalAsrConfig;
import com.aispeech.lite.config.LocalGrammarConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.config.SSLConfig;
import com.aispeech.lite.grammar.LocalGrammarKernel;
import com.aispeech.lite.grammar.LocalGrammarListener;
import com.aispeech.lite.hotword.ssl.SSLKernel;
import com.aispeech.lite.hotword.ssl.SSLKernelListener;
import com.aispeech.lite.param.LocalAsrParams;
import com.aispeech.lite.param.LocalGrammarParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.vad.VadKernel;
import com.aispeech.lite.vad.VadKernelListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 增强唤醒 processor
 *
 * @author wuhua
 */

public class WakeupIncrementProcessor extends BaseProcessor {
    public static final String TAG = "WakeupIncrementProcessor";

    private LocalAsrKernel mAsrKernel;
    private VadKernel mVadKernel;
    private SSLKernel mSslKernel;
    private LocalGrammarKernel localGrammarKernel;

    private LocalGrammarConfig mGrammarConfig;
    private VadParams mVadParams;
    private LocalAsrParams mAsrParams;

    private LocalAsrConfig mAsrConfig;
    private SSLConfig mVadConfig;
    private Scene mCurrentScene;

    private SpeechListener mWakeupIncrementListener;

    private long mVadBeginTime;
    private long mVadEndTime;
    private long mAsrResultTime;
    private long mCount = 0;

    private LinkedBlockingQueue<String> mGramBinFileQueue;

    /**
     * 初始化
     *
     * @param listener      {@link SpeechListener}
     * @param asrConfig     {@link LocalAsrConfig}
     * @param vadConfig     {@link LocalVadConfig}
     * @param grammarConfig {@link LocalGrammarConfig}
     */
    public void init(SpeechListener listener, LocalAsrConfig asrConfig, SSLConfig vadConfig, LocalGrammarConfig grammarConfig) {
        this.mWakeupIncrementListener = listener;
        this.mAsrConfig = asrConfig;
        this.mVadConfig = vadConfig;
        this.mGrammarConfig = grammarConfig;

        if (vadConfig.isVadEnable() || vadConfig.isUseSSL())
            threadCount++;

        mScope = "asr-increment-" + asrConfig.getScope();
        init(listener, asrConfig.getContext(), TAG);

        mAsrKernel = new LocalAsrKernel("wakeupIncrement", new AsrListenerImpl());
        mAsrKernel.setProfile(mProfile);

        localGrammarKernel = new LocalGrammarKernel(new MyLocalGrammarProcessorListener());

        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);

        mGramBinFileQueue = new LinkedBlockingQueue<String>(20);
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

    /***
     *  触发编译
     * @param params {@link LocalGrammarParams}
     */
    public void build(List<LocalGrammarParams> params) {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_BUILD, params);
        } else {
            showErrorMessage();
        }
    }

    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                if (mState == EngineState.STATE_IDLE) {
                    // init gram
                    if (copyAssetsRes(mGrammarConfig) == AIConstant.OPT_FAILED) {
                        Log.e(TAG, "copy gram res fail!!!");
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                        break;
                    }
                    localGrammarKernel.newKernel(mGrammarConfig);

                    // init asr
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
                        mVadKernel = new VadKernel("wakeupIncrement", new VadListenerImpl());
                        mVadKernel.newKernel(mVadConfig);
                    }
                    mAsrKernel.newKernel(mAsrConfig);

                } else {
                    trackInvalidState("new");
                }
                break;
            case MSG_UPDATE://离线语义引擎update消息仅用来更新net.bin文件
                if (mState != EngineState.STATE_IDLE) {
                    Log.d(TAG, "MSG UPDATE");
                    String config = (String) msg.obj;
                    if (mAsrKernel != null) {
                        mAsrKernel.update(config);
                    }
                } else {
                    trackInvalidState("update");
                }
                break;
            case MSG_BUILD:
                if (mState != EngineState.STATE_IDLE) {
                    //gram编译多个bin
                    if (localGrammarKernel != null) {
                        List<LocalGrammarParams> localGrammarParamsList = (List<LocalGrammarParams>) msg.obj;
                        localGrammarKernel.cancelKernel();//清空上一次还在build的操作
                        localGrammarKernel.startKernel(localGrammarParamsList);
                    }
                }
                break;
            case MSG_UPDATE_END:
                if (mWakeupIncrementListener != null) {
                    int ret = (int) msg.obj;
                    mWakeupIncrementListener.onSetScene(ret);
                    //更新成功，重启本地识别内核
                    if (ret == AIConstant.OPT_SUCCESS && null != mCurrentScene && mCurrentScene.isSlotBinFileExists()) {
                        if (!TextUtils.isEmpty(mCurrentScene.getExpandFnJsonPath()) && mAsrParams != null) {
                            mAsrParams.setExpandFnPath(mCurrentScene.getExpandFnJsonPath());//设置ExpandFnPath
                        }
                        reStart(false);
                    }

                }
                break;
            case MSG_BUILD_END:
                if (mState != EngineState.STATE_IDLE) {
                    transferState(EngineState.STATE_RUNNING);
                    try {
                        AIResult aiResult = (AIResult) msg.obj;
                        List<LocalGrammarParams> localGrammarParamsList = (List<LocalGrammarParams>) aiResult.resultObject;
                        if (localGrammarParamsList != null && localGrammarParamsList.size() > 0) {
                            ArrayList jsonSlotList = new ArrayList();
                            if (mAsrConfig != null && mWakeupIncrementListener != null) {
                                for (LocalGrammarParams params : localGrammarParamsList) {
                                    mCurrentScene = params.getScene();
                                    if (params.isBuildSuccess()) {//只有编译成功才会加入到expandFfn.json中
                                        if (!jsonSlotList.contains(params.getSlotName())) {
                                            jsonSlotList.add(params.getSlotName());
                                        }
                                        if (mGramBinFileQueue != null) {
                                            Log.i(TAG, "GramBinFileQueue size : " + mGramBinFileQueue.size() + " ,binPath : " + params.getOutputPath());
                                            boolean isOffer = mGramBinFileQueue.offer(params.getOutputPath());
                                            if (!isOffer) {
                                                String deleteFile = mGramBinFileQueue.poll();
                                                Log.i(TAG, "GramBinFileQueue is full , will delete file : " + deleteFile);
                                                FileUtils.deleteFile(deleteFile);
                                                mGramBinFileQueue.offer(params.getOutputPath());
                                            }
                                        }
                                    }
                                }
                                Log.i(TAG, "gennerateExpandFn，jsonSlotList :" + Arrays.toString(jsonSlotList.toArray()));
                                mWakeupIncrementListener.onGramResults("Gram编译成功：" + Arrays.toString(jsonSlotList.toArray()));
                                if (mCurrentScene != null) {
                                    //组装json文件
                                    String jsonContent = mCurrentScene.getExpandFnJson(jsonSlotList);
                                    FileUtils.createOrExistsFile(mCurrentScene.getExpandFnJsonPath());
                                    FileIOUtils.writeFileFromBytesByStream(mCurrentScene.getExpandFnJsonPath(), jsonContent.getBytes());
                                    Log.i(TAG, "gennerateExpandFn，json file and content :" + mCurrentScene.getExpandFnJsonPath() + " \n  " + jsonContent);
                                    mAsrConfig.setNetBinPath(mCurrentScene.getNetBinPath());
                                    /**
                                     * 1.这里不能调用stop，因为stop会抛识别结果上来，重新Start引擎，此时会报无法启动引擎错误
                                     * 2.跟新引擎的时候，需要先cancel,因为asr running状态无法跟新引擎，会报跟新失败
                                     * */
                                    if (mAsrParams != null) {
                                        cancel();
                                        update(mAsrConfig.toJson().toString());
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                break;
            case MSG_START:
                handleMsgStart();
                break;

            case MSG_RECORDER_START:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
                    if (mCount > AISpeech.asrResetMaxSize) {
                        Log.w(TAG, "native count max size ,reset kernel !");
                        mCount = 0;
                        if (mAsrParams != null)
                            mAsrKernel.resetKernel();
                    } else {
                        if (mAsrParams != null)
                            mAsrKernel.startKernel(mAsrParams);
                    }
                    mCount++;
                    if (mVadConfig.isUseSSL()) {
                        if (mAsrParams != null) {
                            startNoSpeechTimer(mAsrParams);
                        }
                        if (mVadParams != null) {
                            mSslKernel.startKernel(mVadParams);
                        }
                    } else if (mVadConfig.isVadEnable()) {
                        if (mAsrParams != null) {
                            startNoSpeechTimer(mAsrParams);
                        }
                        if (mVadParams != null) {
                            mVadKernel.startKernel(mVadParams);
                        }
                    }
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("recorder start");
                }
                break;

            case MSG_STOP:
                handleMsgStop();
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
                    if (mWakeupIncrementListener != null) {
                        mWakeupIncrementListener.onRawDataReceived(rawBufferData, rawBufferData.length);
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
                    if (mWakeupIncrementListener != null) {
                        mWakeupIncrementListener.onResultDataReceived(bufferData, bufferData.length, 0);
                    }
                }
                break;
            case MSG_VAD_RECEIVE_DATA:
                byte[] vadData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    mAsrKernel.feed(vadData);
                }
                break;
            case MSG_VAD_START:
                if (mState == EngineState.STATE_RUNNING) {
                    Log.d(TAG, "VAD.BEGIN");
                    mVadBeginTime = System.currentTimeMillis();
                    cancelNoSpeechTimer();
                    startMaxSpeechTimerTask(mAsrParams);
                    if (mWakeupIncrementListener != null) {
                        mWakeupIncrementListener.onBeginningOfSpeech();
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

                    if (mWakeupIncrementListener != null) {
                        mWakeupIncrementListener.onEndOfSpeech();
                    }
                } else {
                    trackInvalidState("VAD.END");
                }
                break;
            case MSG_VOLUME_CHANGED:
                float rmsDb = (float) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mWakeupIncrementListener != null) {
                        mWakeupIncrementListener.onRmsChanged(rmsDb);
                    }
                } else {
                    trackInvalidState("volume changed");
                }
                break;
            case MSG_DOA:
                int index = (int) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mWakeupIncrementListener != null) {
                        mWakeupIncrementListener.onSSL(index);
                    }
                }
                break;
            case MSG_RESULT:
                AIResult result = (AIResult) msg.obj;
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    mAsrResultTime = System.currentTimeMillis();
                    Log.d(TAG, "VAD.END.HOTWORDS.RESULT : " + (mAsrResultTime - mVadEndTime) + "ms");
                    Log.d(TAG, "VAD.BEGIN.HOTWORDS.RESULT : " + (mAsrResultTime - mVadBeginTime) + "ms");
                    if (mWakeupIncrementListener != null) {
                        mWakeupIncrementListener.onResults(result);
                    }
                    if (result.isLast()) {
                        transferState(EngineState.STATE_NEWED);
                        unRegisterRecorderIfIsRecording(this);
                    }

                    if (mAsrParams.isUseContinuousRecognition())
                        reStart(false);//重新启动

                } else {
                    trackInvalidState("result");
                }
                break;
            case MSG_RESTART:
                boolean needStop = (Boolean) msg.obj;
                reStart(needStop);
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(this);
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
                    if (localGrammarKernel != null) {
                        localGrammarKernel.releaseKernel();
                        localGrammarKernel = null;
                    }
                    if (mVadKernel != null) {
                        mVadKernel.releaseKernel();
                        mVadKernel = null;
                    }
                    if (mSslKernel != null) {
                        mSslKernel.releaseKernel();
                        mSslKernel = null;
                    }
                    clearObject();//清除实例
                    if (mCurrentScene != null) {
                        FileUtils.deleteFilesInDir(mCurrentScene.getGramSlotPath());
                    }
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
                    if (mWakeupIncrementListener != null) {
                        mWakeupIncrementListener.onError(error);
                    }
                    uploadError(error);
                    return;
                } else if (error.getErrId() == AIError.ERR_MAX_SPEECH) {
//                    //音频时长超出阈值重新start引擎
                    reStart(true);
                }
                if (mState == EngineState.STATE_IDLE) {
                    if (mWakeupIncrementListener != null) {
                        mWakeupIncrementListener.onError(error);
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
                    if (mWakeupIncrementListener != null) {
                        mWakeupIncrementListener.onError(error);
                    }
                } else {
                    trackInvalidState("error");
                }
                break;
            default:
                break;
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


    private void handleMsgStart() {
        Log.d(TAG, "handleMsgStart");
        if (mState == EngineState.STATE_NEWED) {
            syncRecorderId(mAsrParams, mVadParams);
            if (mAsrParams.isUseCustomFeed()) {
                Log.i(TAG, "isUseCustomFeed");
                if (mCount > 2000) {
                    Log.d(TAG, "native count max size ,reset kernel !");
                    mCount = 0;
                    if (mAsrParams != null)
                        mAsrKernel.resetKernel();
                } else {
                    if (mAsrParams != null)
                        mAsrKernel.startKernel(mAsrParams);
                }
                mCount++;
                if (mVadConfig.isUseSSL()) {
                    if (mAsrParams != null) {
                        startNoSpeechTimer(mAsrParams);
                    }
                    if (mVadParams != null) {
                        mSslKernel.startKernel(mVadParams);
                    }
                } else if (mVadConfig.isVadEnable()) {
                    if (mAsrParams != null) {
                        startNoSpeechTimer(mAsrParams);
                    }
                    if (mVadParams != null) {
                        mVadKernel.startKernel(mVadParams);
                    }
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
                if (mAsrParams != null) {
                    startRecorder(mAsrParams, this);
                }
            }
        } else {
            trackInvalidState("start");
        }
    }

    private void handleMsgStop() {
        Log.d(TAG, "handleMsgStop");
        if (mState == EngineState.STATE_RUNNING) {
            unRegisterRecorderIfIsRecording(this);
            mAsrKernel.stopKernel();

            if (mVadConfig.isUseSSL()) {
                mSslKernel.stopKernel();
            } else if (mVadConfig.isVadEnable()) {
                mVadKernel.stopKernel();
            }

            transferState(EngineState.STATE_WAITING);

        } else {
            trackInvalidState("stop");
        }
    }

    /**
     * 重启本地识别内核
     * 该方法必须在processor 中的线程中使用。请不要在其他线程调用，如：kernel回调或者外部engine调用线程
     */
    private void reStart(boolean needStop) {
        Log.d(TAG, "reStart, needStop = " + needStop);
        if (needStop) {
            transferState(EngineState.STATE_RUNNING);
            handleMsgStop();
        }
        transferState(EngineState.STATE_NEWED);
        handleMsgStart();
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
            byte[] buffer = new byte[data.length];
            System.arraycopy(data, 0, buffer, 0, data.length);
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_RECEIVE_DATA, buffer);
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
            Log.d(TAG, "WakeupIncrementProcessor onResults :" + result.toString());
            try {
                JSONObject resultObj = JSONUtil.build(result.getResultObject().toString());
                //识别结果
                Object recObj = JSONUtil.getQuietly(resultObj, KEY_LOCAL_ASR_RESULT_REC);
                String rec = recObj != null ? recObj.toString() : "";
                Object confObj = JSONUtil.getQuietly(resultObj, KEY_LOCAL_ASR_RESULT_CONF);
                double conf = confObj == null ? 0 : Double.parseDouble(confObj.toString());

                resultObj.put("isReachThreshold", conf > getThreshold(rec, mAsrParams));
                result.setResultObject(resultObj);
                Log.d(TAG, "conf:" + conf + "getThreshold:" + getThreshold(rec, mAsrParams) + "IsIgnoreThreshold" + mAsrParams.getIsIgnoreThreshold() + "resultObj:" + resultObj);

                if (mAsrParams.getIsIgnoreThreshold()) {
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, result);
                } else {
                    if (!TextUtils.isEmpty(rec) && conf > getThreshold(rec, mAsrParams)) {
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, result);
                    } else {
                        Log.d(TAG, "DROP NO CONF WAKEUPINCREMENT WORD " + result.getResultObject());
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_RESTART, false);
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "DROP NO CONF WAKEUPINCREMENT WORD " + result.getResultObject());
                e.printStackTrace();
                sendMsgToInnerMsgQueue(EngineMsg.MSG_RESTART, false);
            }
        }

        @Override
        public void onStarted(String recordId) {

        }

        @Override
        public void onUpdateResult(int ret) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE_END, ret);
        }
    }

    private class MyLocalGrammarProcessorListener implements LocalGrammarListener {

        @Override
        public void onBuildCompleted(AIResult aiResult) {
            Log.i(TAG, "Grammar onBuildCompleted, done all , update asr ");
            sendMsgToInnerMsgQueue(EngineMsg.MSG_BUILD_END, aiResult);
        }

        @Override
        public void onInit(int status) {
            if (status == AIConstant.OPT_SUCCESS) {
                mWakeupIncrementListener.onGramResults("Gram初始化成功");
            } else {
                mWakeupIncrementListener.onGramResults("Gram初始化失败");
            }
        }

        @Override
        public void onError(AIError error) {
            //不会被调用
//            mWakeupIncrementListener.onGramResults("Gram编译失败："+error.getError());
        }
    }
}
