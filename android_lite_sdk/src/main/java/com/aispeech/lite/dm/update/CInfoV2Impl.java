package com.aispeech.lite.dm.update;

import android.text.TextUtils;

import com.aispeech.common.AuthUtil;
import com.aispeech.common.Log;
import com.aispeech.export.ProductContext;
import com.aispeech.export.Setting;
import com.aispeech.export.SkillContext;
import com.aispeech.export.Vocab;
import com.aispeech.lite.config.CloudDMConfig;
import com.aispeech.lite.net.WSClientListener;
import com.aispeech.lite.net.WebsocketClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.UUID;

/**
 * cinfo 服务v2 接口实现类
 * wiki https://wiki.aispeech.com.cn/pages/viewpage.action?pageId=25268234
 * @deprecated 请使用 dds 同一个 websocket，保证云端服务的时序
 * @author hehr
 */
public class CInfoV2Impl implements ICInfo {

    private static final String TAG = CInfoV2Impl.class.getSimpleName();

    private String v2Host;

    private String deviceName;

    private String deviceSecret;

    private String productId;

    private String aliasKey;

    private CInfoListener mListener;

    private WebsocketClient mClient;

    private boolean isUseFullDuplex;//是否开启全双工配置

    public CInfoV2Impl(String v2Host, String deviceName, String deviceSecret, String productId, String aliasKey, boolean isUseFullDuplex, CInfoListener mListener) {
        this.v2Host = v2Host == null ? CloudDMConfig.DDS_SERVER : v2Host;;
        this.deviceName = deviceName;
        this.deviceSecret = deviceSecret;
        this.productId = productId;
        this.aliasKey = aliasKey;
        this.mListener = mListener;
        this.isUseFullDuplex = isUseFullDuplex;
        mClient = new WebsocketClient();

    }

    /**
     * 获取 websocket url
     *
     * @return
     */
    private String getWebSocketUrl() {

        String timestamp = new Date().getTime() + "";
        String nonce = UUID.randomUUID() + "";
        String sig = AuthUtil.getSignature(deviceName + nonce + productId + timestamp, deviceSecret);

        StringBuilder url = new StringBuilder();
        url.append(v2Host);
        url.append(aliasKey);
        url.append("?serviceType=");
        url.append("websocket");
        url.append("&productId=");
        url.append(productId);
        url.append("&deviceName=");
        url.append(deviceName);
        url.append("&nonce=");
        url.append(nonce);
        url.append("&sig=");
        url.append(sig);
        url.append("&timestamp=");
        url.append(timestamp);

        if (isUseFullDuplex)//全双工协议
            url.append("&communicationType=" + "fullDuplex");

        Log.d(TAG, " url: " + url.toString());

        return url.toString();
    }

    /**
     * websocket 协议上传 cinfo 数据
     *
     * @param text text 上传文本
     */
    private void upload(String text) {
        if (mClient != null) {
            mClient.startRequest(getWebSocketUrl(), new WSClientListener() {
                @Override
                public void onMessage(String text) {
                    Log.d(TAG, "upload success, text : " + text);
                    if (mListener != null) {
                        mListener.onUploadSuccess();
                    }
                    mClient.closeWebSocket();
                }

                @Override
                public void onError(String text) {
                    Log.e(TAG, "upload failed ,text : " + text);
                    if (mListener != null) {
                        mListener.onUploadFailed();
                    }
                    mClient.closeWebSocket();
                }

                @Override
                public void onOpen() {

                }

                @Override
                public void onClose() {

                }
            });
            mClient.sendText(text);
        }
    }


    @Override
    public void uploadVocabs(Vocab... vocabs) {
        Log.e(TAG, "cinfo v2 not implements uploadVocabs");
    }

    @Override
    public void uploadProductContext(ProductContext intent) {
        JSONObject text = new JSONObject();
        try {
            text.put("topic", "system.settings");
            if (TextUtils.equals(ProductContext.OPTION_DELETE, intent.getOption())) {
                text.put("option", ProductContext.OPTION_DELETE);
            }
            JSONArray settings = new JSONArray();
            for (Setting s : intent.getSettings()) {
                settings.put(s.toJSON());
            }
            text.put("settings", settings);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        upload(text.toString());
    }

    @Override
    public void uploadSkillContext(SkillContext context) {
        JSONObject text = new JSONObject();
        try {
            text.put("topic", "skill.settings");
            if (TextUtils.equals(ProductContext.OPTION_DELETE, context.getOption())) {
                text.put("option", SkillContext.OPTION_DELETE);
            }
            JSONArray settings = new JSONArray();
            for (Setting s : context.getSettings()) {
                settings.put(s.toJSON());
            }
            text.put("settings", settings);
            text.put("skillId", context.getSkillId());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        upload(text.toString());
    }
}
