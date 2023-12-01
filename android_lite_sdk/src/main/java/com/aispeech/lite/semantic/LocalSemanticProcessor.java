package com.aispeech.lite.semantic;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AITimer;
import com.aispeech.common.FileIOUtils;
import com.aispeech.common.Log;
import com.aispeech.common.Transformer;
import com.aispeech.export.Vocab;
import com.aispeech.export.itn.Utils;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.SemanticType;
import com.aispeech.lite.config.LocalSemanticConfig;
import com.aispeech.lite.param.LocalSemanticParams;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by yuruilong on 2017/5/19.
 */

public class LocalSemanticProcessor extends BaseProcessor {
    private final static String TAG = "LocalSemanticProcessor";
    /**
     * 配置的多路语义内核列表
     */
    private List<BaseKernel> mSemanticKernels;
    private LocalSemanticConfig mSemanticConfig;
    private LocalSemanticParams mSemanticParams;
    private SpeechListener mListener;

    private long mStartTime, mResultTime;
    /**
     * 语义循环栅栏；等待语义全部出来都再进行语义抉择
     */
    private CyclicBarrier mResultBarrier;

    private CyclicBarrier mVocabsUpdateBarrier;
    private int mDUIUpdateResult;
    private int mBuildinResult;

    private AIResult naviResult;
    private AIResult buildinResult;
    private AIResult duiResult;

    /**
     * 内置语义skill映射
     */
    private JSONObject skillMappingObj;

    public void init(SpeechListener listener, LocalSemanticConfig semanticConfig) {
        mListener = listener;
        mSemanticKernels = new ArrayList<>();
        this.mSemanticConfig = semanticConfig;
        if ((mSemanticConfig.getSemanticType().getType() & SemanticType.NAVI.getType()) == SemanticType.NAVI.getType()) {
            BaseKernel naviKernel = new SemanticKernel(SemanticType.NAVI, new MySemanticKernelListener(SemanticType.NAVI));
            Log.i(TAG, "init: SemanticType.NAVI  ");
            mSemanticKernels.add(naviKernel);
        }
        if ((mSemanticConfig.getSemanticType().getType() & SemanticType.DUI.getType()) == SemanticType.DUI.getType()) {
            BaseKernel duiKernel = new SemanticDUIKernel(new MySemanticKernelListener(SemanticType.DUI));
            Log.i(TAG, "init: SemanticType.DUI  ");
            mSemanticKernels.add(duiKernel);
        }
        if ((mSemanticConfig.getSemanticType().getType() & SemanticType.BUILDIN.getType()) == SemanticType.BUILDIN.getType()) {
            BaseKernel buildinKernel = new SemanticKernel(SemanticType.BUILDIN, new MySemanticKernelListener(SemanticType.BUILDIN));
            Log.i(TAG, "init: SemanticType.BUILDIN  ");
            mSemanticKernels.add(buildinKernel);
        }
        if ((mSemanticConfig.getSemanticType().getType() & SemanticType.BCDV2.getType()) == SemanticType.BCDV2.getType()) {
            BaseKernel bcdv2Kernel = new SemanticBcdKernel(new MySemanticKernelListener(SemanticType.BCDV2));
            Log.i(TAG, "init: SemanticType.BCDV2  ");
            mSemanticKernels.add(bcdv2Kernel);
        }
        threadCount = mSemanticKernels.size();

        mScope = Scope.LOCAL_SEMANTIC;
        init(listener, semanticConfig.getContext(), TAG);

        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);

        mResultBarrier = new CyclicBarrier(threadCount, new ResultRunnable());

        mVocabsUpdateBarrier = new CyclicBarrier(threadCount, new VocabRunnable());
    }

    public void start(LocalSemanticParams semanticParams) {
        if (this.isAuthorized()) {
            this.mSemanticParams = semanticParams;
            if (TextUtils.isEmpty(semanticParams.getRefText())) {
                Log.i(TAG, "empty input,return empty result");
                AIResult result = createEmptySemantic();
                result.setLast(true);
                sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
                return;
            }
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, null);
        } else {
            showErrorMessage();
        }
    }

    @Override
    public void clearObject() {
        super.clearObject();
        mSemanticKernels.clear();
        mSemanticKernels = null;
        mVocabsUpdateBarrier = null;
        if (mSemanticConfig != null)
            mSemanticConfig = null;
        if (mSemanticParams != null)
            mSemanticParams = null;
    }


    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                handleMsgNew();
                break;
            case MSG_START:
                handleMsgStart();
                break;
            case MSG_STOP:
                handleMsgStop();
                break;
            case MSG_CANCEL:
                handleMsgCancel();
                break;
            case MSG_RESULT:
                handleMsgResult((AIResult) msg.obj);
                break;
            case MSG_UPDATE_VOCAB:
                handleMsgUpdateVocabs((Vocab[]) msg.obj);
                break;
            case MSG_UPDATE_NAVI_VOCAB:
                handleMsgUpdateNaviVocab((String) msg.obj);
                break;
            case MSG_UPDATE_RESULT:
                if (mState != EngineState.STATE_IDLE) {
                    Integer ret = (Integer) msg.obj;
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_UPDATE_RESULT, ret);
                } else {
                    trackInvalidState("update_result");
                }
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


    @Override
    public void processNoSpeechError() {

    }

    @Override
    public void processMaxSpeechError() {

    }

    private void handleMsgNew() {
        if (mState == EngineState.STATE_IDLE) {
            //semantic资源拷贝，多个目录
            int status = copyAssetsFolders(mSemanticConfig);
            if (status == AIConstant.OPT_FAILED) {
                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                return;
            }

            if (((mSemanticConfig.getSemanticType().getType() & SemanticType.NAVI.getType()) == SemanticType.NAVI.getType())) {
                Transformer.load(mSemanticConfig.getNaviSkillConfPath());
            }

            if ((mSemanticConfig.getSemanticType().getType() & SemanticType.BUILDIN.getType()) == SemanticType.BUILDIN.getType()
                    || (mSemanticConfig.getSemanticType().getType() & SemanticType.BCDV2.getType()) == SemanticType.BCDV2.getType()) {
                Transformer.load(mSemanticConfig.getBuildinSkillConfPath());
            }

            if (mSemanticKernels != null && !mSemanticKernels.isEmpty()) {
                for (BaseKernel kernel : mSemanticKernels) {
                    kernel.newKernel(mSemanticConfig);
                }
            }
            if (!TextUtils.isEmpty(mSemanticConfig.getSkillMappingPath())) {
                Log.i(TAG, "read skillMapping start: ");
                String skillMapping = FileIOUtils.readFile2String(mSemanticConfig.getSkillMappingPath());
                try {
                    if (skillMappingObj == null) skillMappingObj = new JSONObject();
                    JSONArray skillArrays = new JSONArray(skillMapping);
                    for (int i = 0; i < skillArrays.length(); i++) {
                        JSONObject obj = skillArrays.optJSONObject(i);
                        skillMappingObj.put(obj.optString("originSkill"), obj);//预处理，提高语义skill映射后处理速度
                        skillMappingObj.put(obj.optString("skillId"), obj);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i(TAG, "read skillMapping end! " + skillMappingObj.toString());
            }
            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("new");
        }
    }


    private void handleMsgUpdateNaviVocab(String cfg){
        if (mState != EngineState.STATE_IDLE) {
            if (cfg != null) {
                for (BaseKernel baseKernel : mSemanticKernels) {
                    if (baseKernel instanceof SemanticKernel) {
                        baseKernel.updateNaviVocab(cfg);
                    }
                }
            } else
                Log.e(TAG, "illegal navivocab param!" + cfg);
        } else {
            trackInvalidState("update navivocab info");
        }
    }

    private void handleMsgUpdateVocabs(Vocab[] vocabs) {
        if (mState != EngineState.STATE_IDLE) {
            if (vocabs != null) {
                for (BaseKernel baseKernel : mSemanticKernels) {
                    baseKernel.updateVocabs(vocabs);
                }
            } else
                Log.e(TAG, "illegal vocab!" + vocabs);
        } else {
            trackInvalidState("update vocab info");
        }
    }

    /**
     * start前处理，如阿拉伯数字，小数点
     */
    private void handlePreStart() {
        //1. 对文本阿拉伯数字转中文（原因：语义引擎不支持阿拉伯数字）
        String refText = mSemanticParams.getRefText();
        if (!TextUtils.isEmpty(refText)) {
            Log.i(TAG, "before handlePreStart refText: " + refText);
            refText = Utils.toChinese(refText);
            refText = Utils.toChinesePoint(refText);
            mSemanticParams.setRefText(refText);
            Log.i(TAG, "after handlePreStart refText: " + refText);
        }
    }

    private void handleMsgStart() {
        if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
            handlePreStart();
            naviResult = null;
            buildinResult = null;
            duiResult = null;
            mStartTime = System.currentTimeMillis();
            startSemanticMaxDelayTimerTask();
            if (mSemanticKernels != null && !mSemanticKernels.isEmpty()) {
                for (BaseKernel kernel : mSemanticKernels) {
                    kernel.startKernel(mSemanticParams);
                }
            }
            transferState(EngineState.STATE_RUNNING);
        } else {
            trackInvalidState("start");
        }
    }

    private void handleMsgStop() {
        if (mState == EngineState.STATE_RUNNING) {
            if (mSemanticKernels != null && !mSemanticKernels.isEmpty()) {
                for (BaseKernel kernel : mSemanticKernels) {
                    kernel.stopKernel();
                }
            }
            transferState(EngineState.STATE_WAITING);
        } else {
            trackInvalidState("stop");
        }
    }

    private void handleMsgCancel() {
        if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING
                || mState == EngineState.STATE_NEWED) {
            removeCallbackMsg();

            transferState(EngineState.STATE_NEWED);
            if (mSemanticKernels != null && !mSemanticKernels.isEmpty()) {
                for (BaseKernel kernel : mSemanticKernels) {
                    kernel.cancelKernel();
                }
            }
        } else {
            trackInvalidState("cancel");
        }
    }

    private void handleMsgResult(AIResult result) {
        cancelSemanticMaxDelayTimerTask();
        if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
            mResultTime = System.currentTimeMillis();
            Log.cost(Log.TagCostTime.NLU_START, Log.TagCostTime.NLU_OUTPUT, mStartTime, mResultTime);
            Log.i(TAG, "START.SEMANTIC.RESULT.DELAY : " + (mResultTime - mStartTime));
            Log.i(TAG, "result : " + result);
            if (result == null || TextUtils.isEmpty(result.toString())) {
                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_NULL_SEMANTIC, AIError.ERR_DESCRIPTION_NULL_SEMANTIC));
                return;
            }

            if (mSemanticConfig.isUseFormat()) {
                String nlu = result.getResultObject().toString();
                nlu = format(nlu);
                result.setResultObject(nlu);
            }

            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);

            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("result");
        }
    }

    private void handleMsgRelease() {
        if (mState != EngineState.STATE_IDLE) {
            cancelSemanticMaxDelayTimerTask();
            if (mSemanticKernels != null && !mSemanticKernels.isEmpty()) {
                for (BaseKernel kernel : mSemanticKernels) {
                    kernel.releaseKernel();
                }
            }
            clearObject();//清除实例
            transferState(EngineState.STATE_IDLE);
        } else {
            trackInvalidState("release");
        }
    }

    private void handleMsgError(AIError error) {
        cancelSemanticMaxDelayTimerTask();
        if (mState != EngineState.STATE_NEWED && mState != EngineState.STATE_IDLE) {
            if (mSemanticKernels != null && !mSemanticKernels.isEmpty()) {
                for (BaseKernel kernel : mSemanticKernels) {
                    kernel.stopKernel();
                }
            }
            transferState(EngineState.STATE_NEWED);
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
            Log.w(TAG, error.toString());
        } else {
            trackInvalidState("error");
        }
    }

    private String format(String result) {
        try {
            long startTime = System.currentTimeMillis();
            JSONObject json = new JSONObject(result);
            if (json.has("source") && !TextUtils.isEmpty(json.getString("source"))
                    && !json.getString("source").equals("dui")) {
                json = Transformer.transNgram(json, mSemanticParams.getDomain());
            }

            // skill 后处理,通过aidui内置技能抛出的skill在skillmapping中取skillid和skill
            if (json.has("skill")) {
                String skill = json.optString("skill");
                if (skillMappingObj != null && skillMappingObj.has(skill)) {
                    JSONObject targetObj = skillMappingObj.optJSONObject(skill);
                    if (targetObj.has("skill")) {
                        json.put("skill", targetObj.optString("skill"));
                    }
                    if (targetObj.has("skillId")) {
                        json.put("skillId", targetObj.optString("skillId"));
                    }
                }
            } else {//dui语义抛出没有skill,有skillid,通过skillId在skillmapping中取skill
                String skillid = json.optString("skillId");
                if (skillMappingObj != null && skillMappingObj.has(skillid)) {
                    JSONObject targetObj = skillMappingObj.optJSONObject(skillid);
                    if (targetObj.has("skill")) {
                        json.put("skill", targetObj.optString("skill"));
                    }
                }
            }
            Log.i(TAG, "format cost time: " + (System.currentTimeMillis() - startTime));
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 更新词库，目前只更新ldm中的词库信息
     */
    public void updateVocab(Vocab... vocabs) {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_UPDATE_VOCAB, vocabs);
        } else {
            showErrorMessage();
        }
    }

    /**
     * 获取词库内容
     */
    public List<String> getVocab(String vocabName) {
        List<String> vocabs = new ArrayList<String>();
        if (isAuthorized()) {
            if (mSemanticKernels != null && !mSemanticKernels.isEmpty()) {
                for (BaseKernel kernel : mSemanticKernels) {
                    if (kernel instanceof SemanticDUIKernel) {
                        vocabs = ((SemanticDUIKernel) kernel).getVocab(vocabName);
                    } else if (kernel instanceof SemanticBcdKernel) {
                        vocabs = ((SemanticBcdKernel) kernel).getVocab(vocabName);

                    }
                }
            }
        } else {
            showErrorMessage();
        }
        return vocabs;
    }


    /**
     * 语义回调
     */
    private class MySemanticKernelListener implements SemanticKernelListener {
        /**
         * 共用监听器，通过此标志来区分语义来源
         */
        private final SemanticType semanticType;

        public MySemanticKernelListener(SemanticType semanticType) {
            this.semanticType = semanticType;
        }

        @Override
        public void onInit(int status) {
            Log.i(TAG, "MySemanticKernelListener onInit : " + status);
            processInit(status);
        }

        @Override
        public void onResults(AIResult result) {
            processResult(semanticType, result);
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onUpdateResult(int ret) {
            Log.i(TAG, "onUpdateResult ret = " + ret);
            if (semanticType == SemanticType.AIDUI || semanticType == SemanticType.BCDV2) {
                mBuildinResult = ret;
            }
            if (semanticType == SemanticType.DUI) {
                mDUIUpdateResult = ret;
            }

            try {
                mVocabsUpdateBarrier.await(AIConstant.VOCAB_UPDATE_TIMEOUT, TimeUnit.MILLISECONDS);
            } catch (BrokenBarrierException | InterruptedException | TimeoutException e) {
                e.printStackTrace();
                if (mVocabsUpdateBarrier != null) {
                    mVocabsUpdateBarrier.reset();
                }
            }

        }
    }

    private void processResult(SemanticType type, AIResult result) {
        Log.i(TAG, "processResult() called with: type = " + type + ", result = " + result);
        try {
            String semantic = result.getResultObject().toString();
            JSONObject data = null;
            if (!TextUtils.isEmpty(semantic)) {
                data = new JSONObject(semantic);
            }
            if (type == SemanticType.NAVI) {
                if (data != null) {
                    data.put(AIConstant.Nlu.KEY_SOURCE, AIConstant.Nlu.SOURCE_NAVI);
                    result.setResultObject(data);
                }
                naviResult = result;
            }
            if (type == SemanticType.DUI) {
                if (data != null) {
                    data.put(AIConstant.Nlu.KEY_SOURCE, AIConstant.Nlu.SOURCE_DUI);
                    result.setResultObject(data);
                }
                duiResult = result;
                Log.cost(Log.TagCostTime.NLU_START, Log.TagCostTime.NLU_DUI_OUTPUT, mStartTime, System.currentTimeMillis());
            }
            if (type == SemanticType.BUILDIN) {
                if (data != null) {
                    data.put(AIConstant.Nlu.KEY_SOURCE, AIConstant.Nlu.SOURCE_AIDUI);
                    result.setResultObject(data);
                }
                buildinResult = result;
                Log.cost(Log.TagCostTime.NLU_START, Log.TagCostTime.NLU_AIDUI_OUTPUT, mStartTime, System.currentTimeMillis());
            }
            if (type == SemanticType.BCDV2) {
                if (data != null) {
                    data.put(AIConstant.Nlu.KEY_SOURCE, AIConstant.Nlu.SOURCE_BCDV2);
                    result.setResultObject(data);
                }
                buildinResult = result;
                Log.cost(Log.TagCostTime.NLU_START, Log.TagCostTime.NLU_BCDV2_OUTPUT, mStartTime, System.currentTimeMillis());
            }
            mResultBarrier.await(mSemanticParams.getWaitingTimeout(), TimeUnit.MILLISECONDS);
        } catch (JSONException | BrokenBarrierException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
            if (mResultBarrier != null) {
                mResultBarrier.reset();
            }
        }
    }

    private class ResultRunnable implements Runnable {
        @Override
        public void run() {
            selectSemanticResult(naviResult, duiResult, buildinResult);
        }
    }

    private class VocabRunnable implements Runnable {
        @Override
        public void run() {
            selectVocabResult(mDUIUpdateResult, mBuildinResult);
        }
    }

    private void selectVocabResult(int mDUIUpdateResult, int mBuildinResult) {
        Log.i(TAG, "selectVocabResult " + mDUIUpdateResult + "  mBuildinResult  " + mBuildinResult);
        int finalResult = -1;
        if (mDUIUpdateResult == 0 && mBuildinResult == 0) {
            finalResult = 0;
        }
        if (mListener != null) {
            mListener.onUpdateResult(finalResult);
        }
        transferState(EngineState.STATE_NEWED);

    }

    /**
     * 抉择多路语义,buildin语义优先，navi次之，dui兜底
     */
    private void selectSemanticResult(AIResult naviResult, AIResult duiResult, AIResult buildinResult) {
        Log.i(TAG, "selectSemanticResult " + duiResult + "   buildinResult " + buildinResult);
        AIResult finalResult = null;
        if (checkSemValid(duiResult) && selectRuleThreshold(duiResult)) {
            finalResult = duiResult;
        } else if (checkSemValid(naviResult)) {
            finalResult = naviResult;
        } else if (checkSemValid(buildinResult)&& selectRuleThreshold(buildinResult)) {
            finalResult = buildinResult;
        }
        if (finalResult == null) {
            if (null != mSemanticConfig && mSemanticConfig.isThrowEmptySemantic()) {
                finalResult = createEmptySemantic();
            } else {
                AIError aiError = new AIError(AIError.ERR_NULL_SEMANTIC,
                        AIError.ERR_DESCRIPTION_NULL_SEMANTIC);
                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, aiError);
                return;
            }
        }

        finalResult.setLast(true);
        sendMsgToInnerMsgQueue(EngineMsg.MSG_RESULT, finalResult);
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
     * @param duiResult DUI semantic
     * @return 是否达到阈值
     */
    public boolean selectRuleThreshold(AIResult duiResult) {
        String semantic = duiResult.getResultObject().toString();
        if (TextUtils.isEmpty(semantic) || "null".equals(semantic)) {
            Log.i(TAG, "selectRuleThreshold semantic empty");
            return false;
        }

        try {
            JSONObject data = new JSONObject(semantic);
            JSONObject semanticJson = data.optJSONObject("semantics");
            if (semanticJson.has("request")) {
                JSONObject request = semanticJson.optJSONObject("request");
                if (request.has("confidence")) {
                    double conf = request.optDouble("confidence");
                    Log.i(TAG, "conf : " + conf);
                    if (conf >= mSemanticParams.getSemanticThreshold()) {
                        return true;
                    }
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return false;
    }


    private AIResult createEmptySemantic() {
        AIResult emptyResult = new AIResult();
        JSONObject emptySemantic = new JSONObject();
        JSONObject semanticJo = new JSONObject();
        JSONObject requestJo = new JSONObject();
        JSONArray slotsJo = new JSONArray();
        try {
            requestJo.put("slotcount", 0);
            requestJo.put("slots", slotsJo);
            semanticJo.put("request", requestJo);
            emptySemantic.put("semantics", semanticJo);
            emptySemantic.put("input", mSemanticParams.getRefText());
            emptyResult.setResultObject(emptySemantic.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return emptyResult;
    }


    /**
     * 检查dui格式语义是否有效
     *
     * @param semanticResult 抛出的语义结果
     */
    private boolean checkSemValid(AIResult semanticResult) {
        if (semanticResult == null || semanticResult.getResultObject() == null) {
            Log.i(TAG, "checkDuiSemValid semanticResult empty");
            return false;
        }
        String semantic = semanticResult.getResultObject().toString();
        if (TextUtils.isEmpty(semantic)) {
            Log.i(TAG, "checkDuiSemValid semantic empty");
            return false;
        }
        try {
            JSONObject data = new JSONObject(semantic);
            JSONObject semanticJson = data.optJSONObject("semantics");
            if (semanticJson == null) {
                Log.i(TAG, "checkDuiSemValid semanticJson == null");
                return false;
            }
            JSONObject request = semanticJson.optJSONObject("request");
            if (request == null) {
                Log.i(TAG, "checkDuiSemValid request == null");
                return false;
            }
            JSONArray slots = request.optJSONArray("slots");
            if (null == slots || slots.length() == 0) {
                Log.e(TAG, "checkDuiSemValid semantic error!");
                return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return true;
    }

    private void startSemanticMaxDelayTimerTask() {
        if (semanticMaxDelayTimerTask != null) {
            semanticMaxDelayTimerTask.cancel();
            semanticMaxDelayTimerTask = null;
        }
        if (mSemanticParams.getWaitingTimeout() > 0) {
            semanticMaxDelayTimerTask = new SemanticMaxDelayTimerTask();
            try {
                AITimer.getInstance().schedule(semanticMaxDelayTimerTask, mSemanticParams.getWaitingTimeout());
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    private void cancelSemanticMaxDelayTimerTask() {
        if (semanticMaxDelayTimerTask != null) {
            semanticMaxDelayTimerTask.cancel();
            semanticMaxDelayTimerTask = null;
        }
    }

    private SemanticMaxDelayTimerTask semanticMaxDelayTimerTask;

    private class SemanticMaxDelayTimerTask extends TimerTask {

        @Override
        public void run() {
            Log.w(TAG, "semantic max delay timeout");
            selectSemanticResult(naviResult, duiResult, buildinResult);
        }
    }

}
