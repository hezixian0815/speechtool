package com.aispeech.lite.wakeup;

import static com.aispeech.lite.AISpeechSDK.LOG_SCENE_TYPE_AICAR;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.common.AIConstant;
import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.WakeupConfig;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.oneshot.OneshotKernel;
import com.aispeech.lite.oneshot.OneshotListener;
import com.aispeech.lite.param.WakeupParams;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;


/**
 * Created by wuwei on 2018/3/29.
 */

public class WakeupProcessor extends BaseProcessor {
    public static String TAG = "WakeupProcessor";
    protected Queue<byte[]> mWkpDataQueue = new LinkedList<>();
    protected int mWakeupCurrentDataSize = 0;
    protected Object mWakeupLock = new Object();
    private WakeupKernel mWakeupKernel;
    private WakeupParams mParams;
    private WakeupConfig mConfig;
    private OneshotKernel mOneshotKernel;

    public void init(SpeechListener listener, WakeupConfig config) {
        threadCount = 1;
        if (config.getOneshotConfig() != null) {
            mOneshotKernel = new OneshotKernel(new OneshotListenerImpl());
            threadCount++;
        }
        mScope = Scope.WAKEUP;
        init(listener, config.getContext(), TAG);
        this.mBaseConfig = config;
        this.mConfig = config;
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }

    public void start(WakeupParams param) {
        if (mProfileState != null && mProfileState.isValid()) {
            this.mParams = param;
            this.inputContinuousAudio = mParams.inputContinuousAudio();
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage(mProfileState);
        }
    }

    private void initWakeupKernel(WakeupConfig config) {
        int status = AIConstant.OPT_FAILED;
        status = copyAssetsRes(config);
        if (status == AIConstant.OPT_SUCCESS) {
            mWakeupKernel.newKernel(config);
        } else {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
        }
    }

    /**
     * 处理内部消息
     *
     * @param engineMsg 　engineMsg
     * @param msg       msg 对象值
     */
    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                if (mState == EngineState.STATE_IDLE) {
                    if (isUploadEnable()) {
                        if (mConfig.getResBinPath() != null && mConfig.getResBinPath().toLowerCase().contains(LOG_SCENE_TYPE_AICAR)) {//车载场景的单麦资源
                            mSceneStr = LOG_SCENE_TYPE_AICAR;
                            Log.d(TAG, "send scene is : " + mSceneStr);
                        }
                        mDataSizeThresh = AISpeech.uploadAudioTime * 32;
                    }
                    if (mConfig.getOneshotConfig() != null) {
                        mOneshotKernel.newKernel(mConfig.getOneshotConfig());
                    }
                    mWakeupKernel = new WakeupKernel(new MyWakeupListener());
                    initWakeupKernel((WakeupConfig) mConfig);
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
                    /**********reset wakeup value***********/
                    /********upload********/
                    mHasPreWakeup = false;
                    mRecorderId = Utils.getRecorderId();
                    synchronized (mLock) {
                        mCurrentDataSize = 0;
                        if (mUploadCacheQueue != null) {
                            mUploadCacheQueue.clear();
                        }
                    }
                    /**********dump***********/
                    mDumpDataThresh = mParams.getDumpTime() * 32;
                    mNeedDumpData = false;
                    mDumpCurrentDataSize = 0;
                    synchronized (mDumpLock) {
                        if (mDumpWkpDataQueue != null) {
                            mDumpWkpDataQueue.clear();
                        }
                    }
                    mWakeupCurrentDataSize = 0;
                    synchronized (mWakeupLock) {
                        if (mWkpDataQueue != null) {
                            mWkpDataQueue.clear();
                        }
                    }
                    /*********************/
                    if (!TextUtils.isEmpty(mParams.getDumpAudioPath())) {
                        mNeedDumpData = true;
                        Log.d(TAG, "dump path is " + mParams.getDumpAudioPath());
                        Log.d(TAG, "dump time is " + mParams.getDumpTime());
                    }
                    if (mParams.isUseCustomFeed()) {
                        Log.i(TAG, "isUseCustomFeed");
                        mWakeupKernel.startKernel(mParams);
                        transferState(EngineState.STATE_RUNNING);
                    } else {
                        // 启动SDK内部录音机
                        startRecorder(mParams, this);
                    }
                } else {
                    trackInvalidState("start");
                }
                break;
            case MSG_SET:
                final String setStr = (String) msg.obj;
                if (mState != EngineState.STATE_IDLE) {
                    mWakeupKernel.set(setStr);
                } else {
                    trackInvalidState("set info");
                }
                break;
            case MSG_RECORDER_START:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
                    mWakeupKernel.startKernel(mParams);
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("recorder start");
                }
                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(this);
                    releaseRecorder();
                    mWakeupKernel.stopKernel();
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("stop");
                }
                break;
            case MSG_RAW_RECEIVE_DATA:
                final byte[] rawBufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING && mOutListener != null) {
                    mOutListener.onRawDataReceived(rawBufferData, rawBufferData.length);
                }
                break;
            case MSG_RAW_WAKEUP_RECEIVED_DATA:
                final byte[] wkpData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING && mOutListener != null) {
                    mOutListener.onRawWakeupDataReceived(wkpData, wkpData.length);
                }
                break;
            case MSG_RESULT_RECEIVE_DATA:
                //此MSG包含录音机/自定义feed音频来源
                final byte[] bufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    /*******offer wakeup data to queue**********/
                    if (isUploadEnable()) {
                        synchronized (mLock) {
                            if (mNeedCache) {
                                if (mCurrentDataSize > mDataSizeThresh && mUploadCacheQueue.size() > 0) {
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
                            if (mDumpCurrentDataSize > mDumpDataThresh && mDumpWkpDataQueue.size() > 0) {
                                mDumpCurrentDataSize -= mDumpWkpDataQueue.remove().length;
                            }
                            mDumpCurrentDataSize += bufferData.length;
                            mDumpWkpDataQueue.offer(bufferData);
                        }
                    }
                    /*********************/
                    /***********wakeup audio(抛出唤醒词音频)**********/
                    synchronized (mWakeupLock) {
                        if (mWakeupCurrentDataSize > mDumpDataThresh && mWkpDataQueue.size() > 0) {
                            mWakeupCurrentDataSize -= mWkpDataQueue.remove().length;
                        }
                        mWakeupCurrentDataSize += bufferData.length;
                        mWkpDataQueue.offer(bufferData);
                    }
                    /***********wakeup audio**********/
                    //修改为从子线程抛出
                    if (mOutListener != null) {
                        mOutListener.onResultDataReceived(bufferData, bufferData.length, 0);
                    }
                    mWakeupKernel.feed(bufferData);
                    //sendMsgToCallbackMsgQueue(CallbackMsg.MSG_BUFFER_RECEIVED, bufferData);

                    if (mConfig.getOneshotConfig() != null && mOneshotKernel != null) {
                        mOneshotKernel.feed(bufferData);
                    }
                }
                break;
            case MSG_VOLUME_CHANGED:
                float rmsDb = (float) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RMS_CHANGED, rmsDb);
                } else {
                    trackInvalidState("volume changed");
                }
                break;
            case MSG_RESULT:
                AIResult result = (AIResult) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    try {
                        JSONObject resultJO = new JSONObject(result.getResultObject().toString());
                        long timeInterval = System.currentTimeMillis() - mLastWakeupTime;
                        mLastWakeupTime = System.currentTimeMillis();

                        if (resultJO.has("status")) {
                            int status = resultJO.optInt("status");
                            result.setRecordId("wakeup_" + Utils.getRecorderId() + "_" + System.currentTimeMillis());
                            JSONUtil.putQuietly(resultJO, "wakeupUid", result.getRecordId());
                            if (status == 4) {//pre wakeup happens
                                mHasPreWakeup = true;
                                mIsRealWakeup = false;//reset value of realWakeup state
                                if (mParams != null && mParams.isPreWakeupOn()) {
                                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
                                }
                            } else if (status == 1) {//real wakeup happens
                                /***dump****/
                                if (!TextUtils.isEmpty(mParams.getDumpAudioPath())) {
                                    startDumpWaitingTimerTask(mParams.getDumpAudioPath());
                                }
                                /***********/
                                /***抛出wakeup音频**/
                                sendWakeupAudioToCustom();
                                /***抛出wakeup音频**/
                                mIsRealWakeup = true;
                                if ((timeInterval <= DEFAULT_VALID_TIME_THRESH) && mHasPreWakeup) {
                                    mWakeupJson = resultJO;
                                }
                                if (updateTrails(mProfileState)) {
                                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
                                }

                                if (mConfig.getOneshotConfig() != null && mOneshotKernel != null) {
                                    mOneshotKernel.notifyWakeup(resultJO.optString("wakeupWord"));
                                }
                            } else if (status == 0) {
                                // 500ms 内有唤醒状态1，2就不再抛出状态0，状态2虽不抛出，但是有状态2一定有状态1
                                if (timeInterval > DEFAULT_VALID_TIME_THRESH || (mHasPreWakeup && !mIsRealWakeup))
                                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
                            }
                            if (status != 0)
                                AnalysisProxy.getInstance().updateConfig(false);

                            // 500ms内预唤醒发生了，本次会忽略本次唤醒/预唤醒音频(会缺少唤醒点前5s音频)问题
                            // 唤醒和预唤醒同时发生则不再产生TimerTask，由预唤醒生成的 TimerTask 上传唤醒音频
                            if (isUploadEnable() && (mHasPreWakeup || mIsRealWakeup) && ((timeInterval > DEFAULT_VALID_TIME_THRESH) || !(mHasPreWakeup && mIsRealWakeup))) {
                                mWakeupJson = resultJO;//update wakeup Json for upload
                                Log.d(TAG, "gourd enable, invoke timerTask");
                                startWakeupUploadWaitingTimerTask();
                            } else {
                                Log.w(TAG, "gourd disable or new wakeup happens within 500ms, ignore");
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    trackInvalidState("result");
                }
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(this);
                    }
                    releaseRecorder();
                    mWakeupKernel.releaseKernel();
                    if (mOneshotKernel != null) {
                        mOneshotKernel.releaseKernel();
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
                    error.setRecordId(Utils.getRecorderId());
                }
                Log.w(TAG, error.toString());
                if (mState == EngineState.STATE_RUNNING
                        || mState == EngineState.STATE_WAITING) {
                    unRegisterRecorderIfIsRecording(this);
                    mWakeupKernel.stopKernel();
                    transferState(EngineState.STATE_NEWED);
                }
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, msg.obj);
                uploadError(error);
                break;
            case MSG_FORCE_REQUEST_WAKEUP_RESULT:
                if (mState == EngineState.STATE_RUNNING) {
                    mWakeupKernel.forceRequestWakeupResult();
                } else {
                    trackInvalidState("MSG_FORCE_REQUEST_WAKEUP_RESULT");
                }
                break;
            default:
                break;
        }
    }

    private void sendWakeupAudioToCustom() {
        synchronized (mWakeupLock) {
            while (mWkpDataQueue.peek() != null) {
                byte[] dumpData = mWkpDataQueue.poll();
                sendMsgToInnerMsgQueue(EngineMsg.MSG_RAW_WAKEUP_RECEIVED_DATA, dumpData);
            }
            mWkpDataQueue.clear();
            mWakeupCurrentDataSize = 0;
        }
    }


    @Override
    public void clearObject() {
        super.clearObject();
        if (mParams != null)
            mParams = null;
        if (mConfig != null)
            mConfig = null;
        if (mWakeupKernel != null)
            mWakeupKernel = null;

        if (mOneshotKernel != null)
            mOneshotKernel = null;
    }

    @Override
    public void processNoSpeechError() {
        //do nothing
    }

    @Override
    public void processMaxSpeechError() {
        //do nothing
    }

    private void uploadError(AIError aiError) {
        JSONObject input = new JSONObject();
        try {
            if (mConfig != null) {
                input.put("config", mConfig.toJson());
            }
            if (mParams != null) {
                input.put("param", mParams.toJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Map<String, Object> entryMap = new HashMap<>();
        //添加message外面的字段
        entryMap.put("recordId", mRecorderId);
        entryMap.put("mode", "lite");
        entryMap.put("module", "local_exception");

        AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("local_wakeup_exception", "info", "local_exception",
                mRecorderId, input, aiError.getOutputJSON(), entryMap);
    }

    class MyWakeupListener implements WakeupThreadListener {

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onResults(AIResult result) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, result);
        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] buffer, int size) {
            if (mOutListener != null)
                mOutListener.onVprintCutDataReceived(dataType, buffer, size);
        }

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

            if (mOutListener != null) {
                mOutListener.onOneShot(word, buffer);
//                mOutListener.onOneshot(word, buffer);
            }
        }

        @Override
        public void onNotOneshot(String word) {
            if (mOutListener != null) {
//                mOutListener.onNotOneshot(word);
                mOutListener.onNotOneShot(word);
            }
        }

    }
}

