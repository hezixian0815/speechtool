package com.aispeech.base;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.aispeech.AIError;
import com.aispeech.auth.AIAuthEngine;
import com.aispeech.auth.ProfileState;
import com.aispeech.common.Log;
import com.aispeech.export.listeners.ICloudListener;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.speech.EngineListener;
import com.aispeech.lite.speech.SpeechListener;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseInnerEngine extends SpeechListener {

    private static final String TAG = "BaseEngine";

    private Handler mCallbackHandler;

    private EngineListener mListener;


    /**
     * 子类实现
     *
     * @param callback {@link CallbackMsg} 消息定义
     * @param obj      消息外挂内容
     */
    protected abstract void callbackInMainLooper(CallbackMsg callback, Object obj);


    public void init(EngineListener listener) {
        this.mListener = listener;
        mCallbackHandler = createCallbackHandler();
    }


    /**
     * callbackInThread  true  往子线程发消息
     * false 往主线程发消息
     *
     * @param msg 消息头
     * @param obj 消息内容
     */
    public void sendMsgToCallbackMsgQueue(CallbackMsg msg, Object obj) {
        if (mCallbackHandler != null) {
            if (!AISpeech.callbackInThread) {
                Message.obtain(mCallbackHandler, msg.msg, obj).sendToTarget();
            } else {
                mCallbackHandler.handleMessage(Message.obtain(mCallbackHandler, msg.msg, obj));
            }
        }
    }

    public void removeCallbackMsg() {
        if (mCallbackHandler != null) {
            mCallbackHandler.removeCallbacksAndMessages(null);
        }
    }

    protected Map<String, Object> optExtra(final String key, final Object extra) {
        return new HashMap<String, Object>(1) {{
            put(key, extra);
        }};
    }

    protected Map<String, Object> optExtra(final String key1, final Object obj1, final String key2, final Object obj2) {
        return new HashMap<String, Object>(2) {{
            put(key1, obj1);
            put(key2, obj2);
        }};
    }


    protected Map<String, Object> optExtra(final String key1, final Object obj1, final String key2, final Object obj2, final String key3, final Object obj3) {

        return new HashMap<String, Object>(3) {{
            put(key1, obj1);
            put(key2, obj2);
            put(key3, obj3);
        }};

    }

    protected Map<String, Object> optExtra(final String key1, final Object obj1, final String key2, final Object obj2, final String key3, final Object obj3, final String key4, final Object obj4) {
        return new HashMap<String, Object>(4) {{
            put(key1, obj1);
            put(key2, obj2);
            put(key3, obj3);
            put(key4, obj4);
        }};
    }

    /**
     * 对外回调消息定义,可以自由定义
     */
    public enum CallbackMsg {
        /**
         * init
         */
        MSG_INIT(1),
        /**
         * begin speech
         */
        MSG_BEGINNING_OF_SPEECH(2),
        /**
         * end speech
         */
        MSG_END_OF_SPEECH(3),
        /**
         * buffer received
         */
        MSG_BUFFER_RECEIVED(4),
        /**
         * recorder release
         */
        MSG_RECORED_RELEASED(5),
        /**
         * error
         */
        MSG_ERROR(6),
        /**
         * ready speech
         */
        MSG_READY_FOR_SPEECH(7),
        /**
         * result
         */
        MSG_RESULTS(8),
        /**
         * rms changed
         */
        MSG_RMS_CHANGED(9),
        /**
         * recorder stopped
         */
        MSG_RECORED_STOPPED(10),
        /**
         * wakeup stopped
         */
        MSG_WAKEUP_STOPPED(11),

        /**
         * grammar success
         */
        MSG_GRAMMAR_SUCCESS(13),
        /**
         * doa result
         */
        MSG_DOA_RESULT(14),
        /**
         * cancel
         */
        MSG_CANCEL(15),
        /**
         * RECEIVE DATA
         */
        MSG_RESULT_RECEIVE_DATA(16),
        /**
         * previous wakeup
         */
        MSG_PRE_WAKEUP(17),
        /**
         * 声纹音频
         */
        MSG_VPRINT_DATA(18),
        /**
         * dm asr
         */
        MSG_DM_ASR(19),
        /**
         * dm end
         */
        MSG_DM_END(20),
        /**
         * dm query
         */
        MSG_DM_QUERY(21),
        /**
         * dm execute
         */
        MSG_DM_CALL(22),
        /**
         * dm display
         */
        MSG_DM_DISPLAY(23),
        /**
         * dm play
         */
        MSG_DM_PLAY(24),
        /**
         * not one shot
         */
        MSG_NOT_ONE_SHOT(12),
        /**
         * not one shot
         */
        MSG_ONE_SHOT(25),
        /**
         * update result
         */
        MSG_UPDATE_RESULT(26),

        /**
         * dm result
         */
        MSG_DM_RESULT(27),

        /**
         * 云端连接状态
         */
        MSG_CONNECT_STATE(28);

        int msg;

        private static Map<Integer, CallbackMsg> map;

        CallbackMsg(int msg) {
            this.msg = msg;
        }

        public static CallbackMsg getMsgByValue(int value) {
            if (map == null) {
                map = new HashMap<>();
                for (CallbackMsg msg : CallbackMsg.values()) {
                    map.put(msg.msg, msg);
                }
            }
            return map.get(value);
        }

    }

    /***
     * 创建主线程消息队列
     * @return {@link Handler}
     */
    protected Handler createCallbackHandler() {
        return new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                CallbackMsg callbackMsg = CallbackMsg.getMsgByValue(msg.what);
                if (callbackMsg != null) {
                    if (callbackMsg != CallbackMsg.MSG_RMS_CHANGED && callbackMsg != CallbackMsg.MSG_VPRINT_DATA)
                        Log.d(TAG, ">>>>>>[Callback]:" + callbackMsg.name());
                    switch (callbackMsg) {
                        case MSG_INIT:
                            if (mListener != null) {
                                mListener.onInit((Integer) msg.obj);
                            }
                            break;
                        case MSG_CANCEL:
                            removeCallbackMsg();
                            break;
                        case MSG_ERROR:
                            if (mListener != null) {
                                mListener.onError((AIError) msg.obj);
                            }
                            break;
                        case MSG_READY_FOR_SPEECH:
                            if (mListener != null) {
                                mListener.onReadyForSpeech();
                            }
                            break;
                        case MSG_CONNECT_STATE:
                            if (mListener != null && mListener instanceof ICloudListener) {
                                ((ICloudListener) mListener).onConnect((Boolean) msg.obj);
                            }
                            break;
                        default:
                            callbackInMainLooper(callbackMsg, msg.obj);
                            break;
                    }
                } else {
                    Log.e(TAG, "undefined callback msg , check SDK code!");
                }
            }
        };
    }


    protected void showErrorMessage(ProfileState state) {
        AIError error = new AIError();
        if (state == null) {
            error.setErrId(AIError.ERR_SDK_NOT_INIT);
            error.setError(AIError.ERR_DESCRIPTION_ERR_SDK_NOT_INIT);
        } else {
            error.setErrId(state.getAuthErrMsg().getId());
            error.setError(state.getAuthErrMsg().getValue());
        }
        if (mListener != null) {
            mListener.onError(error);
        }
    }

    protected boolean updateTrails(ProfileState profileState, String scope) {
        if (profileState.getAuthType() == ProfileState.AUTH_TYPE.TRIAL
                && profileState.getTimesLimit() != -1) {
            ProfileState state = AIAuthEngine.getInstance().getProfile().isProfileValid(scope);
            if (!state.isValid()) {
                showErrorMessage(state);
                return false;
            } else {
                AIAuthEngine.getInstance().getProfile().updateUsedTimes(scope);
                return true;
            }
        } else {
            return true;
        }
    }

    @Override
    public void onInit(int status) {
        sendMsgToCallbackMsgQueue(CallbackMsg.MSG_INIT, status);
    }

    @Override
    public void onError(AIError error) {
        sendMsgToCallbackMsgQueue(CallbackMsg.MSG_ERROR, error);
    }

    @Override
    public void onReadyForSpeech() {
        sendMsgToCallbackMsgQueue(CallbackMsg.MSG_READY_FOR_SPEECH, null);
    }

    @Override
    public void onRawDataReceived(byte[] buffer, int size) {
        //工作线程中抛出数据
        if (mListener != null) {
            mListener.onRawDataReceived(buffer, size);
        }
    }

    @Override
    public void onRecorderStopped() {

    }

    @Override
    public void onCancel() {
        sendMsgToCallbackMsgQueue(CallbackMsg.MSG_CANCEL, null);
    }

    @Override
    public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {
        //工作线程中抛出数据
        if (mListener != null) {
            mListener.onResultDataReceived(buffer, size, wakeupType);
        }

    }

    public void release() {
        if (mListener != null) {
            mListener = null;
        }
        if (mCallbackHandler != null) {
            mCallbackHandler.removeCallbacksAndMessages(null);
            mCallbackHandler = null;
        }
    }

}
