package com.aispeech.export.listeners;

import com.aispeech.lite.speech.EngineListener;

/**
 * 语义事件监听器
 */
public abstract class AILocalSemanticListener implements IListener, EngineListener {

    @Override
    public void onReadyForSpeech() {

    }

    @Override
    public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

    }

    @Override
    public void onRawDataReceived(byte[] buffer, int size) {

    }
}
