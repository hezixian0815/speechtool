package com.aispeech.lite.vprintlite;

import com.aispeech.AIResult;
import com.aispeech.lite.BaseListener;

/**
 * 声纹模块回调接口
 */
public interface VprintLiteKernelListener extends BaseListener {
    void onResults(AIResult result);
}
