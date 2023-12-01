package com.aispeech.auth;

import com.aispeech.common.AuthError;

/**
 * @author wuwei
 * @date 2019-09-27 09:30
 * @email wei.wu@aispeech.com
 */
public class ProfileState {
    private boolean valid = false;
    private AuthError.AUTH_ERR_MSG authErrMsg;
    private AUTH_TYPE authType = AUTH_TYPE.ONLINE;
    private int timesLimit = -1;

    public boolean isValid() {
        return valid;
    }

    public ProfileState setValid(boolean valid) {
        this.valid = valid;
        return this;
    }

    public AuthError.AUTH_ERR_MSG getAuthErrMsg() {
        return authErrMsg;
    }

    public ProfileState setAuthErrMsg(AuthError.AUTH_ERR_MSG authErrMsg) {
        this.authErrMsg = authErrMsg;
        return this;
    }

    public AUTH_TYPE getAuthType() {
        return authType;
    }

    public ProfileState setAuthType(AUTH_TYPE authType) {
        this.authType = authType;
        return this;
    }

    public int getTimesLimit() {
        return timesLimit;
    }

    public ProfileState setTimesLimit(int timesLimit) {
        this.timesLimit = timesLimit;
        return this;
    }

    public enum AUTH_TYPE {
        OFFLINE,TRIAL,ONLINE;
    }

    @Override
    public String toString() {
        return "ProfileState{" +
                "valid=" + valid +
                ", authErrMsg=" + authErrMsg +
                ", authType=" + authType +
                ", timesLimit=" + timesLimit +
                '}';
    }
}
