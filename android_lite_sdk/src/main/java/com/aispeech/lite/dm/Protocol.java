package com.aispeech.lite.dm;

/**
 * 服务端返回字段定义
 * @author hehr
 */
public class Protocol {

    /*** dds 服务返回字段 */
    public static final String DM = "dm";

    public static final String SKILL_ID = "skillId";

    public static final String SKILL = "skill";

    public static final String RECORDER_ID = "recordId";

    public static final String CONTEXT_ID = "contextId";

    public static final String SESSION_ID = "sessionId";

    public static final String DM_OUTPUT = "dm.output";

    public static final String TEXT = "text";

    public static final String VAR = "var";

    public static final String EOF = "eof";

    public static final String REQUEST_ID = "requestId";

    public static final String ERROR = "error";
    public static final String ERROR_ID = "errId";
    public static final String ERROR_MSG = "errMsg";

    public static final String SPEAK_URL = "speakUrl";

    public static final String ALIGNMENT = "alignment";
    public static final String ALIGNMENT_WORD = "word";
    public static final String ALIGNMENT_PINYIN = "pinyin";
    /**
     * SDK 不再对外输出该字段
     */
    @Deprecated
    public static final String NLU = "nlu";

    public static final String TOPIC = "topic";
    public static final String TOPIC_ASR_SPEECH_TEXT ="asr.speech.text";
    public static final String TOPIC_ASR_SPEECH_SENTENCE = "asr.speech.sentence";
    public static final String TOPIC_ASR_SPEECH_RESULT = "asr.speech.result";
    public static final String TOPIC_NLU_OUTPUT = "nlu.output";

    /**
     * dm 结构内字串
     */
    public static final String DM_INTENT_NAME = "intentName";

    public static final String DM_NLG = "nlg";

    public static final String DM_INTENT_ID = "intentId";

    public static final String DM_RUN_SEQUENCE = "runSequence";

    public static final String DM_TASK = "task";

    public static final String DM_COMMAND = "command";

    public static final String DM_WIDGET = "widget";

    public static final String DM_INPUT = "input";

    public static final String DM_STATUS = "status";

    public static final String DM_TASK_ID = "taskId";

    public static final String DM_SHOULD_END_SESSION = "shouldEndSession";

    public static final String DM_END_SKILL_DM = "endSkillDm";

    public static final String DM_API = "api";

    public static final String DM_PARAM = "param";

    public static final String DM_PARAM_CONTACT_NAME = "contact_name";

    public static final String DM_PARAM_CONTACT_PY = "contact_py";

    public static final String DM_PARAM_CONTACT_EXT = "contact_ext";

    public static final String DM_PARAM_CONTACT_EXT_PY = "contact_ext_py";

    public static final String DM_DATA_FROM = "dataFrom";

    public static final String DM_DATA_FROM_NATIVE = "native";

    public static final String DM_PINYIN = "pinyin";

    public static final String DM_COMMAND_API_DISCARD = "discardResponse";
    public static final String DM_COMMAND_PARAM = "param";
    public static final String DM_COMMAND_PARAMS_TYPE = "type";
    public static final String DM_NLU_DISCARD = "nluDiscard";
    public static final String DM_ASR_DISCARD = "asrDiscard";

}
