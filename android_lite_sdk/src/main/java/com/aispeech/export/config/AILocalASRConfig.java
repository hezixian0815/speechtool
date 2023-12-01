package com.aispeech.export.config;

import com.aispeech.export.function.IConfig;
import com.aispeech.lite.Languages;
import com.aispeech.lite.base.BaseConfig;

public class AILocalASRConfig extends BaseConfig implements IConfig {
    public int vadPauseTime = 300;
    /**
     * 语言
     */
    private Languages languages;

    /**
     * 设置是否启用vad，default is true
     */
    private boolean vadEnable = true;

    /**
     * 是否使用双VAD
     */
    private boolean enableDoubleVad;

    /**
     * 本地vad资源
     */
    private String vadResource;
    /**
     * 声学资源
     */
    private String acousticResources;
    /**
     * 网络资源
     */
    private String netbinResource;

    private boolean useItn;
    private String itnLuaResFolderName;
    private String numBinPath;
    private boolean itnUpperCase = true;

    /**
     * 集内匹配
     */
    private boolean useAggregateMate;//是否启用集内匹配
    private String aggregateMateCommandBinPath;//集内匹配资源路径,command.bin
    private String aggregateMateBinPath;//集内匹配资源路径 , 示例:res/itn/res_v1.1.4/command/cmd.bin

    /**
     * ngram编译资源
     */
    private String ngramSlotRes;

    /**
     * 设置是否启用vad，默认为true
     *
     * @param vadEnable true:使用Vad；false:禁止Vad
     */
    public void setVadEnable(boolean vadEnable) {
        this.vadEnable = vadEnable;
    }

    /**
     * 设置网络资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param netbinResource netbin 资源
     */
    public void setNetbinResource(String netbinResource) {
        this.netbinResource = netbinResource;
    }

    /**
     * 设置声学资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/ebnfr.aicar.1.3.0.bin</p>
     * <p>2. 如在 assets 里设置为名称，如：ebnfr.aicar.1.3.0.bin</p>
     *
     * @param acousticResources 声学资源
     */
    public void setAcousticResources(String acousticResources) {
        this.acousticResources = acousticResources;
    }


    /**
     * 设置本地vad资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param vadResource vad资源
     */
    public void setVadResource(String vadResource) {
        this.vadResource = vadResource;
    }

    /**
     * 设置ngram编译需要的资源名，可设置 assets 下的相对路径 或 / 开头的绝对路径
     * 须在init之前设置才生效
     * @param ngramSlotRes ngramSlot 资源
     */
    public void setNgramSlotRes(String ngramSlotRes) {
        this.ngramSlotRes = ngramSlotRes;
    }

    public boolean isVadEnable() {
        return vadEnable;
    }
    /**
     * 设置离线识别引擎是否使用双VAD，使用双VAD 需要 feed 2通道音频，而
     * 使用非双VAD 则 feed 1 通道的音频。
     *
     * @param enableDoubleVad true 使用双VAD , false 非双VAD
     */
    public void setEnableDoubleVad(boolean enableDoubleVad) {
        this.enableDoubleVad = enableDoubleVad;
    }


    /**
     * 设置是否开启英文首字母转换大写，默认为true 转换 please call 911  -->  Please call 911
     *
     * @param itnUpperCase
     * @return Builder
     */
    public void setItnUpperCase(boolean itnUpperCase) {
        this.itnUpperCase = itnUpperCase;
    }

    public boolean isItnUpperCase() {
        return itnUpperCase;
    }

    public boolean isEnableDoubleVad() {
        return enableDoubleVad;
    }

    public String getVadResource() {
        return vadResource;
    }

    public int getVadPauseTime() {
        return vadPauseTime;
    }

    public String getAcousticResources() {
        return acousticResources;
    }

    public String getNetbinResource() {
        return netbinResource;
    }

    public String getNgramSlotRes() {
        return ngramSlotRes;
    }

    public Languages getLanguages() {
        return languages;
    }

    public void setLanguages(Languages languages) {
        this.languages = languages;
    }

    public boolean isUseItn() {
        return useItn;
    }

    public String getItnLuaResFolderName() {
        return itnLuaResFolderName;
    }

    public void setItnLuaResFolderName(String itnLuaResFolderName) {
        this.itnLuaResFolderName = itnLuaResFolderName;
    }

    public String getNumBinPath() {
        return numBinPath;
    }

    public boolean isUseAggregateMate() {
        return useAggregateMate;
    }

    public String getAggregateMateCommandBinPath() {
        return aggregateMateCommandBinPath;
    }

    public String getAggregateMateBinPath() {
        return aggregateMateBinPath;
    }

    public void setUseAggregateMate(boolean useAggregateMate) {
        this.useAggregateMate = useAggregateMate;
    }

    public void setAggregateMateCommandBinPath(String aggregateMateCommandBinPath) {
        this.aggregateMateCommandBinPath = aggregateMateCommandBinPath;
    }

    public void setAggregateMateBinPath(String aggregateMateBinPath) {
        this.aggregateMateBinPath = aggregateMateBinPath;
    }

    public static final class Builder extends BaseConfig.Builder {
        /**
         * ngram编译资源
         */
        private String ngramSlotRes;
        /**
         * 语言
         */
        private Languages languages;

        /**
         * 设置是否启用vad，default is true
         */
        private boolean vadEnable = true;

        private boolean enableDoubleVad;
        /**
         * 本地vad资源
         */
        private String vadResource;
        public int vadPauseTime = 300;
        /**
         * 声学资源
         */
        private String acousticResources;
        /**
         * 网络资源
         */
        private String netbinResource;

        private boolean useItn;
        private String itnLuaResFolderName;
        private String numBinPath;
        private boolean itnUpperCase = true;

        private boolean useAggregateMate;//是否启用集内匹配
        private String aggregateMateCommandBinPath;//集内匹配资源路径,command.bin
        private String aggregateMateBinPath;//集内匹配资源路径 , 示例:res/itn/res_v1.1.4/command/cmd.bin

        /**
         * 设置是否启用vad，默认为true
         *
         * @param vadEnable true:使用Vad；false:禁止Vad
         */
        public Builder setVadEnable(boolean vadEnable) {
            this.vadEnable = vadEnable;
            return this;
        }

        /**
         * 设置网络资源
         * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
         * <p>2. 如在 assets 里设置为名称</p>
         *
         * @param netbinResource netbin 资源
         */
        public Builder setNetbinResource(String netbinResource) {
            this.netbinResource = netbinResource;
            return this;
        }

        /**
         * 设置声学资源
         * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/ebnfr.aicar.1.3.0.bin</p>
         * <p>2. 如在 assets 里设置为名称，如：ebnfr.aicar.1.3.0.bin</p>
         *
         * @param acousticResources 声学资源
         */
        public Builder setAcousticResources(String acousticResources) {
            this.acousticResources = acousticResources;
            return this;
        }

        /**
         * 设置VAD资源名，可设置 assets 下的相对路径 或 / 开头的绝对路径
         * 须在init之前设置才生效
         *
         * @param vadRes vadName
         * @return Builder
         */
        public Builder setVadRes(String vadRes) {
            this.vadResource = vadRes;
            return this;
        }

        /**
         * 设置VAD右边界
         *
         * @param vadPauseTime pauseTime 单位：ms,默认300
         * @return Builder
         */
        public Builder setVadPauseTime(int vadPauseTime) {
            this.vadPauseTime = vadPauseTime;
            return this;
        }

        /**
         * 设置是否开启英文首字母转换大写，默认为true 转换 please call 911  -->  Please call 911
         *
         * @param itnUpperCase
         * @return Builder
         */
        public Builder setItnUpperCase(boolean itnUpperCase) {
            this.itnUpperCase = itnUpperCase;
            return this;
        }

        /**
         * 设置网络资源，可设置 assets 下的相对路径 或 / 开头的绝对路径
         * 须在init之前设置才生效
         *
         * @param netBin 资源名
         * @return Builder
         */
        public Builder setNetBin(String netBin) {
            this.netbinResource = netBin;
            return this;
        }

        /**
         * 设置声学资源，可设置 assets 下的相对路径 或 / 开头的绝对路径
         * 须在init之前设置才生效, 适用于声学资源放在assets目录下
         *
         * @param resBin 资源名
         * @return Builder
         */
        public Builder setResBin(String resBin) {
            this.acousticResources = resBin;
            return this;
        }

        /**
         * 设置本地vad资源
         * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
         * <p>2. 如在 assets 里设置为名称</p>
         *
         * @param vadResource vad资源
         */
        public Builder setVadResource(String vadResource) {
            this.vadResource = vadResource;
            return this;
        }

        /**
         * 设置ngram编译需要的资源名，可设置 assets 下的相对路径 或 / 开头的绝对路径
         * 须在init之前设置才生效
         *
         * @param slotRes ngramSlot 资源
         * @return Builder
         */
        public Builder setNgramSlotRes(String slotRes) {
            this.ngramSlotRes = slotRes;
            return this;
        }

        /**
         * 开启Itn功能 对ASR识别后的文字进行功能转化
         * @return Builder
         */
        public Builder setUseItn(boolean useItn) {
            this.useItn = useItn;
            return this;
        }

        /**
         * 设置Itn numLex 资源目录路径
         * 可设置 assets 下的相对路径 或 / 开头的绝对路径
         *  @return Builder
         */
        public Builder setItnLuaResFolderName(String itnLuaResFolderName) {
            this.itnLuaResFolderName = itnLuaResFolderName;
            return this;
        }

        /**
         * 设置Itn NumBin 文件路径，传入后将支持文本转数字功能
         * 可设置 assets 下的相对路径 或 / 开头的绝对路径
         *
         * @param numBinPath itn 资源路径
         * @return Builder
         */
        public Builder setNumBinPath(String numBinPath) {
            this.numBinPath = numBinPath;
            return this;
        }

        /**
         * 设置语言类型
         *
         * @param languages 语言
         * @return Builder
         */
        public Builder setLanguages(Languages languages) {
            this.languages = languages;
            return this;
        }

        /**
         * 开启集内匹配功能
         * @param useAggregateMate
         */
        public Builder setUseAggregateMate(boolean useAggregateMate) {
            this.useAggregateMate = useAggregateMate;
            return this;
        }

        /**
         * 设置集内匹配 command.bin资源目录路径
         * 可设置 assets 下的相对路径 或 / 开头的绝对路径
         */
        public Builder setAggregateMateCommandBinPath(String aggregateMateCommandBinPath) {
            this.aggregateMateCommandBinPath = aggregateMateCommandBinPath;
            return this;
        }

        /**
         * 设置集内匹配 cmd.bin资源目录路径
         * 可设置 assets 下的相对路径 或 / 开头的绝对路径
         *
         * @param aggregateMateBinPath
         */
        public Builder setAggregateMateBinPath(String aggregateMateBinPath) {
            this.aggregateMateBinPath = aggregateMateBinPath;
            return this;
        }

        /**
         * 设置离线识别引擎是否使用双VAD，使用双VAD 需要 feed 2通道音频，而
         * 使用非双VAD 则 feed 1 通道的音频。
         *
         * @param enableDoubleVad true 使用双VAD , false 非双VAD
         * @return Builder
         */
        public Builder setEnableDoubleVad(boolean enableDoubleVad) {
            this.enableDoubleVad = enableDoubleVad;
            return this;
        }

        @Override
        public Builder setTagSuffix(String tagSuffix) {
            return (Builder) super.setTagSuffix(tagSuffix);
        }

        public AILocalASRConfig build() {
            AILocalASRConfig aiLocalASRConfig = new AILocalASRConfig();
            aiLocalASRConfig.vadEnable = this.vadEnable;
            aiLocalASRConfig.vadResource = this.vadResource;
            aiLocalASRConfig.acousticResources = this.acousticResources;
            aiLocalASRConfig.netbinResource = this.netbinResource;
            aiLocalASRConfig.languages = languages;
            aiLocalASRConfig.vadPauseTime = this.vadPauseTime;
            aiLocalASRConfig.ngramSlotRes = this.ngramSlotRes;
            aiLocalASRConfig.useItn = this.useItn;
            aiLocalASRConfig.itnLuaResFolderName = this.itnLuaResFolderName;
            aiLocalASRConfig.numBinPath = this.numBinPath;
            aiLocalASRConfig.useAggregateMate = this.useAggregateMate;
            aiLocalASRConfig.aggregateMateCommandBinPath = this.aggregateMateCommandBinPath;
            aiLocalASRConfig.aggregateMateBinPath = this.aggregateMateBinPath;
            aiLocalASRConfig.enableDoubleVad = this.enableDoubleVad;
            aiLocalASRConfig.itnUpperCase = this.itnUpperCase;
            return super.build(aiLocalASRConfig);
        }


    }

    @Override
    public String toString() {
        return "AILocalASRConfig{" +
                "netBin='" + netbinResource + '\'' +
                ", resBin='" + acousticResources + '\'' +
                ", vadEnable=" + vadEnable +
                ", vadRes='" + vadResource + '\'' +
                ", vadPauseTime=" + vadPauseTime +
                ", languages=" + languages +
                ", useItn=" + useItn +
                ", itnLuaResFolderName='" + itnLuaResFolderName + '\'' +
                ", numBinPath='" + numBinPath + '\'' +
                ", ngramSlotRes='" + ngramSlotRes + '\'' +
                ", itnUpperCase='" + itnUpperCase + '\'' +
                ", useAggregateMate='" + useAggregateMate + '\'' +
                ", aggregateMateCommandBinPath='" + aggregateMateCommandBinPath + '\'' +
                ", aggregateMateBinPath='" + aggregateMateBinPath + '\'' +
                '}';
    }
}
