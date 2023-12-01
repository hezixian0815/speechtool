package com.aispeech.lite.sspe;

import android.text.TextUtils;

import com.aispeech.common.AIConstant;
import com.aispeech.common.ArrayUtils;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.kernel.Sspe;
import com.aispeech.lite.fespx.FespxKernelListener;
import com.aispeech.lite.message.Message;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/3/28
 */
public class SspeKernel extends BaseSspeKernel {


    public SspeKernel(FespxKernelListener listener) {
        super("SspeKernel", listener);
    }

    @Override
    protected Object[] getSspeCallbacks() {
        return ArrayUtils.concatAll(super.getSspeCallbacks(), new Object[]{new MyMultibfCallbackImpl(), new CommonDoaCallback(), new MyDoaCallback(), new MyBeamformingCallback(), new MyWakeupCallback()});
    }

    /**
     * 多路beamforming音频回调，返回多路beamforming数据
     */
    private class MyMultibfCallbackImpl implements Sspe.multibf_callback {

        @Override
        public int run(int index, byte[] data, int size) {
            byte[] mBufferData = checkNeedCopyResultData(params, data, size);
            mListener.onMultibfDataReceived(index, mBufferData, size);

            return 0;
        }
    }

    private class MyDoaCallback implements Sspe.doa_callback {

        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "MyDoaCallback return : " + retString);
                processDoaResult(retString);
            }
            return 0;
        }
    }

    protected void processDoaResult(String retString) {
        if (!params.isInputContinuousAudio()) {
            if (!mHasDoaOut) {
                mWakeupProcessor.processDoaResult(retString);
                mHasDoaOut = true;
                Log.d(TAG, "first doa cb end");
            } else {
                Log.w(TAG, "more than one doa, ignore");
            }
        } else {
            mWakeupProcessor.processDoaResult(retString);
        }
    }

    private class CommonDoaCallback implements Sspe.doa_common_callback {

        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "MyCommonDoaCallback return : " + retString);
                processDoaResult(retString);
            }
            return 0;
        }
    }

    /**
     * bf音频回调，包含wakeupType
     * 车载不需要用到这个callback 默认为false
     */
    private class MyBeamformingCallback extends Sspe.beamforming_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "MyBeamformingCallback json return : " + retString);
                mListener.onResultDataReceived(new byte[0], 0, mWakeupProcessor.processWakeupType(retString));
            } else if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bufferData = checkNeedCopyResultData(params, data, size);
                mListener.onResultDataReceived(bufferData, size, 0);

                // 音频保存逻辑
                if (TextUtils.isEmpty(params.getSaveAudioPath()) && (params == null || params.isNeedCopyResultData())) {
                    sendMessage(new Message(Message.MSG_BEAMFORMING_DATA, bufferData));
                } else if (TextUtils.isEmpty(params.getSaveAudioPath())) {
                    bufferData = new byte[size];
                    System.arraycopy(data, 0, bufferData, 0, size);
                    sendMessage(new Message(Message.MSG_BEAMFORMING_DATA, bufferData));
                }
            }
            return 0;
        }
    }

    /**
     * 唤醒信息回调
     */
    private class MyWakeupCallback implements Sspe.wakeup_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "MyWakeupCallback return : " + retString);
                processWakeupCallback(retString);
            }
            return 0;
        }
    }
}
