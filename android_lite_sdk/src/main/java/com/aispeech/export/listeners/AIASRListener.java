package com.aispeech.export.listeners;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.JSONResultParser;
import com.aispeech.lite.speech.EngineListener;

/**
 * AIASRListener 接口用以接收 AICloudASREngine 和 AILocalASREngine 中发生的事件。
 * 关注和需要处理相关事件的类须实现该接口，当相关事件发生时，有关方法将会被回调。
 * 所有这些回调方法的触发都是在UI线程中执行的，请不要执行任何阻塞操作。
 */
public interface AIASRListener extends EngineListener {

	/**
	 * 识别引擎初始化结束后执行，在主UI线程
	 *
	 * @param status {@link AIConstant#OPT_SUCCESS}:初始化成功；
	 *               {@link AIConstant#OPT_FAILED}:初始化失败,
	 */
	void onInit(int status);

	/**
	 * 发生错误时执行，在主UI线程
	 *
	 * @param error
	 *                错误信息
	 */
	void onError(AIError error);
	
	/**
	 * 收到结果时执行，请使用{@link JSONResultParser}解析，在主UI线程
	 * 
	 * @param result 结果
	 */
	void onResults(AIResult result);

	/**
	 * 音频音量发生改变时调用，在主UI线程
	 * 
	 * @param rmsdB
	 *                音量标量 0-100
	 */
	void onRmsChanged(float rmsdB);

	/**
	 * 检测到用户开始说话，在主UI线程
	 */
	void onBeginningOfSpeech();

	/**
	 * 用户停止说话时调用，在主UI线程
	 */
	void onEndOfSpeech();

	 /**
     * 录音机数据返回，在SDK内部子线程返回
     * @param buffer 录音机数据
	 * @param size  数据大小
     */
    void onRawDataReceived(byte[] buffer, int size);



	/**
	 * 经过信号出路模块处理后的音频数据返回，1声道pcm数据
	 * @param buffer 数据
	 * @param size  数据大小
	 */
	void onResultDataReceived(byte[] buffer, int size);

	/**
	 * 在识别引擎开启oneshot功能时，来判断用户说的话不是连说（比如你好小乐来首歌）的回调
	 * 没开启oneshot功能时，这个回调方法不会被调用
	 */
	void onNotOneShot();
    
}
