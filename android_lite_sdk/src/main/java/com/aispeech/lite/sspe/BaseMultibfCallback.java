package com.aispeech.lite.sspe;

import com.aispeech.kernel.Sspe;
import com.aispeech.lite.fespx.FespxKernelListener;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/1/30
 */
public abstract class BaseMultibfCallback implements Sspe.multibf_callback {

    protected volatile int mDriveMode = -1;
    protected volatile int cachedDriveMode = -1;
    FespxKernelListener mListener;

    public void setListener(FespxKernelListener mListener) {
        this.mListener = mListener;
    }

    public void updateDriveMode(int mDriveMode, int cachedDriveMode) {
        this.mDriveMode = mDriveMode;
        this.cachedDriveMode = cachedDriveMode;
    }
}
