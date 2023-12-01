package com.aispeech.lite.param;

import android.text.TextUtils;
import android.util.Pair;

import com.aispeech.common.ArrayUtils;
import com.aispeech.common.Log;
import com.aispeech.lite.BuiltinWakeupWords;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新唤醒参数解析及env拼接类
 * <p>
 * 新唤醒算法 start时需要传入更多唤醒参数，考虑部分参数不需要对外开放，将默认参数写入到wakeup.bin文件中，
 * 每次dmasp引擎启动后，调用dds_dmasp_get()方法，在dds_dmasp_new()中声明的Dmasp.dmasp_callback会接收到初始参数的返回
 *
 * @link parseBuiltinWakeupConfig() 对内嵌的默认唤醒参数解析
 * @link processDmaspParams() 解析 DmaspParams中的唤醒词参数，与内嵌唤醒参数匹配，并进行补齐、替换
 * <p>
 * author: shunzhan.cheng
 * date : 2022-11-14
 */
public class BuiltinE2EWakeupWords extends BuiltinWakeupWords {
    private static final String TAG = "BuiltinE2EWakeupWords";

    /**
     * 内嵌参数中 是否被使用的标识
     *
     * @link parseBuiltinWakeupConfig()
     */
    public static final String BUILT_IN_WAKEUPWORDS_FLAG = "use_built_in_wakeupwords";

    /**
     * 资源是否需要使用 E2E唤醒词参数
     */
    private boolean useE2EWakeupWord;

    private ArrayList wakeupWordE2EArrays = new ArrayList();

    public BuiltinE2EWakeupWords() {
    }

    /**
     * 是否启用内嵌唤醒词默认参数
     *
     * @return
     */
    public boolean isUseE2EWakeupWord() {
        return useE2EWakeupWord;
    }

    private static class WakeupWordE2E {
        /**
         * 设置唤醒词
         */
        public String word;

        /**
         * 设置唤醒词对应阈值，是否需要设置和唤醒资源有关系
         */
        public float threshold;

        /**
         * 设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0]
         */
        public int major;

        /**
         * 设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验，如 [1,0,0]
         */
        public int dcheck;

        /**
         * 设置双唤醒唤醒词是否使用自定义网络，"1"表示自定义网络，"0"表示主网络,默认为 0.
         */
        private int customNet;

        public static int CUSTOM_NET_DEFAULT_VALUE = 0;

        /**
         * 设置双唤醒唤醒词对应的网络是否打开，"1"表示打开，"0"表示关闭,默认0
         */
        private int enableNet;

        public static int ENABLE_NET_DEFAULT_VALUE = 1;

        /**
         * 设置双唤醒e2e低阈值，结合高阈值配套使用
         */
        private float threshLow;

        public static float THRESH_LOW_DEFAULT_VALUE = 1.0f;

        /**
         * 设置双唤醒e2e高阈值，结合低阈值配套使用
         */
        private float threshHigh;

        public static float THRESH_HIGH_DEFAULT_VALUE = 1.0f;


        public WakeupWordE2E(String word, float threshold, int customNet, int enableNet, float threshHigh, float threshLow) {
            this.word = word;
            this.threshold = threshold;
            this.customNet = customNet;
            this.enableNet = enableNet;
            this.threshLow = threshLow;
            this.threshHigh = threshHigh;
        }

        public String getWord() {
            return word;
        }

        @Override
        public String toString() {
            return "WakeupWordE2E{" +
                    "word='" + word + '\'' +
                    ", threshold=" + threshold +
                    ", major=" + major +
                    ", dcheck=" + dcheck +
                    ", customNet=" + customNet +
                    ", enableNet=" + enableNet +
                    ", threshLow=" + threshLow +
                    ", threshHigh=" + threshHigh +
                    '}';
        }
    }

    /**
     * json 格式如下：
     * <p>
     * {
     * "use_built_in_wakeupwords": true,
     * "built_in_wakeupwords": {
     * "words": ["ni hao ne zha", "ni_hao_ne_zha"],
     * "thresh": [0.54, 0.45],
     * "thresh_high": [0.65, 0.90],
     * "thresh_low": [0.30, 0.30],
     * "custom": [0, 1],
     * "net": [1, 1]
     * }
     * }
     *
     * @param json 从资源里读取到的配置信息
     */

    public void parseBuiltinWakeupConfig(String json) {
        if (TextUtils.isEmpty(json)) {
            reset();
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject(json);

            boolean useE2eWakeupWord = jsonObject.getBoolean("use_built_in_wakeupwords");
            JSONObject jsonEnv = jsonObject.getJSONObject("built_in_wakeupwords");
            JSONArray wordsJsonArray = jsonEnv.getJSONArray("words");
            JSONArray threshJsonArray = jsonEnv.getJSONArray("thresh");
            JSONArray customJsonArray = jsonEnv.getJSONArray("custom");
            JSONArray netJsonArray = jsonEnv.getJSONArray("net");
            JSONArray threshHighJsonArray = jsonEnv.getJSONArray("thresh_high");
            JSONArray threshLowJsonArray = jsonEnv.getJSONArray("thresh_low");


            if (checkLength(wordsJsonArray, threshJsonArray, customJsonArray, netJsonArray, threshHighJsonArray, threshLowJsonArray)) {
                String[] words = new String[wordsJsonArray.length()];
                for (int i = 0; i < wordsJsonArray.length(); i++) {
                    words[i] = wordsJsonArray.getString(i);
                }

                float[] thresh = new float[threshJsonArray.length()];
                for (int i = 0; i < threshJsonArray.length(); i++) {
                    thresh[i] = (float) threshJsonArray.getDouble(i);
                }

                int[] custom = new int[customJsonArray.length()];
                for (int i = 0; i < customJsonArray.length(); i++) {
                    custom[i] = customJsonArray.getInt(i);
                }

                int[] net = new int[netJsonArray.length()];
                for (int i = 0; i < netJsonArray.length(); i++) {
                    net[i] = netJsonArray.getInt(i);
                }

                float[] threshHigh = new float[threshHighJsonArray.length()];
                for (int i = 0; i < threshHighJsonArray.length(); i++) {
                    threshHigh[i] = (float) threshHighJsonArray.getDouble(i);
                }

                float[] threshLow = new float[threshLowJsonArray.length()];
                for (int i = 0; i < threshLowJsonArray.length(); i++) {
                    threshLow[i] = (float) threshLowJsonArray.getDouble(i);
                }

                this.useE2EWakeupWord = useE2eWakeupWord;

                for (int i = 0; i < words.length; i++) {
                    WakeupWordE2E wakeupWordE2E = new WakeupWordE2E(words[i], thresh[i], custom[i], net[i], threshHigh[i], threshLow[i]);
                    wakeupWordE2EArrays.add(wakeupWordE2E);
                }
            } else {
                reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
            reset();
        }
    }

    private boolean checkLength(JSONArray... arrays) {
        try {
            if (arrays[0] != null) {
                int len = arrays[0].length();
                for (JSONArray array : arrays) {
                    if (array == null || array.length() != len)
                        return false;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private boolean checkLength(Object... arrays) {

        try {
            if (arrays[0] != null) {
                int len = Array.getLength(arrays[0]);
                for (Object array : arrays) {
                    if (array == null || Array.getLength(array) != len)
                        return false;
                }
            }
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    private void reset() {
        this.useE2EWakeupWord = false;
        wakeupWordE2EArrays.clear();
    }


    /**
     * 检查唤醒词是否与默认唤醒词匹配
     *
     * @param word
     * @return 存在 返回唤醒词
     */

    public WakeupWordE2E getMatchingWakeword(String word) {
        if (TextUtils.isEmpty(word))
            return null;
        if (wakeupWordE2EArrays != null && wakeupWordE2EArrays.size() > 0) {
            for (Object wakeupWordE2E : wakeupWordE2EArrays) {
                WakeupWordE2E wakeupWord = (WakeupWordE2E) wakeupWordE2E;
                if (wakeupWord.getWord().equals(word))
                    return wakeupWord;
            }
        }
        return null;
    }


    /**
     * 解析 DmaspParams中的唤醒词参数，与内嵌唤醒参数匹配，并进行补齐、替换
     *
     * @param dmaspParam
     * @return
     */
    public DmaspParams processDmaspParams(DmaspParams dmaspParam) {
        Log.d(TAG, "processDmaspParams original: " + dmaspParam.toJSON());
        if (!useE2EWakeupWord)
            return dmaspParam;
        if (TextUtils.isEmpty(dmaspParam.toJSON().toString()))
            return dmaspParam;

        try {
            String[] wakeupWords = dmaspParam.getWords();
            String[] customNet = dmaspParam.getCustomNet();
            String[] enableNet = dmaspParam.getEnableNet();
            String[] threshHigh = dmaspParam.getThreshHigh();
            String[] threshLow = dmaspParam.getThreshLow();
            dmaspParam.setCustomNet(new String[wakeupWords.length]);
            dmaspParam.setEnableNet(new String[wakeupWords.length]);
            dmaspParam.setThreshHigh(new String[wakeupWords.length]);
            dmaspParam.setThreshLow(new String[wakeupWords.length]);

            // 循环遍历传入的唤醒词，补齐对应参数
            for (int i = 0; i < wakeupWords.length; i++) {
                WakeupWordE2E wakeupWord = getMatchingWakeword(wakeupWords[i]);

                if (wakeupWord == null) {
                    Log.d(TAG, "UnMatching WakeupWord=" + wakeupWords[i] + "    " + threshHigh[i]);
                    // 没有匹配到用默认值补齐
                    if (customNet != null && customNet.length > i && !TextUtils.isEmpty(customNet[i])) {
                        dmaspParam.getCustomNet()[i] = customNet[i];
                    } else {
                        dmaspParam.getCustomNet()[i] = String.valueOf(WakeupWordE2E.CUSTOM_NET_DEFAULT_VALUE);
                    }
                    if (enableNet != null && enableNet.length > i && !TextUtils.isEmpty(enableNet[i])) {
                        dmaspParam.getEnableNet()[i] = enableNet[i];
                    } else {
                        dmaspParam.getEnableNet()[i] = String.valueOf(WakeupWordE2E.ENABLE_NET_DEFAULT_VALUE);
                    }

                    if (threshHigh != null && threshHigh.length > i && !TextUtils.isEmpty(threshHigh[i])) {
                        dmaspParam.getThreshHigh()[i] = threshHigh[i];
                    } else {
                        dmaspParam.getThreshHigh()[i] = String.valueOf(WakeupWordE2E.THRESH_HIGH_DEFAULT_VALUE);
                    }

                    if (threshLow != null && threshLow.length > i && !TextUtils.isEmpty(threshLow[i])) {
                        dmaspParam.getThreshLow()[i] = threshLow[i];
                    } else {
                        dmaspParam.getThreshLow()[i] = String.valueOf(WakeupWordE2E.THRESH_LOW_DEFAULT_VALUE);
                    }

                } else {
                    // 匹配到默认唤醒词， 从配置项中读取参数
                    Log.d(TAG, "Matching WakeupWord=" + wakeupWord.toString());
                    dmaspParam.getCustomNet()[i] = String.valueOf(wakeupWord.customNet);
                    dmaspParam.getEnableNet()[i] = String.valueOf(wakeupWord.enableNet);
                    dmaspParam.getThreshHigh()[i] = String.valueOf(wakeupWord.threshHigh);
                    dmaspParam.getThreshLow()[i] = String.valueOf(wakeupWord.threshLow);

                    // 将匹配到的内核E2E唤醒词添加到配置中
                    if (!TextUtils.isEmpty(wakeupWord.word)) {
                        addBuiltinE2EWakeupWord(dmaspParam, wakeupWord.word);
                    }
                }
            }

            Log.d(TAG, "processDmaspParams result=" + dmaspParam.toJSON().toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return dmaspParam;
    }


    /**
     * 将内置唤醒词中 配置的下划线唤醒词添加到 DmaspParams中
     *
     * @param dmaspParam
     * @param mathchWord
     */
    private void addBuiltinE2EWakeupWord(DmaspParams dmaspParam, String mathchWord) {

        if (wakeupWordE2EArrays != null && wakeupWordE2EArrays.size() > 0) {
            for (Object wakeupWordE2E : wakeupWordE2EArrays) {
                WakeupWordE2E wakeupWord = (WakeupWordE2E) wakeupWordE2E;
                if (wakeupWord.word.equals(mathchWord.replace(" ", "_"))) {
                    // 唤醒词
                    boolean isRepeat = false;
                    if (dmaspParam.getWords() != null) {
                        Pair<String[], Boolean> pair = ArrayUtils.addStringArrayToRepeat(dmaspParam.getWords(), wakeupWord.word);
                        if (pair != null && pair.second) {
                            dmaspParam.setWords(pair.first);
                        } else {
                            isRepeat = true;
                        }
                    }
                    if (isRepeat) {
                        return;
                    }
                    // 阈值
                    if (dmaspParam.getThreshold() != null) {
                        dmaspParam.setThreshold(ArrayUtils.addFloatArry(dmaspParam.getThreshold(), wakeupWord.threshold));
                    }
                    // 主唤醒词
                    if (dmaspParam.getMajors() != null) {
                        dmaspParam.setMajors(ArrayUtils.addIntArry(dmaspParam.getMajors(), wakeupWord.major));
                    }
                    // 双重校验
                    if (dmaspParam.getDcheck() != null) {
                        dmaspParam.setDchecks(ArrayUtils.addIntArry(dmaspParam.getDcheck(), wakeupWord.dcheck));
                    }
                    // 自定义网络
                    if (dmaspParam.getCustomNet() != null) {
                        dmaspParam.setCustomNet(ArrayUtils.addStringArray(dmaspParam.getCustomNet(), String.valueOf(wakeupWord.customNet)));

                    }
                    // 网络类型
                    if (dmaspParam.getEnableNet() != null) {
                        dmaspParam.setEnableNet(ArrayUtils.addStringArray(dmaspParam.getEnableNet(), String.valueOf(wakeupWord.enableNet)));

                    }
                    // 高阈值
                    if (dmaspParam.getThreshHigh() != null) {
                        dmaspParam.setThreshHigh(ArrayUtils.addStringArray(dmaspParam.getThreshHigh(), String.valueOf(wakeupWord.threshHigh)));

                    }
                    // 低阈值
                    if (dmaspParam.getThreshLow() != null) {
                        dmaspParam.setThreshLow(ArrayUtils.addStringArray(dmaspParam.getThreshLow(), String.valueOf(wakeupWord.threshLow)));
                    }
                }
            }
        }
    }


    /**
     * 动态更新唤醒词信息，解析外部传入的env字段，补齐参数重新拼接
     * words=ni hao ne zha,ni_hao_ne_zha,ni hao xiao pi;thresh=0.540,0.450,0.100;major=0,0,0;
     *
     * @param envStr
     */
    public String processEnvJsonString(String envStr) {
        Log.d(TAG, "processEnvJsonString original: " + envStr);

        if (TextUtils.isEmpty(envStr)) {
            return envStr;
        }

        if (!useE2EWakeupWord) {
            return envStr;
        }

        try {
            // 解析传入的env参数
            JSONObject jsonObject = new JSONObject(envStr);
            String str = jsonObject.getString("env");

            String[] params = str.split(";");
            Map<String, String> paramsMap = new HashMap();
            for (String param : params) {
                String[] array = param.split("=");
                paramsMap.put(array[0], array[1]);
            }

            // 创建临时参数对象，完成后续Env拼接
            DmaspParams param = new DmaspParams();
            if (paramsMap.containsKey("words")) {
                String[] words = paramsMap.get("words").toString().split(",");
                param.setWords(words);
                paramsMap.remove("words");
            }
            if (paramsMap.containsKey("thresh")) {
                String[] thresh = paramsMap.get("thresh").toString().split(",");
                param.setThreshold(ArrayUtils.string2Float(thresh));
                paramsMap.remove("thresh");
            }
            if (paramsMap.containsKey("major")) {
                String[] major = paramsMap.get("major").toString().split(",");
                param.setMajors(ArrayUtils.string2Int(major));
                paramsMap.remove("major");
            }
            if (paramsMap.containsKey("dcheck")) {
                String[] dcheck = paramsMap.get("dcheck").toString().split(",");
                param.setDchecks(ArrayUtils.string2Int(dcheck));
                paramsMap.remove("dcheck");
            }

            if (paramsMap.containsKey("thresh_high")) {
                String[] thresh_high = paramsMap.get("thresh_high").toString().split(",");
                param.setThreshHigh(thresh_high);
                paramsMap.remove("thresh_high");
            }

            if (paramsMap.containsKey("thresh_low")) {
                String[] thresh_low = paramsMap.get("thresh_low").toString().split(",");
                param.setThreshLow(thresh_low);
                paramsMap.remove("thresh_low");
            }

            if (paramsMap.containsKey("custom")) {
                String[] custom = paramsMap.get("custom").toString().split(",");
                param.setCustomNet(custom);
                paramsMap.remove("custom");
            }

            if (paramsMap.containsKey("net")) {
                String[] net = paramsMap.get("net").toString().split(",");
                param.setEnableNet(net);
                paramsMap.remove("net");
            }

            String envJosnStr = processDmaspParams(param).toEnv();
            StringBuilder envSb = new StringBuilder(envJosnStr);

            // 补回其他参数
            for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
                if (!envJosnStr.contains((entry.getKey()) + "=")) {
                    envSb.append(entry.getKey()).append("=").append(entry.getValue()).append(";");
                }
            }

            jsonObject.put("env", envSb);
            //取出env，在这里对subword进行判断
            repairSubwords(jsonObject);
            Log.d(TAG, "processEnvJsonString result=" + jsonObject.toString());
            return jsonObject.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return envStr;
    }

    private DmaspParams removeUnderlineWord(DmaspParams param) {
        List<String> wordList = new ArrayList<>();
        List<String> threshList = new ArrayList<>();
        List<String> majorList = new ArrayList<>();
        List<String> dcheckList = new ArrayList<>();

        String[] words = param.getWords();
        for (int i = 0; i < words.length; i++) {
            if (!words[i].contains("_")) {
                wordList.add(words[i]);
                threshList.add(String.valueOf(param.getThreshold()[i]));
                majorList.add(String.valueOf(param.getMajors()[i]));
                dcheckList.add(String.valueOf(param.getDcheck()[i]));
            }
        }

        param.setWords(wordList.toArray(new String[wordList.size()]));
        param.setThreshold(ArrayUtils.string2Float(threshList.toArray(new String[threshList.size()])));
        param.setMajors(ArrayUtils.string2Int(majorList.toArray(new String[majorList.size()])));
        param.setDchecks(ArrayUtils.string2Int(dcheckList.toArray(new String[dcheckList.size()])));
        return param;
    }

    public JSONObject repairSubwords(JSONObject jsonObject) {
        try {
            String str = jsonObject.getString("env");
            String[] Subparams = str.split(";");
            Map<String, String> SubparamsMap = new HashMap();
            String[] words;
            String[] subword_wakeup = null;
            for (String param : Subparams) {
                String[] array = param.split("=");
                SubparamsMap.put(array[0], array[1]);
            }
            if (SubparamsMap.containsKey("subword_wakeup")) {
                if(SubparamsMap.containsKey("words")){
                    words = SubparamsMap.get("words").toString().split(",");
                    subword_wakeup = new String[words.length];
                    for (int i = 0; i < words.length; i++) {
                        String word= words[i];
                       int len = word.split(" ").length;
                       if(len ==2){
                           subword_wakeup[i] = "1";
                       }else {
                           subword_wakeup[i] = "0";
                       }
                    }
                }
                // 将更新后的subword_wakeup重新放入SubparamsMap中
                SubparamsMap.put("subword_wakeup", String.join(",",subword_wakeup));
                // 更新jsonObject中的env字段
                jsonObject.put("env", mapToEnvString(SubparamsMap));
            }else {
                return jsonObject;
            }

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return jsonObject;
    }
    private String mapToEnvString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }

}
