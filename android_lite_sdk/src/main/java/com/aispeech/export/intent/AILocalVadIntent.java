package com.aispeech.export.intent;

import com.aispeech.lite.base.BaseIntent;

public class AILocalVadIntent extends BaseIntent {
    private int pauseTime = 300;
    private boolean isVadEnable = true;

    public int getPauseTime() {
        return pauseTime;
    }

    public void setPauseTime(int pauseTime) {
        this.pauseTime = pauseTime;
    }

    public boolean isVadEnable() {
        return isVadEnable;
    }

    public void setVadEnable(boolean vadEnable) {
        isVadEnable = vadEnable;
    }

    @Override
    public String toString() {
        return "AILocalVadIntent{" +
                "pauseTime=" + pauseTime +
                ", isVadEnable=" + isVadEnable +
                '}';
    }
}
