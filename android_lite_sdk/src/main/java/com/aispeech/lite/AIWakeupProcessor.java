/**
 * 
 */
package com.aispeech.lite;

import android.text.TextUtils;

import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.export.interceptor.DoaInterceptor;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.export.interceptor.WakeupInterceptor;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 类说明： 唤醒结果处理器
 * 
 * @author Everett Li
 * @date 2014年8月12日
 * @version 1.0
 */
public class AIWakeupProcessor {

    public static final String TAG = "AIWakeupProcessor";
    public static final String KEY_WAKEUPWORD = "wakeupWord";
    public static final String KEY_CONF = "confidence";

    WakeupProcessorListener mListener;
    
    public AIWakeupProcessor(WakeupProcessorListener listener) {
        this.mListener = listener;
    }

    /**
     * 处理唤醒返回结果
     * 
     * @param wakeupRetString
     *            aiengine回调的结果
     */
    public void processWakeupCallback(String wakeupRetString) {
        try {
            JSONObject customObj = new JSONObject().put(IInterceptor.Name.WAKEUP, wakeupRetString);
            JSONObject inputObj = WakeupInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.RECEIVE, customObj);
            Object obj = SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.WAKEUP, inputObj);
            if (obj != null && obj instanceof JSONObject) {
                customObj = (JSONObject) obj;
                String interceptorResult = customObj.optString(IInterceptor.Name.WAKEUP);
                if (!TextUtils.isEmpty(interceptorResult)) {
                    wakeupRetString = interceptorResult;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        AIResult result = new AIResult();
        result.setLast(true);
        result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
        result.setResultObject(wakeupRetString);
        result.setTimestamp(System.currentTimeMillis());
        if (mListener != null) {
            if (wakeupRetString.contains(KEY_WAKEUPWORD)) {
                mListener.onWakeup(result);
            }
        }
    }

    /**
     * 处理唤醒type　0：没被唤醒 1:主唤醒词唤醒　2:副唤醒词唤醒
     * @param wakeupTypeRetString
     * @return
     */
    public int processWakeupType(String wakeupTypeRetString) {
        int wakeupType = 0;
        try {
            JSONObject resultJson = new JSONObject(wakeupTypeRetString);
            if (resultJson.has("wakeup_type")) {
                wakeupType = resultJson.optInt("wakeup_type");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return wakeupType;
    }


    public void processDoaResult(String doaRetString) {
        int doaResult;
        int doaType = AIConstant.DOA.TYPE_WAKEUP;
        try {
            JSONObject resultJson = new JSONObject(doaRetString);
            if (resultJson.has("doa")) {
                doaResult = resultJson.optInt("doa");
                if (resultJson.has("backType"))
                    doaType = resultJson.optInt("backType");
                try {
                    JSONObject inputObj = DoaInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.RECEIVE, new JSONObject().put(IInterceptor.Name.DOA, doaResult));
                    SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.DOA, inputObj);
                    Log.d(TAG, "input obj = " + inputObj.toString());
                    if (inputObj.has("custom")) {
                        JSONObject custom = inputObj.getJSONObject("custom");
                        if (custom.has("quickStartInDialog")) {
                            if (custom.getBoolean("quickStartInDialog")) {
                                Log.d(TAG, "quick start in dialog, return");
                                return;
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                if (mListener != null) {
                    mListener.onDoaResult(doaResult, doaType);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public interface WakeupProcessorListener {
        void onWakeup(AIResult result);

        //增加doaType，不影响原生逻辑，默认为0
        void onDoaResult(int doa, int type);
    }

}
