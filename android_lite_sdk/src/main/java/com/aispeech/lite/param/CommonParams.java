/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.param;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务配置，包含<br>
 * <ul>
 * <li>配置使用的服务类型 {@link #setType(int)}:<br>
 * <ul>
 * <li>{@link #TYPE_CLOUD}:使用思必驰云端引擎；[默认]</li>
 * <li>{@link #TYPE_LOCAL}:使用思必驰本地引擎；<br>
 * </li>
 * </ul>
 * <li>是否开启vad {@link #setVadEnable(boolean)}</li>
 * <li>设置服务器地址 {@link #setServer(String)}[云端引擎有效]</li>
 * </ul>
 */
public class CommonParams {

	public static final String KEY_VAD_ENABLE = "vadEnable";
	public static final String KEY_TYPE = "coreProvideType";
	public static final String KEY_VOL = "volumeEnable";
	public static final String KEY_PAUSETIME = "pauseTime";

	@SuppressWarnings("rawtypes")
	private Map<String, Comparable> map = new HashMap<String, Comparable>();
	private String type;
	private boolean vadEnable = true;
	private boolean volumeEnable = false;
	private int pauseTime = 0;

	public CommonParams() {
		// 默认使用云端
		setType(BaseRequestParams.TYPE_CLOUD);
	}

	/**
	 * 获取是否使用vad
	 * 
	 * @return true:使用Vad；false:禁止Vad
	 */
	public boolean getVadEnable() {
		return vadEnable;
	}

	/**
	 * 设置是否使用vad
	 * 
	 * @param vadEnable
	 *                true:使用Vad；false:禁止Vad
	 */
	public void setVadEnable(boolean vadEnable) {
		this.vadEnable = vadEnable;

	}

	/**
	 * 获取使用的引擎类型，本地或者云端
	 * 
	 * @return {@link #TYPE_CLOUD}:使用思必驰云端引擎； {@link #TYPE_LOCAL}:使用思必驰本地引擎；
	 */
	public String getType() {
		return type;
	}

	/**
	 * 设置使用思必驰引擎类型
	 * 
	 * @param type
	 *                {@link #TYPE_CLOUD}:使用思必驰云端引擎； {@link #TYPE_LOCAL}
	 *                :使用思必驰本地引擎；
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * @return Map
	 */
	@SuppressWarnings("rawtypes")
	public Map toMap() {

		map.put(KEY_TYPE, this.type);
//		map.put(KEY_VAD_ENABLE, vadEnable ? 1 : 0);
		map.put(KEY_VOL, volumeEnable ? 1 : 0);
//		if(vadEnable) {
//			map.put(KEY_PAUSETIME, pauseTime);
//		}

		return map;
	}

	public boolean isVolumeEnable() {
		return volumeEnable;
	}

	public void setVolumeEnable(boolean volumeEnable) {
		this.volumeEnable = volumeEnable;
	}

	public int getPauseTime() {
		return pauseTime;
	}

	public void setPauseTime(int pauseTime) {
		this.pauseTime = pauseTime;
	}
	
	

}
