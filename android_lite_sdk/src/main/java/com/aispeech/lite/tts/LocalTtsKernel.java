package com.aispeech.lite.tts;

import com.aispeech.AIError;
import com.aispeech.common.AIConstant;
import com.aispeech.common.Log;
import com.aispeech.common.TextSplitter;
import com.aispeech.kernel.Cntts;
import com.aispeech.lite.BaseKernel;
import com.aispeech.lite.config.LocalTtsConfig;
import com.aispeech.lite.message.Message;
import com.aispeech.lite.param.LocalTtsParams;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by yu on 2018/5/7.
 */

public class LocalTtsKernel extends BaseKernel {
    public static final String TAG = "LocalTtsKernel";
    private Cntts mCntts;
    private MyCntts_CallbackImpl myCntts_callback;
    private TtsKernelListener mListener;
    private volatile boolean mIsCancelled = false;

    private int mLastSize = 0;
    List<String> spiltWords = new ArrayList<>();
    public static final int TEXT_SPILT_UNIT = 50;

    public LocalTtsKernel(TtsKernelListener listener) {
        super(TAG, listener);
        mListener = listener;
    }

    @Override
    public void run() {
        super.run();
        Message message;
        while ((message = waitMessage()) != null) {
            boolean isReleased = false;
            switch (message.mId) {
                case Message.MSG_NEW:
                    mCntts = new Cntts();
                    myCntts_callback = new MyCntts_CallbackImpl();
                    LocalTtsConfig localTtsConfig = (LocalTtsConfig) message.mObject;
                    boolean state = mCntts.init(localTtsConfig.toJson().toString(), myCntts_callback);
                    int status = AIConstant.OPT_FAILED;
                    if (!state) {
                        Log.e(TAG, "引擎初始化失败,请检查资源文件是否在指定路径下！");
                        status = AIConstant.OPT_FAILED;
                    } else {
                        Log.d(TAG, "引擎初始化成功");
                        status = AIConstant.OPT_SUCCESS;
                    }
                    mListener.onInit(status);
                    break;
                case Message.MSG_START:
                    LocalTtsParams param = (LocalTtsParams) message.mObject;
                    String paramStr = param.toJson().toString();
                    Log.d(TAG, "cntts param: " + paramStr);
                    if (mCntts != null) {
                        boolean ret = mCntts.start(paramStr);
                        if (!ret) {
                            mQueue.put(new Message(Message.MSG_ERROR, new AIError(AIError.ERR_AI_ENGINE, AIError.ERR_DESCRIPTION_AI_ENGINE)));
                            break;
                        }
                        String refText = param.getRefText();
                        Log.d(TAG, "refText : " + refText);
                        mIsCancelled = false;
                        mLastSize = 0;
                        if (param.isUseSSML()) {
                            if (null != spiltWords && spiltWords.size() > 0) {
                                spiltWords.clear();
                            }
                            if (spiltWords != null) spiltWords.add(refText);
                        } else {
                            spiltWords = TextSplitter.spiltTextByPunctuation(refText, TEXT_SPILT_UNIT);
                        }
                        if (null != spiltWords && spiltWords.size() > 0) {
                            if (mListener != null) {
                                mListener.onStartFeedData();
                            }
                            Log.d(TAG, "feed cntts = " + spiltWords.get(0));
                            mCntts.feed(spiltWords.get(0));
                        }
                    }
                    break;
                case Message.MSG_SET:
                    String setParam = (String) message.mObject;
                    Log.d(TAG, "set dynamic param is: " + setParam);
                    if (mCntts != null) {
                        mCntts.set(setParam);
                    }
                    break;
                case Message.MSG_RELEASE:
                    // 销毁引擎
                    mLastSize = 0;
                    if (spiltWords != null) {
                        spiltWords.clear();
                    }

                    if (mCntts != null) {
                        mCntts.release();
                        mCntts = null;
                    }
                    if (myCntts_callback != null) {
                        myCntts_callback = null;
                    }
                    isReleased = true;
                    break;
                case Message.MSG_ERROR:
                    mListener.onError((AIError) message.mObject);
                    break;
                case Message.MSG_ASYNC:
                    if (mLastSize < spiltWords.size()) {
                        Log.d(TAG, "feed cntts = " + spiltWords.get(mLastSize));
                        mCntts.feed(spiltWords.get(mLastSize));
                    }
                    break;
                case Message.MSG_CANCEL:
                    // 重置spiltWords
                    mLastSize = 0;
                    if (spiltWords != null) {
                        spiltWords.clear();
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

    @Override
    public synchronized void cancelKernel() {
        super.cancelKernel();
        Log.d(TAG, "cancelKernel");
        mIsCancelled = true;
    }


    private class MyCntts_CallbackImpl extends Cntts.cntts_callback {
        @Override
        public int run(int dataType, byte[] retData, int size) {
            if (mIsCancelled) { //cancel tts
                return -1;
            }
            if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_BIN) {
                if (size == 0) {
                    Log.d(TAG, "run:size " + size);

                    ++mLastSize;
                    if (mLastSize < spiltWords.size()) {
                        mQueue.put(new Message(Message.MSG_ASYNC));
                    } else {
                        mLastSize = 0;
                        mListener.onBufferReceived(retData, size, AIConstant.AIENGINE_MESSAGE_TYPE_BIN);
                    }
                } else {
                    mListener.onBufferReceived(retData, size, AIConstant.AIENGINE_MESSAGE_TYPE_BIN);
                }
            } else if (dataType == AIConstant.AIENGINE_MESSAGE_TYPE_JSON) {
                if (size != 0) {
                    mListener.onTimestampReceived(retData, size);
                }

            }
            if (size == 0) {
                Log.d(TAG, "size= 0");
            }

            return 0;
        }
    }


}
