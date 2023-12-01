package com.aispeech.lite.asr;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AsrUtils;
import com.aispeech.common.FileSaveUtil;
import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.ASRMode;
import com.aispeech.export.engines2.bean.Decoder;
import com.aispeech.gourd.FileBuilder;
import com.aispeech.kernel.Asr;
import com.aispeech.kernel.Itn;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.LocalAsrConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.LocalAsrParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by wuwei on 18-5-14.
 */

public class LocalAsrKernel extends BaseKernel implements Asr.asr_callback {
    public String TAG = "LocalAsrKernel";
    private Asr mAsrEngine;
    private Itn mAggregateMateItn;
    private Itn mItn;
    private final AsrKernelListener mListener;
    private LocalAsrConfig mConfig;
    private LocalAsrParams mParams;
    private volatile boolean isStopped = true;
    private String mRecordId;
    FileSaveUtil mPcmUtil = new FileSaveUtil();
    FileSaveUtil mUploadAudioSaveUtil = new FileSaveUtil();
    private String asrPcmFilePath;
    boolean isMulti = false;
    //判断是否feed了第一帧音频数据
    private AtomicBoolean firstFeedFlag = new AtomicBoolean(true);
    private long startWaitResult;
    private long startTime, stopTime, finalResultTime;

    //统计一次有效音频的内核耗时
    private long feedTimes;
    // itn 文本转化后结果
    private String itnRecResult;
    // 集内匹配结果
    private String aggregateMateRecResult;
    //初始化失败重试次数，背景：https://jira.aispeech.com.cn/browse/YJGGZC-10087
    private int initRetryCount = 5;
    private long getLastResult;

    private String prefixOfTag;


    public LocalAsrKernel(String prefixOfTag, AsrKernelListener listener) {
        super(prefixOfTag + "-asr", listener);
        this.prefixOfTag = prefixOfTag;
        TAG = prefixOfTag + "-LocalAsrKernel";
        this.mListener = listener;
    }

    public LocalAsrKernel(AsrKernelListener listener) {
        super("LocalAsrKernel", listener);
        this.mListener = listener;
    }

    /**
     * 准备资源并初始化引擎
     *
     * @param asr       　内核识别引擎
     * @param configStr 　config参数
     * @return -1(失败)/0(成功)
     */
    private int initAsr(Asr asr, String configStr) {
        int status = AIConstant.OPT_FAILED;
        if (!TextUtils.isEmpty(configStr)) {
            Log.d(TAG, "config: " + configStr);
            long id = asr.init(configStr, this);
            if (id == 0) {
                Log.e(TAG, "引擎初始化失败,请检查资源文件是否在指定路径下！");
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

    /**
     * 更新识别资源
     *
     * @param asr    　内核识别引擎
     * @param config 　config参数
     * @return -1(失败)/0(成功)
     */
    private int updateAsr(Asr asr, String config) {
        int status;
        if (!TextUtils.isEmpty(config)) {
            Log.d(TAG, "config: " + config);
            long id = asr.update(config);
            if (id == 0) {
                Log.d(TAG, "net.bin 更新成功!");
                status = AIConstant.OPT_SUCCESS;
            } else {
                Log.e(TAG, "net.bin 更新失败 , 请检查资源文件是否在指定路径下！");
                status = AIConstant.OPT_FAILED;
            }
        } else {
            status = AIConstant.OPT_FAILED;
        }
        return status;
    }
    protected int startAsr(String paramStr, Asr engine) {
        Log.d(TAG, "engine start before  paramStr " + paramStr);

        startTime = System.currentTimeMillis();
        int ret = engine.start(paramStr);
        Log.d(TAG, "engine start end params " + paramStr + " ret " + ret);
        if (ret < 0) {
            AIError aiError = new AIError(AIError.ERR_AI_ENGINE, AIError.ERR_DESCRIPTION_AI_ENGINE, mRecordId);
            sendMessage(new Message(Message.MSG_ERROR, aiError));
            return ret;
        }
        return ret;
    }
    /**
     * 准备资源并初始化引擎
     *
     * @param itn      文字转数字引擎
     * @param cfg      　config参数
     * @param callback 返回转换后的文本
     * @return -1(失败)/0(成功)
     */
    private int initItn(Itn itn, String cfg, Itn.itn_callback callback) {
        int status = AIConstant.OPT_FAILED;
        if (callback != null) {
            Log.d(TAG, "config: " + cfg);
            long id = itn.init(cfg, callback);
            if (id == 0) {
                Log.e(TAG, "引擎初始化失败,请检查ITN所需资源文件是否在指定路径下！");
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

    /**
     * 送入需转换的"字符串"，处理后通过itn_callback送出（非异步处理）
     *
     * @param str 识别结果
     * @return
     */
    protected int feedItn(final String str) {
        Log.cost(Log.TagCostTime.ASR_RESULT, Log.TagCostTime.ITN_RESULT, new Log.CostCallback() {
            @Override
            public void doIt() {
                if (mItn != null) {
                    itnRecResult = null;
                    int status = -1;
                    try {
                        status = mItn.feed(str);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (status < 0) {
                        Log.e(TAG, "ITN CONVERT FAIL");
                    } else {
                        Log.d(TAG, "ITN CONVERT " + str + " -->  " + itnRecResult);
                    }
                }
            }
        });
        return AIConstant.OPT_SUCCESS;
    }

    /**
     * 送入需转换的"字符串"，处理后通过itn_callback送出（非异步处理）
     *
     * @param str 识别结果
     * @return
     */
    protected int feedAggregateMate(final String str) {
        Log.cost(Log.TagCostTime.ASR_RESULT, Log.TagCostTime.ITN_RESULT, new Log.CostCallback() {
            @Override
            public void doIt() {
                if (mAggregateMateItn != null) {
                    aggregateMateRecResult = null;
                    int status = -1;
                    try {
                        status = mAggregateMateItn.feed(str);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (status < 0) {
                        Log.e(TAG, "AGGREGATE MATE FAIL");
                    } else {
                        Log.d(TAG, "AGGREGATE MATE " + str + " -->  " + aggregateMateRecResult);
                    }
                }
            }
        });
        return AIConstant.OPT_SUCCESS;
    }

    /***
     * 重启引擎
     */
    public void resetKernel() {
        sendMessage(new Message(Message.MSG_EVENT));
    }


    /**
     * 注册、反注册、查询解码网络
     *
     * @param decoders 解码网络
     */
    public void decoder(Decoder... decoders) {
        sendMessage(new Message(Message.MSG_DECODER, decoders));
    }
    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    AnalysisProxy.getInstance().updateConfig(false);
                    mAsrEngine = new Asr();
                    LocalAsrConfig config = (LocalAsrConfig) message.mObject;
                    mConfig = config;
                    int flag = initAsr(mAsrEngine, config.toJson().toString());
                    if (flag == AIConstant.OPT_FAILED) {
                        for (int i = 1; i <= initRetryCount; i++) {
                            Log.w(TAG, "引擎初始化失败，重试：" + i);
                            flag = initAsr(mAsrEngine, config.toJson().toString());
                            if (flag == AIConstant.OPT_SUCCESS) {
                                break;
                            }
                            synchronized (this) {
                                try {
                                    this.wait(50 * i);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    mListener.onInit(flag);
                    if (config.isUseAggregateMate()) {
                        mAggregateMateItn = new Itn();
                        initItn(mAggregateMateItn, config.toAggregateMateJson().toString(), aggregate_mate_callback);
                    }
                    if (config.isUseItn()) {
                        mItn = new Itn();
                        initItn(mItn, config.toItnJson().toString(), itn_callback);
                    }
                    break;
                case Message.MSG_START:
                    feedTimes = 0;
                    mParams = (LocalAsrParams) message.mObject;
                    //检查ExpandFnPath有效性
                    if (!TextUtils.isEmpty(mParams.getExpandFnPath())) {
                        AsrUtils.checkExpandFnValid(mParams.getExpandFnPath());
                    }
                    String paramStr = mParams.toJSON().toString();
                    //TODO LocalAsrKernel RecorderId select
                    mRecordId = mParams.getRecordId();
                    if (TextUtils.isEmpty(mRecordId)) {
                        mRecordId = Utils.getRecorderId();
                    }
                    String saveAudioPath = mParams.getSaveAudioPath();
                    if (!TextUtils.isEmpty(saveAudioPath) && mPcmUtil != null) {
                        mPcmUtil.init(saveAudioPath, prefixOfTag);
                        String path = mPcmUtil.prepare("local_asr_" + mRecordId);
                        Log.d(TAG, "create local asr audio file at: " + path);
                    }

                    if (AISpeech.cacheUploadEnable && AnalysisProxy.getInstance().getAnalysisAudioLocalASR().isUploadEnable()) {
                        if (mUploadAudioSaveUtil == null) {
                            mUploadAudioSaveUtil = new FileSaveUtil();

                        }
                        mUploadAudioSaveUtil.init(getUploadAudioFilePath());
                        asrPcmFilePath = mUploadAudioSaveUtil.prepare(mRecordId);
                        Log.v(TAG, "enable asr upload,path: " + asrPcmFilePath);
                    }

                    Log.d(TAG, "local asr param: " + paramStr);
                    if (mAsrEngine != null) {
                        startAsr(paramStr, mAsrEngine);
                        Log.d(TAG, "LOCAL.ASR.BEGIN");
                        //置位
                        firstFeedFlag.compareAndSet(true, false);
                        isStopped = false;
                    }
                    break;

                case Message.MSG_DECODER:
                    final Decoder[] decoders = (Decoder[]) message.mObject;
                    Log.cost("decoder.begin", "decoder.end", new Log.CostCallback() {
                        @Override
                        public void doIt() {
                            int opt = 0;
                            for (int i = 0; i < decoders.length; i++) {
                                Decoder decoder = decoders[i];
                                if (mAsrEngine != null) {
                                    opt = mAsrEngine.decoder(decoder.getAction(), decoder.toJSON());
                                    Log.d(TAG, "opt=" + opt + ", decoder: " + decoder.toJSON());
                                }
                            }
                            if (mListener != null) {
                                mListener.onUpdateResult(opt);
                            }
                        }
                    });

                    break;

                case Message.MSG_STOP:
                    if (mAsrEngine != null) {
                        Log.d(TAG, "LOCAL.ASR.STOP");
                        startWaitResult = System.currentTimeMillis();
                        Log.d(TAG, "startWaitResult : " + startWaitResult);
                        mAsrEngine.stop();
                    }
                    isStopped = true;
                    if (mPcmUtil != null) mPcmUtil.close();
                    if (mUploadAudioSaveUtil != null) mUploadAudioSaveUtil.close();
                    break;
                case Message.MSG_CANCEL:
                    if (mAsrEngine != null) {
                        Log.d(TAG, "LOCAL.ASR.CANCEL");
                        startWaitResult = System.currentTimeMillis();
                        Log.d(TAG, "startWaitResult : " + startWaitResult);
                        mAsrEngine.cancel();
                        Log.v(TAG, "cancel.wait.time : " + System.currentTimeMillis());
                    }
                    isStopped = true;
                    if (mPcmUtil != null) mPcmUtil.close();
                    if (mUploadAudioSaveUtil != null) mUploadAudioSaveUtil.close();
                    break;
                case Message.MSG_RELEASE:
                    // 销毁引擎
                    if (mAsrEngine != null) {
                        mAsrEngine.destroy();
                        mAsrEngine = null;
                    }
                    if (mItn != null) {
                        mItn.destroy();
                    }
                    if (mAggregateMateItn != null) {
                        mAggregateMateItn.destroy();
                    }
                    isStopped = true;
                    isReleased = true;
                    break;
                case Message.MSG_UPDATE: //识别引擎重置
                    if (mAsrEngine != null) {
                        String updateConfig = (String) message.mObject;
                        int status = updateAsr(mAsrEngine, updateConfig);
                        Log.d(TAG, "update asr status : " + status);
                        if (status == AIConstant.OPT_FAILED) {
                            mListener.onError(new AIError(AIError.ERR_NET_BIN_INVALID,
                                    AIError.ERR_DESCRIPTION_INVALID_NET_BIN));
                        }
                        mListener.onUpdateResult(status);//更新结果通知
                    }
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    if (mAsrEngine != null && !isStopped && !isEmpty(data)) {
                        if (firstFeedFlag.compareAndSet(false, true) && (data.length != 0)) {
                            Log.d(TAG, "LOCAL.ASR.FIRST.FEED");
                        }
                        mAsrEngine.feed(data);
                        if (mPcmUtil != null) mPcmUtil.feedTypeIn(data);
                        if (mUploadAudioSaveUtil != null) mUploadAudioSaveUtil.feedTypeCustom(data);
                    }
                    break;
                case Message.MSG_ERROR:
                    mListener.onError((AIError) message.mObject);
                    break;
                case Message.MSG_EVENT:
                    Log.d(TAG, "MSG_EVENT");
                    if (mAsrEngine != null) {
                        mAsrEngine.destroy();
                    }
                    int status = initAsr(mAsrEngine, mConfig.toJson().toString());
                    Log.d(TAG, "init asr status : " + status);
                    if (status == AIConstant.OPT_FAILED) {
                        mListener.onError(new AIError(AIError.ERR_NET_BIN_INVALID,
                                AIError.ERR_DESCRIPTION_INVALID_NET_BIN));
                    }
                    if (mAsrEngine != null) {
                        startAsr(mParams.toJSON().toString(), mAsrEngine);
                        Log.d(TAG, "LOCAL.ASR.BEGIN");
                        //置位
                        firstFeedFlag.compareAndSet(true, false);
                        isStopped = false;
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

    @Override
    public void cancelKernel() {
        isStopped = true;
        super.cancelKernel();
    }
    private boolean isEmpty(byte[] data) {
        return data == null || data.length == 0;
    }

    private void processLocalAsrCallback(String localAsrRetStr) {
        Log.d(TAG, "LOCAL.ASR.RESULT: " + localAsrRetStr);
        try {
            if (localAsrRetStr != null && localAsrRetStr.contains("FILLER")) {
                localAsrRetStr = localAsrRetStr.replaceAll("FILLER", "").replaceAll(" ", "").trim();
            }
            JSONObject resultJson = new JSONObject(localAsrRetStr);
            if (resultJson != null) {
                AIResult result = new AIResult();
                result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
                result.setResultObject(resultJson);
                result.setTimestamp(System.currentTimeMillis());
                result.setRecordId(mRecordId);
                int eof = 0;
                double wavetime = 1;
                double conf = resultJson.optDouble("conf", 100); // 没取到这个字段，则给1，对外抛出
                if (resultJson.has("eof")) {
                    eof = resultJson.optInt("eof", 1);
                } else if (resultJson.has("grammar")) {
                    JSONObject gramJO = resultJson.optJSONObject("grammar");
                    if (gramJO.has("eof")) {
                        eof = gramJO.optInt("eof", 1);
                    }
                    Log.d(TAG, "eof in grammar is: " + eof);
                    if (eof == 1) {
                        wavetime = gramJO.optDouble("wavetime");
                    }
                } else if (resultJson.has("ngram")) {
                    JSONObject ngramJO = resultJson.optJSONObject("ngram");
                    if (ngramJO.has("eof")) {
                        eof = ngramJO.optInt("eof", 1);
                    }
                    if (eof == 1) {
                        wavetime = ngramJO.optDouble("wavetime");
                    }
                    Log.d(TAG, "eof in ngramJO is: " + eof);
                }
                if (eof == 1) {
                    if (resultJson.has("wavetime")) {
                        wavetime = resultJson.optDouble("wavetime");
                    }
                    Log.v(TAG, String.format(Locale.CHINA, "rtf: %.2f", +feedTimes / wavetime));
                    result.setResultObject(resultJson);
                    result.setLast(true);
                    getLastResult = System.currentTimeMillis();
                    finalResultTime = getLastResult;
                    Log.d(TAG, "getLastResult : " + getLastResult);
                    Log.d(TAG, "LOCAL.ASR.RESULT.DELAY: " + (getLastResult - startWaitResult) + "ms");
                    Log.cost(Log.TagCostTime.ASR_START, Log.TagCostTime.ASR_RESULT, startTime, finalResultTime);
                    Log.cost(Log.TagCostTime.VAD_END, Log.TagCostTime.ASR_RESULT, stopTime, finalResultTime);
                    Log.v(TAG, "LOCAL.ASR.RESULT.DELAY: " + (finalResultTime - stopTime) + "ms");
                    Log.in(TAG, "LOCAL.ASR.RESULT: " + resultJson.toString());

                    uploadCost();
                    uploadAudio(resultJson);
                } else {
                    result.setLast(false);
                }
                if (mListener != null) {
                    mListener.onResults(filter(result));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int run(int type, byte[] retData, int size) {
        byte[] data = new byte[size];
        System.arraycopy(retData, 0, data, 0, size);
        Log.d(TAG, "LOCAL.ASR.CALLBACK: " + new String(data).trim());
        if (mPcmUtil != null) mPcmUtil.close();
        String result = new String(data);
        try {
            JSONObject resultJo = new JSONObject(result);
            // 兼容新多路解码方案,https://jira.aispeech.com.cn/browse/YJGGZC-6457
            if (resultJo.has("callback")) {
                resultJo = resultJo.optJSONObject(resultJo.optString("callback"));
                isMulti = true;
            }

            //设置了唤醒词的情况下，过滤掉开头位置的唤醒词。类似云端识别的唤醒词过滤
            if (mParams.getWakeupWords() != null && mParams.getWakeupWords().length > 0) {
//                Log.d(TAG, "getWakeupWords()" + mParams.getWakeupWords().length);
                JSONObject grammarJo = resultJo.optJSONObject("grammar");
                JSONObject ngramJo = resultJo.optJSONObject("ngram");
                String grammar = "";
                String ngram = "";
                if (grammarJo != null || ngramJo != null) {
                    if (grammarJo != null) {
                        String grammarRec = grammarJo.optString("rec");
                        grammarRec = Util.removeSpaceBetweenChinese(grammarRec);
                        grammar = removeWakeWords(grammarRec, mParams.getWakeupWords());
                        grammarJo.put("rec", grammar);
                    }

                    if (ngramJo != null) {
                        String ngramJoRec = ngramJo.optString("rec");
                        ngramJoRec = Util.removeSpaceBetweenChinese(ngramJoRec);
                        ngram = removeWakeWords(ngramJoRec, mParams.getWakeupWords());
                        ngramJo.put("rec", ngram);
                    }
                    result = resultJo.put("grammar", grammarJo).put("ngram", ngramJo).toString();

                } else {
                    String rec;
                    if (resultJo.optString("rec") != null) {
                        rec = resultJo.optString("rec");
                        if (rec != null) {
                            rec = rec.replaceAll(" ", "");
                        }
                        rec = removeWakeWords(rec, mParams.getWakeupWords());
                        result = resultJo.put("rec", rec).toString();
                    }

                }
                resultJo.put("grammar", grammarJo)
                        .put("ngram", ngramJo);
            }
            result = resultJo.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.v(TAG, "LOCAL.ASR.CALLBACK : " + result.trim());
        if (!isStopped) {
            processLocalAsrCallback(result.trim());
        } else {
            Log.d(TAG, "drop already stop result ");
        }
        return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
    }
    /**
     * Itn 文本转数字 输出文本
     */
    private Itn.itn_callback itn_callback = new Itn.itn_callback() {
        @Override
        public int run(int type, byte[] data, int size) {
            byte[] itnData = new byte[size];
            System.arraycopy(data, 0, itnData, 0, size);
            itnRecResult = new String(itnData);

            return 0;
        }
    };

    /**
     * 集内匹配
     */
    private final Itn.itn_callback aggregate_mate_callback = new Itn.itn_callback() {
        @Override
        public int run(int type, byte[] data, int size) {
            byte[] itnData = new byte[size];
            System.arraycopy(data, 0, itnData, 0, size);
            aggregateMateRecResult = new String(itnData);

            return 0;
        }
    };

    /**
     * 获取itn转化后的文本，
     *
     * @return
     */
    public String getItnRecResult() {
        return itnRecResult;
    }

    /**
     * 获取集内匹配查询后的文本
     * @return
     */
    public String getAggregateMateRecResult() {
        return aggregateMateRecResult;
    }

    /**
     * 过滤开头的唤醒词
     *
     * @param rec         识别结果
     * @param wakeupWords 唤醒词列表
     * @return 过滤唤醒词后的结果
     */
    private String removeWakeWords(String rec, String[] wakeupWords) {
        String result = rec;

        for (String wakeupWord : wakeupWords) {
            if (!TextUtils.isEmpty(rec) && rec.contains(wakeupWord)) {

                String[] recResult = rec.split(wakeupWord);
                if (recResult.length > 1) {
                    result = recResult[recResult.length - 1];
                } else {
                    return "";
                }

                break;
            }
        }
        Log.d(TAG, "recResult  " + result + "  wakeupWords  " + wakeupWords.length);
        return result;

    }
    private void uploadCost() {
        //添加message外面的字段
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("recordId", mRecordId);
        entryMap.put("mode", "lite");
        entryMap.put("module", "local_cost");
        JSONObject inputJson = new JSONObject();
        try {
            if (mConfig != null) {
                inputJson.put("config", mConfig.toJson());
            }
            if (mParams != null) {
                inputJson.put("param", mParams.toJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JSONObject outputJson = new JSONObject();
        try {
            outputJson.put("asrtotalcost", getLastResult - startWaitResult);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("local_asr_cost", "info", "local_cost",
                mRecordId, inputJson, outputJson, entryMap);
    }

    private void uploadAudio(JSONObject asrJSON) {
        boolean isUploadAudio = AnalysisProxy.getInstance().getAnalysisAudioLocalASR().isUploadEnable();
        isUploadAudio = isUploadAudio && !TextUtils.isEmpty(asrPcmFilePath) && new File(asrPcmFilePath).exists();
        if (!isUploadAudio) return;

        if (!checkConf(asrJSON)) {
            Log.d(TAG, "uploadAudio: current conf is out of range del " + asrPcmFilePath);
            FileUtils.deleteFile(asrPcmFilePath);
            return;
        }

        Log.i(TAG, "asr start uploadAudio");
        //添加message外面的字段
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("recordId", mRecordId);
        entryMap.put("mode", "lite");
        entryMap.put("module", "local_ASRLite");
        entryMap.put("asrMode", prefixOfTag);
        JSONObject inputJson = new JSONObject();
        try {
            if (mConfig != null) {
                inputJson.put("config", mConfig.toJson());
            }
            if (mParams != null) {
                inputJson.put("param", mParams.toJSON());
            }

            JSONObject audioParams = new JSONObject();
            audioParams.put("audioType", AnalysisProxy.isLasrAudioEncode() ? AnalysisProxy.FILE_FORMAT_OPUS : AnalysisProxy.FILE_FORMAT_PCM);
            audioParams.put("channel", 1);
            audioParams.put("sampleBytes", 2);
            audioParams.put("sampleRate", 16000);
            inputJson.put("audio", audioParams);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        String uploadFileName = mRecordId + "." + (AnalysisProxy.isLasrAudioEncode() ? AnalysisProxy.FILE_FORMAT_OPUS : AnalysisProxy.FILE_FORMAT_PCM);
        JSONObject outputJson = new JSONObject();
        try {
            outputJson.put("asrtotalcost", getLastResult - startWaitResult);
            outputJson.put("asr", asrJSON);
            outputJson.put("audioUrl", uploadFileName);

            String filePath = asrPcmFilePath;

            long fileLength = new File(filePath).length();

            if (fileLength < AISpeech.uploadAudioMaxLength) {
                if (fileLength > 0) {
                    int duration = (int) (fileLength / 32);
                    outputJson.put("duration", duration);
                }


                FileBuilder fileBuilder = new FileBuilder();
                fileBuilder.setPath(filePath);
                fileBuilder.setFileName(uploadFileName);
                fileBuilder.setEncode(AnalysisProxy.isLasrAudioEncode() ? AnalysisProxy.FILE_FORMAT_OPUS : AnalysisProxy.FILE_FORMAT_PCM);

                JSONObject object = new JSONObject();
                object.put("mode", prefixOfTag);
                fileBuilder.setExtraString(object.toString());
                AnalysisProxy.getInstance().getAnalysisAudioLocalASR().cacheFileBuilder(fileBuilder);
            } else {
                entryMap.put("asr_upload_error", "file length oversize:" + fileLength);
                FileUtils.deleteFile(filePath);
            }


        } catch (JSONException e) {
            e.printStackTrace();
        }
        AnalysisProxy.getInstance().getAnalysisAudioLocalASR().cacheData("local_ASRLite_audio", "info", "local_ASRLite",
                mRecordId, inputJson, outputJson, entryMap);
        AnalysisProxy.getInstance().getAnalysisAudioLocalASR().start();
    }

    private boolean checkConf(JSONObject asrJSON) {
        double uploadThresh = AnalysisProxy.getLocalAsrUploadThresh();

        if (uploadThresh > 1) return true;

        if (isMulti) {
            // 多路识别 只要有一个置信度没达到阈值，就进行上传
            Iterator<String> keys = asrJSON.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject jsonObject = asrJSON.optJSONObject(key);
                if (jsonObject != null) {
                    double conf = jsonObject.optDouble("conf", 0);
                    if (conf != 0 && conf < uploadThresh) return true;
                }
            }
        } else {
            // 单路
            double conf = asrJSON.optDouble("conf", 1); // 没取到这个字段，则给1，对外抛出
            return conf < uploadThresh;
        }

        return false;
    }

    private String getUploadAudioFilePath() {
        if (TextUtils.isEmpty(AISpeech.uploadAudioPath)) {
            return AISpeech.getContext().getCacheDir()
                    .getPath() + File.separator + "gourd" + File.separator + "localASR";
        } else {
            return AISpeech.uploadAudioPath + File.separator + "localASR";
        }
    }

    /**
     * 根据当前引擎启动模式过滤识别结果
     *
     * @param result 包含多个识别结果
     */
    private AIResult filter(AIResult result) {
        try {
            JSONObject retObj = new JSONObject(result.getResultObject().toString());
            if (mParams.getMode() == ASRMode.MODE_ASR.getValue()) {//识别模式时，过滤热词结果
                if (retObj.has("dynamic"))
                    retObj.remove("dynamic");
            } else if (mParams.getMode() == ASRMode.MODE_HOTWORD.getValue()) {//热词模式时，过滤识别结果
                if (retObj.has("grammar"))
                    retObj.remove("grammar");
                if (retObj.has("ngram"))
                    retObj.remove("ngram");
            }
            result.setResultObject(retObj);
        } catch (JSONException exception) {
            exception.printStackTrace();
        }
        return result;
    }
}
