/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.common;

import android.content.Context;
import android.text.TextUtils;

import org.json.JSONObject;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ai.dui.sdk.log2.LogConfig;

/**
 * Log输出类
 */
public class Log extends Constant {
    public static final String ERROR_TAG = "AISpeech Error";
    public static final String WARNING_TAG = "AISpeech Warning";
    private static final String TAG = "DUILite";

    private static final Map<String, Integer> logPrintFrequency = new ConcurrentHashMap<>();
    private static final int LOG_FREQUENCY_DEBUG_DISCOUNT = 100;
    private static final int LOG_FREQUENCY_VERBOSE_DISCOUNT = 200;

    private static int defaultLogLevel = W;
    public static int LOG_NATIVE_LEVEL = android.util.Log.WARN;
    private static int logLevel = defaultLogLevel;
    private static Date datetime = new Date();
    private static String logFilePath;
    private static boolean enableLog;
    private static CustomLog customLog;

    // 宇龙酷派可以输入*20121220#打开日志级别选择开关
    // private static boolean isCoolpad =
    // "YuLong".equalsIgnoreCase(Build.MANUFACTURER)
    private static boolean isCoolpad = false;

    //  close file when exit
    // 不调用init方法，系统中的Log.x()将什么也不做

    protected static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static ILogAdapter diskLogAdapter;

    /**
     * 设置日志写入实现类，此功能仅写入重要日志
     * {@link Log#write(String, String)}
     *
     * @param diskLogAdapter
     */
    public static void setDiskLogAdapter(ILogAdapter diskLogAdapter) {
        Log.diskLogAdapter = diskLogAdapter;
    }

    /**
     * @param tag
     * @param msg
     * @hide
     */
    public static void v(String tag, String msg) {
        if (logLevel > V) {
            return;
        }
        if (!TextUtils.isEmpty(msg)) {
            if (customLog != null) {
                customLog.v(tag, msg);
            } else {
                ai.dui.sdk.log2.Log.v(tag, msg);
            }
        }
    }

    /**
     * @param tag
     * @param msg
     * @hide
     */
    public static void d(String tag, String msg) {
        if (logLevel > D) {
            return;
        }

        if (!TextUtils.isEmpty(msg)) {
            if (customLog != null) {
                customLog.d(tag, msg);
            } else {
                ai.dui.sdk.log2.Log.d(tag, msg);
            }
        }
    }

    /**
     * @param tag
     * @param msg
     * @hide
     */
    public static void i(String tag, String msg) {
        if (logLevel > I) {
            return;
        }

        if (!TextUtils.isEmpty(msg)) {
            if (customLog != null) {
                customLog.i(tag, msg);
            } else {
                ai.dui.sdk.log2.Log.i(tag, msg);
            }
        }
    }

    /**
     * @param tag
     * @param msg
     * @hide
     */
    public static void w(String tag, String msg) {
        if (logLevel > W) {
            return;
        }

        if (!TextUtils.isEmpty(msg)) {
            if (customLog != null) {
                customLog.w(tag, msg);
            } else {
                ai.dui.sdk.log2.Log.w(tag, msg);
            }
        }
    }

    /**
     * @param tag
     * @param msg
     * @hide
     */
    public static void e(String tag, String msg) {
        if (logLevel > E) {
            return;
        }

        if (!TextUtils.isEmpty(msg)) {
            if (customLog != null) {
                customLog.e(tag, msg);
            } else {
                ai.dui.sdk.log2.Log.e(tag, msg);
            }
        }
    }

    /**
     * @param tag
     * @param msg
     * @hide
     */
    public static void f(String tag, String msg) {
        if (logLevel > F) {
            return;
        }

        if (!TextUtils.isEmpty(msg)) {
            if (customLog != null) {
                customLog.f(tag, msg);
            } else {
                ai.dui.sdk.log2.Log.f(tag, msg);
            }
        }
    }
    /**
     * 打印统计耗时
     *
     * @param from  起点
     * @param to    终点
     * @param begin 起点时间戳，单位ms
     * @param end   终点时间戳，单位ms
     */
    public static void cost(String from, String to, long begin, long end) {
        if (logLevel > android.util.Log.ASSERT) {
            return;
        }

        if (end > 0 && end >= begin) {
            String msg = from + " -> " + to + " : " + (end - begin) + "ms";
            if (end - begin > 2000) {
                ai.dui.sdk.log2.Log.e("AI-lite-sdk-cost", msg);
            } else if (end - begin > 500) {
                ai.dui.sdk.log2.Log.w("AI-lite-sdk-cost", msg);
            } else {
                ai.dui.sdk.log2.Log.i("AI-lite-sdk-cost", msg);
            }
        }
    }

    public static void cost(String from, String to, CostCallback callback, long ignoreMs) {
        long startTime = System.currentTimeMillis();
        if (callback != null) {
            callback.doIt();
        }
        long stopTime = System.currentTimeMillis();
        if (ignoreMs > 0) {
            cost(from, to, startTime, stopTime, ignoreMs);
        } else {
            cost(from, to, startTime, stopTime);
        }
    }


    public static void cost(String from, String to, long begin, long end, long ignoreMs) {
        if (end - begin < ignoreMs) {
            return;
        }
        cost(from, to, begin, end);
    }


    /**
     * 打印 sdk 边界输出日志
     *
     * @param tag tag
     * @param msg log内容
     */
    public static void out(String tag, String msg) {
        if (logLevel > android.util.Log.ASSERT) {
            return;
        }

        if (!TextUtils.isEmpty(msg)) {
            ai.dui.sdk.log2.Log.i(tag, " -> " + msg);
        }
    }

    public static void cost(String from, String to, CostCallback callback) {
        long startTime = System.currentTimeMillis();
        if (callback != null) {
            callback.doIt();
        }
        long stopTime = System.currentTimeMillis();
        cost(from, to, startTime, stopTime);
    }

    public interface CostCallback {
        void doIt();
    }

    private static void println(int level, String tag, String msg) {
        // Because CoolPad close the log output which level is below WARN!
        if (isCoolpad && level < W) {
            level = W;
        }
        android.util.Log.println(level, tag, msg);
    }

    /**
     * 打印 sdk 边界输入日志
     *
     * @param tag tag
     * @param msg log内容
     */
    public static void in(String tag, String msg) {
        if (logLevel > android.util.Log.ASSERT) {
            return;
        }

        if (!TextUtils.isEmpty(msg)) {
            ai.dui.sdk.log2.Log.i(tag, " <- " + msg);
        }
    }

    /**
     * 打印日志 D 级别
     *
     * @param logId     日志id，表示这个日志是在哪里打印的。日志id 不可重复
     * @param frequency 打印日志频率,10表示每10次打印一次log，1 表示每次都打印，小于1时会按1频率打印日志
     * @param tag       日志标签
     * @param msgs      日志内容，Object数组只有在能打印日志时才会拼接
     */
    private static void df(String logId, int frequency, final String tag, final Object... msgs) {
        if (logLevel > android.util.Log.DEBUG) return;
        boolean printLog;
        if (frequency <= 1) {
            printLog = true;
        } else {
            Integer times = logPrintFrequency.get(logId);
            if (times == null) times = 0;

            times++;
            printLog = times % frequency == 0;
            if (printLog) times = 0;

            logPrintFrequency.put(logId, times);
        }
        if (printLog) {
            final String msg = getMsgsString(msgs);
            d(tag, msg);
        }
    }

    public static void df(int frequency, final String tag, final Object... msgs) {
        df(getLogId(2), frequency, tag, msgs);
    }

    public static void df(final String tag, String msg) {
        df(getLogId(2), LOG_FREQUENCY_DEBUG_DISCOUNT, tag, msg);
    }

    /**
     * 间隔打印日志 V 级别日志到 D 级别
     *
     * @param logId     日志id，表示这个日志是在哪里打印的。日志id 不可重复
     * @param frequency 打印日志频率,10表示每10次打印一次log，1 表示每次都打印，小于1时会按1频率打印日志
     * @param tag       日志标签
     * @param msgs      日志内容，Object数组只有在能打印日志时才会拼接
     */
    public static void vfd(String logId, int frequency, String tag, String msgs) {
        v(tag, msgs);
        df(logId, frequency, tag, msgs);
    }

    public static void vfd(String logId, String tag, String msgs) {
        v(tag, msgs);
        df(logId, LOG_FREQUENCY_DEBUG_DISCOUNT, tag, msgs);
    }

    public static void vfd(String tag, String msgs) {
        vfd(getLogId(2), tag, msgs);
    }

    public static void dfAudio(final String tag, String msg) {
        df(getLogId(2), 100, tag, msg);
    }

    private static String getMsgsString(final Object[] msgs) {
        try {
            if (msgs == null) {
                return "";
            }
            if (msgs.length == 1) {
                return msgs[0].toString();
            }
            StringBuilder sb = new StringBuilder();
            for (Object s : msgs) {
                if (s != null) {
                    sb.append(" ").append(s);
                }
            }
            return sb.toString();
        } catch (Exception e) {
        }
        return "";
    }


    /**
     * 直接根据调用栈获取logid
     *
     * @param stackIndex 栈深度
     * @return
     */
    public static String getLogId(int stackIndex) {
        if (logLevel > android.util.Log.DEBUG) return "default";
        try {
            StringBuilder sb = new StringBuilder();
            StackTraceElement[] stacks = new Throwable().getStackTrace();
            sb.append(stacks[stackIndex].getClassName()).append("_").append(stacks[stackIndex].getLineNumber());
            return sb.toString();
        } catch (Exception e) {
        }
        return "default";
    }

    public static String getLogId() {
        return getLogId(2);
    }


    private static synchronized void write(String level, String tag, String msg) {
        FileIOUtils.writeFileFromString(logFilePath, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(datetime) + ": " + level + "/" + tag + ": " + msg + "\n", true);
    }

    /**
     * 用来保存一些重要的日志
     * 与XLOG无关，保存为单独的路径，默认仅保留最近7天，文件大小限制为1M(约1W-2W条日志）
     * 常规日志保存、上传请配置日志保存路径
     *
     * @param tag
     * @param msg
     */
    public static void write(String tag, String msg) {
        write(android.util.Log.ERROR, tag, msg);
    }

    public static void write(int level, String tag, String msg) {
        if (diskLogAdapter == null) return;

        diskLogAdapter.log(level, tag, msg);

        switch (level) {
            case android.util.Log.VERBOSE:
                v(tag, msg);
                break;
            case android.util.Log.DEBUG:
                d(tag, msg);
                break;
            case android.util.Log.INFO:
                i(tag, msg);
                break;
            case android.util.Log.WARN:
                w(tag, msg);
                break;
            case android.util.Log.ERROR:
                e(tag, msg);
                break;
            case android.util.Log.ASSERT:
                f(tag, msg);
                break;
        }
    }

    /**
     * gets current log level
     *
     * @return int value, {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     */
    public static int getLogLevel() {
        return logLevel;
    }

    /**
     * sets log level
     *
     * @param logLevel {V = 2 , D =3 , I = 4 , W = 5 , E = 6, F = 7, NONE = 8}
     */
    static void setLogLevel(int logLevel) {
        if (Log.logLevel == logLevel) {
            return;
        }
        if (logLevel > NONE) {
            ai.dui.sdk.log2.Log.e(TAG, "Set log level failed . wrong log level");
        }
        w(TAG, "log.level " + logLevel + "->" + logLevel);
        ai.dui.sdk.log2.Log.setConsoleLogLevel(logLevel - 2);
        w(TAG, "log.level " + logLevel + "->" + logLevel);
        Log.logLevel = logLevel;
    }

    /**
     * sets log maxLength 设置日志一行最多打印字符数
     *
     * @param maxLength 最多字符数
     */
    static void setMaxLength(int maxLength) {
        w(TAG, "log.maxLength ->" + maxLength);
        ai.dui.sdk.log2.Log.setMaxLength(maxLength);
    }

    public static String getLogFilePath() {
        return logFilePath;
    }

    static void setLogFilePath(String logFilePath) {
        if (TextUtils.equals(Log.logFilePath, logFilePath)) {
            return;
        }
        Log.logFilePath = logFilePath;
    }

    /**
     * @param logLevel    {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     * @param logFilePath 日志保存绝对路径
     * @deprecated
     */
    public static void setDebugMode(int logLevel, String logFilePath) {
        setLogLevel(logLevel);
        //setLogFilePath(logFilePath);
    }

    /**
     * @param logLevel {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     */
    public static void setDebugMode(int logLevel) {
        setLogLevel(logLevel);
    }

    /**
     * @param logLevel  {V = 2 , D =3 , I = 4 , W = 5 , E = 6, A = 7}
     * @param maxLength 设置输出的日志一行最多显示多少字符，超过之后，自动换行
     */
    public static void setDebugMode(int logLevel, int maxLength) {
        setLogLevel(logLevel);
        setMaxLength(maxLength);
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface TagPrefix {
        // 引擎 对外接口层
        String ENGINE = "AI-lite-engine_";

        // processor 处理层
        String PROCESSOR = "AI-lite-processor_";

        // kernel 内核层
        String KERNEL = "AI-lite-kernel_";

    }

    public static void openLog(Context context,
                               String logFilePath,
                               int cachedDays,
                               String deviceId,
                               String apiKey,
                               String apiSecret,
                               boolean salvageEnabled, int maxLength) {
        if (customLog != null) {
            return;
        }
        LogConfig config = new LogConfig.Builder(context)
                .setSalvageEnabled(salvageEnabled)
                .setDeviceId(deviceId)
                .setApiKey(apiKey)
                .setApiSecret(apiSecret)
                .setFileLogDir(logFilePath)
                .setCacheDays(cachedDays)
                .setMaxLength(maxLength)
                .setFileLogLevel(ai.dui.sdk.log2.Log.LEVEL_VERBOSE)
                .build();
        ai.dui.sdk.log2.Log.init(context, config);
        enableLog = true;
        setDebugMode(Log.D);
    }

    public interface ILogAdapter {
        void log(int priority, String tag, String message);
    }

    /**
     * 示例
     * {
     * "prof": {
     * "enable": 1,
     * "output": "",
     * "level": 1
     * }
     * }
     * 外部设置以SDK为准，内核日志级别
     * VERBOSE  1    Int
     * DEBUG  2    Int
     * INFO  3    Int
     * WARNING  4    Int
     * ERROR  5    Int
     *
     * @return 拼接后的json
     */
    public static JSONObject parseDuiliteLog() {
        JSONObject profObject = new JSONObject();
        try {
            if (getLogLevel() >= A) {
                profObject.put("enable", 0);
            } else {
                profObject.put("enable", enableLog ? 1 : 0);
                profObject.put("level", getLogLevel() - 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return profObject;
    }

    public static void setCustomLog(CustomLog customLog) {
        Log.customLog = customLog;
        setLogLevel(getLogLevel());
        enableLog = true;
        Log.d(TAG, "setCustomLog:enableLog true loglevel " + getLogLevel());
    }


    @Retention(RetentionPolicy.SOURCE)
    public @interface TagCostTime {
        String WAKEUP_LAST_WORD = "wakeup.last.word";
        String WAKEUP_CALLBACK = "wakeup.callback";
        String DOA_RESULT = "doa.result";
        String VAD_BEGIN = "vad.begin";
        String VAD_END = "vad.end";
        String DM_OUTPUT = "dm.output";
        String ASR_START = "asr.start";
        String ASR_STOP = "asr.stop";
        String ASR_FIRST = "asr.first";
        String ASR_RESULT = "asr.result";
        String ITN_RESULT = "itn.result";
        String NLU_START = "nlu.start";
        String NLU_OUTPUT = "nlu.output";
        String NLU_DUI_OUTPUT = "nlu.dui.output";
        String NLU_AIDUI_OUTPUT = "nlu.aidui.output";

        String NLU_BCDV2_OUTPUT = "nlu.bcdv2.output";

        String TTS_SYNTHESIS_START = "tts.synthesis.start";
        String TTS_SYNTHESIS_FIRST = "tts.synthesis.first";
        String TTS_SYNTHESIS_FINISH = "tts.synthesis.finish";
        String TTS_PLAY_START = "tts.play.start";
        String TTS_PLAY_PROGRESS_FIRST = "tts.play.progress.first";
        String TTS_PLAY_FINISH = "tts.play.finish";
    }
}
