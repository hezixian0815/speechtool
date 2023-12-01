package com.aispeech.export.function;


import com.aispeech.export.listeners.AILocalVprintLiteListener;
import com.aispeech.lite.vprintlite.VprintLiteConfig;
import com.aispeech.lite.vprintlite.VprintLiteIntent;

/**
 * 约束型接口，版本发布后请勿修改或者删除原方法，可添加新内容
 * Created by wanbing on 2021/9/28 16:22
 */
public interface IAILocalVprintLite {
    void init(VprintLiteConfig config, AILocalVprintLiteListener listener);

    void start(VprintLiteIntent intent);

    void stop();

    void cancel();

    void destroy();

    void feedData(byte[] data, int size, VprintLiteIntent intent);

    VprintLiteIntent.Action getAction();

    void notifyEvent(String event);

    void queryModel();

    void queryRegisterAudio(String name, String word);


}
