package com.aispeech.lite.semantic;

import static com.aispeech.lite.SemanticType.BUILDIN;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.FileIOUtils;
import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.Vocab;
import com.aispeech.kernel.SemanticNAVI;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.SemanticType;
import com.aispeech.lite.config.LocalSemanticConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.LocalSemanticParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;


public class SemanticKernel extends BaseKernel {

    private static final String TAG = "SemanticKernel";
    private SemanticKernelListener mListener;
    private SemanticNAVI mEngine;
    private MySemantic_callback mySemantic_callback;
    private LocalSemanticConfig semanticConfig;
    /**
     * 复用SemanticKernel时通过此标志区分多路语义
     */
    private SemanticType semanticType;

    public SemanticKernel(SemanticKernelListener listener) {
        super(TAG,listener);
        mListener = listener;
    }

    public SemanticKernel(SemanticType type, SemanticKernelListener listener) {
        super(TAG,listener);
        mListener = listener;
        semanticType = type;
    }


    /**
     * 准备资源并初始化引擎
     *
     * @return -1(失败)/0(成功)
     */
    private int initSemantic() {
        int status;
        if (semanticConfig != null) {
            String configStr = "";
            if (semanticType == SemanticType.NAVI) {
                configStr = semanticConfig.toJson().toString();
            } else if (semanticType == SemanticType.BUILDIN) {
                configStr = semanticConfig.toBuildinJson().toString();
            }
            Log.d(TAG, "config: " + configStr);
            mySemantic_callback = new MySemantic_callback();
            long id = mEngine.initSemantic(configStr, mySemantic_callback);
            if (id == 0) {
                Log.e(TAG, "引擎初始化失败,type = " + semanticType.toString() + ",请检查资源文件是否在指定路径下！");
                status = AIConstant.OPT_FAILED;
            } else {
                Log.d(TAG, "引擎初始化成功，type = " + semanticType.toString());
                status = AIConstant.OPT_SUCCESS;
            }
        } else {
            status = AIConstant.OPT_FAILED;
            Log.e(TAG, "config == null");
        }
        return status;
    }


    private class MySemantic_callback implements SemanticNAVI.semantic_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            byte[] buffer = new byte[size];
            System.arraycopy(data, 0, buffer, 0, size);
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String result = new String(buffer);
                if (TextUtils.isEmpty(result) || TextUtils.equals("null", result)) {
                    sendMessage(new Message(Message.MSG_ERROR,
                            new AIError(AIError.ERR_NULL_SEMANTIC_INPUT, AIError.ERR_DESCRIPTION_NULL_SEMANTIC_INPUT)));
                } else {
                    try {
                        processLocalSemanticCallback(new String(buffer));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        sendMessage(new Message(Message.MSG_ERROR,
                                new AIError(AIError.ERR_NULL_SEMANTIC_INPUT, AIError.ERR_DESCRIPTION_NULL_SEMANTIC_INPUT)));
                    }
                }
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }

    private void processLocalSemanticCallback(String localSemanticRetStr) throws JSONException {
        Log.d(TAG, "LOCAL.SEMANTIC.RESULT: " + localSemanticRetStr);
        JSONObject resultJson = new JSONObject(localSemanticRetStr);
        if (resultJson != null) {
            AIResult result = new AIResult();
            result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
            result.setResultObject(localSemanticRetStr);
            result.setTimestamp(System.currentTimeMillis());
            if (resultJson.has("eof")) {
                int eof = resultJson.optInt("eof", 1);
                if (eof == 1) {
                    result.setLast(true);
                } else {
                    result.setLast(false);
                }
            }
            if (mListener != null) {
                mListener.onResults(result);
            }
        }
    }

    protected void startSemantic(String paramStr) {
        sendMessage(new Message(Message.MSG_START, paramStr));
    }


    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    mEngine = new SemanticNAVI();
                    semanticConfig = (LocalSemanticConfig) message.mObject;
                    int status = initSemantic();
                    Log.d(TAG, "initSemantic: " + status);
                    mListener.onInit(status);
                    break;
                case Message.MSG_START:
                    if (mEngine != null) {
                        LocalSemanticParams semanticParams = (LocalSemanticParams) message.mObject;
                        int ret = mEngine.startSemantic(semanticParams.toString());
                        Log.d(TAG, "ret:" + ret);
                        if (ret < 0) {
                            AIError aiError = new AIError(AIError.ERR_AI_ENGINE, AIError.ERR_DESCRIPTION_AI_ENGINE);
                            mQueue.put(new Message(Message.MSG_ERROR, aiError));
                        }
                    }
                    break;
                case Message.MSG_UPDATE_VOCAB:
                    //当前只针对多领域的内置语义需要更新词库，纯离线导航不需要更新词库
                    if ((semanticConfig.getSemanticType().getType() & BUILDIN.getType()) == BUILDIN.getType()) {
                        if (TextUtils.isEmpty(semanticConfig.getVocabCfgPath())) {
                            Log.e(TAG, "vocab cfg path is empty,please check config");
                            if (mListener != null) mListener.onUpdateResult(-1);
                            return;
                        }
                        String vocabsTxtDir = semanticConfig.getBuildinResPath() + File.separator + "vocabs_txt";
                        if (FileUtils.createOrExistsDir(vocabsTxtDir)) {
                            Vocab[] vocabs = (Vocab[]) message.mObject;
                            boolean needRestart = false;
                            for (Vocab vocab : vocabs) {
                                if (writeVocabToTxt(vocab, vocabsTxtDir)) needRestart = true;
                            }
                            // 如果存放在assets目录下，每次都会拷贝覆盖，故而每次都需要重启
                            String buildinResPath = semanticConfig.getBuildinResPath();
                            if (buildinResPath == null) buildinResPath = "";
                            if (!needRestart && !buildinResPath.startsWith(Util.getResourceDir(AISpeech.getContext()))) {
                                mListener.onUpdateResult(AIConstant.OPT_SUCCESS);
                                Log.i(TAG, "warning! vocabs is all same or write fail,skip restart kernel");
                                break;
                            }
                            String vocabsTextJson = semanticConfig.getVocabsTxtJson(vocabsTxtDir).toString();
                            Log.d(TAG, "semantic update vocab cfg = " + vocabsTextJson);
                            int ret = mEngine.updateKV2Bin(vocabsTextJson);
                            Log.d(TAG, "update semantic ret : " + ret);
                            if (ret == AIConstant.OPT_SUCCESS) {
                                //note: 需重启引擎生效，如果同时同步多个词库，多次重启不合理。目前场景不多，如果真有需求可兼容下---更新多个词库后只重启一次
                                mEngine.destroySemantic();
                                initSemantic();
                            }
                            mListener.onUpdateResult(ret);
                        }
                    }
                    break;
                case Message.MSG_UPDATE_NAVI_VOCAB:
                    String cfg = (String) message.mObject;
                    updateNaviVocab(cfg);
                    break;
                case Message.MSG_RELEASE:
                    if (mEngine != null) {
                        mEngine.destroySemantic();
                        mEngine = null;
                    }
                    if (mySemantic_callback != null) {
                        mySemantic_callback = null;
                    }
                    isReleased = true;
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

    /**
     * 写入词库到txt
     *
     * @param vocab 词库内容
     * @return 是否写入成功，如果内容为空或者一致则不需要写入，视为写入失败
     */
    private boolean writeVocabToTxt(Vocab vocab, String vocabTxtDir) {
        Log.d(TAG, "updateVocab " + vocab.getName());

        if (TextUtils.isEmpty(vocab.getName()))
            throw new IllegalArgumentException("请设置词库名称!");

        String vocabTxtPath = vocabTxtDir + File.separator + vocab.getName() + ".txt";
        if (FileUtils.createOrExistsFile(vocabTxtPath)) {
            StringBuilder sb = new StringBuilder();
            if (null != vocab.getContents()) {
                for (String letter : vocab.getContents()) {
                    sb.append(letter).append("\n");
                }
                String content = sb.toString();
                // 判断内容是否一致，注意这里可能会有分词操作，直接拿最终的数据进行比较
                String existContent = FileIOUtils.readFile2String(vocabTxtPath);
                if (existContent == null) existContent = "";
                if (content.trim().equals(existContent.trim())) {
                    Log.d(TAG, "same content skip write");
                    return false;
                }
                FileIOUtils.writeFileFromString(vocabTxtPath, content);
                return true;
            }
        } else {
            Log.e(TAG, "create vocab dir error!");
        }
        return false;
    }

    /**
     * 更新离线内置语义词库
     *
     * @param semanticVocabsCfg 词库
     */
    public void updateNaviVocab(String semanticVocabsCfg) {
        if (mEngine != null) {
            int status = mEngine.updateKV2Bin(semanticVocabsCfg);
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
