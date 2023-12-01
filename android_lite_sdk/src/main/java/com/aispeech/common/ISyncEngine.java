package com.aispeech.common;

import com.aispeech.lite.base.BaseConfig;
import com.aispeech.lite.speech.EngineListener;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/7/13
 */
public interface ISyncEngine<C extends BaseConfig, T extends EngineListener> {

    /**
     * 初始化 -- 同步方法
     *
     * @param config   初始化配置
     * @param time     超时的时间，超时返回失败，单位为毫秒
     * @param listener 监听回调
     * @return
     */
    int initSync(C config, long time, T listener);

//    目前看没有这个需求 暂时不做
//    int destroySync();

}
