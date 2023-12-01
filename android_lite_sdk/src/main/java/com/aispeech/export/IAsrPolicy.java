package com.aispeech.export;

import com.aispeech.AIResult;

/**
 * 识别的仲裁策略
 * */
public interface IAsrPolicy {
    /**
     * 识别仲裁结果
     * @param result 原始多路识别结果
     * @return AIResult 仲裁后的识别结果
     * @deprecated 已启用，推荐使用 {@link #onAsrResult}
     * */
    @Deprecated
    AIResult onAsr(AIResult result);

    /**
     * 识别仲裁结果
     * @param result 原始多路识别结果
     * @return String 仲裁后的识别结果
     * */
    String onAsrResult(AIResult result);
}
