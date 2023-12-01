package com.aispeech.common;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.aispeech.lite.AISpeech;

import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Description: 保存重要的日志信息，如授权失败等 线程与IO线程共用
 * Author: junlong.huang
 * CreateTime: 2023/2/8
 */
public class DiskLogAdapter implements Log.ILogAdapter {

    private static final String TAG = "DiskLogAdapter";
    private static final SimpleDateFormat fileSDF = new SimpleDateFormat("yyyy-MM-dd", Locale.CHINA);
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String LOG_FILE_PREFIX = "saved_log_";
    private static final int MAX_SIZE = 1024 * 1024; // 每个文件最多1M 约1.5 - 2W 条日志 超过不再写入
    private static final int LOG_FILE_SAVE_DAYS = 7; // 保存7天的日志

    WriteLogHandler handler;

    public static DiskLogAdapter getInstance() {
        return Holder.diskLogAdapter;
    }

    private static class Holder {
        public static DiskLogAdapter diskLogAdapter = new DiskLogAdapter();
    }

    private DiskLogAdapter() {
        handler = new WriteLogHandler(FileSaveUtil.InnerThread.getInstance().getLooper());
    }


    @Override
    public void log(int priority, String tag, String message) {
        if (handler != null)
            handler.sendMessage(handler.obtainMessage(priority, new Object[]{tag, message}));
    }

    private static class WriteLogHandler extends Handler {

        File file;
        Date date = new Date();

        public WriteLogHandler(Looper looper) {
            super(looper);
            if (AISpeech.getContext() != null) initFile();
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (file == null && AISpeech.getContext() != null) {
                initFile();
            }

            if (file == null) return;

            FileWriter fileWriter = null;
            try {
                Object[] data = (Object[]) msg.obj;

                // timestamp
                date.setTime(System.currentTimeMillis());
                String timestamp = Log.sdf.format(date);
                StringBuilder stringBuilder = new StringBuilder(timestamp);

                // if date changed ,init file again
                if (!timestamp.startsWith(file.getName().replace(LOG_FILE_PREFIX, ""))) {
                    initFile();
                }

                // limit size
                if (file.exists() && file.length() >= MAX_SIZE) {
                    return;
                }

                // logLevel
                stringBuilder.append(" ");
                stringBuilder.append(logLevel(msg.what));

                // tag
                stringBuilder.append("/");
                stringBuilder.append(data[0]);

                // msg
                stringBuilder.append(": ");
                stringBuilder.append(data[1]);

                // new line
                stringBuilder.append(NEW_LINE);

                // write log
                fileWriter = new FileWriter(file, true);
                fileWriter.append(stringBuilder.toString());
                fileWriter.flush();

            } catch (Exception ignore) {
            } finally {

                CloseUtils.closeIO(fileWriter);
            }

        }

        private void initFile() {
            date.setTime(System.currentTimeMillis());
            file = new File(AISpeech.getContext().getFilesDir(), "log" + File.separator + LOG_FILE_PREFIX + fileSDF.format(date));
            FileUtils.createOrExistsFile(file);

            checkClearLog();
        }

        private void checkClearLog() {
            File dir = file.getParentFile();
            File[] files = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.startsWith("saved_log");
                }
            });

            if (files.length <= LOG_FILE_SAVE_DAYS) return;

            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File file, File file1) {

                    if (file.lastModified() > file1.lastModified()) {
                        return 1;
                    } else if (file.lastModified() < file1.lastModified()) {
                        return -1;
                    }

                    return 0;
                }
            });

            for (int i = 0; i < files.length - LOG_FILE_SAVE_DAYS; i++) {
                File file = files[i];
                if (FileUtils.deleteFile(file))
                    android.util.Log.d(TAG, "deleted log file:" + file.getName());
            }
        }
    }

    static String logLevel(int value) {
        switch (value) {
            case android.util.Log.VERBOSE:
                return "VERBOSE";
            case android.util.Log.DEBUG:
                return "DEBUG";
            case android.util.Log.INFO:
                return "INFO";
            case android.util.Log.WARN:
                return "WARN";
            case android.util.Log.ERROR:
                return "ERROR";
            case android.util.Log.ASSERT:
                return "ASSERT";
            default:
                return "UNKNOWN";
        }
    }

}
