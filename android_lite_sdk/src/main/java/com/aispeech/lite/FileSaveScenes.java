package com.aispeech.lite;

import com.aispeech.common.Log;

/**
 * Description: 文件保存的场景 暂留  20 - 25位
 * Author: junlong.huang
 * CreateTime: 2023/2/9
 */
public class FileSaveScenes {

    public static final int LOCAL_SEMANTIC_BCDV2 = 0x01 << 20;
    ;

    /**
     * 判断该场景是否开启了文件保存  {@link Engines}
     *
     * @param scenes
     */
    public static boolean isSavingFile(int scenes) { //0x01 << 20
        Log.d("FileSaveScenes", "AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE" + AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE + "engines get" + ((AISpeechSDK.GLOBAL_AUDIO_SAVE_ENGINES.get() & scenes) > 0));
        return AISpeechSDK.GLOBAL_AUDIO_SAVE_ENABLE && (AISpeechSDK.GLOBAL_AUDIO_SAVE_ENGINES.get() & scenes) > 0;
    }
}
