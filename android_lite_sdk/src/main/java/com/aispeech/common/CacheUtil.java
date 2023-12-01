package com.aispeech.common;

import java.util.Arrays;
import java.util.LinkedList;

public class CacheUtil {
    private static final String TAG = "CacheUtil";
    private CacheType mType;
    private LinkedList<byte[]> mCacheList = new LinkedList<>();
    private int mOneshotCacheTime = 10000;// 要缓存的音频长度, 默认缓存2秒的音频
    private long mOneshotCacheByteLength;// 要缓存的音频长度
    private int mVadCacheTime = 500;// vad回退的音频, 默认500ms

    public CacheUtil(CacheType type) {
        mType = type;
        mOneshotCacheByteLength = mOneshotCacheTime * mType.getSize() / 100;
        Log.d(TAG, "mOneshotCacheByteLength = " + mOneshotCacheByteLength);
    }

    public byte[] getCacheData() {
        return toArray();
    }

    public byte[] toArray() {
        byte[] data = new byte[0];
        LinkedList<byte[]> queueClone = (LinkedList<byte[]>) mCacheList.clone();
        while (!queueClone.isEmpty()) {
            byte[] buffer = queueClone.poll();
            byte[] result = Arrays.copyOf(data, data.length + buffer.length);
            System.arraycopy(buffer, 0, result, data.length, buffer.length);
            data = result;
        }
        return data;
    }

    public int getLength() {
        return mCacheList.size();
    }

    public void setOneshotCacheTime(int time) {
        mOneshotCacheTime = time;
    }

    public void setVadCacheTime(int time) {
        mVadCacheTime = time;
    }

    public void feed(byte[] bytes) {
        mCacheList.addLast(bytes);
        checkMaxCacheList();
    }

    private void checkMaxCacheList() {
        int size = 0;
        for (byte[] bytes : mCacheList) {
            size += bytes.length;
        }
        if (size > mOneshotCacheByteLength) {
            mCacheList.removeFirst();
        }
    }

    // 获取oneshot的音频
    public byte[] getOneshotCacheByte() {
        int size = 0;
        for (byte[] bytes : mCacheList) {
            size += bytes.length;
        }
        byte[] temp = new byte[size];
        size = 0;
        for (byte[] bytes : mCacheList) {
            System.arraycopy(bytes, 0, temp, size, bytes.length);
            size += bytes.length;
        }
        return temp;
    }

    public void clear() {
        mCacheList.clear();
    }

    // 获取vod begin前500ms的音频
    public byte[] getVadCacheByte() {
        return getVadCacheByte(mVadCacheTime);
    }

    // 获取vod begin前500ms的音频
    public byte[] getVadCacheByte(int size) {
        long needGetByteLength = size * mType.getSize() / 100;
        int cacheSize = 0;
        for (byte[] bytes : mCacheList) {
            cacheSize += bytes.length;
        }
        if (cacheSize < needGetByteLength) {
            return getOneshotCacheByte();
        }

        LinkedList<byte[]> needGetList = new LinkedList<>();
        int tmpSize = 0;
        for (int i = mCacheList.size() - 1; i > 0; i--) {
            byte[] bytes = mCacheList.get(i);
            tmpSize += bytes.length;
            needGetList.addFirst(bytes);
            if (tmpSize >= needGetByteLength) {
                break;
            }
        }

        byte[] temp = new byte[tmpSize];
        int tempLength = 0;
        for (int i = 0; i < needGetList.size(); i++) {
            byte[] bytes = needGetList.get(i);
            System.arraycopy(bytes, 0, temp, tempLength, bytes.length);
            tempLength += bytes.length;
        }
        return temp;
    }

    public enum CacheType {
        OPUS(240),
        SBC(570);

        private final int indexSzie;

        CacheType(int size) {
            this.indexSzie = size;
        }

        public int getSize() {
            return indexSzie;
        }
    }

}
