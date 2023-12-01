/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.param;

import android.os.Build;

import com.aispeech.common.DeviceUtil;
import com.aispeech.common.JSONUtil;

import org.json.JSONObject;

/**
 * 使用的应用程序配置，如果应用使用思必驰云端语音引擎，则必须配置此参数
 * <ul>
 * <li>设置appkey {@link #setAppKey(String)}</li>
 * <li>设置secretkey {@link #setSecretKey(String)}</li>
 * <li>设置userid {@link #setUserId(String)}</li>
 * </ul>
 */
public class AppParams {

	public static final String KEY_APP = "app";

	public static final String KEY_USER_ID = "userId";
	
	public static final String KEY_DEVICE_ID = "deviceId";
	
	public static final String KEY_DEVICE_INFO = "deviceInfo";

	public static final String KEY_SDCARD = "sdcard";

	public static final String KEY_CPU = "cpu";

	public static final String KEY_RAM = "ram";

	public static final String KEY_ROM = "rom";

	public static final String KEY_SYSTEM = "system";

	public static final String KEY_IMEI = "imei";

	public static final String KEY_MAC = "mac";

	public static final String KEY_AIENGINE_VERSION = "aiengineVersion";

	public static final String KEY_JAR_VERSION = "jarVersion";

	public static final String UNKNOWN_USER_ID = "";

//	public static int mCount = 0;



	private JSONObject JSON = new JSONObject();

	private String userId;
	private String deviceId;
	private String cpu;
	private String ram;
	private String rom;
	private String system;
	private JSONObject deviceInfo;

	public AppParams() {

	}

	/**
	 * 设置应用程序userId
	 * 
	 * @param userId
	 *                应用程序的userId
	 */
	public void setUserId(String userId) {
		this.userId = userId;
		JSONUtil.putQuietly(JSON, KEY_USER_ID, userId);
	}

	/**
	 * 获取应用userId
	 * 
	 * @return userId
	 */
	public String getUserId() {
		return userId;
	}
	
	/**
     * 设置应用程序deviceId
     * 
     * @param deviceId
     *                应用程序的deviceId
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
        JSONUtil.putQuietly(JSON, KEY_DEVICE_ID, deviceId);
    }

	public void setMac(String mac) {
//		if (mCount == 0)
			JSONUtil.putQuietly(JSON, KEY_MAC, mac);
	}

	public void setImei(String imei) {
//		if (mCount == 0)
			JSONUtil.putQuietly(JSON, KEY_IMEI, imei);
	}
    
    /**
     * 获取应用deviceId
     * 
     * @return deviceId
     */
    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceInfo(JSONObject deviceInfo){
        this.deviceInfo = deviceInfo;
        JSONUtil.putQuietly(JSON, KEY_DEVICE_INFO, deviceInfo);
    }
    
	public JSONObject getDeviceInfo(){
	    return deviceInfo;
	}

	public void setRam(String ram) {
		this.ram = ram;
//		if (mCount == 0)
			JSONUtil.putQuietly(JSON, KEY_RAM, ram);
	}

	public void setAiengineVersion(String version) {
		JSONUtil.putQuietly(JSON, KEY_AIENGINE_VERSION, version);
	}

	public void setJarVersion(String version) {
		JSONUtil.putQuietly(JSON, KEY_JAR_VERSION, version);
	}

	/**
	 * 参数JSON化
	 * 
	 * @return JSONObject
	 */
	public JSONObject toJSON() {
//		if(mCount == 0) { //只上报1次
			JSONUtil.putQuietly(JSON, KEY_SDCARD, DeviceUtil.getSDInfo());
			JSONUtil.putQuietly(JSON, KEY_CPU, Build.CPU_ABI);
			JSONUtil.putQuietly(JSON, KEY_ROM, DeviceUtil.getROMInfo());
			JSONUtil.putQuietly(JSON, KEY_SYSTEM, Build.VERSION.RELEASE);
//			mCount++;
//		} else {
//			JSONUtil.removeQuietly(JSON, KEY_SDCARD);
//			JSONUtil.removeQuietly(JSON, KEY_ROM);
//			JSONUtil.removeQuietly(JSON, KEY_CPU);
//			JSONUtil.removeQuietly(JSON, KEY_SYSTEM);
//			JSONUtil.removeQuietly(JSON, KEY_RAM);
//			JSONUtil.removeQuietly(JSON, KEY_IMEI);
//			JSONUtil.removeQuietly(JSON, KEY_MAC);
//		}
		return JSON;
	}

	@Override
	public String toString() {
		return toJSON().toString();
	}
}
