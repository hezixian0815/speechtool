package com.aispeech.auth;

import com.aispeech.common.AuthError;

/**
 * @author wuwei
 * @date 2019-09-25 11:42
 * @email wei.wu@aispeech.com
 */
public interface AIAuthListener {
    /**
     * 授权失败回调
     * @param errMsg 错误信息
     */
    void onFailure(AuthError.AUTH_ERR_MSG errMsg);

    /**
     * 授权成功回调
     */
    void onSuccess();
}
