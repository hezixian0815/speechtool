package com.aispeech.common;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class AIFileWriter implements TtsFileWriter {

    private static final String TAG = AIFileWriter.class.getCanonicalName();

    private RandomAccessFile mRandomAccessFile = null;
    private File file = null;
    private String filePath = null;

    private AIFileWriter(File file) throws IOException {
        close();
        filePath = file.getAbsolutePath();
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            if (parentDir.exists()) {
                if (parentDir.isFile()) {
                    if (!parentDir.delete()) {
                        Log.e(TAG, "AIFileWriter: file delete failed");
                    }
                    parentDir.mkdirs();
                }
            } else {
                parentDir.mkdirs();
            }
            if (file.exists())
                file.delete();

            mRandomAccessFile = new RandomAccessFile(file, "rw");
        }
    }

    /**
     * 创建WavFileWriter，如果成功则返回实例，如果失败则返回null
     *
     * @param file 待写入文件
     * @return
     */
    public synchronized static AIFileWriter createFileWriter(File file) {
        Log.d(TAG, "create FileWriter.");
        if (file == null) {
            return null;
        }
        try {
            AIFileWriter fileWriter = new AIFileWriter(file);
            return fileWriter;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 向wav文件中写入音频数据
     *
     * @param data 数据块
     */
    public void write(byte[] data) {
        // may run in thread
        if (mRandomAccessFile != null) {
            try {
                mRandomAccessFile.write(data, 0, data.length);
            } catch (IOException e) {
                Log.e(TAG,
                        (e.getMessage() == null) ? "unknown exception in write" : e
                                .getMessage());
                close();
            }
        }
    }

    /**
     * 关闭文件
     */
    public synchronized void close() {
        if (mRandomAccessFile != null) {
            Log.d(TAG, "close File.");
            try {
                mRandomAccessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mRandomAccessFile = null;
        }
    }

    /**
     * 如果文件没有关闭，关闭并删除
     */
    public synchronized void deleteIfOpened() {
        if (!isClosed()) {
            try {
                mRandomAccessFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (file != null && file.exists()) {
                file.delete();
            }
        }
    }

    @Override
    public String getAbsolutePath() {
        return filePath;
    }

    private boolean isClosed() {
        return (mRandomAccessFile == null);
    }

}
