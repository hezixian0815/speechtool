package com.aispeech.kernel;

import com.aispeech.common.Log;

import org.json.JSONObject;

public class MP3 extends Abs {

    public static final int MP3_TO_PCM = 0;
    public static final int PCM_TO_MP3 = 1;
    protected static final String TAG_MP3 = "MP3";
    private static boolean loadSoOk;

    static {
        try {
            Log.d(TAG_MP3, "before load mp3 library");
            System.loadLibrary("mp3");
            Log.d(TAG_MP3, "after load mp3 library");
            loadSoOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadSoOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libmp3.so, and put it in your libs dir!");

        }
    }

    private long engineId;
    private int mType = MP3_TO_PCM;

    public static boolean isSoValid() {
        return loadSoOk;
    }

    public static native long mp3_encode_new(mp3_callback callback);

    public static native int mp3_encode_start(long handle, String param);

    public static native int mp3_encode_feed(long handle, byte[] data);

    public static native int mp3_encode_stop(long handle);

    public static native int mp3_encode_delete(long handle);

    public static native long mp3_decode_new(mp3_callback callback);

    public static native int mp3_decode_start(long handle, String param);

    public static native int mp3_decode_feed(long handle, byte[] data);

    public static native int mp3_decode_stop(long handle);

    public static native int mp3_decode_delete(long handle);

    public static native long mp3_encode_new2(String param);

    public static native int mp3_encode_enc2(long handle, byte[] input, byte[] output);

    public static native int mp3_encode_del2(long handle);

    // -------------------第2组同步接口-------------------

    public static native long mp3_decode_new2(String param);

    public static native int mp3_decode_dec2(long handle, byte[] input, byte[] output);

    public static native int mp3_decode_del2(long handle);

    /**
     * 初始化实例，在调用start, stop, cancel等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException}
     *
     * @param callback callback
     * @return engineId
     */
    public long init(mp3_callback callback) {
        engineId = mp3_encode_new(callback);
        Log.d(TAG_MP3, "init:" + engineId);
        return engineId;
    }

    /**
     * 开始请求，请求过程中会以 callback 回调的形式响应事件
     * <pre>
     * {
     * 	"samplerate":16000,
     * 	"channels":1,
     * 	"bitrate":32000,
     * 	"quality":2
     * }
     * </pre>
     *
     * @param channels   Number of channels (1 or 2) in input signal
     * @param samplerate Sampling rate of input signal (Hz) This must be one of 8000, 12000, 16000, 24000, or 48000.
     * @param bitrate    bitrate is in bits per second (b/s)
     * @param quality    quality is a value from 1 to 10, where 1 is the lowest quality and 10 is the highest
     * @return
     */
    public int start(int channels, int samplerate, int bitrate, int quality, int framesize) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channels", channels);
            jsonObject.put("samplerate", samplerate);
            jsonObject.put("bitrate", bitrate);
            jsonObject.put("quality", quality);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.d(TAG_MP3, "start:" + engineId);
        int ret = mp3_encode_start(engineId, jsonObject.toString());
        if (ret < 0) {
            Log.e(TAG_MP3, "start failed! Error code: " + ret);
            return -1;
        }
        Log.d(TAG_MP3, "start ret:" + ret);
        return ret;
    }

    /**
     * 向内核提交数据
     *
     * @param buffer 提交的数据
     * @return 0 操作成功, others 操作失败
     */
    public int feed(byte[] buffer) {
        return mp3_encode_feed(engineId, buffer);
    }

    /**
     * 停止向服务器提交数据，请求结果
     *
     * @return 0 操作成功, others 操作失败
     */
    public int stop() {
        Log.d(TAG_MP3, "stop:" + engineId);
        return mp3_encode_stop(engineId);
    }

    /**
     * 释放资源
     *
     * @return 0 操作成功, others 操作失败
     */
    public void destroy() {
        Log.d(TAG_MP3, "destroy:" + engineId);
        mp3_encode_delete(engineId);
        Log.d(TAG_MP3, "destroy finished:" + engineId);
        engineId = 0;
    }

    /**
     * 开始请求，请求过程中会以 callback 回调的形式响应事件
     * {
     * "samplerate":16000,
     * "channels":1,
     * "bitrate":32000,
     * "framesize":20,
     * "quality":8
     * }
     *
     * @param type   MP3_TO_PCM/PCM_TO_MP3
     * @param params 初始化参数
     * @return
     */
    public long ddsInit(int type, String params) {
        if (!isSoValid()) {
            Log.e(TAG_MP3, "load libmp3 library error! ddsInit -> return!");
            return 0;
        }

        mType = type;
        switch (mType) {
            case MP3_TO_PCM:
                Log.d(TAG_MP3, "ddsInit mp32pcm...");
                mEngineId = mp3_decode_new2(params);
                break;
            case PCM_TO_MP3:
                Log.d(TAG_MP3, "ddsInit pcm2mp3...");
                mEngineId = mp3_encode_new2(params);
                break;
            default:
                break;
        }
        Log.d(TAG_MP3, "ddsInit result = " + mEngineId);
        return mEngineId;
    }

    /**
     * 向内核提交数据
     */
    public int ddsFeed(byte[] input, int size, byte[] output) {
        if (!checkCore("ddsFeed")) {
            return -1;
        }

        int ret = 0;

        switch (mType) {
            case MP3_TO_PCM:
                ret = mp3_decode_dec2(mEngineId, input, output);
                break;
            case PCM_TO_MP3:
                ret = mp3_encode_enc2(mEngineId, input, output);
                break;
            default:
                break;
        }

        return ret;
    }

    /**
     * 释放资源
     */
    public void ddsDestroy() {
        if (!checkCore("ddsDestroy")) {
            return;
        }

        Log.d(TAG_MP3, "ddsDestroy..., type = " + mType);

        switch (mType) {
            case MP3_TO_PCM:
                mp3_decode_del2(mEngineId);
                break;
            case PCM_TO_MP3:
                mp3_encode_del2(mEngineId);
                break;
            default:
                break;
        }
    }

    public static class mp3_callback {
        public int enc(long handle, byte[] data, long flag) {
            return 0;
        }

        public int dec(long handle, byte[] data, long flag) {
            return 0;
        }
    }

}
