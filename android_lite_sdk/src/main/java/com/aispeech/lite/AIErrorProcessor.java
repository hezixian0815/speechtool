/**
 * 
 */
package com.aispeech.lite;

import android.text.TextUtils;

import org.json.JSONObject;

import java.util.Locale;

/**
 * 类说明： 处理返回的错误
 * 
 * @author Everett Li
 * @date 2014年8月4日
 * @version 1.0
 */
public class AIErrorProcessor {

    /**
     * 处理错误回调
     * 
     * @param res
     * @return
     */
    public boolean processErrorCallbak(final String res) {
        if (TextUtils.isEmpty(res)) {
            return false;
        }
        String lowRes = res.toLowerCase(Locale.ENGLISH);
        boolean errCallback = false;
        try {
            if (!TextUtils.isEmpty(lowRes)) {
                JSONObject jo = new JSONObject(lowRes);
                if (jo.has("errid") || jo.has("error")) {
                    errCallback = true;
                } else {
                    JSONObject resultJso = jo.optJSONObject("result");
                    if (resultJso != null && (resultJso.has("errid") || resultJso.has("errorid"))) {
                        errCallback = true;
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return errCallback;
    }
}
