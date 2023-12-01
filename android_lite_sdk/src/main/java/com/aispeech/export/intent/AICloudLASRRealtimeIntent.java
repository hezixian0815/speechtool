package com.aispeech.export.intent;

import android.text.TextUtils;

import com.aispeech.base.IFespxEngine;
import com.aispeech.common.Log;
import com.aispeech.lite.base.BaseIntent;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AICloudLASRRealtimeIntent extends BaseIntent {

    /**
     * 音频编码类型，和 云端 asr 的 PCM_ENCODE_TYPE 相差一个 OGG_OPUS
     */
    public enum PCM_ENCODE_TYPE {
        OGG, OGG_OPUS, WAV, MP3/*, OPUS*/
    }


    private IFespxEngine fespxEngine;

    /**
     * 用户 feed 的音频是否是编码后的音频。
     * 默认 false 即 feed 的是 pcm 音频，true 表示 feed 的是编码后的音频，如 MP3 OGG OPUS OGG_OPUS
     * 使用前提 用户feed，并且不使用本地vad
     */
    private boolean encodedAudio = false;

    private String server = "wss://lasr.duiopen.com/live/ws2";

    public String getStringAudioType() {
        if (audioType == null)
            return "";
        switch (audioType) {
            case OGG:
                return "ogg";
            case OGG_OPUS:
                return "ogg_opus";
            case WAV:
                return "wav";
            case MP3:
                return "mp3";
            /*case OPUS:
                return "opus";*/
            default:
                return "beyond_definition";
        }
    }

    public boolean isValid() {
        return audioType != null && server != null && server.startsWith("ws");
    }

    //////////////////////////////////////


    /**
     * ⾳频类型，支持：wav, ogg, ogg_opus, opus, mp3。ogg_opus 服务器无法识别
     */
    private PCM_ENCODE_TYPE audioType = PCM_ENCODE_TYPE.OGG;

    /**
     * 音频采样率
     */
    private int sampleRate = 16000;
    /**
     * 取样字节数
     */
    private int sampleBytes = 2;
    /**
     * 音频通道数
     */
    private int channel = 1;
    // asrPlus ==>
    private String serverName;
    private String organization;
    private String domain = "";
    private String contextId = "";
    private String groupId;
    private List<String> users;
    private boolean cloudVprintVadEnable = true;

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getContextId() {
        return contextId;
    }

    public void setContextId(String contextId) {
        this.contextId = contextId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public boolean isCloudVprintVadEnable() {
        return cloudVprintVadEnable;
    }

    public void setCloudVprintVadEnable(boolean cloudVprintVadEnable) {
        this.cloudVprintVadEnable = cloudVprintVadEnable;
    }

    public float getMinSpeechLength() {
        return minSpeechLength;
    }

    public void setMinSpeechLength(float minSpeechLength) {
        this.minSpeechLength = minSpeechLength;
    }

    private float minSpeechLength;
    // asrPlus <==

    private JSONObject getJsonAudio() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("audioType", getStringAudioType());
            jsonObject.put("sampleRate", sampleRate);
            jsonObject.put("sampleBytes", sampleBytes);
            jsonObject.put("channel", channel);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public JSONObject getVPrintJSON() {
        JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("serverName", serverName);
            jsonObject.put("organization", organization);
            if (!domain.isEmpty()) {
                jsonObject.put("domain", domain);
            }
            if (!contextId.isEmpty()) {
                jsonObject.put("contextId", contextId);
            }

            if (TextUtils.isEmpty(groupId)) {
                jsonObject.put("groupId", groupId);
            }
            boolean existUser = users != null && !users.isEmpty();
            if (existUser) {
                JSONArray usersArray = new JSONArray();
                for (String s : users) {
                    usersArray.put(s);
                }
                jsonObject.put("users", usersArray);
            }
            jsonObject.put("enableAsrPlus", !TextUtils.isEmpty(serverName) && existUser);

            JSONObject env = new JSONObject();
            env.put("enableVAD", cloudVprintVadEnable);
            if (minSpeechLength > 0) {
                env.put("minSpeechLength", minSpeechLength);
            }
            jsonObject.put("env", env);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }
    //////////////////////////////////////

    /**
     * 口语顺滑
     */
    private boolean useTxtSmooth = false;
    /**
     * 逆文本
     */
    private boolean useTProcess = true;

    /**
     * 内置敏感词
     */
    private boolean useSensitiveWdsNorm = false;

    private JSONObject getJsonEnv() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("use_txt_smooth", useTxtSmooth ? 1 : 0);
            jsonObject.put("use_tprocess", useTProcess ? 1 : 0);
            jsonObject.put("use_sensitive_wds_norm", useSensitiveWdsNorm ? 1 : 0);

            if (extraParam != null && !extraParam.isEmpty()) {
                Iterator<String> iter = extraParam.keySet().iterator();
                while (iter.hasNext()) {
                    String key = iter.next();
                    Object value = extraParam.get(key);
                    jsonObject.put(key, value);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }


    //////////////////////////////////////

    public JSONObject getJsonLSR() {
        JSONObject lsrJson = new JSONObject();
        try {
            lsrJson.put("command", "start");

            /*if (hotWords != null && hotWords.size() > 0) {
                JSONArray jsonArray = new JSONArray();
                for (String s : hotWords) {
                    jsonArray.put(s);
                }
                lsrJson.put("hotWords", jsonArray);
            }*/

            JSONObject paramsJSON = new JSONObject();
            paramsJSON.put("env", getJsonEnv());
            paramsJSON.put("audio", getJsonAudio());
            paramsJSON.put("asrPlus", getVPrintJSON());

            lsrJson.put("params", paramsJSON);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return lsrJson;
    }

    // url 里的参数
    /**
     * res=lasr-cn-en使用中英文混合，不设置res字段使用中文在线
     */
    private String res = null;

    /**
     * 当参数不为空时，启动转发模式。 当有转写结果时，会往注册的WebSocket地址实时推送转写结果。
     * <p>
     * 支持多个转写websocket服务地址，多个地址中间用英文逗号 , 隔开。
     * <p>
     * 格式： ws://xxxx:port,ws://xxxx:port,ws://xxxx:port
     */
    private String forwardAddresses = null;

    /**
     * en(英文在线)/cn(中文在线)，不传此参数时默认使用中文，如需使用英文，请传参时设置lang=en
     */
    private String lang = null;

    private Map<String, Object> extraParam = null;

    //////////////////////////////////////

    @Override
    public String toString() {
        return "AICloudLASRRealtimeIntent{" +
                "useCustomFeed=" + useCustomFeed +
                ", fespxEngine=" + fespxEngine +
                ", encodedAudio=" + encodedAudio +
                ", server='" + server + '\'' +
                ", audioType=" + audioType +
                ", sampleRate=" + sampleRate +
                ", sampleBytes=" + sampleBytes +
                ", channel=" + channel +
                ", useTxtSmooth=" + useTxtSmooth +
                ", useTProcess=" + useTProcess +
                ", useSensitiveWdsNorm=" + useSensitiveWdsNorm +
                ", res='" + res + '\'' +
                ", forwardAddresses='" + forwardAddresses + '\'' +
                ", lang='" + lang + '\'' +
                ", extraParam=" + extraParam +
                '}';
    }

    /**
     * 设置是否自行feed数据,不使用内部录音机(包括MockRecord和AIAudioRecord)
     * feed 的音频如果不是pcm音频，则不能使用 vad 功能
     *
     * @param useCustomFeed 设置是否自行feed数据，默认false
     * @param encodedAudio  feed的音频是否是编码成 MP3 OGG OPUS OGG_OPUS 等音频的
     */
    public void setUseCustomFeed(boolean useCustomFeed, boolean encodedAudio) {
        this.useCustomFeed = useCustomFeed;
        this.encodedAudio = encodedAudio;
        if (!this.useCustomFeed && this.encodedAudio) {
            Log.e("AICloudASRIntent", "encodedAudio set error, and set encodedAudio false");
            this.encodedAudio = false;
        }
    }

    public boolean isEncodedAudio() {
        return encodedAudio;
    }

    public String getServer() {
        return server;
    }

    /**
     * 服务器地址
     *
     * @param server 服务器地址
     */
    public void setServer(String server) {
        this.server = server;
    }

    public IFespxEngine getFespxEngine() {
        return fespxEngine;
    }

    public void setFespxEngine(IFespxEngine fespxEngine) {
        this.fespxEngine = fespxEngine;
    }

    public PCM_ENCODE_TYPE getAudioType() {
        return audioType;
    }

    /**
     * ⾳频类型，支持：wav, ogg, ogg_opus, opus, mp3。ogg_opus 服务器无法识别
     *
     * @param audioType 频类型
     */
    public void setAudioType(PCM_ENCODE_TYPE audioType) {
        this.audioType = audioType;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * 音频采样率，默认16000
     *
     * @param sampleRate 音频采样率
     */
    public void setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
    }

    public int getSampleBytes() {
        return sampleBytes;
    }

    /**
     * 取样字节数，默认2（16bit）
     *
     * @param sampleBytes 取样字节数
     */
    public void setSampleBytes(int sampleBytes) {
        this.sampleBytes = sampleBytes;
    }

    public int getChannel() {
        return channel;
    }

    /**
     * 音频通道数，default is 1 单声道
     * 目前仅支持1通道数，不支持多通道
     * @param channel 音频通道数
     */
    public void setChannel(int channel) {
        this.channel = channel;
    }

    public boolean isUseTxtSmooth() {
        return useTxtSmooth;
    }

    /**
     * 是否启用口语顺滑
     *
     * @param useTxtSmooth 是否启用，默认 false
     */
    public void setUseTxtSmooth(boolean useTxtSmooth) {
        this.useTxtSmooth = useTxtSmooth;
    }

    public boolean isUseTProcess() {
        return useTProcess;
    }

    /**
     * 逆文本，将识别的中文数字改成阿拉伯数字
     *
     * @param useTProcess 是否启用，默认 true
     */
    public void setUseTProcess(boolean useTProcess) {
        this.useTProcess = useTProcess;
    }

    public boolean isUseSensitiveWdsNorm() {
        return useSensitiveWdsNorm;
    }

    /**
     * 是否使用内置敏感词
     *
     * @param useSensitiveWdsNorm 是否使用，默认 false
     */
    public void setUseSensitiveWdsNorm(boolean useSensitiveWdsNorm) {
        this.useSensitiveWdsNorm = useSensitiveWdsNorm;
    }

    public String getRes() {
        return res;
    }

    /**
     * res=lasr-cn-en使用中英文混合，不设置res字段使用中文在线
     *
     * @param res 资源名
     */
    public void setRes(String res) {
        this.res = res;
    }

    public String getForwardAddresses() {
        return forwardAddresses;
    }

    /**
     * 当参数不为空时，启动转发模式。 当有转写结果时，会往注册的WebSocket地址实时推送转写结果。
     * <p>
     * 支持多个转写websocket服务地址，多个地址中间用英文逗号 , 隔开。
     * <p>
     * 格式： ws://xxxx:port,ws://xxxx:port,ws://xxxx:port
     *
     * @param forwardAddresses websocket服务地址
     */
    public void setForwardAddresses(String forwardAddresses) {
        this.forwardAddresses = forwardAddresses;
    }

    public String getLang() {
        return lang;
    }

    /**
     * en(英文在线)/cn(中文在线)，不传此参数时默认使用中文，如需使用英文，请传参时设置lang=en
     * 如果要使用纯英文，请设置{@link AICloudLASRRealtimeIntent#setRes(String)}为"aitranson"，本参数设置为"en"即可实现
     *
     * @param lang 语种
     */
    public void setLang(String lang) {
        this.lang = lang;
    }

    public Map<String, Object> getExtraParam() {
        return extraParam;
    }

    /**
     * 设置额外的参数。请求的url和报文的 env 字段下都加上额外参数
     *
     * @param key   key
     * @param value value 只能是 String Integer Float Double Boolean
     */
    public synchronized void putExtraParam(String key, Object value) {
        if (extraParam == null)
            extraParam = new HashMap<>();
        extraParam.put(key, value);
    }

}
