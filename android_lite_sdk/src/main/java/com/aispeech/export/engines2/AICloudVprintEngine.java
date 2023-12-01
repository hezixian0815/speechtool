package com.aispeech.export.engines2;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.aispeech.auth.AIAuthEngine;
import com.aispeech.common.Log;
import com.aispeech.common.ThreadNameUtil;
import com.aispeech.common.WavFileWriter;
import com.aispeech.export.config.AICloudVprintConfig;
import com.aispeech.export.intent.AICloudVprintIntent;
import com.aispeech.export.listeners.AICloudVprintListener;
import com.aispeech.lite.AISampleRate;
import com.aispeech.lite.AIThreadFactory;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.vprint.CloudVprintKernel;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 在提交同一个用户的声纹采样时：
 * <p>
 * 文本无关声纹
 * 每次声纹采样可以是任意的音频，直到注册成功或者失败。
 * 文本相关（半相关）
 * 每次声纹采样需要用户念诵指定文本的语音，每次需要念诵的文本包含在上一次调用开始注册声纹或注册声纹接口的返回结果中（即content参数）。注册成功或失败时，返回结果中不包含语音文本。
 * 关于声纹内容的限制：
 * </p>
 * 声纹样本的位数必须为16bit，使用单声道采样。
 * 声纹样本：文本无关：8k采样率的wav文件;文本半相关：16k采样率的wav文件。
 */
public class AICloudVprintEngine extends BaseEngine {

    private CloudVprintKernel cloudVprintKernel;

    public static AICloudVprintEngine createInstance() {
        return new AICloudVprintEngine();
    }

    private AICloudVprintEngine() {
        cloudVprintKernel = new CloudVprintKernel();
    }

    @Override
    public String getTag() {
        return "cloud_vprint";
    }

    public synchronized void init(AICloudVprintConfig config, AICloudVprintListener listener) {
        super.init();
        cloudVprintKernel.init(config.getMode(), config.getHost(), AIAuthEngine.getInstance().getProfile(), listener);
    }

    /**
     * 1. 注册声纹
     *
     * @param intent 注册配置参数
     */
    public void register(AICloudVprintIntent intent) {
        cloudVprintKernel.register(intent);
    }

    /**
     * 2. 验证声纹
     *
     * @param intent 配置验证参数
     */
    public void verifyHttp(AICloudVprintIntent intent) {
        cloudVprintKernel.verifyHttp(intent);
    }

    /**
     * 通过webSocket连接验证声纹，一般用于云端声纹实时验证
     * {@link AICloudVprintConfig.Mode#TEXT_NO_RELATED_SHORT_TIME}
     *
     * @param intent 验证声纹参数
     */
    public void verifyStartWS(AICloudVprintIntent intent) {
        cloudVprintKernel.verifyStartWS(intent);
    }

    /**
     * 通过webSocket送音频数据给云端声纹，一般用于云端声纹实时验证
     * {@link AICloudVprintConfig.Mode#TEXT_NO_RELATED_SHORT_TIME}
     *
     * @param data 音频数据
     * @param size 音频数据大小
     */
    public void verifyFeedWS(byte[] data, int size) {
        cloudVprintKernel.verifyFeedWS(data, size);
    }

    /**
     * 通过webSocket停止声纹验证，一般用于云端声纹实时验证
     * {@link AICloudVprintConfig.Mode#TEXT_NO_RELATED_SHORT_TIME}
     */
    public void verifyStopWS() {
        cloudVprintKernel.verifyStopWS();
    }

    /**
     * 5. 注销声纹
     *
     * @param userId       用户ID，用户ID在您的系统里应该是唯一的
     * @param organization 非必须，可填null。用户所在的公司，项目
     */
    @Deprecated
    public void unregister(String userId, String organization) {
        AICloudVprintIntent intent = new AICloudVprintIntent();
        intent.setUserId(userId);
        intent.setOrganization(organization);
        cloudVprintKernel.unregister(intent);
    }

    public void unregister(AICloudVprintIntent intent) {
        cloudVprintKernel.unregister(intent);
    }

    /***
     * 设置生成音频路径
     * @param path 音频路径
     */
    public void setVprintAudioPath(String path) {
        cloudVprintKernel.setVprintAudioPath(path);
    }

    /**
     * 结束喂音频
     */
    public void stopFeedData() {
        cloudVprintKernel.stopFeedData();
    }

    /**
     * 查询信息
     *
     * @param intent 需要设置声纹类型，
     *               intent.setMode(AICloudVprintConfig.Mode mode)
     *               intent.setUserId(String userId)
     *               intent.setOrganization(String organization)
     *               intent.setGroupId(String groupId)
     */
    public void query(AICloudVprintIntent intent) {
        if (intent == null) {
            Log.e(TAG, "getUsersInfo intent is null");
        }
        cloudVprintKernel.query(intent);
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     */
    public void startFeedData(byte[] data) {
        if (cloudVprintKernel != null) {
            cloudVprintKernel.startFeedData(data, data.length);
        }
    }

    /**
     * 销毁，释放资源
     */
    public synchronized void destroy() {
        super.destroy();
        cloudVprintKernel.destroy();
    }

    /**
     * 录制wav音频文件的工具类
     */
    public static class AudioTool {

        private String TAG = "AICloudVprintEngine";
        private AudioRecord recorder = null;
        private boolean recording = false;
        private ExecutorService mPool;
        private int lastSampleRate;
        private static MyAudioToolListener listener = new MyAudioToolListener();

        public AudioTool() {
        }

        public synchronized void startRecord(AICloudVprintConfig.Mode mode, String filepath, final AICloudVprintListener.AudioToolListener audioToolListener) {
            listener.setListener(audioToolListener);
            if (TextUtils.isEmpty(filepath) || recording) {
                if (listener != null)
                    listener.onError(-1, "AudioRecord is recording");
                return;
            }
            final int sampleRate = mode == null ? 16000 : mode.getSupportSampleRate();
            if (lastSampleRate != sampleRate)
                releaseRecord();
            lastSampleRate = sampleRate;
            if (recorder == null)
                recorder = new AudioRecord(0, sampleRate,
                        AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT, 192000);
            recorder.startRecording();
            int recordingState = recorder.getRecordingState();
            if (recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                String log = "startRecording fail: " + recordingState;
                Log.d(TAG, log);
                if (listener != null)
                    listener.onError(-1, "startRecording fail, recordingState is " + recordingState);
                return;
            }
            final File wavFile = new File(filepath);
            final WavFileWriter fileWriter = WavFileWriter.createWavFileWriter(wavFile,
                    AISampleRate.toAISampleRate(sampleRate), AudioFormat.CHANNEL_OUT_DEFAULT,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (mPool == null) {
                String name = ThreadNameUtil.getFixedThreadName(TAG) + "-recorder";
                mPool = Executors.newSingleThreadExecutor(AIThreadFactory.newSimpleNameFactory(name));
            }

            recording = true;
            mPool.execute(new Runnable() {
                @Override
                public void run() {
                    int read_buffer_size = sampleRate * 1 * AudioFormat.ENCODING_PCM_16BIT * 100 / 1000;
                    byte[] readBuffer = new byte[read_buffer_size];
                    int readSize;
                    if (listener != null)
                        listener.onRecordStart();
                    while (true) {
                        readSize = 0;
                        if (!recording)
                            break;
                        try {
                            if (recorder != null)
                                readSize = recorder.read(readBuffer, 0, read_buffer_size);
                        } catch (Exception e) {
                        }
                        if (readSize > 0) {
                            byte[] bytes = new byte[readSize];
                            System.arraycopy(readBuffer, 0, bytes, 0, readSize);
                            fileWriter.write(bytes);
                        }
                    }
                    fileWriter.close();
                    if (listener != null) {
                        listener.onRecordStop();
                    }
                }
            });
        }

        public synchronized void stopRecord() {
            recording = false;
            if (recorder != null && recorder.getState() == AudioRecord.RECORDSTATE_RECORDING) {
                recorder.stop();
            }
        }

        public synchronized void releaseRecord() {
            recording = false;
            if (recorder != null) {
                if (recorder.getState() == AudioRecord.RECORDSTATE_RECORDING)
                    recorder.stop();
                recorder.release();
                recorder = null;
            }
        }

        public synchronized void destroy() {
            releaseRecord();
            if (mPool != null) {
                mPool.shutdown();
            }
            listener.setListener(null);
        }
    }

    private static class MyAudioToolListener implements AICloudVprintListener.AudioToolListener {
        private AICloudVprintListener.AudioToolListener listener;
        private static Handler mainHandler = new Handler(Looper.getMainLooper());

        public void setListener(AICloudVprintListener.AudioToolListener listener) {
            this.listener = listener;
        }

        @Override
        public void onError(final int state, final String err) {
            if (listener != null)
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null)
                            listener.onError(state, err);
                    }
                });
        }

        @Override
        public void onRecordStart() {
            if (listener != null)
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null)
                            listener.onRecordStart();
                    }
                });
        }

        @Override
        public void onRecordStop() {
            if (listener != null)
                mainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (listener != null)
                            listener.onRecordStop();
                    }
                });
        }
    }
}
