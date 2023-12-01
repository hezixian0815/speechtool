package com.aispeech.export.intent;


import com.aispeech.base.IFespxEngine;
import com.aispeech.lite.base.BaseIntent;

/**
 * LocalAsrpp start 的配置信息
 */
public class AILocalAsrppIntent extends BaseIntent {

    /**
     * 无语音超时时长，单位毫秒，default is {@value}ms
     */
    private int noSpeechTimeOut = 5000;
    /**
     * 允许的最大录音时长 单位秒,default is {@value}s
     */
    private int maxSpeechTimeS = 60;

    /**
     * 音量检测
     */
    private boolean volumeCheck = false;
    /**
     * env字符串,
     */
    private String env;


    private IFespxEngine fespxEngine;


    public IFespxEngine getFespxEngine() {
        return fespxEngine;
    }

    /**
     * 设置关联的信号处理引擎AILocalSignalAndWakeupEngine实例，只在使用内部录音机且多麦模式下才需要设置
     * @throws RuntimeException 内部录音机且多麦模式下没设置
     * @param fespxEngine 引擎实例
     */
    public void setFespxEngine(IFespxEngine fespxEngine) {
        this.fespxEngine = fespxEngine;
    }

    public String getEnv() {
        return env;
    }

    /**
     * 直接设置env会覆盖其他的参数例如setVolumeCheck
     * @param env
     */
    public void setEnv(String env) {
        this.env = env;
    }

    /**
     * 设置无语音超时时长，单位毫秒，默认值为5000ms ；如果达到该设置值时，自动停止录音并放弃请求识别内核
     *
     * @param milliSecond 超时时长，单位毫秒
     */
    public void setNoSpeechTimeOut(int milliSecond) {
        this.noSpeechTimeOut = milliSecond;
    }

    public int getNoSpeechTimeOut() {
        return noSpeechTimeOut;
    }

    /**
     * 设置音频最大录音时长，达到该值将取消语音引擎并抛出异常<br>
     * 允许的最大录音时长 单位秒
     * <ul>
     * <li>0 表示无最大录音时长限制</li>
     * <li>默认大小为60S</li>
     * </ul>
     *
     * @param seconds maxSpeechTimeS
     */
    public void setMaxSpeechTimeS(int seconds) {
        this.maxSpeechTimeS = seconds;
    }

    public int getMaxSpeechTimeS() {
        return maxSpeechTimeS;
    }

    public boolean isVolumeCheck() {
        return volumeCheck;
    }

    /**
     * 音量检测
     * @param volumeCheck  默认是false
     */
    public void setVolumeCheck(boolean volumeCheck) {
        this.volumeCheck = volumeCheck;
    }

    @Override
    public String toString() {
        return "AILocalAsrppIntent{" +
                "useCustomFeed=" + useCustomFeed +
                ", noSpeechTimeOut=" + noSpeechTimeOut +
                ", maxSpeechTimeS=" + maxSpeechTimeS +
                ", volumeCheck=" + volumeCheck +
                ", env=" + env +
                '}';
    }
}
