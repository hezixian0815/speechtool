package com.aispeech.speex;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.FileUtil;
import com.aispeech.common.Log;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.message.Message;


/**
 * pcm 转 ogg
 */
public class SpeexKernel extends BaseKernel {
    public static final String TAG = "SpeexKernel";
    private Utils mEngine;
    private SpeexKernelListener mListener;
    private MySpeex_callback mySpeex_callback;
    private volatile boolean isStopped = true;
    private FileUtil speexInFileUtil = null;
    private FileUtil speexOutFileUtil = null;
    private long mEngineId;
    private int mQuality = 8;
    private int mSampleRate = 16000;
    private int mVbr = 0;
    private int mComplexity = 2;
    private String speexSavedPath = "";

    public SpeexKernel(SpeexKernelListener listener) {
        super(TAG, listener);
        this.mListener = listener;
    }

    /**
     * 检查libduiutils.so是否存在
     *
     * @return 存在为true
     */
    public static boolean checkLibValid() {
        return Utils.isUtilsSoValid();
    }


    /**
     * 设置SPEEX保存的音频文件目录
     * speex压缩之前的原始pcm音频文件格式：speexSavedPath/speex_in_时间戳.pcm
     * speex压缩之后的ogg音频文件格式：speexSavedPath/speex_out_时间戳.ogg
     *
     * @param speexSavedPath 保存speex文件的路径
     */
    public void setSpeexSavedPath(String speexSavedPath) {
        this.speexSavedPath = speexSavedPath;
    }


    /**
     * 设置压缩后ogg音频质量，取值范围：1～10，越大质量越好，cpu占用越高。默认为：8
     *
     * @param quality ogg音频质量
     *                须在startKernel之前调用生效
     */
    public void setQuality(int quality) {
        this.mQuality = quality;
    }


    /**
     * 设置音频采样率：只支持8k和16K，默认为16k
     *
     * @param sampleRate 音频采样率
     *                   须在startKernel之前调用生效
     */
    public void setSampleRate(int sampleRate) {
        this.mSampleRate = sampleRate;
    }


    /*public void setVbr(int vbr) {
        this.mVbr = vbr;
    }*/

    /**
     * 设置压缩的复杂度 可取值：2,3,4,5，默认为2
     *
     * @param complexity 压缩的复杂度
     *                   须在startKernel之前调用生效
     */
    public void setComplexity(int complexity) {
        this.mComplexity = complexity;
    }

    /**
     * 初始化speex内核
     */
    public void newKernel() {
        Log.d(TAG, "newKernel");
        sendMessage(new Message(Message.MSG_NEW));
    }

    /**
     * 启动speex内核
     */
    public void startKernel() {
        Log.d(TAG, "startKernel");
        sendMessage(new Message(Message.MSG_START));
    }

    /**
     * feed 原始单路pcm音频
     *
     * @param data 数据大小
     */
    @Override
    public void feed(byte[] data) {
        super.feed(data);
    }

    /**
     * 取消speex内核
     */
    @Deprecated
    @Override
    public void cancelKernel() {
        super.cancelKernel();
    }

    /**
     * 停止speex内核
     */
    @Override
    public void stopKernel() {
        super.stopKernel();
    }

    /**
     * 释放speex内核
     */
    @Override
    public void releaseKernel() {
        super.releaseKernel();
    }

    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    mySpeex_callback = new MySpeex_callback();
                    mEngine = new Utils();
                    int flag = initSpeex(mEngine, mySpeex_callback);
                    if (mListener != null) {
                        mListener.onInit(flag);
                    }
                    break;
                case Message.MSG_START:
                    if (TextUtils.isEmpty(speexSavedPath)) {
                        speexSavedPath = AISpeechSDK.GLOBAL_AUDIO_SAVE_PATH;
                    }
                    if (!TextUtils.isEmpty(speexSavedPath) && AISpeech.isLocalSaveEnabled()) {
                        speexInFileUtil = new FileUtil();
                        speexOutFileUtil = new FileUtil();
                        long time = System.currentTimeMillis();
                        speexInFileUtil.createFile(speexSavedPath + "/" + "speex_in_" + time + ".pcm");
                        speexOutFileUtil.createFile(speexSavedPath + "/" + "speex_out_" + time + ".ogg");
                    }
                    if (mEngine != null) {
                        mEngine.startEncode(mEngineId, mQuality, mSampleRate, mVbr, mComplexity);
                    }
                    isStopped = false;
                    break;
                case Message.MSG_STOP:
                    if (!TextUtils.isEmpty((speexSavedPath))) {
                        if (speexInFileUtil != null && speexOutFileUtil != null) {
                            speexInFileUtil.closeFile();
                            speexOutFileUtil.closeFile();
                            speexInFileUtil = null;
                            speexOutFileUtil = null;
                        }
                    }
                    if (mEngine != null) {
                        mEngine.stopEncode(mEngineId);
                    }
                    isStopped = true;
                    break;
                /*case Message.MSG_CANCEL:
                    if (mEngine != null)
                        mEngine.cancel();
                    isStopped = true;
                    break;*/
                case Message.MSG_RELEASE:
                    if (!TextUtils.isEmpty((speexSavedPath))) {
                        if (speexInFileUtil != null && speexOutFileUtil != null) {
                            speexInFileUtil.closeFile();
                            speexOutFileUtil.closeFile();
                            speexInFileUtil = null;
                            speexOutFileUtil = null;
                        }
                    }
                    if (mEngine != null) {
                        mEngine.destroyEncode(mEngineId);
                        mEngine = null;
                    }
                    if (mySpeex_callback != null) {
                        mySpeex_callback = null;
                    }
                    isStopped = true;
                    isReleased = true;
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    if (!TextUtils.isEmpty(speexSavedPath)) {//保存原始录音的音频数据aec_in
                        if (speexInFileUtil != null && !isStopped && AISpeech.isLocalSaveEnabled()) {
                            speexInFileUtil.write(data);
                        }
                    }
                    if (mEngine != null && !isStopped) {
                        mEngine.feedEncode(mEngineId, data, data.length);
                    }
                    break;
                case Message.MSG_BEAMFORMING_DATA:
                    byte[] speexData = (byte[]) message.mObject;
                    if (mListener != null) {
                        mListener.onResultBufferReceived(speexData, speexData.length);
                        if (!TextUtils.isEmpty(speexSavedPath)) {
                            //本地保存处理后的音频文件
                            if (speexOutFileUtil != null && AISpeech.isLocalSaveEnabled()) {
                                speexOutFileUtil.write(speexData);
                            }
                        }
                    }
                    break;
                case Message.MSG_ERROR:
                    mListener.onError((AIError) message.mObject);
                    break;
                default:
                    break;
            }
            if (isReleased) {
                innerRelease();
                break;//release后跳出while循环
            }
        }
    }

    private int initSpeex(Utils engine, Utils.speex_callback speex_callback) {
        int status = AIConstant.OPT_FAILED;
        if (engine != null) {
            // 创建引擎
            mEngineId = engine.initEncode(speex_callback);
            Log.d(TAG, "speex create return " + mEngineId + ".");
            if (mEngineId == 0) {
                Log.d(TAG, "引擎初始化失败");
                status = AIConstant.OPT_FAILED;
            } else {
                Log.d(TAG, "引擎初始化成功");
                status = AIConstant.OPT_SUCCESS;
            }
        } else {
            status = AIConstant.OPT_FAILED;
        }
        return status;
    }

    private class MySpeex_callback extends Utils.speex_callback {
        @Override
        public int run(int type, byte[] data, int size) {
            if (size != 0) {
                byte[] oggBuffer = new byte[size];
                System.arraycopy(data, 0, oggBuffer, 0, size);
                if (mListener != null) {
                    mListener.onResultBufferReceived(oggBuffer, oggBuffer.length);
                    if (!TextUtils.isEmpty(speexSavedPath)) {
                        //本地保存处理后的音频文件
                        if (speexOutFileUtil != null && AISpeech.isLocalSaveEnabled()) {
                            speexOutFileUtil.write(oggBuffer);
                        }
                    }
                }
//                sendMessage(new Message(Message.MSG_BEAMFORMING_DATA, oggBuffer));
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }
}
