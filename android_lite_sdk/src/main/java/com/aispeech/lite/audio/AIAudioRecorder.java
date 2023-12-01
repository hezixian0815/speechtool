/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import com.aispeech.AIAudioRecord;
import com.aispeech.AIError;
import com.aispeech.DUILiteConfig;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.echo.EchoKernel;
import com.aispeech.echo.EchoKernelListener;
import com.aispeech.lite.AISampleRate;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AIThreadFactory;
import com.aispeech.util.Utils;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * AudioRecord for speech engine 需要权限：
 * {@link android.Manifest.permission#RECORD_AUDIO
 * android.permission.RECORD_AUDIO}
 */
public class AIAudioRecorder {

    private static final String TAG = "AIAudioRecorder";
    // 录音机重试次数
    private static final int AUDIORECORD_RETRY_TIMES = 4;
    private static final String LOG_RECORDER_NEW_FAILED = "recorder new failed";
    public static int INTERVAL = AISpeech.intervalTime; // read buffer interval in ms.
    private static int audio_source = AISpeech.audioSource;
    private static int audio_encoding = AudioFormat.ENCODING_PCM_16BIT;// bits/16
    //　录音机声道数,默认单通道
    private static int audio_channel_num = AudioFormat.CHANNEL_IN_MONO;
    /**
     * 前端信号处理是否获取参考音，需 HAL 层支持，default is {@value}。
     * <ul>
     * <li>false, 得到的底层音频数据不含参考音，用户<strong>不能</strong>在 java 层使用 AEC 功能</li>
     * <li>true, 得到的底层音频数据包含2路参考音，用户必须在 java 层使用 AEC 功能。前端信号处理使用统一参数：Source 6， 32k, mono，音频路数在原有基础上加2路</li>
     * </ul>
     */
    private static boolean fespOriginalAudio = false;
    private AISampleRate audioSampleRate;
    //echo
    private EchoKernel mEchoKernel;
    private MyEchoKernelListener myEchoKernelListener;
    /**
     * Android 标准录音机api
     */
    private volatile AudioRecord mCommonRecorder;
    /**
     * JNI封装TinyCap自定义录音机api
     */
    private volatile AIAudioRecord mTinyCapRecorder;
    /**
     * 录音机资源信号量
     */
    private Semaphore mSemaphore = new Semaphore(0);
    /**
     * 后台读任务线程池
     */
    private ExecutorService mPool;
    private AIThreadFactory mRecorderThreadFactory;
    /**
     * 录音机启动SessionId
     */
    private long mSessionId;
    /**
     * 标记是否正在录音
     */
    private volatile Boolean mIsRecording = false;
    private volatile boolean maxVolumeDetectOn = false;
    private Lock mLock = new ReentrantLock();
    private CopyOnWriteArrayList<AIRecordListener> mListenerList = new CopyOnWriteArrayList<>();//这些listener用于start后的一些回调
    private AIRecordListener mListener;//这个listener用于在new recorder或new aec时出现error时给外部回调信息
    /**
     * oneshot 保存音频变量
     */
    private Lock mOneShotLock = new ReentrantLock();
    private Queue<byte[]> mCacheQueue = new LinkedList<>();
    private int mQueueSize = 0;//缓存音频队列的大小
    private boolean mIsNeedCaching = false; //是否需要缓存唤醒点之前的音频（用于oneshot）
    private boolean mIsNeedSendCachingData = false;

    public AIAudioRecorder() {
        //nothing
    }

    public static long generateSessionId() {
        return Util.generateRandom(10);
    }

    public static boolean isFespOriginalAudio() {
        return fespOriginalAudio;
    }

    public static void setFespOriginalAudio(boolean fespOriginalAudio) {
        AIAudioRecorder.fespOriginalAudio = fespOriginalAudio;
        Log.d(TAG, "fespOriginalAudio " + fespOriginalAudio);
    }

    /**
     * 使用指定参数来初始化录音机
     *
     * @param sampleRate 采样率
     * @param listener
     */
    public synchronized void init(AISampleRate sampleRate, AIRecordListener listener) {
        AIAudioRecorder.INTERVAL = AISpeech.intervalTime;
        this.audioSampleRate = sampleRate;
        this.mListener = listener;
        Log.d(TAG, "audioSampleRate: " + sampleRate.getValue() + "\t" +
                "intervalTime: " + AIAudioRecorder.INTERVAL);
        if (audio_source == 0) {
            if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_MIC
                    || AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
                audio_source = MediaRecorder.AudioSource.MIC;
            } else {
                audio_source = MediaRecorder.AudioSource.VOICE_RECOGNITION;
            }
        }
        Log.d(TAG, "audioSourceType: " + audio_source);
        if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE4 ||
                AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE4 ||
                AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE6 ||
                AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE6 ||
                AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE8 ||
                AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_SHAPE_L4) {
            maxVolumeDetectOn = AISpeech.maxVolumeMode;//线性/环形四麦，环形六麦模式检测是否开启大音量模式
        }
        _newAudioRecorder();
    }


    /**
     * 对外返回当前录音机是否正在录音 注意：此方法不对等实际的录音机是否处于录音的状态
     *
     * @return true isRecording, false otherwise.
     */
    public synchronized boolean isRecording() {
        return mIsRecording;
    }

    public synchronized boolean isRegistered(AIRecordListener listener) {
        return listener != null && mListenerList.contains(listener);
    }

    public synchronized boolean hasListener() {
        return mListenerList.size() != 0;
    }

    private synchronized void registerListener(AIRecordListener listener) {
        Log.d(TAG, "registerListener " + listener.toString());
        if (listener != null && !mListenerList.contains(listener)) {
            Log.d(TAG, "add listener " + listener.toString());
            mListenerList.add(listener);
        }
    }

    private synchronized void unRegisterListener(AIRecordListener listener) {
        if (listener != null && mListenerList.contains(listener)) {
            Log.d(TAG, "remove listener " + listener.toString());
            mListenerList.remove(listener);
        }
    }

    private synchronized void onException(AIError e) {
        for (AIRecordListener listener : mListenerList) {
            listener.onException(e);
        }
    }

    private synchronized void onRecordStarted(long sessionId) {
        for (AIRecordListener listener : mListenerList) {
            listener.onRecordStarted(sessionId);
        }
    }

    /**
     * 从录音机获取的原始pcm数据
     *
     * @param sessionId
     * @param buffer
     * @param size
     */
    private synchronized void onRawBufferReceived(long sessionId, final byte[] buffer, final int size) {
        for (AIRecordListener listener : mListenerList) {
            listener.onRawDataReceived(sessionId, buffer, size);
        }
    }

    /**
     * 经过信号处理后的单路数据，或mic模式下的原始单路pcm数据
     *
     * @param sessionId
     * @param buffer
     * @param size
     */
    private synchronized void onResultBufferReceived(long sessionId, final byte[] buffer, final int size, boolean isCachedData) {
        for (AIRecordListener listener : mListenerList) {
            if (isCachedData) {
                if (!listener.getTag().equals("WakeupProcessor") &&
                        !listener.getTag().equals("VprintProcessor")) {//如果是oneshot的cacheddata，不送给唤醒
                    listener.onResultDataReceived(buffer, size);
                }
            } else {
                listener.onResultDataReceived(buffer, size);
            }
        }
    }

    private synchronized void onRecordStopped(long sessionId) {
        for (AIRecordListener listener : mListenerList) {
            listener.onRecordStopped(sessionId);
        }
    }

    private synchronized void onRecordReleased() {
        for (AIRecordListener listener : mListenerList) {
            listener.onRecordReleased();
        }
    }

    private void clearListener() {
        Log.d(TAG, "clearListener");
        mListenerList.clear();
    }

    /**
     * 启动录音机
     *
     * @param listener 回调接口
     * @return sessionId -1 发生错误, 其它返回10位长度的long值
     */
    public long start(AIRecordListener listener) {
        Log.i(TAG, "start");
        if (mCommonRecorder == null && mTinyCapRecorder == null) {
            Log.e(TAG, LOG_RECORDER_NEW_FAILED);
            return -1;
        }
        mLock.lock();
        try {
            registerListener(listener);
            if (mIsRecording) {
                Log.w(TAG, "AudioRecorder has been started!");
                if (listener != null) {
                    listener.onRecordStarted(mSessionId);
                }
                return mSessionId;
            }
            mSessionId = generateSessionId();
            mIsRecording = _startAudioRecorder();
            mPool.execute(new ReadRunnable(mSessionId));
            if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
                mEchoKernel.startKernel();
            }
            return mSessionId;
        } finally {
            mLock.unlock();
        }
    }

    /**
     * 停录音机
     * listener 为null时，才停止，否则不停止，只unRegister
     */
    public void stop(AIRecordListener listener) {
        Log.i(TAG, "stop");
        if (mCommonRecorder == null && mTinyCapRecorder == null) {
            Log.e(TAG, LOG_RECORDER_NEW_FAILED);
            return;
        }
        mLock.lock();
        if (mIsRecording) {
            if (listener != null) {
                if (mListenerList.contains(listener)) {
                    unRegisterListener(listener);
                } else {
                    Log.d(TAG, "the listener has been unRegistered");
                }
            } else {
                Log.d(TAG, "stop recorder");
                mIsRecording = false;
                semaphoreP("stop start.");
                _stopAudioRecorder();
            }
        } else {
            Log.w(TAG, "AudioRecorder has been stopped!");
        }
        mLock.unlock();
    }

    /**
     * 释放录音机
     */
    public synchronized void release() {
        Log.i(TAG, "release");
        if (mCommonRecorder == null && mTinyCapRecorder == null) {
            Log.e(TAG, LOG_RECORDER_NEW_FAILED);
            return;
        }
        clearListener();
        stop(null);
        _releaseAudioRecorder();
        if (mPool != null) {
            mPool.shutdown();
            mPool = null;
        }
        if (mRecorderThreadFactory != null) {
            mRecorderThreadFactory = null;
        }

    }

    public synchronized boolean isReleased() {
        return mPool == null;
    }

    /**
     * 创建录音机及读线程池
     */
    private void _newAudioRecorder() {
        int retryCounter = 0;
        while (true) {
            // 带重试的创建录音机
            try {
                if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_MIC) {
                    createCommonRecorder(audio_source, 16000,
                            AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 192000);//NEW mCommonReorder_dual
                    audio_channel_num = 1;
                } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
                    createCommonRecorder(audio_source, 16000,
                            AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, 192000);//NEW mCommonReorder_dual
                    audio_channel_num = 2;
                } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_FESPCAR) {
                    createCommonRecorder(audio_source, 16000,
                            AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, 192000);//NEW mCommonReorder_dual
                    audio_channel_num = 2;
                } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_FESPCAR4) {
                    createCommonRecorder(audio_source, 16000,
                            AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, 192000);//NEW mCommonReorder_dual
                    audio_channel_num = 4;
                } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_DUAL) {
                    if (!fespOriginalAudio) {
                        createCommonRecorder(audio_source, 16000,
                                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, 192000);
                        audio_channel_num = 2;
                    } else {
                        createCommonRecorder(6, 32000,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 192000);//NEW mCommonReorder_dual
                        audio_channel_num = 2 + 2;
                    }
                } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE4 ||
                        AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE4 ||
                        AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_SHAPE_L4) {
                    if (!fespOriginalAudio) {
                        createCommonRecorder(audio_source, 32000,
                                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, 192000);//NEW mCommonReorder_line
                        audio_channel_num = 4;
                    } else {
                        createCommonRecorder(6, 32000,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 192000);//NEW mCommonReorder_line
                        audio_channel_num = 4 + 2;
                    }
                } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE6
                        || AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE6) {
                    if (!fespOriginalAudio) {
                        createCommonRecorder(audio_source, 48000,
                                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, 192000);//NEW mCommonReorder_line
                        audio_channel_num = 6;
                    } else {
                        createCommonRecorder(6, 32000,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 192000);//NEW mCommonReorder_line
                        audio_channel_num = 6 + 2;
                    }
                }  else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE8) {
                    if (!fespOriginalAudio) {
                        createCommonRecorder(audio_source, 64000,
                                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, 192000);//NEW mCommonReorder_line
                        audio_channel_num = 8;
                    } else {
                        createCommonRecorder(6, 32000,
                                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, 192000);//NEW mCommonReorder_line
                        audio_channel_num = 8 + 2;
                    }
                } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_DUAL) {
                    mTinyCapRecorder = new AIAudioRecord();
                    mTinyCapRecorder._native_setup(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, 4);
                    audio_channel_num = 4;
                } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_LINE4 ||
                        AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE4) {
                    mTinyCapRecorder = new AIAudioRecord();
                    mTinyCapRecorder._native_setup(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, 6);
                    audio_channel_num = 6;
                } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_LINE6 ||
                        AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE6) {
                    mTinyCapRecorder = new AIAudioRecord();
                    mTinyCapRecorder._native_setup(MediaRecorder.AudioSource.VOICE_RECOGNITION, 16000, 8);
                    audio_channel_num = 8;
                }
                Log.d(TAG, "audio_channel_num is : " + audio_channel_num);
                if (mCommonRecorder == null && mTinyCapRecorder == null) {
                    throw new AIError(AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE);
                } else {
                    Log.d(TAG, "recorder.new() retry count: " + (retryCounter - 0));
                    break;
                }
            } catch (AIError e) {
                e.printStackTrace();

                if (retryCounter < AUDIORECORD_RETRY_TIMES) {
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e1) {
                        Log.w(TAG, "_newAudioRecorder: " + e1.toString());
                        Thread.currentThread().interrupt();
                    }
                    retryCounter++;
                    continue;
                }
                onException(e);
                return;
            }
        }
        if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
            if (myEchoKernelListener == null) {
                myEchoKernelListener = new MyEchoKernelListener();
            }
            if (mEchoKernel == null) {
                mEchoKernel = new EchoKernel(myEchoKernelListener);
            }
            mEchoKernel.newKernel();
        }
        mRecorderThreadFactory = new AIThreadFactory("AIRecorder", Thread.MAX_PRIORITY);
        mPool = Executors.newCachedThreadPool(mRecorderThreadFactory);
    }

    private void createCommonRecorder(int audioSource, int sampleRateInHz, int audioFormat, int encoding, int bufferSizeInBytes) {
        //判断一下是不是项目外部传入audioRecord的方式
        if (DUILiteConfig.getExternalAudioRecordProvider() != null) {
            // 相关参数还是内部生成后传给外部使用
            mCommonRecorder = DUILiteConfig.getExternalAudioRecordProvider().provideAudioRecord(audioSource, sampleRateInHz,
                    audioFormat, encoding, bufferSizeInBytes);
        } else {
            mCommonRecorder = new AudioRecord(audioSource, sampleRateInHz, audioFormat, encoding, bufferSizeInBytes);
        }
    }

    /**
     * 带重试地启动录音机
     *
     * @return true 启动成功, false 启动失败
     */
    private boolean _startAudioRecorder() {
        int retryCounter = 0;
        while (true) {
            try {
                Log.d(TAG, "recorder.startRecording()");
                if (mCommonRecorder != null) {//common audioRecorder
                    mCommonRecorder.startRecording();
                    if (mCommonRecorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                        throw new AIError(AIError.ERR_RECORDING, AIError.ERR_DESCRIPTION_RECORDING);
                    } else {
                        onRecordStarted(mSessionId);
                        Log.d(TAG, "recorder.start() retry count: " + (retryCounter - 0));
                        break;
                    }
                } else if (mTinyCapRecorder != null) {//tinycap audioRecorder
                    mTinyCapRecorder._native_start();
                    onRecordStarted(mSessionId);
                    break;
                }
            } catch (AIError e) {
                e.printStackTrace();
                if (retryCounter < AUDIORECORD_RETRY_TIMES) {
                    try {
                        Thread.sleep(40);
                    } catch (InterruptedException e1) {
                        Log.w(TAG, "_startAudioRecorder: " + e1.toString());
                        Thread.currentThread().interrupt();
                    }
                    retryCounter++;
                    continue;
                }
                onException(e);
                return false;
            }
        }
        return true;
    }

    /**
     * 停止录音机
     */
    private void _stopAudioRecorder() {
        Log.d(TAG, "AudioRecord.stop() before");
        if (mCommonRecorder != null) {
            mCommonRecorder.stop();
        } else if (mTinyCapRecorder != null) {
            mTinyCapRecorder._native_stop();
        }
        if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
            if (mEchoKernel != null) {
                mEchoKernel.stopKernel();
            }
        }
        Log.d(TAG, "AudioRecord.stop() end");
    }

    /**
     * 释放录音机
     */
    private void _releaseAudioRecorder() {
        Log.d(TAG, "AudioRecord.release() before");
        if (mCommonRecorder != null) {
            mCommonRecorder.release();
            mCommonRecorder = null;
        } else if (mTinyCapRecorder != null) {
            mTinyCapRecorder = null;
        }
        if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
            if (mEchoKernel != null) {
                mEchoKernel.releaseKernel();
                mEchoKernel = null;
            }
        }
        Log.d(TAG, "AudioRecord.release() after");
        Log.d(TAG, "Release AIAudioRecord, AudioRecord = null");
    }

    /**
     * 开始缓存音频
     */
    public void startCachingData() {
        Log.d(TAG, "startCachingData");
        mIsNeedCaching = true;
    }

    public void stopCachingData() {
        Log.d(TAG, "stopCachingData");
        mIsNeedCaching = false;
    }

    /**
     * 优先把缓存队列的音频送给识别等引擎
     */
    public void startSendCachingData() {
        Log.d(TAG, "startSendCachingData");
        mOneShotLock.lock();
        mIsNeedCaching = false;
        mIsNeedSendCachingData = true;
        mOneShotLock.unlock();
    }

    /**
     * 从录音机中读取数据
     */
    private void readDataFromSoloAudioRecorderInLoop(long sessionId) {
        int useReadBufferSize = AudioRecordUtils.getReadBufferSize(audioSampleRate, INTERVAL);
        byte[] readBuffer = new byte[useReadBufferSize];
        int readSize = 0;
        try {
            Log.d(TAG, "AIAudioRecord.read()...");
            while (true) {
                if (!mIsRecording) {//若没在录音，则退出while循环
                    break;
                }
                if (mCommonRecorder != null) {
                    readSize = mCommonRecorder.read(readBuffer, 0, useReadBufferSize);//从录音机录的实时数据
                } else if (mTinyCapRecorder != null) {
                    readSize = mTinyCapRecorder._native_read_in_byte_array(readBuffer, 0, useReadBufferSize);
                }
                if (readSize > 0) {
                    byte[] bytes = new byte[readSize];
                    System.arraycopy(readBuffer, 0, bytes, 0, readSize);
                    if (AISpeech.getRecoderType() != DUILiteConfig.TYPE_COMMON_MIC) {//非单麦模式
                        if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
                            mEchoKernel.feed(bytes);
                        }
                        onRawBufferReceived(sessionId, bytes, readSize);
                    } else {//单麦模式，含oneshot场景
                        mOneShotLock.lock();
                        if (mIsNeedCaching) {
                            mQueueSize = AISpeech.getOneShotCacheTime() / INTERVAL;
                            if (mCacheQueue.size() > mQueueSize && mCacheQueue.size() > 0) {//默认只缓存唤醒点之前1.2s的音频
                                mCacheQueue.remove();
                            }
                            mCacheQueue.offer(bytes);
                        }
                        if (mIsNeedSendCachingData) {
                            //把queue里的数据(你好小乐)送往asr，asr里有功能来检测vad
                            mIsNeedSendCachingData = false;
                            mIsNeedCaching = true;
                            while (mCacheQueue.peek() != null) {
                                byte[] data = mCacheQueue.poll();
                                onResultBufferReceived(sessionId, data, data.length, true);
                            }
                            mCacheQueue.clear();
                        }
                        onResultBufferReceived(sessionId, bytes, readSize, false);
                        mOneShotLock.unlock();
                    }
                    /**
                     * 线性/环形四麦的大音量检测状态
                     */
                    if (maxVolumeDetectOn) {
                        onPowerChanged(isHighPower());
                    }
                } else {
                    Log.e(TAG, "recorder error read size : " + readSize);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            semaphoreV("stop end.");
        }
    }

    /**
     * 获取当前录音数据的读缓冲区大小
     *
     * @return 当前录音数据读缓冲区大小
     */
    public int getReadBufferSize() {
        return AudioRecordUtils.getReadBufferSize(audioSampleRate, INTERVAL);
    }

    /**
     * 获取当前音频采样率
     *
     * @return 音频采样率
     * @see AISampleRate
     */
    public AISampleRate getSampleRate() {
        return audioSampleRate;
    }

    /**
     * 获取录音机Channel
     *
     * @return channel值
     */
    public int getAudioChannel() {
        return audio_channel_num;
    }

    /**
     * 返回音频编码格式
     *
     * @return audioEncoding, 始终为2
     */
    public int getAudioEncoding() {
        return audio_encoding;
    }

    /**
     * 计算指定时长的音频大小
     *
     * @param sec 音频时长，单位秒
     * @return 音频字节大小
     */
    private long calcAudioSize(int sec) {
        return audio_channel_num * audioSampleRate.getValue() * audio_encoding * sec;
    }

    /**
     * P operation
     *
     * @param log
     */
    private void semaphoreP(final String log) {
        try {
            Log.i(TAG, "Semaphore acquire before: " + log);
            mSemaphore.tryAcquire(600, TimeUnit.MILLISECONDS);
            Log.i(TAG, "Semaphore acquire end: " + log);
        } catch (InterruptedException e) {
            Log.w(TAG, "semaphoreP: " + e.toString());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * V operation
     *
     * @param log
     */
    private void semaphoreV(final String log) {
        Log.i(TAG, "Semaphore release before: " + log);
        mSemaphore.release();
        Log.i(TAG, "Semaphore release end: " + log);
    }

    private void onEchoBufferReceived(final byte[] resultBuffer, final int size) {
        for (AIRecordListener listener : mListenerList) {
            listener.onResultDataReceived(resultBuffer, size);
        }
    }

    private boolean isHighPower() {
        boolean isHigh = false;
        String keyMaxVolumeState = "max_volume_state";
        AudioManager audioManager = (AudioManager) AISpeech.getContext().getSystemService(Context.AUDIO_SERVICE);
        String valueMaxVolumeState = audioManager.getParameters(keyMaxVolumeState);
        if (valueMaxVolumeState.startsWith(keyMaxVolumeState)) {
            valueMaxVolumeState = valueMaxVolumeState.substring(keyMaxVolumeState.length() + 1);
        }
        //Log.d(TAG, "valueMaxVolumeState is " + valueMaxVolumeState);
        if ("max_volume_state=1".equals(valueMaxVolumeState) || "1".equals(valueMaxVolumeState)) {
            isHigh = true;
        } else {
            isHigh = false;
        }
//        Log.d(TAG, "is high power " + isHigh);
        return isHigh;
    }

    private synchronized void onPowerChanged(boolean isHighPower) {
        for (AIRecordListener listener : mListenerList) {
            listener.onPowerChanged(isHighPower);
        }
    }

    /**
     * Util to get record device.
     */
    private static class AudioRecordUtils {

        private static int calc_buffer_size(AISampleRate sampleRate) {
            int _sample_rate = sampleRate.getValue();
//            int bufferSize = _sample_rate * audio_channel_num * audio_encoding;
            int minBufferSize = AudioRecord.getMinBufferSize(_sample_rate, audio_channel_num,
                    audio_encoding);
            int bufferSize = 2 * minBufferSize;

            Log.d(TAG, "[MinBufferSize = " + minBufferSize + ", BufferSize = " + bufferSize + "]");
            return bufferSize;
        }

        public static int getReadBufferSize(AISampleRate sampleRate, int intervalTime) {
            int _sample_rate = sampleRate.getValue();
            Integer read_buffer_size = _sample_rate * audio_channel_num * audio_encoding
                    * intervalTime / 1000;
            Log.d(TAG, "[SampleRate = " + _sample_rate + ", ReadBufferSize = " + read_buffer_size
                    + "]");
            // }
            return read_buffer_size;
        }

        /**
         * 根据默认配置初始化一个AudioRecord对象
         *
         * @return AudioRecord 成功初始化录音设备 null 如果无法获取录音设备
         */
        /*public static AudioRecord newInstance(AISampleRate sampleRate)
                throws IllegalArgumentException {
            Integer buffer_size = calc_buffer_size(sampleRate);
            Log.d(TAG, "recorder.new() ");
            *//*if(AUDIO_CHANNEL == AudioFormat.CHANNEL_IN_STEREO) {
            	Log.d(TAG, "AUDIO_CHANNEL : stero  channel_num: "  + audio_channel_num);
            } else {
            	Log.d(TAG, "AUDIO_CHANNEL : mono channel_num: " + audio_channel_num );
            }*//*
            AudioRecord audioRecord = new AudioRecord(audio_source, sampleRate.getValue(),
                    AUDIO_CHANNEL, audio_encoding, buffer_size);
            Log.d(TAG, "recorder.new() end");
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                return audioRecord;
            }
            return null;
        }*/
    }

    /**
     * read buffer task
     */
    class ReadRunnable implements Runnable {

        private long sessionId = 0;

        public ReadRunnable(long sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public void run() {
            Utils.checkThreadAffinity();
            Log.d(TAG, "Read Buffer Task run...");
            readDataFromSoloAudioRecorderInLoop(sessionId);
            Log.d(TAG, "Read Buffer Task end...");
        }
    }

    private class MyEchoKernelListener extends EchoKernelListener {

        @Override
        public void onInit(int status) {
            if (status == AIConstant.OPT_FAILED) {
                if (mListener != null)
                    mListener.onException(new AIError("new echo kernel failed"));
            }
        }

        @Override
        public void onResultBufferReceived(byte[] data) {
            onEchoBufferReceived(data, data.length);
        }

        @Override
        public void onAgcDataReceived(byte[] data) {

        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onException(error);
            }
        }
    }
}
