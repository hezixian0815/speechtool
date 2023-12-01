package com.aispeech.common;

import android.app.ActivityManager;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.DecimalFormat;
import java.util.Enumeration;


public class DeviceUtil {
    private static final String TAG = "DeviceUtil";

    private DeviceUtil() {
    }

    /*返回为字符串数组[0]为大小[1]为单位KB或者MB*/
    private static String[] fileSize(long size) {
        String str = "";
        if (size >= 1024) {
            str = "KB";
            size /= 1024;
            if (size >= 1024) {
                str = "MB";
                size /= 1024;
            }
        }
        /*将每3个数字用,分隔如:1,000*/
        DecimalFormat formatter = new DecimalFormat();
        formatter.setGroupingSize(3);
        String result[] = new String[2];
        result[0] = formatter.format(size);
        result[1] = str;
        return result;
    }

    public static String getRAMInfo(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        String[] available = fileSize(mi.availMem);
        String[] total = fileSize(getTotalRamSize());
        return ("RAM " + available[0] + available[1] + "/" + total[0] + total[1]);
    }

    /**
     * 获取SD卡路径
     *
     * @return SD卡存在返回正常路径；SD卡不存在返回""
     */
    public static String getSDCradPath(Context context) {
        File sdDir = null;
        if (isSDCardEnable()) {
            if (Build.VERSION.SDK_INT >= 29) {
//Android10之后
                sdDir = context.getExternalFilesDir(null);
            } else {
                sdDir = Environment.getExternalStorageDirectory();// 获取SD卡根目录
            }
        } else {
            sdDir = Environment.getRootDirectory();// 获取跟目录
        }
        return sdDir.toString();
    }

    /**
     * 判断SD卡是否可用
     *
     * @return ture：可用；false：不可用
     */
    public static boolean isSDCardEnable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    private static long getTotalRamSize() {
        String str1 = "/proc/meminfo";// 系统内存信息文件
        String str2;
        String[] arrayOfString;
        long totalMem = 0;
        try (FileReader localFileReader = new FileReader(str1);
             BufferedReader localBufferedReader = new BufferedReader(
                     localFileReader, 8192)) {
            str2 = localBufferedReader.readLine();// 读取meminfo第一行，系统总内存大小
            Log.i(TAG, "" + str2);
            arrayOfString = str2.split("\\s+");
            totalMem = Long.valueOf(arrayOfString[1]).intValue() * 1024;// 获得系统总内存，单位是KB，乘以1024转换为Byte
        } catch (IOException e) {
            e.printStackTrace();
        }
        return totalMem;
    }

    public static String getROMInfo() {
        File file = Environment.getDataDirectory();
        StatFs statFs = new StatFs(file.getPath());
        long blockSize = statFs.getBlockSize();
        long totalBlocks = statFs.getBlockCount();
        long availableBlocks = statFs.getAvailableBlocks();

        String[] total = fileSize(totalBlocks * blockSize);
        String[] available = fileSize(availableBlocks * blockSize);

        return ("ROM " + available[0] + available[1] + "/" + total[0] + total[1]);
    }

    /*显示SD卡的可用和总容量，SD卡就相当于电脑C盘以外的硬盘*/
    public static String getSDInfo() {
        if (Environment.getExternalStorageState().equals
                (Environment.MEDIA_MOUNTED)) {
            File file = Environment.getExternalStorageDirectory();
            StatFs statFs = new StatFs(file.getPath());
            long blockSize = statFs.getBlockSize();
            long totalBlocks = statFs.getBlockCount();
            long availableBlocks = statFs.getAvailableBlocks();

            String[] total = fileSize(totalBlocks * blockSize);
            String[] available = fileSize(availableBlocks * blockSize);

            return ("SD " + available[0] + available[1] + "/" + total[0] + total[1]);
        } else {
            return ("SD CARD 已删除");
        }
    }

    public static String getWlanMac(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) return null;
        String mac = null;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo != null && !TextUtils.isEmpty(wifiInfo.getMacAddress())) {
            mac = wifiInfo.getMacAddress().trim();
        }
        if (mac != null) {
            mac = mac.replace(":", "");
        }
        return mac;
    }

    public static String getEthMac() {
        String mac = "";
        try {
            Process cmd = Runtime.getRuntime().exec("cat /sys/class/net/eth0/address");
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(cmd.getInputStream()));
            mac = br.readLine();
            Log.d(TAG, "MAC : " + mac);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mac != null) {
            mac = mac.replace(":", "");
        }
        return mac;
    }

    private static final String DEFAULT_MAC_ADDRESS = "02:00:00:00:00:00";

    /**
     * Try to get mac address from SharedPreference firstly.
     * Try to get mac address, if wifi is disable, enable it until got mac address.
     * Try to get mac address in a loop per 600ms getInstance timeout 30s until got it.
     *
     * @param context Android context.
     * @return non-empty, trimed, mac address. If can not get, then return 02:00:00:00:00:00.
     * @Exception RuntimeException on calling from main thread.
     */
    public static String getMacAddress(Context context) {
        String mac = null;
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mac = tryGetMac(wifiManager);
        if (TextUtils.isEmpty(mac)) {
            mac = DEFAULT_MAC_ADDRESS;
        }
        Log.i(TAG, "mac address: " + mac);
        return mac;
    }

    private static String tryGetMac(WifiManager manager) {
        String mac;
        if (Build.VERSION.SDK_INT < 24) {
            mac = getMacAddressFromWifiInfo(manager);
            if (TextUtils.isEmpty(mac) || TextUtils.equals(mac, DEFAULT_MAC_ADDRESS)) {
                mac = getMacByCat();
            }
        } else {
            mac = getMacAddressFromNetworkInterfaces();
        }
        return mac;
    }

    @SuppressWarnings("all")
    private static boolean wifiEnabled(WifiManager manager, boolean enabled) {
        try {
            return manager.setWifiEnabled(enabled);
        } catch (SecurityException e) {
            Log.i(TAG, "no permission to change wifi state");
            return false;
        }
    }

    /**
     * @param manager WifiManager
     * @return null or trimed mac address.
     */
    private static String getMacAddressFromWifiInfo(WifiManager manager) {
        String mac = null;
        WifiInfo wifiInfo = manager.getConnectionInfo();
        if (wifiInfo != null && !TextUtils.isEmpty(wifiInfo.getMacAddress())) {
            mac = wifiInfo.getMacAddress().trim();
        }

        return mac;
    }

    private static String getMacByCat() {
        String str = "";
        String macSerial = "";
        try {
            Process pp = Runtime.getRuntime().exec(
                    "cat /sys/class/net/wlan0/address");
            InputStreamReader ir = new InputStreamReader(pp.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);

            for (; null != str; ) {
                str = input.readLine();
                if (str != null) {
                    macSerial = str.trim();
                    break;
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "getMacByCat: " + ex.toString());
        }
        if (TextUtils.isEmpty(macSerial)) {
            try {
                return loadFileAsString("/sys/class/net/eth0/address")
                        .toUpperCase().substring(0, 17);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return macSerial;
    }


    private static String loadFileAsString(String fileName) {
        String text = "";
        try (FileReader reader = new FileReader(fileName)) {
            text = loadReaderAsString(reader);
        } catch (Exception e) {
            Log.e(TAG, "loadFileAsString: " + e.toString());
        }
        return text;
    }

    private static String loadReaderAsString(Reader reader) {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        try {
            int readLength = reader.read(buffer);
            while (readLength >= 0) {
                builder.append(buffer, 0, readLength);
                readLength = reader.read(buffer);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadReaderAsString: " + e.toString());
        }

        return builder.toString();
    }


    private static String getMacAddressFromNetworkInterfaces() {
        Enumeration<NetworkInterface> interfaces = null;
        try {
            interfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (interfaces == null)
            return "";

        String hardWareAddress = null;
        NetworkInterface iF = null;
        while (interfaces.hasMoreElements()) {
            iF = interfaces.nextElement();
            try {
                if (iF.getName().equals("eth0") || iF.getName().equals("wlan0")) {
                    hardWareAddress = bytesToString(iF.getHardwareAddress());
                    break;
                }
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }
        return hardWareAddress;
    }


    /***
     * byte转为String
     * @param bytes
     * @return
     */
    private static String bytesToString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        for (byte b : bytes) {
            buf.append(String.format("%02X:", b));
        }
        if (buf.length() > 0) {
            buf.deleteCharAt(buf.length() - 1);
        }
        return buf.toString();
    }
}
