package com.aispeech.analysis;

import com.aispeech.common.Log;
import com.aispeech.gourd.FileBuilder;

import org.json.JSONObject;

import java.util.Map;

public interface IAnalysisMonitor {

    boolean init(JSONObject object);//修改成服务下发开关的模式，需要解析json

    boolean isUploadEnable();//服务开关

    void disableUpload();

    void enableUpload();

    void cacheData(String tag, String level, String module, JSONObject input);

    void cacheData(String tag, String level, String module, String recordId, JSONObject input, JSONObject output, Map<String, Object> entryMap);

    void cacheFile(String filePath);

    void cacheFileBuilder(FileBuilder builder);

    void startUploadImmediately();
    void start();
    void stop();

    void release();


    class IAnalysisMonitorEmpty implements IAnalysisMonitor {
        private static volatile IAnalysisMonitor instance = null;

        private IAnalysisMonitorEmpty() {
            Log.d("IAnalysisMonitorEmpty", "No Implement IAnalysisMonitor, use default Empty");
        }

        public static IAnalysisMonitor getInstance() {
            if (instance == null)
                synchronized (IAnalysisMonitorEmpty.class) {
                    if (instance == null)
                        instance = new IAnalysisMonitorEmpty();
                }
            return instance;
        }

        @Override
        public boolean init(JSONObject object) {
            return false;
        }

        @Override
        public boolean isUploadEnable() {
            return false;
        }

        @Override
        public void disableUpload() {

        }

        @Override
        public void enableUpload() {

        }

        @Override
        public void cacheData(String tag, String level, String module, JSONObject input) {

        }

        @Override
        public void cacheData(String tag, String level, String module, String recordId, JSONObject input, JSONObject output, Map<String, Object> entryMap) {

        }

        @Override
        public void cacheFileBuilder(FileBuilder builder) {

        }

        @Override
        public void cacheFile(String filePath) {

        }

        @Override
        public void startUploadImmediately() {

        }

        @Override
        public void start() {

        }

        @Override
        public void stop() {

        }

        @Override
        public void release() {

        }

    }
}
