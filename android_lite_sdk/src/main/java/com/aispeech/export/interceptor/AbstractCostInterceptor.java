package com.aispeech.export.interceptor;

import com.aispeech.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class AbstractCostInterceptor implements IInterceptor {

    /**
     * 构造拦截器输入对象
     *
     * @param layer    sdk 层级，如：{@link Layer}
     * @param flowType 边界流向类型，取值：{@link FlowType}
     * @param custom   自定义参数
     * @return JSONObject
     */
    public static JSONObject getInputObj(String layer, String flowType, JSONObject custom) {
        try {
            return new JSONObject()
                    .put(Layer.KEY, layer)
                    .put(FlowType.KEY, flowType)
                    .put("custom", custom);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Object intercept(final JSONObject inputObj) {
        final String layer = inputObj.optString(Layer.KEY);
        final String flowType = inputObj.optString(FlowType.KEY);

        Log.cost("intercept." + layer + "." + getName() + "." + flowType + ".start",
                "intercept." + layer + "." + getName() + "." + flowType + ".end",
                new Log.CostCallback() {
                    @Override
                    public void doIt() {
                        try {
                            inputObj.put("custom", handleIntercept(layer, flowType, inputObj.optJSONObject("custom")));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, 10);
        Log.d(getName(), "handleIntercept() after: inputObj = " + inputObj);
        return inputObj;
    }

    /**
     * 自定义拦截处理逻辑
     *
     * @param layer    sdk 层级，如：{@link Layer}
     * @param flowType 边界流向类型，取值：{@link FlowType}
     * @param custom   自定义参数
     * @return JSONObject inputObj
     */
    public abstract JSONObject handleIntercept(String layer, String flowType, JSONObject custom);
}
