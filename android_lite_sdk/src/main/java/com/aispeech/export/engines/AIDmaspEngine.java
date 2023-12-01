package com.aispeech.export.engines;

import com.aispeech.export.config.AIDmaspConfig;
import com.aispeech.export.exception.IllegalPinyinException;
import com.aispeech.export.intent.AIDmaspIntent;
import com.aispeech.export.listeners.AIDmaspListener;
import com.aispeech.kernel.Dmasp;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.function.ICarFunction;

/**
 * @deprecated 废弃，参考{@link com.aispeech.export.engines2.AIDmaspEngine}
 */
@Deprecated
public class AIDmaspEngine {
    private final String TAG = "AIDmaspEngine";
    private static AIDmaspEngine sInstance = null;
    private com.aispeech.export.engines2.AIDmaspEngine dmaspEngine;

    /**
     * 主驾驶方位 doa
     */
    public final static int DMASP_CAR_DOA_MAIN = 1;
    /**
     * 副驾驶方位 doa
     */
    public final static int DMASP_CAR_DOA_COPILOT = 2;
    /**
     * 左后 doa
     */
    public final static int DMASP_CAR_DOA_LEFT_BACKSEAT = 3;
    /**
     * 右后 doa
     */
    public final static int DMASP_CAR_DOA_RIGHT_BACKSEAT = 4;

    /**
     * 定位模式,指向唤醒方位
     */
    public static final int DMASP_CAR_DRIVE_MODE_POSITIONING = ICarFunction.DRIVEMODE_POSITIONING;
    /**
     * 主驾模式,指向主驾驶方位
     */
    public static final int DMASP_CAR_DRIVE_MODE_MAIN = ICarFunction.DRIVEMODE_MAIN;
    /**
     * 副驾驶模式,指向副驾驶方位
     */
    public static final int DMASP_CAR_DRIVE_MODE_COPILOT = ICarFunction.DRIVEMODE_COPILOT;
    /**
     * 全车模式,无指向
     */
    public static final int DMASP_CAR_DRIVE_MODE_ENTIRE = ICarFunction.DRIVEMODE_ENTIRE;
    /**
     * 自由自核模式,无指向
     */
    public static final int DMASP_CAR_DRIVE_MODE_FREE_COMBINTION = ICarFunction.DRIVEMODE_FREE_COMBINTION;

    private AIDmaspEngine() {
        dmaspEngine = com.aispeech.export.engines2.AIDmaspEngine.getInstance();
    }

    /**
     * 4mic    唤醒引擎
     *
     * @return 唤醒引擎实例
     */
    public static synchronized AIDmaspEngine getInstance() {
        if (null == sInstance) {
            synchronized (AIDmaspEngine.class) {
                if (null == sInstance) {
                    sInstance = new AIDmaspEngine();
                }
            }
        }
        return sInstance;
    }

    public void init(AIDmaspConfig aiDmaspConfig, AIDmaspListener listener) {
        if (dmaspEngine != null) {
            dmaspEngine.init(aiDmaspConfig, listener);
        }
    }

    /**
     * 设置不使用内部录音机时可用，自行feed音频数据
     *
     * @param data 音频数据
     */
    public void feed(byte[] data) {
        if (dmaspEngine != null) {
            dmaspEngine.feed(data);
        }
    }

    /**
     * get value
     *
     * @param param key
     * @return value
     */
    public int getValueOf(String param) {
        if (dmaspEngine != null) {
            dmaspEngine.getValueOf(param);
        }

        return -1;
    }

    /**
     * 开启唤醒
     *
     * @param intent 唤醒启动配置信息
     */
    public void start(AIDmaspIntent intent) {
        if (dmaspEngine != null) {
            dmaspEngine.start(intent);
        }
    }

    /**
     * 开启唤醒，仅仅开启唤醒，并且是在前端信号处理正常的情况下开启唤醒
     */
    public void startNWakeup() {
        if (dmaspEngine != null) {
            dmaspEngine.startNWakeup();
        }
    }

    /**
     * 动态调整参数，具体请参照 demo
     *
     * @param wakeupWord 唤醒词，参数示例：["ni hao xiao chi","xiao bu xiao bu"]
     * @param threshold  唤醒词对应的阈值，参数示例：[0.2, 0.3]
     * @param majors     是否主唤醒词，主唤醒词为1，副唤醒词为0，如 [1,0]
     *                   设置主唤醒词后，内核会对唤醒词部分音频进行回溯
     * @throws IllegalPinyinException {@link IllegalPinyinException} 非法拼音异常
     */
    public void setWakeupWords(String[] wakeupWord, float[] threshold, int[] majors) throws IllegalPinyinException {
        if (dmaspEngine != null) {
            dmaspEngine.setWakeupWords(wakeupWord, threshold, majors);
        }
    }

    /**
     * 关闭前端信号处理和唤醒，如果使用内部录音机的话一并关闭
     */
    public void stop() {
        if (dmaspEngine != null) {
            dmaspEngine.stop();
        }
    }

    /**
     * 关闭唤醒，仅仅关闭唤醒
     */
    public void stopNWakeup() {
        if (dmaspEngine != null) {
            dmaspEngine.stopNWakeup();
        }
    }

    /**
     * 销毁唤醒内核
     */
    public void destroy() {
        if (dmaspEngine != null) {
            dmaspEngine.destroy();
        }
    }

    /**
     * 检查唤醒内核是否准备好，外部可以不关注
     *
     * @return true or false
     */
    public static boolean checkLibValid() {
        return Dmasp.isDmaspSoValid() && Utils.isUtilsSoValid();
    }

    /**
     * 获取唤醒资源是否带VAD状态流
     *
     * @return true or false
     */
    public boolean isWakeupSsp() {
        if (dmaspEngine != null) {
            dmaspEngine.isWakeupSsp();
        }
        return false;
    }


    /**
     * 定位模式下通知引擎回正 beamforming 指向，定位模式下需要外部在结束对话状态后通知到引擎内部
     */
    public void notifyDialogEnd() {
        resetDriveMode();
    }

    public void resetDriveMode() {
        if (dmaspEngine != null) {
            dmaspEngine.resetDriveMode();
        }
    }

    /**
     * 获取 驾驶模式
     *
     * @return 0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式，-1 错误，没有获取到
     */
    public synchronized int getDriveMode() {
        if (dmaspEngine != null) {
            return dmaspEngine.getDriveMode();
        }

        return -1;
    }

    /**
     * 设置驾驶模式， 0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式
     * @param driveMode
     */
    public synchronized void setDriveMode(int driveMode) {
        if (dmaspEngine != null) {
            dmaspEngine.setDriveMode(driveMode);
        }
    }

    /**
     * 设置驾驶模式，此方法主要为自由组合模式传入wakeupChannelMask参数
     *
     * @param driveMode         0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式，4为自由组合模式
     * @param wakeupChannelMask 自由驾驶模式的组合：整数的二进制表示
     *                          0001：主驾
     *                          0010：副驾
     *                          0100：左后
     *                          1000：右后
     *                          1111：主+副+左后+右后
     */
    public synchronized void setDriveMode(int driveMode, int wakeupChannelMask) {
        if (dmaspEngine != null) {
            dmaspEngine.setDriveMode(driveMode, wakeupChannelMask);
        }
    }

    /**
     * 在定位模式下，手动设置为主驾唤醒或者副驾唤醒
     *
     * @param doa 1为主驾唤醒; 2为副驾唤醒;
     */
    public synchronized void setDoaManually(int doa) {
        if (dmaspEngine != null) {
            dmaspEngine.setDoaManually(doa);
        }
    }

}
