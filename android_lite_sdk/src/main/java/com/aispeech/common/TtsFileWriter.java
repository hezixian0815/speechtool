package com.aispeech.common;

public interface TtsFileWriter {

    /**
     * 如果文件未关闭就删除文件，主要用于TTS未正常写完数据时
     */
    void deleteIfOpened();

    String getAbsolutePath();

    void close();

    void write(byte[] data);
}
