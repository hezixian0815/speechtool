package com.aispeech.export;

import android.text.TextUtils;

import com.aispeech.lite.dm.Protocol;

import org.json.JSONObject;

/**
 * 播报文本内容
 *
 * @author hehr
 * 该版本按照dds服务端给出来的参数信息解析speak信息
 */
public class Speaker {

    /**
     * 播报文本,必选
     */
    public String nlg;
    /**
     * 语音播报nlg的url链接,非必选,优先播报 speakUrl
     */
    public String speakUrl;

    public String getNlg() {
        return nlg;
    }

    public void setNlg(String nlg) {
        this.nlg = nlg;
    }

    public String getSpeakUrl() {
        return speakUrl;
    }

    public void setSpeakUrl(String speakUrl) {
        this.speakUrl = speakUrl;
    }

    /**
     * 构建Speak对象
     *
     * @param object 完整的dds server 返回的数据结果
     * @return {@link Speaker}
     */
    public static Speaker transform(JSONObject object) {

        JSONObject dm = object.optJSONObject(Protocol.DM);

        if (dm.has(Protocol.DM_NLG)) {
            Speaker speaker = new Speaker();
            if (object.has(Protocol.SPEAK_URL)) {
                speaker.setSpeakUrl(object.optString(Protocol.SPEAK_URL));
            }
            speaker.setNlg(dm.optString(Protocol.DM_NLG));
            return speaker;
        }

        return null;

    }

    @Override
    public String toString() {
        return "Speaker{" +
                "nlg='" + nlg + '\'' +
                ", speakUrl='" + speakUrl + '\'' +
                '}';
    }

    public boolean isEmpty() {
        return TextUtils.isEmpty(nlg) && TextUtils.isEmpty(speakUrl);
    }
}
