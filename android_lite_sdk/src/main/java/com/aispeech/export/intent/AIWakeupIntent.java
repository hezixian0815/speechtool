package com.aispeech.export.intent;

import com.aispeech.export.config.AIOneshotConfig;
import com.aispeech.lite.base.BaseIntent;

import java.util.Arrays;

/**
 * wakeup 引擎 start 时的参数设置。
 */
public class AIWakeupIntent extends BaseIntent {

    /**
     * 是否使用oneshot功能,default is {@value}
     */
    private boolean isUseOneShot = false;

    /**
     * 设置是否输入实时的长音频，默认为true，接受长音频(如果是一二级唤醒，即每个唤醒词独立且非实时，则需要设置为false，如果不设置会影响性能)
     * 当设置为false时,每次送一段音频段都会给予是否唤醒的反馈，如果没有被唤醒，则抛出wakeupWord:null, confidence:0的信息
     */
    private boolean inputContinuousAudio = true;

    /**
     * feed段音频时是否增加额外的音频，目的是为了使本应唤醒的音频更容易唤醒
     */
    private boolean addExtraAudioWhenFeedNotContinuousAudio = false;


    /**
     * 设置唤醒词拼音列表
     */
    private String[] pinyin;

    /**
     * 唤醒词拼音对应的阈值
     */
    private float[] threshold;

    /**
     * 唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验
     */
    private String[] dcheck;


    /**
     * 设置双唤醒唤醒词是否使用自定义网络，"1"表示自定义网络，"0"表示主网络
     */
    private String[] customNet;

    /**
     * 设置双唤醒唤醒词对应的网络是否打开，"1"表示打开，"0"表示关闭
     */
    private String[] enableNet;

    /**
     * 设置双唤醒e2e低阈值，结合高阈值配套使用
     */
    private float[] threshLow;

    /**
     * 设置双唤醒e2e高阈值，结合低阈值配套使用
     */
    private float[] threshHigh;

    /**
     * 设置是否启动vad功能，需要资源配合。在资源支持vad功能的情况下，默认启动vad。<br>
     * vad 需引擎 stop 后重新 start 才能生效
     */
    private boolean vadEnable = true;

    /**
     * 设置原始音频的保存路径，比如/sdcard/speech,不设置并且没有设置全局的保存路径{@link com.aispeech.DUILiteConfig#setLocalSaveAudioPath(String)}，则不作保存
     */
    private String saveAudioFilePath;
    /**
     * dump唤醒音频保存的文件夹，比如/sdcard/speech/dumpwkp。不设置则不dump音频
     */
    private String dumpWakeupAudioPath;

    /**
     * 设置dump唤醒点回退音频的时间，默认5000ms。
     */
    private int dumpWakeupTime = 5000;

    /**
     * 设置是否使用oneshot功能,默认为false
     * 须在start之前设置才生效,默认为false
     */
    private AIOneshotConfig aiOneshotConfig;

    /**
     * 设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0]
     */
    private int[] majors;

    /**
     * 设置是否使用 oneshot 功能,default is false
     * <p>
     * oneshot 功能即 唤醒词+命令词，如用户说：你好小弛，打开设置。
     * “你好小弛”是唤醒词，唤醒回调出发，此时使用 asr识别，可以识别出“打开设置”
     * </p>
     *
     * @param useOneShot useOneShot true 使用 oneshot 功能，false(default) 不使用
     */
    public void setUseOneShot(boolean useOneShot) {
        this.isUseOneShot = useOneShot;
    }

    /**
     * 设置是否输入实时的长音频，默认接受长音频为true(如果是一二级唤醒，即每个唤醒词独立且非实时，则需要设置为false，如果不设置会影响性能)
     * <p>
     * 当设置为false时,每次送一段音频段都会给予是否唤醒的反馈，如果没有被唤醒，则抛出wakeupWord:null, confidence:0的信息
     * </p>
     *
     * @param inputContinuousAudio 是否输入实时的长音频
     */
    public void setInputContinuousAudio(boolean inputContinuousAudio) {
        this.inputContinuousAudio = inputContinuousAudio;
    }

    /**
     * 设置是否输入实时的长音频，默认接受长音频为true(如果是一二级唤醒，即每个唤醒词独立且非实时，则需要设置为false，如果不设置会影响性能)
     * <p>
     * 当设置为false时,每次送一段音频段都会给予是否唤醒的反馈，如果没有被唤醒，则抛出wakeupWord:null, confidence:0的信息
     * </p>
     * <p>
     * 当 inputContinuousAudio 设置为  false 时，可以设置 addExtraAudioWhenFeedNotContinuousAudio 为 true，
     * 会在段音频尾部再加上一些音频，使本应能够唤醒的音频更容易唤醒
     * </p>
     *
     * @param inputContinuousAudio                    是否输入实时的长音频,默认为true
     * @param addExtraAudioWhenFeedNotContinuousAudio false（默认）不增加额外音频，true 增加额外音频
     */
    public void setInputContinuousAudio(boolean inputContinuousAudio, boolean addExtraAudioWhenFeedNotContinuousAudio) {
        this.inputContinuousAudio = inputContinuousAudio;
        this.addExtraAudioWhenFeedNotContinuousAudio = addExtraAudioWhenFeedNotContinuousAudio;
    }

    /**
     * 设置唤醒词列表
     *
     * @param pinyin    唤醒词的拼音，建议三到五字，如：["ni hao xiao chi","xiao chi ni hao"]
     *                  当设置E2E双唤醒的情形下，E2E支持的唤醒词需要_做拼音的连接，如["ni_hao_xiao_chi","xiao_chi_xiao_chi"]
     * @param threshold 阈值，0-1，可根据需求自行调整，如：[0.1, 0.1]
     */
    public void setWakeupWord(String[] pinyin, float[] threshold) {
        this.pinyin = pinyin;
        this.threshold = threshold;
    }


    /**
     * 设置唤醒词列表
     *
     * @param wakeupWords 唤醒词的拼音，建议三到五字，如：["ni hao xiao chi","xiao chi ni hao"]
     */
    public void setWakeupWords(String[] wakeupWords) {
        this.pinyin = wakeupWords;
    }

    /**
     * 设置唤醒阈值
     *
     * @param threshold 阈值，0-1，可根据需求自行调整，如：[0.1, 0.1]
     */
    public void setThreshold(float[] threshold) {
        this.threshold = threshold;
    }



    /**
     * 设置唤醒词的major，主唤醒词为1,副唤醒词为0
     *
     * @param majors 如 [1,0,0]
     */
    public void setMajors(int[] majors) {
        this.majors = majors;
    }


    /**
     * 设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验
     *
     * @param dcheck 校验值，如：["1","0"]
     */
    public void setDcheck(String[] dcheck) {
        this.dcheck = dcheck;
    }

    /**
     * 设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验
     *
     * @param dcheck 校验值，如：[1,0]
     */
    public void setDcheck(int[] dcheck) {
        if (dcheck != null && dcheck.length > 0) {
            String[] dCheckNew = new String[dcheck.length];
            for(int i=0;i<dcheck.length;i++){
                dCheckNew[i] = String.valueOf(dcheck[i]);
            }
            this.dcheck = dCheckNew;
        }
    }

    public boolean isUseOneShot() {
        return isUseOneShot;
    }

    public boolean isInputContinuousAudio() {
        return inputContinuousAudio;
    }

    public String[] getPinyin() {
        return pinyin;
    }

    public float[] getThreshold() {
        return threshold;
    }

    public String[] getDcheck() {
        return dcheck;
    }

    public boolean isVadEnable() {
        return vadEnable;
    }

    /**
     * 设置是否启动vad功能，需要资源配合。在资源支持vad功能的情况下，默认启动vad。<br>
     * vad 需引擎 stop 后重新 start 才能生效
     *
     * @param vadEnable true 启动，false 不启动
     */
    public void setVadEnable(boolean vadEnable) {
        this.vadEnable = vadEnable;
    }

    public String getDumpWakeupAudioPath() {
        return dumpWakeupAudioPath;
    }

    /**
     * 设置dump唤醒音频保存的文件夹，比如/sdcard/speech/dumpwkp。不设置则不dump音频
     *
     * @param dumpWakeupAudioPath dump唤醒音频保存的文件夹
     */
    public void setDumpWakeupAudioPath(String dumpWakeupAudioPath) {
        this.dumpWakeupAudioPath = dumpWakeupAudioPath;
    }

    /**
     * 设置dump唤醒点回退音频时长，默认5000ms。
     *
     * @param dumpWakeupTime dump唤醒点回退音频的时长
     */
    public void setDumpWakeupTime(int dumpWakeupTime) {
        this.dumpWakeupTime = dumpWakeupTime;
    }

    public int getDumpWakeupTime() {
        return dumpWakeupTime;
    }

    public boolean isAddExtraAudioWhenFeedNotContinuousAudio() {
        return addExtraAudioWhenFeedNotContinuousAudio;
    }

    /**
     * 设置音频保存路径，会保存原始单声道音频
     * 如果设置了就会保存，全局{@link com.aispeech.DUILiteConfig#setLocalSaveAudioPath(String)}没设置不会保存
     *
     * @param saveAudioFilePath 文件夹路径
     * @deprecated 统一规范 这里不生效
     **/
    public void setSaveAudioFilePath(String saveAudioFilePath) {
        this.saveAudioFilePath = saveAudioFilePath;
    }

    public String getSaveAudioFilePath() {
        return saveAudioFilePath;
    }

    /**
     * 设置双唤醒,双唤醒即基于当前唤醒模型，对不同的唤醒词做更好的响应。普通的唤醒词对某些唤醒词响应较好，如对
     * "你好小驰"响应比较好，则对"小吃小吃"识别不是那么完美，双唤醒主要用于更好的兼容不同的唤醒词
     * 设置的数量必须和唤醒词保持数量一致{@link AIWakeupIntent#setWakeupWord(String[], float[])}
     *
     * @param customNet 设置唤醒词对应的网络，分为主网络和自定义网络，主网络是"0",自定义网络设置"1"
     * @param enableNet 设置是否启用当前唤醒词对应的网络，"0"为关闭,"1"为启用
     */
    public void enableDulWakeup(String[] customNet, String[] enableNet) {
        this.customNet = customNet;
        this.enableNet = enableNet;
    }

    /**
     * E2E提供两个计算网络，使用双计算网络综合计算，综合最后的结果对特定的唤醒词达到提高唤醒率与降低误唤醒率的
     * 作用。
     * Note:E2E仅支持资源内内置的唤醒词
     * 设置的数量必须和唤醒词数量保持一致{@link AIWakeupIntent#setWakeupWord(String[], float[])}
     * 同时，E2E要求同时设置普通唤醒词和E2E的唤醒词，E2E唤醒词用下划线做拼音之间的连接，如"ni_hao_xiao_chi"，
     * 即如果唤醒词为 “你好小驰”，则需要同时配置普通网络模型的唤醒词"ni hao xiao chi"，并将其网络模型设置为主网络
     * 即，"0";同时，设置E2E网络模型唤醒词"ni_hao_xiao_chi"，网络模型设置为"1"，这样才完成了一个基本的E2E
     * 双唤醒设置，可参考如下示例
     * {@code
     * setWakeupWord(new String[]{"ni hao xiao chi", "ni_hao_xiao_chi"}, new float[]{0.5f, 0.6f});
     * enableDulWakeupWithE2E(new String[] {"0","1"}, new String[] {"1", "1"}, new float[] {0.9f, 0.8f}, new float[] {0.1f, 0.2f});
     * }
     *
     * @param customNet  设置唤醒词对应的网络，分为主网络和自定义网络，主网络是"0",E2E网络设置"1"
     * @param enableNet  设置是否启用当前唤醒词对应的网络，"0"为关闭,"1"为启用
     * @param threshHigh 高唤醒阈值，用于E2E的内部计算，设置数据请咨询思必驰研究
     * @param threshLow  低唤醒阈值，用于E2E的内部计算，设置数据请咨询思必驰研究
     */
    public void enableDulWakeupWithE2E(String[] customNet, String[] enableNet, float[] threshHigh, float[] threshLow) {
        this.customNet = customNet;
        this.enableNet = enableNet;
        this.threshHigh = threshHigh;
        this.threshLow = threshLow;
    }

    public String[] getCustomNet() {
        return customNet;
    }

    public String[] getEnableNet() {
        return enableNet;
    }

    public float[] getThreshLow() {
        return threshLow;
    }

    public float[] getThreshHigh() {
        return threshHigh;
    }

    public int[] getMajors() {
        return majors;
    }

    /**
     * 此方法无效，请在初始化之前调用
     *
     * @param aiOneshotConfig
     */
    @Deprecated
    public void setAiOneshotConfig(AIOneshotConfig aiOneshotConfig) {
        this.aiOneshotConfig = aiOneshotConfig;
    }


    @Override
    public String toString() {
        return "AIWakeupIntent{" +
                "useCustomFeed=" + useCustomFeed +
                ", isUseOneShot=" + isUseOneShot +
                ", inputContinuousAudio=" + inputContinuousAudio +
                ", addExtraAudioWhenFeedNotContinuousAudio=" + addExtraAudioWhenFeedNotContinuousAudio +
                ", pinyin=" + Arrays.toString(pinyin) +
                ", threshold=" + Arrays.toString(threshold) +
                ", dcheck=" + Arrays.toString(dcheck) +
                ", threshHigh=" + Arrays.toString(threshHigh) +
                ", threshLow=" + Arrays.toString(threshLow) +
                ", customNet=" + Arrays.toString(customNet) +
                ", enableNet=" + Arrays.toString(enableNet) +
                ", vadEnable=" + vadEnable +
                ", dumpWakeupAudioPath='" + dumpWakeupAudioPath + '\'' +
                ", dumpWakeupTime=" + dumpWakeupTime +
                '}';
    }
}
