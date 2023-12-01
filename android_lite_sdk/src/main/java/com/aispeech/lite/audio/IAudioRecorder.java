package com.aispeech.lite.audio;

import com.aispeech.lite.AISampleRate;

public interface IAudioRecorder {

	public long startRecorder(AIRecordListener listener);

	public void unRegisterRecorder(AIRecordListener listener);

	public void releaseRecorder();

	public AISampleRate getSampleRate();

	public int getAudioChannel();

	public int getAudioEncoding();

	public boolean isRecording(AIRecordListener listener);

	public void startCachingData();

	public void startSendCachingData();

}
