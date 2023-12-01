package com.aispeech.analysis;

import android.content.Context;

import com.aispeech.common.Log;

/**
 * @author wuwei
 */
public interface IAnalysisConfig {
    String getUploadConfig(Context context, int[] logId, String productId, String deviceId);

    class IAnalysisConfigEmpty implements IAnalysisConfig {
        private static volatile IAnalysisConfigEmpty instance = null;

        private IAnalysisConfigEmpty() {
            Log.d("IAnalysisConfigEmpty", "No Implement IAnalysisConfig, use default Empty");
        }

        @Override
        public String getUploadConfig(Context context, int[] logId, String productId, String deviceId) {
            return null;
        }

        public static IAnalysisConfig getInstance() {
            if (instance == null)
                synchronized (IAnalysisConfigEmpty.class) {
                    if (instance == null) {
                        instance = new IAnalysisConfigEmpty();
                    }
                }
            return instance;
        }
    }

}
