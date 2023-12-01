/*******************************************************************************
 * Copyright 2013 aispeech
 ******************************************************************************/
package com.aispeech.common;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Util for AISpeech android sdk.
 */
public class Util {

    public static final String TAG = "DUILIte";

    public static final String UTF8 = "UTF-8";

    static final int BUFF_SIZE = 10240;
    public static byte[] MAGIC = {'P', 'K', 0x3, 0x4};
    public static String uniqueID = null;
    private static Random random = new Random();

    /**
     * 获取应用程序可以用的存储目录空间，以requireSizeInByte字节大小为最低存储大小要求限制
     * 优先选择内部存储空间，若内部存储空间不足则使用外部存储空间，若都不满足，则返回null
     *
     * @param ctx
     * @param relativePath
     * @param requireSizeInByte
     * @return File
     * <ul>
     * <li>内部： /data/data/{包名}/files/releativePath</li>
     * <li>外部： {外部存储}/Android/data/files/releativePath</li>
     * </ul>
     */
    public static File getAvaiableAppDataDirPerInternal(Context ctx, String relativePath,
                                                        long requireSizeInByte) {
        if (ctx == null) {
            return null;
        }
        File path = null;
        if (getAvailableInternalMemorySize() >= requireSizeInByte) {
            path = new File(ctx.getFilesDir(), relativePath);
        } else if (getAvailableExternalMemorySize() >= requireSizeInByte) {
            path = new File(getAvaiableExternalDataDir(ctx), relativePath);
        }
        return path;
    }

    /**
     * 获取应用程序可以用的存储目录空间，以10MB大小为最低存储大小要求限制
     * 优先选择内部存储空间，若内部存储空间不足则使用外部存储空间，若都不满足，则返回null
     *
     * @param ctx
     * @param relativePath
     * @return File
     * <ul>
     * <li>内部： /data/data/{包名}/files/releativePath</li>
     * <li>外部： {外部存储}/Android/data/files/releativePath</li>
     * </ul>
     */
    public static File getAvaiableAppDataDirPerInternal(Context ctx, String relativePath) {
        return getAvaiableAppDataDirPerInternal(ctx, relativePath, 1024 * 1024 * 10);
    }

    /**
     * 获取可用的外部存储数据目录
     *
     * @param ctx
     * @return
     */
    public static File getAvaiableExternalDataDir(Context ctx) {
        if (ctx == null) {
            return null;
        }
        File path = null;
        if (externalMemoryAvailable()) {
            path = ctx.getExternalFilesDir(null);
        }
        return path;
    }

    /**
     * 获取可用的内部存储空间大小(单位:byte)
     *
     * @return
     */
    public static long getAvailableInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        return availableBlocks * blockSize;
    }

    /**
     * 获取内部存储空间大小(单位:byte)
     *
     * @return
     */
    public static long getTotalInternalMemorySize() {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long totalBlocks = stat.getBlockCount();
        return totalBlocks * blockSize;
    }

    /**
     * 获取外部存储空间是否可用
     *
     * @return true 可用
     */
    public static boolean externalMemoryAvailable() {
        return Environment.getExternalStorageState().equals(
                Environment.MEDIA_MOUNTED);
    }

    /**
     * 获取外部可用的存储空间大小(单位:byte)
     *
     * @return 大小，-1表示外部存储空间不可用
     */
    public static long getAvailableExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long availableBlocks = stat.getAvailableBlocks();
            return availableBlocks * blockSize;
        } else {
            return -1;
        }
    }

    public static synchronized void deleteFile(Context context, String resName) {
        MD5Checker checker = new MD5Checker();
        checker.deleteFile(context, resName);
    }

    /**
     * 获取外部存储空间的大小(单位:byte)
     *
     * @return 大小，-1表示外部存储不可用
     */
    public static long getTotalExternalMemorySize() {
        if (externalMemoryAvailable()) {
            File path = Environment.getExternalStorageDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSize();
            long totalBlocks = stat.getBlockCount();
            return totalBlocks * blockSize;
        } else {
            return -1;
        }
    }

    /**
     * 获取资源目录
     *
     * @param context
     * @return
     */
    public static String getResourceDir(Context context) {
        return AssetsHelper.getResourceDir(context);
    }

    public static String getResPath(Context context, String resName) {
        if (TextUtils.isEmpty(resName)) {
            Log.e(TAG, "file res " + resName + " not found");
            return null;
        }
        return Util.getResourceDir(context) + File.separator + resName;
    }

    /**
     * 从assets目录中拷贝资源到资源目录，如果是zip文件则解压
     *
     * @param context Android环境句柄
     * @param resName 资源名
     * @return true 执行成功
     */
    public static int copyResource(final Context context, String resName, String resMd5sumName) {
        int ret = AssetsHelper.copyResource(context, resName, true, resMd5sumName);
        if (ret == 1) AssetsHelper.updateMapFile(context);
        return ret;
    }

    /**
     * 从assets目录中拷贝资源到资源目录，如果是zip文件则解压
     *
     * @param context Android环境句柄
     * @param resName 资源名
     * @return true 执行成功
     */
    public static int copyResource(final Context context, String resName) {
        int ret = AssetsHelper.copyResource(context, resName, true, null);
        if (ret == 1) AssetsHelper.updateMapFile(context);
        return ret;
    }

    private static File getFile(Context context, String fileName) {
        File file = new File(context.getFilesDir() + File.separator + fileName);
        File parentFile = file.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        return file;
    }

    /**
     * 从assets目录中拷贝资源文件到/data/data/$pkgname/files目录下，如果是zip文件则解压
     *
     * @param context Android环境句柄
     * @param resName 资源名
     * @param isMD5   是否进行MD5校验,如果校验和相同则忽略拷贝和解压
     * @return -1 拷贝失败; 0 MD5相同,略过拷贝; 1 拷贝成功
     */
    public static synchronized int copyResource(Context context, String resName, boolean isMD5, String resMd5sumName) {
        if (context == null) {
            return -1;
        }
        InputStream is;
        try {
            is = context.getAssets().open(resName);
        } catch (IOException e) {
            Log.e(Log.ERROR_TAG, "file " + resName
                    + " not found in assest folder, Did you forget add it?");
            return -1;
        }
        try {
            is.read(new byte[1]);
            is.reset();
        } catch (IOException e) {
            Log.e(Log.ERROR_TAG,
                    "file"
                            + resName
                            + "should be one of the suffix below to avoid be compressed in assets floder."
                            + "“.jpg”, “.jpeg”, “.png”, “.gif”, “.wav”, “.mp2″, “.mp3″, “.ogg”, “.aac”, “.mpg”, “.mpeg”, “.mid”, “.midi”, “.smf”, “.jet”, “.rtttl”, “.imy”, “.xmf”, “.mp4″, “.m4a”, “.m4v”, “.3gp”, “.3gpp”, “.3g2″, “.3gpp2″, “.amr”, “.awb”, “.wma”, “.wmv”");
            return -1;
        }

        File destFile = new File(Util.getResourceDir(context), resName);
        if (resMd5sumName == null) {//如果没有对应资源的md5文件
            Log.i("speech", "there is no md5 file of : " + resName);
            // if file exists, verify MD5 code
            if (isMD5 && checkMD5(is, destFile)) {
                Log.i("speech", "md5 is same : " + resName);
                try {
                    is.close();
                    return 0; // MD5 same do nothing
                } catch (IOException e) {
                    e.printStackTrace();
                    return -1;
                }
            } else {
                saveDestFile(context, is, resName);
                if (isZipFile(getFile(context, resName))) {
                    // unzip
                    unZip(context, destFile);
                }
                return 1;
            }
        } else { //如果有对应资源的md5文件
            Log.i("speech", "there is md5 file of : " + resName);
            InputStream isMd5sumIs;
            try {
                isMd5sumIs = context.getAssets().open(resMd5sumName);
            } catch (IOException e) {
                Log.e(Log.ERROR_TAG, "file " + resMd5sumName
                        + " not found in assest floder, Did you forget add it?");
                e.printStackTrace();
                return -1;
            }
            File dstMd5sumFile = new File(Util.getResourceDir(context), resMd5sumName);
            if (isMD5 && checkMD5(isMd5sumIs, dstMd5sumFile)) {
                Log.i("speech", " md5 file in assets and data drectory is same : " + resName);
                try {
                    isMd5sumIs.close();
                    is.close();
                    return 0; // MD5 same do nothing
                } catch (IOException e) {
                    e.printStackTrace();
                    return -1;
                }
            } else {
                Log.i("speech", " md5 file in assets and data drectory is not same : " + resName);
                saveDestFile(context, is, resName);
                saveDestFile(context, isMd5sumIs, resMd5sumName);
                if (isZipFile(getFile(context, resName))) {
                    // unzip
                    unZip(context, destFile);
                }
                return 1;
            }
        }
    }

    /**
     * 拷贝assets目录下文件夹到指定目录
     *
     * @param context    上下文
     * @param assetsPath assets中文件夹或文件名
     * @param savePath   目标路径
     * @return 0：成功，-1：失败
     */
    public static int copyFilesFromAssets(Context context, String assetsPath, String savePath) {
        if (context == null) {
            return -1;
        }
        try {
            String fileNames[] = context.getAssets().list(assetsPath);// 获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {// 如果是目录
                File file = new File(savePath);
                file.mkdirs();// 如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyFilesFromAssets(context, assetsPath + "/" + fileName,
                            savePath + "/" + fileName);
                }
            } else {// 如果是文件
                try (InputStream is = context.getAssets().open(assetsPath);
                     FileOutputStream fos = new FileOutputStream(new File(savePath))) {
                    byte[] buffer = new byte[1024];
                    int byteCount = 0;
                    while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
                        // buffer字节
                        fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
                    }
                    fos.flush();// 刷新缓冲区
                }
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * 拷贝assets目录下文件夹到指定目录 MD5校验
     *
     * @param context    上下文
     * @param assetsPath assets中文件夹或文件名
     * @param savePath   目标路径
     * @return 0：成功，-1：失败
     */
    //fix https://jira.aispeech.com.cn/browse/YJGGZC-12899
    public static synchronized int copyFilesFromAssetsMd5(Context context, String assetsPath, String savePath) {
        if (context == null) {
            return -1;
        }
        try {
            String fileNames[] = context.getAssets().list(assetsPath);// 获取assets目录下的所有文件及目录名
            if (fileNames.length > 0) {// 如果是目录
                File file = new File(savePath);
                file.mkdirs();// 如果文件夹不存在，则递归
                for (String fileName : fileNames) {
                    copyFilesFromAssetsMd5(context, assetsPath + "/" + fileName,
                            savePath + "/" + fileName);
                }
            } else {// 如果是文件
                try (InputStream is = context.getAssets().open(assetsPath)) {
                    int status = saveFile(context, is, new File(savePath));
                    if (status == -1) {
                        return status;
                    }
                }
            }
            return 0;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static int saveFile(Context context, InputStream is, File savePath) {
        if (checkMD5(is, savePath)) {
            try {
                is.close();
                return 0; // MD5 same do nothing
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        } else {
            try {
                FileOutputStream fos = new FileOutputStream(savePath);
                byte[] buffer = new byte[1024];
                int byteCount = 0;
                while ((byteCount = is.read(buffer)) != -1) {// 循环从输入流读取
                    // buffer字节
                    fos.write(buffer, 0, byteCount);// 将读取的输入流写入到输出流
                }
                fos.flush();// 刷新缓冲区
                return 0;
            } catch (Exception e) {
                return -1;
            }
        }
    }

    public static void saveDestFile(Context context, InputStream is, String resName) {
        Log.i("speech", "save to data : " + resName);
        try (FileOutputStream fos = new FileOutputStream(getFile(context, resName))) {
            is.reset();
            byte[] data = new byte[BUFF_SIZE];
            int len = 0;
            while ((len = is.read(data)) != -1) {
                fos.write(data, 0, len);
            }
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 检测是否是zip文件
     *
     * @param f
     * @return
     */
    public static boolean isZipFile(File f) {
        boolean isZip = true;
        byte[] buffer = new byte[MAGIC.length];
        try (RandomAccessFile raf = new RandomAccessFile(f, "r")) {
            raf.readFully(buffer);
            for (int i = 0; i < MAGIC.length; i++) {
                if (buffer[i] != MAGIC[i]) {
                    isZip = false;
                    break;
                }
            }
        } catch (Throwable e) {
            isZip = false;
        }
        return isZip;
    }

    /**
     * 比较输入流和文件的MD5码是否相同，内部未关闭输入流
     *
     * @param is
     * @param file
     * @return true MD5校验通过， false 文件不存在或MD5校验未通过
     */
    private static boolean checkMD5(final InputStream is, File file) {
        if (file.exists()) {
            try {
                FileInputStream destFis = new FileInputStream(file);
                byte[] md51 = getFileMD5String(destFis);
                byte[] md52 = getFileMD5String(is);
                boolean same = true;
                int minLength = (md51.length > md52.length) ? md52.length : md51.length;
                for (int k = 0; k < minLength; k++) {
                    if (md51[k] != md52[k]) {
                        same = false;
                        break;
                    }
                }
                destFis.close();
                return same;
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * unzip zipfile under destDir, support subdir
     *
     * @param zipfileName the zip file
     */
    public static void unZip(final Context context, File zipfileName) {
        byte data[] = new byte[BUFF_SIZE];
        try (ZipFile zipFile = new ZipFile((zipfileName))) {
            Enumeration<? extends ZipEntry> emu = zipFile.entries();
            while (emu.hasMoreElements()) {
                ZipEntry entry = emu.nextElement();
                if (entry.getName().contains("../")) {
                    throw new Exception("unsecurity zipfile!!!");
                }
                if (entry.isDirectory()) {
                    new File(Util.getResourceDir(context), entry.getName()).mkdirs();
                    continue;
                }
                try (BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));
                     OutputStream outputStream = new FileOutputStream(new File(getResourceDir(context),
                             entry.getName()));
                     BufferedOutputStream bos = new BufferedOutputStream(outputStream, BUFF_SIZE)) {
                    Log.d("unzip", entry.getName());
                    int readSize;
                    while ((readSize = bis.read(data, 0, BUFF_SIZE)) != -1) {
                        bos.write(data, 0, readSize);
                    }
                    bos.flush();
                    CloseUtils.closeIO(bos, outputStream, bis);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param decript 要加密的字符串
     * @return 加密的字符串
     * SHA1加密
     */
    public final static String SHA1(String decript) {
        try {
            MessageDigest digest = MessageDigest
                    .getInstance("SHA-1");
            digest.update(decript.getBytes());
            byte messageDigest[] = digest.digest();
            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            // 字节数组转换为 十六进制 数
            for (int i = 0; i < messageDigest.length; i++) {
                String shaHex = Integer.toHexString(messageDigest[i] & 0xFF);
                if (shaHex.length() < 2) {
                    hexString.append(0);
                }
                hexString.append(shaHex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * get File MD5 codes string
     *
     * @param in inputstream
     * @return
     */
    private static byte[] getFileMD5String(InputStream in) {
        try {
            MessageDigest messagedigest = MessageDigest.getInstance("MD5");

            byte[] buffer = new byte[BUFF_SIZE];
            int length = -1;
            while ((length = in.read(buffer)) != -1) {
                messagedigest.update(buffer, 0, length);
            }
            return messagedigest.digest();

        } catch (NoSuchAlgorithmException nsaex) {
            nsaex.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }

    public static String md5File(File file) {
        return md5File(file, 16);
    }

    /**
     * 获取单个文件的MD5值
     *
     * @param file  文件
     * @param radix 位 16 32 64
     * @return
     */
    public static String md5File(File file, int radix) {
        if (file == null || !file.exists() || !file.isFile()) {
            return null;
        }
        MessageDigest digest = null;
        byte buffer[] = new byte[1024];
        int len;
        try (FileInputStream in = new FileInputStream(file)) {
            digest = MessageDigest.getInstance("MD5");
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        BigInteger bigInt = new BigInteger(1, digest.digest());
        return bigInt.toString(radix);
    }

    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * 生成32位的uuid字符串
     *
     * @return uuid字符串
     */
    public static String uuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 字节数组转换为utf8字符串
     *
     * @param bytes 字节数组
     * @return utf8字符串
     */
    public static String newUTF8String(byte[] bytes) {
        try {
            return new String(bytes, UTF8);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return new String(bytes);
        }
    }

    /**
     * 获取UTF-8编码的字符数组
     *
     * @param str UTF8字符串
     * @return 出错时返回null
     */
    public static byte[] getUTF8Bytes(String str) {
        try {
            return str.getBytes(UTF8);
        } catch (Exception ex) {
            return new byte[0];
        }
    }

    /**
     * 获取设备唯一32位标志符
     *
     * @param context
     *            Android环境句柄
     * @return
     */
//    public synchronized static String getDid(final Context context) {
//        if (context == null) {
//            return null;
//        }
//        SharedPreferences sharedPrefs = context.getSharedPreferences(AISpeechSDK.PREF_AISPEECH,
//                Context.MODE_PRIVATE);
//        uniqueID = sharedPrefs.getString(AISpeechSDK.PREFKEY_UNIQUE_ID, null);
//        if (TextUtils.isEmpty(uniqueID)) {
//            uniqueID = generateDeviceId16(context);
//            Editor editor = sharedPrefs.edit();
//            editor.putString(AISpeechSDK.PREFKEY_UNIQUE_ID, uniqueID);
//            editor.commit();
//        }
//        return uniqueID;
//    }

    /**
     * 通过获取tm.DeviceID地址及android_id等来生成全球唯一的32位设备id号(不含'-')
     *
     * @param context Android环境句柄
     */
    /*public static String generateDeviceId32(final Context context) {
        if (context == null) {
            return null;
        }
        final String androidId, tmDeviceId, tmSerial;
        try {
            TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            tmDeviceId = (tm.getDeviceId() != null) ? tm.getDeviceId() : "hello world aispeech";
            tmSerial = "" + tm.getSimSerialNumber();
            String szDevIDShort = "35" + (Build.BOARD.length() % 10) + (Build.BRAND.length() % 10)
                    + (Build.CPU_ABI.length() % 10) + (Build.DEVICE.length() % 10)
                    + (Build.MANUFACTURER.length() % 10) + (Build.MODEL.length() % 10)
                    + (Build.PRODUCT.length() % 10);

            androidId = ""
                    + android.provider.Settings.Secure.getString(context.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);

            UUID deviceUuid = new UUID((long) androidId.hashCode() << 32 | tmDeviceId.hashCode(),
                    (long) szDevIDShort.hashCode() << 32 | tmSerial.hashCode());
            Log.i("version", deviceUuid.version() + "");
            Log.i("android_id", androidId.hashCode() + "");
            Log.i("tmDevice", tmDeviceId.hashCode() + "");
            Log.i("m_szDevIDShort", szDevIDShort.hashCode() + "");
            Log.i("UUID", deviceUuid.toString());

            return deviceUuid.toString().replaceAll("-", "");
        } catch (Exception e) {
            Log.e("",
                    "Did you forget add android.permission.READ_PHONE_STATE permission in your application? Add it now to fix this bug!");
            return null;
        }
    }*/

    /**
     * 生成16位deviceId
     *
     * @param context
     * @return
     */
    /*public static String generateDeviceId16(final Context context) {
        String did;
        String androidId, serial = null, imei = null;
        androidId = android.provider.Settings.Secure.getString(context.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        if (Build.VERSION.SDK_INT > 9) {
            serial = Build.SERIAL;
        }
        try {
            TelephonyManager tm = (TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            imei = tm.getSimSerialNumber();
        } catch (Exception e) {
            Log.e("",
                    "Did you forget add android.permission.READ_PHONE_STATE permission in your application? Add it now to fix this bug!");
        }

        if (!TextUtils.isEmpty(androidId) && !TextUtils.equals(androidId, "9774d56d682e549c")) {
            did = androidId;
        } else if (!TextUtils.isEmpty(imei)) {
            did = imei;
        } else if (!TextUtils.isEmpty(serial)) {
            did = serial;
        } else {
            did = "";
        }

        if (did.length() < 8) {
            did = "";
        }

        return did.toLowerCase();
    }*/

    /**
     * 设置设备唯一ID
     *
     * @param context
     * @param deviceId
     */
    public static void setDid(final Context context, String deviceId) {
//        if (TextUtils.isEmpty(deviceId)) {
//            return;
//        }
//        byte[] didByte = deviceId.getBytes();
//        AIEngine.aiengine_opt(0, AISpeechSDK.AIENGINE_OPT_SET_DEVICEID, didByte, didByte.length);
//        SharedPreferences sharedPrefs = context.getSharedPreferences(AISpeechSDK.PREF_AISPEECH,
//                Context.MODE_PRIVATE);
//        Editor editor = sharedPrefs.edit();
//        editor.putString(AISpeechSDK.PREFKEY_UNIQUE_ID, deviceId);
//        editor.commit();
    }

    /**
     * 获取显示屏分辨率信息
     *
     * @return
     */
    public static String getDisplayInfo(final Context context) {
        if (context == null) {
            return null;
        }
        int height = context.getResources().getDisplayMetrics().heightPixels;
        int width = context.getResources().getDisplayMetrics().widthPixels;
        return width + "x" + height;
    }

    /**
     * 显示当前执行的线程信息
     */
    public static final void logThread(String tag) {
        Thread t = Thread.currentThread();
        Log.d(tag, "<" + t.getName() + ">id: " + t.getId() + ", Priority: " + t.getPriority()
                + ", Group: " + t.getThreadGroup().getName());
    }

    /**
     * 获取当前网络连接类型
     *
     * @param ctx
     * @return
     */
    public static String getNetWorkType(Context ctx) {
        if (ctx == null) {
            return null;
        }
        String type = "unknown_type"; // maybe usb network access is
        // this
        try {
            ConnectivityManager cm = (ConnectivityManager) ctx
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            TelephonyManager tm = (TelephonyManager) ctx
                    .getSystemService(Context.TELEPHONY_SERVICE);

            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

            if (activeNetwork == null) {
                return null;
            }

            switch (activeNetwork.getType()) {
                case (ConnectivityManager.TYPE_WIFI):
                    type = "WIFI";
                    break;
                case (ConnectivityManager.TYPE_MOBILE): {
                    switch (tm.getNetworkType()) {
                        case TelephonyManager.NETWORK_TYPE_CDMA:
                            type = "CDMA";
                            break;
                        case TelephonyManager.NETWORK_TYPE_EDGE:
                            type = "EDGE";
                            break;
                        case TelephonyManager.NETWORK_TYPE_GPRS:
                            type = "GPRS";
                            break;
                        case TelephonyManager.NETWORK_TYPE_HSDPA:
                            type = "HSDPA";
                            break;
                        case TelephonyManager.NETWORK_TYPE_HSPA:
                            type = "HSPA";
                            break;
                        case TelephonyManager.NETWORK_TYPE_HSUPA:
                            type = "HSUPA";
                            break;
                        case TelephonyManager.NETWORK_TYPE_UMTS:
                            type = "UMTS";
                            break;
                        case TelephonyManager.NETWORK_TYPE_EVDO_0:
                            type = "EVDO_0";
                            break;
                        case TelephonyManager.NETWORK_TYPE_EVDO_A:
                            type = "EVDO_A";
                            break;
                        // case TelephonyManager.NETWORK_TYPE_EVDO_B:
                        // type = "EVDO_B";
                        // break;
                        case TelephonyManager.NETWORK_TYPE_IDEN:
                            type = "IDEN";
                            break;
                        case TelephonyManager.NETWORK_TYPE_1xRTT:
                            type = "1xRTT";
                            break;
                        default:
                            break;
                    }
                    break;
                }
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(Log.ERROR_TAG,
                    "Did you forget add android.permission.ACCESS_NETWORK_STATE permission in your application? Add it to fix this bug!");
            return null;
        }
        return type;
    }

    /**
     * 获取现有网络所对应的speex编码等级
     * <ul>
     * <li>3:2G或2.5G网络</li>
     * <li>8:WIFI、3G及更快的网络</li>
     * </ul>
     *
     * @param ctx
     * @return speex编码等级
     */
    public static int getNetworkQuality(final Context ctx) {
        int quailty = 8;
        String type = getNetWorkType(ctx);
        if (type == null || type.equals("EDGE") || type.equals("GPRS") || type.equals("1xRTT")
                || type.equals("IDEN")) {
            quailty = 3;
        }
        return quailty;
    }

    /**
     * 判断当前线程是不是单元测试线程
     *
     * @return
     */
    public static boolean isUnitTesting() {
        return Thread.currentThread().getName().contains("android.test.InstrumentationTestRunner");
    }

    /**
     * 将一个Runnable的任务在主线程或测试线程中执行
     *
     * @param context android上下文环境
     * @param r       Runnable任务
     */
    public static void executeRunnableInMainOrTestThread(Context context, Runnable r) {
        if (context != null) {
            HandlerThread thread = null;
            boolean isUT = isUnitTesting();
            if (isUT) {
                thread = new HandlerThread(ThreadNameUtil.getSimpleThreadName("TestHandlerThread"));
                thread.start();
            }
            Handler handler = new Handler(isUT ? thread.getLooper() : context.getMainLooper());
            handler.post(r);
        }
    }

    /**
     * 获取时间串
     *
     * @return
     */
    public static String getCurrentTimeStamp() {
        SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-sss", Locale.CHINA);
        Date now = new Date();
        return sdfDate.format(now);
    }

    /**
     * 返回当前Wifi是否连接上
     *
     * @param context
     * @return true 已连接
     */
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager conMan = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMan.getActiveNetworkInfo();
        if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
            return true;
        }
        return false;
    }

    /**
     * 生成随机数字串
     *
     * @param length 数字串长度
     * @return
     */
    public static long generateRandom(int length) {
        char[] digits = new char[length];
        digits[0] = (char) (random.nextInt(9) + '1');
        for (int i = 1; i < length; i++) {
            digits[i] = (char) (random.nextInt(10) + '0');
        }
        return Long.parseLong(new String(digits));
    }

    public static byte[] toByteArray(short[] src) {
        byte[] dest = new byte[src.length * 2];
        for (int i = 0; i < src.length; i++) {
            dest[i * 2] = (byte) (src[i] & 0xFF);
            dest[i * 2 + 1] = (byte) ((src[i] >> 8) & 0xff);
        }
        return dest;
    }

    public static short[] toShortArray(byte[] src) {
        int count = src.length / 2;
        short[] dest = new short[count];
        for (int i = 0; i < count; i++) {
            dest[i] = (short) ((src[i * 2] & 0xff) | ((src[2 * i + 1] & 0xff) << 8));
        }
        return dest;
    }

    public static byte[] getRecChannelData(byte[] src) {
        int i;
        short[] shorArray = toShortArray(src);
        short temp = 0;
        for (i = 0; i < src.length / 2; i = i + 2) {
            temp = shorArray[i];
            shorArray[i] = shorArray[i + 1];
            shorArray[i + 1] = temp;
        }
        return toByteArray(shorArray);
    }

    public static double calcVolume(short[] buffer) {
        double sum = 0.0;
        // 将 buffer 内容取出，进行平方和运算
        for (int i = 0; i < buffer.length; i++) {
            double sample = buffer[i] / 32768.0;
            sum += sample * sample;
        }
        double rms = 0;
        if (buffer.length > 0) {
            rms = Math.sqrt(sum / buffer.length);
        }
        double volume;
        if (rms - 0.0 <= 0.001) {
            volume = 0.0;
        } else {
            volume = 20 * Math.log10(rms) + 100;
        }
        return volume;
    }

    public static String getEthMacAddress() {
        String address = "";
        NetworkInterface ni = null;
        try {
            ni = NetworkInterface.getByName("eth0");
            if (ni != null) {
                byte[] mac = ni.getHardwareAddress();
                //下面代码是把mac地址拼装成String
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < mac.length; i++) {
                    //mac[i] & 0xFF 是为了把byte转化为正整数
                    String s = Integer.toHexString(mac[i] & 0xFF);
                    sb.append(s.length() == 1 ? 0 + s : s);
                }
                address = sb.toString();
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return address;
    }

    public static String getWIFIMacAddress(Context context) {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifi.getConnectionInfo();
        return info.getMacAddress().replace(":", "");
    }

    public static byte[] hexToByte(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() == 0) {
            return new byte[0];
        }
        byte[] byteArray = new byte[content.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            String subStr = content.substring(2 * i, 2 * i + 2);
            byteArray[i] = ((byte) Integer.parseInt(subStr, 16));
        }
        return byteArray;
    }

    public static String byteToHex(byte[] data) {
        if (data == null) {
            return null;
        }
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[data.length * 2];
        for (int j = 0; j < data.length; j++) {
            int v = data[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    /**
     * 去掉中文之间的空格,不去掉英文之间的空格
     *
     * @return 不带中文之间的空格
     */
    public static String removeSpaceBetweenChinese(String origin) {
        if (TextUtils.isEmpty(origin)) return "";
        origin = origin.replaceAll("([A-Za-z-]) +([A-Za-z-])", "$1@$2");
        origin = origin.replaceAll("\\s+", "").replaceAll("@", " ");
        return origin;
    }

    // 把byte数据转为每份为size的list
    public static List<byte[]> handAudio(byte[] data, int size) {
        List<byte[]> byteList = new ArrayList<>();
        for (int start = 0; start < data.length; start += size) {
            int end = start + size;
            end = end < data.length ? end : data.length;
            byte[] buf = Arrays.copyOfRange(data, start, end);
            byteList.add(buf);
        }
        return byteList;
    }

    /**
     * debug系统默认开启上传系统，正式版本不开启上传日志信息
     *
     * @return false为不开启，true为开启
     */
    public static boolean isDebugSystem() {
        if (TextUtils.isEmpty(Build.TYPE)) {
            return false;
        }
        return !Build.TYPE.equals("user");
    }

}
