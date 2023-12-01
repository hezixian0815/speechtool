package com.aispeech.export.engines2.bean;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 解码网络实体类
 */
public class Decoder implements Parcelable {
    public enum Action {
        REGISTER("register"),//注册解码网络
        UNREGISTER("unregister"),//反注册解码网络
        QUERY("query");//查询解码网络

        private final String action;

        Action(String action) {
            this.action = action;
        }

        public String getAction() {
            return action;
        }
    }

    public enum NetType {
        GRAMMAR("grammar"),
        DYNAMIC("dynamic");

        private String type;

        NetType(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }

    private String action;
    private String type;
    private String name;
    private String netBin;
    private String expandFn;
    private String dynamicList;
    private boolean isHolderFst;
    private boolean useXbnfRec;
    private double threshHold;
    private Map<String, Double> dynamicThresholds;

    private Decoder() {
    }


    protected Decoder(Parcel in) {
        action = in.readString();
        type = in.readString();
        name = in.readString();
        netBin = in.readString();
        expandFn = in.readString();
        dynamicList = in.readString();
        isHolderFst = in.readByte() != 0;
        useXbnfRec = in.readByte() != 0;
        threshHold = in.readDouble();
        dynamicThresholds = in.readHashMap(HashMap.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(action);
        dest.writeString(type);
        dest.writeString(name);
        dest.writeString(netBin);
        dest.writeString(expandFn);
        dest.writeString(dynamicList);
        dest.writeByte((byte) (isHolderFst ? 1 : 0));
        dest.writeByte((byte) (useXbnfRec ? 1 : 0));
        dest.writeDouble(threshHold);
        dest.writeMap(dynamicThresholds);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Decoder> CREATOR = new Creator<Decoder>() {
        @Override
        public Decoder createFromParcel(Parcel in) {
            return new Decoder(in);
        }

        @Override
        public Decoder[] newArray(int size) {
            return new Decoder[size];
        }
    };

    public String getAction() {
        return action;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getNetBin() {
        return netBin;
    }

    public void setNetBin(String binPath) {
        this.netBin = binPath;
    }

    public String getExpandFn() {
        return expandFn;
    }

    public void setExpandFn(String fnPath) {
        this.expandFn = fnPath;
    }

    public String getDynamicList() {
        return dynamicList;
    }

    public boolean isHolderFst() {
        return isHolderFst;
    }

    public boolean isUseXbnfRec() {
        return useXbnfRec;
    }

    public double getThreshHold() {
        return threshHold;
    }

    public Map<String, Double> getDynamicThresholds() {
        return dynamicThresholds;
    }


    @Override
    public String toString() {
        return toJSON();
    }

    public String toJSON() {
        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("name", getName());
            jsonObj.put("type", getType());
            jsonObj.put("thresh", getThreshHold());
            if (!TextUtils.isEmpty(getNetBin())) {
                JSONObject grammarObj = new JSONObject();
                grammarObj.put("net_fn", getNetBin());
                grammarObj.put("env", getGrammarEnv());
                jsonObj.put(NetType.GRAMMAR.getType(), grammarObj);
            }
            if (!TextUtils.isEmpty(getDynamicList())) {
                JSONObject grammarObj = new JSONObject();
                grammarObj.put("env", getDynamicEnv());
                jsonObj.put(NetType.DYNAMIC.getType(), grammarObj);
            }

            jsonObj.put("name", getName());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObj.toString();
    }

    private String getGrammarEnv() {
        StringBuilder envSb = new StringBuilder();
        if (!TextUtils.isEmpty(getExpandFn())) {
            envSb.append("expand_fn=" + getExpandFn() + ";");
        }
        if (isUseXbnfRec()) {
            envSb.append("use_xbnf_rec=1;");
        }
        return envSb.toString();
    }

    private String getDynamicEnv() {
        StringBuilder envSb = new StringBuilder();
        envSb.append("dynamic_list=" + getDynamicList() + ";");
        return envSb.toString();
    }


    public static final class Builder {
        private Action action;
        private NetType type;
        private String name;
        private String netBin;
        private String expandFn;
        private boolean useXbnfRec;
        private String dynamicList;
        private Map<String, Double> dynamicThresholds;
        private boolean isHolderFst;
        private double threshHold;//当前解码网络的阈值

//        /**
//         * 内核暂不支持对单解码网络设置该字段，统一 start env 中开启
//         * 设置是否启用基于语法的语义识别
//         *
//         * @param useXbnfRec true 启用，默认为false
//         * @return {@link Builder}
//         */
//        public Builder useXbnfRec(boolean useXbnfRec) {
//            this.useXbnfRec = useXbnfRec;
//            return this;
//        }

        /**
         * 设置是否给ngram兜底，内部会仲裁ngram和当前解码网络
         *
         * @param isHolderFst 是否与ngram仲裁兜底
         * @return {@link Builder}
         */
        public Builder setHolderFst(boolean isHolderFst) {
            this.isHolderFst = isHolderFst;
            return this;
        }

        /**
         * 解码网络增删查的动作
         *
         * @param action 内核操作
         * @return {@link Builder}
         */
        public Builder setAction(Action action) {
            this.action = action;
            return this;
        }

        /**
         * 网络类型
         *
         * @param type 支持：grammar 和 dynamic
         * @return {@link Builder}
         */
        public Builder setType(NetType type) {
            this.type = type;
            return this;
        }

        /**
         * 解码网络的名称作为解码网络标志符。
         * 解码网络命名要求: 26 个英文字母、数字、下划线_ 的任意组合。长度不超过 64 字节（包含字符串
         * 末尾的'\0'）。
         * 下方的预留标识符为识别内部预留标识符，用户不可使用于 register 操作中。
         * 识别内部预留标识符：
         *  "ngram"
         * "ngram"是识别内部预留给 asr_new()时传递的 ngram 使用的。
         *  " asr_*"
         * 预留所有以"asr_"开头的标识符
         *
         * @param name 自定义网络名称
         * @return {@link Builder}
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * 当type=grammar时，需要指定对应的net.bin网络资源
         *
         * @param netBin net.bin路径，支持assets相对路径和sdcard绝对路径
         * @return {@link Builder}
         */
        public Builder setNetBin(String netBin) {
            this.netBin = netBin;
            return this;
        }

        /**
         * 当type=grammar且开启slots动态加载时，需要配置
         *
         * @param expandFn slots.json配置路径
         * @return {@link Builder}
         */
        public Builder setExpandFn(String expandFn) {
            this.expandFn = expandFn;
            return this;
        }

        /**
         * 当type=dynamic时，需要配置对应的热词指令列表
         *
         * @param commands 热词指令列字符串，逗号隔开："打开空调,关闭空调"
         * @return {@link Builder}
         */
        public Builder setDynamicList(String... commands) {

            StringBuilder builder = new StringBuilder();
            builder.append("\"");
            for (String d : commands) {
                if (!TextUtils.isEmpty(d)) {
                    builder.append(d + ",");
                }
            }
            builder.deleteCharAt(builder.lastIndexOf(","));
            builder.append("\"");
            this.dynamicList = builder.toString();
            return this;
        }

        /**
         * 设置当前解码网络的阈值
         * 当类型为dynamic时，为所有热词指令统一的阈值。
         * 同时支持对每个指令设置不同的阈值：see{@link #setDynamicThresholds}
         *
         * @param threshHold 解码网络的阈值，conf小于threshHold时，将会被丢弃
         * @return {@link Builder}
         */
        public Builder setThreshHold(double threshHold) {
            this.threshHold = threshHold;
            return this;
        }

        /**
         * 设置热词类型的解码网络的阈值集合，支持对不同的指令设置对应的阈值
         *
         * @param dynamicThresholds 热词指令和阈值的映射集合，当conf小于threshHold时，将会被丢弃
         * @return {@link Builder}
         */
        public Builder setDynamicThresholds(Map<String, Double> dynamicThresholds) {
            this.dynamicThresholds = dynamicThresholds;
            return this;
        }

        public Decoder build() {
            checkInvalid();

            Decoder decoder = new Decoder();
            decoder.action = action.getAction();
            if (type != null) {
                decoder.type = type.getType();
            }
            decoder.name = name;
            decoder.netBin = netBin;
            decoder.expandFn = expandFn;
            decoder.dynamicList = dynamicList;
            decoder.isHolderFst = isHolderFst;
            decoder.useXbnfRec = useXbnfRec;
            decoder.threshHold = threshHold;
            decoder.dynamicThresholds = dynamicThresholds;
            return decoder;
        }

        /**
         * 检测配置是否存在不允许的情况
         */
        private void checkInvalid() {
            if (action == null) {
                throw new IllegalArgumentException("must set asr decoder action!");
            }
            if (name == null) {
                throw new IllegalArgumentException("must set asr decoder name!");
            }

            if (action == Action.REGISTER) {
                if (type == null) {
                    throw new IllegalArgumentException("must set asr decoder type!");
                }

                if (type == NetType.GRAMMAR) {
                    if (TextUtils.isEmpty(netBin)) {
                        throw new IllegalArgumentException("must set asr netBin!");
                    }
                }

                if (type == NetType.DYNAMIC) {
                    if (TextUtils.isEmpty(dynamicList)) {
                        throw new IllegalArgumentException("must set dynamicList!");
                    }
                }
            }
        }
    }
}