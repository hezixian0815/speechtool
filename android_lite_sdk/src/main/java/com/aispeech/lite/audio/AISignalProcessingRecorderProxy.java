package com.aispeech.lite.audio;

import android.media.AudioFormat;

import com.aispeech.lite.AISampleRate;
import com.aispeech.lite.fespx.FespxProcessor;

public class AISignalProcessingRecorderProxy implements IAudioRecorder {
	private FespxProcessor mFespxProcessor;

	
	public static AISignalProcessingRecorderProxy create(FespxProcessor fespxProcessor) {
		return new AISignalProcessingRecorderProxy(fespxProcessor);
	}

	private AISignalProcessingRecorderProxy(FespxProcessor fespxProcessor) {
		this.mFespxProcessor = fespxProcessor;
	}


	
	public boolean isRunning() {
    	return mFespxProcessor.isRunning();
    }

	@Override
	public long startRecorder(AIRecordListener listener) {
		mFespxProcessor.registerListener(listener);
		return 0;
	}

	@Override
	public void unRegisterRecorder(AIRecordListener listener) {
		mFespxProcessor.unRegisterListener(listener);
	}

	@Override
	public void releaseRecorder() {
		
	}

	@Override
	public AISampleRate getSampleRate() {
		return AISampleRate.SAMPLE_RATE_16K;
	}

	@Override
	public int getAudioChannel() {
		return 8;
	}

	@Override
	public int getAudioEncoding() {
		return AudioFormat.ENCODING_PCM_16BIT;
	}

	@Override
	public boolean isRecording(AIRecordListener listener) {
		return (mFespxProcessor.isRecorderRecording() || mFespxProcessor.isUseCustomFeed())
				&& mFespxProcessor.isRegistered(listener);
	}

	@Override
	public void startCachingData() {

	}

	@Override
	public void startSendCachingData() {

	}

}
