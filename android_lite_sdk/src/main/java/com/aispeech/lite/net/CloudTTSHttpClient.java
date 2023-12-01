package com.aispeech.lite.net;

import android.text.TextUtils;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.AITimer;
import com.aispeech.common.AuthUtil;
import com.aispeech.common.ByteConvertUtil;
import com.aispeech.common.Log;
import com.aispeech.common.NetworkUtil;
import com.aispeech.lite.AISpeech;
import com.aispeech.lite.param.CloudTtsParams;
import com.aispeech.net.NetProxy;
import com.aispeech.net.http.HttpCallback;
import com.aispeech.net.http.IHttp;
import com.aispeech.net.http.IResponse;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.util.TimerTask;
import java.util.UUID;

import ai.dui.sdk.core.codec.Base64;

/**
 * @auther wuwei
 */
public class CloudTTSHttpClient {
    private static final String TAG = "CloudTTSHttpClient";
    private CloudTTSHttpClientListener mListener;
    private boolean isOpened = false;
    private boolean mCancelFlag = false;
    private String url;
    private String deviceSecret;
    private CloudTtsParams mCloudTtsParams;
    private static final byte ASCII_A = 65;
    private static final byte ASCII_S = 83;

    public synchronized void startRequest(CloudTtsParams params, CloudTTSHttpClientListener listener) {
        this.mListener = listener;
        mCloudTtsParams = params;
        isOpened = false;
        mCancelFlag = false;
        if (NetworkUtil.isNetworkConnected(AISpeech.getContext())) {
            if (!TextUtils.isEmpty(params.getSpeakUrl())) {
                url = params.getSpeakUrl();
                NetProxy.getHttp().get(url, new MyHttpCallback());
            } else {
                url = getUrl(params, deviceSecret);
                NetProxy.getHttp().post(url, params.getTtsJSON(), new MyHttpCallback());
            }
            Log.d(TAG, "CTTS.START " + url + "  params:" + params.getTtsJSON());
            startWaitTimerTask();
        } else {
            Log.d(TAG, "CTTS.ERROR: " + "网络连接错误");
            if (mListener != null && !mCancelFlag) {
                mListener.onError(new AIError(AIError.ERR_NETWORK, AIError.ERR_DESCRIPTION_ERR_NETWORK));
            }
        }
    }

    public synchronized void closeHttp() {
        cancelWaitTimerTask();
        NetProxy.getHttp().cancel();
        Log.d(TAG, "closeHttp");
        mCancelFlag = true;
    }

    public synchronized void destroy() {
        Log.d(TAG, "destroy");
        closeHttp();
        if (mListener != null) {
            mListener = null;
        }
    }

    public String getUrl() {
        return url;
    }

    private class MyHttpCallback implements HttpCallback {

        @Override
        public void onFailure(IHttp iHttp, IOException e) {
            Log.d(TAG, "CTTS.ERROR onFailure: " + e.toString());
            AIError aiError;
            if (e instanceof UnknownHostException) {
                aiError = new AIError(AIError.ERR_DNS, AIError.ERR_DESCRIPTION_ERR_DNS);
            } else {
                aiError = new AIError(AIError.ERR_NETWORK, AIError.ERR_DESCRIPTION_ERR_NETWORK);
            }

            /*
             * 2021/6/24 [低概率性的网络正常，但是依然会提示 onError（网络错误）的问题。]
             *
             * 正常情况下，在调用 cancel 方法以后，会回调该 onFailure 方法 ，即会抛出异常
             * Socket closed 、Canceled , 通过变量 mCancelFlag 的状态来标识是否将异常信息抛到上层，当
             * mCancelFlag（表示取消中），则不会向上抛。
             *
             * 但是有一种低概率的情况，如：
             * 当 cancel() 之后立即播放一个新的文本，这时 mCancelFlag 已经重置为 false(意味着 onFailure
             * 回调需要向上抛出)，接着之前cancel 的 onFailure 才回来， 于是就有存在低概率性的网络正常，
             * 但是依然会提示 onError（网络错误）的问题。
             *
             * 因为在 closeHttp() 被调用的地方 TtsProcessor 层对 MSG_STOP 、MSG_ERROR ， 已经做了做播放完成
             * 及错误的回调；因此，应该对于 "Socket closed"、"Canceled" 之后的 onFailure 回调进行过滤。
             */
            String lMsg = e.getLocalizedMessage();
            if ("Socket closed".equals(lMsg) || "Canceled".equals(lMsg)) {
                mCancelFlag = false;
                return;
            }

            if (mListener != null && !mCancelFlag) {
                mListener.onError(aiError);
            }
            mCancelFlag = false;
        }


        @Override
        public void onResponse(IHttp iHttp, IResponse response) throws IOException {
            if (response.isSuccessful()) {
                if (!isOpened) {
                    isOpened = true;
                }
                cancelWaitTimerTask();
                String emotion = response.getResponse().header("speakingStyle");
                String emotionOriginBase64 = response.getResponse().header("emotionStyle");
                String emotionOrigin = "";
                if (!TextUtils.isEmpty(emotionOriginBase64)) {
                    emotionOrigin = Base64.decodeStr(emotionOriginBase64);
                }
                if (mListener != null && !TextUtils.isEmpty(emotion)) {
                    mListener.onEmotion(emotion, emotionOrigin);
                }

                InputStream is = response.byteStream();
                try {
                    splitDataFromServer(is);
                } catch (Exception e) {
                    Log.w(TAG, "onResponse: " + e.toString());
                    e.printStackTrace();
                }
            } else {
                Log.d(TAG, "CTTS.ERROR response code : " + response.code());
                if (mListener != null && !mCancelFlag) {
                    if (response.code() == 401) {
                        mListener.onError(new AIError(AIError.ERR_DEVICE_ID_CONFLICT_TTS, AIError.ERR_DESCRIPTION_DEVICE_ID_CONFLICT));
                    } else if (response.code() == 403) {
                        mListener.onError(new AIError(AIError.ERR_403_FORBIDDEN, AIError.ERR_DESCRIPTION_403_FORBIDDEN));
                    } else if (response.code() == 400) {
                        //参数异常
                        String body = response.string();
                        Log.i(TAG, "response err, errcode = 400, errorbody = " + body);

                        boolean hasSpecialErr = false;
                        try {
                            JSONObject jb = new JSONObject(body);
                            if (jb != null) {
                                String errId = jb.getString("errId");
                                if ("010361".equals(errId)) {
                                    Log.i(TAG, "声纹服务运行出错");
                                    hasSpecialErr = true;
                                    mListener.onError(new AIError(AIError.ERR_VOICE_SERVER_TTS, AIError.ERR_DESCRIPTION_TTS_VOICE_SERVER));
                                } else if ("011006".equals(errId)) {
                                    Log.i(TAG, "产品ID和voiceId无法匹配");
                                    hasSpecialErr = true;
                                    mListener.onError(new AIError(AIError.ERR_TTS_PRODUCT_ID, AIError.ERR_DESCRIPTION_TTS_PRODUCT_ID));
                                } else if ("011000".equals(errId)) {
                                    Log.i(TAG, "请求参数错误");
                                    hasSpecialErr = true;
                                    mListener.onError(new AIError(AIError.ERR_TTS_PARAMETERS, AIError.ERR_DESCRIPTION_TTS_PARAMETERS));
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        if (!hasSpecialErr) {
                            mListener.onError(new AIError(AIError.ERR_NETWORK, AIError.ERR_DESCRIPTION_ERR_NETWORK));
                        }

                    } else {
                        mListener.onError(new AIError(AIError.ERR_NETWORK, AIError.ERR_DESCRIPTION_ERR_NETWORK));
                    }
                }
                mCancelFlag = false;
            }
        }

        /**
         * 对云端的信息进行拆分向外抛出，单元测试中TestReturnPhone.java做数据的模拟测试，逻辑变更之后需要同步修改测试
         *
         * @param is 输入流
         * @throws Exception 抛出的异常
         */
        private void splitDataFromServer(InputStream is) throws Exception {
            byte[] buf = new byte[1024];
            int ret;
            if (is != null) {
                if (mCloudTtsParams.isReturnPhone()) {
                    int currentHead = 0;//包头所在位置
                    int headLength = 0;//包头长度
                    int pakType = 0;//当前数据包类型
                    int packLength = 0;//包数据长度
                    byte[] contents = null;//包内容
                    byte[] lastRemainContents = null;//如果包头信息被分割到两个单独包里面，进行数据拼接
                    int currentContentLength = 0;
                    //当前包是否处理完成
                    boolean currentDataFinished = true;//当前数据包处理完成
                    boolean currentRetFinished = true;//当前流处理完成
                    int lastRet = 0;
                    boolean breakCurrentLoop = false;
                    while ((ret = is.read(buf)) != -1) {
                        for (int i = 0; i < ret; i++) {
                            if (buf[i] == ASCII_A || buf[i] == ASCII_S) {
                                Log.v(TAG, "splitDataFromServer:head location " + i);
                            }
                        }
                        Log.d(TAG, "response CTTS data length with returePhone: " + ret + " currentHead " + currentHead + new String(buf));
                        //当前单次流是否完成
                        currentRetFinished = false;
                        //上一轮有被裁掉的数据，拼装在本轮数据的开头
                        if (breakCurrentLoop) {
                            break;
                        }
                        if (lastRemainContents != null) {
                            //如果数据头被分割，则证明上次数据已经被处理完成，不需要在进行head移动，所有数据置零
                            byte[] clonedData = buf.clone();
                            buf = new byte[lastRemainContents.length + clonedData.length];
                            System.arraycopy(lastRemainContents, 0, buf, 0, lastRemainContents.length);
                            System.arraycopy(clonedData, 0, buf, lastRemainContents.length, clonedData.length);
                            ret += lastRemainContents.length;
                            currentHead = 0;
                            lastRet = 0;
                            headLength = 0;
                            packLength = 0;
                            lastRemainContents = null;
                            Log.v(TAG, "splitDataFromServer data from last: currentHead " + currentHead + " headLength " + headLength + " packLength " + packLength + " ret " + ret);
                        }
                        //上一轮数据处理完成，更新头的位置
                        if (currentDataFinished) {
                            currentHead += -lastRet + headLength + packLength;
                            Log.v(TAG, "splitDataFromServer: change head with datafinished packLength " + packLength + " lastRet " + lastRet + " currentHead " + currentHead + " currentDataFinished " + currentDataFinished);
                        } else {
                            currentHead -= lastRet;
                            Log.v(TAG, "splitDataFromServer: orign1  change head with ret finished but data not finished packLength " + packLength + " lastRet " + lastRet + " currentHead " + currentHead);
                        }
                        boolean firstInRet = true;
                        while (!currentRetFinished) {
                            Log.v(TAG, "splitDataFromServer:currentHead " + currentHead + " currentDataFinished " + currentDataFinished + " firstInRet " + firstInRet);
                            if (!firstInRet) {
                                currentHead += headLength + packLength;
                            }
                            firstInRet = false;
                            if (!currentDataFinished) {
                                Log.v(TAG, "splitDataFromServer: 11 not end pack, continue");
                            } else if (currentHead <= ret - 8 && currentDataFinished) {
                                headLength = buf[currentHead + 2];
                                pakType = buf[currentHead + 3];
                                packLength = ByteConvertUtil.getShort(new byte[]{buf[currentHead + 4], buf[currentHead + 5]}, true);
                                currentDataFinished = false;
                                if (!(buf[currentHead] == ASCII_A && buf[currentHead + 1] == ASCII_S)) {
                                    Log.w(TAG, "splitDataFromServer: pack error, current is no start with AS");
                                    //mListener.onError(new AIError(AIError.ERR_TTS_NO_SUPPORT_PHONEME, AIError.ERR_DESCRIPTION_TTS_NO_SUPPORT_PHONEME));
                                    breakCurrentLoop = true;
                                    break;
                                }
                                contents = new byte[packLength];
                                currentContentLength = 0;
                                Log.v(TAG, "splitDataFromServer: pack head is headLength " + headLength + " pakType " + pakType + " packLength " + packLength + " currentDataFinished " + currentDataFinished);
                            } else if (currentHead <= ret && currentDataFinished) {
                                //包头被裁掉了，需要将这些信息拼装在下一轮，并将指针指向数据末尾
                                lastRemainContents = new byte[ret - currentHead];
                                System.arraycopy(buf, currentHead, lastRemainContents, 0, lastRemainContents.length);
                                currentRetFinished = true;
                                Log.v(TAG, "splitDataFromServer: remain last info currentRetFinished " + currentRetFinished + " lastRemainContents " + lastRemainContents.length);
                            } else {
                                Log.v(TAG, "splitDataFromServer: may be lost some data");
                            }
                            while (!currentDataFinished) {
                                if (currentHead + headLength + packLength <= ret) {
                                    Log.v(TAG, "splitDataFromServer222: currentHead " + currentHead + " headLength " + headLength + " packLength " + packLength + " ret " + ret + " currentContentLength " + currentContentLength);
                                    if (currentContentLength != 0) {
                                        System.arraycopy(buf, 0, contents, currentContentLength, contents.length - currentContentLength);
                                    } else {
                                        System.arraycopy(buf, currentHead + headLength, contents, 0, packLength);
                                    }
                                    if (pakType == 0) {
                                        android.util.Log.v(TAG, "splitDataFromServer: current head " + currentHead + " lastRet " + lastRet + " ret " + ret);
                                        if (contents.length != 0) {
                                            sendSynthesizedData(contents, contents.length, AIConstant.AIENGINE_MESSAGE_TYPE_BIN);
                                        }
                                    } else if (pakType == 1) {
                                        Log.v(TAG, "splitDataFromServer: current head  " + currentHead + " lastRet " + lastRet + " ret " + ret + " \nstring " + new String(contents));
                                        sendSynthesizedData(contents, contents.length, AIConstant.AIENGINE_MESSAGE_TYPE_JSON);
                                    }
                                    contents = null;
                                    currentContentLength = 0;
                                    currentDataFinished = true;
                                    if (currentHead + headLength + packLength == ret) {
                                        currentRetFinished = true;
                                    }
                                    Log.v(TAG, "splitDataFromServer333: currentHead " + currentHead + " headLength " + headLength + " packLength " + packLength + " ret " + ret + " currentContentLength " + currentContentLength + " lastRet " + lastRet);
                                } else {
                                    Log.v(TAG, "splitDataFromServer444: currentHead " + currentHead + " headLength " + headLength + " packLength " + packLength + " ret " + ret + " currentContentLength " + currentContentLength + " contents " + contents.length);
                                    if (currentContentLength != 0) {
                                        System.arraycopy(buf, 0, contents, currentContentLength, ret);
                                        currentContentLength += ret;
                                    } else {
                                        int currentDataLength = ret - currentHead - headLength;
                                        System.arraycopy(buf, currentHead + headLength, contents, currentContentLength, currentDataLength);
                                        currentContentLength += currentDataLength;
                                    }
                                    currentRetFinished = true;
                                    Log.v(TAG, "splitDataFromServer555: currentHead " + currentHead + " headLength " + headLength + " packLength " + packLength + " ret " + ret + " currentContentLength " + currentContentLength + " contents " + contents.length + " currentRetFinished " + currentRetFinished + " currentDataFinished " + currentDataFinished);
                                    break;
                                }
                            }
                        }
                        lastRet = ret;
                    }
                } else {
                    while ((ret = is.read(buf)) != -1) {
                        Log.d(TAG, "response CTTS data length: " + ret);
                        sendSynthesizedData(buf, ret, AIConstant.AIENGINE_MESSAGE_TYPE_BIN);
                    }
                }
                Log.d(TAG, "response CTTS data length: " + 0);
                sendSynthesizedData(buf, 0, AIConstant.AIENGINE_MESSAGE_TYPE_BIN);
            }
        }
    }

    public String getDeviceSecret() {
        return deviceSecret;
    }

    public void setDeviceSecret(String deviceSecret) {
        this.deviceSecret = deviceSecret;
    }

    /**
     * 获取云端合成url
     *
     * @param cloudTtsParams
     * @return
     */
    public static String getUrl(CloudTtsParams cloudTtsParams, String deviceSecret) {
        String timestamp = System.currentTimeMillis() + "";
        String nonce = UUID.randomUUID() + "";
        String sig = AuthUtil.getSignature(cloudTtsParams.getDeviceName() + nonce + cloudTtsParams.getProductId() + timestamp, deviceSecret);
        StringBuilder url = new StringBuilder();
        url.append(cloudTtsParams.getServer()).append("?productId=").append(cloudTtsParams.getProductId()).append("&apikey=").append(cloudTtsParams.getServerApiKey()).append("&deviceName=").append(cloudTtsParams.getDeviceName()).append("&timestamp=").append(timestamp).append("&nonce=").append(nonce).append("&sig=").append(sig).append("&voiceId=").append(cloudTtsParams.getSpeaker());
        return url.toString();
    }


    private WaitTimerTask mWaitTimerTask = null;

    protected synchronized void startWaitTimerTask() {
        cancelWaitTimerTask();
        mWaitTimerTask = new WaitTimerTask();
        long delay = 3000;
        if (mCloudTtsParams != null && mCloudTtsParams.getWaitingTimeout() > 0) {
            delay = mCloudTtsParams.getWaitingTimeout();
        }
        try {
            AITimer.getInstance().schedule(mWaitTimerTask, delay);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    protected void sendSynthesizedData(byte[] data, int size, int dataType) {
        if (mListener != null && !mCancelFlag) {
            mListener.onBufferReceived(data, size, dataType);
        }
    }

    protected synchronized void cancelWaitTimerTask() {
        if (mWaitTimerTask != null) {
            mWaitTimerTask.cancel();
            mWaitTimerTask = null;
        }
    }

    class WaitTimerTask extends TimerTask {

        @Override
        public void run() {
            if (mListener != null && !isOpened) {
                Log.e(TAG, "connect timeout");
                if (!mCancelFlag) {
                    mListener.onError(new AIError(AIError.ERR_CONNECT_TIMEOUT, AIError.ERR_DESCRIPTION_CONNECT_TIMEOUT));
                }
                mCancelFlag = false;
                closeHttp();
            }
        }
    }
}
