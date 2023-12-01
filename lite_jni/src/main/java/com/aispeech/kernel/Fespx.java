package com.aispeech.kernel;

/**
 * Created by wuwei on 18-7-9.
 */

public abstract class Fespx {

    public static boolean isFespxSoValid() {
        return false;
    }

    public long initFespx(String cfg) {
        return 0;
    }

    public int startFespx() {
        return 0;
    }

    public int setFespx(String setParam) {
        return 0;
    }

    public int feedFespx(byte[] data, int size) {
        return 0;
    }

    public int stopFespx() {
        return 0;
    }

    public int destroyFespx() {
        return 0;
    }

    public abstract int getFespx(String param);

    public abstract int setFespxWakeupcb(Sspe.wakeup_callback callback);

    public abstract int setFespxDoacb(Sspe.doa_callback callback);

    public abstract int setFespxBeamformingcb(Sspe.beamforming_callback callback);

    public abstract int setFespxVprintCutcb(Sspe.vprintcut_callback callback);

    public abstract int setFespxInputcb(Sspe.input_callback callback);

    public abstract int setFespxOutputcb(Sspe.output_callback callback);

    public abstract int setFespxEchocb(Sspe.echo_callback callback);

    public abstract int setFespxSevcNoise(Sspe.sevc_noise_callback callback);

    public abstract int setFespxSevcDoa(Sspe.sevc_doa_callback callback);

    public abstract int setMultBfcb(Sspe.multibf_callback callback);

    /**
     * 获取资源内的唤醒配置信息，从 callback 回调。此方法可以不需要引擎初始化就可以调用
     *
     * @param initConfig 和 {@link #initFespx} 时传入的 cfg，用到cfg里的资源文件信息
     * @param callback   回调唤醒配置信息的接口
     */
    public int getWakeupConfig(String initConfig, Sspe.config_callback callback) {
        return 0;
    }

    /**
     * 唤醒回调
     */
    public interface wakeup_callback {
        int run(int type, byte[] data, int size);
    }

    /**
     * doa回调
     */
    public interface doa_callback {
        int run(int type, byte[] data, int size);
    }

    /**
     * bf音频回调接口
     */
    public static class beamforming_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    /**
     * vp_cut音频回调接口
     */
    public static class vprintcut_callback {
        public static byte[] bufferData = new byte[3200];
        public static volatile byte[] bufferData2 = null;
        public volatile byte[] bufferData_V2 = null;

        public static byte[] getBufferData() {
            return bufferData;
        }

        public static byte[] getBufferData(int length) {
            if (length < 0)
                length = 0;
            if (bufferData2 == null || bufferData2.length != length)
                synchronized (vprintcut_callback.class) {
                    if (bufferData2 == null || bufferData2.length != length)
                        bufferData2 = new byte[length];
                }
            return bufferData2;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }

        public byte[] getBufferData_v2(int length) {
            if (length < 0)
                length = 0;
            if (bufferData_V2 == null || bufferData_V2.length != length)
                synchronized (this) {
                    if (bufferData_V2 == null || bufferData_V2.length != length)
                        bufferData_V2 = new byte[length];
                }
            return bufferData_V2;
        }
    }
}
