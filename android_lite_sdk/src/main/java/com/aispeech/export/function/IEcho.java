package com.aispeech.export.function;

import com.aispeech.export.config.EchoConfig;
import com.aispeech.export.engines2.listeners.AILocalEchoListener;
import com.aispeech.export.intent.AILocalEchoIntent;

/**
 * @Description:
 * @Author: junlong.huang
 * @CreateTime: 2022/8/15
 */
public interface IEcho extends IEngine<EchoConfig, AILocalEchoIntent, AILocalEchoListener> {


    void feed(byte[] data);

}
