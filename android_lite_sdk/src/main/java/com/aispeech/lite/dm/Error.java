package com.aispeech.lite.dm;

import org.json.JSONObject;

/**
 * 对话错误
 *
 * @author hehr
 */
public class Error {

    private String errMsg;

    private String errId;

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public String getErrId() {
        return errId;
    }

    public void setErrId(String errId) {
        this.errId = errId;
    }

    public Error(String errMsg, String errId) {
        setErrId(errId);
        setErrMsg(errMsg);
    }

    private static final String MSG = "errMsg";

    private static final String ID = "errId";

    public static Error transform(JSONObject object) {
        return new Error(object.optString(MSG), object.optString(ID));
    }

    @Override
    public String toString() {
        return "Error{" +
                "errMsg='" + errMsg + '\'' +
                ", errId='" + errId + '\'' +
                '}';
    }
}
