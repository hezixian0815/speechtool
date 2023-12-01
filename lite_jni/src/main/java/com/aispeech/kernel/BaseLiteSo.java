package com.aispeech.kernel;

import com.aispeech.util.CoreVersionUtil;

public abstract class BaseLiteSo {
    // 内核对应的地址
    protected long mEngineId = 0;

    protected abstract String JNIVersionInfo();


    protected void init(String cfg) {
        if (mEngineId != 0) {
            //用于获取正在运行中引擎的版本信息
            //引擎销毁,从集合中移除,引擎初始化成功加入集合
            CoreVersionUtil.getInstance().add(this);
        }
    }

    protected void destroyEngine() {
        //用于获取正在运行中引擎的版本信息
        //引擎销毁,从集合中移除,引擎初始化成功加入集合
        CoreVersionUtil.getInstance().remove(this);
    }


    public long getEngineId() {
        return mEngineId;
    }

    /**
     * 获取内核版本信息
     * @return
     */
    public String getVersionInfo() {
        if (mEngineId == 0) {
            return null;
        }
        return JNIVersionInfo();
    }

}
