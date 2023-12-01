package com.aispeech.common;

public interface CustomLog {
    int setLogLevel();

    void v(String tag, String message);

    void d(String tag, String message);

    void i(String tag, String message);

    void w(String tag, String message);

    void e(String tag, String message);

    void f(String tag, String message);
}
