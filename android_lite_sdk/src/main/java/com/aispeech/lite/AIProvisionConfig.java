package com.aispeech.lite;

import android.content.Context;
import android.text.TextUtils;

import com.aispeech.common.Util;

import java.io.File;

public class AIProvisionConfig {
	
	private static final String PROVISION_NAME = "provision.file";
	
	private String provisionName = PROVISION_NAME;
	
	/**
	 * 拷贝Provision资源
	 * @param ctx
	 * @return -1 资源拷贝失败
	 *          0 资源MD5一致，略过拷贝
	 *          1 资源拷贝完成
	 */
	public synchronized int prepareUpdateProvision(Context ctx) {
		if(TextUtils.isEmpty(provisionName)){
			provisionName = PROVISION_NAME;
		}
		return Util.copyResource(ctx, provisionName);
	}
	
	public AIProvisionConfig() {
	}
	
	/**
	 * 设置证书文件名(默认不用设置)
	 * @param provisionName
	 */
	void setProvisionName(Context ctx, String provisionName){
		if(TextUtils.isEmpty(provisionName)){
			return;
		}
		this.provisionName = provisionName;
	}
	
	/**
	 * 取得证书文件存放路径
	 * @return
	 */
	public String getProvisionPath(Context ctx){
		return Util.getResourceDir(ctx)+File.separator+ provisionName;
	}
	
	public String getProvisionName() {
		return provisionName;
	}
}
