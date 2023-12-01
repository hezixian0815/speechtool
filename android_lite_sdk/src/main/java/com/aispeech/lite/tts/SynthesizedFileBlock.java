package com.aispeech.lite.tts;

import java.io.File;


/**
 * 类说明： 合成文件数据块,包含文件块对应的合成文本块,合成文件
 * 
 * @author Everett Li
 * @date Nov 10, 2014
 * @version 1.0
 */
public class SynthesizedFileBlock extends SynthesizedBlock {
    private File data;

    public SynthesizedFileBlock(String text, File data) {
        super(text);
        this.data = data;
    }

    @Override
    public void setData(Object data) {
        this.data = (File) data;
    }
    
    @Override
    public Object getData() {
        return data;
    }
}
