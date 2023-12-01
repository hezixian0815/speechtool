package com.aispeech.export.listeners;

import com.aispeech.lite.nr.NRKernelListener;

/**
 * 降噪的回调类，feed音频后，经过降噪处理过的音频从 onBufferReceived 方法输出
 */
public interface AILocalNRListener extends NRKernelListener {
}
