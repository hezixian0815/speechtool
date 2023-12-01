package com.aispeech.export.engines;

import com.aispeech.export.config.AIFespCarConfig;
import com.aispeech.export.intent.AIFespCarIntent;
import com.aispeech.export.listeners.AIFespCarListener;
import com.aispeech.lite.fespx.FespxProcessor;
import com.aispeech.lite.function.ICarFunction;

/**
 * Created by wanbing on 2021/9/6 11:57
 */
@Deprecated
public class AIFespCarEngine {
    /**
     * 定位模式,beamforming 全指向
     */
    public static final int FESP_CAR_DRIVE_MODE_POSITIONING = com.aispeech.export.engines2.AIFespCarEngine.FESP_CAR_DRIVE_MODE_POSITIONING;
    /**
     * 主驾模式,beamforming指向主驾驶方位
     */
    public static final int FESP_CAR_DRIVE_MODE_MAIN = com.aispeech.export.engines2.AIFespCarEngine.FESP_CAR_DRIVE_MODE_MAIN;
    /**
     * 副驾驶模式,beamforming 指向副驾驶方位
     */
    public static final int FESP_CAR_DRIVE_MODE_COPILOT = com.aispeech.export.engines2.AIFespCarEngine.FESP_CAR_DRIVE_MODE_COPILOT;
    /**
     * 全车模式,beamforming无指向
     */
    public static final int FESP_CAR_DRIVE_MODE_ENTIRE = com.aispeech.export.engines2.AIFespCarEngine.FESP_CAR_DRIVE_MODE_ENTIRE;

    /**
     * 主驾驶方位 doa
     */
    public static int FESP_CAR_DOA_MAIN = ICarFunction.CAR_DOA_MAIN;
    /**
     * 副驾驶方位 doa
     */
    public static int FESP_CAR_DOA_COPILOT = ICarFunction.CAR_DOA_COPILOT;

    private com.aispeech.export.engines2.AIFespCarEngine fespCarEngine;

    private AIFespCarEngine() {
        fespCarEngine = com.aispeech.export.engines2.AIFespCarEngine.getInstance();
    }

    /**
     * 创建前端信号处理引擎对象
     *
     * @return AILocalSignalAndWakeupEngine  返回实例对象，可支持多实例
     */
    public static AIFespCarEngine getInstance() {
        return new AIFespCarEngine();
    }

    public void init(AIFespCarConfig config, AIFespCarListener listener) {
        if (fespCarEngine != null) {
            fespCarEngine.init(config, listener);
        }
    }

    /**
     * 启动信号处理模块和唤醒引擎
     *
     * @param aiFespCarIntent 前端信号处理参数
     * @see AIFespCarIntent
     */
    public void start(AIFespCarIntent aiFespCarIntent) {
        if (fespCarEngine != null) {
            fespCarEngine.start(aiFespCarIntent);
        }
    }

    /**
     * get value
     *
     * @param param key
     * @return value
     */
    public int getValueOf(String param) {
        if (fespCarEngine != null) {
            fespCarEngine.getValueOf(param);
        }
        return -1;
    }

    /**
     * 开启/关闭声纹唤醒词截断
     *
     * @param boundary 0:关闭，1：开启
     */
    public void setBoundary(int boundary) {
        if (fespCarEngine != null) {
            fespCarEngine.setBoundary(boundary);
        }
    }

    /**
     * 设置唤醒词
     *
     * @param words      唤醒词
     * @param thresholds 阈值
     */
    public void setWakeupWords(String[] words, float[] thresholds) {
        if (fespCarEngine != null) {
            fespCarEngine.setWakeupWords(words, thresholds);
        }
    }

    /**
     * 设置唤醒词
     *
     * @param words      唤醒词
     * @param thresholds 阈值
     * @param majors     1主 0副
     */
    public void setWakeupWords(String[] words, float[] thresholds, int[] majors) {
        if (fespCarEngine != null) {
            fespCarEngine.setWakeupWords(words, thresholds, majors);
        }
    }

    /**
     * 设置唤醒词
     *
     * @param words      唤醒词
     * @param thresholds 阈值
     * @param majors     1主 0副
     * @param ranges     是否定位模式下切换音区，1切换， 0 不切换
     */
    public void setWakeupWords(String[] words, float[] thresholds, int[] majors, int[] ranges) {
        if (fespCarEngine != null) {
            fespCarEngine.setWakeupWords(words, thresholds, majors, ranges);
        }
    }


    /**
     * 动态设置唤醒env，在其中加入 driveMode 参数
     * 设置模式后，会取消beamforming指向。
     * <p>
     * 注意：设置为定位模式后，会根据唤醒角度自动设置为主驾或者副驾模式，
     * 对话完成后需要调用 {@link #notifyDialogEnd()}
     *
     * @param driveMode 0为定位模式,按照声源定位; 1为主驾模式; 2为副驾模式; 3为全车模式
     */
    public synchronized void setDriveMode(int driveMode) {
        if (fespCarEngine != null) {
            fespCarEngine.setDriveMode(driveMode);
        }
    }

    /**
     * 在定位模式下，手动设置为主驾唤醒或者副驾唤醒
     *
     * @param doa 1为主驾唤醒; 2为副驾唤醒;
     */
    public synchronized void setDoaManually(int doa) {
        if (fespCarEngine != null) {
            fespCarEngine.setDoaManually(doa);
        }
    }

    /**
     * 定位模式下通知引擎回正 beamforming 指向，定位模式下需要外部在结束对话状态后通知到引擎内部,和 {@link AIFespCarIntent#setAutoHoldBeamforming(boolean)} 配置搭配使用
     */
    public void notifyDialogEnd() {
        if (fespCarEngine != null) {
            fespCarEngine.notifyDialogEnd();
        }
    }

    public void resetDriveMode() {
        if (fespCarEngine != null) {
            fespCarEngine.resetDriveMode();
        }
    }

    /**
     * 获取 驾驶模式，只有车载双麦模块有这个功能
     *
     * @return 0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式，-1 错误，没有获取到
     */
    public synchronized int getDriveMode() {
        if (fespCarEngine != null) {
            return fespCarEngine.getDriveMode();
        }
        return -1;
    }


    /**
     * 停止信号处理模块和唤醒引擎
     * 该方法会停止接收录音数据和停止信号处理，唤醒，程序退出时可以调用
     */
    public void stop() {
        if (fespCarEngine != null) {
            fespCarEngine.stop();
        }
    }

    /**
     * 销毁信号处理模块和唤醒引擎
     * 该方法会停止录音机和销毁录音机
     */
    public synchronized void destroy() {
        if (fespCarEngine != null) {
            fespCarEngine.destroy();
        }
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     * @param size 数据大小
     * @see AIFespCarIntent#setUseCustomFeed(boolean)
     */
    public void feedData(byte[] data, int size) {
        if (fespCarEngine != null) {
            fespCarEngine.feedData(data, size);
        }
    }

    public FespxProcessor getFespxProcessor() {
        if (fespCarEngine != null) {
            return fespCarEngine.getFespxProcessor();
        }
        return null;
    }
}
