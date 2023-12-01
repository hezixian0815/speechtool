package com.aispeech.util;

import android.util.Log;

import com.hzx.aispeech.BuildConfig;
import com.aispeech.common.AuthUtil;
import com.aispeech.export.listeners.AIVersionInfoListener;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AIThreadFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiteSdkCoreVersionUtil {

    public static final String TAG = "LiteSdkCoreVersionUtil";

    private static volatile LiteSdkCoreVersionUtil liteSdkCoreVersionUtil;

    private LiteSdkCoreVersionUtil() {
    }

    public static LiteSdkCoreVersionUtil getInstance() {
        if (liteSdkCoreVersionUtil == null) {
            synchronized (LiteSdkCoreVersionUtil.class) {
                if (liteSdkCoreVersionUtil == null) {
                    liteSdkCoreVersionUtil = new LiteSdkCoreVersionUtil();
                }
            }
        }
        return liteSdkCoreVersionUtil;
    }

    public void getSdkInfo(final AIVersionInfoListener aiVersionInfoListener) {
        AIThreadFactory myThreadFactory = new AIThreadFactory("LiteSdkCoreVersion", Thread.NORM_PRIORITY);
        ExecutorService mPool = Executors.newSingleThreadExecutor(myThreadFactory);
        mPool.execute(new Runnable() {
            @Override
            public void run() {
                getSdkInfoThread(aiVersionInfoListener);
            }
        });
    }

    /**
     * 获取版本信息
     */
    private void getSdkInfoThread(AIVersionInfoListener aiVersionInfoListener) {
        if (AISpeech.getContext() == null) {
            Log.i(TAG, "context or json is null");
            return;
        }
        Log.i(TAG, "getSdkInfoThread: ");
        JSONObject versionObject = new JSONObject();
        try {
            versionObject.put("core", CoreVersionUtil.getInstance().coreObject());
            versionObject.put("lite", getLiteSdkInfo());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (aiVersionInfoListener != null) {
            aiVersionInfoListener.onResult(versionObject.toString());
        }
    }


    /**
     * 获取lite相关信息
     *
     * @return
     */
    private JSONObject getLiteSdkInfo() {
        JSONObject jsonObject = new JSONObject();
        try {
//            jsonObject.put("version", BuildConfig.VERSION_NAME);
            jsonObject.put("version", "2.40.0-ef2751e7");

            JSONObject extra = new JSONObject();
            extra.put("buildType", AuthUtil.getBuildVariant(AISpeech.getContext()));
            extra.put("versionDate", BuildConfig.VERSION_DATE);
            jsonObject.put("extra", extra);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
}
