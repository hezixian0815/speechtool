package com.aispeech.lite.param;

import com.aispeech.common.JSONUtil;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * CInfo服务请求参数 api文档：https://wiki.aispeech.com.cn/pages/viewpage.action?pageId=25268234#CINFOV1toV2升级-1.4cinfov1存储/缓存设计
 *
 * @author hehr
 */
public class CInfoParams extends BaseRequestParams {

    private static final String KEYS_TOPIC = "topic";

    private static final String KEYS_TOPIC_SYSTEMS_SETTING = "system.settings";

    private static final String KEYS_TOPIC_SKILL_SETTING = "skill.settings";

    private static final String KEYS_SETTING = "settings";

    private static final String KEYS_SKILL_PRIORITY = "skillPriority";

    private static final String KEYS_LOCATION = "location";

    private static final String KEYS_KEY = "key";

    private static final String KEYS_VALUE = "value";

    private static final String KEYS_SKILL_ID = "skillId";

    /**
     * 生成技能配置参数
     *
     * @return
     */
    public JSONObject toSkillContextJSON(String skillId, JSONArray setting) {
        JSONObject object = new JSONObject();
        JSONUtil.putQuietly(object, KEYS_TOPIC, KEYS_TOPIC_SKILL_SETTING);
        JSONUtil.putQuietly(object, KEYS_SKILL_ID, skillId);
        JSONUtil.putQuietly(object, KEYS_SETTING, setting);
        return object;
    }

    /**
     * 生成技能优先级配置参数
     *
     * @return
     */
    public JSONObject toSkillPriorityJSON(JSONArray skillPriorityOfSystemSetting) {
        JSONObject object = new JSONObject();
        JSONUtil.putQuietly(object, KEYS_TOPIC, KEYS_TOPIC_SYSTEMS_SETTING);
        JSONArray setting = new JSONArray();
        if (skillPriorityOfSystemSetting != null) {
            JSONObject skillPriority = new JSONObject();
            JSONUtil.putQuietly(skillPriority, KEYS_KEY, KEYS_SKILL_PRIORITY);
            JSONUtil.putQuietly(skillPriority, KEYS_VALUE, skillPriorityOfSystemSetting);
            setting.put(skillPriority);
        }
        JSONUtil.putQuietly(object, KEYS_SETTING, setting);
        return object;
    }

    /**
     * 生成定位信息配置参数
     *
     * @return
     */
    public JSONObject toLocationJSON(JSONObject locationOfSystemSetting) {
        JSONObject object = new JSONObject();
        JSONUtil.putQuietly(object, KEYS_TOPIC, KEYS_TOPIC_SYSTEMS_SETTING);
        JSONArray setting = new JSONArray();
        if (locationOfSystemSetting != null) {
            JSONObject location = new JSONObject();
            JSONUtil.putQuietly(location, KEYS_KEY, KEYS_LOCATION);
            JSONUtil.putQuietly(location, KEYS_VALUE, locationOfSystemSetting);
            setting.put(location);
        }
        JSONUtil.putQuietly(object, KEYS_SETTING, setting);
        return object;
    }

    /**
     * 生成cinfo v1 vocab参数
     *
     * @return
     */
    public JSONObject toVocabsContactJSON(boolean addOrDelete, JSONArray data) {
        if (data != null) {
            JSONObject jsonObject = new JSONObject();
            JSONObject payloadJson = new JSONObject();
            JSONObject vocab = new JSONObject();
            JSONUtil.putQuietly(jsonObject, "option", addOrDelete ? "post" : "delete");
            JSONUtil.putQuietly(jsonObject, "data", data);
            JSONUtil.putQuietly(jsonObject, "type", "vocab");
            JSONUtil.putQuietly(payloadJson, "payload", jsonObject);
            JSONUtil.putQuietly(vocab, "data", payloadJson.toString());
            JSONUtil.putQuietly(vocab, "ctype", "vocabs");
            JSONUtil.putQuietly(vocab, "vocabName", "sys.联系人");
            return vocab;
        }
        return null;
    }

    @Override
    public String toString() {
        return super.toString();
    }

}
