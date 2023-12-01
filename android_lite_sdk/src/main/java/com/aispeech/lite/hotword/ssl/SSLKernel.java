package com.aispeech.lite.hotword.ssl;

import com.aispeech.AIError;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.SSLConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.SpeechParams;

/**
 * 4音区声源定位
 *
 * @author hehr
 */
public class SSLKernel extends BaseKernel {

    private static final String TAG = "SSLKernel";

    private SSLKernelListener mListener;

    private ISSL mSSL;

    public SSLKernel(SSLKernelListener listener) {
        super(TAG, listener);
        mListener = listener;
        mSSL = new SSLByVadEngine();
    }


    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            final boolean isReleased = false;

            switch (message.mId) {
                case Message.MSG_NEW:
                    SSLConfig config = (SSLConfig) message.mObject;
                    if (mSSL != null) {
                        mSSL.init(config, new SSLListenerImpl());
                    }
                    break;
                case Message.MSG_START:
                    SpeechParams params = (SpeechParams) message.mObject;
                    if (mSSL != null) {
                        mSSL.start(params);
                    }
                    break;
                case Message.MSG_STOP:
                    if (mSSL != null) {
                        mSSL.stop();
                    }
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    if (mSSL != null) {
                        mSSL.feed(data);
                    }
                    break;
                case Message.MSG_RELEASE:
                    if (mSSL != null) {
                        mSSL.release();
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

    private class SSLListenerImpl implements SSLListener {

        @Override
        public void init(int status) {
            if (mListener != null) {
                mListener.onInit(status);
            }
        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(error);
            }
        }

        @Override
        public void onBufferReceived(byte[] data) {
            if (mListener != null) {
                mListener.onBufferReceived(data);
            }
        }

        @Override
        public void onStart(String recordID) {
            if (mListener != null) {
                mListener.onVadStart(recordID);
            }
        }

        @Override
        public void onSsl(int index) {
            if (mListener != null) {
                mListener.onSsl(index);
            }
        }


        @Override
        public void onEnd(String recordID) {
            if (mListener != null) {
                mListener.onVadEnd(recordID);
            }
        }
    }


}
