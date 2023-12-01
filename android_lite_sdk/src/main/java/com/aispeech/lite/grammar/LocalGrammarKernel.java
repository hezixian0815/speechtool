package com.aispeech.lite.grammar;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.export.Vocab;
import com.aispeech.kernel.Gram;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.LocalGrammarConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.LocalGrammarParams;
import com.aispeech.lite.param.SpeechParams;

import java.util.ArrayList;
import java.util.List;

/**
 * gram 编译 xbnf 生成 net.bin
 *
 * @author aispeech
 */
public class LocalGrammarKernel extends BaseKernel {
    private static final String TAG = "LocalGrammarKernel";
    private LocalGrammarListener mListener;
    private LocalGrammarConfig grammarConfig;
    private Gram mEngine;
    private volatile boolean isCanceled = false;
    private volatile boolean isMultiple = false;
    private List<List<LocalGrammarParams>> multGrammerParamsList;
    private List<Integer> multGrammerParamsIndexList;

    public LocalGrammarKernel(LocalGrammarListener listener) {
        super(TAG, listener);
        mListener = new LocalGrammarListenerPoxy(listener);
        multGrammerParamsList = new ArrayList<>();
        multGrammerParamsIndexList = new ArrayList<>();
    }


    @Override
    public void run() {
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    mEngine = new Gram();
                    grammarConfig = (LocalGrammarConfig) message.mObject;
                    int status = AIConstant.OPT_FAILED;
                    if (grammarConfig != null) {
                        String configStr = grammarConfig.toJson().toString();
                        Log.d(TAG, "config: " + configStr);
                        long id = mEngine.init(configStr);
                        if (id == 0) {
                            Log.e(TAG, "引擎初始化失败,请检查资源文件是否在指定路径下！");
                            status = AIConstant.OPT_FAILED;
                        } else {
                            Log.d(TAG, "引擎初始化成功");
                            status = AIConstant.OPT_SUCCESS;
                        }
                    }
                    mListener.onInit(status);
                    break;
                case Message.MSG_START:
                    isCanceled = false;
                    LocalGrammarParams params = (LocalGrammarParams) message.mObject;
                    String paramStr = params.toJson().toString();
                    Log.d(TAG, "params: " + paramStr);
                    if (mEngine != null) {
                        int ret = mEngine.start(paramStr);
                        Log.d(TAG, "ret = " + ret);
                        if (ret == AIConstant.OPT_SUCCESS) {
                            params.setBuildSuccess(true);//设置编译状态，后续需要根据状态决定是否组装json
                            if (mListener != null) {
                                AIResult aiResult = new AIResult();
                                aiResult.resultObject = params;
                                mListener.onBuildCompleted(aiResult);
                            }
                        } else {
                            AIError aiError = new AIError(AIError.ERR_GRAMMAR_FAILED, AIError.ERR_DESCRIPTION_ERR_GRAMMAR_FAILED);
                            params.setBuildSuccess(false);//设置编译状态，后续需要根据状态决定是否组装json
                            if (mListener != null) {
                                mListener.onError(aiError);
                            }
                        }
                    }
                    break;
                case Message.MSG_CANCEL:
                    multGrammerParamsList.clear();
                    multGrammerParamsIndexList.clear();
                    break;
                case Message.MSG_EVENT:
                    List<LocalGrammarParams> paramsList = (List<LocalGrammarParams>) message.mObject;
                    multGrammerParamsList.add(paramsList);
                    multGrammerParamsIndexList.add(paramsList.size());
                    break;
                case Message.MSG_RELEASE:
                    if (mEngine != null) {
                        mEngine.destroy();
                        mEngine = null;
                    }
                    isReleased = true;
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
    public void startKernel(SpeechParams param) {
        Log.i(TAG, "startKernel");
        isMultiple = false;
        super.startKernel(param);
    }

    public void startKernel(List<LocalGrammarParams> params) {
        if (params != null) {
            Log.i(TAG, "startKernel,multiple param size : " + params.size());
            isMultiple = true;
            //发送到Handler对List集合做同步处理，params保存在List集合中
            sendMessage(new Message(Message.MSG_EVENT, params));
            for (LocalGrammarParams param : params) {
                sendMessage(new Message(Message.MSG_START, param));
            }
        }
    }

    @Override
    public void cancelKernel() {
        isCanceled = true;
        Log.d(TAG, "reset cancel flag");
        clearMessage();
        super.cancelKernel();
    }

    private void saveErrorSafety(AIError error, int index, List<LocalGrammarParams> paramsList) {
        try {
            LocalGrammarParams params = paramsList.get(paramsList.size() - index);
            params.setAIError(error);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //处理多个xbnf编译的返回结果
    class LocalGrammarListenerPoxy implements LocalGrammarListener {

        private LocalGrammarListener listener;

        public LocalGrammarListenerPoxy(LocalGrammarListener listener) {
            this.listener = listener;
        }

        @Override
        public void onInit(int status) {
            listener.onInit(status);
        }

        @Override
        public void onError(AIError error) {
            try {
                if (!isMultiple) {
                    listener.onError(error);
                } else {
                    if (multGrammerParamsIndexList.size() > 0) {
                        int index = multGrammerParamsIndexList.get(0);
                        multGrammerParamsIndexList.set(0, index - 1);
                        Log.w(TAG, "LocalGrammarListenerPoxy onError, index = " + index);
                        if (isCanceled) {//kernel取消状态不返回结果
                            Log.w(TAG, "canceled gram engine, ignore return result");
                            return;
                        }
                        List<LocalGrammarParams> paramsList = multGrammerParamsList.get(0);
                        saveErrorSafety(error, index, paramsList);
                        if (paramsList != null && index <= 1) {
                            AIResult aiResult = new AIResult();
                            aiResult.resultObject = paramsList;
                            listener.onBuildCompleted(aiResult);
                            multGrammerParamsList.remove(0);
                            multGrammerParamsIndexList.remove(0);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onBuildCompleted(AIResult aiResult) {
            try {
                if (!isMultiple) {
                    listener.onBuildCompleted(aiResult);
                } else {
                    /**
                     * startKernel一次发送多个的gram编译，需要List判断当前startKernel的所有gram编译是否已经完成。
                     * 一次startKernel只回调一次onBuildCompleted()，回调时携带List，包含了所有gram编译的结果。
                     */
                    if (multGrammerParamsIndexList.size() > 0) {
                        int index = multGrammerParamsIndexList.get(0);
                        multGrammerParamsIndexList.set(0, index - 1);
                        Log.w(TAG, "LocalGrammarListenerPoxy onBuildCompleted, index = " + index);
                        if (isCanceled) {//kernel取消状态不返回结果
                            Log.w(TAG, "canceled gram engine, ignore return result");
                            return;
                        }
                        List<LocalGrammarParams> paramsList = multGrammerParamsList.get(0);
                        if (paramsList != null && index <= 1) {
                            aiResult.resultObject = paramsList;
                            listener.onBuildCompleted(aiResult);
                            multGrammerParamsList.remove(0);
                            multGrammerParamsIndexList.remove(0);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 生成 gram 启动参数，含 net.bin 生成路径和 ebnf
     *
     * @param localGrammarParams gram 启动参数
     * @param vocab              词库
     */
    public LocalGrammarParams update(LocalGrammarParams localGrammarParams, Vocab vocab) {
        if (TextUtils.isEmpty(localGrammarParams.getOutputPath())) {
            throw new IllegalArgumentException("请先设置 net.bin 路径");
        }
        localGrammarParams.setOutputPath(localGrammarParams.getOutputPath());
        StringBuilder strBuilder = new StringBuilder();
        for (String letter : vocab.getContents()) {
            strBuilder.append(letter + "|");
        }
        String content = strBuilder.substring(0, strBuilder.length() - 1);
        //更新 xbnf 中词槽占位 key，如联系人：#CONTACTS#、#SINGERS#、#SONGS#
        String ebnf = localGrammarParams.getEbnf();
        ebnf = ebnf.replaceAll("#" + vocab.getName() + "#", "(" + content + ")");
        localGrammarParams.setEbnf(ebnf);
        return localGrammarParams;
    }

}
