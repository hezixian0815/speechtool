package com.aispeech.lite.function;

import com.aispeech.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/2/23
 */
public class CarFunctionHelper {

    private static final String TAG = "CarFunctionHelper";

    /**
     * 根据传入的音区，判断唤醒的doa是否可以抛出唤醒
     * 目前支持全车模式（音区锁定功能）和 自由组合模式
     *
     * @param doa
     * @return
     */
    public static boolean isCombinationEnable(int wakeupChannelMask, int doa) {

        if (wakeupChannelMask <= 0) return false;

        // 兼容老资源返回角度
        if (isDoaInMain(doa)) {
            return (wakeupChannelMask & ICarFunction.COMBINATION_POSITION_MAIN) > 0;
        }

        // 兼容老资源返回角度
        if (isDoaInCoPilot(doa)) {
            return (wakeupChannelMask & ICarFunction.COMBINATION_POSITION_COPILOT) > 0;
        }

        switch (doa) {
            case ICarFunction.CAR_DOA_LEFT_BACKSEAT:
                return (wakeupChannelMask & ICarFunction.COMBINATION_POSITION_LEFT_BACKSEAT) > 0;
            case ICarFunction.CAR_DOA_RIGHT_BACKSEAT:
                return (wakeupChannelMask & ICarFunction.COMBINATION_POSITION_RIGHT_BACKSEAT) > 0;
        }

        return false;
    }

    /**
     * 主驾模式，不响应副驾唤醒
     * 副驾模式，不响应主驾唤醒
     * 自由组合模式,根据doa与传入的mask 决定是否唤醒
     * 全车模式和定位模式未定位时可以全局响应唤醒
     * 新增全车模式也需要根据mask 进行判定是否唤醒
     *
     * @param driveMode       驾驶模式
     * @param cachedWakeUpDoa 缓存的唤醒角度
     * @param retString
     * @return
     */
    public static boolean filterWithDriveMode(int driveMode, int cachedWakeUpDoa, int wakeupChannelMask, String retString) {
        int doa = -1;

        // 定位模式直接返回
        if (driveMode == ICarFunction.DRIVEMODE_POSITIONING) {
            return true;
        }

        try {
            JSONObject resultJson = new JSONObject(retString);
            if (resultJson.has("doa")) {
                doa = resultJson.getInt("doa");
            }

            Log.i(TAG, "doa = " + doa + " ---driveMode = " + driveMode + "  ---mCachedWakeUpDoa = " + cachedWakeUpDoa + " --wakeupChannelMask:" + wakeupChannelMask);

            //主驾判断
            if (driveMode == ICarFunction.DRIVEMODE_MAIN
                    && isDoaInMain(doa)) {
                return true;
            }

            //副驾判断
            if (driveMode == ICarFunction.DRIVEMODE_COPILOT
                    && isDoaInCoPilot(doa)) {
                return true;
            }

            // 自有组合模式判断
            // 2023.06.08 长城需要支持音区锁定抛出特定混音，故而使全车模式也支持mask判断，与自由模式区别主要在抛出的音频是混音
            if ((driveMode == ICarFunction.DRIVEMODE_FREE_COMBINTION || driveMode == ICarFunction.DRIVEMODE_ENTIRE)
                    && CarFunctionHelper.isCombinationEnable(wakeupChannelMask, doa)) {
                return true;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 依照唤醒角度自动设置主副驾模式
     *
     * @param doa 唤醒角度
     * @return 主副驾模式
     */
    public static int transformDoa(int doa) {
        if (isDoaInMain(doa)) {
            return ICarFunction.DRIVEMODE_MAIN;
        }

        if (isDoaInCoPilot(doa)) {
            return ICarFunction.DRIVEMODE_COPILOT;
        }

        if (doa == ICarFunction.CAR_DOA_LEFT_BACKSEAT) {
            return ICarFunction.CAR_DOA_LEFT_BACKSEAT;
        }

        if (doa == ICarFunction.CAR_DOA_RIGHT_BACKSEAT) {
            return ICarFunction.CAR_DOA_RIGHT_BACKSEAT;
        }

        return -1;
    }

    /**
     * 主驾唤醒
     *
     * @return
     */
    public static boolean isDoaInMain(int doa) {
        return (doa >= 10 && doa <= 90) || (doa == 1);
    }

    /**
     * 副驾唤醒
     *
     * @return
     */
    public static boolean isDoaInCoPilot(int doa) {
        return (doa > 90 && doa <= 180) || (doa == 2);
    }

}
