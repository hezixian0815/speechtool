package com.aispeech.net.http;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.Response;

public class ResponseImpl implements IResponse {

    private Response okhttpResponse;

    public ResponseImpl(Response okhttpResponse) {
        this.okhttpResponse = okhttpResponse;
    }

    @Override
    public int code() {
        return okhttpResponse != null ? okhttpResponse.code() : 0;
    }

    @Override
    public boolean isSuccessful() {
        return code() >= 200 && code() < 300;
    }

    @Override
    public InputStream byteStream() {
        return okhttpResponse != null && okhttpResponse.body() != null ? okhttpResponse.body().byteStream() : null;
    }

    @Override
    public String string() {
        try {
            return okhttpResponse != null && okhttpResponse.body() != null ? okhttpResponse.body().string() : null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Response getResponse() {
        return okhttpResponse;
    }
}
