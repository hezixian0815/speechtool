package com.aispeech.export.interceptor;

import android.text.TextUtils;

import org.json.JSONObject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SpeechInterceptor {
    private volatile static SpeechInterceptor mInstance;
    private volatile Map<String, IInterceptor> mInterceptorMap = new ConcurrentHashMap<>();

    public static SpeechInterceptor getInstance() {
        SpeechInterceptor localResource = mInstance;
        if (localResource == null) {
            synchronized (SpeechInterceptor.class) {
                localResource = mInstance;
                if (localResource == null) {
                    mInstance = localResource = new SpeechInterceptor();
                }
            }
        }
        return localResource;
    }

    public void addInterceptor(IInterceptor iInterceptor) {
        String key = iInterceptor.getName();
        if (!TextUtils.isEmpty(key)) {
            mInterceptorMap.put(key, iInterceptor);
        }
    }

    public void addInterceptors(IInterceptor... iInterceptors) {
        for (IInterceptor interceptor : iInterceptors) {
            addInterceptor(interceptor);
        }
    }

    public void removeInterceptor(IInterceptor iInterceptor) {
        String key = iInterceptor.getName();
        if (!TextUtils.isEmpty(key)) {
            mInterceptorMap.remove(key);
        }
    }

    public Object doInterceptor(String interceptKey, JSONObject inputObj) {
        if (!TextUtils.isEmpty(interceptKey) && mInterceptorMap.containsKey(interceptKey)) {
            IInterceptor iInterceptor = mInterceptorMap.get(interceptKey);
            Object resultObj = iInterceptor.intercept(inputObj);
            return resultObj;
        }
        return null;
    }

}
