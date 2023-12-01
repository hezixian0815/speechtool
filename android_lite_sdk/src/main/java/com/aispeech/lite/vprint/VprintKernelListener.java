package com.aispeech.lite.vprint;

import com.aispeech.AIResult;
import com.aispeech.lite.BaseListener;

/**
 * 声纹模块回调接口
 */
public interface VprintKernelListener extends BaseListener {
    void onResults(AIResult result);
}
