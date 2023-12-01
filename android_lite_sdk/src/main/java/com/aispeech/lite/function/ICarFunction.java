package com.aispeech.lite.function;

import java.util.List;

/**
 * Created by wanbing on 2021/9/7 17:28
 */
public interface ICarFunction {
    /**
     * sp 名字
     */
    String SP_NAME_CAR_CACHE = "car_cache";

    String SP_KEY_DRIVER_MODEL = "sspe_driver_model";

    String SP_KEY_WAKEUP_MASK = "sspe_wakeup_mask";

    /**
     * 定位模式
     */
    int DRIVEMODE_POSITIONING = 0;

    /**
     * 主驾模式
     */
    int DRIVEMODE_MAIN = 1;

    /**
     * 副驾模式
     */
    int DRIVEMODE_COPILOT = 2;

    /**
     * 全车模式
     */
    int DRIVEMODE_ENTIRE = 3;
    /**
     * 自由组合
     * 0001：主驾
     * 0010：副驾
     * 0100：左后
     * 1000：右后
     * 1111：主+副+左后+右后
     */
    int DRIVEMODE_FREE_COMBINTION = 4;

    /**
     * 主驾驶方位 doa
     */
    int CAR_DOA_MAIN = 1;
    /**
     * 副驾驶方位 doa
     */
    int CAR_DOA_COPILOT = 2;

    /**
     * 左后 doa
     */
    int CAR_DOA_LEFT_BACKSEAT = 3;
    /**
     * 右后 doa
     */
    int CAR_DOA_RIGHT_BACKSEAT = 4;


    int COMBINATION_POSITION_MAIN = 0x01;

    int COMBINATION_POSITION_COPILOT = 0x01 << 1;

    int COMBINATION_POSITION_LEFT_BACKSEAT = 0x01 << 2;

    int COMBINATION_POSITION_RIGHT_BACKSEAT = 0x01 << 3;

    int COMBINATION_POSITION_ENTIRE = 0b1111;

    int getDriveMode();

    void resetDriveMode();

    void setDriveMode(int driveMode);

    void setDriveMode(int driveMode, int wakeupChannelMask);

    /**
     * 手动设置唤醒角度
     *
     * @param doa 1：主驾唤醒 2：副驾唤醒
     */
    void setDoaManually(int doa);

    /**
     * 设置定位模式下唤醒后不切换音区的唤醒词
     *
     * @param rangesWords 不主动切换音区的词
     */
    void setRangesWords(List<String> rangesWords);

}
