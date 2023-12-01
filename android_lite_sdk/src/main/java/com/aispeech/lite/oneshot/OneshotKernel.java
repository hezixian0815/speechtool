package com.aispeech.lite.oneshot;


import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.auth.ProfileState;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AITimer;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.lite.AIErrorProcessor;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AIThreadFactory;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.OneshotConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.message.MessageQueue;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.vad.VadKernel;
import com.aispeech.lite.vad.VadKernelListener;

import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;


/**
 * oneshot processor
 *
 * @author hehr
 */
public class OneshotKernel implements Runnable, VadKernelListener {

    private static final String TAG = "oneshot";
    protected MessageQueue mQueue;
    protected AIErrorProcessor mErrorProcessor;
    private AIThreadFactory myThreadFactory;
    private ExecutorService mPool;
    private Semaphore mSemaphore = new Semaphore(0);
    public static final int INTERVAL = AISpeech.intervalTime; // read buffer interval in ms.
    private OneshotListener mListener;
    private VadKernel mVadKernel;
    private OneshotCache mCache;
    private ProfileState mProfileState;
    private EngineState mState = EngineState.STATE_IDLE;
    /**
     * oneshot check time
     */
    private int middleTime = 500;//毫秒

    private OneshotCheckTask mTask = null;
    /**
     * oneshot 需要响应的唤醒词
     */
    private String[] oneshotWord;


    public OneshotKernel(OneshotListener listener) {
        mProfileState = AISpeech.getProfile().isProfileValid(Scope.LOCAL_ONESHOT);
        if (mProfileState.isValid()) {
            this.mQueue = new MessageQueue();
            mErrorProcessor = new AIErrorProcessor();
            myThreadFactory = new AIThreadFactory(TAG, Thread.NORM_PRIORITY);
            mPool = Executors.newSingleThreadExecutor(myThreadFactory);
            mPool.execute(this);
            mListener = listener;
        } else {
            showErrorMessage(mProfileState);
        }

    }

    /**
     * 创建引擎
     *
     * @param config {@link }
     */
    public void newKernel(OneshotConfig config) {
        if (mProfileState != null && mProfileState.isValid()) {
            sendMessage(new Message(OneshotEvent.NEW, config));
        } else {
            showErrorMessage(mProfileState);
        }
    }

    /**
     * feed音频
     *
     * @param data 音频数据
     */
    public void feed(byte[] data) {
        if (mProfileState != null && mProfileState.isValid()) {
            byte[] buffer = new byte[data.length];
            System.arraycopy(data, 0, buffer, 0, data.length);
            sendMessage(new Message(OneshotEvent.FEED, buffer));
        } else {
            showErrorMessage(mProfileState);
        }
    }

    /**
     * 通知主唤醒词唤醒消息
     *
     * @param word 唤醒词信息
     */
    public void notifyWakeup(String word) {
        if (mProfileState != null && mProfileState.isValid()) {
            if (isOneshotWord(word)) {
                sendMessage(new Message(OneshotEvent.NOTIFY_WAKEUP, word));
            } else {
                Log.d(TAG, "drop illegal notify oneshot word : " + word);
            }
        } else {
            showErrorMessage(mProfileState);
        }
    }

    /**
     * 释放引擎
     */
    public void releaseKernel() {
        if (mProfileState != null && mProfileState.isValid()) {
            Log.d(TAG, "releaseKernel");
            sendMessage(new Message(Message.MSG_RELEASE));
            semaphoreP();
            mPool.shutdown();
            mPool = null;
            if (myThreadFactory != null) {
                myThreadFactory = null;
            }
        } else {
            showErrorMessage(mProfileState);
        }
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

    /**
     * oneshot  event
     */
    static class OneshotEvent {
        /**
         * 错误
         */
        public static final int ERROR = -1;
        /**
         * 初始化
         */
        public static final int NEW = 0;
        /**
         * feed 音频
         */
        public static final int FEED = 1;
        /**
         * 唤醒事件
         */
        public static final int NOTIFY_WAKEUP = 4;
        /**
         * 检测到人声
         */
        public static final int VAD_BEGIN = 5;
        /**
         * 计时器超时
         */
        public static final int TIMEOUT = 6;
        /**
         * release 引擎
         */
        public static final int RELEASE = 7;

    }

    /**
     * 当前回溯音频的唤醒词
     */
    private String currentWakeupWord;

    @Override
    public void run() {
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case OneshotEvent.NEW:
                    if (mState == EngineState.STATE_IDLE) {
                        OneshotConfig config = (OneshotConfig) message.mObject;
                        int status = 0;
                        try {
                            status = copyAssetsRes(config.getVadConfig());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (status == AIConstant.OPT_FAILED) {
                            sendMessage(new Message(OneshotEvent.ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED)));
                            break;
                        }
                        Log.d(TAG, "oneshot config : " + config.toString());
                        oneshotWord = config.getWords();
                        Log.d(TAG, "oneshot wakeup word : " + oneshotWord);
                        middleTime = config.getMiddleTime();
                        Log.d(TAG, "oneshot middle time : " + middleTime);
                        int size = config.getCacheAudioTime() / INTERVAL;
                        Log.d(TAG, "set cache size : " + size);
                        mVadKernel = new VadKernel("oneshot", this);
                        mVadKernel.newKernel(config.getVadConfig());
                        mCache = new OneshotCache<byte[]>(size);
                    } else {
                        trackInvalidState("new");
                    }
                    break;
                case OneshotEvent.FEED: //feed音频
                    if (mState != EngineState.STATE_IDLE) {
                        byte[] data = (byte[]) message.mObject;
                        if (mCache != null) {
                            mCache.offer(data);
                        }
                        if (isFeedVad() && mVadKernel != null) {
                            mVadKernel.feed(data); //送vad音频需要在唤醒之后才开启
                        }
                        if (mState == EngineState.STATE_NEWED) {
                            transferState(EngineState.STATE_RUNNING);
                        }
                    } else {
                        trackInvalidState("feed");
                    }
                    break;
                case OneshotEvent.NOTIFY_WAKEUP:
                    if (mState == EngineState.STATE_RUNNING) {
                        String word = (String) message.mObject;
                        Log.d(TAG, "notify wakeup word :" + word);
                        currentWakeupWord = word;
                        setFeedVad(true);
                        startTimer();
                        if (mVadKernel != null) {
                            VadParams vadParams = new VadParams();
                            vadParams.setPauseTime(0);//oneshot 设置vad pauseTime 0
                            mVadKernel.startKernel(vadParams);
                        }
                        transferState(EngineState.STATE_WAITING);
                    } else {
                        trackInvalidState("notify.wakeup");
                    }
                    break;
                case OneshotEvent.VAD_BEGIN:
                    if (mState == EngineState.STATE_WAITING) {
                        if (mListener != null) {
                            mListener.onOneshot(currentWakeupWord, mCache);
                        }
                        cancelTimer();
                        setFeedVad(false);
                        if (mVadKernel != null) {
                            mVadKernel.stopKernel();
                        }
                        transferState(EngineState.STATE_NEWED);
                    } else {
                        trackInvalidState("vad.begin");
                    }

                    break;
                case OneshotEvent.TIMEOUT:
                    if (mState == EngineState.STATE_WAITING) {
                        if (mListener != null) {
                            mListener.onNotOneshot(currentWakeupWord);
                        }
                        cancelTimer();
                        setFeedVad(false);
                        if (mVadKernel != null) {
                            mVadKernel.stopKernel();
                        }
                        transferState(EngineState.STATE_NEWED);
                    } else {
                        trackInvalidState("timeout");
                    }

                    break;
                case OneshotEvent.ERROR:
                    if (mListener != null) {
                        mListener.onError((AIError) message.mObject);
                    }
                    break;
                case OneshotEvent.RELEASE:
                    if (mVadKernel != null) {
                        mVadKernel.releaseKernel();
                        mVadKernel = null;
                    }
                    if (mCache != null) {
                        mCache.clear();
                        mCache = null;
                    }
                    // 修复资源一直不释放的问题
                    isReleased = true;

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


    private void startTimer() {
        if (mTask != null) {
            mTask.cancel();
            mTask = null;
        }
        mTask = new OneshotCheckTask();
        AITimer.getInstance().schedule(mTask, middleTime);
    }

    private void cancelTimer() {
        if (mTask != null) {
            mTask.cancel();
            mTask = null;
        }
    }

    class OneshotCheckTask extends TimerTask {
        @Override
        public void run() {
            Log.d(TAG, "oneshot check timer execute");
            if (mVadKernel != null) {
                mVadKernel.stopKernel();
            }
            sendMessage(new Message(OneshotEvent.TIMEOUT));
        }
    }


    /**
     * 音频是否feedVad
     */
    private volatile boolean isFeedVad = false;

    private boolean isFeedVad() {
        return isFeedVad;
    }

    private void setFeedVad(boolean feedVad) {
        isFeedVad = feedVad;
    }

    /**
     * 类说明： 引擎内部状态列表
     */
    public enum EngineState {
        /**
         * 空闲状态
         */
        STATE_IDLE(0),
        /**
         * 初始化完毕等待就绪状态
         */
        STATE_NEWED(1),
        /**
         * 录音中或运行中状态
         */
        STATE_RUNNING(2),
        /**
         * 录音停止，等待结果状态
         */
        STATE_WAITING(3),
        /**
         * 错误状态
         */
        STATE_ERROR(4);

        private int value;

        EngineState(int value) {
            this.value = value;
        }

        /**
         * @return the value
         */
        public int getValue() {
            return value;
        }
    }


    protected void trackInvalidState(String msg) {
        Log.w(TAG, "Invalid State：" + mState.name() + " when MSG: " + msg);
    }

    protected void transferState(EngineState nextState) {
        Log.d(TAG, "transfer:" + mState + " to:" + nextState);
        mState = nextState;
    }

    /**
     * 是否oneshot 唤醒词
     *
     * @param word 检测词汇
     * @return boolean
     */
    private boolean isOneshotWord(String word) {
        if (oneshotWord == null || oneshotWord.length == 0) {
            return false;
        }

        for (String var : oneshotWord) {
            if (TextUtils.equals(var, word)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void onInit(int status) {
        Log.d(TAG, "oneshot kernel init status : " + status);

        if (status == AIConstant.OPT_SUCCESS) {
            transferState(EngineState.STATE_NEWED);
        }

        if (mListener != null) {
            mListener.onInit(status);
        }
    }

    @Override
    public void onVadStart(String recordID) {
        Log.d(TAG, "vad.begin");
        sendMessage(new Message(OneshotEvent.VAD_BEGIN));
    }

    @Override
    public void onVadEnd(String recordID) {
        Log.d(TAG, "vad.end");
    }

    @Override
    public void onRmsChanged(float rmsDb) {

    }

    @Override
    public void onBufferReceived(byte[] data) {

    }

    @Override
    public void onResults(String result) {

    }

    @Override
    public void onError(AIError error) {
        Log.e(TAG, "vad onError : " + error.toString());
        sendMessage(new Message(OneshotEvent.ERROR, error));
    }

    protected void showErrorMessage(ProfileState state) {
        AIError error = new AIError();
        if (state == null) {
            error.setErrId(AIError.ERR_SDK_NOT_INIT);
            error.setError(AIError.ERR_DESCRIPTION_ERR_SDK_NOT_INIT);
        } else {
            error.setErrId(state.getAuthErrMsg().getId());
            error.setError(state.getAuthErrMsg().getValue());
        }
        sendMessage(new Message(OneshotEvent.ERROR, error));

    }


    protected void innerRelease() {
        releaseMessage();
        semaphoreV();
    }

    protected void clearMessage() {
        Log.d(TAG, "clear message in queue");
        if (mQueue != null) {
            mQueue.clear();
        }
    }

    protected void sendMessage(Message msg) {
        if (mQueue != null) {
            mQueue.put(msg);
        }

    }

    protected Message waitMessage() {
        if (mQueue != null) {
            return (Message) mQueue.get();
        } else {
            return null;
        }
    }

    private void releaseMessage() {
        if (mQueue != null) {
            mQueue.clear();
            mQueue = null;
        }
    }

    protected void semaphoreP() {
        try {
            Log.i(TAG, "Semaphore acquire before");
            mSemaphore.acquire();
            Log.i(TAG, "Semaphore acquire end");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void semaphoreV() {
        mSemaphore.release();
        Log.i(TAG, "Semaphore release");
    }

    /**
     * 拷贝assets目录下的制定资源
     *
     * @param config 配置
     * @return status status
     */
    protected int copyAssetsRes(AIEngineConfig config) {
        int status = AIConstant.OPT_SUCCESS;
        String[] assetsResNames = config.getAssetsResNames();
        Map<String, String> assetsResMd5sumMap = config.getAssetsResMd5sum();
        if (assetsResNames != null && assetsResNames.length > 0) {
            for (String resName : assetsResNames) {
                String resMd5sumName = null;
                if (assetsResMd5sumMap != null) {
                    resMd5sumName = assetsResMd5sumMap.get(resName);
                }
                int ret = Util.copyResource(AISpeech.getContext(), resName, resMd5sumName);
                if (ret == -1) {
                    Log.e(TAG, "file " + resName + " not found in assest folder, Did you forget add it?");
                    return status;
                }
            }
        }
        return status;
    }

}
