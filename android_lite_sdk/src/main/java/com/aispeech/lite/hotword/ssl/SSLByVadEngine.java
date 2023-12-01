package com.aispeech.lite.hotword.ssl;

import com.aispeech.AIError;
import com.aispeech.common.AITimer;
import com.aispeech.common.Log;
import com.aispeech.lite.config.SSLConfig;
import com.aispeech.lite.hotword.ssl.n.INRms;
import com.aispeech.lite.hotword.ssl.n.INVad;
import com.aispeech.lite.hotword.ssl.n.NRmsEngine;
import com.aispeech.lite.hotword.ssl.n.NVadEngine;
import com.aispeech.lite.hotword.ssl.n.NVadListener;
import com.aispeech.lite.param.SpeechParams;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;

/**
 * 借助vad实现的声源定位方案
 *
 * @author hehr
 */
public class SSLByVadEngine implements ISSL {

    private static final String TAG = "SSLByVadEngine";

    /**
     * 音频通道数
     */
    private static final int CHANEL = 4;

    /**
     * 计算音频能量音频长度
     */
    private static final int RMS_CHECK_LENGTH = 300;//ms
    /**
     * dms 计算SSL 加权系数
     */
    private static final double RMS_WEIGHT_FACTOR = 0.9;

    /**
     * nvad
     */
    private INVad mNVad;
    /**
     * nrms
     */
    private INRms mNRms;
    /**
     * 计数器
     */
    private Counter mCounter;


    public SSLByVadEngine() {
        mNVad = new NVadEngine();
        mNRms = new NRmsEngine();
        mCounter = new Counter(CHANEL);
    }

    private SSLListener mListener;

    @Override
    public void init(SSLConfig config, SSLListener listener) {

        mListener = listener;

        if (mNVad != null)
            mNVad.init(config, new NVadListenerImpl());

        if (mNRms != null)
            //vad 本身存在300ms左右延迟，计时器也要等待300ms,故dms统计音频长度等于300ms * 2,需往前多看300ms
            mNRms.init((int) Math.ceil(RMS_CHECK_LENGTH * 2.0 / config.getFrameLength()));

    }

    private boolean isStart = false;

    @Override
    public void start(SpeechParams params) {

        if (mNVad != null)
            mNVad.start(params);

        if (mNRms != null)
            mNRms.start();

        if (mCounter != null)
            mCounter.clear();

        isStart = true;
    }

    @Override
    public void stop() {

        if (mNVad != null) {
            mNVad.stop();
        }

        if (mNRms != null) {
            mNRms.stop();
        }

        if (mCounter != null)
            mCounter.clear();

        isStart = false;

    }


    @Override
    public void feed(byte[] bytes) {

        if (isStart) {

            byte[] chanel0 = new byte[bytes.length / CHANEL];
            byte[] chanel1 = new byte[bytes.length / CHANEL];
            byte[] chanel2 = new byte[bytes.length / CHANEL];
            byte[] chanel3 = new byte[bytes.length / CHANEL];

            for (int n = 0; n < CHANEL; n++) {
                byte[] temp = new byte[bytes.length / CHANEL];
                for (int i = 0; i < bytes.length / (CHANEL * 2); i++) {
                    temp[2 * i] = bytes[2 * (i * CHANEL + n)];
                    temp[2 * i + 1] = bytes[2 * (i * CHANEL + n) + 1];
                }
                if (n == 0) {
                    chanel0 = temp;
                } else if (n == 1) {
                    chanel1 = temp;
                } else if (n == 2) {
                    chanel2 = temp;
                } else {
                    chanel3 = temp;
                }
            }

            //1. feed vad
            if (mNVad != null) {
                mNVad.feed(chanel0, chanel1, chanel2, chanel3);
            }

            //2. feed dms
            if (mNRms != null) {
                mNRms.feed(chanel0, chanel1, chanel2, chanel3);
            }
        } else {
            Log.e(TAG, "drop not begin feed ...");
        }


    }


    @Override
    public void release() {

        if (mNVad != null) {
            mNVad.release();
            mNVad = null;
        }

        if (mNRms != null) {
            mNRms.release();
            mNRms = null;
        }

        if (mCounter != null) {
            mCounter.clear();
            mCounter = null;
        }

    }

    /**
     *
     */
    private class NVadListenerImpl implements NVadListener {

        @Override
        public void onInit(int status) {
            if (mListener != null) {
                mListener.init(status);
            }
        }

        @Override
        public void onBegin(int index, String recordId) {

            synchronized (this) {
                Log.d(TAG, "vad  begin index " + index);
                //首次回调，启动计时器
                if (mCounter.value() == 0) {
                    if (mListener != null) {
                        mListener.onStart(recordId);//触发首帧vad
                    }
                    mCounter.begin(index);
                    startWaitTask();
                } else {
                    mCounter.increment(index);
                }
            }

        }

        @Override
        public void onEnd(String recordId) {
            if (mListener != null) {
                mListener.onEnd(recordId);
            }
        }

        @Override
        public void onBufferReceived(byte[] data) {
            if (mListener != null) {
                mListener.onBufferReceived(data);
            }
        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(error);
            }
        }
    }

    private WaitTask mWaitTask;

    /**
     * 启动wait timer task
     */
    private void startWaitTask() {

        if (mWaitTask != null) {
            mWaitTask.cancel();
            mWaitTask = null;
        }

        mWaitTask = new WaitTask();

        AITimer.getInstance().schedule(mWaitTask, RMS_CHECK_LENGTH);

    }


    private class WaitTask extends TimerTask {


        @Override
        public void run() {

            mCounter.end();
            int ssl = choice(mCounter.optIndex());
            Log.d(TAG, "choice ssl:" + ssl);
            if (mNVad != null) {
                mNVad.notifySSL(ssl);//开始回溯vad音频
            }
            if (mListener != null) {
                mListener.onSsl(ssl);
            }

        }

        /**
         * 抉择ssl
         *
         * @param triggerIndex 被触发的index数据
         * @return ssl
         */
        private int choice(int[] triggerIndex) {
            int length = triggerIndex.length;
            Map<Integer, Double> map = new HashMap<>(length);
            for (int i = 0; i < length; i++) {
                int index = triggerIndex[i];
                double dmsWeight = mNRms.optDms(index) * RMS_WEIGHT_FACTOR;
                double orderWeight = ((100 * (1 - RMS_WEIGHT_FACTOR)) / length) * (length - i);
                Log.d(TAG, "index : " + index + " ,orderWeight: " + orderWeight + " ,dmsWeight: " + dmsWeight);
                map.put(index, orderWeight + dmsWeight);
            }
            return findMax(map);
        }

        /**
         * 查找出当前map中最大数值的key
         *
         * @param map
         * @return index
         */
        private int findMax(Map<Integer, Double> map) {
            int maxIndex = 0;
            double maxWeight = 0;
            for (Map.Entry<Integer, Double> entry : map.entrySet()) {
                if (entry.getValue() > maxWeight) {
                    maxIndex = entry.getKey();
                    maxWeight = entry.getValue();
                }
            }
            return maxIndex;
        }

    }

}
