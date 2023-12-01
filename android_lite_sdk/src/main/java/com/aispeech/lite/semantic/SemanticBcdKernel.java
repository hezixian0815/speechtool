package com.aispeech.lite.semantic;

import static com.aispeech.lite.SemanticType.BCDV2;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.FileIOUtils;
import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.export.Vocab;
import com.aispeech.kernel.SemanticBCD;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.LocalSemanticConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.LocalSemanticParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SemanticBcdKernel extends BaseKernel {

    private static final String TAG = "SemanticBcdKernel";
    private SemanticKernelListener mListener;
    private SemanticBCD mEngine;
    private MySemantic_callback mySemantic_callback;
    private LocalSemanticConfig semanticConfig;
    private LocalSemanticParams semanticParams;
    private SemanticDataHelper semanticDataHelper;

    public SemanticBcdKernel(SemanticKernelListener listener) {
        super(TAG, listener);
        mListener = listener;
    }

    /**
     * 准备资源并初始化引擎
     *
     * @return -1(失败)/0(成功)
     */
    private int initSemantic() {
        int status;
        if (semanticConfig != null) {
            String configStr = semanticConfig.toBuildinBcdV2Json().toString();
            Log.d(TAG, "config: " + configStr);
            mySemantic_callback = new MySemantic_callback();
            long id = mEngine.initSemantic(configStr, mySemantic_callback);
            if (id == 0) {
                Log.e(TAG, "引擎初始化失败,请检查资源文件是否在指定路径下！configStr = " + configStr);
                status = AIConstant.OPT_FAILED;
            } else {
                Log.d(TAG, "引擎初始化成功，configStr = " + configStr);
                status = AIConstant.OPT_SUCCESS;
            }
        } else {
            status = AIConstant.OPT_FAILED;
            Log.e(TAG, "config == null");
        }
        return status;
    }


    private class MySemantic_callback implements SemanticBCD.semantic_callback {
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
        Log.i(TAG, "LOCAL.SEMANTICBCDV2.RESULT: " + localSemanticRetStr);
        JSONObject resultJson = new JSONObject(localSemanticRetStr);
        if (semanticDataHelper != null)
            semanticDataHelper.saveBCDV2OriginalOutput(localSemanticRetStr);

        AIResult result = new AIResult();
        result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
        result.setTimestamp(System.currentTimeMillis());


        if (resultJson.has("semantics")) {
            result.setLast(true);
            if (semanticDataHelper != null)
                semanticDataHelper.saveBCDV2FinalOutput(resultJson.toString());

            if (semanticParams.isEnableBCDDiscard() && semanticDataHelper != null)
                semanticDataHelper.saveBCDV2PassDiscard(resultJson.toString());
        }

        result.setResultObject(resultJson.toString());
        if (mListener != null) {
            mListener.onResults(result);
        }
    }

    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    mEngine = new SemanticBCD();
                    semanticDataHelper = new SemanticDataHelper();
                    semanticDataHelper.init();
                    semanticConfig = (LocalSemanticConfig) message.mObject;
                    int status = initSemantic();
                    Log.d(TAG, "initSemantic: " + status);
                    mListener.onInit(status);
                    break;
                case Message.MSG_START:
                    if (mEngine != null) {
                        semanticParams = (LocalSemanticParams) message.mObject;
                        Log.d(TAG, "start with:" + semanticParams);
                        if (semanticDataHelper != null)
                            semanticDataHelper.saveBCDV2RefText(semanticParams.getRefText());
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
                    if ((semanticConfig.getSemanticType().getType() & BCDV2.getType()) == BCDV2.getType()) {
                        if (TextUtils.isEmpty(semanticConfig.getVocabCfgPath())) {
                            Log.e(TAG, "vocab cfg path is empty,please check config");
                            if (mListener != null) mListener.onUpdateResult(-1);
                            return;
                        }
                        String vocabsTxtDir = semanticConfig.getBuildinResPath() + File.separator + "vocabs_bcdv2_txt";
                        if (FileUtils.createOrExistsDir(vocabsTxtDir)) {
                            Vocab[] vocabs = (Vocab[]) message.mObject;
                            for (Vocab vocab : vocabs) {
                                writeVocabToTxt(vocab, vocabsTxtDir);
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
                case Message.MSG_RELEASE:
                    if (mEngine != null) {
                        mEngine.destroySemantic();
                        mEngine = null;
                    }
                    if (mySemantic_callback != null) {
                        mySemantic_callback = null;
                    }
                    if (semanticDataHelper != null) {
                        semanticDataHelper.release();
                        semanticDataHelper = null;
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
     */
    private void writeVocabToTxt(Vocab vocab, String vocabTxtDir) {
        Log.d(TAG, "updateVocab " + vocab.getName());

        if (TextUtils.isEmpty(vocab.getName()))
            throw new IllegalArgumentException("请设置词库名称!");

        String vocabTxtPath = vocabTxtDir + File.separator + vocab.getName() + ".txt";
        if (FileUtils.createOrExistsFile(vocabTxtPath)) {
            StringBuilder sb = new StringBuilder();
            if (null != vocab.getContents()) {
                for (String letter : vocab.getContents()) {
                    sb.append(letter + "\n");
                }
                FileIOUtils.writeFileFromString(vocabTxtPath, sb.toString());
            }
        } else {
            Log.e(TAG, "create vocab dir error!");
        }
    }

    public List<String> getVocab(String vocabName) {
        List<String> contents = new ArrayList<>();
        File file = new File(semanticConfig.getBuildinResPath() + "/vocabs/bin/" + vocabName + ".txt");
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


}
