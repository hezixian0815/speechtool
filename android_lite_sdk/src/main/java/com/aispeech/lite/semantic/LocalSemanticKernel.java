package com.aispeech.lite.semantic;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.common.Transformer;
import com.aispeech.export.Vocab;
import com.aispeech.kernel.SemanticDUI;
import com.aispeech.kernel.SemanticNAVI;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.SemanticType;
import com.aispeech.lite.config.LocalSemanticConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.LocalSemanticParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 离线语义内核，包含 dui 和 navi 语义
 *
 * @author aispeech
 * @deprecated {@link SemanticKernel}
 */
public class LocalSemanticKernel extends BaseKernel {
    private static final String TAG = "LocalSemanticKernel";

    private SemanticKernelListener mListener;
    private LocalSemanticConfig mSemanticConfig;
    private LocalSemanticParams mSemanticParams;

    private SemanticNAVI mNaviEngine;
    private NaviCallback mNaviCallback;

    private SemanticDUI mDuiEngine;
    private DUICallback mDuiCallback;

    private String naviSemResult;
    private String duiSemResult;

    public LocalSemanticKernel(SemanticKernelListener listener) {
        super(TAG, listener);
        mListener = listener;
    }

    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    mSemanticConfig = (LocalSemanticConfig) message.mObject;
                    if (mSemanticConfig != null) {
                        Log.d(TAG, "mSemanticConfig: " + mSemanticConfig);
                        int status = 0;
                        if (mSemanticConfig.getSemanticType() == SemanticType.NAVI) {
                            int retNavi = initNaviSemantic();
                            status = retNavi;
                        } else if (mSemanticConfig.getSemanticType() == SemanticType.DUI) {
                            int retDUI = initDuiSemantic();
                            status = retDUI;
                        } else {
                            int retDUI = initDuiSemantic();
                            int retNavi = initNaviSemantic();
                            status = (retDUI < 0 || retNavi < 0) ? -1 : 0;
                        }
                        Log.d(TAG, "initSemantic: " + status);
                        mListener.onInit(status);
                    } else {
                        Log.e(TAG, "mSemanticConfig is null!!!");
                        mListener.onError(new AIError(AIError.ERR_AI_ENGINE, AIError.ERR_DESCRIPTION_AI_ENGINE));
                    }
                    break;
                case Message.MSG_START:
                    duiSemResult = null;
                    naviSemResult = null;
                    mSemanticParams = (LocalSemanticParams) message.mObject;
                    int retDUI = 0;
                    int retNavi = 0;
                    if (mSemanticConfig.getSemanticType() == SemanticType.NAVI) {
                        if (mNaviEngine != null) {
                            retNavi = mNaviEngine.startSemantic(mSemanticParams.toJSON().toString());
                        }
                    } else if (mSemanticConfig.getSemanticType() == SemanticType.DUI) {
                        if (mDuiEngine != null) {
                            retDUI = mDuiEngine.startSemanticDUI(mSemanticParams.toDUIJSON().toString());
                        }
                    } else {
                        if (mNaviEngine != null && mDuiEngine != null) {
                            retDUI = mDuiEngine.startSemanticDUI(mSemanticParams.toDUIJSON().toString());
                            retNavi = mNaviEngine.startSemantic(mSemanticParams.toJSON().toString());
                        }
                    }
                    Log.d(TAG, "MSG_START,retDUI : " + retDUI + ",retNavi : " + retNavi);
                    if ((retDUI + retNavi) < 0) {
                        AIError aiError = new AIError(AIError.ERR_AI_ENGINE, AIError.ERR_DESCRIPTION_AI_ENGINE);
                        mQueue.put(new Message(Message.MSG_ERROR, aiError));
                    }
                    break;
                case Message.MSG_RELEASE:
                    if (mNaviEngine != null) {
                        mNaviEngine.destroySemantic();
                        mNaviEngine = null;
                    }
                    if (mNaviCallback != null) {
                        mNaviCallback = null;
                    }
                    if (mDuiEngine != null) {
                        mDuiEngine.destroySemanticDUI();
                        mDuiEngine = null;
                    }
                    if (mDuiCallback != null) {
                        mDuiCallback = null;
                    }
                    isReleased = true;
                    break;
                case Message.MSG_ERROR:
                    mListener.onError((AIError) message.mObject);
                    break;
                case Message.MSG_UPDATE_VOCAB:
                    if (mDuiEngine != null) {
                        mDuiEngine.destroySemanticDUI();
                    }
                    Vocab vocab = (Vocab) message.mObject;
                    if (null != vocab) {
                        updateldmVocab(vocab);
                    }
                    int status_init = initDuiSemantic();
                    Log.d(TAG, "update semantic status : " + status_init);
                    if (status_init == AIConstant.OPT_FAILED) {
                        mListener.onError(new AIError(AIError.ERR_NET_BIN_INVALID,
                                AIError.ERR_DESCRIPTION_INVALID_NET_BIN));
                    }
                    mListener.onUpdateResult(status_init);
                    break;
                case Message.MSG_UPDATE_NAVI_VOCAB:
                    String cfg = (String) message.mObject;
                    updateNaviVocab(cfg);
                default:
                    break;
            }
            if (isReleased) {
                innerRelease();
                break;//release后跳出while循环
            }
        }
    }

    private int initNaviSemantic() {
        int status;
        if (mSemanticConfig != null) {
            String configStr = mSemanticConfig.toJson().toString();
            Log.d(TAG, "config: " + configStr);
            mNaviCallback = new NaviCallback();
            mNaviEngine = new SemanticNAVI();
            long id = mNaviEngine.initSemantic(configStr, mNaviCallback);
            if (id == 0) {
                Log.e(TAG, "Navi 引擎初始化失败,请检查资源文件是否在指定路径下！");
                status = AIConstant.OPT_FAILED;
            } else {
                Log.d(TAG, "Navi 引擎初始化成功");
                status = AIConstant.OPT_SUCCESS;
            }
        } else {
            status = AIConstant.OPT_FAILED;
            Log.e(TAG, "config == null");
        }
        return status;
    }

    private int initDuiSemantic() {
        int status;
        if (mSemanticConfig != null) {
            String configStr = mSemanticConfig.toDUIJson().toString();
            Log.d(TAG, "initDuiSemantic: configStr = " + configStr);
            mDuiCallback = new DUICallback();
            mDuiEngine = new SemanticDUI();
            long id = mDuiEngine.initSemanticDUI(configStr, mDuiCallback);
            if (id == 0) {
                Log.e(TAG, "DUI 引擎初始化失败,请检查资源文件是否在指定路径下！");
                status = AIConstant.OPT_FAILED;
            } else {
                Log.d(TAG, "DUI 引擎初始化成功");
                status = AIConstant.OPT_SUCCESS;
            }
        } else {
            status = AIConstant.OPT_FAILED;
            Log.e(TAG, "config == null");
        }
        return status;
    }

    /**
     * dui 结果抉择
     *
     * @param semantic 语义结果
     */
    private void selectFromDui(String semantic) {
        Log.d(TAG, "select from dui,semantic = " + semantic);
        duiSemResult = semantic;
        if (!checkSemValid(duiSemResult)) {
            if (naviSemResult == null) {
                Log.d(TAG, "dui first come, but dui sem is empty, waiting navi... ");
            } else {
                finalSemResult(naviSemResult);// dui second come, but dui sem is empty, select navi
            }
        } else {
            finalSemResult(duiSemResult);
        }
    }

    /**
     * navi 结果抉择
     *
     * @param semantic 语义结果
     */
    private void selectFromNavi(String semantic) {
        Log.d(TAG, "select from navi,semantic = " + semantic);
        try {
            if (mSemanticConfig.isUseFormat()) {
                naviSemResult = Transformer.transNgram(new JSONObject(semantic), mSemanticParams.getTask()).toString();
            } else {
                naviSemResult = semantic;
            }

            if (duiSemResult == null) {
                Log.d(TAG, "navi first come, but navi sem is invalid, waiting dui... ");
            } else {
                if (!checkSemValid(naviSemResult)) {
                    finalSemResult(duiSemResult);
                } else {
                    finalSemResult(naviSemResult);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            sendMessage(new Message(Message.MSG_ERROR,
                    new AIError(AIError.ERR_NULL_SEMANTIC, AIError.ERR_DESCRIPTION_NULL_SEMANTIC)));
        }
    }

    /**
     * 检查语义是否有效
     *
     * @param semantic 抛出的语义结果
     */
    private boolean checkSemValid(String semantic) {
        return !TextUtils.isEmpty(semantic) && !TextUtils.equals(semantic, "null")
                && semantic.contains("semantics");
    }

    /**
     * 输出最终语义结果
     *
     * @param semantic 语义信息
     */
    private void finalSemResult(String semantic) {
        Log.d(TAG, "finalSemResult = " + semantic);
        if (!checkSemValid(semantic)) {
            Log.e(TAG, "finalSemResult semantic is invalid!!!");
            sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_NULL_SEMANTIC, AIError.ERR_DESCRIPTION_NULL_SEMANTIC)));
            return;
        }
        try {
            AIResult result = new AIResult();
            JSONObject semObj = new JSONObject(semantic);
            result.setResultObject(semObj.toString());
            result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
            result.setTimestamp(System.currentTimeMillis());
            if (semObj.has("eof")) {
                int eof = semObj.optInt("eof", 1);
                result.setLast(eof == 1);
            } else {
                result.setLast(true);
            }
            if (mListener != null) {
                mListener.onResults(result);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 离线 navi 语义回调 如果只需要navi结果 直接抛出，否则需要走 navi dui结果抉择
     */
    private class NaviCallback implements SemanticNAVI.semantic_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            byte[] buffer = new byte[size];
            System.arraycopy(data, 0, buffer, 0, size);
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                if (mSemanticConfig.getSemanticType() == SemanticType.NAVI) {
                    finalSemResult(new String(buffer));
                } else if (mSemanticConfig.getSemanticType() == SemanticType.MIX_NAVI_DUI) {
                    selectFromNavi(new String(buffer));
                }
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }

    /**
     * 离线 dui 语义回调 如果只需要dui结果 直接抛出，否则需要走 navi dui结果抉择
     */
    private class DUICallback implements SemanticDUI.semantic_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            byte[] buffer = new byte[size];
            System.arraycopy(data, 0, buffer, 0, size);
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                if (mSemanticConfig.getSemanticType() == SemanticType.DUI) {
                    finalSemResult(new String(buffer));
                } else if (mSemanticConfig.getSemanticType() == SemanticType.MIX_NAVI_DUI) {
                    selectFromDui(new String(buffer));
                }
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }

    public List<String> getVocab(String vocabName) {
        List<String> contents = new ArrayList<>();
        File file = new File(mSemanticConfig.getDUIResPath() + "/ldm/" + vocabName + ".txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                contents.add(tempStr);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return contents;
    }

    /**
     * 更新词库，跟新ldm下的词库信息
     *
     * @param vocab 词库
     */
    public void updateldmVocab(Vocab vocab) {
        if (TextUtils.isEmpty(mSemanticConfig.getDUIResPath())) {
            throw new IllegalArgumentException("请设置DUI Res路径!");
        }
        if (TextUtils.isEmpty(vocab.getName()))
            throw new IllegalArgumentException("请设置词库名称!");

        if (FileUtils.createOrExistsFile(mSemanticConfig.getDUIResPath() + "/ldm/" + vocab.getName() + ".txt")) {
            FileWriter writer;
            try {
                writer = new FileWriter(mSemanticConfig.getDUIResPath() + "/ldm/" + vocab.getName() + ".txt");
                StringBuilder sb = new StringBuilder();
                if (null != vocab.getContents()) {
                    for (String letter : vocab.getContents()) {
                        sb.append(letter + "\n");
                    }
                }
                if (vocab.getAction().equals(Vocab.ACTION_INSERT)) {
                    writer.write(sb.toString());
                } else if (vocab.getAction().equals(Vocab.ACTION_CLEAR_AND_INSERT)) {
                    writer.write("");
                    writer.write(sb.toString());
                } else {
                    writer.write("");
                }
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 更新离线内置语义词库
     *
     * @param semanticVocabsCfg 词库
     */
    public void updateNaviVocab(String semanticVocabsCfg) {
        if (mNaviEngine != null) {
            int status = mNaviEngine.updateKV2Bin(semanticVocabsCfg);
            Log.d(TAG, "update semantic navi vocab status : " + status);
            if (status!=0){
                mListener.onError(new AIError(AIError.ERR_SEMANTIC_UPDAYE_NAVI, AIError.ERR_DESCRIPTION_SEMANTIC_UPDAYE_NAVI));
            }else{
                mListener.onUpdateResult(status);
            }
        }else {
            Log.e(TAG,"mNaviEngine null");
            mListener.onError(new AIError(AIError.ERR_FDM_NO_INIT, AIError.ERR_DESCRIPTION_ERR_SDK_NOT_INIT));
        }

    }

}
