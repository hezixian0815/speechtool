package com.aispeech.common;

import com.aispeech.kernel.MP3;
import com.aispeech.kernel.Opus;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.param.CloudASRParams;

/**
 * pcm 音频压缩为 ogg ogg_opus opus mp3
 */
public class PcmToOgg {

    public interface Callback {
        int run(byte[] data);
    }

    public static final String TAG = "PcmToOgg";
    private Utils speexEngine;
    private Opus opus;
    private MP3 mp3;
    private long engineId;
    private int type = CloudASRParams.OGG;
    private Callback callback;
    /**
     * 是否是编码后的音频，如果是的话就不需要再编码，直接抛出
     */
    private boolean encodedAudio = false;

    private Utils.speex_callback defaultCallback = new Utils.speex_callback() {
        @Override
        public int run(int type, byte[] data, int size) {
            if (callback != null) {
                byte[] byteData = new byte[size];
                System.arraycopy(data, 0, byteData, 0, size);
                callback.run(byteData);
            }
            return 0;
        }
    };

    private Opus.opus_callback opusCallback = new Opus.opus_callback() {
        @Override
        public int enc(long handle, byte[] data, long flag) {
            if (callback != null) {
                byte[] byteData = new byte[data.length];
                System.arraycopy(data, 0, byteData, 0, data.length);
                callback.run(byteData);
            }
            return 0;
        }
    };

    private MP3.mp3_callback mp3_callback = new MP3.mp3_callback() {
        @Override
        public int enc(long handle, byte[] data, long flag) {
            if (callback != null) {
                byte[] byteData = new byte[data.length];
                System.arraycopy(data, 0, byteData, 0, data.length);
                callback.run(byteData);
            }
            return 0;
        }

        @Override
        public int dec(long handle, byte[] data, long flag) {
            return super.dec(handle, data, flag);
        }
    };

    public PcmToOgg() {

    }

    public synchronized void initEncode(int type, boolean encodedAudio, Callback callback) {
        Log.d(TAG, "initEncode type " + type);
        this.callback = callback;
        this.type = type;
        this.encodedAudio = encodedAudio;
        if (encodedAudio)
            return;
        switch (this.type) {
            case CloudASRParams.OGG:
                if (speexEngine == null) {
                    speexEngine = new Utils();
                    engineId = speexEngine.initEncode(defaultCallback);
                    Log.d(TAG, "speex init id:" + engineId);
                } else
                    Log.d(TAG, "speex inited id:" + engineId);
                break;
            case CloudASRParams.OGG_OPUS:
            case CloudASRParams.OPUS:
                if (opus == null) {
                    Log.i(TAG, "Opus.isSoValid " + Opus.isSoValid());
                    opus = new Opus();
                    engineId = opus.init(this.type == CloudASRParams.OGG_OPUS, opusCallback);
                    Log.d(TAG, "opus init id:" + engineId);
                } else
                    Log.d(TAG, "opus inited id:" + engineId);
                break;
            case CloudASRParams.MP3:
                if (mp3 == null) {
                    Log.i(TAG, "MP3.isSoValid " + MP3.isSoValid());
                    mp3 = new MP3();
                    engineId = mp3.init(mp3_callback);
                } else
                    Log.d(TAG, "mp3 inited id:" + engineId);
                break;
        }
    }


    public synchronized void startEncode() {
        Log.d(TAG, "startEncode id:" + engineId);
        if (encodedAudio)
            return;
        switch (this.type) {
            case CloudASRParams.OGG:
                if (speexEngine != null) {
                    speexEngine.startEncode(engineId, 8, 16000, 0, 2);
                }
                break;
            case CloudASRParams.OGG_OPUS:
            case CloudASRParams.OPUS:
                if (opus != null) {
                    opus.start(1, 16000, 32000, 8, 20);
                }
                break;
            case CloudASRParams.MP3:
                if (mp3 != null)
                    mp3.start(1, 16000, 32000, 8, 20);
                break;
        }
    }

    public synchronized void feedData(byte[] pcmBuffer, int size) {
        if (encodedAudio) {
            if (callback != null) {
                byte[] byteData;
                if (pcmBuffer.length != size) {
                    byteData = new byte[size];
                    System.arraycopy(pcmBuffer, 0, byteData, 0, size);
                } else
                    byteData = pcmBuffer;

                callback.run(byteData);
            }
            return;
        }
        // Log.d(TAG, "feedData length " + pcmBuffer.length);
        switch (this.type) {
            case CloudASRParams.OGG:
                if (speexEngine != null) {
                    speexEngine.feedEncode(engineId, pcmBuffer, size);
                }
                break;
            case CloudASRParams.OGG_OPUS:
            case CloudASRParams.OPUS:
                if (opus != null) {
                    byte[] byteData;
                    if (pcmBuffer.length != size) {
                        byteData = new byte[size];
                        System.arraycopy(pcmBuffer, 0, byteData, 0, size);
                    } else
                        byteData = pcmBuffer;
                    // Log.d(TAG, "feedData length " + byteData.length);
                    opus.feed(byteData);
                }
                break;
            case CloudASRParams.MP3:
                if (mp3 != null) {
                    byte[] byteData;
                    if (pcmBuffer.length != size) {
                        byteData = new byte[size];
                        System.arraycopy(pcmBuffer, 0, byteData, 0, size);
                    } else
                        byteData = pcmBuffer;
                    // Log.d(TAG, "feedData length " + byteData.length);
                    mp3.feed(byteData);
                }
                break;
            case CloudASRParams.WAV:
                if (callback != null) {
                    byte[] byteData;
                    if (pcmBuffer.length != size) {
                        byteData = new byte[size];
                        System.arraycopy(pcmBuffer, 0, byteData, 0, size);
                    } else
                        byteData = pcmBuffer;

                    callback.run(byteData);
                }
                break;
        }
    }

    public synchronized void stopEncode() {
        Log.d(TAG, "encode stop before");
        if (encodedAudio)
            return;
        switch (this.type) {
            case CloudASRParams.OGG:
                if (speexEngine != null) {
                    speexEngine.stopEncode(engineId);
                }
                break;
            case CloudASRParams.OGG_OPUS:
                if (opus != null) {
                    opus.stop();
                }
                break;
            case CloudASRParams.OPUS:
                //直接调用stop会内存泄露
                if (opus != null) {
                    opus.destroy();
                    opus = null;
                }
                break;
            case CloudASRParams.MP3:
                if (mp3 != null) {
                    mp3.stop();
                }
                break;
        }
        Log.d(TAG, "encode stop after");
    }

    public synchronized void destroyEncode() {
        Log.d(TAG, "destroyEncode id " + engineId);
        if (encodedAudio)
            return;
        if (speexEngine != null) {
            speexEngine.destroyEncode(engineId);
            speexEngine = null;
        }
        if (opus != null) {
            opus.destroy();
            opus = null;
        }
        if (mp3 != null) {
            mp3.destroy();
            mp3 = null;
        }
        engineId = 0;
        Log.d(TAG, "destroyEncode after");
    }

}
