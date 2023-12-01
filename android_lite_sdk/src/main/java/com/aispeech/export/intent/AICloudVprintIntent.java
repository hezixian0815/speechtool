package com.aispeech.export.intent;

import com.aispeech.common.JSONUtil;
import com.aispeech.export.config.AICloudVprintConfig;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class AICloudVprintIntent {

    /**
     * 用于注册和验证的音频文件路径，注册和验证时使用
     */
    private String wavFilepath = "";

    /**
     * 标记一次请求，可选, 如果不存在则服务端会生成一个
     * 非必需
     */
    private String requestId = "";

    /**
     * 音频的格式
     */
    private Audio audio;
    /**
     * 声纹ID，用户ID在系统中应该是唯一的
     * 注册时非必需，不设置服务端会生成一个
     * 注销时必须设置
     * 该属性不用于验证
     */
    private String userId = "";
    /**
     * 用户所在的公司，项目,非必需
     */
    private String organization = "";
    /**
     * 领域（comm/aihome/aitv/aicar/aiphone/airobot/aitranson-cn-16k/aitranson-en-16k)
     * 用于注册和实时声纹验证，注册时非必需，实时声纹验证必需设置
     */
    private String domain = "";

    /**
     * 用户设备相关的内容
     */
    private JSONObject app;
    /**
     * 为true不保存音频，默认false，非必需设置
     */
    private boolean skip_saving = false;
    /**
     * 文本半相关语音文本结果和custom_context的字错误率的百分比阈值 [0,100]，结果向下取整，仅用于文本相关的注册和验证，默认值0
     */
    private float asrErrorRate = 0;
    /**
     * 文本（半）相关时输入的语音文本。使用文本（半）相关时必需设置
     */
    private String customContent;
    /**
     * 注册增强，仅用于注册
     */
    private boolean enhanceRegister;
    /**
     * 最小有效音频长度，单位秒,默认注册声纹，离线验证0.4s,验证声纹sti-sr 1s， lti-sr 500ms
     */
    private float minSpeechLength;
    /**
     * 文本无关vad开关
     */
    private boolean enableVad = true;

    /**
     * 要对比的用户id列表。若users列表为空,择选取organization所有的id做1：N比对，仅用于验证
     */
    private List<String> users;

    /**
     * users和groupId，二选一，或者2个都填（按最小集），仅用于离线声纹验证
     */
    private String groupId;
    /**
     * 1:N按照score排序,输出前n个得分,若不配置该选项,会在result字段中返回所有得分,开启该选项后会在topN字段中返回得分排名靠前的最多N个得分.仅用于离线验证
     */
    private int topN;

    /**
     * 语音活体检测，仅用于离线验证
     */
    private boolean enableSpeakerAntiSpoofing;

    /**
     * 如果有contextId，服务端认为是同一个验证，会使用保存的cache和新的数据进行加强验证，仅用于实时验证
     */
    private String contextId;

    /**
     * 声纹服务版本，可设置为prod用以使用新的模型
     */
    private String aliasKey;

    /**
     * 声纹类型
     */
    private AICloudVprintConfig.Mode mode = AICloudVprintConfig.Mode.TEXT_OFFLINE_HALF_RELATED;

    public AICloudVprintConfig.Mode getMode() {
        return mode;
    }

    /**
     * 设置声纹类型
     * 查询的时候设置声纹类型
     * @param mode
     */
    public void setMode(AICloudVprintConfig.Mode mode) {
        this.mode = mode;
    }

    public JSONObject getApp() {
        return app;
    }

    public Audio getAudio() {
        return audio;
    }

    /**
     * 如果有contextId，服务端认为是同一个验证，会使用保存的cache和新的数据进行加强验证，仅用于实时验证
     *
     * @return 返回contextID
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * 如果有contextId，服务端认为是同一个验证，会使用保存的cache和新的数据进行加强验证，仅用于实时验证
     *
     * @param contextId 用于服务器判断是同一个验证
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    /**
     * 1:N按照score排序,输出前n个得分,若不配置该选项,会在result字段中返回所有得分,开启该选项后会在topN字段中返回得分排名靠前的最多N个得分.仅用于离线验证
     *
     * @return 返回前n个得分
     */
    public int getTopN() {
        return topN;
    }

    /**
     * 1:N按照score排序,输出前n个得分,若不配置该选项,会在result字段中返回所有得分,开启该选项后会在topN字段中返回得分排名靠前的最多N个得分.仅用于离线验证
     *
     * @param topN 设置前N个得分
     */
    public void setTopN(int topN) {
        this.topN = topN;
    }

    /**
     * 语音活体检测，仅用于离线验证
     *
     * @return 活体检测
     */
    public boolean isEnableSpeakerAntiSpoofing() {
        return enableSpeakerAntiSpoofing;
    }

    /**
     * 语音活体检测，仅用于离线验证
     *
     * @param enableSpeakerAntiSpoofing 语音活体检测
     */
    public void setEnableSpeakerAntiSpoofing(boolean enableSpeakerAntiSpoofing) {
        this.enableSpeakerAntiSpoofing = enableSpeakerAntiSpoofing;
    }

    /**
     * users和groupId，二选一，或者2个都填（按最小集），仅用于离线声纹验证
     *
     * @return groupID
     */

    public String getGroupId() {
        return groupId;
    }

    /**
     * users和groupId，二选一，或者2个都填（按最小集），仅用于离线声纹验证
     *
     * @param groupId 设置groupID
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * 要对比的用户id列表。若users列表为空,择选取organization所有的id做1：N比对，仅用于验证
     *
     * @return 用户列表返回
     */
    public List<String> getUsers() {
        return users;
    }

    /**
     * 要对比的用户id列表。若users列表为空,择选取organization所有的id做1：N比对，仅用于验证
     *
     * @param users 设置用户列表
     */
    public void setUsers(List<String> users) {
        this.users = users;
    }

    /**
     * 标记一次请求，可选, 如果不存在则服务端会生成一个
     * 非必需
     *
     * @return 请求ID
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * 标记一次请求，可选, 如果不存在则服务端会生成一个
     * 非必需
     *
     * @param requestId 请求ID
     */
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * 声纹ID，用户ID在系统中应该是唯一的
     * 注册时非必需，不设置服务端会生成一个
     * 注销时必须设置
     * 仅用于注册和注销
     *
     * @return 用户ID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * 声纹ID，用户ID在系统中应该是唯一的
     * 注册时非必需，不设置服务端会生成一个
     * 注销时必须设置
     * 仅用于注册和注销
     *
     * @param userId 用户ID
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * 用户所在的公司，项目,非必需
     *
     * @return 用户所在的公司，项目
     */
    public String getOrganization() {
        return organization;
    }

    /**
     * 用户所在的公司，项目,非必需
     *
     * @param organization 用户所在的公司，项目
     */
    public void setOrganization(String organization) {
        this.organization = organization;
    }

    /**
     * 领域（comm/aihome/aitv/aicar/aiphone/airobot/aitranson-cn-16k/aitranson-en-16k)
     * 用于注册和实时声纹验证，注册时非必需，实时声纹验证必需设置
     *
     * @return domain 领域
     */
    public String getDomain() {
        return domain;
    }

    /**
     * 领域（comm/aihome/aitv/aicar/aiphone/airobot/aitranson-cn-16k/aitranson-en-16k)
     * 用于注册和实时声纹验证，注册时非必需，实时声纹验证必需设置
     *
     * @param domain 领域
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * 为true不保存音频，默认false，非必需设置
     *
     * @return 是否保存音频
     */
    public boolean isSkip_saving() {
        return skip_saving;
    }

    /**
     * 为true不保存音频，默认false，非必需设置
     *
     * @param skip_saving 是否保存音频
     */
    public void setSkip_saving(boolean skip_saving) {
        this.skip_saving = skip_saving;
    }

    /**
     * 文本半相关语音文本结果和custom_context的字错误率的百分比阈值 [0,100]，结果向下取整，仅用于文本相关的注册和验证，默认值0
     *
     * @return 错误率百分比阈值
     */
    public float getAsrErrorRate() {
        return asrErrorRate;
    }

    /**
     * 文本半相关语音文本结果和custom_context的字错误率的百分比阈值 [0,100]，结果向下取整，仅用于文本相关的注册和验证，默认值0
     *
     * @param asrErrorRate 错误率百分比阈值
     */
    public void setAsrErrorRate(float asrErrorRate) {
        this.asrErrorRate = asrErrorRate;
    }

    /**
     * 文本（半）相关时输入的语音文本。使用文本（半）相关时必需设置
     *
     * @return 用户注册文本
     */
    public String getCustomContent() {
        return customContent;
    }

    /**
     * 文本（半）相关时输入的语音文本。使用文本（半）相关时必需设置
     *
     * @param customContent 用户注册文本
     */
    public void setCustomContent(String customContent) {
        this.customContent = customContent;
    }

    /**
     * 注册增强，仅用于注册
     *
     * @return 当前注册是否是注册增强
     */
    public boolean isEnhanceRegister() {
        return enhanceRegister;
    }

    /**
     * 注册增强，仅用于注册，默认false
     *
     * @param enhanceRegister 是否是注册增强
     */
    public void setEnhanceRegister(boolean enhanceRegister) {
        this.enhanceRegister = enhanceRegister;
    }

    /**
     * 最小有效音频长度，单位秒,默认注册声纹，离线验证0.4s,验证声纹sti-sr 1s， lti-sr 500ms
     *
     * @return 最小有效音频长度
     */
    public float getMinSpeechLength() {
        return minSpeechLength;
    }

    /**
     * 最小有效音频长度，单位秒,默认注册声纹，离线验证0.4s,验证声纹sti-sr 1s， lti-sr 500ms
     *
     * @param minSpeechLength 最小有效音频长度
     */
    public void setMinSpeechLength(float minSpeechLength) {
        this.minSpeechLength = minSpeechLength;
    }

    /**
     * 文本无关vad开关
     *
     * @return 是否使用文本无关vad
     */
    public boolean isEnableVad() {
        return enableVad;
    }

    /**
     * 文本无关vad开关，默认为true
     *
     * @param enableVad 文本无关是否使用vad
     */
    public void setEnableVad(boolean enableVad) {
        this.enableVad = enableVad;
    }

    /**
     * 用于注册和验证声纹的音频文件路径，必需设置
     *
     * @return 设置的注册或者验证声纹的音频文件路径
     */
    public String getWavFilepath() {
        return wavFilepath;
    }

    /**
     * 用于注册和验证声纹的音频文件路径，必需设置
     *
     * @param wavFilepath 注册和验证的声纹音频路径
     */
    public void setWavFilepath(String wavFilepath) {
        this.wavFilepath = wavFilepath;
    }

    /**
     * 获取当前云端声纹使用的资源版本
     *
     * @return 云端声纹的资源版本
     */
    public String getAliasKey() {
        return aliasKey;
    }

    /**
     * 设置当前云端声纹使用的资源版本，可设置为prod
     *
     * @param aliasKey 设置当前使用的资源版本
     */
    public void setAliasKey(String aliasKey) {
        this.aliasKey = aliasKey;
    }

    /**
     * 信噪比阈值,默认8.67
     */
    private float snrRate = 8.67f;

    public AICloudVprintIntent() {
    }

    private AICloudVprintIntent(Builder builder) {
        this.wavFilepath = builder.wavFilepath;
        this.requestId = builder.requestId;
        this.audio = builder.audio;
        this.userId = builder.userId;
        this.organization = builder.organization;
        this.domain = builder.domain;
        this.app = builder.app;
        if (builder.env != null) {
            this.asrErrorRate = builder.env.asrErrorRate;
            this.customContent = builder.env.customContent;
            this.enhanceRegister = builder.env.enhanceRegister;
            this.minSpeechLength = builder.env.minSpeechLength;
            this.enableVad = builder.env.enableVad;
            this.enableSpeakerAntiSpoofing = builder.env.enableSpeakerAntiSpoofing;
            this.snrRate = builder.env.snrRate;
        }
        this.skip_saving = builder.skip_saving;
        if (builder.users != null) {
            this.users = Arrays.asList(builder.users);
        }
        this.groupId = builder.groupId;
        this.topN = builder.topN;
        this.contextId = builder.contextId;
        this.aliasKey = builder.aliasKey;
        this.mode = builder.mode;
    }

    public static class Builder {
        /**
         * 声纹类型
         */
        private AICloudVprintConfig.Mode mode = AICloudVprintConfig.Mode.TEXT_OFFLINE_HALF_RELATED;
        /**
         * 用于注册和验证的音频文件路径，注册和验证时使用
         */
        String wavFilepath;
        /**
         * 唯一，标记一次请求，可选, 如果不存在则服务端会生成一个
         */
        private String requestId;

        /**
         * 音频的格式
         */
        private Audio audio;

        /**
         * 用户ID，用户ID在您的系统里应该是唯一的（没有服务端会生成）。
         */
        private String userId;

        /**
         * 用户所在的公司，项目。。。
         */
        private String organization;

        /**
         * 领域（aihome/aitv）
         */
        private String domain;

        /**
         * 用户设备相关的内容
         */
        private JSONObject app;

        /**
         * 扩展选项
         */
        private Env env;

        /**
         * 为true不保存音频
         */
        private boolean skip_saving;

        /**
         * 要对比的用户id列表。若users列表为空,择选取organization所有的id做1：N比对。
         */
        private String[] users;

        /**
         * users和groupId，二选一，或者2个都填（按最小集）
         */
        private String groupId;

        /**
         * 1:N按照score排序,输出前n个得分,若不配置该选项,会在result字段中返回所有得分,开启该选项后会在topN字段中返回得分排名靠前的最多N个得分.仅用于离线验证
         */
        private int topN;
        /**
         * 如果有contextId，服务端认为是同一个验证，会使用保存的cache和新的数据进行加强验证，仅用于实时验证
         */
        private String contextId;
        /**
         * 声纹服务版本，可设置为prod用以使用新的模型
         */
        private String aliasKey;

        /**
         * 查询的时候设置声纹类型
         */
        public Builder setMode(AICloudVprintConfig.Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder setAliasKey(String aliasKey) {
            this.aliasKey = aliasKey;
            return this;
        }

        public Builder setContextId(String contextId) {
            this.contextId = contextId;
            return this;
        }

        public Builder setTopN(int topN) {
            this.topN = topN;
            return this;
        }

        public Builder setWavFilePath(String wavFilepath) {
            this.wavFilepath = wavFilepath;
            return this;
        }

        public Builder setRequestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder setAudio(Audio audio) {
            this.audio = audio;
            return this;
        }

        public Builder setUserId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder setOrganization(String organization) {
            this.organization = organization;
            return this;
        }

        public Builder setDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public Builder setApp(JSONObject app) {
            this.app = app;
            return this;
        }

        public Builder setEnv(Env env) {
            this.env = env;
            return this;
        }

        public Builder setSkip_saving(boolean skip_saving) {
            this.skip_saving = skip_saving;
            return this;
        }

        public Builder setGroupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public Builder setUsers(String[] users) {
            this.users = users;
            return this;
        }

        public AICloudVprintIntent build() {
            return new AICloudVprintIntent(this);
        }
    }


    /**
     * 音频的格式
     * "audio":{
     * "audioType": "wav",            // 必选
     * "sampleRate": 16000,            // 必选
     * "channel": 1,                  // 必选
     * "sampleBytes": 2               // 必选
     * }
     */
    public static class Audio {

        private String audioType = "wav";
        private int sampleRate = 16000;
        private int channel = 1;
        private int sampleBytes = 2;

        public String getAudioType() {
            return audioType;
        }

        public void setAudioType(String audioType) {
            this.audioType = audioType;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public int getChannel() {
            return channel;
        }

        public void setChannel(int channel) {
            this.channel = channel;
        }

        public int getSampleBytes() {
            return sampleBytes;
        }

        public void setSampleBytes(int sampleBytes) {
            this.sampleBytes = sampleBytes;
        }

        public JSONObject toJson() {
            JSONObject JSON = new JSONObject();
            JSONUtil.putQuietly(JSON, "audioType", audioType);
            JSONUtil.putQuietly(JSON, "sampleRate", sampleRate);
            JSONUtil.putQuietly(JSON, "channel", channel);
            JSONUtil.putQuietly(JSON, "sampleBytes", sampleBytes);
            return JSON;
        }
    }

    public static class Env {
        /**
         * 文本半相关语音文本结果和custom_context的字错误率的百分比阈值 [0,100]	，默认0
         */
        private float asrErrorRate = 0;

        /**
         * 文本（半）相关时输入的语音文本
         */
        private String customContent;

        /**
         * 注册增强 ， 默认false
         */
        private boolean enhanceRegister = false;

        /**
         * 最小有效音频长度，单位秒 , 默认	0.4
         */
        private float minSpeechLength = 0.4f;

        /**
         * 文本无关vad开关,默认true
         */
        private boolean enableVad = true;

        /**
         * 语音活体检测,默认true
         */
        private boolean enableSpeakerAntiSpoofing = true;

        /**
         * 信噪比阈值,默认8.67
         */
        private float snrRate = 8.67f;

        public float getAsrErrorRate() {
            return asrErrorRate;
        }

        public void setAsrErrorRate(float asrErrorRate) {
            this.asrErrorRate = asrErrorRate;
        }

        public String getCustomContent() {
            return customContent;
        }

        public void setCustomContent(String customContent) {
            this.customContent = customContent;
        }

        public boolean isEnhanceRegister() {
            return enhanceRegister;
        }

        public void setEnhanceRegister(boolean enhanceRegister) {
            this.enhanceRegister = enhanceRegister;
        }

        public float getMinSpeechLength() {
            return minSpeechLength;
        }

        public void setMinSpeechLength(float minSpeechLength) {
            this.minSpeechLength = minSpeechLength;
        }

        public boolean isEnableVad() {
            return enableVad;
        }

        public void setEnableVad(boolean enableVad) {
            this.enableVad = enableVad;
        }

        public boolean isEnableSpeakerAntiSpoofing() {
            return enableSpeakerAntiSpoofing;
        }

        public void setEnableSpeakerAntiSpoofing(boolean enableSpeakerAntiSpoofing) {
            this.enableSpeakerAntiSpoofing = enableSpeakerAntiSpoofing;
        }

        public float getSnrRate() {
            return snrRate;
        }

        public void setSnrRate(float snrRate) {
            this.snrRate = snrRate;
        }

        public JSONObject toJson() {
            JSONObject JSON = new JSONObject();
            JSONUtil.putQuietly(JSON, "asrErrorRate", asrErrorRate);
            JSONUtil.putQuietly(JSON, "customContent", customContent);
            JSONUtil.putQuietly(JSON, "enhanceRegister", enhanceRegister);
            JSONUtil.putQuietly(JSON, "minSpeechLength", minSpeechLength);
            JSONUtil.putQuietly(JSON, "enableVad", enableVad);
            JSONUtil.putQuietly(JSON, "enableSpeakerAntiSpoofing", enableSpeakerAntiSpoofing);
            JSONUtil.putQuietly(JSON, "snrRate", snrRate);
            return JSON;
        }
    }


    public JSONObject toJson() {
        JSONObject JSON = new JSONObject();
        JSONUtil.putQuietly(JSON, "requestId", requestId);
        JSONUtil.putQuietly(JSON, "userId", userId);
        JSONUtil.putQuietly(JSON, "organization", organization);
        JSONUtil.putQuietly(JSON, "domain", domain);
        JSONUtil.putQuietly(JSON, "app", app);
        JSONUtil.putQuietly(JSON, "skip_saving", skip_saving);

        JSONObject env = new JSONObject();
        JSONUtil.putQuietly(env, "asrErrorRate", asrErrorRate);
        JSONUtil.putQuietly(env, "customContent", customContent);
        JSONUtil.putQuietly(env, "enhanceRegister", enhanceRegister);
        JSONUtil.putQuietly(env, "minSpeechLength", minSpeechLength);
        JSONUtil.putQuietly(env, "enableVad", enableVad);
        JSONUtil.putQuietly(env, "enableSpeakerAntiSpoofing", enableSpeakerAntiSpoofing);
        JSONUtil.putQuietly(env, "snrRate", snrRate);

        JSONUtil.putQuietly(JSON, "env", env);
        if (audio == null) {
            audio = new Audio();
        }
        JSONUtil.putQuietly(JSON, "audio", audio.toJson());
        if (users != null) {
            JSONArray array = new JSONArray();
            for (String user : users) {
                array.put(user);
            }
            JSONUtil.putQuietly(JSON, "users", array);
        }

        JSONUtil.putQuietly(JSON, "groupId", groupId);
        return JSON;
    }

}
