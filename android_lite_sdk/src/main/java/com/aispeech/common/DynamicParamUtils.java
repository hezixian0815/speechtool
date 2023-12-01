package com.aispeech.common;

import com.aispeech.export.exception.IllegalPinyinException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class DynamicParamUtils {
    public static final String TAG = "DynamicParamUtils";

    private static boolean checkParam(String[] wakeupWord, float[] threshold, int[] majors, boolean checkPinyin) throws IllegalPinyinException {
        PinYinUtils.checkPinyin(wakeupWord);

        if (wakeupWord == null || wakeupWord.length == 0 || threshold == null || threshold.length == 0
                || majors == null || majors.length == 0) {
            return false;
        }

        if (wakeupWord.length != threshold.length || wakeupWord.length != majors.length) {
            throw new IllegalPinyinException("illegal wakeup setting");
        }

        return true;
    }


    /**
     * 动态调整参数，具体请参照 demo
     *
     * @param wakeupWord  唤醒词，参数示例：["ni hao xiao chi","xiao bu xiao bu"]
     * @param threshold   唤醒词对应的阈值，参数示例：[0.2, 0.3]
     * @param majors      是否主唤醒词，主唤醒词为1，副唤醒词为0，如 [1,0]
     *                    设置主唤醒词后，内核会对唤醒词部分音频进行回溯
     * @param checkPinyin 检查拼音
     * @return 唤醒词热更新参数
     * @throws IllegalPinyinException {@link IllegalPinyinException} 非法拼音异常
     */
    public static String getWakeupWordsParams(String[] wakeupWord, float[] threshold, int[] majors, boolean checkPinyin) throws IllegalPinyinException {
        if (!checkParam(wakeupWord, threshold, majors, checkPinyin)) {
            Log.e(TAG, "drop illegal wakeup params setting");
            return "";
        }

        StringBuilder sb = new StringBuilder();

        sb.append("words=");
        for (int i = 0; i < wakeupWord.length; i++) {
            sb.append(wakeupWord[i].trim());
            if (i < wakeupWord.length - 1)
                sb.append(",");
        }

        sb.append(";thresh=");
        for (int i = 0; i < threshold.length; i++) {
            sb.append(String.format("%.3f", threshold[i]));
            if (i < threshold.length - 1)
                sb.append(",");
        }

        sb.append(";major=");
        for (int i = 0; i < majors.length; i++) {
            sb.append(majors[i]);
            if (i < majors.length - 1)
                sb.append(",");
        }
        sb.append(";");

        Map<String, Object> dynamicParam = new HashMap<>();
        // 动态设置唤醒env
        dynamicParam.put("env", sb.toString());
        return getDynamicParam(dynamicParam);
    }

    /**
     * 设置唤醒env热更新/smode/nlms模式切换，可以在引擎初始化成功后动态设置
     *
     * @param dynamicParam 动态参数, Map 类型，key 为 String 类型，value 一般为 String int float 等基础数据类型
     * @return 唤醒词热更新参数
     */
    public static String getDynamicParam(Map<String, ?> dynamicParam) {
        if (dynamicParam == null || dynamicParam.isEmpty()) {
            Log.i(TAG, "dynamicParam: dynamicParam isEmpty ");
            return "";
        }

        JSONObject setJson = new JSONObject();
        Iterator<String> iter = dynamicParam.keySet().iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            Object value = dynamicParam.get(key);
            try {
                setJson.put(key, value);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "dynamicParam " + e);
            }
        }

        return setJson.toString();
    }

}
