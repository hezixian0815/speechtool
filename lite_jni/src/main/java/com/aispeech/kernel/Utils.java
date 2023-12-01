package com.aispeech.kernel;

import com.aispeech.common.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

/**
 * Created by yu on 2018/5/4.
 */

public class Utils {
    private static final String TAG = "Utils";
    private static boolean loadUtilsOk = false;

    static {
        try {
            Log.d(TAG, "before load duiutils library");
            System.loadLibrary("duiutils");
            Log.d(TAG, "after load duiutils library");
            loadUtilsOk = true;
        } catch (UnsatisfiedLinkError e) {
            loadUtilsOk = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libduiutils.so, and put it in your libs dir!");

        }
    }

    private long engineId;

    public static boolean isUtilsSoValid() {
        return loadUtilsOk;
    }

    /**
     * 获取请求的唯一recorderID，采用UUID方案，不再使用get_recordid()通过时间戳+进程ID+设备名+计数器的方式做唯一标识，容易产生数据的重复
     *
     * @return 唯一标识
     */
    public static String getRecorderId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 计算唯一标识符，采用进程ID+时间戳+计数器+设备名的方案去做唯一标识符的生成
     * @deprecated 废弃，出现不同设备的标识符重复的问题
     * @return 唯一标识
     */
    @Deprecated
    public static native String get_recordid();

    public static native String get_version();

    /*
     * speex_encode
     * */
    public static native long speex_encode_new(speex_callback callback);

    public static native int speex_encode_start(long speex, String param);

    public static native int speex_encode_feed(long speex, byte[] data, int size);

    public static native int speex_encode_stop(long speex);

    public static native int speex_encode_delete(long speex);

    public static native int jni_duilite_set_thread_affinity(int speex);

    public static native int jni_duilite_set_thread_multi_affinity(int speex);

    /**
     * 返回系统生成的当前的speex_encod对应的id
     *
     * @return ret
     */
    public long getSpeexEncodeId() {
        return engineId;
    }

    /**
     * 初始化speex编码器实例，在调用start, feed, stop, delete等操作前必须调用此方法初始化，否则将抛出
     * {@link RuntimeException} 线程安全
     *
     * @param callback 　编码之后返回的ogg音频流
     * @return speexEncodeId
     */
    public long initEncode(speex_callback callback) {
        engineId = speex_encode_new(callback);
        Log.d(TAG, "speex_encode_new():" + engineId);
        return engineId;
    }

    /**
     * @param speexId    new返回的speexEncode实例id
     * @param quality    取值范围:1～10
     * @param sampleRate 音频采样率:支持8k和16K
     * @param vbr        每个ogg包中包含的帧数　1,2,3,4,......
     * @param complexity 　复杂度 2,3,4,5
     * @return retId：成功；null: 失败；
     */
    public int startEncode(long speexId, int quality, int sampleRate, int vbr, int complexity) {
        int ret = 0;
        Log.d(TAG, "speex_encode_start():" + speexId);
        Log.d(TAG, "params: " + speexId + " " + quality + " " + sampleRate + " " + vbr + " " + complexity);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("quality", quality);
            jsonObject.put("sampleRate", sampleRate);
            jsonObject.put("vbr", vbr);
            jsonObject.put("complexity", complexity);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ret = speex_encode_start(speexId, jsonObject.toString());
        Log.d(TAG, "speex encode start end" + ret);
        if (ret < 0) {
            Log.e(TAG, "speex_encode_start() failed! Error code: " + ret);
            return -1;
        }
        return ret;
    }

    /**
     * @param speexId 　编码器id
     * @param data    传入的pcm音频字节流
     * @param size    　音频流的大小
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int feedEncode(long speexId, byte[] data, int size) {
        return speex_encode_feed(speexId, data, data.length);
    }

    /**
     * 　关闭音频编码器
     *
     * @param speexId 　编码器id
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int stopEncode(long speexId) {
        Log.d(TAG, "speex_encode_stop():" + speexId);
        return speex_encode_stop(speexId);
    }

    /**
     * 　释放音频编码器
     *
     * @param speexId 　编码器id
     * @return 0:操作成功；
     * -1 :操作失败；
     */
    public int destroyEncode(long speexId) {
        Log.d(TAG, "speex_encode_cancel():" + speexId);
        return speex_encode_delete(speexId);
    }

    public static class speex_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

}
