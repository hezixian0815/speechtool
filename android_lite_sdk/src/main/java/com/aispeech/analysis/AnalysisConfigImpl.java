package com.aispeech.analysis;

import android.content.Context;

import com.aispeech.gourd.ConfigRequestBean;
import com.aispeech.gourd.Gourd;
import com.aispeech.lite.AISpeech;


/**
 * @author wuwei
 */
public class AnalysisConfigImpl implements IAnalysisConfig {
    private String uploadUrl;

    public AnalysisConfigImpl() {
        this(AISpeech.uploadUrl);
    }

    public AnalysisConfigImpl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    @Override
    public String getUploadConfig(Context context, int[] logId, String productId, String deviceId) {
        ConfigRequestBean requestBean = new ConfigRequestBean(logId);
        requestBean.setProductId(productId);
        requestBean.setDeviceId(deviceId);
        requestBean.setTimeOut(5000);
        requestBean.setHttpHeadUrl(uploadUrl);
        return Gourd.getConfigInfo(context, requestBean);
    }
}
