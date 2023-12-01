package com.aispeech.lite.net;


import com.aispeech.common.Log;
import com.aispeech.common.ThreadNameUtil;
import com.aispeech.common.Util;
import com.aispeech.net.NetProxy;
import com.aispeech.net.ws.IWebsocket;
import com.aispeech.net.ws.WebsocketCallback;

import java.io.EOFException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by yrl on 17-10-13.
 */

public class WebsocketClient {
    private static final String TAG = "WebsocketClient";
    private static final int NORMAL_CLOSURE_STATUS = 1000;

    private IWebsocket mWebSocket;
    private WSClientListener mCloudListener;
    /**
     * socket 状态，0 未打开，1 已打开，2 关闭，3 主动关闭
     */
    private int socketStatus = 0;
    private Timer mTimer;
    private boolean mCancelFlag = false;

    /**
     * 连接超时
     */
    private int connectTimeout = 3000;

    public WebsocketClient() {

    }

    public WebsocketClient(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * socket 链接状态
     *
     * @return true|false
     */
    public boolean isConnected() {
        Log.i(TAG, "isConnected: " + (socketStatus == 1));
        return socketStatus == 1;
    }

    public synchronized void startRequest(String url, WSClientListener cloudListener) {
        mCloudListener = cloudListener;
        if (mWebSocket == null) {
            socketStatus = 0;
            mCancelFlag = false;
            lasrMessage = null;
            mWebSocket = NetProxy.newWebsocket(url, new WebsocketCallbackImpl());
            Log.d(TAG, "new websocket " + mWebSocket);
            startTimer();
        }
    }

    private String lasrMessage = null;

    public void setLasrMessage(String lasrMessage) {
        this.lasrMessage = lasrMessage;
    }

    public synchronized void sendText(String message) {
        if (mWebSocket != null) {
            boolean suc = mWebSocket.send(message);
            Log.d(TAG, "sendText " + suc + " " + message);
        }
    }

    public synchronized void sendBinary(byte[] data) {
        if (data == null)
            return;
        if (mWebSocket != null) {
            if (data.length > 30000) {
                Log.d(TAG, "async message support and buffer too small. Buffer size: 30000, Message size: " + data.length);
                List<byte[]> byteList = Util.handAudio(data, 20000);
                if (!byteList.isEmpty()) {
                    for (byte[] bytes : byteList) {
                        if (bytes.length > 0) {
                            boolean suc = mWebSocket.send(bytes);
                            Log.d(TAG, "sendBinary split " + suc + " " + bytes.length);
                        }
                    }
                }
            } else {
                boolean suc = mWebSocket.send(data);
                Log.d(TAG, "sendBinary " + suc + " " + data.length);
            }
        } else {
            Log.e(TAG, "mWebSocket IS NULL");
        }
    }

    private synchronized void resetWebSocket(IWebsocket webSocket) {
        if (webSocket == mWebSocket) {
            mWebSocket = null;
            Log.i(TAG, "resetWebSocket");
        }
    }

    public synchronized void closeWebSocket() {
        if (mWebSocket != null) {
            mWebSocket.close(NORMAL_CLOSURE_STATUS, "Goodbye!");
            Log.d(TAG, "closeWebSocket");
            mCancelFlag = true;
//            mWebSocket.cancel();
            mWebSocket = null;
            socketStatus = 3;
        }
        if (mCloudListener != null) {
            mCloudListener.onClose();
        }
    }

    public synchronized void destroy() {
        Log.d(TAG, "destroy");
        closeWebSocket();

        if (mCloudListener != null) {
            mCloudListener = null;
        }
    }

    public class WebsocketCallbackImpl implements WebsocketCallback {


        @Override
        public void onOpen(IWebsocket webSocket) {
            Log.i(TAG, "onOpen");
            if (lasrMessage != null && lasrMessage.length() > 0 && webSocket == mWebSocket) {
                boolean suc = mWebSocket.send(lasrMessage);
                Log.d(TAG, "onOpen send " + suc + " " + lasrMessage);
                lasrMessage = null;
            }
            socketStatus = 1;
            if (mCloudListener != null) {
                mCloudListener.onOpen();
            }
        }

        @Override
        public void onMessage(IWebsocket webSocket, String text) {
            Log.i(TAG, "Receiving: " + text);
            try {
                if (mCloudListener != null) {
                    mCloudListener.onMessage(text);
                }
            } catch (Exception e) {

            }
        }

        @Override
        public void onClosing(IWebsocket webSocket, int code, String reason) {
            webSocket.close(NORMAL_CLOSURE_STATUS, null);
            Log.i(TAG, "Closing: " + code + " " + reason);
        }

        @Override
        public void onClosed(IWebsocket webSocket, int code, String reason) {
            synchronized (WebsocketClient.this) {
                socketStatus = 2;
                Log.i(TAG, "Closed: " + code + " " + reason);
                try {
                    if (mCloudListener != null && webSocket == mWebSocket && !mCancelFlag && code != 1000) {
                        mCloudListener.onError("Closed:" + code + " " + reason);
                    }
                    mCancelFlag = false;
                } catch (Exception e) {

                }
                resetWebSocket(webSocket);
            }
        }

        @Override
        public void onFailure(IWebsocket webSocket, Throwable t) {
            synchronized (WebsocketClient.this) {
                socketStatus = 2;
                t.printStackTrace();
                Log.i(TAG, "onFailure BEFORE: ");
                try {
                    // Log.d(TAG, "onFailure: "+response.body().string());
                    //EOFException代表链接已经完成了数据传输，当前链接已经断开，这种情形下，非网络错误，不再输出网络错误异常
                    if (mCloudListener != null && webSocket == mWebSocket && !mCancelFlag && !(t instanceof EOFException)) {
                        mCloudListener.onError(t.getMessage());
                    }
                    mCancelFlag = false;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "onFailure END: ");
                Log.i(TAG, "cancel current webSocket");
                webSocket.cancel();
                resetWebSocket(webSocket);
            }
        }
    }

    private void startTimer() {
        if (mTimer != null)
            mTimer.cancel();
        mTimer = new Timer(ThreadNameUtil.getSimpleThreadName("timer-ws"));
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                if (mWebSocket == null) return;
                if (mCloudListener != null && socketStatus == 0) {
                    Log.d(TAG, "websocket connect timeout");
                    mCloudListener.onError("network error, connect asr server timeout");
                }
            }
        };
        mTimer.schedule(timerTask, 8000);
    }


}
