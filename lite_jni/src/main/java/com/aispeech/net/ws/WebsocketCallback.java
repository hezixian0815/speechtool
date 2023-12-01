package com.aispeech.net.ws;

public interface WebsocketCallback {

    void onOpen(IWebsocket webSocket);

    void onMessage(IWebsocket webSocket, String text);

    void onClosing(IWebsocket webSocket, int code, String reason);

    void onClosed(IWebsocket webSocket, int code, String reason);

    void onFailure(IWebsocket webSocket, Throwable t);
}
