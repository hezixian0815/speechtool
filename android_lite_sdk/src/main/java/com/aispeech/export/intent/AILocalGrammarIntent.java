package com.aispeech.export.intent;


import java.util.ArrayList;
import java.util.List;

/**
 * ProjectName: duilite-for-car-android
 * Author: huwei
 * Describe:
 * Since 2021/2/5 14:25
 * Copyright(c) 2019 苏州思必驰信息科技有限公司  www.aispeech.com
 */

public class AILocalGrammarIntent {
    private String outputPath;
    private String content;
    private boolean isBuildMulti = false;
    private final List<GrammarBean> grammarBeanList = new ArrayList<>();

    public String getOutputPath() {
        return outputPath;
    }

    /**
     * 设置编译语法后生成的本地识别所需要的资源的输出文件路径
     * 须在start之前设置才生效
     *
     * @param outputPath 输出文件路径
     */
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }

    public String getContent() {
        return content;
    }

    public boolean isBuildMulti() {
        return isBuildMulti;
    }

    public void setBuildMulti(boolean buildMulti) {
        isBuildMulti = buildMulti;
    }

    /**
     * 设置编译的 xbnf 内容
     *
     * @param content xbnf 语法内容
     */
    public void setContent(String content) {
        this.content = content;
    }

    public void addGrammarBean(GrammarBean bean) {
        grammarBeanList.add(bean);
    }

    public void setGrammarBeanList(List<GrammarBean> beanList) {
        grammarBeanList.clear();
        grammarBeanList.addAll(beanList);
    }

    public List<GrammarBean> getGrammarBeanList() {
        return grammarBeanList;
    }

    public static class GrammarBean {

        private String outputPath;
        private String content;

        public GrammarBean(String outputPath, String content) {
            this.outputPath = outputPath;
            this.content = content;
        }

        public String getOutputPath() {
            return outputPath;
        }

        /**
         * 设置编译语法后生成的本地识别所需要的资源的输出文件路径
         * 须在start之前设置才生效
         *
         * @param outputPath 输出文件路径
         */
        public void setOutputPath(String outputPath) {
            this.outputPath = outputPath;
        }

        public String getContent() {
            return content;
        }

        /**
         * 设置编译的 xbnf 内容
         *
         * @param content xbnf 语法内容
         */
        public void setContent(String content) {
            this.content = content;
        }
    }
}