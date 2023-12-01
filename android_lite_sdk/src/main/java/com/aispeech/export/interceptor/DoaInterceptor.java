package com.aispeech.export.interceptor;

import org.json.JSONObject;

/**
 * 唤醒消息拦截器
 */
public abstract class DoaInterceptor extends AbstractCostInterceptor {

    @Override
    public String getName() {
        return Name.DOA;
    }

    @Override
    public JSONObject handleIntercept(String layer, String flowType, JSONObject custom) {
        onIntercept(layer, flowType, custom, custom == null ? -1 : custom.optInt(getName()));
        return custom;
    }

    /**
     * 自定义拦截处理逻辑
     *
     * @param layer    sdk 层级，如：{@link Layer}
     * @param flowType 边界流向类型，取值：{@link FlowType}
     * @param custom   自定义参数
     * @param doa      doa 位置
     */
    public abstract void onIntercept(String layer, String flowType, JSONObject custom, int doa);
}
