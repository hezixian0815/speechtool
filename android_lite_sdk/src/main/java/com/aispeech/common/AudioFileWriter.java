package com.aispeech.common;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;

public class AudioFileWriter {

    private File mFile = null;
    private FileOutputStream mStream = null;

    private FileWriter mFileWriter;
    private BufferedWriter mBufferedWriter;

    /**
     * 创建指定路径的音频输入流文件
     *
     * @param path 音频文件路径
     */
    public synchronized void createFile(String path) {
        mFile = new File(path);
        if (!mFile.exists()) {
            try {
                File parentDir = mFile.getParentFile();
                if (parentDir != null) {
                    if (!parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                }
                mFile.createNewFile();
                mStream = new FileOutputStream(mFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void createTextFile(String path) {
        mFile = new File(path);
        if (!mFile.exists()) {
            try {
                File parentDir = mFile.getParentFile();
                if (parentDir != null) {
                    if (!parentDir.exists()) {
                        parentDir.mkdirs();
                    }
                }
                mFile.createNewFile();
                mFileWriter = new FileWriter(mFile);
                mBufferedWriter = new BufferedWriter(mFileWriter);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 往文件中写入制定大小的音频字节流
     *
     * @param data 　数据
     * @param size 　数据大小
     */
    public synchronized void write(byte[] data, int size) {
        if (mStream != null) {
            try {
                mStream.write(data, 0, size);
                mStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void writeString(String data) {
        if (mBufferedWriter != null) {
            try {
                mBufferedWriter.append(data + "\r\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void writeBytesAsString(byte[] data, int size) {
        if (mBufferedWriter != null) {
            byte[] bytes = new byte[size];
            System.arraycopy(data, 0, bytes, 0, size);
            writeString(byteArrayToHexStr(bytes));
        }
    }

    public static String byteArrayToHexStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


    /**
     * 关闭音频流文件
     */
    public synchronized void close() {
        if (mStream != null) {
            try {
                mStream.flush();
                mStream.close();
                mStream = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (mBufferedWriter != null) {
            try {
                mBufferedWriter.close();
                mBufferedWriter = null;
                mFileWriter = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
