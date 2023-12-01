package com.aispeech.lite.fespx;

import android.text.TextUtils;

import com.aispeech.auth.AIAuthEngine;
import com.aispeech.common.Log;
import com.aispeech.common.ThreadNameUtil;
import com.aispeech.kernel.Opus;
import com.aispeech.lite.param.CloudASRParams;
import com.aispeech.net.ws.IWebsocket;
import com.aispeech.net.ws.WebsocketCallback;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 唤醒模型云端二次校验
 */
public class WakeupCloudCheck {

    public interface OnCloudCheckListener {
        void onCloudCheck(String asr);

        void onError();
    }

    public WakeupCloudCheck() {
        callback = new WebsocketCallbackImpl("");
        callback.setWakeupCloudCheck(this);
        multiWebSocketCreator = new MultiWebSocketCreator();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channels", 1);
            jsonObject.put("samplerate", 16000);
            jsonObject.put("bitrate", 32000);
            jsonObject.put("complexity", 8);
            jsonObject.put("framesize", 20);
        } catch (Exception e) {
            e.printStackTrace();
        }
        opusParam = jsonObject.toString();
    }

    private static final String TAG = "WakeupCloudCheck";
    private int channel = 4;
    private boolean isEncodeOpus = false;
    private final List<IWebsocket> webSocketList = new LinkedList<>();
    private final List<Opus> opusList = new LinkedList<>();
    private OnCloudCheckListener listener;
    private String[] cnWakeupWord = null;
    private WebsocketCallbackImpl callback;
    private Timer mTimer;
    /**
     * 0 无操作状态，1 start 后 feed 音频状态，2 stop 后等待识别结果状态
     */
    private int status = 0;
    private final MultiWebSocketCreator multiWebSocketCreator;
    private final String opusParam;
    private int uploadAudioLength;
    private List<String> recordIdList;

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }

    public synchronized boolean isFeedingStatus() {
        return status == 1;
    }

    public synchronized boolean isASRingStatus() {
        return status == 2;
    }

    private int timeout = 600;

    public synchronized void init(String[] cnWakeupWord, int timeout, OnCloudCheckListener listener) {
        this.cnWakeupWord = cnWakeupWord;
        this.timeout = timeout;
        this.listener = listener;
        multiWebSocketCreator.init(channel, isEncodeOpus ? CloudASRParams.OPUS : CloudASRParams.WAV, AIAuthEngine.getInstance().getProfile(), cnWakeupWord, callback);
    }

    public synchronized void destroy() {
        Log.d(TAG, "destroy");
        this.listener = null;
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
        multiWebSocketCreator.destroy();
        closeAll();
    }

    /**
     * 预唤醒时开始上传音频
     */
    public synchronized boolean start() {
        if (webSocketList.size() > 0) {
            // 为了解决 预唤醒 唤醒 预唤醒 没有再次真正唤醒 这种情况
            Log.w(TAG, "还有 webSocket 请求未完，不能再请求");
            return false;
        }

        Log.d(TAG, "start()============>");
        Object[] oo = multiWebSocketCreator.getWebSockets();
        webSocketList.addAll((List<IWebsocket>) oo[0]);
        recordIdList = (List<String>) oo[1];
        if (isEncodeOpus)
            createOpusList();
        else
            destroyOpusList();
        startTimer(timeout);
        uploadAudioLength = 0;
        status = 1;
        return true;
    }

    private synchronized void createOpusList() {
        destroyOpusList();

        Opus opus;
        for (int i = 0; i < channel; i++) {
            // false Opus 非标准，true OGG_OPUS 标准
            opus = new Opus();
            opus.ddsInit(false, Opus.PCM_TO_OPUS, opusParam);
            opusList.add(opus);
        }
    }

    private synchronized void destroyOpusList() {
        if (!opusList.isEmpty()) {
            for (int i = opusList.size() - 1; i >= 0; i--) {
                Opus o = opusList.remove(i);
                o.ddsDestroy();
            }
        }
    }


    public synchronized void stop() {
        if (status != 1)
            return;
        Log.d(TAG, "stop() <============");

        int times = 3;
        while (times > 0) {
            int feedCount = 0;
            for (int i = 0; i < channel; i++) {
                List<byte[]> list = new LinkedList<>();
                list.add(new byte[0]);
                feedCount += feed(i, list);
            }
            if (feedCount != 0) {
                Log.d(TAG, "stop() feedCount:" + feedCount + " times:" + times);
                times--;
                feedCount = 0;
            } else {
                break;
            }
        }
        destroyOpusList();
        Log.d(TAG, "upload audio length " + uploadAudioLength);
        status = 2;
    }

    public synchronized void cancelAll() {
        Log.d(TAG, "cancelAll");
        for (int i = 0; i < webSocketList.size(); i++) {
            IWebsocket w = webSocketList.get(i);
            if (w != null)
                w.cancel();
        }
        webSocketList.clear();
    }

    private byte[] opusBuffer = new byte[3200];

    public synchronized int feed(int channelCount, List<byte[]> data) {
        if (data == null)
            return -1;
        if (webSocketList.size() < channelCount + 1) {
            // Log.d(TAG, "webSocket 未准备好,无需发送数据");
            return -2;
        }

        int feedCount = 0;
        Log.d(TAG, "feed data.length " + data.size());
        for (int i = 0; i < data.size(); i++) {
            IWebsocket w;
            try {
                w = webSocketList.get(channelCount);
            } catch (Exception e) {
                Log.d(TAG, "webSocketList.get " + channelCount + " " + e);
                continue;
            }
            if (data.get(i) == null) {
                Log.d(TAG, "data[] " + i + " is null");
                continue;
            }

            if (isEncodeOpus) {
                if (opusList.size() > i) {
                    Opus opus = opusList.get(i);
                    int len = opus.ddsFeed(data.get(i), data.get(i).length, opusBuffer);
                    uploadAudioLength += len;
                    byte[] sendByte = new byte[len];
                    System.arraycopy(opusBuffer, 0, sendByte, 0, len);

                    boolean suc = w.send(sendByte);
                    feedCount += len;
                    // Log.d(TAG, "send index:" + i + " data[i].length:" + data[i].length + " OpusLen:" + len + " suc:" + suc);
                } else {
                    Log.w(TAG, "no Opus Object in opusList index:" + i);
                }
            } else {
                boolean suc = w.send(data.get(i));
                uploadAudioLength += data.get(i).length;
                feedCount += data.get(i).length;
            }
        }
        return feedCount;
    }

    public synchronized void closeAll() {
        destroyOpusList();
        cancelAll();
        status = 0;
    }

    private synchronized void onError() {
        if (listener != null) {
            listener.onError();
        }
        closeAll();
    }

    private synchronized void onCloudCheck(IWebsocket iWebsocket, String requestId, String asr) {
        if (!webSocketList.contains(iWebsocket))
            return;
        if (recordIdList == null || !recordIdList.contains(requestId))
            return;
        if (cnWakeupWord != null && cnWakeupWord.length > 0) {
            for (String s : cnWakeupWord) {
                if (asr.equals(s)) {
                    if (listener != null) {
                        listener.onCloudCheck(asr);
                    }
                    closeAll();
                    break;
                }
            }
        }
    }

    private synchronized void onWebsocketClosedOrFailure(IWebsocket websocket) {
        Log.d(TAG, "onWebsocketClosedOrFailure() ");
        // multiWebSocketCreator.createNewWhenFailure(websocket);
        if (!webSocketList.contains(websocket))
            return;
        webSocketList.remove(websocket);
        if (webSocketList.size() == 0) {
            if (listener != null) {
                listener.onError();
            }
            destroyOpusList();
            status = 0;
        }
    }

    private class WebsocketCallbackImpl implements WebsocketCallback {

        private WakeupCloudCheck wakeupCloudCheck;
        private final String tag;

        public WebsocketCallbackImpl(String tag) {
            this.tag = TAG + "_" + tag;
        }

        public void setWakeupCloudCheck(WakeupCloudCheck wakeupCloudCheck) {
            this.wakeupCloudCheck = wakeupCloudCheck;
        }

        @Override
        public void onOpen(IWebsocket iWebsocket) {
            Log.d(tag, "onOpen()");
        }

        @Override
        public void onMessage(IWebsocket iWebsocket, String text) {
            Log.d(tag, "onMessage() text " + text);
            try {
                JSONObject jsonObject = new JSONObject(text);
                String requestId = jsonObject.getString("requestId");
                JSONObject resultJson = jsonObject.getJSONObject("result");
                double conf = resultJson.getDouble("conf");
                String rec = resultJson.getString("rec").replace(" ", "").trim();
                Log.d(TAG, "onMessage:conf " + conf + " rec " + rec);
                if (wakeupCloudCheck != null && conf > 0.8 && !TextUtils.isEmpty(rec))
                    wakeupCloudCheck.onCloudCheck(iWebsocket, requestId, rec);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onClosing(IWebsocket iWebsocket, int code, String reason) {
            Log.d(tag, "onClosing() code " + code + " reason " + reason);
        }

        @Override
        public void onClosed(IWebsocket iWebsocket, int code, String reason) {
            Log.d(tag, "onClosed() code " + code + " reason " + reason);
            if (wakeupCloudCheck != null) {
                wakeupCloudCheck.onWebsocketClosedOrFailure(iWebsocket);
            }
        }

        @Override
        public void onFailure(IWebsocket iWebsocket, Throwable throwable) {
            Log.d(tag, "onFailure() throwable " + throwable + "  " + iWebsocket);
            if (wakeupCloudCheck != null) {
                wakeupCloudCheck.onWebsocketClosedOrFailure(iWebsocket);
            }
        }
    }

    private synchronized void startTimer(int timeout) {
        if (mTimer != null)
            mTimer.cancel();
        mTimer = new Timer(ThreadNameUtil.getSimpleThreadName("timer-wake-check"));
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "timeout");
                cancelAll();
                if (webSocketList.size() > 0)
                    onError();
                else
                    Log.d(TAG, "timeout but no error");
            }
        };
        mTimer.schedule(timerTask, timeout);
        Log.d(TAG, "startTimer timeout:" + timeout);
    }

    public int getStatus() {
        return status;
    }
}
