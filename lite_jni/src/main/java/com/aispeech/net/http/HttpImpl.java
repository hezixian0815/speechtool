package com.aispeech.net.http;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.net.NetConfig;
import com.aispeech.net.dns.DnsResolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpImpl implements IHttp {

    private static final String TAG = "HttpImpl";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static volatile IHttp instance;
    private OkHttpClient mClient;
    private Call mCall;

    private HttpImpl() {
        OkHttpClient.Builder builder = new OkHttpClient()
                .newBuilder()
                .pingInterval(30, TimeUnit.SECONDS)// websocket 轮训间隔
                .readTimeout(20, TimeUnit.SECONDS);
        if (NetConfig.isUseSpeechDns()) {
            builder.dns(new DnsResolver());
        }
        mClient = builder.build();
    }

    public synchronized static IHttp getInstance() {
        if (null == instance) {
            instance = new HttpImpl();
        }

        return instance;
    }

    @Override
    public void get(String url, final HttpCallback callback) {
        Log.d(TAG, "GET " + url);
        Request request = new Request
                .Builder()
                .url(url)
                .get()
                .build();
        mCall = mClient.newCall(request);
        mCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null)
                    callback.onFailure(HttpImpl.this, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (callback != null)
                    callback.onResponse(HttpImpl.this, new ResponseImpl(response));
            }
        });
    }

    @Override
    public void post(String url, String json, final HttpCallback callback) {
        Log.d(TAG, "POST json " + url + "  " + json);
        Request request = new Request
                .Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(JSON, json))
                .build();
        mCall = mClient.newCall(request);
        mCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null)
                    callback.onFailure(HttpImpl.this, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (callback != null)
                    callback.onResponse(HttpImpl.this, new ResponseImpl(response));
            }
        });
    }

    @Override
    public void postMultipart(String url, String json, String filepath, final HttpCallback callback) {

        if (TextUtils.isEmpty(filepath)) {
            Log.w(TAG, "file not exist: " + filepath);
            callback.onFailure(HttpImpl.this, new FileNotFoundException(filepath));
            return;
        }

        File file = new File(filepath);
        if (!file.exists() || !file.isFile()) {
            Log.w(TAG, "file not exist: " + filepath);
            callback.onFailure(HttpImpl.this, new FileNotFoundException(filepath));
            return;
        }

        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(),
                        RequestBody.create(MediaType.parse("application/octet-stream"), file));
        if (!TextUtils.isEmpty(json)) {
            builder.addFormDataPart("params", json);
        }
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(builder.build())
                .build();
        mCall = mClient.newCall(request);
        mCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null)
                    callback.onFailure(HttpImpl.this, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (callback != null)
                    callback.onResponse(HttpImpl.this, new ResponseImpl(response));
            }
        });
    }

    @Override
    public void post(String url, Map<String, String> map, String filepath, final HttpCallback callback) {
        Log.d(TAG, "POST Multipart " + url + " filepath " + filepath + " map " + map);
        File file = null;
        if (!TextUtils.isEmpty(filepath)) {
            file = new File(filepath);
            if (!file.exists() || !file.isFile()) {
                Log.d(TAG, "file not exist: " + filepath);
                callback.onFailure(HttpImpl.this, new FileNotFoundException(filepath));
                return;
            }
        }
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        if (map != null && !map.isEmpty()) {
            Iterator<String> iter = map.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                builder.addFormDataPart(key, map.get(key));
                Log.d(TAG, "post:key " + key + "  " + map.get(key));
            }
            boolean existFileKey = map.containsKey("file");
            if (existFileKey)
                Log.d(TAG, "exist file key");
        }
        if (file != null) {
            Log.d(TAG, "post:11 " + file.getName());
            builder.addFormDataPart("file", file.getName(),
                    RequestBody.create(MediaType.parse("application/octet-stream"), file));
        }

        Request request = new Request.Builder().url(url).post(builder.build()).build();

        mCall = mClient.newCall(request);
        mCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null)
                    callback.onFailure(HttpImpl.this, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (callback != null)
                    callback.onResponse(HttpImpl.this, new ResponseImpl(response));
            }
        });
    }


    @Override
    public void post(String url, Request.Builder requestBuilder, int type, String json, final HttpCallback callback) {
        Log.d(TAG, "POST json " + url + "  " + json);
        Request request = requestBuilder.url(url)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(JSON, json))
                .build();
        mCall = mClient.newCall(request);
        mCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null)
                    callback.onFailure(HttpImpl.this, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (callback != null)
                    callback.onResponse(HttpImpl.this, new ResponseImpl(response));
            }
        });
    }

    @Override
    public void post(String url, Request.Builder requestBuilder, Map<String, String> map, String filepath, final HttpCallback callback) {
        Log.d(TAG, "POST Multipart " + url + " filepath " + filepath + " map " + map);
        File file = null;
        if (!TextUtils.isEmpty(filepath)) {
            file = new File(filepath);
            if (!file.exists() || !file.isFile()) {
                Log.d(TAG, "file not exist: " + filepath);
                callback.onFailure(HttpImpl.this, new FileNotFoundException(filepath));
                return;
            }
        }
        MultipartBody.Builder builder = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);
        builder.setType(MultipartBody.FORM);
        if (map != null && !map.isEmpty()) {
            Iterator<String> iter = map.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                builder.addFormDataPart(key, map.get(key));
            }
            boolean existFileKey = map.containsKey("file");
            if (existFileKey)
                Log.d(TAG, "exist file key");
        }
        if (file != null) {
            builder.addFormDataPart("file", file.getName(),
                    RequestBody.create(MediaType.parse("multipart/form-data"), file));
        }


        Map<String, String> headersParams = new HashMap<>();
        headersParams.put("Content-Type", "application/json");
        headersParams.put("Transfer-Encoding", "chunked");
        headersParams.put("Connection", "keep-alive");
        headersParams.put("Vary", "Accept-Encoding");
        headersParams.put("Content-Encoding", "gzip");


        Request request = requestBuilder
                //.headers(setHeaders(headersParams))
                .url(url).post(builder.build()).build();

        mCall = mClient.newCall(request);
        mCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (callback != null)
                    callback.onFailure(HttpImpl.this, e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (callback != null)
                    callback.onResponse(HttpImpl.this, new ResponseImpl(response));
            }
        });
    }

    public static Headers setHeaders(Map<String, String> headersParams) {
        Headers headers;
        okhttp3.Headers.Builder hBuilder = new okhttp3.Headers.Builder();
        if (headersParams != null) {
            Iterator<String> iterator = headersParams.keySet().iterator();
            String key = "";
            while (iterator.hasNext()) {
                key = iterator.next();
                hBuilder.add(key, headersParams.get(key));
            }
        }
        headers = hBuilder.build();
        return headers;
    }

    @Override
    public void cancel() {
        if (mCall != null && mCall.isExecuted() && !mCall.isCanceled()) {
            Log.d(TAG, "cancel");
            mCall.cancel();
        }
    }
}
