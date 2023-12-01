/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.audio;

import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.lite.AISampleRate;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class AIAudioRecorderProxy implements IAudioRecorder {

    private static final String TAG = "AIAudioRecorderProxy";
    private static volatile AIAudioRecorderProxy instance = null;

    /**
     * 唯一的录音机实例
     */
    private AIAudioRecorder sAIAudioRecorder;

    private Lock sLock = new ReentrantLock();

    public synchronized static AIAudioRecorderProxy getInstance(AISampleRate sampleRate, AIRecordListener listener) {
        if (instance == null)
            instance = new AIAudioRecorderProxy();
        instance.init(sampleRate, listener);
        return instance;
    }

    private AIAudioRecorderProxy() {
        sAIAudioRecorder = new AIAudioRecorder();
    }

    /**
     * @param sampleRate 采样率，应该都是16K
     * @param listener   EchoKernel的回调里使用
     */
    private void init(AISampleRate sampleRate, AIRecordListener listener) {
        if (sAIAudioRecorder.isReleased()) {
            sAIAudioRecorder.init(sampleRate, listener);
        }
    }

    @Override
    public long startRecorder(AIRecordListener listener) {
        long sessionId;
        sessionId = sAIAudioRecorder.start(listener);
        return sessionId;
    }

    @Override
    public void unRegisterRecorder(AIRecordListener listener) {
        sLock.lock();
        sAIAudioRecorder.stop(listener);

        if (!sAIAudioRecorder.hasListener()) {
            Log.i(TAG, "unRegisterRecorder() releaseRecorder");
            sAIAudioRecorder.release();
        }
        sLock.unlock();
    }

    @Override
    public void releaseRecorder() {
        // 无用
    }

    @Override
    public AISampleRate getSampleRate() {
        return sAIAudioRecorder.getSampleRate();
    }

    @Override
    public int getAudioChannel() {
        return sAIAudioRecorder.getAudioChannel();
    }

    @Override
    public int getAudioEncoding() {
        return sAIAudioRecorder.getAudioEncoding();
    }

    @Override
    public boolean isRecording(AIRecordListener listener) {
        return sAIAudioRecorder.isRecording() && sAIAudioRecorder.isRegistered(listener);
    }

    @Override
    public void startCachingData() {
        sAIAudioRecorder.startCachingData();
    }

    @Override
    public void startSendCachingData() {
        sAIAudioRecorder.startSendCachingData();
    }


    private long generateTokenId() {
        return Util.generateRandom(8);
    }

}
