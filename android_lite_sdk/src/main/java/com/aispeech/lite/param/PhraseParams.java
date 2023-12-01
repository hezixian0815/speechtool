package com.aispeech.lite.param;

import java.util.Arrays;

public class PhraseParams {
    //槽位名称，目前只支持name（人名），location（地理位置），common（泛热词)
    private String name;
    //字符串列表。热词词语，个数不超过1500
    private String[] words;
    //整形。热词加强程度，可选值：[1,2,3]，数值越大，热词加强程度越高
    private int boost;

    public String getName() {
        return name;
    }

    /**
     * 槽位名称，目前只支持name（人名），location（地理位置），common（泛热词)
     */
    public void setName(String name) {
        this.name = name;
    }

    public String[] getWords() {
        return words;
    }

    /**
     * 字符串列表。热词词语，个数不超过1500
     * @param words 热词数组
     */
    public void setWords(String[] words) {
        this.words = words;
    }

    public int getBoost() {
        return boost;
    }
    /**
     * 整形。热词加强程度，可选值：[1,2,3]，数值越大，热词加强程度越高
     * @param boost
     */
    public void setBoost(int boost) {
        this.boost = boost;
    }

    @Override
    public String toString() {
        return "PhraseParams{" +
                "name='" + name + '\'' +
                ", words=" + Arrays.toString(words) +
                ", boost=" + boost +
                '}';
    }
}
