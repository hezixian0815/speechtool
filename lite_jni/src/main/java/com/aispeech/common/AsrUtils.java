package com.aispeech.common;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class AsrUtils {
    private static final String TAG = "AsrUtils";
    /**
     * 检查asr 启动时携带的expandFnPath参数是否有效，如expandFnPath中的文件不存在，会导致asr启动报错
     * @param expandFnPath
     */
    public static boolean checkExpandFnValid(String expandFnPath) {
        try {
            if(FileUtils.isFileExists(expandFnPath)){
                File file = FileUtils.getFileByPath(expandFnPath);
                String fileContext = FileIOUtils.readFile2String(file);
                JSONObject jsonObject = new JSONObject(fileContext);
                JSONArray jsonArray = jsonObject.optJSONArray("slot");
                for (int i = 0 ; i < jsonArray.length();i++){
                    JSONObject binJson = (JSONObject) jsonArray.get(i);
                    String path = binJson.optString("path");
                    if(!FileUtils.isFileExists(path)){
                        Log.w(TAG,"File is not exist! binPath = " + path);
                        return false;
                    }
                    Log.w(TAG,"exist:"+path);
                }
                return true;
            } else {
                Log.w(TAG,"File is not exist! expandFnPath = "+expandFnPath);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }
}
