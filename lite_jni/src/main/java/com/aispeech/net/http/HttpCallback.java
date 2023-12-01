package com.aispeech.net.http;

import java.io.IOException;

public interface HttpCallback {

    void onFailure(IHttp iHttp, IOException e);

    void onResponse(IHttp iHttp, IResponse response) throws IOException;
}
