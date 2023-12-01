package com.aispeech.lite.semantic;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.CloseUtils;
import com.aispeech.common.FileIOUtils;
import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.Vocab;
import com.aispeech.kernel.SemanticDUI;
import com.aispeech.lite.AISpeech;
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
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class SemanticDUIKernel extends BaseKernel {

    private static final String TAG = "SemanticDUIKernel";
    private SemanticKernelListener mListener;
    private SemanticDUI mEngine;
    private MySemantic_callback mySemantic_callback;
    private LocalSemanticConfig semanticConfig;

    public SemanticDUIKernel(SemanticKernelListener listener) {
        super(TAG,listener);
        mListener = listener;
    }

    /**
     * 准备资源并初始化引擎
     *
     * @param semantic 　内核语义引擎
     * @param config   　config参数
     * @return -1(失败)/0(成功)
     */
    public int initSemantic(SemanticDUI semantic, LocalSemanticConfig config) {
        int status = AIConstant.OPT_FAILED;
        if (config != null) {
            String configStr = config.toDUIJson().toString();
            Log.d(TAG, "config: " + configStr);
            mySemantic_callback = new MySemantic_callback();
            long id = semantic.initSemanticDUI(configStr, mySemantic_callback);
            if (id == 0) {
                Log.e(TAG, "引擎初始化失败,请检查资源文件是否在指定路径下！");
                status = AIConstant.OPT_FAILED;
            } else {
                Log.d(TAG, "引擎初始化成功");
                status = AIConstant.OPT_SUCCESS;
            }
        } else {
            status = AIConstant.OPT_FAILED;
            Log.e(TAG, "config == null");
        }
        return status;
    }


    private class MySemantic_callback implements SemanticDUI.semantic_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            byte[] buffer = new byte[size];
            System.arraycopy(data, 0, buffer, 0, size);
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String result = new String(buffer);
                if (TextUtils.isEmpty(result) || TextUtils.equals("null", result)) {
                    if (null != mListener) {
                        AIResult nullResult = new AIResult();
                        nullResult.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
                        nullResult.setResultObject("");
                        nullResult.setTimestamp(System.currentTimeMillis());
                        nullResult.setLast(true);
                        mListener.onResults(nullResult);
                    }
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
                    mEngine = new SemanticDUI();
                    semanticConfig = (LocalSemanticConfig) message.mObject;
                    int status = initSemantic(mEngine, semanticConfig);
                    Log.d(TAG, "initSemantic: " + status);
                    mListener.onInit(status);
                    break;
                case Message.MSG_START:
                    if (mEngine != null) {
                        LocalSemanticParams semanticParams = (LocalSemanticParams) message.mObject;
                        int ret = mEngine.startSemanticDUI(semanticParams.toDUIJSON().toString());
                        if (ret < 0) {
                            AIError aiError = new AIError(AIError.ERR_AI_ENGINE, AIError.ERR_DESCRIPTION_AI_ENGINE);
                            mQueue.put(new Message(Message.MSG_ERROR, aiError));
                        }
                    }
                    break;
                case Message.MSG_UPDATE_VOCAB:
                    Vocab[] vocabs = (Vocab[]) message.mObject;
                    boolean needRestart = false;
                    for (Vocab vocab : vocabs) {
                        if (null != vocab) {
                            if (writeVocabToTxt(vocab)) needRestart = true;
                        }
                    }
                    // 如果存放在assets目录下，每次都会拷贝覆盖，故而每次都需要重启
                    if (!needRestart && !semanticConfig.getDUIResPath().startsWith(Util.getResourceDir(AISpeech.getContext()))) {
                        mListener.onUpdateResult(AIConstant.OPT_SUCCESS);
                        Log.i(TAG, "warning! vocabs is all same or write fail,skip restart kernel");
                        break;
                    }

                    if (mEngine != null) {
                        mEngine.destroySemanticDUI();
                    }
                    mEngine = new SemanticDUI();
                    int status_init = initSemantic(mEngine, semanticConfig);
                    Log.d(TAG, "update semantic status : " + status_init);
                    if (status_init == AIConstant.OPT_FAILED) {
                        mListener.onError(new AIError(AIError.ERR_NET_BIN_INVALID,
                                AIError.ERR_DESCRIPTION_INVALID_NET_BIN));
                    }
                    mListener.onUpdateResult(status_init);
                    break;
                case Message.MSG_RELEASE:
                    if (mEngine != null) {
                        mEngine.destroySemanticDUI();
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
     * 写入词库到ldm目录下
     *
     * @param vocab 词库
     * @return 是否写入成功，如果内容为空或者一致则不需要写入，视为写入失败
     */
    private boolean writeVocabToTxt(Vocab vocab) {
        try {
            if (TextUtils.isEmpty(semanticConfig.getDUIResPath())) {
                throw new IllegalArgumentException("请设置DUI Res路径!");
            }
            if (TextUtils.isEmpty(vocab.getName()))
                throw new IllegalArgumentException("请设置词库名称!");

            String vocabTxtPath = semanticConfig.getDUIResPath() + "/ldm/" + vocab.getName() + ".txt";
            if (FileUtils.createOrExistsFile(vocabTxtPath)) {
                FileWriter writer = null;
                try {
                    StringBuilder sb = new StringBuilder();
                    if (null != vocab.getContents()) {
                        for (String letter : vocab.getContents()) {
                            sb.append(letter).append("\n");
                        }
                    }
                    String content = sb.toString();
                    // 判断内容是否一致，注意这里可能会有分词操作，直接拿最终的数据进行比较
                    String existContent = FileIOUtils.readFile2String(vocabTxtPath);
                    if (existContent == null) existContent = "";
                    if (content.trim().equals(existContent.trim())) {
                        Log.d(TAG, "same content skip write");
                        return false;
                    }
                    writer = new FileWriter(vocabTxtPath);
                    if (vocab.getAction().equals(Vocab.ACTION_INSERT)) {
                        writer.write(content);
                    } else if (vocab.getAction().equals(Vocab.ACTION_CLEAR_AND_INSERT)) {
                        writer.write("");
                        writer.write(content);
                    } else {
                        writer.write("");
                    }
                    writer.flush();
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    CloseUtils.closeIO(writer);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

    public List<String> getVocab(String vocabName) {
        List<String> contents = new ArrayList<>();
        File file = new File(semanticConfig.getDUIResPath() + "/ldm/" + vocabName + ".txt");
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
