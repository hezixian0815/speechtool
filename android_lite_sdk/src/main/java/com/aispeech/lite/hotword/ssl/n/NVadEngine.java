package com.aispeech.lite.hotword.ssl.n;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.lite.config.LocalVadConfig;
import com.aispeech.lite.param.SpeechParams;
import com.aispeech.lite.vad.VadKernel;
import com.aispeech.lite.vad.VadKernelListener;

import java.util.LinkedList;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

/**
 * nvad 引擎封装
 *
 * @author hehr
 */
public class NVadEngine implements INVad {

    private static final String TAG = "NVadEngine";

    private volatile boolean isInit = false;

    private CyclicBarrier mBarrier;

    private NVadListener mListener;

    /**
     * 音频通道数
     */
    private static final int CHANEL = 4;

    private VadKernel vad0;
    private VadKernel vad1;
    private VadKernel vad2;
    private VadKernel vad3;

    private LinkedList<byte[]> cache0;
    private LinkedList<byte[]> cache1;
    private LinkedList<byte[]> cache2;
    private LinkedList<byte[]> cache3;

    public NVadEngine() {
        vad0 = new VadKernel("NVad0", new Vad0ListenerImpl());
        vad1 = new VadKernel("NVad1", new Vad1ListenerImpl());
        vad2 = new VadKernel("NVad2", new Vad2ListenerImpl());
        vad3 = new VadKernel("NVad3", new Vad3ListenerImpl());

        cache0 = new LinkedList<>();
        cache1 = new LinkedList<>();
        cache2 = new LinkedList<>();
        cache3 = new LinkedList<>();

        if (mBarrier == null) {

            mBarrier = new CyclicBarrier(CHANEL, new Runnable() {
                @Override
                public void run() {
                    if (mListener != null) {
                        if (!isInit) {
                            mListener.onInit(AIConstant.OPT_FAILED);
                        } else {
                            mListener.onInit(AIConstant.OPT_SUCCESS);
                        }

                    }
                }
            });
        }
    }

    @Override
    public void init(LocalVadConfig config, NVadListener listener) {

        mListener = listener;

        if (vad0 != null)
            vad0.newKernel(config);
        if (vad1 != null)
            vad1.newKernel(config);
        if (vad2 != null)
            vad2.newKernel(config);
        if (vad3 != null)
            vad3.newKernel(config);
    }

    @Override
    public void start(SpeechParams params) {

        resetSSL();

        //启动vad
        if (vad0 != null)
            vad0.startKernel(params);
        if (vad1 != null)
            vad1.startKernel(params);
        if (vad2 != null)
            vad2.startKernel(params);
        if (vad3 != null)
            vad3.startKernel(params);

        if (cache0 != null)
            cache0.clear();
        if (cache1 != null)
            cache1.clear();
        if (cache2 != null)
            cache2.clear();
        if (cache3 != null)
            cache3.clear();


    }

    @Override
    public void feed(byte[] chanel0, byte[] chanel1, byte[] chanel2, byte[] chanel3) {
        if (vad0 != null)
            vad0.feed(chanel0);
        if (vad1 != null)
            vad1.feed(chanel1);
        if (vad2 != null)
            vad2.feed(chanel2);
        if (vad3 != null)
            vad3.feed(chanel3);

    }

    @Override
    public void stop() {
        if (vad0 != null)
            vad0.stopKernel();
        if (vad1 != null)
            vad1.stopKernel();
        if (vad2 != null)
            vad2.stopKernel();
        if (vad3 != null)
            vad3.stopKernel();

        resetSSL();
    }

    @Override
    public void release() {
        if (vad0 != null) {
            vad0.releaseKernel();
            vad0 = null;
        }
        if (vad1 != null) {
            vad1.releaseKernel();
            vad1 = null;
        }
        if (vad2 != null) {
            vad2.releaseKernel();
            vad2 = null;
        }
        if (vad3 != null) {
            vad3.releaseKernel();
            vad3 = null;
        }
        if (mBarrier != null) {
            mBarrier = null;
        }
        if (mListener != null) {
            mListener = null;
        }
        if (cache0 != null) {
            cache0.clear();
            cache0 = null;
        }
        if (cache1 != null) {
            cache1.clear();
            cache1 = null;
        }
        if (cache2 != null) {
            cache2.clear();
            cache2 = null;
        }
        if (cache3 != null) {
            cache3.clear();
            cache3 = null;
        }
    }

    @Override
    public void notifySSL(int index) {

        synchronized (this) {
            LinkedList<byte[]> selectCache = optCache(index);
            while (selectCache != null && !selectCache.isEmpty()) {
                if (mListener != null) {
                    mListener.onBufferReceived(selectCache.poll());
                }
            }
            setSSL(index);
        }


        //stop其他非被选中的vad引擎
        stopOtherVad(index);

    }

    /**
     * ssl 未选中默认值
     */
    private static final int DEFAULT_SSL = -1;

    private int ssl = DEFAULT_SSL;

    private void resetSSL() {
        ssl = DEFAULT_SSL;
    }

    private void setSSL(int index) {
        ssl = index;
    }

    /**
     * 停止其他非被选中的vad引擎
     *
     * @param index
     */
    private void stopOtherVad(int index) {
        switch (index) {
            case 0:
                if (vad1 != null)
                    vad1.stopKernel();
                if (vad2 != null)
                    vad2.stopKernel();
                if (vad3 != null)
                    vad3.stopKernel();
                break;
            case 1:
                if (vad0 != null)
                    vad0.stopKernel();
                if (vad2 != null)
                    vad2.stopKernel();
                if (vad3 != null)
                    vad3.stopKernel();
                break;
            case 2:
                if (vad0 != null)
                    vad0.stopKernel();
                if (vad1 != null)
                    vad1.stopKernel();
                if (vad3 != null)
                    vad3.stopKernel();
                break;
            case 3:
                if (vad0 != null)
                    vad0.stopKernel();
                if (vad1 != null)
                    vad1.stopKernel();
                if (vad2 != null)
                    vad2.stopKernel();
                break;
            default:
                break;
        }
    }


    private LinkedList<byte[]> optCache(int index) {
        switch (index) {
            case 0:
                return cache0;
            case 1:
                return cache1;
            case 2:
                return cache2;
            case 3:
                return cache3;
            default:
                return null;
        }
    }

    /**
     * 同步操作
     *
     * @param status 状态
     */
    private void processInit(int status) {

        if (status == AIConstant.OPT_SUCCESS) {
            isInit = true;
        } else {
            isInit = false;
        }

        if (mBarrier != null) {
            try {
                mBarrier.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (BrokenBarrierException e) {
                e.printStackTrace();
            }
        }
    }

    private class Vad0ListenerImpl implements VadKernelListener {

        private static final int INDEX = 0;

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onVadStart(String recordID) {
            if (mListener != null) {
                mListener.onBegin(INDEX, recordID);
            }
        }

        @Override
        public void onVadEnd(String recordID) {
            if (INDEX == ssl && mListener != null) {
                mListener.onEnd(recordID);
            }
        }

        @Override
        public void onRmsChanged(float rmsDb) {

        }

        @Override
        public void onBufferReceived(byte[] data) {
            if (INDEX == ssl) {
                if (mListener != null) {
                    mListener.onBufferReceived(data);
                }
            } else if (DEFAULT_SSL == ssl) {
                if (cache0 != null) {
                    cache0.offer(data);
                } else {
                    Log.e(TAG, "vad cache is null!!!");
                }
            }
        }

        @Override
        public void onResults(String result) {

        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(error);
            }
        }

        @Override
        public void onReadyForSpeech() {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }
    }

    private class Vad1ListenerImpl implements VadKernelListener {

        private static final int INDEX = 1;

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onVadStart(String recordID) {
            if (mListener != null) {
                mListener.onBegin(INDEX, recordID);
            }
        }

        @Override
        public void onVadEnd(String recordID) {
            if (INDEX == ssl && mListener != null) {
                mListener.onEnd(recordID);
            }
        }

        @Override
        public void onRmsChanged(float rmsDb) {

        }

        @Override
        public void onBufferReceived(byte[] data) {
            if (INDEX == ssl) {
                if (mListener != null) {
                    mListener.onBufferReceived(data);
                }
            } else if (DEFAULT_SSL == ssl) {
                if (cache1 != null) {
                    cache1.offer(data);
                } else {
                    Log.e(TAG, "vad cache is null!!!");
                }
            }
        }

        @Override
        public void onResults(String result) {

        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(error);
            }
        }

        @Override
        public void onReadyForSpeech() {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }
    }

    private class Vad2ListenerImpl implements VadKernelListener {

        private static final int INDEX = 2;

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onVadStart(String recordID) {
            if (mListener != null) {
                mListener.onBegin(INDEX, recordID);
            }
        }

        @Override
        public void onVadEnd(String recordID) {
            if (INDEX == ssl && mListener != null) {
                mListener.onEnd(recordID);
            }
        }

        @Override
        public void onRmsChanged(float rmsDb) {

        }

        @Override
        public void onBufferReceived(byte[] data) {
            if (INDEX == ssl) {
                if (mListener != null) {
                    mListener.onBufferReceived(data);
                }
            } else if (DEFAULT_SSL == ssl) {
                if (cache2 != null) {
                    cache2.offer(data);
                } else {
                    Log.e(TAG, "vad cache is null!!!");
                }
            }
        }

        @Override
        public void onResults(String result) {

        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(error);
            }
        }

        @Override
        public void onReadyForSpeech() {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }
    }

    private class Vad3ListenerImpl implements VadKernelListener {

        private static final int INDEX = 3;

        @Override
        public void onInit(int status) {
            processInit(status);
        }

        @Override
        public void onVadStart(String recordID) {
            if (mListener != null) {
                mListener.onBegin(INDEX, recordID);
            }
        }

        @Override
        public void onVadEnd(String recordID) {
            if (INDEX == ssl && mListener != null) {
                mListener.onEnd(recordID);
            }
        }

        @Override
        public void onRmsChanged(float rmsDb) {

        }

        @Override
        public void onBufferReceived(byte[] data) {
            if (INDEX == ssl) {
                if (mListener != null) {
                    mListener.onBufferReceived(data);
                }
            } else if (DEFAULT_SSL == ssl) {
                if (cache3 != null) {
                    cache3.offer(data);
                } else {
                    Log.e(TAG, "vad cache is null!!!");
                }
            }
        }

        @Override
        public void onResults(String result) {

        }

        @Override
        public void onError(AIError error) {
            if (mListener != null) {
                mListener.onError(error);
            }
        }

        @Override
        public void onReadyForSpeech() {

        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size, int wakeupType) {

        }

        @Override
        public void onRawDataReceived(byte[] buffer, int size) {

        }
    }

}
