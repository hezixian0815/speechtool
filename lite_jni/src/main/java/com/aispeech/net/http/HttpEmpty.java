package com.aispeech.net.http;

import android.os.Handler;
import android.os.Looper;

import java.util.Map;

import okhttp3.Request;

public class HttpEmpty implements IHttp {

    private static IHttp instance = new HttpEmpty();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    private HttpEmpty() {

    }

    public static IHttp getInstance() {
        return instance;
    }

    @Override
    public void get(String url, final HttpCallback callback) {
        if (callback == null)
            return;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(instance, new NoImplementHttpException("NoImplementHttpException"));
            }
        }, 200);
    }

    @Override
    public void post(String url, String content, final HttpCallback callback) {
        if (callback == null)
            return;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(instance, new NoImplementHttpException("NoImplementHttpException"));
            }
        }, 200);
    }

    @Override
    public void postMultipart(String url, String json, String filepath, final HttpCallback callback) {
        if (callback == null)
            return;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(instance, new NoImplementHttpException("NoImplementHttpException"));
            }
        }, 200);
    }

    @Override
    public void post(String url, Map<String, String> map, String filepath, final HttpCallback callback) {
        if (callback == null)
            return;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(instance, new NoImplementHttpException("NoImplementHttpException"));
            }
        }, 200);
    }

    @Override
    public void post(String url, Request.Builder requestBuilder, Map<String, String> map,
                     String filepath, final HttpCallback callback) {
        if (callback == null)
            return;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(instance, new NoImplementHttpException("NoImplementHttpException"));
            }
        }, 200);
    }


    @Override
    public void post(String url, Request.Builder requestBuilder, int type, String json,
                     final HttpCallback callback) {
        if (callback == null)
            return;
        mainHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                callback.onFailure(instance, new NoImplementHttpException("NoImplementHttpException"));
            }
        }, 200);
    }


    @Override
    public void cancel() {

    }
}
