package com.aispeech.lite.tts;

import com.aispeech.common.Log;
import com.aispeech.common.WavFileWriter;

import java.util.concurrent.LinkedBlockingQueue;

/**
 * 类说明： 合成的数据文件阻塞队列，由合成引擎生产数据，播放器消费数据
 * 
 * @author Everett Li
 * @date Nov 10, 2014
 * @version 1.0
 */
public class SynthesizedBlockQueue {

    public static final String TAG = "SynthesizedBlockQueue";

    LinkedBlockingQueue<SynthesizedBlock> mQueue = new LinkedBlockingQueue<SynthesizedBlock>();

    private int mDataSize;

    /**
     * 向队尾增加新的数据块
     */
    public void addBlock(SynthesizedBlock block) {
//        Log.d(TAG, "add one block:"
//                + ((block.getTextBean() != null) ? (block.getTextBean().getRefText()) : null));
        if (block instanceof SynthesizedBytesBlock) {
            byte[] data = (byte[]) block.getData();
            byte[] noWavHeaderData = WavFileWriter.removeWaveHeader(data);
            block.setData(noWavHeaderData);
            if (noWavHeaderData != null) {
                mDataSize += noWavHeaderData.length;
            }
        }
        mQueue.add(block);
    }

    /**
     * 从队首取出数据块,并将数据块转存至永久缓存中
     * 
     * @return
     */
    public SynthesizedBlock pollBlock() {
        SynthesizedBlock block = null;
        try {
            block = mQueue.take();
//            Log.d(TAG, "poll one block:"
//                    + ((block.getTextBean() != null) ? block.getTextBean().getRefText() : null));
        } catch (InterruptedException e) {
            e.printStackTrace();
            Thread.currentThread().interrupt();
        }
        return block;
    }

    /**
     * 清除数据队列和数据缓存
     */
    public void clear() {
        Log.d(TAG, "clear all blocks");
        mQueue.clear();
        mDataSize = 0;
    }

    public int getTotalDataSize() {
        return mDataSize;
    }

}
