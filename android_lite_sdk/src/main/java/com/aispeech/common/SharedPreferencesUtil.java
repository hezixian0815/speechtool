package com.aispeech.common;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesUtil {
    //文件名称为config
    private static final String PREFERENCE_NAME = "duilite_config";
    //可以在此定义常亮，当做key使用
    //版本号
    public static final String SDK_INIT_UPDATE_TIME = "SDK_INIT_UPDATE_TIME";
    public static final String SDK_INIT_UPDATE_INTERVAL = "SDK_INIT_UPDATE_INTERVAL";

    private static SharedPreferences sharedPreferences;

    /**
     * 写入Boolean变量至sharedPreferences中
     *
     * @param context 上下文环境
     * @param key     存储节点名称
     * @param value   存储节点的值
     */
    public static void putBoolean(Context context, String key, boolean value) {
        //(存储节点文件名称，读写方式)
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
        sharedPreferences.edit().putBoolean(key, value).commit();
    }

    /**
     * 读取boolean标识从sharedPreferences中
     *
     * @param context 上下文环境
     * @param key     存储节点名称
     * @param value   没有此节点的默认值
     * @return 默认值或者此节点读取到的结果
     */
    public static boolean getBoolean(Context context, String key, boolean value) {
        //(存储节点文件名称,读写方式)
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
        return sharedPreferences.getBoolean(key, value);
    }

    /**
     * 写入String变量至sharedPreferences中
     *
     * @param context 上下文环境
     * @param key     存储节点名称
     * @param value   存储节点的值String
     */
    public static void putString(Context context, String key, String value) {
        //存储节点文件的名称，读写方式
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
        sharedPreferences.edit().putString(key, value).commit();
    }

    /**
     * 读取String标识从sharedPreferences中
     *
     * @param context  上下文环境
     * @param key      存储节点名称
     * @param defValue 没有此节点的默认值
     * @return 返回默认值或者此节点读取到的结果
     */
    public static String getString(Context context, String key, String defValue) {
        //存储节点文件的名称，读写方式
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
        return sharedPreferences.getString(key, defValue);
    }

    /**
     * 写入int变量至sharedPreferences中
     *
     * @param context 上下文环境
     * @param key     存储节点名称
     * @param value   存储节点的值int
     */
    public static void putInt(Context context, String key, int value) {
        //存储节点文件的名称，读写方式
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
        sharedPreferences.edit().putInt(key, value).commit();
    }

    /**
     * 读取int标识从sharedPreferences中
     *
     * @param context  上下文环境
     * @param key      存储节点名称
     * @param defValue 没有此节点的默认值
     * @return 返回默认值或者此节点读取到的结果
     */
    public static int getInt(Context context, String key, int defValue) {
        //存储节点文件的名称，读写方式
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
        return sharedPreferences.getInt(key, defValue);
    }

    /**
     * 写入long变量至sharedPreferences中
     * @param context 上下文环境
     * @param key     存储节点名称
     * @param value   存储节点的值long
     */
    public static void putLong(Context context, String key, long value) {
        //存储节点文件的名称，读写方式
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
        sharedPreferences.edit().putLong(key, value).commit();
    }

    /**long标识从sharedPreferences中
     *
     * @param context  上下文环境
     * @param key      存储节点名称
     * @param defValue 没有此节点的默认值
     * @return 返回默认值或者此节点读取到的结果
     */
    public static long getLong(Context context, String key, long defValue) {
        //存储节点文件的名称，读写方式
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
        return sharedPreferences.getLong(key, defValue);
    }

    /**
     * 从sharedPreferences中移除指定节点
     *
     * @param context 上下文环境
     * @param key     需要移除节点的名称
     */
    public static void remove(Context context, String key) {
        //存储节点文件的名称，读写方式
        if (sharedPreferences == null) {
            sharedPreferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        }
        sharedPreferences.edit().remove(key).commit();
    }

}
