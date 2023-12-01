package com.aispeech.export.engines;

import android.text.TextUtils;

import com.aispeech.export.config.AILocalGrammarConfig;
import com.aispeech.export.intent.AILocalGrammarIntent;
import com.aispeech.export.listeners.AILocalGrammarListener;
import com.aispeech.kernel.Gram;
import com.aispeech.lite.config.LocalGrammarConfig;
import com.aispeech.lite.param.LocalGrammarParams;


/**
 * @deprecated 废弃，参考{@link com.aispeech.export.engines2.AILocalGrammarEngine}
 */
@Deprecated
public class AILocalGrammarEngine {
    public static final String TAG = "AILocalGrammarEngine";

    private LocalGrammarParams mParam;
    private LocalGrammarConfig mConfig;

    private String resFileName = "";
    private String resPath = "";

    private com.aispeech.export.engines2.AILocalGrammarEngine aiLocalGrammarEngine2;

    private AILocalGrammarEngine() {
        aiLocalGrammarEngine2 = com.aispeech.export.engines2.AILocalGrammarEngine.createInstance();
        mParam = new LocalGrammarParams();
        mConfig = new LocalGrammarConfig();
    }


    public static boolean checkLibValid() {
        return Gram.isGramSoValid();
    }

    /**
     * 创建实例
     *
     * @return AILocalGrammarEngine实例
     */
    @Deprecated
    public static AILocalGrammarEngine createInstance() {
        return new AILocalGrammarEngine();
    }

    /**
     * 设置语法编译所需的资源文件名，适用于资源放在assets目录的情况
     * 须在init之前设置才生效
     *
     * @param resBin 资源文件名
     * @deprecated 已过时 改为在AILocalGrammarConfig中配置{@link #init(AILocalGrammarConfig, AILocalGrammarListener)}
     */
    @Deprecated
    public void setRes(String resBin) {
        this.resFileName = resBin;
    }

    /**
     * 设置语法编译所需的资源文件名全路径，适用于资源放在自定义目录下
     * 须在init之前设置才生效
     *
     * @param path 资源文件名全路径
     * @deprecated 已过时 改为在AILocalGrammarConfig中配置{@link #init(AILocalGrammarConfig, AILocalGrammarListener)}
     */
    @Deprecated
    public void setResPath(String path) {
        this.resPath = path;
    }


    /**
     * 初始化本地语法编译引擎
     *
     * @param listener 本地语法编译回调接口
     * @deprecated 已过时 {@link #init(AILocalGrammarConfig, AILocalGrammarListener)}
     */
    @Deprecated
    public void init(AILocalGrammarListener listener) {
        if (aiLocalGrammarEngine2 != null) {
            aiLocalGrammarEngine2.init(resPath, listener);
        }
    }

    /**
     * 初始化本地语法编译引擎
     *
     * @param config   配置实体类
     * @param listener 本地语法编译回调接口
     */
    @Deprecated
    public void init(AILocalGrammarConfig config, AILocalGrammarListener listener) {
        if (config == null) {
            throw new IllegalArgumentException("AILocalGrammarConfig should not be null");
        }
        if (aiLocalGrammarEngine2 != null) {
            aiLocalGrammarEngine2.init(config.getRes(), listener);
        }
    }

    /**
     * 设置编译语法后生成的本地识别所需要的资源的输出文件路径
     * 须在start之前设置才生效
     *
     * @param path 输出文件路径
     * @deprecated 已过时  在AILocalGrammarIntent中配置 {@link #buildGrammar(AILocalGrammarIntent, String)}
     */
    @Deprecated
    public void setOutputPath(String path) {
        mParam.setOutputPath(path);
    }

    /**
     * 构建语法，生成或更新本地识别需要的资源，如果资源文件不存在，则生成，如果存在，则覆盖
     *
     * @param grammarContent 语法内容
     * @deprecated 已过时  {@link #buildGrammar(AILocalGrammarIntent)}
     */
    @Deprecated
    public void buildGrammar(String grammarContent) {
        if (aiLocalGrammarEngine2 != null) {
            aiLocalGrammarEngine2.startBuild(grammarContent,mParam.getOutputPath());
        }
    }

    /**
     * 构建语法，生成或更新本地识别需要的资源，如果资源文件不存在，则生成，如果存在，则覆盖
     *
     * @param intent         动态配置项实体类
     * @param grammarContent 语法内容
     * @deprecated 已过时  {@link #buildGrammar(AILocalGrammarIntent)}
     */
    @Deprecated
    public void buildGrammar(AILocalGrammarIntent intent, String grammarContent) {
        intent.setContent(grammarContent);
        buildGrammar(intent);
    }

    /**
     * 构建语法，生成或更新本地识别需要的资源，如果资源文件不存在，则生成，如果存在，则覆盖
     *
     * @param intent 动态配置项实体类
     */
    @Deprecated
    public void buildGrammar(AILocalGrammarIntent intent) {
        parseIntent(intent);
        if (aiLocalGrammarEngine2 != null) {
            aiLocalGrammarEngine2.startBuild(mParam.getEbnf(),mParam.getOutputPath());
        }
    }

    private void parseIntent(AILocalGrammarIntent intent) {
        if (!TextUtils.isEmpty(intent.getOutputPath())) {
            mParam.setOutputPath(intent.getOutputPath());
        }
        if (!TextUtils.isEmpty(intent.getContent())) {
            mParam.setEbnf(intent.getContent());
        }
    }

    /**
     * 销毁本地语法编译引擎
     */
    public void destroy() {
        if (aiLocalGrammarEngine2 != null) {
            aiLocalGrammarEngine2.destroy();
        }
        if (mConfig != null) {
            mConfig = null;
        }
        if (mParam != null) {
            mParam = null;
        }

    }


}
