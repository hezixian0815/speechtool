package com.aispeech.lite.sspe;

import android.text.TextUtils;

import com.aispeech.common.ArrayUtils;
import com.aispeech.common.AudioHelper;
import com.aispeech.common.FileSaveUtil;
import com.aispeech.common.Log;
import com.aispeech.export.engines2.AIFespCarEngine;
import com.aispeech.kernel.Sspe;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.fespx.FespxKernelListener;
import com.aispeech.lite.function.ICarFunction;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/2/27
 */
public class CarTwoMicSspeKernel extends BaseCarSspeKernel {

    //    0-asr0  , 1-asr1 , 2-vad0 , 3-vad1 , 4-asr2 , 5-asr3
    private static final int AUDIO_TYPE_ASR0 = 0;
    private static final int AUDIO_TYPE_ASR1 = 1;
    private static final int AUDIO_TYPE_VAD0 = 2;
    private static final int AUDIO_TYPE_VAD1 = 3;
    private static final int AUDIO_TYPE_ASR2 = 4;
    private static final int AUDIO_TYPE_ASR3 = 5;

    byte[] bf0, bf1, vad0, vad1, asr0, asr1;
    byte[] bfCallback0, vadCallback0, bfCallback1, vadCallback1;

    FileSaveUtil bfFileSaveUtil;
    FileSaveUtil vadFileSaveUtil;
    FileSaveUtil asrFileSaveUtil;

    public CarTwoMicSspeKernel(FespxKernelListener listener) {
        super("CarTwoMicSspeKernel", listener);
    }

    @Override
    protected Object[] getSspeCallbacks() {
        return ArrayUtils.concatAll(super.getSspeCallbacks(), new Object[]{
                new MyMultibfCallback(),new DoaCommonCallback(), new CarDoaCallbackImpl(), new CarWakeupCallbackImpl()});
    }

    /**
     * 多路beamforming callback
     */
    private class MyMultibfCallback implements Sspe.multibf_callback {

        @Override
        public int run(int index, byte[] data, int size) {
//            Log.d(TAG,"MyMultibfCallback:" + index);
            //六路音频
            switch (index) {
                case AUDIO_TYPE_ASR0:
                    bf0 = data;
                    break;
                case AUDIO_TYPE_ASR1:
                    bf1 = data;
                    break;
                case AUDIO_TYPE_VAD0:
                    vad0 = data;
                    break;
                case AUDIO_TYPE_VAD1:
                    vad1 = data;
                    break;
                case AUDIO_TYPE_ASR2:
                    asr0 = data;
                    break;
                case AUDIO_TYPE_ASR3:
                    asr1 = data;
                    break;
            }

            saveOutData(index);

            //回调信号处理后的音频，跟驾驶模式无关
            processorCallbackData(index, data);

            //全车模式过滤，或者非驾驶模式
            if (mDriveMode == ICarFunction.DRIVEMODE_ENTIRE || (mDriveMode == ICarFunction.DRIVEMODE_POSITIONING && mCachedWakeUpDoa == -1)) {
                if (AISpeech.useDoubleVad && index == 5 && asr0 != null && mDriveMode == ICarFunction.DRIVEMODE_ENTIRE) {//全车模式做混音
                    if (isCallBackResultData(AIFespCarEngine.FESPCAR_DOUBLEVAD_DATARECEIVED_ENABLE)) {
                        byte[] multiData = AudioHelper.mixRawAudioBytes(new byte[][]{asr0, asr1});
                        byte[] bufferData = new byte[multiData.length];
                        System.arraycopy(multiData, 0, bufferData, 0, multiData.length);

                        byte[] bufferData1 = new byte[multiData.length];
                        System.arraycopy(multiData, 0, bufferData1, 0, multiData.length);
                        mListener.onResultDataReceived(bufferData, bufferData1);
                        asr0 = null;
                    }
                } else if ((index == 1 && bf0 != null) && (mDriveMode == ICarFunction.DRIVEMODE_POSITIONING && mCachedWakeUpDoa == -1)) {
                    if (isCallBackResultData(AIFespCarEngine.FESPCAR_DOUBLEVAD_DATARECEIVED_ENABLE)) {
                        byte[] multData = AudioHelper.mixRawAudioBytes(new byte[][]{bf0, bf1});
                        byte[] bufferData = new byte[multData.length];
                        System.arraycopy(multData, 0, bufferData, 0, multData.length);
                        if (AISpeech.useDoubleVad) {
                            byte[] bufferData1 = new byte[multData.length];
                            System.arraycopy(multData, 0, bufferData1, 0, multData.length);
                            mListener.onResultDataReceived(bufferData, bufferData1);
                        } else {
                            if (isCallBackResultData(AIFespCarEngine.FESPCAR_SINGLEVAD_DATARECEIVED_ENABLE)) {
                                mListener.onResultDataReceived(bufferData, bufferData.length, 0);
                            }
                        }
                        bf0 = null;
                    } else if ((index == 1 && bf0 != null) && (mDriveMode == ICarFunction.DRIVEMODE_ENTIRE) && !AISpeech.useDoubleVad) {
                        if (isCallBackResultData(AIFespCarEngine.FESPCAR_SINGLEVAD_DATARECEIVED_ENABLE)) {
                            byte[] multData = AudioHelper.mixRawAudioBytes(new byte[][]{bf0, bf1});
                            mListener.onResultDataReceived(multData, multData.length, 0);
                            bf0 = null;
                        }
                        return 0;
                    }
                }
            }

            //定位模式过滤
//            if (mDriveMode == ICarFunction.DRIVEMODE_POSITIONING && cachedDriveMode == -1) {
//                return 0;
//            }

            if (AISpeech.useDoubleVad) {
                if (isCallBackResultData(AIFespCarEngine.FESPCAR_DOUBLEVAD_DATARECEIVED_ENABLE)) {
                    byte[] vadBufferData = new byte[size];
                    byte[] asrBufferData = new byte[size];
                    if ((mDriveMode == ICarFunction.DRIVEMODE_MAIN || mCachedWakeUpDoa == ICarFunction.DRIVEMODE_MAIN)
                            && (index == 2 && bf0 != null)) {
                        System.arraycopy(bf0, 0, asrBufferData, 0, size);
                        System.arraycopy(vad0, 0, vadBufferData, 0, size);
                        mListener.onResultDataReceived(vadBufferData, asrBufferData);
                        return 0;
                    } else if ((mDriveMode == ICarFunction.DRIVEMODE_COPILOT || mCachedWakeUpDoa == ICarFunction.DRIVEMODE_COPILOT)
                            && (index == 3 && bf1 != null)) {
                        System.arraycopy(bf1, 0, asrBufferData, 0, size);
                        System.arraycopy(vad1, 0, vadBufferData, 0, size);
                        mListener.onResultDataReceived(vadBufferData, asrBufferData);
                        return 0;
                    }
                }
            } else {
                //主驾模式过滤 ，只取index = 0 音频,双VAD模式，取index = 0 & 2 音频
                if ((mDriveMode == ICarFunction.DRIVEMODE_MAIN || mCachedWakeUpDoa == ICarFunction.DRIVEMODE_MAIN)
                        && (index != 0)) {
                    return 0;
                }

                //副驾模式过滤， 只取index = 1 音频,双VAD模式，取index = 1 & 3 音频
                if ((mDriveMode == ICarFunction.DRIVEMODE_COPILOT || mCachedWakeUpDoa == ICarFunction.DRIVEMODE_COPILOT)
                        && (index != 1)) {
                    return 0;
                }
                if (isCallBackResultData(AIFespCarEngine.FESPCAR_SINGLEVAD_DATARECEIVED_ENABLE)) {
                    byte[] bufferData = new byte[size];
                    System.arraycopy(data, 0, bufferData, 0, size);
                    mListener.onResultDataReceived(bufferData, size, 0);
                }
            }


            return 0;
        }

        /**
         * 经过信号出路模块处理后的音频数据返回（携带是否开启双VAD标识）
         *
         * @param index 音频标识
         * @param data  bf数据
         */
        private void processorCallbackData(int index, byte[] data) {
            // Log.i(TAG, "---processorCallbackData() index:" + index);
            if (AISpeech.useDoubleVad) {
                byte[] callbackData = new byte[data.length];
                System.arraycopy(data, 0, callbackData, 0, data.length);
                switch (index) {
                    case AUDIO_TYPE_ASR0:
                        bfCallback0 = callbackData;
                        break;
                    case AUDIO_TYPE_ASR1:
                        bfCallback1 = callbackData;
                        break;
                    case AUDIO_TYPE_VAD0:
                        vadCallback0 = callbackData;
                        break;
                    case AUDIO_TYPE_VAD1:
                        vadCallback1 = callbackData;
                        break;
                    default:
                        break;
                }
                if (index == 3) {
                    if (bfCallback0 != null && bfCallback1 != null && vadCallback0 != null) {
                        if (isCallBackResultData(AIFespCarEngine.FESPCAR_MIXING_DATARECEIVED_ENABLE)) {
                            byte[] multData = AudioHelper.mixMultChannel(4,
                                    vadCallback0, bfCallback0, vadCallback1, bfCallback1);
                            mListener.onResultDataReceived(multData, true);
                        }
                    }
                }
            } else {
                if (isCallBackResultData(AIFespCarEngine.FESPCAR_MIXING_DATARECEIVED_ENABLE)) {
                    //新增经过信号出路模块处理后的音频数据返回（携带是否开启双VAD标识）
                    mListener.onResultDataReceived(data, false);
                }
            }
        }

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

    private void saveOutData(int index) {

        if (index == AUDIO_TYPE_ASR1 && bfFileSaveUtil != null) {
            byte[] multiBf = AudioHelper.mixMultChannel(2, bf0, bf1);
            bfFileSaveUtil.feedTypeOut(multiBf);
        }

        if (index == AUDIO_TYPE_VAD1 && vadFileSaveUtil != null) {
            byte[] multiVad = AudioHelper.mixMultChannel(2, vad0, vad1);
            vadFileSaveUtil.feedTypeOut(multiVad);
        }

        if (index == AUDIO_TYPE_ASR3 && asrFileSaveUtil != null) {
            byte[] multiAsr = AudioHelper.mixMultChannel(2, asr0, asr1);
            asrFileSaveUtil.feedTypeOut(multiAsr);
        }

    }
}
