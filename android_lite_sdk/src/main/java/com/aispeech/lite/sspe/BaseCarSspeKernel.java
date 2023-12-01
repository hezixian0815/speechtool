package com.aispeech.lite.sspe;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.engines2.AIFespCarEngine;
import com.aispeech.kernel.LiteSoFunction;
import com.aispeech.kernel.Sspe;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.config.LocalSignalProcessingConfig;
import com.aispeech.lite.fespx.FespxKernelListener;
import com.aispeech.lite.function.CarFunctionHelper;
import com.aispeech.lite.function.ICarFunction;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.SignalProcessingParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Description:
 * Author: junlong.huang
 * CreateTime: 2023/3/29
 */
public abstract class BaseCarSspeKernel extends BaseSspeKernel implements ICarFunction {


    protected volatile String cachedWakeupData;
    protected String dynamicParams;
    protected String currentWakeupWord;
    protected List<String> mRangesWords; //触发唤醒后不切换音频范围
    private volatile boolean isBackRowRestrainEnable = false;
    private AtomicInteger fespcarDataReceivedEngines = new AtomicInteger(Integer.MAX_VALUE);


    protected volatile int mDriveMode = -1; // 驾驶模式
    /**
     * 定位模式下/自由组合模式，通过唤醒角度doa来自动设置主驾或者副驾模式
     * 对话完成后需要重新复位，设置为-1
     */
    protected volatile int mCachedWakeUpDoa = -1;

    /**
     * 自由组合模式，默认全车
     */
    protected volatile int mWakeupChannelMask = ICarFunction.COMBINATION_POSITION_ENTIRE;

    public BaseCarSspeKernel(String tag, FespxKernelListener listener) {
        super(tag, listener);
        mDriveMode = getSspeDriverModel();
        mWakeupChannelMask = getWakeupChannelMask();
    }

    @Override
    protected void handleCustomMessage(Message message) {
        switch (message.mId) {
            case Message.MSG_UPDATE:
                if (mState != EngineState.STATE_RUNNING) {
                    trackInvalidState("update");
                } else {
                    mConfig = (LocalSignalProcessingConfig) message.mObject;
                    if (engine != null) {
                        engine.destroy();

                        int flag = initEngine(engine, mConfig);
                        if (flag == AIConstant.OPT_SUCCESS) {
                            transferState(EngineState.STATE_NEWED);
                        }
                        engine.start("");
                        isStopped = false;
                        isWakeuped = false;
                        isBackRowRestrainEnable = mConfig.isBackRowRestrainEnable();
                        set(getDynamicParams());
                        transferState(EngineState.STATE_RUNNING);
                    }
                }
                break;
        }
    }

    @Override
    public void onDoaResult(int doa, int doaType) {
        super.onDoaResult(doa, doaType);
        if (mRangesWords != null && mRangesWords.contains(currentWakeupWord)) {
            Log.d(TAG, "don't have to switch modes automatically.");
        } else {
            updateCacheDoa(CarFunctionHelper.transformDoa(doa));
        }
    }

    private String getDynamicParams() {
        try {
            JSONObject jsonObj;
            if (TextUtils.isEmpty(dynamicParams)) {
                jsonObj = new JSONObject();
            } else {
                jsonObj = new JSONObject(dynamicParams);
            }
            if (isBackRowRestrainEnable) {
                jsonObj.put("driveMode", 0);
            } else {
                //全车模式后排不抑制，https://jira.aispeech.com.cn/browse/YJGGZC-10642
                jsonObj.put("driveMode", mDriveMode == ICarFunction.DRIVEMODE_ENTIRE ? 1 : 0);
            }
            dynamicParams = jsonObj.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return dynamicParams;
    }

    @Override
    public void setDriveMode(int driveMode, int wakeupChannelMask) {
        setDriveMode(driveMode);
        mWakeupChannelMask = wakeupChannelMask;
        Log.i(TAG, "setDriveMode =" + driveMode + "    mWakeupChannelMask = " + mWakeupChannelMask);
        saveWakeupChannelMask(mWakeupChannelMask);
    }

    @Override
    public void setDoaManually(int doa) {
        Log.i(TAG, "setDoaManually doa = " + doa);
        updateCacheDoa(CarFunctionHelper.transformDoa(doa));
    }

    @Override
    protected void preInitEngine(LiteSoFunction engine, LocalSignalProcessingConfig config) {
        setRangesWords(config.getRangesWords());
        //判断为0,则未设置取消音频回抛接口
        if (config.getDisCallBackResultData()==0) return;
        setFespcarDataReceivedEnable(config.getDisCallBackResultData());
    }

    @Override
    public void setRangesWords(List<String> rangesWords) {
        this.mRangesWords = rangesWords;
        Log.i(TAG, "setRangesWords rangesWords = " + (mRangesWords != null ? mRangesWords.toString() : ""));
    }

    @Override
    public void set(String setParam) {
        dynamicParams = setParam;
        super.set(getDynamicParams());
    }

    /**
     * 设置取消fesp回传，关闭fespcar音频接口回调的抛出
     **/
    public void setFespcarDataReceivedEnable(int fespcarDataReceivedEnable) {
        fespcarDataReceivedEngines.set(~fespcarDataReceivedEnable & Integer.MAX_VALUE);
    }


    /**
     * 获取 驾驶模式，只有 fespCar 模块有这个功能
     *
     * @return 0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式，-1 错误，没有获取到
     */
    @Override
    public synchronized int getDriveMode() {
        return mDriveMode;
    }

    private static void setSspeDriverModel(int model) {

        SharedPreferences sp = AISpeech.getContext().getSharedPreferences(ICarFunction.SP_NAME_CAR_CACHE, Context.MODE_PRIVATE);
        sp.edit().putInt(ICarFunction.SP_KEY_DRIVER_MODEL, model).apply();
    }

    private static int getSspeDriverModel() {
        SharedPreferences sp = AISpeech.getContext().getSharedPreferences(ICarFunction.SP_NAME_CAR_CACHE, Context.MODE_PRIVATE);
        return sp.getInt(ICarFunction.SP_KEY_DRIVER_MODEL, ICarFunction.DRIVEMODE_POSITIONING);
    }

    private static void saveWakeupChannelMask(int wakeupChannelMask) {
        SharedPreferences sp = AISpeech.getContext().getSharedPreferences(ICarFunction.SP_NAME_CAR_CACHE, Context.MODE_PRIVATE);
        sp.edit().putInt(ICarFunction.SP_KEY_WAKEUP_MASK, wakeupChannelMask).apply();
    }

    private static int getWakeupChannelMask() {
        SharedPreferences sp = AISpeech.getContext().getSharedPreferences(ICarFunction.SP_NAME_CAR_CACHE, Context.MODE_PRIVATE);
        return sp.getInt(ICarFunction.SP_KEY_WAKEUP_MASK, ICarFunction.COMBINATION_POSITION_ENTIRE);
    }

    public boolean isAutoSetDriveMode() {
        return mDriveMode == ICarFunction.DRIVEMODE_POSITIONING;
    }


    /**
     * 在定位模式下，如果原来自动设置成主驾模式或者副驾模式，则还原成定位模式
     */
    @Override
    public void resetDriveMode() {
        Log.i(TAG, "resetDriveMode");
        mCachedWakeUpDoa = -1;
    }

    @Override
    public synchronized void setDriveMode(int driveMode) {
        resetDriveMode();
        Log.i(TAG, "setDriveMode =" + driveMode);
        mDriveMode = driveMode;
        setSspeDriverModel(driveMode);
        set(dynamicParams);

        // 默认全部打开
        mWakeupChannelMask = ICarFunction.COMBINATION_POSITION_ENTIRE;
        saveWakeupChannelMask(mWakeupChannelMask);
    }

    @Override
    public void onWakeup(AIResult result) {
        super.onWakeup(result);

        JSONObject resultJSONObject = result.getResultJSONObject();
        if (resultJSONObject.has("wakeupWord")) {
            currentWakeupWord = resultJSONObject.optString("wakeupWord");
        }

    }

    @Override
    protected void handleMsgRelease() {
        super.handleMsgRelease();
        resetDriveMode();
        isBackRowRestrainEnable = false;
    }

    @Override
    protected void handleMsgStop() {
        super.handleMsgStop();
        resetDriveMode();
    }

    @Override
    protected void handleMsgStart(Message message) {
        if (mState != EngineState.STATE_NEWED) {
            trackInvalidState("start");
        } else {
            params = (SignalProcessingParams) message.mObject;
            Log.w(TAG, "feed data module isInputContinuousAudio:  "
                    + params.isInputContinuousAudio());
            Log.d(TAG, "start");
            createFileWriter();
            if (engine != null) {
                engine.start("");
                isStopped = false;
                isWakeuped = false;
                set(getDynamicParams()); // 车载额外的逻辑
                transferState(EngineState.STATE_RUNNING);
            }
        }
    }

    @Override
    protected void createFileWriter() {
        createFileSaveUtil();
    }

    /**
     * 唤醒信息回调
     */
    protected class CarWakeupCallbackImpl implements Sspe.wakeup_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "MyWakeupCallback return : " + retString);
                // 全车模式 定位模式 自由组合模式且全勾选了（1111），直接返回;半字唤醒直接处理
                if ((mDriveMode == ICarFunction.DRIVEMODE_ENTIRE &&
                        (mWakeupChannelMask & ICarFunction.COMBINATION_POSITION_ENTIRE) == ICarFunction.COMBINATION_POSITION_ENTIRE)
                        || mDriveMode == ICarFunction.DRIVEMODE_POSITIONING
                        || (mDriveMode == ICarFunction.DRIVEMODE_FREE_COMBINTION && isChannelMaskEntire())
                        || isSubWakeup(retString)
                ) {
                    cachedWakeupData = null;
                    processWakeupCallback(retString);
                } else {
                    //数据暂存,等定位后处理
                    cachedWakeupData = retString;
                }
            }
            return 0;
        }
    }

    /**
     * subWord返回唤醒角度doa
     */
    protected class DoaCommonCallback implements Sspe.doa_common_callback {

        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "DoaCommonCallback return  -- : " + retString);
                processDoaResultByDriveModel(retString);
            }
            return 0;
        }

    }


    protected class CarDoaCallbackImpl implements Sspe.doa_callback {

        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                String retString = Util.newUTF8String(data);
                Log.d(TAG, "MyDoaCallback return : " + retString);
                processDoaResultByDriveModel(retString);
            }
            return 0;
        }
    }

    /**
     * 在返回唤醒角度后，依据设置的驾驶模式来处理唤醒
     *
     * @param retString
     */
    protected void processDoaResultByDriveModel(String retString) {
        Log.d(TAG, "processDoaResultByDriveModel  : " + retString);
        try {
            JSONObject resultJson = new JSONObject(retString);
            if (resultJson.has("backType") && resultJson.getInt("backType") == AIConstant.DOA.TYPE_QUERY) {
                Log.d(TAG, "active get doa !");
                //返回doa
                mWakeupProcessor.processDoaResult(retString);
                return;
            }

            if (!CarFunctionHelper.filterWithDriveMode(mDriveMode, mCachedWakeUpDoa, mWakeupChannelMask, retString)) {
                Log.e(TAG, "!filterWithDriveMode");
                processInterceptWakeup(retString);
                return;
            }

            if (!params.isInputContinuousAudio()) {
                if (!mHasDoaOut) {
                    //处理缓存唤醒信息
                    if (cachedWakeupData != null) {
                        processWakeupCallback(cachedWakeupData);
                        cachedWakeupData = null;
                    }

                    //返回doa
                    mWakeupProcessor.processDoaResult(retString);

                    mHasDoaOut = true;
                    Log.d(TAG, "first doa cb end");
                } else {
                    Log.w(TAG, "more than one doa, ignore");
                }
            } else {
                Log.i(TAG, "dealwith wakeup and doa");
                //处理缓存唤醒信息
                if (cachedWakeupData != null) {
                    processWakeupCallback(cachedWakeupData);
                    cachedWakeupData = null;
                }

                mWakeupProcessor.processDoaResult(retString);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void processInterceptWakeup(String retStrung) {

        if (mListener == null || TextUtils.isEmpty(retStrung)) {
            Log.i(TAG, "processInterceptWakeup return: " + mListener + "," + retStrung);
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject(retStrung);
            JSONObject wakeupJsonObj;
            String wakeupWord = "";
            double confidence = 0.0f;
            if (!TextUtils.isEmpty(cachedWakeupData)) {
                wakeupJsonObj = new JSONObject(cachedWakeupData);
                wakeupWord = wakeupJsonObj.optString("wakeupWord", "");
                confidence = wakeupJsonObj.optDouble("confidence", 0.0f);
            }
            int doa = jsonObject.optInt("doa");

            mListener.onInterceptWakeup(doa, confidence, wakeupWord);
        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    public boolean needUpdateCacheDoa() {
        return mDriveMode == ICarFunction.DRIVEMODE_POSITIONING || mDriveMode == ICarFunction.DRIVEMODE_FREE_COMBINTION;
    }

    /**
     * 当定位模式时，根据唤醒角度自动设置成主驾模式或者副驾模式
     *
     * @param driveMode 1 为主驾方位，2 位副驾方位
     */
    private void updateCacheDoa(int driveMode) {
        Log.i(TAG, "updateCacheDoa drivemode = " + driveMode + "; currentDriveMode = " + getDriveMode());
        if (!needUpdateCacheDoa())
            return;

        Log.i(TAG, "updateCacheDoa mCachedWakeUpDoa = " + driveMode);
        mCachedWakeUpDoa = driveMode;
    }

    protected void doCallbackResultData(byte[] vadData, byte[] asrData) {

        if (!isCallBackResultData(AIFespCarEngine.FESPCAR_DOUBLEVAD_DATARECEIVED_ENABLE)) return;

        if (mListener == null) return;

        // 保持和原来的逻辑一致
        if (params == null || params.isNeedCopyResultData()) {
            byte[] bufferVad = new byte[vadData.length];
            byte[] bufferAsr = new byte[asrData.length];
            System.arraycopy(vadData, 0, bufferVad, 0, bufferVad.length);
            System.arraycopy(asrData, 0, bufferAsr, 0, bufferAsr.length);
            mListener.onResultDataReceived(bufferVad, bufferAsr);
        } else {
            mListener.onResultDataReceived(vadData, asrData);
        }

    }

    protected void doCallbackResultData(byte[] data, int size, int wakeupType) {

        if (!isCallBackResultData(AIFespCarEngine.FESPCAR_SINGLEVAD_DATARECEIVED_ENABLE)) return;

        if (mListener == null) return;

        // 保持和原来的逻辑一致
        if (params == null || params.isNeedCopyResultData()) {
            byte[] buffer = new byte[size];
            System.arraycopy(data, 0, buffer, 0, size);
            mListener.onResultDataReceived(buffer, size, wakeupType);
        } else {
            mListener.onResultDataReceived(data, size, wakeupType);
        }

    }

    /**
     * 判断该场景是否开启了音频回调
     */
    public boolean isCallBackResultData(int ResultData) {
        return (fespcarDataReceivedEngines.get() & ResultData) > 0;
    }

    /**
     * 自由组合模式下 mask是否设置了全车
     *
     * @return
     */
    private boolean isChannelMaskEntire() {
        return (mWakeupChannelMask & ICarFunction.COMBINATION_POSITION_ENTIRE) == ICarFunction.COMBINATION_POSITION_ENTIRE;
    }

    /**
     * 是否为半字唤醒
     *
     * @param data
     * @return
     */
    private boolean isSubWakeup(String data) {
        try {
            JSONObject jsonObject = new JSONObject(data);
            return jsonObject.optInt("status", -1) == 7;
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return false;
    }

}
