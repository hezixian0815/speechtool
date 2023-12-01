package com.aispeech.common;

import android.text.TextUtils;
import android.util.Pair;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * PerjectName: duilite-for-car-android
 * Author: huwei
 * Describe:
 * Since 2020/11/3 12:01
 * Copyright(c) 2019 苏州思必驰信息科技有限公司  www.aispeech.com
 */
public class ArrayUtils {

    /**
     * String数组转float数组
     *
     * @param strings 要转换的字符串
     * @return float[]
     * @throws NumberFormatException String转float失败时抛出异常
     */
    public static float[] string2Float(String[] strings) {
        if (strings == null) {
            return null;
        }
        float[] floats = new float[strings.length];

        for (int i = 0; i < strings.length; i++) {
            floats[i] = Float.parseFloat(strings[i]);
        }
        return floats;
    }

    public static <T> T[] concatAll(T[] first, T[]... rest) {
        int totalLength = first.length;

        for (T[] array : rest) {
            totalLength += array.length;
        }

        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;

        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }

        return result;
    }


    /**
     * 拼接两个数组
     *
     * @param a 数组A
     * @param b 数组B
     * @return 新的数组
     */

    public static String[] concat(String[] a, String[] b) {
        String[] c = new String[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static float[] concat(float[] a, float[] b) {
        float[] c = new float[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    public static int[] concat(int[] a, int[] b) {
        int[] c = new int[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }

    /**
     * String数组转int数组
     *
     * @param strings 要转换的字符串
     * @return int[]
     * @throws NumberFormatException String转int失败时抛出异常
     */
    public static int[] string2Int(String[] strings) {
        if (strings == null) {
            return null;
        }
        int[] ints = new int[strings.length];

        for (int i = 0; i < strings.length; i++) {
            ints[i] = Integer.parseInt(strings[i]);
        }
        return ints;
    }

    public static Object remove(final Object array, final int index) {
        final int length = getLength(array);
        if (index < 0 || index >= length) {
            return array;
        }

        final Object result = Array.newInstance(array.getClass().getComponentType(), length - 1);
        System.arraycopy(array, 0, result, 0, index);
        if (index < length - 1) {
            System.arraycopy(array, index + 1, result, index, length - index - 1);
        }

        return result;
    }

    public static int getLength(final Object array) {
        if (array == null) {
            return 0;
        }
        return Array.getLength(array);
    }

    /**
     * float[] 数组最后添加一项
     *
     * @param floats
     * @param f
     * @return
     */
    public static float[] addFloatArry(float[] floats, float f) {
        if (floats == null) {
            return null;
        }
        float[] newArray = new float[floats.length + 1];
        for (int i = 0; i < floats.length; i++) {
            newArray[i] = floats[i];
        }
        newArray[floats.length] = f;
        return newArray;
    }

    /**
     * int[] 数组最后添加一项
     *
     * @param ints
     * @param i
     * @return
     */
    public static int[] addIntArry(int[] ints, int i) {
        if (ints == null) {
            return null;
        }
        int[] newArray = new int[ints.length + 1];
        for (int j = 0; j < ints.length; j++) {
            newArray[j] = ints[j];
        }
        newArray[ints.length] = i;
        return newArray;
    }

    /**
     * String[]数组最后添加一项
     *
     * @param arrays
     * @param s
     * @return
     */
    public static String[] addStringArray(String[] arrays, String s) {
        if (arrays == null) {
            return null;
        }
        List list = new ArrayList(Arrays.asList(arrays));
        list.add(s);
        String[] newArray = (String[]) list.toArray(new String[list.size()]);
        return newArray;
    }


    /**
     * String[]数组去重并添加新数据
     *
     * @param arrays
     * @param s
     * @return
     */
    public static Pair<String[], Boolean> addStringArrayToRepeat(String[] arrays, String s) {
        if (arrays == null) {
            return null;
        }
        List<String> list = new ArrayList(Arrays.asList(arrays));

        //暂时避开LinkedHashSet去重，防止用户传入的arrays中就带有重复的，LinkedHashSet去重后导致角标错乱
        boolean addSuccessful = true;
        for (String str : list) {
            if (!TextUtils.isEmpty(str) && str.equals(s)) {
                addSuccessful = false;
                break;
            }
        }
        String[] newArray;
        if (addSuccessful) {
            list.add(s);
            newArray = list.toArray(new String[list.size()]);
        } else {
            newArray = arrays;
        }
        return new Pair(newArray, addSuccessful);
    }
}
