package com.aispeech.lite.tts;

import android.annotation.TargetApi;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Build;

import com.aispeech.AIError;
import com.aispeech.common.AITimer;
import com.aispeech.common.CloseUtils;
import com.aispeech.common.Log;
import com.aispeech.common.ThreadNameUtil;
import com.aispeech.common.Util;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.audio.AIPlayerListener;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 类说明： MediaPlayer播放器，可以播放完整的MP3和wav文件
 *
 * @author Everett Li
 * @version 1.0
 * @date Nov 12, 2014
 */
public class AIMediaPlayer implements IAIPlayer {

    public static final String TAG = "AIMediaPlayer";

    private Context mContext;
    private MediaPlayer mMediaPlayer;
    private boolean mIsDataFeedEnd = false;

    private SynthesizedBlockQueue mDataQueue;

    private FeedTask mFeedTask;

    private AIPlayerListener mListener;

    private boolean mIsInitialized = false;

    private long mSessionId;

    @Override
    public void init(final Context context, final int streamType, final int sampleRate) {
        mContext = context;
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(new OnCompletionListenerImpl());
            mMediaPlayer.setOnErrorListener(new OnErrorListenerImpl());
            mMediaPlayer.setOnPreparedListener(new OnPreparedListenerImpl());
            mMediaPlayer.setAudioStreamType(streamType);
        }
        if (mFeedTask == null) {
            mFeedTask = new FeedTask();
        }

    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void init(Context context, AudioAttributes audioAttributes, int sampleRate) {
        mContext = context;
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnCompletionListener(new OnCompletionListenerImpl());
            mMediaPlayer.setOnErrorListener(new OnErrorListenerImpl());
            mMediaPlayer.setOnPreparedListener(new OnPreparedListenerImpl());
            mMediaPlayer.setAudioAttributes(audioAttributes);
        }
        if (mFeedTask == null) {
            mFeedTask = new FeedTask();
        }
    }

    @Override
    public long play() {
        if (mMediaPlayer != null) {
            if (!mMediaPlayer.isPlaying()) {
                Log.d(TAG, "AIMediaPlayer.play()");
                mIsDataFeedEnd = false;
                // 开始塞入数据
                if (mFeedTask != null) {
                    mFeedTask.startTask();
                }
                startProgressTimer();
                mSessionId = generateSessionId();
            } else {
                Log.w(TAG, "MediaPlayer not response play() because is in playing!");
            }
        }
        return mSessionId;
    }

    @Override
    public void stop() {
        if (mMediaPlayer != null && isInitialized()) {
            stopProgressTimer();
            Log.d(TAG, "AIMediaPlayer.stop()");
//            try {
//                if (mMediaPlayer.isPlaying() && isInitialized()) {
            mMediaPlayer.stop();
            mIsInitialized = false;
//                }
//            } catch (IllegalStateException e) {
//            	mIsInitialized = false;
//                e.printStackTrace();
//            }
            if (mFeedTask != null) {
                mFeedTask.stopTask();
            }
        } else {
            Log.d(TAG, "media player not initialized , so not response to stop");
        }
    }

    @Override
    public void resume() {
        if (mMediaPlayer != null && isInitialized()) {
            Log.d(TAG, "AIMediaPlayer.resume()");
            if (!mMediaPlayer.isPlaying()) {
                Log.d(TAG, "Duration:" + mMediaPlayer.getDuration());
                if (mMediaPlayer.getDuration() > 0) {
                    mMediaPlayer.start();
                    startProgressTimer();
                }
            } else {
//                if (mFeedTask != null) {
//                    mFeedTask.resumeTask();
//                }
            }
        } else {
            Log.d(TAG, "media player not initialized , so not response to resume");
        }
    }

    @Override
    public void pause() {
        if (mMediaPlayer != null && isInitialized()) {
            Log.d(TAG, "AIMediaPlayer.pause()");
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            } else {
//                if (mFeedTask != null) {
//                    mFeedTask.pauseTask();
//                }
            }
            stopProgressTimer();
        } else {
            Log.d(TAG, "media player not initialized , so not response to pause");
        }
    }

    @Override
    public void release() {
        stopProgressTimer();
        Log.d(TAG, "AIMediaPlayer.release()");
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }
        if (mFeedTask != null) {
            mFeedTask.destroy();
        }
        deleteCacheFile();
        if (mContext != null) {
            mContext = null;
        }
    }

    private boolean isInitialized() {
        return mIsInitialized;
    }

    @Override
    public void setDataQueue(SynthesizedBlockQueue queue) {
        mDataQueue = queue;
    }

    @Override
    public void setPlayerListener(AIPlayerListener listener) {
        mListener = listener;
    }

    @Override
    public void notifyDataIsReady(boolean isFinish) {
        Log.d(TAG, "TotalDataSize:" + mDataQueue.getTotalDataSize());
        mIsDataFeedEnd = isFinish;
    }

    @Override
    public void setStreamType(int streamType) {
        if (mMediaPlayer != null) {
            Log.d(TAG, "streamType is: " + streamType);
            mMediaPlayer.setAudioStreamType(streamType);
        }
    }

    @Override
    @TargetApi(Build.VERSION_CODES.M)
    public void setAudioAttributes(AudioAttributes audioAttributes) {
        Log.d(TAG, "setAudioAttributes : " + audioAttributes);
        if (audioAttributes == null) return;
        if (mMediaPlayer != null) {
            Log.d(TAG, "usage : " + audioAttributes.getUsage() + "contentType" + audioAttributes.getContentType());
            mMediaPlayer.setAudioAttributes(audioAttributes);
        }
    }

    @Override
    public void setupVolume(float volume, float pan) {
        if (mMediaPlayer != null) {
            setupVolume(mMediaPlayer, volume, pan);
        }
    }

    @Override
    @Deprecated
    public void setSampleRate(int sampleRate) {
    }

    private void playFile(File file) {
        try {
            mMediaPlayer.reset();
        } catch (IllegalStateException e) {
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            mMediaPlayer.setDataSource(fis.getFD());
            mMediaPlayer.prepareAsync();
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            mIsInitialized = false;
            mListener.onError(new AIError(AIError.ERR_TTS_MEDIAPLAYER,
                    AIError.ERR_DESCRIPTION_TTS_MEDIAPLAYER));
            e.printStackTrace();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private File storageBytesToCacheFile(byte[] data) {
        File cacheFile = new File(mContext.getCacheDir(), "AISpeech_tts_" + this.hashCode()
                + ".cache");
        BufferedOutputStream bos = null;
        try {
            bos = new BufferedOutputStream(new FileOutputStream(cacheFile));
            bos.write(data);
            bos.flush();
            bos.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            CloseUtils.closeIO(bos);
        }
        return cacheFile;
    }

    private void deleteCacheFile() {
        File cacheFile = new File(mContext.getCacheDir(), "AISpeech_tts_" + this.hashCode()
                + ".cache");
        if (cacheFile != null && cacheFile.exists()) {
            cacheFile.delete();
        }
    }

    private static void setupVolume(MediaPlayer mediaPlayer, float volume, float pan) {
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
        mediaPlayer.setVolume(volLeft, volRight);
    }

    private static float clip(float value, float min, float max) {
        return value > max ? max : (value < min ? min : value);
    }

    class OnCompletionListenerImpl implements OnCompletionListener {

        @Override
        public void onCompletion(MediaPlayer mp) {
            int totalTime = mp.getDuration();
            mListener.onProgress(totalTime, totalTime, true);
            mFeedTask.resumeTask();
        }
    }

    class OnErrorListenerImpl implements OnErrorListener {

        @Override
        public boolean onError(MediaPlayer mp, int what, int extra) {
            mIsInitialized = false;
            mListener.onError(new AIError(AIError.ERR_TTS_MEDIAPLAYER,
                    AIError.ERR_DESCRIPTION_TTS_MEDIAPLAYER + "what(" + what + ")" + "  extra("
                            + extra + ")"));
            return false;
        }
    }

    class OnPreparedListenerImpl implements OnPreparedListener {

        @Override
        public void onPrepared(MediaPlayer mp) {
            mIsInitialized = true;
            mp.start();
        }

    }

    /**
     * 音频文件准备任务
     */
    class FeedTask extends Thread {

        private boolean stopFlag = false;
        private boolean pauseFlag = false;
        private boolean terminatedFlag = false;
        private final AtomicBoolean firstFlag = new AtomicBoolean(false);

        public FeedTask() {
            super(ThreadNameUtil.getSimpleThreadName("player-feed"));
        }

        @Override
        public void run() {
            Log.d(TAG, "Feed Task begin!");
            SynthesizedBlock block;
            while ((block = mDataQueue.pollBlock()) != null) {
                String text = block.getText();
                if (text == null) {
                    // 数据已取完毕
                    try {
                        AIMediaPlayer.this.stop();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    mListener.onCompletion(mSessionId);
                    stopTask();
                } else {
                    if (firstFlag.compareAndSet(false, true)) {
                        mListener.onReady();
                    }
                    File cacheFile = null;
                    if (block instanceof SynthesizedFileBlock) {
                        cacheFile = (File) block.getData();
                    } else if (block instanceof SynthesizedBytesBlock) {
                        byte[] data = (byte[]) block.getData();
                        // storage and PLAY
                        cacheFile = storageBytesToCacheFile(data);
                    }
                    playFile(cacheFile);
                    pauseTask();
                    if (pauseFlag) {
                        synchronized (this) {
                            try {
                                Log.d(TAG, "Feed Task stopped for waiting play completion!");
                                this.wait();
                                Log.d(TAG, "Feed Task restared!");
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
                if (terminatedFlag) {
                    Log.d(TAG, "Feed Task terminated!");
                    return;
                }
                if (stopFlag) {
                    firstFlag.set(false);
                    synchronized (this) {
                        try {
                            Log.d(TAG, "Feed Task stopped!");
                            this.wait();
                            Log.d(TAG, "Feed Task stared!");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        /**
         * 暂停任务，会暂停对当前数据块的处理
         */
        public void pauseTask() {
            pauseFlag = true;
        }

        /**
         * 恢复任务，会将之前暂停时块内未处理的数据写入到AudioTrack
         */
        public void resumeTask() {
            pauseFlag = false;
            synchronized (this) {
                this.notifyAll();
            }
        }

        /**
         * 停止任务，将会清空所有在队列中存在且未处理的数据
         */
        public void stopTask() {
            stopFlag = true;
            mDataQueue.clear();
            if (pauseFlag) {
                resumeTask();
            }
        }

        /**
         * 启动任务，如果线程未启动，则启动线程，开始从队列中取出数据块进行处理
         */
        public void startTask() {
            stopFlag = false;
            Log.i(TAG, " isAlive() : " + isAlive());
            if (!isAlive()) {
                try {
                    super.start();
                } catch (IllegalThreadStateException e) {
                    Log.e(TAG, " startTask : IllegalThreadStateException");
                }
            }
            synchronized (this) {
                this.notifyAll();
            }
        }

        /**
         * 销毁任务，销毁线程
         */
        public void destroy() {
            terminatedFlag = true;
            stopTask();
            mDataQueue.addBlock(new SynthesizedFileBlock(null, null));
            startTask();
        }

    }

    /**
     * 音频播放进度定时任务
     */
    private ProgressTimerTask mProgressTimerTask = null;

    private void startProgressTimer() {
        stopProgressTimer();
        mProgressTimerTask = new ProgressTimerTask();
        try {
            AITimer.getInstance().schedule(mProgressTimerTask, 0,
                    AISpeechSDK.TTS_PROGRESS_INTERVAL_VALUE);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void stopProgressTimer() {
        if (mProgressTimerTask != null) {
            mProgressTimerTask.cancel();
            mProgressTimerTask = null;
        }
    }

    private class ProgressTimerTask extends TimerTask {

        @Override
        public void run() {
            // Log.d(TAG, "on Tick!");
            try {
                if (mMediaPlayer.isPlaying()) {
                    int currentTime = mMediaPlayer.getCurrentPosition();
                    int totalTime = mMediaPlayer.getDuration();
                    mListener.onProgress(currentTime, totalTime, true);
                }
            } catch (IllegalStateException e) {
                e.printStackTrace();
                mIsInitialized = false;
            }
        }
    }

    private long generateSessionId() {
        return Util.generateRandom(8);
    }
}
