package com.aispeech.lite.tts;

import android.content.Context;
import android.media.AudioFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.auth.AIAuthEngine;
import com.aispeech.auth.AIProfile;
import com.aispeech.auth.ProfileState;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AIFileWriter;
import com.aispeech.common.CloseUtils;
import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.common.ThreadNameUtil;
import com.aispeech.common.TtsFileWriter;
import com.aispeech.common.Util;
import com.aispeech.common.WavFileWriter;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.export.interceptor.TTSInterceptor;
import com.aispeech.kernel.MP3;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISampleRate;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.Scope;
import com.aispeech.lite.audio.AIPlayerListener;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.CloudTtsConfig;
import com.aispeech.lite.config.LocalTtsConfig;
import com.aispeech.lite.param.CloudTtsParams;
import com.aispeech.lite.param.LocalTtsParams;
import com.aispeech.lite.param.TTSParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * @auther wuwei
 */
public class TtsProcessor implements TtsKernelListener {
    public static final String MODEL_LOCAL = "LocalTts";
    public static final String MODEL_CLOUD = "CloudTts";
    public String TAG = "";
    private static TtsProcessor mInstance = null;//只有本地合成采用到单例
    //授权处理
    protected AIProfile mAIProfile;
    protected ProfileState mProfileState;
    private SynthesizeState mState = SynthesizeState.STATE_IDLE;
    private Handler mCallbackHandler;
    private Handler mInnerHandler;
    private HandlerThread mHandlerThread;
    private Looper mLooper;
    private SynthesizedBlockQueue mBlockQueue;
    private MP3 mDecoder;
    private AIPlayerFactory mPlayerFactory;
    private IAIPlayer mPlayer;
    private BaseKernel mTtsKernel;
    private TTSParams mTtsParams;
    private AIEngineConfig mTtsConfig;
    private SynthesizerListener mListener;
    private Context mContext;
    private String mRecordId;
    private ArrayList<byte[]> mByteBuffer = new ArrayList<>();
    private StringBuilder mReturnPhone = new StringBuilder();
    private String mModelName = "";
    //判断是否第一帧音频数据
    private AtomicBoolean firstFlag = new AtomicBoolean(false);
    private long mStartTime;
    private long mFirstAudioTime;
    private long mLastAudioTime;
    private String paramSet = null;
    private Map<AISampleRate, IAIPlayer> mPlayerMap = new HashMap<>();
    /**
     * 播放器会话Id
     */
    private long mAIPlayerSessionId;
    private TtsFileWriter ttsFileWriter;
    private TtsFileWriter ttsReturnMeFileWriter;
    private TTSCache ttsCache;
    private TTSCache ttsReturnMeCache;

    private boolean isStartFeedData; //是否开始feed数据
    private long mTotalTime;

    /**
     * 初始化
     *
     * @param listener 合成回调
     * @param config   合成config
     */
    public void init(SynthesizerListener listener, AIEngineConfig config, String modelName) {
        this.mListener = listener;
        this.mContext = config.getContext();
        this.mTtsConfig = config;
        TAG = modelName + "Processor";
        this.mModelName = modelName;
        mAIProfile = AIAuthEngine.getInstance().getProfile();
        if (TextUtils.equals(modelName, MODEL_LOCAL)) {
            mProfileState = mAIProfile.isProfileValid(Scope.LOCAL_TTS);
        } else {
            mProfileState = mAIProfile.isProfileValid(Scope.CLOUD_MODEL);
        }
        Log.d(TAG, "authstate: " + mProfileState.toString());
        if (mProfileState.isValid()) {
            if (mInnerHandler == null) {
                mInnerHandler = createInnerHandler();
            }
            if (mCallbackHandler == null) {
                mCallbackHandler = createCallbackHandler();
            }
            if (TextUtils.equals(modelName, MODEL_LOCAL)) {//local tts
                mTtsConfig = (LocalTtsConfig) config;
                if (mTtsKernel == null) mTtsKernel = new LocalTtsKernel(this);
            } else if (TextUtils.equals(modelName, MODEL_CLOUD)) {//cloud tts
                mTtsConfig = (CloudTtsConfig) config;
                if (mTtsKernel == null) mTtsKernel = new CloudTtsKernel(this);
                mTtsKernel.setProfile(mAIProfile);
            }
            sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_NEW, null);
        } else {
            showErrorMessage();
        }
    }

    private void showErrorMessage() {
        AIError error = new AIError();
        if (mProfileState == null) {
            error.setErrId(AIError.ERR_SDK_NOT_INIT);
            error.setError(AIError.ERR_DESCRIPTION_ERR_SDK_NOT_INIT);
        } else {
            error.setErrId(mProfileState.getAuthErrMsg().getId());
            error.setError(mProfileState.getAuthErrMsg().getValue());
        }
        if (mListener != null) {
            mListener.onError(error);
        }
    }

    private void readDataFromCache(File file) {
        int bufferSize = 2048;
        if (mPlayer != null && mPlayer instanceof AIAudioTrack) {
            bufferSize = ((AIAudioTrack) mPlayer).getMinBufferSize();
        }
        if (bufferSize <= 0) {
            bufferSize = 2048;
        }
        byte[] data = new byte[bufferSize];
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(file);
            int len;
            boolean head = true;
            while ((len = fi.read(data)) != -1) {
                if (head) {
                    head = false;
                    byte[] data2 = WavFileWriter.removeWaveHeader(data);
                    this.onBufferReceived(data2, data2.length, AIConstant.AIENGINE_MESSAGE_TYPE_BIN, "cache");
                } else
                    this.onBufferReceived(data, len, AIConstant.AIENGINE_MESSAGE_TYPE_BIN, "cache");
            }
            // 结束标志
            this.onBufferReceived(data, 0, AIConstant.AIENGINE_MESSAGE_TYPE_BIN, "cache");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseUtils.closeIO(fi);
        }
    }

    private void readReturnPhoneDataFromCache(File file) {
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(file);
            byte[] data = new byte[fi.available()];
            int len;
            while ((len = fi.read(data)) != -1) {
                if (!MODEL_LOCAL.equals(mModelName)) {
                    this.onBufferReceived(data, len, AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
                } else {
                    readLocalReturnMe(data, len);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            CloseUtils.closeIO(fi);
        }
    }

    /**
     * 从自定义音频文件读取音频
     */
    private void readDataFromCustomAudio(String customAudioPath) {
        FileInputStream fi = null;
        try {
            fi = new FileInputStream(customAudioPath);
            if (customAudioPath.endsWith(AIConstant.TTS_AUDIO_TYPE_MP3)) {
                byte[] dataMp3 = new byte[1024];
                byte[] dataPcm = new byte[1024 * 16];
                byte[] tempMp3;
                byte[] tempPcm;
                int len;
                while ((len = fi.read(dataMp3)) != -1) {
                    tempMp3 = new byte[len];
                    System.arraycopy(dataMp3, 0, tempMp3, 0, len);
                    int outLen = mDecoder.ddsFeed(tempMp3, len, dataPcm);
//                    Log.d(TAG, "inputLen = " + len + "  outLen = " + outLen);
                    if (outLen > 0) {
                        tempPcm = new byte[outLen];
                        System.arraycopy(dataPcm, 0, tempPcm, 0, outLen);
                        this.onBufferReceived(tempPcm, outLen, "custom");
                    }
                }

                //音频文件读完后可能仍有数据未解码完成，继续解码直到返回为0

                byte[] last = new byte[1024 * 16];
                int outLength;
                while ((outLength = mDecoder.ddsFeed(new byte[0], dataMp3.length, last)) > 0) {
//                    Log.d(TAG, "last outLen = " + outLength);
                    tempPcm = new byte[outLength];
                    System.arraycopy(last, 0, tempPcm, 0, outLength);
                    this.onBufferReceived(tempPcm, outLength, "custom");
                }


                this.onBufferReceived(new byte[0], 0, "custom");
                if (mDecoder != null) {
                    mDecoder.ddsDestroy();
                    mDecoder = null;
                }

            } else if (customAudioPath.endsWith("wav")) {
                boolean head = true;
                int bufferSize = 2048;
                int len;
                byte[] data = new byte[bufferSize];
                while ((len = fi.read(data)) != -1) {
                    if (head) {
                        head = false;
                        byte[] data2 = WavFileWriter.removeWaveHeader(data);
                        this.onBufferReceived(data2, data2.length, "cache");
                    } else
                        this.onBufferReceived(data, len, "cache");
                }
                // 结束标志
                this.onBufferReceived(data, 0, "cache");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fi != null) {
                try {
                    fi.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 创建使用工作线程创建Handler用于处理消息
     *
     * @return handler
     */
    private Handler createInnerHandler() {
        mHandlerThread = new HandlerThread(ThreadNameUtil.getFixedThreadName(TAG));
        mHandlerThread.start();
        mLooper = mHandlerThread.getLooper();
        Handler innerHandler = new Handler(mLooper) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                SynthesizeMsg synthesizeMsg = SynthesizeMsg
                        .getMsgByValue(msg.what);
                if (isFilterMsg(synthesizeMsg)) {
                    Log.d(TAG, ">>>>>>Event: " + (synthesizeMsg != null ? synthesizeMsg.name() : ""));
                    Log.d(TAG, "[Current]:" + mState.name());
                }
                if (synthesizeMsg == null) {
                    return;
                }
                switch (synthesizeMsg) {
                    case MSG_NEW:
                        if (mState == SynthesizeState.STATE_IDLE || mState == SynthesizeState.STATE_WAITING) {
                            transferSynthesizeState(SynthesizeState.STATE_WAITING);
                            initTtsKernel(mTtsConfig);
                        } else {
                            trackInvalidState(synthesizeMsg);
                        }
                        break;
                    case MSG_START:
                        if (mState == SynthesizeState.STATE_INITIALIZED
                                || mState == SynthesizeState.STATE_STOPPED) {
                            //试用检查判断
                            if (MODEL_LOCAL.equals(mModelName) && !updateTrails(Scope.LOCAL_TTS)) {
                                return;
                            }
                            isStartFeedData = false;
                            mRecordId = Utils.getRecorderId();
                            mTtsParams = (TTSParams) msg.obj;
                            String text = mTtsParams.getRefText();
                            if (TextUtils.isEmpty(text)) {
                                sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_ERROR,
                                        new AIError(AIError.ERR_TTS_INVALID_REFTEXT, AIError.ERR_DESCRIPTION_TTS_INVALID_REFTEXT));
                                break;
                            }

                            if (mTtsParams.isOutRealData()) {
                                Log.d(TAG, "output real pcm audio data");
                            }
                            if (mTtsParams.isAutoPlay()) {
                                initPlayer();
                            }
                            if (mTtsParams instanceof LocalTtsParams) {
                                if (TextUtils.isEmpty(paramSet)) {
                                    ((LocalTtsParams) mTtsParams).setBackBin(((LocalTtsConfig) mTtsConfig).getBackBinPath());
                                } else {
                                    ((LocalTtsParams) mTtsParams).setBackBin(paramSet);
                                }
                            }

                            String customAudioPath = TTSCache.getInstanceLocal().getCustomAudioPath(text);

                            if (mDecoder == null && isCloudTTSRealBack(mTtsParams) || customAudioPath != null &&
                                    customAudioPath.endsWith(AIConstant.TTS_AUDIO_TYPE_MP3)) {//云端实时反馈合成需要解码mp3 to pcm
                                mDecoder = new MP3();
                                JSONObject jsonObject = new JSONObject();
                                try {
                                    jsonObject.put("channels", 1);
                                    jsonObject.put("samplerate", 16000);
                                    jsonObject.put("bitrate", 32000);
                                    jsonObject.put("quality", 8);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                mDecoder.ddsInit(MP3.MP3_TO_PCM, jsonObject.toString());
                            }

                            //置返回第一帧合成数据标志位
                            firstFlag.compareAndSet(true, false);

                            mStartTime = System.currentTimeMillis();
                            Log.d(TAG, "mStartTime : " + mStartTime);
                            Log.d(TAG, "TTS.START");
                            //在包含在自定义音频文本内，则读取本地录音播放，否则走本地缓存逻辑
                            if (FileUtils.isFileExists(customAudioPath)) {
                                readDataFromCustomAudio(customAudioPath);
                            } else {
                                String savedAudioFileName = mTtsParams.getSaveAudioFileName();
                                if (ttsCache == null) ttsCache = getTTSCache();
                                createAudioFile(savedAudioFileName, mTtsParams, ttsCache.isUseCache());

                                createReturnMeFile();

                                File file = ttsCache.get(mTtsParams);
                                File returnMeFile = null;
                                if (ttsReturnMeCache != null) {
                                    returnMeFile = ttsReturnMeCache.get(mTtsParams);
                                }

                                if (file != null && file.exists() && file.canRead() && file.length() > 0) {
                                    if (mTtsParams.isReturnPhone()) {
                                        if (returnMeFile != null && returnMeFile.exists() && returnMeFile.canRead() && returnMeFile.length() > 0) {
                                            readReturnPhoneDataFromCache(returnMeFile);
                                            readDataFromCache(file);
                                        } else {
                                            mTtsKernel.startKernel(mTtsParams);
                                        }
                                    } else {
                                        readDataFromCache(file);
                                    }
                                } else {
                                    mTtsKernel.startKernel(mTtsParams);
                                }
                            }

                            if (!mTtsParams.isRealBack() && mByteBuffer != null) {
                                mByteBuffer.clear();
                            }
                            transferSynthesizeState(SynthesizeState.STATE_STARTED);
                        } else {
                            trackInvalidState(synthesizeMsg);
                        }
                        break;
                    case MSG_STOP:
                        if (mState == SynthesizeState.STATE_STARTED
                                || mState == SynthesizeState.STATE_PAUSED) {
                            if (mBlockQueue != null) {
                                mBlockQueue.clear();
                            }
                            // 停止当前的播放器
                            if (mPlayer != null) {
                                mPlayer.stop();
                                mPlayer.release();
                                mPlayer = null;
                                mPlayerMap.clear();
                            }
                            if (mPlayerFactory != null) {
                                mPlayerFactory = null;
                            }
                            if (ttsFileWriter != null) {
                                ttsFileWriter.deleteIfOpened();
                            }
                            if (ttsReturnMeFileWriter != null) {
                                ttsReturnMeFileWriter.deleteIfOpened();
                            }
                            if (mDecoder != null) {
                                mDecoder.ddsDestroy();
                                mDecoder = null;
                            }
                            // 取消当前的数据请求
                            if (mTtsKernel != null) {
                                mTtsKernel.cancelKernel();
                            }

                            this.removeMessages(SynthesizeMsg.MSG_RETURN_PHONE_DATA.getValue());
                            this.removeMessages(SynthesizeMsg.MSG_FEED_DATA_BY_STREAM.getValue());
                            if (mTtsConfig != null && mTtsConfig.isUseStopCallback()) {
                                /*
                                 * 仅合成模式，合成中stop则回调 onSynthesizeFinish；合成+播报模式，stop
                                 * 之后需要回调 onCompletion 。
                                 */
                                if (mTtsParams.isAutoPlay()) {
                                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_SPEECH_FINISH, null);
                                } else {
                                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_SYNTHESIZE_FINISH, null);
                                }
                            }
                            transferSynthesizeState(SynthesizeState.STATE_STOPPED);
                        } else {
                            trackInvalidState(synthesizeMsg);
                        }
                        break;
                    case MSG_SET:
                        String dynamicParam = (String) msg.obj;
                        try {
                            JSONObject jsonObject = new JSONObject(dynamicParam);
                            paramSet = jsonObject.optString("backBinPath");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        if (mState != SynthesizeState.STATE_IDLE && mState != SynthesizeState.STATE_WAITING) {
                            mTtsKernel.set(dynamicParam);
                        } else {
                            trackInvalidState(synthesizeMsg);
                        }
                        break;
                    case MSG_RESUME:
                        if (mState == SynthesizeState.STATE_PAUSED) {
                            // 发起恢复播放请求
                            if (mPlayer != null) {
                                mPlayer.resume();
                            }
                            transferSynthesizeState(SynthesizeState.STATE_STARTED);
                        } else {
                            trackInvalidState(synthesizeMsg);
                        }
                        break;
                    case MSG_PAUSE:
                        if (mState == SynthesizeState.STATE_STARTED) {
                            // 发起暂停播放请求
                            if (mPlayer != null) {
                                mPlayer.pause();
                            }
                            transferSynthesizeState(SynthesizeState.STATE_PAUSED);
                        } else {
                            trackInvalidState(synthesizeMsg);
                        }
                        break;
                    case MSG_RELEASE:
                        if (mState != SynthesizeState.STATE_IDLE && mState != SynthesizeState.STATE_WAITING) {
                            clearObject();//清除实例
                            transferSynthesizeState(SynthesizeState.STATE_IDLE);
                        } else {
                            trackInvalidState(synthesizeMsg);
                        }
                        break;
                    case MSG_EMOTION:
                        if (mListener != null) {
                            String[] emotions = (String[]) msg.obj;
                            mListener.onEmotion(emotions[0], emotions[1]);
                        }
                        break;
                    case MSG_ERROR:
                        AIError error = (AIError) msg.obj;
                        Log.w(TAG, "TTS.ERROR: " + error.toString());
                        if (mState != SynthesizeState.STATE_IDLE && mState != SynthesizeState.STATE_WAITING) {
                            if (mPlayer != null) {
                                mPlayer.stop();
                            }
                            mTtsKernel.cancelKernel();
                            transferSynthesizeState(SynthesizeState.STATE_INITIALIZED);
                            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, msg.obj);
                            uploadError(error);
                        }
                        break;
                    case MSG_FEED_DATA_BY_STREAM:
                        if (mState == SynthesizeState.STATE_STARTED
                                || mState == SynthesizeState.STATE_PAUSED) {
                            byte[] bytes = (byte[]) msg.obj;
                            if (mTtsParams.isAutoPlay()) { //送给audioTrack播放
                                if (isCloudTTSRealBack(mTtsParams)) {//cloud
                                    processCloudRealBackData(bytes);
                                } else {//local
                                    processNativeRealBackData(bytes);
                                }
                            } else if (mTtsParams.isOutRealData() && mListener != null) {//抛出实时音频
                                mListener.onSynthesizeDataArrived(bytes);
                            }
                            if (ttsFileWriter != null) {//保存音频至文件
                                ttsFileWriter.write(bytes);
                            }
                            boolean isLast = (bytes.length == 0);
                            if (isLast) {
                                //TODO 正常合成结束
                                //sendMsgToCallbackMsgQueue(CallbackMsg.MSG_SYNTHESIZE_FINISH, null);
                                if (ttsFileWriter != null) {
                                    ttsFileWriter.close();
                                    // 本地的
                                    File tempFile = new File(ttsFileWriter.getAbsolutePath());
                                    if (ttsCache != null && !isMaxCacheTextWordCount()) {
                                        boolean suc = ttsCache.put(mTtsParams, tempFile);
                                        if (suc)
                                            ttsCache.saveToLocal();
                                    }
//                                    //如果不需要保存文件，则在缓存之后进行文件删除
//                                    if (!AISpeech.isLocalSaveEnabled()) {
//                                        FileUtils.deleteFile(tempFile);
//                                    }
                                }
                                if (ttsReturnMeFileWriter != null) {
                                    ttsReturnMeFileWriter.close();
                                    // 本地的
                                    File tempFile = new File(ttsReturnMeFileWriter.getAbsolutePath());
                                    if (ttsReturnMeCache != null) {
                                        boolean suc = ttsReturnMeCache.put(mTtsParams, tempFile);
                                        if (suc)
                                            ttsReturnMeCache.saveToLocal();
                                    }
//                                    //如果不需要保存文件，则在缓存之后进行文件删除
//                                    if (!AISpeech.isLocalSaveEnabled()) {
//                                        FileUtils.deleteFile(tempFile);
//                                    }
                                }
                                if (mListener != null) {
                                    mListener.onSynthesizeFinish();
                                }
                                if (!mTtsParams.isAutoPlay()) {
                                    transferSynthesizeState(SynthesizeState.STATE_INITIALIZED);
                                }
                            }
                        }
                        break;
                    case MSG_RETURN_PHONE_DATA:
                        if (mState == SynthesizeState.STATE_STARTED
                                || mState == SynthesizeState.STATE_PAUSED) {
                            String phoneme = (String) msg.obj;
                            if (ttsReturnMeFileWriter != null) {//保存音频至文件
                                ttsReturnMeFileWriter.write(Util.getUTF8Bytes(phoneme));
                            }
                            if (mListener != null) {
                                mListener.onPhonemesDataArrived(phoneme);
                            }
                        }
                        break;
                    case MSG_FEED_DATA_BY_CHUNK:
                        if (mState == SynthesizeState.STATE_STARTED
                                || mState == SynthesizeState.STATE_PAUSED) {
                            byte[] bytesAll = (byte[]) msg.obj;
                            if (mTtsParams.isAutoPlay()) {
                                String refText = mTtsParams.getRefText();
                                if (!TextUtils.isEmpty(refText) && (bytesAll.length != 0)) {
                                    SynthesizedBlock block = new SynthesizedBytesBlock(
                                            refText, bytesAll);
                                    mBlockQueue.addBlock(block);
                                    mPlayer.notifyDataIsReady(false);
                                }
                                SynthesizedBlock block = new SynthesizedBytesBlock(null, null);
                                mBlockQueue.addBlock(block);
                                mPlayer.notifyDataIsReady(true);
                            } else {
                                if (mListener != null) {
                                    mListener.onSynthesizeDataArrived(bytesAll);
                                }
                                if (ttsFileWriter != null) {//保存音频至文件
                                    ttsFileWriter.write(bytesAll);
                                    ttsFileWriter.close();
                                    File tempFile = new File(ttsFileWriter.getAbsolutePath());
                                    // 本地的
                                    if (ttsCache != null && !isMaxCacheTextWordCount()) {
                                        boolean suc = ttsCache.put(mTtsParams, tempFile);
                                        if (suc)
                                            ttsCache.saveToLocal();
                                    }
//                                    if (!AISpeech.isLocalSaveEnabled()) {
//                                        FileUtils.deleteFile(tempFile);
//                                    }
                                }
                                if (ttsReturnMeFileWriter != null) {//保存音频至文件
                                    ttsReturnMeFileWriter.close();
                                    File tempFile = new File(ttsReturnMeFileWriter.getAbsolutePath());
                                    // 本地的
                                    if (ttsReturnMeCache != null) {
                                        boolean suc = ttsReturnMeCache.put(mTtsParams, tempFile);
                                        if (suc)
                                            ttsReturnMeCache.saveToLocal();
                                    }
//                                    if (!AISpeech.isLocalSaveEnabled()) {
//                                        FileUtils.deleteFile(tempFile);
//                                    }
                                }
                                if (mListener != null) {
                                    mListener.onSynthesizeFinish();
                                }
                                transferSynthesizeState(SynthesizeState.STATE_INITIALIZED);
                            }
                        } else {
                            trackInvalidState(synthesizeMsg);
                        }
                        break;
                    case MSG_COMPLETED:
                        long sessionId = (Long) msg.obj;
                        if (sessionId == mAIPlayerSessionId) {
                            Log.d(TAG, "MSG_COMPLETED seesionId:" + sessionId
                                    + "  mAIPlayerSessionId : "
                                    + mAIPlayerSessionId);
                            Log.d(TAG, "TotalTime = " + mTotalTime + " TTS.FIRST.END.AVERAGE.TIME.RATE : "
                                    + ((mTotalTime > 0) ? ((float) (mLastAudioTime - mFirstAudioTime) / mTotalTime) : mTotalTime));
                            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_SPEECH_FINISH,
                                    null);
                            transferSynthesizeState(SynthesizeState.STATE_INITIALIZED);
                        } else {
                            Log.d(TAG, "player session expired");
                        }
                        break;
                    default:
                        break;
                }
            }
        };
        return innerHandler;
    }

    private void initPlayer() {
        Log.d(TAG, "auto play audioData " + mPlayerFactory);
        if (mPlayerFactory == null) {
            mPlayerFactory = new AIPlayerFactory();
        }

        mPlayer = mPlayerMap.get(mTtsParams.getSampleRate());
        if (mPlayer == null) {
            Log.d(TAG, "before create " + mPlayer);
            mPlayer = mPlayerFactory.createAIPlayer(mTtsParams);
            if (mBlockQueue == null) {
                mBlockQueue = mPlayerFactory.createSynthesizedBlockQueue();
            }
            mBlockQueue.clear();
            mPlayer.setDataQueue(mBlockQueue);
            mPlayer.setPlayerListener(new AIPlayerListenerImpl());
            mPlayer.init(mContext, mTtsParams.getStreamType(), mTtsParams.getSampleRate().getValue());
            mPlayerMap.put(mTtsParams.getSampleRate(), mPlayer);
        }
        //设置保存合成音频路径:为了兼容对外现有接口，local:外面指定文件名绝对路径；cloud:根据文件夹内部拼接文件名绝对路径
        // 音频保存动作放在 TtsProcessor 里
        // mPlayer.setAudioFileSavePath(mTtsParams.getSaveAudioFileName());

        if (mTtsParams.isUseStreamType()
                || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            mPlayer.setStreamType(mTtsParams.getStreamType());
        } else {
            mPlayer.setAudioAttributes(mTtsParams.getAudioAttributes());
        }

        mAIPlayerSessionId = mPlayer.play();
    }

    private void closeFileWriter() {
        if (ttsFileWriter != null) {
            ttsFileWriter.deleteIfOpened();
            ttsFileWriter = null;
        }
    }

    private void clearObject() {
        // 销毁引擎
        if (mTtsKernel != null) {
            mTtsKernel.releaseKernel();
            mTtsKernel = null;
        }
        if (mContext != null) {
            mContext = null;
        }
        closeFileWriter();
        if (ttsReturnMeFileWriter != null) {
            ttsReturnMeFileWriter.deleteIfOpened();
            ttsReturnMeFileWriter = null;
        }
        if (ttsCache != null)
            ttsCache = null;
        if (ttsReturnMeCache != null)
            ttsReturnMeCache = null;
        // 停止当前播放
        if (mPlayerMap != null) {
            for (AISampleRate aiSampleRate : mPlayerMap.keySet()) {
                IAIPlayer player = mPlayerMap.get(aiSampleRate);
                if (player != null) player.release();
            }
            mPlayerMap.clear();
            mPlayer = null;
        }
        if (mDecoder != null) {
            mDecoder.ddsDestroy();
            mDecoder = null;
        }
        if (mTtsConfig != null)
            mTtsConfig = null;
        if (mTtsParams != null)
            mTtsParams = null;
        if (mBlockQueue != null)
            mBlockQueue = null;
        if (mListener != null)
            mListener = null;
        if (mPlayerFactory != null)
            mPlayerFactory = null;
        if (mCallbackHandler != null)
            mCallbackHandler.removeCallbacksAndMessages(null);
        if (mInnerHandler != null)
            mInnerHandler.removeCallbacksAndMessages(null);
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
    }


    // TODO : diff resource
    private void initTtsKernel(AIEngineConfig aiEngineConfig) {
        String[] assetsResNames = aiEngineConfig.getAssetsResNames();
        Map<String, String> assetsResMd5sumMap = aiEngineConfig.getAssetsResMd5sum();
        if (assetsResNames != null && assetsResNames.length > 0) {
            for (String resName : assetsResNames) {
                String resMd5sumName = null;
                if (assetsResMd5sumMap != null) {
                    resMd5sumName = assetsResMd5sumMap.get(resName);
                }
                Util.copyResource(aiEngineConfig.getContext(), resName, resMd5sumName);
            }
        }
        if (mTtsKernel != null) {
            mTtsKernel.newKernel(aiEngineConfig);
        }

        com.aispeech.util.Utils.checkThreadAffinity();

    }


    public void deleteFile() {
        Log.i(TAG, "=======deleteFile======");
        if (mTtsConfig == null) {
            return;
        }
        String[] assetsResNames = mTtsConfig.getAssetsResNames();
        Map<String, String> assetsResMd5sumMap = mTtsConfig.getAssetsResMd5sum();
        if (assetsResNames != null && assetsResNames.length > 0) {
            for (String resName : assetsResNames) {
                if (assetsResMd5sumMap != null) {
                    String resMd5sumName = assetsResMd5sumMap.get(resName);
                    Util.deleteFile(mTtsConfig.getContext(), resMd5sumName);
                }
                Util.deleteFile(mTtsConfig.getContext(), resName);
            }
        }
    }


    private boolean isCloudTTSRealBack(TTSParams params) {
        return params.isRealBack()
                && TextUtils.equals(params.getType(), TTSParams.TYPE_CLOUD);
    }


    /**
     * 处理返回的本地合成音频
     *
     * @param bytes 数据
     */
    private void processNativeRealBackData(byte[] bytes) {
        if (bytes.length == 0) { // 标记块内数据的结束
            SynthesizedBlock block = new SynthesizedBytesBlock(null, null);
            mBlockQueue.addBlock(block);
            mPlayer.notifyDataIsReady(true);
        } else {
            if (mTtsParams.getRefText() != null) {
                SynthesizedBlock block = new SynthesizedBytesBlock(mTtsParams.getRefText(), bytes);
                mBlockQueue.addBlock(block);
                mPlayer.notifyDataIsReady(false);
            }
        }
    }

    /**
     * 处理实时返回的云端合成音频
     *
     * @param mp3TotalBuf mp3TotalBuf
     */
    private void processCloudRealBackData(byte[] mp3TotalBuf) {
        int outLen = 0;
        byte[] pcm_l = new byte[24096];
        int baseLength = 800;//以800为界限，随便取的一个值，如果mp3Buf过大的话，输出的buf（pcm_l）也会过大，所以分段处理
        int buffCount = mp3TotalBuf.length / baseLength + 1;
        for (int i = 0; i < buffCount; i++) {
            byte[] mp3SubBuf;
            if (i < buffCount - 1 && mp3TotalBuf.length != 0) {
                mp3SubBuf = new byte[baseLength];
            } else if (i < buffCount - 1 && mp3TotalBuf.length == 0) {
                mp3SubBuf = new byte[0];
            } else {
                mp3SubBuf = new byte[mp3TotalBuf.length % baseLength];
            }
            System.arraycopy(mp3TotalBuf, i * baseLength, mp3SubBuf, 0, mp3SubBuf.length);
            if (mDecoder != null) {
                outLen = mDecoder.ddsFeed(mp3SubBuf, mp3SubBuf.length, pcm_l);
            }

            if (outLen > 0) {
                byte[] temp;
                temp = new byte[outLen];
                System.arraycopy(pcm_l, 0, temp, 0, outLen <= pcm_l.length ? outLen : pcm_l.length);
                if (mTtsParams.getRefText() != null) {
                    SynthesizedBlock block = new SynthesizedBytesBlock(mTtsParams.getRefText(), temp);
                    mBlockQueue.addBlock(block);
                    mPlayer.notifyDataIsReady(false);
                }
            }
        }
        if (mp3TotalBuf.length == 0) { //结束标志
            if (mDecoder != null) {
                mDecoder.ddsDestroy();
                mDecoder = null;
            }
            SynthesizedBlock block = new SynthesizedBytesBlock(null, null);
            mBlockQueue.addBlock(block);
            mPlayer.notifyDataIsReady(true);
        }
    }

    /**
     * 主UI线程Handler
     *
     * @return handler
     */
    private Handler createCallbackHandler() {
        Looper looper = mContext.getMainLooper();
        Handler handler = new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                CallbackMsg callbackMsg = CallbackMsg.getMsgByValue(msg.what);
                // Log.d(TAG, "event: " + callbackMsg.name());
                if (callbackMsg == null) {
                    Log.e(TAG, "handleMessage:callbackMsg is null,return");
                    return;
                }
                switch (callbackMsg) {
                    case MSG_INIT:
                        if (mListener != null)
                            mListener.onInit((Integer) msg.obj);
                        break;
                    case MSG_SPEECH_START:
                        if (mListener != null)
                            mListener.onSpeechStart();
                        break;
                    case MSG_SPEECH_FINISH:
                        if (mListener != null)
                            mListener.onSpeechFinish();
                        break;
                    case MSG_ERROR:
                        if (mListener != null)
                            mListener.onError((AIError) msg.obj);
                        break;
                    case MSG_SPEECH_PROGRESS:
                        Object[] speakObjs = (Object[]) msg.obj;
                        int currentTime = (Integer) speakObjs[0];
                        int totalTime = (Integer) speakObjs[1];
                        boolean isFeedEnd = (Boolean) speakObjs[2];
                        if (mListener != null)
                            mListener.onSpeechProgress(currentTime, totalTime,
                                    isFeedEnd);
                        break;
                    case MSG_SYNTHESIZE_START:
                        if (mListener != null)
                            mListener.onSynthesizeStart();
                        break;
                    case MSG_SYNTHESIZE_FINISH:
                        if (ttsFileWriter != null) {
                            ttsFileWriter.close();
                        }
                        if (mListener != null)
                            mListener.onSynthesizeFinish();
                        break;
                    default:
                        break;
                }
            }
        };
        return handler;
    }


    /**
     * 发送合成请求
     *
     * @param ttsParams 请求参数
     */
    public void start(TTSParams ttsParams) {
        ProfileState localState = mProfileState;
        if (localState != null) {
            if (localState.isValid()) {
                try {
                    sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_START,
                            ttsParams.clone());
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            } else {
                showErrorMessage();
            }
        } else {
            Log.e(TAG, "start: profile is null, ignore");
        }
    }

    /**
     * 设置本地后端合成音色资源路径
     *
     * @param dynamic 动态参数
     */
    public void set(String dynamic) {
        ProfileState localState = mProfileState;
        if (localState != null) {
            if (localState.isValid()) {
                sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_SET, dynamic);
            } else {
                showErrorMessage();
            }
        } else {
            Log.e(TAG, "set: profile is null, ignore");
        }
    }

    /**
     * 暂停播放
     */
    public void pause() {
        ProfileState localState = mProfileState;
        if (localState != null) {
            if (localState.isValid()) {
                sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_PAUSE, null);
            } else {
                showErrorMessage();
            }
        } else {
            Log.e(TAG, "pause: profile is null, ignore");
        }
    }

    /**
     * 恢复播放
     */
    public void resume() {
        ProfileState localState = mProfileState;
        if (localState != null) {
            if (localState.isValid()) {
                sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_RESUME, null);
            } else {
                showErrorMessage();
            }
        } else {
            Log.e(TAG, "resume: profile is null, ignore");
        }
    }

    protected void removeInnerMsg() {
        if (mInnerHandler != null) {
            mInnerHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 停止播放
     */
    public void stop() {
        ProfileState localState = mProfileState;
        if (localState != null) {
            if (localState.isValid()) {
                removeInnerMsg();
                sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_STOP, null);
            } else {
                showErrorMessage();
            }
        } else {
            Log.e(TAG, "stop: profile is null, ignore");
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        ProfileState localState = mProfileState;
        if (localState != null) {
            if (localState.isValid()) {
                if (mInstance != null) {
                    mInstance = null;
                }
                sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_RELEASE, null);
                this.mListener = null;
            } else {
                showErrorMessage();
            }
        } else {
            Log.e(TAG, "release: profile is null, ignore");
        }
    }

    /**
     * 向外部回调消息队列发送消息
     *
     * @param msg CallbackMsg枚举
     * @param obj msg.obj
     */
    private void sendMsgToCallbackMsgQueue(CallbackMsg msg, Object obj) {
        if (mCallbackHandler != null) {
            Message.obtain(mCallbackHandler, msg.getValue(), obj).sendToTarget();
        }
    }

    @Override
    public void onInit(int status) {
        if (status == AIConstant.OPT_SUCCESS) {
            transferSynthesizeState(SynthesizeState.STATE_INITIALIZED);
        } else if (status == AIConstant.OPT_FAILED) {
            transferSynthesizeState(SynthesizeState.STATE_IDLE);
        }
        sendMsgToCallbackMsgQueue(CallbackMsg.MSG_INIT, status);
    }

    @Override
    public void onBufferReceived(byte[] data, int size, int dataType) {
        onBufferReceived(data, size, dataType, "kernel");
    }

    private void onBufferReceived(byte[] data, int size, String from) {
        onBufferReceived(data, size, AIConstant.AIENGINE_MESSAGE_TYPE_BIN, from);
    }

    private void onBufferReceived(byte[] data, int size, int dataType, String from) {
        if (firstFlag.compareAndSet(false, true)) {
            mFirstAudioTime = System.currentTimeMillis();
            Log.d(TAG, "TTS.FIRST : " + from);
            Log.d(TAG, "mFirstAudioTime : " + mFirstAudioTime);
            Log.d(TAG, "TTS.START.FIRST.DELAY : " + (mFirstAudioTime - mStartTime));
            //sendMsgToCallbackMsgQueue(CallbackMsg.MSG_SYNTHESIZE_START, null);
            if (mListener != null) {
                mListener.onSynthesizeStart();
            }
            try {

                String interceptorName;
                if (isCloudTTSRealBack(mTtsParams)) { //cloud
                    interceptorName = IInterceptor.Name.CLOUD_TTS_SYNTHESIS_FIRST;
                } else {
                    interceptorName = IInterceptor.Name.LOCAL_TTS_SYNTHESIS_FIRST;
                }
                JSONObject customObj = new JSONObject().put(interceptorName, from);
                JSONObject inputObj = TTSInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.RECEIVE, customObj);
                SpeechInterceptor.getInstance().doInterceptor(interceptorName, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        /*
         * 压测时存在极小概率的合成异常问题，表现为：刚开始播报，立即回调播报完毕！
         * 经分析是内核在合成时被打断（size=0）引起的，由于合成的回调存在一定的延迟，连续压测下概率性的
         * 会出现在播报时收到上次的打断回调结果，使得SDK层状态错误。
         * 合成回调的延迟不可避免，所以在这里做一个过滤，即：首帧从合成到帧尾小于等于 1ms 的数据过滤掉。
         */
        if (size == 0 && "kernel".equals(from) && MODEL_LOCAL.equals(mModelName)) {
            if (System.currentTimeMillis() - mFirstAudioTime <= 1 && !isStartFeedData) {
                Log.e(TAG, "过滤掉小于等于 1ms 的打断回调！");
                return;
            }
        }

        byte[] ttsData = new byte[size];
        if (size > 0) {
            System.arraycopy(data, 0, ttsData, 0, size);//规避size为0时候，data为null,拷贝失败bug
        }
        AIResult results = AIResult.bundleResults(dataType, mRecordId, ttsData);
        // 处理binary结果回调
        boolean isLast = (size == 0);
        if (isLast) {
            mLastAudioTime = System.currentTimeMillis();
            Log.d(TAG, "TTS.START.END.DELAY ： " + (mLastAudioTime - mStartTime));
            Log.d(TAG, "TTS.FIRST.END.DELAY ： " + (mLastAudioTime - mFirstAudioTime));
            Log.d(TAG, "TTS.END");
            Log.d(TAG, "TTS.START.FIRST.AVERAGE.WORD.TIME : " + (float) (mFirstAudioTime - mStartTime) / mTtsParams.getRefText().length());
            Log.d(TAG, "TTS.END.FIRST.AVERAGE.WORD.TIME : " + (float) (mLastAudioTime - mStartTime) / mTtsParams.getRefText().length());
            uploadCost();
            try {
                String interceptorName;
                if (isCloudTTSRealBack(mTtsParams)) { //cloud
                    interceptorName = IInterceptor.Name.CLOUD_TTS_SYNTHESIS_END;
                } else {
                    interceptorName = IInterceptor.Name.LOCAL_TTS_SYNTHESIS_END;
                }
                JSONObject customObj = new JSONObject().put(interceptorName, from);
                JSONObject inputObj = TTSInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.RECEIVE, customObj);
                SpeechInterceptor.getInstance().doInterceptor(interceptorName, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        results.setLast(isLast);
        processResult(results);
    }

    @Override
    public void onMessage(String key, Object object) {
        //do nothing
    }


    @Override
    public void onTimestampReceived(byte[] timeStampJson, int size) {
        if (mListener != null) {
            mListener.onTimestampReceived(timeStampJson, size);
        }
        writeLocalReturnMe(timeStampJson);
    }

    /**
     * 将本地合成的音素保存
     * 每个音素包结尾增加",_,"，用于读的时候进行切割
     * {@link #readLocalReturnMe}
     *
     * @param timeStampJson
     */
    private void writeLocalReturnMe(byte[] timeStampJson) {
        if (ttsReturnMeFileWriter != null) {//保存音频至文件
            byte[] time = Util.getUTF8Bytes(",_,");
            ttsReturnMeFileWriter.write(timeStampJson);
            ttsReturnMeFileWriter.write(time);
        }
    }

    /**
     * 末尾增加特殊符号，用于读的时候进行切割
     *
     * @param timeStampJson
     * @param size
     */
    private void readLocalReturnMe(byte[] timeStampJson, int size) {
        try {
            String time = Util.newUTF8String(timeStampJson);
            String[] timeArr = time.split(",_,");
            for (String s : timeArr) {
                byte[] timeJson = Util.getUTF8Bytes(s);
                mListener.onTimestampReceived(timeJson, timeJson.length);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStartFeedData() {
        isStartFeedData = true;
    }

    @Override
    public void onEmotion(String emotion, String emotionOrigin) {
        sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_EMOTION, new String[]{emotion, emotionOrigin});
    }

    @Override
    public void onError(AIError error) {
        sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_ERROR, error);
    }

    private void processResult(AIResult result) {
        // 置null说明release了
        if (mTtsParams == null)
            return;
        // 实时反馈需要及时抛出数据
        if (mTtsParams.isRealBack()) {//audioTrack播放
            if (result.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_RETURN_PHONE_DATA, result.getResultObject());
            } else {
                sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_FEED_DATA_BY_STREAM, (byte[]) result.getResultObject());
            }
        } else {//mediaPlayer播放
            // 将音频缓存入链表
            if (result.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                if (mReturnPhone == null) {
                    mReturnPhone = new StringBuilder();
                }
                mReturnPhone.append(result.getResultObject());
            } else {
                mByteBuffer.add((byte[]) result.getResultObject());
            }
            if (result.isLast()) {
                int sumLength = 0;
                for (byte[] bytes : mByteBuffer) {
                    sumLength += bytes.length;
                }
                byte[] data = new byte[sumLength];
                int index = 0;
                for (byte[] bytes : mByteBuffer) {
                    System.arraycopy(bytes, 0, data, index, bytes.length);
                    index += bytes.length;
                }
                mByteBuffer.clear();
                if (mReturnPhone != null) {
                    sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_RETURN_PHONE_DATA, mReturnPhone.toString());
                    mReturnPhone = null;
                }
                sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_FEED_DATA_BY_CHUNK, data);
            }
        }
    }

    protected void transferSynthesizeState(SynthesizeState nextState) {
        Log.d(TAG, "transfer:" + mState + " to:" + nextState);
        mState = nextState;
    }

    private boolean isFilterMsg(SynthesizeMsg msgWhat) {
        return (msgWhat != SynthesizeMsg.MSG_FEED_DATA_BY_STREAM && msgWhat != SynthesizeMsg.MSG_FEED_DATA_BY_CHUNK);
    }

    /**
     * 向内部消息队列发送消息
     *
     * @param msg SynthesizeMsg枚举
     * @param obj msg.obj
     */
    private void sendMsgToInnerMsgQueue(SynthesizeMsg msg, Object obj) {
        if (mLooper != null && mInnerHandler != null && mHandlerThread != null && mHandlerThread.isAlive()) {
            Message.obtain(mInnerHandler, msg.getValue(), obj).sendToTarget();
        }
    }

    private void trackInvalidState(SynthesizeMsg msg) {
        Log.w(TAG, "Invalid State：" + mState.name() + " when MSG: " + msg.name());
    }


    private boolean isMaxCacheTextWordCount() {
        // 不支持缓存200字符以上长文本
        if (mTtsParams != null && mTtsParams.getRefText().length() > TTSCache.MAX_CACHE_TEXT_WORD_COUNT) {
            Log.i(TAG, "The cached text is greater than " + TTSCache.MAX_CACHE_TEXT_WORD_COUNT + " words");
            return true;
        }
        return false;
    }


    private void createAudioFile(String filePath, TTSParams ttsParams, boolean isUseCache) {
        closeFileWriter();
        if (TextUtils.isEmpty(filePath) && !isUseCache) {
            return;
        }

        if (ttsCache == null) ttsCache = getTTSCache();

        File cacheFile = ttsCache.get(ttsParams);
        /**
         * 如果开启缓存且缓存列表中存在
         * filepath 为null 直接返回，否则将缓存复制到filepath
         * filepath 路径下文件，名称一样，每次都会重新保存文件
         */
        if (isUseCache && cacheFile != null) {
            Log.d(TAG, "user cache set filePath: " + cacheFile.getAbsolutePath() + "  filePath: " + filePath);
            // filePath 不为null，判断文件是否存在，存在先删除，再copy
            if (!TextUtils.isEmpty(filePath)) {
                File saveFilePath = new File(filePath);
                File parentDir = saveFilePath.getParentFile();
                FileUtils.createOrExistsDir(parentDir);
                Log.d(TAG, " copy cache to filePath : " + saveFilePath);
                //判断是否是文件并且存在，如果是则直接删除
                if (saveFilePath.exists() && saveFilePath.isFile()) {
                    saveFilePath.delete();
                }
                //如果不存在且缓存中有，直接copy缓存中文件
                if (FileUtils.copyFile(cacheFile, saveFilePath)) {
                    Log.d(TAG, "copy cache to filePath successful ");
                    return;
                }
                //拷贝失败，继续往下走
            } else {
                //如果没有设置保存路径，直接返回
                return;
            }
        }

        // if (TTSParams.TYPE_CLOUD.equals(ttsParams.getType()) && !ttsParams.isRealBack()) {
        if (!MODEL_LOCAL.equals(mModelName)) {
            File mp3File;
            // 云端TTS是需要设置目录的，之后会自动生成文件名
            if (TextUtils.isEmpty(filePath)) {
                // 不支持缓存200字符以上长文本
                if (ttsParams.getRefText().length() > TTSCache.MAX_CACHE_TEXT_WORD_COUNT) {
                    Log.i(TAG, "The cached text is greater than " + TTSCache.MAX_CACHE_TEXT_WORD_COUNT + " words");
                    return;
                }
                mp3File = ttsCache.newMp3File();
            } else
                mp3File = new File(filePath);

            if (mp3File == null) {
                Log.d(TAG, "user set mp3File null");
                return;
            }

            Log.d(TAG, "user set filePath: " + mp3File.getAbsolutePath() + "     filePath：" + filePath);
            ttsFileWriter = AIFileWriter.createFileWriter(mp3File);
        } else {
            File wavFile;
            if (TextUtils.isEmpty(filePath)) {
                // 不支持缓存200字符以上长文本
                if (ttsParams.getRefText().length() > TTSCache.MAX_CACHE_TEXT_WORD_COUNT) {
                    Log.i(TAG, "The cached text is greater than " + TTSCache.MAX_CACHE_TEXT_WORD_COUNT + " words");
                    return;
                }
                wavFile = ttsCache.newWavFile();
            } else {
                wavFile = new File(filePath);
            }
            if (wavFile == null) {
                Log.d(TAG, "user set wavFile null");
                return;
            }

            Log.d(TAG, "user set filePath: " + wavFile.getAbsolutePath() + "     filePath：" + filePath);
            ttsFileWriter = WavFileWriter.createWavFileWriter(wavFile,
                    AISampleRate.toAISampleRate(mTtsParams.getSampleRate().getValue()), AudioFormat.CHANNEL_OUT_DEFAULT,
                    AudioFormat.ENCODING_PCM_16BIT);
        }
    }

    private File getOutFile(File parentDir, File file) {
        if (parentDir == null || file == null) {
            return null;
        }
        String fileName = file.getName();
        File outFile = new File(parentDir, (System.currentTimeMillis() + "_") + fileName);
        Log.i(TAG, "outFile: " + outFile.getPath());
        return outFile;
    }

    private void createReturnMeFile() {
        if (mTtsParams != null && mTtsParams.isReturnPhone()) {
            if (ttsReturnMeCache == null) {
                if (!MODEL_LOCAL.equals(mModelName)) {
                    ttsReturnMeCache = TTSCache.getInstanceReturnMe();
                } else {
                    ttsReturnMeCache = TTSCache.getInstanceReturnMeLocal();
                }

            }
            // 云端TTS是需要设置目录的，之后会自动生成文件名
            File returnmeFile = ttsCache.newReturnmeFile();

            if (returnmeFile == null) {
                Log.d(TAG, "user set returnmeFile null");
                return;
            }

            Log.d(TAG, "user set filePath: " + returnmeFile.getAbsolutePath());
            ttsReturnMeFileWriter = AIFileWriter.createFileWriter(returnmeFile);
        }
    }

    private TTSCache getTTSCache() {
        return MODEL_LOCAL.equals(mModelName) ? TTSCache.getInstanceLocal() : TTSCache.getInstanceCloud();
    }

    // upload
    private void uploadError(AIError aiError) {
        //添加message外面的字段
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("recordId", mRecordId);
        entryMap.put("mode", "lite");
        if (mModelName.equals(MODEL_LOCAL)) {
            entryMap.put("module", "local_exception");
            JSONObject input = new JSONObject();
            try {
                if (mTtsConfig != null) {
                    input.put("config", mTtsConfig.toJson());
                }
                if (mTtsParams != null) {
                    input.put("param", mTtsParams.toJson());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("local_tts_exception", "info", "local_exception",
                    null, input, aiError.getOutputJSON(), entryMap);
        } else if (mModelName.equals(MODEL_CLOUD)) {
            entryMap.put("module", "cloud_exception");
            AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("cloud_tts_exception", "info", "cloud_exception",
                    mRecordId, aiError.getInputJSON(), aiError.getOutputJSON(), entryMap);
        }
    }

    private void uploadCost() {
        try {
            JSONObject output = new JSONObject();
            output.put("totalcost", mLastAudioTime - mStartTime);
            output.put("firstcost", mFirstAudioTime - mStartTime);
            JSONObject inputJson = null;
            //添加message外面的字段
            Map<String, Object> entryMap = new HashMap<>();
            entryMap.put("recordId", mRecordId);
            entryMap.put("mode", "lite");
            if (mModelName.equals(MODEL_LOCAL)) {
                entryMap.put("module", "local_cost");
                inputJson = new JSONObject();
                try {
                    if (mTtsConfig != null) {
                        inputJson.put("config", mTtsConfig.toJson());
                    }
                    if (mTtsParams != null) {
                        inputJson.put("param", mTtsParams.toJson());
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("local_tts_cost", "info", "local_cost",
                        null, inputJson, output, entryMap);
            } else if (mModelName.equals(MODEL_CLOUD)) {
                entryMap.put("module", "cloud_cost");
                inputJson = new JSONObject(((CloudTtsParams) mTtsParams).getTtsJSON());
                AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("cloud_tts_cost", "info", "cloud_cost",
                        mRecordId, inputJson, output, entryMap);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    protected boolean updateTrails(String scope) {
        ProfileState localState = mProfileState;
        if (localState != null && localState.getAuthType() == ProfileState.AUTH_TYPE.TRIAL
                && localState.getTimesLimit() != -1) {
            if (!localState.isValid()) {
                showErrorMessage();
                return false;
            } else {
                mAIProfile.updateUsedTimes(scope);
                return true;
            }
        } else {
            return true;
        }
    }

    /**
     * 类说明： 对外回调消息列表
     */
    public enum CallbackMsg {
        MSG_INIT(0), MSG_SPEECH_START(1), MSG_SPEECH_FINISH(2), MSG_ERROR(3), MSG_BUFFER_PROGRESS(
                4), MSG_SPEECH_PROGRESS(5), MSG_SYNTHESIZE_START(6), MSG_SYNTHESIZE_FINISH(7);

        private int value;

        private CallbackMsg(int value) {
            this.value = value;
        }

        public static CallbackMsg getMsgByValue(int value) {
            for (CallbackMsg msg : CallbackMsg.values()) {
                if (value == msg.value) {
                    return msg;
                }
            }
            return null;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 子线程handler处理消息
     */
    public enum SynthesizeMsg {
        MSG_NEW(1), MSG_START(2), MSG_STOP(3), MSG_PAUSE(4), MSG_RESUME(5), MSG_RELEASE(
                6), MSG_ERROR(7), MSG_FEED_DATA_BY_STREAM(8), MSG_COMPLETED(9), MSG_SET(
                10), MSG_FEED_DATA_BY_CHUNK(11), MSG_RETURN_PHONE_DATA(12), MSG_EMOTION(13);;
        private int value;

        private SynthesizeMsg(int value) {
            this.value = value;
        }

        public static SynthesizeMsg getMsgByValue(int value) {
            for (SynthesizeMsg msg : SynthesizeMsg.values()) {
                if (value == msg.value) {
                    return msg;
                }
            }
            return null;
        }

        public int getValue() {
            return value;
        }
    }


    /**
     * 合成引擎状态
     */
    public enum SynthesizeState {
        STATE_IDLE(0), STATE_INITIALIZED(1), STATE_STARTED(2), STATE_STOPPED(3),
        STATE_PAUSED(4), STATE_WAITING(5);

        private int value;

        private SynthesizeState(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    class AIPlayerListenerImpl implements AIPlayerListener {

        @Override
        public void onReady() {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_SPEECH_START, null);
        }

        @Override
        public void onPaused() {
            //do nothing
        }

        @Override
        public void onStopped() {
            //do nothing
        }

        @Override
        public void onResumed() {
            //do nothing
        }

        @Override
        public void onCompletion(long sessionId) {
            sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_COMPLETED, sessionId);
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(SynthesizeMsg.MSG_ERROR, error);
        }

        @Override
        public void onProgress(int currentTime, int totalTime,
                               boolean isDataFeedEnd) {
            mTotalTime = totalTime;
            Object[] objs = new Object[]{currentTime, totalTime, isDataFeedEnd};
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_SPEECH_PROGRESS, objs);
        }

    }


}
