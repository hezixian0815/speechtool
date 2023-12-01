package com.aispeech.lite.vprint;

import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_ASR_ERR;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_ASR_EXCEPTION;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_ASR_NOT_MATCH;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_CACHE_FAILED;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_CANT_ENHANCE;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_CONTEXT_NOT_MATCHED;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_DELETE_PARAMS_ERR;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_DELETE_USER_FAILED;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_GET_MODLE_FAILED;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_HTTP_PARAMS_ERR;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_HTTP_VIDEO_ERR;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_JSON_ERR;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_MODEL_LOADED_FAILED;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_MODEL_SERVER_FAILED;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_MODE_NOT_SUPPORT;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_MONGO_SAVE_FAILED;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_MUL_START;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_NO_CONTEXT_FROM_KERNEL;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_NO_DELETEDUSER;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_NO_FILE;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_NO_KERNEL_LINKED;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_NO_SESSION;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_NO_TASKS;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_NO_USERS;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_OVER_PLS_COUNT;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_PATH_NOT_SUPPORT;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_PLAY_RECORDED_VIDEO;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_PLS_AUTH_FAILED;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_PRE_REGISTER_ERR;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_QUEST_ERROR;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_QUEST_NOT_SUPPORT;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_REQUEST_TYPE_NOT_SUPPORT;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_RESULT_NOT_MATCHED;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_SPEAKER_BACK_FAILED;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_TASKS_OUT_OF_TIME;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_VERIFY_PARAMS_ERR;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_VIDEO_LEAK;
import static com.aispeech.AIError.ERR_CODE_CLOUD_VPRINT_VIDEO_PARAMS;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_ASR_ERR;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_ASR_EXCEPTION;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_ASR_NOT_MATCH;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_CACHE_FAILED;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_CANT_ENHANCE;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_CONTEXT_NOT_MATCHED;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_DELETE_PARAMS_ERR;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_DELETE_USER_FAILED;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_GET_MODLE_FAILED;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_HTTP_PARAMS_ERR;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_HTTP_VIDEO_ERR;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_JSON_ERR;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_MODEL_LOADED_FAILED;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_MODEL_SERVER_FAILED;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_MODE_NOT_SUPPORT;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_MONGO_SAVE_FAILED;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_MUL_START;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_NO_CONTEXT_FROM_KERNEL;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_NO_DELETEDUSER;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_NO_FILE;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_NO_KERNEL_LINKED;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_NO_SESSION;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_NO_TASKS;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_NO_USERS;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_OVER_PLS_COUNT;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_PATH_NOT_SUPPORT;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_PLAY_RECORDED_VIDEO;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_PLS_AUTH_FAILED;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_PRE_REGISTER_ERR;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_QUEST_ERROR;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_QUEST_NOT_SUPPORT;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_REQUEST_TYPE_NOT_SUPPORT;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_RESULT_NOT_MATCHED;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_SPEAKER_BACK_FAILED;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_TASKS_OUT_OF_TIME;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_VERIFY_PARAMS_ERR;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_VIDEO_LEAK;
import static com.aispeech.AIError.ERR_DESCRIPTION_CLOUD_VPRINT_VIDEO_PARAMS;
import static com.aispeech.AIError.ERR_DESCRIPTION_ERR_NETWORK;
import static com.aispeech.AIError.ERR_NETWORK;

import android.media.AudioFormat;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.auth.AIProfile;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AuthUtil;
import com.aispeech.common.Log;
import com.aispeech.common.WavFileWriter;
import com.aispeech.export.config.AICloudVprintConfig;
import com.aispeech.export.intent.AICloudVprintIntent;
import com.aispeech.export.listeners.AICloudVprintListener;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISampleRate;
import com.aispeech.net.NetProxy;
import com.aispeech.net.http.HttpCallback;
import com.aispeech.net.http.IHttp;
import com.aispeech.net.http.IResponse;
import com.aispeech.net.ws.IWebsocket;
import com.aispeech.net.ws.WebsocketCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CloudVprintKernel implements AICloudVprintListener, WebsocketCallback {

    private static final String TAG = "CloudVprintKernel";
    protected AIProfile profile;
    private String host = "https://vpr.duiopen.com";
    // V2 接口的 res 都使用 AICloudVprintConfig.Mode.getValue()
    private String API3_REGISTER = "/vpr/v3/register?res=%s";
    private String API3_VERIFY = "/vpr/v3/verify?res=%s";
    private String API3_VERIFY_WEB_SOCKET = "/vpr/v3/verify/online?res=%s";
    private String API3_UNREGISTER = "/vpr/v3/unregister?res=%s";
    private String API3_LISTUSERS = "/vpr/v3/listUsers?res=%s";
    private AICloudVprintListener listener;
    /**
     * 模式
     */
    private AICloudVprintConfig.Mode mode = null;
    private int sampleRate = 16000;
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private IWebsocket mWebSocket = null;
    private boolean canceled = false;
    private WavFileWriter wavFileWriter;

    public CloudVprintKernel() {
    }

    /**
     * 初始化
     *
     * @param mode     声纹模式
     * @param host     服务器域名，填空则为默认域名，默认是 https://vpr.duiopen.com
     * @param profile  授权
     * @param listener 回调
     */
    public synchronized void init(AICloudVprintConfig.Mode mode, String host, AIProfile profile, final AICloudVprintListener listener) {
        this.profile = profile;
        this.listener = listener;
        if (!TextUtils.isEmpty(host))
            this.host = host;
        if (mode == null) {
            onInit(AIConstant.OPT_FAILED, "Illegal Argument: mode is null");
            return;
        } else {
            this.mode = mode;
            this.sampleRate = mode.getSupportSampleRate();
        }
        if (profile == null || !profile.isProfileValid(null).isValid()) {
            onInit(AIConstant.OPT_FAILED, "auth fail");
        } else {
            onInit(AIConstant.OPT_SUCCESS, "");
        }
    }

    private String getQueryParameter(AICloudVprintIntent intent) {
        if (profile == null || TextUtils.isEmpty(profile.getProductId())) {
            return "&no_profile_info";
        }
        StringBuilder stringBuilder = new StringBuilder(generateCommonParameter(intent.getAliasKey()));
        stringBuilder.append("&apikey=" + profile.getApiKey());
        if (!TextUtils.isEmpty(host) && host.contains("vpr.t.duiopen.com")) {
            stringBuilder.append("&debug=0");
        }
//        stringBuilder.append("&res=" + intent.getMode().getValue());
        String requestId = intent.getRequestId();
        if (TextUtils.isEmpty(requestId)) {
            requestId = Utils.getRecorderId();
        }
        stringBuilder.append("&requestId=" + requestId);
        return stringBuilder.toString();
    }

    private String generateCommonParameter(String aliasKey) {
        if (profile == null || TextUtils.isEmpty(profile.getProductId())) {
            return "&no_profile_info";
        }
        // 设备对云方式
        String productId = profile.getProductId();
        String deviceName = profile.getDeviceName();
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sig = AuthUtil.getSignature(deviceName + nonce + productId + timestamp,
                profile.getDeviceSecret());
        String aliasKeyParams = TextUtils.isEmpty(aliasKey) ? "" : "&aliasKey=" + aliasKey;
        return String.format("&productId=%s&deviceName=%s&nonce=%s&timestamp=%s&sig=%s%s", productId, deviceName, nonce, timestamp, sig, aliasKeyParams);

        // 云对云方式
        // return String.format("&productId=%s&apikey=%s", profile.getProductId(), profile.getApiKey());

        // 云对云方式 测试环境用的产品
        // return "&productId=278582465&apikey=6cf44bcec9464daeb70f06cdadcf0d8b";
    }

    public void setVprintAudioPath(String path) {
        final File wavFile = new File(path);
        wavFileWriter = WavFileWriter.createWavFileWriter(wavFile,
                AISampleRate.toAISampleRate(sampleRate), AudioFormat.CHANNEL_OUT_DEFAULT,
                AudioFormat.ENCODING_PCM_16BIT);
    }

    public void stopFeedData() {
        if (null != wavFileWriter)
            wavFileWriter.close();
    }

    public void startFeedData(byte[] data, int size) {
        final byte[] buffer = new byte[size];
        System.arraycopy(data, 0, buffer, 0, size);
        if (null != wavFileWriter)
            wavFileWriter.write(buffer);
    }

    /**
     * 1. 注册声纹
     *
     * @param intent 声纹注册参数
     */
    public void register(AICloudVprintIntent intent) {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONObject audio = new JSONObject();
            if (intent.getAudio() != null) {
                audio = intent.getAudio().toJson();
            } else {
                audio.put("audioType", "wav");
                audio.put("sampleRate", sampleRate);
                audio.put("channel", 1);
                audio.put("sampleBytes", 2);
            }

            JSONObject env = new JSONObject();
            env.put("customContent", intent.getCustomContent());
            env.put("asrErrorRate", intent.getAsrErrorRate());
            env.put("enhanceRegister", intent.isEnhanceRegister());
            float minSpeechLength = intent.getMinSpeechLength();
            if (minSpeechLength > 0.000001f) {
                env.put("minSpeechLength", intent.getMinSpeechLength());
            }
            env.put("enableVad", intent.isEnableVad());

            String requestId = intent.getRequestId();
            jsonObject.put("requestId", TextUtils.isEmpty(requestId) ? null : requestId);
            String userId = intent.getUserId();
            if (!TextUtils.isEmpty(userId)) {
                jsonObject.put("userId", userId);
            }
            String organization = intent.getOrganization();
            if (!TextUtils.isEmpty(organization)) {
                jsonObject.put("organization", organization);
            }
            String groupId = intent.getGroupId();
            if (!TextUtils.isEmpty(groupId)) {
                jsonObject.put("groupId", groupId);
            }
            jsonObject.put("app", intent.getApp());
            jsonObject.put("domain", intent.getDomain());
            jsonObject.put("skip_saving", intent.isSkip_saving());
            jsonObject.put("audio", audio);
            jsonObject.put("env", env);
        } catch (JSONException e) {
            Log.d(TAG, "register:json error " + e.toString());
            onError(new AIError(ERR_CODE_CLOUD_VPRINT_JSON_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_JSON_ERR));
            return;
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("params", jsonObject.toString());
        Log.d(TAG, "params " + jsonObject.toString());
        NetProxy.getHttp().post(String.format(host + API3_REGISTER + generateCommonParameter(intent.getAliasKey()), mode.getValue()), map, intent.getWavFilepath(), new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.v(TAG, "onFailure: " + e.toString());
                onError(new AIError(ERR_NETWORK, ERR_DESCRIPTION_ERR_NETWORK));
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse iResponse) throws IOException {
                String response = iResponse.string();
                Log.d(TAG, "register onResponse: " + response);
                if (iResponse.isSuccessful()) {
                    try {
                        JSONObject jj = new JSONObject(response);
                        int state = jj.getInt("errno");
                        AIError aiError = parseErrorCode(state);
                        if (aiError != null) {
                            onError(aiError);
                        } else {
                            JSONObject data = jj.optJSONObject("data");
                            onRegister(state, data != null ? data.toString() : "");
                        }
                    } catch (Exception e) {
                        Log.v(TAG, "onResponse: " + e.toString());
                        onError(checkNetError(iResponse.code()));
                    }
                } else {
                    onError(checkNetError(iResponse.code()));
                }
            }
        });
    }

    /**
     * 4. 验证声纹
     *
     * @param intent 配置参数
     */
    public void verifyHttp(AICloudVprintIntent intent) {
        if (!mode.isHttpVerify()) {
            onError(new AIError(ERR_CODE_CLOUD_VPRINT_MODE_NOT_SUPPORT, ERR_DESCRIPTION_CLOUD_VPRINT_MODE_NOT_SUPPORT));
            return;
        }
        if (TextUtils.isEmpty(intent.getWavFilepath())) {
            onError(new AIError(ERR_CODE_CLOUD_VPRINT_NO_FILE, ERR_DESCRIPTION_CLOUD_VPRINT_NO_FILE));
            Log.e(TAG, "verifyHttp: wavfilepath is empty");
            return;
        }
        JSONObject jsonObject = new JSONObject();
        try {
            JSONObject audio = new JSONObject();
            audio.put("audioType", "wav");
            audio.put("sampleRate", sampleRate);
            audio.put("channel", 1);
            audio.put("sampleBytes", 2);

            JSONObject env = new JSONObject();
            env.put("asrErrorRate", intent.getAsrErrorRate());
            float minSpeechLength = intent.getMinSpeechLength();
            if (minSpeechLength > 0.000001f) {
                env.put("minSpeechLength", intent.getMinSpeechLength());
            }
            env.put("customContent", intent.getCustomContent());
            env.put("enableVad", intent.isEnableVad());
            env.put("topN", intent.getTopN());
            env.put("enableSpeakerAntiSpoofing", intent.isEnableSpeakerAntiSpoofing());

            String requestId = intent.getRequestId();
            jsonObject.put("requestId", TextUtils.isEmpty(requestId) ? null : requestId);
            JSONArray users = new JSONArray();
            List<String> userlist = intent.getUsers();
            if (userlist != null && !userlist.isEmpty()) {
                for (String userId : userlist) {
                    users.put(userId);
                }
                jsonObject.put("users", users);
            }
            String groupId = intent.getGroupId();
            if (!TextUtils.isEmpty(groupId)) {
                jsonObject.put("groupId", groupId);
            }
            String organization = intent.getOrganization();
            if (!TextUtils.isEmpty(organization)) {
                jsonObject.put("organization", organization);
            }
            jsonObject.put("skip_saving", intent.isSkip_saving());
            jsonObject.put("audio", audio);
            jsonObject.put("env", env);
        } catch (JSONException e) {
            Log.d(TAG, "verifyHttp: " + e.toString());
            onError(new AIError(ERR_CODE_CLOUD_VPRINT_JSON_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_JSON_ERR));
            return;
        }
        HashMap<String, String> map = new HashMap<>();
        map.put("params", jsonObject.toString());
        Log.d(TAG, "verifyHttp:params " + jsonObject.toString());
        Log.d(TAG, "mode " + mode.getValue());
        Log.d(TAG, "verifyHttp:host " + String.format(host + API3_VERIFY + generateCommonParameter(intent.getAliasKey()), mode.getValue()));
        NetProxy.getHttp().post(String.format(host + API3_VERIFY + generateCommonParameter(intent.getAliasKey()), mode.getValue()), map, intent.getWavFilepath(), new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.v(TAG, "onFailure: " + e.toString());
                onError(new AIError(ERR_NETWORK, ERR_DESCRIPTION_ERR_NETWORK));
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse iResponse) throws IOException {
                String response = iResponse.string();
                Log.d(TAG, "verify onResponse: " + response);
                if (iResponse.isSuccessful()) {
                    try {
                        JSONObject jj = new JSONObject(response);
                        int state = jj.getInt("errno");
                        AIError aiError = parseErrorCode(state);
                        if (aiError != null) {
                            onError(aiError);
                        } else {
                            JSONObject data = jj.optJSONObject("data");
                            onVerifyHttp(state, data != null ? data.toString() : "");
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "onResponse: " + e.toString());
                        onError(new AIError(ERR_CODE_CLOUD_VPRINT_JSON_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_JSON_ERR));
                    }
                } else {
                    onError(checkNetError(iResponse.code()));
                }
            }
        });
    }

    private String getParams(AICloudVprintIntent intent) {
        JSONObject jsonObject = new JSONObject();
        try {
            JSONObject audio = new JSONObject();
            audio.put("audioType", "wav");
            audio.put("sampleRate", sampleRate);
            audio.put("channel", 1);
            audio.put("sampleBytes", 2);

            JSONArray users = new JSONArray();
            JSONObject asrPlus = new JSONObject();
            List<String> usersGroup = intent.getUsers();
            if (usersGroup != null && !usersGroup.isEmpty()) {
                for (String userId : usersGroup) {
                    users.put(userId);
                }
                asrPlus.put("users", users);
            }

            String organization = intent.getOrganization();
            if (!TextUtils.isEmpty(organization)) {
                asrPlus.put("organization", organization);
            }
            asrPlus.put("domain", intent.getDomain());
            asrPlus.put("contextId", intent.getContextId());
            String groupId = intent.getGroupId();
            if (!TextUtils.isEmpty(groupId)) {
                asrPlus.put("groupId", groupId);
            }

            JSONObject env = new JSONObject();
            env.put("asrErrorRate", intent.getAsrErrorRate());
            env.put("customContent", intent.getCustomContent());
            env.put("enhanceRegister", intent.isEnhanceRegister());
            float minSpeechLength = intent.getMinSpeechLength();
            if (minSpeechLength > 0.000001f) {
                env.put("minSpeechLength", intent.getMinSpeechLength());
            }
            env.put("enableVad", intent.isEnableVad());
            env.put("enableSpeakerAntiSpoofing", intent.isEnableSpeakerAntiSpoofing());

            String requestId = intent.getRequestId();
            if (TextUtils.isEmpty(requestId)) {
                requestId = null;
            }
            jsonObject.put("requestId", requestId);
            jsonObject.put("asrPlus", asrPlus);
            jsonObject.put("audio", audio);
            jsonObject.put("env", env);
            jsonObject.put("skip_saving", intent.isSkip_saving());
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i(TAG, "getParams: " + jsonObject.toString());
        return jsonObject.toString();
    }

    public synchronized void verifyStartWS(AICloudVprintIntent intent) {
        if (!mode.isWebSocketVerify()) {
            onError(new AIError(ERR_CODE_CLOUD_VPRINT_MODE_NOT_SUPPORT, ERR_DESCRIPTION_CLOUD_VPRINT_MODE_NOT_SUPPORT));
            return;
        }
        if (mWebSocket != null) {
            mWebSocket.close(1000, "Goodbye!");
            mWebSocket = null;
        }
        /**
         * 服务端最新验证，user非必传字段
         */
//        if (intent.getUsers() == null || intent.getUsers().isEmpty()) {
//            Log.e(TAG, "verifyStartWS: users is null");
//            onError(new AIError(ERR_CODE_CLOUD_VPRINT_NO_USERS, ERR_DESCRIPTION_CLOUD_VPRINT_NO_USERS));
//            return;
//        }
        canceled = false;
        String url = String.format(host + API3_VERIFY_WEB_SOCKET + generateCommonParameter(intent.getAliasKey()), mode.getValue());
        mWebSocket = NetProxy.newWebsocket(url,
                this);
        mWebSocket.send(getParams(intent));
        Log.d(TAG, "verifyStartWS url :" + url);
    }

    public synchronized void verifyFeedWS(byte[] data, int size) {
        if (data == null || size < 0 || data.length < size) {
            onError(new AIError(ERR_CODE_CLOUD_VPRINT_NO_FILE, ERR_DESCRIPTION_CLOUD_VPRINT_NO_FILE));
            Log.d(TAG, "verifyFeedWS data is null or size not matched");
            return;
        }

        if (mWebSocket != null) {
            byte[] data2 = new byte[size];
            System.arraycopy(data, 0, data2, 0, size);
            boolean ret = mWebSocket.send(data2);
            Log.d(TAG, "verifyFeedWS " + ret + " length " + data2.length);
        } else
            Log.d(TAG, "verifyFeedWS mWebSocket is null");
    }

    public synchronized void verifyStopWS() {
        canceled = true;
        if (mWebSocket != null) {
            boolean ret = mWebSocket.send(new byte[0]);
            mWebSocket = null;
            Log.d(TAG, "verifyStopWS " + ret);
        } else
            Log.d(TAG, "verifyStopWS mWebSocket is null");
    }

    public synchronized void query(AICloudVprintIntent intent) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("userId", intent.getUserId());
            jsonObject.put("organization", intent.getOrganization());
            jsonObject.put("groupId", intent.getGroupId());
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (intent.getMode() == null) {
            Log.d(TAG, "query data res is null");
            return;
        }
        String urlQuery = getQueryParameter(intent);
        String url = String.format(host + API3_LISTUSERS, intent.getMode().getValue()) + urlQuery;

        Log.d(TAG, "query : " + url + "\nbody:" + jsonObject);

        NetProxy.getHttp().post(url, jsonObject.toString(), new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.v(TAG, "onFailure: " + e.toString());
                onError(new AIError(ERR_NETWORK, ERR_DESCRIPTION_ERR_NETWORK));
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse iResponse) throws IOException {
                String response = iResponse.string();
                Log.d(TAG, "verify onResponse: " + response);
                if (iResponse.isSuccessful()) {
                    try {
                        JSONObject jj = new JSONObject(response);
                        int state = jj.getInt("errno");
                        AIError aiError = parseErrorCode(state);
                        if (aiError != null) {
                            onError(aiError);
                        } else {
                            JSONObject data = jj.optJSONObject("data");
                            onQueryResult(data != null ? data.toString() : "");
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "onResponse: " + e.toString());
                        onError(new AIError(ERR_CODE_CLOUD_VPRINT_JSON_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_JSON_ERR));
                    }
                } else {
                    onError(checkNetError(iResponse.code()));
                }
            }
        });
    }

    public synchronized void verifyCancelWS() {
        canceled = true;
        if (mWebSocket != null) {
            mWebSocket.cancel();
            Log.d(TAG, "verifyCancelWS");
        } else
            Log.d(TAG, "verifyCancelWS mWebSocket is null");
        mWebSocket = null;
    }

    private synchronized void resetWebSocket(IWebsocket websocket) {
        if (websocket == mWebSocket) {
            mWebSocket = null;
            Log.d(TAG, "mWebSocket set null");
        }
    }

    // WebSocket ==>
    @Override
    public void onOpen(IWebsocket iWebsocket) {
        Log.d(TAG, "verify onOpen");
    }

    @Override
    public synchronized void onMessage(IWebsocket iWebsocket, String response) {
        Log.d(TAG, "verify onMessage: " + response);
        try {
            JSONObject jj = new JSONObject(response);
            int state = jj.getInt("errno");
            AIError aiError = parseErrorCode(state);
            if (aiError != null) {
                onError(aiError);
            } else {
                onVerifyWS(response);
            }
        } catch (Exception e) {
            Log.v(TAG, "onResponse: " + e.toString());
            onVerifyWS(response);
        }
    }

    @Override
    public void onClosing(IWebsocket iWebsocket, int code, String reason) {
        Log.d(TAG, "verify onClosing: code " + code + " reason " + reason);
    }

    @Override
    public synchronized void onClosed(IWebsocket iWebsocket, int code, String reason) {
        Log.d(TAG, "verify onClosed: code " + code + " reason " + reason);
        if (!canceled && code != 1000 && iWebsocket == mWebSocket) {
            onError(checkNetError(code));
        }
        resetWebSocket(iWebsocket);
    }

    @Override
    public synchronized void onFailure(IWebsocket iWebsocket, Throwable throwable) {
        Log.d(TAG, "verify onClosed: throwable " + throwable);
        if (!canceled && iWebsocket == mWebSocket)
            onError(new AIError(ERR_NETWORK, ERR_DESCRIPTION_ERR_NETWORK));
        resetWebSocket(iWebsocket);
    }
    // WebSocket <==

    /**
     * 5. 注销声纹
     *
     * @param intent 注销声纹参数
     */
    public void unregister(AICloudVprintIntent intent) {
        JSONObject jsonObject = new JSONObject();
        try {
            String requestId = intent.getRequestId();
            jsonObject.put("requestId", TextUtils.isEmpty(requestId) ? null : requestId);
            jsonObject.put("userId", intent.getUserId());
            String groupId = intent.getGroupId();
            if (!TextUtils.isEmpty(groupId)) {
                jsonObject.put("groupId", groupId);
            }
            String organization = intent.getOrganization();
            if (!TextUtils.isEmpty(organization)) {
                jsonObject.put("organization", organization);
            }
        } catch (JSONException e) {
            Log.d(TAG, "unregister:json error " + e.toString());
            onError(new AIError(ERR_CODE_CLOUD_VPRINT_JSON_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_JSON_ERR));
        }

        NetProxy.getHttp().post(String.format(host + API3_UNREGISTER + generateCommonParameter(intent.getAliasKey()), mode.getValue()), jsonObject.toString(), new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.v(TAG, "onFailure: " + e.toString());
                onError(new AIError(ERR_NETWORK, ERR_DESCRIPTION_ERR_NETWORK));
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse iResponse) throws IOException {
                String response = iResponse.string();
                Log.d(TAG, "unregister onResponse: " + response);
                if (iResponse.isSuccessful()) {
                    try {
                        JSONObject jj = new JSONObject(response);
                        int state = jj.getInt("errno");
                        AIError aiError = parseErrorCode(state);
                        if (aiError != null) {
                            onError(aiError);
                        } else {
                            JSONObject data = jj.optJSONObject("data");
                            onUnregister(state, data != null ? data.toString() : "");
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "onResponse: " + e.toString());
                        onError(new AIError(ERR_CODE_CLOUD_VPRINT_JSON_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_JSON_ERR));
                    }
                } else {
                    onError(checkNetError(iResponse.code()));
                }
            }
        });
    }


    public void destroy() {
        listener = null;
    }

    @Override
    public void onInit(final int status, final String errMsg) {
        if (listener != null)
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onInit(status, errMsg);
                }
            });
    }

    @Override
    public void onRegister(final int state, final String json) {
        if (listener != null)
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onRegister(state, json);
                }
            });
    }

    @Override
    public void onQueryResult(final String result) {
        if (listener != null)
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onQueryResult(result);
                }
            });
    }

    @Override
    public void onVerifyHttp(final int state, final String json) {
        if (listener != null)
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onVerifyHttp(state, json);
                }
            });
    }

    @Override
    public void onVerifyWS(final String message) {
        if (listener != null)
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onVerifyWS(message);
                }
            });
    }

    @Override
    public void onUnregister(final int state, final String json) {
        if (listener != null)
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onUnregister(state, json);
                }
            });
    }

    @Override
    public void onError(final AIError aiError) {
        if (listener != null)
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onError(aiError);
                }
            });
    }

    private AIError checkNetError(int errorID) {
        AIError aiError = parseErrorCode(errorID);
        if (aiError == null) {
            aiError = new AIError(ERR_NETWORK, ERR_DESCRIPTION_ERR_NETWORK);
        }
        return aiError;
    }

    private AIError parseErrorCode(int errorId) {
        switch (errorId) {
            case 1:
                return new AIError(ERR_CODE_CLOUD_VPRINT_VIDEO_LEAK, ERR_DESCRIPTION_CLOUD_VPRINT_VIDEO_LEAK);
            case 3:
                return new AIError(ERR_CODE_CLOUD_VPRINT_RESULT_NOT_MATCHED, ERR_DESCRIPTION_CLOUD_VPRINT_RESULT_NOT_MATCHED);
            case 10:
                return new AIError(ERR_CODE_CLOUD_VPRINT_CONTEXT_NOT_MATCHED, ERR_DESCRIPTION_CLOUD_VPRINT_CONTEXT_NOT_MATCHED);
            case 11:
                return new AIError(ERR_CODE_CLOUD_VPRINT_MODEL_SERVER_FAILED, ERR_DESCRIPTION_CLOUD_VPRINT_MODEL_SERVER_FAILED);
            case 12:
                return new AIError(ERR_CODE_CLOUD_VPRINT_PLAY_RECORDED_VIDEO, ERR_DESCRIPTION_CLOUD_VPRINT_PLAY_RECORDED_VIDEO);
            case 15:
                return new AIError(ERR_CODE_CLOUD_VPRINT_MODEL_LOADED_FAILED, ERR_DESCRIPTION_CLOUD_VPRINT_MODEL_LOADED_FAILED);
            case 16:
                return new AIError(ERR_CODE_CLOUD_VPRINT_CANT_ENHANCE, ERR_DESCRIPTION_CLOUD_VPRINT_CANT_ENHANCE);
            case 17:
                return new AIError(ERR_CODE_CLOUD_VPRINT_CACHE_FAILED, ERR_DESCRIPTION_CLOUD_VPRINT_CACHE_FAILED);
            case 200:
                return new AIError(ERR_CODE_CLOUD_VPRINT_QUEST_ERROR, ERR_DESCRIPTION_CLOUD_VPRINT_QUEST_ERROR);
            case 201:
                return new AIError(ERR_CODE_CLOUD_VPRINT_QUEST_NOT_SUPPORT, ERR_DESCRIPTION_CLOUD_VPRINT_QUEST_NOT_SUPPORT);
            case 202:
                return new AIError(ERR_CODE_CLOUD_VPRINT_VIDEO_PARAMS, ERR_DESCRIPTION_CLOUD_VPRINT_VIDEO_PARAMS);
            case 203:
                return new AIError(ERR_CODE_CLOUD_VPRINT_NO_USERS, ERR_DESCRIPTION_CLOUD_VPRINT_NO_USERS);
            case 204:
                return new AIError(ERR_CODE_CLOUD_VPRINT_NO_TASKS, ERR_DESCRIPTION_CLOUD_VPRINT_NO_TASKS);
            case 205:
                return new AIError(ERR_CODE_CLOUD_VPRINT_TASKS_OUT_OF_TIME, ERR_DESCRIPTION_CLOUD_VPRINT_TASKS_OUT_OF_TIME);
            case 305:
                return new AIError(ERR_CODE_CLOUD_VPRINT_MONGO_SAVE_FAILED, ERR_DESCRIPTION_CLOUD_VPRINT_MONGO_SAVE_FAILED);
            case 306:
                return new AIError(ERR_CODE_CLOUD_VPRINT_GET_MODLE_FAILED, ERR_DESCRIPTION_CLOUD_VPRINT_GET_MODLE_FAILED);
            case 308:
                return new AIError(ERR_CODE_CLOUD_VPRINT_DELETE_USER_FAILED, ERR_DESCRIPTION_CLOUD_VPRINT_DELETE_USER_FAILED);
            case 309:
                return new AIError(ERR_CODE_CLOUD_VPRINT_NO_DELETEDUSER, ERR_DESCRIPTION_CLOUD_VPRINT_NO_DELETEDUSER);
            case 310:
                return new AIError(ERR_CODE_CLOUD_VPRINT_DELETE_PARAMS_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_DELETE_PARAMS_ERR);
            case 311:
                return new AIError(ERR_CODE_CLOUD_VPRINT_NO_SESSION, ERR_DESCRIPTION_CLOUD_VPRINT_NO_SESSION);
            case 312:
                return new AIError(ERR_CODE_CLOUD_VPRINT_REQUEST_TYPE_NOT_SUPPORT, ERR_DESCRIPTION_CLOUD_VPRINT_REQUEST_TYPE_NOT_SUPPORT);
            case 313:
                return new AIError(ERR_CODE_CLOUD_VPRINT_PRE_REGISTER_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_PRE_REGISTER_ERR);
            case 314:
                return new AIError(ERR_CODE_CLOUD_VPRINT_NO_KERNEL_LINKED, ERR_DESCRIPTION_CLOUD_VPRINT_NO_KERNEL_LINKED);
            case 315:
                return new AIError(ERR_CODE_CLOUD_VPRINT_NO_CONTEXT_FROM_KERNEL, ERR_DESCRIPTION_CLOUD_VPRINT_NO_CONTEXT_FROM_KERNEL);
            case 316:
                return new AIError(ERR_CODE_CLOUD_VPRINT_ASR_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_ASR_ERR);
            case 317:
                return new AIError(ERR_CODE_CLOUD_VPRINT_ASR_EXCEPTION, ERR_DESCRIPTION_CLOUD_VPRINT_ASR_EXCEPTION);
            case 318:
                return new AIError(ERR_CODE_CLOUD_VPRINT_ASR_NOT_MATCH, ERR_DESCRIPTION_CLOUD_VPRINT_ASR_NOT_MATCH);
            case 319:
                return new AIError(ERR_CODE_CLOUD_VPRINT_MUL_START, ERR_DESCRIPTION_CLOUD_VPRINT_MUL_START);
            case 400:
                return new AIError(ERR_CODE_CLOUD_VPRINT_HTTP_VIDEO_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_HTTP_VIDEO_ERR);
            case 401:
                return new AIError(ERR_CODE_CLOUD_VPRINT_HTTP_PARAMS_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_HTTP_PARAMS_ERR);
            case 402:
                return new AIError(ERR_CODE_CLOUD_VPRINT_VERIFY_PARAMS_ERR, ERR_DESCRIPTION_CLOUD_VPRINT_VERIFY_PARAMS_ERR);
            case 403:
                return new AIError(ERR_CODE_CLOUD_VPRINT_PATH_NOT_SUPPORT, ERR_DESCRIPTION_CLOUD_VPRINT_PATH_NOT_SUPPORT);
            case 410:
                return new AIError(ERR_CODE_CLOUD_VPRINT_PLS_AUTH_FAILED, ERR_DESCRIPTION_CLOUD_VPRINT_PLS_AUTH_FAILED);
            case 411:
                return new AIError(ERR_CODE_CLOUD_VPRINT_OVER_PLS_COUNT, ERR_DESCRIPTION_CLOUD_VPRINT_OVER_PLS_COUNT);
            case 501:
                return new AIError(ERR_CODE_CLOUD_VPRINT_SPEAKER_BACK_FAILED, ERR_DESCRIPTION_CLOUD_VPRINT_SPEAKER_BACK_FAILED);
        }
        return null;
    }
}
