package com.aispeech.export.itn;

import android.os.SystemClock;
import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.export.Vocab;
import com.aispeech.lite.dm.Protocol;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * create by wanzhicheng on 20210416
 */
public class Convert {
    private final static String TAG = Convert.class.getSimpleName();
    private final JChineseConvertor jChineseConvertor;
    private static Convert instance;
    private final Map<String, String> map;
    private String mPath = "";

    private final Advanced advanced;

    private Convert() {
        jChineseConvertor = JChineseConvertor.getInstance();
        advanced = new Advanced();
        map = new HashMap<>();
    }

    /**
     * @return Convert
     */
    public static synchronized Convert getInstance() {
        if (instance == null) {
            synchronized (Convert.class) {
                if (instance == null) {
                    instance = new Convert();
                }
            }
        }
        return instance;
    }

    /**
     * 获取词库还原的高级配置
     *
     * @return Advanced
     */
    public Advanced getAdvanced() {
        return advanced;
    }

    public void setJChinesePath(String path) {
        mPath = path;
        if (!TextUtils.isEmpty(path)) {
            jChineseConvertor.setPath(path);
        } else {
            Log.e(TAG, "path is empty !!");
        }

    }


    /**
     * 将小写英文转换成大写英文
     * 将阿拉伯数字转换成中文数字
     * 将特殊符号过滤掉
     *
     * @param text text
     * @return encode
     */
    public synchronized String encode(String text) {
        return encode(text, true);
    }

    /**
     * 将阿拉伯数字转换成中文数字
     *
     * @param text text
     * @return encode
     */
    public synchronized String encodeNumber(String text) {
        return encode(text, false);
    }

    /**
     * 将小写英文转换成大写英文
     * 将阿拉伯数字转换成中文数字
     * 将特殊符号过滤掉
     *
     * @param text         text
     * @param needRestored 是否需要被恢复
     * @return encode
     */
    public synchronized String encode(String text, boolean needRestored) {
        Log.v(TAG, "text = " + text + "  needRestored " + needRestored);
        if (TextUtils.isEmpty(text)) return "";
        String ret = Utils.delSpecialCharacters(text);
        Log.v(TAG, "delSpecialCharacters ret" + ret);
        if (Utils.hasEnglish(text)) {
            if (ret != null) {
                ret = ret.toUpperCase();
            }
        }

        if (Utils.hasDigit(ret) && !Utils.isNumeric(ret) && !needRestored) {
            Log.v(TAG, "   ret " + ret + "  text  " + text);
            ret = Utils.toChinese(ret);
            map.put(ret, text);//只需要对数字还原
        }

        Log.v(TAG, "needRestored " + needRestored + "   ret " + ret + "  text  " + text);
        if (needRestored && !TextUtils.isEmpty(ret)
                && !TextUtils.isEmpty(text)
                && !text.equals(ret)) {
            map.put(ret, text);
            Log.d(TAG, "convert-- encode text : " + text + ", ret = " + ret + ",map= " + map.size());

        }
        return ret;
    }

    /**
     * 将小写英文转换成大写英文
     * 将阿拉伯数字转换成中文数字
     * 将特殊符号过滤掉
     *
     * @param text       text
     * @param onlyNumber 是否只需要数字
     * @return encode
     */
    public synchronized String encodeOnlyNumber(String text, boolean onlyNumber) {
        Log.v(TAG, "text = " + text + "  onlyNumber " + onlyNumber);
        if (TextUtils.isEmpty(text)) return "";
        String ret = Utils.delSpecialCharacters(text);
        Log.v(TAG, "delSpecialCharacters ret" + ret);

        if (Utils.hasDigit(ret) && !Utils.isNumeric(ret) && !TextUtils.isEmpty(ret)
                && !TextUtils.isEmpty(text)
                && !text.equals(ret)) {
            Log.v(TAG, "   ret " + ret + "  text  " + text);
            ret = Utils.toChinese(ret);
            map.put(ret, text);
        }
        return ret;
    }

    /**
     * @param text 需要decode的text
     * @return decode 返回未处理之前的text
     */
    public synchronized String decode(String text) {
        if (TextUtils.isEmpty(text)) return "";
        text = text.replaceAll(" ", "");
        String result = map.get(text);
        return result == null ? text : result;
    }

    private final Comparator<String> comparator = new Comparator<String>() {
        @Override
        public int compare(String s, String t1) {
            return s.length() > t1.length() ? -1 : 1;
        }
    };

    /**
     * 简体转繁体
     *
     * @param text 简体
     * @return 繁体
     */
    public String simplifiedToTraditional(String text) {
        if (TextUtils.isEmpty(text)) return text;
        if (TextUtils.isEmpty(mPath)) {
            Log.e(TAG, "mPath is empty !! please set itn ts.tab path");
            return text;
        }
        if (jChineseConvertor == null) {
            Log.e(TAG, "jChineseConvertor is null,please set ts.tab path!!!");
            return text;
        }
        return jChineseConvertor.s2t(text);
    }

    /**
     * 繁体转简体
     *
     * @param text 繁体
     * @return 简体
     */
    public String traditionalToSimplified(String text) {
        if (TextUtils.isEmpty(text)) return text;
        if (TextUtils.isEmpty(mPath)) {
            Log.e(TAG, "mPath is empty !! please set itn ts.tab path");
            return text;
        }

        if (jChineseConvertor == null) {
            Log.e(TAG, "jChineseConvertor is null,please set ts.tab path!!!");
            return text;
        }

        return jChineseConvertor.t2s(text);
    }

    public void clearEncodeData() {
        if (map != null) {
            map.clear();
        }
    }

    /**
     * 加工多个词库
     *
     * @param vocabs 词库列表
     * @return 处理后的词库列表
     */
    public Vocab[] encodeVocabs(Vocab[] vocabs) {
        if (vocabs == null || vocabs.length == 0) {
            return null;
        }
        List<String> encodedContents = new ArrayList<>();
        for (Vocab vocab : vocabs) {
            if (vocab.getContents() == null || vocab.getContents().size() == 0) {
                return null;
            }
            long start = SystemClock.uptimeMillis();
            if (advanced != null && advanced.vocabsWhiteList.contains(vocab.getName())) {
                clearEncodeData();
                for (String content : vocab.getContents()) {
                    String encoded = encodeVocab(content);
                    encodedContents.add(encoded);
                }
                vocab.updateContents(encodedContents);
            }
            Log.d(TAG, "encodeVocabs() called with: cost = [" + (SystemClock.uptimeMillis() - start) + "]" + "size = " + encodedContents.size());
        }
        return vocabs;
    }

    /**
     * 加工多个词库
     * @param  name 词库名称
     * @param  contents 词库列表
     * @return 处理后的词库列表
     */
    public List<String> encodeVocab(String name, List<String> contents, boolean isOnlyNumber) {
        if (advanced != null && advanced.vocabsWhiteList.contains(name)) {
            clearEncodeData();
            for (int i = 0; i < contents.size(); i++) {
                String old = contents.get(i);
                String content = "";
                if (!isOnlyNumber) {
                    content = encode(old);
                } else {
                    content = encodeNumber(old);
                }

                if (content.contains(":")) {//处理同义词问题 上传词库时 不能上传冒号后面的拼接部分
                    String[] arrayContent = content.split(":", 2);
                    if (arrayContent.length == 2) {
                        content = arrayContent[0];
                    }
                }
                contents.set(i, content);
                Log.d(TAG, "encodeVocab:" + old + "--->" + content);
            }
        }
        return contents;
    }

    /**
     * 加工单个词
     *
     * @param originalContent encode 的词条内容
     * @return 处理后的词条列表
     */
    public String encodeVocab(String originalContent) {
        String content = encode(originalContent);
        if (content.contains(":")) {//处理同义词问题 上传词库时 不能上传冒号后面的拼接部分
            String[] arrayContent = content.split(":", 2);
            if (arrayContent.length == 2) {
                content = arrayContent[0];
            }
        }
        Log.d(TAG, "encodeVocab:" + originalContent + "|--->" + content);

        return content;
    }

    /**
     * 还原报文中的联系人语义槽字段
     *
     * @param result asr+nlu+dm报文
     * @return 还原后的json
     */
    public String restore(String result) {
        JSONObject json = null;
        try {
            json = new JSONObject(result);
            String skillId = json.optString("skillId");
            if (!Convert.getInstance().advanced.skillIdsWhiteList.contains(skillId)) {
                Log.i(TAG, "restore skip : " + skillId);
                return result;
            }
            Log.i(TAG, "restore skillId = " + skillId);
            // 有可能nlu和dm同一个报文下发，故切记不要直接用else if判断
            json = dealNlu(json);
            json = dealDm(json);
            result = json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }


    private JSONObject dealNlu(JSONObject json) {
        try {
            if (json.has(Protocol.NLU)) {
                JSONObject nlu = json.optJSONObject(Protocol.NLU);
                if (nlu.has("semantics")) {
                    JSONObject semantics = nlu.optJSONObject("semantics");
                    if (semantics.has("request")) {
                        JSONObject request = semantics.optJSONObject("request");
                        if (request.has("slots")) {
                            JSONArray slotsJson = request.optJSONArray("slots");
                            if (slotsJson != null) {
                                for (int i = 0; i < slotsJson.length(); i++) {
                                    JSONObject slotObj = slotsJson.optJSONObject(i);
                                    String value = slotObj.optString("value");
                                    String name = slotObj.optString("name");
                                    String slots = decode(value);
                                    Log.i(TAG, "deal nlu decode : " + value + " = " + slots);
                                    slotObj.put("value", slots);

                                }
                                request.put("slots", slotsJson);
                            }
                        }
                        semantics.put("request", request);
                    }
                    nlu.put("semantics", semantics);
                }
                json.put(Protocol.NLU, nlu);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }

    private JSONObject dealDm(JSONObject json) {
        try {
            if (json.has(Protocol.DM)) {
                JSONObject dmJo = json.optJSONObject(Protocol.DM);
                if (dmJo.has(Protocol.DM_PARAM)) {
                    JSONObject paramJo = dmJo.optJSONObject(Protocol.DM_PARAM);
                    if (paramJo != null) {
                        String keyName = Protocol.DM_PARAM_CONTACT_NAME;
                        String name = paramJo.optString(Protocol.DM_PARAM_CONTACT_NAME);
                        if (TextUtils.isEmpty(name)) {
                            name = paramJo.optString(Protocol.DM_PARAM_CONTACT_EXT);
                            keyName = Protocol.DM_PARAM_CONTACT_EXT;
                        }
                        if (TextUtils.isEmpty(name)) {
                            return json;
                        }
                        paramJo.put(keyName, decode(name));
                        dmJo.put(Protocol.DM_PARAM, paramJo);
                        json.put(Protocol.DM, dmJo);
                        Log.i(TAG, "deal dm decode : " + name + " = " + name + " decodeName = " + decode(name));
                    }
                }

                if (dmJo.has(Protocol.DM_COMMAND)) {
                    JSONObject commandObj = dmJo.optJSONObject(Protocol.DM_COMMAND);
                    if (commandObj.has(Protocol.DM_PARAM)) {
                        JSONObject paramJo = commandObj.optJSONObject(Protocol.DM_PARAM);
                        if (paramJo != null) {
                            String nameTmp = paramJo.optString("name");
                            String name = decode(nameTmp);
                            Log.i(TAG, "name decode : " + name);
                            paramJo.put("name", name);

                            commandObj.put(Protocol.DM_PARAM, paramJo);
                            dmJo.put(Protocol.DM_COMMAND, commandObj);
                            json.put(Protocol.DM, dmJo);
                            Log.i(TAG, "deal dm decode : " + name + " = " + name + " decodeName = " + decode(name));
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json;
    }
}
