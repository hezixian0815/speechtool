package com.aispeech.lite.sspe;

import android.util.Log;

import com.aispeech.lite.fespx.FespxKernelListener;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/2/27
 */
public class SspeKernelFactory {

    private String TAG = getClass().getSimpleName();

    public BaseSspeKernel getSspeKernel(FespxKernelListener listener, int sspeType) {
        Log.i(TAG, "getSspeKernel: " + sspeType);

        switch (sspeType) {
            case SspeConstant.SSPE_TYPE_CAR_TWO:
                return new CarTwoMicSspeKernel(listener);
            case SspeConstant.SSPE_TYPE_CAR_FOUR:
                return new CarFourMicSspeKernel(listener);
            default:
                return new SspeKernel(listener);
        }

    }

}
