package com.aispeech.base;

import com.aispeech.lite.fespx.FespxProcessor;

/**
 * @author wuwei
 * @decription 多路唤醒的引擎
 * @date 2019-09-23 11:02
 * @email wei.wu@aispeech.com
 */
public interface IFespxEngine {
    FespxProcessor getFespxProcessor();
}
