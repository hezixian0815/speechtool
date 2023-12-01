package com.aispeech.lite.sspe;

import android.text.TextUtils;

import com.aispeech.common.ArrayUtils;
import com.aispeech.common.AudioHelper;
import com.aispeech.common.FileSaveUtil;
import com.aispeech.common.Log;
import com.aispeech.export.engines2.AIFespCarEngine;
import com.aispeech.kernel.LiteSoFunction;
import com.aispeech.kernel.Sspe;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.config.LocalSignalProcessingConfig;
import com.aispeech.lite.fespx.FespxKernelListener;
import com.aispeech.lite.function.ICarFunction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CyclicBarrier;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/2/27
 */
public class CarFourMicSspeKernel extends BaseCarSspeKernel {

    private static final int AUDIO_TYPE_BF0 = 0;
    private static final int AUDIO_TYPE_BF1 = 1;
    private static final int AUDIO_TYPE_BF2 = 2;
    private static final int AUDIO_TYPE_BF3 = 3;

    private static final int AUDIO_TYPE_VAD0 = 0;
    private static final int AUDIO_TYPE_VAD1 = 1;
    private static final int AUDIO_TYPE_VAD2 = 2;
    private static final int AUDIO_TYPE_VAD3 = 3;

    private static final int AUDIO_TYPE_ASR0 = 0;
    private static final int AUDIO_TYPE_ASR1 = 1;
    private static final int AUDIO_TYPE_ASR2 = 2;
    private static final int AUDIO_TYPE_ASR3 = 3;

    volatile byte[] bf0, bf1, bf2, bf3;
    volatile byte[] vad0, vad1, vad2, vad3;
    volatile byte[] asr0, asr1, asr2, asr3;

    FileSaveUtil bfFileSaveUtil;
    FileSaveUtil vadFileSaveUtil;
    FileSaveUtil asrFileSaveUtil;

    CyclicBarrier mVadAsrBarrier;

    public CarFourMicSspeKernel(FespxKernelListener listener) {
        super("CarFourMicSspeKernel", listener);
    }

    @Override
    protected Object[] getSspeCallbacks() {
        return ArrayUtils.concatAll(super.getSspeCallbacks(), new Object[]{
                new MyMultibfCallback(),new DoaCommonCallback(), new CarDoaCallbackImpl(), new CarWakeupCallbackImpl(),
                new MyVadCallback(), new MyAsrCallback()});
    }

    @Override
    protected void preInitEngine(LiteSoFunction engine, LocalSignalProcessingConfig config) {
        super.preInitEngine(engine, config);
        if (mVadAsrBarrier == null) {
            mVadAsrBarrier = new CyclicBarrier(2, this::processVadAsrData);
        }
    }

    /**
     * 多路beamforming callback
     * nn 方案三组：
     * asr -- 第一组
     * vad -- 第二组
     * mulbf -- 第三组 全车混音取第三组
     * <p>
     * 抛出顺序： multi - vad - asr
     */
    private class MyMultibfCallback implements Sspe.multibf_callback {

        @Override
        public int run(int index, byte[] data, int size) {
//            Log.d(TAG, "MyMultibfCallback:" + index);

            if (!AISpeech.useDoubleVad) {
                //新增经过信号出路模块处理后的音频数据返回（携带是否开启双VAD标识）
                processData(index, data, size);
            }

            switch (index) {
                case AUDIO_TYPE_BF0:
                    bf0 = data;
                    break;
                case AUDIO_TYPE_BF1:
                    bf1 = data;
                    break;
                case AUDIO_TYPE_BF2:
                    bf2 = data;
                    break;
                case AUDIO_TYPE_BF3:
                    bf3 = data;
                    break;
            }

            if (index == AUDIO_TYPE_BF3) {
                processBfData();
            }

            if (index == AUDIO_TYPE_BF3 && bfFileSaveUtil != null) {
                byte[] multiBf = AudioHelper.mixMultChannel(4, bf0, bf1, bf2, bf3);
                bfFileSaveUtil.feedTypeOut(multiBf);
            }

            return 0;
        }

    }

    private class MyAsrCallback extends Sspe.asr_audio_callback {

        @Override
        public int run(int index, byte[] data, int size) {
//            Log.d(TAG, "MyAsrCallback:" + index);
            switch (index) {
                case AUDIO_TYPE_ASR0:
                    asr0 = data;
                    break;
                case AUDIO_TYPE_ASR1:
                    asr1 = data;
                    break;
                case AUDIO_TYPE_ASR2:
                    asr2 = data;
                    break;
                case AUDIO_TYPE_ASR3:
                    asr3 = data;
                    break;
            }

            if (index == AUDIO_TYPE_ASR3) {
                processAsrData();
            }

            if (index == AUDIO_TYPE_ASR3 && asrFileSaveUtil != null) {
                byte[] multiAsr = AudioHelper.mixMultChannel(4, asr0, asr1, asr2, asr3);
                asrFileSaveUtil.feedTypeOut(multiAsr);
            }

            if (AISpeech.useDoubleVad && index == AUDIO_TYPE_ASR3) {
                try {
                    mVadAsrBarrier.await();
                } catch (Exception e) {
                    mVadAsrBarrier.reset();
                }
            }
            return 0;
        }
    }

    private class MyVadCallback extends Sspe.vad_audio_callback {

        @Override
        public int run(int index, byte[] data, int size) {
//            Log.d(TAG, "MyVadCallback:" + index);
            switch (index) {
                case AUDIO_TYPE_VAD0:
                    vad0 = data;
                    break;
                case AUDIO_TYPE_VAD1:
                    vad1 = data;
                    break;
                case AUDIO_TYPE_VAD2:
                    vad2 = data;
                    break;
                case AUDIO_TYPE_VAD3:
                    vad3 = data;
                    break;
            }

            if (index == AUDIO_TYPE_VAD3 && vadFileSaveUtil != null) {
                byte[] multiVad = AudioHelper.mixMultChannel(4, vad0, vad1, vad2, vad3);
                vadFileSaveUtil.feedTypeOut(multiVad);
            }

            if (AISpeech.useDoubleVad && index == AUDIO_TYPE_VAD3) {
                try {
                    mVadAsrBarrier.await();
                } catch (Exception e) {
                    mVadAsrBarrier.reset();
                }
            }

            return 0;
        }

    }


    private void processVadAsrData() {
//        Log.i(TAG, "processVadAsrData");

        if (vad0 == null || vad1 == null || vad2 == null || vad3 == null) {
            Log.e(TAG, "error state ,some vad data is null!!!");
            return;
        }

        if (asr0 == null || asr1 == null || asr2 == null || asr3 == null) {
            Log.e(TAG, "error state ,some asr data is null!!!");
            return;
        }

//        Log.i(TAG, "real processVadAsrData");

        if (mConfig.isFourHost()) {
            if (isCallBackResultData(AIFespCarEngine.FESPCAR_DOUBLEVAD_DATARECEIVED_ENABLE)) {
                byte[] multasr = AudioHelper.mixMultChannel(4, asr0, asr1, asr2, asr3);
                byte[] multvad = AudioHelper.mixMultChannel(4, vad0, vad1, vad2, vad3);
                if (mListener != null) mListener.onResultDataReceived(multvad, multasr);
            }
        } else {
            byte[][] datas = new byte[][]{vad0, asr0, vad1, asr1, vad2, asr2, vad3, asr3};

            if (isCallBackResultData(AIFespCarEngine.FESPCAR_MIXING_DATARECEIVED_ENABLE)) {
                // 抛出音频 与驾驶模式无关
                byte[] multData = AudioHelper.mixMultChannel(8, datas);
                if (mListener != null) {
                    mListener.onResultDataReceived(multData, true);

                }
            }

            // 根据驾驶模式/唤醒位置 抛出对应位置音频

            // 主驾模式
            if ((mDriveMode == ICarFunction.DRIVEMODE_MAIN || mCachedWakeUpDoa == ICarFunction.DRIVEMODE_MAIN)) {
                doCallbackResultData(datas[0], datas[1]);
            }

            // 副驾模式
            if ((mDriveMode == ICarFunction.DRIVEMODE_COPILOT || mCachedWakeUpDoa == ICarFunction.DRIVEMODE_COPILOT)) {
                doCallbackResultData(datas[2], datas[3]);
            }

            // 自由组合模式 左后
            if (mCachedWakeUpDoa == ICarFunction.CAR_DOA_LEFT_BACKSEAT) {
                doCallbackResultData(datas[4], datas[5]);
            }

            // 自由组合模式 右后
            if (mCachedWakeUpDoa == ICarFunction.CAR_DOA_RIGHT_BACKSEAT) {
                doCallbackResultData(datas[6], datas[7]);
            }
        }

//        Log.i(TAG, "reset");
        // reset
        vad0 = vad1 = vad2 = vad3 = asr0 = asr1 = asr2 = asr3 = null;
    }

    private void processAsrData() {
//        Log.i(TAG, "processAsrData");
        byte[][] datas;
        if (asr0 != null && asr1 != null && asr2 != null && asr3 != null) {
            datas = new byte[][]{asr0, asr1, asr2, asr3};
        } else {
            Log.e(TAG, "error state " + Arrays.asList(asr0, asr1, asr2, asr3));
            return;
        }

        // 全车模式做混音
        if (mDriveMode == ICarFunction.DRIVEMODE_ENTIRE && !mConfig.isFourHost()) {
            if (isCallBackResultData(AIFespCarEngine.FESPCAR_DOUBLEVAD_DATARECEIVED_ENABLE)) {
                byte[] multiData = AudioHelper.mixRawAudioBytes(filterWithChannelMask(datas, mWakeupChannelMask));
                if (multiData == null) return;

                byte[] bufferData = new byte[multiData.length];
                System.arraycopy(multiData, 0, bufferData, 0, multiData.length);

                byte[] bufferData1 = new byte[multiData.length];
                System.arraycopy(multiData, 0, bufferData1, 0, multiData.length);
                if (mListener != null) mListener.onResultDataReceived(bufferData, bufferData1);
            }
        }
    }

    private void processBfData() {

        byte[][] datas = null;
        if (bf0 != null && bf1 != null && bf2 != null) {
            datas = new byte[][]{bf0, bf1, bf2, bf3};
        }

        if (datas == null) return;

        // 定位模式/自由组合模式 未唤醒时 做混音抛出
        if ((mDriveMode == ICarFunction.DRIVEMODE_POSITIONING || mDriveMode == ICarFunction.DRIVEMODE_FREE_COMBINTION)
                && mCachedWakeUpDoa == -1) {

            byte[] multData;
            if (mDriveMode == ICarFunction.DRIVEMODE_POSITIONING) {
                multData = AudioHelper.mixRawAudioBytes(datas);
            } else {
                // 自由组合模式 未唤醒时 根据mask抛出混音
                multData = AudioHelper.mixRawAudioBytes(filterWithChannelMask(datas, mWakeupChannelMask));
            }


            if (AISpeech.useDoubleVad && !mConfig.isFourHost() && multData != null) {
                if (isCallBackResultData(AIFespCarEngine.FESPCAR_DOUBLEVAD_DATARECEIVED_ENABLE)) {
                    byte[] bufferData = new byte[multData.length];
                    System.arraycopy(multData, 0, bufferData, 0, multData.length);
                    // 保证两路输出是独立的不互相影响
                    if (params.isNeedCopyResultData()) {
                        byte[] bufferData1 = new byte[multData.length];
                        System.arraycopy(multData, 0, bufferData1, 0, multData.length);
                        if (mListener != null)
                            mListener.onResultDataReceived(bufferData, bufferData1);
                    } else {
                        if (mListener != null) mListener.onResultDataReceived(multData, bufferData);
                    }
                }
            } else if (!AISpeech.useDoubleVad && multData != null) {
                doCallbackResultData(multData, multData.length, 0);
            }
        }

        // 全车模式 非双vad 做混音抛出
        if ((mDriveMode == ICarFunction.DRIVEMODE_ENTIRE) && !AISpeech.useDoubleVad) {
            if (isCallBackResultData(AIFespCarEngine.FESPCAR_SINGLEVAD_DATARECEIVED_ENABLE)) {
                byte[] multData = AudioHelper.mixRawAudioBytes(filterWithChannelMask(datas, mWakeupChannelMask));
                if (mListener != null && multData != null)
                    mListener.onResultDataReceived(multData, multData.length, 0);
            }
        }
    }

    /**
     * 这里抛出非双VAD情况下 除了全车模式外的音频
     * 全车模式需要做混音所以不再此处抛出
     *
     * @param index
     * @param data
     * @param size
     */
    private void processData(int index, byte[] data, int size) {

        byte[] bufferData = checkNeedCopyResultData(params, data, size);

        if (mListener != null) {
            if (isCallBackResultData(AIFespCarEngine.FESPCAR_MIXING_DATARECEIVED_ENABLE)) {
                mListener.onResultDataReceived(bufferData, false);
            }
        }

        // 定位模式or自由组合模式 未唤醒时
        if ((mDriveMode == ICarFunction.DRIVEMODE_POSITIONING || mDriveMode == ICarFunction.DRIVEMODE_FREE_COMBINTION)
                && mCachedWakeUpDoa == -1)
            return;

        // 全车模式 后面做混音抛出
        if (mDriveMode == ICarFunction.DRIVEMODE_ENTIRE)
            return;

        //主驾模式过滤 ，只取index = 0 音频,双VAD模式，取index = 0 & 4 音频
        if ((mDriveMode == ICarFunction.DRIVEMODE_MAIN || mCachedWakeUpDoa == ICarFunction.DRIVEMODE_MAIN)
                && (index != 0)) {
            return;
        }

        //副驾模式过滤， 只取index = 1 音频,双VAD模式，取index = 1 & 5 音频
        if ((mDriveMode == ICarFunction.DRIVEMODE_COPILOT || mCachedWakeUpDoa == ICarFunction.DRIVEMODE_COPILOT)
                && (index != 1)) {
            return;
        }

        // 自有组合模式过滤 左后
        if (mCachedWakeUpDoa == ICarFunction.CAR_DOA_LEFT_BACKSEAT && index != 2) {
            return;
        }

        // 自有组合模式过滤 右后
        if (mCachedWakeUpDoa == ICarFunction.CAR_DOA_RIGHT_BACKSEAT && index != 3) {
            return;
        }

        if (mListener != null) {
            if (isCallBackResultData(AIFespCarEngine.FESPCAR_SINGLEVAD_DATARECEIVED_ENABLE)) {
                mListener.onResultDataReceived(bufferData, size, 0);
            }
        }
    }

    private byte[][] filterWithChannelMask(byte[][] data, int mask) {
        ArrayList<byte[]> result = new ArrayList<>(4);

        if ((ICarFunction.COMBINATION_POSITION_MAIN & mask) > 0) {
            result.add(data[0]);
        }

        if ((ICarFunction.COMBINATION_POSITION_COPILOT & mask) > 0) {
            result.add(data[1]);
        }

        if ((ICarFunction.COMBINATION_POSITION_LEFT_BACKSEAT & mask) > 0) {
            result.add(data[2]);
        }

        if ((ICarFunction.COMBINATION_POSITION_RIGHT_BACKSEAT & mask) > 0) {
            result.add(data[3]);
        }
        return result.toArray(new byte[0][]);
    }

    @Override
    protected void createFileWriter() {
        if (params == null) return;
        super.createFileWriter();

        String saveAudioPath = params.getSaveAudioPath();
        if (!TextUtils.isEmpty(saveAudioPath)) {
            Log.d(TAG, "raw path: " + saveAudioPath);

            bfFileSaveUtil = new FileSaveUtil();
            bfFileSaveUtil.init(saveAudioPath);
            bfFileSaveUtil.prepare("k-bf");

            if (AISpeech.useDoubleVad) {
                vadFileSaveUtil = new FileSaveUtil();
                vadFileSaveUtil.init(saveAudioPath);
                vadFileSaveUtil.prepare("k-vad");

                asrFileSaveUtil = new FileSaveUtil();
                asrFileSaveUtil.init(saveAudioPath);
                asrFileSaveUtil.prepare("k-asr");
            }

        }
    }

    @Override
    protected void closeFileWriter() {
        super.closeFileWriter();
        Log.d(TAG, "closeFileSaveUtil");

        if (bfFileSaveUtil != null) {
            bfFileSaveUtil.close();
            bfFileSaveUtil = null;
        }

        if (vadFileSaveUtil != null) {
            vadFileSaveUtil.close();
            vadFileSaveUtil = null;
        }

        if (asrFileSaveUtil != null) {
            asrFileSaveUtil.close();
            asrFileSaveUtil = null;
        }
    }


}
