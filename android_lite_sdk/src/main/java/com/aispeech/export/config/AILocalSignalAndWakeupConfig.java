package com.aispeech.export.config;

import com.aispeech.DUILiteConfig;
import com.aispeech.export.intent.AIWakeupIntent;

import java.util.Arrays;
import java.util.List;

public class AILocalSignalAndWakeupConfig {

    /**
     * oneshot回退的时间，单位为ms
     */
    private int rollBackTime = 0;

    /**
     * 设置唤醒词对应阈值，是否需要设置和唤醒资源有关系
     */
    private float[] threshold;

    /**
     * 设置唤醒词对应阈值，唤醒二次判决
     */
    private float[] ccThreshold;
    /**
     * 事件
     */
    private String env;

    /**
     * 设置大音量场景下的唤醒阈值，是否需要设置和唤醒资源有关系
     */
    private float[] lowThreshold;

    /**
     * 设置唤醒词
     */
    private String[] wakeupWord;
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

    /**
     * 设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0]
     */
    private int[] majors;

    /**
     * 设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验，如 [1,0,0]
     */
    private int[] dcheck;

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


    private int micType = -1;

    /**
     * 设置前端信号处理内核是否使用vad功能, default is true
     */
    private boolean vad = true;
    /**
     * 状态流帧数,影响识别阶段音频抑制生效时间，默认20帧状态
     */
    private int stateFrame = 20;

    /**
     * 缓存音频帧数,影响识别阶段音频抑制前后缓冲区长度，默认10帧音频,默认保留状态流前后10帧左右长度音频，有助于提升识别准确率
     */
    private int rightMarginFrame = 10;
    /**
     * 参考音路数
     */
    private int echoChannelNum = 0;
    private int mPreUploadRollbackTime = 1000;
    private int maxMessageQueueSize = -1;
    private NearWakeupConfig nearWakeupConfig = null;
    private boolean isSspe = true;
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

    public int getMicType() {
        return micType;
    }

    /**
     * 设置启用麦克风阵列类型。默认以录音机类型进行加载相应的模块
     *
     * @param type 麦克风阵列类型
     */
    public void setMicType(int type) {
        micType = type;
    }

    public int getMaxMessageQueueSize() {
        return maxMessageQueueSize;
    }

    /**
     * 设置消息队列最大长度
     * <ul>
     *     <li>默认-1 使用 {@linkplain DUILiteConfig#getMaxMessageQueueSize() DUILiteConfig#getMaxMessageQueueSize()} 的配置</li>
     *     <li>0表示不限制长度, 建议大于100</li>
     * </ul>
     * <p>动态库方法运行在一个单独的线程里，通过消息队列依次调用。
     * 在设备性能不好的设备上可以设置消息队列最大长度，防止算力不够导致内核无法及时处理完音频数据而导致内存过大的问题</p>
     *
     * @param maxMessageQueueSize 消息队列最大长度
     */
    public void setMaxMessageQueueSize(int maxMessageQueueSize) {
        this.maxMessageQueueSize = maxMessageQueueSize;
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
        return isSspe;
    }

    /**
     * 设置是否使用sspe
     *
     * @param sspe 是否使用sspe，默认为true
     * @deprecated
     */
    public void setUseSspe(boolean sspe) {
        isSspe = sspe;
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
     * 设置唤醒词以及是否作为主唤醒词，主唤醒词为1,副唤醒词为0
     *
     * @param wakeupWord 唤醒词，如 ["ni hao xiao chi", "ni hao xiao le","bu ding bu ding"]
     *                   还需要设置唤醒词相应的阈值{@link  #setThreshold(float[])} 和 {@link #setLowThreshold(float[])}
     * @param majors     是否是主唤醒词，如 [1,0,0]
     */
    public void setWakeupWord(String[] wakeupWord, int[] majors) {
        this.wakeupWord = wakeupWord;
        this.majors = majors;
        if (this.wakeupWord != null) {
            this.subwordWakeup = new int[this.wakeupWord.length];
        }
    }

    /**
     * 设置唤醒词，以及是否是主副唤醒词，以及是否是半字唤醒
     *
     * @param wakeupWord    唤醒词，如 ["ni hao xiao chi", "ni hao xiao le","bu ding bu ding"]
     *                      *                   还需要设置唤醒词相应的阈值{@link  #setThreshold(float[])} 和 {@link #setLowThreshold(float[])}
     * @param majors        是否是主唤醒词，如 [1,0,0]
     * @param subwordWakeup 是否是半字唤醒词，是的话设置为1，不是的话是0, 如[0,0,1]
     */
    public void setWakeupWord(String[] wakeupWord, int[] majors, int[] subwordWakeup) {
        this.wakeupWord = wakeupWord;
        this.majors = majors;
        this.subwordWakeup = subwordWakeup;
    }

    public int[] getSubwordWakeup() {
        return subwordWakeup;
    }

    /**
     * 使用云端ASR进行唤醒校验时需要设置 中文唤醒词和高阈值
     *
     * @param cnWakeupWord            中文唤醒词
     * @param highThreshold           高阈值，当本地唤醒的 confidence 高于高阈值时不需要进行云端ASR校验
     * @param cloudWakeupCheckTimeout 唤醒进行云端check时的超时设置，单位 毫秒，默认 600 毫秒
     * @param preUploadRollbackTime   半字唤醒缓存的时间长度， 默认1000ms
     */
    public void setCloudWakeupCheck(String[] cnWakeupWord, float[] highThreshold, int cloudWakeupCheckTimeout, int preUploadRollbackTime) {
        this.cnWakeupWord = cnWakeupWord;
        this.highThreshold = highThreshold;
        this.cloudWakeupCheckTimeout = cloudWakeupCheckTimeout;
        mPreUploadRollbackTime = preUploadRollbackTime;
    }

    public String[] getCnWakeupWord() {
        return cnWakeupWord;
    }

    public float[] getHighThreshold() {
        return highThreshold;
    }

    public int getCloudWakeupCheckTimeout() {
        return cloudWakeupCheckTimeout;
    }

    public int getRollBackTime() {
        return rollBackTime;
    }

    /**
     * oneshot回退的时间，单位为ms(只有主唤醒词才会回退音频,即major为1)
     *
     * @param rollBackTime 回退的时间，单位为ms.
     */
    public void setRollBackTime(int rollBackTime) {
        this.rollBackTime = rollBackTime;
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

    public float[] getCcThreshold() {
        return ccThreshold;
    }

    /**
     * 设置唤醒词对应阈值，是唤醒二次判决
     *
     * @param ccThreshold 置信度
     */
    public void setCcThreshold(float[] ccThreshold) {
        this.ccThreshold = ccThreshold;
    }

    public float[] getLowThreshold() {
        return lowThreshold;
    }


    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }


    /**
     * 设置大音量场景下的预唤醒阈值，是否需要设置和唤醒资源有关系
     *
     * @param lowThreshold lowThreshold
     */
    public void setLowThreshold(float[] lowThreshold) {
        this.lowThreshold = lowThreshold;
    }

    public String[] getWakeupWord() {
        return wakeupWord;
    }

    public int[] getMajors() {
        return majors;
    }

    public int[] getDcheck() {
        return dcheck;
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
     * @deprecated 2.23.0版本之后废弃
     */
    @Deprecated
    public void setBeamformingResource(String beamformingResource) {
        this.beamformingResource = beamformingResource;
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
     * @deprecated 2.23.0版本之后废弃
     */
    @Deprecated
    public void setAecResource(String aecResource) {
        this.aecResource = aecResource;
    }

    public void setWakeupword(List<WakeupWord> wakeupWords) {
        if (wakeupWords == null || wakeupWords.isEmpty())
            return;
        setWakeupword(wakeupWords.toArray(new WakeupWord[wakeupWords.size()]));
    }

    /**
     * 设置唤醒词信息{@code config.setWakeupword(new WakeupWord("ni hao xiao le", 0.34f, 0.25f, 1, 0));}
     *
     * @param wakeupWords 配置的唤醒词
     */
    public void setWakeupword(WakeupWord... wakeupWords) {
        if (wakeupWords == null || wakeupWords.length == 0)
            return;
        wakeupWord = new String[wakeupWords.length];
        threshold = new float[wakeupWords.length];
        lowThreshold = new float[wakeupWords.length];
        majors = new int[wakeupWords.length];
        dcheck = new int[wakeupWords.length];
        for (int i = 0; i < wakeupWords.length; i++) {
            WakeupWord w = wakeupWords[i];
            wakeupWord[i] = w.getWakeupWord();
            threshold[i] = w.getThreshold();
            lowThreshold[i] = w.getLowThreshold();
            majors[i] = w.getMajor();
            dcheck[i] = w.getDcheck();
        }
    }

    public boolean isVad() {
        return vad;
    }

    /**
     * 设置前端信号处理内核是否使用vad功能
     *
     * @param vad 内核是否使用vad功能，default is true
     */
    public void setVad(boolean vad) {
        this.vad = vad;
    }

    public int getEchoChannelNum() {
        return echoChannelNum;
    }

    /**
     * 设置参考音路数，默认为0 即不含参考音
     *
     * @param echoChannelNum 参考音路数
     * @see AILocalSignalAndWakeupConfig#setAecResource(String)
     */
    public void setEchoChannelNum(int echoChannelNum) {
        this.echoChannelNum = echoChannelNum;
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

    @Override
    public String toString() {
        return "AILocalSignalAndWakeupConfig{" +
                "rollBackTime=" + rollBackTime +
                ", threshold=" + Arrays.toString(threshold) +
                ", ccThreshold=" + Arrays.toString(ccThreshold) +
                ", lowThreshold=" + Arrays.toString(lowThreshold) +
                ", wakeupWord=" + Arrays.toString(wakeupWord) +
                ", subwordWakeup=" + Arrays.toString(subwordWakeup) +
                ", cnWakeupWord=" + Arrays.toString(cnWakeupWord) +
                ", highThreshold=" + Arrays.toString(highThreshold) +
                ", cloudWakeupCheckTimeout=" + cloudWakeupCheckTimeout +
                ", majors=" + Arrays.toString(majors) +
                ", dcheck=" + Arrays.toString(dcheck) +
                ", wakeupResource='" + wakeupResource + '\'' +
                ", beamformingResource='" + beamformingResource + '\'' +
                ", aecResource='" + aecResource + '\'' +
                ", sspeResource='" + sspeResource + '\'' +
                ", micType=" + micType +
                ", vad=" + vad +
                ", echoChannelNum=" + echoChannelNum +
                ", maxMessageQueueSize=" + maxMessageQueueSize +
                ", nearWakeupConfig=" + nearWakeupConfig +
                ", threshHigh=" + Arrays.toString(threshHigh) +
                ", threshLow=" + Arrays.toString(threshLow) +
                ", customNet=" + Arrays.toString(customNet) +
                ", enableNet=" + Arrays.toString(enableNet) +
                ", mPreUploadRollbackTime=" + mPreUploadRollbackTime +
                ", env=" + env +
                '}';
    }

    public NearWakeupConfig getNearWakeupConfig() {
        return nearWakeupConfig;
    }

    /**
     * 就近唤醒的配置，包含 net 和 mds 的配置
     *
     * @param nearWakeupConfig 配置
     */
    public void setNearWakeupConfig(NearWakeupConfig nearWakeupConfig) {
        this.nearWakeupConfig = nearWakeupConfig;
    }


    public int getPreUploadRollbackTime() {
        return mPreUploadRollbackTime;
    }


    /**
     * 是否注册接口的标记
     */
    private boolean implWakeupCk = true;
    private boolean implMultiBfCk = true;
    private boolean implOutputCk = true;
    private boolean implInputCk = true;
    private boolean implBfCk = true;
    private boolean implDoaCk = true;
    private boolean implVprintCutCk = true;
    private boolean implEchoCk = true;
    private boolean implEchoVoipCk = true;
    private boolean implSevcDoaCk = true;
    private boolean implSevcNoiseCk = true;
    private boolean implVoipCk = true;
    private boolean implAgcCk = true;
    public boolean implVadCk = true;

    public boolean isImplVadCk() {
        return implVadCk;
    }

    /**
     * 设置是否回调输出vad结果
     *
     * @param implVadCk
     */
    public void setImplVadCk(boolean implVadCk) {
        this.implVadCk = implVadCk;
    }

    public boolean isImplMultiBfCk() {
        return implMultiBfCk;
    }

    /**
     * 设置是否回调输出多路beamforming音频
     *
     * @param implMultiBfCk true 是  false 否
     */
    public void setImplMultiBfCk(boolean implMultiBfCk) {
        this.implMultiBfCk = implMultiBfCk;
    }

    public boolean isImplWakeupCk() {
        return implWakeupCk;
    }

    /**
     * 设置是否回调输出唤醒回调，如果关掉了则无法唤醒
     *
     * @param implWakeupCk true 是  false 否
     */
    public void setImplWakeupCk(boolean implWakeupCk) {
        this.implWakeupCk = implWakeupCk;
    }

    public boolean isImplOutputCk() {
        return implOutputCk;
    }

    /**
     * 设置是否回调输出output音频回调，返回内核定制资源输出的音频数据
     *
     * @param implOutputCk true 是  false 否
     */
    public void setImplOutputCk(boolean implOutputCk) {
        this.implOutputCk = implOutputCk;
    }

    public boolean isImplInputCk() {
        return implInputCk;
    }

    /**
     * 设置是否回调输出送入内核的参考音频
     *
     * @param implInputCk true 是  false 否
     */
    public void setImplInputCk(boolean implInputCk) {
        this.implInputCk = implInputCk;
    }

    public boolean isImplBfCk() {
        return implBfCk;
    }

    /**
     * 设置是否输出Beamforming音频
     *
     * @param implBfCk true 是  false 否
     */
    public void setImplBfCk(boolean implBfCk) {
        this.implBfCk = implBfCk;
    }

    public boolean isImplDoaCk() {
        return implDoaCk;
    }

    /**
     * 设置是否输出doa
     *
     * @param implDoaCk true 是  false 否
     */
    public void setImplDoaCk(boolean implDoaCk) {
        this.implDoaCk = implDoaCk;
    }

    public boolean isImplVprintCutCk() {
        return implVprintCutCk;
    }

    /**
     * 设置是否输出用于声纹的音频
     *
     * @param implVprintCutCk true 是  false 否
     */
    public void setImplVprintCutCk(boolean implVprintCutCk) {
        this.implVprintCutCk = implVprintCutCk;
    }

    public boolean isImplEchoCk() {
        return implEchoCk;
    }

    /**
     * 设置是否输出消除回路之后的音频 数据
     *
     * @param implEchoCk true 是  false 否
     */
    public void setImplEchoCk(boolean implEchoCk) {
        this.implEchoCk = implEchoCk;
    }

    public boolean isImplEchoVoipCk() {
        return implEchoVoipCk;
    }

    /**
     * 设置是否输出经过回声消除的送给VoIP使用的音频数据
     *
     * @param implEchoVoipCk true 是  false 否
     */
    public void setImplEchoVoipCk(boolean implEchoVoipCk) {
        this.implEchoVoipCk = implEchoVoipCk;
    }

    public boolean isImplSevcDoaCk() {
        return implSevcDoaCk;
    }

    /**
     * 设置是否输出信号处理后语音通信的beam index信息
     *
     * @param implSevcDoaCk true 是  false 否
     */
    public void setImplSevcDoaCk(boolean implSevcDoaCk) {
        this.implSevcDoaCk = implSevcDoaCk;
    }

    public boolean isImplSevcNoiseCk() {
        return implSevcNoiseCk;
    }

    /**
     * 设置是否输出信号处理估计噪声最大的beam index 信息和该方向的音量信息，为 json 字符串
     *
     * @param implSevcNoiseCk true 是  false 否
     */
    public void setImplSevcNoiseCk(boolean implSevcNoiseCk) {
        this.implSevcNoiseCk = implSevcNoiseCk;
    }

    public boolean isImplAgcCk() {
        return implAgcCk;
    }

    /**
     * 设置是否输出经过前端信号处理放大后的agc音频数据
     *
     * @param implAgcCk true 是  false 否
     */
    public void setImplAgcCk(boolean implAgcCk) {
        this.implAgcCk = implAgcCk;
    }


}
