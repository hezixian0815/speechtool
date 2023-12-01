package com.aispeech.net;

import com.aispeech.net.http.HttpEmpty;
import com.aispeech.net.http.HttpImpl;
import com.aispeech.net.http.IHttp;
import com.aispeech.net.ws.IWebsocket;
import com.aispeech.net.ws.WebsocketCallback;
import com.aispeech.net.ws.WebsocketImpl;

public class NetProxy {

    private static boolean IHttpNotImplement = false;
    private static boolean IWebsocketNotImplement = false;

    public static IHttp getHttp() {
        if (!IHttpNotImplement) {
            try {
                return HttpImpl.getInstance();
            } catch (NoClassDefFoundError e) {
                IHttpNotImplement = true;
            }
        }
        return HttpEmpty.getInstance();
    }

    public static IWebsocket newWebsocket(String url, WebsocketCallback callback) {
        if (!IWebsocketNotImplement) {
            try {
                return new WebsocketImpl(url, callback);
            } catch (NoClassDefFoundError e) {
                IWebsocketNotImplement = true;
            }
        }
        return null;
    }
}
