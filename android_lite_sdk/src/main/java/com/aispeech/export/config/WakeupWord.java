package com.aispeech.export.config;

public class WakeupWord {

    /**
     * 设置唤醒词
     */
    private String wakeupWord;

    /**
     * 设置唤醒词对应阈值，是否需要设置和唤醒资源有关系
     */
    private float threshold;

    /**
     * 设置大音量场景下的唤醒阈值，是否需要设置和唤醒资源有关系
     */
    private float lowThreshold;

    /**
     * 设置唤醒词的major，主唤醒词为1,副唤醒词为0，如 [1,0,0]
     */
    private int major;

    /**
     * 设置唤醒是否开启校验，"1"表示开启校验，"0"表示不开启校验，如 [1,0,0]
     */
    private int dcheck;


    public String getWakeupWord() {
        return wakeupWord;
    }

    /**
     * 设置唤醒词，例："ni hao xiao chi"
     *
     * @param wakeupWord 唤醒词
     */
    public void setWakeupWord(String wakeupWord) {
        this.wakeupWord = wakeupWord;
    }

    public float getThreshold() {
        return threshold;
    }

    /**
     * 设置唤醒词对应阈值，是否需要设置和唤醒资源有关系
     *
     * @param threshold 唤醒词的阈值，例：0.35f
     */
    public void setThreshold(float threshold) {
        this.threshold = threshold;
    }

    public float getLowThreshold() {
        return lowThreshold;
    }

    /**
     * 设置大音量场景下的唤醒阈值，是否需要设置和唤醒资源有关系
     *
     * @param lowThreshold 大音量场景下的唤醒阈值,例：0.21f
     */
    public void setLowThreshold(float lowThreshold) {
        this.lowThreshold = lowThreshold;
    }

    public int getMajor() {
        return major;
    }

    /**
     * 设置唤醒词的major，即主副唤醒词，主唤醒词唤醒后会回滚唤醒音频
     *
     * @param major 主唤醒词为1,副唤醒词为0 (default)
     */
    public void setMajor(int major) {
        this.major = major;
    }

    public int getDcheck() {
        return dcheck;
    }

    /**
     * 设置唤醒是否开启校验
     *
     * @param dcheck 是否开启校验，1表示开启校验，0表示不开启校验(default)
     */
    public void setDcheck(int dcheck) {
        this.dcheck = dcheck;
    }

    public WakeupWord() {
    }

    /**
     * 设置唤醒词
     *
     * @param wakeupWord 唤醒词，例："ni hao xiao chi"
     * @param threshold  唤醒词对应阈值，是否需要设置和唤醒资源有关系，例：0.35f
     * @param major      唤醒词的major，主唤醒词为1,副唤醒词为0 (default)
     */
    public WakeupWord(String wakeupWord, float threshold, int major) {
        this.wakeupWord = wakeupWord;
        this.threshold = threshold;
        this.major = major;
    }

    /**
     * 设置唤醒词
     *
     * @param wakeupWord 唤醒词，例："ni hao xiao chi"
     * @param threshold  唤醒词对应阈值，是否需要设置和唤醒资源有关系，例：0.35f
     * @param major      唤醒词的major，主唤醒词为1,副唤醒词为0 (default)
     * @param dcheck     唤醒是否开启校验，1表示开启校验，0表示不开启校验(default)
     */
    public WakeupWord(String wakeupWord, float threshold, int major, int dcheck) {
        this.wakeupWord = wakeupWord;
        this.threshold = threshold;
        this.major = major;
        this.dcheck = dcheck;
    }

    /**
     * 设置唤醒词
     *
     * @param wakeupWord   唤醒词，例："ni hao xiao chi"
     * @param threshold    唤醒词对应阈值，是否需要设置和唤醒资源有关系，例：0.35f
     * @param lowThreshold 大音量场景下的唤醒阈值，是否需要设置和唤醒资源有关系，例：0.21f
     * @param major        唤醒词的major，主唤醒词为1,副唤醒词为0 (default)
     */
    public WakeupWord(String wakeupWord, float threshold, float lowThreshold, int major) {
        this.wakeupWord = wakeupWord;
        this.threshold = threshold;
        this.lowThreshold = lowThreshold;
        this.major = major;
    }

    /**
     * 设置唤醒词
     *
     * @param wakeupWord   唤醒词，例："ni hao xiao chi"
     * @param threshold    唤醒词对应阈值，是否需要设置和唤醒资源有关系，例：0.35f
     * @param lowThreshold 大音量场景下的唤醒阈值，是否需要设置和唤醒资源有关系，例：0.21f
     * @param major        唤醒词的major，主唤醒词为1,副唤醒词为0 (default)
     * @param dcheck       唤醒是否开启校验，1表示开启校验，0表示不开启校验(default)
     */
    public WakeupWord(String wakeupWord, float threshold, float lowThreshold, int major, int dcheck) {
        this.wakeupWord = wakeupWord;
        this.threshold = threshold;
        this.lowThreshold = lowThreshold;
        this.major = major;
        this.dcheck = dcheck;
    }

    public boolean isValid() {
        return wakeupWord != null && wakeupWord.length() > 0;
    }

    @Override
    public String toString() {
        return "WakeupWord{" +
                "wakeupWord='" + wakeupWord + '\'' +
                ", threshold=" + threshold +
                ", lowThreshold=" + lowThreshold +
                ", major=" + major +
                ", dcheck=" + dcheck +
                '}';
    }
}
