package com.aispeech.lite.dm;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.export.Command;
import com.aispeech.export.NativeApi;
import com.aispeech.export.Speaker;
import com.aispeech.export.widget.callback.CallbackWidget;

import org.json.JSONObject;

/**
 * dm输出结果
 * wiki：https://wiki.aispeech.com.cn/pages/viewpage.action?pageId=47231615
 *
 * @author hehr
 */
public class Task {

    /**
     * skill id
     */
    private String skillId;

    /**
     * record id
     */
    private String recordId;
    /**
     * session id
     */
    private String sessionId;
    /**
     * request id
     */
    private String requestId;

    /***  dm 结果  ***/
    /**
     * 是否结束当前对话，必选
     */
    private boolean shouldEndSession;

    /**
     * 技能结束对话
     */
    private boolean endSkillDm;

    /**
     * 对话控件{@link CallbackWidget}
     */
    private CallbackWidget callbackWidget;
    /**
     * 意图名称,非必选
     */
    private String intentName;
    /**
     * 本次请求的task名称,非必选
     */
    private String taskName;
    /**
     * 客户端需要执行的动作
     */
    private Command command;
    /**
     * native api
     */
    private NativeApi nativeApi;
    /**
     * 播报信息
     */
    private Speaker speaker;

    /**
     * 错误信息
     */
    private Error mError;

    public String getSkillId() {
        return skillId;
    }

    public Speaker getSpeak() {
        return speaker;
    }

    public String getRecordId() {
        return recordId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isShouldEndSession() {
        return shouldEndSession;
    }

    public boolean isEndSkillDm() {
        return endSkillDm;
    }

    public CallbackWidget getWidget() {
        return callbackWidget;
    }

    public String getIntentName() {
        return intentName;
    }

    public String getTaskName() {
        return taskName;
    }

    public Command getCommand() {
        return command;
    }

    public Error getDMError() {
        return mError;
    }

    public NativeApi getNativeApi() {
        return nativeApi;
    }

    private Task setSkillId(String skillId) {
        this.skillId = skillId;
        return this;
    }

    private Task setRecordId(String recordId) {
        this.recordId = recordId;
        return this;
    }


    private Task setSessionId(String sessionId) {
        this.sessionId = sessionId;
        return this;
    }

    private Task setRequestId(String requestId) {
        this.requestId = requestId;
        return this;
    }

    private Task setShouldEndSession(boolean shouldEndSession) {
        this.shouldEndSession = shouldEndSession;
        return this;
    }


    private Task setWidget(CallbackWidget callbackWidget) {
        this.callbackWidget = callbackWidget;
        return this;
    }

    private Task setIntentName(String intentName) {
        this.intentName = intentName;
        return this;
    }

    public Task setTaskName(String taskName) {
        this.taskName = taskName;
        return this;
    }

    public Task setCommand(Command command) {
        this.command = command;
        return this;
    }

    public Task setDMError(Error Error) {
        this.mError = Error;
        return this;
    }

    public Task setSpeak(Speaker speaker) {
        this.speaker = speaker;
        return this;
    }

    public Task setNativeApi(NativeApi nativeApi) {
        this.nativeApi = nativeApi;
        return this;
    }

    public Task setEndSkillDm(boolean endSkillDm) {
        this.endSkillDm = endSkillDm;
        return this;
    }

    private Task() {
    }

    public static Task build() {
        return new Task();
    }

    @Override
    public String toString() {
        return "Task{" +
                "skillId='" + skillId + '\'' +
                ", recordId='" + recordId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", requestId='" + requestId + '\'' +
                ", shouldEndSession=" + shouldEndSession +
                ", callbackWidget=" + callbackWidget +
                ", intentName='" + intentName + '\'' +
                ", taskName='" + taskName + '\'' +
                ", command=" + command +
                ", nativeApi=" + nativeApi +
                ", speaker=" + speaker +
                ", Error=" + mError +
                '}';
    }

    /**
     * 将云端结果转换成 Task
     *
     * @param object 云端json结果
     * @return {@link Task}
     */
    public static Task transform(JSONObject object) {

        Task task = build()
                .setSessionId(object.optString(Protocol.SESSION_ID))
                .setRecordId(object.optString(Protocol.RECORDER_ID))
                .setRequestId(object.optString(Protocol.REQUEST_ID))
                .setSkillId(object.optString(Protocol.SKILL_ID));

        JSONObject dm = object.optJSONObject(Protocol.DM);

        if (dm == null) {
            Log.d(TAG, "transform failed,invalid dm result");
            return null;
        }

        //intent name
        if (dm.has(Protocol.DM_INTENT_NAME)) {
            task.setIntentName(dm.optString(Protocol.DM_INTENT_NAME));
        }
        // should end session
        if (dm.has(Protocol.DM_SHOULD_END_SESSION)) {
            task.setShouldEndSession(dm.optBoolean(Protocol.DM_SHOULD_END_SESSION));
        }

        // end skill dm
        if (dm.has(Protocol.DM_END_SKILL_DM)) {
            task.setEndSkillDm(dm.optBoolean(Protocol.DM_END_SKILL_DM));
        }

        //Task name
        if (dm.has(Protocol.DM_TASK)) {
            task.setTaskName(dm.optString(Protocol.DM_TASK));
        }
        //nlg
        if (dm.has(Protocol.DM_NLG)) {
            task.setSpeak(Speaker.transform(object));
        }
        //Error
        if (object.has(Protocol.ERROR)) {
            task.setDMError(Error.transform(object.optJSONObject(Protocol.ERROR)));
        }
        //native api
        if (TextUtils.equals(Protocol.DM_DATA_FROM_NATIVE, dm.optString(Protocol.DM_DATA_FROM))) {
            task.setNativeApi(NativeApi.transform(dm, task.getSkillId(), task.getTaskName(), task.getIntentName()));
        }
        //callbackWidget
        if (dm.has(Protocol.DM_WIDGET)) {
            task.setWidget(CallbackWidget.transForm(dm.optJSONObject(Protocol.DM_WIDGET), task.getSkillId(), task.getTaskName(), task.getIntentName()));
        }
        //command
        if (dm.has(Protocol.DM_COMMAND) ) {
            task.setCommand(Command.transform(dm, task.getSkillId(), task.getTaskName(), task.getIntentName()));
        }

        return task;
    }


    private static final String TAG = Task.class.getSimpleName();

}
