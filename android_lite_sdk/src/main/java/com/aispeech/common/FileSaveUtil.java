package com.aispeech.common;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.aispeech.util.Utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 文件保存工具类
 */
public class FileSaveUtil {
    public static final int TYPE_PCM_INPUT = 0;// 输入音频
    public static final int TYPE_PCM_OUTPUT = 1;// 输出
    public static final int TYPE_PCM_CUSTOM = 2;// 其他

    private static final String TAG = "FileSaveUtil";
    private String mFilePath;  // 文件路径 如 /aispeech/vad
    private String mModelName; // 所属模块名 hotWord-VadKernel

    private String mInFileName;
    private String mOutFileName;
    private String mCustomFileName;

    volatile IFileOperator customFileOperator;
    volatile IFileOperator inFileOperator;
    volatile IFileOperator outFileOperator;

    private final AtomicBoolean hasPrepare = new AtomicBoolean(false);
    private InnerSavingHandler innerSavingHandler;

    private static final int CODE_FILE_SAVING = 0x001;
    private static final int CODE_FILE_CLOSE = 0x002;
    private static final int CODE_FILE_FLUSH = 0x003;
    private static final int CODE_INIT = 0x004;

    public static final String FILE_TYPE_PCM = ".pcm";
    public static final String FILE_TYPE_TXT = ".txt";

    private final String fileType;

    public FileSaveUtil(String fileType) {
        this.fileType = fileType;
    }

    public FileSaveUtil() {
        this.fileType = FILE_TYPE_PCM;
    }

    // 判断文件夹是否存在，不存在在创建
    private boolean checkDirExist(String path) {
        return FileUtils.createOrExistsDir(path);
    }

    /**
     * @param path      路径 如 /aispeech/vad
     * @param modelName 所属模块名 hotWord-VadKernel
     */
    public void init(String path, String modelName) {
        mFilePath = path;
        mModelName = modelName;
        checkDirExist(path + File.separator + modelName);
    }

    public void init(String path) {
        init(path, "");
    }

    public String prepare() {
        return prepare("");
    }

    /**
     * 此方法会根据当前时间和名字、模块初始化输入、输出音频的文件名，不会创建文件
     * 在feed数据的时候，如果文件不存在会创建
     * @param name
     * @return
     */
    public String prepare(String name) {

        String fileSuffix = getFileSuffix(fileType);
        StringBuilder sb = new StringBuilder();

        // get parent dir
        sb.append(mFilePath).append(File.separator);
        if (!TextUtils.isEmpty(mModelName)) {
            sb.append(mModelName).append(File.separator);
        }

        // build file name
        if (TextUtils.isEmpty(name)) {
            mInFileName = String.format("%sin_%s", sb, fileSuffix);
            mOutFileName = String.format("%sout_%s", sb, fileSuffix);
            mCustomFileName = String.format("%s%s", sb, fileSuffix);
        } else {
            mInFileName = String.format("%sin_%s_%s", sb, name, fileSuffix);
            mOutFileName = String.format("%sout_%s_%s", sb, name, fileSuffix);
            mCustomFileName = String.format("%s%s_%s", sb, name, fileSuffix);
        }

        innerSavingHandler = new InnerSavingHandler(InnerThread.getInstance().mHandlerThread.getLooper());
        innerSavingHandler.sendEmptyMessage(CODE_INIT);
        hasPrepare.set(true);
        return mCustomFileName;
    }

    /**
     * 标记位输入音频文件，以in_开头
     *
     * @param data
     */
    public void feedTypeIn(byte[] data) {
        feedTypeIn(data, data.length);
    }

    public void feedTypeIn(byte[] data, int length) {
        feed(data, length, TYPE_PCM_INPUT);
    }

    /**
     * 标记位输出音频文件，以out_开头
     *
     * @param data
     */
    public void feedTypeOut(byte[] data) {
        feedTypeOut(data, data.length);
    }

    public void feedTypeOut(byte[] data, int length) {
        feed(data, length, TYPE_PCM_OUTPUT);
    }

    /**
     * 标记位其他类型的文件，无开头
     *
     * @param data
     */
    public void feedTypeCustom(byte[] data) {
        feedTypeCustom(data, data.length);
    }

    public void feedTypeCustom(byte[] pcm, int length) {
        feed(pcm, length, TYPE_PCM_CUSTOM);
    }

    /**
     * feed数据
     *
     * @param data
     * @param type
     */
    public void feed(final byte[] data, final int type) {
        feed(data, data.length, type);
    }

    public void feed(final byte[] data, int length, final int type) {

        if (!hasPrepare.get()) {
            return;
        }

        byte[] temp = new byte[length];
        System.arraycopy(data, 0, temp, 0, length);


        if (innerSavingHandler != null) {
            Message msg = Message.obtain();
            msg.obj = temp;
            msg.what = CODE_FILE_SAVING;
            msg.arg1 = type;
            innerSavingHandler.sendMessage(msg);
        }

    }

    /**
     * 将字节数组写入文件
     *
     * @param type  类型
     * @param bytes 字节数组
     * @return {@code true}: 写入成功<br>{@code false}: 写入失败
     */
    private boolean writeFile(int type, final byte[] bytes) {
        if (bytes == null) return false;

        try {
            IFileOperator fileOperator;
            if (type == TYPE_PCM_INPUT) {
                if (inFileOperator == null) inFileOperator = new BufferFileOperator(mInFileName);
                fileOperator = inFileOperator;
            } else if (type == TYPE_PCM_OUTPUT) {
                if (outFileOperator == null) outFileOperator = new BufferFileOperator(mOutFileName);
                fileOperator = outFileOperator;
            } else {
                if (customFileOperator == null)
                    customFileOperator = new BufferFileOperator(mCustomFileName);
                fileOperator = customFileOperator;
            }

            fileOperator.write(bytes);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void close() {
        hasPrepare.set(false);
        if (innerSavingHandler != null) {
            innerSavingHandler.sendEmptyMessage(CODE_FILE_CLOSE);
        }
    }

    public void flush() {
        if (innerSavingHandler != null) {
            innerSavingHandler.sendEmptyMessage(CODE_FILE_FLUSH);
        }
    }

    private void realFlush() {
        if (inFileOperator instanceof IFlushable) {
            ((IFlushable) inFileOperator).flush();
        }

        if (outFileOperator instanceof IFlushable) {
            ((IFlushable) outFileOperator).flush();
        }

        if (customFileOperator instanceof IFlushable) {
            ((IFlushable) customFileOperator).flush();
        }
    }

    private void realClose() {
        if (inFileOperator != null) {
            inFileOperator.close();
            inFileOperator = null;
        }
        if (outFileOperator != null) {
            outFileOperator.close();
            outFileOperator = null;
        }
        if (customFileOperator != null) {
            customFileOperator.close();
            customFileOperator = null;
        }
    }

    /**
     * 使用BufferedOutputStream的文件保存类，实测性能较高
     */
    static class BufferFileOperator implements IFileOperator, IFlushable {

        String name;
        boolean hasInit = false;
        BufferedOutputStream bos;

        public BufferFileOperator(String name) {
            this.name = name;
        }

        @Override
        public void init() {
            if (hasInit) return;

            try {
                File file = new File(name);
                if (!file.exists()) file.createNewFile();
                if (bos == null) bos = new BufferedOutputStream(new FileOutputStream(file, true));
                hasInit = true;
            } catch (Exception e) {
                Log.e(TAG, "init file error:" + e.getMessage());
            }
        }

        @Override
        public void write(byte[] bytes) {
            init();

            try {
                if (bos != null) {
                    bos.write(bytes);
                }
            } catch (Exception e) {
                Log.e(TAG, "write file error:" + e.getMessage());
            }
        }

        @Override
        public void close() {
            try {
                if (bos != null) {
                    Log.d(TAG, "close: " + name);
                    bos.flush();
                    CloseUtils.closeIO(bos);
                    bos = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "close with exception:" + e.getMessage());
            }
        }

        @Override
        public void flush() {
            try {
                if (bos != null) {
                    bos.flush();
                }
            } catch (Exception e) {
                Log.e(TAG, "flush with exception:" + e.getMessage());
            }
        }
    }

    /**
     * 文件保存操作类;无缓存，每次写入强刷
     */
    static class FileOperator implements IFileOperator {
        String name;
        FileChannel fileChannel;
        boolean hasInit = false;

        public FileOperator(String name) {
            this.name = name;
        }

        @Override
        public void init() {
            if (hasInit) return;

            try {
                File file = new File(name);
                if (!file.exists()) file.createNewFile();
                if (fileChannel == null)
                    fileChannel = new FileOutputStream(file, true).getChannel();
                hasInit = true;
            } catch (Exception e) {
                Log.e(TAG, "init file error:" + e.getMessage());
            }
        }

        @Override
        public void write(byte[] bytes) {
            init();

            try {
                if (fileChannel != null) {
                    fileChannel.position(fileChannel.size());
                    fileChannel.write(ByteBuffer.wrap(bytes));
                    fileChannel.force(true);
                }
            } catch (Exception e) {
                Log.e(TAG, "write file error:" + e.getMessage());
            }

        }

        @Override
        public void close() {
            try {
                if (fileChannel != null && fileChannel.isOpen()) {
                    Log.d(TAG, "close: " + name);
                    CloseUtils.closeIO(fileChannel);
                }
                fileChannel = null;
            } catch (Exception e) {
                Log.e(TAG, "close with exception:" + e.getMessage());
            }
        }

    }

    /**
     * 使用MappedByteBuffer文件保存类，实测速度可以，但是内存占用会耗费较多
     */

    static class MappedFileOperator implements IFileOperator {

        boolean hasInit = false;
        String name;
        FileChannel fileChannel;
        RandomAccessFile file;

        public MappedFileOperator(String name) {
            this.name = name;
        }

        @Override
        public void init() {
            if (hasInit) return;

            try {
                file = new RandomAccessFile(name, "rw");
                if (fileChannel == null) fileChannel = file.getChannel();
                hasInit = true;
            } catch (Exception e) {
                Log.e(TAG, "init file error:" + e.getMessage());
            }
        }

        @Override
        public void write(byte[] bytes) {
            init();
            try {
                if (fileChannel != null) {
                    MappedByteBuffer map = fileChannel.map(FileChannel.MapMode.READ_WRITE, fileChannel.size(), bytes.length);
                    map.put(bytes);
                }
            } catch (Exception e) {
                Log.e(TAG, "write with exception:" + e.getMessage());
            }
        }

        @Override
        public void close() {
            try {
                if (file != null) {
                    Log.d(TAG, "close: " + name);
                    file.close();
                    fileChannel = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "close with exception:" + e.getMessage());
            }
        }
    }

    interface IFileOperator {

        void init();

        void write(byte[] bytes);

        void close();

    }

    interface IFlushable {

        void flush();

    }

    private class InnerSavingHandler extends Handler {


        public InnerSavingHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            switch (msg.what) {
                case CODE_FILE_SAVING:
                    if (msg.obj instanceof byte[]) {
                        writeFile(msg.arg1, (byte[]) msg.obj);
                    }
                    break;
                case CODE_FILE_CLOSE:
                    realClose();
                    break;
                case CODE_FILE_FLUSH:
                    realFlush();
                    break;
                case CODE_INIT:
                    Utils.checkThreadAffinity();
                    break;
            }


        }
    }

    public static class InnerThread {


        // 内部服务线程
        private HandlerThread mHandlerThread;

        private static class Holder {
            public static InnerThread innerThread = new InnerThread();
        }

        public static InnerThread getInstance() {
            return Holder.innerThread;
        }

        private InnerThread() {
            initHandlerThread();
        }

        // 初始化内部服务线程
        private void initHandlerThread() {
            Log.d(TAG, "FileSaveUtil initHandlerThread...");
            mHandlerThread = new HandlerThread(ThreadNameUtil.getSimpleThreadName("io-savefile"));
            mHandlerThread.start();
        }

        public synchronized Looper getLooper() {
            if (mHandlerThread == null) {
                initHandlerThread();
            }

            return mHandlerThread.getLooper();
        }

        public void release() {
            Log.d(TAG, "FileSaveUtil release...");
            if (mHandlerThread != null) {
                mHandlerThread.quit();
            }
            mHandlerThread = null;
        }

    }

    public static String getFileSuffix(String fileType) {
        return new SimpleDateFormat("MM-dd_HH-mm-ss-SSS", Locale.CHINA).format(new Date()) + fileType;
    }
}
