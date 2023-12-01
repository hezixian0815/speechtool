package com.aispeech.lite.dmasp;

import android.os.Message;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AudioHelper;
import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.LocalDmaspConfig;
import com.aispeech.lite.function.ICarFunction;
import com.aispeech.lite.param.DmaspParams;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by wuwei on 2018/3/29.
 */

public class DmaspProcessor extends BaseProcessor {
    public static String TAG = "DmaspProcessor";
    private DmaspParams mParams;
    private String mRecordId;

    //dmasp&nwakeup
    private DmaspKernel mDmaspKernel;

    private SpeechListener mOutListener;

    private LocalDmaspConfig mConfig;

    // Dmasp 输出的通道数 支持 4Mic 2Mic
    private int dmaspChannelCountr = 4;

    protected static final int DEFAULT_VALID_TIME_THRESH = 500;//有效唤醒时长，默认500ms

    public void init(SpeechListener listener, LocalDmaspConfig dmaspConfig) {
        mOutListener = listener;
        this.mBaseConfig = dmaspConfig;
        threadCount = 1;
        mConfig = dmaspConfig;
        dmaspChannelCountr = mConfig.getDmaspChannelCount();

        mScope = Scope.DMASP;
        init(listener, mConfig.getContext(), TAG);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }

    public void start(DmaspParams param) {
        if (isAuthorized()) {
            this.mParams = param;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage();
        }
    }

    /**
     * 单独唤醒开关
     *
     * @param enable true 打开 ，false 关闭
     */
    public void setNWakeupEnable(boolean enable) {
        if (mState == EngineState.STATE_RUNNING) {
            if (mDmaspKernel != null) {
                mDmaspKernel.setNWakeupEnable(enable);
            }
        }
    }

    /**
     * 获取唤醒资源是否带VAD状态流
     *
     * @return true 带状态流 false 不带状态流
     */
    public boolean isWakeupSsp() {
        if (mDmaspKernel != null) {
            return mDmaspKernel.isWakeupSsp();
        }
        return false;
    }

    /**
     * 自定义feed音频
     *
     * @param data 数据
     * @param size 大小
     */
    @Override
    public void feedData(byte[] data, int size) {
        if (isAuthorized()) {
            if (mConfig != null) {
                byte[] bufferData = new byte[size];
                System.arraycopy(data, 0, bufferData, 0, size);
                sendMsgToInnerMsgQueue(EngineMsg.MSG_RAW_RECEIVE_DATA, bufferData);
            }
        } else {
            showErrorMessage();
        }
    }

    public synchronized int getValueOf(String param) {
        if (mState != EngineState.STATE_IDLE && mDmaspKernel != null) {
            return mDmaspKernel.getValueOf(param);
        } else {
            return -1;
        }
    }

    public void resetDriveMode() {
        if (mDmaspKernel instanceof ICarFunction) {
            ((ICarFunction) mDmaspKernel).resetDriveMode();
        }
    }

    public synchronized int getDriveMode() {
        if (!mProfileState.isValid() || mDmaspKernel == null) {
            return -1;
        }
        if (mDmaspKernel instanceof ICarFunction) {
            return ((ICarFunction) mDmaspKernel).getDriveMode();
        }
        return -1;
    }

    public synchronized void setDriveMode(int driveMode) {

        if (!mProfileState.isValid() || mDmaspKernel == null) {
            return;
        }

        if (mDmaspKernel instanceof ICarFunction) {
            ((ICarFunction) mDmaspKernel).setDriveMode(driveMode);
        }

    }

    public synchronized void setDriveMode(int driveMode, int wakeupChannelMask) {

        if (!mProfileState.isValid() || mDmaspKernel == null) {
            return;
        }
        if (mDmaspKernel instanceof ICarFunction) {
            ((ICarFunction) mDmaspKernel).setDriveMode(driveMode, wakeupChannelMask);
        }

    }

    public synchronized void setDoaManually(int doa) {
        if (!mProfileState.isValid() || mDmaspKernel == null) {
            return;
        }
        if (mDmaspKernel instanceof ICarFunction) {
            ((ICarFunction) mDmaspKernel).setDoaManually(doa);
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
                    int status = copyAssetsRes(mConfig);
                    Log.d(TAG, "status = " + status);
                    if (status == AIConstant.OPT_FAILED) {
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                        break;
                    }
                    mDataSizeThresh = mConfig.getDmaspChannelCount() * AISpeech.uploadAudioTime * 32;//当前上传音频长度以实际配置为主
                    mDmaspKernel = new DmaspKernel(mConfig, new MyDmaspKernelListener());
                    mDmaspKernel.newKernel();
                } else {
                    trackInvalidState("new");
                }
                break;
            case MSG_START:
                if (mState == EngineState.STATE_NEWED) {
                    mRecordId = Utils.get_recordid();
                    if (mDmaspKernel != null) {
                        Log.i(TAG, "start dmasp " + mParams.toString());
                        mDmaspKernel.startKernel(mParams);
                    }
                    resetWakeupCacheQueue();
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("start");
                }
                break;
            case MSG_SET:
                final String setStr = (String) msg.obj;
                if (mState != EngineState.STATE_IDLE) {
                    mDmaspKernel.set(setStr);
                } else {
                    trackInvalidState("set info");
                }
                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                    unRegisterRecorderIfIsRecording(this);
                    if (mDmaspKernel != null) {
                        mDmaspKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                } else {
                    trackInvalidState("stop");
                }
                break;
            case MSG_RAW_RECEIVE_DATA:
                if (mDmaspKernel != null && mState == EngineState.STATE_RUNNING) {
                    final byte[] rawBufferData = (byte[]) msg.obj;
                    mDmaspKernel.feed(rawBufferData);
                    if (mOutListener != null) {
                        mOutListener.onRawDataReceived(rawBufferData, rawBufferData.length);
                    }

                    /*******offer wakeup data to queue**********/
                    offerUploadQueueData(rawBufferData);
                    /**********dump***********/

                }
                break;
            case MSG_RESULT_RECEIVE_DATA:
                byte[] bufferData = (byte[]) msg.obj;
                int doa = msg.arg1;
                if (mDmaspKernel != null && mState == EngineState.STATE_RUNNING) {
                    // 多路音频分离
                    byte[][] original = new byte[dmaspChannelCountr][];
                    for (int i = 0; i < dmaspChannelCountr; i++) {
                        original[i] = AudioHelper.splitOriginalChannel(bufferData, i, dmaspChannelCountr);
                    }

                    byte[] doaChannelData = new byte[bufferData.length / dmaspChannelCountr];

                    if (mOutListener != null) {
                        if (mDmaspKernel.isDriveModeEntire() || doa == -1) {
                            // 全车模式或未成功定位doa 返回多路合一 单路数据
                            byte[] merge = AudioHelper.mixRawAudioBytes(original);
                            mOutListener.onResultDataReceived(merge, merge.length);
                            mDmaspKernel.saveDriveModeData(merge, merge.length);
                        } else {
                            // 返回对应驾驶模式下 doa 单路数据
                            switch (doa) {
                                case ICarFunction.CAR_DOA_MAIN:
                                    doaChannelData = original[0];
                                    break;
                                case ICarFunction.CAR_DOA_COPILOT:
                                    doaChannelData = original[1];
                                    break;
                                case ICarFunction.CAR_DOA_LEFT_BACKSEAT:
                                    doaChannelData = original[2];
                                    break;
                                case ICarFunction.CAR_DOA_RIGHT_BACKSEAT:
                                    doaChannelData = original[3];
                                    break;
                                default:
                                    break;
                            }

                            if (ICarFunction.CAR_DOA_MAIN <= doa && doa <= ICarFunction.CAR_DOA_RIGHT_BACKSEAT) {
                                mOutListener.onResultDataReceived(doaChannelData, doaChannelData.length);
                                mDmaspKernel.saveDriveModeData(doaChannelData, doaChannelData.length);
                            }
                        }

                        // 返回原始四路数据
                        mOutListener.onResultDataReceived(bufferData, bufferData.length, 0);
                    }
                }
                break;
            case MSG_RESULT:
                AIResult result = (AIResult) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    result.setRecordId("wakeup_" + Utils.getRecorderId() + "_" + System.currentTimeMillis());
                    if (mOutListener != null) {
                        Log.d(TAG, "------Dmasp Processor---wakeup- ");
                        mOutListener.onResults(result);
                    }


                    JSONObject wkpResultObj = result.getResultJSONObject();
                    JSONUtil.putQuietly(wkpResultObj, "wakeupUid", result.getRecordId());
                    if (!wkpResultObj.has("status")) {
                        return;
                    }
                    long timeInterval = System.currentTimeMillis() - mLastWakeupTime;
                    mLastWakeupTime = System.currentTimeMillis();

                    int status = wkpResultObj.optInt("status");
                    if (status == 4) {//pre wakeup happens
                        mHasPreWakeup = true;
                        mIsRealWakeup = false;//reset value of realWakeup state

                    } else if (status == 1 || status == 2) {//real wakeup happens
                        mIsRealWakeup = true;
                        if ((timeInterval <= DEFAULT_VALID_TIME_THRESH) && mHasPreWakeup) {
                            mWakeupJson = wkpResultObj;
                        }
                    }

                    if (status != 0) {
                        AnalysisProxy.getInstance().updateConfig(false);
                    }
                    // 500ms内预唤醒发生了，本次会忽略本次唤醒/预唤醒音频(会缺少唤醒点前5s音频)问题
                    // 唤醒和预唤醒同时发生则不再产生TimerTask，由预唤醒生成的 TimerTask 上传唤醒音频
                    handleWakeupIgnore(wkpResultObj, timeInterval);
                } else {
                    trackInvalidState("result");
                }
                break;
            case MSG_DOA:
                String sslDoa = (String) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mOutListener != null) {
                        mOutListener.onDoaResult(sslDoa);
                    }
                }
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(this);
                    }
                    //releaseRecorder();
                    if (mDmaspKernel != null) {
                        mDmaspKernel.releaseKernel();
                    }
                    clearObject();//清除实例
                    transferState(EngineState.STATE_IDLE);
                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_ERROR:
                AIError error = (AIError) msg.obj;
                Log.w(TAG, error.toString());
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    //unRegisterRecorderIfIsRecording(this);
                    if (mDmaspKernel != null) {
                        mDmaspKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                }
                if (mOutListener != null) {
                    mOutListener.onError((AIError) msg.obj);
                }
                uploadError(error);
                break;
            default:
                break;
        }
    }


    private class MyDmaspKernelListener implements DmaspKernelListener {

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onResultBufferReceived(byte[] data) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT_RECEIVE_DATA, data);
        }

        @Override
        public void onResultBufferReceived(int doa, byte[] data) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT_RECEIVE_DATA, doa, -1, data);
        }

        @Override
        public void onResults(AIResult result) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, result);
        }


        @Override
        public void onDoa(int ssl, int doa) {
            String sslDoa = ssl + String.valueOf(doa);
            sendMsgToInnerMsgQueue(EngineMsg.MSG_DOA, sslDoa);
        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] data, int size) {
            if (mOutListener != null) {
                mOutListener.onVprintCutDataReceived(dataType, data, size);
            }
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }
    }


    @Override
    public void clearObject() {
        super.clearObject();
        if (mParams != null)
            mParams = null;
        if (mConfig != null)
            mConfig = null;
        if (mDmaspKernel != null)
            mDmaspKernel = null;
    }

    @Override
    public void processNoSpeechError() {
        //do nothing
    }

    @Override
    public void processMaxSpeechError() {
        //do nothing
    }

    private void handleWakeupIgnore(JSONObject resultJO, long timeInterval) {
        if ((mHasPreWakeup || mIsRealWakeup) && ((timeInterval > DEFAULT_VALID_TIME_THRESH) || !(mHasPreWakeup && mIsRealWakeup))) {
            if (isUploadEnable()) {
                mWakeupJson = resultJO;//update wakeup Json for upload
                Log.d(TAG, "upload enable, invoke upload timerTask");
                startWakeupUploadWaitingTimerTask();
            }
        } else {
            Log.w(TAG, "new wakeup happens within 500ms, ignore");
        }
    }

    private void offerUploadQueueData(byte[] bufferData) {
        if (isUploadEnable()) {
            synchronized (mLock) {
                if (mNeedCache) {
                    if (mCurrentDataSize >= mDataSizeThresh && !mUploadCacheQueue.isEmpty()) {
                        mCurrentDataSize -= mUploadCacheQueue.remove().length;
                    }
                    mCurrentDataSize += bufferData.length;
                    mUploadCacheQueue.offer(bufferData);
                }
            }
        }
    }

    private void resetWakeupCacheQueue() {
        synchronized (mLock) {
            mCurrentDataSize = 0;
            if (mUploadCacheQueue != null) {
                mUploadCacheQueue.clear();
            }
        }
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
        entryMap.put("recordId", getRecorderId());
        entryMap.put("mode", "lite");
        entryMap.put("module", "local_exception");
        AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("local_wakeup_exception", "info", "local_exception",
                getRecorderId(), input, aiError.getOutputJSON(), entryMap);
    }
}
