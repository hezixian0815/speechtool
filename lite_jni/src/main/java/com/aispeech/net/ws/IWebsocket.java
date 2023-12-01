package com.aispeech.net.ws;


public interface IWebsocket {

    long queueSize();

    boolean send(String data);

    boolean send(byte[] data);

    boolean close(int var1, String var2);

    void cancel();
}
