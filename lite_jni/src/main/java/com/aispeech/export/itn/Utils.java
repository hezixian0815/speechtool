package com.aispeech.export.itn;

import android.text.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class Utils {

    public static final String SYS_NUMBER = "sys.序列号";
    public static final String SYS_PAPER = "sys.页码";

    /**
     * 特殊符号的unicode
     * //https://unicode-table.com/cn/blocks/
     */
    public static final String unicode = "[\\u0020-\\u002F\\u003A-\\u0040\\u005B-\\u0060\\u007B-\\u007F\\u00A1-\\u00BF\\u00D7\\u00F7"
            + "\\u2000-\\u2BFF"//符号
            + "\\u2E00-\\u2E7F"//追加标点
            + "\\u3000-\\u303F"//中日韩符号和标点
            + "\\u3099-\\u309C"//
            + "\\uFFF0-\\uFFFF"//杂项
            + "\\uD800\\uDC00-\\uD881\\uDC0F"//表情
            + "]";

//    public static final String textUnicode = ""

    public static String toChineseNumber(String str) {
        switch (str) {
            case "0":
                return "零";
            case "1":
                return "一";
            case "2":
                return "二";
            case "3":
                return "三";
            case "4":
                return "四";
            case "5":
                return "五";
            case "6":
                return "六";
            case "7":
                return "七";
            case "8":
                return "八";
            case "9":
                return "九";
        }
        return "";
    }

    public static String toarabNumber(String str) {
        switch (str) {
            case "零":
                return "0";
            case "一":
                return "1";
            case "二":
                return "2";
            case "三":
                return "3";
            case "四":
                return "4";
            case "五":
                return "5";
            case "六":
                return "6";
            case "七":
                return "7";
            case "八":
                return "8";
            case "九":
                return "9";
        }
        return "";
    }

    private static final Pattern digitPattern = Pattern.compile(".*\\d+.*");

    public static boolean hasDigit(String content) {
        boolean flag = false;
        Matcher m = digitPattern.matcher(content);
        if (m.matches()) {
            flag = true;
        }
        return flag;
    }

    private static final Pattern numbericPattern = Pattern.compile("[0-9]*");

    public static boolean isNumeric(String str) {
        Matcher isNum = numbericPattern.matcher(str);
        return isNum.matches();
    }

    /**
     * 替换小数点
     *
     * @param str 含小数点文本
     * @return 字符串
     */
    public static String toChinesePoint(String str) {
        if (str.contains(".")) {
            str = str.replace(".", "点");
        }
        return str;
    }

    /**
     * 转换成中文数字
     *
     * @param str 阿拉伯数字
     * @return 中文数字
     */
    public static String toChinese(String str) {
        StringBuffer sb = new StringBuffer();
        for (char c : str.toCharArray()) {
            if (TextUtils.isDigitsOnly(String.valueOf(c))) {
                sb.append(toChineseNumber(String.valueOf(c)));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 转换成阿拉伯数字
     *
     * @param str 中文数字
     * @return 阿拉伯数字
     */
    public static String toArab(String str) {
        StringBuffer sb = new StringBuffer();
        for (char c : str.toCharArray()) {
            String ret = toarabNumber(String.valueOf(c));
            if (!TextUtils.isEmpty(ret)) {
                sb.append(ret);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 是否包含特殊字符
     *
     * @param str text
     * @return true/false
     * @throws PatternSyntaxException Exception
     */
    public static boolean checkSpecialChar(String str) throws PatternSyntaxException {
//        String regEx = ".*[\"`~!@#$%^&*()+=|{}\\-':;,\\[\\].<>/?！￥…（）—【】‘；：”“’。，、？\\\\]+.*";
        Pattern p = Pattern.compile(unicode);
        Matcher m = p.matcher(str);
        return m.matches();
    }

    /**
     * 过滤掉特殊字符
     *
     * @param str 包含特殊字符的text
     * @return 不包含特殊字符的text
     * @throws PatternSyntaxException Exception
     */
    public static String filterString(String str) throws PatternSyntaxException {
//        String regEx = "[\"`~!@#$%^&*()+=|{}\\-':;,\\[\\].<>/?！￥…（）—【】‘；：”“’《》_·。，、？\\\\]";
        Pattern p = Pattern.compile(unicode);
        Matcher m = p.matcher(str);
        return m.replaceAll("").trim();
    }

    /**
     * 判断字符串是否纯英文
     *
     * @param str 字符串
     * @return true false
     */
    public static boolean isEnglish(String str) {
        byte[] bytes = str.getBytes();
        int i = bytes.length;// i为字节长度
        int j = str.length();// j为字符长度
        boolean result = i == j;
        return result;
    }

    /**
     * 将字符串的首字母转大写
     *
     * @param str 需要转换的字符串
     * @return str 转换后的字符串
     */
    public static String initialUpperCase(String str) {
        if (TextUtils.isEmpty(str)) return str;
        char[] chars = str.toCharArray();
        if (chars[0] >= 'a' && chars[0] <= 'z') {
            chars[0] -= 32;
            return String.valueOf(chars);
        }
        return str;
    }

    private static final Pattern englishPattern = Pattern.compile("[a-zA-Z]");

    /**
     * 是否含有英文字符
     *
     * @param text text
     * @return true/false
     */
    public static boolean hasEnglish(String text) {
        return englishPattern.matcher(text).find();
    }

    private static final Pattern specialCharPattern = Pattern.compile("[^a-zA-Z0-9\\u4e00-\\u9fa5]");

    /**
     * 过滤掉特殊字符
     *
     * @param text text
     * @return 删除特殊字符后的text
     */
    public static String delSpecialCharacters(String text) {
        if (TextUtils.isEmpty(text)) return text;
        return replaceAll(text, specialCharPattern, "");
    }

    public static String replaceAll(String srcStr, Pattern pattern, String replace) {
        return pattern.matcher(srcStr).replaceAll(replace);
    }

    /**
     * 是否包含特殊字符
     *
     * @param text text
     * @return true/false
     */
    public static boolean isSpecialCharacters(String text) {
        if (TextUtils.isEmpty(text)) return false;
        return checkSpecialChar(text);
    }

}
