package com.aispeech.export.engines;

import com.aispeech.export.config.AICloudVprintConfig;
import com.aispeech.export.intent.AICloudVprintIntent;
import com.aispeech.export.listeners.AICloudVprintListener;

import java.util.List;

/**
 * opcode 整型 标记本次注册是文本无关（1），文本半相关（2），文本相关（3）
 * 在提交同一个用户的声纹采样时：
 * <p>
 * 文本无关声纹
 * 每次声纹采样可以是任意的音频，直到注册成功或者失败。
 * 文本相关（半相关）
 * 每次声纹采样需要用户念诵指定文本的语音，每次需要念诵的文本包含在上一次调用开始注册声纹或注册声纹接口的返回结果中（即content参数）。注册成功或失败时，返回结果中不包含语音文本。
 * 关于声纹内容的限制：
 * </p>
 * 云端声纹-V3
 * <p>
 * 注册接口-V3：/vpr/v3/register?res=ti-sr
 * 注销接口-V3：/vpr/v3/unregister?res=lti-sr
 * 验证接口-V3（离线）：/vpr/v3/verify?res=ti-sr
 * 验证接口-V3（在线）：/vpr/v3/verify/online?res=sti-sd
 * res 字段：
 * ti-sr/离线一句话文本无关	适用于自由文本的一句话离线说话人识别场景。
 * dp-sr/离线文本半相关(数字串/文本相关）	适用于数字串，固定文本（唤醒词），固定文本（唤醒词）+数字串的一句话离线说话人识别场景。
 * sti-sr/实时短语音文本无关	适用于问答形式的场景下，说话人身份的确认。可以有效拒绝非注册人，但不支持一个vad片段内多个说话人的分离。（如多轮对话场景）
 * lti-sr/实时长语音文本无关	适用于多说话人轮流交替说话的场景下的说话人分离。不可以有效拒绝非注册人，支持一个vad片段内多个说话人的分离。（如会议转写场景）
 * </p>
 * 声纹样本的位数必须为16bit，使用单声道采样。
 * 声纹样本：文本无关：16k采样率的 wav文件;文本半相关：16k采样率的 wav文件。
 * @deprecated {@link com.aispeech.export.engines2.AICloudVprintEngine}
 */

@Deprecated
public class AICloudVprintEngine {

    private static final String TAG = "AICloudVprintEngine";
    private com.aispeech.export.engines2.AICloudVprintEngine mAICloudVprintEngine;

    public static AICloudVprintEngine createInstance() {
        return new AICloudVprintEngine();
    }

    private AICloudVprintEngine() {
        mAICloudVprintEngine = com.aispeech.export.engines2.AICloudVprintEngine.createInstance();
    }

    public synchronized void init(AICloudVprintConfig config, AICloudVprintListener listener) {
        if (mAICloudVprintEngine != null) {
            mAICloudVprintEngine.init(config, listener);
        }
    }

    /**
     * 1. 准备注册声纹
     *
     * @param userId   用户ID，用户ID在您的系统里应该是唯一的。
     * @param userName 用户名称
     * @param groupId  用户所在的组ID，需要跟内核组约定，不支持任意指定。
     * @deprecated 已过时, 使用 registerV3 方法代替
     */
    public void prepareRegister(int userId, String userName, String groupId) {
    }

    /**
     * 2. 注册声纹
     *
     * @param wavFilepath wav 音频路径
     * @deprecated 已过时, 使用 registerV3 方法代替
     */
    public void register(String wavFilepath) {
    }


    /**
     * 3. 准备验证声纹
     *
     * @param userIdList 要对比的用户id列表
     * @param groupId    通过指定组id拉取对应的用户数据
     * @deprecated 已过时, 使用 verifyV3 方法代替
     */
    public void prepareVerify(List<Integer> userIdList, String groupId) {
    }

    /**
     * 4. 验证声纹
     *
     * @param wavFilepath wav 音频路径
     * @deprecated 已过时, 使用 verifyV3 方法代替
     */
    public void verify(String wavFilepath) {
    }

    /**
     * 5. 注销声纹
     *
     * @param userId  用户ID，用户ID在您的系统里应该是唯一的
     * @param groupId 用户所在的组ID，需要跟内核组约定，不支持任意指定
     * @deprecated 已过时, 使用 unregisterV3 方法代替
     */
    public void unregister(int userId, String groupId) {
    }

    /**
     * 注册声纹-V3
     *
     * @param intent    注册声纹参数
     * @param audioPath 音频路径
     */
    public void registerV3(AICloudVprintIntent intent, String audioPath) {
        if (mAICloudVprintEngine != null) {
            intent.setWavFilepath(audioPath);
            mAICloudVprintEngine.register(intent);
        }
    }

    /**
     * 注册声纹-V3
     *
     * @param intent    注册声纹参数
     * @param audioPath 音频路径
     */
    public void verifyV3(AICloudVprintIntent intent, String audioPath) {
        if (mAICloudVprintEngine != null) {
            intent.setWavFilepath(audioPath);
            mAICloudVprintEngine.verifyHttp(intent);
        }
    }

    /**
     * 注销声纹-V3，注意带 organization 时，userId 在对应的 organization 去做注销
     *
     * @param userId       用户ID，用户ID在您的系统里应该是唯一的
     * @param organization 用户所在的公司，项目。。。
     * @param requestId    唯一，标记一次请求，可选, 如果不存在则服务端会生成一个
     */
    public void unregisterV3(String userId, String organization, String requestId) {
        if (mAICloudVprintEngine != null) {
            AICloudVprintIntent intent = new AICloudVprintIntent();
            intent.setUserId(userId);
            intent.setOrganization(organization);
            intent.setRequestId(requestId);
            mAICloudVprintEngine.unregister(intent);
        }
    }

    /***
     * 设置生成音频路径
     * @param path 音频路径
     */
    public void setVprintAudioPath(String path) {
        if (mAICloudVprintEngine != null) {
            mAICloudVprintEngine.setVprintAudioPath(path);
        }
    }

    /**
     * 结束喂音频
     */
    public void stopFeedData() {
        if (mAICloudVprintEngine != null) {
            mAICloudVprintEngine.stopFeedData();
        }
    }

    /**
     * 传入数据,在不使用SDK录音机时调用
     *
     * @param data 音频数据流
     */
    public void startFeedData(byte[] data) {
        if (mAICloudVprintEngine != null) {
            mAICloudVprintEngine.startFeedData(data);
        }
    }


    /**
     * 销毁，释放资源
     */
    public synchronized void destroy() {
        if (mAICloudVprintEngine != null) {
            mAICloudVprintEngine.destroy();
            mAICloudVprintEngine = null;
        }
    }

    /**
     * 录制wav音频文件的工具类
     */
    public static class AudioTool {

        com.aispeech.export.engines2.AICloudVprintEngine.AudioTool mAudioTool;

        public AudioTool() {
            mAudioTool = new com.aispeech.export.engines2.AICloudVprintEngine.AudioTool();
        }

        public synchronized void startRecord(String filepath, final AICloudVprintListener.AudioToolListener audioToolListener) {
            startRecord(null, filepath, audioToolListener);
        }

        public synchronized void startRecord(AICloudVprintConfig.Mode opcode, String filepath, final AICloudVprintListener.AudioToolListener audioToolListener) {
            if (mAudioTool != null) {
                mAudioTool.startRecord(opcode, filepath, audioToolListener);
            }
        }

        public synchronized void stopRecord() {
            if (mAudioTool != null) {
                mAudioTool.stopRecord();
            }
        }

        public synchronized void releaseRecord() {
            if (mAudioTool != null) {
                mAudioTool.releaseRecord();
            }
        }

        public synchronized void destroy() {
            if (mAudioTool != null) {
                mAudioTool.destroy();
            }
        }
    }
}
