package com.aispeech.common;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Locale;

/**
 * Description: 统一线程命名入口，便于统一修改
 * Author: junlong.huang
 * CreateTime: 2022/8/16
 */
public class ThreadNameUtil {


    private static final String TAG = "ThreadNameUtil";

    private static final HashMap<String, String> cacheMap = new HashMap<>();

    /**
     * 线程命名 统一前缀
     */
    public static String THREAD_PRE_FIX = "lite-";
    private final static int LEVEL_ENGINE = 1;
    private final static int LEVEL_PROCESSOR = 2;
    private final static int LEVEL_KERNEL = 3;

    private final static int TYPE_LOCAL = 1;
    private final static int TYPE_CLOUD = 2;

    public static final String SEPARATOR = "-";

    /**
     * prefix 映射表，注意prefix不能包含 “-” 。
     * 目前只有两个内核用到这种格式   [prefix]-asr  和  [prefix]-VadKernel
     */

    private static final HashMap<String, String> modelNameMap = new HashMap<>();

    /**
     * 不规范的TAG 映射表
     * 后续重构可以参考这里的映射对TAG进行规范命名
     */
    private static final HashMap<String, String> missTAGMap = new HashMap<>();

    static {
        // init data
        modelNameMap.put("lgram", "0");
        modelNameMap.put("hotword", "1");
        modelNameMap.put("hotWord", "1");
        modelNameMap.put("localSem", "2");
        modelNameMap.put("multi", "3");
        modelNameMap.put("wakeupIncrement", "4");
        modelNameMap.put("VadEngine", "5");
        modelNameMap.put("asr", "6");
        modelNameMap.put("LocalAsrpp", "7");
        modelNameMap.put("CloudDM", "8");
        modelNameMap.put("NVad0", "9");
        modelNameMap.put("NVad1", "10");
        modelNameMap.put("NVad2", "11");
        modelNameMap.put("NVad3", "12");
        modelNameMap.put("oneshot", "13");
        modelNameMap.put("csem", "14");
        modelNameMap.put("lsem", "15");

        // 不规范的TAG Local
        missTAGMap.put("SemanticProcessor", "LocalSemanticProcessor");
        missTAGMap.put("VprintProcessor", "LocalVprintProcessor");
        missTAGMap.put("VprintLiteProcessor", "LocalVprintLiteProcessor");
        missTAGMap.put("HotWordsProcessor", "LocalHotWordsProcessor");
        missTAGMap.put("AIGrammarProcessor", "AILocalGrammarProcessor");

        missTAGMap.put("LocalSemanticKernel", "LocalSemanticDUIKernel");
        missTAGMap.put("SemanticKernel", "LocalSemanticAIDUIKernel");
        missTAGMap.put("AsrppKernel", "LocalAsrppKernel");
        missTAGMap.put("VprintKernel", "LocalVprintKernel");

    }

    public static String getSimpleThreadName(String tag) {
        return THREAD_PRE_FIX + tag;
    }

    public static String getFixedThreadName(String tag) {

        if (!TextUtils.isEmpty(cacheMap.get(tag))) return cacheMap.get(tag);

        String temp = tag;
        int type = 0, level = 0;

        // 含有 prefix 的tag 转为数字,目前只有两个内核用到
        if (tag.endsWith("-asr") || tag.endsWith("-VadKernel")) {
            temp = parsePrefixTag(tag);
            level = LEVEL_KERNEL;
        }

        // 预处理
        temp = pretreatTag(temp);

        // parse type
        if (temp.startsWith("ailocal") || temp.startsWith("local")) {
            type = TYPE_LOCAL;
            temp = temp.replaceFirst("ailocal", "").replaceFirst("local", "");
        } else if (temp.startsWith("aicloud") || temp.startsWith("cloud")) {
            type = TYPE_CLOUD;
            temp = temp.replaceFirst("aicloud", "").replaceFirst("cloud", "");
        }

//        Log.d(TAG, "parse type: " + type + " tag :" + temp);

        // parse level
        if (temp.endsWith("engine")) {
            level = LEVEL_ENGINE;
            temp = temp.substring(0, temp.length() - 6);
        } else if (temp.endsWith("processor")) {
            level = LEVEL_PROCESSOR;
            temp = temp.substring(0, temp.length() - 9);
        } else if (temp.endsWith("kernel")) {
            level = LEVEL_KERNEL;
            temp = temp.substring(0, temp.length() - 6);
        }

//        Log.d(TAG, "parse level: " + level + " tag :" + temp);

        String fixedTag = temp.replace("increment", "I")
                .replace("wakeup", "wake")
                .replace("fullduplex", "full")
                .replace("semantic", "sem")
                .replace("grammar", "gram")
                .replace("vprintlite", "vpr_lite")
                .replace("hotwords", "hot");

        String result = buildTAG(fixedTag, level, type);

        cacheMap.put(tag, result);
        Log.i(TAG, tag + " --> fix tag:" + result);
        if (result.length() >= 15) {
            Log.e(TAG, "result size max: " + result.length());
        }

        return result;
    }

    /**
     * 对不规范的tag进行匹配，对结果转小写
     *
     * @param tag
     * @return 处理后的tag
     */
    private static String pretreatTag(String tag) {

        if (TextUtils.isEmpty(tag)) return "";

        String result = tag;
        if (missTAGMap.containsKey(result)) {
            result = missTAGMap.get(result);
        }

        return result == null ? tag.toLowerCase(Locale.ROOT) : result.toLowerCase(Locale.ROOT);
    }

    /**
     * 解析prefix 开头的tag
     *
     * @param tag
     * @return 处理后的tag
     */
    private static String parsePrefixTag(String tag) {

        try {
            String[] split = tag.split("-");
            if (split.length != 2) return tag;

            String prefix = split[0];
            String result = modelNameMap.get(prefix) == null ? prefix : modelNameMap.get(prefix);
            String model = split[1];

            return model.replace("Kernel", "") + result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tag;
    }


    /**
     * 根据解析的内容构建最终tag
     *
     * @param tag   处理后的关键字
     * @param level 层级
     * @param type  类型，前面已经处理过了，先留着
     * @return
     */
    private static String buildTAG(String tag, int level, int type) {
        // type 改为大写前缀，与DDS层统一
        StringBuilder stringBuilder = new StringBuilder(THREAD_PRE_FIX).append(tag);
        switch (level) {
            case LEVEL_ENGINE:
                stringBuilder.append("-e");
                break;
            case LEVEL_PROCESSOR:
                stringBuilder.append("-p");
                break;
            case LEVEL_KERNEL:
                stringBuilder.append("-k");
                break;
            default:
//                if (type > 0) stringBuilder.append("-n");
                break;
        }

//        if (type > 0) stringBuilder.append(type);

        return stringBuilder.toString();
    }


}
