package com.aispeech.lite.fasp;


import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.kernel.Fasp;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.FaspConfig;
import com.aispeech.lite.message.Message;

public class FaspKernel extends BaseKernel {

    private static final String TAG = "FaspKernel";
    private FaspListener mListener;
    private Fasp fasp;
    private Fasp.data_chs1_callback callback1;
    private Fasp.data_chs2_callback callback2;
    private volatile boolean isStopped = true;

    public FaspKernel(FaspListener listener) {
        super(TAG, listener);
        this.mListener = listener;
    }

    private class MyChs1Callback extends Fasp.data_chs1_callback {
        @Override
        public int run(int type, byte[] data, int size) {
            byte[] buffer = new byte[size];
            System.arraycopy(data, 0, buffer, 0, size);
            if (mListener != null)
                mListener.onChs1DataReceived(type, buffer);
            return 0;
        }
    }

    private class MyChs2Callback extends Fasp.data_chs2_callback {
        @Override
        public int run(int type, byte[] data, int size) {
            byte[] buffer = new byte[size];
            System.arraycopy(data, 0, buffer, 0, size);
            if (mListener != null)
                mListener.onChs2DataReceived(type, buffer);
            return 0;
        }
    }

    private boolean init(Fasp fasp, String cfg) {
        long enginId = fasp.init(cfg);
        if (enginId == 0)
            return false;
        callback1 = new MyChs1Callback();
        callback2 = new MyChs2Callback();
        return fasp.setCallback(callback1, callback2);
    }

    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    fasp = new Fasp();
                    FaspConfig faspConfig = (FaspConfig) message.mObject;
                    boolean suc = init(fasp, faspConfig.toJson().toString());
                    mListener.onInit(suc ? AIConstant.OPT_SUCCESS : AIConstant.OPT_FAILED);
                    break;
                case Message.MSG_START:
                    int ret = fasp.start();
                    if (ret != 0) {
                        sendMessage(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_AI_ENGINE, AIError.ERR_DESCRIPTION_AI_ENGINE)));
                    } else {
                        isStopped = false;
                    }
                    break;
                case Message.MSG_SET:
                    String setParam = (String) message.mObject;
                    if (fasp != null) {
                        fasp.set(setParam);
                    }
                    break;
                case Message.MSG_CANCEL:
                case Message.MSG_STOP:
                    if (fasp != null) {
                        fasp.stop();
                    }
                    isStopped = true;
                    break;
                case Message.MSG_RELEASE:
                    // 销毁引擎
                    if (fasp != null) {
                        fasp.destroy();
                    }
                    callback1 = null;
                    callback2 = null;
                    isStopped = true;
                    isReleased = true;
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    if (fasp != null && !isStopped) {
                        fasp.feed(data, data.length);
                    }
                    break;
                case Message.MSG_ERROR:
                    mListener.onError((AIError) message.mObject);
                    break;
                case MSG_INPUT_WAV_CHAN:
                    String getParam = (String) message.mObject;
                    if (fasp != null) {
                        int inputWavChan = fasp.get(getParam);
                        if (mListener != null)
                            mListener.onGotInputWavChan(inputWavChan);
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

    protected static final int MSG_INPUT_WAV_CHAN = 51;

    public void getInputWavChan() {
        sendMessage(new Message(MSG_INPUT_WAV_CHAN, "inputWavChan"));
    }
}
