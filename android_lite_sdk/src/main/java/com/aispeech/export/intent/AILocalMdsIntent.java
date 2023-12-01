package com.aispeech.export.intent;

import com.aispeech.lite.base.BaseIntent;

public class AILocalMdsIntent extends BaseIntent {

    /**
     * 说话人角度（多通道输入时使用），可选，根据资源配置而定
     */
    private int doa = -1;

    public int getDoa() {
        return doa;
    }

    /**
     * 说话人角度（多通道输入时使用），可选，根据资源配置而定
     *
     * @param doa 说话人角度
     */
    public void setDoa(int doa) {
        this.doa = doa;
    }

    @Override
    public String toString() {
        return "AILocalMdsIntent{" +
                "useCustomFeed=" + useCustomFeed +
                ", doa=" + doa +
                '}';
    }
}
