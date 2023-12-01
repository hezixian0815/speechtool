package com.aispeech.export.config;

import static com.aispeech.lite.SemanticType.BUILDIN;
import static com.aispeech.lite.SemanticType.DUI;
import static com.aispeech.lite.SemanticType.NAVI;

import android.text.TextUtils;

import com.aispeech.export.IAsrPolicy;
import com.aispeech.export.function.IConfig;
import com.aispeech.lite.Languages;
import com.aispeech.lite.SemanticType;
import com.aispeech.lite.base.BaseConfig;

/**
 * 离线语义引擎初始化参数
 *
 * @author hehr
 */
public class AILocalSemanticConfig extends BaseConfig implements IConfig {
    /**
     * 指定语义引擎类型
     */
    public SemanticType semanticType = NAVI;

    /**
     * 是否客户自定义feed 引擎
     */
    public boolean useCustomFeed;
    /**
     * 是否启用内置vad
     */
    public boolean useVad;

    /**
     * 设置内置vad资源路径或名称(assets路径下)
     */
    public String vadRes;

    /**
     * 离线导航语义资源文件夹
     */
    public String semNaviResFolder;

    /**
     * 设置离线语义lua资源文件夹
     */
    public String semNaviLuaResFolder;

    /**
     * 设置离线DUI资源文件夹
     */
    public String semDUIResFloder;

    /***
     * 设置离线DUI custom资源文件夹
     */
    public String semDUIResCustomFloder;

    /**
     * 设置离线DUI语义lua资源文件夹
     */
    public String semDUILuaResFloder;

    /**
     * 设置ngram离线通用识别资源
     */
    public String ngramBin;

    /**
     * 设置asr net.bin 资源，同 grammar 输出资源
     */
    public String netBin;

    /**
     * 设置语种
     */
    public Languages languages;

    /**
     * 纯语义模式
     */
    public boolean useRefText;

    /**
     * 语义格式  falseWTK格式 true DUI格式
     */
    public boolean useFormat;

    /**
     * nlu格式兼容：
     * 开启，返回、ngram、select等字段: {"grammar":{},"ngram":{},"select",{}}
     * 关闭，返回nlu字段: {"nlu":{}}
     * */
    public boolean enableNluFormatV2;

    /**
     * 外部设置识别仲裁策略
     */
    public IAsrPolicy asrPolicy;

    /**
     * 外部设置是否开启内置语义
     */
    public String builtInSemanticSkillID;

    //设置是否抛出空语义 true：抛出空语义不返回错误，false，返回识别为空的错误，不抛语义
    public boolean throwEmptySemantic;

    public boolean useSelectRule = false;
    public double selectRuleThreshold;

    /**
     * 内置语义资源文件夹路径
     */
    public String buildinResFolder;

    /**
     * 内置语义lua资源文件夹路径
     */
    public String buildinLuaResFolder;

    /**
     * product.cfg 文件夹路径
     */
    public String vocabsCfgFolder;

    /**
     * 离线内置语义与在线语义的映射表，如果外部配置，sdk内部会对齐在线skill名和补充离线skillId
     */
    public String skillMapping;

    private AILocalSemanticConfig(Builder builder) {
        this.useCustomFeed = builder.useCustomFeed;
        this.useVad = builder.useVad;
        this.vadRes = builder.vadRes;
        this.semNaviResFolder = builder.semNaviResFolder;
        this.semNaviLuaResFolder = builder.semNaviLuaResFolder;
        this.semDUIResFloder = builder.semDUIResFloder;
        this.semDUILuaResFloder = builder.semDUILuaResFloder;
        this.semDUIResCustomFloder = builder.semDUIResCustomFloder;
        this.ngramBin = builder.ngramBin;
        this.netBin = builder.netBin;
        this.languages = builder.languages;
        this.useRefText = builder.useRefText;
        this.useFormat = builder.useFormat;
        this.asrPolicy = builder.asrPolicy;
        this.semanticType = builder.semanticType;
        this.useSelectRule = builder.useSelectRule;
        this.selectRuleThreshold = builder.selectRuleThreshold;
        this.enableNluFormatV2 = builder.enableNluFormatV2;
        this.builtInSemanticSkillID = builder.builtInSemanticSkillID;
        this.throwEmptySemantic = builder.throwEmptySemantic;
        this.buildinResFolder = builder.buildinResFolder;
        this.buildinLuaResFolder = builder.buildinLuaResFolder;
        this.skillMapping = builder.skillMapping;
        this.vocabsCfgFolder = builder.vocabsCfgFolder;
    }

    public static class Builder extends BaseConfig.Builder {
        /**
         * 是否客户自定义feed 引擎
         */
        private boolean useCustomFeed = false;
        /**
         * 是否启用内置vad
         */
        private boolean useVad = true;

        /**
         * 设置内置vad资源路径或名称(assets路径下)
         */
        private String vadRes;

        /**
         * 离线导航语义资源文件夹
         */
        private String semNaviResFolder;

        /**
         * 设置离线导航语义lua资源文件夹
         */
        private String semNaviLuaResFolder;

        /**
         * 设置离线DUI资源文件夹
         */
        public String semDUIResFloder;

        /**
         * 设置离线DUI Custom资源文件夹
         */
        public String semDUIResCustomFloder;

        /**
         * 设置product.cfg 文件所在目录
         */
        public String vocabsCfgFolder;

        /**
         * 设置离线DUI语义lua资源文件夹
         */
        public String semDUILuaResFloder;

        /**
         * 设置ngram离线通用识别资源
         */
        private String ngramBin;

        /**
         * 设置 asr net.bin 资源，同 grammar 输出资源
         */
        private String netBin;

        /**
         * 设置语种
         */
        private Languages languages = Languages.CHINESE;

        /**
         * 纯语义模式，该模式下feed进入文本，输出语义
         */
        private boolean useRefText = true;

        /**
         * 离线语义归一化
         */
        public boolean useFormat = true;

        /**
         * 是否启用内部语义仲裁规则
         * */
        private boolean useSelectRule = false;
        public double selectRuleThreshold = 0.63d;

        /**
         * 外部设置识别仲裁策略
         */
        private IAsrPolicy asrPolicy;

        /**
         * 指定语义引擎类型
         * */
        private SemanticType semanticType = NAVI;

        /**
         * nlu格式兼容，后续版本将改为默认 true
         * 开启，返回、ngram、select等字段: {"grammar":{},"ngram":{},"select",{},"nlu":{}}
         * 关闭，返回nlu字段: {"nlu":{}}
         * */
        public boolean enableNluFormatV2 = false;

        /**
         * 是否抛出空语义，默认为false，返回语义为空的错误。true：不返回错误，抛出空语义
         */
        private boolean throwEmptySemantic;

        /**
         * 内置语义资源文件夹路径
         */
        private String buildinResFolder;

        /**
         * 内置语义lua资源文件夹路径
         */
        private String buildinLuaResFolder;

        /**
         * 离线内置语义与在线语义的映射表，如外部配置，sdk内部会对齐在线skill名和补充离线skillId
         */
        public String skillMapping;

        /**
         * 是否启用内部语义仲裁规则
         * @param useSelectRule boolean
         * @return {@link AILocalSemanticConfig}
         * */
        public Builder setUseSelectRule(boolean useSelectRule) {
            this.useSelectRule = useSelectRule;
            return this;
        }

        /**
         * 设置仲裁阈值
         * @param threshold 阈值
         * @return {@link AILocalSemanticConfig}
         * */
        public Builder setSelectRuleThreshold(double threshold){
            this.selectRuleThreshold = threshold;
            return this;
        }


        /**
         * 是否设置离线内置语义(自定义技能)优先
         */
        private String builtInSemanticSkillID;
        /**
         * 设置离线导航语义资源文件夹
         *
         * @param semNaviResFolder 语义资源
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setSemNaviResFolder(String semNaviResFolder) {
            this.semNaviResFolder = semNaviResFolder;
            return this;
        }

        /**
         * 设置语义lua资源文件夹
         *
         * @param semNaviLuaResFolder lua 语义资源
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setSemNaviLuaResFolder(String semNaviLuaResFolder) {
            this.semNaviLuaResFolder = semNaviLuaResFolder;
            return this;
        }

        /**
         * 设置离线导DUI语义资源文件夹
         *
         * @param semDUIResFloder 语义资源
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setSemDUIResFolder(String semDUIResFloder) {
            this.semDUIResFloder = semDUIResFloder;
            return this;
        }

        /**
         * 设置离线DUI custom资源目录文件夹
         *
         * @param semDUIResCustomFloder 语义资源
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setSemDUIResCustomFolder(String semDUIResCustomFloder) {
            this.semDUIResCustomFloder = semDUIResCustomFloder;
            return this;
        }

        /**
         * 设置product.cfg 文件所在目录
         *
         * @param vocabsCfgFolder 语义资源
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setVocabsCfgFolder(String vocabsCfgFolder) {
            this.vocabsCfgFolder = vocabsCfgFolder;
            return this;
        }

        /**
         * 设置语义资源文件夹
         *
         * @param semDUILuaResFloder dui语义资源
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setSemDUILuaResFolder(String semDUILuaResFloder){
            this.semDUILuaResFloder = semDUILuaResFloder;
            return this;
        }

        /**
         * 设置离线通用识别资源
         *
         * @param ngramBin ngram识别资源
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setNgramBin(String ngramBin) {
            this.ngramBin = ngramBin;
            return this;
        }

        /**
         * 设置本地net.bin 识别资源
         *
         * @param netBin net.bin 识别资源
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setNetBin(String netBin) {
            this.netBin = netBin;
            return this;
        }

        /**
         * 设置语种
         *
         * @param languages {@link Languages} 默认中文
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setLanguages(Languages languages) {
            this.languages = languages;
            return this;
        }

        /**
         * 是否启用离线语义纯语义模式，该模式下feed 文本，输出语义
         *
         * @param useRefText boolean ,默认 false
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setUseRefText(boolean useRefText) {
            this.useRefText = useRefText;
            return this;
        }

        public Builder setUseFormat(boolean useFormat){
            this.useFormat = useFormat;
            return  this;
        }

        /**
         * 设置识别仲裁策略
         * @param asrPolicy 识别决策策略
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setAsrPolicy(IAsrPolicy asrPolicy) {
            this.asrPolicy = asrPolicy;
            return this;
        }

        /**
         * 设置语义引擎类型
         * @param semanticType 语义类型
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setSemanticType(SemanticType semanticType) {
            this.semanticType = semanticType;
            return this;
        }

        public Builder setEnableNluFormatV2(boolean enableNluFormatV2) {
            this.enableNluFormatV2 = enableNluFormatV2;
            return this;
        }
        /**
         * 设置是否内置自定义技能优先
         * @param builtInSemanticSkillID 内置自定义技能ID
         * @return {@link AILocalSemanticConfig}
         */
        public Builder setBuiltInSemanticSkillID(String builtInSemanticSkillID){
            this.builtInSemanticSkillID = builtInSemanticSkillID;
            return  this;
        }

        /**
         * 设置是否抛出空语义
         * @param throwEmptySemantic true：抛出空语义不返回错误，false，返回识别为空的错误，不抛语义
         * @return {@link Builder}
         */
        public Builder setThrowEmptySemantic(boolean throwEmptySemantic) {
            this.throwEmptySemantic = throwEmptySemantic;
            return this;
        }

        /**
         * 设置根据dui语义生成的内置语义资源路径
         * @param buildinResFolder 资源路径
         * @return {@link Builder}
         */
        public Builder setSemBuildinResFolder(String buildinResFolder) {
            this.buildinResFolder = buildinResFolder;
            return this;
        }

        /**
         * 设置根据内置语义lua资源路径
         * @param buildinLuaResFolder 资源路径
         * @return {@link Builder}
         */
        public Builder setSemBuildinLuaResFolder(String buildinLuaResFolder) {
            this.buildinLuaResFolder = buildinLuaResFolder;
            return this;
        }

        /**
         * 设置离线内置语义的skill映射表
         *
         * @param skillMapping skill映射表
         * @return {@link Builder}
         */
        public Builder setSkillMapping(String skillMapping) {
            this.skillMapping = skillMapping;
            return this;
        }

        @Override
        public Builder setTagSuffix(String tagSuffix) {
            return (Builder) super.setTagSuffix(tagSuffix);
        }

        /**
         * 初始化配置检查
         */
        void check() {
            if (!this.useRefText) {//非纯语义模式下，需校验识别资源

                if (TextUtils.isEmpty(ngramBin))
                    throw new IllegalArgumentException("请设置ngram.bin识别资源");
            }

            if((semanticType.getType() & DUI.getType()) == DUI.getType()){
                if (TextUtils.isEmpty(semDUIResFloder))
                    throw new IllegalArgumentException("请设置离线DUI语义资源文件名路径");

                if (TextUtils.isEmpty(semDUILuaResFloder))
                    throw new IllegalArgumentException("请设置离线DUI语义内置lua资源文件路径");
            }
            if((semanticType.getType() & NAVI.getType()) == NAVI.getType()){
                if (TextUtils.isEmpty(semNaviResFolder))
                    throw new IllegalArgumentException("请设置离线导航语义资源文件名路径");

                if (TextUtils.isEmpty(semNaviLuaResFolder))
                    throw new IllegalArgumentException("请设置离线导航语义内置lua资源文件路径");
            }
            if((semanticType.getType() & BUILDIN.getType()) == BUILDIN.getType()){
                if (TextUtils.isEmpty(buildinResFolder))
                    throw new IllegalArgumentException("请设置离线内置语义资源文件名路径");

                if (TextUtils.isEmpty(buildinLuaResFolder))
                    throw new IllegalArgumentException("请设置离线内置语义内置lua资源文件路径");
            }
        }


        /**
         * 构建 AILocalSemanticConfig
         *
         * @return {@link AILocalSemanticConfig}
         */
        public AILocalSemanticConfig build() {
            check();
            return super.build(new AILocalSemanticConfig(this));
        }
    }

}
