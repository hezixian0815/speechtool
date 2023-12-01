package com.aispeech.export.engines;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.text.TextUtils;

import com.aispeech.export.config.AILocalTTSConfig;
import com.aispeech.export.intent.AILocalTTSIntent;
import com.aispeech.export.listeners.AILocalTTSListener;
import com.aispeech.export.listeners.AITTSListener;
import com.aispeech.kernel.Cntts;
import com.aispeech.lite.config.LocalTtsConfig;
import com.aispeech.lite.param.LocalTtsParams;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地合成
 *
 * @deprecated replaced by {@link com.aispeech.export.engines2.AILocalTTSEngine}
 */
@Deprecated
public class AILocalTTSEngine {
    public static final String TAG = "AILocalTTSEngine";

    private com.aispeech.export.engines2.AILocalTTSEngine ttsEngine2;
    private LocalTtsParams mParams;
    private LocalTtsConfig mConfig;

    private final List<String> assetsResList = new ArrayList<>();

    private String mDictDbName = "";
    private String mDictDbPath = "";

    private String mFrontBin = "";
    private String mFrontBinPath = "";

    private String mBackBin = "";
    private String mBackBinPath = "";

    private boolean useCache = true;
    private String[] backResBinArray;
    private final AtomicInteger ref;
    private static boolean singleInstanceState;

    private AILocalTTSEngine() {
        ref = new AtomicInteger(0);

        if (ttsEngine2 == null)
            ttsEngine2 = com.aispeech.export.engines2.AILocalTTSEngine.createInstance();

        if (singleInstanceState) {
            addReference();
        } else {
            mParams = new LocalTtsParams();
            mConfig = new LocalTtsConfig();
        }
    }

    private static AILocalTTSEngine mInstance;

    /**
     * LocalTTS 请使用creatInstance 支持多实例调用
     */
    @Deprecated
    public static AILocalTTSEngine getInstance() {
        if (mInstance == null) {
            synchronized (AILocalTTSEngine.class) {
                if (mInstance == null) {
                    mInstance = new AILocalTTSEngine();
                }
            }
        }
        singleInstanceState = true;
        return mInstance;
    }


    public static AILocalTTSEngine createInstance() {
        singleInstanceState = false;
        return mInstance = new AILocalTTSEngine();
    }

    public static boolean checkLibValid() {
        return Cntts.isSoValid();
    }

    /**
     * 设置assets目录下的合成前端资源，包含文本归一化，分词的，韵律等
     *
     * @param frontResBin 前端资源
     *                    需要在init之前设置生效
     * @deprecated 已过时, 使用AILocalTTSConfig统一配置 {@link #init(AILocalTTSConfig, AITTSListener)}
     */
    @Deprecated
    public void setFrontResBin(String frontResBin) {
        this.mFrontBin = frontResBin;
    }

    /**
     * 设置assets目录下的合成前端资源的名字和对应的md5文件,包含文本归一化，分词的，韵律等
     *
     * @param frontResBin           资源文件名
     * @param frontResBinMd5sumName 对应的md5文件
     *                              需要在init之前设置生效
     */
    @Deprecated
    public void setFrontResBin(String frontResBin, String frontResBinMd5sumName) {
        setFrontResBin(frontResBin);
    }


    /**
     * 设置合成前端资源的自定义路径，比如/sdcard/front.bin，包含文本归一化，分词的，韵律等
     *
     * @param frontResBinPath 前端资源
     *                        需要在init之前设置生效
     * @deprecated 已过时, 使用AILocalTTSConfig统一配置 {@link #init(AILocalTTSConfig, AITTSListener)}
     */
    @Deprecated
    public void setFrontResBinPath(String frontResBinPath) {
        this.mFrontBinPath = frontResBinPath;
    }


    /**
     * 设置assets目录下的后端发音人资源名，若只需要一个发音人，则设置一个即可，初始化时默认以第一个资源名加载进内核
     *
     * @param backResBinArray 后端发音人资源名
     *                        需要在init之前设置生效
     * @deprecated 已过时, 使用AILocalTTSConfig统一配置 {@link #init(AILocalTTSConfig, AITTSListener)}
     */
    @Deprecated
    public void setBackResBinArray(String[] backResBinArray) {
        this.backResBinArray = backResBinArray;
        for (int i = 0; i < backResBinArray.length; i++) {
            if (i == 0) {
                mBackBin = backResBinArray[i];
            }
            assetsResList.add(backResBinArray[i]);
        }
    }

    /**
     * 设置assets目录下的后端发音人资源名和对应的md5文件，若只需要一个发音人，则设置一个即可，初始化时默认以第一个资源名加载进内核
     *
     * @param backResBinArray       后端发音人资源名
     * @param backResBinMd5sumArray 对应的md5文件
     *                              需要在init之前设置生效
     */
    @Deprecated
    public void setBackResBinArray(String[] backResBinArray, String[] backResBinMd5sumArray) {
        this.setBackResBinArray(backResBinArray);
    }

    /**
     * 动态设置assets目录下发音人资源名，只适用于在合成前调用，init前调用无效。且设置的资源名已经在init前setBackResBinArray设置过
     *
     * @param backResBin 需要在合成之前设置生效
     * @deprecated 已过使用, 使用AILocalTTSIntent统一配置 {@link #speak(String, String, AILocalTTSIntent)}
     */
    @Deprecated
    public void setBackResBin(String backResBin) {
        this.mBackBin = backResBin;
        if (ttsEngine2 != null) {
//            ttsEngine2.setBackResBinPath(Util.getResourceDir(AISpeech.getContext()) + File.separator + backResBin);
        }
    }


    /**
     * 设置是否开启cpu优化，默认开启为true。
     * 若某些机器合成速度慢，可以关闭cpu优化功能，设置为false
     *
     * @param enableOptimization cpu优化使能参数
     *                           需要在init之前设置生效
     * @deprecated 已过时, 使用AILocalTTSConfig统一配置 {@link #init(AILocalTTSConfig, AITTSListener)}
     */
    @Deprecated
    public void setEnableOptimization(boolean enableOptimization) {
        mConfig.setEnableOptimization(enableOptimization);
    }

    /**
     * 设置后端发音人资源的自定义路径，比如/sdcard/back.bin,只适用于在合成前调用,init前调用无效。且设置的资源名已经在init前setBackResBinArray设置过
     *
     * @param backResBinPath 后端发音人资源的自定义路径
     * @deprecated 已过使用, 使用AILocalTTSIntent统一配置 {@link #speak(String, String, AILocalTTSIntent)}
     */
    @Deprecated
    public void setBackResBinPath(String backResBinPath) {
        this.mBackBinPath = backResBinPath;
        if (ttsEngine2 != null) {
//            ttsEngine2.setBackResBinPath(backResBinPath);
        }
    }

    /*
     * 删除TTS本地资源方法 ，可以在onInit回调失败时调用，一般是相关bin资源在assets下时调用，
     * 若是外置目录需注意需要重新copy进去。
     *
     * 1. 若相关bin资源是放置在assets目录下，则删除为：
     *  setFrontResBin
     *  setBackResBinArray
     *  setDictDb
     *  方法设置的资源(路径：data/data/包名/files/)
     *
     * 2. 若相关bin资源放置在外部目录下，则删除为：
     *  setFrontResBinPath
     *  setDictDbPath
     *  setBackResBinPath
     *
     */
    public void deleteLocalResFile() {
        if (ttsEngine2 != null) {
            ttsEngine2.deleteLocalResFile();
        }
    }


    /**
     * 设置assets目录下的合成资源名字
     *
     * @param modelName 合成资源文件名
     *                  需要在init之前设置生效
     */
    @Deprecated
    public void setRes(String modelName) {
//        this.mModelName = modelName;
    }

    /**
     * 设置assets目录下的合成资源名字和对应的md5文件
     *
     * @param modelName       资源文件名
     * @param modelMd5sumName 对应的md5文件
     *                        需要在init之前设置生效
     */
    @Deprecated
    public void setRes(String modelName, String modelMd5sumName) {
//        this.mModelName = modelName;
//        mResMd5Map.put(modelName, modelMd5sumName);
    }

    /**
     * 设置合成资源的自定义路径，比如/sdcard/a.bin
     *
     * @param modelPath 合成资源自定义路径，包含文件名
     *                  需要在init之前设置生效
     */
    @Deprecated
    public void setResPath(String modelPath) {
//        this.mModelPath = modelPath;
    }

    /**
     * 设置assets目录下的合成字典名字
     *
     * @param dictDbName 合成字典文件名
     *                   需要在init之前设置生效
     * @deprecated 已过时, 使用AILocalTTSConfig统一配置 {@link #init(AILocalTTSConfig, AITTSListener)}
     */
    @Deprecated
    public void setDictDb(String dictDbName) {
        this.mDictDbName = dictDbName;
    }

    /**
     * 设置assets目录下的合成字典名字和对应的md5文件
     *
     * @param dictDbName      合成字典文件名
     * @param dicDbMd5sumName 对应的md5文件
     *                        需要在init之前设置生效
     */
    @Deprecated
    public void setDictDb(String dictDbName, String dicDbMd5sumName) {
        setDictDb(dictDbName);
    }

    /**
     * 设置合成字典的自定义路径
     *
     * @param dictDbPath 合成字典的自定义路径，包含文件名
     *                   需要在init之前设置生效
     * @deprecated 已过时, 使用AILocalTTSConfig统一配置 {@link #init(AILocalTTSConfig, AITTSListener)}*
     */
    @Deprecated
    public void setDictDbPath(String dictDbPath) {
        this.mDictDbPath = dictDbPath;
    }

    /**
     * 设置语音合成的速度
     *
     * @param speechRate 合成语速 范围为0.5～2.0
     *                   需要在start之前设置生效
     * @deprecated 已过使用, 使用AILocalTTSIntent统一配置 {@link #speak(String, String, AILocalTTSIntent)}
     */
    @Deprecated
    public void setSpeechRate(float speechRate) {
        mParams.setSpeed(speechRate);
    }

    /**
     * 设置语音合成的音量
     *
     * @param speechVolume 合成音量 范围为1～500
     *                     需要在start之前设置生效
     * @deprecated 已过使用, 使用AILocalTTSIntent统一配置 {@link #speak(String, String, AILocalTTSIntent)}
     */
    @Deprecated
    public void setSpeechVolume(int speechVolume) {
        mParams.setVolume(speechVolume);
    }

    /**
     * 设置是否使用ssml 默认不使用为false
     *
     * @param useSSML 是否配置ssml
     *                需要在start之前设置生效
     * @deprecated 已过使用, 使用AILocalTTSIntent统一配置 {@link #speak(String, String, AILocalTTSIntent)}
     */
    @Deprecated
    public void setUseSSML(boolean useSSML) {
        mParams.setUseSSML(useSSML);
    }

    /**
     * 设置播放器的stream type,默认为{@link AudioManager#STREAM_MUSIC}
     *
     * @param streamType audioTrack播放stream type
     *                   需要在start之前设置生效
     * @deprecated 已过使用, 使用AILocalTTSIntent统一配置 {@link #speak(String, String, AILocalTTSIntent)}
     */
    @Deprecated
    public void setStreamType(int streamType) {
        mParams.setStreamType(streamType);
    }

    /**
     * @param audioAttributes 音频属性:指定播放来源的原因，并控制导向、焦点和音量决策
     * @deprecated 已过使用, 使用AILocalTTSIntent统一配置 {@link #speak(String, String, AILocalTTSIntent)}
     */
    @Deprecated
    @TargetApi(Build.VERSION_CODES.M)
    public void setAudioAttributes(AudioAttributes audioAttributes) {
        mParams.setAudioAttributes(audioAttributes);
    }

    /**
     * 设置是否强制使用StreamType
     *
     * @param useStreamType 默认是false
     * @deprecated 已过使用, 使用AILocalTTSIntent统一配置 {@link #speak(String, String, AILocalTTSIntent)}
     */
    @Deprecated
    public void setUseStreamType(boolean useStreamType) {
        mParams.setUseStreamType(useStreamType);
    }

    /**
     * 设置是否在stop之后回调 onSpeechFinish ,默认是true 回调
     *
     * @param useStopCallback stop后是否回调 onSpeechFinish ，需要在init之前设置生效
     * @deprecated 已过时, 使用AILocalTTSConfig统一配置 {@link #init(AILocalTTSConfig, AITTSListener)}
     */
    public void setUseStopCallback(boolean useStopCallback) {
        mConfig.setUseStopCallback(useStopCallback);
    }

    /**
     * 初始化合成引擎,请参考使用 {@link #init(AITTSListener)}
     *
     * @param listener 合成回调接口
     * @see #init(AITTSListener)
     */
    @Deprecated
    public void init(AILocalTTSListener listener) {
        AILocalTTSConfig config = new AILocalTTSConfig.Builder()
                .setBackResBinArray(backResBinArray)
                .setDictDb(mDictDbName)
                .setEnableOptimization(mConfig.isEnableOptimization())
                .setFrontResBin(TextUtils.isEmpty(mFrontBin) ? mFrontBinPath : mFrontBin)
                .setUseCache(useCache)
                .setUseStopCallback(mConfig.isUseStopCallback())
                .build();
        init(config, listener);
    }

    /**
     * 初始化合成引擎,请参考使用 {@link #init(AITTSListener)}
     *
     * @param listener 合成回调接口
     * @param config   AILocalTTSConfig 初始化参数配置实体类
     * @see #init(AITTSListener)
     */
    @Deprecated
    public void init(AILocalTTSConfig config, AILocalTTSListener listener) {
        if (ttsEngine2 != null) {
            ttsEngine2.init(config, listener);
        }
    }

    /**
     * 初始化合成引擎
     *
     * @param listener 合成回调接口
     * @deprecated 已过时 {@link #init(AILocalTTSConfig, AITTSListener)}
     */
    @Deprecated
    public void init(AITTSListener listener) {
        AILocalTTSConfig config = new AILocalTTSConfig.Builder()
                .setBackResBinArray(backResBinArray)
                .setDictDb(mDictDbName)
                .setEnableOptimization(mConfig.isEnableOptimization())
                .setFrontResBin(TextUtils.isEmpty(mFrontBin) ? mFrontBinPath : mFrontBin)
                .setUseCache(useCache)
                .setUseStopCallback(mConfig.isUseStopCallback())
                .build();
        init(config, listener);
    }

    /**
     * 初始化合成引擎
     *
     * @param config   localTTS初始化参数配置实体类
     * @param listener 合成回调接口
     */
    public void init(AILocalTTSConfig config, AITTSListener listener) {
        if (ttsEngine2 != null) {
            ttsEngine2.init(config, listener);
        }
    }

    private synchronized void addReference() {
        int count = ref.incrementAndGet();
        if (count <= 0)
            ref.set(1);

        if (mParams == null)
            mParams = new LocalTtsParams();
        if (mConfig == null)
            mConfig = new LocalTtsConfig();
    }


    /**
     * 合成并播放
     *
     * @param refText     合成文本
     * @param utteranceId utteranceId
     * @deprecated 已过时 {@link #speak(String, String, AILocalTTSIntent)}
     */
    public void speak(String refText, String utteranceId) {
        if (ttsEngine2 != null) {
            ttsEngine2.speak(parseParams(), refText, utteranceId);
        }
    }

    /**
     * 合成并播放
     *
     * @param refText     合成文本
     * @param utteranceId utteranceId
     * @param intent      合成前可动态配置的参数实体类
     */
    public void speak(String refText, String utteranceId, AILocalTTSIntent intent) {
        if (ttsEngine2 != null) {
            ttsEngine2.speak(intent, refText, utteranceId);
        }
    }


    /**
     * 暂停播放
     */
    public void pause() {
        if (ttsEngine2 != null) {
            ttsEngine2.pause();
        }
    }

    /**
     * 继续播放
     */
    public void resume() {
        if (ttsEngine2 != null) {
            ttsEngine2.resume();
        }
    }

    /**
     * 停止合成和播放
     */
    public void stop() {
        if (ttsEngine2 != null) {
            ttsEngine2.stop();
        }
    }


    /**
     * 只合成，不播放，同时抛出实时合成音频流
     *
     * @param text        合成文本
     * @param utteranceId utteranceId
     * @deprecated 已过时 {@link #synthesize(String, String, AILocalTTSIntent)}
     */
    public void synthesize(String text, String utteranceId) {
        if (ttsEngine2 != null) {
            ttsEngine2.synthesize(parseParams(), text, utteranceId);
        }
    }

    /**
     * 只合成，不播放，同时抛出实时合成音频流
     *
     * @param text        合成文本
     * @param utteranceId utteranceId
     * @param intent      合成前可动态配置的参数实体类
     */
    public void synthesize(String text, String utteranceId, AILocalTTSIntent intent) {
        if (ttsEngine2 != null) {
            ttsEngine2.synthesize(intent, text, utteranceId);
        }
    }

    /**
     * 合成音频到某个文件
     *
     * @param text        合成文本
     * @param fileName    保存的合成音频文件，包含路径
     * @param utteranceId utteranceId
     * @deprecated 已过时 {@link #synthesizeToFile(String, String, String, AILocalTTSIntent)}
     */
    @Deprecated
    public void synthesizeToFile(String text, String fileName, String utteranceId) {
        if (ttsEngine2 != null) {
            ttsEngine2.synthesizeToFile(text, fileName, utteranceId, parseParams());
        }
    }

    private AILocalTTSIntent parseParams() {
        AILocalTTSIntent intent = new AILocalTTSIntent();
        intent.setSpeed(mParams.getSpeed());
        intent.setVolume(mParams.getVolume());
        intent.setUseSSML(mParams.isUseSSML());
        intent.setStreamType(mParams.getStreamType());
        intent.setAudioAttributes(mParams.getAudioAttributes());
        intent.setUseStreamType(mParams.isUseStreamType());
        intent.setSaveAudioFilePath(mParams.getSaveAudioFileName());

        return intent;
    }

    /**
     * 合成音频到某个文件
     *
     * @param text        合成文本
     * @param fileName    保存的合成音频文件，包含路径
     * @param utteranceId utteranceId
     * @param intent      合成前可动态配置的参数实体类
     */
    public void synthesizeToFile(String text, String fileName, String utteranceId, AILocalTTSIntent intent) {
        if (ttsEngine2 != null) {
            ttsEngine2.synthesizeToFile(text, fileName, utteranceId, intent);
        }
    }


    /**
     * 设置合成的音频的全路径包含文件名
     * 须在start之前设置才生效
     *
     * @param fileName fileName
     *                 需要在start之前设置生效
     * @deprecated 已过使用, 使用AILocalTTSIntent统一配置 {@link #speak(String, String, AILocalTTSIntent)}
     */
    @Deprecated
    public void setSaveAudioFileName(String fileName) {
        mParams.setSaveAudioFileName(fileName);
    }

    /**
     * 销毁合成引擎
     */
    public void destroy() {

        if (ttsEngine2 != null) {
            ttsEngine2.destroy();
        }
        if (assetsResList != null) {
            assetsResList.clear();
        }

        mInstance = null;
        mDictDbName = null;
        mDictDbPath = null;
        mFrontBin = null;
        mFrontBinPath = null;
        mBackBin = null;
        mBackBinPath = null;
    }

    /**
     * 设置是否使用缓存，默认为true <br>
     * 缓存TTS缓存信息和音频文件，存放在应用外部缓存目录下的 ttsCache 文件夹下。
     *
     * @param useCache 是否使用缓存，默认为true
     *                 parse     * @deprecated 已过时,使用AILocalTTSConfig统一配置 {@link #init(AILocalTTSConfig, AITTSListener)}
     */
    public void setUseCache(boolean useCache) {
        this.useCache = useCache;
        if (ttsEngine2 != null) {
            ttsEngine2.setUseCache(useCache);
        }
    }

    /**
     * 是否使用了缓存功能
     *
     * @return true 使用，false 未使用
     */
    public boolean isUseCache() {
        if (ttsEngine2 != null) {
            return ttsEngine2.isUseCache();
        }
        return useCache;
    }

}
