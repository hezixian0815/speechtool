package com.aispeech.export.engines;

import com.aispeech.export.config.AICloudVoiceCopyConfig;
import com.aispeech.export.listeners.AICloudVoiceCopyListener;
import com.aispeech.lite.voicecopy.CloudVoiceCopyKernel;

import java.util.ArrayList;

/**
 * 声音复刻引擎
 * @deprecated 推荐使用dca sdk中的声音复刻
 */
@Deprecated
public class AICloudVoiceCopyEngine {

    private static AICloudVoiceCopyEngine instance;
    private CloudVoiceCopyKernel voiceCopyKernel;

    private AICloudVoiceCopyEngine() {
        voiceCopyKernel = new CloudVoiceCopyKernel();
    }

    public static synchronized AICloudVoiceCopyEngine getInstance() {
        if (instance == null) {
            instance = new AICloudVoiceCopyEngine();
        }
        return instance;
    }

    /**
     * 初始化声音复刻引擎
     *
     * @param config   声音复刻配置信息
     * @param listener 声音复刻监听接口
     */
    public synchronized void init(AICloudVoiceCopyConfig config
            , AICloudVoiceCopyListener listener) {
        if (voiceCopyKernel != null) {
            voiceCopyKernel.init(config, listener);
        }
    }

    /**
     * 获取复刻对应的录音文本
     */
    public void getText() {
        if (voiceCopyKernel != null) {
            voiceCopyKernel.getText();
        }
    }

    /**
     * 上传音频（音频检测）
     *
     * @param textId   文本ID，即录音对应的文本编号，必填。如："fda77fe181ce4072bc2e75c9751f71db-003":
     *                 "但愿花常好，月常圆！"；textId = "fda77fe181ce4072bc2e75c9751f71db-003"。
     *                 注意：复刻所需的录音文本由思必驰提供，不同的客户对接可能有差异。
     * @param gender   MALE / FEMALE ， 必填
     * @param age      年龄段 (成人|儿童)  ADULT / CHILD， 非必填
     * @param filePath 音频文件路径
     */
    public void upload(String textId, String gender, String age, String filePath) {
        if (voiceCopyKernel != null) {
            voiceCopyKernel.upload(textId, gender, age, filePath);
        }
    }


    /**
     * 提交训练
     *
     * @param gender       自定义声音录音性别（MALE 和 FEMALE），必填
     * @param age          年龄段 (成人|儿童)  ADULT / CHILD
     * @param customInfo   自定义音色资源名称
     * @param audio_list   音频检测接口所返回的 "audio_reserve_id" 集合
     * @param pre_tts_text 训练后,试听音频文本
     */
    public void training(String gender, String age, String customInfo
            , ArrayList<String> audio_list, ArrayList<String> pre_tts_text) {
        if (voiceCopyKernel != null) {
            voiceCopyKernel.training(gender, age, customInfo, audio_list, pre_tts_text);
        }
    }


    /**
     * 查询任务状态，查询全部
     *
     * @param taskId 复刻任务ID
     */
    public void query(String taskId) {
        if (voiceCopyKernel != null) {
            voiceCopyKernel.query(taskId);
        }
    }

    /**
     * 删除音色
     *
     * @param taskId 任务ID
     */
    public void delete(String taskId) {
        if (voiceCopyKernel != null) {
            voiceCopyKernel.delete(taskId);
        }
    }

    /**
     * 自定义任务相关的信息
     *
     * @param taskId     任务ID
     * @param customInfo 自定义信息
     */
    public void customize(String taskId, String customInfo) {
        if (voiceCopyKernel != null) {
            voiceCopyKernel.customize(taskId, customInfo);
        }
    }

    /**
     * 停止声音复刻
     */
    public void stop() {
        if (voiceCopyKernel != null) {
            voiceCopyKernel.stop();
        }
    }

    /**
     * 释放复刻资源
     */
    public void destroy() {
        if (voiceCopyKernel != null) {
            voiceCopyKernel.destroy();
        }
    }

}
