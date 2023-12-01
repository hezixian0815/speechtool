package com.aispeech.util;

import com.aispeech.common.Log;
import com.aispeech.lite.AISpeech;

/**
 * Description: 通用工具类
 * Author: junlong.huang
 * CreateTime: 2023/7/21
 */
public class Utils {

    private static String TAG = "Utils";

    public static void checkThreadAffinity() {
        Log.d(TAG, "checkThreadAffinity");
        int cpuId = AISpeech.threadAffinity;
        Log.d(TAG, "SET_THREAD_AFFINITY cpuId is : " + cpuId);
        //绑核，用于优化cpu占用
        if (cpuId > 0) {
            int ret = com.aispeech.kernel.Utils.jni_duilite_set_thread_multi_affinity(cpuId);
            Log.d(TAG, "SET_THREAD_AFFINITY ret:" + ret);
        }
    }
}
