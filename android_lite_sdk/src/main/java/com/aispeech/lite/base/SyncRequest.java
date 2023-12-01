package com.aispeech.lite.base;


import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.SynchronizedHelper;

/**
 * Description: 同步方法委托类 目前只有初始化方法 后续可以补充
 * Author: junlong.huang
 * CreateTime: 2023/7/13
 */
public abstract class SyncRequest {

    protected String TAG = getClass().getSimpleName();

    private final SynchronizedHelper<Integer> mSyncInitHelper = new SynchronizedHelper<Integer>() {

        @Override
        protected void doRequest() {
            doInit();
        }

        @Override
        protected Integer onTimeOut() {
            return AIConstant.OPT_FAILED;
        }
    };

    public SyncRequest() {
        mSyncInitHelper.TAG = TAG;
    }

    public abstract void doInit();

    /**
     * 目前消耗的方法没有内核回调出来，先直接调Kernel层实现同步,默认
     */
    public void doDestroy() {

    }

    public int initSync(long time) {
        Log.i(TAG, "initSync: " + time);
        return mSyncInitHelper.request(time);
    }


    public void notifyInitResult(int code) {
        Log.i(TAG, "notifyInitResult: " + code);
        mSyncInitHelper.notifyResult(code);
    }

    public boolean isInitRequesting() {
        return mSyncInitHelper.isRequesting();
    }

    //    private SynchronizedHelper<Integer> mSyncDestroyHelper = new SynchronizedHelper<Integer>() {
//
//        @Override
//        protected void doRequest() {
//            doDestroy();
//        }
//
//        @Override
//        protected Integer onTimeOut() {
//            return AIConstant.OPT_FAILED;
//        }
//    };

//    public int destroySync(long time){
//        Log.i(TAG, "destroySync: " + time);
//        return mSyncDestroyHelper.request(time);
//    }
//
//
//    public void notifyDestroyResult(int code){
//        Log.i(TAG, "notifyDestroyResult: " + code);
//        mSyncDestroyHelper.notifyResult(code);
//    }

//    public boolean isDestroyRequesting(){
//        return mSyncDestroyHelper.isRequesting();
//    }


}
