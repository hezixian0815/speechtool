package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.export.bean.BuildGrammarResult;
import com.aispeech.lite.speech.EngineListener;

import java.util.List;

/**
 * AILocalGrammarListener 接口用以接收 AILocalGrammarEngine 中发生的事件。
 * 关注和需要处理相关事件的类须实现该接口，当相关事件发生时，有关方法将会被回调。
 * 所有这些回调方法的触发都是在UI线程中执行的，请不要执行任何阻塞操作。
 */
public interface AILocalGrammarListener extends EngineListener {

    /**
     * 本地语法编译引擎初始化结束后执行，在主UI线程
     *
     * @param status {@link AIConstant#OPT_SUCCESS}:初始化成功；
     *               {@link AIConstant#OPT_FAILED}:初始化失败,
     */
    void onInit(int status);

    /**
     * 发生错误时执行，在主UI线程
     *
     * @param error 错误信息
     */
    void onError(AIError error);

    /**
     * 语法构建结束后执行，在主UI线程
     *
     * @param path 生成资源的绝对路径
     */
    void onBuildCompleted(String path);

    /**
     * 语法构建结束后执行，在主UI线程
     *
     * @param result 生成资源的绝对路径
     */
    void onBuildMultiCompleted(List<BuildGrammarResult> result);

}
