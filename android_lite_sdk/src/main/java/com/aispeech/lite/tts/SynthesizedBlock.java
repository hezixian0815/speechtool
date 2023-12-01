package com.aispeech.lite.tts;


public abstract class SynthesizedBlock {
    private String mText;

    public SynthesizedBlock(String text) {
        this.mText = text;
    }

    public String getText() {
        return mText;
    }

    public void setText(String text) {
        this.mText = text;
    }

    public abstract void setData(Object data);

    public abstract Object getData();

}
