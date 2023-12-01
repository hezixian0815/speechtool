package com.aispeech.export.intent;

import android.text.TextUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @deprecated 废弃，使用{@link com.aispeech.lite.vprint.VprintIntent }替代
 */
@Deprecated
public class VprintIntent {
    private int bfChannelNum;
    private int aecChannelNum;
    private int outChannelNum;
    private Action action;
    private String[] vprintWord;
    private String userId;
    private int trainNum = 4;
    private float thresh = Float.MAX_VALUE;
    private float snrThresh = 8.67f;
    private String saveAudioPath;
    private String vprintCutSaveDir;
    private int sensitivityLevel = 0;


    public enum Action {

        /**
         * 注册模式
         */
        REGISTER("register"),

        /**
         * 更新模式
         */
        UPDATE("update"),

        /**
         * 追加模式
         */
        APPEND("append"),

        /**
         * 签到模式
         */
        TEST("test"),

        /**
         * 删除模式(删除模型中某条记录)
         */
        UNREGISTER("unregister"),

        /**
         * 删除模式(删除模型中所有记录)
         */
        UNREGISTER_ALL("unregisterall");

        private String value;

        private static Map<String,Action> map;

        Action(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }

        public static Action getActionByValue(String value) {
            if(map == null){
                map = new HashMap<>();
                for(Action action: Action.values()){
                    map.put(action.getValue(), action);
                }
            }
            return map.get(value);
        }
    }

    private VprintIntent(Builder builder) throws IllegalArgumentException {
        this.bfChannelNum = builder.mBbfChannelNum;
        this.aecChannelNum = builder.mAecChannelNum;
        this.outChannelNum = builder.mOutChannelNum;
        this.action = builder.mAction;
        this.vprintWord = builder.mVprintWord;
        this.sensitivityLevel = builder.sensitivityLevel;
        this.userId = builder.mUserId;
        this.trainNum = builder.mTrainNum;
        this.snrThresh = builder.mSnrThresh;
        this.thresh = builder.mThresh;
        this.saveAudioPath = builder.mSaveAudioPath;
        this.vprintCutSaveDir = builder.mVprintCutSaveDir;
        //check argument valid
        if (action == null) {
            throw new IllegalArgumentException("Vprint intent is invalid, lost action");
        } else if (action != Action.UNREGISTER_ALL) {//非全部删除模式
            if (action != Action.TEST && (vprintWord == null || vprintWord.length == 0)) {
                //非test模式，需要vprintWord
                throw new IllegalArgumentException("Vprint intent is invalid, lost vpirntWord");
            }
            if (action != Action.TEST && TextUtils.isEmpty(userId)) {
                //非test模式，需要userId
                throw new IllegalArgumentException("Vprint intent is invalid, lost userId");
            }
//            if (action != Action.UNREGISTER && channelNum == 0) {
//                //需要音频的模式下，需要channelNum
//                throw new IllegalArgumentException("Vprint intent is invalid, lost channelNum");
//            }
        }
    }

    public int getBfChannelNum() {
        return bfChannelNum;
    }

    public int getAecChannelNum() {
        return aecChannelNum;
    }

    public int getOutChannelNum() {
        return outChannelNum;
    }

    public Action getAction() {
        return action;
    }

    public String[] getVprintWord() {
        return vprintWord;
    }

    public int getSensitivityLevel() {
        return sensitivityLevel;
    }

    public String getUserId() {
        return userId;
    }

    public int getTrainNum() {
        return trainNum;
    }

    public float getSnrThresh() {
        return snrThresh;
    }

    public float getThresh() {
        return thresh;
    }

    public String getSaveAudioPath() {
        return saveAudioPath;
    }

    public String getVprintCutSaveDir() {
        return vprintCutSaveDir;
    }

    public void setBfChannelNum(int bfChannelNum) {
        this.bfChannelNum = bfChannelNum;
    }

    public void setAecChannelNum(int aecChannelNum) {
        this.aecChannelNum = aecChannelNum;
    }

    public void setOutChannelNum(int outChannelNum) {
        this.outChannelNum = outChannelNum;
    }

    @Override
    public String toString() {
        return "VprintIntent{" +
                "bfChannelNum=" + bfChannelNum +
                ", aecChannelNum=" + aecChannelNum +
                ", outChannelNum=" + outChannelNum +
                ", action=" + action +
                ", vprintWord=" + Arrays.toString(vprintWord) +
                ", userId='" + userId + '\'' +
                ", trainNum=" + trainNum +
                ", thresh=" + thresh +
                ", snrThresh=" + snrThresh +
                ", saveAudioPath='" + saveAudioPath + '\'' +
                ", vprintCutSaveDir='" + vprintCutSaveDir + '\'' +
                ", sensitivityLevel=" + sensitivityLevel +
                '}';
    }

    public static class Builder {
        private int mBbfChannelNum;
        private int mAecChannelNum;
        private int mOutChannelNum;
        private Action mAction;
        private String[] mVprintWord;
        private String mUserId;
        private int mTrainNum;
        private float mThresh = Float.MAX_VALUE;
        private float mSnrThresh;
        private String mSaveAudioPath;
        private String mVprintCutSaveDir;
        private int sensitivityLevel = 0;

        /**
         * 多麦模式下设置增强后音频通道数，AIFespCarEngine#getValueOf(String) 获取
         *
         * @param bfChannelNum 增强后音频通道数
         * @return {@link Builder}
         */
        public Builder setBfChannelNum(int bfChannelNum) {
            this.mBbfChannelNum = bfChannelNum;
            return this;
        }

        /**
         * 多麦模式下设置回声消除后音频通道数，从AIFespCarEngine#getValueOf(String) 获取
         *
         * @param aecChannelNum 回声消除后音频通道数
         * @return {@link Builder}
         */
        public Builder setAecChannelNum(int aecChannelNum) {
            this.mAecChannelNum = aecChannelNum;
            return this;
        }

        /**
         * 设置声纹音频输入通道数，表示使用几个通道作为声纹输入，通常为1或回声消除后音频通道数,
         * 单麦不需要设置,只在多麦模式下才需要设置，视项目而定，<strong>思必驰</strong>会给出具体值
         *
         * @param outChannelNum 声纹音频输入通道数
         * @return {@link Builder}
         */
        public Builder setOutChannelNum(int outChannelNum) {
            this.mOutChannelNum = outChannelNum;
            return this;
        }

        /**
         * 设置声纹工作模式,
         * 若不设置，会抛{@link IllegalArgumentException}异常
         *
         * @param action {@link Action}
         * @return {@link Builder}
         */
        public Builder setAction(Action action) {
            this.mAction = action;
            return this;
        }

        /**
         * 设置当前响应的唤醒词。每次只能设置一个，若需要切换声纹响应的唤醒词，则需要重新start设置
         * 只在{@link Action#REGISTER},{@link Action#UPDATE},
         * {@link Action#APPEND}或{@link Action#UNREGISTER}模式才需要设置
         * 若不设置，会抛{@link IllegalArgumentException}异常
         *
         * @param vprintWord 声纹响应的唤醒词
         * @return {@link Builder}
         */
        public Builder setVprintWord(String vprintWord) {
            if (!TextUtils.isEmpty(vprintWord))
                this.mVprintWord = new String[]{vprintWord};
            else
                this.mVprintWord = null;
            return this;
        }

        public Builder setVprintWord(String... vprintWords) {
            if (vprintWords != null && vprintWords.length > 0)
                this.mVprintWord = vprintWords;
            else
                this.mVprintWord = null;
            return this;
        }

        /**
         * 敏感度设置，取值范围是 0-totalSensitivityLevel 。默认为0，对应cfg中的第一组阈值。
         * <p>
         * totalSensitivityLevel 会在声纹初始化时通过回调接口吐出来
         * </p>
         *
         * @param sensitivityLevel 敏感度设置
         * @return {@link Builder}
         */
        public Builder setSensitivityLevel(int sensitivityLevel) {
            this.sensitivityLevel = sensitivityLevel;
            return this;
        }

        /**
         * 设置用户名id
         * 只在{@link Action#REGISTER},{@link Action#UPDATE},
         * {@link Action#APPEND}或{@link Action#UNREGISTER}模式才需要设置
         * 若不设置，会抛{@link IllegalArgumentException}异常
         *
         * @param userId 用户名id
         * @return {@link Builder}
         */
        public Builder setUserId(String userId) {
            this.mUserId = userId;
            return this;
        }

        /**
         * 设置训练次数，只在{@link Action#REGISTER}和{@link Action#UPDATE}模式才需要设置
         * 若不设置，会抛{@link IllegalArgumentException}异常
         *
         * @param trainNum 训练次数
         * @return {@link Builder}
         */
        public Builder setTrainNum(int trainNum) {
            this.mTrainNum = trainNum;
            return this;
        }

        /**
         * 设置注册时最小信噪比阈值，数值越大，环境静音要求越高 <br>
         * 默认8.67,注册时若音频的snr值低于设置的阈值，则会抛{@link com.aispeech.AIError#ERR_DESCRIPTION_SNR_LOW}
         *
         * @param snrThresh 最小信噪比阈值
         * @return {@link Builder}
         */
        public Builder setSnrThresh(float snrThresh) {
            this.mSnrThresh = snrThresh;
            return this;
        }

        /**
         * 默认不用设置
         *
         * @param thresh 声纹阈值
         * @return {@link Builder}
         */
        public Builder setThresh(float thresh) {
            this.mThresh = thresh;
            return this;
        }

        /**
         * 设置保存feed进声纹模块音频的保存路径，供debug时候dump音频，不设置则不保存音频
         * 比如"/sdcard/speech/"
         *
         * @param saveAudioPath 音频的保存路径
         * @return {@link Builder}
         */
        public Builder setSaveAudioPath(String saveAudioPath) {
            this.mSaveAudioPath = saveAudioPath;
            return this;
        }

        /**
         * 设置保存唤醒内核给声纹的音频数据
         *
         * @param vprintCutSaveDir feed 给声纹内核的音频数据保存的文件夹路径
         * @return {@link Builder}
         */
        public Builder setVprintCutSaveDir(String vprintCutSaveDir) {
            this.mVprintCutSaveDir = vprintCutSaveDir;
            return this;
        }

        public VprintIntent create() throws IllegalArgumentException {
            return new VprintIntent(this);
        }
    }

    public com.aispeech.lite.vprint.VprintIntent transferIntent() {
        return new com.aispeech.lite.vprint.VprintIntent.Builder()
                .setVprintCutSaveDir(getVprintCutSaveDir())
                .setThresh(getThresh())
                .setAction(com.aispeech.lite.vprint.VprintIntent.Action
                        .getActionByValue(getAction().getValue()))
                .setAecChannelNum(getAecChannelNum())
                .setBfChannelNum(getBfChannelNum())
                .setOutChannelNum(getOutChannelNum())
                .setSaveAudioPath(getSaveAudioPath())
                .setSensitivityLevel(getSensitivityLevel())
                .setSnrThresh(getSnrThresh())
                .setTrainNum(getTrainNum())
                .setUserId(getUserId())
                .setVprintWord(getVprintWord())
                .create();

    }
}
