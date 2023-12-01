package com.aispeech.lite.dm.update;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import com.aispeech.auth.AIProfile;
import com.aispeech.export.ProductContext;
import com.aispeech.export.SkillContext;
import com.aispeech.export.Vocab;
import com.aispeech.export.listeners.AIUpdateListener;
import com.aispeech.lite.AISpeech;

/**
 * 用户词库更新
 *
 * @author hehr
 */
public class UpdaterImpl implements IUpdater {

    private static final String TAG = "UpdaterImpl";

    private Context mContext;

    private CInfoV1Impl cInfoV1;

    private CInfoV2Impl cInfoV2;

    private AIProfile profile = AISpeech.getProfile();

    private UpdaterImpl(String v1Host, String webSocketHost, String aliasKey, boolean isFullDuplex) {

        mContext = AISpeech.getContext();

        mHandler = createHandler(mContext);

        cInfoV1 = new CInfoV1Impl(v1Host, profile.getDeviceName(), profile.getDeviceSecret(), profile.getProductId(), aliasKey, new CInfoListener() {
            @Override
            public void onUploadSuccess() {
                notifySuccess();
            }

            @Override
            public void onUploadFailed() {
                notifyFailed();
            }
        });

        cInfoV2 = new CInfoV2Impl(webSocketHost, profile.getDeviceName(), profile.getDeviceSecret(), profile.getProductId(), aliasKey, isFullDuplex, new CInfoListener() {
            @Override
            public void onUploadSuccess() {
                notifySuccess();
            }

            @Override
            public void onUploadFailed() {
                notifyFailed();
            }
        });
    }

    private UpdaterImpl(Builder builder) {
        this(builder.getV1Host(), builder.getWebSocketHost(), builder.getAliasKey(), builder.isFullDuplex());
    }

    public static class Builder {

        /**
         * cinfo v1 服务地址
         */
        private String v1Host;
        /**
         * websocket 服务地址
         */
        private String webSocketHost;
        /**
         * 产品分支信息
         */
        private String aliasKey;

        /**
         * 是否开启全双工配置
         */
        private boolean isFullDuplex;

        public String getV1Host() {
            return v1Host;
        }

        public Builder setV1Host(String v1Host) {
            this.v1Host = v1Host;
            return this;
        }

        public String getWebSocketHost() {
            return webSocketHost;
        }

        public Builder setWebSocketHost(String webSocketHost) {
            this.webSocketHost = webSocketHost;
            return this;
        }

        public String getAliasKey() {
            return aliasKey;
        }

        public Builder setAliasKey(String aliasKey) {
            this.aliasKey = aliasKey;
            return this;
        }

        public boolean isFullDuplex() {
            return isFullDuplex;
        }

        public Builder setFullDuplex(boolean fullDuplex) {
            isFullDuplex = fullDuplex;
            return this;
        }

        public UpdaterImpl build() {
            return new UpdaterImpl(this);
        }
    }

    private Handler mHandler;

    /**
     * 创建主线程的handler
     *
     * @param context
     * @return
     */
    private Handler createHandler(Context context) {
        return new Handler(context.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                switch (msg.what) {
                    case WHAT_UPDATE_FAILED:
                        if (mListener != null) {
                            mListener.failed();
                        }
                        break;
                    case WHAT_UPDATE_SUCCESS:
                        if (mListener != null) {
                            mListener.success();
                        }
                        break;
                    default:
                        break;
                }

            }
        };
    }

    /**
     * callback listener
     */
    private AIUpdateListener mListener;

    /**
     * 数据上传失败
     */
    private static final int WHAT_UPDATE_FAILED = -1;
    /**
     * 数据上传成功
     */
    private static final int WHAT_UPDATE_SUCCESS = 0;


    private void notifySuccess() {
        if (mHandler != null) {
            Message.obtain(mHandler, WHAT_UPDATE_SUCCESS, null).sendToTarget();
        }
    }

    private void notifyFailed() {
        if (mHandler != null) {
            Message.obtain(mHandler, WHAT_UPDATE_FAILED, null).sendToTarget();
        }
    }


    /**
     * 上传词库
     *
     * @param vocabs   {@link Vocab}
     * @param listener {@link AIUpdateListener}
     */
    @Override
    public void updateVocabs(AIUpdateListener listener, Vocab... vocabs) {
        mListener = listener;
        if (cInfoV1 != null) {
            cInfoV1.uploadVocabs(vocabs);
        }
    }

    /**
     * 更新产品配置
     *
     * @param listener {@link AIUpdateListener}
     * @param context  {@link ProductContext}
     */
    @Override
    public void updateProductContext(AIUpdateListener listener, ProductContext context) {
        mListener = listener;
        if (cInfoV2 != null) {
            cInfoV2.uploadProductContext(context);
        }
    }

    /**
     * 更新技能配置
     *
     * @param listener {@link AIUpdateListener}
     * @param context  {@link SkillContext}
     */
    @Override
    public void updateSkillContext(AIUpdateListener listener, SkillContext context) {
        mListener = listener;
        if (cInfoV2 != null) {
            cInfoV2.uploadSkillContext(context);
        }
    }

}
