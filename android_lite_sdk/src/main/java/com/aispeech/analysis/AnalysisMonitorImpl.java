package com.aispeech.analysis;


import static com.aispeech.lite.AISpeechSDK.KEY_UPLOAD_ENTRY;
import static com.aispeech.lite.AISpeechSDK.UPLOAD_MODE_FORBIDDEN;
import static com.aispeech.lite.AISpeechSDK.UPLOAD_MODE_FULL;
import static com.aispeech.lite.AISpeechSDK.UPLOAD_MODE_FULL_RETRY;
import static com.aispeech.lite.AISpeechSDK.UPLOAD_MODE_SAMPLE;

import android.content.Context;
import android.text.TextUtils;

import com.aispeech.auth.AIAuthEngine;
import com.aispeech.common.AITimer;
import com.aispeech.common.Log;
import com.aispeech.gourd.EncodeCallback;
import com.aispeech.gourd.FileBuilder;
import com.aispeech.gourd.Gourd;
import com.aispeech.gourd.InitParams;
import com.aispeech.gourd.ModelBuilder;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;

import org.json.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.TimerTask;

/**
 * 通用大数据日志监控类
 * Created by yu on 2018/8/13.
 */

public class AnalysisMonitorImpl implements IAnalysisMonitor {
    private static String TAG = "AnalysisMonitorImpl";
    public static final int LOGID = 129;//master主干logID:129
    private static final int UPLOAD_DELAY_TIME_DEFAULT = 5 * 60 * 1000;//默认5分钟
    private boolean enabled = true;
    public Gourd mUploadUtil;
    private static final String PRE_TAG = "AnalysisMonitorImpl-";
    private int mUploadDelayTime = UPLOAD_DELAY_TIME_DEFAULT;
    private String mUploadMode = UPLOAD_MODE_FORBIDDEN;
    private boolean mDeleteFileWhenNetError = false;
    private String mDBName;
    private EncodeCallback mCallback;
    private int mMaxCacheNum = 100;
    private final Context context;
    private final String uploadUrl;
    private final int logID;
    private final String project;
    private final String callerType;
    private final String productId;
    private final String deviceId;
    private final String sdkVersion;
    private final boolean logcatDebugable;
    private final String logfilePath;
    private final boolean uploadImmediately;
    private final int maxCacheNum;
    private final Map<String, Object> map;
    private boolean encode = false;

    public AnalysisMonitorImpl() {
        this(new AnalysisParam.Builder()
                .setContext(AISpeech.getContext())
                .setUploadUrl(AISpeech.uploadUrl)
                .setLogID(LOGID)
                .setProject("duilite_master_monitor")
                .setCallerType("duilite")
                .setProductId(AIAuthEngine.getInstance().getProfile().getProductId())
                .setDeviceId(AIAuthEngine.getInstance().getProfile().getDeviceName())
                .setSdkVersion(AISpeechSDK.SDK_VERSION)
                .setLogcatDebugable(AISpeechSDK.LOGCAT_DEBUGABLE)
                .setLogfilePath(AISpeechSDK.LOGFILE_PATH)
                .setUploadImmediately(false)//更改成false，延时默认时间五分钟上传
                .setMaxCacheNum(100).create(), null);
    }

    public AnalysisMonitorImpl(AnalysisParam param, EncodeCallback callback) {
        this.mCallback = callback;
        this.context = param.getContext();
        this.uploadUrl = param.getUploadUrl();
        this.logID = param.getLogID();
        this.project = param.getProject();
        this.callerType = param.getCallerType();
        this.productId = param.getProductId();
        this.deviceId = param.getDeviceId();
        this.sdkVersion = param.getSdkVersion();
        this.logcatDebugable = param.isLogcatDebugable();
        this.logfilePath = param.getLogfilePath();
        this.uploadImmediately = param.isUploadImmediately();
        this.maxCacheNum = param.getMaxCacheNum();
        this.map = param.getMap();
        Log.d(TAG, "AnalysisParam " + param.toString());
    }

    @Override
    public synchronized void start() {
        if (mUploadUtil != null) {
            startRealWakeupGourdTask();
        }
    }

    @Override
    public synchronized void startUploadImmediately() {
        if (mUploadUtil != null) {
            if (realWakeupGourdTask != null) {
                realWakeupGourdTask.cancel();
                realWakeupGourdTask = null;
            }
            mUploadUtil.start();
        }
    }

    @Override
    public synchronized void stop() {
        if (mUploadUtil != null) {
            mUploadUtil.stop();
        }
    }


    private void init() {
        InitParams params = new InitParams(logID)
                .setProject(project)// 项目标识
                .setProductId(productId)// 产品ID
                .setDeviceId(deviceId)// 设备ID
                .setVersion(sdkVersion)// SDK版本
                .setCallerType(callerType)// 调用方类型
                .setCallerVersion(sdkVersion)// 调用方版本
                .setHttpUrl(uploadUrl)//设置上传大数据地址
                .setDBName(mDBName)//设置db文件名附加字段
                .setDeleFileWhenNetError(mDeleteFileWhenNetError)//设置是否网络出错后删除本地文件
                .setMaxCacheNum(maxCacheNum);// 最大缓存数据数量

        if (uploadImmediately) {
            params.setImmediately();//立即上传
        }
        if (logcatDebugable) {
            String path = logfilePath;
            try {
                File file = new File(logfilePath);
                if (file.isDirectory()) {
                    path = logfilePath + File.separator + "gourd.log";
                }
            } catch (Exception e) {
                Log.e(TAG, "logfilePath " + logfilePath + " is illegal");
            }
            params.openLog(path);
        }
        if (map != null && map.size() > 0) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                params.addEntry(entry.getKey(), entry.getValue());
            }
        }
        Log.d(TAG, "init : " + params.toString());
        if (mUploadUtil != null) {
            mUploadUtil.release();
            mUploadUtil = null;
        }
        mUploadUtil = Gourd.init(context, params, mCallback);
    }

    @Override
    public synchronized boolean init(JSONObject object) {
        if (object == null)
            return false;
        String uploadMode = object.optString("uploadMode");
        String DBName = object.optString("dBName");
        encode = object.optBoolean("encode", false);
        int maxCacheNum = 100;
        int uploadDelayTime = UPLOAD_DELAY_TIME_DEFAULT;

        switch (uploadMode) {
            case UPLOAD_MODE_FULL_RETRY:
                break;
            case UPLOAD_MODE_FULL:
                break;
            case UPLOAD_MODE_SAMPLE:
                try {
                    JSONObject paramJson = object.optJSONObject("param");
                    if (paramJson != null) {
                        maxCacheNum = paramJson.optInt("number");
                        uploadDelayTime = paramJson.getInt("period");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case UPLOAD_MODE_FORBIDDEN:
                maxCacheNum = 0;
                break;
            default:
                Log.d(TAG, "Gourd Mode is error: " + uploadMode);
                return false;
        }

        boolean same = TextUtils.equals(uploadMode, mUploadMode) && TextUtils.equals(DBName, mDBName);
        if (same)
            same = mMaxCacheNum == maxCacheNum && mUploadDelayTime == uploadDelayTime;

        if (same) {
            Log.d(TAG, "init same: " + mUploadMode);
            return false;
        }
        mUploadMode = uploadMode;
        mDBName = DBName;
        mMaxCacheNum = maxCacheNum;
        mUploadDelayTime = uploadDelayTime;

        mDeleteFileWhenNetError = UPLOAD_MODE_FULL.equals(mUploadMode) || UPLOAD_MODE_SAMPLE.equals(mUploadMode);

        TAG = PRE_TAG + mDBName;
        Log.d(TAG, "mode is: " + mUploadMode + " MaxCacheNum " + mMaxCacheNum + " delayTime " + mUploadDelayTime);

        if (mUploadUtil != null) {
            mUploadUtil.release();
            mUploadUtil = null;
        }
        if (isUploadEnable()) {
            init();
        }else {
            Log.d(TAG,"monitor upload is forbidden,will not init!");
        }
        return true;
    }

    @Override
    public synchronized void disableUpload() {
        enabled = false;
        if (mUploadUtil != null) {
            mUploadUtil.release();
            mUploadUtil = null;
        }
    }

    @Override
    public boolean isUploadEnable() {
        return !TextUtils.equals(mUploadMode, UPLOAD_MODE_FORBIDDEN);
    }


    @Override
    public synchronized void enableUpload() {
        enabled = true;
        init();
    }

    /**
     * 上传日志
     *
     * @param modelBuilder
     */
    private void cacheData(ModelBuilder modelBuilder) {
        if (mUploadUtil != null && enabled) {
            mUploadUtil.cacheData(modelBuilder);
        }
    }

    @Override
    public void cacheData(String tag, String level, String module, JSONObject input) {
        cacheData(tag, level, module, null, input, null, null);
    }

    @Override
    public void cacheData(String tag, String level, String module, String recordId, JSONObject input, JSONObject output, Map<String, Object> msgObject) {
        ModelBuilder builder = ModelBuilder.create();
        if (tag != null)
            builder.addTag(tag);
        if (level != null)
            builder.addLevel(level);
        if (module != null)
            builder.addModule(module);
        if (recordId != null)
            builder.addRecordId(recordId);
        if (input != null)
            builder.addInput(input);
        if (output != null)
            builder.addOutput(output);
        if (msgObject != null && !msgObject.isEmpty()) {
            Log.d(TAG, "msgObject: " + msgObject.toString());
            for (String key : msgObject.keySet()) {
                if (TextUtils.equals(key, KEY_UPLOAD_ENTRY)) {
                    Map<String, Object> entryMap = (Map<String, Object>) msgObject.get(key);
                    for (String entryKey : entryMap.keySet()) {
                        builder.addEntry(entryKey, entryMap.get(entryKey));
                    }
                } else {
                    builder.addMsgObject(key, msgObject.get(key));
                }
            }
        }

        cacheData(builder.build());
    }


    @Override
    public synchronized void release() {
        if (mUploadUtil != null) {
            mUploadUtil.release();
            mUploadUtil = null;
        }
    }

    /**
     * 上传文件
     *
     * @param filePath 该接口一般用不到
     */
    @Override
    public void cacheFile(String filePath) {
        if (mUploadUtil != null && enabled) {
            mUploadUtil.cacheFile(filePath);
        }
    }

    @Override
    public void cacheFileBuilder(FileBuilder builder) {
        if (mUploadUtil != null && enabled) {
            mUploadUtil.cacheFile(builder);
        }
    }


    private RealMonitorGourdTask realWakeupGourdTask = null;
    private long lastTimeMillis = 0;
    private long haveDelayTimeMillis = 0;


    private void startRealWakeupGourdTask() {
        if (mUploadDelayTime <= 0) {
            if (mUploadUtil != null)
                mUploadUtil.start();
            Log.d(TAG, "startRealMonitorGourdTask start()");
        } else {
            lastTimeMillis = System.currentTimeMillis();
            Log.d(TAG, "startRealMonitorGourdTask lastTimeMillis " + lastTimeMillis);
            if (realWakeupGourdTask == null) {
                realWakeupGourdTask = new RealMonitorGourdTask();
                long realDaley = (mUploadDelayTime - haveDelayTimeMillis) > 0 ? mUploadDelayTime - haveDelayTimeMillis : 0;
                AITimer.getInstance().schedule(realWakeupGourdTask,
                        realDaley);
                Log.d(TAG, "AITimer schedule startRealMonitorGourdTask realDaley " + realDaley);
            }
        }
    }

    private class RealMonitorGourdTask extends TimerTask {

        @Override
        public void run() {
            realWakeupGourdTask = null;
            final long diff = System.currentTimeMillis() - lastTimeMillis;
            haveDelayTimeMillis = haveDelayTimeMillis + diff;
            // 可以给与的误差
            long rang = mUploadDelayTime > 60000 ? mUploadDelayTime / 60 : (mUploadDelayTime > 10000 ? 600 : 200);
            Log.d(TAG, "haveDelayTimeMillis " + haveDelayTimeMillis + " gourd DelayTime " + mUploadDelayTime + " rang " + rang);
            if (haveDelayTimeMillis < mUploadDelayTime - rang) {
                Log.d(TAG, "restart task");
                haveDelayTimeMillis = diff; // 只保留上一次已delay的时间
                startRealWakeupGourdTask();
            } else {
                haveDelayTimeMillis = 0;
                lastTimeMillis = System.currentTimeMillis();
                if (mUploadUtil != null) {
                    mUploadUtil.start();
                    Log.d(TAG, "Gourd start");
                } else {
                    Log.d(TAG, "Gourd not start");
                }
            }
        }
    }


}
