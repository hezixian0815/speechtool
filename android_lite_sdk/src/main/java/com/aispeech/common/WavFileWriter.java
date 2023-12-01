package com.aispeech.common;

import com.aispeech.lite.AISampleRate;
import com.aispeech.lite.audio.IAudioRecorder;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class WavFileWriter implements TtsFileWriter{

    private static final String TAG = WavFileWriter.class.getCanonicalName();

    private RandomAccessFile mRandomAccessFile = null;
    private File mWavFile = null;
    
    /**
     * 创建WavFileWriter,默认不追加,如果成功则返回实例，如果失败则返回null
     * 
     * @param file
     *            音频文件
     * @param sampleRate
     *            采样率
     * @param channelCount
     *            频道数
     * @param audioEncoding
     *            采样位宽
     * @return
     */
    public synchronized static WavFileWriter createWavFileWriter(File file,
            AISampleRate sampleRate, int channelCount, int audioEncoding) {
        return createWavFileWriter(file, sampleRate, channelCount, audioEncoding, false);
    }

    /**
     * 创建WavFileWriter，如果成功则返回实例，如果失败则返回null
     * 
     * @param file
     *            音频文件
     * @param sampleRate
     *            采样率
     * @param channelCount
     *            频道数
     * @param audioEncoding
     *            采样位宽
     * @param append
     *            是否使用追加的方式
     * @return
     */
    public synchronized static WavFileWriter createWavFileWriter(File file,
            AISampleRate sampleRate, int channelCount, int audioEncoding, boolean append) {
        Log.d(TAG, "create WavFileWriter.");
        if (file == null) {
            return null;
        }
        try {
            WavFileWriter wavFileWriter = new WavFileWriter(file, sampleRate, channelCount,
                    audioEncoding, append);
            return wavFileWriter;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 创建WavFileWriter, 默认不追加方式,如果成功则返回实例，如果失败则返回null
     * 
     * @param file
     *            wav文件对象
     * @throws IOException
     */
    public static WavFileWriter createWavFileWriter(File file,
            IAudioRecorder audioRecord) {
        return createWavFileWriter(file, audioRecord, false);
    }
    
    /**
     * 创建WavFileWriter, 如果成功则返回实例，如果失败则返回null
     * 
     * @param file
     *            wav文件对象
     * @param append
     *            是否使用追加的方式
     * @throws IOException
     */
    public synchronized static WavFileWriter createWavFileWriter(File file,
            IAudioRecorder audioRecord, boolean append) {
        if (file == null) {
            return null;
        }

        try {
            WavFileWriter wavFileWriter = new WavFileWriter(file, audioRecord, append);
            return wavFileWriter;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private WavFileWriter(File file, IAudioRecorder aiRecord, boolean append) throws IOException {
        AISampleRate sampleRate = aiRecord.getSampleRate();
        int channelCount = aiRecord.getAudioChannel();
        int audioEncoding = aiRecord.getAudioEncoding();
        create(file, sampleRate, channelCount, audioEncoding, append);
    }

    private WavFileWriter(File file, AISampleRate sampleRate, int channelCount, int audioEncoding,
            boolean append) throws IOException {
        create(file, sampleRate, channelCount, audioEncoding, append);
    }

    private void create(File file, AISampleRate sampleRate, int channelCount, int audioEncoding,
            boolean append) throws IOException {
        mWavFile = file;
        close();
        File parentDir = file.getParentFile();
        if (parentDir != null) {
            if (parentDir.exists()) {
                if (parentDir.isFile()) {
                    parentDir.delete();
                    parentDir.mkdirs();
                }
            } else {
                parentDir.mkdirs();
            }
        }
        boolean isFileExist = file.exists();
        
        if(!append && isFileExist && file.length() > 44) {
        	file.delete();
        }
        
        mRandomAccessFile = new RandomAccessFile(file, "rw");
        if (append) {
            if (isFileExist && file.length() > 44) {
                mRandomAccessFile.seek(file.length());
            } else {
                mRandomAccessFile.seek(0);
                writeWavHeader(sampleRate, channelCount, audioEncoding);
            }
        } else {
                writeWavHeader(sampleRate, channelCount, audioEncoding);
        }
    }

    /**
     * 依据录音机配置写入Wav头
     *
     * @throws IOException
     */
    private void writeWavHeader(AISampleRate sampleRate, int channelCount, int audioEncoding)
            throws IOException {
        Log.d(TAG, "writer header to Wav File.");
        int bytesPerSample = channelCount * audioEncoding;
        /* RIFF header */
        mRandomAccessFile.writeBytes("RIFF"); // riff id
        mRandomAccessFile.writeInt(0); // riff chunk size *PLACEHOLDER*
        mRandomAccessFile.writeBytes("WAVE"); // wave type
        mRandomAccessFile.writeBytes("fmt "); // fmt id
        mRandomAccessFile.writeInt(Integer.reverseBytes(0x10)); // 16 for PCM
                                                                // format
        mRandomAccessFile.writeShort(Short.reverseBytes((short) 1)); // 1 for
                                                                     // PCM
                                                                     // format
        mRandomAccessFile.writeShort(Short.reverseBytes((short) (channelCount))); // number
                                                                                  // of
                                                                                  // channel
        mRandomAccessFile.writeInt(Integer.reverseBytes(sampleRate.getValue())); // sampling
                                                                                 // frequency
        mRandomAccessFile.writeInt(Integer.reverseBytes(bytesPerSample * sampleRate.getValue())); // bytes
                                                                                                  // per
                                                                                                  // second
        mRandomAccessFile.writeShort(Short.reverseBytes((short) (bytesPerSample))); // bytes
                                                                                    // by
                                                                                    // capture
        mRandomAccessFile.writeShort(Short.reverseBytes((short) (audioEncoding * 8))); // bits
                                                                                       // per
                                                                                       // sample
        /* data chunk */
        mRandomAccessFile.writeBytes("data"); // data id
        mRandomAccessFile.writeInt(0); // data chunk size *PLACEHOLDER*
    }

    /**
     * 向wav文件中写入音频数据
     * 
     * @param data
     *            数据块
     */
    public void write(byte[] data) {
        // may run in thread
        // Log.d(TAG, "write data to Wav File.Data Length:"+data.length);
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
     * 关闭wav文件
     */
    public synchronized void close() {
        if (mRandomAccessFile != null) {
            Log.d(TAG, "close wav File.");
            try {
                int fLength = (int) mRandomAccessFile.length();
                mRandomAccessFile.seek(4); // riff chunk size
                mRandomAccessFile.writeInt(Integer.reverseBytes(fLength - 8));
                mRandomAccessFile.seek(40); // data chunk size
                mRandomAccessFile.writeInt(Integer.reverseBytes(fLength - 44));
            } catch (IOException e) {
                Log.e(TAG,
                        (e.getMessage() == null) ? "unknown exception in close" : e.getMessage());
            } finally {
                try {
                    mRandomAccessFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mRandomAccessFile = null;
            }
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
            if (mWavFile != null && mWavFile.exists()) {
                mWavFile.delete();
            }
        }
    }

    private boolean isClosed() {
        return (mRandomAccessFile == null);
    }

    public String getAbsolutePath(){
        return mWavFile == null ? null : mWavFile.getAbsolutePath();
    }

    /**
     * 去除wav头
     * 
     * @param data
     * @return 去头的wav数据
     */
    public static byte[] removeWaveHeader(final byte[] data) {
        if (data != null && data.length > 44) {
            if (data[0] == 82 && data[1] == 73 && data[2] == 70 && data[3] == 70) {
                byte[] noWavHeaderData = new byte[data.length - 44];
                System.arraycopy(data, 44, noWavHeaderData, 0, noWavHeaderData.length);
                Log.d(TAG, "remove wav header!");
                return noWavHeaderData;
            }
        }
        return data;
    }

}
