package com.aispeech.common;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextSplitter {
    /**
     * 正则匹配中英文基本标点符号
     */
    private static String reg = "[,.;!?，。；！？]";
    private static Pattern mPattern = Pattern.compile(reg);

    /**
     * 将长文本分割为短句，每间隔spiltIndex个字符进行一次分割
     *
     * @param str        文本
     * @param spiltIndex 切割点
     * @return 切割的文本集合
     */
    public static List<String> spiltText(String str, int spiltIndex) {
        List<String> list = new ArrayList<>();
        if (null != str && str.length() >= spiltIndex) {
            for (int i = 0; i <= str.length() / spiltIndex; i++) {
                if (i == (str.length() / spiltIndex)) {
                    if (str.length() % spiltIndex != 0)
                        list.add(str.substring(i * spiltIndex));
                } else {
                    list.add(str.substring(i * spiltIndex, (i + 1) * spiltIndex));
                }
            }
        } else {
            list.add(str);
        }
        return list;
    }


    /**
     * 将长文本分割为短句，每超过 spiltUnit 个字符向后寻找标点符号进行断句
     *
     * @param str       文本
     * @param spiltUnit 断句跨度单位
     * @return 返回短句集合
     */
    public static List<String> spiltTextByPunctuation(String str, int spiltUnit) {

        List<String> list = new ArrayList<>();
        int lastSpiltIndex = 0;
        if (null != str && str.length() > spiltUnit) {
            Matcher matcher = mPattern.matcher(str);
            while (matcher.find()) {
                // 每次断句 间隔需大于spiltUnit
                int end = matcher.end();
                if (end - lastSpiltIndex > spiltUnit) {
                    list.add(str.substring(lastSpiltIndex, end));
                    lastSpiltIndex = end;
                    Log.i("spiltWords", "matcher=" + matcher.group() + " end=" + end);
                }
            }
            // 添加剩余段落
            if (lastSpiltIndex != str.length()) {
                list.add(str.substring(lastSpiltIndex));
            }
        } else {
            list.add(str);
        }

        return list;
    }
}
