package com.aispeech.lite.dm.dispather;

import android.text.TextUtils;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.export.Command;
import com.aispeech.export.NativeApi;
import com.aispeech.export.Speaker;
import com.aispeech.export.widget.callback.CallbackWidget;
import com.aispeech.lite.dm.Error;
import com.aispeech.lite.dm.Protocol;
import com.aispeech.lite.dm.Task;
import com.aispeech.lite.dm.cache.Cache;
import com.aispeech.lite.dm.cache.CacheLastAsrResult;
import com.aispeech.lite.dm.cache.Item;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;


/**
 * @author hehr
 * dm 结果分发
 */
public class Dispatcher {

    private static final String TAG = "Dispatcher";

    private DispatcherListener mListener;

    /**
     * 缓存待执行的task
     */
    private Cache mCache;

    /**
     * 是否对话透传
     */
    private boolean isRouteMode;

    /**
     * 是否全双工模式
     */
    private boolean isFullDuplex;

    /**
     * 统计拒识的次数：在一次对话中（topic = "recorder.stream.start"）连续收到多次拒识，那么需要过滤掉
     * 识别为空的拒识结果。过滤的逻辑 {@link Dispatcher#checkDiscardFilter(JSONObject)}
     */
    private int discardCounter;

    /**
     * 缓存上次的识别结果
     */
    private CacheLastAsrResult lastAsrResult;

    private boolean enableAlignment;

    public Dispatcher(DispatcherListener mListener, boolean isRouteMode, boolean isFullDuplex) {
        this.mListener = mListener;
        this.isRouteMode = isRouteMode;
        this.isFullDuplex = isFullDuplex;
        mCache = new Cache();
    }


    public Dispatcher(DispatcherListener mListener, boolean isRouteMode) {
        this(mListener, isRouteMode, false);
    }

    public Dispatcher(DispatcherListener mListener) {
        this(mListener, false, false);
    }


    /**
     * 解析云端DM返回结果
     *
     * @param object            对话服务下发的结果
     * @param currentRecorderId 当前轮请求的recorderId
     */
    public void deal(JSONObject object, String currentRecorderId) {

        //包含联系人拼音信息时，使用aligment中的拼音进行校准
        if (enableAlignment && object.has(Protocol.DM) && (object.toString().contains(Protocol.DM_PARAM_CONTACT_PY) ||
                object.toString().contains(Protocol.DM_PARAM_CONTACT_EXT_PY))) {
            fixDMContactPy(object);
        }
        //校准nlu中的拼音字段
        if (enableAlignment && object.has(Protocol.NLU)) {
            fixNluPy(object);
        }

        if (checkRecorderId(object.optString(Protocol.RECORDER_ID), currentRecorderId)) {
            optDM(object);
        } else {
            if (isFullDuplex) {
                Task task = Task.transform(object);
                if (task != null) {
                    Error mError = task.getDMError();
                    if (mError != null) {
                        optDM(object);
                        return;
                    }
                }
            }
            Log.d(TAG, "drop not current recorderId : " + currentRecorderId + ", message : " + object.toString());
        }

    }

    private void optDM(JSONObject object) {

//        Log.d(TAG, "optDM() called with: object = [" + object + "]");

        //1. execute asr
        executeAsr(object);
        executeNlu(object);
        executeDm(object);
        //2. execute sessionId
        executeSessionId(object.optString(Protocol.SESSION_ID));


        if (!isRouteMode) {
            Task task = Task.transform(object);
            if (task != null) {
                //4. execute widget
                executeWidget(task.getWidget());
                //5. executeSpeak
                executeSpeak(task.getSpeak());
                //6. executeCommand
                executeCommand(task, object);
                //7. executeError
                executeError(task.getDMError());
                //8. executeShouldEngSession
                executeShouldEndSession(task);
            }
        } else {
            // 对话透传模式，通过onAsr回调直接透出
            executeRoute(object);
        }


    }

    /**
     * @param remoteId  云端返回的 recordId
     * @param currentId 当前的 recordId
     * @return boolean 检查报文信息是否当前是当前请求的报文信息
     */
    private boolean checkRecorderId(String remoteId, String currentId) {
        if (TextUtils.isEmpty(currentId) || TextUtils.isEmpty(remoteId)) {
            return true;
        }
        return remoteId.contains(currentId);
    }

    /**
     * Dispatcher 中是否包含有未执行任务
     *
     * @return true 表示有未执行的任务，fase 表示没有待执行的任务
     */
    public boolean hasCache() {
        if (mCache == null) {
            return false;
        } else {
            return !mCache.isEmpty();
        }
    }

    /**
     * 清空缓存的任务
     */
    public void clearCache() {
        if (mCache != null) {
            Log.d(TAG, "clear cache task");
            mCache.clear();
        }
    }


    /**
     * 重置拒识计数
     */
    public void resetCounter() {
        discardCounter = 0;
        Log.i(TAG, "---resetCounter()");
    }

    /**
     * 销毁资源
     */
    public void release() {
        if (mCache != null) {
            mCache.clear();
            mCache = null;
        }
        if (mListener != null) {
            mListener = null;
        }
    }

    /**
     * nlg end
     */
    public void notifyNlgEnd() {
        if (!isRouteMode) {
            Cache.TaskIterator iterator = mCache.iterator();
            while (iterator.hasNext()) {
                Item item = iterator.next();
                switch (item.getKey()) {
                    case Cache.KEY_COMMAND:
                        runCommand((Command) item.getValue());
                        break;
                    case Cache.KEY_SHOULD_END_SESSION:
                        if ((Boolean) item.getValue()) {
                            runClose();
                        }
                        break;
                    case Cache.KEY_SHOULD_LISTEN:
                        if ((Boolean) item.getValue()) {
                            runListen();
                        }
                        break;
                    default:
                        Log.e(TAG, "invalid pending task");
                        break;
                }
            }

        } else {
            //透传模式，直接重启识别
            runListen();
        }

    }


    /**
     * 对话透传模式直接给出原始结果
     *
     * @param object JSONObject
     */
    private void executeRoute(JSONObject object) {
        if (checkDiscardFilter(object)) {
            Log.i(TAG, "---executeRoute() 过滤掉识别为空的拒识结果！---");
            return;
        }
        if (mListener != null) {
            boolean isLast =
                    ((object.optInt(Protocol.EOF) == 0) && (!TextUtils.isEmpty(object.optString(Protocol.TEXT)))) || object.optInt(Protocol.EOF) == 1;
            mListener.onAsr(isLast ? 1 : 0, object.toString());
        }
    }

    /**
     *  修复nlu结果中多音字拼音错误问题
     * @param object nlu json对象
     */
    private void fixNluPy(JSONObject object) {
        JSONObject nluJo = object.optJSONObject(Protocol.NLU);
        if (nluJo == null) {
            return;
        }
        if (!TextUtils.isEmpty(nluJo.optString(Protocol.DM_PINYIN))) {
            StringBuilder fixedPy = new StringBuilder();
            for (int i = 0; i < lastAsrPinyin.length; i++) {
                if (i == 0) {
                    fixedPy.append(lastAsrPinyin[i]);
                } else {
                    fixedPy.append(" ").append(lastAsrPinyin[i]);
                }
            }
            try {
                nluJo.put(Protocol.DM_PINYIN, fixedPy);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        JSONObject semanticJo = nluJo.optJSONObject("semantics");
        if (semanticJo == null) {
            return;
        }
        JSONObject requestJo = semanticJo.optJSONObject("request");
        if (requestJo == null) {
            return;
        }
        JSONArray slotsJa = requestJo.optJSONArray("slots");
        if (slotsJa == null || slotsJa.length() == 0) {
            return;
        }
        for (int i = 0; i < slotsJa.length(); i++) {
            try {
                JSONObject slotJo = slotsJa.getJSONObject(i);
                String name = slotJo.optString("rawvalue");
                if (TextUtils.isEmpty(slotJo.optString("rawpinyin")) || TextUtils.isEmpty(name)) {
                    continue;
                }
                StringBuilder rawpinyinSb = new StringBuilder();
                if (lastAsr != null && lastAsr.contains(name)) {
                    int firstIndex = lastAsr.indexOf(name); //找到联系人名在完整识别结果中的位置
                    for (int j = 0; j < name.length(); j++) {//根据下标和人名长度取出alignment中对应的拼音
                        if (firstIndex + j >= lastAsrPinyin.length) {
                            continue;
                        }
                        if (j == 0) {
                            rawpinyinSb.append(lastAsrPinyin[firstIndex + j]);
                        } else {
                            rawpinyinSb.append(" ").append(lastAsrPinyin[firstIndex + j]);
                        }
                    }
                    slotJo.put("rawpinyin", rawpinyinSb.toString());
                    slotsJa.put(i, slotJo);
                    requestJo.put("slots", slotsJa);
                    semanticJo.put("request", requestJo);
                    nluJo.put("semantics", semanticJo);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }



    }

    /**
     *  修复包含dm结果中联系人多音字拼音错误问题
     * @param object 包含联系人拼音的dm json对象
     */
    private void fixDMContactPy(JSONObject object) {
        Log.d(TAG, "fixDMContactPy() called with: object = [" + object + "]");
        JSONObject dmJo = object.optJSONObject(Protocol.DM);
        if (dmJo == null) {
            return;
        }
        StringBuilder contactPyFixed = new StringBuilder();
        if (dmJo.has(Protocol.DM_PARAM)) {
            JSONObject paramJo = dmJo.optJSONObject(Protocol.DM_PARAM);
            if (paramJo != null) {
                String name = paramJo.optString(Protocol.DM_PARAM_CONTACT_NAME);
                String pinyinKey = Protocol.DM_PARAM_CONTACT_PY;
                if (TextUtils.isEmpty(name)) {
                    name = paramJo.optString(Protocol.DM_PARAM_CONTACT_EXT);
                    pinyinKey = Protocol.DM_PARAM_CONTACT_EXT_PY;
                }
                if (TextUtils.isEmpty(name) || TextUtils.isEmpty(lastAsr)) {
                    return;
                }
                if (lastAsr != null && lastAsr.contains(name)) {
                    int firstIndex = lastAsr.indexOf(name); //找到联系人名在完整识别结果中的位置
                    for (int i = 0; i < name.length(); i++) {//根据下标和人名长度取出alignment中对应的拼音
                        if (firstIndex + i >= lastAsrPinyin.length) {
                            continue;
                        }
                        if (i == 0) {
                            contactPyFixed.append(lastAsrPinyin[firstIndex + i]);
                        } else {
                            contactPyFixed.append(" ").append(lastAsrPinyin[firstIndex + i]);
                        }
                    }
                }
                try {
                    paramJo.put(pinyinKey, contactPyFixed.toString());
                    dmJo.put(Protocol.DM_PARAM, paramJo);
                    object.put(Protocol.DM, dmJo);
                    Log.d(TAG, "dm object after fix contactpy = [" + object + "]");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * 过滤掉识别为空的拒识结果：根据缓存的识别结果 与 拒识类型一起来判断。
     * [过滤条件]：满足 识别拒识( asrDiscard ) + recordId + sessionId + text不为空。
     *
     * @param object 对话结果
     * @return 是否过滤拒识 true 过滤，false 不过滤
     */
    private boolean checkDiscardFilter(JSONObject object) {

        if (object == null || !object.has(Protocol.DM)) {
            return false;
        }

        JSONObject dmObj = object.optJSONObject(Protocol.DM);
        if (dmObj.has(Protocol.DM_COMMAND)) {
            JSONObject commandObj = dmObj.optJSONObject(Protocol.DM_COMMAND);
            if (commandObj == null || !commandObj.has(Protocol.DM_API)) {
                return false;
            }

            // command.api = discardResponse (标识拒识)
            if (Protocol.DM_COMMAND_API_DISCARD.equals(commandObj.optString(Protocol.DM_API))) {
                discardCounter++;
                Log.i(TAG, "discardCounter:" + discardCounter);
                if (!commandObj.has(Protocol.DM_COMMAND_PARAM)) {
                    return false;
                }
                JSONObject commandParams = commandObj.optJSONObject(Protocol.DM_COMMAND_PARAM);
                if (commandParams != null && commandParams.has(Protocol.DM_COMMAND_PARAMS_TYPE)) {

                    //过滤条件：识别拒识( asrDiscard ) + recordId + sessionId + text不为空
                    if (Protocol.DM_ASR_DISCARD.equals(commandParams.optString(Protocol.DM_COMMAND_PARAMS_TYPE))) {

                        if (lastAsrResult == null) {
                            return false;
                        }
                        if (!TextUtils.isEmpty(lastAsrResult.getText())) {
                            return false;
                        }
                        if (TextUtils.isEmpty(lastAsrResult.getSessionId())
                                || TextUtils.isEmpty(lastAsrResult.getRecordId())) {
                            return false;
                        }
                        if (object.has(Protocol.SESSION_ID) && object.has(Protocol.RECORDER_ID)) {
                            return lastAsrResult.getSessionId().equals(object.optString(Protocol.SESSION_ID))
                                    && lastAsrResult.getRecordId().equals(object.optString(Protocol.RECORDER_ID))
                                    && TextUtils.isEmpty(lastAsrResult.getText()) && discardCounter >= 2;
                        }
                    }
                }
            }
        }
        return false;
    }


    /**
     * 识别结果
     */
    private StringBuilder asrBuilder = new StringBuilder();

    private void clearAsr() {
        asrBuilder = new StringBuilder();
    }

    /****************************** execute ****************************/

    private void executeNlu(JSONObject object) {
        if (object != null && object.has(Protocol.NLU)) {
            JSONObject nluObj = object.optJSONObject(Protocol.NLU);
            //todo 新增onNlu回调
        }
    }

    private void executeDm(JSONObject object) {
        if (object != null && object.has(Protocol.DM)) {
            JSONObject dmObj = object.optJSONObject(Protocol.DM);
            //todo 新增onDm回调
        }
    }

    /**
     * 执行asr识别结果
     *
     * @param object 请求返回的json对象
     */
    private void executeAsr(JSONObject object) {
        if (object != null) {
            if (object.has((Protocol.EOF))) {
                if (object.has(Protocol.ALIGNMENT)) {
                    JSONArray alignmentJa = object.optJSONArray(Protocol.ALIGNMENT);
                    if (alignmentJa != null && alignmentJa.length() > 0) {
                        loadAlignmentPinyin(object);
                    }
                }
                if (!isRouteMode) { //非透传模式才回调识别结果，避免透传模式时混在一起
                    //中间识别结果
                    if (object.has(Protocol.VAR)) {
                        mListener.onAsr(0, object.optString(Protocol.VAR));
                        Log.d(TAG, "execute.asr : " + object.optString(Protocol.VAR));
                    }
                    //全双工模式eof==0拼接最终识别结果
                    else if (!TextUtils.isEmpty(object.optString(Protocol.TEXT)) && object.optInt(Protocol.EOF) == 0) {
                        asrBuilder.append(object.optString(Protocol.TEXT));
                    }
                    //半双工模式eof==1时给出最终识别结果
                    else if (!TextUtils.isEmpty(object.optString(Protocol.TEXT)) && object.optInt(Protocol.EOF) == 1) {
                        if (TextUtils.isEmpty(asrBuilder.toString())) {
                            asrBuilder.append(object.optString(Protocol.TEXT));
                        }
                    }
                    //eof ==1 时识别结束，回调最终识别结果并清除拼接字符串
                    if (object.optInt(Protocol.EOF) == 1) {
                        mListener.onAsr(1, asrBuilder.toString());
                        clearAsr();
                    }
                }
                //全双工模式：通过 缓存本地的识别结果  来实现过滤 识别为空的拒识 。
                if (isFullDuplex && object.optInt(Protocol.EOF) == 1) {
                    if (object.has(Protocol.TOPIC) && Protocol.TOPIC_ASR_SPEECH_TEXT.equals(
                            object.optString(Protocol.TOPIC))) {
                        lastAsrResult = new CacheLastAsrResult(
                                object.optString(Protocol.TOPIC),
                                object.optString(Protocol.DM_PINYIN),
                                object.optString(Protocol.RECORDER_ID),
                                object.optString(Protocol.SESSION_ID),
                                object.optInt(Protocol.EOF),
                                object.optString(Protocol.REQUEST_ID),
                                object.optString(Protocol.TEXT)
                        );
                    }
                }
            }
        }
    }


    /**
     * 缓存最新请求的识别结果
     */
    String lastAsr;
    /**
     * 缓存最新请求对应的alignment拼音结果，用于修复部分多音词拼音错误问题
     */
    String[] lastAsrPinyin;
    StringBuilder stringBuilderWord = new StringBuilder();
    StringBuilder stringBuilderPinyin = new StringBuilder();

    /**
     * 获取alignment字段中的识别结果和拼音，用于纠正语义槽中的拼音，解决部分多音字拼音错误问题。
     * @param object  最终识别结果json对象
     */
    private void loadAlignmentPinyin(JSONObject object) {

        stringBuilderWord.setLength(0);
        stringBuilderPinyin.setLength(0);

        if (object.has(Protocol.ALIGNMENT)) {

            JSONArray jsonArray = object.optJSONArray(Protocol.ALIGNMENT);
            if (jsonArray == null || jsonArray.length() == 0) {
                return;
            }
            for (int i = 0; i < jsonArray.length(); i++) {
                try {
                    JSONObject jo = jsonArray.getJSONObject(i);
                    stringBuilderWord.append(jo.optString(Protocol.ALIGNMENT_WORD));
                    if (i == 0) {
                        stringBuilderPinyin.append(jo.optString(Protocol.ALIGNMENT_PINYIN));
                    } else {
                        stringBuilderPinyin.append(" ").append(jo.optString(Protocol.ALIGNMENT_PINYIN));
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            lastAsr = stringBuilderWord.toString();
            lastAsrPinyin = stringBuilderPinyin.toString().split(" ");
            Log.d(TAG, "loadAlignmentPinyin() called with: "+"  words = " + lastAsr +
                    " pinyins = " + Arrays.toString(lastAsrPinyin));
        }

    }

    /**
     * 执行session id
     *
     * @param sessionId
     */
    private void executeSessionId(String sessionId) {
        if (!TextUtils.isEmpty(sessionId)) {
            mListener.onSessionId(sessionId);
        }
    }


    /**
     * 执行widget
     *
     * @param callbackWidget {@link CallbackWidget}
     */
    private void executeWidget(CallbackWidget callbackWidget) {
        if (callbackWidget != null) {
            Log.d(TAG, "execute.callbackWidget : " + callbackWidget.getType());
            mListener.onWidget(callbackWidget);
        }
    }

    /**
     * 执行 speaker
     *
     * @param speaker {@link Speaker}
     */
    private void executeSpeak(Speaker speaker) {
        if (speaker != null && !speaker.isEmpty()) {
            Log.d(TAG, "execute.speaker : " + speaker.toString());
            mListener.onSpeak(speaker);
        }
    }

    /**
     * 执行command
     *
     * @param task   {@link Task}
     * @param object JSONObject
     */
    private void executeCommand(Task task, JSONObject object) {

        if (checkDiscardFilter(object)) {
            Log.i(TAG, "---executeCommand() 过滤掉识别为空的拒识结果！---");
            return;
        }

        Command command = task.getCommand();
        if (command == null) {
            return;
        }
        Speaker speaker = task.getSpeak();
        if (TextUtils.isEmpty(command.getRunSequence())) {
            Log.d(TAG, "no sequence,execute command.");
            runCommand(command);
        } else {
            Log.d(TAG, "run sequence : " + command.getRunSequence());
            if (speaker != null && !task.getSpeak().isEmpty() && TextUtils.equals(Command.RunSequence.NLG_FIRST, command.getRunSequence())) {
                cacheCommand(command);
            } else {
                runCommand(command);
            }
        }

    }

    /**
     * 执行 是否需要结束对话
     *
     * @param task
     */
    private void executeShouldEndSession(Task task) {
        Log.d(TAG, "execute.should.end.session : " + task.isShouldEndSession() +
                ",endSkillDm = " + task.isEndSkillDm());
        if (task.isShouldEndSession()) {
            if (task.getSpeak() != null && !task.getSpeak().isEmpty() && !isFullDuplex) {
                cacheClose();
            } else {
                runClose();
            }
        } else {
            if (task.getNativeApi() != null) {
                runNativeApi(task.getNativeApi());
            } else {
                if (isFullDuplex) {
                    executeSessionId(task.getSessionId());
                } else {
                    if (task.getSpeak() != null) {
                        cacheListen();
                    } else {
                        executeSessionId(task.getSessionId());
                        runListen();
                    }
                }
            }
        }
    }

    /**
     * 执行对话错误提示
     *
     * @param error
     */
    private void executeError(Error error) {
        if (error != null) {
            Log.d(TAG, "execute.Error");
            Log.e(TAG, error.toString());
            mListener.onError(error);
        }

    }

    /*********************************** run *******************************/

    /**
     * 执行command
     *
     * @param command {@link Command}
     */
    private void runCommand(Command command) {
        if (command != null) {
            Log.d(TAG, "run.command : " + command.toString());
            mListener.onCommand(command);
        }
    }

    /**
     * 结束对话
     */
    private void runClose() {
        Log.d(TAG, "run.close.dialog");
        mCache.clear();
        mListener.onClose();
    }

    /**
     * 执行native api
     *
     * @param api {@link NativeApi}
     */
    private void runNativeApi(NativeApi api) {

        if (api != null) {
            Log.d(TAG, "run.native.api : " + api.toString());
            mListener.onNativeApi(api);
        }
    }

    /**
     * 进入倾听
     */
    private void runListen() {
        Log.d(TAG, "run.listen");
        mListener.onListen();
    }

    /**
     * cache 通知
     */
    private void runWait() {
        mListener.onWait();
    }

    /********************************** cache ******************************/
    /**
     * cache command
     *
     * @param command {@link Command}
     */
    private void cacheCommand(Command command) {
        if (mCache != null) {
            Log.d(TAG, "cache.command : " + command.toString());
            mCache.setCommand(command);
            runWait();
        }
    }

    /**
     * cache close
     */
    private void cacheClose() {
        if (mCache != null) {
            Log.d(TAG, "cache.close");
            mCache.setShouldEndSession(true);
            runWait();
        }
    }

    /**
     * cache listen
     */
    private void cacheListen() {
        if (mCache != null) {
            Log.d(TAG, "cache.listen");
            mCache.setShouldListen(true);
            runWait();
        }
    }

    public void setEnableAlignment(boolean enableAlignment) {
        this.enableAlignment = enableAlignment;
    }
}
