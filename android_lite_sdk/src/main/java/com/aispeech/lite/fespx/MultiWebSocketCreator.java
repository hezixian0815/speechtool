package com.aispeech.lite.fespx;

import com.aispeech.auth.AIProfile;
import com.aispeech.common.AuthUtil;
import com.aispeech.common.Log;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.param.CloudASRParams;
import com.aispeech.net.ws.IWebsocket;
import com.aispeech.net.ws.WebsocketCallback;
import com.aispeech.net.ws.WebsocketImpl;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

public class MultiWebSocketCreator {

    private static final String TAG = "MultiWebSocketCreator";

    public MultiWebSocketCreator() {
    }

    private CloudASRParams mCloudASRParams;
    private String deviceSecret;
    private int channel;
    private WebsocketCallback callback;

    public synchronized void init(int channel, int audioType, AIProfile profile, String[] cnWakeupWord, WebsocketCallback callback) {
        Log.d(TAG, "init");
        this.channel = channel;
        this.callback = callback;
        mCloudASRParams = new CloudASRParams(profile);
        mCloudASRParams.setAudioType(audioType);
        if (cnWakeupWord != null && cnWakeupWord.length > 0) {
            JSONArray jsonArray = null;
            try {
                jsonArray = new JSONArray(cnWakeupWord);
                Log.d(TAG, "jsonArray " + jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            mCloudASRParams.setCustomWakeupWord(jsonArray);
        }
        deviceSecret = profile.getDeviceSecret();
    }

    private synchronized List<IWebsocket> createWebSockets() {
        List<IWebsocket> webSocketList = new LinkedList<>();
        for (int i = 0; i < channel; i++) {
            String url = getUrl(mCloudASRParams, deviceSecret);
            Log.d(TAG, "createWebSockets:url " + url);
            webSocketList.add(new WebsocketImpl(url, callback));
        }
        return webSocketList;
    }

    private synchronized List<String> sendParams(List<IWebsocket> webSocketList) {
        List<String> recordIdList = new LinkedList<>();
        for (IWebsocket w : webSocketList) {
            String recordId = Utils.getRecorderId();
            recordIdList.add(recordId);
            Log.d(TAG, "recordId " + recordId);
            mCloudASRParams.setRequestId(recordId);
            String params = mCloudASRParams.toJSON().toString();
            boolean suc = w.send(params);
            Log.d(TAG, "params " + params);
            Log.d(TAG, "send params IWebsocket:" + w + " suc:" + suc);
        }
        return recordIdList;
    }

    private String getUrl(CloudASRParams params, String deviceSecret) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String sig = AuthUtil.getSignature(params.getDeviceName() + nonce + params.getProductId() + timestamp,
                deviceSecret);
        StringBuilder url = new StringBuilder();

        url.append(params.getServer())
                .append("?productId=")
                .append(params.getProductId())
                .append("&res=")
                .append(params.getRes())
                .append("&deviceName=")
                .append(params.getDeviceName())
                .append("&timestamp=")
                .append(timestamp)
                .append("&nonce=")
                .append(nonce)
                .append("&sig=")
                .append(sig);


        return url.toString();
    }

    public synchronized Object[] getWebSockets() {
        List<IWebsocket> webSocketList = createWebSockets();
        List<String> recordIdList = sendParams(webSocketList);
        Object[] oo = new Object[2];
        oo[0] = new LinkedList<>(webSocketList);
        oo[1] = recordIdList;
        return oo;
    }

    public synchronized void destroy() {
        Log.d(TAG, "destroy");
        callback = null;
    }
}
