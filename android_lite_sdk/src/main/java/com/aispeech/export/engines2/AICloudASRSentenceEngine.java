package com.aispeech.export.engines2;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.auth.AIAuthEngine;
import com.aispeech.auth.AIProfile;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AuthUtil;
import com.aispeech.common.Log;
import com.aispeech.export.config.AICloudASRSentenceConfig;
import com.aispeech.export.listeners.AIASRSentenceListener;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.net.dns.DnsResolver;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

/**
 * 一句话识别，可以识别1分钟5M以内的音频
 */
public class AICloudASRSentenceEngine extends BaseEngine {

    /**
     * 一句话识别url
     */
    private static final String URL_ASR_SENTENCE = "/lasr-sentence-api/v2/sentence";
    /**
     * 上传音频文件最大 5M = 5242880
     */
    private static final int MAX_FILE_LENGTH = 5242880;
    private OkHttpClient mClient;
    private String host = "https://lasr.duiopen.com";
    private AIASRSentenceListenerImpl mListener;
    private Map<String, Call> mapCall = new ConcurrentHashMap<>();

    private AICloudASRSentenceEngine() {
    }

    @Override
    public String getTag() {
        return "cloud_asr_sen";
    }

    public static AICloudASRSentenceEngine createInstance() {
        return new AICloudASRSentenceEngine();
    }

    private static String generateAuthParams() {
        AIProfile profile = AIAuthEngine.getInstance().getProfile();
        if (profile == null) {
            Log.w("cloud_asr_sen", "profile is null, do you auth success?");
            return "no_profile";
        }
        String timestamp = String.valueOf(System.currentTimeMillis());
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String sig = AuthUtil.getSignature(profile.getDeviceName() + nonce + profile.getProductId() + timestamp, profile.getDeviceSecret());
        StringBuilder urlParams = new StringBuilder();
        urlParams.append("?productId=")
                .append(profile.getProductId())
                .append("&deviceName=")
                .append(profile.getDeviceName())
                .append("&timestamp=")
                .append(timestamp)
                .append("&nonce=")
                .append(nonce)
                .append("&sig=")
                .append(sig);
        String params = urlParams.toString();
        Log.d("cloud_asr_sen", "generateAuthParams: " + params);
        return params;
    }

    private static AIError getErrorMessage(int errno, String error) {
        AIError aiError = new AIError();
        switch (errno) {
            case 1:
                aiError.setErrId(AIError.ERR_ASR_SENTENCE_SERVER_INNER_ERR);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_INNER_ERR);
                break;
            case 2:
                aiError.setErrId(AIError.ERR_ASR_SENTENCE_SERVER_METHOD_ERR);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_METHOD_ERR);
                break;
            case 10:
                aiError.setErrId(AIError.ERR_ASR_SENTENCE_SERVER_PARAM_LOST);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_PARAM_LOST);
                break;
            case 13:
                aiError.setErrId(AIError.ERR_ASR_SENTENCE_SERVER_UNKONW_PATH);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_UNKONW_PATH);
                break;
            case 211:
                aiError.setErrId(AIError.ERR_ASR_SENTENCE_SERVER_SAVE_AUDIO);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_SAVE_AUDIO);
                break;
            case 521:
                aiError.setErrId(AIError.ERR_ASR_SENTENCE_SERVER_ASR_ERROR);
                aiError.setError(String.format(AIError.ERR_DESCRIPTION_ASR_SENTENCE_ASR_ERROR, error == null ? "" : error));
                break;
            default:
                aiError.setErrId(AIError.ERR_DEFAULT);
                aiError.setError(AIError.ERR_DESCRIPTION_DEFAULT);
                break;
        }
        return aiError;
    }

    /**
     * 解析 401 403 错误
     *
     * @param httpCode httpCode
     * @param body     服务器返回数据
     * @return
     */
    private static AIError parseHttpCode(int httpCode, String body) {
        if (TextUtils.isEmpty(body))
            return new AIError(AIError.ERR_NETWORK, String.valueOf(httpCode));
        String errId = null;
        try {
            JSONObject jsonObject = new JSONObject(body);
            errId = jsonObject.getString("errId");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(errId))
            return new AIError(AIError.ERR_NETWORK, String.valueOf(httpCode));

        if ("120000".equals(errId)) {
            return new AIError(AIError.ERR_ASR_SENTENCE_SERVER_AUTH_FAILED, AIError.ERR_DESCRIPTION_LASR_SERVER_AUTH_FAILED);
        } else if ("120100".equals(errId)) {
            return new AIError(AIError.ERR_ASR_SENTENCE_SERVER_USE_UP, AIError.ERR_DESCRIPTION_LASR_SERVER_USE_UP);
        }
        if ("120200".equals(errId)) {
            return new AIError(AIError.ERR_ASR_SENTENCE_SERVER_FLOW_CONTROL, AIError.ERR_DESCRIPTION_LASR_SERVER_FLOW_CONTROL);
        } else
            return new AIError(AIError.ERR_NETWORK, String.valueOf(httpCode));
    }

    private static RequestBody createFileRequestBody(final MediaType contentType, final File file, final ProgressListener listener) {
        return new RequestBody() {
            @Override
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public long contentLength() {
                return file.length();
            }

            @SuppressLint("NewApi")
            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                try (Source source = Okio.source(file);
                     Buffer buf = new Buffer()) {
                    long haveRead = 0;
                    long allLength = contentLength();
                    for (long readCount; (readCount = source.read(buf, 4096)) != -1; ) {
                        sink.write(buf, readCount);
                        haveRead += readCount;
                        listener.onProgress((int) (haveRead * 100 / allLength));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    /**
     * 初始化
     *
     * @param listener 回调
     */
    public void init(AIASRSentenceListener listener) {
        init(null, listener);
    }

    /**
     * 初始化
     *
     * @param host     自定义域名
     * @param listener 回调
     */
    public synchronized void init(String host, AIASRSentenceListener listener) {
        super.init();
        if (!TextUtils.isEmpty(host)) {
            Log.d(TAG, "Host " + host);
            this.host = host;
        }
        if (mListener == null)
            mListener = new AIASRSentenceListenerImpl();
        mListener.setListener(listener);
        mapCall.clear();
        if (!AIAuthEngine.getInstance().isAuthorized()) {
            if (mListener != null)
                mListener.onInit(AIConstant.OPT_FAILED, "auth failed");
            return;
        }
        if (mClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.pingInterval(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS);
            if (AISpeech.isUseSpeechDns()) {
                builder.dns(new DnsResolver());
            }
            mClient = builder.build();
        }
        if (mClient == null)
            mListener.onInit(AIConstant.OPT_FAILED, "Http init failed");
        else
            mListener.onInit(AIConstant.OPT_SUCCESS, "");
    }

    /**
     * 释放资源
     */
    public synchronized void destroy() {
        super.destroy();
        if (mListener != null)
            mListener.setListener(null);
        cancelAllASR();
        mClient = null;
    }

    /**
     * 一句话识别
     *
     * @param audioFilePath 音频路径
     * @param config        配置参数
     */
    public synchronized void asrSentence(final String audioFilePath, final AICloudASRSentenceConfig config) {
        if (audioFilePath == null || audioFilePath.length() == 0 || config == null) {
            Log.e(TAG, "audioFilePath " + audioFilePath + " config " + config);
            return;
        }
        if (!config.isValid()) {
            if (mListener != null)
                mListener.onAsrSentenceResult(audioFilePath, null, new AIError(AIError.ERR_ASR_SENTENCE_AUDIO_PARAM_ERR, AIError.ERR_DESCRIPTION_LASR_AUDIO_PARAM_ERR));
            return;
        }

        Log.d(TAG, "audioFilePath " + audioFilePath);

        File file = new File(audioFilePath);
        if (!file.exists() || file.length() == 0) {
            if (mListener != null)
                mListener.onAsrSentenceResult(audioFilePath, null, new AIError(AIError.ERR_ASR_SENTENCE_FILE_NOT_EXIST_OR_FILELENGTH_0, AIError.ERR_DESCRIPTION_LASR_FILE_NOT_EXIST_OR_FILELENGTH_0));
            return;
        }

        if (file.length() > MAX_FILE_LENGTH) {
            Log.d(TAG, "文件大小超过限制，不能上传");
            if (mListener != null)
                mListener.onAsrSentenceResult(audioFilePath, null, new AIError(AIError.ERR_ASR_SENTENCE_FILE_OVER_SIZE, AIError.ERR_DESCRIPTION_LASR_FILE_OVER_SIZE));
            return;
        }
        String params = config.toJson();
        Log.d(TAG, "params " + params);
        MultipartBody multipartBody = new MultipartBody.Builder()
                // .setType(MultipartBody.FORM)
                .setType(MediaType.parse("multipart/form-data"))
                .addFormDataPart("params", params)
                .addFormDataPart("file", file.getName(),
                        createFileRequestBody(MediaType.parse("application/octet-stream"), file, new ProgressListener() {
                            @Override
                            public void onProgress(int process) {
                                Log.d(TAG, "process " + process);
                                if (mListener != null)
                                    mListener.onProgress(process);
                            }
                        }))
                .build();
        String url = host + URL_ASR_SENTENCE + generateAuthParams();
        Request request = new Request.Builder()
                .url(url)
                .post(multipartBody)
                .build();
        Log.d(TAG, "asrSentence " + url);
        if (mClient == null) {
            Log.w(TAG, "mClient is null, do you init success?");
            return;
        }
        Call fileCall = mClient.newCall(request);
        mapCall.put(audioFilePath, fileCall);
        fileCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mapCall.remove(audioFilePath);
                Log.d(TAG, "onFailure " + e);
                if (mListener != null)
                    mListener.onAsrSentenceResult(audioFilePath, null, new AIError(AIError.ERR_NETWORK, e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                mapCall.remove(audioFilePath);
                Log.d(TAG, "onResponse code " + response.code());
                if (response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : null;
                    Log.d(TAG, "onResponse " + body);
                    try {
                        JSONObject jsonObject = new JSONObject(body);
                        int errno = jsonObject.optInt("errno", -1);
                        if (errno == 0) {
                            JSONObject dataJson = jsonObject.optJSONObject("data");
                            if (mListener != null)
                                mListener.onAsrSentenceResult(audioFilePath, dataJson != null ? dataJson.toString() : "", null);
                        } else {
                            if (mListener != null)
                                mListener.onAsrSentenceResult(audioFilePath, null, getErrorMessage(errno, jsonObject.optString("error")));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (mListener != null)
                            mListener.onAsrSentenceResult(audioFilePath, null, new AIError(AIError.ERR_ASR_SENTENCE_JSON_ERR, AIError.ERR_DESCRIPTION_LASR_JSON_ERR));
                    }
                } else {
                    String body = null;
                    if (response.body() != null) {
                        body = response.body().string();
                        Log.d(TAG, "onResponse " + body);
                    }
                    if (mListener != null)
                        mListener.onAsrSentenceResult(audioFilePath, null, parseHttpCode(response.code(), body));
                }
            }
        });
    }

    /**
     * 一句话识别
     *
     * @param uniqueId  本次识别的唯一标识，会随着回调一起返回，不能为空。例如 "ABC123"
     * @param audioData 音频数据
     * @param config    配置参数
     */
    public synchronized void asrSentence(final String uniqueId, final byte[] audioData, final AICloudASRSentenceConfig config) {
        if (TextUtils.isEmpty(uniqueId) || audioData == null || audioData.length == 0 || config == null) {
            Log.e(TAG, "uniqueId " + uniqueId + " audioData " + audioData + " config " + config);
            return;
        }
        if (!config.isValid()) {
            if (mListener != null)
                mListener.onAsrSentenceResult(uniqueId, null, new AIError(AIError.ERR_ASR_SENTENCE_AUDIO_PARAM_ERR, AIError.ERR_DESCRIPTION_LASR_AUDIO_PARAM_ERR));
            return;
        }

        Log.d(TAG, "uniqueId " + uniqueId + " audioData.length " + audioData.length);

        if (audioData.length > MAX_FILE_LENGTH) {
            Log.d(TAG, "音频大小超过限制，不能上传");
            if (mListener != null)
                mListener.onAsrSentenceResult(uniqueId, null, new AIError(AIError.ERR_ASR_SENTENCE_FILE_OVER_SIZE, AIError.ERR_DESCRIPTION_LASR_FILE_OVER_SIZE));
            return;
        }
        String params = config.toJson();
        Log.d(TAG, "params " + params);
        MultipartBody multipartBody = new MultipartBody.Builder()
                // .setType(MultipartBody.FORM)
                .setType(MediaType.parse("multipart/form-data"))
                .addFormDataPart("params", params)
                .addFormDataPart("file", uniqueId,
                        new BufferRequestBody(audioData, MediaType.parse("application/octet-stream"), new ProgressListener() {
                            @Override
                            public void onProgress(int process) {
                                Log.d(TAG, "process " + process);
                                if (mListener != null)
                                    mListener.onProgress(process);
                            }
                        }))
                .build();
        String url = host + URL_ASR_SENTENCE + generateAuthParams();
        Request request = new Request.Builder()
                .url(url)
                .post(multipartBody)
                .build();
        Log.d(TAG, "asrSentence " + url);
        if (mClient == null) {
            Log.w(TAG, "mClient is null, do you init success?");
            return;
        }
        Call fileCall = mClient.newCall(request);
        mapCall.put(uniqueId, fileCall);
        fileCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                mapCall.remove(uniqueId);
                Log.d(TAG, "onFailure " + e);
                if (mListener != null)
                    mListener.onAsrSentenceResult(uniqueId, null, new AIError(AIError.ERR_NETWORK, e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                mapCall.remove(uniqueId);
                Log.d(TAG, "onResponse code " + response.code());
                if (response.isSuccessful()) {
                    String body = response.body() != null ? response.body().string() : null;
                    Log.d(TAG, "onResponse " + body);
                    try {
                        JSONObject jsonObject = new JSONObject(body);
                        int errno = jsonObject.optInt("errno", -1);
                        if (errno == 0) {
                            JSONObject dataJson = jsonObject.optJSONObject("data");
                            if (mListener != null)
                                mListener.onAsrSentenceResult(uniqueId, dataJson != null ? dataJson.toString() : "", null);
                        } else {
                            if (mListener != null)
                                mListener.onAsrSentenceResult(uniqueId, null, getErrorMessage(errno, jsonObject.optString("error")));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (mListener != null)
                            mListener.onAsrSentenceResult(uniqueId, null, new AIError(AIError.ERR_ASR_SENTENCE_JSON_ERR, AIError.ERR_DESCRIPTION_LASR_JSON_ERR));
                    }
                } else {
                    String body = null;
                    if (response.body() != null) {
                        body = response.body().string();
                        Log.d(TAG, "onResponse " + body);
                    }
                    if (mListener != null)
                        mListener.onAsrSentenceResult(uniqueId, null, parseHttpCode(response.code(), body));
                }
            }
        });
    }

    /**
     * 取消识别请求
     *
     * @param audioFilePathOrUniqueId 音频路径或者唯一标识
     * @see #asrSentence(String, AICloudASRSentenceConfig)
     * @see #asrSentence(String, byte[], AICloudASRSentenceConfig)
     */
    public synchronized void cancelASR(String audioFilePathOrUniqueId) {
        super.cancel();
        if (mapCall.containsKey(audioFilePathOrUniqueId)) {
            Call fileCall = mapCall.remove(audioFilePathOrUniqueId);
            if (fileCall != null && !fileCall.isCanceled()) {
                Log.d(TAG, "cancelASR");
                try {
                    fileCall.cancel();
                } catch (Throwable t) {
                }
            }
        }
    }

    /**
     * 取消所有识别请求
     */
    public synchronized void cancelAllASR() {
        if (!mapCall.isEmpty()) {
            Log.d(TAG, "cancelAllASR " + mapCall.size());
            Iterator<String> iter = mapCall.keySet().iterator();
            while (iter.hasNext()) {
                Call fileCall = mapCall.get(iter.next());
                if (fileCall != null && !fileCall.isCanceled()) {
                    try {
                        fileCall.cancel();
                    } catch (Throwable t) {
                    }
                }
            }
            mapCall.clear();
        }
    }

    private interface ProgressListener {
        /**
         * 上传文件的进度
         *
         * @param process 0 - 100, 100 即上传成功
         */
        void onProgress(int process);
    }

    private static class BufferRequestBody extends RequestBody {
        private final byte[] content;
        private final MediaType mediaType;
        private final ProgressListener progressListener;

        public BufferRequestBody(byte[] content, MediaType mediaType, ProgressListener progressListener) {
            this.content = content;
            this.mediaType = mediaType;
            this.progressListener = progressListener;
        }

        @Override
        public MediaType contentType() {
            return mediaType;
        }

        @Override
        public long contentLength() {
            return content.length;
        }

        @Override
        public void writeTo(BufferedSink sink) throws IOException {
            if (progressListener != null)
                progressListener.onProgress(0);
            int offset = 0;
            while (offset < content.length) {
                int byteCount = content.length - offset > 1024 ? 1024 : content.length - offset;
                sink.write(content, offset, byteCount);
                offset += byteCount;
                if (progressListener != null)
                    progressListener.onProgress(offset * 100 / content.length);
            }
            Log.d("cloud_asr_sen", "BufferRequestBody write " + offset);
        }
    }

    private static class AIASRSentenceListenerImpl implements AIASRSentenceListener {
        private static Handler mainHandler = new Handler(Looper.getMainLooper());
        private AIASRSentenceListener listener;

        public void setListener(AIASRSentenceListener listener) {
            this.listener = listener;
        }

        @Override
        public void onInit(final int status, final String errMsg) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onInit(status, errMsg);
                }
            });
        }

        @Override
        public void onProgress(final int process) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onProgress(process);
                }
            });
        }

        @Override
        public void onAsrSentenceResult(final String audioFilePath, final String result, final AIError error) {
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (listener != null)
                        listener.onAsrSentenceResult(audioFilePath, result, error);
                }
            });
        }
    }

}
