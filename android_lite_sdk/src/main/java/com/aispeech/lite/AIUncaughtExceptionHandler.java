package com.aispeech.lite;

import com.aispeech.common.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;

public class AIUncaughtExceptionHandler implements UncaughtExceptionHandler {

    public AIUncaughtExceptionHandler() {
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        String stackTrace = getStackTrace(ex);
        if (AISpeechSDK.LOGCAT_DEBUGABLE) {
            Log.e("AIUncaughtExceptionHandler", stackTrace);
        }else{
            Log.w("AIUncaughtExceptionHandler", stackTrace);
        }

//        System.exit(2);
    }

    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw, true);
        throwable.printStackTrace(pw);
        return sw.getBuffer().toString();
    }
}