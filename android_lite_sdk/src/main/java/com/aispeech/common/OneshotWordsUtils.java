package com.aispeech.common;

import android.text.TextUtils;
import android.util.Log;

import com.aispeech.lite.AISpeech;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

public class OneshotWordsUtils {
    private static Vector<String> lastWakeupWordsList = new Vector<>();

    public static synchronized void writeWakeupWordsToFile(String[] wakeupWords, String wakeupFilePath) {
        if (wakeupWords == null) {
            Log.w("OneshotWordsUtils", "wakeupWords is null ");
            return;
        }
        try {
            //1. 检查内容是否重复
            List<String> wakeupWordsList = Arrays.asList(wakeupWords);
            if (lastWakeupWordsList.isEmpty()) {
                String lastWakeupWordsJson = FileIOUtils.readFile2String(wakeupFilePath);
                if (!TextUtils.isEmpty(lastWakeupWordsJson)) {
                    JSONObject lastWakeupWordsObj = new JSONObject(lastWakeupWordsJson);
                    JSONArray lastWakeupWordsArrays = lastWakeupWordsObj.optJSONArray("one_shot_words");
                    for (int i = 0; i < lastWakeupWordsArrays.length(); i++) {
                        lastWakeupWordsList.add(lastWakeupWordsArrays.optString(i));
                    }
                }
            }

            Collections.sort(wakeupWordsList);
            Collections.sort(lastWakeupWordsList);
            if (!lastWakeupWordsList.isEmpty() && lastWakeupWordsList.containsAll(wakeupWordsList) && wakeupWordsList.containsAll(lastWakeupWordsList)) {
                return;
            }

            //2. 重新覆盖写入
            JSONObject wakeupWordsJo = new JSONObject();
            JSONArray jsonArray = new JSONArray();
            for (String word : wakeupWords) {
                jsonArray.put(word);
            }
            wakeupWordsJo.put("one_shot_words", jsonArray);

            FileUtil fileUtil = new FileUtil(AISpeech.getContext());
            FileUtils.deleteFile(wakeupFilePath);
            fileUtil.createFile(wakeupFilePath);
            fileUtil.write(wakeupWordsJo.toString().getBytes());
            fileUtil.closeFile();
            lastWakeupWordsList.clear();
            lastWakeupWordsList.addAll(wakeupWordsList);
            Log.i("OneshotWordsUtils", "update wakeupWords: " + Arrays.toString(wakeupWords) + ",wakeupFilePath = " + wakeupFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
