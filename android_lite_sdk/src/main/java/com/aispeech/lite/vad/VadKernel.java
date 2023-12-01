package com.aispeech.lite.vad;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.DUILiteConfig;
import com.aispeech.common.AIConstant;
import com.aispeech.common.ByteConvertUtil;
import com.aispeech.common.FileSaveUtil;
import com.aispeech.common.LimitAudioQueue;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.export.interceptor.VadInterceptor;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Vad;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.SpeechParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yuruilong on 2017/5/16.
 */

public class VadKernel extends BaseKernel {
    public String TAG = "VadKernel";
    private VadKernelListener mListener;
    private Vad mEngine;
    private MyVadCallbackImpl myVadCallback;
    private MyMultionecbCallbackImpl myMultionecbCallback;
    private MyMultitwocbCallbackImpl myMultitwocbCallback;
    private volatile int multiVadPauseTimeNum = -1; //动态vad pauseTime当前的设置的序列，必须是pauseTimeArray中的一个，默认为-1，即没有使用动态vad vad PauseTime 序列，如果用户自动配置，目前为0 1 2
    private ConcurrentHashMap<Integer, Boolean> multiVadEndMap; //使用map缓存vad pauseTime 对应index对应callback下的vadEnd信息
    private List<Integer> pauseTimeLists; //保存初始化传入的vad pauseTime 列表
    private SpeechParams mParams;
    private volatile boolean isStopped = true;
    private volatile boolean vadBegin = false;
    private volatile boolean vadEnd = false;
    private boolean useCalcVolume = false;
    private FileSaveUtil mAudioSaveUtil;
    private FileSaveUtil mAsrSaveUtil;
    private volatile FileSaveUtil mAudioSaveOutUtil;
    int mVadScene = -1; // vad使用的场景，通过prefixoftag判断
    private String mRecordID = "";

    private LocalVadConfig config;
    private LimitAudioQueue mLimitAudioQueue = new LimitAudioQueue(1500, 1);

    private static final int FLAG_VAD_BEGIN = 1;
    private static final int FLAG_VAD_END = 2;

    public VadKernel(String prefixOfTag, VadKernelListener listener) {
        super(prefixOfTag + "-VadKernel", listener);
        TAG = prefixOfTag + "-VadKernel";
        mVadScene = VadScenes.parseTag2Scene(prefixOfTag);
        useCalcVolume = AISpeech.useCalcVolume;
        this.mListener = listener;
    }

    public VadKernel(VadKernelListener listener) {
        super("VadKernel", listener);
        useCalcVolume = AISpeech.useCalcVolume;
        mVadScene = VadScenes.VAD_ENGINE;
        this.mListener = listener;
    }

    @Override
    public void stopKernel() {
        Log.d(TAG, "stopKernel");
        sendMessage(new Message(Message.MSG_STOP));
    }


    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    config = (LocalVadConfig) message.mObject;
                    mEngine = new Vad();
                    myVadCallback = new MyVadCallbackImpl();
                    if (config.isMultiMode() == 1) { //初始化配置多vad pauseTime时才去初始化，防止内存浪费
                        myMultionecbCallback = new MyMultionecbCallbackImpl();
                        myMultitwocbCallback = new MyMultitwocbCallbackImpl();
                        if (multiVadEndMap == null) {
                            multiVadEndMap = new ConcurrentHashMap<>();
                        }
                        if (pauseTimeLists == null) {
                            pauseTimeLists = new ArrayList<>();
                        }
                    }
                    int flag = initVad(config, mEngine);
                    if (mListener != null)
                        mListener.onInit(flag);
                    break;
                case Message.MSG_START:
                    mLimitAudioQueue.clear();
                    mParams = (SpeechParams) message.mObject;
                    Log.d(TAG, "MSG_START vad param: " + mParams.toString());
                    if (mEngine != null) {
                        vadBegin = false;
                        startVad(mParams, mEngine, true);
                        isStopped = false;
                    }
                    if (!TextUtils.isEmpty(mParams.getSaveAudioPath()) &&
                            isSaveAudioByScene()) {

                        mAudioSaveUtil = new FileSaveUtil();
                        mAudioSaveUtil.init(mParams.getSaveAudioPath(), TAG);
                        String path = mAudioSaveUtil.prepare("vad_" + mParams.getRecordId());
                        Log.v(TAG, "create local vad audio file at: " + path);

                        if ((AISpeech.useDoubleVad && mVadScene == VadScenes.VAD_ENGINE) || (config != null && config.isEnableDoubleVad())) {
                            mAsrSaveUtil = new FileSaveUtil();
                            mAsrSaveUtil.init(mParams.getSaveAudioPath(), TAG);
                            mAsrSaveUtil.prepare("asr_" + mParams.getRecordId());
                        }
                        mAudioSaveOutUtil = new FileSaveUtil();
                    }
                    break;
                case Message.MSG_STOP:
                    if (mEngine != null)
                        mEngine.stop();
                    vadEnd = true;
                    isStopped = true;
                    Log.i(TAG, "MSG_STOP " + vadEnd + "       isStopped:" + isStopped);
                    closeAudioSaver();
                    break;
                case Message.MSG_CANCEL://vad cancel 直接调用stop 接口
                    if (mEngine != null)
                        mEngine.stop();
                    isStopped = true;
                    Log.i(TAG, "MSG_CANCEL ");
                    closeAudioSaver();
                    break;
                case Message.MSG_RELEASE:
                    // 销毁引擎
                    if (mEngine != null) {
                        mEngine.release();
                        mEngine = null;
                    }
                    if (myVadCallback != null) {
                        myVadCallback = null;
                    }
                    closeAudioSaver();
                    mListener = null;
                    isStopped = true;
                    isReleased = true;
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    if (mEngine != null && !isStopped) {
                        if (mListener != null && vadBegin) {
                            // 优化耗时 提前feed音频
                            mEngine.feed(AIConstant.AIENGINE_MESSAGE_TYPE_BIN, data, data.length);
                            if (useCalcVolume) {
                                short[] shortBuffer = Util.toShortArray(data);
                                double db = Util.calcVolume(shortBuffer);
                                mListener.onRmsChanged((float) db);
                            }
                            if (AISpeech.zoomAudioRate != 1.0f) {
                                // 指定对vad.out单路音频 做缩放处理
                                if ((AISpeech.zoomAudioFlag & DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_VAD_OUT) > 0) {
                                    data = ByteConvertUtil.bigPcm(data, AISpeech.zoomAudioRate);
                                }
                            }
                            mListener.onBufferReceived(data);
                            // 写入输出数据，此时vadBegin 为 true
                            if (mAudioSaveOutUtil != null) mAudioSaveOutUtil.feedTypeOut(data);
                        } else {
                            mEngine.feed(AIConstant.AIENGINE_MESSAGE_TYPE_BIN, data, data.length);
                        }
                        if (mAudioSaveUtil != null) mAudioSaveUtil.feedTypeIn(data);
                    }
                    break;
                case Message.MSG_FEED_BF_VAD_DATA_BY_STREAM:
                    byte[][] dataDouble = (byte[][]) message.mObject;
                    byte[] dataVad = dataDouble[0];
                    byte[] dataAsr = dataDouble[1];
//                    Log.v(TAG, "feed  isStopped " + isStopped + "    vadBegin：" + vadBegin + "      mListener : " + mListener);
                    if (mEngine != null && !isStopped) {
                        if (mListener != null && vadBegin) {
                            mLimitAudioQueue.clear();
                            // 优先feed音频
                            int status = mEngine.feed(AIConstant.AIENGINE_MESSAGE_TYPE_BIN, dataVad, dataVad.length);
//                            Log.i(TAG, "vad begin feed status =  " + status);
                            if (useCalcVolume) {
                                short[] shortBuffer = Util.toShortArray(dataAsr);
                                double db = Util.calcVolume(shortBuffer);
                                mListener.onRmsChanged((float) db);
                            }
                            // 新增Vad放大的逻辑
                            if (AISpeech.zoomAudioRate != 1.0f) {
                                // 指定对vad.out单路音频 做缩放处理
                                if ((AISpeech.zoomAudioFlag & DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_VAD_OUT) > 0) {
                                    dataAsr = ByteConvertUtil.bigPcm(dataAsr, AISpeech.zoomAudioRate);
                                }
                            }

                            mListener.onBufferReceived(dataAsr);
                            // 写入输出数据，此时vadBegin 为 true
                            if (mAudioSaveOutUtil != null) mAudioSaveOutUtil.feedTypeOut(dataAsr);
                        } else {
                            mLimitAudioQueue.offer(dataAsr);
//                            Log.d(TAG, "add " + cacheUtil.getLength());
                            mEngine.feed(AIConstant.AIENGINE_MESSAGE_TYPE_BIN, dataVad, dataVad.length);
                        }

                        if (mAsrSaveUtil != null) mAsrSaveUtil.feedTypeIn(dataAsr);
                        if (mAudioSaveUtil != null) mAudioSaveUtil.feedTypeIn(dataVad);
                    }
                    break;
                case Message.MSG_ERROR:
                    closeAudioSaver();
                    if (mListener != null)
                        mListener.onError((AIError) message.mObject);
                    break;
                case Message.MSG_EVENT:
                    int status = (int) message.mObject;
                    if (status == FLAG_VAD_BEGIN && mListener != null) {
                        if (isStopped) {
                            Log.e(TAG, "state:1 vad start when vad stopped ");
                        } else {
                            vadEnd = false;
                            try {
                                JSONObject customObj = new JSONObject();
                                customObj.put("tag", TAG);
                                customObj.put("tid", Thread.currentThread().getId());
                                JSONObject inputObj = VadInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, new JSONObject().put(IInterceptor.Name.VAD_BEGIN, customObj));
                                SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.VAD_BEGIN, inputObj);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            mRecordID = Utils.get_recordid();
                            Log.d(TAG,"onVadStart : " + mRecordID);
                            mListener.onVadStart(mRecordID);
                            if (!TextUtils.isEmpty(mParams.getSaveAudioPath()) &&
                                    isSaveAudioByScene()) {
                                if (mAudioSaveOutUtil != null) {
                                    mAudioSaveOutUtil.close();
                                    mAudioSaveOutUtil = null;
                                } else {
                                    Log.d(TAG, "error state , this instance should not be null");
                                }
                                mAudioSaveOutUtil = new FileSaveUtil();
                                mAudioSaveOutUtil.init(mParams.getSaveAudioPath(), TAG);
                                String path = mAudioSaveOutUtil.prepare("vad_" + mParams.getRecordId());
                                Log.d(TAG, "create local vad out audio file at: " + path);
                            }
                        }
                    } else if (status == FLAG_VAD_END && mListener != null) {
                        if (isStopped) {
                            Log.e(TAG, "state:2 vad end when vad stopped ");
                        } else {
                            if (!vadEnd) {
                                try {
                                    JSONObject customObj = new JSONObject();
                                    customObj.put("tag", TAG);
                                    customObj.put("tid", Thread.currentThread().getId());
                                    JSONObject inputObj = VadInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, new JSONObject().put(IInterceptor.Name.VAD_END, customObj));
                                    SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.VAD_END, inputObj);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                mListener.onVadEnd(mRecordID);
                                vadEnd = true;
                                if (multiVadPauseTimeNum != -1 && multiVadEndMap != null) {
                                    multiVadEndMap.clear(); //当前一轮vad完成，清空缓存的vadEnd的信息
                                }
                            }
                        }
                        vadBegin = false;
                    }
                    break;
                case Message.MSG_PULL_CACHE:
                    byte[] mData = (byte[]) message.mObject;
                    if (isStopped) {
                        Log.w(TAG, "bin callback: when vad is stopped");
                    } else if (mListener != null) {
                        if (useCalcVolume) {
                            short[] shortBuffer = Util.toShortArray(mData);
                            double db = Util.calcVolume(shortBuffer);
                            Log.v(TAG, "bin callback: data.length " + mData.length + " db " + db);
                            mListener.onRmsChanged((float) db);
                        }
                        Log.d(TAG, "roll back data.length " + mData.length);
                        if ((AISpeech.useDoubleVad && TAG.equals("VadEngine-VadKernel")) || config.isUseDoubleVad()) {
                            byte[] mDataAsr = mLimitAudioQueue.toArray();
                            byte[] dataRollBack = new byte[mData.length];
                            Log.d(TAG, "asr cache length " + mDataAsr.length);
                            if (AISpeech.zoomAudioRate != 1.0f) {
                                // 指定对vad.out单路音频 做缩放处理
                                if ((AISpeech.zoomAudioFlag & DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_VAD_OUT) > 0) {
                                    mDataAsr = ByteConvertUtil.bigPcm(mDataAsr, AISpeech.zoomAudioRate);
                                }
                            }
                            // 将vadBegin之前的数据从缓存队列中拷贝出来，避免拷贝出多余数据
                            if (mDataAsr.length > dataRollBack.length) {
                                System.arraycopy(mDataAsr, mDataAsr.length - mData.length, dataRollBack, 0, mData.length);
                                mListener.onBufferReceived(dataRollBack);
                                if (mAudioSaveOutUtil != null)
                                    mAudioSaveOutUtil.feedTypeOut(dataRollBack);
                            }else {
                                Log.e(TAG,"Error roll back size");
                            }
                        } else {
                            if (AISpeech.zoomAudioRate != 1.0f) {
                                // 指定对vad.out单路音频 做缩放处理
                                if ((AISpeech.zoomAudioFlag & DUILiteConfig.CONFIG_VAL_ZOOM_AUDIO_FLAG_VAD_OUT) > 0) {
                                    mData = ByteConvertUtil.bigPcm(mData, AISpeech.zoomAudioRate);
                                }
                            }
                            mListener.onBufferReceived(mData);
                            if (mAudioSaveOutUtil != null) mAudioSaveOutUtil.feedTypeOut(mData);
                        }
                        vadBegin = true;
                    }
                    break;
                case Message.MSG_TICK:
                    doTick();
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

    private void closeAudioSaver() {
        if (mAudioSaveUtil != null) mAudioSaveUtil.close();
        if (mAsrSaveUtil != null) mAsrSaveUtil.close();
        if (mAudioSaveOutUtil != null) mAudioSaveOutUtil.close();
    }

    private int processVadStatus(String jsonStr) {
        int status = 0;
        try {
            if (!TextUtils.isEmpty(jsonStr)) {
                JSONObject jo = new JSONObject(jsonStr);
                if (jo.has("status")) {
                    status = jo.getInt("status");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
            e.printStackTrace();
        }
        return status;
    }

    protected int initVad(LocalVadConfig config, Vad engine) {
        int status = AIConstant.OPT_FAILED;
        if (config != null) {
            // 创建引擎
            String cfg = (config == null) ? null : config.toString();
            Log.d(TAG, "config" + cfg);
            boolean initSuccess = engine.init(cfg, myVadCallback);
            Log.d(TAG, "vad create return " + initSuccess + ".");
            if (!initSuccess) {
                Log.d(TAG, "引擎初始化失败");
                status = AIConstant.OPT_FAILED;
            } else {
                Log.d(TAG, "引擎初始化成功");
                status = AIConstant.OPT_SUCCESS;
                if(config.isMultiMode() == 1 && config.getPauseTimeArray() != null && config.getPauseTimeArray().length > 0){
                    //必现要一一对应，不能多注册和少注册calback，第一个参数对应init设置myVadCallback，第二个参数对应mySetmultionecbCallback，第三个对应mySetmultitwocbCallback
                    if (config.getPauseTimeArray().length == 2){
                        engine.setmultionecb(myMultionecbCallback);
                    } else if (config.getPauseTimeArray().length == 3){
                        engine.setmultionecb(myMultionecbCallback);
                        engine.setmultitwocb(myMultitwocbCallback);
                    }
                }
            }
        } else {
            status = AIConstant.OPT_FAILED;
        }
        return status;
    }

    protected boolean startVad(SpeechParams param, Vad engine, boolean needRecordIdMap) {
        String paramString = param.toString();
        handlerVadParam(paramString);
        Log.d(TAG, "SpeechParams:\t" + paramString);

        Log.d(TAG, "engine start before");
        boolean ret = engine.start(paramString);
        Log.d(TAG, "engine start end");
        Log.d(TAG, "ret: " + ret);
        if (!ret) {
            mQueue.put(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_AI_ENGINE, AIError.ERR_DESCRIPTION_AI_ENGINE)));
        }
        return ret;
    }

    private class MyMultionecbCallbackImpl extends Vad.multione_callback{
        @Override
        public int run(int dataType, byte[] retData, int size) {
            if (multiVadPauseTimeNum != 1) { //如果multiVadPauseTimeNum为1（index为第二个的vad pauseTime），正常回调
                saveVadEndInfo(dataType, 1, retData);
                return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
            }
            byte[] data = new byte[size];
            System.arraycopy(retData, 0, data, 0, size);

            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {

                Log.d(TAG, "MySetmultionecbCallbackImpl json callback:" + new String(data).trim());
                String retString = Util.newUTF8String(data);
                if (mErrorProcessor != null) {
                    boolean isError = mErrorProcessor.processErrorCallbak(retString);
                    if (isError) {
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(Util.newUTF8String(data))));
                        return AISpeechSDK.AIENGINE_CALLBACK_CANCEL;
                    }
                }
                // 这里的代码执行优先级高于队列中正在处理的消息，类似于中断，将这块的逻辑移至消息队列处理，避免时序问题
                if (isStopped) {
                    Log.e(TAG, "vad stop when call back status 1 !");
                } else {
                    sendMessage(new Message(Message.MSG_EVENT, processVadStatus(retString)));
                }
            }

            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                // 这里的代码执行优先级高于队列中正在处理的消息，类似于中断，将这块的逻辑移至消息队列处理，避免时序问题
                //要触发vadEnd = false,vad处于begin状态才能发送rollBack音频
                Log.d(TAG, "isVadEnd : " + vadEnd);
                if (isStopped) {
                    Log.e(TAG, "vad stop when roll back data !");
                } else {
                    sendMessage(new Message(Message.MSG_PULL_CACHE, data));
                }
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }


    private class MyMultitwocbCallbackImpl extends Vad.multitwo_callback{
        @Override
        public int run(int dataType, byte[] retData, int size) {
            if (multiVadPauseTimeNum != 2) { //如果multiVadPauseTimeNum为2（index为第三个的vad pauseTime），正常回调
                saveVadEndInfo(dataType, 2, retData);
                return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
            }
            byte[] data = new byte[size];
            System.arraycopy(retData, 0, data, 0, size);

            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                Log.d(TAG, "MySetmultitwocbCallbackImpl json callback:" + new String(data).trim());
                String retString = Util.newUTF8String(data);
                if (mErrorProcessor != null) {
                    boolean isError = mErrorProcessor.processErrorCallbak(retString);
                    if (isError) {
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(Util.newUTF8String(data))));
                        return AISpeechSDK.AIENGINE_CALLBACK_CANCEL;
                    }
                }
                // 这里的代码执行优先级高于队列中正在处理的消息，类似于中断，将这块的逻辑移至消息队列处理，避免时序问题
                if (isStopped) {
                    Log.e(TAG, "vad stop when call back status 1 !");
                } else {
                    sendMessage(new Message(Message.MSG_EVENT, processVadStatus(retString)));
                }
            }

            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                // 这里的代码执行优先级高于队列中正在处理的消息，类似于中断，将这块的逻辑移至消息队列处理，避免时序问题
                //要触发vadEnd = false,vad处于begin状态才能发送rollBack音频
                Log.d(TAG, "isVadEnd : " + vadEnd);
                if (isStopped) {
                    Log.e(TAG, "vad stop when roll back data !");
                } else {
                    sendMessage(new Message(Message.MSG_PULL_CACHE, data));
                }
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }

    private class MyVadCallbackImpl extends Vad.vad_callback {
        @Override
        public int run(int dataType, byte[] retData, int size) {
            if (multiVadPauseTimeNum > 0) {  //如果multiVadPauseTimeNum 为-1（正常vad pauseTime） 或者0（index为第一个的vad pauseTime）时，正常回调
                saveVadEndInfo(dataType, 0, retData);
                return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
            }

            byte[] data = new byte[size];
            System.arraycopy(retData, 0, data, 0, size);

            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                Log.d(TAG, "MyVadCallbackImpl json callback:" + new String(data).trim());
                String retString = Util.newUTF8String(data);
                if (mListener != null) {
                    mListener.onResults(retString);
                }
                if (mErrorProcessor != null) {
                    boolean isError = mErrorProcessor.processErrorCallbak(retString);
                    if (isError) {
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(Util.newUTF8String(data))));
                        return AISpeechSDK.AIENGINE_CALLBACK_CANCEL;
                    }
                }
                // 这里的代码执行优先级高于队列中正在处理的消息，类似于中断，将这块的逻辑移至消息队列处理，避免时序问题
                if (isStopped) {
                    Log.e(TAG, "vad stop when call back status 1 !");
                }else{
                    sendMessage(new Message(Message.MSG_EVENT, processVadStatus(retString)));
                }
            }

            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                // 这里的代码执行优先级高于队列中正在处理的消息，类似于中断，将这块的逻辑移至消息队列处理，避免时序问题
                //要触发vadEnd = false,vad处于begin状态才能发送rollBack音频
                Log.d(TAG,"isVadEnd : " + vadEnd);
                if(isStopped){
                    Log.e(TAG,"vad stop when roll back data !");
                }else {
                    sendMessage(new Message(Message.MSG_PULL_CACHE, data));
                }
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }
    /**
     * 动态切换vad pauseTime
     *
     * @param pauseTime
     */
    public void executeVadPauseTime(int pauseTime) {
        if (pauseTimeLists == null) {
            Log.d(TAG, "pauseTimeLists is null");
            return;
        }
        int index = -1;
        for (int i = 0; i < pauseTimeLists.size(); i++) {
            if (pauseTime == pauseTimeLists.get(i)) {
                index = i;
            }
        }
        if (index != -1 && multiVadEndMap != null) {
            multiVadPauseTimeNum = index;
            Boolean isEnd = multiVadEndMap.get(multiVadPauseTimeNum);
            if (isEnd != null && isEnd && !isStopped) {
                Log.d(TAG, "executeVadPauseTime: dynamic switch vad pauseTime,and manual send a vadEnd!");
                sendMessage(new Message(Message.MSG_EVENT, FLAG_VAD_END));
            }
        } else {
            Log.e(TAG, "you must set a pauseTime include in PauseTimeArray when init !");
        }
        Log.d(TAG, "executeVadPauseTime,pauseTime is : " + pauseTime + " multiVadPauseTimeNum is： " + multiVadPauseTimeNum);
    }

    /**
     * 解析vad start的参数，如果有pauseTimeArray，则认为是动态设置vad pauseTime的模式
     * 将pauseTimeArray里面的值保存起来，用于动态切换时做判断
     *
     * @param param
     */
    private void handlerVadParam(String param) {
        if (pauseTimeLists == null) {
            Log.d(TAG, "pauseTimeLists is null!");
            return;
        }
        multiVadPauseTimeNum = -1;
        try {
            JSONObject jsonObject = new JSONObject(param);
            if (jsonObject.has("pauseTimeArray")) {
                pauseTimeLists.clear();
                int pauseTime = jsonObject.optInt("pauseTime");
                JSONArray jsonArray = jsonObject.optJSONArray("pauseTimeArray");
                for (int i = 0; i < jsonArray.length(); i++) {
                    int tmpPauseTime = jsonArray.optInt(i);
                    pauseTimeLists.add(tmpPauseTime);
                    if (pauseTime == tmpPauseTime) {
                        multiVadPauseTimeNum = i;
                    }
                }
                Log.d(TAG, "use multi vad pauseTime,and multiVadPauseTimeNum is " + multiVadPauseTimeNum);
            }
        } catch (JSONException e) {
            Log.e(TAG, "can't parse param ,error msg is : " + e);
        }
    }

    /**
     * 暂存非当前vadPauseTime对应callback是否有回调vadEnd，用于处理动态切换到该vadPauseTime时，能知晓是否过时了vadEnd
     *
     * @param dataType
     * @param pauseTimeIndex
     * @param retData
     */
    private void saveVadEndInfo(int dataType, int pauseTimeIndex, byte[] retData) {
        if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
            String retString = Util.newUTF8String(retData);
            int vadStatus = processVadStatus(retString);
            Log.d(TAG, "use multi pauseTime,did not choose the pauseTime,so discard the callback! vadStatus is : " + vadStatus + " pauseTimeIndex: " + pauseTimeIndex);
            if (vadStatus == FLAG_VAD_END && multiVadEndMap != null) {
                //如果多路vad下，且当前不监听该路vad的回调，则暂存起该vadEnd，如果从大端pauseTime切换到小端pauseTime，则直接返回vadEnd
                multiVadEndMap.put(pauseTimeIndex, true);
                Log.d(TAG, "saveVadEndInfo vadStatus is : " + vadStatus + " pauseTimeIndex: " + pauseTimeIndex);
            }
        }
    }


    /**
     * mVadScene 见 {@link VadScenes}
     *
     * @return
     */
    private boolean isSaveAudioByScene() {
        // 小于0代表该场景不在限制范围内
        if (mVadScene < 0) return true;

        return AISpeech.isSavingEngineAudioEnable(mVadScene);
    }
}
