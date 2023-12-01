package com.aispeech.common;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 离线语义格式转换
 *
 * @author hehr
 */
public class Transformer {

    private static final String TAG = "Transformer";

    private static Set<String> TaskNames = new HashSet<String>() {{
        add("task");
        add("action");
    }};

    private static Set<String> SkillNames = new HashSet<String>() {{
        add("domain");
        add("skill");
    }};

    private static List<Map<String, String[]>> NAVI_CONF = new ArrayList<>();

    /**
     * 读取配置文件，需要预先加载
     *
     * @param path 配置文件
     */
    public static void load(final String path) {
        Log.d(TAG, " load path " + path);
        try {
            JSONObject conf = new JSONObject(FileUtils.read2Str(path));
            JSONArray navi = conf.optJSONArray("地图");
            if (navi == null || navi.length() == 0) {
                throw new IllegalArgumentException("地图产品配置资源无效，请检查 navi_skill.conf 文件.");
            }

            NAVI_CONF.clear();//资源加载之前，先清空数据

            for (int i = 0; i < navi.length(); i++) {
                JSONObject item = navi.optJSONObject(i);
                Iterator iterator = item.keys();
                while (iterator.hasNext()) {
                    final String task = (String) iterator.next();
                    JSONArray array = item.getJSONArray(task);
                    final String[] intents = new String[array.length()];
                    for (int j = 0; j < array.length(); j++) {
                        intents[j] = array.optString(j);
                    }
                    NAVI_CONF.add(new HashMap<String, String[]>() {{
                        put(task, intents);
                    }});
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("Transformer", " load skill conf failed,check skill conf path . ");
        }
    }

    /**
     * 转换grammar 部分的语义格式
     *
     * @param grammer grammar部分的原始语义格式
     * @return JSON
     */
    public static JSONObject transGrammmer(JSONObject grammer) {
        JSONObject nlu = new JSONObject();
        try {
            JSONObject request = new JSONObject();
            Iterator iterator = grammer.keys();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                if (TextUtils.equals("rec", key)) {
                    nlu.put("input", grammer.opt(key));
                } else if (TextUtils.equals("post", key)) {
                    JSONObject post = grammer.optJSONObject("post");
                    if (post.has("sem")) {
                        JSONObject sem = post.optJSONObject("sem");
                        Iterator semIterator = sem.keys();
                        JSONArray slots = new JSONArray();
                        int slotcount = 0;
                        while (semIterator.hasNext()) {
                            String name = (String) semIterator.next();
                            String value = sem.getString(name);
                            if (SkillNames.contains(name)) {
                                nlu.put(name, value);
                            } else if (TaskNames.contains(name)) {
                                request.put(name, value);
                            } else {
                                JSONObject item = new JSONObject();
                                item.put("name", name);
                                item.put("value", value.replaceAll("\\s*" , ""));//语义去字符
                                slots.put(item);
                                slotcount++;
                            }
                        }
                        request.put("slots", slots);
                        request.put("slotcount", slotcount);
                        request.put("confidence", 1);//默认置信度 1
                    }
                } else {
                    nlu.put(key, grammer.opt(key));
                }
            }
            JSONObject semantics = new JSONObject();
            semantics.put("request", request);
            nlu.put("semantics", semantics);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return nlu;
    }

    /**
     * 转换ngram 部分的离线语义
     *
     * @param ngram ngram部分的原始语义格式
     * @param task  前一轮命中的task信息
     * @return JSON
     */
    public static JSONObject transNgram(JSONObject ngram, String task) {

        JSONObject nlu = new JSONObject();

        try {
            Iterator iterator = ngram.keys();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                nlu.put(key, ngram.opt(key));
            }
            if (nlu.has("semantics")) {
                JSONObject semantics = nlu.optJSONObject("semantics");
                if (semantics.has("request")) {
                    JSONObject request = semantics.optJSONObject("request");
                    if (request.has("param")) {
                        JSONObject param = request.optJSONObject("param");
                        Iterator slotsIterator = param.keys();
                        JSONArray slots = new JSONArray();
                        int slotcount = 0;
                        while (slotsIterator.hasNext()) {
                            String key = (String) slotsIterator.next();
                            String value = param.getString(key);
                            JSONObject item = new JSONObject();
                            item.put("name", key);
                            item.put("value", value);
                            slots.put(item);
                            slotcount++;
                            if (TextUtils.equals(key, "intent")) {
                                String normTask = normNluTask(value, task);
                                if (!TextUtils.isEmpty(normTask)) {
                                    request.put("task", normTask);
                                }
                            }
                        }

                        request.put("slots", slots);
                        request.put("slotcount", slotcount);
                        request.put("confidence", 1);//默认置信度 1

                        request.remove("param");
                    }

                    //domain换成skill
                    if (request.has("domain")) {
                        String domain = request.optString("domain");
                        if(domain.contains("_@@_")){
                            domain = domain.split("_@@_")[0];
                        }
                        nlu.put("skill", domain);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return nlu;

    }

    /**
     * 根据intent 和 产品配置表，生成 task 信息
     *
     * @param intent  intent
     * @param preTask 前一轮命中的task信息
     * @return norm task
     */
    private static String normNluTask(String intent, String preTask) {
        if (NAVI_CONF.isEmpty())
            return "";

        String task = normByTask(preTask, intent);
        if (!TextUtils.isEmpty(task)) {
            return task;
        }
        return normByLoop(intent);
    }

    /**
     * 通过前一轮的task信息输出当前轮的task信息
     *
     * @param task   前一轮对话的task信息
     * @param intent intent信息
     * @return task
     */
    private static String normByTask(String task, String intent) {

        String[] intents = findTakInConf(task);

        if (intents != null && intents.length != 0) {
            for (String in : intents) {
                if (TextUtils.equals(in, intent)) {
                    return task;
                }
            }
        }

        return "";
    }

    private static String[] findTakInConf(String task) {
        for (Map<String, String[]> map : NAVI_CONF) {
            if (map.containsKey(task)) {
                return map.get(task);
            }
        }

        return new String[0];

    }

    /**
     * 通过配置文件默认顺序输出当前轮的task信息
     *
     * @param intent intent 信息
     * @return intent
     */
    private static String normByLoop(String intent) {
        for (Map<String, String[]> map : NAVI_CONF) {
            Iterator<String> iterator = map.keySet().iterator();
            while (iterator.hasNext()) {
                String task = iterator.next();
                String[] intents = map.get(task);
                for (String i : intents) {
                    if (TextUtils.equals(i, intent)) {
                        return task;
                    }
                }
            }
        }
        return "";

    }

}
