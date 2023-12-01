package com.aispeech.lite.grammar;

import android.os.Message;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.analysis.AnalysisProxy;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.config.LocalGrammarConfig;
import com.aispeech.lite.param.LocalGrammarParams;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by wuwei on 18-5-11.
 */

public class LocalGrammarProcessor extends BaseProcessor {
    public static final String TAG = "AIGrammarProcessor";
    private LocalGrammarKernel grammarKernel;
    private LocalGrammarConfig mConfig;
    private LocalGrammarParams mParams;

    public void init(SpeechListener listener, LocalGrammarConfig config) {
        mScope = Scope.LOCAL_GRAMMAR;
        init(listener, config.getContext(), TAG);
        grammarKernel = new LocalGrammarKernel(new MyGrammarListenerImpl());
        sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, config);
    }

    public void startMulti(LocalGrammarParams... params) {
        Log.i(TAG, "startMulti");
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, params);
        } else {
            showErrorMessage();
        }
    }

    public void start(LocalGrammarParams params) {
        Log.i(TAG, "start");
        if (isAuthorized()) {
            mParams = params;
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, params);
        } else {
            showErrorMessage();
        }
    }
    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                handleMsgNew(msg);
                break;
            case MSG_START:
                handleMsgStart(msg);
                break;
            case MSG_RELEASE:
                if (mState != EngineState.STATE_IDLE) {
                    if (grammarKernel != null) {
                        grammarKernel.releaseKernel();
                        grammarKernel = null;
                    }
                    transferState(EngineState.STATE_IDLE);
                    clearObject();
                } else {
                    trackInvalidState("release");
                }
                break;
            case MSG_ERROR:
                AIError error = (AIError) msg.obj;
                Log.w(TAG, error.toString());
                if (mState != EngineState.STATE_IDLE) {
                    transferState(EngineState.STATE_NEWED);
                    sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, msg.obj);
                    uploadError(error, mParams);
                }
                break;
            default:
                break;
        }
    }

    private void handleMsgStart(Message msg) {
        if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_RUNNING) {
            // 更新试用次数
            if (!updateTrails(mProfileState)) {
                return;
            }
            //gram编译多个bin
            if (grammarKernel != null) {
                if (msg.obj instanceof LocalGrammarParams[]) {
                    LocalGrammarParams[] obj = (LocalGrammarParams[]) msg.obj;
//                    grammarKernel.cancelKernel();//清空上一次还在build的操作
                    grammarKernel.startKernel(Arrays.asList(obj));
                } else if (msg.obj instanceof LocalGrammarParams) {
                    grammarKernel.startKernel((LocalGrammarParams) msg.obj);
                } else {
                    Log.e(TAG, "illegal params when msg start :" + msg.obj);
                }
            }
        } else {
            trackInvalidState("start");
        }
    }

    private void handleMsgNew(Message msg) {
        if (mState == EngineState.STATE_IDLE) {
            LocalGrammarConfig config = (LocalGrammarConfig) msg.obj;
            mConfig = config;
            // init gram
            if (copyAssetsRes(mConfig) == AIConstant.OPT_FAILED) {
                Log.e(TAG, "copy gram res fail!!!");
                sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_RES_PREPARE_FAILED,
                        AIError.ERR_DESCRIPTION_RES_PREPARE_FAILED));
                return;
            }

            grammarKernel.newKernel(mConfig);
            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("new");
        }
    }

    @Override
    public void processNoSpeechError() {
        //do nothing
    }

    @Override
    public void processMaxSpeechError() {
        //do nothing
    }

    class MyGrammarListenerImpl implements LocalGrammarListener {

        @Override
        public void onInit(int status) {
            if (mOutListener != null) {
                mOutListener.onInit(status);
            }
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }

        @Override
        public void onBuildCompleted(AIResult aiResult) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_GRAMMAR_SUCCESS, aiResult);
            uploadMultiError(aiResult);
        }
    }

    private void uploadMultiError(AIResult aiResult) {
        try {
            if (aiResult.getResultObject() instanceof List) {
                List<LocalGrammarParams> paramsList = (List<LocalGrammarParams>) aiResult.getResultObject();
                if (paramsList == null) return;
                for (LocalGrammarParams params : paramsList) {
                    if (!params.isBuildSuccess() && params.getAIError() != null) {
                        uploadError(params.getAIError(), params);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadError(AIError aiError, LocalGrammarParams params) {
        Log.d(TAG, "uploadError");
        JSONObject input = new JSONObject();
        //添加message外面的字段
        Map<String, Object> entryMap = new HashMap<>();
        entryMap.put("recordId", mRecorderId);
        entryMap.put("mode", "lite");
        entryMap.put("module", "local_exception");
        try {
            if (mConfig != null) {
                input.put("config", mConfig.toJson());
            }
            if (params != null) {
                input.put("param", params.toJson());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AnalysisProxy.getInstance().getAnalysisMonitor().cacheData("local_grammar_exception", "info", "local_exception",
                mRecorderId, input, aiError.getOutputJSON(), entryMap);
    }

}
