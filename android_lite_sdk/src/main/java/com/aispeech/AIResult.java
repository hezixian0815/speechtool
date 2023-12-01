/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech;

import android.os.Parcel;
import android.os.Parcelable;

import com.aispeech.common.AIConstant;
import com.aispeech.common.JSONResultParser;
import com.aispeech.common.Util;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 本类用于封装语音引擎的结果<br>
 * 封装包含以下信息:
 * <ul>
 * <li>recordId录音ID</li>
 * <li>resultType结果类型
 * <ul>
 * <li>{@link AIConstant#AIENGINE_MESSAGE_TYPE_JSON}表示结果为JSON字符串</li>
 * <li>{@link AIConstant#AIENGINE_MESSAGE_TYPE_BIN}表示结果为字节数组</li>
 * </ul>
 * </li>
 * <li>TimeStamp时间戳</li>
 * <li>resultObject结果对象</li>
 * <li>isLast是否结果返回完毕</li>
 * </ul>
 * <br>
 * Usage1:<br>
 * 处理文本结果(json):
 *
 * <pre>
 *  public void onResults(AIResult results) {
 *         if (results.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
 *              {@link JSONResultParser} parser = new {@link JSONResultParser}(results.getResultObject().toString());
 *              String text = parser.getText();
 *           ...
 *           }
 *  }
 * </pre>
 *
 */
public class AIResult implements Parcelable {

	public String recordId;
	public Object resultObject; // 数据对象
	public String topic;

	private int length;
	public long timestamp;

	public int dataType; // resultObject对象数据类型：binary/json

	public boolean last = false;

	/**
	 * 本次结果对应的start操作返回的recordId
	 * @return recordId
	 */
	public String getRecordId() {
		return recordId;
	}

	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}

	/**
	 * 获取结果内容
	 *
	 * @return 结果内容
	 */
	public Object getResultObject() {
		return resultObject;
	}
	/**
	 * 获取结果内容
	 *
	 * @return 结果内容JSONObject格式
	 */
	public JSONObject getResultJSONObject() {
		if (resultObject instanceof JSONObject)
			return (JSONObject) resultObject;
		if (resultObject instanceof String) {
			try {
				return new JSONObject((String) resultObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	public void setResultObject(Object resultObject) {
		this.resultObject = resultObject;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	/**
	 * 返回结果的时间戳
	 *
	 * @return 结果返回时间戳
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * 设置时间戳
	 * @param timestamp timestamp
	 */
	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * 返回是否是最后的结果，配合返回结果为byte[]类型使用
	 *
	 * @return true：表示所有结果已经返回；false：表示只是部分结果，还有结果尚未返回；
	 */
	public boolean isLast() {
		return last;
	}

	public void setLast(boolean last) {
		this.last = last;
	}

	public int getResultType() {
		return dataType;
	}

	public void setResultType(int resultType) {
		this.dataType = resultType;
	}

	public String toString() {
		return resultObject.toString();
	}

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(dataType);
		dest.writeByte((byte) (last ? 1 : 0));
		dest.writeString(recordId);
		if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
			dest.writeString((String) resultObject);
		} else {
			length = ((byte[]) resultObject).length;
			dest.writeInt(length);
			dest.writeByteArray((byte[]) resultObject);
		}
		dest.writeLong(timestamp);
	}

	public static final Creator<AIResult> CREATOR = new Creator<AIResult>() {
		public AIResult createFromParcel(Parcel in) {
			return new AIResult(in);
		}

		public AIResult[] newArray(int size) {
			return new AIResult[size];
		}
	};

	private AIResult(Parcel in) {
		dataType = in.readInt();
		last = in.readByte() == 1;
		recordId = in.readString();
		if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
			resultObject = in.readString();
		} else {
			length = in.readInt();
			resultObject = new byte[length];
			in.readByteArray(((byte[]) resultObject));
		}
		timestamp = in.readLong();
	}

	public AIResult() {
	}

	public static AIResult bundleResults(int dataType, String recordId, byte[] data) {

		AIResult results = new AIResult();
		Object res = data;

		if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
			res = Util.newUTF8String(data);
		}

		results.setResultObject(res);
		results.setRecordId(recordId);
		results.setTimestamp(System.currentTimeMillis());
		results.setResultType(dataType);

		return results;
	}

	public static AIResult bundleResults(int dataType, String recordId, String data) {

		AIResult results = new AIResult();

		results.setResultObject(data);
		results.setRecordId(recordId);
		results.setTimestamp(System.currentTimeMillis());
		results.setResultType(dataType);

		return results;
	}
}
