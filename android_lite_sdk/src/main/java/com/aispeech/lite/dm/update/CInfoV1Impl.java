package com.aispeech.lite.dm.update;


import com.aispeech.common.AuthUtil;
import com.aispeech.common.Log;
import com.aispeech.export.ProductContext;
import com.aispeech.export.SkillContext;
import com.aispeech.export.Vocab;
import com.aispeech.lite.config.CloudDMConfig;
import com.aispeech.net.NetProxy;
import com.aispeech.net.http.HttpCallback;
import com.aispeech.net.http.IHttp;
import com.aispeech.net.http.IResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;

/**
 * cinfo v1 实现类
 * wiki https://wiki.aispeech.com.cn/display/DUISYS/CInfoServer
 *
 * @author hehr
 */
public class CInfoV1Impl implements ICInfo {

    private static final String TAG = CInfoV1Impl.class.getSimpleName();

    private String v1Host;

    private String deviceName;

    private String productId;

    private CInfoListener listener;

    private String aliasKey;

    private String deviceSecret;

    /**
     * 构造函数
     *
     * @param v1Host       cinfo 服务地址
     * @param deviceName   授权后下发的deviceName
     * @param productId    产品id
     * @param deviceSecret 设备授权
     * @param aliasKey     分支信息
     * @param listener     监听器
     */
    public CInfoV1Impl(String v1Host, String deviceName, String deviceSecret, String productId, String aliasKey, CInfoListener listener) {
        this.v1Host = v1Host == null ? CloudDMConfig.CINFO_SERVER : v1Host;
        this.deviceName = deviceName;
        this.deviceSecret = deviceSecret;
        this.productId = productId;
        this.listener = listener;
        this.aliasKey = aliasKey;
    }

    /**
     * 生成url
     *
     * @param vocabName 词库名称
     * @return url
     */
    private String getV1Url(String vocabName) {
        String timestamp = new Date().getTime() + "";
        String nonce = UUID.randomUUID() + "";
        String sig = AuthUtil.getSignature(deviceName + nonce + productId + timestamp, deviceSecret);
        StringBuilder url = new StringBuilder();
        url.append(v1Host)
                .append("/vocabs")
                .append("/" + vocabName)
                .append("?")
                .append("productId=")
                .append(productId)
                .append("&aliasKey=")
                .append(aliasKey)
                .append("&deviceName=")
                .append(deviceName)
                .append("&nonce=")
                .append(nonce)
                .append("&sig=")
                .append(sig)
                .append("&timestamp=")
                .append(timestamp);
        Log.i(TAG, "url :" + url.toString());
        return url.toString();
    }

    /**
     * http 协议上传 cinfo 数据
     *
     * @param name 词库名
     * @param data json
     * @see Vocab
     * @see Vocab#ACTION_INSERT
     * @see Vocab#ACTION_REMOVE
     * @see Vocab#ACTION_CLEAR_AND_INSERT
     * @see Vocab#ACTION_CLEAR_ALL
     */
    private void upload(final String name, String data) {

        NetProxy.getHttp().post(getV1Url(name), data, new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.e(TAG, "vocabs " + name + " upload failed , name : " + name);
                if (listener != null) {
                    listener.onUploadFailed();
                }
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "vocabs " + name + " upload success");
                    try {
                        if (listener != null && new JSONObject(response.string()).optInt("errId") == 0) {
                            listener.onUploadSuccess();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        if (listener != null) {
                            listener.onUploadFailed();
                        }
                    }
                } else {
                    Log.e(TAG, "vocabs " + name + " upload failed , http code : " + response.code());
                    if (listener != null) {
                        listener.onUploadFailed();
                    }
                }
            }
        });
    }

    /**
     * cinfo option
     */
    private enum OPTION {
        /**
         * 新增
         */
        POST("post"),
        /**
         * 删除
         */
        DELETE("delete");

        String value;

        OPTION(String value) {
            this.value = value;
        }
    }

    /**
     * cinfo mode
     */
    private enum MODE {
        /**
         * 默认合并
         */
        MERGE("merge"),
        /**
         * 覆盖
         */
        OVERRIDE("override"),

        ;

        String value;

        MODE(String value) {
            this.value = value;
        }
    }

    /**
     * 生成cinfo联系人报文
     *
     * @param vocab {@link Vocab}
     * @return
     */
    private String getPayload(Vocab vocab) throws JSONException {

        JSONObject text = new JSONObject();

        JSONObject payLoad = new JSONObject();

        payLoad.put("type", "vocab");

        JSONArray data = new JSONArray();

        switch (vocab.getAction()) {
            case Vocab.ACTION_CLEAR_ALL:
                payLoad.put("mode", MODE.OVERRIDE.value);
                break;
            case Vocab.ACTION_CLEAR_AND_INSERT:
                payLoad.put("mode", MODE.OVERRIDE.value);
                payLoad.put("option", OPTION.POST.value);
                for (String s : vocab.getContents()) {
                    data.put(s);
                }
                break;
            case Vocab.ACTION_INSERT:
                payLoad.put("mode", MODE.MERGE.value);
                payLoad.put("option", OPTION.POST.value);
                for (String s : vocab.getContents()) {
                    data.put(s);
                }
                break;
            case Vocab.ACTION_REMOVE:
                payLoad.put("mode", MODE.MERGE.value);
                payLoad.put("option", OPTION.DELETE.value);
                for (String s : vocab.getContents()) {
                    data.put(s);
                }
                break;
            default:
                break;
        }

        payLoad.put("data", data);

        text.put("payload", payLoad);

        Log.d(TAG, "action :" + vocab.getAction() + " , " + "payload:" + text);

        return text.toString();

    }


    @Override
    public void uploadVocabs(Vocab... vocabs) {

        for (Vocab vocab : vocabs) {
            try {
                upload(vocab.getName(), getPayload(vocab));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void uploadProductContext(ProductContext context) {
        Log.e(TAG, "cinfo v1 not implements uploadProductContext");
    }

    @Override
    public void uploadSkillContext(SkillContext context) {
        Log.e(TAG, "cinfo v1 not implements uploadSkillContext");
    }


}
