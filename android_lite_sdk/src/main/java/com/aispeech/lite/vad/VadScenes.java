package com.aispeech.lite.vad;

import java.util.HashMap;
import java.util.Map;

/**
 * Description: Vad使用场景标记类，可以用来标记不同场景，进行细分控制
 * Author: junlong.huang
 * CreateTime: 2022/9/8
 */
public class VadScenes {

    public static final int VAD_ENGINE = 0x01 << 11;

    public static final int VAD_HOT_WORD = 0x01 << 12;

    public static final int VAD_WAKEUP_INCREMENT = 0x01 << 13;


    static Map<String, Integer> tagScenesMap = new HashMap<>();

    static {
        tagScenesMap.put("VadEngine", VAD_ENGINE);
        tagScenesMap.put("hotWord", VAD_HOT_WORD);
        tagScenesMap.put("wakeupIncrement", VAD_WAKEUP_INCREMENT);
    }

    /**
     * 将pretag 解析为场景
     *
     * @param tag
     * @return
     */
    public static int parseTag2Scene(String tag) {
        return tagScenesMap.get(tag) == null ? -1 : tagScenesMap.get(tag);
    }
}
