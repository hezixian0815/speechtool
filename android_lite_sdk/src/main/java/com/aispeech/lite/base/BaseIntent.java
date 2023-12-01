package com.aispeech.lite.base;

/**
 * Description: 抽离公共Intent基类
 * Author: junlong.huang
 * CreateTime: 2023/5/22
 */
public class BaseIntent {

    /**
     * 自行feed数据的时候，是否需要对数据进行拷贝，默认会进行拷贝
     */
    protected boolean needCopyFeedData = true;

    /**
     * 对外抛出的数据是否进行拷贝，默认拷贝
     */
    protected boolean needCopyResultData = true;

    /**
     * 设置是否自行feed数据,不使用内部录音机(包括MockRecord和AIAudioRecord), default is {@value}
     */
    protected boolean useCustomFeed = false;


    public boolean isNeedCopyFeedData() {
        return needCopyFeedData;
    }

    /**
     * 自行feed数据的时候，是否需要对数据进行拷贝，默认会进行拷贝
     */
    public void setNeedCopyFeedData(boolean needCopyFeedData) {
        this.needCopyFeedData = needCopyFeedData;
    }

    public boolean isUseCustomFeed() {
        return useCustomFeed;
    }

    /**
     * 设置是否自行feed数据,不使用内部录音机(包括MockRecord和AIAudioRecord)
     *
     * @param useCustomFeed the useCustomFeed to set
     */
    public void setUseCustomFeed(boolean useCustomFeed) {
        this.useCustomFeed = useCustomFeed;
    }

    public boolean isNeedCopyResultData() {
        return needCopyResultData;
    }

    /**
     * 对外抛出的数据是否进行拷贝，默认拷贝
     */
    public void setNeedCopyResultData(boolean needCopyResultData) {
        this.needCopyResultData = needCopyResultData;
    }
}
