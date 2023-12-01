package com.aispeech.lite.dmasp;

import static com.aispeech.export.engines2.AIDmaspEngine.DMASP_CAR_DOA_COPILOT;
import static com.aispeech.export.engines2.AIDmaspEngine.DMASP_CAR_DOA_MAIN;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.AIResult;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AudioHelper;
import com.aispeech.common.FileSaveUtil;
import com.aispeech.common.LimitQueue;
import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.export.interceptor.IInterceptor;
import com.aispeech.export.interceptor.SpeechInterceptor;
import com.aispeech.export.interceptor.WakeupInterceptor;
import com.aispeech.kernel.Dmasp;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.AISpeechSDK;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.LocalDmaspConfig;
import com.aispeech.lite.function.ICarFunction;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.DmaspParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by wuwei on 18-6-19.
 */

public class DmaspKernel extends BaseKernel implements ICarFunction {
    private static final String TAG = "DmaspKernel";
    private DmaspKernelListener mListener;
    private Dmasp mEngine;
    private volatile boolean isStopped = true;
    /**
     * 屏蔽kernel多次唤醒回调
     */
    private volatile boolean mHasWkpOut = false;
    private MyDmasp_callback myDmasp_callback;
    private DmaspParams mParams;
    /**
     * 保存唤醒之前的音频
     */
    private LimitQueue<byte[]> mDmaspQueue;

    private LocalDmaspConfig mDmaspConfig;

    /**
     * 当前设置的驾驶模式
     * 默认使用全车模式
     */
    private volatile int mDriveMode = ICarFunction.DRIVEMODE_ENTIRE;
    /**
     * 自由组合模式，默认只有主驾
     */
    private volatile int mWakeupChannelMask = 0b000000001;

    /**
     * 定位模式下，记录唤醒角度doa，只反馈对应方向信道数据
     * 对话完成后需要重新复位，设置为-1
     */
    private volatile int mCachedWakeUpDoa = -1;
    // Dmasp 输出的通道数 支持 4Mic 2Mic
    private int dmaspChannelCountr = 4;

    /**
     * 唤醒状态
     * wakeupStates[0] 是否需要动态对齐
     * wakeupStates[1] chanIndex
     * wakeupStates[2] sslIndex
     */
    protected int[] wakeupStates = new int[3];

    /**
     * 配置是否启用 音频通道动态对齐
     */
    private boolean isDynamicAlignment = true;


    public DmaspKernel(LocalDmaspConfig dmaspConfig, DmaspKernelListener listener) {
        super(TAG, listener);
        this.mListener = listener;
        mDmaspConfig = dmaspConfig;
        mWriterHelper = new AudioWriterHelper();
    }

    private final AudioWriterHelper mWriterHelper;

    class AudioWriterHelper {
        FileSaveUtil mDmaspFileSaver = null;
        FileSaveUtil mDriveFileSaver = null;

        public AudioWriterHelper() {

        }

        public void createAudioWriter() {
            String audioSavePath = mParams.getSaveAudioPath();
            if (!TextUtils.isEmpty(audioSavePath)) {
                mDmaspFileSaver = new FileSaveUtil();
                mDriveFileSaver = new FileSaveUtil();

                mDmaspFileSaver.init(audioSavePath);
                mDmaspFileSaver.prepare("dmasp");

                mDriveFileSaver.init(audioSavePath);
                mDriveFileSaver.prepare("dmasp_single_channel");

                // 创建唤醒音频缓存队列
                mDmaspQueue = new LimitQueue<>(80);
            }


        }

        public void closeAudioWriter() {

            if (mDriveFileSaver != null) {
                mDriveFileSaver.close();
                mDriveFileSaver = null;
            }
            if (mDmaspFileSaver != null) {
                mDmaspFileSaver.close();
                mDmaspFileSaver = null;
            }

            if (mDmaspQueue != null) {
                mDmaspQueue.clear();
                mDmaspQueue = null;
            }
        }

    }

    /**
     * 全车模式保存四合一单路数据
     * 其他驾驶模式保存对应doa单路数据
     */
    public void saveDriveModeData(byte[] data, int size) {
        if (mWriterHelper != null && mWriterHelper.mDriveFileSaver != null) {
            mWriterHelper.mDriveFileSaver.feedTypeCustom(data, size);
        }
    }


    /**
     * 当前是否为全车模式
     *
     * @return
     */
    public boolean isDriveModeEntire() {
        return mDriveMode == ICarFunction.DRIVEMODE_ENTIRE;
    }

    /**
     * 当前是否为定位模式
     *
     * @return
     */
    public boolean isDriveModePositioning() {
        return mDriveMode == ICarFunction.DRIVEMODE_POSITIONING;
    }

    private int[] getMicSequence(int doaIndex, int sslIndex) {

        int[] MIC_SEQUENCE = dmaspChannelCountr == 2 ? new int[]{1, 2} : new int[]{1, 2, 3, 4};

        int channelId = 0;
        for (int i = 0; i < MIC_SEQUENCE.length; i++) {
            if (i == (doaIndex - 1)) {
                channelId = i;
                break;
            }
        }
        MIC_SEQUENCE[channelId] = sslIndex;
        MIC_SEQUENCE[sslIndex - 1] = doaIndex;
        return MIC_SEQUENCE;
    }


    private class MyDmasp_callback extends Dmasp.dmasp_callback {
        @Override
        public int run(int dataType, byte[] data, int size) {
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                byte[] bufferData = new byte[size];
                if (isDynamicAlignment && wakeupStates[0] == 1) {
                    int[] micSequence = getMicSequence(wakeupStates[1], wakeupStates[2]);
                    //Log.i(TAG, "micSequence:" + Arrays.toString(micSequence));
                    byte[] newData = AudioHelper.changeChannel(data, mDmaspConfig.getDmaspChannelCount(), micSequence);
                    System.arraycopy(newData, 0, bufferData, 0, size);
                } else {
                    System.arraycopy(data, 0, bufferData, 0, size);
                }
                if (mListener != null) {
                    mListener.onResultBufferReceived(getDoaByDriveMode(), bufferData);
                }
                if (mWriterHelper != null && mWriterHelper.mDmaspFileSaver != null) {
                    // 本地保存处理后的音频文件
                    mWriterHelper.mDmaspFileSaver.feedTypeOut(bufferData, bufferData.length);
                }
            } else if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {//唤醒的回调
                String wakeUpStr = new String(data).trim();
                Log.d(TAG, "WAKEUP.CALLBACK: " + wakeUpStr);
                if (filterDriveMode(mDriveMode, wakeUpStr) || isDriveModeEntire() || isDriveModePositioning()) {
                    try {
                        processWakeupCallback(wakeUpStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
            return AISpeechSDK.AIENGINE_CALLBACK_COMPLETE;
        }
    }

    /**
     * 过滤驾驶模式 确认是否唤醒
     *
     * @param driveMode
     * @param wakeUpStr
     */
    private boolean filterDriveMode(int driveMode, String wakeUpStr) {

        try {
            JSONObject jsonObject = new JSONObject(wakeUpStr);
            if (!jsonObject.isNull("wakeupWord")) {
                StringBuffer log = new StringBuffer("wakeup driveMode=" + driveMode);
                if (jsonObject.has("ssl_index") && jsonObject.has("chan_index")) {
                    int chanIndex = jsonObject.getInt("chan_index");
                    log.append(", doa = " + chanIndex);
                    int sslIndex = jsonObject.getInt("ssl_index");
                    log.append(", index = " + sslIndex);
                    if (isDynamicAlignment) {
                        if (chanIndex != sslIndex) {
                            wakeupStates[0] = 1;
                            jsonObject.put("ssl_index", chanIndex);
                            Log.e(TAG, "chanIndex:" + chanIndex + ",sslIndex:" + sslIndex);
                        } else {
                            wakeupStates[0] = 0;
                        }
                        wakeupStates[1] = chanIndex;
                        wakeupStates[2] = sslIndex;
                    }
                    Log.d(TAG, log.toString());
                    // 主驾驶模式
                    if (driveMode == ICarFunction.DRIVEMODE_MAIN && chanIndex == 1) {
                        return true;
                    }
                    // 副驾驶模式
                    if (driveMode == ICarFunction.DRIVEMODE_COPILOT && chanIndex == 2) {
                        return true;
                    }
                    // 全车模式
                    if (driveMode == ICarFunction.DRIVEMODE_ENTIRE) {
                        return true;
                    }
                    // 定位模式
                    if (driveMode == ICarFunction.DRIVEMODE_POSITIONING) {
                        mCachedWakeUpDoa = chanIndex;
                        return true;
                    }
                    // 自由组合模式
                    if (driveMode == ICarFunction.DRIVEMODE_FREE_COMBINTION) {
                        if (checkDriverMode(chanIndex)) {
                            Log.d(TAG, "current doa is in free combintion driver mode, doa = " + chanIndex + " ,  mCachedWakeUpDoa = " + mCachedWakeUpDoa);
                            mCachedWakeUpDoa = chanIndex;
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    //自由组合模式下，把传入的mWakeupChannelMask整数转化为二进制，1表示可以抛出音频
    private boolean checkDriverMode(int doa) {
        if (mWakeupChannelMask > 0) {
            try {
                String binaryStr = Integer.toBinaryString(mWakeupChannelMask + 16);//toBinaryString(16)=10000，得到五个二进制
                Log.d(TAG, "checkDriverMode:" + binaryStr + " ,  doa:" + doa);
                String[] binarySplit = binaryStr.split("");
                switch (doa) {
                    case ICarFunction.DRIVEMODE_MAIN://自由组合模式是否包含主驾
                        if ("1".equals(binarySplit[binarySplit.length - 1])) {
                            return true;
                        }
                        break;
                    case ICarFunction.DRIVEMODE_COPILOT://自由组合模式是否包含副驾
                        if ("1".equals(binarySplit[binarySplit.length - 2])) {
                            return true;
                        }
                        break;
                    case 3://自由组合模式是否包含左后
                        if ("1".equals(binarySplit[binarySplit.length - 3])) {
                            return true;
                        }
                        break;
                    case 4://自由组合模式是否包含右后
                        if ("1".equals(binarySplit[binarySplit.length - 4])) {
                            return true;
                        }
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            throw new IllegalArgumentException("Illegal Argument: free combintion driver mode mWakeupChannelMask: " + mWakeupChannelMask);
        }
        return false;
    }


    /**
     * 通过驾驶模式获取对应音频线路
     * 定位模式 返回缓存的唤醒线路
     */
    public int getDoaByDriveMode() {
        int doa = -1;
        switch (mDriveMode) {
            case ICarFunction.DRIVEMODE_MAIN:
                doa = DMASP_CAR_DOA_MAIN;
                break;
            case ICarFunction.DRIVEMODE_COPILOT:
                doa = DMASP_CAR_DOA_COPILOT;
                break;
            case ICarFunction.DRIVEMODE_POSITIONING:
                doa = mCachedWakeUpDoa;
                break;
            case ICarFunction.DRIVEMODE_FREE_COMBINTION:
                doa = mCachedWakeUpDoa;
                break;
            default:
                break;

        }
        return doa;
    }

    @Override
    public synchronized int getValueOf(String param) {
        if (mEngine != null) {
            return mEngine.get(param);
        } else {
            Log.e(TAG, "getValueOf() err: engine is null");
            return -1;
        }
    }

    private void processWakeupCallback(String wakeupRetString) throws JSONException {
        if (TextUtils.isEmpty(wakeupRetString)) return;
        try {
            JSONObject customObj = new JSONObject().put(IInterceptor.Name.WAKEUP, wakeupRetString);
            JSONObject inputObj = WakeupInterceptor.getInputObj(IInterceptor.Layer.LITE, IInterceptor.FlowType.RECEIVE, customObj);
            Object obj = SpeechInterceptor.getInstance().doInterceptor(IInterceptor.Name.WAKEUP, inputObj);
            if (obj != null && obj instanceof JSONObject) {
                customObj = (JSONObject) obj;
                String interceptorResult = customObj.optString(IInterceptor.Name.WAKEUP);
                if (!TextUtils.isEmpty(interceptorResult)) {
                    wakeupRetString = interceptorResult;
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JSONObject resultObj;
        AIResult result = new AIResult();
        result.setLast(true);
        result.setResultType(AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
        result.setResultObject(wakeupRetString);
        result.setTimestamp(System.currentTimeMillis());

        resultObj = new JSONObject(result.getResultObject().toString());
        if (resultObj.has("status")) {
            int mWakeupStatus = resultObj.optInt("status");
            if (mWakeupStatus != 1 && mWakeupStatus != 2) {
                Log.w(TAG, "mWakeupStatus is not on real wakeup ");
                return;
            }
        }

        if (!mHasWkpOut) {
            mHasWkpOut = true;
            mListener.onResults(result);
            saveWakeupData(result);
            Log.d(TAG, "real wakeup");
        } else {
            Log.w(TAG, "more than one wkp, ignore");
        }

        if (wakeupRetString.contains("ssl_index") && wakeupRetString.contains("chan_index")) {
            mHasWkpOut = false;
            if (!resultObj.isNull("ssl_index") && !resultObj.isNull("chan_index")) {
                int index = resultObj.getInt("ssl_index");
                int doa = resultObj.getInt("chan_index");
                mListener.onDoa(1, doa);
            }

        }



    }


    /**
     * 初始化Dmasp内核
     */
    public void newKernel() {
        Log.d(TAG, "newKernel");
        sendMessage(new Message(Message.MSG_NEW));

    }

    /**
     * 启动Dmasp内核
     */
    public void startKernel() {
        Log.d(TAG, "startKernel");
        sendMessage(new Message(Message.MSG_START));
    }

    /**
     * 唤醒模块的单独控制开关
     *
     * @param enable true 可用 false 不可用
     */
    public void setNWakeupEnable(boolean enable) {
        if (mEngine != null) {
            JSONObject setObject = new JSONObject();
            try {
                setObject.put("wakeupSwitch", enable ? 1 : 0);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            String param = setObject.toString();
            mEngine.set(param);
        }
    }

    /**
     * 获取唤醒资源是否带VAD状态流
     *
     * @return true 带状态流 false 不带状态流
     */
    public boolean isWakeupSsp() {
        if (mEngine != null) {
            return mEngine.isWakeupSsp();
        }
        return false;
    }


    @Override
    public void feed(byte[] data) {
        super.feed(data);
    }

    @Override
    public void cancelKernel() {
        super.cancelKernel();
    }

    @Override
    public void stopKernel() {
        super.stopKernel();
    }

    @Override
    public void releaseKernel() {
        super.releaseKernel();
    }

    @Override
    public void run() {
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    myDmasp_callback = new MyDmasp_callback();
                    mEngine = new Dmasp();
                    Log.d(TAG, "mDmaspConfig : " + mDmaspConfig.toJSON().toString());
                    int flag = initDmasp(mDmaspConfig, mEngine);
                    mListener.onInit(flag);
                    break;
                case Message.MSG_START:
                    DmaspParams param = (DmaspParams) message.mObject;
                    this.mParams = param;
                    isDynamicAlignment = param.isDynamicAlignment();
                    if (mWriterHelper != null) mWriterHelper.createAudioWriter();
                    mEngine.start(param.toString());
                    isStopped = false;
                    break;
                case Message.MSG_SET:
                    String setParam = (String) message.mObject;
                    Log.d(TAG, "setParam" + setParam);
                    if (mEngine != null) {
                        mEngine.start(setParam);//add by wanzhicheng 四麦动态设置
                    }
                    break;
                case Message.MSG_STOP:
                    if (mWriterHelper != null) mWriterHelper.closeAudioWriter();
                    if (mEngine != null)
                        mEngine.stop();
                    isStopped = true;
                    resetDriveMode();
                    break;
                case Message.MSG_RELEASE:
                    if (mWriterHelper != null) mWriterHelper.closeAudioWriter();
                    if (mEngine != null) {
                        mEngine.destroy();
                        mEngine = null;
                    }
                    if (mDmaspConfig != null) {
                        mDmaspConfig = null;
                    }
                    if (myDmasp_callback != null) {
                        myDmasp_callback = null;
                    }
                    isStopped = true;
                    isReleased = true;
                    resetDriveMode();
                    break;
                case Message.MSG_FEED_DATA_BY_STREAM:
                    byte[] data = (byte[]) message.mObject;
                    if (AISpeech.recChannel == 1) {//无需通道互换的音频
                        if (mEngine != null && !isStopped) {
                            mEngine.feed(data);
                        }
                    } else if (AISpeech.recChannel == 2) {//需要通道互换,保存互换之后的aec_in
                        byte[] recChannelData = Util.getRecChannelData(data);
                        //处理互换之后的原始数据
                        if (mEngine != null && !isStopped) {
                            mEngine.feed(recChannelData);
                        }
                    }

                    // 保存原始录音的音频数据aec_in
                    if (mWriterHelper != null && !isStopped) {
                        if (mWriterHelper.mDmaspFileSaver != null) {
                            mWriterHelper.mDmaspFileSaver.feedTypeIn(data, data.length);
                        }

                        // 保存到唤醒音频队列中
                        if (mDmaspQueue != null && data != null) {
                            mDmaspQueue.offer(data);
                        }
                    }
                    break;
                case Message.MSG_BEAMFORMING_DATA:
                    byte[] DmaspData = (byte[]) message.mObject;
                    if (mListener != null) {
                        mListener.onResultBufferReceived(DmaspData);

                        if (mWriterHelper != null && mWriterHelper.mDmaspFileSaver != null) {
                            // 本地保存处理后的音频文件
                            mWriterHelper.mDmaspFileSaver.feedTypeOut(DmaspData, DmaspData.length);
                        }
                    }
                    break;
                case Message.MSG_ERROR:
                    mListener.onError((AIError) message.mObject);
                    resetDriveMode();
                    break;
                case Message.MSG_WAKEUP:
                    if (message.mObject instanceof String) {
                        doSaveWakeupData((String) message.mObject);
                    }

                    break;
                default:
                    break;
            }
            if (isReleased) {
                innerRelease();
                break;//release后跳出while循环
            }
        }
    }


    /**
     * @param driveMode 0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式
     */
    @Override
    public void setDriveMode(int driveMode) {
        resetDriveMode();
        Log.i(TAG, "setDriveMode =" + driveMode);
        mDriveMode = driveMode;
    }

    @Override
    public void setDriveMode(int driveMode, int wakeupChannelMask) {
        setDriveMode(driveMode);
        mWakeupChannelMask = wakeupChannelMask;
        Log.i(TAG, "setDriveMode =" + driveMode + "    mWakeupChannelMask = " + mWakeupChannelMask);
    }

    @Override
    public void setDoaManually(int doa) {
        Log.i(TAG, "setDoaManually doa = " + doa);
        autoSetDriveMode(doa2DriveMode(doa));
    }

    @Override
    public void setRangesWords(List<String> rangesWords) {

    }

    /**
     * 依照唤醒角度自动设置主副驾模式
     *
     * @param doa 唤醒角度
     * @return 主副驾模式
     */
    public int doa2DriveMode(int doa) {
        if (DMASP_CAR_DOA_MAIN == doa) {
            return ICarFunction.DRIVEMODE_MAIN;
        }

        if (DMASP_CAR_DOA_COPILOT == doa) {
            return ICarFunction.DRIVEMODE_COPILOT;
        }
        return -1;
    }

    /**
     * 获取 驾驶模式，
     *
     * @return 0为定位模式, 按照声源定位;1为主驾模式;2为副驾模式;3为全车模式，-1 错误，没有获取到
     */
    @Override
    public int getDriveMode() {
        return mDriveMode;
    }

    /**
     * 每次对话结束需要重置唤醒缓存，用于定位模式
     */
    @Override
    public void resetDriveMode() {
        Log.i(TAG, "resetDriveMode");
        if (mCachedWakeUpDoa != -1) {
            mCachedWakeUpDoa = -1;
        }
    }

    private int initDmasp(LocalDmaspConfig config, Dmasp engine) {
        int status = AIConstant.OPT_FAILED;
        long engineId = engine.init(config.toString(), myDmasp_callback);
        Log.d(TAG, "Dmasp create return " + engineId + ".");
        dmaspChannelCountr = config.getDmaspChannelCount();

        if (engineId == 0) {
            Log.d(TAG, "引擎初始化new失败");
            return AIConstant.OPT_FAILED;
        }

        int ret = engine.setvprintcutcb(new MyVprintCut_callbackImpl());
        Log.d(TAG, "Dmasp setvprintcutcb return " + ret);
        if (ret != 0 && ret != -9892) {
            Log.d(TAG, "引擎初始化失败");
            status = AIConstant.OPT_FAILED;
        } else {
            Log.d(TAG, "引擎初始化成功");
            status = AIConstant.OPT_SUCCESS;
        }
        return status;
    }

    class MyVprintCut_callbackImpl extends Dmasp.vprintcut_callback {
        public int run(int type, byte[] data, int size) {
            if (mListener != null) {
                mListener.onVprintCutDataReceived(type, data, size);
            }
            return 0;
        }
    }

    /**
     * 当定位模式时，根据唤醒角度自动设置成主驾模式或者副驾模式
     *
     * @param driveMode 1 为主驾方位，2 位副驾方位
     */
    private void autoSetDriveMode(int driveMode) {
        Log.i(TAG, "autoSetDriveMode drivemode = " + driveMode + "; currentDriveMode = " + getDriveMode());
        if (!isAutoSetDriveMode())
            return;
        if (driveMode != ICarFunction.DRIVEMODE_MAIN && driveMode != ICarFunction.DRIVEMODE_COPILOT)
            return;
        if (ICarFunction.DRIVEMODE_POSITIONING == getDriveMode()) {
            try {
                Log.i(TAG, "autoSetDriveMode cachedDriveMode = " + driveMode);
                mCachedWakeUpDoa = driveMode;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean isAutoSetDriveMode() {
        return mDriveMode == ICarFunction.DRIVEMODE_POSITIONING;
    }

    private void saveWakeupData(AIResult result) {
        if (!TextUtils.isEmpty(mParams.getSaveAudioPath())) {
            try {
                JSONObject jsonObject = new JSONObject(result.getResultObject().toString());
                sendMessage(new Message(Message.MSG_WAKEUP, jsonObject.optString("wakeupWord")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void doSaveWakeupData(String result) {
        Log.d(TAG, "wake up result = " + result);
        String saveAudioPath = mParams.getSaveAudioPath();
        if (!TextUtils.isEmpty(saveAudioPath) && mDmaspQueue != null && !mDmaspQueue.isEmpty()) {
            FileSaveUtil fileSaveUtil = new FileSaveUtil();
            fileSaveUtil.init(saveAudioPath);
            fileSaveUtil.prepare("Dmasp-" + result.replaceAll(" ", ""));

            while (!mDmaspQueue.isEmpty()) {
                byte[] data = mDmaspQueue.poll();
                if (data != null && data.length > 0) fileSaveUtil.feedTypeCustom(data, data.length);
            }
            fileSaveUtil.close();
        }
    }
}
