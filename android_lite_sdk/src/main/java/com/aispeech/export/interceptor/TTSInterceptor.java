package com.aispeech.export.interceptor;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * TTS 消息拦截器
 */
public abstract class TTSInterceptor extends AbstractCostInterceptor {


    @Override
    public JSONObject handleIntercept(String layer, String flowType, JSONObject custom) {
        String ttsResult = customIntercept(layer, flowType, custom, custom == null ? null : custom.optString(getName()));
        try {
            if (custom != null) {
                custom.put(getName(), ttsResult);
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
     * @return String 返回需要修改的 TTS 结果
     */
    public abstract String customIntercept(String layer, String flowType, JSONObject custom, String ttsResult);


}
