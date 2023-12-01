package com.aispeech.export.config;


/**
 * ProjectName: duilite-for-car-android
 * Author: huwei
 * Describe:
 * Since 2021/2/5 14:20
 * Copyright(c) 2019 苏州思必驰信息科技有限公司  www.aispeech.com
 */

public class AILocalGrammarConfig {
    private String res;

    public String getRes() {
        return res;
    }

    public static final class Builder {
        private String res;

        /**
         * 设置语法编译所需的资源文件名，适用于资源放在assets目录的情况
         * 须在init之前设置才生效
         *
         * @param res 资源文件名
         * @return Builder
         */
        public Builder setRes(String res) {
            this.res = res;
            return this;
        }

        public AILocalGrammarConfig build() {
            AILocalGrammarConfig aILocalGrammarConfig = new AILocalGrammarConfig();
            aILocalGrammarConfig.res = this.res;
            return aILocalGrammarConfig;
        }
    }
}