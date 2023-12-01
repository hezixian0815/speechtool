package com.aispeech.lite.tts;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.AudioTrack.OnPlaybackPositionUpdateListener;
import android.os.Build;

import com.aispeech.AIError;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.AIThreadFactory;
import com.aispeech.lite.audio.AIPlayerListener;
import com.aispeech.util.Utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 类说明： wav流播放器
 *
 * @author Everett Li
 * @version 1.0
 * @date Nov 12, 2014
 */
public class AIAudioTrack implements IAIPlayer {

    public static final String TAG = "AIAudioTrack";

    private AudioTrack mAudioTrack;
    private int mMinBufferSize;
    private int mTotalDataSize;
    private int mPeriodInFrames;
    private boolean mIsDataFeedEnd = false;

    private SynthesizedBlockQueue mDataQueue;

    private int mSampleRate;
    private int mStreamType;
    private int mUsage;
    private int mContentType;

    /**
     * 后台处理数据队列的工作线程池
     */
    private ExecutorService mPool;

    private AIThreadFactory myThreadFactory;

    /**
     * 数据处理任务
     */
    private FeedTask mFeedTask;

    private AIPlayerListener mListener;

    @Override
    public void init(final Context context, final int streamType, final int sampleRate) {
        if (mAudioTrack == null) {
            mSampleRate = sampleRate;
            mStreamType = streamType;
            mMinBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);

            mAudioTrack = new AudioTrack(streamType, sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize, AudioTrack.MODE_STREAM);
            mPeriodInFrames = sampleRate / 1000 * AISpeechSDK.TTS_PROGRESS_INTERVAL_VALUE;
            mAudioTrack.setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListenerImpl());
            mAudioTrack.setPositionNotificationPeriod(mPeriodInFrames);

            Log.i(TAG, "AudioTrack Output stream is " + streamType + " , mMinBufferSize is:"
                    + mMinBufferSize + " , the sampleRate is : " + sampleRate
                    + ",  PeriodInFrame is :" + mPeriodInFrames);
        }

        myThreadFactory = new AIThreadFactory(TAG, Thread.NORM_PRIORITY);
        mPool = Executors.newFixedThreadPool(1, myThreadFactory);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void init(Context context, AudioAttributes audioAttributes, int sampleRate) {
        if (mAudioTrack == null) {
            mSampleRate = sampleRate;
            mMinBufferSize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mAudioTrack = new AudioTrack(
                        audioAttributes,
                        new AudioFormat.Builder()
                                .setSampleRate(mSampleRate)
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build(),
                        mMinBufferSize,
                        AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
            }
        }

        myThreadFactory = new AIThreadFactory(TAG, Thread.NORM_PRIORITY);
        mPool = Executors.newFixedThreadPool(1, myThreadFactory);

    }

    @Override
    public long play() {
        long sessionId = 0;
        if (mAudioTrack != null) {
            Log.d(TAG, "AIAudioTrack.play()");
            if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_STOPPED) {
                mIsDataFeedEnd = false;
                try {
                    mAudioTrack.setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListenerImpl());
                    mAudioTrack.setPositionNotificationPeriod(mPeriodInFrames);
                    mAudioTrack.play();
                } catch (Exception e) {
                    if (mListener != null)
                        mListener.onError(new AIError(AIError.ERR_TTS_AUDIO_TRACK,
                                AIError.ERR_DESCRIPTION_TTS_AUDIO_TRACK));
                }
                // 开始塞入数据
                sessionId = generateSessionId();
                mFeedTask = new FeedTask(mPool, sessionId);
                mFeedTask.startTask();
            } else {
                Log.w(TAG,
                        "AudioTrack not response play() because is in PlayState:"
                                + mAudioTrack.getPlayState());
            }
        }
        return sessionId;
    }

    @Override
    public void stop() {
        if (mAudioTrack != null) {
            if (mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_STOPPED) {
                Log.d(TAG, "AIAudioTrack.stop()");
//                mAudioTrack.stop();
                // 停止塞入数据
                if (mFeedTask != null) {
                    mFeedTask.stopTask();
                } else {
                    Log.d(TAG, "mFeedTask is null");
                }
            } else {
                Log.w(TAG,
                        "AudioTrack not response stop() because is in PlayState:"
                                + mAudioTrack.getPlayState());
            }
        }
    }

    @Override
    public void resume() {
        if (mAudioTrack != null) {
            if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
                Log.d(TAG, "AIAudioTrack.resume()");
                mAudioTrack.play();
                if (mFeedTask != null) {
                    mFeedTask.resumeTask();
                }
            } else {
                Log.w(TAG, "AudioTrack not response resume() because is in PlayState:"
                        + mAudioTrack.getPlayState());
            }
        }
    }

    @Override
    public void pause() {
        if (mAudioTrack != null) {
            if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                Log.d(TAG, "AIAudioTrack.pause()");
                mAudioTrack.pause();
            } else {
                Log.w(TAG,
                        "AudioTrack not response pause() because is in PlayState:"
                                + mAudioTrack.getPlayState());
            }
        }
    }

    @Override
    public void release() {
        if (mAudioTrack != null) {
            mAudioTrack.release();
            mAudioTrack = null;
        }
        if (mPool != null) {
            mPool.shutdown();
            mPool = null;
        }
        if (myThreadFactory != null) {
            myThreadFactory = null;
        }
    }

    @Override
    public void notifyDataIsReady(boolean isFinish) {
        mTotalDataSize = mDataQueue.getTotalDataSize();
        Log.df(TAG, "TotalDataSize:" + mTotalDataSize);
        mIsDataFeedEnd = isFinish;
    }

    @Override
    public void setDataQueue(SynthesizedBlockQueue queue) {
        Log.d(TAG, "queue  " + queue);
        mDataQueue = queue;
    }

    /**
     * 当检测到不同的StreamType时，将会重新载入AudioTrack
     */
    @Override
    public void setStreamType(int streamType) {
        Log.d(TAG, "streamType is: " + streamType);
        if (streamType != mStreamType) {
            mStreamType = streamType;
            reloadAudioTrack();
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void setAudioAttributes(AudioAttributes audioAttributes) {
        if (audioAttributes == null) return;
        Log.d(TAG, "setAudioAttributes usage: " + audioAttributes.getUsage() + " contentType :" + audioAttributes.getContentType());
        reloadAudioTrack(audioAttributes);

    }

    @Override
    public void setupVolume(float volume, float pan) {
        setupVolume(mAudioTrack, volume, pan);
    }

    /**
     * AudioTrack 请在初始化时指定SampleRate
     */
    @Override
    @Deprecated
    public void setSampleRate(int sampleRate) {

    }

    @Override
    public void setPlayerListener(AIPlayerListener listener) {
        mListener = listener;
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void reloadAudioTrack(AudioAttributes audioAttributes) {
        if (mAudioTrack != null) {
            mAudioTrack.release();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mAudioTrack = new AudioTrack(
                    audioAttributes,
                    new AudioFormat.Builder()
                            .setSampleRate(mSampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build(),
                    mMinBufferSize,
                    AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
        }

        Log.i(TAG, "Reloaded AudioTrack  audioAttributes output stream is " + mStreamType + " , mMinBufferSize is:"
                + mMinBufferSize + " , the sampleRate is : " + mSampleRate
                + ",  PeriodInFrame is :" + mPeriodInFrames);
    }

    private void reloadAudioTrack() {
        if (mAudioTrack != null) {
            mAudioTrack.release();
        }

        mAudioTrack = new AudioTrack(mStreamType, mSampleRate, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, mMinBufferSize, AudioTrack.MODE_STREAM);
        mAudioTrack.setPlaybackPositionUpdateListener(new OnPlaybackPositionUpdateListenerImpl());
        mAudioTrack.setPositionNotificationPeriod(mPeriodInFrames);

        Log.i(TAG, "Reloaded AudioTrack output stream is " + mStreamType + " , mMinBufferSize is:"
                + mMinBufferSize + " , the sampleRate is : " + mSampleRate
                + ",  PeriodInFrame is :" + mPeriodInFrames);
    }

    private void _onReady() {
        mListener.onReady();
    }

    private void _onStopped() {
        mListener.onStopped();
    }

    private void _onComplete(long sessionId) {
        // sync with timeline
        int totalFrame = mTotalDataSize / (mPeriodInFrames * mAudioTrack.getAudioFormat());
        long remainFrame = totalFrame - (mAudioTrack.getPlaybackHeadPosition() / mPeriodInFrames);
        Log.d(TAG, "sleep totalFrame: " + totalFrame + "  remainFrame:" + remainFrame
                + " headPosition:" + mAudioTrack.getPlaybackHeadPosition());
        mDataQueue.clear();
        mAudioTrack.stop();
        if (remainFrame > 0) {
            if (remainFrame > 10) {
                remainFrame = 10;
            }
            try {
                Log.d(TAG, "sleep " + remainFrame * AISpeechSDK.TTS_PROGRESS_INTERVAL_VALUE
                        + " ms for sync with onComplete");
                Thread.sleep(remainFrame * AISpeechSDK.TTS_PROGRESS_INTERVAL_VALUE);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
        mListener.onProgress(totalFrame * AISpeechSDK.TTS_PROGRESS_INTERVAL_VALUE, totalFrame
                * AISpeechSDK.TTS_PROGRESS_INTERVAL_VALUE, true);
        mListener.onCompletion(sessionId);
    }

    class OnPlaybackPositionUpdateListenerImpl implements OnPlaybackPositionUpdateListener {

        @Override
        public void onMarkerReached(AudioTrack track) {
            Log.d(TAG, "on marker reached");
        }

        @Override
        public void onPeriodicNotification(AudioTrack track) {
            if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                int currentFrame = track.getPlaybackHeadPosition() / mPeriodInFrames;
                int totalFrame = mTotalDataSize / (mPeriodInFrames * mAudioTrack.getAudioFormat());
//                Log.i(TAG, "on Tick: (currentFrame=" + currentFrame +
//                        " totalFrame=" + totalFrame
//                        + " isDataFeedEnd=" + mIsDataFeedEnd + ")");
                mListener.onProgress(currentFrame * AISpeechSDK.TTS_PROGRESS_INTERVAL_VALUE,
                        totalFrame * AISpeechSDK.TTS_PROGRESS_INTERVAL_VALUE, mIsDataFeedEnd);

                //2021/12/15 合成的途中网络终端的情况，在这里进行单独的处理
                if (!mIsDataFeedEnd && currentFrame == totalFrame && mFeedTask != null) {
                    if (mDataQueue.getTotalDataSize() != 0) {
                        stop();
                        _onComplete(mFeedTask.mSessionId);
                        Log.i(TAG, "Handling the situation of the combined en route network terminal.");
                    }
                }
            }
        }
    }

    /**
     * wav数据feed任务
     */
    class FeedTask implements Runnable {

        private boolean stopFlag = false;
        private final AtomicBoolean firstFlag = new AtomicBoolean(false);
        private Boolean isRunning = false;
        private final ExecutorService executors;

        private Object mLock = new Object();
        /**
         * 数据队列资源信号量
         */
        private final Semaphore mSemaphore = new Semaphore(0);
        private final long mSessionId;

        public FeedTask(ExecutorService executor, long sessionId) {
            this.executors = executor;
            this.mSessionId = sessionId;
        }

        @Override
        public void run() {
            Utils.checkThreadAffinity();
            Log.i(TAG, "Feed Task begin!");
            SynthesizedBlock block;
            try {
                while ((block = mDataQueue.pollBlock()) != null) {
                    String text = block.getText();
                    Log.d(TAG, "one peek =  " + text);
                    // 数据已取完毕
                    if (text == null) {
                        if (stopFlag) {
                            Log.d(TAG, "detect stop flag , break");
                            mAudioTrack.stop();
                            _onStopped();
                            break;
                        }
                        Log.d(TAG, "mDataQueue.getTotalDataSize() " + mDataQueue.getTotalDataSize());
                        if (mDataQueue.getTotalDataSize() != 0) {
                            _onComplete(mSessionId);
                        }
                        break;
                    } else { // 队列中有数据
                        boolean breakFlag = false;  // for breaking from inner loop
                        if (firstFlag.compareAndSet(false, true)) {
                            _onReady();
                        }
                        final byte[] data = (byte[]) block.getData();
                        int p = 0;
                        int q = 0;
                        do {
                            q += mMinBufferSize;
                            if (q > data.length) {
                                q = data.length;
                            }
//                            Log.d(TAG, "write size:" + (q - p));
                            if (mAudioTrack != null) {
                                if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
                                    mAudioTrack.write(data, p, q - p);
                                }
                                if (mAudioTrack.getPlayState() == AudioTrack.PLAYSTATE_PAUSED) {
                                    Log.d(TAG, "Feed task has been Paused due to AudioTrack paused.");
                                    synchronized (this) {
                                        this.wait();
                                    }
                                }
                            }
                            p = q;
                            if (stopFlag) {
                                byte[] temp = new byte[mMinBufferSize];
                                for (int i = 0; i < temp.length; i++) {
                                    temp[i] = 0;
                                }
                                mAudioTrack.write(temp, 0, mMinBufferSize); //解决在停止后，再开始会有爆音的问题
                                mAudioTrack.stop();
                                _onStopped();
                                breakFlag = true;
                                break;
                            }
                        } while (p < data.length);
                        if (breakFlag) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                isRunning = false;
                if (mAudioTrack != null && mAudioTrack.getState() != AudioTrack.PLAYSTATE_STOPPED) {
                    mAudioTrack.flush();
                    Log.d(TAG, "audioTrack flushed");
                }
                semaphoreV("feed task terminated.");
            }
            Log.d(TAG, "Feed Task terminated!");
        }

        /**
         * 停止任务，将会清空所有在队列中存在且未处理的数据
         */
        public void stopTask() {
            synchronized (mLock) {
                if (isRunning) {
                    stopFlag = true;
                    resumeTask();
                    mDataQueue.addBlock(new SynthesizedBytesBlock(null, null));
                    semaphoreP("stop start.");
                    mDataQueue.clear();
//                    resumeTask();
                } else {
                    Log.e(TAG, "task is not running");
                }
            }
        }

        /**
         * 启动任务，如果线程未启动，则启动线程，开始从队列中取出数据块进行处理
         */
        public void startTask() {
            synchronized (mLock) {
                executors.execute(this);
                isRunning = true;
            }
        }

        /**
         * 恢复任务，从上次write处继续
         */
        public void resumeTask() {
            synchronized (this) {
                this.notify();
            }
        }

        /**
         * P operation
         *
         * @param log
         */
        private void semaphoreP(final String log) {
            try {
                Log.i(TAG, "Semaphore acquire : " + log);
                mSemaphore.acquire();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

        /**
         * V operation
         *
         * @param log
         */
        private void semaphoreV(final String log) {
            mSemaphore.release();
            Log.i(TAG, "Semaphore release : " + log);
        }
    }


    private static void setupVolume(AudioTrack audioTrack, float volume, float pan) {
        final float vol = clip(volume, 0.0f, 1.0f);
        final float panning = clip(pan, -1.0f, 1.0f);

        float volLeft = vol;
        float volRight = vol;
        if (panning > 0.0f) {
            volLeft *= (1.0f - panning);
        } else if (panning < 0.0f) {
            volRight *= (1.0f + panning);
        }
        Log.d(TAG, "volLeft=" + volLeft + ",volRight=" + volRight);
        if (audioTrack.setStereoVolume(volLeft, volRight) != AudioTrack.SUCCESS) {
            Log.e(TAG, "Failed to set volume");
        }
    }

    private static float clip(float value, float min, float max) {
        return value > max ? max : (value < min ? min : value);
    }

    private long generateSessionId() {
        return Util.generateRandom(8);
    }

    public int getMinBufferSize() {
        return mMinBufferSize;
    }
}
