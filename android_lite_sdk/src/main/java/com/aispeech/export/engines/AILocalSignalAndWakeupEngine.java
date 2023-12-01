package com.aispeech.export.engines;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.base.IFespxEngine;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.listeners.AILocalSignalAndWakeupListener;
import com.aispeech.kernel.Fespa;
import com.aispeech.kernel.Fespd;
import com.aispeech.kernel.Fespl;
import com.aispeech.kernel.fespCar;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.config.LocalSignalProcessingConfig;
import com.aispeech.lite.fespx.FespxProcessor;
import com.aispeech.lite.param.SignalProcessingParams;
import com.aispeech.lite.speech.SpeechListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 多麦唤醒
 *
 * @deprecated replaced by {@link com.aispeech.export.engines2.AILocalSignalAndWakeupEngine}
 */
@Deprecated
public class AILocalSignalAndWakeupEngine implements IFespxEngine {
    public static final String TAG = "AILSAWEngine";
    private FespxProcessor mFespxProcessor;
    private LocalSignalProcessingConfig mConfig;
    private SignalProcessingParams mParam;
    private SpeechListenerImpl mSpeechListener;


    private String aecBinPath = "";
    private String aecResName = "";
    private String wakeupBinPath = "";
    private String wakeupResName = "";
    private String beamformingBinPath = "";
    private String beamformingResName = "";
    private boolean mIsDisableAec = false;


    private AILocalSignalAndWakeupEngine() {
        mConfig = new LocalSignalProcessingConfig();
        mParam = new SignalProcessingParams();
        mSpeechListener = new SpeechListenerImpl(null);
        mFespxProcessor = new FespxProcessor();
    }


    public static AILocalSignalAndWakeupEngine createInstance() {
        return new AILocalSignalAndWakeupEngine();
    }


    @Override
    public FespxProcessor getFespxProcessor() {
        return this.mFespxProcessor;
    }


    public static boolean checkLibValid() {
        Log.d(TAG, "RecoderType " + AISpeech.getRecoderType());
        return Fespl.isFespxSoValid() || Fespa.isFespxSoValid() || fespCar.isFespxSoValid() || Fespd.isFespxSoValid();
    }


    public void init(AILocalSignalAndWakeupListener listener) {
        List<String> assetsResList = new ArrayList<>();
        mSpeechListener.setListener(listener);
        if (mIsDisableAec) {
            mConfig.setAecBinPath("OFF");
        } else {
            if (TextUtils.isEmpty(aecBinPath) && !TextUtils.isEmpty(aecResName)) {
                assetsResList.add(aecResName);
                mConfig.setAecBinPath(Util.getResPath(AISpeech.getContext(), aecResName));
            } else {
                mConfig.setAecBinPath(aecBinPath);
            }
        }
        if (TextUtils.isEmpty(beamformingBinPath) && !TextUtils.isEmpty(beamformingResName)) {
            assetsResList.add(beamformingResName);
            mConfig.setBeamformingBinPath(Util.getResPath(AISpeech.getContext(), beamformingResName));
        } else {
            mConfig.setBeamformingBinPath(beamformingBinPath);
        }
        if (TextUtils.isEmpty(wakeupBinPath) && !TextUtils.isEmpty(wakeupResName)) {
            assetsResList.add(wakeupResName);
            mConfig.setWakupBinPath(Util.getResPath(AISpeech.getContext(), wakeupResName));
        } else {
            mConfig.setWakupBinPath(TextUtils.isEmpty(wakeupBinPath) ? "OFF" : wakeupBinPath);
        }
        mConfig.setAssetsResNames(assetsResList.toArray(new String[assetsResList.size()]));
        mFespxProcessor.init(mSpeechListener, mConfig);
    }


    /**
     * 启动信号处理模块和唤醒引擎
     */
    public void start() {
        if (mFespxProcessor != null) {
            mFespxProcessor.start(mParam);
        }
    }

    /**
     * 设置dump唤醒音频保存的文件夹，比如/sdcard/speech/dumpwkp。不设置则不dump音频
     *
     * @param dumpAudioPath dump唤醒音频保存的文件夹
     */
    public void setDumpWakeupAudioPath(String dumpAudioPath) {
        mParam.setDumpAudioPath(dumpAudioPath);
    }

    /**
     * 设置dump唤醒点回退音频的时间，默认5000ms。
     *
     * @param dumpTime dump唤醒点回退音频的时长
     */
    public void setDumpWakeupTime(int dumpTime) {
        mParam.setDumpTime(dumpTime);
    }

    /**
     * 设置唤醒env热更新/nlms模式切换，可以在引擎初始化成功后动态设置
     *
     * @param setJson setJson
     */
    public void set(JSONObject setJson) {
        if (mFespxProcessor != null) {
            mFespxProcessor.set(setJson.toString());
        }
    }

    /**
     * 获取 驾驶模式，只有车载双麦模块有这个功能
     *
     * @return 0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式，-1 错误，没有获取到
     */
    public synchronized int getDriveMode() {
        return mFespxProcessor != null ? mFespxProcessor.getDriveMode() : -1;
    }

    /**
     * 停止信号处理模块和唤醒引擎
     * 该方法会停止接收录音数据和停止信号处理，唤醒，程序退出时可以调用
     */
    public void stop() {
        if (mFespxProcessor != null) {
            mFespxProcessor.stop();
        }
    }


    /**
     * 设置启用麦克风阵列类型。默认以录音机类型进行加载相应的模块
     *
     * @param type 麦克风阵列类型
     */
    public void setMicType(int type) {
        mConfig.setMicType(type);
    }

    /**
     * 销毁信号处理模块和唤醒引擎
     * 该方法会停止录音机和销毁录音机
     */
    public void destroy() {
        if (mFespxProcessor != null) {
            mFespxProcessor.release();
            mFespxProcessor = null;
        }
        if (mSpeechListener != null) {
            mSpeechListener.setListener(null);
            mSpeechListener = null;
        }
        mParam = null;
        mConfig = null;
        aecResName = null;
        aecBinPath = null;
        beamformingResName = null;
        beamformingBinPath = null;
        wakeupResName = null;
        wakeupBinPath = null;
        mIsDisableAec = false;
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     * @param size 数据大小
     * @see #setUseCustomFeed(boolean)
     */
    public void feedData(byte[] data, int size) {
        if (mParam != null && !mParam.isUseCustomFeed())
            return;
        if (mFespxProcessor != null) {
            mFespxProcessor.feedData(data, size);
        }
    }


    /**
     * oneshot回退的时间，单位为ms(只有主唤醒词才会回退音频,即major为1)
     * 须在init之前设置才生效
     *
     * @param rollBackTime 回退的时间，单位为ms.
     */
    public void setRollBackTime(int rollBackTime) {
        if (mConfig != null) {
            mConfig.setRollBack(rollBackTime);
        }
    }

    /**
     * 该接口可设置内核算法是否对原始音频进行aec处理.默认为false即算法执行aec
     * 须在init之前设置才生效
     */
    public void disableAec() {
        this.mIsDisableAec = true;
    }


    /**
     * 设置aec资源名，请事先放置在assets目录下
     * 须在init之前设置才生效
     *
     * @param aecResBin 　aec资源名
     */
    public void setAecResBin(String aecResBin) {
        this.aecResName = aecResBin;
    }


    @Deprecated
    public void setAecResName(String aecResName) {
        this.aecResName = aecResName;
    }

    /**
     * 设置aec资源绝对路径，包含资源名
     * 须在init之前设置才生效
     *
     * @param aecBinPath aec资源绝对路径
     */
    public void setAecBinPath(String aecBinPath) {
        this.aecBinPath = aecBinPath;
    }

    /**
     * 设置beamforming资源名，请事先放置在assets目录下
     * 须在init之前设置才生效
     *
     * @param beamformingResBin beamforming资源名
     */
    public void setBeamformingResBin(String beamformingResBin) {
        this.beamformingResName = beamformingResBin;
    }


    @Deprecated
    public void setBeamformingResName(String beamformingResName) {
        this.beamformingResName = beamformingResName;
    }

    /**
     * 设置beamforming资源绝对路径，包含资源名
     * 须在init之前设置才生效
     *
     * @param beamformingBinPath beamforming资源绝对路径
     */
    public void setBeamformingResBinPath(String beamformingBinPath) {
        this.beamformingBinPath = beamformingBinPath;
    }

    /**
     * 设置唤醒词资源名，请事先放置在assets目录下
     * 须在init之前设置才生效
     *
     * @param wakeupResBin 唤醒资源名
     */
    public void setWakeupResBin(String wakeupResBin) {
        this.wakeupResName = wakeupResBin;
    }


    @Deprecated
    public void setWakeupResName(String wakeupResName) {
        this.wakeupResName = wakeupResName;
    }

    /**
     * 设置唤醒资源绝对路径，包含资源名
     * 须在init之前设置才生效
     *
     * @param wakeupBinPath 唤醒资源绝对路径
     */
    public void setWakeupBinPath(String wakeupBinPath) {
        this.wakeupBinPath = wakeupBinPath;
    }

    /**
     * 设置是否自行feed数据,不使用内部录音机
     * 须在init之前设置才生效
     *
     * @param useCustomFeed true:使用外部开发者自己的录音数据  false:用SDK内部的录音机 默认为false
     */
    public void setUseCustomFeed(boolean useCustomFeed) {
        mParam.setUseCustomFeed(useCustomFeed);
    }


    /**
     * 设置唤醒词对应阈值，是否需要设置和唤醒资源有关系
     * 须在init之前设置才生效
     *
     * @param threshold 置信度
     */
    public void setThreshold(float[] threshold) {
        mConfig.setThreshs(threshold);
    }

    /**
     * 设置大音量场景下的预唤醒阈值，是否需要设置和唤醒资源有关系
     * 须在init之前调用生效
     *
     * @param lowThreshold lowThreshold
     */
    public void setLowThreshold(float[] lowThreshold) {
        mConfig.setThreshs2(lowThreshold);
    }

    /**
     * 设置唤醒词，是否需要设置和唤醒资源有关系
     * 须在init之前设置才生效
     *
     * @param words 唤醒词
     *              设置唤醒词的同时还需要设置相应的阈值{@link AILocalSignalAndWakeupEngine#setThreshold(float[])}
     *              和major{@link AILocalSignalAndWakeupEngine#setMajors(int[])}
     */
    public void setWords(String[] words) {
        mConfig.setWakupWords(words);
    }


    /**
     * 设置唤醒词的major，主唤醒词为1,副唤醒词为0
     * 须在init之前设置才生效
     *
     * @param majors major
     */
    public void setMajors(int[] majors) {
        mConfig.setMajors(majors);
    }


    /**
     * 设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验
     * 须在init之前设置才生效 ，设置方法和{@link AILocalSignalAndWakeupEngine#setMajors(int[])}类似
     *
     * @param dchecks dchecks
     */
    public void setDchecks(int[] dchecks) {
        mConfig.setDchecks(dchecks);
    }

    /**
     * 设置音频保存路径，会保存原始多声道音频和经过beamforming后的单声道音频
     * 如果设置了就会保存，没设置不会保存
     * 须在init之前设置才生效
     *
     * @param path path
     **/
    public void setSaveAudioFilePath(String path) {
        if (mParam != null) {
            mParam.setSaveAudioFilePath(path);
        }
    }

    public void setSaveWakeupCutDataPath(String path) {
        if (mParam != null) {
            mParam.setSaveWakeupCutDataPath(path);
        }
    }

    /**
     * 获取{@link AILocalSignalAndWakeupListener#onVprintCutDataReceived(int, byte[], int)}返回的音频通道
     * 用于设置给{@link com.aispeech.lite.vprint.VprintIntent#setAecChannelNum(int)}
     * 和{@link com.aispeech.lite.vprint.VprintIntent#setBfChannelNum(int)}
     *
     * @param param {@link AIConstant#KEY_FESPX_AEC_CHANNEL} 和 {@link AIConstant#KEY_FESPX_BF_CHANNEL}
     * @return aecChannelNum或bfChannelNum
     * 须在引擎初始化{@link AILocalSignalAndWakeupListener#onInit(int)}成功后才可以获取,若返回值为-1，则表示引擎未初始化
     */
    public int getValueOf(String param) {
        if (mFespxProcessor != null) {
            return mFespxProcessor.getValueOf(param);
        }
        return -1;
    }

    /**
     * 设置是否输入实时的长音频，默认接受长音频为true(如果是一二级唤醒，即每个唤醒词独立且非实时，则需要设置为false，如果不设置会影响性能)
     * 须在start之前设置生效
     * 当设置为false时,每次送一段音频段都会给予是否唤醒的反馈，如果没有被唤醒，则抛出wakeupWord:null, confidence:0的信息
     *
     * @param inputContinuousAudio 是否输入实时的长音频
     */
    public void setInputContinuousAudio(boolean inputContinuousAudio) {
        if (mParam != null)
            mParam.setInputContinuousAudio(inputContinuousAudio);
    }


    /**
     * The adapter for convert SpeechListener to AILocalSignalAndWakeupListener.
     */
    private class SpeechListenerImpl extends SpeechListener {
        AILocalSignalAndWakeupListener mListener;

        public SpeechListenerImpl(AILocalSignalAndWakeupListener listener) {
            mListener = listener;
        }

        public void setListener(AILocalSignalAndWakeupListener listener) {
            mListener = listener;
        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(error);
            }
        }

        @Override
        public void onInit(int status) {
            if (mListener != null) {
                mListener.onInit(status);
            }
        }


        @Override
        public void onDoaResult(int doa) {
            if (mListener != null) {
                mListener.onDoaResult(doa);
            }
        }

        @Override
        public void onResults(AIResult result) {
            if (result.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                if (mListener != null) {
                    try {
                        JSONObject obj = new JSONObject(result.getResultObject().toString());
                        mListener.onWakeup(obj.getDouble("confidence"), obj.getString("wakeupWord"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {
            if (mListener != null) {
                mListener.onRawDataReceived(buffer, size);
            }
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeup_type) {
            if (mListener != null) {
                mListener.onResultDataReceived(buffer, size, wakeup_type);
            }
        }


        @Override
        public void onEndOfSpeech() {
        }


        @Override
        public void onReadyForSpeech() {
            if (mListener != null) {
                mListener.onReadyForSpeech();
            }
        }

        @Override
        public void onBeginningOfSpeech() {

        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onRecorderStopped() {
        }


        @Override
        public void onEvent(int eventType, Map params) {

        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] buffer, int size) {
            if (mListener != null) {
                mListener.onVprintCutDataReceived(dataType, buffer, size);
            }
        }

        @Override
        public void onNotOneShot() {

        }
    }


}
