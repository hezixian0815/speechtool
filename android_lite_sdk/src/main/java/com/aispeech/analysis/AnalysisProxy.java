package com.aispeech.analysis;

import static com.aispeech.lite.AISpeechSDK.LOG_LOCAL_ASR;
import static com.aispeech.lite.AISpeechSDK.LOG_MONITOR;
import static com.aispeech.lite.AISpeechSDK.LOG_WK_TYPE_PREWAKEUP;
import static com.aispeech.lite.AISpeechSDK.LOG_WK_TYPE_WAKEUP;

import android.content.Context;
import android.text.TextUtils;

import com.aispeech.auth.AIAuthEngine;
import com.aispeech.auth.ProfileState;
import com.aispeech.common.AITimer;
import com.aispeech.common.Log;
import com.aispeech.common.SharedPreferencesUtil;
import com.aispeech.gourd.EncodeCallback;
import com.aispeech.gourd.FileBuilder;
import com.aispeech.kernel.Opus;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnalysisProxy {

    public static final String FILE_FORMAT_PCM = "pcm";
    public static final String FILE_FORMAT_OGG = "ogg";
    public static final String FILE_FORMAT_OPUS = "opus";
    private final static String TAG = "AnalysisProxy";
    Context mContext;
    /**
     * 本地asr音频是否压缩上传
     */
    private static volatile boolean lasrAudioEncode;
    private static volatile double lasrUploadThresh = 1;   // 本地asr上传阈值，默认为1，全量上传，采样下云端可以配置
    private static boolean ianalysisAudionNotImplement = false;
    private static boolean IAnalysisAudioLocalASRNotImplement = false;
    private static boolean IAnalysisMonitorNotImplement = false;
    private static boolean IAnalysisConfigNotImplement = false;
    private static volatile IAnalysisAudio preWkpAnalysis;
    private static volatile IAnalysisAudio wkpAnalysis;
    private static volatile IAnalysisAudio wkpAnalysisLocalASR;
    private static volatile AnalysisProxy mInstance = null;
    private static volatile IAnalysisMonitor analysisMonitor = null;
    private static volatile IAnalysisConfig analysisConfig = null;
    private static final String KEY_MDBNAME = "dBName";
    private UpdateUploadConfigTask mUpdateConfigTask = null;
    private volatile boolean configGetting = false;
    private long lastUpdateTime = 0;
    private int logIdWakeup = AnalysisAudioImpl.LOG_ID_WAKEUP;
    private int logIdLocalAsr = AnalysisAudioImpl.LOG_ID_LOCAL_ASR;
    private final AtomicBoolean useExternalIAnalysisAudio = new AtomicBoolean(false);
    private final AtomicBoolean useExternalASRAnalysisAudio = new AtomicBoolean(false);

    public void setExternalAsrAnalysisAudio(IAnalysisAudio localAsrAnalysis) {
        if (localAsrAnalysis == null)
            return;
        synchronized (useExternalASRAnalysisAudio) {
            Log.d(TAG, "setExternalAsrAnalysisAudio asrAnalysis: " + localAsrAnalysis);
            useExternalASRAnalysisAudio.set(true);
            this.wkpAnalysisLocalASR = localAsrAnalysis;
        }
    }

    public void setExternalIAnalysisAudio(IAnalysisAudio wkpAnalysis, IAnalysisAudio preWkpAnalysis) {
        if (wkpAnalysis == null && preWkpAnalysis == null)
            return;
        synchronized (useExternalIAnalysisAudio) {
            Log.d(TAG, "setExternalIAnalysisAudio wkpAnalysis: " + wkpAnalysis + " preWkpAnalysis: " + preWkpAnalysis);
            useExternalIAnalysisAudio.set(true);
            if (wkpAnalysis != null)
                this.wkpAnalysis = wkpAnalysis;
            if (preWkpAnalysis != null)
                this.preWkpAnalysis = preWkpAnalysis;
        }
    }

    public static boolean isLasrAudioEncode() {
        return lasrAudioEncode;
    }

    public static double getLocalAsrUploadThresh() {
        return lasrUploadThresh;
    }

    public static AnalysisProxy getInstance() {
        if (mInstance == null) {
            synchronized (AnalysisProxy.class) {
                if (mInstance == null) {
                    mInstance = new AnalysisProxy();
                }
            }
        }
        return mInstance;
    }

    private static IAnalysisConfig getAnalysisConfig() {
        if (analysisConfig == null && !IAnalysisConfigNotImplement) {
            synchronized (AnalysisProxy.class) {
                if (analysisConfig == null) {
                    try {
                        analysisConfig = new AnalysisConfigImpl();
                    } catch (NoClassDefFoundError error) {
                        IAnalysisConfigNotImplement = true;
                    }
                }
            }
        }

        return analysisConfig != null ? analysisConfig : IAnalysisConfig.IAnalysisConfigEmpty.getInstance();
    }


    public void updateConfig(boolean forceUpdate) {
        updateConfig(forceUpdate, null);
    }

    /**
     * 从服务器更新配置信息，鉴权成功之后必须从服务器重新获取数据，防止数据被默认配置覆盖；否则则24小时更新一次数据
     *
     * @param forceUpdate 是否强制更新数据
     */
    public synchronized void updateConfig(boolean forceUpdate, Context context) {
        if (context != null) mContext = context;
        if (AIAuthEngine.getInstance().getProfile() != null
                && AIAuthEngine.getInstance().getProfile().isProfileValid("") != null
                && AIAuthEngine.getInstance().getProfile().isProfileValid("").getAuthType() == ProfileState.AUTH_TYPE.OFFLINE) {
            return;
        }
        long diff = System.currentTimeMillis() - lastUpdateTime;
        // 大于 24 小时才会再次更新配置
        if (diff < 24 * 60 * 60 * 1000 && !forceUpdate)
            return;
        if (configGetting)
            return;
        configGetting = true;
        Log.d(TAG, "start updateConfig");
        if (mUpdateConfigTask != null) {
            mUpdateConfigTask.cancel();
            mUpdateConfigTask = null;
        }
        mUpdateConfigTask = new UpdateUploadConfigTask();
        AITimer.getInstance().schedule(mUpdateConfigTask, 0);
    }


    public void scheduleTask() {

    }


    public IAnalysisAudio getAnalysisAudio(int wkType) {
        if (!ianalysisAudionNotImplement) {
            try {
                if (wkType == LOG_WK_TYPE_WAKEUP) {
                    if (wkpAnalysis == null) {
                        synchronized (this) {
                            if (wkpAnalysis == null) {
                                wkpAnalysis = new AnalysisAudioImpl(new AnalysisParam.Builder()
                                        .setContext(AISpeech.getContext())
                                        .setUploadUrl(AISpeech.uploadUrl)
                                        .setLogID(logIdWakeup)
                                        .setProject("duilite_master_audio")
                                        .setCallerType("duilite")
                                        .setProductId(AIAuthEngine.getInstance().getProfile().getProductId())
                                        .setDeviceId(AIAuthEngine.getInstance().getProfile().getDeviceName())
                                        .setSdkVersion(AISpeechSDK.SDK_VERSION)
                                        .setLogcatDebugable(AISpeechSDK.LOGCAT_DEBUGABLE)
                                        .setLogfilePath(AISpeechSDK.LOGFILE_PATH)
                                        .setUploadImmediately(false)
                                        .setMaxCacheNum(100).create(), null);
                            }
                        }
                    }
                    return wkpAnalysis;
                } else if (wkType == LOG_WK_TYPE_PREWAKEUP) {
                    if (preWkpAnalysis == null) {
                        synchronized (this) {
                            if (preWkpAnalysis == null) {
                                preWkpAnalysis = new AnalysisAudioImpl(new AnalysisParam.Builder()
                                        .setContext(AISpeech.getContext())
                                        .setUploadUrl(AISpeech.uploadUrl)
                                        .setLogID(logIdWakeup)
                                        .setProject("duilite_master_audio")
                                        .setCallerType("duilite")
                                        .setProductId(AIAuthEngine.getInstance().getProfile().getProductId())
                                        .setDeviceId(AIAuthEngine.getInstance().getProfile().getDeviceName())
                                        .setSdkVersion(AISpeechSDK.SDK_VERSION)
                                        .setLogcatDebugable(AISpeechSDK.LOGCAT_DEBUGABLE)
                                        .setLogfilePath(AISpeechSDK.LOGFILE_PATH)
                                        .setUploadImmediately(false)
                                        .setMaxCacheNum(100).create(), null);
                            }
                        }
                    }
                    return preWkpAnalysis;
                } else {
                    Log.d(TAG, "wkType invalid");
                }
            } catch (NoClassDefFoundError e) {
                ianalysisAudionNotImplement = true;
            }
        }

        return IAnalysisAudio.AnalysisAudioEmpty.getInstance();
    }

    public IAnalysisAudio getAnalysisAudioLocalASR() {
        if (!IAnalysisAudioLocalASRNotImplement) {
            try {
                if (wkpAnalysisLocalASR == null) {
                    synchronized (this) {
                        if (wkpAnalysisLocalASR == null) {
                            final Opus opus;
                            final byte[] opusBuffer;
                            if (Opus.isSoValid()) {
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
                                opusBuffer = new byte[3200];
                                String opusParam = jsonObject.toString();
                                opus = new Opus();
                                opus.ddsInit(false, Opus.PCM_TO_OPUS, opusParam);
                            } else {
                                opus = null;
                                opusBuffer = null;
                            }
                            wkpAnalysisLocalASR = new AnalysisAudioImpl(new AnalysisParam.Builder()
                                    .setContext(AISpeech.getContext())
                                    .setUploadUrl(AISpeech.uploadUrl)
                                    .setLogID(logIdLocalAsr)
                                    .setProject("duilite_master_audio")
                                    .setCallerType("duilite")
                                    .setProductId(AIAuthEngine.getInstance().getProfile().getProductId())
                                    .setDeviceId(AIAuthEngine.getInstance().getProfile().getDeviceName())
                                    .setSdkVersion(AISpeechSDK.SDK_VERSION)
                                    .setLogcatDebugable(AISpeechSDK.LOGCAT_DEBUGABLE)
                                    .setLogfilePath(AISpeechSDK.LOGFILE_PATH)
                                    .setUploadImmediately(false)
                                    .setMaxCacheNum(100).create(), new EncodeCallback() {
                                @Override
                                public void onStart(FileBuilder fileBuilder) {
                                    if (!lasrAudioEncode || opus == null)
                                        fileBuilder.setEncode(FILE_FORMAT_PCM);
                                }

                                @Override
                                public byte[] onEncode(byte[] bytes, FileBuilder fileBuilder) {
                                    if (!lasrAudioEncode || opus == null)
                                        return bytes;
                                    int len = opus.ddsFeed(bytes, bytes.length, opusBuffer);
                                    byte[] sendByte = new byte[len];
                                    System.arraycopy(opusBuffer, 0, sendByte, 0, len);
                                    return sendByte;
                                }

                                @Override
                                public List<byte[]> onStop(FileBuilder fileBuilder) {
                                    if (!lasrAudioEncode || opus == null)
                                        return null;
                                    int times = 6; //最多循环6次
                                    List<byte[]> byteList = new ArrayList<>();
                                    while (times > 0) {
                                        int len = opus.ddsFeed(new byte[0], 0, opusBuffer);
                                        if (len != 0) {
                                            byte[] sendByte = new byte[len];
                                            System.arraycopy(opusBuffer, 0, sendByte, 0, len);
                                            byteList.add(sendByte);
                                            times--;
                                        } else {
                                            break;
                                        }
                                    }
                                    Log.d(TAG, "EncodeCallback onStop " + (6 - times + 1));
                                    return byteList;
                                }
                            });
                        }
                    }
                }
                return wkpAnalysisLocalASR;
            } catch (NoClassDefFoundError e) {
                IAnalysisAudioLocalASRNotImplement = true;
            }
        }

        return IAnalysisAudio.AnalysisAudioEmpty.getInstance();
    }

    public IAnalysisMonitor getAnalysisMonitor() {
        if (analysisMonitor == null && !IAnalysisMonitorNotImplement) {
            synchronized (this) {
                if (analysisMonitor == null) {
                    try {
                        analysisMonitor = new AnalysisMonitorImpl();
                    } catch (NoClassDefFoundError e) {
                        IAnalysisMonitorNotImplement = true;
                    }
                }
            }
        }

        return analysisMonitor != null ? analysisMonitor : IAnalysisMonitor.IAnalysisMonitorEmpty.getInstance();
    }

    class UpdateUploadConfigTask extends TimerTask {

        /**
         * <pre>
         * {
         * 	"code": 200,
         * 	"msg": "",
         * 	"data": [
         *                {
         * 			"deviceId": "ca1e1434243759365f9614b5198dbc98",
         * 			"logId": "136",
         * 			"preWakeUP": {
         * 				"uploadMode": "full-retry"
         *            },
         * 			"productId": "278582820",
         * 			"wakeUP": {
         * 				"uploadMode": "full-retry"
         *            }
         *        },
         *        {
         * 			"deviceId": "ca1e1434243759365f9614b5198dbc98",
         * 			"localASR": {
         * 				"param": {
         * 					"timeUnit": "ms",
         * 					"number": 10,
         * 					"period": 60000
         *                },
         * 				"uploadMode": "sample"
         *            },
         * 			"logId": "280",
         * 			"productId": "278582820"
         *        }
         * 	],
         * 	"isCloud": true
         * }
         * </pre>
         */
        @Override
        public void run() {
            String config = null;
            try {
                // 同步方法，最多5秒
                config = getAnalysisConfig().getUploadConfig(AISpeech.getContext(),
                        new int[]{AnalysisAudioImpl.LOG_ID_WAKEUP, AnalysisAudioImpl.LOG_ID_LOCAL_ASR, AnalysisMonitorImpl.LOGID},
                        AIAuthEngine.getInstance().getProfile().getProductId(),
                        AIAuthEngine.getInstance().getProfile().getDeviceName()
                );
            } catch (Throwable t) {
            }
            // config = "{\"code\":200,\"msg\":\"\",\"data\":[{\"deviceId\":\"ca1e1434243759365f9614b5198dbc98\",\"logId\":\"136\",\"preWakeUP\":{\"uploadMode\":\"full-retry\"},\"productId\":\"278582820\",\"wakeUP\":{\"uploadMode\":\"full-retry\"}},{\"deviceId\":\"ca1e1434243759365f9614b5198dbc98\",\"localASR\":{\"param\":{\"timeUnit\":\"ms\",\"number\":10,\"period\":60000},\"uploadMode\":\"sample\"},\"logId\":\"280\",\"productId\":\"278582820\"}],\"isCloud\":true}"
            Log.d(TAG, "config: " + config);
            JSONObject preWakeUpJson = null;
            JSONObject wakeUpJson = null;
            JSONObject localASRJson = null;
            JSONObject monitorJson = null;
            boolean isCloud = false;
            if (!TextUtils.isEmpty(config)) {
                try {
                    JSONObject configJson = new JSONObject(config);
                    isCloud = configJson.getBoolean("isCloud");
                    JSONArray dataJson = configJson.getJSONArray("data");
                    for (int i = 0; i < dataJson.length(); i++) {
                        JSONObject cfgJson = dataJson.getJSONObject(i);
                        if (cfgJson == null) return;
                        if (cfgJson.has("config_interval") && mContext != null) {
                            int configInterval = cfgJson.optInt("config_interval") * 1000;
                            SharedPreferencesUtil.putInt(mContext, SharedPreferencesUtil.SDK_INIT_UPDATE_INTERVAL, configInterval);
                        }
                        if (cfgJson.getInt("logId") == AnalysisAudioImpl.LOG_ID_WAKEUP) {
                            preWakeUpJson = cfgJson.optJSONObject("preWakeUp");
                            wakeUpJson = cfgJson.optJSONObject("wakeUp");
                            if (preWakeUpJson != null) {
                                preWakeUpJson.put(KEY_MDBNAME, String.valueOf(LOG_WK_TYPE_PREWAKEUP));
                                Log.d(TAG, "preWakeUpJson is : " + preWakeUpJson.toString());
                            }
                            if (wakeUpJson != null) {
                                wakeUpJson.put(KEY_MDBNAME, String.valueOf(LOG_WK_TYPE_WAKEUP));
                                Log.d(TAG, "wakeUpJson is : " + wakeUpJson.toString());
                            }
                        } else if (cfgJson.getInt("logId") == AnalysisAudioImpl.LOG_ID_LOCAL_ASR) {
                            localASRJson = cfgJson.optJSONObject("localASR");
                            if (localASRJson != null) {
                                localASRJson.put(KEY_MDBNAME, String.valueOf(LOG_LOCAL_ASR));
                                Log.d(TAG, "localASRJson is : " + localASRJson);

                                JSONObject param = localASRJson.optJSONObject("param");
                                if (param != null) {
                                    lasrUploadThresh = param.optDouble("uploadThresh", 1);
                                }
                            }
                        } else if (cfgJson.getInt("logId") == AnalysisMonitorImpl.LOGID) {
                            monitorJson = cfgJson.optJSONObject("wakeUp");
                            if (monitorJson != null) {
                                monitorJson.put(KEY_MDBNAME, String.valueOf(LOG_MONITOR));
                                Log.d(TAG, "monitorJson is : " + monitorJson.toString());
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            configGetting = false;
            boolean initPreWakeup = getAnalysisAudio(LOG_WK_TYPE_PREWAKEUP).init(preWakeUpJson);
            boolean initWakeup = getAnalysisAudio(LOG_WK_TYPE_WAKEUP).init(wakeUpJson);
            boolean initAnalysisAudioLocalASR = getAnalysisAudioLocalASR().init(localASRJson);
            boolean initAnalysisMonitor = getAnalysisMonitor().init(monitorJson);
            // 本地asr上传压缩的音频，首先需要 opus 库
            lasrAudioEncode = Opus.isSoValid() && getAnalysisAudioLocalASR().isEncode();
            Log.d(TAG, "initAnalysisAudioLocalASR " + initAnalysisAudioLocalASR);
            if (initPreWakeup || initWakeup || initAnalysisAudioLocalASR || initAnalysisMonitor || isCloud)
                lastUpdateTime = System.currentTimeMillis();
            Log.d(TAG, "initPreWakeup: " + initPreWakeup + "   initWakeup: " + initWakeup + "   isCloud: " + isCloud
                    + "   initAnalysisAudioLocalASR: " + initAnalysisAudioLocalASR
                    + "   initAnalysisMonitor: " + initAnalysisMonitor
            );
        }
    }
}
