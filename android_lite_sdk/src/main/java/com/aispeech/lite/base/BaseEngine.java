package com.aispeech.lite.base;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.export.bean.VoiceQueueStrategy;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.param.SpeechParams;

/**
 * Description: 定义为所有引擎的基类，用于下沉通用逻辑
 * Author: junlong.huang
 * CreateTime: 2023/2/15
 */
public abstract class BaseEngine {

    // 非静态 各实例单独
    protected String TAG;
    protected String tagSuffix;
    protected BaseProcessor mBaseProcessor;

    // 是否为同步初始化请求 默认都是异步
    protected boolean isInitSync = false;

    public BaseEngine() {
        initTAG();
    }

    protected void initTAG() {
        if (TextUtils.isEmpty(tagSuffix)) {
            TAG = Log.TagPrefix.ENGINE + getTag();
        } else {
            TAG = Log.TagPrefix.ENGINE + getTag() + "_" + tagSuffix;
        }
    }

    protected void parseConfig(BaseConfig config, AIEngineConfig... engineConfigs) {
        if (config == null) return;

        if (!TextUtils.isEmpty(config.getTagSuffix())) {
            this.tagSuffix = config.getTagSuffix();
            initTAG();
            for (AIEngineConfig engineConfig : engineConfigs) {
                if (engineConfig != null) engineConfig.setTag(getTag());
            }
        }

        if (config instanceof IVoiceRestrictive) {
            VoiceQueueStrategy maxVoiceQueueSize = ((IVoiceRestrictive) config).getMaxVoiceQueueSize();
            if (maxVoiceQueueSize != null) {
                Log.i(getTag(), "setVoice strategy:" + maxVoiceQueueSize.getMaxVoiceQueueSize() + "," + maxVoiceQueueSize.getIgnoreSize());
            }

            for (AIEngineConfig engineConfig : engineConfigs) {
                if (engineConfig != null) engineConfig.setVoiceQueueStrategy(maxVoiceQueueSize);
            }
        }
    }

    protected void parseIntent(Object intent, SpeechParams... speechParams) {
        if (intent == null) return;

        if (mBaseProcessor != null && intent instanceof BaseIntent) {
            BaseIntent baseIntent = (BaseIntent) intent;
            Log.d(TAG, "setNeedCopyFeedData:" + baseIntent.isNeedCopyFeedData());
            mBaseProcessor.setNeedCopyFeedData(baseIntent.isNeedCopyFeedData());

            for (SpeechParams speechParam : speechParams) {
                if (speechParam != null) {
                    speechParam.setNeedCopyResultData(baseIntent.isNeedCopyResultData());
                }
            }

        }
    }

    public abstract String getTag();

    protected void init() {
        Log.in(TAG, "init");
        isInitSync = false;
    }

    protected void initSync() {
        Log.in(TAG, "initSync");
        isInitSync = true;
        if (mBaseProcessor != null) {
            mBaseProcessor.setCallbackInMainThread(false);
        }
    }

    protected void start() {
        Log.in(TAG, "start");
    }

    protected void destroy() {
        Log.in(TAG, "destroy");
    }

    protected void stop() {
        Log.in(TAG, "stop");
    }

    protected void cancel() {
        Log.in(TAG, "cancel");
    }
}
