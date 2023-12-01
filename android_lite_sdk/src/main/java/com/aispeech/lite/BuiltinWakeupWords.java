package com.aispeech.lite;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.lite.config.LocalSignalProcessingConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BuiltinWakeupWords {
    private static final String TAG = "BuiltinWakeupWords";
    private boolean useBuiltInWakeupWords = false;
    private final List<String> words = new ArrayList<>();
    private float[] thresh;
    private float[] threshLoud;

    public BuiltinWakeupWords() {
    }

    /**
     * json 格式如下：
     * <pre>
     * {
     * 	"use_built_in_wakeupwords": true,
     * 	"built_in_wakeupwords": {
     * 		"words": ["ni hao xiao bu", "xiao bu xiao bu", "hei bu rui nuo"],
     * 		"thresh": [0.2,	0.3, 0.4],
     * 		"thresh2": [0.25, 0.35, 0.45]
     *   }
     * }
     * </pre>
     *
     * @param json 从资源里读取到的配置信息
     */
    public void parseConfig(String json) {
        if (TextUtils.isEmpty(json)) {
            reset();
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);
            boolean useBuiltInWakeupWords = jsonObject.getBoolean("use_built_in_wakeupwords");
            if (useBuiltInWakeupWords) {
                JSONObject built_in_wakeupwords = jsonObject.getJSONObject("built_in_wakeupwords");
                JSONArray wordsJSONArray = built_in_wakeupwords.getJSONArray("words");
                JSONArray threshJSONArray = built_in_wakeupwords.getJSONArray("thresh");
                JSONArray thresh_loudJSONArray = built_in_wakeupwords.getJSONArray("thresh2");
                if (wordsJSONArray.length() > 0 && wordsJSONArray.length() == threshJSONArray.length()
                        && wordsJSONArray.length() == thresh_loudJSONArray.length()) {
                    String[] words = new String[wordsJSONArray.length()];
                    for (int i = 0; i < wordsJSONArray.length(); i++) {
                        words[i] = wordsJSONArray.getString(i);
                    }
                    float[] thresh = new float[threshJSONArray.length()];
                    for (int i = 0; i < threshJSONArray.length(); i++) {
                        thresh[i] = (float) threshJSONArray.getDouble(i);
                    }
                    float[] thresh_loud = new float[thresh_loudJSONArray.length()];
                    for (int i = 0; i < thresh_loudJSONArray.length(); i++) {
                        thresh_loud[i] = (float) thresh_loudJSONArray.getDouble(i);
                    }

                    this.useBuiltInWakeupWords = true;
                    this.words.clear();
                    this.words.addAll(Arrays.asList(words));
                    this.thresh = thresh;
                    this.threshLoud = thresh_loud;
                } else {
                    reset();
                }
            } else
                reset();
        } catch (Exception e) {
            e.printStackTrace();
            reset();
        }
    }

    private void reset() {
        useBuiltInWakeupWords = false;
        words.clear();
        thresh = null;
        threshLoud = null;
    }

    public boolean isUseBuiltInWakeupWords() {
        return useBuiltInWakeupWords;
    }

    public boolean checkWords(String[] wakeup) {
        if (wakeup == null || wakeup.length == 0)
            return false;
        for (String s : wakeup) {
            if (!words.contains(s))
                return false;
        }
        return true;
    }

    /**
     * 此方法在 {@link #checkWords(String[])} 后调用
     *
     * @param wakeup 唤醒词
     * @return 阈值数组
     */
    public String[] getThreshString(String[] wakeup) {
        try {
            String[] threshString = new String[wakeup.length];
            for (int i = 0; i < wakeup.length; i++) {
                int pos = words.indexOf(wakeup[i]);
                threshString[i] = String.valueOf(thresh[pos]);
            }
            return threshString;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 此方法在 {@link #checkWords(String[])} 后调用
     *
     * @param wakeup 唤醒词
     * @return 阈值数组
     */
    public float[] getThresh(String[] wakeup) {
        try {
            float[] threshFloat = new float[wakeup.length];
            for (int i = 0; i < wakeup.length; i++) {
                int pos = words.indexOf(wakeup[i]);
                threshFloat[i] = thresh[pos];
            }
            return threshFloat;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 此方法在 {@link #checkWords(String[])} 后调用
     *
     * @param wakeup 唤醒词
     * @return 阈值数组
     */
    public float[] getThreshLoud(String[] wakeup) {
        try {
            float[] threshFloat = new float[wakeup.length];
            for (int i = 0; i < wakeup.length; i++) {
                int pos = words.indexOf(wakeup[i]);
                threshFloat[i] = threshLoud[pos];
            }
            return threshFloat;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String toString() {
        return "BuiltinWakeupWords{" +
                "useBuiltInWakeupWords=" + useBuiltInWakeupWords +
                ", words=" + words +
                ", thresh=" + Arrays.toString(thresh) +
                ", threshLoud=" + Arrays.toString(threshLoud) +
                '}';
    }

    public String processWakeupWordInSetMethod(String setparam) {
        Log.d(TAG, "processWakeupWordInSetMethod original: " + setparam);
        if (!isUseBuiltInWakeupWords())
            return setparam;
        if (TextUtils.isEmpty(setparam))
            return setparam;
        if (!setparam.contains("env"))
            return setparam;

        try {
            JSONObject jsonObject = new JSONObject(setparam);
            String env = jsonObject.getString("env");
            String[] ss = env.split(";");
            int wakeupPos = -1;
            String[] words = null;
            int threshPos = -1;
            int thresh2Pos = -1;
            for (int i = 0; i < ss.length; i++) {
                String s = ss[i];
                if (TextUtils.isEmpty(s))
                    continue;
                String[] kv = s.split("=");
                if (kv.length != 2 || TextUtils.isEmpty(kv[0].trim()))
                    continue;
                if ("words".equals(kv[0].trim())) {
                    wakeupPos = i;
                    String[] vv = kv[1].trim().split(",");
                    ArrayList<String> wordsList = new ArrayList();
                    for (String v : vv) {
                        String vtrim = v.trim();
                        if (!TextUtils.isEmpty(vtrim)) {
                            wordsList.add(vtrim);
                        }
                    }
                    words = new String[wordsList.size()];
                    wordsList.toArray(words);
                } else if ("thresh".equals(kv[0].trim())) {
                    threshPos = i;
                } else if ("thresh2".equals(kv[0].trim())) {
                    thresh2Pos = i;
                }
            }

            if (wakeupPos == -1 || words == null || words.length == 0 || (threshPos == -1 && thresh2Pos == -1)) {
                Log.d(TAG, "processWakeupWordInSetMethod 获取唤醒词信息失败 wakeupPos:" + wakeupPos
                        + " threshPos:" + threshPos + " thresh2Pos:" + thresh2Pos + " words: " + words);
                return setparam;
            }

            // check words
            if (!checkWords(words)) {
                Log.d(TAG, "processWakeupWordInSetMethod 唤醒词检查不通过");
                return null;
            }

            // thresh
            float[] builtinThresh = getThresh(words);
            if (builtinThresh == null) {
                Log.d(TAG, "processWakeupWordInSetMethod 获取资源内置阈值error");
                return null;
            }

            StringBuilder threshSb = new StringBuilder();
            for (int i = 0; i < builtinThresh.length; i++) {
                threshSb.append(builtinThresh[i]);
                if (i != builtinThresh.length - 1) {
                    threshSb.append(",");
                }
            }
            ss[threshPos] = "thresh=" + threshSb.toString();

            if (thresh2Pos != -1) {
                // thresh2
                float[] builtinThreshLoud = getThreshLoud(words);
                if (builtinThreshLoud == null) {
                    Log.d(TAG, "processWakeupWordInSetMethod 获取资源内置Loud阈值error");
                    return null;
                }

                StringBuilder sbthresh2 = new StringBuilder();
                for (int i = 0; i < builtinThreshLoud.length; i++) {
                    sbthresh2.append("" + builtinThreshLoud[i]);
                    if (i != builtinThreshLoud.length - 1) {
                        sbthresh2.append(",");
                    }
                }
                ss[thresh2Pos] = "thresh2=" + sbthresh2.toString();
            }

            StringBuilder sbSS = new StringBuilder();
            for (int i = 0; i < ss.length; i++) {
                sbSS.append(ss[i]);
                if (i < ss.length - 1) {
                    sbSS.append(";");
                }
            }

            jsonObject.put("env", sbSS.toString());
            String jsong = jsonObject.toString();
            Log.d(TAG, "processWakeupWordInSetMethod success: " + jsong);
            return jsong;
        } catch (Exception e) {
            e.printStackTrace();
            return setparam;
        }
    }


    public boolean useBuiltinWakeupWords(LocalSignalProcessingConfig config) {
        Log.d(TAG, this.toString());
        if (config == null || !this.isUseBuiltInWakeupWords())
            return true;

        if (!this.checkWords(config.getWakupWords())) {
            Log.d(TAG, "useBuiltinWakeupWords 唤醒词检查不通过");
            return false;
        }

        float[] builtinThresh = this.getThresh(config.getWakupWords());
        if (builtinThresh == null) {
            Log.d(TAG, "useBuiltinWakeupWords 获取资源内置阈值error");
            return false;
        }
        config.setThreshs(builtinThresh);

        float[] builtinThreshLoud = this.getThreshLoud(config.getWakupWords());
        if (builtinThreshLoud == null) {
            Log.d(TAG, "useBuiltinWakeupWords 获取资源内置Loud阈值error");
            return false;
        }
        config.setThreshs2(builtinThreshLoud);

        Log.d(TAG, "useBuiltinWakeupWords success");
        return true;
    }
}
