package com.aispeech.export.engines;

import com.aispeech.export.config.VprintConfig;
import com.aispeech.export.intent.VprintIntent;
import com.aispeech.export.listeners.AILocalVprintListener;
import com.aispeech.kernel.Utils;
import com.aispeech.kernel.Vprint;

/**
 * Created by wuwei on 2019/5/14.
 * @deprecated 废弃，参考{@link com.aispeech.export.engines2.AILocalVprintEngine}
 */
@Deprecated
public class AILocalVprintEngine {
    public static final String TAG = "AILocalVprintEngine";
    private static AILocalVprintEngine mInstance = null;
    private com.aispeech.export.engines2.AILocalVprintEngine mAILocalVprintEngine2;

    private AILocalVprintEngine() {
        mAILocalVprintEngine2 = com.aispeech.export.engines2.AILocalVprintEngine.getInstance();
    }


    public static boolean checkLibValid() {
        return Vprint.isVprintSoValid() &&
                Utils.isUtilsSoValid();
    }


    public static synchronized AILocalVprintEngine getInstance() { //懒汉式单例
        if (mInstance == null) {
            mInstance = new AILocalVprintEngine();
        }
        return mInstance;
    }


    /**
     * 获取当前声纹模型中的注册信息，需要在init成功后调用生效。
     */
    public void queryModel() {
        if (mAILocalVprintEngine2 != null) {
            mAILocalVprintEngine2.queryModel();
        }
    }


    /**
     * 初始化声纹引擎
     *
     * @param config              声纹配置
     * @param localVprintListener 声纹回调
     */
    public void init(VprintConfig config, AILocalVprintListener localVprintListener) {
        if (mAILocalVprintEngine2 != null) {
            mAILocalVprintEngine2.init(config.transferConfig(), localVprintListener);
        }
    }


    /**
     * 启动声纹引擎
     *
     * @param intent 声纹Intent
     */
    public void start(VprintIntent intent) {
        if (mAILocalVprintEngine2 != null) {
            mAILocalVprintEngine2.start(intent.transferIntent());
        }
    }


    /**
     * 传入事件信息，比如唤醒JSON字符串
     *
     * @param event 事件信息
     * @deprecated  请替代使用 feedData(int dataType, byte[] data, int size)
     */
    public void notifyEvent(String event) {
        if (mAILocalVprintEngine2 != null) {
            mAILocalVprintEngine2.notifyEvent(event);
        }
    }

    /**
     * 获取当前声纹模式
     *
     * @return 当前声纹模式
     */
    public VprintIntent.Action getAction() {
        if (mAILocalVprintEngine2 != null && mAILocalVprintEngine2.getAction() != null) {
            return VprintIntent.Action.getActionByValue(mAILocalVprintEngine2.getAction().getValue());
        }
        return null;
    }

    /**
     * 传入数据
     *
     * @param data 音频数据流
     * @param size 数据大小
     * @deprecated 请替代使用 feedData(int dataType, byte[] data, int size)
     */
    public void feedData(byte[] data, int size) {
        if (mAILocalVprintEngine2 != null) {
            mAILocalVprintEngine2.feedData(data, size);
        }
    }

    /**
     * 传入数据
     *
     * @param dataType 数据类型
     * @param data     数据流
     * @param size     数据流大小
     */
    public void feedData(int dataType, byte[] data, int size) {
        if (mAILocalVprintEngine2 != null) {
            mAILocalVprintEngine2.feedData(dataType, data, size);
        }
    }

    /**
     * 停止声纹引擎，该接口只在通用声纹模式下需要调用，唤醒+声纹不需要调用
     */
    public void stop() {
        if (mAILocalVprintEngine2 != null) {
            mAILocalVprintEngine2.stop();
        }
    }


    /**
     * 取消声纹引擎，当不再接受内部消息时或切换模式前调用
     */
    public void cancel() {
        if (mAILocalVprintEngine2 != null) {
            mAILocalVprintEngine2.cancel();
        }
    }

    /**
     * 销毁声纹引擎
     */
    public void destroy() {
        if (mAILocalVprintEngine2 != null) {
            mAILocalVprintEngine2.destroy();
            mAILocalVprintEngine2 = null;
        }
    }
}
