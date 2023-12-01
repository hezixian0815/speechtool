package com.aispeech.export.interceptor;


import org.json.JSONObject;

public interface IInterceptor {

    Object intercept(JSONObject inputObj);

    String getName();

    interface Name {
        String WAKEUP = "wakeup.result";
        String DOA = "doa.result";

        String VAD_BEGIN = "vad.begin";
        String VAD_END = "vad.end";

        String DM_OUTPUT = "dm.output";

        String LOCAL_ASR_FIRST = "local.asr.first";
        String LOCAL_ASR_RESULT = "local.asr.result";
        String LOCAL_ITN_RESULT = "local.itn.result";

        String LOCAL_NLU_OUTPUT = "local.nlu.output";
        String LOCAL_NLU_DUI_OUTPUT = "local.nlu.dui.output";
        String LOCAL_NLU_AIDUI_OUTPUT = "local.nlu.aidui.output";

        String LOCAL_TTS_SYNTHESIS_FIRST = "local.tts.synthesis.first";
        String LOCAL_TTS_SYNTHESIS_END = "local.tts.synthesis.end";
        String LOCAL_TTS_PLAY_FIRST = "local.tts.play.first";

        String CLOUD_ASR_FIRST = "cloud.asr.first";
        String CLOUD_ASR_RESULT = "cloud.asr.result";

        String CLOUD_TTS_SYNTHESIS_FIRST = "cloud.tts.synthesis.first";
        String CLOUD_TTS_SYNTHESIS_END = "cloud.tts.synthesis.end";
        String CLOUD_TTS_PLAY_FIRST = "cloud.tts.play.first";

    }

    interface Layer {
        String KEY = "layer";
        String LITE = "lite";
        String DDS = "dds";
        String LYRA = "lyra";
    }

    interface FlowType {
        String KEY = "flowType";
        String RECEIVE = "receive";
        String CALLBACK = "callback";
    }
}
