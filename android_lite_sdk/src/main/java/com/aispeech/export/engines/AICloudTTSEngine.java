package com.aispeech.export.engines;

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.os.Build;

import com.aispeech.common.AIConstant;
import com.aispeech.export.config.AICloudTTSConfig;
import com.aispeech.export.intent.AICloudTTSIntent;
import com.aispeech.export.listeners.AITTSListener;
import com.aispeech.lite.config.CloudTtsConfig;
import com.aispeech.lite.param.CloudTtsParams;

@Deprecated
public class AICloudTTSEngine {
    public static final String TAG = "AICloudTTSEngine";
    private final CloudTtsConfig mConfig;
    private final CloudTtsParams mParams;
    private boolean useCache = true;
    private com.aispeech.export.engines2.AICloudTTSEngine ttsEngine2;

    private AICloudTTSEngine() {
        ttsEngine2 = com.aispeech.export.engines2.AICloudTTSEngine.createInstance();
        mParams = new CloudTtsParams();
        mConfig = new CloudTtsConfig();
    }


    /**
     * 获取云端合成引擎实例
     *
     * @return 云端tts引擎
     */
    public static AICloudTTSEngine createInstance() {
        return new AICloudTTSEngine();
    }


    /**
     * 初始化在线tts引擎
     *
     * @param listener 回调监听器
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @deprecated 已过时, 使用带AICloudTTSConfig参数的初始化方法
     */
    public void init(AITTSListener listener) {
        AICloudTTSConfig cloudTTSConfig = new AICloudTTSConfig.Builder()
                .setUseCache(useCache)
                .setUseStopCallback(mConfig.isUseStopCallback())
                .build();

        init(cloudTTSConfig, listener);
    }

    /**
     * 初始化在线tts引擎
     *
     * @param ttsConfig 引擎配置信息实体类
     * @param listener  回调监听器
     * @see #init(AICloudTTSConfig, AITTSListener)
     */
    public void init(AICloudTTSConfig ttsConfig, AITTSListener listener) {
        if (ttsEngine2 != null) {
            ttsEngine2.init(ttsConfig, listener);
        }
    }


    public void speak(String refText, String utteranceId) {
        speak(refText, utteranceId, parseParams());
    }

    public void speak(String refText, String utteranceId, AICloudTTSIntent intent) {
        if (ttsEngine2 != null) {
            ttsEngine2.speak(intent, refText, utteranceId);
        }
    }

    /**
     * 只合成，不播放，同时抛出实时合成音频流
     *
     * @param refText     合成文本
     * @param utteranceId utteranceId
     * @deprecated 已过时 {@link #synthesize(String, String, AICloudTTSIntent)}
     */
    public void synthesize(String refText, String utteranceId) {
        synthesize(refText, utteranceId, parseParams());
    }

    /**
     * 只合成，不播放，同时抛出实时合成音频流
     *
     * @param refText     合成文本
     * @param utteranceId utteranceId
     * @param intent      合成意图参数
     */
    public void synthesize(String refText, String utteranceId, AICloudTTSIntent intent) {
        if (ttsEngine2 != null) {
            ttsEngine2.synthesize(intent, refText, utteranceId);
        }
    }

    public void stop() {
        if (ttsEngine2 != null) {
            ttsEngine2.stop();
        }
    }

    public void pause() {
        if (ttsEngine2 != null) {
            ttsEngine2.pause();
        }
    }

    public void resume() {
        if (ttsEngine2 != null) {
            ttsEngine2.resume();
        }
    }

    public void release() {
        if (ttsEngine2 != null) {
            ttsEngine2.destroy();
        }
    }

    private AICloudTTSIntent parseParams() {
        AICloudTTSIntent intent = new AICloudTTSIntent();
        intent.setRealBack(mParams.isRealBack());
        intent.setSpeaker(mParams.getSpeaker());
        intent.setAudioType(mParams.getAudioType());
        intent.setMp3Quality(mParams.getMp3Quality());
        intent.setTextType(mParams.getTextType());
        intent.setVolume(mParams.getVolume());
        intent.setSpeed(mParams.getSpeed());
        intent.setSaveAudioPath(mParams.getSaveAudioPath());
        intent.setServer(mParams.getServer());
        intent.setStreamType(mParams.getStreamType());
        intent.setAudioAttributes(mParams.getAudioAttributes());
        intent.setUseStreamType(mParams.isUseStreamType());

        return intent;
    }

    /**
     * 设置是否开启实时反馈，默认开启为true
     * 须在speak之前设置才生效
     *
     * @param realBack realBack
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     */
    public void setRealBack(boolean realBack) {
        mParams.setRealBack(realBack);
    }


    /**
     * 设置是否使用缓存，默认为true <br>
     * 缓存TTS缓存信息和音频文件，存放在应用外部缓存目录下的 ttsCache 文件夹下。
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @param useCache 是否使用缓存，默认为true
     */
    public void setUseCache(boolean useCache) {
//        if (ttsEngine2 != null) {
//            ttsEngine2.setUseCache(useCache);
//        }
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

    /**
     * 设置合成音类型
     * 须在speak之前设置才生效
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @param speaker 例如：zhilingf
     */
    public void setSpeaker(String speaker) {
        mParams.setSpeaker(speaker);
    }


    /**
     * 设置合成音频格式，支持mp3{@link AIConstant#TTS_AUDIO_TYPE_MP3}
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @param audioType 合成音频格式
     *                  须在speak之前设置才生效
     */
    public void setAudioType(String audioType) {
        mParams.setAudioType(audioType);
    }

    /**
     * 设置云端合成mp3码率，支持low{@link AIConstant#TTS_MP3_QUALITY_LOW}
     * 和high{@link AIConstant#TTS_MP3_QUALITY_HIGH}，默认为low码率
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @param mp3Quality 云端合成mp3码率
     *                   须在speak之前设置才生效，且只在合成音频格式为mp3前提下才有效,详见{@link AICloudTTSEngine#setAudioType(String)}
     */
    public void setMP3Quality(String mp3Quality) {
        mParams.setMp3Quality(mp3Quality);
    }

    /**
     * 设置合成的文本类型
     * 须在speak之前设置才生效
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @param type text or ssml
     */
    public void setTextType(String type) {
        mParams.setTextType(type);
    }

    /**
     * 设置音量大小
     * 须在speak之前设置才生效
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @param volume 1-100, 100声音最响
     */
    public void setVolume(String volume) {
        mParams.setVolume(volume);
    }

    /**
     * 设置合成音语速
     * 须在speak之前设置才生效
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @param speed 0.5-2, 0.5语速最快
     */
    public void setSpeed(String speed) {
        mParams.setSpeed(speed);
    }


    /**
     * 设置合成音的保存路径
     * 须须在speak之前设置才生效
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @param filePath 文件路径字符串
     */
    public void setAudioPath(String filePath) {
        mParams.setSaveAudioPath(filePath);
    }


    /**
     * 设置云端合成请求地址
     * 须在speak之前设置才生效
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @param server 合成服务访问地址，默认为 "https://tts.dui.ai/runtime/v2/synthesize"
     */
    public void setServer(String server) {
        mParams.setServer(server);
    }


    /**
     * 设置音频流通道
     * 须在speak之前设置才生效
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @param streamType streamType,默认为{@link android.media.AudioManager#STREAM_MUSIC}
     */
    public void setStreamType(int streamType) {
        mParams.setStreamType(streamType);
    }

    /**
     * @param audioAttributes 音频属性:指定播放来源的原因，并控制导向、焦点和音量决策
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void setAudioAttributes(AudioAttributes audioAttributes) {
        mParams.setAudioAttributes(audioAttributes);
    }

    /**
     * 设置是否自定义播报SteamType
     *
     * @param useStreamType 默认是false
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     */
    public void setUseStreamType(boolean useStreamType) {
        mParams.setUseStreamType(useStreamType);
    }

    /**
     * 设置是否在stop之后回调 onSpeechFinish ,默认是true 回调
     *
     * @param useStopCallback stop后是否回调 onSpeechFinish ，需要在init之前设置生效
     * @see #init(AICloudTTSConfig, AITTSListener)
     * @deprecated 已过时, 使用AICloudTTSConfig统一配置
     */
    public void setUseStopCallback(boolean useStopCallback) {
        mConfig.setUseStopCallback(useStopCallback);
    }

}
