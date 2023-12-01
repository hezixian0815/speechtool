package com.aispeech.net.ws;

import com.aispeech.net.NetConfig;
import com.aispeech.net.dns.DnsResolver;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

public class WebsocketImpl implements IWebsocket {

    private static volatile OkHttpClient mClient;
    private WebSocket mWebSocket;

    public WebsocketImpl(String url, WebsocketCallback callback) {
        this(url, 0, callback);
    }

    public WebsocketImpl(String url, int pingInterval, WebsocketCallback callback) {
        if (mClient == null)
            synchronized (WebsocketImpl.class) {
                if (mClient == null) {
                    OkHttpClient.Builder builder = new OkHttpClient()
                            .newBuilder()
                            .pingInterval(pingInterval, TimeUnit.SECONDS)// websocket 轮训间隔
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .connectTimeout(30, TimeUnit.SECONDS);
                    if (NetConfig.isUseSpeechDns()) {
                        builder.dns(new DnsResolver());
                    }
                    // .sslSocketFactory(new TLSSocketFactory(), TLSSocketFactory.DEFAULT_TRUST_MANAGERS)
                    mClient = builder.build();
                }
            }
        Request request = new Request.Builder().url(url).build();
        mWebSocket = mClient.newWebSocket(request, new WebSocketListenerImpl(this, callback));
    }

    @Override
    public long queueSize() {
        return mWebSocket != null ? mWebSocket.queueSize() : 0;
    }

    @Override
    public boolean send(String data) {
        if (mWebSocket == null)
            return false;
        return mWebSocket.send(data);
    }

    @Override
    public boolean send(byte[] data) {
        if (mWebSocket == null)
            return false;
        return mWebSocket.send(ByteString.of(data));
    }

    @Override
    public boolean close(int var1, String var2) {
        if (mWebSocket == null)
            return false;
        return mWebSocket.close(var1, var2);
    }

    @Override
    public void cancel() {
        if (mWebSocket != null)
            mWebSocket.cancel();
    }


    private static class WebSocketListenerImpl extends WebSocketListener {
        private WebsocketCallback callback;
        private IWebsocket mIWebsocket;

        public WebSocketListenerImpl(IWebsocket mIWebsocket, WebsocketCallback callback) {
            super();
            this.mIWebsocket = mIWebsocket;
            this.callback = callback;
        }

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            super.onOpen(webSocket, response);
            if (callback != null)
                callback.onOpen(mIWebsocket);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            super.onMessage(webSocket, text);
            if (callback != null)
                callback.onMessage(mIWebsocket, text);
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            super.onClosing(webSocket, code, reason);
            if (callback != null)
                callback.onClosing(mIWebsocket, code, reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            super.onClosed(webSocket, code, reason);
            if (callback != null)
                callback.onClosed(mIWebsocket, code, reason);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable t, Response response) {
            super.onFailure(webSocket, t, response);
            if (callback != null)
                callback.onFailure(mIWebsocket, t);
        }
    }
}
