package com.aispeech.export.function;


import com.aispeech.lite.speech.EngineListener;

public interface IEngine<S extends IConfig, T extends IIntent, U extends EngineListener> {
    void init(S config, U listener);

    void start(T intent);

    void stop();

    void destroy();
}

