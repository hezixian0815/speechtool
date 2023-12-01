package com.aispeech.lite.echo;

import android.os.Message;

import com.aispeech.AIError;
import com.aispeech.common.Log;
import com.aispeech.echo.EchoKernel;
import com.aispeech.echo.EchoKernelListener;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.BaseProcessor;
import com.aispeech.lite.Scope;
import com.aispeech.lite.param.EchoParams;
import com.aispeech.lite.speech.SpeechListener;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2022/8/25
 */
public class LocalEchoProcessor extends BaseProcessor {

    public static String TAG = LocalEchoProcessor.class.getSimpleName();
    private EchoKernel mEchoKernel;
    private SpeechListener mListener;
    private EchoParams echoParams;

    // 由于初始化一部分变量在主线程，一部分逻辑在工作线程，资源释放和初始化需要同步锁
    // 防止init中已赋值变量，被工作线程中release消息清空
    private final Object mutex = new Object();

    public void init(SpeechListener listener) {
        synchronized (mutex) {
            mListener = listener;
            mScope = Scope.LOCAL_ECHO;
            init(listener, AISpeech.getContext(), TAG);

            mEchoKernel = new EchoKernel(new EchoKernelListenerImpl());
            sendMsgToInnerMsgQueue(EngineMsg.MSG_NEW, null);
        }
    }


    public void start(EchoParams params) {
        if (isAuthorized()) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_START, params);
        } else {
            showErrorMessage();
        }
    }

    @Override
    protected void handlerInnerMsg(EngineMsg engineMsg, Message msg) {
        switch (engineMsg) {
            case MSG_NEW:
                handleMsgNew();
                break;
            case MSG_START:
                echoParams = (EchoParams) msg.obj;
                handleMsgStart();
                break;
            case MSG_STOP:
                handleMsgStop();
                break;
            case MSG_ERROR:
                handleMsgError((AIError) msg.obj);
                break;
            case MSG_RELEASE:
                handleMsgRelease();
                break;
            case MSG_RESULT_RECEIVE_DATA:
                handleDataRec((byte[]) msg.obj);
                break;
            default:
                Log.w(TAG, "unhandle msg:" + engineMsg.name());
                break;
        }
    }

    private void handleDataRec(byte[] data) {
        if (mState == EngineState.STATE_RUNNING || mState == EngineState.STATE_WAITING) {
            if (mEchoKernel != null) mEchoKernel.feed(data);
        }
    }

    private void handleMsgNew() {
        if (mState == EngineState.STATE_IDLE) {
            // 配置解析走 全局的 AIspeech
            mEchoKernel.newKernel();
            transferState(EngineState.STATE_NEWED);
        } else {
            trackInvalidState("new");
        }
    }

    private void handleMsgError(AIError error) {
        if (mState != EngineState.STATE_NEWED && mState != EngineState.STATE_IDLE) {
            transferState(EngineState.STATE_NEWED);
            if (mEchoKernel != null) {
                mEchoKernel.stopKernel();
            }
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
        } else {
            trackInvalidState("error");
        }

    }

    private void handleMsgStop() {
        if (mState == EngineState.STATE_RUNNING) {
            if (mEchoKernel != null) mEchoKernel.stopKernel();
            transferState(EngineState.STATE_WAITING);
        } else {
            trackInvalidState("stop");
        }
    }

    private void handleMsgStart() {
        if (mState == EngineState.STATE_NEWED || mState == EngineState.STATE_WAITING) {
            if (mEchoKernel != null) mEchoKernel.startKernel(echoParams);
            transferState(EngineState.STATE_RUNNING);
        } else {
            trackInvalidState("start");
        }
    }

    private void handleMsgRelease() {
        if (mState != EngineState.STATE_IDLE) {
            removeCallbackMsg();
            if (mState == EngineState.STATE_RUNNING) {
                if (mEchoKernel != null) mEchoKernel.stopKernel();
            }
            clearObject();//清除实例
            transferState(EngineState.STATE_IDLE);
        } else {
            trackInvalidState("release");
        }

        synchronized (mutex) {
            mutex.notifyAll();
        }
    }

    @Override
    public void release() {
        synchronized (mutex) {
            super.release();
            try {
                mutex.wait(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void clearObject() {
        super.clearObject();

        if (mEchoKernel != null) {
            Log.i(TAG, "releaseKernel");
            mEchoKernel.releaseKernel();
            mEchoKernel = null;
        }

    }

    @Override
    public void processNoSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(AIError.ERR_NO_SPEECH,
                AIError.ERR_DESCRIPTION_NO_SPEECH));
        Log.w(TAG, "no speech timeout!");
    }

    @Override
    public void processMaxSpeechError() {
        sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, new AIError(
                AIError.ERR_MAX_SPEECH, AIError.ERR_DESCRIPTION_MAX_SPEECH));
    }

    /**
     * echo 回调
     */
    private class EchoKernelListenerImpl extends EchoKernelListener {

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onResultBufferReceived(byte[] data) {
            if (mListener != null) mListener.onEchoDataReceived(data, data.length);
        }

        @Override
        public void onAgcDataReceived(byte[] data) {
            if (mListener != null) mListener.onEchoVoipDataReceived(data, data.length);
        }

        @Override
        public void onError(AIError error) {
            sendMsgToInnerMsgQueue(EngineMsg.MSG_ERROR, error);
        }
    }
}
