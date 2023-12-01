package com.aispeech.lite.tts;

import android.os.Environment;
import android.text.TextUtils;

import com.aispeech.common.FileUtils;
import com.aispeech.common.Log;
import com.aispeech.common.SharedPrefsUtil;
import com.aispeech.common.Util;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.param.TTSParams;

import org.apache.commons.collections4.map.LRUMap;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 缓存TTS缓存信息和音频文件，存放在应用外部缓存目录下的 ttsCache 文件夹下。<br>
 * 与云端TTS、本地TTS相对应分辨做缓存。
 */
public class TTSCache {

    private static final String TAG = "TTSCache";
    private static final String LRUMAP_FILE_NAME_PREFIX = "TTSCache_";
    private static final String DEFAULT_DIRECTORY = "ttsCache";
    private static final int TYPE_LOCAL = 0;
    private static final int TYPE_CLOUD = 1;
    private static final int TYPE_RETURNME = 2;

    private volatile static TTSCache instanceLocal = null;
    private volatile static TTSCache instanceCloud = null;
    private volatile static TTSCache instanceReturnMe = null;
    private volatile static TTSCache instanceReturnMeLocal = null;

    /**
     * 缓存 20 条音频文件
     */
    private LRUMap<String, String> mLruMap = new LRUMap<>(100);

    /**
     * 只支持200字符及以下文本缓存
     * 避免超大音频读取导致进程长期占用
     */
    public static int MAX_CACHE_TEXT_WORD_COUNT = 200;

    private int type = TYPE_LOCAL;
    private String infoFileName;
    private boolean useCache = true;
    private File dir = null;

    private TTSCache() {
        initDirectory();
    }

    public static TTSCache getInstanceLocal() {
        if (instanceLocal == null)
            synchronized (TTSCache.class) {
                if (instanceLocal == null) {
                    instanceLocal = new TTSCache();
                    instanceLocal.type = TYPE_LOCAL;
                    instanceLocal.infoFileName = LRUMAP_FILE_NAME_PREFIX + "local";
                }
            }
        return instanceLocal;
    }

    public static TTSCache getInstanceCloud() {
        if (instanceCloud == null)
            synchronized (TTSCache.class) {
                if (instanceCloud == null) {
                    instanceCloud = new TTSCache();
                    instanceCloud.type = TYPE_CLOUD;
                    instanceCloud.infoFileName = LRUMAP_FILE_NAME_PREFIX + "cloud";
                }
            }
        return instanceCloud;
    }

    public static TTSCache getInstanceReturnMe() {
        if (instanceReturnMe == null)
            synchronized (TTSCache.class) {
                if (instanceReturnMe == null) {
                    instanceReturnMe = new TTSCache();
                    instanceReturnMe.type = TYPE_RETURNME;
                    instanceReturnMe.infoFileName = LRUMAP_FILE_NAME_PREFIX + "returnme";
                    instanceReturnMe.restoreFromLocal();
                }
            }
        return instanceReturnMe;
    }

    public static TTSCache getInstanceReturnMeLocal() {
        if (instanceReturnMeLocal == null)
            synchronized (TTSCache.class) {
                if (instanceReturnMeLocal == null) {
                    instanceReturnMeLocal = new TTSCache();
                    instanceReturnMeLocal.type = TYPE_RETURNME;
                    instanceReturnMeLocal.infoFileName = LRUMAP_FILE_NAME_PREFIX + "localTtsreturnme";
                    instanceReturnMeLocal.restoreFromLocal();
                }
            }
        return instanceReturnMeLocal;
    }


    public File getCacheDirectory() {
        return dir;
    }

    /**
     * 保存tts 信息和 tts文件对应的路径
     *
     * @param ttsParams
     * @param wavFile
     */
    public boolean put(TTSParams ttsParams, File wavFile) {
        if (!useCache)
            return false;
        if (ttsParams == null || wavFile == null || !wavFile.exists() || !wavFile.isFile())
            return false;

        // 不支持缓存200字符以上长文本
        if (ttsParams.getRefText().length() > TTSCache.MAX_CACHE_TEXT_WORD_COUNT) {
            Log.i(TAG, "The cached text is greater than " + TTSCache.MAX_CACHE_TEXT_WORD_COUNT + " words");
            return false;
        }
        String md5 = Util.md5(ttsParams.toString());

        if (mLruMap.containsKey(md5)) {
            String lastPath = mLruMap.get(md5);
            if (wavFile.getPath().equals(lastPath)) {
                return false;
            }
            if (!TextUtils.isEmpty(lastPath)) {
                File f = new File(lastPath);
                if (f.exists() && f.isFile() && f.getParentFile() != null && f.getParentFile().equals(dir)) {
                    boolean deleteSuc = f.delete();
                    Log.d(TAG, "delete " + deleteSuc + " cache file " + f.getAbsolutePath());
                }
            }
        } else {
            if (mLruMap.isFull()) {
                String filePath = mLruMap.remove(mLruMap.firstKey());
                File f = new File(filePath);
                if (f.exists() && f.isFile() && f.getParentFile() != null && f.getParentFile().equals(dir)) {
                    boolean deleteSuc = f.delete();
                    Log.d(TAG, "delete " + deleteSuc + " cache file " + f.getAbsolutePath());
                }
            }
        }

        String cacheFilePath = copyIfNeed(wavFile, ttsParams.getSaveAudioFileName());
        Log.d(TAG, "add cache file " + cacheFilePath);
        if (!TextUtils.isEmpty(cacheFilePath))
            mLruMap.put(md5, cacheFilePath);
        return true;
    }

    private String copyIfNeed(File file, String audioFileName) {
        if (file == null || dir == null)
            return null;
        /**
         * fix：同一目录下，只会生成一个文件
         * 1. 如果只设置cache,没有在intent中设置saveAudioPath，TtsProcessor的createAudioFile中会先将文件写入缓存列表中，（mp3File = ttsCache.newMp3File();）
         * 判断如果是在一个文件夹：且只设置缓存，则直接返回路径，
         *
         * 2.如果设置cache,且intent中设置saveAudioPath，TtsProcessor的createAudioFile中会先将文件写入 intent设置的路径下（mp3File = new File(filePath)），
         *   如果cache和saveAudioPath是同一个路径，也会走到下面（file.getParentFile() != null && file.getParentFile().equals(dir)）逻辑中，需要复制一个文件，返回新的名称给缓存
         */
        //音频文件路径是否跟缓存在一个路径下
        if (file.getParentFile() != null && file.getParentFile().equals(dir)) {
            //如果intent中设置路径，则复制一个文件，文件夹下两个音频，一个是缓存使用，一个是intent中存储的
            if (!TextUtils.isEmpty(audioFileName)) {
                String newFile = copyFileToPath(file);
                return newFile;
            } else {
                return file.getAbsolutePath();
            }
        }
        String fileName = file.getName();
        int lastIndex = fileName.lastIndexOf(".");
        String suffix = lastIndex == -1 ? "" : fileName.substring(lastIndex);

        String newPath = dir.getAbsolutePath() + File.separator + System.currentTimeMillis() + suffix;
        FileUtils.copyFile(file.getAbsolutePath(), newPath);
        return newPath;
    }

    private String copyFileToPath(File file) {
        String fileName = file.getName();
        String newPath = dir.getAbsolutePath() + File.separator + System.currentTimeMillis() + "_" + fileName;
        FileUtils.copyFile(file.getAbsolutePath(), newPath);
        return newPath;
    }

    public File get(TTSParams ttsParams) {
        if (!useCache)
            return null;
        if (ttsParams == null)
            return null;

        String md5 = Util.md5(ttsParams.toString());

        String path = mLruMap.get(md5);
        if (TextUtils.isEmpty(path)) {
            mLruMap.remove(md5);
            return null;
        }

        File file = new File(path);
        if (file.exists() && file.isFile())
            return file;
        else {
            mLruMap.remove(md5);
            return null;
        }
    }

    public synchronized File newWavFile() {
        return useCache ? new File(dir, System.currentTimeMillis() + ".wav") : null;
    }

    public synchronized File newMp3File() {
        return useCache ? new File(dir, System.currentTimeMillis() + ".mp3") : null;
    }

    public synchronized File newReturnmeFile() {
        return useCache ? new File(dir, System.currentTimeMillis() + ".info") : null;
    }

    public void saveToLocal() {
        if (mLruMap.isEmpty())
            return;

        Map<String, String> map = new HashMap<>(mLruMap);

        JSONObject jsonObject = new JSONObject();
        Iterator<String> it = map.keySet().iterator();
        while (it.hasNext()) {
            String key = it.next();
            try {
                jsonObject.put(key, map.get(key));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String json = jsonObject.toString();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(new File(dir, infoFileName));
            fos.write(json.getBytes());
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void restoreFromLocal() {
        File file = new File(dir, infoFileName);
        if (!file.exists())
            return;

        StringBuffer sb = new StringBuffer();
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        String json = sb.toString();
        if (TextUtils.isEmpty(json))
            return;
        mLruMap.clear();
        try {
            JSONObject jsonObject = new JSONObject(json);
            Iterator<String> it = jsonObject.keys();
            while (it.hasNext()) {
                String key = it.next();
                String value = jsonObject.getString(key);
                mLruMap.put(key, value);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 是否使用了缓存功能
     *
     * @return true 使用，false 未使用
     */
    public synchronized boolean isUseCache() {
        return useCache;
    }

    public synchronized void setUseCache(boolean useCache) {
        setUseCache(useCache, null);
    }

    /**
     * 设置tts缓存数量上限,默认为100
     *
     * @param cacheSize 是否使用缓存，默认为true
     */
    public void setCacheSize(int cacheSize) {
        mLruMap.clear();
        mLruMap = new LRUMap<>(cacheSize);
        if (useCache) {
            restoreFromLocal();
        }
    }

    /**
     * 设置缓存目录，设置为 null 则使用默认目录
     *
     * @param useCache       是否使用缓存功能
     * @param cacheDirectory 设置缓存目录，设置为 null 则使用默认目录
     */
    public synchronized void setUseCache(boolean useCache, File cacheDirectory) {
        this.useCache = useCache;
        this.dir = cacheDirectory;
        initDirectory();

        if (this.useCache) {
            restoreFromLocal();
        } else {
            mLruMap.clear();
        }
    }

    private void initDirectory() {
        if (this.dir == null) {
            if (TextUtils.isEmpty(AISpeech.ttsCacheDir)) {
                if (Environment.isExternalStorageEmulated())
                    dir = new File(AISpeech.getContext().getExternalCacheDir(), DEFAULT_DIRECTORY);
                else
                    dir = new File(AISpeech.getContext().getCacheDir(), DEFAULT_DIRECTORY);
            } else {
                dir = new File(AISpeech.ttsCacheDir, DEFAULT_DIRECTORY);
            }
        }
        Log.d(TAG, "cache dir is " + dir.exists() + "      " + dir.isDirectory());
        if (!dir.exists() || !dir.isDirectory()) {
            boolean mkdir = dir.mkdirs();
            Log.d(TAG, "cache dir is mkdir : " + mkdir);
        }
        Log.d(TAG, "cache dir is " + dir.getAbsolutePath());
    }

    /**
     * 设置自定义播报音频
     *
     * @param customAudioList 自定义播报音频列表
     */
    public void setCustomAudioList(List<CustomAudioBean> customAudioList) {
        if (customAudioList == null || customAudioList.isEmpty()) {
            SharedPrefsUtil.putLocalTTSCustomAudio(AISpeech.getContext(), null);
            return;
        }
        JSONObject jsonObject = new JSONObject();
        for (CustomAudioBean customAudioBean : customAudioList) {
            try {
                jsonObject.put(customAudioBean.getText(), customAudioBean.getAudioPath());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Log.d(TAG, "setCustomAudioList() called with: customAudioList = " + jsonObject.toString());
        SharedPrefsUtil.putLocalTTSCustomAudio(AISpeech.getContext(), jsonObject.toString());
    }

    /**
     * 设置单次缓存最大支持的文本字数，默认限制为200
     *
     * @param wordCount 文字字数
     */
    public void setCacheWordCount(int wordCount) {
        MAX_CACHE_TEXT_WORD_COUNT = wordCount;
    }

    public String getCustomAudioPath(String text) {
        String ret = null;
        String customAudioJsonStr = SharedPrefsUtil.getLocalTTSCustomAudio(AISpeech.getContext());
        if (!TextUtils.isEmpty(customAudioJsonStr)) {
            try {
                JSONObject jsonObject = new JSONObject(customAudioJsonStr);
                ret = jsonObject.optString(text);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }
}
