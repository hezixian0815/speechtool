package com.aispeech.lite.message;

/**
 * Created by yuruilong on 2017/5/11.
 */

public class Message {
    public static final int MSG_NEW = 1;
    public static final int MSG_START = 2;
    public static final int MSG_STOP = 3;
    public static final int MSG_CANCEL = 4;
    public static final int MSG_PAUSE = 5;
    public static final int MSG_RESUME = 6;
    public static final int MSG_RELEASE = 7;
    public static final int MSG_ERROR = 8;
    public static final int MSG_FEED_DATA_BY_STREAM = 9;
    public static final int MSG_COMPLETED = 10;
    public static final int MSG_SETUP_VOLUME = 11;
    public static final int MSG_AIENGINE_RESULT = 12;
    public static final int MSG_WAKEUP_RESULT = 13;
    public static final int MSG_BEAMFORMING_DATA = 14;
    public static final int MSG_DOA_RESULT = 15;
    public static final int MSG_FDM_PARAM_SET = 17;
    public static final int MSG_FDM_PARAM_GET = 18;
    public static final int MSG_SET = 19;
    public static final int MSG_EVENT = 20;
    public static final int MSG_UPDATE = 21;
    public static final int MSG_UPDATE_VOCAB = 22;
    public static final int MSG_GET = 23;
    public static final int MSG_FORCE_REQUEST_WAKEUP_RESULT = 24;
    public static final int MSG_VAD_RESULT_DATA = 25;
    public static final int MSG_INPUT_DATA = 26;
    public static final int MSG_OUTPUT_DATA = 27;
    public static final int MSG_ECHO_DATA = 28;
    public static final int MSG_FEEDBACK = 29;
    public static final int MSG_CLOSE = 30;
    public static final int MSG_TRIGGER_INTENT = 31;
    public static final int MSG_ASYNC = 32;
    public static final int MSG_FEEDBACK_2_PRIV_CLOUD = 33;
    public static final int MSG_DECODER = 34;
    public static final int MSG_UPDATE_NAVI_VOCAB = 35;
    public static final int MSG_PULL_CACHE = 36;
    public static final int MSG_WAKEUP = 37;
    public static final int MSG_FEED_BF_VAD_DATA_BY_STREAM = 38;
    public static final int MSG_TICK = 39;


    public int mId;
    public Object mObject;

    public Message(int id) {
        this.mId = id;
    }

    public Message(int id, Object object) {
        this.mId = id;
        this.mObject = object;
    }
}
