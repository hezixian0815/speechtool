package com.aispeech.export.engines2;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.bean.BuildGrammarResult;
import com.aispeech.export.intent.AILocalGrammarIntent;
import com.aispeech.export.intent.AILocalMultiGrammarIntent;
import com.aispeech.export.listeners.AILocalGrammarListener;
import com.aispeech.kernel.Gram;
import com.aispeech.lite.base.BaseEngine;
import com.aispeech.lite.config.LocalGrammarConfig;
import com.aispeech.lite.grammar.LocalGrammarProcessor;
import com.aispeech.lite.param.LocalGrammarParams;
import com.aispeech.lite.speech.SpeechListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地语法编译
 */
public class AILocalGrammarEngine extends BaseEngine {

    private LocalGrammarProcessor mLocalGrammarProcessor;
    private LocalGrammarParams mParam;
    private LocalGrammarConfig mConfig;
    private SpeechListenerImpl mListenerImpl;

    private AILocalGrammarEngine() {
        mLocalGrammarProcessor = new LocalGrammarProcessor();
        mLocalGrammarProcessor.setUseSingleMessageProcess(false); // 编译耗时较长 使用单独线程处理 不阻塞共用线程
        mParam = new LocalGrammarParams();
        mConfig = new LocalGrammarConfig();
        mListenerImpl = new SpeechListenerImpl(null);
        mBaseProcessor = mLocalGrammarProcessor;
    }

    @Override
    public String getTag() {
        return "local_grammar";
    }


    private static boolean checkLibValid() {
        return Gram.isGramSoValid();
    }

    /**
     * 创建实例
     *
     * @return AILocalGrammarEngine实例
     */
    public static AILocalGrammarEngine createInstance() {
        return new AILocalGrammarEngine();
    }

    /**
     * 初始化本地语法编译引擎
     * <p> 语法编译所需的资源 </p>
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param grammarResource 资源文件
     * @param listener        回调接口
     */
    public void init(String grammarResource, AILocalGrammarListener listener) {
        if (!checkLibValid()) {
            if (listener != null) {
                listener.onInit(AIConstant.OPT_FAILED);
                listener.onError(new AIError(AIError.ERR_SO_INVALID, AIError.ERR_DESCRIPTION_SO_INVALID));
            }
            Log.e(TAG, "so动态库加载失败 !");
            return;
        }
        super.init();
        // grammarResource
        if (TextUtils.isEmpty(grammarResource)) {
            Log.e(TAG, "grammarResource not found !!");
        } else if (grammarResource.startsWith("/")) {
            mConfig.setResBinPath(grammarResource);
        } else {
            mConfig.setAssetsResNames(new String[]{grammarResource});
            mConfig.setResBinPath(Util.getResourceDir(mConfig.getContext()) + File.separator + grammarResource);
        }

        mListenerImpl.setListener(listener);
        mLocalGrammarProcessor.init(mListenerImpl, mConfig);
    }

    /**
     * 构建语法，生成或更新本地识别需要的资源，如果资源文件不存在，则生成，如果存在，则覆盖
     *
     * @param grammarContent 语法内容, 语法内容生成的过程请参考 demo
     * @param outputPath     编译语法后生成的本地识别所需要的资源的输出文件路径
     * @deprecated 已过时  {@link #buildGrammar(AILocalGrammarIntent)}
     */
    public void startBuild(String grammarContent, String outputPath) {
        AILocalGrammarIntent grammarIntent = new AILocalGrammarIntent();
        grammarIntent.setOutputPath(outputPath);
        grammarIntent.setContent(grammarContent);
        buildGrammar(grammarIntent);
    }

    /**
     * 构建语法，生成或更新本地识别需要的资源，如果资源文件不存在，则生成，如果存在，则覆盖
     *
     * @param intent 动态配置项实体类
     */
    public void buildGrammar(AILocalGrammarIntent intent) {
        if (mLocalGrammarProcessor == null) return;
        Log.i(TAG, "buildGrammar:" + intent.isBuildMulti());
        if (intent.isBuildMulti()) {
            mLocalGrammarProcessor.startMulti(parseMultiGrammarIntent(intent));
        } else {
            parseIntent(intent);
            mLocalGrammarProcessor.start(mParam);
        }
    }

    public void buildGrammars(AILocalMultiGrammarIntent intent) {
        buildGrammar(intent);
    }

    private LocalGrammarParams[] parseMultiGrammarIntent(AILocalGrammarIntent intent) {
        List<AILocalMultiGrammarIntent.GrammarBean> grammarBeanList = intent.getGrammarBeanList();
        LocalGrammarParams[] results = new LocalGrammarParams[grammarBeanList.size()];

        for (int i = 0; i < grammarBeanList.size(); i++) {
            AILocalMultiGrammarIntent.GrammarBean bean = grammarBeanList.get(i);
            LocalGrammarParams params = new LocalGrammarParams();
            params.setOutputPath(bean.getOutputPath());
            params.setEbnf(bean.getContent());
            results[i] = params;
        }

        return results;
    }

    private void parseIntent(AILocalGrammarIntent intent) {
        if (!TextUtils.isEmpty(intent.getOutputPath())) {
            if (mParam != null) mParam.setOutputPath(intent.getOutputPath());
        }
        if (!TextUtils.isEmpty(intent.getContent())) {
            if (mParam != null) mParam.setEbnf(intent.getContent());
        }
    }

    /**
     * 销毁本地语法编译引擎
     */
    public void destroy() {
        if (mLocalGrammarProcessor != null) {
            mLocalGrammarProcessor.release();
        }

        if (mListenerImpl != null)
            mListenerImpl.setListener(null);
    }

    private class SpeechListenerImpl extends SpeechListener {
        AILocalGrammarListener mListener;

        public SpeechListenerImpl(AILocalGrammarListener listener) {
            mListener = listener;
        }

        public void setListener(AILocalGrammarListener listener) {
            mListener = listener;
        }

        @Override
        public void onInit(int status) {
            if (mListener != null) {
                mListener.onInit(status);
            }
        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(error);
            }
        }

        @Override
        public void onResults(AIResult result) {
            //do nothing
        }

        @Override
        public void onReadyForSpeech() {
            //do nothing
        }

        @Override
        public void onBeginningOfSpeech() {
            //do nothing
        }

        @Override
        public void onRmsChanged(float rmsdB) {
            //do nothing
        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {
            //do nothing
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {
            //do nothing
        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] data, int size) {
            //do nothing
        }


        @Override
        public void onEndOfSpeech() {
            //do nothing
        }

        /**
         * @deprecated 废弃
         */
        @Deprecated
        @Override
        public void onRecorderStopped() {
            //do nothing
        }

        @Override
        public void onNotOneShot() {
            //do nothing
        }

        @Override
        public void onBuildCompleted(AIResult result) {
            Log.i(TAG, "build grammar success,resultObject:" + result.getResultObject());
            if (result.getResultObject() instanceof List) {
                List<BuildGrammarResult> results = transformResult(result);
                if (mListener != null) mListener.onBuildMultiCompleted(results);
            } else if (result.getResultObject() instanceof LocalGrammarParams) {
                LocalGrammarParams params = (LocalGrammarParams) result.getResultObject();
                if (mListener != null) mListener.onBuildCompleted(params.getOutputPath());
            }
        }
    }

    private List<BuildGrammarResult> transformResult(AIResult aiResult) {
        ArrayList<BuildGrammarResult> results = new ArrayList<>();

        try {
            List<LocalGrammarParams> paramsList = (List<LocalGrammarParams>) aiResult.getResultObject();
            for (LocalGrammarParams localGrammarParams : paramsList) {
                results.add(new BuildGrammarResult(
                        localGrammarParams.getOutputPath(),
                        localGrammarParams.isBuildSuccess(),
                        localGrammarParams.getEbnf(),
                        localGrammarParams.getAIError()
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }


        return results;
    }

}
