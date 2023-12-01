package com.aispeech.common;

import java.util.ArrayList;
import java.util.Arrays;

public class AudioHelper {

    private static final String TAG = "AudioHelper";

    /**
     * 多路音频混合叠加成一路
     *
     * @param bMulRoadAudioes 多路音频
     * @return 一路音频
     */
    public static byte[] mixRawAudioBytes(byte[][] bMulRoadAudioes) {

        if (bMulRoadAudioes == null || bMulRoadAudioes.length == 0)
            return null;

        byte[] realMixAudio = bMulRoadAudioes[0];

        if (bMulRoadAudioes.length == 1)
            return realMixAudio;

        for (int rw = 0; rw < bMulRoadAudioes.length; ++rw) {
            if (bMulRoadAudioes[rw].length != realMixAudio.length) {
                Log.e("app", "column of the road of audio + " + rw + " is diffrent.");
                return null;
            }
        }

        //row 代表参与合成的音频数量
        //column 代表一段音频的采样点数，这里所有参与合成的音频的采样点数都是相同的
        int row = bMulRoadAudioes.length;
        int coloum = realMixAudio.length / 2;
        short[][] sMulRoadAudioes = new short[row][coloum];

        //PCM音频16位的存储是大端存储方式，即低位在前，高位在后，例如(X1Y1, X2Y2, X3Y3)数据，它代表的采样点数值就是(（Y1 * 256 + X1）, （Y2 * 256 + X2）, （Y3 * 256 + X3）)
        for (int r = 0; r < row; ++r) {
            for (int c = 0; c < coloum; ++c) {
                sMulRoadAudioes[r][c] = (short) ((bMulRoadAudioes[r][c * 2] & 0xff) | (bMulRoadAudioes[r][c * 2 + 1] & 0xff) << 8);
            }
        }

        short[] sMixAudio = new short[coloum];
        int mixVal;
        int sr = 0;
        for (int sc = 0; sc < coloum; ++sc) {
            mixVal = 0;
            sr = 0;
            //这里采取累加法
            for (; sr < row; ++sr) {
                mixVal += sMulRoadAudioes[sr][sc];
            }
            //最终值不能大于short最大值，因此可能出现溢出
            sMixAudio[sc] = (short) (mixVal);
        }

        //short值转为大端存储的双字节序列
        for (sr = 0; sr < coloum; ++sr) {
            realMixAudio[sr * 2] = (byte) (sMixAudio[sr] & 0x00FF);
            realMixAudio[sr * 2 + 1] = (byte) ((sMixAudio[sr] & 0xFF00) >> 8);
        }

        return realMixAudio;
    }


    /**
     * 从多路原始音频中裁取指定路音频数据
     *
     * @param sourceData 从read方法中读取的原始数据。
     * @param channelNum 要分离的声道数的索引，范围[0，channelMax-1]。具体哪个声道要依赖于数据的排布顺序。
     *                   例如：双声道（前排左右），
     *                   四声道（前排左右参考1参考2），
     *                   六声道排布（前排左右后排左右参考1参考2）
     *                   八声道排布（前排左右后排左右参考1参考2参考3参考4）
     * @param channelMax 音频总通道数。
     * @return channelNum 返回指定的音频路数据
     */
    public static byte[] splitOriginalChannel(byte[] sourceData, int channelNum, int channelMax) {
        if (channelMax <= 0) {
            Log.e(TAG, "splitOriginalChannel channelMax error，channelMax=" + channelMax);
            return null;
        }
        if (channelNum < 0 || channelNum >= channelMax) {
            Log.e(TAG, "splitOriginalChannel channelNum error， channelNum=" + channelNum);
            return null;
        }
        if (sourceData == null || sourceData.length <= 0) {
            Log.e(TAG, "splitOriginalChannel sourceData is null");
            return null;
        }
        byte[] localData = Arrays.copyOf(sourceData, sourceData.length);
        byte[] channelData = new byte[localData.length / channelMax];
        int j = 0;
        for (int i = 0; i < localData.length; ) {
            channelData[j++] = localData[i + 2 * channelNum];
            channelData[j++] = localData[i + 2 * channelNum + 1];
            i = i + channelMax * 2;
        }
        return channelData;
    }


    /***
     * 按采样点交替混合多路音频
     * @param channelNum 音频路数
     * @param channelData 需要混合的各路音频
     * @return 混合之后的一路音频数据
     */
    public static byte[] mixMultChannel(int channelNum, byte[]... channelData) {
        byte[] mixMultChannelBuffer = new byte[channelData[0].length * channelNum];
        for (int i = 0; i < mixMultChannelBuffer.length; ) {
            for (int j = 0; j < channelNum * 2; ) {
                mixMultChannelBuffer[i + j] = channelData[j / 2][i / channelNum];
                mixMultChannelBuffer[i + 1 + j] = channelData[j / 2][i / channelNum + 1];
                j = j + 2;
            }
            i = i + channelNum * 2;
        }
        return mixMultChannelBuffer;
    }

    /**
     * 调换5,6路音频通路顺序
     *
     * @param orignalData 原始数据
     * @return 调换顺序后的音频
     */
    public static byte[] rearrangeAudioData(byte[] orignalData) {
        byte[] fisrtChannel = splitOriginalChannel(orignalData, 0, 6);
        byte[] secondChannel = splitOriginalChannel(orignalData, 1, 6);
        byte[] thirdChannel = splitOriginalChannel(orignalData, 2, 6);
        byte[] fourthChannel = splitOriginalChannel(orignalData, 3, 6);
        byte[] fifthChannel = splitOriginalChannel(orignalData, 4, 6);
        byte[] sixthChannel = splitOriginalChannel(orignalData, 5, 6);
        return mixMultChannel(6, fisrtChannel, secondChannel, fifthChannel, sixthChannel, thirdChannel, fourthChannel);
    }


    /**
     * 调换音频通道
     *
     * @param data     音频数据
     * @param channel  音频通道数量
     * @param sequence mic序列
     * @return 调整通道后的音频
     */
    public static byte[] changeChannel(byte[] data, int channel, int[] sequence) {
        if (channel != sequence.length) {
            throw new IllegalArgumentException("parameter error!");
        }
        ArrayList<AudioData> audios = splitDataV2(data, data.length, channel);
        byte[] buffer = null;
        if (channel == 2) {
            buffer = mixMultChannel(channel,
                    audios.get(sequence[0] - 1).getData(),
                    audios.get(sequence[1] - 1).getData()
            );
        } else if (channel == 4) {
            buffer = mixMultChannel(channel,
                    audios.get(sequence[0] - 1).getData(),
                    audios.get(sequence[1] - 1).getData(),
                    audios.get(sequence[2] - 1).getData(),
                    audios.get(sequence[3] - 1).getData()
            );
        }
        return buffer;
    }


    public static class AudioData {
        private int channel;
        private byte[] data;

        public AudioData(int channel, byte[] data) {
            this.channel = channel;
            this.data = data;
        }

        public int getChannel() {
            return channel;
        }

        public void setChannel(int channel) {
            this.channel = channel;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }

    /**
     * 分离音频通道
     *
     * @param data    音频数据
     * @param size    音频大小
     * @param channel 音频通道
     */
    private static ArrayList<AudioData> splitDataV2(byte[] data, int size, int channel) {
        ArrayList<AudioData> audios = new ArrayList<>();
        for (int i = 0; i < channel; i++) {
            byte[] buffer = new byte[size / channel];
            for (int j = 0; j < size / (channel * 2); j++) {
                buffer[2 * j] = data[2 * (j * channel + i)];
                buffer[2 * j + 1] = data[2 * (j * channel + i) + 1];
            }
            AudioData mAudioData = new AudioData(i, buffer);
            audios.add(mAudioData);
        }
        return audios;
    }
}
