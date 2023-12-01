package com.aispeech.lite.param;

import com.aispeech.common.JSONUtil;
import com.aispeech.export.SkillIntent;

import org.json.JSONObject;

/**
 * trigger intent params
 *
 * @author hehr
 */
public class TriggerIntentParams {

    private static final String KEY_TOPIC = "topic";

    private static final String VALUE_TOPIC = "dm.input.intent";

    private SkillIntent intent;

    private SpeechParams param;

    public SkillIntent getIntent() {
        return intent;
    }

    public TriggerIntentParams setIntent(SkillIntent intent) {
        this.intent = intent;
        return this;
    }

    public TriggerIntentParams(SkillIntent intent) {
        setIntent(intent);
    }

    public void setParam(SpeechParams param) {
        this.param = param;
    }

    public SpeechParams getParam() {
        return param;
    }

    public JSONObject toJSON() {

        JSONObject trigger = JSONUtil.build(getIntent().toString());

        if (trigger != null) {
            JSONUtil.putQuietly(trigger, KEY_TOPIC, VALUE_TOPIC);
        }

        return trigger;

    }
}
