package com.aispeech.lite.asr;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.DUILiteConfig;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AudioHelper;
import com.aispeech.common.FileIOUtils;
import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.common.NetworkUtil;
import com.aispeech.common.SlotUtil;
import com.aispeech.common.Util;
import com.aispeech.export.Vocab;
import com.aispeech.export.engines2.bean.Decoder;
import com.aispeech.export.interceptor.AsrInterceptor;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.kernel.Ngram;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.AIEngineConfig;
import com.aispeech.lite.config.LocalAsrConfig;
import com.aispeech.lite.config.LocalNgramConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.param.LocalAsrParams;
import com.aispeech.lite.param.LocalNgramParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.vad.VadKernel;
import com.aispeech.lite.vad.VadKernelListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by xueyong.zou on 2021/1/19.
 */

public class LocalAsrProcessor extends BaseProcessor {
    public static String TAG = "LocalAsrProcessor";
    private BaseKernel mAsrKernel;
    private LocalAsrParams mAsrParams;
    private AIEngineConfig mAsrConfig;
    private VadKernel mVadKernel;
    private VadParams mVadParams;
    private LocalVadConfig mVadConfig;
    private SpeechListener mListener;

    private long mVadBeginTime;
    private long mVadEndTime;
    private long mAsrResultTime;

    private String holderFst;//保存当前兜底的网络类型
    private Map<String, Double> decoderThreshMaps; // 解码网络的阈值集合
    private Map<String, Double> dynamicThreshMaps; // 热词指令的阈值集合


    public void init(SpeechListener listener, AIEngineConfig asrConfig, LocalVadConfig vadConfig) {

        mListener = listener;

        if (vadConfig.isVadEnable()) {
            threadCount++;
        }
        mScope = Scope.LOCAL_ASR;
        init(mListener, asrConfig.getContext(), TAG);

        mAsrKernel = new LocalAsrKernel("multi", new MyAsrKernelListener());
        mAsrKernel.setProfile(mProfile);
        // config 需要保证在同一个线程中赋值和置空
        Object[] configObj = new Object[]{asrConfig, vadConfig};
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, configObj);

    }


    public void start(LocalAsrParams asrParams, VadParams vadParams) {
        if (isAuthorized()) {
            // param 需要保证在同一个线程中赋值和置空
            Object[] paramsObj = new Object[]{asrParams, vadParams};
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, paramsObj);
        } else {
            showErrorMessage();
        }
    }

    public void updateVocab(Vocab... vocabs) {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE_VOCAB, vocabs.clone());
        } else {
            showErrorMessage();
        }
    }

    public void decoder(Decoder... decoders) {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_DECODER, decoders);
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void clearObject() {
        super.clearObject();
        if (mAsrKernel != null) {
            mAsrKernel.releaseKernel();
            mAsrKernel = null;
        }
        if (mAsrParams != null)
            mAsrParams = null;
        if (mAsrConfig != null)
            mAsrConfig = null;
        if (mVadKernel != null) {
            mVadKernel.releaseKernel();
            mVadKernel = null;
        }
        if (mVadParams != null)
            mVadParams = null;
        if (mVadConfig != null)
            mVadConfig = null;
        if (mListener != null)
            mListener = null;
    }

    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                handleMsgNew((Object[]) msg.obj);
                break;
            case MSG_UPDATE:
                handleMsgUpdate((String) msg.obj);
                break;
            case MSG_UPDATE_VOCAB:
                handleMsgUpdateVocab((Vocab[]) msg.obj);
                break;
            case MSG_DECODER:
                handleMsgDecoder((Decoder[]) msg.obj);
                break;
            case MSG_START:
                handleMsgStart((Object[]) msg.obj);
                break;
            case MSG_RECORDER_START:
                handleMsgRecorderStart();
                break;
            case MSG_STOP:
                handleMsgStop();
                break;
            case MSG_CANCEL:
                handleMsgCancel();
                break;
            case MSG_RAW_RECEIVE_DATA:
                final byte[] rawBufferData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mListener != null) {
                        mListener.onRawDataReceived(rawBufferData, rawBufferData.length);
                    }
                }
                break;
            case MSG_RESULT_RECEIVE_DATA:
                //此MSG包含录音机/自定义feed音频来源
                final byte[] bufferData = (byte[]) msg.obj;
                handleMsgResultPcmData(bufferData);
                break;
            case MSG_VAD_RECEIVE_DATA:
                final byte[] vadData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    mAsrKernel.feed(vadData);
                }
                break;
            case MSG_VAD_START:
                handleMsgVadStart();
                break;
            case MSG_VAD_END:
                handleMsgVadEnd();
                break;
            case MSG_VOLUME_CHANGED:
                handleMsgVolumeChanged((float) msg.obj);
                break;
            case MSG_RESULT:
                handleMsgResult((AIResult) msg.obj);
                break;
            case MSG_RELEASE:
                handleMsgRelease();
                break;
            case MSG_ERROR:
                handleMsgError((AIError) msg.obj);
                break;
            default:
                break;
        }
    }

    private void handleMsgNew(Object[] configObj) {
        if (mState == EngineState.STATE_IDLE) {
            mAsrConfig = (AIEngineConfig) configObj[0];
            mVadConfig = (LocalVadConfig) configObj[1];

            int status = copyAssetsFolders(mAsrConfig);
            // 开启itn模式需要copy itn下指定目录
            int itnStatus = copyAssetsFolder(mAsrConfig);

            if (status == AIConstant.OPT_FAILED || itnStatus == AIConstant.OPT_FAILED) {
                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                return;
            }
            if (mVadConfig != null && mVadConfig.isVadEnable()) {
                status = copyAssetsRes(mVadConfig);
                if (status == AIConstant.OPT_FAILED) {
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                            AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                    return;
                }
                mVadKernel = new VadKernel("asr", new MyVadKernelListener());
                mVadKernel.newKernel(mVadConfig);
            }
            if (mAsrKernel != null) {
                mAsrKernel.newKernel(mAsrConfig);
            }

            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("new");
        }
    }

    /**
     * 更新词库
     *
     * @param vocabs 待更新的词库
     */
    private void handleMsgUpdateVocab(Vocab[] vocabs) {
        if (vocabs == null && vocabs.length <= 0) {
            Log.e(TAG, "vocabs is null, please set vocab !!");
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, AIConstant.OPT_FAILED);
            return;
        }

        if (mState == EngineState.STATE_NEWED) {
            //依赖ngram支持更新词库 https://jira.aispeech.com.cn/browse/YJGGZC-6326
            LocalAsrConfig asrConfig = (LocalAsrConfig) mAsrConfig;
            if (TextUtils.isEmpty(asrConfig.getNgramSlotBinPath())) {
                Log.e(TAG, "ngramSlotBinPath is null, please set ngramSlotBinPath !!");
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, AIConstant.OPT_FAILED);
                return;
            }
            LocalNgramConfig config = new LocalNgramConfig();
            config.setResBinPath(asrConfig.getNgramSlotBinPath());
            String configStr = config.toJson().toString();
            Log.d(TAG, "config: " + configStr);
            Ngram ngram = new Ngram();
            long id = ngram.init(configStr);
            if (id == 0) {
                Log.e(TAG, "词库更新失败,请检查资源文件是否在指定路径下！");
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, AIConstant.OPT_FAILED);
            } else {
                LocalNgramParams ngramParams = new LocalNgramParams();
                int ret = -1;
                for (Vocab vocab : vocabs) {
                    String outputPath = vocab.getOutputPath();
                    if (TextUtils.isEmpty(outputPath)) {
                        File ngramResFile = new File(asrConfig.getResBinPath());
                        outputPath = ngramResFile.getParentFile().getAbsolutePath() + File.separator + mAsrConfig.getTag() + "_" + vocab.getName() + ".bin";
                    } else {
                        File file = new File(outputPath);
                        //外部传入的文件夹没有创建，则先创建
                        if (!file.getParentFile().exists()) {
                            file.getParentFile().mkdirs();
                        }
                    }
                    ngramParams.setOutputPath(outputPath);
                    if (!TextUtils.isEmpty(vocab.getInputPath())) {
                        if (TextUtils.isEmpty(FileIOUtils.readFile2StringDelSpecial(vocab.getInputPath()))) {
                            Log.w(TAG, "clear vocab data : " + vocab.getName());
                            ngramParams.setSlotData(vocab.getName());
                        } else {
                            ngramParams.setInputPath(vocab.getInputPath());
                        }
                    } else {
                        ngramParams.setSlotData(SlotUtil.vocab2Slot(vocab));
                    }
                    String params = ngramParams.toJson().toString();
                    Log.d(TAG, "params: " + params);
                    ret = ngram.start(params);
                }
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, ret < 0 ? AIConstant.OPT_FAILED : AIConstant.OPT_SUCCESS);
                ngram.destroy();
            }
        } else {
            trackInvalidState("updateVocab");
            Log.e(TAG, "词库更新失败，请在init后和start前调用！");
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, AIConstant.OPT_FAILED);
        }
    }

    private void handleMsgDecoder(Decoder[] decoders) {
        if (decoders == null) {
            Log.e(TAG, "decoders is null, please set decoder info !!");
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, AIConstant.OPT_FAILED);
            return;
        }
        if (mState == EngineState.STATE_NEWED) {
            if (mAsrKernel != null && mAsrKernel instanceof LocalAsrKernel) {
                for (Decoder decoder : decoders) {
                    if (decoderThreshMaps == null) {
                        decoderThreshMaps = new HashMap<>();
                    }
                    decoderThreshMaps.put(decoder.getName(), decoder.getThreshHold());
                    if (Decoder.NetType.DYNAMIC.getType().equals(decoder.getType())) {
                        if (dynamicThreshMaps == null) {
                            dynamicThreshMaps = new HashMap<>();
                        }
                        if (decoder.getDynamicThresholds() != null && decoder.getDynamicThresholds().size() > 0) {
                            dynamicThreshMaps.putAll(decoder.getDynamicThresholds());
                        }
                    } else {
                        //资源兼容支持assets相对路径和指定绝对路径
                        String resDir = Util.getResourceDir(AISpeech.getContext());
                        String netBinPath = resDir + File.separator + decoder.getNetBin();
                        if (checkCopyAssetsRes(decoder.getNetBin(), netBinPath)) {
                            decoder.setNetBin(netBinPath);
                        }
                        String expandFnPath = resDir + File.separator + decoder.getExpandFn();
                        if (checkCopyAssetsRes(decoder.getExpandFn(), expandFnPath)) {
                            decoder.setExpandFn(expandFnPath);
                        }
                    }
                    if (decoder.isHolderFst()) {
                        if (Decoder.Action.REGISTER.getAction().equals(decoder.getAction())) {
                            //只会保存第一个isHolderFst = true 解码网络为兜底
                            holderFst = decoder.getName();
                        }
                        if (Decoder.Action.UNREGISTER.getAction().equals(decoder.getAction())) {
                            holderFst = null;
                        }
                        break;
                    }
                }
                ((LocalAsrKernel) mAsrKernel).decoder(decoders);
            }
        } else {
            trackInvalidState("decoder");
        }
    }

    private boolean checkCopyAssetsRes(String assetsPath, String destResPath) {
        if (!TextUtils.isEmpty(assetsPath) && !assetsPath.startsWith("/")) {
            if (new File(destResPath).exists()) return true;
            int ret = Util.copyFilesFromAssets(mContext, assetsPath, destResPath);
            if (ret == 0) return true;
            Log.e(TAG, "file " + assetsPath + " not found in assets folder, Did you forget add it?");
        }
        return false;
    }

    private void handleMsgRecorderStart() {
        if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
            if (mAsrKernel != null) {
                mAsrKernel.startKernel(mAsrParams);
            }
            if (mVadKernel != null && mVadParams != null && mVadParams.isVadEnable()) {
                startNoSpeechTimer(mAsrParams);
                if (mVadKernel != null) {
                    mVadKernel.startKernel(mVadParams);
                }
            }
            if (mAsrParams.getOneshotCache() != null) {
                OneshotCache<byte[]> buffer = mAsrParams.getOneshotCache();
                if (buffer.isValid()) {
                    OneshotCache.OneshotIterator iterator = buffer.iterator();
                    while (iterator.hasNext()) {
                        byte[] data = (byte[]) iterator.next();
                        if (data != null) {
                            feedData(data, data.length);
                        }
                    }
                }
            }
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_READY_FOR_SPEECH, null);
            transferState(EngineState.STATE_RUNNING);
        } else {
            trackInvalidState("recorder start");
        }
    }

    /**
     * start 预处理，比如ngram_slot.json
     */
    private void handlePreStart() {
        Log.d(TAG, "before handlePreStart!");
        try {
            String expandFnPath = mAsrParams.getExpandFnPath();
            Log.i(TAG, "expandFnPath : " + expandFnPath);
            if (!TextUtils.isEmpty(expandFnPath)) {
                // fix https://jira.aispeech.com.cn/browse/VEHICLEDUILFC-191
                // 兼容assets路径文件，将文件判断及复制动作放到progress中操作
                if (!expandFnPath.startsWith("/")) {
                    int ret = Util.copyResource(mAsrConfig.getContext(), expandFnPath);
                    if (ret == -1) {//拷贝失败
                        if (mListener != null) {
                            mListener.onInit(AIConstant.OPT_FAILED);
                            mListener.onError(new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                        }
                        return;
                    }
                    String resPath = Util.getResourceDir(mAsrConfig.getContext()) + File.separator + expandFnPath;
                    expandFnPath = resPath;
                    mAsrParams.setExpandFnPath(resPath);
                    Log.i(TAG, "copy assets to desPath : " + expandFnPath);
                }
                String expandFnDir = expandFnPath.substring(0, expandFnPath.lastIndexOf("/"));
                JSONObject expandFnObj = new JSONObject(FileIOUtils.readFile2String(expandFnPath));
                JSONArray slotsArrays = expandFnObj.optJSONArray("slot");
                boolean isChanged = false;
                for (int i = 0; i < slotsArrays.length(); i++) {
                    JSONObject slotObj = slotsArrays.optJSONObject(i);
                    String path = slotObj.optString("path");
                    if (!path.startsWith("/")) {
                        //动态更新词库路径
                        String destPath = expandFnDir + File.separator + mAsrConfig.getTag() + "_" + path;
                        slotObj.put("path", destPath);
                        slotsArrays.put(i, slotObj);
                        //检查当前目录下是否有默认词库资源,没有则从assets/asr下拷贝
                        checkCopyAssetsRes("asr" + File.separator + path, destPath);
                        isChanged = true;
                    }
                }
                //重新写入文件
                if (isChanged) {
                    expandFnObj.put("slot", slotsArrays);
                    FileIOUtils.writeFileFromString(expandFnPath, expandFnObj.toString(4), false);
                }

                //再次检测bin文件是否存在，如果bin文件不存在，则生成bin文件做占位
                JSONObject expandFnObjAgain = new JSONObject(FileIOUtils.readFile2String(expandFnPath));
                JSONArray slotsArraysAgain = expandFnObjAgain.optJSONArray("slot");
                int type = expandFnObjAgain.optInt("type", 0);
                Log.d(TAG, "before handlePreStart,type=" + type);
                if (type == 1) {//只针对ngram词库做bin文件占位操作
                    List<String> contents = new ArrayList<>();
                    contents.add("filler");
                    List<Vocab> vocabList = new ArrayList<>();
                    Log.w(TAG, "expandFnObjAgain:" + expandFnObjAgain);
                    for (int i = 0; i < slotsArraysAgain.length(); i++) {
                        JSONObject slotObj = slotsArraysAgain.optJSONObject(i);
                        String path = slotObj.optString("path");
                        File file = new File(path);
                        Log.w(TAG, "file:" + file + "  ,isFileExists: " + FileUtils.isFileExists(path));
                        if (path.startsWith("/") && (!FileUtils.isFileExists(path) || FileUtils.getFileLength(path) <= 0)) {
                            Vocab vocab = new Vocab.Builder()
                                    .setAction(Vocab.ACTION_CLEAR_AND_INSERT)
                                    .setName(file.getName())
                                    .setUseSegment(true)
                                    .setOutputPath(path)
                                    .setContents(contents)
                                    .build();
                            vocabList.add(vocab);
                            Log.w(TAG, "vocabList: " + vocabList.size() + "   " + vocab.getName() + "   " + vocab.getContents() + "   " + vocab.getOutputPath());
                        }
                    }
                    if (vocabList.size() > 0) {
                        handleMsgUpdateVocab(vocabList.toArray(new Vocab[vocabList.size()]));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG, "end handlePreStart!");
    }


    private void handleMsgStart(Object[] paramsObj) {
        if (mState == EngineState.STATE_NEWED) {
            mAsrParams = (LocalAsrParams) paramsObj[0];
            mVadParams = (VadParams) paramsObj[1];
            syncRecorderId(mAsrParams, mVadParams);
            handlePreStart();
            if (mAsrParams.isUseCustomFeed()) {
                unRegisterRecorderIfIsRecording(this);
                Log.i(TAG, "isUseCustomFeed");
                mAsrKernel.startKernel(mAsrParams);
                if (mVadKernel != null && mVadParams != null && mVadParams.isVadEnable()) {
                    startNoSpeechTimer(mAsrParams);
                    mVadKernel.startKernel(mVadParams);
                }
                //回溯oneshot 音频
                if (mAsrParams.getOneshotCache() != null) {
                    OneshotCache<byte[]> buffer = mAsrParams.getOneshotCache();
                    if (buffer.isValid()) {
                        OneshotCache.OneshotIterator iterator = buffer.iterator();
                        while (iterator.hasNext()) {
                            byte[] data = (byte[]) iterator.next();
                            if (data != null) {
                                feedData(data, data.length);
                            }
                        }
                    }
                }
                if (mListener != null) {
                    mListener.onReadyForSpeech();
                }
                transferState(EngineState.STATE_RUNNING);
            } else {
                initRecorder();
                startRecorder(mAsrParams, LocalAsrProcessor.this);
            }
        } else {
            trackInvalidState("start");
        }
    }

    private boolean initRecorder() {
        if (mAIRecorder == null) {
            if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_MIC ||//音频来源于AudioRecorder节点
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_ECHO) {
                mAIRecorder = createRecorder(LocalAsrProcessor.this);
                if (mAIRecorder == null) {
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                            AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE));
                    return true;
                }
            } else if (AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_DUAL ||//音频来源于信号处理引擎节点
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_FESPCAR ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_FESPCAR4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_DUAL ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_LINE4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_LINE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_TINYCAP_CIRCLE4) {
                if (mAsrParams.getFespxEngine() == null) {
                    throw new RuntimeException("need to setFespxEngine before start engine");
                }
                mAIRecorder = createSignalProcessingRecorder(mAsrParams.getFespxEngine().getFespxProcessor());
                if (mAIRecorder == null) {
                    sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                            AIError.ERR_SIGNAL_PROCESSING_NOT_STARTED,
                            AIError.ERR_DESCRIPTION_SIGNAL_PROCESSING_NOT_STARTED));
                    return true;
                }
            }
        }
        return false;
    }

    private void handleMsgStop() {
        if (mState == EngineState.STATE_RUNNING) {
            unRegisterRecorderIfIsRecording(LocalAsrProcessor.this);
            mAsrKernel.stopKernel();
            if (mVadKernel != null && mVadParams != null && mVadParams.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_WAITING);
        } else {
            trackInvalidState("stop");
        }
    }

    private void handleMsgCancel() {
        if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
            removeCallbackMsg();// 清除消息队列，取消上一次消息结果

            if (mAsrParams != null && !mAsrParams.isUseCustomFeed()) {
                unRegisterRecorderIfIsRecording(LocalAsrProcessor.this);
            }
            if (mAsrKernel != null) {
                mAsrKernel.cancelKernel();
            }
            if (mVadKernel != null && mVadParams != null && mVadParams.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("cancel");
        }
    }

    private void handleMsgVadStart() {
        if (mState == EngineState.STATE_RUNNING) {
            Log.d(TAG, "VAD.BEGIN");
            mVadBeginTime = System.currentTimeMillis();
            cancelNoSpeechTimer();
            startMaxSpeechTimerTask(mAsrParams);
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_BEGINNING_OF_SPEECH, null);
        } else {
            trackInvalidState("VAD.BEGIN");
        }
    }

    private void handleMsgVadEnd() {
        if (mState == EngineState.STATE_RUNNING) {
            Log.d(TAG, "VAD.END");
            mVadEndTime = System.currentTimeMillis();
            unRegisterRecorderIfIsRecording(LocalAsrProcessor.this);
            mAsrKernel.stopKernel();
            if (mVadKernel != null && mVadParams != null && mVadParams.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_WAITING);
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_END_OF_SPEECH, null);
        } else {
            trackInvalidState("VAD.END");
        }
    }

    private void handleMsgVolumeChanged(float rmsDb) {
        if (mState == EngineState.STATE_RUNNING) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RMS_CHANGED, rmsDb);
        } else {
            trackInvalidState("volume changed");
        }
    }

    /**
     * update消息仅用来更新net.bin文件
     *
     * @param config 更新配置
     * @deprecated see {@link #decoder(Decoder...)}
     */
    @Deprecated
    private void handleMsgUpdate(String config) {
        if (mState != EngineState.STATE_IDLE) {
            if (mAsrKernel != null) {
                mAsrKernel.update(config);
            }
        } else {
            trackInvalidState("update");
        }
    }

    private void handleMsgResultPcmData(byte[] data) {
        if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
            if (mVadKernel != null && mVadParams != null && mVadParams.isVadEnable()) {//送vad模块，vad处理后再送asr
                if (mAsrConfig.isEnableDoubleVad()) {
                    byte[] byte0 = AudioHelper.splitOriginalChannel(data, 0, 2);
                    byte[] byte1 = AudioHelper.splitOriginalChannel(data, 1, 2);
                    mVadKernel.feed(new byte[][]{byte0, byte1});
                } else {
                    mVadKernel.feed(data);
                }
            } else {
                mAsrKernel.feed(data);
            }
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_BUFFER_RECEIVED, data);
            if (mListener != null) {
                mListener.onResultDataReceived(data, data.length, 0);
            }
        }
    }

    private void handleMsgResult(AIResult result) {
        if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {

            String interceptorName = IInterceptor.Name.LOCAL_ASR_RESULT;

            if (result.isLast()) {
                mAsrResultTime = System.currentTimeMillis();
                Log.d(TAG, "VAD.BEGIN.ASR.RESULT.DELAY : " + (mAsrResultTime - mVadBeginTime));
                Log.d(TAG, "VAD.END.ASR.RESULT.DELAY : " + (mAsrResultTime - mVadEndTime));
                transferState(EngineState.STATE_NEWED);
                unRegisterRecorderIfIsRecording(LocalAsrProcessor.this);

                result = handleAsrResult(result);

                //进行集内匹配查询
                if (((LocalAsrConfig) mAsrConfig).isUseAggregateMate()) {
                    result = parseJsonAggregateMetaResult(result, false);
                }

                // 开启ITN转换
                if (((LocalAsrConfig) mAsrConfig).isUseItn()) {
                    // 1.解析返回的Json 增加nluRec字段 修改eof=0
                    result = parseJsonLastResult(result, false);
                    // 2.抛出nluRec字段送语义
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
                    // 3.更新rec为itn转换后文本 修改eof=1，
                    result = parseJsonItnResult(result, true);
                    interceptorName = IInterceptor.Name.LOCAL_ITN_RESULT;
                } else {
                    // 解析返回的Json 增加nluRec字段
                    result = parseJsonLastResult(result, true);
                    interceptorName = IInterceptor.Name.LOCAL_ASR_RESULT;
                }
            }

            try {
                JSONObject customObj = new JSONObject().put(interceptorName, result);
                JSONObject inputObj = AsrInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.RECEIVE, customObj);
                SpeechInterceptor.getInstance().doInterceptor(interceptorName, inputObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
        } else {
            trackInvalidState("result");
        }
    }


    /**
     * 解析最后返回的 Json字段 增加字段nluRec用于语义识别
     * isLast = true 最后一次返回 更新eof=1， false 更新eof=0
     *
     * @param result ASR识别后返回的数据
     * @param isLast 是否是最后返回
     * @return AIResult
     */
    private AIResult parseJsonLastResult(AIResult result, boolean isLast) {

        try {
            JSONObject resultJo = result.getResultJSONObject();
            JSONObject grammarJo = resultJo.optJSONObject("grammar");
            JSONObject ngramJo = resultJo.optJSONObject("ngram");

            // 修改grammar、ngram中字段
            if (grammarJo != null || ngramJo != null) {
                if (grammarJo != null) {
                    String grammarRec = grammarJo.optString("rec");
                    if (!TextUtils.isEmpty(grammarRec)) {
                        grammarJo.put("nluRec", grammarRec);
                        grammarJo.put("eof", isLast ? 1 : 0);
                        resultJo.put("grammar", grammarJo);
                    }
                }
                if (ngramJo != null) {
                    String ngramRec = ngramJo.optString("rec");
                    if (!TextUtils.isEmpty(ngramRec)) {
                        ngramJo.put("nluRec", ngramRec);
                        ngramJo.put("eof", isLast ? 1 : 0);
                        resultJo.put("ngram", ngramJo);
                    }
                }
            } else {
                if (resultJo.optString("rec") != null) {
                    String rec = resultJo.optString("rec");
                    if (!TextUtils.isEmpty(rec)) {
                        resultJo.put("nluRec", rec);
                        resultJo.put("eof", isLast ? 1 : 0);
                    }
                }
            }
            result.setLast(isLast);
            result.setResultObject(resultJo);
            result.setTimestamp(System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 解析最后返回的 Json字段 更新rec为itn转换后文本，移除nluRec字段
     * isLast = true 最后一次返回 更新eof=1， false 更新eof=0
     *
     * @param result ASR识别后返回的数据
     * @param isLast 是否是最后返回
     * @return AIResult
     */
    private AIResult parseJsonItnResult(AIResult result, boolean isLast) {

        try {
            JSONObject resultJo = result.getResultJSONObject();
            JSONObject grammarJo = resultJo.optJSONObject("grammar");
            JSONObject ngramJo = resultJo.optJSONObject("ngram");
            JSONObject e2eJo = resultJo.optJSONObject("e2e");


            // 修改grammar、ngram中字段
            if (grammarJo != null || ngramJo != null || e2eJo != null) {
                if (grammarJo != null) {
                    String grammarRec = grammarJo.optString("rec");
                    if (!TextUtils.isEmpty(grammarRec)) {
                        String itnRec = feedItnResult(grammarRec);
                        if (!TextUtils.isEmpty(itnRec)) {
                            //移除nluRec结果 nluRec字段只在itn前抛出一次
                            grammarJo.put("rec", itnRec);
                            grammarJo.put("eof", isLast ? 1 : 0);
                        }
                        resultJo.put("grammar", grammarJo);
                    }
                }
                if (ngramJo != null) {
                    String ngramRec = ngramJo.optString("rec");
                    if (!TextUtils.isEmpty(ngramRec)) {
                        String itnRec = feedItnResult(ngramRec);
                        if (!TextUtils.isEmpty(itnRec)) {
                            ngramJo.put("rec", itnRec);
                            ngramJo.put("eof", isLast ? 1 : 0);
                        }
                        resultJo.put("ngram", ngramJo);
                    }
                }
                if (e2eJo != null){
                    String e2eRec = e2eJo.optString("rec");
                    if (!TextUtils.isEmpty(e2eRec)) {
                        String itnRec = feedItnResult(e2eRec);
                        if (!TextUtils.isEmpty(itnRec)) {
                            e2eJo.put("rec", itnRec);
                            e2eJo.put("eof", isLast ? 1 : 0);
                        }
                        resultJo.put("e2e", e2eJo);
                    }
                }
            } else {
                if (resultJo.optString("rec") != null) {
                    String rec = resultJo.optString("rec");
                    if (!TextUtils.isEmpty(rec)) {
                        String itnRec = feedItnResult(rec);
                        if (!TextUtils.isEmpty(itnRec)) {
                            resultJo.put("rec", itnRec);
                            resultJo.put("eof", isLast ? 1 : 0);
                        }
                    }
                }
            }
            result.setLast(isLast);
            result.setResultObject(resultJo);
            result.setTimestamp(System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 解析最后返回的 Json字段.
     * 如果开启了集内匹配则往grammar或ngram结构体中追加 aggregate_mate 字段,匹配为1,否则为0 .
     *
     * @param result ASR识别后返回的数据
     * @param isLast 是否是最后返回
     * @return AIResult
     */
    private AIResult parseJsonAggregateMetaResult(AIResult result, boolean isLast) {
        Log.d(TAG,"parseJsonAggregateMetaResult");
        try {
            JSONObject resultJo = result.getResultJSONObject();
            JSONObject grammarJo = resultJo.optJSONObject("grammar");
            JSONObject ngramJo = resultJo.optJSONObject("ngram");

            // 修改grammar、ngram中字段
            if (grammarJo != null || ngramJo != null) {
                if (grammarJo != null) {
                    String grammarRec = grammarJo.optString("rec");
                    if (!TextUtils.isEmpty(grammarRec)) {
                        String aggregateMetaResult = feedAggregateMetaResult(grammarRec);
                        if (!TextUtils.isEmpty(aggregateMetaResult) && !TextUtils.equals(aggregateMetaResult,"[]")) {
                            grammarJo.put("aggregate_mate", 1);
                        } else {
                            grammarJo.put("aggregate_mate", 0);
                        }
                        resultJo.put("grammar", grammarJo);
                    }
                }
                if (ngramJo != null) {
                    String ngramRec = ngramJo.optString("rec");
                    if (!TextUtils.isEmpty(ngramRec)) {
                        String aggregateMetaResult = feedAggregateMetaResult(ngramRec);
                        if (!TextUtils.isEmpty(aggregateMetaResult) && !TextUtils.equals(aggregateMetaResult,"[]")) {
                            ngramJo.put("aggregate_mate", 1);
                        } else {
                            ngramJo.put("aggregate_mate", 0);
                        }
                        resultJo.put("ngram", ngramJo);
                    }
                }
            } else {
                if (resultJo.optString("rec") != null) {
                    String rec = resultJo.optString("rec");
                    if (!TextUtils.isEmpty(rec)) {
                        String aggregateMetaResult = feedAggregateMetaResult(rec);
                        if (!TextUtils.isEmpty(aggregateMetaResult) && !TextUtils.equals(aggregateMetaResult,"[]")) {
                            resultJo.put("aggregate_mate", 1);
                        } else {
                            resultJo.put("aggregate_mate", 0);
                        }
                    }
                }
            }
            result.setLast(isLast);
            result.setResultObject(resultJo);
            result.setTimestamp(System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 对ASR识别返回的rec字段进行ITN文本转化
     *
     * @param rec
     * @return
     */
    private String feedItnResult(String rec) {
        if (!TextUtils.isEmpty(rec) && mAsrKernel instanceof LocalAsrKernel) {
            LocalAsrKernel kernel = (LocalAsrKernel) mAsrKernel;
            int status = kernel.feedItn(rec);
            if (status == AIConstant.OPT_SUCCESS) {
                return kernel.getItnRecResult();
            }
        }
        return null;
    }

    /**
     * 对ASR识别返回的rec字段进行集内匹配检查
     *
     * @param rec
     * @return
     */
    private String feedAggregateMetaResult(String rec) {
        if (!TextUtils.isEmpty(rec) && mAsrKernel instanceof LocalAsrKernel) {
            LocalAsrKernel kernel = (LocalAsrKernel) mAsrKernel;
            rec = rec.replaceAll(" ","");//集内匹配不支持文字之间的空格处理
            int status = kernel.feedAggregateMate(rec);
            if (status == AIConstant.OPT_SUCCESS) {
                return kernel.getAggregateMateRecResult();
            }
        }
        return null;
    }

    /**
     * 处理识别结果
     */
    private AIResult handleAsrResult(AIResult result) {
        try {
            JSONObject retObj = result.getResultJSONObject();
            // 识别结果数据结构包含多路结果，去除置信度<阈值的结果
            if (!retObj.has("eof")) {
                retObj = filterLowConfAsrResult(retObj);
            }

            // 仲裁ngram和holder_fst，holder_fst给ngram兜底
            if (!TextUtils.isEmpty(holderFst) && retObj.has(holderFst)) {
                JSONObject holderObj = retObj.optJSONObject(holderFst);
                double holderConf = holderObj.optDouble("conf");
                double ngramConf = 0;
                if (retObj.has("ngram")) {
                    JSONObject ngramObj = retObj.optJSONObject("ngram");
                    ngramConf = ngramObj.optDouble("conf");
                }
                double holderThresh = 0;
                if (decoderThreshMaps != null && decoderThreshMaps.containsKey(holderFst)) {
                    holderThresh = decoderThreshMaps.get(holderFst);
                }
                if (ngramConf <= holderConf) {
                    retObj.put("ngram", holderObj);
                } else {
                    if (holderThresh > 0 && holderConf >= holderThresh) {
                        retObj.put("ngram", holderObj);
                    }
                }
                retObj.remove(holderFst);
            }
            result.setResultObject(retObj);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return result;
    }

    private JSONObject filterLowConfAsrResult(JSONObject retObj) {
        try {
            JSONObject copy = new JSONObject(retObj.toString());//此处需要copy一份，以备后续迭代remove节点
            Iterator<String> iterator = copy.keys();
            while (iterator.hasNext()) {
                String key = iterator.next();
                JSONObject value = copy.optJSONObject(key);
                Log.d(TAG, "filterLowConfAsrResult  " + key + " = " + value);
                if (value == null) continue;
                if (!"ngram".equals(key) && value.has("forceout") && value.optInt("forceout") == 1) { //识别非正常解码结束，此场景下识别结果可直接丢弃
                    Log.d(TAG, "drop " + key + ", forceout == 1 ---> " + value);
                    retObj.remove(key);
                    continue;
                }
                double conf = value.has("conf") ? value.optDouble("conf") : 0;
                double thresh = mAsrParams.getUseThreshold(); //默认获取统一的阈值
                double netType = value.optDouble("net_type");//0=grammar,1=ngram,2=dynamic
                if (netType == 2 && dynamicThreshMaps != null && dynamicThreshMaps.containsKey(key)) {
                    Double customThreshold = dynamicThreshMaps.get(key);
                    if (customThreshold != null && customThreshold > 0) {
                        thresh = customThreshold;
                    }
                } else if (netType == 0 && decoderThreshMaps != null && decoderThreshMaps.containsKey(key)) {
                    Double customThreshold = decoderThreshMaps.get(key);
                    if (customThreshold != null && customThreshold > 0) {
                        thresh = customThreshold;
                    }
                }
                if (mAsrParams.getIsIgnoreThreshold()) {
                    if (netType != 1) {
                        boolean isReachThreshold = conf > thresh;
                        retObj.put("isReachThreshold", isReachThreshold);
                    } else {
                        retObj.put("isReachThreshold", true);//net_type为1的情况不做拦截处理
                    }
                } else {
                    if (netType != 1) {//实际置信度低于阈值，丢弃(ngram结果除外，否则全部删除可能会返回{} )
                        if (conf < thresh) {
                            Log.d(TAG, "drop " + key + ", conf=" + conf + " < thresh=" + thresh + " ---> " + value);
                            retObj.remove(key);
                        }
                        retObj.put("isReachThreshold", conf > thresh);
                    } else {
                        retObj.put("isReachThreshold", true);//net_type为1的情况不做拦截处理
                    }
                }
                Log.d(TAG, "net_type: " + netType + "thresh: " + thresh + "conf: " + conf + "retObj: " + retObj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return retObj;
    }

    private void handleMsgRelease() {
        if (mState != EngineState.STATE_IDLE) {
            removeCallbackMsg();
            unRegisterRecorderIfIsRecording(LocalAsrProcessor.this);
            if (mAsrKernel != null) {
                mAsrKernel.stopKernel();
            }
            if (mVadConfig != null && mVadConfig.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            releaseRecorder();
            cancelNoSpeechTimer();
            clearObject();//清除实例
            transferState(EngineState.STATE_IDLE);
        } else {
            trackInvalidState("release");
        }
    }

    private void handleMsgError(AIError error) {
        if (TextUtils.isEmpty(error.getRecordId())) {
            error.setRecordId(Utils.get_recordid());
        }
        if (error.getErrId() == AIError.ERR_RES_PREPARE_FAILED) {
            Log.w(TAG, error.toString());
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
            uploadError(error);
            return;
        }
        if (mState == EngineState.STATE_IDLE) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
            uploadError(error);
            return;
        }
        if (mState != EngineState.STATE_NEWED && mState != EngineState.STATE_IDLE) {
            unRegisterRecorderIfIsRecording(LocalAsrProcessor.this);

            if (error.getErrId() == AIError.ERR_TIMEOUT_ASR)
                mAsrKernel.cancelKernel(); // cloud asr 才处理 cancelKernel
            else
                mAsrKernel.stopKernel();
            if (mVadKernel != null && mVadParams != null && mVadParams.isVadEnable()) {
                mVadKernel.stopKernel();
            }
            transferState(EngineState.STATE_NEWED);
            Log.w(TAG, error.toString());
            uploadError(error);
            if (error.getErrId() == AIError.ERR_DNS) {
                error.setErrId(AIError.ERR_NETWORK);
                error.setError(AIError.ERR_DESCRIPTION_ERR_NETWORK);
            }
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
        } else {
            trackInvalidState("error");
        }
    }

    private void uploadError(AIError aiError) {
        if ((aiError.getErrId() == AIError.ERR_NETWORK || aiError.getErrId() == AIError.ERR_DNS)
                && !NetworkUtil.isNetworkConnected(AISpeech.getContext())) {
            Log.d(TAG, "network is not connected, ignore upload error");
            return;
        }
        String recordId = aiError.getRecordId();
        if (TextUtils.isEmpty(recordId)) {
            recordId = Utils.get_recordid();
        }
        //添加message外面的字段
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("recordId", recordId);
        entryMap.put("mode", "lite");
        entryMap.put("module", "local_exception");
        JSONObject input = new JSONObject();
        try {
            if (mAsrConfig != null) {
                input.put("config", ((LocalAsrConfig) mAsrConfig).toJson());
            }
            if (mAsrParams != null) {
                input.put("param", ((LocalAsrParams) mAsrParams).toJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("local_asr_exception", "info", "local_exception",
                recordId, input, aiError.getOutputJSON(), entryMap);
    }


    @Override
    public void processNoSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_NO_SPEECH,
                AIError.ERR_DESCRIPTION_NO_SPEECH));
        Log.w(TAG, "no speech timeout!");
    }

    @Override
    public void processMaxSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                AIError.ERR_MAX_SPEECH, AIError.ERR_DESCRIPTION_MAX_SPEECH));
    }

    /**
     * asr回调
     */
    private class MyAsrKernelListener implements AsrKernelListener {

        @Override
        public void onInit(int status) {
            Log.i(TAG, "MyAsrKernelListener onInit : " + status);
            processInit(status);
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onResults(AIResult result) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, result);
        }

        @Override
        public void onStarted(String recordId) {

        }

        @Override
        public void onUpdateResult(int ret) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, ret);
        }
    }


    /**
     * vad模块回调
     */
    private class MyVadKernelListener implements VadKernelListener {

        @Override
        public void onInit(int status) {
            Log.i(TAG, "MyVadKernelListener onInit : " + status);
            processInit(status);
        }

        @Override
        public void onVadStart(String recordID) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_START, null);
        }

        @Override
        public void onVadEnd(String recordID) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_END, null);
        }

        @Override
        public void onRmsChanged(float rmsDb) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VOLUME_CHANGED, rmsDb);
        }

        @Override
        public void onBufferReceived(byte[] data) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_RECEIVE_DATA, data);
        }

        @Override
        public void onResults(String result) {

        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onReadyForSpeech() {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }
    }
}
