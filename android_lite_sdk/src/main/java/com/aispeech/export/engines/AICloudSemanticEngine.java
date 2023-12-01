package com.aispeech.export.engines;

import com.aispeech.export.ProductContext;
import com.aispeech.export.SkillIntent;
import com.aispeech.export.Vocab;
import com.aispeech.export.config.AICloudSemanticConfig;
import com.aispeech.export.intent.AICloudSemanticIntent;
import com.aispeech.export.listeners.AIASRListener;
import com.aispeech.export.listeners.AIUpdateListener;

import org.json.JSONArray;

/**
 * 云端识别引擎(含语义)
 * 不再维护,推荐使用 AICloudDMEngine
 */
@Deprecated
public class AICloudSemanticEngine {

    private static final String TAG = "AICloudSemanticEngine";
    private com.aispeech.export.engines2.AICloudSemanticEngine cloudSemanticEngine;

    private AICloudSemanticEngine() {
        cloudSemanticEngine = com.aispeech.export.engines2.AICloudSemanticEngine.createInstance();
    }

    /**
     * 创建实例
     *
     * @return AICloudSemanticEngine 实例
     */
    public static AICloudSemanticEngine createInstance() {
        return new AICloudSemanticEngine();
    }

    /**
     * 初始化云端识别引擎。
     *
     * @param config   初始化参数 {@link AICloudSemanticConfig}
     * @param listener 引擎回调 {@link AIASRListener}
     */
    public void init(AICloudSemanticConfig config, AIASRListener listener) {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.init(config, listener);
        }
    }


    /**
     * 启动引擎
     *
     * @param intent 启动参数
     */
    public void start(AICloudSemanticIntent intent) {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.start(intent);
        }
    }

    /**
     * 主动结束语义
     */
    public void stop() {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.stop();
        }
    }


    /**
     * 停止录音，等待识别结果
     */
    public void stopRecording() {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.stopRecording();
        }
    }


    /**
     * 取消本次识别操作
     */
    public void cancel() {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.cancel();
        }
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     */
    public void feedData(byte[] data) {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.feedData(data);
        }
    }

    /**
     * 销毁云端识别引擎
     */
    public void destroy() {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.destroy();
        }
    }


    /**
     * 更新技能配置，调整技能优先级
     * 须在start启动成功后可以调用
     *
     * @param skills 技能排序列表 ["skillId1" , ""skillId2"","skillId3"]
     * @see #updateProductContext(AIUpdateListener, ProductContext)
     * @deprecated 已废弃, 使用下面的接口
     */
    @Deprecated
    public void updateSkillPriority(JSONArray skills) {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.updateSkillPriority(skills);
        }
    }


    /**
     * 更新产品配置
     *
     * @param listener {@link AIUpdateListener}
     * @param context  {@link ProductContext}
     *                 usage: http://car.aispeech.com/duilite/docs/duilite/yu-yin-shi-bie/56-yun-duan-dui-hua.html
     */
    public void updateProductContext(AIUpdateListener listener, ProductContext context) {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.updateProductContext(listener, context);
        }
    }


    /**
     * 上传联系人接口
     *
     * @param addOrDelete 删除还是增加
     * @param data        联系人数据 ["张三","李四"]
     * @see #updateVocabs(AIUpdateListener, Vocab...)
     * @deprecated 已废弃, 使用下面的新接口替换
     */
    @Deprecated
    public void updateContact(boolean addOrDelete, JSONArray data) {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.updateContact(addOrDelete, data);
        }
    }

    /**
     * 更新词库接口
     * <p>
     * 更新指定词库的词条。
     *
     * @param listener {@link AIUpdateListener} 上传词库结果回调监听
     * @param vocabs   {@link Vocab} 需要更新的词库列表
     */
    public void updateVocabs(AIUpdateListener listener, Vocab... vocabs) {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.updateVocabs(listener, vocabs);
        }
    }

    /**
     * 主动触发意图
     *
     * @param intent {@link SkillIntent}
     */
    public void triggerIntent(SkillIntent intent) {
        if (cloudSemanticEngine != null) {
            cloudSemanticEngine.triggerIntent(intent);
        }
    }
}
