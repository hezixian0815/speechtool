package com.aispeech.kernel;

public interface LiteSoFunction {

    long init(String cfg, Object... callbacks);

    /**
     * 设置回调，多个回调全部成功才返回0，只要有一个失败则返回失败的错误码
     *
     * @param callbacks 回调
     * @return 0 全部成功，其它失败
     */
    int setCallback(Object... callbacks);

    int start(String param);

    int set(String setParam);

    int get(String getParam);

    int feed(byte[] data, int size);

    /**
     * 如果没有提供 cancel 的 jni 方法，可以用 stop 实现
     *
     * @return
     */
    int cancel();

    int stop();

    int destroy();

}
