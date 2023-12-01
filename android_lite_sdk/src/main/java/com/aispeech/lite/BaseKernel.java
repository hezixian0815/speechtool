package com.aispeech.lite;

import com.aispeech.AIError;
import com.aispeech.auth.AIProfile;
import com.aispeech.auth.ProfileState;
import com.aispeech.common.Log;
import com.aispeech.export.Vocab;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.LocalSignalProcessingConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.message.MessageQueue;
import com.aispeech.lite.param.FeedbackJsonParams;
import com.aispeech.lite.param.FeedbackParams;
import com.aispeech.lite.param.MultiModalParams;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.param.TriggerIntentParams;
import com.aispeech.util.KernelWatchDog;
import com.aispeech.util.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by yu on 2018/5/16.
 */

public class BaseKernel implements Runnable {

    protected MessageQueue<Message> mQueue;
    protected AIErrorProcessor mErrorProcessor;
    protected volatile EngineState mState = EngineState.STATE_IDLE;
    protected AIProfile profile;
    private String tag = "BaseKernel";
    private AIThreadFactory myThreadFactory;
    private ExecutorService mPool;
    private Semaphore mSemaphore = new Semaphore(0);
    private BaseListener mListener;
    private AtomicInteger _tick;
    private Thread innerThread;

    public BaseKernel(String tag, BaseListener listener) {
        this.tag = tag;
        this.mListener = listener;
        this.mQueue = new MessageQueue<>();
        this.mQueue.setTAG(tag);
        this.mQueue.setMaxMessageQueueSize(AISpeech.maxMessageQueueSize);
        mErrorProcessor = new AIErrorProcessor();
        myThreadFactory = new AIThreadFactory(getInnerThreadName(), Thread.NORM_PRIORITY);
        mPool = Executors.newSingleThreadExecutor(myThreadFactory);
        mPool.execute(BaseKernel.this);
        _tick = new AtomicInteger(0);
    }

    public int getTick() {
        return _tick.get();
    }

    public void doTick() {
        _tick.incrementAndGet();
    }

    public Thread getInnerThread() {
        return innerThread;
    }

    public void enableWatchDog() {
        KernelWatchDog.getInstance().addChecker(this);
    }

    /**
     * 如果需要自定义内部的线程池命名名称，可重写此方法
     *
     * @return 默认返回tag
     */
    public String getInnerThreadName() {
        return tag;
    }

    public void setMaxVoiceQueueSize(int maxVoiceQueueSize, int ignoreSize) {
        Log.d(tag, "setMaxVoiceQueueSize:" + maxVoiceQueueSize + "," + ignoreSize);
        if (this.mQueue != null) {
            mQueue.setMaxVoiceQueueSize(maxVoiceQueueSize, ignoreSize);
        } else {
            Log.d(tag, "mQueue is null");
        }
    }

    public void setMaxMessageQueueSize(int maxQueueSize) {
        if (this.mQueue != null) {
            if (maxQueueSize < 0) {
                this.mQueue.setMaxMessageQueueSize(AISpeech.maxMessageQueueSize);
            } else {
                this.mQueue.setMaxMessageQueueSize(maxQueueSize);
            }
        } else {
            Log.d(tag, "mQueue is null");
        }
    }

    protected boolean updateTrails(AIProfile profile, ProfileState profileState, String scope) {
        if (profileState.getAuthType() == ProfileState.AUTH_TYPE.TRIAL
                && profileState.getTimesLimit() != -1) {
            ProfileState state = profile.isProfileValid(scope);
            if (!state.isValid()) {
                showErrorMessage(state);
                return false;
            } else {
                profile.updateUsedTimes(scope);
                return true;
            }
        } else {
            return true;
        }
    }

    public synchronized int getValueOf(String param) {
        return -1;
    }

    public String getNewConf() {
        return "";
    }

    public String getStartConf() {
        return "";
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
        if (mListener != null) {
            mListener.onError(error);
        }
    }

    public void tick() {
        sendMessage(new Message(Message.MSG_TICK));
    }

    public void startKernel(SpeechParams param) {
        Log.d(tag, "startKernel");
        try {
            sendMessage(new Message(Message.MSG_START, param.clone()));
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    public void newKernel(AIEngineConfig config) {
        Log.d(tag, "newKernel");
        try {
            sendMessage(new Message(Message.MSG_NEW, config.clone()));
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置参数
     *
     * @param setParam 参数
     */
    public void set(String setParam) {
        sendMessage(new Message(Message.MSG_SET, setParam));
    }

    public int get(String setParam) {
        return 0;
    }

    /**
     * 更新引擎配置
     *
     * @param updateParam
     */
    public void update(String updateParam) {
        Log.d(tag, "updateKernel");
        sendMessage(new Message(Message.MSG_UPDATE, updateParam));
    }

    public void update(AIEngineConfig config) {
        Log.i(tag, "update config");
        try {
            sendMessage(new Message(Message.MSG_UPDATE, config.clone()));
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }


    /**
     * 关闭对话
     */
    public void close() {
        Log.i(tag, "close");
        sendMessage(new Message(Message.MSG_CLOSE));
    }

    /**
     * feedback
     *
     * @param params feedback 参数
     */
    public void feedback(FeedbackParams params) {
        Log.i(tag, "feedback");
        sendMessage(new Message(Message.MSG_FEEDBACK, params));
    }

    /**
     * feedback (feed数据到私有云)
     *
     * @param params feedback 参数
     */
    public void feedback2PRIVCloud(FeedbackJsonParams params){
        Log.d(tag, "feedback2PRIVCloud");
        sendMessage(new Message(Message.MSG_FEEDBACK_2_PRIV_CLOUD, params));
    }

    /**
     * triggerIntent
     *
     * @param params triggerIntent 参数
     */
    public void triggerIntent(TriggerIntentParams params) {
        Log.i(tag, "triggerIntent");
        sendMessage(new Message(Message.MSG_TRIGGER_INTENT, params));
    }

    /**
     * 同步多模态数据
     *
     * @param params MultiModalParams 参数
     */
    public void async(MultiModalParams params) {
        Log.i(tag, "async");
        sendMessage(new Message(Message.MSG_ASYNC, params));
    }

    /**
     * 更新内置语义vocab
     *
     * @param updateParam
     */
    public void updateNaviVocab(String updateParam) {
        Log.d(tag, "updateNaviVocab param:" + updateParam);
        sendMessage(new Message(Message.MSG_UPDATE_NAVI_VOCAB, updateParam));
    }

    /**
     * 更新vocab
     *
     * @param updateParam
     */
    public void updateVocab(String updateParam) {
        Log.d(tag, "updateVocab");
        sendMessage(new Message(Message.MSG_UPDATE_VOCAB, updateParam));
    }

    public void updateVocab(Vocab vocab) {
        Log.d(tag, "updateVocab");
        sendMessage(new Message(Message.MSG_UPDATE_VOCAB, vocab));
    }

    public void updateVocabs(Vocab ... vocabs){
        Log.d(tag, "updateVocabs");
        sendMessage(new Message(Message.MSG_UPDATE_VOCAB, vocabs.clone()));
    }

    public void feed(byte[] data) {
        sendMessage(new Message(Message.MSG_FEED_DATA_BY_STREAM, data));
    }

    public void feed(byte [][] data) {
        sendMessage(new Message(Message.MSG_FEED_BF_VAD_DATA_BY_STREAM, data));
    }

    public void cancelKernel() {
        Log.d(tag, "cancelKernel");
        sendMessage(new Message(Message.MSG_CANCEL));
    }

    public void stopKernel() {
        Log.d(tag, "stopKernel");
        sendMessage(new Message(Message.MSG_STOP));
    }

    public void releaseKernel() {
        Log.d(tag, "releaseKernel");
        sendMessage(new Message(Message.MSG_RELEASE));
        semaphoreP();
        mPool.shutdown();
        mPool = null;
        if (myThreadFactory != null) {
            myThreadFactory = null;
        }
    }

    public void forceRequestWakeupResult() {
        Log.d(tag, "forceRequestWakeupResult");
        sendMessage(new Message(Message.MSG_FORCE_REQUEST_WAKEUP_RESULT));
    }

    protected void innerRelease() {
        releaseMessage();
        semaphoreV();
        KernelWatchDog.getInstance().removeChecker(this);
    }

    public void clearMessage() {
        Log.d(tag, "clear message in queue");
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
            return mQueue.get();
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
            Log.i(tag, "Semaphore acquire before");
            mSemaphore.acquire();
            Log.i(tag, "Semaphore acquire end");
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
    }

    protected void semaphoreV() {
        mSemaphore.release();
        Log.i(tag, "Semaphore release");
    }

    protected void trackInvalidState(String msg) {
        Log.w(tag, "Invalid State：" + mState.name() + " when MSG: " + msg);
    }

    protected void transferState(EngineState nextState) {
        Log.d(tag, "transfer:" + mState + " to:" + nextState);
        mState = nextState;
    }

    @Override
    public void run() {
        Utils.checkThreadAffinity();
        innerThread = Thread.currentThread();
    }

    public AIProfile getProfile() {
        return profile;
    }

    public void setProfile(AIProfile profile) {
        this.profile = profile;
    }

    protected boolean useBuiltinWakeupWords(BuiltinWakeupWords builtinWakeupWords, LocalSignalProcessingConfig config) {
        Log.d(tag, builtinWakeupWords.toString());
        if (config == null || !builtinWakeupWords.isUseBuiltInWakeupWords())
            return true;

        if (!builtinWakeupWords.checkWords(config.getWakupWords())) {
            Log.d(tag, "useBuiltinWakeupWords 唤醒词检查不通过");
            return false;
        }

        float[] builtinThresh = builtinWakeupWords.getThresh(config.getWakupWords());
        if (builtinThresh == null) {
            Log.d(tag, "useBuiltinWakeupWords 获取资源内置阈值error");
            return false;
        }
        config.setThreshs(builtinThresh);

        float[] builtinThreshLoud = builtinWakeupWords.getThreshLoud(config.getWakupWords());
        if (builtinThreshLoud == null) {
            Log.d(tag, "useBuiltinWakeupWords 获取资源内置Loud阈值error");
            return false;
        }
        config.setThreshs2(builtinThreshLoud);

        Log.d(tag, "useBuiltinWakeupWords success");
        return true;
    }

    protected byte[] checkNeedCopyResultData(SpeechParams params, byte[] data, int size) {
        if (params == null || params.isNeedCopyResultData()) {
            byte[] result = new byte[size];
            System.arraycopy(data, 0, result, 0, size);
            return result;
        } else {
            return data;
        }
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
        STATE_ERROR(4),
        /**
         * 取消状态，该状态暂时给callback handler用
         */
        STATE_CANCELED(5);

        private int value;

        private EngineState(int value) {
            this.value = value;
        }

        /**
         * @return the value
         * @Override
         */
        public int getValue() {
            return value;
        }
    }
}
