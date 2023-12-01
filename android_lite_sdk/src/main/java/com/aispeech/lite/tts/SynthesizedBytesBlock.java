package com.aispeech.lite.tts;


public class SynthesizedBytesBlock extends SynthesizedBlock {
    private byte[] data;

    public SynthesizedBytesBlock(String text, byte[] data) {
        super(text);
        this.data = data;
    }

    @Override
    public void setData(Object data) {
        this.data = (byte[]) data;
    }
    
    @Override
    public Object getData() {
        return data;
    }
}
