package com.aispeech.util;

import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import com.aispeech.kernel.BaseLiteSo;
import com.aispeech.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.SoftReference;

public class CoreVersionUtil {

    public static final String TAG = "CoreVersionUtil";

    private static volatile CoreVersionUtil coreVersionUtil;

    SparseArray<SoftReference<BaseLiteSo>> sparseArray;

    private CoreVersionUtil() {
        sparseArray = new SparseArray<>();
    }

    public static CoreVersionUtil getInstance() {
        if (coreVersionUtil == null) {
            synchronized (CoreVersionUtil.class) {
                if (coreVersionUtil == null) {
                    coreVersionUtil = new CoreVersionUtil();
                }
            }
        }
        return coreVersionUtil;
    }


    public synchronized void add(BaseLiteSo object) {
        Log.i(TAG, "add: " + object);
        if (object == null) {
            return;
        }
        SoftReference<BaseLiteSo> softReference = new SoftReference(object);
        sparseArray.put(object.hashCode(), softReference);

        Log.i(TAG, "add   end: " + object);
    }

    public synchronized void remove(BaseLiteSo object) {
        Log.i(TAG, "remove: " + object);
        if (object == null) {
            return;
        }
        sparseArray.remove(object.hashCode());
    }

    public synchronized void clear() {
        sparseArray.clear();
    }

    private synchronized JSONArray getCoreVersion() {
        JSONArray jsonArray = new JSONArray();
        try {
            for (int i = 0; i < sparseArray.size(); i++) {
                SoftReference<BaseLiteSo> baseLiteSoSoftReference = sparseArray.valueAt(i);

                if (baseLiteSoSoftReference != null && baseLiteSoSoftReference.get() != null && baseLiteSoSoftReference.get().getEngineId() != 0) {
                    String resource = baseLiteSoSoftReference.get().getVersionInfo();
                    if (!TextUtils.isEmpty(resource)) {
                        JSONObject jsonObject = new JSONObject(resource);
                        jsonArray.put(jsonObject);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    public synchronized JSONObject coreObject() {
        Log.i(TAG, "coreObject: ");
        JSONObject coreVersionObject = new JSONObject();
        try {
            coreVersionObject.put("version", BuildConfig.SO_VERSION);
            coreVersionObject.put("moduleInfo", getCoreVersion());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return coreVersionObject;
    }
}
