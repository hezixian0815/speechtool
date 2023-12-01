package com.aispeech.export.interceptor;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 唤醒消息拦截器
 */
public abstract class WakeupInterceptor extends AbstractCostInterceptor {

    @Override
    public String getName() {
        return Name.WAKEUP;
    }

    @Override
    public JSONObject handleIntercept(String layer, String flowType, JSONObject custom) {
        String wakeupResult = customIntercept(layer, flowType, custom, custom == null ? null : custom.optString(getName()));
        try {
            if (custom != null) {
                custom.put(getName(), wakeupResult);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return custom;
    }

    /**
     * 自定义拦截处理逻辑
     *
     * @param layer    sdk 层级，如：{@link Layer}
     * @param flowType 边界流向类型，取值：{@link FlowType}
     * @param custom   自定义参数
     * @return String 返回需要修改的唤醒结果
     */
    public abstract String customIntercept(String layer, String flowType, JSONObject custom, String wakeupResult);
}
