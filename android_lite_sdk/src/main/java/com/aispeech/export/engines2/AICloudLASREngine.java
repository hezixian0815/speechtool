package com.aispeech.export.engines2;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.auth.AIAuthEngine;
import com.aispeech.auth.AIProfile;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AuthUtil;
import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.export.config.AICloudLASRConfig;
import com.aispeech.export.lasr.LasrDatabaseManager;
import com.aispeech.export.lasr.LasrSqlEntity;
import com.aispeech.export.listeners.AILASRListener;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.net.dns.DnsResolver;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;

/**
 * 长语音音频文件识别，整个流程如下，如果识别文件以http开头，则直接从第三步开始：
 * <ol>
 * <li>创建上传音频文件的任务</li>
 * <li>分片上传音频文件</li>
 * <li>创建识别任务</li>
 * <li>查询识别进度</li>
 * <li>查询识别结果</li>
 * </ol>
 */
public class AICloudLASREngine extends BaseEngine {

    private interface ProgressListener {
        /**
         * 上传文件的进度
         *
         * @param process 0 - 100, 100 即上传成功
         */
        void onProgress(int process);
    }

    /**
     * 创建上传音频文件的任务
     */
    private static final String URL_CREATE_UPLOAD_AUDIO_TASK = "/lasr-file-api/v2/audio";

    /**
     * 分片上传音频文件 /lasr-file-api/v2/audio/{audio_id}/slice/{slice_index}
     */
    private static final String URL_UPLOAD_AUDIO_SLICE = "/lasr-file-api/v2/audio/%s/slice/%s";

    /**
     * 创建任务并开始转写
     */
    private static final String URL_CREATE_TASK = "/lasr-file-api/v2/task";

    /**
     * 查询转写进度 /lasr-file-api/v2/task/<task_id>/progress
     */
    private static final String URL_QUERY_TASK_PROCESS = "/lasr-file-api/v2/task/%s/progress";

    /**
     * 请求离线语音转写结果 /lasr-file-api/v2/task/<task_id>/result
     */
    private static final String URL_QUERY_TASK_RESULT = "/lasr-file-api/v2/task/%s/result";

    private String hostLasr = "https://lasr.duiopen.com";
    /**
     * 上传音频文件最大 500M = 524288000
     */
    private static final int MAX_FILE_LENGTH = 524288000;
    private OkHttpClient mClient;
    private String productId;

    private Call fileCall = null;
    private AILASRListenerImpl lasrListener;
    private LasrDatabaseManager dbManager;
    private LasrSqlEntity lasrSqlEntity;
    private AtomicBoolean isUploadFile;
    private static volatile AICloudLASREngine engine = null;

    public static AICloudLASREngine getInstance() {
        if (engine == null)
            synchronized (AICloudLASREngine.class) {
                if (engine == null)
                    engine = new AICloudLASREngine();
            }
        return engine;
    }

    private AICloudLASREngine() {
        isUploadFile = new AtomicBoolean(false);
        lasrListener = new AILASRListenerImpl();
    }

    @Override
    public String getTag() {
        return "cloud_lasr";
    }

    public void init(Context context, AILASRListener listener) {
        init(context, null, listener);
    }

    public synchronized void init(Context context, String host, AILASRListener listener) {
        super.init();
        if (dbManager == null)
            dbManager = new LasrDatabaseManager(context.getApplicationContext());
        if (!TextUtils.isEmpty(host)) {
            Log.d(TAG, "Host " + host);
            this.hostLasr = host;
        }
        lasrListener.setLasrListener(listener);
        this.fileCall = null;
        this.productId = null;
        if (!AIAuthEngine.getInstance().isAuthorized()) {
            if (lasrListener != null)
                lasrListener.onInit(AIConstant.OPT_FAILED, "auth failed");
            return;
        }
        AIProfile profile = AIAuthEngine.getInstance().getProfile();
        if (profile != null) {
            productId = profile.getProductId();
        }
        if (mClient == null) {
            OkHttpClient.Builder builder = new OkHttpClient()
                    .newBuilder()
                    .pingInterval(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .connectTimeout(30, TimeUnit.SECONDS);
            if (AISpeech.isUseSpeechDns()) {
                builder.dns(new DnsResolver());
            }
            mClient = builder.build();
        }
        if (mClient == null)
            lasrListener.onInit(AIConstant.OPT_FAILED, "Http init failed");
        else
            lasrListener.onInit(AIConstant.OPT_SUCCESS, "");
    }

    protected static String generateAuthParams() {
        AIProfile profile = AIAuthEngine.getInstance().getProfile();
        if (profile == null)
            return "no_profile";
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
        Log.d("cloud_lasr", "generateAuthParams: " + params);

        return params;
    }

    /**
     * 创建上传音频任务
     * 5H之内的音频， 文件最大500M
     *
     * @param audioFilePath 本地音频文件的绝对路径
     * @param audioParam    音频文件的参数
     */
    public void uploadAudioFile(final String audioFilePath, final AICloudLASRConfig.AudioParam audioParam) {
        if (audioFilePath == null || audioParam == null) {
            Log.d(TAG, "uploadFile()  audioFilePath or audioParam is null");
            return;
        }
        if (!audioParam.isValid()) {
            Log.d(TAG, "uploadFile()  audioParam.isValid() false");
            if (lasrListener != null)
                lasrListener.onUploadFileResult(audioFilePath, null, new AIError(AIError.ERR_LASR_AUDIO_PARAM_ERR, AIError.ERR_DESCRIPTION_LASR_AUDIO_PARAM_ERR));
            return;
        }

        File audioFile = new File(audioFilePath);
        if (!audioFile.exists() || !audioFile.isFile() || audioFile.length() == 0) {
            Log.d(TAG, "uploadFile() audioFilePath not exists or length is 0");
            if (lasrListener != null)
                lasrListener.onUploadFileResult(audioFilePath, null, new AIError(AIError.ERR_LASR_FILE_NOT_EXIST_OR_FILELENGTH_0, AIError.ERR_DESCRIPTION_LASR_FILE_NOT_EXIST_OR_FILELENGTH_0));
            return;
        }

        if (audioFile.length() > MAX_FILE_LENGTH) {
            Log.d(TAG, "文件大小超过限制，不能上传");
            if (lasrListener != null)
                lasrListener.onUploadFileResult(audioFilePath, null, new AIError(AIError.ERR_LASR_FILE_OVER_SIZE, AIError.ERR_DESCRIPTION_LASR_FILE_OVER_SIZE));
            return;
        }
        if (isUploadFile.get()) {
            Log.d(TAG, "uploadSlice isUploadFile true");
            if (lasrListener != null)
                lasrListener.onUploadFileResult(audioFilePath, null, new AIError(AIError.ERR_LASR_ONLY_ONE_TASK, AIError.ERR_DESCRIPTION_LASR_ONLY_ONE_TASK));
            return;
        }
        isUploadFile.set(true);
        String uuid = UUID.randomUUID().toString();
        Log.d(TAG, "uuid " + uuid);
        int blockSize = LasrSqlEntity.calculateBlockSize(audioFile.length());
        int sliceNum = LasrSqlEntity.calculateSliceNum(audioFile.length(), blockSize);
        Log.d(TAG, "audioFile.length " + audioFile.length() + " blockSize " + blockSize + " sliceNum " + sliceNum);
        lasrSqlEntity = new LasrSqlEntity(LasrSqlEntity.TYPE_LOCAL_FILE, audioFilePath, audioFile.length(),
                null, uuid, blockSize, sliceNum);
        if (dbManager != null)
            dbManager.insert(lasrSqlEntity);

        FormBody.Builder requestBodyBuilder = new FormBody.Builder()
                //  .add("app_id", this.productId)
                .add("audio_type", audioParam.getAudioType())
                .add("sample_rate", String.valueOf(audioParam.getSampleRate()))
                .add("channel", String.valueOf(audioParam.getChannel()))
                .add("sample_bytes", String.valueOf(audioParam.getSampleBytes()))
                .add("slice_num", String.valueOf(sliceNum));

        Request request = new Request.Builder()
                .url(hostLasr + URL_CREATE_UPLOAD_AUDIO_TASK + generateAuthParams())
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("x-sessionId", lasrSqlEntity.getUuid())
                .post(requestBodyBuilder.build())
                .build();

        if (mClient == null) {
            Log.d(TAG, "uploadAudioFile() mClient is null");
            return;
        }
        Call call = mClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isUploadFile.set(false);
                Log.d(TAG, "uploadFile onFailure " + e.getMessage());
                if (lasrListener != null)
                    lasrListener.onUploadFileResult(audioFilePath, null, new AIError(AIError.ERR_NETWORK, e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                isUploadFile.set(false);
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    Log.d(TAG, "uploadFile " + body);
                    try {
                        JSONObject jsonObject = new JSONObject(body);
                        int errno = jsonObject.optInt("errno", -1);
                        if (errno == 0) {
                            String audio_id = jsonObject.getJSONObject("data").getString("audio_id");
                            if (lasrSqlEntity != null) {
                                lasrSqlEntity.setAudioId(audio_id);
                                if (dbManager != null)
                                    dbManager.update(lasrSqlEntity);
                            }
                            uploadSlice();
                        } else {
                            if (lasrListener != null)
                                lasrListener.onUploadFileResult(audioFilePath, null, getErrorMessage(errno));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (lasrListener != null)
                            lasrListener.onUploadFileResult(audioFilePath, null, new AIError(AIError.ERR_LASR_JSON_ERR, AIError.ERR_DESCRIPTION_LASR_JSON_ERR));
                    }
                } else {
                    String body = null;
                    if (response.body() != null) {
                        body = response.body().string();
                        Log.d(TAG, "uploadFile " + body);
                    }
                    if (lasrListener != null)
                        lasrListener.onUploadFileResult(audioFilePath, null, parseHttpCode(response.code(), body));
                }
            }
        });
    }

    private synchronized void uploadSlice() {
        if (lasrSqlEntity == null)
            return;
        if (!lasrSqlEntity.isLocalFile() || TextUtils.isEmpty(lasrSqlEntity.getAudioId()) || lasrSqlEntity.isUploadSucess())
            return;
        File audioFile = new File(lasrSqlEntity.getUri());
        if (lasrSqlEntity.getFileLength() > 0 && audioFile.length() > 0 &&
                lasrSqlEntity.getFileLength() != audioFile.length()) {
            // 上传文件时做一下文件大小的检查
            Log.d(TAG, "File length does not match expected");
            if (lasrListener != null)
                lasrListener.onUploadFileResult(lasrSqlEntity.getUri(), null, new AIError(AIError.ERR_LASR_FILE_DIFFERENT, AIError.ERR_DESCRIPTION_LASR_FILE_DIFFERENT));
            return;
        }
        if (isUploadFile.get()) {
            Log.d(TAG, "uploadSlice isUploadFile true");
            if (lasrListener != null)
                lasrListener.onUploadFileResult(lasrSqlEntity.getUri(), null, new AIError(AIError.ERR_LASR_ONLY_ONE_TASK, AIError.ERR_DESCRIPTION_LASR_ONLY_ONE_TASK));
            return;
        }
        isUploadFile.set(true);
        MultipartBody multipartBody = new MultipartBody.Builder()
                // .setType(MultipartBody.FORM)
                .setType(MediaType.parse("multipart/form-data"))
                .addFormDataPart("file", audioFile.getName(),
                        new SliceRequestBody(lasrSqlEntity, MediaType.parse("application/octet-stream")))
                .build();

        // lasrSqlEntity.getSliceIndex() 保存的是已上传的 SliceIndex
        String url = String.format(URL_UPLOAD_AUDIO_SLICE, lasrSqlEntity.getAudioId(), lasrSqlEntity.getSliceIndex() + 1);
        Request request = new Request.Builder()
                .url(hostLasr + url + generateAuthParams())
                .addHeader("x-sessionId", lasrSqlEntity.getUuid())
                .post(multipartBody)
                .build();
        Log.d(TAG, "uploadSlice " + url);

        if (mClient == null) {
            Log.d(TAG, "uploadSlice() mClient is null");
            return;
        }
        fileCall = mClient.newCall(request);
        fileCall.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                isUploadFile.set(false);
                Log.d(TAG, "uploadSlice onFailure " + e.getMessage());
                if (lasrListener != null)
                    lasrListener.onUploadFileResult(lasrSqlEntity != null ? lasrSqlEntity.getUri() : "", null,
                            new AIError("Canceled".equals(e.getMessage()) ? AIError.ERR_LASR_CALL_CANCEL : AIError.ERR_NETWORK, e.getMessage()));
            }

            @Override
            public synchronized void onResponse(Call call, Response response) throws IOException {
                isUploadFile.set(false);
                Log.d(TAG, "uploadSlice code " + response.code());
                if (lasrSqlEntity == null)
                    return;
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    Log.d(TAG, "uploadSlice " + body);
                    try {
                        JSONObject jsonObject = new JSONObject(body);
                        int errno = jsonObject.getInt("errno");
                        if (errno == 0) {
                            // 最后一次会多一个url字段 {"errno":0,"error":"","data":{"slices":9,"total":9,"url":"http://XXXXXX"}}
                            int slices = jsonObject.getJSONObject("data").getInt("slices");
                            lasrSqlEntity.setSliceIndex(lasrSqlEntity.getSliceIndex() + 1);
                            if (dbManager != null)
                                dbManager.update(lasrSqlEntity);
                            Log.d(TAG, "current sliceIndex " + lasrSqlEntity.getSliceIndex() + " server slices " + slices);
                            if (lasrListener != null)
                                lasrListener.onUploadFileProcess(lasrSqlEntity.getUri(), lasrSqlEntity.getUploadProgress());
                            // Log.d(TAG, "query " + query(lasrSqlEntity.getId()));
                            if (!lasrSqlEntity.isUploadSucess()) {
                                /*if (slices == 6) {
                                    lasrSqlEntity = null;
                                    return;
                                }*/
                                uploadSlice();
                            } else if (lasrListener != null)
                                lasrListener.onUploadFileResult(lasrSqlEntity.getUri(), lasrSqlEntity.getAudioId(), null);
                        } else if (lasrListener != null)
                            lasrListener.onUploadFileResult(lasrSqlEntity.getUri(), null, getErrorMessage(errno));

                    } catch (Exception e) {
                        e.printStackTrace();
                        if (lasrListener != null)
                            lasrListener.onUploadFileResult(lasrSqlEntity.getUri(), null, new AIError(AIError.ERR_LASR_JSON_ERR, AIError.ERR_DESCRIPTION_LASR_JSON_ERR));
                    }
                } else {
                    String body = null;
                    if (response.body() != null) {
                        body = response.body().string();
                        Log.d(TAG, "uploadSlice " + body);
                    }
                    if (lasrListener != null)
                        lasrListener.onUploadFileResult(lasrSqlEntity.getUri(), null, parseHttpCode(response.code(), body));
                }
            }
        });
    }

    /**
     * 恢复上传，从数据库里取最近上传失败的任务
     *
     * @return 返回上次未完成上传任务的信息，null 说明没有需要恢复上传的任务
     */
    public synchronized LasrSqlEntity uploadResume() {
        Log.d(TAG, "uploadResume()");
        if (lasrSqlEntity == null) {
            // 读本地数据库，获取最新的没有上传的任务
            if (dbManager != null)
                lasrSqlEntity = dbManager.queryLatestUpload();
            Log.d(TAG, "uploadResume from db: " + lasrSqlEntity);
        }
        if (lasrSqlEntity != null)
            uploadSlice();
        return lasrSqlEntity;
    }

    /**
     * 查询所有的识别任务信息，但不包含识别结果
     *
     * @return 长语音的识别任务
     */
    private List<LasrSqlEntity> queryAllTaskExcludeASR() {
        return dbManager != null ? dbManager.query(false) : null;
    }

    /**
     * 查询识别结果
     *
     * @param taskId 创建的识别任务的id
     * @return 识别结果
     */
    private String queryASR(String taskId) {
        List<LasrSqlEntity> list = dbManager != null ? dbManager.query(taskId, true) : null;
        if (list != null && list.size() > 0 && list.get(0) != null)
            return list.get(0).getAsr();
        else
            return null;
    }

    /**
     * 取消文件上传。调用本方法后需要等 {@linkplain AILASRListener#onUploadFileResult}
     * 方法回调 {@linkplain AIError#ERR_LASR_CALL_CANCEL} 错误码之后才能说明文件上传的请求已经被取消掉
     */
    public synchronized void cancelUploadFile() {
        if (fileCall != null && !fileCall.isCanceled()) {
            Log.d(TAG, "cancelUploadFile");
            fileCall.cancel();
            fileCall = null;
        }
    }

    /**
     * 是否正在上传文件
     *
     * @return true 是，false 否
     */
    public boolean isUploadFile() {
        return isUploadFile.get();
    }

    private class SliceRequestBody extends RequestBody {
        private final String filePath;
        private final byte[] content;
        private final MediaType mediaType;
        private final int offest;

        public SliceRequestBody(LasrSqlEntity lasrSqlEntity, MediaType mediaType) {
            this.filePath = lasrSqlEntity.getUri();
            offest = lasrSqlEntity.getUploadOffset();
            long rest = lasrSqlEntity.getFileLength() - offest;
            //
            Log.d(TAG, "rest " + rest + " offest " + offest + " blockSize " + lasrSqlEntity.getBlockSize());
            if (rest > lasrSqlEntity.getBlockSize())
                this.content = new byte[lasrSqlEntity.getBlockSize()];
            else
                this.content = new byte[rest < 0 ? 0 : (int) rest];
            this.mediaType = mediaType;
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
            File file = new File(filePath);
            int allReadSize = 0;
            int times = 0;
            while (allReadSize < content.length) {
                // 正常来讲一次就行
                int readSize = FileUtils.getFileBlock(file, content, content.length - allReadSize, offest + allReadSize);
                int offset = 0;
                while (offset < readSize) {
                    int byteCount = readSize - offset > 5120 ? 5120 : readSize - offset;
                    sink.write(content, offset, byteCount);
                    offset += byteCount;
                }
                allReadSize += readSize;
                Log.d(TAG, "readSize " + readSize + " allReadSize " + allReadSize);
                // 防止死循环
                if (++times > 4)
                    break;
            }
        }
    }

    /**
     * 创建识别任务，使用http格式的音频文件
     *
     * @param fileHttpUrl 音频文件的HTTP下载地址。 可选：值可以为一个可以下载的HTTP地址（http地址格式不正确或不能正常下载都会报错）， 或者空字符串。 (警告) 如果下载的地址里包含中文，需要把中文的文件名和路径名进行urlencode。
     * @param taskParam   音频参数和识别参数
     */
    public void createTaskWithHttpFile(final String fileHttpUrl, AICloudLASRConfig.TaskParam taskParam) {
        if (!TextUtils.isEmpty(fileHttpUrl))
            createTask(null, fileHttpUrl, taskParam);
    }

    /**
     * 创建识别任务，使用上传文件后得到的 audioId，只有文件上传完毕 audioId 才能使用
     *
     * @param audioId   本地文件上传服务器后得到的音频文件
     * @param taskParam 音频参数和识别参数
     */
    public void createTaskWithAudioId(final String audioId, AICloudLASRConfig.TaskParam taskParam) {
        if (!TextUtils.isEmpty(audioId))
            createTask(audioId, null, taskParam);
    }

    private synchronized void createTask(final String audioId, final String fileHttpUrl, AICloudLASRConfig.TaskParam taskParam) {
        if (TextUtils.isEmpty(audioId) && TextUtils.isEmpty(fileHttpUrl)) {
            Log.w(TAG, "audioId and fileHttpUrl are null");
            return;
        }
        if (!TextUtils.isEmpty(audioId) && !TextUtils.isEmpty(fileHttpUrl)) {
            Log.w(TAG, "audioId and fileHttpUrl are not null: audioId " + audioId + " fileHttpUrl " + fileHttpUrl);
            return;
        }
        if (taskParam == null)
            return;
        if (!taskParam.isValid())
            return;

        if (!TextUtils.isEmpty(audioId)) {
            if (lasrSqlEntity == null || !audioId.equals(lasrSqlEntity.getAudioId())) {
                lasrSqlEntity = new LasrSqlEntity(LasrSqlEntity.TYPE_LOCAL_FILE, null, 0, audioId, null, 0, 0);
                if (dbManager != null)
                    dbManager.insert(lasrSqlEntity);
            }
        } else {
            lasrSqlEntity = new LasrSqlEntity(LasrSqlEntity.TYPE_HTTP_FILE, fileHttpUrl, 516);
            if (dbManager != null)
                dbManager.insert(lasrSqlEntity);
        }

        Log.d(TAG, "fileHttpUrl " + fileHttpUrl + " audioId " + audioId);
        Log.d(TAG, "taskParam " + taskParam);

        FormBody.Builder requestBodyBuilder = new FormBody.Builder()
                .add("productId", this.productId)
                .add("audio_type", taskParam.getAudioParam().getAudioType()) // 前面做了检查 isValid()
                .add("file_len", String.valueOf(taskParam.getFileLen()))
                .add("check_length", String.valueOf(taskParam.getCheckLength()))
                .add("speaker_number", String.valueOf(taskParam.getSpeakerNumber()))
                .add("use_segment", taskParam.isUseSegment() ? "1" : "0")
                .add("use_fullstop", taskParam.isUseFullstop() ? "1" : "0")
                .add("task_type", String.valueOf(taskParam.getTaskType()))
                .add("use_inverse_txt", taskParam.isUseInverseTxt() ? "1" : "0")
                .add("use_txt_smooth", taskParam.isUseTxtSmooth() ? "1" : "0")
                .add("use_aux",taskParam.isUseAux() ? "1" : "0")
                .add("use_paragraph",taskParam.isUseParagraph() ? "1" : "0");
        if (!TextUtils.isEmpty(taskParam.getCallback())) {
            requestBodyBuilder.add("callback", taskParam.getCallback());
        }
        if (!TextUtils.isEmpty(audioId)) {
            requestBodyBuilder.add("audio_id", audioId);
        } else {
            requestBodyBuilder.add("file_path", fileHttpUrl);
        }

        if (!TextUtils.isEmpty(taskParam.getRes())) {
            requestBodyBuilder.add("res", taskParam.getRes());
        }
        if (!TextUtils.isEmpty(taskParam.getLang())) {
            requestBodyBuilder.add("lang", taskParam.getLang());
        }
        if (taskParam.getSpeakerRate() > 0) {
            requestBodyBuilder.add("speaker_rate", String.valueOf(taskParam.getSpeakerRate()));
        }
        if (!TextUtils.isEmpty(taskParam.getPhraseFileId())) {
            requestBodyBuilder.add("phrase_file_id", taskParam.getPhraseFileId());
        }

        if (taskParam.getHotwords() != null && taskParam.getHotwords().size() > 0) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < taskParam.getHotwords().size(); i++) {
                sb.append(taskParam.getHotwords().get(i));
                if (i < taskParam.getHotwords().size() - 1)
                    sb.append(",");
            }
            requestBodyBuilder.add("hotwords", sb.toString());
        }
        if (!TextUtils.isEmpty(taskParam.getLmid())) {
            requestBodyBuilder.add("lmid", taskParam.getLmid());
        }
        if (!TextUtils.isEmpty(taskParam.getSensitiveFileId())) {
            requestBodyBuilder.add("sensitive_file_id", taskParam.getSensitiveFileId());
        }

        Request request = new Request.Builder()
                .url(hostLasr + URL_CREATE_TASK + generateAuthParams())
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .post(requestBodyBuilder.build())
                .build();

        if (mClient == null) {
            Log.d(TAG, "createTask() mClient is null");
            return;
        }
        Call call = mClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "createTask onFailure " + e.getMessage());
                if (lasrListener != null)
                    lasrListener.onTaskCreate(TextUtils.isEmpty(audioId) ? fileHttpUrl : audioId, null, new AIError(AIError.ERR_NETWORK, e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    Log.d(TAG, "createTask " + body);
                    try {
                        JSONObject jsonObject = new JSONObject(body);
                        int errno = jsonObject.optInt("errno", -1);
                        if (errno == 0) {
                            String task_id = jsonObject.getJSONObject("data").getString("task_id");
                            if (lasrSqlEntity != null) {
                                lasrSqlEntity.setTaskId(task_id);
                                if (dbManager != null)
                                    dbManager.update(lasrSqlEntity);
                            }
                            if (lasrListener != null)
                                lasrListener.onTaskCreate(TextUtils.isEmpty(audioId) ? fileHttpUrl : audioId, task_id, null);
                        } else {
                            if (lasrListener != null)
                                lasrListener.onTaskCreate(TextUtils.isEmpty(audioId) ? fileHttpUrl : audioId, null, getErrorMessage(errno));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (lasrListener != null)
                            lasrListener.onTaskCreate(TextUtils.isEmpty(audioId) ? fileHttpUrl : audioId, null, new AIError(AIError.ERR_LASR_JSON_ERR, AIError.ERR_DESCRIPTION_LASR_JSON_ERR));
                    }
                } else {
                    String body = null;
                    if (response.body() != null) {
                        body = response.body().string();
                        Log.d(TAG, "createTask " + body);
                    }
                    if (lasrListener != null)
                        lasrListener.onTaskCreate(TextUtils.isEmpty(audioId) ? fileHttpUrl : audioId, null, parseHttpCode(response.code(), body));
                }
            }
        });

    }

    public void queryTaskProcess(final String taskId) {
        if (TextUtils.isEmpty(taskId)) {
            if (lasrListener != null)
                lasrListener.onTaskProcess(taskId, 0, new AIError(AIError.ERR_DEFAULT, "taskId is empty"));
            return;
        }
        FormBody.Builder requestBodyBuilder = new FormBody.Builder()
                .add("productId", this.productId)
                .add("task_id", taskId);
        Request request = new Request.Builder()
                .url(hostLasr + String.format(URL_QUERY_TASK_PROCESS, taskId) + generateAuthParams())
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .post(requestBodyBuilder.build())
                .build();

        if (mClient == null) {
            Log.d(TAG, "queryTaskProcess() mClient is null");
            return;
        }
        Call call = mClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "queryTaskProcess onFailure " + e.getMessage());
                if (lasrListener != null)
                    lasrListener.onTaskProcess(taskId, 0, new AIError(AIError.ERR_NETWORK, e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    Log.d(TAG, "queryTaskProcess " + body);
                    try {
                        JSONObject jsonObject = new JSONObject(body);
                        int errno = jsonObject.optInt("errno", -1);
                        if (errno == 0) {
                            int progress = jsonObject.getJSONObject("data").getInt("progress");
                            if (lasrSqlEntity != null) {
                                lasrSqlEntity.setProgress(progress);
                                if (dbManager != null)
                                    dbManager.update(lasrSqlEntity);
                            } else
                                Log.d(TAG, "lasrSqlEntity is null");

                            if (lasrListener != null)
                                lasrListener.onTaskProcess(taskId, progress, null);
                        } else {
                            if (lasrListener != null)
                                lasrListener.onTaskProcess(taskId, 0, getErrorMessage(errno));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (lasrListener != null)
                            lasrListener.onTaskProcess(taskId, 0, new AIError(AIError.ERR_LASR_JSON_ERR, AIError.ERR_DESCRIPTION_LASR_JSON_ERR));
                    }
                } else {
                    String body = null;
                    if (response.body() != null) {
                        body = response.body().string();
                        Log.d(TAG, "queryTaskProcess " + body);
                    }
                    if (lasrListener != null)
                        lasrListener.onTaskProcess(taskId, 0, parseHttpCode(response.code(), body));
                }
            }
        });
    }

    public void queryTaskResult(final String taskId) {
        if (TextUtils.isEmpty(taskId)) {
            if (lasrListener != null)
                lasrListener.onTaskResult(taskId, null, new AIError(AIError.ERR_DEFAULT, "taskId is empty"));
            return;
        }
        FormBody.Builder requestBodyBuilder = new FormBody.Builder()
                .add("productId", this.productId)
                .add("task_id", taskId);
        Request request = new Request.Builder()
                .url(hostLasr + String.format(URL_QUERY_TASK_RESULT, taskId) + generateAuthParams())
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .post(requestBodyBuilder.build())
                .build();

        if (mClient == null) {
            Log.d(TAG, "queryTaskResult() mClient is null");
            return;
        }
        Call call = mClient.newCall(request);
        call.enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.d(TAG, "queryTaskResult onFailure " + e.getMessage());
                if (lasrListener != null)
                    lasrListener.onTaskResult(taskId, null, new AIError(AIError.ERR_NETWORK, e.getMessage()));
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.d(TAG, "queryTaskResult code " + response.code());
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    Log.d(TAG, "queryTaskResult " + body);
                    try {
                        JSONObject jsonObject = new JSONObject(body);
                        int errno = jsonObject.optInt("errno", -1);
                        if (errno == 0) {
                            JSONObject data = jsonObject.getJSONObject("data");
                            String asr = data.toString();
                            /*lasrSqlEntity.setAsr(asr);
                            if (dbManager != null)
                                dbManager.update(lasrSqlEntity);*/
                            if (lasrListener != null)
                                lasrListener.onTaskResult(taskId, asr, null);
                        } else {
                            if (lasrListener != null)
                                lasrListener.onTaskResult(taskId, null, getErrorMessage(errno));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (lasrListener != null)
                            lasrListener.onTaskResult(taskId, null, new AIError(AIError.ERR_LASR_JSON_ERR, AIError.ERR_DESCRIPTION_LASR_JSON_ERR));
                    }
                } else {
                    String body = null;
                    if (response.body() != null) {
                        body = response.body().string();
                        Log.d(TAG, "queryTaskResult " + body);
                    }
                    if (lasrListener != null)
                        lasrListener.onTaskResult(taskId, null, parseHttpCode(response.code(), body));
                }
            }
        });
    }

    /**
     * 销毁，{@link #init} 时设置的 AILASRListener 置为null
     */
    public synchronized void destroy() {
        if (lasrListener != null)
            lasrListener.setLasrListener(null);
        cancelUploadFile();
        if (dbManager != null) {
            dbManager.close();
            dbManager = null;
        }
        mClient = null;
        lasrSqlEntity = null;
        productId = null;
    }

    private static class AILASRListenerImpl implements AILASRListener {
        private AILASRListener lasrListener;
        private static Handler mainHandler = new Handler(Looper.getMainLooper());

        public void setLasrListener(AILASRListener LASRListener) {
            this.lasrListener = LASRListener;
        }

        @Override
        public void onInit(final int status, final String errMsg) {
            if (lasrListener != null && mainHandler != null)
                mainHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (lasrListener != null)
                                    lasrListener.onInit(status, errMsg);
                            }
                        }
                );
        }

        @Override
        public void onUploadFileProcess(final String audioFilePath, final int process) {
            if (lasrListener != null && mainHandler != null)
                mainHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (lasrListener != null)
                                    lasrListener.onUploadFileProcess(audioFilePath, process);
                            }
                        }
                );
        }

        @Override
        public void onUploadFileResult(final String audioFilePath, final String audioId, final AIError error) {
            if (lasrListener != null && mainHandler != null)
                mainHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (lasrListener != null)
                                    lasrListener.onUploadFileResult(audioFilePath, audioId, error);
                            }
                        }
                );
        }

        @Override
        public void onTaskCreate(final String audioIdOrUri, final String taskId, final AIError error) {
            if (lasrListener != null && mainHandler != null)
                mainHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (lasrListener != null)
                                    lasrListener.onTaskCreate(audioIdOrUri, taskId, error);
                            }
                        }
                );
        }

        @Override
        public void onTaskProcess(final String taskId, final int process, final AIError error) {
            if (lasrListener != null && mainHandler != null)
                mainHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (lasrListener != null)
                                    lasrListener.onTaskProcess(taskId, process, error);
                            }
                        }
                );
        }

        @Override
        public void onTaskResult(final String taskId, final String results, final AIError error) {
            if (lasrListener != null && mainHandler != null)
                mainHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                if (lasrListener != null)
                                    lasrListener.onTaskResult(taskId, results, error);
                            }
                        }
                );
        }
    }

    private static AIError getErrorMessage(int errno) {
        AIError aiError = new AIError();
        switch (errno) {
            case 1:
                aiError.setErrId(AIError.ERR_LASR_SERVER_INNER_ERR);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_INNER_ERR);
                break;
            case 2:
                aiError.setErrId(AIError.ERR_LASR_SERVER_METHOD_ERR);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_METHOD_ERR);
                break;
            case 10:
                aiError.setErrId(AIError.ERR_LASR_SERVER_PARAM_LOST);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_PARAM_LOST);
                break;
            case 11:
                aiError.setErrId(AIError.ERR_LASR_SERVER_SIGNA_ERR);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_SIGNA_ERR);
                break;
            case 12:
                aiError.setErrId(AIError.ERR_LASR_SERVER_URL_PARAM_ERR);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_URL_PARAM_ERR);
                break;
            case 13:
                aiError.setErrId(AIError.ERR_LASR_SERVER_UNKONW_PATH);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_UNKONW_PATH);
                break;
            case 101:
                aiError.setErrId(AIError.ERR_LASR_SERVER_QUERY_TASK_INFO);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_QUERY_TASK_INFO);
                break;
            case 102:
                aiError.setErrId(AIError.ERR_LASR_SERVER_CREATE_TASK_FAIL);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_CREATE_TASK_FAIL);
                break;
            case 103:
                aiError.setErrId(AIError.ERR_LASR_SERVER_UPDATE_INFO_FAILE);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_UPDATE_INFO_FAILE);
                break;
            case 201:
                aiError.setErrId(AIError.ERR_LASR_SERVER_AUDIO_DOWNLOAD_FIAL);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_AUDIO_DOWNLOAD_FIAL);
                break;
            case 202:
                aiError.setErrId(AIError.ERR_LASR_SERVER_AUDIO_CUT_FAILED);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_AUDIO_CUT_FAILED);
                break;
            case 203:
                aiError.setErrId(AIError.ERR_LASR_SERVER_AUDIO_SAVE_ERR);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_AUDIO_SAVE_ERR);
                break;
            case 211:
                aiError.setErrId(AIError.ERR_LASR_SERVER_SAVE_AUDIO);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_SAVE_AUDIO);
                break;
            case 301:
                aiError.setErrId(AIError.ERR_LASR_SERVER_POSTPROCESSING_FAIL);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_POSTPROCESSING_FAIL);
                break;
            case 302:
                aiError.setErrId(AIError.ERR_LASR_SERVER_POSTPROCESSING_TIMEOUT);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_POSTPROCESSING_TIMEOUT);
                break;
            case 501:
                aiError.setErrId(AIError.ERR_LASR_SERVER_FILE_NO_EXIST);
                aiError.setError(AIError.ERR_DESCRIPTION_LASR_SERVER_FILE_NO_EXIST);
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
            return new AIError(AIError.ERR_LASR_SERVER_AUTH_FAILED, AIError.ERR_DESCRIPTION_LASR_SERVER_AUTH_FAILED);
        } else if ("120100".equals(errId)) {
            return new AIError(AIError.ERR_LASR_SERVER_USE_UP, AIError.ERR_DESCRIPTION_LASR_SERVER_USE_UP);
        } else if ("120200".equals(errId)) {
            return new AIError(AIError.ERR_LASR_SERVER_FLOW_CONTROL, AIError.ERR_DESCRIPTION_LASR_SERVER_FLOW_CONTROL);
        } else
            return new AIError(AIError.ERR_NETWORK, String.valueOf(httpCode));
    }

}
