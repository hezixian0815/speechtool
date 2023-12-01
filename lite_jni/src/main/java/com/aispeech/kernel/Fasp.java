package com.aispeech.kernel;

import com.aispeech.common.Log;

public class Fasp {
    private static final String TAG = "Fasp";
    private static boolean loadSoSuc;

    static {
        try {
            Log.d(TAG, "before load fasp library");
            System.loadLibrary("fasp");
            Log.d(TAG, "after load fasp library");
            loadSoSuc = true;
        } catch (UnsatisfiedLinkError e) {
            loadSoSuc = false;
            e.printStackTrace();
            Log.e(Log.ERROR_TAG, "Please check useful libfasp.so, and put it in your libs dir!");
        }
    }

    private long enginId;

    public static boolean isLoadSoSuc() {
        return loadSoSuc;
    }

    public static native long dds_fasp_new(String cfg);

    public static native int dds_fasp_start(long id, String param);

    public static native int dds_fasp_feed(long id, byte[] data, int size);

    public static native int dds_fasp_set(long id, String setParam);

    public static native int dds_fasp_get(long id, String param);

    public static native int dds_fasp_stop(long id);

    public static native int dds_fasp_delete(long id);

    public static native int dds_fasp_setdatachs1cb(long id, data_chs1_callback callback);

    public static native int dds_fasp_setdatachs2cb(long id, data_chs2_callback callback);

    public long init(String cfg) {
        enginId = dds_fasp_new(cfg);
        Log.d(TAG, "init enginId " + enginId + " " + cfg);
        return enginId;
    }

    public int start() {
        int ret = dds_fasp_start(enginId, "");
        Log.d(TAG, "start ret " + ret);
        return ret;
    }

    public int set(String setParam) {
        int ret = dds_fasp_set(enginId, setParam);
        Log.d(TAG, "set ret " + ret + " " + setParam);
        return ret;
    }

    public int get(String getParam) {
        int ret = dds_fasp_get(enginId, getParam);
        Log.d(TAG, "get ret " + ret + " " + getParam);
        return ret;
    }

    public int feed(byte[] data, int size) {
        return dds_fasp_feed(enginId, data, size);
    }

    public int stop() {
        int ret = dds_fasp_stop(enginId);
        Log.d(TAG, "stop ret " + ret);
        return ret;
    }

    public int destroy() {
        int ret = dds_fasp_delete(enginId);
        Log.d(TAG, "destroy ret " + ret);
        return ret;
    }

    public boolean setCallback(data_chs1_callback callback1, data_chs2_callback callback2) {
        int ret = dds_fasp_setdatachs1cb(enginId, callback1);
        Log.d(TAG, "setCallback1 ret " + ret);
        if (ret != 0)
            return false;
        ret = dds_fasp_setdatachs2cb(enginId, callback2);
        Log.d(TAG, "setCallback2 ret " + ret);
        return ret == 0;
    }


    enum duilite_callback_type {
        DUILITE_CALLBACK_FESPA_WAKEUP(0),
        DUILITE_CALLBACK_FESPA_DOA(1),
        DUILITE_CALLBACK_FESPA_BEAMFORMING(2),
        DUILITE_CALLBACK_FESPA_VPRINTCUT(3),
        DUILITE_CALLBACK_FESPL_WAKEUP(4),
        DUILITE_CALLBACK_FESPL_DOA(5),
        DUILITE_CALLBACK_FESPL_BEAMFORMING(6),
        DUILITE_CALLBACK_FESPL_VPRINTCUT(7),
        DUILITE_CALLBACK_MR_BEAMFORMING(8),
        DUILITE_CALLBACK_MR_POST(9),
        DUILITE_CALLBACK_MR_DOA(10),
        DUILITE_CALLBACK_FESPCAR_WAKEUP(11),
        DUILITE_CALLBACK_FESPCAR_BEAMFORMING(12),
        DUILITE_CALLBACK_FESPCAR_DOA(13),
        DUILITE_CALLBACK_FESPCAR_VPRINTCUT(14),
        DUILITE_CALLBACK_FESPD_WAKEUP(15),
        DUILITE_CALLBACK_FESPD_DOA(16),
        DUILITE_CALLBACK_FESPD_BEAMFORMING(17),
        DUILITE_CALLBACK_FESPD_VPRINTCUT(18),
        DUILITE_CALLBACK_WAKEUP_VPRINTCUT(19),
        DUILITE_CALLBACK_AECAGC_AECPOP(20),
        DUILITE_CALLBACK_AECAGC_VOIP(21),
        DUILITE_CALLBACK_FASP_DATA_CHS1(22),
        DUILITE_CALLBACK_FASP_DATA_CHS2(23);
        private int value;

        duilite_callback_type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static class data_chs1_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

    public static class data_chs2_callback {
        public static byte[] bufferData = new byte[3200];

        public static byte[] getBufferData() {
            return bufferData;
        }

        public int run(int type, byte[] data, int size) {
            return 0;
        }
    }

}
