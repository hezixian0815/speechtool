package com.aispeech.export.config;

import com.aispeech.lite.base.BaseConfig;
import com.aispeech.lite.tts.CustomAudioBean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AILocalTTSConfig extends BaseConfig {

    /**
     * 方言选项，用于支持粤语、上海话、四川话等音色，默认为 0，可选。
     * 4-粤语；5-英语；6-法语；7-泰语；8-四川话；9-东北话；10-闽南语；11-德语
     */
    private int language = 0;

    /**
     * 是否使用缓存，default is {@value}
     */
    private boolean useCache = true;

    /**
     * TTS缓存目录，null 则为默认文件夹
     */
    private String cacheDirectory = null;

    /**
     * 设置是否开启cpu优化
     * 若某些机器合成速度慢，可以关闭cpu优化功能，设置为false
     *
     * @param enableOptimization 是否开启cpu优化, default is true
     */
    private boolean enableOptimization = true;

    /**
     * 设置合成字典
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.db</p>
     * <p>2. 如在 assets 里设置为名称 如：aitts_sent_dict_idx_2.0.4_20190215.db</p>
     */
    private String dictResource;

    /**
     * 用户自定义词典，用于修复离线合成问题，如多音字发音、停顿和数字字母符号读法错误等
     * <p>非必选</p>
     */
    private String userDictResource = null;

    /**
     * 设置 前端资源,包含文本归一化，分词的，韵律等
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     */
    private String frontBinResource;

    /**
     * 发音人资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     */
    private List<String> speakerResourceList = new ArrayList<>();

    /**
     * 发音人对应的MD5文件，assets 里的资源复制到sd卡使用时可以提供MD5进行匹配
     */
    private final Map<String, String> speakerResourceMD5Map = new HashMap<>();

    private boolean useStopCallback;
    private int cacheSize = 100;
    private int cacheWordCount = 200;
    private List<CustomAudioBean> customAudioList;

    public void setCacheDirectory(String cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    public String[] getBackResBinArray() {
        return (String[]) speakerResourceList.toArray();
    }

    public void setBackResBinArray(String[] backResBinArray) {
        this.speakerResourceList.clear();
        speakerResourceList.addAll(Arrays.asList(backResBinArray));
    }

    public boolean isUseStopCallback() {
        return useStopCallback;
    }

    public void setUseStopCallback(boolean useStopCallback) {
        this.useStopCallback = useStopCallback;
    }

    public int getCacheSize() {
        return cacheSize;
    }

    public void setCacheSize(int cacheSize) {
        this.cacheSize = cacheSize;
    }

    public int getCacheWordCount() {
        return cacheWordCount;
    }

    public void setCacheWordCount(int cacheWordCount) {
        this.cacheWordCount = cacheWordCount;
    }

    public List<CustomAudioBean> getCustomAudioList() {
        return customAudioList;
    }

    public void setCustomAudioList(List<CustomAudioBean> customAudioList) {
        this.customAudioList = customAudioList;
    }

    /**
     * 设置是否使用缓存，默认为true <br>
     * 缓存TTS缓存信息和音频文件，存放在应用外部缓存目录下的 ttsCache 文件夹下。
     *
     * @param useCache 是否使用缓存，默认为true
     */
    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
    }

    /**
     * 设置是否使用缓存和缓存的文件夹
     *
     * @param useCache       是否使用缓存，默认为true
     * @param cacheDirectory 缓存目录，设置为 null，则为默认缓存目录：应用外部缓存目录下的 ttsCache 文件夹
     */
    public void setUseCache(boolean useCache, String cacheDirectory) {
        this.useCache = useCache;
        this.cacheDirectory = cacheDirectory;
    }

    /**
     * 设置 发音人资源，若只需要一个发音人，则设置一个即可。设置多个时第1个即为使用的发音人
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param speakerResource 后端发音人资源名
     */
    public void addSpeakerResource(String[] speakerResource) {
        this.speakerResourceList.addAll(Arrays.asList(speakerResource));
    }

    public void addSpeakerResource(String speakerResource) {
        this.speakerResourceList.add(speakerResource);
    }

    /**
     * 设置assets目录下的后端发音人资源名和对应的md5文件，若只需要一个发音人，则设置一个即可，初始化时默认以第一个资源名加载进内核
     *
     * @param speakerResource       assets 目录下发音人资源名，sd 卡里的可用 {@link #addSpeakerResource(String[])}
     * @param speakerResourceMd5sum 对应的md5文件
     * @see #addSpeakerResource(String[])
     */
    public void addSpeakerResource(String[] speakerResource, String[] speakerResourceMd5sum) {
        if (speakerResource == null || speakerResourceMd5sum == null)
            return;
        if (speakerResource.length != speakerResourceMd5sum.length) {
            throw new RuntimeException("setSpeakerResourceListAndMd5File: length not the same");
        }

        for (int i = 0; i < speakerResource.length; i++) {
            this.speakerResourceList.add(speakerResource[i]);
            this.speakerResourceMD5Map.put(speakerResource[i], speakerResourceMd5sum[i]);
        }
    }

    public void clearSpeakerResourceAndMD5() {
        speakerResourceList.clear();
        speakerResourceMD5Map.clear();
    }


    /**
     * 设置是否开启cpu优化
     * 若某些机器合成速度慢，可以关闭cpu优化功能，设置为false
     *
     * @param enableOptimization 是否开启cpu优化, default is true
     */
    public void setEnableOptimization(boolean enableOptimization) {
        this.enableOptimization = enableOptimization;
    }

    public int getLanguage() {
        return language;
    }


    /**
     * 方言选项，用于支持粤语、上海话、四川话等音色，默认为 0，可选。
     *
     * @param language 4-粤语；5-英语；6-法语；7-泰语；8-四川话；9-东北话；10-闽南语；11-德语
     */
    public void setLanguage(int language) {
        this.language = language;
    }

    /**
     * 设置合成字典
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.db</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param dictResource 合成字典资源 如：aitts_sent_dict_idx_2.0.4_20190215.db
     */
    public void setDictResource(String dictResource) {
        this.dictResource = dictResource;
    }

    /**
     * 设置assets目录下的合成字典资源名字和对应的md5文件
     *
     * @param dictResource       合成字典文件名
     * @param dictResourceMd5sum 对应的md5文件
     */
    public void setDictResource(String dictResource, String dictResourceMd5sum) {
        this.dictResource = dictResource;
        speakerResourceMD5Map.put(dictResource, dictResourceMd5sum);
    }

    public String getUserDictResource() {
        return userDictResource;
    }

    /**
     * 用户自定义词典，用于修复离线合成问题，如多音字发音、停顿和数字字母符号读法错误等
     * <p>非必需,正常情況用不着，这个只有有发音问题需要紧急修复可以改这个资源。</p>
     *
     * @param userDictResource 自定义词典文件名或者文件路径
     */
    public void setUserDictResource(String userDictResource) {
        this.userDictResource = userDictResource;
    }

    /**
     * 设置 FrontBinResource,包含文本归一化，分词的，韵律等
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param frontBinResource 前端资源
     */
    public void setFrontBinResource(String frontBinResource) {
        this.frontBinResource = frontBinResource;
    }

    /**
     * 设置assets目录下的合成前端资源的名字和对应的md5文件,包含文本归一化，分词的，韵律等
     *
     * @param frontBinResource       资源文件名
     * @param frontBinResourceMd5sum 对应的md5文件
     */
    public void setFrontBinResource(String frontBinResource, String frontBinResourceMd5sum) {
        this.frontBinResource = frontBinResource;
        speakerResourceMD5Map.put(frontBinResource, frontBinResourceMd5sum);
    }


    public boolean isUseCache() {
        return useCache;
    }

    public String getCacheDirectory() {
        return cacheDirectory;
    }

    public List<String> getSpeakerResourceList() {
        return speakerResourceList;
    }

    public Map<String, String> getSpeakerResourceMD5Map() {
        return speakerResourceMD5Map;
    }

    public boolean isEnableOptimization() {
        return enableOptimization;
    }

    public String getDictResource() {
        return dictResource;
    }


    public String getFrontBinResource() {
        return frontBinResource;
    }

    public static final class Builder extends BaseConfig.Builder {
        private String frontResBin;
        private String[] backResBinArray;
        private boolean enableOptimization = true;
        private String dictDb;
        private String userDictResource;
        private boolean useCache = true;
        private boolean useStopCallback;
        private int cacheSize = 100;
        private int cacheWordCount = 200;
        private List<CustomAudioBean> customAudioList;
        private String cacheDirectory = null;
        private Map<String, String> speakerResourceMD5Map = new HashMap<>();
        /**
         * 方言选项，用于支持粤语、上海话、四川话等音色，默认为 0，可选。
         * 4 为选择粤语发 5 英语
         */
        private int language = 0;

        /**
         * 方言选项，用于支持粤语、上海话、四川话等音色，默认为 0，可选。
         *
         * @param language 4 为选择粤语  5 为粤语
         */
        public Builder setLanguage(int language) {
            this.language = language;
            return this;
        }

        public Builder setCacheDirectory(String cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
            return this;
        }

        /**
         * 设置assets目录下的合成前端资源，包含文本归一化，分词的，韵律等
         *
         * @param frontResBin 前端资源
         *                    需要在init之前设置生效
         * @return Builder.this
         */
        public Builder setFrontResBin(String frontResBin) {
            this.frontResBin = frontResBin;
            return this;
        }

        public Builder setFrontResBin(String frontResBin, String md5sum) {
            this.frontResBin = frontResBin;
            speakerResourceMD5Map.put(frontResBin, md5sum);
            return this;
        }

        /**
         * 设置assets目录下的后端发音人资源名，若只需要一个发音人，则设置一个即可，初始化时默认以第一个资源名加载进内核
         *
         * @param backResBinArray 后端发音人资源名
         *                        需要在init之前设置生效
         * @return Builder.this
         */
        public Builder setBackResBinArray(String[] backResBinArray) {
            this.backResBinArray = backResBinArray;
            return this;
        }


        /**
         * 设置是否开启cpu优化，默认开启为true。
         * 若某些机器合成速度慢，可以关闭cpu优化功能，设置为false
         *
         * @param enableOptimization cpu优化使能参数
         *                           需要在init之前设置生效
         * @return Builder.this
         */
        public Builder setEnableOptimization(boolean enableOptimization) {
            this.enableOptimization = enableOptimization;
            return this;
        }


        /**
         * 设置assets目录下的合成字典名字
         *
         * @param dictDb 合成字典文件路径
         *               需要在init之前设置生效
         * @return Builder.this
         */
        public Builder setDictDb(String dictDb) {
            this.dictDb = dictDb;
            return this;
        }

        public Builder setDictDb(String dictResource, String dictResourceMd5sum) {
            this.dictDb = dictResource;
            speakerResourceMD5Map.put(dictResource, dictResourceMd5sum);
            return this;
        }

        /**
         * 设置是否使用缓存，默认为true <br>
         * 缓存TTS缓存信息和音频文件，存放在应用外部缓存目录下的 ttsCache 文件夹下。
         *
         * @param useCache 是否使用缓存，默认为true
         * @return Builder.this
         */
        public Builder setUseCache(boolean useCache) {
            this.useCache = useCache;
            return this;
        }

        /**
         * 设置是否在stop之后回调 onSpeechFinish ,默认是true 回调
         *
         * @param useStopCallback stop后是否回调 onSpeechFinish ，需要在init之前设置生效
         * @return Builder.this
         */
        public Builder setUseStopCallback(boolean useStopCallback) {
            this.useStopCallback = useStopCallback;
            return this;
        }

        /**
         * 设置tts缓存数量上限,默认为100
         *
         * @param cacheSize 是否使用缓存，默认为true
         * @return Builder.this
         */
        public Builder setCacheSize(int cacheSize) {
            this.cacheSize = cacheSize;
            return this;
        }

        /**
         * 设置单次缓存最大支持的文本字数，默认限制为200
         *
         * @param wordCount 文字字数
         * @return Builder.this
         */
        public Builder setCacheWordCount(int wordCount) {
            this.cacheWordCount = wordCount;
            return this;
        }

        /**
         * 自定义外部录音，列表中的文本使用对应的录音文件播报
         *
         * @param customAudioList 自定义外部录音实体类列表
         * @return Builder.this
         */
        public Builder setCustomAudioList(List<CustomAudioBean> customAudioList) {
            this.customAudioList = customAudioList;
            return this;
        }

        @Override
        public Builder setTagSuffix(String tagSuffix) {
            return (Builder) super.setTagSuffix(tagSuffix);
        }

        public AILocalTTSConfig build() {
            AILocalTTSConfig aILocalTTSConfig = new AILocalTTSConfig();
            aILocalTTSConfig.dictResource = this.dictDb;
            aILocalTTSConfig.useCache = this.useCache;
            aILocalTTSConfig.enableOptimization = this.enableOptimization;
            aILocalTTSConfig.frontBinResource = this.frontResBin;
            if (backResBinArray != null) {
                aILocalTTSConfig.speakerResourceList.addAll(Arrays.asList(backResBinArray));
            }
            aILocalTTSConfig.useStopCallback = this.useStopCallback;
            aILocalTTSConfig.cacheSize = this.cacheSize;
            aILocalTTSConfig.cacheWordCount = this.cacheWordCount;
            aILocalTTSConfig.cacheDirectory = this.cacheDirectory;
            aILocalTTSConfig.userDictResource = this.userDictResource;
            aILocalTTSConfig.customAudioList = this.customAudioList;
            aILocalTTSConfig.speakerResourceMD5Map.putAll(this.speakerResourceMD5Map);
            aILocalTTSConfig.language = this.language;
            return super.build(aILocalTTSConfig);
        }

        @Override
        public String toString() {
            return "Builder{" +
                    "frontResBin='" + frontResBin + '\'' +
                    ", backResBinArray=" + Arrays.toString(backResBinArray) +
                    ", enableOptimization=" + enableOptimization +
                    ", dictDb='" + dictDb + '\'' +
                    ", useCache=" + useCache +
                    ", useStopCallback=" + useStopCallback +
                    ", cacheSize=" + cacheSize +
                    ", cacheWordCount=" + cacheWordCount +
                    ", cacheDirectory=" + cacheDirectory +
                    ", userDictResource=" + userDictResource +
                    ", customAudioList=" + customAudioList +
                    ", speakerResourceMD5Map=" + speakerResourceMD5Map +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "AILocalTTSConfig{" +
                "frontResBin='" + frontBinResource + '\'' +
                ", backResBinArray=" + speakerResourceList +
                ", enableOptimization=" + enableOptimization +
                ", dictDb='" + dictResource + '\'' +
                ", useCache=" + useCache +
                ", useStopCallback=" + useStopCallback +
                ", cacheSize=" + cacheSize +
                ", cacheWordCount=" + cacheWordCount +
                ", cacheDirectory=" + cacheDirectory +
                ", userDictResource=" + userDictResource +
                ", customAudioList=" + customAudioList +
                ", speakerResourceMD5Map=" + speakerResourceMD5Map +
                '}';
    }
}
