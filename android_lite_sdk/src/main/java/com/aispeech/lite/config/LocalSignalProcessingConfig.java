package com.aispeech.lite.config;

import android.text.TextUtils;

import com.aispeech.DUILiteConfig;
import com.aispeech.common.JSONUtil;
import com.aispeech.common.Log;
import com.aispeech.export.config.NearWakeupConfig;
import com.aispeech.kernel.Fespa;
import com.aispeech.kernel.Fespd;
import com.aispeech.kernel.Fespl;
import com.aispeech.kernel.NearFespx;
import com.aispeech.lite.AISpeech;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * Created by wuwei on 18-6-4.
 */

public class LocalSignalProcessingConfig extends AIEngineConfig {
    public static final String KEY_AEC_BIN_PTAH = "aecBinPath";
    public static final String KEY_WAKUP_BIN_PATH = "wakeupBinPath";
    public static final String KEY_BEAMFORMING_BIN_PATH = "beamformingBinPath";
    public static final String KEY_ENV = "env";
    public static final String KEY_ROLLBACK = "rollBack";
    public static final String KEY_WORD = "words";
    public static final String KEY_THRESH = "thresh";
    public static final String KEY_CCTHRESH = "cc_thresh";
    public static final String KEY_THRESH_2 = "thresh2";
    public static final String KEY_MAJOR = "major";
    public static final String KEY_DCHECK = "dcheck";
    public static final String KEY_MAX_VOLUME = "maxVolume";
    public static final Object KEY_RANGES = "ranges";
    public static final Object KEY_SUB_WORD_WAKEUP = "subword_wakeup";


    private String saveAudioFilePath;

    /**
     * 状态流帧数,影响识别阶段音频抑制生效时间，默认20帧状态
     */
    private int stateFrame = 20;

    /**
     * 缓存音频帧数,影响识别阶段音频抑制前后缓冲区长度，默认10帧音频,默认保留状态流前后10帧左右长度音频，有助于提升识别准确率
     */
    private int rightMarginFrame = 5;
//    private String saveAudioFilePath;
//    private String mSaveWakeupCutDataPath;
    private static final String KEY_LIBRARY_TYPE = "libraryType";
    private static final String KEY_LIBRARY_PATH = "libraryPath";
    private static final String TAG = "LocalSignalProcessingCo";
    /**
     * "aecBinPath": "./third/res/fespl/aec.bin",
     * "wakeupBinPath": "./third/res/fespl/wakeup_aihome_tcent_ed_20170704_8bit.bin",
     * "beamformingBinPath ": ". / third / res / fespl / fend.bin ",
     * "env ": " words = xiao le;thresh = 0.05;major = 1;",
     * "rollBack ": 1200
     */

    protected JSONObject jsonObject;
    private int micType = -1;
    private int mPreUploadRollbackTime = 1000;
    private String aecBinPath = "";
    private String wakupBinPath = "";
    private String beamformingBinPath = "";
    private int rollBack = 0;
    private String[] wakupWords;
    /**
     * 是否是半词唤醒，是的话标为1
     */
    private int[] subwordWakeup;
    /**
     * 使用云端ASR进行唤醒校验时需要设置中文唤醒词
     */
    private String[] cnWakeupWord;
    /**
     * 使用云端ASR进行唤醒校验时需要设置高阈值，当本地唤醒的 confidence 高于高阈值时不需要进行云端ASR校验
     */
    private float[] highThreshold;

    private int cloudWakeupCheckTimeout = 600;

    private float[] threshs;
    private float[] ccThreshs;
    private float[] threshs2;
    private int[] majors;
    private int[] dchecks;
    private int[] ranges; //定位模式下唤醒切换音区，"1"表示切换，"0"表示不切换，如 [1,0,0]
    private boolean vad;
    private String env;
    private int boundary = -1;//开启/关闭声纹唤醒词截断，0:关闭，1：开启
    private int maxVolume = -1;
    // sspe
    private String sspeBinPath;

    //后排抑制开关
    private boolean backRowRestrainEnable;

    // fespcar 兼容星际四音区独立交互音频分离
    private boolean isFourHost = false;
    private int disCallBackResultData;
    public boolean isFourHost() {
        return isFourHost;
    }

    public void setFourHost(boolean fourHost) {
        isFourHost = fourHost;
    }

    /**
     * oneshot 配置
     */
    private OneshotConfig oneshotConfig;

    public OneshotConfig getOneshotConfig() {
        return oneshotConfig;
    }

    public void setOneshotConfig(OneshotConfig oneshotConfig) {
        this.oneshotConfig = oneshotConfig;
    }


    /**
     * 参考音路数
     */
    private int echoChannelNum = 0;
    // voip END<==
    private NearWakeupConfig nearWakeupConfig;
    private String[] mNet;
    private String[] mCustom;
    private String[] mThreshLow;
    private String[] mThreshHigh;
    private boolean isSspe = true;
    private int sspeType = -1;

    private static String getSoFileName(String libraryName) {
        return "lib" + libraryName + ".so";
    }

    public int getMicType() {
        return micType;
    }

    /**
     * 设置后排抑制开关
     *
     * @param backRowRestrainEnable 后排抑制开关，true：开启、false：关闭
     */
    public void setBackRowRestrainEnable(boolean backRowRestrainEnable) {
        this.backRowRestrainEnable = backRowRestrainEnable;
    }

    /**
     * 获取后排抑制开关
     *
     * @return true：开启、false：关闭
     */
    public boolean isBackRowRestrainEnable() {
        return backRowRestrainEnable;
    }

    /**
     * 将设置范围扩散到全局，即设置全局的麦克风类型，等同于DUILiteConfig.setAudioRecorderType(int)
     *
     * @param micType 设置麦克风类型
     */
    public void setMicType(int micType) {
        this.micType = micType;
        if (micType != -1) {
            AISpeech.setRecoderType(micType);
        }
    }

    public int getSspeType() {
        return sspeType;
    }

    public void setSspeType(int sspeType) {
        this.sspeType = sspeType;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public int getMaxVolume() {
        return maxVolume;
    }

    public void setMaxVolume(int maxVolume) {
        this.maxVolume = maxVolume;
    }

    public boolean isVad() {
        return vad;
    }

    public void setVad(boolean vad) {
        this.vad = vad;
    }

    public int getBoundary() {
        return boundary;
    }

    public void setBoundary(int boundary) {
        this.boundary = boundary;
    }

    public String getAecBinPath() {
        return aecBinPath;
    }

    public void setAecBinPath(String aecBinPath) {
        this.aecBinPath = aecBinPath;
    }

    public String getWakupBinPath() {
        return wakupBinPath;
    }

    public void setWakupBinPath(String wakupBinPath) {
        this.wakupBinPath = wakupBinPath;
    }

    public String getBeamformingBinPath() {
        return beamformingBinPath;
    }

    public void setBeamformingBinPath(String beamformingBinPath) {
        this.beamformingBinPath = beamformingBinPath;
    }

    public int getRollBack() {
        return rollBack;
    }

    public void setRollBack(int rollBack) {
        this.rollBack = rollBack;
    }

    public String[] getWakupWords() {
        return wakupWords;
    }

    public void setWakupWords(String[] wakupWords) {
        this.wakupWords = wakupWords;
    }

    public int[] getSubwordWakeup() {
        return subwordWakeup;
    }

    public void setSubwordWakeup(int[] subwordWakeup) {
        this.subwordWakeup = subwordWakeup;
    }

    public String[] getCnWakeupWord() {
        return cnWakeupWord;
    }

    public void setCnWakeupWord(String[] cnWakeupWord) {
        this.cnWakeupWord = cnWakeupWord;
    }

    public float[] getHighThreshold() {
        return highThreshold;
    }

    public float getHighThreshold(String wakeupWord) {
        int index = -1;
        if (wakupWords != null)
            for (int i = 0; i < wakupWords.length; i++) {
                String s = wakupWords[i];
                if (s != null && s.equals(wakeupWord)) {
                    index = i;
                    break;
                }
            }

        if (index >= 0 && highThreshold != null && index < highThreshold.length) {
            return highThreshold[index];
        }

        return 1;
    }

    public void setHighThreshold(float[] highThreshold) {
        this.highThreshold = highThreshold;
    }

    public int getCloudWakeupCheckTimeout() {
        return cloudWakeupCheckTimeout;
    }

    public void setCloudWakeupCheckTimeout(int cloudWakeupCheckTimeout) {
        this.cloudWakeupCheckTimeout = cloudWakeupCheckTimeout;
    }

    public float[] getThreshs() {
        return threshs;
    }

    public void setThreshs(float[] threshs) {
        this.threshs = threshs;
    }

    public float[] getCcThreshs() {
        return ccThreshs;
    }

    public void setCcThreshs(float[] ccThreshs) {
        this.ccThreshs = ccThreshs;
    }

    public void setThreshs2(float[] threshs2) {
        this.threshs2 = threshs2;
    }

    public int[] getMajors() {
        return majors;
    }

    public void setMajors(int[] majors) {
        this.majors = majors;
    }

    public void setDchecks(int[] dchecks) {
        this.dchecks = dchecks;
    }

    public int[] getRanges() {
        return ranges;
    }

    public void setRanges(int[] ranges) {
        this.ranges = ranges;
    }

    public List<String> getRangesWords() {
        if (ranges != null && wakupWords != null && ranges.length == wakupWords.length) {
            List<String> rangesWords = new ArrayList<>();
            for (int i = 0; i < ranges.length; i++) {
                if (ranges[i] == 0) {
                    rangesWords.add(wakupWords[i]);
                }
            }
            return rangesWords;
        }
        return null;
    }


    private String toEnv() {
        StringBuilder envSb = new StringBuilder();
        appendWords(envSb);
        appendThreshs(envSb);
        appendCcThreshs(envSb);

        // 是否开启大音量检测判断
        appendThresh2(envSb);
        appendMajors(envSb);
        if (subwordWakeup != null && subwordWakeup.length > 0) {
            StringBuilder subwordWakeupSb = new StringBuilder();
            for (int i = 0; i < subwordWakeup.length; i++) {
                subwordWakeupSb.append(subwordWakeup[i]);
                if (i != subwordWakeup.length - 1) {
                    subwordWakeupSb.append(",");
                }
            }
            envSb.append(KEY_SUB_WORD_WAKEUP + "=" + subwordWakeupSb.toString() + ";");
        }
        if (dchecks != null && dchecks.length > 0) {
            StringBuilder dcheckSb = new StringBuilder();
            for (int i = 0; i < dchecks.length; i++) {
                dcheckSb.append(dchecks[i]);
                if (i != dchecks.length - 1) {
                    dcheckSb.append(",");
                }
            }
            envSb.append(KEY_DCHECK + "=" + dcheckSb.toString() + ";");
        }

        envSb.append("vad=").append(vad ? "1" : "0").append(";");
        if (mCustom != null) {
            StringBuilder customs = new StringBuilder();
            for (int i = 0; i < mCustom.length; i++) {
                customs.append(mCustom[i]);
                if (i == mCustom.length - 1) {
                    customs.append(";");
                } else {
                    customs.append(",");
                }
            }
            envSb.append("custom=");
            envSb.append(customs);
        }
        if (mNet != null) {
            StringBuilder nets = new StringBuilder();
            for (int i = 0; i < mNet.length; i++) {
                nets.append(mNet[i]);
                if (i == mNet.length - 1) {
                    nets.append(";");
                } else {
                    nets.append(",");
                }
            }
            envSb.append("net=");
            envSb.append(nets);
        }
        if (mThreshHigh != null) {
            StringBuilder threshHigh = new StringBuilder();
            for (int i = 0; i < mThreshHigh.length; i++) {
                threshHigh.append(mThreshHigh[i]);
                if (i == mThreshHigh.length - 1) {
                    threshHigh.append(";");
                } else {
                    threshHigh.append(",");
                }
            }
            envSb.append("thresh_high=");
            envSb.append(threshHigh);
        }
        if (mThreshLow != null) {
            StringBuilder threshLow = new StringBuilder();
            for (int i = 0; i < mThreshLow.length; i++) {
                threshLow.append(mThreshLow[i]);
                if (i == mThreshLow.length - 1) {
                    threshLow.append(";");
                } else {
                    threshLow.append(",");
                }
            }
            envSb.append("thresh_low=");
            envSb.append(threshLow);
        }

        if (boundary != -1) {//不设置默认不加此参数，避免sspe内核报错
            envSb.append("boundary=").append(boundary >= 1 ? "1" : "0").append(";");
        }

        return envSb.toString();
    }

    private void appendMajors(StringBuilder envSb) {
        if (majors != null && majors.length > 0) {
            StringBuilder majorSb = new StringBuilder();
            for (int i = 0; i < majors.length; i++) {
                majorSb.append(majors[i]);
                if (i != majors.length - 1) {
                    majorSb.append(",");
                }
            }
            envSb.append(KEY_MAJOR + "=" + majorSb.toString() + ";");
        }
    }

    private void appendThresh2(StringBuilder envSb) {
        if (threshs2 != null && threshs2.length > 0) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < threshs2.length; i++) {
                sb.append("" + threshs2[i]);
                if (i != threshs2.length - 1) {
                    sb.append(",");
                }
            }
            envSb.append(KEY_THRESH_2 + "=" + sb + ";");
        }
    }

    private void appendThreshs(StringBuilder envSb) {
        if (threshs != null && threshs.length > 0) {
            StringBuilder threshSb = new StringBuilder();
            for (int i = 0; i < threshs.length; i++) {
                threshSb.append(threshs[i]);
                if (i != threshs.length - 1) {
                    threshSb.append(",");
                }
            }
            envSb.append(KEY_THRESH + "=" + threshSb.toString() + ";");
        }
    }

    private void appendCcThreshs(StringBuilder envSb) {
        if (ccThreshs != null && ccThreshs.length > 0) {
            StringBuilder ccthreshSb = new StringBuilder();
            for (int i = 0; i < ccThreshs.length; i++) {
                ccthreshSb.append(ccThreshs[i]);
                if (i != ccThreshs.length - 1) {
                    ccthreshSb.append(",");
                }
            }
            envSb.append(KEY_CCTHRESH + "=" + ccthreshSb.toString() + ";");
        }
    }

    private void appendWords(StringBuilder envSb) {
        if (wakupWords != null && wakupWords.length > 0) {
            StringBuilder wordSb = new StringBuilder();
            for (int i = 0; i < wakupWords.length; i++) {
                wordSb.append(wakupWords[i]);
                if (i != wakupWords.length - 1) {
                    wordSb.append(",");
                }
            }
            envSb.append(KEY_WORD + "=" + wordSb.toString() + ";");
        }
    }
    public int getStateFrame() {
        return stateFrame;
    }

    public void setStateFrame(int stateFrame) {
        this.stateFrame = stateFrame;
    }

    public int getRightMarginFrame() {
        return rightMarginFrame;
    }

    public void setRightMarginFrame(int rightMarginFrame) {
        this.rightMarginFrame = rightMarginFrame;
    }

    /**
     * 参数JSON化
     *
     * @return JSONObject
     */
    @Override
    public JSONObject toJson() {
        jsonObject = new JSONObject();
        JSONUtil.putQuietly(jsonObject, "prof", Log.parseDuiliteLog());
        if (!TextUtils.isEmpty(aecBinPath)) {
            JSONUtil.putQuietly(jsonObject, KEY_AEC_BIN_PTAH, aecBinPath);
        }
        if (!TextUtils.isEmpty(wakupBinPath)) {
            JSONUtil.putQuietly(jsonObject, KEY_WAKUP_BIN_PATH, wakupBinPath);
        }

        if (!TextUtils.isEmpty(sspeBinPath)) {
            JSONUtil.putQuietly(jsonObject, "sspeBinPath", sspeBinPath);
        }

        if (!TextUtils.isEmpty(beamformingBinPath)) {
            JSONUtil.putQuietly(jsonObject, KEY_BEAMFORMING_BIN_PATH, beamformingBinPath);
        }
        if (!TextUtils.isEmpty(env)) {
            JSONUtil.putQuietly(jsonObject, KEY_ENV, this.env);
        } else {
            JSONUtil.putQuietly(jsonObject, KEY_ENV, toEnv());
        }
        if (rollBack >= 0) {
            JSONUtil.putQuietly(jsonObject, KEY_ROLLBACK, rollBack);
        }
        if (maxVolume == 1) {
            JSONUtil.putQuietly(jsonObject, KEY_MAX_VOLUME, 1);
        } else {
            if ((AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE4 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_CIRCLE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE6 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_LINE8 ||
                    AISpeech.getRecoderType() == DUILiteConfig.TYPE_COMMON_SHAPE_L4)
                    && AISpeech.maxVolumeMode) {
                JSONUtil.putQuietly(jsonObject, KEY_MAX_VOLUME, 1);
            }
        }
        calNearWakeupJsons();
        return jsonObject;
    }

    private void calNearWakeupJsons() {
        if (nearWakeupConfig != null) {
            // netCfg 和 mdsCfg 这2个key会在init时读取
            JSONUtil.putQuietly(jsonObject, NearFespx.NET_CONFIG, nearWakeupConfig.toNetJson());
            JSONUtil.putQuietly(jsonObject, NearFespx.MDS_CONFIG, nearWakeupConfig.toMdsJson());
            // libraryType 取值范围：单麦"wakeup"、双麦"fespd"、线麦"fespl"、环麦"fespa"
            if (micType < 0) {
                micType = AISpeech.getRecoderType();
            }
            if (micType == DUILiteConfig.TYPE_COMMON_DUAL ||
                    micType == DUILiteConfig.TYPE_TINYCAP_DUAL) {
                Fespd.isFespxSoValid();
                JSONUtil.putQuietly(jsonObject, KEY_LIBRARY_PATH, getSoFileName(Fespd.LIBRARY_NAME));
                JSONUtil.putQuietly(jsonObject, KEY_LIBRARY_TYPE, "fespd");
            } else if (micType == DUILiteConfig.TYPE_COMMON_LINE4 ||
                    micType == DUILiteConfig.TYPE_TINYCAP_LINE4 ||
                    micType == DUILiteConfig.TYPE_TINYCAP_LINE6 ||
                    micType == DUILiteConfig.TYPE_COMMON_LINE8 ||
                    micType == DUILiteConfig.TYPE_COMMON_LINE6) {
                Fespl.isFespxSoValid();
                JSONUtil.putQuietly(jsonObject, KEY_LIBRARY_PATH, getSoFileName(Fespl.LIBRARY_NAME));
                JSONUtil.putQuietly(jsonObject, KEY_LIBRARY_TYPE, "fespl");
            } else if (micType == DUILiteConfig.TYPE_COMMON_CIRCLE6 ||
                    micType == DUILiteConfig.TYPE_COMMON_CIRCLE4 ||
                    micType == DUILiteConfig.TYPE_TINYCAP_CIRCLE4 ||
                    micType == DUILiteConfig.TYPE_TINYCAP_CIRCLE6 ||
                    micType == DUILiteConfig.TYPE_COMMON_SHAPE_L4) {
                Fespa.isFespxSoValid();
                JSONUtil.putQuietly(jsonObject, KEY_LIBRARY_PATH, getSoFileName(Fespa.LIBRARY_NAME));
                JSONUtil.putQuietly(jsonObject, KEY_LIBRARY_TYPE, "fespa");
            } else {
                Log.w(TAG, "RecoderType set wrong: " + AISpeech.getRecoderType());
            }
        }
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public String getSspeBinPath() {
        return sspeBinPath;
    }

    public void setSspeBinPath(String sspeBinPath) {
        this.sspeBinPath = sspeBinPath;
    }

    public boolean isSspe() {
        return isSspe;
    }

    public void setUseSspe(boolean useSspe) {
        isSspe = useSspe;
    }

    public int getEchoChannelNum() {
        return echoChannelNum;
    }

    public void setEchoChannelNum(int echoChannelNum) {
        this.echoChannelNum = echoChannelNum;
    }

    public int getPreUploadRollbackTime() {
        return mPreUploadRollbackTime;
    }

    /**
     * 设置半字唤醒的回溯时长，用于云端二次校验，缓存时长到收到第一次半字唤醒的回调
     * @param preUploadRollbackTime 截止半字唤醒的缓存时长，单位ms，默认1000
     */
    public void setPreUploadRollbackTime(int preUploadRollbackTime) {
        mPreUploadRollbackTime = preUploadRollbackTime;
    }

    public NearWakeupConfig getNearWakeupConfig() {
        return nearWakeupConfig;
    }

    public void setNearWakeupConfig(NearWakeupConfig nearWakeupConfig) {
        this.nearWakeupConfig = nearWakeupConfig;
    }

    public void setNet(String[] net) {
        mNet = net;
    }

    public String[] getNet() {
        return mNet;
    }

    public void setCustom(String[] custom) {
        mCustom = custom;
    }

    public String[] getCustom() {
        return mCustom;
    }

    public void setThreshLow(String[] threshLow) {
        mThreshLow = threshLow;
    }

    public String[] getThreshLow() {
        return mThreshLow;
    }

    public void setThreshHigh(String[] threshHigh) {
        mThreshHigh = threshHigh;
    }

    public String[] getThreshHigh() {
        return mThreshHigh;
    }


    /**
     * 是否注册接口的标记
     */
    public boolean implWakeupCk = true;
    public boolean implMultiBfCk = true;
    public boolean implOutputCk = true;
    public boolean implInputCk = true;
    public boolean implBfCk = true;
    public boolean implDoaCk = true;
    public boolean implVprintCutCk = true;
    public boolean implEchoCk = true;
    public boolean implEchoVoipCk = true;
    public boolean implSevcDoaCk = true;
    public boolean implSevcNoiseCk = true;
    public boolean implVoipCk = true;
    public boolean implAgcCk = true;
    public boolean implVadCk = true;

    public boolean isImplVadCk() {
        return implVadCk;
    }

    public void setImplVadCk(boolean implVadCk) {
        this.implVadCk = implVadCk;
    }

    public boolean isImplMultiBfCk() {
        return implMultiBfCk;
    }

    public void setImplMultiBfCk(boolean implMultiBfCk) {
        this.implMultiBfCk = implMultiBfCk;
    }

    public boolean isImplWakeupCk() {
        return implWakeupCk;
    }

    public void setImplWakeupCk(boolean implWakeupCk) {
        this.implWakeupCk = implWakeupCk;
    }

    public boolean isImplOutputCk() {
        return implOutputCk;
    }

    public void setImplOutputCk(boolean implOutputCk) {
        this.implOutputCk = implOutputCk;
    }

    public boolean isImplInputCk() {
        return implInputCk;
    }

    public void setImplInputCk(boolean implInputCk) {
        this.implInputCk = implInputCk;
    }

    public boolean isImplBfCk() {
        return implBfCk;
    }

    public void setImplBfCk(boolean implBfCk) {
        this.implBfCk = implBfCk;
    }

    public boolean isImplDoaCk() {
        return implDoaCk;
    }

    public void setImplDoaCk(boolean implDoaCk) {
        this.implDoaCk = implDoaCk;
    }

    public boolean isImplVprintCutCk() {
        return implVprintCutCk;
    }

    public void setImplVprintCutCk(boolean implVprintCutCk) {
        this.implVprintCutCk = implVprintCutCk;
    }

    public boolean isImplEchoCk() {
        return implEchoCk;
    }

    public void setImplEchoCk(boolean implEchoCk) {
        this.implEchoCk = implEchoCk;
    }

    public boolean isImplEchoVoipCk() {
        return implEchoVoipCk;
    }

    public void setImplEchoVoipCk(boolean implEchoVoipCk) {
        this.implEchoVoipCk = implEchoVoipCk;
    }

    public boolean isImplSevcDoaCk() {
        return implSevcDoaCk;
    }

    public void setImplSevcDoaCk(boolean implSevcDoaCk) {
        this.implSevcDoaCk = implSevcDoaCk;
    }

    public boolean isImplSevcNoiseCk() {
        return implSevcNoiseCk;
    }

    public void setImplSevcNoiseCk(boolean implSevcNoiseCk) {
        this.implSevcNoiseCk = implSevcNoiseCk;
    }

    public boolean isImplAgcCk() {
        return implAgcCk;
    }

    public void setImplAgcCk(boolean implAgcCk) {
        this.implAgcCk = implAgcCk;
    }


    public void setFespcarDataReceivedEnable(int fespCallBackResultData) {
        this.disCallBackResultData = fespCallBackResultData;
    }
    public int getDisCallBackResultData() {
        return disCallBackResultData;
    }
}
