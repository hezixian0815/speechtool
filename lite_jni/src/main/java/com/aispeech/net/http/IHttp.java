package com.aispeech.net.http;

import java.util.Map;

import okhttp3.Request;

public interface IHttp {

    void get(String url, HttpCallback callback);

    void post(String url, String json, HttpCallback callback);

    void postMultipart(String url, String json, String filepath, HttpCallback callback);

    void post(String url, Map<String, String> map, String filepath, HttpCallback callback);

    void post(String url, Request.Builder requestBuilder, int type, String json, HttpCallback callback);

    void post(String url, Request.Builder requestBuilder, Map<String, String> map, String filepath, HttpCallback callback);

    void cancel();
}
