package com.aispeech.lite.semantic;

import static com.aispeech.lite.SemanticType.NAVI;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.FileUtil;
import com.aispeech.common.Log;
import com.aispeech.common.Transformer;
import com.aispeech.export.ASRMode;
import com.aispeech.export.IAsrPolicy;
import com.aispeech.export.Vocab;
import com.aispeech.export.config.RecorderConfig;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.asr.AsrKernelListener;
import com.aispeech.lite.asr.LocalAsrKernel;
import com.aispeech.lite.config.LocalAsrConfig;
import com.aispeech.lite.config.LocalSemanticConfig;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.oneshot.OneshotCache;
import com.aispeech.lite.param.LocalAsrParams;
import com.aispeech.lite.param.LocalSemanticParams;
import com.aispeech.lite.param.VadParams;
import com.aispeech.lite.speech.SpeechListener;
import com.aispeech.lite.vad.VadKernel;
import com.aispeech.lite.vad.VadKernelListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yuruilong on 2017/5/19.
 * @deprecated {@link LocalSemanticProcessor}
 */

public class SemanticProcessor extends BaseProcessor {
    private final String TAG = "SemanticProcessor";
    private LocalAsrKernel mAsrKernel;
    private LocalAsrParams mAsrParams;
    private LocalAsrConfig mAsrConfig;
    private VadKernel mVadKernel;
    private VadParams mVadParams;
    private LocalVadConfig mVadConfig;
    private LocalSemanticKernel mSemanticKernel;
    private LocalSemanticConfig mSemanticConfig;
    private LocalSemanticParams mSemanticParams;
    private JSONObject finalSemantic;
    private String mRecordId;
    FileUtil mFileUtil;
    private SpeechListener mListener;

    private long mStartTime;
    private long mSemanticResultTime;
    private long mAsrResultTime;
    private long mVadBeginTime;
    private long mVadEndTime;
    private IAsrPolicy mAsrPolicy;
    private String currentSelected;

    public void init(SpeechListener listener, LocalAsrConfig asrConfig,
                     LocalVadConfig vadConfig, LocalSemanticConfig semanticConfig) {
        mListener = listener;
        if (!semanticConfig.isUseRefText()) {
            this.mVadConfig = vadConfig;
            threadCount++;
            if (vadConfig.isVadEnable()) {
                threadCount++;
            }
            this.mAsrConfig = asrConfig;
            mAsrKernel = new LocalAsrKernel("localSem", new MyAsrKernelListener());
            mFileUtil = new FileUtil(AISpeech.getContext());
        }

        this.mSemanticConfig = semanticConfig;
        mSemanticKernel = new LocalSemanticKernel(new MySemanticKernelListener());

        mScope =  Scope.LOCAL_SEMANTIC;
        init(listener, semanticConfig.getContext(), TAG);

        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
    }

    public void start(LocalAsrParams asrParams, VadParams vadParams, LocalSemanticParams semanticParams) {
        if (this.isAuthorized()) {
            this.mAsrParams = asrParams;
            this.mVadParams = vadParams;
            this.mSemanticParams = semanticParams;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void clearObject() {
        super.clearObject();
        if (mAsrKernel != null)
            mAsrKernel = null;
        if (mAsrParams != null)
            mAsrParams = null;
        if (mAsrConfig != null)
            mAsrConfig = null;
        if (mVadKernel != null)
            mVadKernel = null;
        if (mVadParams != null)
            mVadParams = null;
        if (mVadConfig != null)
            mVadConfig = null;
        if (mSemanticKernel != null)
            mSemanticKernel = null;
        if (mSemanticConfig != null)
            mSemanticConfig = null;
        if (mSemanticParams != null)
            mSemanticParams = null;
        if (finalSemantic != null)
            finalSemantic = null;
    }


    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                if (mState == EngineState.STATE_IDLE) {
                    int status = AIConstant.OPT_FAILED;
                    if (!mSemanticConfig.isUseRefText()) {
                        //asr资源拷贝和检查
                        status = copyAssetsRes(mAsrConfig);
                        if (status == AIConstant.OPT_FAILED) {
                            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                    AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                            break;
                        }
                        if (!mSemanticConfig.isUseCustomFeed()) {
                            if (mAIRecorder == null) {
                                if (AISpeech.getRecoderType() == RecorderConfig.TYPE_COMMON_MIC ||//音频来源于AudioRecorder节点
                                        AISpeech.getRecoderType() == RecorderConfig.TYPE_COMMON_ECHO) {
                                    mAIRecorder = createRecorder(SemanticProcessor.this);
                                    if (mAIRecorder == null) {
                                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                                                AIError.ERR_DEVICE, AIError.ERR_DESCRIPTION_DEVICE));
                                        return;
                                    }
                                }
                            }
                        }
                        //vad 资源拷贝和检查
                        if (mVadConfig != null && mVadConfig.isVadEnable()) {
                            status = copyAssetsRes(mVadConfig);
                            if (status == AIConstant.OPT_FAILED) {
                                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                                break;
                            }
                            mVadKernel = new VadKernel("lsem", new MyVadKernelListener());
                            mVadKernel.newKernel(mVadConfig);
                        }
                        mAsrKernel.newKernel(mAsrConfig);
                    }
                    //semantic资源拷贝，多个目录
                    status = copyAssetsFolders(mSemanticConfig);
                    if (status == AIConstant.OPT_FAILED) {
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                                AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                        break;
                    }

                    Transformer.load(mSemanticConfig.getNaviSkillConfPath());
                    if (mSemanticKernel != null) {
                        mSemanticKernel.newKernel(mSemanticConfig);
                    }

                    if (((mSemanticConfig.getSemanticType().getType() & 0x01) == NAVI.getType()) && mSemanticConfig.isUseFormat()) {
                        Transformer.load(mSemanticConfig.getNaviSkillConfPath());
                    }
                } else {
                    trackInvalidState("new");
                }
                break;
            case MSG_START:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
                    finalSemantic = new JSONObject();
                    mStartTime = System.currentTimeMillis();
                    mRecordId = Utils.getRecorderId();
                    syncRecorderId(mRecordId, mAsrParams, mVadParams);
                    if (!mSemanticConfig.isUseRefText()) {
                        String saveAudioPath = mAsrParams.getSaveAudioPath();
                        if (!TextUtils.isEmpty(saveAudioPath) && mFileUtil != null) {
                            Log.d(TAG, "create local semantic audio file at: " + saveAudioPath + "/local_semantic_" + mRecordId + ".pcm");
                            mFileUtil.createFile(saveAudioPath + "/local_semantic_" + mRecordId + ".pcm");
                        }
                        if (mSemanticConfig.isUseCustomFeed()) {
                            Log.i(TAG, "isUseCustomFeed");
                            mAsrKernel.startKernel(mAsrParams);
                            if (mVadConfig != null && mVadConfig.isVadEnable()) {
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
                            transferState(EngineState.STATE_RUNNING);
                        } else {
                            // 启动SDK内部录音机
                            startRecorder(mSemanticParams, SemanticProcessor.this);
                        }
                    } else {
                        if (mSemanticKernel != null) {
                            mSemanticKernel.startKernel(mSemanticParams);
                            transferState(EngineState.STATE_RUNNING);
                        }
                    }
                } else {
                    trackInvalidState("start");
                }
                break;
            case MSG_RECORDER_START:
                if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
                    mAsrKernel.startKernel(mAsrParams);
                    if (mVadConfig != null && mVadConfig.isVadEnable()) {
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
                    transferState(EngineState.STATE_RUNNING);
                } else {
                    trackInvalidState("recorder start");
                }
                break;
            case MSG_STOP:
                if (mState == EngineState.STATE_RUNNING) {
                  //  unRegisterRecorderIfIsRecording(SemanticProcessor.this);
                    if (mAsrKernel != null) {
                        mAsrKernel.stopKernel();
                    }
                    if (mVadKernel != null && mVadConfig != null && mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    if (mFileUtil != null)
                        mFileUtil.closeFile();
                    transferState(EngineState.STATE_WAITING);
                } else {
                    trackInvalidState("stop");
                }
                break;
            case MSG_CANCEL:
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING
                        || mState == EngineState.STATE_NEWED) {
                    transferState(EngineState.STATE_NEWED);
                    unRegisterRecorderIfIsRecording(SemanticProcessor.this);
                    if (mVadConfig != null && mVadConfig.isVadEnable()) {
                        mVadKernel.cancelKernel();
                    }
                    if (mAsrKernel != null) {
                        mAsrKernel.cancelKernel();
                    }
                    if (mSemanticKernel != null) {
                        mSemanticKernel.cancelKernel();
                    }

                    if (mFileUtil != null)
                        mFileUtil.closeFile();

                } else {
                    trackInvalidState("cancel");
                }
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
                if (mState == EngineState.STATE_RUNNING) {
                    if (mVadConfig != null && mVadConfig.isVadEnable()) {//送vad模块，vad处理后再送asr
                        mVadKernel.feed(bufferData);
                    } else {
                        if (mAsrKernel != null) {
                            mAsrKernel.feed(bufferData);
                        }
                        if (mFileUtil != null)
                            mFileUtil.write(bufferData);
                    }
                    //sendMsgToCallbackMsgQueue(CallbackMsg.MSG_BUFFER_RECEIVED, bufferData);
                    if (mListener != null) {
                        mListener.onResultDataReceived(bufferData, bufferData.length, 0);
                    }
                }
                break;
            case MSG_VAD_RECEIVE_DATA:
                final byte[] vadData = (byte[]) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mAsrKernel != null) {
                        mAsrKernel.feed(vadData);
                    }
                    if (mFileUtil != null)
                        mFileUtil.write(vadData);
                }
                break;
            case MSG_VAD_START:
                if (mState == EngineState.STATE_RUNNING) {
                    Log.d(TAG, "VAD.BEGIN");
                    cancelNoSpeechTimer();
                    startMaxSpeechTimerTask(mAsrParams);
                    if (mListener != null) {
                        mListener.onBeginningOfSpeech();
                    }
                } else {
                    trackInvalidState("VAD.BEGIN");
                }
                break;
            case MSG_VAD_END:
                if (mState == EngineState.STATE_RUNNING) {
                    Log.d(TAG, "VAD.END");
                   // unRegisterRecorderIfIsRecording(SemanticProcessor.this);
                    mAsrKernel.stopKernel();
                    if (mVadConfig != null && mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    if (mFileUtil != null)
                        mFileUtil.closeFile();
                    transferState(EngineState.STATE_WAITING);
                    if (mListener != null) {
                        mListener.onEndOfSpeech();
                    }
                } else {
                    trackInvalidState("VAD.END");
                }
                break;
            case MSG_VOLUME_CHANGED:
                float rmsDb = (float) msg.obj;
                if (mState == EngineState.STATE_RUNNING) {
                    if (mListener != null) {
                        mListener.onRmsChanged(rmsDb);
                    }
                } else {
                    trackInvalidState("volume changed");
                }
                break;
            case MSG_RESULT:
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    mSemanticResultTime = System.currentTimeMillis();
                    Log.d(TAG, "START.SEMANTIC.RESULT.DELAY : " + (mSemanticResultTime - mStartTime));
                    Log.d(TAG, "FINAL.ASR.SEMANTIC.RESULT.DELAY : " + (mSemanticResultTime - mAsrResultTime));
                    Log.d(TAG, "VAD.BEGIN.SEMANTIC.RESULT.DELAY : " + (mSemanticResultTime - mVadBeginTime));
                    Log.d(TAG, "VAD.END.SEMANTIC.RESULT.DELAY : " + (mSemanticResultTime - mVadEndTime));
                    AIResult result = (AIResult) msg.obj;
                    Log.d(TAG, "final semantic result before format: " + result);
                    if(TextUtils.isEmpty(result.toString())){
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_NULL_SEMANTIC, AIError.ERR_DESCRIPTION_NULL_SEMANTIC));
                        return;
                    }
                    result = selectNlu(result);
                    if(result == null){
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_NULL_SEMANTIC, AIError.ERR_DESCRIPTION_NULL_SEMANTIC));
                        return;
                    }
                    if (mListener != null) {
                        mListener.onResults(result);
                    }
                    if (result.isLast()) {
//                        Upload.startUploadTimer();
                        transferState(EngineState.STATE_NEWED);
                        // unRegisterRecorderIfIsRecording(SemanticProcessor.this);
                    }
                } else {
                    trackInvalidState("result");
                }
                break;
            case MSG_UPDATE://离线语义引擎update消息仅用来更新net.bin文件
                if (mState == EngineState.STATE_NEWED) {
                    if (!mSemanticConfig.isUseRefText()) {
                        String config = (String) msg.obj;
                        if (mAsrKernel != null) {
                            mAsrKernel.update(config);
                        }
                    } else {
                        Log.e(TAG, "update not support when isUseRefText is true");
                    }
                } else {
                    trackInvalidState("update");
                }
                break;
            case MSG_UPDATE_VOCAB:
                if (mState != EngineState.STATE_IDLE) {
                    Vocab [] vocab = (Vocab[]) msg.obj;
                    if (vocab != null) {
                        mSemanticKernel.updateVocabs(vocab);
                    } else
                        Log.e(TAG, "illegal vocab!" + vocab);
                } else {
                    trackInvalidState("update vocab info");
                }
                break;

            case MSG_UPDATE_NAVI_VOCAB:
                if (mState != EngineState.STATE_IDLE) {
                    String cfg = (String) msg.obj;
                    if (cfg != null) {
                        mSemanticKernel.updateNaviVocab(cfg);
                    } else
                        Log.e(TAG, "illegal vocab param!" + cfg);
                } else {
                    trackInvalidState("update vocab info");
                }
                break;
            case MSG_UPDATE_RESULT:
                if (mState != EngineState.STATE_IDLE) {
                    Integer ret = (Integer) msg.obj;
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, ret);
                } else {
                    trackInvalidState("update_result");
                }
                break;
            case MSG_ASR_RESULT:
                if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
                    AIResult aiResult = (AIResult) msg.obj;
                    try {
                        if (!aiResult.isLast()) { //实时识别结果
                            if (mListener != null) {
                                mListener.onResults(aiResult);
                            }
                        } else {//识别最终结果
                            mAsrResultTime = System.currentTimeMillis();
                            Log.d(TAG, "VAD.BEGIN.ASR.RESULT.DELAY : " + (mAsrResultTime - mVadBeginTime));
                            Log.d(TAG, "VAD.END.ASR.RESULT.DELAY : " + (mAsrResultTime - mVadEndTime));
                            if (mAsrPolicy == null) {
                                mAsrPolicy = new MyAsrPolicy();
                            }
                            String recText = mAsrPolicy.onAsrResult(aiResult);
                            if (TextUtils.isEmpty(recText)) {
                                aiResult = mAsrPolicy.onAsr(aiResult);//兼容老接口
                                JSONObject retObj = new JSONObject(aiResult.getResultObject().toString());
                                //拿到决策后的识别结果送语义
                                if(retObj != null) {
                                    JSONObject object = retObj.optJSONObject("select");
                                    if (object != null) {
                                        recText = object.optString("rec");
                                    }
                                }
                            }

                            if (TextUtils.isEmpty(recText)) {
                                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_NULL_SEMANTIC_INPUT, AIError.ERR_DESCRIPTION_NULL_SEMANTIC_INPUT));
                            }else {
                                mSemanticParams.setRefText(recText.replaceAll(" ", ""));
                                if (mSemanticKernel != null)
                                    mSemanticKernel.startKernel(mSemanticParams);
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    trackInvalidState("result");
                }
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (mState == EngineState.STATE_RUNNING) {
                        unRegisterRecorderIfIsRecording(SemanticProcessor.this);
                        if (mAsrKernel != null) {
                            mAsrKernel.stopKernel();
                        }
                        if (mVadConfig != null && mVadConfig.isVadEnable()) {
                            mVadKernel.stopKernel();
                        }
                    }
                    releaseRecorder();
                    if (mAsrKernel != null) {
                        mAsrKernel.releaseKernel();
                        mAsrKernel = null;
                    }
                    if (mVadKernel != null) {
                        mVadKernel.releaseKernel();
                        mVadKernel = null;
                    }
                    if (mFileUtil != null)
                        mFileUtil.closeFile();

                    if (mSemanticKernel != null) {
                        mSemanticKernel.releaseKernel();
                        mSemanticKernel = null;
                    }
                    clearObject();//清除实例
//                    Upload.stopUploadTimer();
                    transferState(EngineState.STATE_IDLE);
                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_ERROR:
                AIError error = (AIError) msg.obj;
                if (error.getErrId() == AIError.ERR_RES_PREPARE_FAILED || mState == EngineState.STATE_IDLE || mSemanticConfig.isUseRefText()) {
                    if (mListener != null) {
                        mListener.onError(error);
                    }
                    transferState(EngineState.STATE_NEWED);
                    // uploadError(error);
                    // Upload.startUploadTimer();
                    return;
                }
                if (mState != EngineState.STATE_NEWED && mState != EngineState.STATE_IDLE) {
                  //  unRegisterRecorderIfIsRecording(SemanticProcessor.this);
                    if (mAsrKernel != null) {
                        mAsrKernel.stopKernel();
                    }
                    if (mVadConfig != null && mVadConfig.isVadEnable()) {
                        mVadKernel.stopKernel();
                    }
                    transferState(EngineState.STATE_NEWED);
                    Log.w(TAG, error.toString());
//                    uploadError(error);
//                    Upload.startUploadTimer();
                    if (!(mAsrParams.isUseMaxSpeechResult() && error.getErrId() == AIError.ERR_MAX_SPEECH)) {
                        if (error.getErrId() == AIError.ERR_DNS) {
                            error.setErrId(AIError.ERR_NETWORK);
                            error.setError(AIError.ERR_DESCRIPTION_ERR_NETWORK);
                        }
                        if (mListener != null) {
                            mListener.onError(error);
                        }
                    }
                } else {
                    trackInvalidState("error");
                }
                break;
            default:
                break;
        }
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
     * 更新词库，目前只更新ldm中的词库信息
     */
    public void updateVocabs(Vocab ... vocabs) {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE_VOCAB, vocabs.clone());
        } else {
            showErrorMessage();
        }
    }

    /**
     * 更新离线内置语义词库
     *
     * @param semanticVocabsCfg 词库
     */
    public void updateNaviVocab(String semanticVocabsCfg) {
        if (mProfileState != null && mProfileState.isValid()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE_NAVI_VOCAB, semanticVocabsCfg);
        } else {
            showErrorMessage(mProfileState);
        }

    }

    /**
     * 获取词库内容
     */
    public List<String> getVocab(String vocabName) {
        List<String> vocabs = new ArrayList<String>();
        if (isAuthorized()) {
            if (mSemanticKernel != null) {
                vocabs = mSemanticKernel.getVocab(vocabName);
            }
        } else {
            showErrorMessage();
        }
        return vocabs;
    }


    /**
     * 语义内容归一化
     *
     * @param json 输入数据结构
     * @return json
     */
    private JSONObject format(JSONObject json) {
        json = formatGrammar(json,DYNAMIC);
        json = formatGrammar(json,GRAMMAR);
        json = formatGrammar(json,NGRAM);

        try {
            JSONObject jsonSelect = json.getJSONObject(SELECT);
            if (jsonSelect != null) {
                String source = jsonSelect.optString("source");
                if (GRAMMAR.equals(source)) {
                    json = formatGrammar(json, SELECT);
                    json = formatGrammar(json, NLU);
                }
                if (NGRAM.equals(source)) {
                    json = formatGrammar(json, SELECT);
                    json = formatGrammar(json, NLU);
                }

                if ("navi_local".equals(source)) {
                    json = formatNavi(json, SELECT);
                    json = formatNavi(json, NLU);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return json;
    }

    private JSONObject formatGrammar(JSONObject json, String name) {
        try {
            if (json.has(name)) {
                JSONObject grammarObj = Transformer.transGrammmer(json.optJSONObject(name));
                json.put(name, grammarObj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private JSONObject formatNavi(JSONObject json, String name) {
        try {
            if (json.has(name)) {
                JSONObject ngramObj = Transformer.transNgram(json.optJSONObject(name), mSemanticParams.getTask());
                ngramObj.put("conf", 0.631);
                json.put(name, ngramObj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }


    private JSONObject formatNgram(JSONObject json, String name) {
        try {
            if (json.has(name)) {
                JSONObject ngramObj = json.optJSONObject(name);
                if (ngramObj.has("post")) {// 当前为 post.sem 的语义格式
                    ngramObj = Transformer.transGrammmer(ngramObj);
                } else {
                    ngramObj = Transformer.transNgram(ngramObj, mSemanticParams.getTask());
                }
                json.put(name, ngramObj);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    private void selectGrammarNgram() throws JSONException {
        JSONObject grammarObj = finalSemantic.optJSONObject(GRAMMAR);
        JSONObject ngramObj = finalSemantic.optJSONObject(NGRAM);
        if (grammarObj != null && ngramObj != null) {
            double grammarconf = 0;
            double ngramconf = 0;
            if (grammarObj.has("conf")) {
                grammarconf = grammarObj.getDouble("conf");
            }
            if (ngramObj.has("conf")) {
                ngramconf = ngramObj.getDouble("conf");
            }

            Log.d(TAG, "grammarconf : " + grammarconf + "  ngramconf " + ngramconf);
            if (grammarconf >= ngramconf) {
                grammarObj.put("source", GRAMMAR);
                finalSemantic.put(NLU, grammarObj);
                currentSelected = GRAMMAR;
            } else {
                ngramObj.put("source", NGRAM);
                finalSemantic.put(NLU, ngramObj);
                currentSelected = NGRAM;
            }

        }
    }

    /**
     * 语义仲裁：
     *
     * @param result 语义结果
     * @return AIResult
     */
    private AIResult selectNlu(AIResult result) {
        try {
            JSONObject nluObj = new JSONObject(result.toString());
            if (nluObj.has("semantics")
                    && nluObj.optJSONObject("semantics").has("request")) {

                JSONObject request = nluObj.optJSONObject("semantics").optJSONObject("request");
                if (request != null) {
                    JSONArray jsonArray = request.optJSONArray("slots");
                    if (jsonArray != null && jsonArray.length() > 0) {
                        currentSelected = NLU;
                        finalSemantic.put(NLU, nluObj);
                    } else {
                        selectGrammarNgram();
                    }
                }

            } else {
                if (GRAMMAR.equals(currentSelected)) {
                    nluObj = finalSemantic.optJSONObject(GRAMMAR);
                    nluObj.put("source", GRAMMAR);
                    finalSemantic.put(NLU, nluObj);
                } else {
                    return null;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mSemanticConfig.isUseSelectRule()) {
//            currentSelected = NLU;
            Log.d(TAG, "finalSemantic >>>> " + finalSemantic + "  currentSelected  " + currentSelected);
            select(finalSemantic, currentSelected);
        } else {
            finalSemantic.remove("select");
        }

        //离线语义格式归一化
        if (mSemanticConfig.isUseFormat()) {
            finalSemantic = format(finalSemantic);
            Log.d(TAG, "finalSemantic after formated --------------------" + finalSemantic);
        }

        if (mSemanticConfig.isEnableNluFormatV2()) {
            finalSemantic.remove("select");
            finalSemantic.remove(GRAMMAR);
            finalSemantic.remove(NGRAM);
        }

        result.setResultObject(finalSemantic);
        result.setLast(true);
        result.setRecordId(mRecordId);

        return result;
    }

    /**
     * 往结果对象中添加选择结果
     *
     * @param retObj 识别结果
     * @param name   识别结果来源，取值 grammar / ngram / nlu等
     *
     * @deprecated 已弃用，兼容老版本会继续抛出此字段
     * */
    @Deprecated
    private JSONObject select(JSONObject retObj, String name) {
        try {
            JSONObject select = retObj.optJSONObject(name);
            retObj.put("select", select);
            finalSemantic = retObj;
            Log.d(TAG, "select: " + name + ", " + select);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retObj;
    }

    /**
     * 外部设置识别仲裁策略
     */
    public void setAsrPolicy(IAsrPolicy asrPolicy) {
        this.mAsrPolicy = asrPolicy;
    }

    private final static String NGRAM = "ngram";
    private final static String GRAMMAR = "grammar";
    private final static String DYNAMIC = "dynamic";
    private final static String NLU = "nlu";
    private final static String SELECT = "select";

    /**
     * 最终仲裁 grammar 和 ngram 识别结果
     */
    private class MyAsrPolicy implements IAsrPolicy {

        /**
         * 热词和识别置信度绝对差值范围内，优先选用热词
         */
        private final static double confDifferenceRange = 0.15f;

        /**
         * 获取 ASR 返回的阈值
         *
         * @param rec    识别热词结果
         * @param params 本地识别参数
         * @return 热词对应的阈值大小
         */
        private double getThreshold(String rec, LocalAsrParams params) {

            //判断是否为自定义热词阈值
            if (!params.getCustomThresholdMap().isEmpty() && params.getCustomThresholdMap().containsKey(rec)) {
                //返回自定义热词所对应的阈值
                return params.getCustomThresholdMap().get(rec);
            } else {
                //返回统一设定的热词所对应的阈值
                return params.getUseThreshold();
            }
        }

        /**
         * 检查热词结果
         */
        private boolean checkDynamicResult(JSONObject dynamicObj) {
            int forceOut = dynamicObj.optInt("forceout", 0);
            double conf = dynamicObj.optDouble("conf");
            String rec = dynamicObj.optString("rec");
            if (forceOut == 1) {
                Log.w(TAG, "DROP FORCE OUT HOT WORD: " + dynamicObj.toString());
                return false;
            } else if (conf < getThreshold(rec, mAsrParams)) {
                Log.d(TAG, "DROP CONF NOT QUALIFIED HOT WORD: " + dynamicObj.toString());
                return false;
            } else if (TextUtils.isEmpty(rec)) {
                Log.d(TAG, "DROP REC NOT QUALIFIED HOT WORD " + dynamicObj.toString());
                return false;
            }
            return true;
        }

        @Override
        public AIResult onAsr(AIResult result) {
            result.setResultObject(selectAsr(result));
            return result;
        }

        @Override
        public String onAsrResult(AIResult result) {
            return selectAsr(result).optString("rec");
        }

        private JSONObject selectAsr(AIResult result){
            try {
                JSONObject object = new JSONObject(result.getResultObject().toString());
                JSONObject ngramObj = object.optJSONObject(NGRAM);
                JSONObject grammarObj = object.optJSONObject(GRAMMAR);
                JSONObject dynamicObj = object.optJSONObject(DYNAMIC);

                currentSelected = NGRAM;
                if (mAsrParams.getMode() == ASRMode.MODE_HOTWORD.getValue()) {
                    currentSelected = DYNAMIC;
                    if (!(object.has(DYNAMIC) && checkDynamicResult(dynamicObj))) {
                        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_DEFAULT, AIError.ERR_DESCRIPTION_DEFAULT));
                    }
                } else {
                    if (object.has(GRAMMAR)) {
                        int grammarForceOut = grammarObj.optInt("forceout", 0);
                        double grammarConf = grammarObj.optDouble("conf");
                        String rec = grammarObj.optString("rec");
                        Log.d(TAG, "rec " + rec);
                        if (grammarForceOut == 0 && grammarConf >= mSemanticConfig.getSelectRuleThreshold()
                                && !TextUtils.isEmpty(rec)) {
                            currentSelected = GRAMMAR;
                        }
                    }
                    if (mAsrParams.getMode() == ASRMode.MODE_ASR_X.getValue()) {
                        if (object.has(DYNAMIC) && checkDynamicResult(dynamicObj)) {
                            JSONObject temObj = NGRAM.equals(currentSelected) ? ngramObj : grammarObj;
                            double tempConf = temObj.optDouble("conf");
                            double dynamicConf = dynamicObj.optDouble("conf");
                            if (Math.abs(dynamicConf) - Math.abs(tempConf) <= confDifferenceRange) {
                                currentSelected = DYNAMIC;
                            }
                        }
                    }
                }

                finalSemantic = select(object, currentSelected);
                return object.optJSONObject(currentSelected);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new JSONObject();
        }
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
            Log.d(TAG, "asr error :" + error.toString());
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onResults(AIResult result) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ASR_RESULT, result);
        }

        @Override
        public void onStarted(String recordId) {

        }

        @Override
        public void onUpdateResult(int ret) {
            if (mListener != null) {
                mListener.onUpdateResult(ret);
            }
        }
    }

    /**
     * 语义回调
     */
    private class MySemanticKernelListener implements SemanticKernelListener {

        @Override
        public void onInit(int status) {
            Log.i(TAG, "MySemanticKernelListener onInit : " + status);
            processInit(status);
        }

        @Override
        public void onResults(AIResult result) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, result);
        }

        @Override
        public void onError(AIError error) {
            //2021/10/12 若 dui + navi 语义为空，则根据仲裁规则抛出对应 grammar 的语义
            if (AIError.ERR_NULL_SEMANTIC == error.getErrId()) {
                if (finalSemantic != null && finalSemantic.has(GRAMMAR)) {
                    JSONObject selectObj = finalSemantic.optJSONObject(GRAMMAR);
                    if (mSemanticConfig != null) {
                        if (selectObj.has("conf") && selectObj.optDouble("conf") >= mSemanticConfig.getSelectRuleThreshold()) {
                            currentSelected = GRAMMAR;
                            sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, AIResult.bundleResults(AIConstant.AIENGINE_MESSAGE_TYPE_JSON, mRecordId, finalSemantic.toString()));
                            return;
                        }
                    }
                }
            }
            if (mSemanticConfig != null && mSemanticConfig.isThrowEmptySemantic()) {
                sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT,
                        AIResult.bundleResults(AIConstant.AIENGINE_MESSAGE_TYPE_JSON, mRecordId,
                                error.getExt().toString()));
            } else {
                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
            }
        }

        @Override
        public void onUpdateResult(int ret) {
            Log.d(TAG,"ret = " + ret);
            if (mListener != null) {
                mListener.onUpdateResult(ret);
            }
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
            mVadBeginTime = System.currentTimeMillis();
            sendMsgToInnerMsgQueue(EngineMsg.MSG_VAD_START, null);
        }

        @Override
        public void onVadEnd(String recordID) {
            mVadEndTime = System.currentTimeMillis();
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
