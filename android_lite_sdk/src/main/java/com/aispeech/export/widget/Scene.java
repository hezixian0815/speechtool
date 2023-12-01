package com.aispeech.export.widget;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.common.Util;
import com.aispeech.lite.AISpeech;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

public class Scene {
    public static final String TAG = Scene.class.getName();

    /***
     * 场景名/生成的slot.json的名字
     */
    private String name;

    /**
     * Asr初始化所需的参数netBinPath：可以是assets路径，会拷贝到手机目录，也可以是手机的绝对路径
     */
    private String netBinPath;

    /**
     * 是否为默认场景
     */
    private boolean defaultScene;

    /***
     * net.bin中的词库的名称以及词库所对应的bin的名称。
     */
    private ArrayList<String> slots;

    /**
     * 场景对应的ExpandJson路径（这个参数给到Asr进行start，动态加载xbnf）
     */
    private String expandFnJsonPath;

    /***
     * 场景对应的json内容
     */
    private String expandFnJson;

    private ArrayList<File> slotBinFile = new ArrayList<>();

    public static String BIN_SUFFIX = "_slot.bin";
    public static String JSON_SUFFIX = "_slot.json";

    private Scene(Builder builder) {
        this(builder.name, builder.netBinPath, builder.defaultScene, builder.slots);
    }

    private Scene(String name, String netBinPath, boolean defaultScene, ArrayList<String> slots) {
        this.name = name;
        this.netBinPath = netBinPath;
        this.defaultScene = defaultScene;
        this.slots = slots;
        this.expandFnJsonPath = getNetPath() + name + JSON_SUFFIX;
    }

    public boolean isSlotBinFileExists() {
        try {
            if (slotBinFile != null) {
                for (File file : slotBinFile) {
                    if (!file.exists()) {
                        Log.e(TAG, "file is no exitst , asr update will be error, path = " + file);
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    //取出net.bin存放的目录
    public String getNetPath() {
        String path;
        if (netBinPath.startsWith("/")) {
            String[] split = netBinPath.split("/");
            String replace = split[split.length - 1];
            path = netBinPath.replaceAll(replace, "");
        } else {
            path = Util.getResourceDir(AISpeech.getContext()) + File.separator;
            String[] split = netBinPath.split("/");
            if (split != null && split.length > 1) {
                String replace = split[split.length - 1];
                path += netBinPath.replaceAll(replace, "");
            }
        }
        if (!path.contains("wakeupIncrement")) {
            path = path + "wakeupIncrement/";
            File file = new File(path);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
        return path;
    }

    //gram编译生成的bin的路径
    public String getGramSlotPath() {
        String path = getNetPath() + "build/";
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();
        }
        return path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNetBinPath() {
        return netBinPath;
    }

    public void setNetBinPath(String path) {
        this.netBinPath = path;
    }

    public boolean isDefaultScene() {
        return defaultScene;
    }

    public void setDefaultScene(boolean defaultScene) {
        this.defaultScene = defaultScene;
    }

    public String getExpandFnJsonPath() {
        return expandFnJsonPath;
    }

    public void setExpandFnJsonPath(String expandFnJsonPath) {
        this.expandFnJsonPath = expandFnJsonPath;
    }

    /**
     * 生成ExpandFnJson 内容
     *
     * @return ExpandFnJson
     */
    public String getExpandFnJson(ArrayList<String> jsonSlotList) {
        JSONObject expandFnObject = new JSONObject();
        try {
            if (slots != null) {
                if (slotBinFile != null) {
                    slotBinFile.clear();
                }
                JSONArray expandFnArray = new JSONArray();
                for (String slot : jsonSlotList) {
                    String binPath = getGramSlotPath() + slot + BIN_SUFFIX;
                    File file = new File(binPath);
                    if (slotBinFile != null) {
                        slotBinFile.add(file);
                    }
                    if (file.exists()) {
                        JSONObject slotsItem = new JSONObject();
                        slotsItem.put("name", slot.substring(slot.indexOf("_") + 1));
                        slotsItem.put("path", binPath);//场景+slot.bin文件路径
                        slotsItem.put("use_slot_sem", 1);
                        expandFnArray.put(slotsItem);
                    } else {
                        Log.e(TAG, "file is no exitst , asr update will be error, path = " + binPath);
                    }
                }
                expandFnObject.put("slot", expandFnArray);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return expandFnObject.toString();
    }

    public void setExpandFnJson(String expandFnJson) {
        this.expandFnJson = expandFnJson;
    }

    public ArrayList<String> getSlots() {
        return slots;
    }

    public void setSlots(ArrayList<String> slots) {
        this.slots = slots;
    }

    @Override
    public String toString() {
        return "Scene{" +
                "name='" + name + '\'' +
                ", netBinPath='" + netBinPath + '\'' +
                ", defaultScene=" + defaultScene +
                ", slots=" + slots +
                ", expandFnJsonPath='" + expandFnJsonPath + '\'' +
                ", expandFnJson='" + expandFnJson + '\'' +
                '}';
    }

    public static class Builder {
        /**
         * 场景名/生成的slot.json的名字
         */
        private String name;

        /***
         * 是否为默认scene,非必选
         */
        private boolean defaultScene;

        /***
         * 词库的名称以及词库所对应的bin的名称。
         */
        private ArrayList<String> slots;

        /**
         * 场景对应的 path，必填
         */
        private String netBinPath;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setNetBinPath(String netBinPath) {
            this.netBinPath = netBinPath;
            return this;
        }

        public Builder setDefaultScene(boolean defaultScene) {
            this.defaultScene = defaultScene;
            return this;
        }

        public Builder setSlots(ArrayList<String> slots) {
            this.slots = slots;
            return this;
        }

        public Scene Build() {
            checkParamas();
            return new Scene(this);
        }

        private void checkParamas() {

            if (TextUtils.isEmpty(name)) {
                throw new IllegalArgumentException("Illegal Argument: null name");
            }

            if (TextUtils.isEmpty(netBinPath)) {
                throw new IllegalArgumentException("Illegal Argument: null net bin netBinPath");
            }

            if (slots == null) {
                throw new IllegalArgumentException("Illegal Argument: null slots");
            }
        }

    }
}
