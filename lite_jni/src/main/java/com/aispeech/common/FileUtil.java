package com.aispeech.common;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 文件IO操作类
 * Created by wuwei on 17-5-23.
 */

public class FileUtil {
    private static final String TAG = "FileUtil";
    private File mFile = null;
    private FileOutputStream mFileOutputStream = null;


    public FileUtil() {
    }

    public FileUtil(Context context) {
    }

    public static void copyFile(File src, File dest) {
        try (FileInputStream inStream = new FileInputStream(src);
             FileOutputStream outStream = new FileOutputStream(dest);
             FileChannel in = inStream.getChannel();
             FileChannel out = outStream.getChannel()) {
            in.transferTo(0, in.size(), out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 从assets目录中复制整个文件夹内容
     *
     * @param context Context 使用CopyFiles类的Activity
     * @param oldPath String  原文件路径  如：/aa
     * @param newPath String  复制后路径  如：xx:/bb/cc
     */
    public static void copyFilesFassets(Context context, String oldPath, String newPath) throws IOException {
        String[] fileNames = context.getAssets().list(oldPath);//获取assets目录下的所有文件及目录名
        if (fileNames.length > 0) {//如果是目录
            File file = new File(newPath);
            file.mkdirs();//如果文件夹不存在，则递归
            for (String fileName : fileNames) {
                copyFilesFassets(context, oldPath + "/" + fileName, newPath + "/" + fileName);
            }
        } else {//如果是文件
            try (InputStream is = context.getAssets().open(oldPath);
                 FileOutputStream fos = new FileOutputStream(new File(newPath))) {
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {//循环从输入流读取 buffer字节
                    fos.write(buffer, 0, byteCount);//将读取的输入流写入到输出流
                }
                fos.flush();//刷新缓冲区
            }
        }
    }

    public static boolean isWaveHeader(final byte[] data) {
        return (data.length == 44 && data[0] == 82 && data[1] == 73 && data[2] == 70 && data[3] == 70);
    }

    public synchronized void createFile(String filePath) {
        mFile = new File(filePath);
        File parentDir = mFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        if (!mFile.exists()) {//若不存在，则新建
            try {
                if (!mFile.createNewFile()) {
                    Log.e(TAG, "createFile: file created failed " + mFile.getPath());
                }
                mFileOutputStream = new FileOutputStream(mFile, true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 写文件到本地
     */
    public synchronized void write(byte[] retData) {
        try {
            if (mFileOutputStream != null) {
                mFileOutputStream.write(retData);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 关闭文件输出流
     */
    public synchronized void closeFile() {
        if (mFileOutputStream != null) {
            try {
                mFileOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    mFileOutputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mFileOutputStream = null;
            }
        }
    }

    /**
     * 创建文件夹
     *
     * @param dirPath 文件夹
     */
    public synchronized void createFileDir(String dirPath) {
        mFile = new File(dirPath);
        if (!mFile.exists()) {
            mFile.mkdirs();
        }
        mFile = null;
    }

    /**
     * 写 wav 头信息
     *
     * @param out            FileOutputStream 输出流
     * @param totalAudioLen  从08H开始到结尾的字节数
     * @param totalDataLen   data总数据长度字节数
     * @param longSampleRate 采样率 16000 8000
     * @param channels       通道数 单声道 1 双声道 2
     * @param audioEncoding  采样字节书 2即表示16bit
     * @param byteRate       波形数据传输速率（每秒平均字节数）
     * @throws IOException
     */
    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, int audioEncoding, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (channels * audioEncoding); // block align
        header[33] = 0;
        header[34] = (byte) (audioEncoding * 8); // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    /**
     * 依据录音机配置写入Wav头, 文件和数据大小都为0
     */
    public void writeWaveFileHeader() {
        if (mFileOutputStream == null)
            return;
        long sampleRate = 16000;
        int channelCount = 1;
        int audioEncoding = 2;
        try {
            writeWaveFileHeader(mFileOutputStream, 0,
                    0, sampleRate, channelCount, audioEncoding, channelCount * audioEncoding * sampleRate);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 修改wav头信息的文件大小和数据大小
     */
    public void modifyWaveFileHeaderFileLength() {
        if (mFile == null || !mFile.exists())
            return;
        try (RandomAccessFile mRandomAccessFile = new RandomAccessFile(mFile, "rw")) {
            int fLength = (int) mRandomAccessFile.length();
            if (fLength >= 44) {
                byte[] header = new byte[44];
                mRandomAccessFile.read(header);
                if (isWaveHeader(header)) {
                    mRandomAccessFile.seek(4); // riff chunk size
                    mRandomAccessFile.writeInt(Integer.reverseBytes(fLength - 8));
                    mRandomAccessFile.seek(40); // data chunk size
                    mRandomAccessFile.writeInt(Integer.reverseBytes(fLength - 44));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 限制文件大小
     *
     * @param path       文件路径
     * @param targetSize 限制文件总大小
     * @param targetSize 文件类型
     */
    public static void limitFileTotalSize(String path, int targetSize, String type) {
        Log.e(TAG, path + "  " + type);
        if (path == null) {
            return;
        }
        if (targetSize <= 0) {
            return;
        }
        try {
            File tempFile = new File(path);
            final File[] files = orderByDate(path);
            if (files == null || files.length == 0) {
                return;
            }

            int totalSize;
            for (File file : files) {
                totalSize = byteToMB(getTotalFileSize(tempFile));
                if (totalSize > targetSize) {
                    if (file.exists() && file.isFile()) {
                        file.delete();
                        Log.d(TAG, "deleteFile:" + file.getAbsolutePath());
                    }
                } else {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static File[] orderByDate(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }
        File[] fs = file.listFiles();
        Arrays.sort(fs, new Comparator<File>() {
            public int compare(File f1, File f2) {
                long diff = f1.lastModified() - f2.lastModified();
                if (diff > 0)
                    return 1;
                else if (diff == 0)
                    return 0;
                else
                    return -1;
            }

            public boolean equals(Object obj) {
                return true;
            }

        });
        return fs;
    }

    /**
     * 获取文件夹总大小
     *
     * @param path 文件夹路径
     * @return 文件夹总大小
     */
    public static long getTotalFileSize(File path) {
        long size = 0;
        try {
            final File[] files = path.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    size = size + getTotalFileSize(file);
                } else {
                    size = size + getFileSize(file);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return size;
    }

    /**
     * 获取文件大小
     *
     * @param file 文件名称
     * @return 文件大小
     */
    private static long getFileSize(File file) {
        return file != null && file.exists() ? file.length() : 0L;
    }

    /**
     * 长度转换
     *
     * @param size 文件大小
     * @return 文件大小，单位M
     */
    public static int byteToMB(long size) {
        long kb = 1000;
        long mb = kb * 1000;
        float f = (float) size / mb;
        return (int) f;
    }
}
