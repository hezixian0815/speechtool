package com.aispeech.lite.tts;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.TextSplitter;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.net.CloudTTSHttpClient;
import com.aispeech.lite.net.CloudTTSHttpClientListener;
import com.aispeech.lite.param.CloudTtsParams;

import java.util.ArrayList;
import java.util.List;


/**
 * @auther wuwei
 */
public class CloudTtsKernel extends BaseKernel {
    public static final String TAG = "CloudTtsKernel";
    private String mRecordId;
    private CloudTtsParams mParams;
    private TtsKernelListener mListener;
    private MyHttpClientListener mMyHttpClientListener;
    private volatile boolean isStarted = false;
    private CloudTTSHttpClient mHttpClient;
    private int mLastSize = 0;
    List<String> spiltWords = new ArrayList<>();
    public static final int MAX_TTS_LENGTH = 999;


    public CloudTtsKernel(TtsKernelListener listener) {
        super(TAG, listener);
        mListener = listener;
    }


    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    mHttpClient = new CloudTTSHttpClient();
                    mHttpClient.setDeviceSecret(getProfile().getDeviceSecret());
                    mMyHttpClientListener = new MyHttpClientListener();
                    mListener.onInit(AIConstant.OPT_SUCCESS);
                    break;
                case Message.MSG_START:
                    mParams = (CloudTtsParams) message.mObject;
                    mParams.setProductId(profile.getProductId());
                    mParams.setServerApiKey(profile.getApiKey());
                    mParams.setDeviceName(profile.getDeviceName());
                    mRecordId = Utils.getRecorderId();
                    mParams.setRequestId(mRecordId);
                    spiltWords = TextSplitter.spiltText(mParams.getRefText(), MAX_TTS_LENGTH);
                    if (spiltWords.size() > 0) {
                        mParams.setRefText(spiltWords.get(0));
                    }
                    if (mHttpClient != null) {
                        mHttpClient.startRequest(mParams, mMyHttpClientListener);
                    }
                    if (mListener != null && mHttpClient != null)
                        mListener.onMessage("url", mHttpClient.getUrl());
                    isStarted = true;
                    break;
                case Message.MSG_STOP:
                    if (isStarted) {
                        isStarted = false;
                    }
                    break;
                case Message.MSG_CANCEL:
                    if (isStarted) {
                        isStarted = false;
                    }
                    if (mHttpClient != null)
                        mHttpClient.closeHttp();
                    break;
                case Message.MSG_RELEASE:
                    // 销毁引擎
                    Log.d(TAG, "MSG_RELEASE");
                    if (mHttpClient != null) {
                        mHttpClient.destroy();
                        mHttpClient = null;
                    }
                    if (mMyHttpClientListener != null) {
                        mMyHttpClientListener = null;
                    }
                    isReleased = true;
                    Log.d(TAG, "MSG_RELEASE END");
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    break;
                case Message.MSG_ERROR:
                    mListener.onError((AIError) message.mObject);
                    break;
                case Message.MSG_ASYNC:
                    if (mLastSize < spiltWords.size()) {
                        Log.d(TAG, "feed cntts = " + spiltWords.get(mLastSize));
                        mParams.setRefText(spiltWords.get(mLastSize));
                        mHttpClient.startRequest(mParams, mMyHttpClientListener);
                    }
                    break;
                default:
                    break;
            }
            if (isReleased) {
                innerRelease();
                break;//release后跳出while循环
            }
        }
    }


    private class MyHttpClientListener implements CloudTTSHttpClientListener {

        @Override
        public void onBufferReceived(byte[] data, int size, int dataType) {

            if (null == mListener)
                return;

            if (size == 0) {
                ++mLastSize;
                if (mLastSize < spiltWords.size()) {
                    mQueue.put(new Message(Message.MSG_ASYNC));
                } else {
                    mLastSize = 0;
                    mListener.onBufferReceived(data, size, dataType);
                }
            } else {
                mListener.onBufferReceived(data, size, dataType);
            }
        }

        @Override
        public void onError(AIError error) {
            sendMessage(new Message(Message.MSG_ERROR, error));
        }

        @Override
        public void onEmotion(String emotion, String emotionOrigin) {
            if (null != mListener) {
                mListener.onEmotion(emotion, emotionOrigin);
            }
        }
    }
}
