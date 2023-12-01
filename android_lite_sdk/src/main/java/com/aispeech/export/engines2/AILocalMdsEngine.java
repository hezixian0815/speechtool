package com.aispeech.export.engines2;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.config.AILocalMdsConfig;
import com.aispeech.export.intent.AILocalMdsIntent;
import com.aispeech.export.listeners.AILocalMdsListener;
import com.aispeech.kernel.Mds;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalMdsConfig;
import com.aispeech.lite.mds.MdsProcessor;
import com.aispeech.lite.param.MdsParams;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 多设备选择（mds，multiple device selection），用于在存在多个语音采集设备的场景中，选择最优输入设备。
 */
public class AILocalMdsEngine extends BaseEngine {

    private final LocalMdsConfig innerConfig;
    private final MdsParams innerParam;
    private final SpeechListenerImpl innerSpeechListener;
    private MdsProcessor innerProcessor;

    private AILocalMdsEngine() {
        innerConfig = new LocalMdsConfig();
        innerParam = new MdsParams();
        innerSpeechListener = new SpeechListenerImpl();
    }

    @Override
    public String getTag() {
        return "local_mds";
    }


    public static AILocalMdsEngine createInstance() { //懒汉式单例
        return new AILocalMdsEngine();
    }

    private boolean checkLibValid() {
        return Mds.isSoValid();
    }

    public synchronized void init(AILocalMdsConfig config, AILocalMdsListener listener) {
        if (!checkLibValid()) {
            if (listener != null) {
                listener.onInit(AIConstant.OPT_FAILED);
                listener.onError(new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }
        super.init();
        parseConfig(config);
        innerSpeechListener.setListener(listener);
        if (innerProcessor == null)
            innerProcessor = new MdsProcessor();
        innerProcessor.init(innerSpeechListener, innerConfig);
        mBaseProcessor = innerProcessor;
    }

    private void parseConfig(AILocalMdsConfig config) {
        if (config == null) {
            Log.e(TAG, "AILocalMdsConfig is null !");
            return;
        }
        Log.d(TAG, "AILocalMdsConfig " + config);

        innerConfig.setChannels(config.getChannels());

        // resource in assets
        final List<String> resourceInAssetsList = new ArrayList<>();

        // mdsResource
        String mdsResource = config.getMdsResource();
        if (TextUtils.isEmpty(mdsResource)) {
            Log.e(TAG, "mdsResource not found !!");
        } else if (mdsResource.startsWith("/")) {
            innerConfig.setResBinPath(mdsResource);
        } else {
            resourceInAssetsList.add(mdsResource);
            innerConfig.setResBinPath(Util.getResourceDir(innerConfig.getContext()) + File.separator + mdsResource);
        }

        innerConfig.setAssetsResNames(resourceInAssetsList.toArray(new String[resourceInAssetsList.size()]));
    }

    /**
     * 启动引擎
     *
     * @param aiLocalMdsIntent 参数
     */
    public synchronized void start(AILocalMdsIntent aiLocalMdsIntent) {
        super.start();
        parseIntent(aiLocalMdsIntent);
        if (innerProcessor != null) {
            innerProcessor.start(innerParam);
        }
    }

    /**
     * 动态设置部分参数，start之前调用，json格式的String，用于做数据扩展
     * {@code 示例传参
     * {
     * //配置输入数据流的方式,0为默认的wav/pcm格式，1为定制的音频格式
     * "input_order": 0
     * }
     * }
     *
     * @param setParams 设置参数
     */
    public synchronized void set(String setParams) {
        if (innerProcessor != null) {
            innerProcessor.set(setParams);
        }
    }

    /**
     * 用户通过此方法传入mds的算法值，决策出哪个是最优设备
     *
     * @param data float数组：每个设备的snr算法值有三个，实例：如三台设备a、b、c各有三个值 数组格式为 [a1,b1,c1,a2,b2,c2,a3,b3,c3]
     * @param num
     * @param size
     * @return 唤醒设备的索引 错误返回-1
     */
    public int mcdmFeed(float[] data, int num, int size) {
        if (innerProcessor != null) {
            return innerProcessor.mcdmFeed(data, num, size);
        }
        return -1;
    }

    /**
     * 配置输入数据流的方式
     *
     * @param order 音频流输入方式，0为wav/pcm格式，1为定制格式
     */
    public void setInputOrder(int order) {
        try {
            JSONObject object = new JSONObject();
            object.put("input_order", order);
            set(object.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseIntent(AILocalMdsIntent aiLocalMdsIntent) {
        if (aiLocalMdsIntent == null) {
            Log.e(TAG, "AILocalMdsIntent is null !");
            return;
        }
        super.parseIntent(aiLocalMdsIntent, innerParam);
        Log.d(TAG, "AILocalMdsIntent " + aiLocalMdsIntent);
        innerParam.setUseCustomFeed(aiLocalMdsIntent.isUseCustomFeed());
        innerParam.setDoa(aiLocalMdsIntent.getDoa());
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     * @param size 数据大小
     */
    public synchronized void feedData(byte[] data, int size) {
        if (!innerParam.isUseCustomFeed()) {
            Log.d(TAG, "feedData, but not UseCustomFeed");
            return;
        }
        if (innerProcessor != null) {
            innerProcessor.feedData(data, size);
        }
    }


    public synchronized void cancel() {
        super.cancel();
        if (innerProcessor != null) {
            innerProcessor.cancel();
        }
    }

    /**
     * 停止引擎
     * <p>
     * 该方法会停止接收录音数据和停止引擎，程序退出时可以调用
     * </p>
     */
    public synchronized void stop() {
        super.stop();
        if (innerProcessor != null) {
            innerProcessor.stop();
        }
    }

    /**
     * 销毁引擎
     * <p>
     * 该方法会停止录音机和销毁录音机
     * </p>
     */
    public synchronized void destroy() {
        super.destroy();
        if (innerProcessor != null) {
            innerProcessor.release();
            innerProcessor = null;
        }
        if (innerSpeechListener != null) {
            innerSpeechListener.setListener(null);
        }
    }

    private class SpeechListenerImpl extends SpeechListener {

        private AILocalMdsListener listener;

        public void setListener(AILocalMdsListener listener) {
            this.listener = listener;
        }

        @Override
        public void onInit(final int status) {
            if (listener != null)
                listener.onInit(status);
        }

        @Override
        public void onError(final AIError error) {
            if (listener != null)
                listener.onError(error);
        }

        @Override
        public void onResults(final AIResult result) {
            if (listener != null)
                listener.onResults(result);
        }
    }
}
