/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.lite.param;

import com.aispeech.common.JSONUtil;

import org.json.JSONObject;

/**
 * 内核请求参数配置基类
 */
public abstract class BaseRequestParams implements Cloneable{

	public static final String KEY_REQUEST = "request";
	public static final String KEY_CORE_TYPE = "coreType";
	public static final String KEY_RES = "res";
	public static final String KEY_REF_TEXT = "refText";
	public static final String KEY_RANK = "rank";
	public static final String KEY_CALLBACK_TYPE = "callback_type";
	public static final String KEY_USER_KEY = "userkey";
	public static final String KEY_USER_ID = "userid";
	public static final String KEY_PROVISION = "provision";
	public static final String KEY_ATTACH_URL = "attachAudioUrl";
	public static final String KEY_ATTACH_APPLICATION_ID = "attachApplicationId";
	public static final String KEY_ATTACH_RECORD_ID = "attachRecordId";
	public static final String KEY_CLOUD = "cloud";
	public static final String KEY_VERSION = "version";
	public static final String KEY_PAUSETIME = "pauseTime";

	public static final String CN_ASR_REC = "cn.asr.rec";
	public static final String CN_TTS = "cn.sent.syn";
	public static final String EN_TTS = "en.syn";
	public static final String CN_DLG_ITA = "cn.dlg.ita";
	public static final String CN_SDS = "cn.sds";
	public static final String SV_CLOUD = "sv.cloud";
	public static final String GRAMMAR_ASR_REC = "cn.gram.usr";
	public static final String CUSTOM_ASR_REC = "cn.gram.cus";
	public static final String CN_CLOUD_GRAMMAR = "gram.compile";
	public static final String CN_DOUBLE_DECODE_ASR_REC = "gram.decode";
	public static final String CN_LOCAL_WAKEUP = "cn.wakeup";
	public static final String CN_LOCAL_WAKEUP_DNN = "cn.dnn";
	public static final String CN_WAKEUP_REC = "cn.wakeuprec";
	public static final String CN_LOCAL_SV = "sv";
	public static final String CN_LOCAL_GRAMMAR = "cn.gram";
	public static final String CN_LOCAL_SEMANTIC = "cn.semantic";
	public static final String CN_LOCAL_DIALOG = "cn.sds";
	public static final String CN_LOCAL_DIALOG_RES = "cn.dlgres";
	public static final String VAD = "vad";
	public static final String ECHO= "echo";


	public static final String SPEEX = "speex";
	/**
	 * 云端引擎
	 */
	public static final String TYPE_CLOUD = "cloud";
	/**
	 * 本地引擎
	 */
	public static final String TYPE_NATIVE = "native";

	public static final int RANK_100 = 100;
	public static final int RANK_4 = 4;
	public static final int RANK_2 = 2;

	protected JSONObject reqJson = new JSONObject();

	private String coreType;

	private String res;

	private String version = "";

	/**
	 * 是否启用录音机
	 */
	private boolean useRecorder = true;

	/**
	 * 是否自动停止录音机
	 */
	private boolean isAutoStopRecorder = true;

	/**
	 * 如果不使用录音机，是否在start调用后立即调用stop
	 */
	private boolean isQuickStopWhenNoUseRecorder = true ;

	protected String tag;

	private int pauseTime = 0;

	public BaseRequestParams() {
	}

	/**
	 * 设置请求内核类型
	 *
	 * @param coreType
	 *                请求内核类型
	 */
	protected void setCoreType(String coreType) {
		this.coreType = coreType;
		JSONUtil.putQuietly(reqJson, KEY_CORE_TYPE, coreType);
	}

	/**
	 * 获取请求内核类型
	 *
	 * @return 请求内核类型
	 */
	protected String getCoreType() {
		return coreType;
	}

	protected void setVersion(String version) {
		this.version = version;
		JSONUtil.putQuietly(reqJson, KEY_VERSION, version);
	}

	protected String getVersion() {
		return version;
	}

	/**
	 * 设置请求内核使用资源
	 *
	 * @param res
	 *                内核使用资源
	 */
	public void setRes(String res) {
		this.res = res;
		JSONUtil.putQuietly(reqJson, KEY_RES, res);
	}

	/**
	 * 获取请求内核使用资源
	 *
	 * @return 内核使用资源
	 */
	public String getRes() {
		return res;
	}

	/**
	 * 设置本次请求是否启用录音机
	 * @param useRecorder
	 */
	public void setUseRecorder(boolean useRecorder){
		this.useRecorder = useRecorder;
	}

	/**
	 * 获取本次请求是否启用录音机
	 * @return true 启用
	 */
	public boolean isUseRecorder(){
		return useRecorder;
	}

	/**
	 * 是否自动停止录音机
	 * @return true 自动停止录音机
	 */
	public boolean isAutoStopRecorder() {
		return isAutoStopRecorder;
	}

	/**
	 * 设置是否自动停止录音机
	 * @param isAutoStopRecorder
	 */
	public void setAutoStopRecorder(boolean isAutoStopRecorder) {
		this.isAutoStopRecorder = isAutoStopRecorder;
	}

	/**
	 * 设置本次在不启用录音机的情况下，start之后是否立刻调用stop
	 */
	public void setQuickStopWhenNoUseRecorder(boolean isQuickStopWhenNoUseRecorder) {
		this.isQuickStopWhenNoUseRecorder = isQuickStopWhenNoUseRecorder ;
	}

	/**
	 *   获取本次在不启用录音机的情况下，start之后是否立刻调用stop
	 *    true 立刻调用stop
	 */
	public boolean getQuickStopWhenNoUseRecorder() {
		return isQuickStopWhenNoUseRecorder  ;
	}


	protected void setTag(String tag) {
		this.tag = tag;
	}

	public String getTag(){
		return tag;
	}

	/**
	 * 参数JSON化
	 *
	 * @return JSONObject
	 */
	public JSONObject toJSON() {
		return reqJson;
	}

	@Override
	public String toString() {
		return toJSON().toString();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}
}
