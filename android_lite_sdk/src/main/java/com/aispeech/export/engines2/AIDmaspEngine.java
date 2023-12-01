package com.aispeech.export.engines2;

import android.text.TextUtils;

import com.aispeech.AIResult;
import com.aispeech.base.BaseInnerEngine;
import com.aispeech.common.AIConstant;
import com.aispeech.common.DynamicParamUtils;
import com.aispeech.common.JNIFlag;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.config.AIDmaspConfig;
import com.aispeech.export.exception.IllegalPinyinException;
import com.aispeech.export.intent.AIDmaspIntent;
import com.aispeech.export.interceptor.DoaInterceptor;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.export.interceptor.WakeupInterceptor;
import com.aispeech.export.listeners.AIDmaspListener;
import com.aispeech.kernel.Dmasp;
import com.aispeech.kernel.Utils;
import com.aispeech.lite.config.LocalDmaspConfig;
import com.aispeech.lite.dmasp.DmaspProcessor;
import com.aispeech.lite.function.ICarFunction;
import com.aispeech.lite.param.DmaspParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 车载4mic
 * @deprecated 废弃，参考{@link com.aispeech.export.engines2.AIFespCarEngine}
 */
@Deprecated
public class AIDmaspEngine {
    private final String TAG = "AIDmaspEngine";
    private static AIDmaspEngine sInstance = null;
    private DmaspProcessor mDmaspProcessor;
    private DmaspParams mParam;
    private InnerEngine mInnerEngine;
    private LocalDmaspConfig mConfig;
    private AIDmaspConfig mAIDmaspConfig;
    /**
     * 定位模式,指向唤醒方位
     */
    public static final int DMASP_CAR_DRIVE_MODE_POSITIONING = ICarFunction.DRIVEMODE_POSITIONING;
    /**
     * 主驾模式,指向主驾驶方位
     */
    public static final int DMASP_CAR_DRIVE_MODE_MAIN = ICarFunction.DRIVEMODE_MAIN;
    /**
     * 副驾驶模式,指向副驾驶方位
     */
    public static final int DMASP_CAR_DRIVE_MODE_COPILOT = ICarFunction.DRIVEMODE_COPILOT;
    /**
     * 全车模式,无指向
     */
    public static final int DMASP_CAR_DRIVE_MODE_ENTIRE = ICarFunction.DRIVEMODE_ENTIRE;
    /**
     * 自由自核模式,无指向
     */
    public static final int DMASP_CAR_DRIVE_MODE_FREE_COMBINTION = ICarFunction.DRIVEMODE_FREE_COMBINTION;
    /**
     * 主驾驶方位 doa
     */
    public final static int DMASP_CAR_DOA_MAIN = ICarFunction.CAR_DOA_MAIN;
    /**
     * 副驾驶方位 doa
     */
    public final static int DMASP_CAR_DOA_COPILOT = ICarFunction.CAR_DOA_COPILOT;
    /**
     * 左后 doa
     */
    public final static int DMASP_CAR_DOA_LEFT_BACKSEAT = ICarFunction.CAR_DOA_LEFT_BACKSEAT;
    /**
     * 右后 doa
     */
    public final static int DMASP_CAR_DOA_RIGHT_BACKSEAT = ICarFunction.CAR_DOA_RIGHT_BACKSEAT;


    private AIDmaspEngine() {
        if (null == mParam)
            mParam = new DmaspParams();
        if (null == mDmaspProcessor)
            mDmaspProcessor = new DmaspProcessor();
        if (null == mConfig)
            mConfig = new LocalDmaspConfig();
        if (null == mInnerEngine)
            mInnerEngine = new InnerEngine();
        JNIFlag.isLoadCarSspe = true;
    }

    /**
     * 4mic    唤醒引擎
     *
     * @return 唤醒引擎实例
     */
    public static synchronized AIDmaspEngine getInstance() {
        if (null == sInstance) {
            synchronized (AIDmaspEngine.class) {
                if (null == sInstance) {
                    sInstance = new AIDmaspEngine();
                }
            }
        }
        return sInstance;
    }

    public void init(AIDmaspConfig aiDmaspConfig, AIDmaspListener listener) {
        Log.i(TAG, "init");
        mAIDmaspConfig = aiDmaspConfig;
        mInnerEngine.init(listener);
        parseConfig(aiDmaspConfig);
        mDmaspProcessor.init(mInnerEngine, mConfig);
    }

    private void parseConfig(AIDmaspConfig config) {

        mParam.setWords(config.getWakeupWord().pinyin);
        mParam.setThreshold(config.getWakeupWord().threshold);
        mParam.setMajors(config.getWakeupWord().majors);
        mParam.setDchecks(config.getWakeupWord().dcheck);
        mParam.setDynamicAlignment(config.isDynamicAlignment());

        mConfig.setDmaspChannelCount(config.getDmaspChannelCount());


        List<String> assetList = new ArrayList<>();

        String wakeupResource = config.getWakeupResource();
        if (TextUtils.isEmpty(wakeupResource)) {
            Log.e(TAG, "wakeupResource not found int asserts!!");
            mConfig.setWakeupBinPath("OFF");
        } else if (wakeupResource.startsWith("/")) {
            mConfig.setWakeupBinPath(wakeupResource);
        } else {
            assetList.add(wakeupResource);
            mConfig.setWakeupBinPath(Util.getResourceDir(mConfig.getContext()) + File.separator + wakeupResource);
        }

        String dmaspResource = config.getDmaspResource();
        if (TextUtils.isEmpty(dmaspResource)) {
            Log.d(TAG, "dmaspResource not found in asserts !");
            mConfig.setResBinPath("OFF");
        } else if (dmaspResource.startsWith("/")) {
            mConfig.setResBinPath(dmaspResource);
        } else {
            assetList.add(dmaspResource);
            mConfig.setResBinPath(Util.getResourceDir(mConfig.getContext()) + File.separator + dmaspResource);
        }

        mConfig.setAssetsResNames(assetList.toArray(new String[assetList.size()]));
    }

    public void parseIntent(AIDmaspIntent aiDmaspIntent) {
        if (aiDmaspIntent == null) {
            Log.e(TAG, "aiDmaspIntent is null !");
            return;
        }
        Log.d(TAG, "aiDmaspIntent " + aiDmaspIntent.toString());
        mParam.setSaveAudioPath(aiDmaspIntent.getSaveAudioFilePath());
        mParam.setDumpAudioPath(aiDmaspIntent.getDumpWakeupAudioPath());
        mParam.setDumpTime(aiDmaspIntent.getDumpWakeupTime());
    }

    /**
     * 设置不使用内部录音机时可用，自行feed音频数据
     *
     * @param data 音频数据
     */
    public void feed(byte[] data) {
        if (mDmaspProcessor != null) {
            mDmaspProcessor.feedData(data, data.length);
        }
    }

    /**
     * get value
     * @param param key
     * @return value
     */
    public int getValueOf(String param) {
        if (mDmaspProcessor != null) {
            return mDmaspProcessor.getValueOf(param);
        }

        return -1;
    }

    /**
     * 开启唤醒
     *
     * @param intent 唤醒启动配置信息
     */
    public void start(AIDmaspIntent intent) {
        Log.i(TAG, "start");
        parseIntent(intent);
        if (mDmaspProcessor != null) {
            mDmaspProcessor.start(mParam);
        }
    }

    /**
     * 开启唤醒，仅仅开启唤醒，并且是在前端信号处理正常的情况下开启唤醒
     */
    public void startNWakeup() {
        Log.i(TAG, "startNWakeup");
        if (mDmaspProcessor != null) {
            mDmaspProcessor.setNWakeupEnable(true);
        }
    }

    /**
     * 动态调整参数，具体请参照 demo
     *
     * @param wakeupWord 唤醒词，参数示例：["ni hao xiao chi","xiao bu xiao bu"]
     * @param threshold  唤醒词对应的阈值，参数示例：[0.2, 0.3]
     * @param majors     是否主唤醒词，主唤醒词为1，副唤醒词为0，如 [1,0]
     *                   设置主唤醒词后，内核会对唤醒词部分音频进行回溯
     * @throws IllegalPinyinException {@link IllegalPinyinException} 非法拼音异常
     */
    public void setWakeupWords(String[] wakeupWord, float[] threshold, int[] majors) throws IllegalPinyinException {

        // 同步更新mParam中记录的唤醒词信息
        mParam.setWords(wakeupWord);
        mParam.setThreshold(threshold);
        mParam.setMajors(majors);
        mParam.setDchecks(null);

        String DmaspParams = DynamicParamUtils.getWakeupWordsParams(wakeupWord, threshold, majors, true);
        if (mDmaspProcessor != null) {
            mDmaspProcessor.set(DmaspParams);
        }
    }

    /**
     * 关闭前端信号处理和唤醒，如果使用内部录音机的话一并关闭
     */
    public void stop() {
        Log.i(TAG, "stop");
        if (mDmaspProcessor != null) {
            mDmaspProcessor.stop();
        }
    }

    /**
     * 关闭唤醒，仅仅关闭唤醒
     */
    public void stopNWakeup() {
        Log.i(TAG, "stopNWakeup");
        if (mDmaspProcessor != null) {
            mDmaspProcessor.setNWakeupEnable(false);
        }
    }

    /**
     * 销毁唤醒内核
     */
    public void destroy() {
        Log.i(TAG, "destroy");
        if (mDmaspProcessor != null) {
            mDmaspProcessor.release();
        }
    }

    /**
     * 检查唤醒内核是否准备好，外部可以不关注
     *
     * @return true or false
     */
    public static boolean checkLibValid() {
        return Dmasp.isDmaspSoValid() && Utils.isUtilsSoValid();
    }

    /**
     * 获取唤醒资源是否带VAD状态流
     *
     * @return true or false
     */
    public boolean isWakeupSsp() {
        if (mDmaspProcessor != null) {
            return mDmaspProcessor.isWakeupSsp();
        }
        return false;
    }


    /**
     * 定位模式下通知引擎回正 beamforming 指向，定位模式下需要外部在结束对话状态后通知到引擎内部
     */
    public void notifyDialogEnd() {
        resetDriveMode();
    }

    public void resetDriveMode() {
        if (mDmaspProcessor != null) {
            Log.i(TAG, "resetDriveMode");
            mDmaspProcessor.resetDriveMode();
        }
    }

    /**
     * 获取 驾驶模式
     *
     * @return 0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式，-1 错误，没有获取到
     */
    public synchronized int getDriveMode() {
        return mDmaspProcessor != null ? mDmaspProcessor.getDriveMode() : -1;
    }

    /**
     * 设置驾驶模式， 0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式
     * @param driveMode
     */
    public synchronized void setDriveMode(int driveMode) {
        if (mDmaspProcessor != null) {
            mDmaspProcessor.setDriveMode(driveMode);
        }
    }

    /**
     * 设置驾驶模式，此方法主要为自由组合模式传入wakeupChannelMask参数
     *
     * @param driveMode         0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式，4为自由组合模式
     * @param wakeupChannelMask 自由驾驶模式的组合：整数的二进制表示
     *                          0001：主驾
     *                          0010：副驾
     *                          0100：左后
     *                          1000：右后
     *                          1111：主+副+左后+右后
     */
    public synchronized void setDriveMode(int driveMode, int wakeupChannelMask) {
        if (mDmaspProcessor != null) {
            mDmaspProcessor.setDriveMode(driveMode, wakeupChannelMask);
        }
    }

    /**
     * 在定位模式下，手动设置为主驾唤醒或者副驾唤醒
     *
     * @param doa 1为主驾唤醒; 2为副驾唤醒;
     */
    public synchronized void setDoaManually(int doa) {
        if (doa != DMASP_CAR_DOA_MAIN && doa != DMASP_CAR_DOA_COPILOT) {
            Log.e(TAG, "doa is not DMASP_CAR_DOA_MAIN or DMASP_CAR_DOA_COPILOT");
            return;
        }

        if (mDmaspProcessor != null) {
            mDmaspProcessor.setDoaManually(doa);
        }
    }


    private class InnerEngine extends BaseInnerEngine {

        private AIDmaspListener mListener;

        void init(AIDmaspListener listener) {
            super.init(listener);
            mListener = listener;
        }

        @Override
        public void release() {
            super.release();
            if (mListener != null) {
                mListener = null;
            }
        }

        @Override
        protected void callbackInMainLooper(CallbackMsg callback, Object obj) {
            switch (callback) {
                case MSG_RESULTS:
                    AIResult result = (AIResult) obj;
                    if (result.getResultType() == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                        Log.d(TAG, "---------> result = " + result.toString());
                        JSONObject resultObj;
                        double confidence;
                        double wakeupEnergy;
                        String wakeupWord;
                        try {
                            resultObj = new JSONObject(result.getResultObject().toString());
                            String recordId = result.getRecordId();
                            if (!resultObj.isNull("wakeupWord")) {
                                if (!resultObj.isNull("wakeupWord") && !resultObj.isNull("confidence")) {
                                    confidence = resultObj.getDouble("confidence");
                                    wakeupEnergy = resultObj.has("wakeupEnergy") ? resultObj.getDouble("wakeupEnergy") : -1;//新算法不提供此值
                                    wakeupWord = resultObj.getString("wakeupWord");
                                    if (mListener != null) {
                                        Log.d(TAG, "wakeup callback success:START");
                                        Log.d(TAG, "wakeup callback success:{\"wakeupWord\":" + wakeupWord + "}");
                                        if (mAIDmaspConfig.getBlackWords() != null && mAIDmaspConfig.getBlackWords().length > 0) {
                                            if (Arrays.asList(mAIDmaspConfig.getBlackWords()).contains(wakeupWord)) {
                                                //唤醒词吸收
                                                Log.w(TAG, "black wakeup is shot,return ");
                                                return;
                                            }
                                        }
                                        JSONObject customObj = new JSONObject().put(IInterceptor.Name.WAKEUP, resultObj.toString());
                                        JSONObject inputObj = WakeupInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, customObj);
                                        SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.WAKEUP, inputObj);
                                        Log.d(TAG, "WAKEUP..... " + wakeupWord);
                                        mListener.onWakeup(recordId, confidence, wakeupWord);
                                    }
                                }
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                case MSG_DOA_RESULT:
                    String sslDoa = (String) obj;
                    if (TextUtils.isEmpty(sslDoa) || sslDoa.length() < 2) {
                        Log.e(TAG, "sslDoa is empty  or ssl doa len < 2");
                        return;
                    }

                    int ssl = Integer.parseInt(sslDoa.substring(0, 1));
                    int doa = Integer.parseInt(sslDoa.substring(1, 2));
                    Log.i(TAG, "onDoaResult, ssl:" + ssl + " , doa:" + doa);
                    try {
                        JSONObject inputObj = DoaInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.CALLBACK, new JSONObject().put(IInterceptor.Name.DOA, doa));
                        SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.DOA, inputObj);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    if (mListener != null) {
                        mListener.onDoaResult(ssl, doa);
                    }

                    break;
                default:
                    break;
            }
        }

        @Override
        public void onResults(AIResult result) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_RESULTS, result);
        }

        @Override
        public void onDoaResult(String ssldoa) {
            sendMsgToCallbackMsgQueue(CallbackMsg.MSG_DOA_RESULT, ssldoa);
        }

        @Override
        public void onResultDataReceived(byte[] buffer, int size) {
            if (mListener != null) {
                mListener.onResultDataReceived(buffer, size);
            }
        }

        @Override
        public void onVprintCutDataReceived(int dataType, byte[] data, int size) {
            if (mListener != null) {
                mListener.onVprintCutDataReceived(dataType, data, size);
            }
        }

    }

}
