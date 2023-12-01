package com.aispeech.lite.voicecopy;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.export.config.AICloudVoiceCopyConfig;
import com.aispeech.export.listeners.AICloudVoiceCopyListener;
import com.aispeech.net.NetProxy;
import com.aispeech.net.http.HttpCallback;
import com.aispeech.net.http.HttpImpl;
import com.aispeech.net.http.IHttp;
import com.aispeech.net.http.IResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import okhttp3.Request;

/**
 * 云端声音复刻内核
 */
public class CloudVoiceCopyKernel {

    private static final String TAG = "CloudVoiceCopyKernel";

    /**
     * 获取复刻录音文本
     */
    private static final String URL_GET_TEXT = "/api/v1/voicecopy/app/audio/text?";

    /**
     * 上传音频（音频检测）
     */
    private static final String URL_UPLOAD_VOICE = "/api/v1/voicecopy/app/audio/testing/upload?";

    /**
     * 提交训练
     */
    private static final String URL_SUBMIT_TRAINING = "/api/v1/voicecopy/app/task/training?";

    /**
     * 查询复刻的状态
     */
    private static final String URL_QUERY_STATE = "/api/v1/voicecopy/app/task/query?";

    /**
     * 删除音色资源
     */
    private static final String URL_DELETE_VOICE = "/api/v1/voicecopy/app/delete/timbre?";

    /**
     * 更新自定义资源名称
     */
    private static final String URL_UPDATE_CUSTOM = "/api/v1/voicecopy/app/task/update/custominfo?";


    /**
     * 默认的域名地址
     */
    private static final String DEFAULT_HOST = "https://tts.duiopen.com";

    public static class AIConstant {
        /**
         * 表示操作成功
         */
        public static final int OPT_SUCCESS = 0;
        /**
         * 表示操作失败
         */
        public static final int OPT_FAILED = -1;
    }


    private String productId;
    private String apiKey;
    private String host;
    private AICloudVoiceCopyListener voiceCopyListener;

    private Map<String, String> headersMap;

    public CloudVoiceCopyKernel() {
        headersMap = new HashMap<>();
    }

    /**
     * 初始化声音复刻内核
     *
     * @param config   复刻配置信息
     * @param listener 声音复刻监听接口
     */
    public void init(AICloudVoiceCopyConfig config
            , AICloudVoiceCopyListener listener) {

        this.voiceCopyListener = listener;
        this.productId = config.getProductId();
        this.apiKey = config.getApiKey();

        String host = config.getHost();
        String token = config.getToken();
        String rememberToken = config.getRememberToken();

        if (!TextUtils.isEmpty(host)) {
            this.host = host;
        } else {
            this.host = DEFAULT_HOST;
        }


        if (TextUtils.isEmpty(apiKey) && TextUtils.isEmpty(productId)) {
            onInit(AIConstant.OPT_FAILED, "apiKey or productid error,init failed!");
        } else {
            onInit(AIConstant.OPT_SUCCESS, "success");
        }

        if (!TextUtils.isEmpty(token)) {
            headersMap.put("authorization", token);
        }
        if (!TextUtils.isEmpty(rememberToken)) {
            headersMap.put("rmem-auth", rememberToken);
        }
    }

    /**
     * 获取复刻对应的录音文本
     */
    public void getText() {

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("_productId", productId);
        params.put("_apikey", apiKey);

        String queryUrl = host + URL_GET_TEXT + "productId=%s&apikey=%s";
        String url = String.format(queryUrl, productId, apiKey);

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.headers(HttpImpl.setHeaders(headersMap));
        NetProxy.getHttp().post(url, requestBuilder, params, "", new HttpCallback() {

            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.i(TAG, "onFailure() e = " + e.toString());
                onGetText(-1, "", e);
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse response) {
                if (response != null) {
                    int code = response.code();
                    Log.i(TAG, "onResponse()  code = " + code + "，response = "
                            + response.toString());
                    if (code == 200) {
                        onGetText(0, response.string(), null);
                    } else {
                        onGetText(-1, response.string(), null);
                    }
                }
            }
        });
    }


    /**
     * 上传音频（音频检测）
     *
     * @param textId   文本ID，即录音对应的文本编号，必填。如："fda77fe181ce4072bc2e75c9751f71db-003":
     *                 "但愿花常好，月常圆！"；textId = "fda77fe181ce4072bc2e75c9751f71db-003"。
     *                 注意：复刻所需的录音文本由思必驰提供，不同的客户对接可能有差异。
     * @param gender   MALE / FEMALE ， 必填
     * @param age      年龄段 (成人|儿童)  ADULT / CHILD， 非必填
     * @param filePath 音频文件路径
     */
    public void upload(String textId, String gender, String age, String filePath) {

        String uploadUrl = host + URL_UPLOAD_VOICE + "productId=%s&apikey=%s";
        String url = String.format(uploadUrl, productId, apiKey);

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("textId", textId);
        params.put("gender", gender);
        params.put("age", age);

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.headers(HttpImpl.setHeaders(headersMap));
        NetProxy.getHttp().post(url, requestBuilder, params, filePath, new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.e(TAG, "onFailure() e = " + e.toString());
                onUpload(-1, "", e);
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse response) {
                if (response != null) {
                    int code = response.code();
                    Log.i(TAG, "onResponse()  code = " + code + "，response = "
                            + response.toString());
                    if (code == 200) {
                        onUpload(0, response.string(), null);
                    } else {
                        onUpload(code, response.string(), null);
                    }
                }
            }
        });
    }


    /**
     * 提交训练
     *
     * @param gender       自定义声音录音性别（MALE 和 FEMALE），必填
     * @param age          年龄段 (成人|儿童)  ADULT / CHILD
     * @param customInfo   自定义音色资源名称
     * @param audio_list   为上传音频返回的 "audio_reserve_id" 集合
     * @param pre_tts_text 训练后,试听音频文本
     */
    public void training(String gender, String age, String customInfo
            , ArrayList<String> audio_list, ArrayList<String> pre_tts_text) {

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("gender", gender);
            jsonObj.put("age", age);
            jsonObj.put("customInfo", customInfo);
            JSONArray audioArray = new JSONArray();
            JSONObject audioObj;
            for (String audio : audio_list) {
                audioObj = new JSONObject();
                audioObj.put("audio_reserve_id", audio);
                audioArray.put(audioObj);
            }
            JSONArray preTTSArray = new JSONArray();
            JSONObject preTTSObj;
            for (String preTTS : pre_tts_text) {
                preTTSObj = new JSONObject();
                preTTSObj.put("text", preTTS);
                preTTSArray.put(preTTSObj);
            }
            jsonObj.put("audio_list", audioArray);
            jsonObj.put("pre_tts_text", preTTSArray);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        String req_params = jsonObj.toString();

        String trainingUrl = host + URL_SUBMIT_TRAINING + "productId=%s&apikey=%s";
        String url = String.format(trainingUrl, productId, apiKey);

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.headers(HttpImpl.setHeaders(headersMap));

        NetProxy.getHttp().post(url, requestBuilder, 0, req_params, new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.i(TAG, "onFailure() e = " + e.toString());
                onTraining(-1, "", e);
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse response) {
                if (response != null) {
                    int code = response.code();
                    Log.i(TAG, "onResponse()  code = " + code + "，response = "
                            + response.toString());
                    if (code == 200) {
                        onTraining(0, response.string(), null);
                    } else {
                        onTraining(-1, response.string(), null);
                    }
                }
            }
        });
    }


    /**
     * 查询任务状态，查询全部
     *
     * @param taskId 复刻任务ID
     */
    public void query(String taskId) {

        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("_productId", productId);
        params.put("_apikey", apiKey);
        params.put("taskId", taskId);

        String queryUrl = host + URL_QUERY_STATE + "productId=%s&apikey=%s";
        String url = String.format(queryUrl, productId, apiKey);

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.headers(HttpImpl.setHeaders(headersMap));
        NetProxy.getHttp().post(url, requestBuilder, params, "", new HttpCallback() {

            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.i(TAG, "onFailure() e = " + e.toString());
                onQuery(-1, "", e);
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse response) {
                if (response != null) {
                    int code = response.code();
                    Log.i(TAG, "onResponse()  code = " + code + "，response = "
                            + response.toString());
                    if (code == 200) {
                        onQuery(0, response.string(), null);
                    } else {
                        onQuery(-1, response.string(), null);
                    }
                }
            }
        });
    }

    /**
     * 删除音色
     *
     * @param taskId 任务ID
     */
    public void delete(String taskId) {

        JSONObject deleteObj = new JSONObject();
        try {
            deleteObj.put("taskId", taskId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String req_params = deleteObj.toString();
        String deleteUrl = host + URL_DELETE_VOICE + "productId=%s&apikey=%s";
        String url = String.format(deleteUrl, productId, apiKey);

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.headers(HttpImpl.setHeaders(headersMap));
        NetProxy.getHttp().post(url, requestBuilder, 0, req_params, new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.i(TAG, "onFailure() e = " + e.toString());
                onDelete(-1, "", e);
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse response) {
                if (response != null) {
                    int code = response.code();
                    Log.i(TAG, "onResponse()  code = " + code + "，response = "
                            + response.toString());
                    if (code == 200) {
                        onDelete(0, response.string(), null);
                    } else {
                        onDelete(-1, response.string(), null);
                    }
                }
            }
        });
    }

    /**
     * 更新任务相关的自定义信息
     *
     * @param taskId     任务ID
     * @param customInfo 自定义信息
     */
    public void customize(String taskId, String customInfo) {
        JSONObject customObj = new JSONObject();
        try {
            //customObj.put("taskId", taskId);
            customObj.put("customInfo", customInfo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String req_params = customObj.toString();
        String customUrl = host + URL_UPDATE_CUSTOM + "productId=%s&apikey=%s&taskId=%s";
        String url = String.format(customUrl, productId, apiKey, taskId);

        Request.Builder requestBuilder = new Request.Builder();
        requestBuilder.headers(HttpImpl.setHeaders(headersMap));
        NetProxy.getHttp().post(url, requestBuilder, 0, req_params, new HttpCallback() {
            @Override
            public void onFailure(IHttp iHttp, IOException e) {
                Log.i(TAG, "onFailure() e = " + e.toString());
                onUpdateCustomInfo(-1, "", e);
            }

            @Override
            public void onResponse(IHttp iHttp, IResponse response) {
                if (response != null) {
                    int code = response.code();
                    Log.i(TAG, "onResponse()  code = " + code + "，response = "
                            + response.toString());
                    if (code == 200) {
                        onUpdateCustomInfo(0, response.string(), null);
                    } else {
                        onUpdateCustomInfo(-1, response.string(), null);
                    }
                }
            }
        });
    }


    /**
     * 处理初始化声音复刻
     *
     * @param status 0 表示成功，1 表示失败
     * @param msg    详细描述
     */
    private void onInit(int status, String msg) {
        if (voiceCopyListener != null) {
            voiceCopyListener.onInit(status, msg);
        }
    }

    /**
     * 复刻录音文本内容
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    private void onGetText(int state, String data, IOException e) {
        if (voiceCopyListener != null) {
            voiceCopyListener.onRecordText(state, data, e);
        }
    }


    /**
     * 处理上传音频检测
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    private void onUpload(int state, String data, IOException e) {
        if (voiceCopyListener != null) {
            voiceCopyListener.onUpload(state, data, e);
        }
    }

    /**
     * 处理提交训练
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    private void onTraining(int state, String data, IOException e) {
        if (voiceCopyListener != null) {
            voiceCopyListener.onTraining(state, data, e);
        }
    }

    /**
     * 处理查询任务状态，查询全部
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    private void onQuery(int state, String data, IOException e) {
        if (voiceCopyListener != null) {
            voiceCopyListener.onQuery(state, data, e);
        }
    }


    /**
     * 处理删除音色资源
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    private void onDelete(int state, String data, IOException e) {
        if (voiceCopyListener != null) {
            voiceCopyListener.onDelete(state, data, e);
        }
    }

    /**
     * 停止声音复刻
     */
    public void stop() {
        NetProxy.getHttp().cancel();
    }


    /**
     * 释放复刻资源
     */
    public void destroy() {
        voiceCopyListener = null;
        NetProxy.getHttp().cancel();
    }


    /**
     * 处理更新任务相关的自定义字段
     *
     * @param state 0 表示成功，-1 表示失败
     * @param data  服务端响应的数据
     * @param e     异常信息
     */
    private void onUpdateCustomInfo(int state, String data, IOException e) {
        if (voiceCopyListener != null) {
            voiceCopyListener.onCustomize(state, data, e);
        }
    }
}
