/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.param;

import android.text.TextUtils;

import com.aispeech.base.IFespxEngine;
import com.aispeech.common.FileUtils;
import com.aispeech.common.JSONUtil;
import com.aispeech.lite.AISampleRate;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.Engines;
import com.aispeech.lite.oneshot.OneshotCache;

import org.json.JSONObject;

/**
 * 公用语音参数封装类<br>
 * <br>
 * <b>具体业务请使用子类:</b>
 * <ul>
 * <li>{@link CloudASRParams CloudASRParams}语音识别参数</li>
 * </ul>
 * <p/>
 * 包含参数:<br>
 * <table border="1">
 * <tr>
 * <th>服务器参数</th>
 * <th>{@link CommonParams serverParams}</th>
 * </tr>
 * <tr>
 * <th>应用配置参数</th>
 * <th>{@link AppParams appParams}</th>
 * </tr>
 * <tr>
 * <th>音频参数</th>
 * <th>{@link AudioParams audioParams}</th>
 * </tr>
 * <tr>
 * <th>会话请求参数</th>
 * <th>{@link BaseRequestParams baseRequestParmas}需要在派生类中实现</th>
 * </tr>
 * </table>
 * </br> 参数设置方法列表:
 * <ul>
 * <li>设置无语音超时时长 {@link #setNoSpeechTimeout(int)}</li>
 * <li>设置录音时长上限 {@link #setMaxSpeechTimeS(int)}</li>
 * <li>设置采样率 {@link #setSampleRate(AISampleRate)}</li>
 * </ul>
 */
public class SpeechParams extends BaseRequestParams implements Cloneable {

    public final static String TAG = SpeechParams.class.getCanonicalName();

    private final static String aiType_key = "aiType";
    private final static String topic_key = "topic";
    private final static String recordId_key = "recordId";
    private final static String sessionId_key = "sessionId";
    private final static String PRODUCTID = "productId";
    private final static String USERID = "userId";
    private final static String DEVICENAME = "deviceName";
    private final static String SDKNAME = "sdkName";
    private final static String CONTEXT = "context";
    private final static String REQUEST = "request";
    private final static int DEFAULT_TIMEOUT_MS = 5000; // 5s
    private final static int MAX_SPEECH_SEC = 60;

    private static final String CLOUD_ASR_AUDIO_DIR = "cloudAsr";
    private static final String CLOUD_TTS_AUDIO_DIR = "cloudTts";
    private static final String CLOUD_SEMANTIC_AUDIO_DIR = "cloudsSemantic";
    private static final String LOCAL_ASR_AUDIO_DIR = "localAsr";
    private static final String VPRINT_CUT_AUDIO_DIR = "vprintCut";
    private static final String VAD_AUDIO_DIR = "vad";
    private static final String FESP_CAR_AUDIO_DIR = "fespCar";
    private static final String DMASP_CAR_AUDIO_DIR = "dmaspCar";
    private static final String WAKEUP_AUDIO_DIR = "wakeup";

    // 设置使用自定义接口流式传入数据
    private boolean useCustomFeed = false;
    protected volatile boolean needCopyResultData = true; // 是否需要抛出的数据，默认拷贝

    private AudioParams audioParams;

    private byte[] data;

    // 录音最大录音时长
    private int maxSpeechTimeS = MAX_SPEECH_SEC;
    // 无语音超时时间
    private int noSpeechTimeout = DEFAULT_TIMEOUT_MS;

    // 是否需要添加音频编码参数
    private boolean isAttachAudioParam = true;

    private IFespxEngine fespxEngine;


    public IFespxEngine getFespxEngine() {
        return fespxEngine;
    }

    public void setFespxEngine(IFespxEngine fespxEngine) {
        this.fespxEngine = fespxEngine;
    }


    private long wakeupTime = 0;
    private int intervalTime = 600;
    private boolean isUseOneShot = false;

    private String userId = "";
    private String deviceId = "";
    private String productId = "";
    private String server = AISpeechSDK.CLOUD_SERVER;
    private String aliasKey = AISpeechSDK.ALIAS_KEY;
    private String aiType = "asr";
    private String topic = "recorder.stream.start";
    private String recordId = "";
    private String sessionId = "";
    private String saveAudioPath = "";
    private int waitingTimeout = 5000;//等待结果超时，单位ms

    private String dumpAudioPath;//dum音频保存路径
    private int dumpTime = 5000;//dump音频时长

    private OneshotCache<byte[]> oneshotCache;//oneshot cache音频

    /**
     * 自行feed数据的时候，是否需要对数据进行拷贝，默认会进行拷贝
     */
    protected boolean needCopyFeedData = true;

    public boolean isNeedCopyFeedData() {
        return needCopyFeedData;
    }

    public void setNeedCopyFeedData(boolean needCopyFeedData) {
        this.needCopyFeedData = needCopyFeedData;
    }

    public SpeechParams() {
        audioParams = new AudioParams();
        setTag(TAG);
    }

    public String getTag() {
        return tag;
    }


    public int getWaitingTimeout() {
        return waitingTimeout;
    }


    public String getDumpAudioPath() {
        if (TextUtils.isEmpty(dumpAudioPath)) {
            if (!TextUtils.isEmpty(AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH)) {
                return AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH + "/wakeupDump";
            }
        }
        return dumpAudioPath;
    }

    public void setDumpAudioPath(String dumpAudioPath) {
        this.dumpAudioPath = dumpAudioPath;
    }

    public int getDumpTime() {
        return dumpTime;
    }

    public void setDumpTime(int dumpTime) {
        this.dumpTime = dumpTime;
    }

    /**
     * 设置等待识别结果超时时长
     *
     * @param waitingTimeout
     */
    public void setWaitingTimeout(int waitingTimeout) {
        this.waitingTimeout = waitingTimeout;
    }

    /**
     * 设置音频最大录音时长，达到该值将取消语音引擎并抛出异常<br>
     *
     * @param sec 允许的最大录音时长 单位秒
     *            <ul>
     *            <li>0 表示无最大录音时长限制</li>
     *            <li>默认大小为60S</li>
     *            </ul>
     */
    public void setMaxSpeechTimeS(int sec) {
        this.maxSpeechTimeS = sec;
    }

    public int getMaxSpeechTimeS() {
        return maxSpeechTimeS;
    }

    /**
     * {@link #setNoSpeechTimeout(int)}
     *
     * @return noSpeechTimeout配置值
     */
    public int getNoSpeechTimeout() {
        return noSpeechTimeout;
    }

    /**
     * 设置无语音超时时长，单位毫秒，默认值为{@link #DEFAULT_TIMEOUT_MS}
     * ；如果达到该设置值时，自动停止录音并放弃请求内核(即调用
     *
     * @param noSpeechTimeout 超时时长，单位毫秒
     */
    public void setNoSpeechTimeout(int noSpeechTimeout) {
        this.noSpeechTimeout = noSpeechTimeout;
    }

    /**
     * 设置音频类型
     *
     * @param audioType 音频类型
     */
    public void setAudioType(String audioType) {
        audioParams.setAudioType(audioType);
    }


    /**
     * 设置语音音频采样率
     *
     * @param sampleRate 音频采样率，目前仅支持{@link AISampleRate#SAMPLE_RATE_16K}和
     *                   {@link AISampleRate#SAMPLE_RATE_8K}
     * @see AISampleRate
     */
    public void setSampleRate(AISampleRate sampleRate) {
        audioParams.setSampleRate(sampleRate);
    }

    /**
     * 获取语音音频采样率
     *
     * @return 音频采样率，目前仅支持{@link AISampleRate#SAMPLE_RATE_16K}和
     * {@link AISampleRate#SAMPLE_RATE_8K}
     * @see AISampleRate
     */
    public AISampleRate getSampleRate() {
        return audioParams.getSampleRate();
    }


    /**
     * \cond PRIVATE 获得录音机返回数据时间间隔
     *
     * @return \endcond
     */
    public int getIntervalTime() {
        return audioParams.getIntervalTime();
    }

    /**
     * \cond PRIVATE 设置录音机抛出数据时间间隔
     *
     * @param interval 时间间隔，单位ms \endcond
     */
    public void setIntervalTime(int interval) {
        audioParams.setIntervalTime(interval);
    }


    /**
     * 是否再本次参数请求中附上音频编码参数
     *
     * @param isAttachAudioParam
     */
    public void setIsAttachAudioParam(boolean isAttachAudioParam) {
        this.isAttachAudioParam = isAttachAudioParam;
    }

    public void setWakeupTime(long time) {
        this.wakeupTime = time;
    }

    public long getWakeupTime() {
        return this.wakeupTime;
    }

    public void setOneShotIntervalTime(int intervalTime) {
        this.intervalTime = intervalTime;
    }

    public int getOneShotIntervalTime() {
        return this.intervalTime;
    }

    public void setUseOneShotFunction(boolean useOneShot) {
        this.isUseOneShot = useOneShot;
    }

    public boolean isUseOneShot() {
        return this.isUseOneShot;
    }

    public boolean isNeedCopyResultData() {
        return needCopyResultData;
    }

    public void setNeedCopyResultData(boolean needCopyResultData) {
        this.needCopyResultData = needCopyResultData;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }


    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public void setAilasKey(String ailasKey) {
        this.aliasKey = ailasKey;
    }

    public String getAilasKey() {
        return aliasKey;
    }

    public String getAiType() {
        return aiType;
    }

    public void setAiType(String aiType) {
        this.aiType = aiType;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 1.全局音频保存开关关闭时，不保存音频
     * 2.全局音频保存开关开启时，保存音频，音频路径优先来源于单引擎设置的路径，其次是GLOBAL_AUDIO_SAVE_PATH中设置的路径
     * 3.全局音频保存开关开启时，且设置了多引擎保存引擎GLOBAL_MULTIPLE_ENGINES_AUDIO_SAVE，则只保存设置中的引擎的音频
     *
     * @return
     */
    public synchronized String getSaveAudioPath() {
        // 音频保存是否开启
        if (!AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE) {
            return null;
        }
        String audioPath = "";
        int currentType = 0;
        if (this instanceof LocalAsrParams) {
            audioPath = LOCAL_ASR_AUDIO_DIR;
            currentType = Engines.LOCAL_ASR;
        }
        if (this instanceof VadParams) {
            audioPath = VAD_AUDIO_DIR;
            currentType = Engines.VAD;
        }
        if (this instanceof CloudASRParams) {
            audioPath = CLOUD_ASR_AUDIO_DIR;
            currentType = Engines.CLOUD_ASR;
        }
        if (this instanceof CloudSemanticParams) {
            audioPath = CLOUD_SEMANTIC_AUDIO_DIR;
            currentType = Engines.CLOUD_SEMANTIC;
        }
        if (this instanceof CloudTtsParams) {
            audioPath = CLOUD_TTS_AUDIO_DIR;
            currentType = Engines.CLOUD_TTS;
        }
        if (this instanceof VprintParams) {
            audioPath = VPRINT_CUT_AUDIO_DIR;
            currentType = Engines.VPRINT;
        }
        if (this instanceof FespCarParams || this instanceof FespxCarParams || this instanceof SignalProcessingParams) {
            audioPath = FESP_CAR_AUDIO_DIR;
            currentType = Engines.FESP;
        }
        if (this instanceof DmaspParams) {
            audioPath = DMASP_CAR_AUDIO_DIR;
            currentType = Engines.DMASP;
        }
        if (this instanceof WakeupParams) {
            audioPath = WAKEUP_AUDIO_DIR;
            currentType = Engines.WAKEUP;
        }
        //优先保存在GLOBAL_AUDIO_SAVE_PATH中设置的路径
        if (!TextUtils.isEmpty(AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH)) {
            saveAudioPath = AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH;
            if (!saveAudioPath.endsWith("/")) {
                saveAudioPath += "/";
            }
            saveAudioPath += audioPath;
        }
        //只保存 GLOBAL_AUDIO_SAVE_ENGINE_TYPE 设置中的引擎的音频
        if (!Engines.isSavingEngineAudioEnable(currentType)) {
            saveAudioPath = "";
        }
        FileUtils.createOrExistsDir(saveAudioPath);
        return saveAudioPath;
    }

    /**
     * @param saveAudioPath
     */

    public synchronized void setSaveAudioPath(String saveAudioPath) {
//        Log.w(TAG, "setSaveAudioPath is deprecated,please config global save path");
        this.saveAudioPath = saveAudioPath;
    }

    public boolean isUseCustomFeed() {
        return useCustomFeed;
    }

    public void setUseCustomFeed(boolean useCustomFeed) {
        this.useCustomFeed = useCustomFeed;
    }

    public OneshotCache<byte[]> getOneshotCache() {
        return oneshotCache;
    }

    public void setOneshotCache(OneshotCache<byte[]> oneshotCache) {
        this.oneshotCache = oneshotCache;
    }

    /**
     * 封装相关参数为JSON格式并返回
     *
     * @return JSON格式的参数配置
     */
    @SuppressWarnings("unchecked")
    public JSONObject toJSON() {
        JSONObject JSON = new JSONObject();
        if (isAttachAudioParam) {
            JSONUtil.putQuietly(JSON, AudioParams.KEY_AUDIO, audioParams.toJSON());
        }
        JSONObject context = new JSONObject();
        JSONUtil.putQuietly(context, PRODUCTID, productId);
        JSONUtil.putQuietly(context, USERID, userId);
        JSONUtil.putQuietly(context, DEVICENAME, deviceId);
        JSONUtil.putQuietly(context, SDKNAME, AISpeechSDK.SDK_VERSION);
        JSONUtil.putQuietly(JSON, CONTEXT, context.toString());
        JSONObject request = new JSONObject();
        if (isAttachAudioParam) {
            JSONUtil.putQuietly(request, AudioParams.KEY_AUDIO, audioParams.toJSON());
        }
        JSONUtil.putQuietly(JSON, REQUEST, context.toString());

//        JSONUtil.putQuietly(JSON, aiType_key, aiType);
//        JSONUtil.putQuietly(JSON, topic_key, topic);
        JSONUtil.putQuietly(JSON, recordId_key, recordId);
//        JSONUtil.putQuietly(JSON, sessionId_key, sessionId);
        return JSON;
    }

    @Override
    public String toString() {
        return toJSON().toString();
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
