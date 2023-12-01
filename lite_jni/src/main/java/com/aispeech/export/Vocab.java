package com.aispeech.export;


import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.export.itn.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 词库操作类
 * <p>
 * 可以对词库的词条进行增删改。
 * <p>
 *
 * @author Jinrui on 18-10-11.
 * @see <a href="https://www.dui.ai/docs/ct_common_Andriod_SDK">https://www.dui.ai/docs/ct_common_Andriod_SDK</a>
 */
public class Vocab {
    public static final String TAG = "VOCAB";
    /**
     * 词库操作：添加词条
     */
    public static final String ACTION_INSERT = "ACTION_INSERT";

    /**
     * 词库操作：删除词条
     */
    public static final String ACTION_REMOVE = "ACTION_REMOVE";

    /**
     * 词库操作：清空所有词条后，添加词条
     */
    public static final String ACTION_CLEAR_AND_INSERT = "ACTION_CLEAR_AND_INSERT";

    /**
     * 词库操作：清空所有词条
     */
    public static final String ACTION_CLEAR_ALL = "ACTION_CLEAR_ALL";

    /**
     * 定义离线ngram内置的词库名
     */
    public enum Name {
        CONTACTS("sys.联系人"),
        RADIOS("sys.电台"),
        SONGS("sys.歌曲名"),
        SINGERS("sys.歌手名"),
        CAR_APPS("sys.车载控制应用名");

        private final String name;

        Name(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    private String action;
    private String name;
    private List<String> segmentContents = new ArrayList<>();
    private List<String> contents;
    /**
     * 离线词库保存路径
     */
    private String outputPath;

    /**
     * 离线词库内容输入路径，如：/sdcard/aispeech/semantic/联系人.txt
     */
    private String inputPath;

    /**
     * 是否分词：默认 true ，即分词
     */
    private boolean useSegment = true;

    /***
     * 语义同义词 : 默认false
     */
    private boolean useSynonym = true;

    public Vocab() {
    }

    public String getAction() {
        return action;
    }

    public String getName() {
        return name;
    }

    /**
     * 是否将带有数字的文本转换为分词形式
     */
    boolean useTransformArab = false;

    public List<String> getContents() {
        List<String> allContents = new ArrayList<>();
        if (contents != null) {
            Log.d(TAG, " useSynonym " + useSynonym);
            if (useSynonym) {
                allContents.addAll(spiltContents(contents));
            } else {
                allContents.addAll(contents);
            }
        }
        if (segmentContents != null && segmentContents.size() > 0) {
            allContents.removeAll(segmentContents);
            allContents.addAll(segmentContents);
        }

        // 放在拆分联系人后；对拆分后的所有词进行转换
        if (useTransformArab) {
            transformArab(allContents);
        }
        return allContents;
    }


    /**
     * 将带有数字的文本（非纯数字）转化为分词形式
     * 示例词条转换: 测试2   ==》 测试二:测试2
     *
     * @param contents
     */
    private void transformArab(List<String> contents) {

        if (contents == null || contents.size() == 0) return;

        for (int index = 0; index < contents.size(); index++) {
            String content = contents.get(index);

            if (content.contains(":")) continue;

            // 不使用正则: 效率比正则高约10倍
            boolean hasDigit = false;
            boolean hasOther = false;
            for (int i = 0; i < content.length(); i++) {

                if (Character.isDigit(content.charAt(i))) {
                    hasDigit = true;
                } else {
                    hasOther = true;
                }

                if (hasDigit && hasOther) {
                    String synonym = Utils.toChinese(content);
                    contents.set(index, synonym + ":" + content);
                    break;
                }
            }
        }
    }

    /***
     *
     * 输入 :["个人中心","车控车设:车控,车设,车辆设置",雷石"]
     * 输出 :["个人中心","车控车设","车控,"车设,"车辆设置",雷石"]
     *
     * 同义词拆分，需要转成单词设置给内核
     * @param contents
     * @return
     */
    private List<String> spiltContents(List<String> contents){
        List<String> contentsArray = new ArrayList<>();
        if(null != contents && contents.size() > 0){
            for(String vocab : contents){
                if(vocab.contains(":")){
                    String[] datas = vocab.split(":");
                    String majorStr = datas[0];
                    contentsArray.add(majorStr);

                    String minorStr = datas[1];
                    String[] minors = minorStr.split(",");
                    for (String minor : minors) {
                        contentsArray.add(minor);
                    }
                }else{
                    contentsArray.add(vocab);
                }
            }
        }
        Log.d(TAG, "contents array" + contentsArray);
        return contentsArray;
    }


    public boolean isUseSegment() {
        return useSegment;
    }

    /**
     * 是否原始输入词条，区分分词后词条
     *
     * @param content 词条
     * @return true/false
     */
    public boolean isOrigin(String content) {
        return contents.contains(content);
    }


    /**
     * 更新content内容
     *
     * @param contents list
     */
    public void updateContents(List<String> contents) {
        this.contents = contents;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getInputPath() {
        return inputPath;
    }

    /**
     * 进行分词
     *
     * @param contents 原始contents
     * @return 分词后的 contents
     */
    private List<String> segment(List<String> contents) {
        if (contents == null || contents.isEmpty()) {
            return contents;
        }

        for (String content : contents) {
            if (TextUtils.isEmpty(content)) {
                continue;
            }
            splitContacts(content, segmentContents);
        }
        //去重
        HashSet<String> set = new HashSet<>(segmentContents);
        return new ArrayList<>(set);
    }

    private static final Pattern noTextPattern = Pattern.compile("[^\\u4E00-\\u9FA5A-Za-z0-9]");
    private static final Pattern noChinesePattern = Pattern.compile("[^\\u4E00-\\u9FA5]");

    public static String replaceAll(String srcStr, Pattern pattern, String replace) {
        return pattern.matcher(srcStr).replaceAll(replace);
    }

    //拆分联系人
    public static void splitContacts(String name, List<String> splitContacts) {
        if (TextUtils.isEmpty(name)) {
            return;
        }
        //添加原始名称
        splitContacts.add(name);
        //添加去掉特殊符号的原始名称
        name = replaceAll(name, noTextPattern, "");
        splitContacts.add(name);
        String nameChineseOnly = replaceAll(name, noChinesePattern, "");
        //添加去除非中文字符后的名称
        if (!TextUtils.equals(nameChineseOnly, name)) {
            splitContacts.add(nameChineseOnly);
        }
//        添加拆分后的词语,大于等于三个字的才做拆分,拆分后只取大于等于两个字的词


//       3个字 12,21拆分
        if (nameChineseOnly != null && nameChineseOnly.length() == 3) {
            splitContacts.add(nameChineseOnly.substring(0, 2));
            splitContacts.add(nameChineseOnly.substring(1, 3));
        }
//        4个字 22拆分
        if (nameChineseOnly != null && nameChineseOnly.length() == 4) {
            splitContacts.add(nameChineseOnly.substring(0, 2));
            splitContacts.add(nameChineseOnly.substring(2, 4));
        }
//        大于等于5个字  取前2前3、后2后3及第二三个字  其余舍弃
        if (nameChineseOnly != null && nameChineseOnly.length() >= 5) {
            splitContacts.add(nameChineseOnly.substring(0, 2));
            splitContacts.add(nameChineseOnly.substring(0, 3));
            int length = nameChineseOnly.length();
            splitContacts.add(nameChineseOnly.substring(length - 2, length));
            splitContacts.add(nameChineseOnly.substring(length - 3, length));
            splitContacts.add(nameChineseOnly.substring(1, 3));
        }
    }

    public static class Builder {

        private String action;

        private String name;

        private List<String> contents;

        /**
         * 是否分词：默认 true ，即分词
         */
        private boolean useSegment = true;

        /**
         * 是否拆分同义词：默认 true ，拆分
         */
        private boolean useSynonym = true;

        /**
         * 离线词库保存路径
         */
        private String outputPath;

        /**
         * 离线词库内容输入路径，如：/sdcard/aispeech/semantic/联系人.txt
         */
        private String inputPath;
        /**
         * 是否将带有数字的文本转换为分词形式
         */
        boolean useTransformArab = false;

        /**
         * 获取词库操作
         *
         * @return string     需要执行的词库操作
         * @see #setAction(String)
         */
        public String getAction() {
            return action;
        }

        /**
         * 设置需要执行的词库操作
         *
         * @param action 需要执行的词库操作
         * @return Builder，便于连续调用
         * @see #getAction()
         * @see #ACTION_INSERT
         * @see #ACTION_REMOVE
         * @see #ACTION_CLEAR_AND_INSERT
         * @see #ACTION_CLEAR_ALL
         */
        public Builder setAction(String action) {
            this.action = action;
            return this;
        }

        /**
         * 获取词库名称
         *
         * @return 词库名称
         * @see #setName(String)
         */
        public String getName() {
            return name;
        }

        /**
         * 设置词库名称
         * <p>
         * 可以是自定义词库，比如“我的应用”。<br>
         * 可以是系统词库，比如“sys.联系人”。
         *
         * @param name 词库名称
         * @return Builder，便于连续调用
         * @see #getName()
         */
        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        /**
         * 是否开启数字转换为同义词格式
         * 示例词条转换: 测试2   ==》 测试二:测试2
         *
         * @param useTransformArab 开关
         */
        public Builder setUseTransformArab(boolean useTransformArab) {
            this.useTransformArab = useTransformArab;
            return this;
        }


        /**
         * 设置内置词库名称
         *
         * @param name 词库名称
         * @return Builder，便于连续调用
         * @see Name
         */
        public Builder setName(Name name) {
            this.name = name.getName();
            return this;
        }

        /**
         * 获取词条列表
         *
         * @return 词条列表
         * @see #setContents(List)
         * @see #addContent(String)
         */
        public List<String> getContents() {
            return contents;
        }

        /**
         * 设置需要操作的词条列表
         * <p>
         * 若需要上传带同义词的词条，格式如下："${词条取值}:${同义词1}[,${同义词2}]"。<br>
         * 比如："电灯:电灯泡,灯泡"，"支付宝:支护宝"。
         *
         * @param contents 词条列表
         * @return Builder，便于连续调用
         * @see #addContent(String)
         * @see #getContents()
         */
        public Builder setContents(List<String> contents) {
            this.contents = contents;
            return this;
        }

        /**
         * 向词条列表中添加词条
         *
         * @param content 词条
         * @return Builder，便于连续调用
         * @see #setContents(List)
         * @see #getContents()
         */
        public Builder addContent(String content) {
            if (null == this.contents) {
                this.contents = new ArrayList<>();
            }

            this.contents.add(content);
            return this;
        }

        /**
         * 设置是否分词
         *
         * @param useSegment true 分词，false 不分词，默认 true
         * @return Builder，便于连续调用
         */
        public Builder setUseSegment(boolean useSegment) {
            this.useSegment = useSegment;
            return this;
        }

        /**
         * 设置是否拆分同义词 ：识别需要拆 ，语义不需要拆分
         *
         * @param useSynonym true 拆分同义词，false 不拆分同义词，默认 true
         * @return Builder，便于连续调用
         */
        public Builder setUseSynonym(boolean useSynonym) {
            this.useSynonym = useSynonym;
            return this;
        }

        /**
         * 设置词库保存路径，仅离线识别更新词库时需要设置
         *
         * @param outputPath 词库保存路径
         * @return Builder，便于连续调用
         */
        public Builder setOutputPath(String outputPath) {
            this.outputPath = outputPath;
            return this;
        }

        /**
         * 设置词库保存路径，仅离线识别更新词库时需要设置，与{@link #addContent(String)}和{@link #setContents(List)}相斥
         *
         * @param inputPath 词库内容输入路径
         * @return Builder，便于连续调用
         */
        public Builder setInputPath(String inputPath) {
            this.inputPath = inputPath;
            return this;
        }

        private void checkParams() {
            if (null == this.name) {
                throw new IllegalArgumentException("Illegal Argument: null name");
            }

            if (null == this.action) {
                throw new IllegalArgumentException("Illegal Argument: null action");
            }

//            if ((null == this.contents || 0 == this.contents.size())
//                    && TextUtils.isEmpty(inputPath)
//                    && (ACTION_INSERT.equals(this.getAction()) || ACTION_REMOVE.equals(this.getAction()) || ACTION_CLEAR_AND_INSERT.equals(this.getAction()))) {
//                throw new IllegalArgumentException("Illegal Argument: " + this.getAction() + " without contents");
//            }
        }

        /**
         * 创建 Vocab
         *
         * @return {@link Vocab}
         */
        public Vocab build() {
            checkParams();
            Vocab vocab = new Vocab();
            vocab.action = this.action;
            vocab.name = this.name;
            vocab.useSegment = useSegment;
            if (useSegment) {
                vocab.segmentContents = vocab.segment(contents);
            }
            vocab.useSynonym = this.useSynonym;
            vocab.contents = this.contents;
            vocab.outputPath = this.outputPath;
            vocab.inputPath = this.inputPath;
            vocab.useTransformArab = this.useTransformArab;
            return vocab;
        }
    }


    @Override
    public String toString() {
        return "Vocab{" +
                "action='" + action + '\'' +
                ", name='" + name + '\'' +
                ", contents=" + contents + '\'' +
                ", useSegment=" + useSegment +
                ", addNumSynonym=" + useTransformArab +
                '}';
    }
}
