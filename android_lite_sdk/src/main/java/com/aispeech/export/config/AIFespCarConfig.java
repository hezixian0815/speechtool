package com.aispeech.export.config;

import android.text.TextUtils;

import com.aispeech.common.PinYinUtils;
import com.aispeech.export.bean.VoiceQueueStrategy;
import com.aispeech.lite.Languages;
import com.aispeech.lite.base.BaseConfig;
import com.aispeech.lite.base.IVoiceRestrictive;
import com.aispeech.lite.sspe.SspeConstant;

import java.util.Arrays;

/**
 * Created by wanbing on 2021/9/6 11:38
 */
public class AIFespCarConfig extends BaseConfig implements IVoiceRestrictive {

    /**
     * 唤醒词信息
     */
    private WakeupWord wakeupWord;


    /**
     * 设置唤醒词对应阈值，是否需要设置和唤醒资源有关系
     */
    private float[] threshold;

    /**
     * 设置大音量场景下的唤醒阈值，是否需要设置和唤醒资源有关系
     */
    private float[] lowThreshold;

    /**
     * 设置唤醒词
     */
    private String[] wakeupWordArray;

    /**
     * 设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0]
     */
    private int[] majors;

    /**
     * 设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验，如 [1,0,0]
     */
    private int[] dcheck;


    /**
     * 设置唤醒词定位模式下是否切换音区，"1"表示切换音区，"0"表示不切换，如 [1,0,0]
     */
    private int[] ranges;

    /**
     * 开启/关闭声纹唤醒词截断，0:关闭，1：开启
     */
    private int boundary = -1;

    /**
     * 唤醒资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     */
    private String wakeupResource;

    /**
     * beamforming 资源名，beamforming 即波束成形，将多路音频数据 beamforming 成单声道音频，方便后续做唤醒，识别等
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     */
    private String beamformingResource;

    /**
     * 设置aec资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     */
    private String aecResource;

    /**
     * sspe 资源，和 {@link #wakeupResource} 配合使用。
     * <p>
     * 设置 sspe 资源时，无需设置 {@link #aecResource} 和 {@link #beamformingResource}
     * </p>
     */
    private String sspeResource;

    /**
     * 状态流帧数,影响识别阶段音频抑制生效时间，默认20帧状态
     */
    private int stateFrame = 20;

    /**
     * 缓存音频帧数,影响识别阶段音频抑制前后缓冲区长度，默认10帧音频,默认保留状态流前后10帧左右长度音频，有助于提升识别准确率
     */
    private int rightMarginFrame = 10;

    private Languages language = Languages.CHINESE;


    private AIOneshotConfig oneshotConfig;

    /**
     * 内核是否开启vad 默认为true 车载考虑误唤醒场景
     */
    private boolean vad = true;

    private int sspeType = SspeConstant.SSPE_TYPE_CAR_TWO;
    private int disCallBackResultData;

    private boolean isFourHost = false;

    private SubWakeupWord subWakeupWord;

    public SubWakeupWord getSubWakeupWord() {
        return subWakeupWord;
    }

    public void setSubWakeupWord(SubWakeupWord subWakeupWord) {
        this.subWakeupWord = subWakeupWord;
    }

    public boolean isFourHost() {
        return isFourHost;
    }

    public void setFourHost(boolean fourHost) {
        isFourHost = fourHost;
    }

    public int getSspeType() {
        return sspeType;
    }
    public int getFespCallBackResultData() {
        return disCallBackResultData;
    }

    /**
     * 设置FespCar类型
     *
     * @param sspeType
     */
    public void setSspeType(int sspeType) {
        this.sspeType = sspeType;
    }

    public boolean isVad() {
        return vad;
    }

    public void setVad(boolean vad) {
        this.vad = vad;
    }

    public AIOneshotConfig getOneshotConfig() {
        return oneshotConfig;
    }

    public void setOneshotConfig(AIOneshotConfig oneshotConfig) {
        this.oneshotConfig = oneshotConfig;
    }

    public int getBoundary() {
        return boundary;
    }

    /**
     * 设置唤醒词语种
     *
     * @param language {@link Languages}
     */
    public void setLanguage(Languages language) {
        this.language = language;
    }


    public Languages getLanguage() {
        return language;
    }

    public int getStateFrame() {
        return stateFrame;
    }

    public int getRightMarginFrame() {
        return rightMarginFrame;
    }


    public String getSspeResource() {
        return sspeResource;
    }

    /**
     * sspe 资源, 包含 AEC BSS 等，不同项目含义有所差别
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * <p> sspe 资源，和 {@link #wakeupResource} 配合使用。</p>
     * <p>
     * 设置 sspe 资源时，无需设置 {@link #aecResource} 和 {@link #beamformingResource}
     * </p>
     *
     * @param sspeResource sspe 资源
     */
    public void setSspeResource(String sspeResource) {
        this.sspeResource = sspeResource;
    }

    public boolean isSspe() {
        return !TextUtils.isEmpty(sspeResource);
    }

    /**
     * 设置唤醒词以及是否作为主唤醒词，主唤醒词为1,副唤醒词为0
     *
     * @param wakeupWord 唤醒词，如 ["ni hao xiao chi", "ni hao xiao le","bu ding bu ding"]
     *                   还需要设置唤醒词相应的阈值{@link  #setThreshold(float[])} 和 {@link #setLowThreshold(float[])}
     * @param majors     是否是主唤醒词，如 [1,0,0]
     */
    public void setWakeupWord(String[] wakeupWord, int[] majors) {
        this.wakeupWordArray = wakeupWord;
        this.majors = majors;
    }


    public float[] getThreshold() {
        return threshold;
    }

    /**
     * 设置唤醒词对应阈值，是否需要设置和唤醒资源有关系
     *
     * @param threshold 置信度
     */
    public void setThreshold(float[] threshold) {
        this.threshold = threshold;
    }

    public float[] getLowThreshold() {
        return lowThreshold;
    }

    /**
     * 设置大音量场景下的预唤醒阈值，是否需要设置和唤醒资源有关系
     *
     * @param lowThreshold lowThreshold
     */
    private void setLowThreshold(float[] lowThreshold) {
        this.lowThreshold = lowThreshold;
    }

    public String[] getWakeupWordArray() {
        return wakeupWordArray;
    }

    public WakeupWord getWakeupWord() {
        return wakeupWord;
    }

    public int[] getMajors() {
        return majors;
    }

    public int[] getDcheck() {
        return dcheck;
    }

    public int[] getRanges() {
        return ranges;
    }

    /**
     * 设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验
     *
     * @param dcheck 是否开启校验，如 [1,0,0]
     */
    public void setDcheck(int[] dcheck) {
        this.dcheck = dcheck;
    }

    public String getWakeupResource() {
        return wakeupResource;
    }

    /**
     * 唤醒资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param wakeupResource 唤醒资源
     */
    public void setWakeupResource(String wakeupResource) {
        this.wakeupResource = wakeupResource;
    }

    public String getBeamformingResource() {
        return beamformingResource;
    }

    /**
     * beamforming 资源，beamforming 即波束成形，将多路音频数据 beamforming 成单声道音频，方便后续做唤醒，识别等
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param beamformingResource beamforming 资源
     *                            兼容老接口，会将beamform资源对接上sspe资源
     */
    public void setBeamformingResource(String beamformingResource) {
        this.beamformingResource = beamformingResource;
        this.sspeResource = beamformingResource;
    }

    public String getAecResource() {
        return aecResource;
    }

    /**
     * 设置aec资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param aecResource 　aec资源
     */
    public void setAecResource(String aecResource) {
        this.aecResource = aecResource;
    }


    @Override
    public String toString() {
        return "AILocalSignalAndWakeupConfig{" +
                ", threshold=" + Arrays.toString(threshold) +
                ", lowThreshold=" + Arrays.toString(lowThreshold) +
                ", wakeupWord=" + Arrays.toString(wakeupWordArray) +
                ", majors=" + Arrays.toString(majors) +
                ", dcheck=" + Arrays.toString(dcheck) +
                ", wakeupResource='" + wakeupResource + '\'' +
                ", beamformingResource='" + beamformingResource + '\'' +
                ", aecResource='" + aecResource + '\'' +
                ", sspeResource='" + sspeResource + '\'' +
                //", maxMessageQueueSize=" + maxMessageQueueSize +
                //", nearWakeupConfig=" + nearWakeupConfig +
                '}';
    }

    @Override
    public void setVoiceStrategy(VoiceQueueStrategy voiceStrategy) {
        this.voiceQueueStrategy = voiceStrategy;
    }

    @Override
    public VoiceQueueStrategy getMaxVoiceQueueSize() {
        return voiceQueueStrategy;
    }

    /**
     * 唤醒词
     */
    /**
     * 唤醒词
     */
    public static class WakeupWord {

        /**
         * 设置唤醒词
         */
        public String[] pinyin;

        /**
         * 设置唤醒词对应阈值，是否需要设置和唤醒资源有关系
         */
        public float[] threshold;

        /**
         * 设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0]
         */
        public int[] majors;

        /**
         * 设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验，如 [1,0,0]
         */
        public int[] dcheck;

        /**
         * 设置唤醒词定位模式下是否切换音区，"1"表示切换音区，"0"表示不切换，如 [1,0,0]
         */
        private int[] ranges;

        /**
         * 唤醒词构造函数
         *
         * @param pinyin    唤醒词,如 {"ni hao xiao chi","ni hao a bao"}
         * @param threshold 唤醒阈值,如 {0.1 , 0.2}
         * @param majors    设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0] , 设置主唤醒词,信号处理后的音频会自动回溯音频
         * @param dcheck    设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验，如 [1,0,0]
         */
        public WakeupWord(String[] pinyin, float[] threshold, int[] majors, int[] dcheck) {
            this.pinyin = pinyin;
            this.threshold = threshold;
            this.majors = majors;
            this.dcheck = dcheck;
        }

        /**
         * 唤醒词构造函数
         *
         * @param pinyin    唤醒词,如 {"ni hao xiao chi","ni hao a bao"}
         * @param threshold 唤醒阈值,如 {0.1 , 0.2}
         * @param majors    设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0] , 设置主唤醒词,信号处理后的音频会自动回溯音频
         * @param dcheck    设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验，如 [1,0,0]
         * @param ranges    设置唤醒词定位模式下是否切换音区，"1"表示切换音区，"0"表示不切换，如 [1,0,0]
         */
        public WakeupWord(String[] pinyin, float[] threshold, int[] majors, int[] dcheck, int[] ranges) {
            this.pinyin = pinyin;
            this.threshold = threshold;
            this.majors = majors;
            this.dcheck = dcheck;
            this.ranges = ranges;
        }

        /**
         * 唤醒词构造函数
         *
         * @param pinyin    唤醒词,如 {"ni hao xiao chi","ni hao a bao"}
         * @param threshold 唤醒阈值,如 {0.1 , 0.2}
         * @param majors    设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0] , 设置主唤醒词,信号处理后的音频会自动回溯音频
         */
        public WakeupWord(String[] pinyin, float[] threshold, int[] majors) {
            this.pinyin = pinyin;
            this.threshold = threshold;
            this.majors = majors;
        }
    }

    /**
     * 半字唤醒词
     */
    public static class SubWakeupWord {
        /**
         * 设置半字唤醒词
         */
        private String[] pinyin;

        /**
         * 设置半字唤醒词对应阈值
         */
        private float[] threshold;

        public SubWakeupWord(String[] pinyin, float[] threshold) {
            this.pinyin = pinyin;
            this.threshold = threshold;
        }

        public String[] getPinyin() {
            return pinyin;
        }

        public float[] getThreshold() {
            return threshold;
        }
    }

    public static class Builder extends BaseConfig.Builder {

        /**
         * 唤醒词信息
         */
        private WakeupWord wakeupWord;

        /**
         * 半字唤醒信息
         */
        private SubWakeupWord subWakeupWord;

        /**
         * 唤醒资源
         * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
         * <p>2. 如在 assets 里设置为名称</p>
         */
        private String wakeupResource;
        /**
         * beamforming 资源名，beamforming 即波束成形，将多路音频数据 beamforming 成单声道音频，方便后续做唤醒，识别等
         * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
         * <p>2. 如在 assets 里设置为名称</p>
         */
        private String beamformingResource;

        /**
         * oneShot
         */
        private AIOneshotConfig oneshotConfig;

        /**
         * 状态流帧数,影响识别阶段音频抑制生效时间，默认20帧状态
         */
        private int stateFrame = 20;

        /**
         * 缓存音频帧数,影响识别阶段音频抑制前后缓冲区长度,默认10帧音频
         */
        private int rightMarginFrame = 10;

        private Languages language = Languages.CHINESE;

        /**
         * 开启/关闭声纹唤醒词截断，0:关闭，1：开启
         */
        private int boundary = -1;

        /**
         * sspe的类型
         */
        private int sspeType = SspeConstant.SSPE_TYPE_CAR_TWO;
        private int disCallBackResultData;

        public SubWakeupWord getSubWakeupWord() {
            return subWakeupWord;
        }

        public Builder setSubWakeupWord(SubWakeupWord subWakeupWord) {
            this.subWakeupWord = subWakeupWord;
            return this;
        }

        /**
         * 四个主机独立交互,在双vad下抛出vad 4路 asr 4路
         */
        private boolean isFourHost = false;

        protected VoiceQueueStrategy voiceQueueStrategy = null;

        public boolean isFourHost() {
            return isFourHost;
        }

        public Builder setFourHost(boolean fourHost) {
            isFourHost = fourHost;
            return this;
        }

        /**
         * 设置FespCar类型
         *
         * @param sspeType
         * @see SspeConstant#SSPE_TYPE_CAR_TWO
         * @see SspeConstant#SSPE_TYPE_CAR_FOUR
         */
        public Builder setSspeType(int sspeType) {
            this.sspeType = sspeType;
            return this;
        }
        /**
         * 设置取消fesp回传，关闭fespcar音频接口回调的抛出
         **/
        public Builder setDisResultDataReceived(int disCallBackResultData) {
            this.disCallBackResultData = disCallBackResultData;
            return this;
        }
        /**
         * 设置唤醒词语种
         *
         * @param language {@link Languages}
         */
        public void setLanguage(Languages language) {
            this.language = language;
        }

        public Languages getLanguage() {
            return language;
        }

        public WakeupWord getWakeupWord() {
            return wakeupWord;
        }

        public Builder setWakeupWord(WakeupWord wakeupWord) {
            this.wakeupWord = wakeupWord;
            return this;
        }

        public int getBoundary() {
            return boundary;
        }

        public Builder setBoundary(int boundary) {
            this.boundary = boundary;
            return this;
        }

        public Builder setVoiceStrategy(VoiceQueueStrategy voiceStrategy) {
            this.voiceQueueStrategy = voiceStrategy;
            return this;
        }

        public VoiceQueueStrategy getMaxVoiceQueueSize() {
            return voiceQueueStrategy;
        }

        public String getWakeupResource() {
            return wakeupResource;
        }

        public Builder setWakeupResource(String wakeupResource) {
            this.wakeupResource = wakeupResource;
            return this;
        }

        public String getBeamformingResource() {
            return beamformingResource;
        }

        public Builder setBeamformingResource(String beamformingResource) {
            this.beamformingResource = beamformingResource;
            return this;
        }

        public AIOneshotConfig getOneshotConfig() {
            return this.oneshotConfig;
        }

        public Builder setOneshotConfig(AIOneshotConfig oneshotConfig) {
            this.oneshotConfig = oneshotConfig;
            return this;
        }

        public int getStateFrame() {
            return stateFrame;
        }

        public Builder setStateFrame(int stateFrame) {
            this.stateFrame = stateFrame;
            return this;
        }

        public int getRightMarginFrame() {
            return rightMarginFrame;
        }

        public Builder setRightMarginFrame(int rightMarginFrame) {
            this.rightMarginFrame = rightMarginFrame;
            return this;
        }

        private boolean checkLength(String[] word, float[] threshold, int[] majors, int[] dcheck) {

            return word.length == threshold.length
                    && word.length == majors.length
                    && word.length == dcheck.length;

        }

        private boolean checkLength(String[] word, float[] threshold, int[] majors) {
            return word.length == threshold.length
                    && word.length == majors.length;
        }

        private void checkWakeupWord(WakeupWord word) {

            boolean isLengthValid;

            if (word.dcheck == null) {
                isLengthValid = checkLength(word.pinyin, word.threshold, word.majors);
            } else {
                isLengthValid = checkLength(word.pinyin, word.threshold, word.majors, word.dcheck);
            }

            if (!isLengthValid) {
                throw new IllegalArgumentException(" invalid length ! ");
            }

            if (language == Languages.CHINESE) {
                PinYinUtils.checkPinyin(word.pinyin);
            }
        }

        @Override
        public Builder setTagSuffix(String tagSuffix) {
            return (Builder) super.setTagSuffix(tagSuffix);
        }

        public AIFespCarConfig create() {
            checkWakeupWord(this.getWakeupWord());
            return super.build(new AIFespCarConfig(this));
        }
    }

    private AIFespCarConfig(Builder builder) {
        this.wakeupWord = builder.wakeupWord;
        this.wakeupWordArray = builder.wakeupWord.pinyin;
        this.threshold = builder.wakeupWord.threshold;
        this.majors = builder.wakeupWord.majors;
        this.dcheck = builder.wakeupWord.dcheck;
        this.ranges = builder.wakeupWord.ranges;
        this.wakeupResource = builder.wakeupResource;
        this.beamformingResource = builder.beamformingResource;
        this.sspeResource = builder.beamformingResource;
        this.oneshotConfig = builder.oneshotConfig;
        this.stateFrame = builder.stateFrame;
        this.rightMarginFrame = builder.rightMarginFrame;
        this.language = builder.language;
        this.boundary = builder.boundary;
        this.sspeType = builder.sspeType;
        this.isFourHost = builder.isFourHost;
        this.subWakeupWord = builder.subWakeupWord;
        this.disCallBackResultData = builder.disCallBackResultData;
        this.voiceQueueStrategy = builder.getMaxVoiceQueueSize();
        this.subWakeupWord = builder.subWakeupWord;
    }

}
