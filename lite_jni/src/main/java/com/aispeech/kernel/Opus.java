package com.aispeech.kernel;

import com.aispeech.common.Log;

import org.json.JSONObject;

public class Opus extends Abs {

    public static final int OPUS_TO_PCM = 0;
    public static final int PCM_TO_OPUS = 1;
    private static boolean loadSoOk;

    static {
        try {
            Log.d(TAG, "before load opusogg library");
            System.loadLibrary("opusogg");
            Log.d(TAG, "after load opusogg library");
            loadSoOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadSoOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libopusogg.so, and put it in your libs dir!");

        }
    }

    private long engineId;
    private int mType = OPUS_TO_PCM;

    public static boolean isSoValid() {
        return loadSoOk;
    }

    public static native long opus_encode_new(int type, opus_callback callback);

    public static native int opus_encode_start(long handle, String param);

    public static native int opus_encode_feed(long handle, byte[] data);

    public static native int opus_encode_stop(long handle);

    public static native int opus_encode_delete(long handle);

    public static native long opus_decode_new(int type, opus_callback callback);

    public static native int opus_decode_start(long handle, String param);

    public static native int opus_decode_feed(long handle, byte[] data);

    public static native int opus_decode_stop(long handle);

    public static native int opus_decode_delete(long handle);

    public static native long opus_encode_new2(int type, String param);

    public static native int opus_encode_enc2(long speex, byte[] input, byte[] output);

    public static native int opus_encode_del2(long speex);

    // -------------------第2组同步接口-------------------

    public static native long opus_decode_new2(int type, String param);

    public static native int opus_decode_dec2(long speex, byte[] input, byte[] output);

    public static native int opus_decode_del2(long speex);

    public static native int opus_upgrade(String srcPath, String destPath);

    /**
     * 初始化实例，在调用start, stop, cancel等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException}
     *
     * @param standard 是否标准，true 是 ogg_opus，false 非标准 思必驰内部使用的 opus
     * @param callback callback
     * @return engineId
     */
    public long init(boolean standard, opus_callback callback) {
        engineId = opus_encode_new(standard ? 0 : 1, callback);
        Log.d(TAG, "init:" + engineId);
        return engineId;
    }

    /**
     * 开始请求，请求过程中会以 callback 回调的形式响应事件
     * <pre>
     * {
     * 	"samplerate":16000,
     * 	"channels":1,
     * 	"bitrate":32000,
     * 	"framesize":20,
     * 	"complexity":8
     * }
     * </pre>
     *
     * @param channels   Number of channels (1 or 2) in input signal
     * @param samplerate Sampling rate of input signal (Hz) This must be one of 8000, 12000, 16000, 24000, or 48000.
     * @param bitrate    bitrate is in bits per second (b/s)
     * @param complexity complexity is a value from 1 to 10, where 1 is the lowest complexity and 10 is the highest
     * @param framesize  one frame (2.5, 5, 10, 20, 40 or 60 ms) of audio data
     * @return
     */
    public int start(int channels, int samplerate, int bitrate, int complexity, int framesize) {

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("channels", channels);
            jsonObject.put("samplerate", samplerate);
            jsonObject.put("bitrate", bitrate);
            jsonObject.put("complexity", complexity);
            jsonObject.put("framesize", framesize);
        } catch (Exception e) {
            e.printStackTrace();
        }

        int ret = 0;
        Log.d(TAG, "start:" + engineId);
        ret = opus_encode_start(engineId, jsonObject.toString());
        if (ret < 0) {
            Log.e(TAG, "start failed! Error code: " + ret);
            return -1;
        }
        Log.d(TAG, "start ret:" + ret);
        return ret;
    }

    /**
     * 向内核提交数据
     *
     * @param buffer 提交的数据
     * @return 0 操作成功, others 操作失败
     */
    public int feed(byte[] buffer) {
        int opt = opus_encode_feed(engineId, buffer);
        return opt;
    }

    /**
     * 停止向服务器提交数据，请求结果
     *
     * @return 0 操作成功, others 操作失败
     */
    public int stop() {
        Log.d(TAG, "stop:" + engineId);
        return opus_encode_stop(engineId);
    }

    /**
     * 释放资源
     *
     * @return 0 操作成功, others 操作失败
     */
    public void destroy() {
        Log.d(TAG, "destroy:" + engineId);
        opus_encode_delete(engineId);
        Log.d(TAG, "destroy finished:" + engineId);
        engineId = 0;
    }

    /**
     * 开始请求，请求过程中会以 callback 回调的形式响应事件
     * {
     * "samplerate":16000,
     * "channels":1,
     * "bitrate":32000,
     * "framesize":20,
     * "complexity":8
     * }
     *
     * @return
     */
    public long ddsInit(int type, String params) {
        return ddsInit(false, type, params);
    }

    /**
     * @param norm   是否是标准编解码
     * @param type   OPUS_TO_PCM/PCM_TO_OPUS
     * @param params 初始化参数
     * @return
     */
    public long ddsInit(boolean norm, int type, String params) {
        if (!isSoValid()) {
            android.util.Log.e(TAG, "load libopusogg library error! ddsInit -> return!");
            return 0;
        }
        int normInt = norm ? 0 : 1;// 0: 标准  1: 非标准
        mType = type;
        switch (mType) {
            case OPUS_TO_PCM:
                android.util.Log.d(TAG, "ddsInit opus2pcm..." + normInt);
                mEngineId = opus_decode_new2(normInt, params);
                break;
            case PCM_TO_OPUS:
                android.util.Log.d(TAG, "ddsInit pcm2opus..." + normInt);
                mEngineId = opus_encode_new2(normInt, params);
                break;
            default:
                break;
        }
        android.util.Log.d(TAG, "ddsInit result = " + mEngineId);
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
            case OPUS_TO_PCM:
                ret = opus_decode_dec2(mEngineId, input, output);
                break;
            case PCM_TO_OPUS:
                ret = opus_encode_enc2(mEngineId, input, output);
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

        android.util.Log.d(TAG, "ddsDestroy..., type = " + mType);

        switch (mType) {
            case OPUS_TO_PCM:
                opus_decode_del2(mEngineId);
                break;
            case PCM_TO_OPUS:
                opus_encode_del2(mEngineId);
                break;
            default:
                break;
        }
    }

    public static class opus_callback {
        public int enc(long handle, byte[] data, long flag) {
            return 0;
        }

        public int dec(long handle, byte[] data, long flag) {
            return 0;
        }
    }


}
