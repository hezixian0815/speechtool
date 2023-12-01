package com.aispeech.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

import dalvik.system.DexFile;

/**
 * 类描述：获取类信息
 * 修改备注：
 */
public class ClassUtil {
    /**
     * @param clazz       接口
     * @param packageName 实现类所在的包的包名
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static ArrayList<Class> getAllClassByInterface(Class clazz, String packagePath, String packageName) {
        ArrayList<Class> list = new ArrayList<>();
        /**
         * 判断是不是接口
         */
        try {
            ArrayList<Class> allClass = getAllClass(packagePath, packageName);
            /**
             * 循环判断路径下的所有类是否实现了指定的接口 并且排除接口类自己
             */
            for (int i = 0; i < allClass.size(); i++) {
                /**
                 * 判断是不是同一个接口
                 */
                if (clazz.isAssignableFrom(allClass.get(i))) {
                    if (!clazz.equals(allClass.get(i))) {
                        // 自身并不加进去
                        list.add(allClass.get(i));
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * 从一个指定路径下查找所有的类
     *
     * @param packageName
     * @param packagePath
     */
    private static ArrayList<Class> getAllClass(String packagePath, String packageName) {
        ArrayList<Class> list = new ArrayList<>();
        try {
            DexFile df = new DexFile(packagePath);//通过DexFile查找当前的APK中可执行文件
            Enumeration<String> enumeration = df.entries();
            while (enumeration.hasMoreElements()) {
                try {
                    String className = enumeration.nextElement();
                    if (className.contains(packageName)) {//在当前所有可执行的类里面查找包含有该包名的所有类
                        Class clazz = Class.forName(className);
                        list.add(clazz);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } catch (Error e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ExceptionInInitializerError e) {
            e.printStackTrace();
        } catch (NoClassDefFoundError e) {
            e.printStackTrace();
        }
        return list;
    }
}