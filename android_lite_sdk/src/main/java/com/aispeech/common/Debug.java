package com.aispeech.common;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Debug bean.
 */
public class Debug {

	boolean enable = false;
	long _start_listening;
	long _stop_listening;

	long _begin_read;
	long _read_record; // 一次循环做完

	long _begin_create_record;
	long _end_create_record;
	long _begin_perform;
	long _end_perform;
	long _ready_for_speech;
	long _beginning_of_speech;
	long _end_of_speech;
	long _on_error;
	long _vad_idle;
	long _vad_start;
	long _vad_end;
	int _payload_size;
	JSONArray performs;

	private long ts() {
		return System.currentTimeMillis();
	}

	void startListening() {
		if (enable) {
			_start_listening = ts();
		}
	}

	void stopListening() {
		if (enable) {
			_stop_listening = ts();
		}
	}

	void beginRead() {
		if (enable)
			_begin_read = ts();
	}

	void beginPerform() { // and end_read
		if (enable) {
			_begin_perform = ts();
			if (_begin_read <= 0) {
				_begin_read = _start_listening;
			}
			// 第1位为开始录音到本地read完成的时间
			// 第2位开始为上一次read到本次read完成的时间
			_read_record = _begin_perform - _begin_read;
			// _end_read = _begin_perform;
		}
	}

	void endPerform(int readSize) {
		if (enable) {
			_end_perform = ts();
			JSONObject jo = new JSONObject();
			JSONUtil.putQuietly(jo, "read_record", _read_record); // 这个时间越小说明录音机缓冲区溢出的可能性越大，接近0的时候要注意
			JSONUtil.putQuietly(jo, "perform", _end_perform - _begin_perform);
			JSONUtil.putQuietly(jo, "read_size", readSize);
			performs.put(jo);
		}
	}

	void beginCreateRecord() {
		if (enable) {
			_begin_create_record = ts();
		}
	}

	void endCreateRecord() {
		if (enable) {
			_end_create_record = ts();
		}
	}

	void readyForSpeech() {
		if (enable) {
			_ready_for_speech = ts();
		}
	}

	void beginningOfSpeech() {
		if (enable) {
			_beginning_of_speech = ts();
		}
	}

	void endOfSpeech() {
		if (enable) {
			_end_of_speech = ts();
		}
	}

	void onError() {
		if (enable) {
			_on_error = ts();
		}
	}

	void vadIdle() {
		if (enable)
			_vad_idle = ts();
	}

	void vadStart() {
		if (enable)
			_vad_start = ts();
	}

	void vadEnd() {
		if (enable)
			_vad_end = ts();
	}

	void setPayloadSize(int payloadSize) {
		if (enable)
			_payload_size = payloadSize;
	}

	public void setEnable(boolean enable) {
		this.enable = enable;
		if (enable) {
			reset();
		}
	}

	public void reset() {
		_begin_perform = _end_perform = -1;
		_ready_for_speech = _beginning_of_speech = _end_of_speech = -1L;
		_start_listening = _stop_listening = _on_error = -1L;
		_vad_start = _vad_end = _vad_idle = -1L;
		_begin_read = _read_record = -1L;
		performs = new JSONArray();
	}

	public JSONObject toJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put("start_listening", _start_listening);
			json.put("ready_for_speech", _ready_for_speech);
			json.put("beginning_of_speech", _beginning_of_speech);
			json.put("performs", performs);
			json.put("vad_idle", _vad_idle);
			json.put("vad_start", _vad_start);
			json.put("vad_end", _vad_end);
			json.put("end_of_speech", _end_of_speech);
			json.put("payload_size", _payload_size);
		} catch (Exception e) {

		}
		return json;
	}

	public String toString() {
		return toJSON().toString();
	}
}