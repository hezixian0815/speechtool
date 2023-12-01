package com.aispeech.net.http;

import java.io.InputStream;

import okhttp3.Response;

public interface IResponse {

    int code();

    boolean isSuccessful();

    InputStream byteStream();

    String string();

    Response getResponse();
}
