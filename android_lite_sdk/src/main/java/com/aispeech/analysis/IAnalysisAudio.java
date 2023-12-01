package com.aispeech.analysis;

import static com.aispeech.lite.AISpeechSDK.UPLOAD_MODE_FORBIDDEN;

import com.aispeech.common.Log;
import com.aispeech.gourd.FileBuilder;

import org.json.JSONObject;

import java.util.Map;

public interface IAnalysisAudio {

    String getAudioMode();

    boolean init(JSONObject object);

    boolean isUploadEnable();

    void setAudioUpload(boolean uploadEnable);

    /**
     * 上传的音频是否要编码
     *
     * @return
     */
    boolean isEncode();

    void cacheData(String tag, String level, String module, String recordId, JSONObject input, JSONObject output, Map<String, Object> msgObject);

    void cacheFile(String filePath);

    @Deprecated
    void cacheFileBuilder(String filePath, String fileName, String encode);

    void cacheFileBuilder(FileBuilder builder);

    void start();

    void stop();

    void release();

    void startUploadImmediately();

    int getUploadDelayTime();


    class AnalysisAudioEmpty implements IAnalysisAudio {
        private static volatile IAnalysisAudio instance = null;

        private AnalysisAudioEmpty() {
            Log.d("AnalysisAudioEmpty", "No Implement IAnalysisAudio, use default Empty");
        }

        public static IAnalysisAudio getInstance() {
            if (instance == null)
                synchronized (AnalysisAudioEmpty.class) {
                    if (instance == null)
                        instance = new AnalysisAudioEmpty();
                }
            return instance;
        }


        @Override
        public String getAudioMode() {
            return UPLOAD_MODE_FORBIDDEN;
        }

        @Override
        public boolean init(JSONObject object) {
            return true;
        }

        @Override
        public boolean isUploadEnable() {
            return false;
        }

        @Override
        public void setAudioUpload(boolean uploadEnable) {

        }

        @Override
        public boolean isEncode() {
            return false;
        }

        @Override
        public void cacheData(String tag, String level, String module, String recordId, JSONObject input, JSONObject output, Map<String, Object> msgObject) {

        }

        @Override
        public void cacheFile(String filePath) {

        }

        @Override
        public void cacheFileBuilder(String filePath, String fileName, String encode) {

        }

        @Override
        public void cacheFileBuilder(FileBuilder builder) {

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

        @Override
        public void startUploadImmediately() {

        }

        @Override
        public int getUploadDelayTime() {
            return 0;
        }
    }
}
