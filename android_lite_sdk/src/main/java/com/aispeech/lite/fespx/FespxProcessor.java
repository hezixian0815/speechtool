package com.aispeech.lite.fespx;

import static com.aispeech.lite.AISpeechSDK.LOG_MIC_MATRIX_TYPE_EIGHT_LINE;
import static com.aispeech.lite.AISpeechSDK.LOG_MIC_MATRIX_TYPE_FOUR_CAR;
import static com.aispeech.lite.AISpeechSDK.LOG_MIC_MATRIX_TYPE_FOUR_CIRCLE;
import static com.aispeech.lite.AISpeechSDK.LOG_MIC_MATRIX_TYPE_FOUR_L;
import static com.aispeech.lite.AISpeechSDK.LOG_MIC_MATRIX_TYPE_FOUR_LINE;
import static com.aispeech.lite.AISpeechSDK.LOG_MIC_MATRIX_TYPE_SIX_CIRCLE;
import static com.aispeech.lite.AISpeechSDK.LOG_MIC_MATRIX_TYPE_SIX_LINE;
import static com.aispeech.lite.AISpeechSDK.LOG_MIC_MATRIX_TYPE_TWO_LINE;
import static com.aispeech.lite.AISpeechSDK.LOG_SCENE_TYPE_AIHOME;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.DUILiteConfig;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AudioFileWriter;
import com.aispeech.common.ByteConvertUtil;
import com.aispeech.common.FileSaveUtil;
import com.aispeech.common.FileUtil;
import com.aispeech.common.JSONUtil;
import com.aispeech.common.LimitQueue;
import com.aispeech.common.Log;
import com.aispeech.export.bean.VoiceQueueStrategy;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.audio.AIAudioRecorder;
import com.aispeech.lite.audio.AIRecordListener;
import com.aispeech.lite.config.LocalSignalProcessingConfig;
import com.aispeech.lite.function.ICarFunction;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.oneshot.OneshotKernel;
import com.aispeech.lite.oneshot.OneshotListener;
import com.aispeech.lite.param.SignalProcessingParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.sspe.SspeKernelFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by wuwei on 18-6-4.
 */

public class FespxProcessor extends BaseProcessor {
    private static final String TAG_FESP = "FespxProcessor";
    private BaseKernel mFespxKernel;
    private OneshotKernel mOneshotKernel;
    private LocalSignalProcessingConfig mConfig;
    private SignalProcessingParams mParams;
    private CopyOnWriteArrayList<AIRecordListener> mListenerList = new CopyOnWriteArrayList<AIRecordListener>();
    private int mMicType;
    private WakeupCloudCheck wakeupCloudCheck = new WakeupCloudCheck();


    private FileSaveUtil mAudioPcmUtil;  // 单VAD 输入输出文件保存
    private FileSaveUtil mVadPcmUtil;    // 双vad vad 保存文件
    private FileSaveUtil mAsrPcmUtil;    // 双vad asr 保存文件
    private FileSaveUtil mVadAsrPcmUtil;    // 双vad + asr 保存文件

    private AudioFileWriter mWakeupCutFileWriter;
    private final boolean shouldKeepWakeupFile = true;
    /**
     * 保存唤醒之前的音频
     */
    private LimitQueue<byte[]> mMergeQueue;

    /**
     * 根据配置设置缓存队列的大小
     *
     * @param config
     * @return
     */
    public int getChannelNum(LocalSignalProcessingConfig config) {
        int channelNum = 2;//默认值2
        switch (mMicType) {
            case DUILiteConfig.TYPE_COMMON_DUAL:
            case DUILiteConfig.TYPE_TINYCAP_DUAL:
                mMicMatrixStr = LOG_MIC_MATRIX_TYPE_TWO_LINE;
                channelNum = 2;
                mScope = Scope.FESPD;
                break;
            case DUILiteConfig.TYPE_COMMON_FESPCAR:
                mMicMatrixStr = LOG_MIC_MATRIX_TYPE_TWO_LINE;
                channelNum = 2;
                mScope = Scope.FESPCAR;
                break;
            case DUILiteConfig.TYPE_COMMON_LINE4:
            case DUILiteConfig.TYPE_TINYCAP_LINE4:
                mMicMatrixStr = LOG_MIC_MATRIX_TYPE_FOUR_LINE;
                channelNum = 4;
                mScope = Scope.FESPL_4;
                break;
            case DUILiteConfig.TYPE_COMMON_CIRCLE4:
            case DUILiteConfig.TYPE_TINYCAP_CIRCLE4:
                mMicMatrixStr = LOG_MIC_MATRIX_TYPE_FOUR_CIRCLE;
                channelNum = 4;
                mScope = Scope.FESPA_4;
                break;
            case DUILiteConfig.TYPE_COMMON_SHAPE_L4:
                mMicMatrixStr = LOG_MIC_MATRIX_TYPE_FOUR_L;
                channelNum = 4;
                mScope = Scope.FESP_SHAPE_L4;
                break;
            case DUILiteConfig.TYPE_COMMON_FESPCAR4:
                mMicMatrixStr = LOG_MIC_MATRIX_TYPE_FOUR_CAR;
                channelNum = 4;
                mScope = Scope.SSPE;
                break;
            case DUILiteConfig.TYPE_COMMON_CIRCLE6:
            case DUILiteConfig.TYPE_TINYCAP_CIRCLE6:
                mMicMatrixStr = LOG_MIC_MATRIX_TYPE_SIX_CIRCLE;
                channelNum = 6;
                mScope = Scope.FESPA_6;
                break;
            case DUILiteConfig.TYPE_COMMON_LINE8:
                mMicMatrixStr = LOG_MIC_MATRIX_TYPE_EIGHT_LINE;
                channelNum = 8;
                mScope = Scope.FESPL_8;
                break;
            case DUILiteConfig.TYPE_COMMON_LINE6:
            case DUILiteConfig.TYPE_TINYCAP_LINE6:
                mMicMatrixStr = LOG_MIC_MATRIX_TYPE_SIX_LINE;
                channelNum = 6;
                mScope = Scope.FESPL_6;
                break;
            default:
                break;
        }
        if (config.isSspe()) {
            mScope = Scope.SSPE;
        }
        mSceneStr = LOG_SCENE_TYPE_AIHOME;
        if (config.getEchoChannelNum() > 0) {
            channelNum += config.getEchoChannelNum();
        } else {
            // 兼容原有不设置 echoChannel
            if (!TextUtils.equals(config.getAecBinPath(), "OFF") && !config.isSspe()) {
                // 需要做AEC 不使用新的 sspe 模块下又设置了 aec 资源则默认含2路两路参考音
                channelNum += 2;
            }
        }
        Log.d(TAG_FESP, "channelNum is : " + channelNum);
        return channelNum;
    }

    public void init(SpeechListener listener, LocalSignalProcessingConfig config) {
        this.mConfig = config;
        this.mBaseConfig = config;
        this.mOutListener = listener;
        boolean haveSetAecRes = !(TextUtils.isEmpty(config.getAecBinPath()) || "OFF".equals(config.getAecBinPath()));
        AIAudioRecorder.setFespOriginalAudio(haveSetAecRes || (config.getEchoChannelNum() > 0));
        if (config.getMicType() >= 0) {
            mMicType = config.getMicType();
        } else {
            mMicType = AISpeech.getRecoderType();
        }
        mDataSizeThresh = getChannelNum(mConfig) * AISpeech.uploadAudioTime * 32;//初始化音频缓存队列大小
        if (null != config.getOneshotConfig()) {
            mOneshotKernel = new OneshotKernel(new OneshotListenerImpl());
            threadCount++;
        }
        init(listener, mConfig.getContext(), TAG_FESP);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }

    class OneshotListenerImpl implements OneshotListener {

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onOneshot(String word, OneshotCache<byte[]> buffer) {
            Map oneshotMap = new HashMap();
            oneshotMap.put("words", word);
            oneshotMap.put("audio", buffer);

            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ONE_SHOT, oneshotMap);
        }

        @Override
        public void onNotOneshot(String word) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_NOT_ONE_SHOT, word);
        }

    }

    public void start(SignalProcessingParams params) {
        if (mProfileState != null && mProfileState.isValid()) {
            this.mParams = params;
            this.inputContinuousAudio = mParams.isInputContinuousAudio();
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                if (mState == EngineState.STATE_IDLE) {

                    if (mConfig.getNearWakeupConfig() != null) {
                        mFespxKernel = new FespxKernel(new MyFespxKernelListener());
                    } else {
                        SspeKernelFactory sspeKernelFactory = new SspeKernelFactory();
                        mFespxKernel = sspeKernelFactory.getSspeKernel(new MyFespxKernelListener(), mConfig.getSspeType());
                    }
                    mFespxKernel.setMaxMessageQueueSize(mConfig.getMaxMessageQueueSize());
                    VoiceQueueStrategy queueStrategy = mConfig.getVoiceQueueStrategy();
                    if (queueStrategy != null) {
                        mFespxKernel.setMaxVoiceQueueSize(queueStrategy.getMaxVoiceQueueSize(), queueStrategy.getIgnoreSize());
                    }

                    if (mConfig.getOneshotConfig() != null) {
                        mOneshotKernel.newKernel(mConfig.getOneshotConfig());
                    }
                    mMergeQueue = new LimitQueue<>(20 * 6);

                    int status = copyAssetsRes(mConfig);
                    if (status == AIConstant.OPT_FAILED) {
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                        break;
                    }
                    mFespxKernel.newKernel(mConfig);
                    wakeupCloudCheck.init(mConfig.getCnWakeupWord(), mConfig.getCloudWakeupCheckTimeout(), onCloudCheckListener);
                } else {
                    trackInvalidState("new");
                }
                break;
            case MSG_START:
                if (mState == EngineState.STATE_NEWED) {
                    if (!mParams.isUseCustomFeed()) {
                        if (mAIRecorder == null) {
                            mAIRecorder = createRecorder(this);
                            if (mAIRecorder == null) {
                                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                                        AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE));
                                return;
                            }
                        }
                    } else {
                        unRegisterRecorderIfIsRecording(this);
                        releaseRecorder();
                    }
                    mCallbackState = EngineState.STATE_RUNNING;
                    /**********reset wakeup value***********/
                    /********upload********/
                    mHasPreWakeup = false;
                    synchronized (mPreuploadLock) {
                        mHasHalfWakeup = false;
                        mPreUploadRollbackDataSize = 0;
                        mPreUploadRollbackTime = mConfig.getPreUploadRollbackTime();
                        if (gscData != null) {
                            gscData.clear();
                        }
                    }
                    mRecorderId = Utils.getRecorderId();

                    resetWakeupCacheQueue();
                    createFileWriter();
                    if (mParams.isUseCustomFeed()) {
                        Log.i(TAG_FESP, "isUseCustomFeed");
                        mFespxKernel.startKernel(mParams);
                        transferState(EngineState.STATE_RUNNING);
                    } else {
                        // 启动SDK内部录音机
                        startRecorder(mParams, FespxProcessor.this);
                    }
                } else {
                    trackInvalidState("start");
                }
                break;
            case MSG_SET:
                final String setStr = (String) msg.obj;
                if (mState != EngineState.STATE_IDLE) {
                    mFespxKernel.set(setStr);
                } else {
                    trackInvalidState("set info");
                }
                break;
            case MSG_RECORDER_START:
                if (mState == EngineState.STATE_NEWED) {
                    mFespxKernel.startKernel(mParams);
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("recorder start");
                }
                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(FespxProcessor.this);
                    releaseRecorder();
                    mFespxKernel.stopKernel();
                    mMergeQueue.clear();
                    closeFileWriter();
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("stop");
                }
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(FespxProcessor.this);
                    }
                    closeFileWriter();
                    wakeupCloudCheck.destroy();
                    releaseRecorder();
                    mFespxKernel.releaseKernel();
                    if (mOneshotKernel != null) {
                        mOneshotKernel.releaseKernel();
                    }
                    clearObject();//清除实例
                    transferState(EngineState.STATE_IDLE);
                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_RAW_RECEIVE_DATA:
                //此MSG包含录音机/自定义feed音频来源
                final byte[] bufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    /*******offer wakeup data to queue**********/
                    if (isUploadEnable()) {
                        synchronized (mLock) {
                            if (mNeedCache) {
                                if (mCurrentDataSize >= mDataSizeThresh && mUploadCacheQueue.size() > 0) {
                                    mCurrentDataSize -= mUploadCacheQueue.remove().length;
                                }
                                mCurrentDataSize += bufferData.length;
                                mUploadCacheQueue.offer(bufferData);
                            }
                        }
                    }
                    /**********dump***********/
                    if (mNeedDumpData) {
                        synchronized (mDumpLock) {
                            if (mDumpCurrentDataSize >= mDumpDataThresh && mDumpWkpDataQueue.size() > 0) {
                                mDumpCurrentDataSize -= mDumpWkpDataQueue.remove().length;
                            }
                            mDumpCurrentDataSize += bufferData.length;
                            mDumpWkpDataQueue.offer(bufferData);
                        }
                    }
                    /*********************/
                    mFespxKernel.feed(bufferData);
                    saveInData(bufferData, bufferData.length);
                    mMergeQueue.offer(bufferData);
                    if (mOutListener != null) {
                        mOutListener.onRawDataReceived(bufferData, bufferData.length);
                    }
                }
                break;
            case MSG_RESULT:
                AIResult result = (AIResult) msg.obj;
                if (mState != EngineState.STATE_IDLE) {
                    try {
                        JSONObject resultJO = new JSONObject(result.getResultObject().toString());
                        long timeInterval = System.currentTimeMillis() - mLastWakeupTime;
                        mLastWakeupTime = System.currentTimeMillis();

                        if (resultJO.has("status")) {
                            int status = resultJO.optInt("status");
                            Log.d(TAG_FESP, "status " + status);
                            result.setRecordId("wakeup_" + Utils.getRecorderId() + "_" + System.currentTimeMillis());//设置唤醒唯一标识符
                            JSONUtil.putQuietly(resultJO, "wakeupUid", result.getRecordId());
                            if (status == 7) {
                                if (mConfig.getCnWakeupWord() != null && mConfig.getCnWakeupWord().length > 0) {
                                    if (System.currentTimeMillis() - wakeupCheckTimestamp > 1000) {
                                        wakeupCloudCheck.cancelAll();
                                    }
                                    boolean suc = wakeupCloudCheck.start();
                                    if (suc) {
                                        wakeupCheckTimestamp = System.currentTimeMillis();
                                        synchronized (mPreuploadLock) {
                                            for (int i = 0; i < gscData.size(); i++) {
                                                Log.v(TAG_FESP, "handlerInnerMsg:i " + i + " gsclength " + gscData.get(i).size());
                                                wakeupCloudCheck.feed(i, gscData.get(i));
                                                gscData.get(i).clear();
                                                Log.v(TAG_FESP, "111handlerInnerMsg:i " + i + " gsclength " + gscData.get(i).size());
                                            }
                                            mPreUploadRollbackDataSize = 0;
                                            //gscData.clear();
                                        }
                                    }
                                    mHasHalfWakeup = true;
                                }

                                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);

                                if (!TextUtils.isEmpty(mParams.getSaveAudioPath())) {
                                    saveWakeupData(resultJO.optString("wakeupWord").replaceAll(" ", ""));
                                }

                                return;
                            } else if (status == 4) {//pre wakeup happens
                                mHasPreWakeup = true;
                                mIsRealWakeup = false;//reset value of realWakeup state
                                Log.d(TAG_FESP, "预唤醒 " + result.getResultObject().toString());
                                Log.d(TAG_FESP, "半词唤醒->预唤醒 时间：" + (System.currentTimeMillis() - wakeupCheckTimestamp));
                                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
                            } else if (status == 1 || status == 2) {//real wakeup happens
                                /***dump****/
                                if (AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE) {
                                    startDumpWaitingTimerTask(mParams.getDumpAudioPath());
                                }
                                /***********/
                                mIsRealWakeup = true;
                                mHasHalfWakeup = false;
                                if ((timeInterval <= DEFAULT_VALID_TIME_THRESH) && mHasPreWakeup) {
                                    mWakeupJson = resultJO;
                                }

                                if (resultJO.getDouble("confidence") < mConfig.getHighThreshold(resultJO.getString("wakeupWord"))
                                        && wakeupCloudCheck.isFeedingStatus()) {
                                    mResult = result;
                                    long now = System.currentTimeMillis();
                                    Log.d(TAG_FESP, "半词唤醒->唤醒 时间：" + (now - wakeupCheckTimestamp));
                                    wakeupCheckTimestamp = now;
                                    wakeupCloudCheck.stop();
                                } else {
                                    // 云端asr没有处理中或者 confidence 比设置的 highThreshold 大则无需云端asr校验
                                    mResult = null;
                                    if (updateTrails(mProfileState)) {
                                        sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
                                    }
                                    Log.d(TAG_FESP, ">highThreshold 无需校验");
                                    wakeupCloudCheck.stop();
                                }

                                if (!TextUtils.isEmpty(mParams.getSaveAudioPath())) {
                                    saveWakeupData(resultJO.optString("wakeupWord").replaceAll(" ", ""));
                                }

                                if (mConfig.getOneshotConfig() != null && mOneshotKernel != null) {
                                    mOneshotKernel.notifyWakeup(resultJO.optString("wakeupWord"));
                                }

                            } else if (status == 0) {
                                // 500ms 内有唤醒状态1，2就不再抛出状态0，状态2虽不抛出，但是有状态2一定有状态1
                                if (timeInterval > DEFAULT_VALID_TIME_THRESH || (mHasPreWakeup && !mIsRealWakeup)) {
                                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
                                }
                                if (!TextUtils.isEmpty(mParams.getSaveAudioPath())) {
                                    saveWakeupData(resultJO.optString("wakeupWord").replaceAll(" ", ""));
                                }

                                if (mConfig.getOneshotConfig() != null && mOneshotKernel != null) {
                                    mOneshotKernel.notifyWakeup(resultJO.optString("wakeupWord"));
                                }
                            }
                            if (status != 0)
                                AnalysisProxy.getInstance().updateConfig(false);

                            // 500ms内预唤醒发生了，本次会忽略本次唤醒/预唤醒音频(会缺少唤醒点前5s音频)问题
                            // 唤醒和预唤醒同时发生则不再产生TimerTask，由预唤醒生成的 TimerTask 上传唤醒音频
                            if (isUploadEnable() && (mHasPreWakeup || mIsRealWakeup) && ((timeInterval > DEFAULT_VALID_TIME_THRESH) || !(mHasPreWakeup && mIsRealWakeup))) {
                                /***dump****/
                                if (AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE) {
                                    startDumpWaitingTimerTask(mParams.getDumpAudioPath());
                                }
                                /***********/
                                mWakeupJson = resultJO;//update wakeup Json for upload
                                Log.d(TAG_FESP, "upload enable, invoke upload timerTask");
                                startWakeupUploadWaitingTimerTask();
                            } else {
                                Log.w(TAG_FESP, "upload disable or new wakeup happens within 500ms, ignore");
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    trackInvalidState("result");
                }
                break;
            case MSG_WAKEUP_CLOUD_CHECK:
                if (mState != EngineState.STATE_IDLE) {
                    AIResult resultCloudCheck = (AIResult) msg.obj;
                    Log.d(TAG_FESP, "MSG_WAKEUP_CLOUD_CHECK");
                    if (updateTrails(mProfileState)) {
                        sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, resultCloudCheck);
                    }
                }
                break;
            case MSG_NEAR_INFORMATION:
                if (mState != EngineState.STATE_IDLE) {
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_NEAR_INFORMATION, msg.obj);
                } else {
                    trackInvalidState("near information");
                }
                break;
            case MSG_DOA:
                Object[] data = (Object[]) msg.obj;
                int doaValue = (int) data[0];
                if (mState == EngineState.STATE_RUNNING) {
                    if (wakeupCloudCheck.isASRingStatus()) {
                        mDoaValue = doaValue;
                        Log.d(TAG_FESP, "save mDoaValue:" + mDoaValue);
                    } else {
                        mDoaValue = -1;
                        sendMsgToCallbackMsgQueue(CallbackMsg.MSG_DOA_RESULT, data);
                    }
                } else {
                    trackInvalidState("doa result");
                }
                break;
            case MSG_SEVC_DOA:
                int doa = (Integer) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_SEVC_DOA_RESULT, doa);
                } else {
                    trackInvalidState("sevc doa result");
                }
                break;
            case MSG_SEVC_NOISE:
                String noise = (String) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_SEVC_NOISE_RESULT, noise);
                } else {
                    trackInvalidState("sevc noise result");
                }
                break;
            case MSG_ERROR:
                AIError error = (AIError) msg.obj;
                Log.w(TAG_FESP, error.toString());
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(this);
                    mFespxKernel.stopKernel();
                    closeFileWriter();
                    transferState(EngineState.STATE_NEWED);
                }
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, msg.obj);
                break;
            case MSG_FORCE_REQUEST_WAKEUP_RESULT:
                if (mState == EngineState.STATE_RUNNING) {
                    mFespxKernel.forceRequestWakeupResult();
                } else {
                    trackInvalidState("MSG_FORCE_REQUEST_WAKEUP_RESULT");
                }
                break;
            case MSG_UPDATE:
                handleMsgUpdate();
                break;
            default:
                break;
        }
    }

    private void resetWakeupCacheQueue() {
        synchronized (mLock) {
            mCurrentDataSize = 0;
            if (mUploadCacheQueue != null) {
                mUploadCacheQueue.clear();
            }
        }
        /**********dump***********/
        mDumpDataThresh = mParams.getDumpTime() * 32 * getChannelNum(mConfig);
        mNeedDumpData = false;
        mDumpCurrentDataSize = 0;
        synchronized (mDumpLock) {
            if (mDumpWkpDataQueue != null) {
                mDumpWkpDataQueue.clear();
            }
        }
        /*********************/
        if (!TextUtils.isEmpty(mParams.getDumpAudioPath())) {
            mNeedDumpData = true;
            Log.d(TAG_FESP, "dump path is " + mParams.getDumpAudioPath());
            Log.d(TAG_FESP, "dump time is " + mParams.getDumpTime());
        }
    }

    @Override
    public void clearObject() {
        super.clearObject();
        if (mOneshotKernel != null) {
            mOneshotKernel = null;
        }

        if (mMergeQueue != null) {
            mMergeQueue = null;
        }

        removeCallbackMsg();
        mFespxKernel = null;
        mListenerList = null;
        mParams = null;
        mConfig = null;
    }

    public synchronized int getValueOf(String param) {
        if (mState != EngineState.STATE_IDLE && mFespxKernel != null) {
            return mFespxKernel.getValueOf(param);
        } else {
            return -1;
        }
    }


    private void saveWakeupData(String result) {
        Log.d(TAG_FESP, "wake up result = " + result);
        String saveAudioPath = mParams.getSaveAudioPath();
        if (shouldKeepWakeupFile && !TextUtils.isEmpty(saveAudioPath)) {
            Log.d(TAG_FESP, "audio wake path = " + saveAudioPath);

            FileSaveUtil mWakeUpPcmUtil = new FileSaveUtil();
            mWakeUpPcmUtil.init(saveAudioPath);
            mWakeUpPcmUtil.prepare("wakeup");

            while (!mMergeQueue.isEmpty()) {
                byte[] data = mMergeQueue.poll();
                mWakeUpPcmUtil.feedTypeCustom(data);
            }
            mWakeUpPcmUtil.close();
            FileUtil.limitFileTotalSize(saveAudioPath, (int) (AISpeechSDK.GLOBAL_AUDIO_FILE_ALL_SIZE * 0.4), "fesp");
        }
    }


    private void createFileWriter() {
        DateFormat sdf = new SimpleDateFormat("yyyy-dd-MM_HH-mm-ss", Locale.CHINA);
        String time = sdf.format(new Date());
        String saveAudioPath = mParams.getSaveAudioPath();
        if (!TextUtils.isEmpty(saveAudioPath)) {
            Log.d(TAG_FESP, "raw path: " + saveAudioPath);
            mAudioPcmUtil = new FileSaveUtil();
            mAudioPcmUtil.init(saveAudioPath);
            mAudioPcmUtil.prepare();

            if (AISpeech.useDoubleVad) {
                mVadPcmUtil = new FileSaveUtil();
                mVadPcmUtil.init(saveAudioPath);
                mVadPcmUtil.prepare("vad");

                mAsrPcmUtil = new FileSaveUtil();
                mAsrPcmUtil.init(saveAudioPath);
                mAsrPcmUtil.prepare("asr");

                mVadAsrPcmUtil = new FileSaveUtil();
                mVadAsrPcmUtil.init(saveAudioPath);
                mVadAsrPcmUtil.prepare("vad_asr");

            }
        }

        if (AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE && !TextUtils.isEmpty(mParams.getSaveWakeupCutFilePath())) {
            Log.d(TAG_FESP, "wkcut path: " + mParams.getSaveWakeupCutFilePath());
            mWakeupCutFileWriter = new AudioFileWriter();
            mWakeupCutFileWriter.createTextFile(mParams.getSaveWakeupCutFilePath() + "/wkcut_" + time + ".txt");
        }
    }

    private void closeFileWriter() {
        if (mAudioPcmUtil != null) {
            mAudioPcmUtil.close();
            mAudioPcmUtil = null;
        }
        if (mWakeupCutFileWriter != null) {
            mWakeupCutFileWriter.close();
            mWakeupCutFileWriter = null;
        }

        if (mVadPcmUtil != null) {
            mVadPcmUtil.close();
            mVadPcmUtil = null;
        }

        if (mAsrPcmUtil != null) {
            mAsrPcmUtil.close();
            mAsrPcmUtil = null;
        }

        if (mVadAsrPcmUtil != null) {
            mVadAsrPcmUtil.close();
            mVadAsrPcmUtil = null;
        }
    }

    private void saveInData(final byte[] data, final int size) {
        if (mAudioPcmUtil != null) {
            mAudioPcmUtil.feedTypeIn(data);
        } else {
            createFileWriter();
        }
    }

    public boolean isRegistered(AIRecordListener listener) {
        return listener != null && mListenerList.contains(listener);
    }

    public boolean isRunning() {
        return mState == EngineState.STATE_NEWED || mState == EngineState.STATE_RUNNING;
    }

    public void registerListener(AIRecordListener listener) {
        Log.d(TAG_FESP, "registerListener " + listener.toString());
        if (isRecorderRecording() || isUseCustomFeed()) {
            listener.onRecordStarted(0);
        }
        if (!mListenerList.contains(listener)) {
            Log.d(TAG_FESP, "add listener " + listener.toString());
            mListenerList.add(listener);
        }
    }

    public void unRegisterListener(AIRecordListener listener) {
        if (listener != null && mListenerList.contains(listener)) {
            Log.d(TAG_FESP, "remove listener " + listener.toString());
            mListenerList.remove(listener);
        }
    }

    public void notifyBufferReceived(final byte[] buffer, final int size) {
        for (AIRecordListener listener : mListenerList) {
            listener.onResultDataReceived(buffer, size);
        }
    }

    public boolean isRecorderRecording() {
        if (mAIRecorder != null) {
            return mAIRecorder.isRecording(this);
        } else {
            return false;
        }
    }

    public boolean isUseCustomFeed() {
        if (mParams != null) {
            return mParams.isUseCustomFeed();
        } else {
            Log.e(TAG_FESP, "must start asr engine after signal engine!!!");
            return false;
        }
    }

    public synchronized int getDriveMode() {
        if (!mProfileState.isValid() || mFespxKernel == null) {
            return -1;
        }

        if (mFespxKernel instanceof ICarFunction) {
            return ((ICarFunction) mFespxKernel).getDriveMode();
        }

        return mFespxKernel.getValueOf("driveMode");
    }

    public synchronized int getFespx(String param) {
        return mFespxKernel != null ? mFespxKernel.getValueOf(param) : -1;
    }

    public synchronized void setDriveMode(int driveMode) {

        if (!mProfileState.isValid() || mFespxKernel == null) {
            return;
        }

        if (mFespxKernel instanceof ICarFunction) {
            ((ICarFunction) mFespxKernel).setDriveMode(driveMode);
        }

    }

    public synchronized void setDriveMode(int driveMode, int wakeupChannelMask) {

        if (!mProfileState.isValid() || mFespxKernel == null) {
            return;
        }

        if (mFespxKernel instanceof ICarFunction) {
            ((ICarFunction) mFespxKernel).setDriveMode(driveMode, wakeupChannelMask);
        }

    }

    public synchronized void setDoaManually(int doa) {
        if (!mProfileState.isValid() || mFespxKernel == null) {
            return;
        }

        if (mFespxKernel instanceof ICarFunction) {
            ((ICarFunction) mFespxKernel).setDoaManually(doa);
        }
    }

    /**
     * 设置定位模式下唤醒后不切换音区的唤醒词
     *
     * @param rangesWords 不主动切换音区的词
     */
    public synchronized void setRangesWords(List<String> rangesWords) {
        if (mFespxKernel != null && mFespxKernel instanceof ICarFunction) {
            ((ICarFunction) mFespxKernel).setRangesWords(rangesWords);
        }
    }

    public void setResBin(LocalSignalProcessingConfig signalConfig) {
        this.mConfig = signalConfig;
        sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE, null);
    }

    private void handleMsgUpdate() {
        int status = copyAssetsRes(mConfig);
        if (status == AIConstant.OPT_FAILED) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
            return;
        }
        mFespxKernel.update(mConfig);
    }

    @Override
    public void processNoSpeechError() {
        //do nothing
    }

    @Override
    public void processMaxSpeechError() {
        //do nothing
    }

    public void resetDriveMode() {
        if (mFespxKernel instanceof FespxKernel)
            ((FespxKernel) mFespxKernel).resetDriveMode();

        if (mFespxKernel instanceof ICarFunction) {
            ((ICarFunction) mFespxKernel).resetDriveMode();
        }
    }

    /**
     * fespl 内部信息
     */
    private class MyFespxKernelListener implements FespxKernelListener {

        @Override
        public void onInit(int status) {
            Log.i(TAG_FESP, "MyFespxKernelListener onInit : " + status);
            processInit(status);
        }

        @Override
        public void onDoaResult(int doa) {
            //sendMsgToInnerMsgQueue(EngineMsg.MSG_DOA, doa);
        }

        @Override
        public void onDoaResult(int doa, int type) {
            Object[] objects = new Object[]{doa, type};
            sendMsgToInnerMsgQueue(EngineMsg.MSG_DOA, objects);
        }

        @Override
        public void onInterceptWakeup(int doa, double confidence, String wakeupWord) {
            if (mOutListener != null) {
                mOutListener.onInterceptWakeup(doa, confidence, wakeupWord);
            }
        }

        @Override
        public void onNearInformation(String info) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_NEAR_INFORMATION, info);
        }

        @Override
        public void onSevcNoiseResult(String retString) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_SEVC_NOISE, retString);
        }

        @Override
        public void onSevcDoaResult(Object doa) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_SEVC_DOA, doa);
        }

        @Override
        public void onMultibfDataReceived(int index, byte[] bufferData, int size) {
            byte[] data = new byte[size];
            System.arraycopy(bufferData, 0, data, 0, size);
            if (mOutListener != null) {
                mOutListener.onMultibfDataReceived(data, data.length, index);
            }
            synchronized (mPreuploadLock) {
                int listSize = gscData.size();
                if (index + 1 > listSize) {
                    List<byte[]> list = new LinkedList<>();
                    list.add(data);
                    mPreUploadRollbackDataSize += data.length;
                    gscData.add(list);
                    wakeupCloudCheck.setChannel(index + 1);
                } else {
                    List<byte[]> list = gscData.get(index);
                    //缓存最大500ms的音频，这里触发半字唤醒会上传一半的音频，触发真实唤醒会把另一半的音频送上去，
                    //这里存缓存60帧，也就是1s左右的数据
                    if (mPreUploadRollbackDataSize > mPreUploadRollbackTime * 32 * wakeupCloudCheck.getChannel()
                            && !list.isEmpty()) {
                        mPreUploadRollbackDataSize -= list.get(0).length;
                        list.remove(0);
                    }
                    list.add(data);
                    mPreUploadRollbackDataSize += data.length;
                  //  Log.v(TAG_FESP, "onMultibfDataReceived: " + gscData.get(index).size() + " index " + index + " size " + size + " mPreUploadRollbackDataSize " + mPreUploadRollbackDataSize);
                }
                //半字唤醒之后使用连续feed的方式上传数据，防止一次性上传数据太多导致算法累积，从而出现云端识别慢的现象
                if (mHasHalfWakeup && wakeupCloudCheck != null && wakeupCloudCheck.getStatus() == 1) {
                    List<byte[]> list1 = new LinkedList<>();
                    list1.add(data);
                    wakeupCloudCheck.feed(index, list1);
                } else {
                    mHasHalfWakeup = false;
                }
            }
        }

        @Override
        public void onEchoVoipDataReceived(int type, byte[] bufferData, int size) {
            if (mOutListener != null) {
                mOutListener.onEchoVoipDataReceived(bufferData, size);
            }
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeup_type) {
            processBFData(buffer, wakeup_type);
        }

        @Override
        public void onResultDataReceived(byte[] bufferVad, byte[] bufferAsr) {
            processBFData(bufferVad, bufferAsr);
        }

        @Override
        public void onResultDataReceived(byte[] buffer, boolean useDoubleVad) {
            if (mOutListener != null) {
                mOutListener.onResultDataReceived(buffer, useDoubleVad);
            }
            if (mVadAsrPcmUtil != null) mVadAsrPcmUtil.feedTypeOut(buffer);
        }

        @Override
        public void onResult(AIResult aiResult) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, aiResult);
        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] data, int size) {
            if (mOutListener != null) {
                mOutListener.onVprintCutDataReceived(dataType, data, size);
            }
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String wakeupStr = new String(data);
                Log.i(TAG_FESP, "vprint cut info: " + wakeupStr);
                if (mWakeupCutFileWriter != null) {
                    mWakeupCutFileWriter.writeString(wakeupStr);
                }
            } else if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                Log.i(TAG_FESP, "vprint cut data: " + size);
                if (mWakeupCutFileWriter != null) {
                    mWakeupCutFileWriter.writeBytesAsString(data, size);
                }
            }
        }

        @Override
        public void onInputDataReceived(byte[] data, int size) {
            if (mState != EngineState.STATE_IDLE && mOutListener != null) {
                mOutListener.onInputDataReceived(data, size);
            }
        }

        @Override
        public void onOutputDataReceived(byte[] data, int size) {
            if (mState != EngineState.STATE_IDLE && mOutListener != null) {
                mOutListener.onOutputDataReceived(data, size);
            }
        }

        @Override
        public void onEchoDataReceived(byte[] data, int size) {
            if (mState != EngineState.STATE_IDLE && mOutListener != null) {
                mOutListener.onEchoDataReceived(data, size);
            }
        }

        @Override
        public void onAgcDataReceived(byte[] data, int size) {
            if (mState != EngineState.STATE_IDLE && mOutListener != null) {
                mOutListener.onAgcDataReceived(data);
            }
        }
    }

    private void processBFData(byte[] bfData, int wakeupType) {
        if (AISpeech.zoomAudioRate != 1.0f) {
            if (AISpeech.zoomAudioFlag == (DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_VAD | DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_ASR)) {
                // 当配置asr、vad都放大时，才对sspe抛出的音频做放大处理，与DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_VAD_OUT放大位置做区分
                bfData = ByteConvertUtil.bigPcm(bfData, AISpeech.zoomAudioRate);
            }
        }
        notifyBufferReceived(bfData, bfData.length);
        if (mAudioPcmUtil != null) {
            mAudioPcmUtil.feedTypeOut(bfData);
        }
        if (mOutListener != null) {
            mOutListener.onResultDataReceived(bfData, bfData.length, wakeupType);
        }
        if (mOneshotKernel != null) {
            mOneshotKernel.feed(bfData);
        }
    }

    private void processBFData(byte[] vadData, byte[] asrData) {
        if (AISpeech.zoomAudioRate != 1.0f) {
            if ((DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_VAD & AISpeech.zoomAudioFlag) == DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_VAD) {
                vadData = ByteConvertUtil.bigPcm(vadData, AISpeech.zoomAudioRate);
            }
            if ((DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_ASR & AISpeech.zoomAudioFlag) == DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_ASR) {
                asrData = ByteConvertUtil.bigPcm(asrData, AISpeech.zoomAudioRate);
            }
        }
        if (mOutListener != null) {
            mOutListener.onResultDataReceived(vadData, asrData);
        }

        if (mVadPcmUtil != null) mVadPcmUtil.feedTypeOut(vadData);
        if (mAsrPcmUtil != null) mAsrPcmUtil.feedTypeOut(asrData);
    }

    private final List<List<byte[]>> gscData = new LinkedList<>();
    private int mPreUploadRollbackTime = 1000;//半字唤醒缓存时间，用于云端二次校验，默认1s
    private int mPreUploadRollbackDataSize = 0;//当前缓存的数据量大小
    private Object mPreuploadLock = new Object();
    private volatile AIResult mResult = null;
    private long wakeupCheckTimestamp = 0;
    private volatile int mDoaValue = -1;

    private WakeupCloudCheck.OnCloudCheckListener onCloudCheckListener = new WakeupCloudCheck.OnCloudCheckListener() {

        @Override
        public void onCloudCheck(String asr) {
            Log.d(TAG_FESP, "唤醒->识别结果 时间：" + (System.currentTimeMillis() - wakeupCheckTimestamp));
            wakeupCheckTimestamp = 0;
            Log.d(TAG_FESP, "onCloudCheck " + asr);
            if (mResult != null) {
                sendMsgToInnerMsgQueue(EngineMsg.MSG_WAKEUP_CLOUD_CHECK, mResult);
                if (mDoaValue != -1)
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_DOA, mDoaValue);
            } else
                Log.d(TAG_FESP, "onCloudCheck mResult is null");
        }

        @Override
        public void onError() {
            Log.d(TAG_FESP, "onError()");
            mDoaValue = -1;
            mResult = null;
        }
    };

}
