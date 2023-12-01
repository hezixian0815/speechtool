package com.aispeech.common;


import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;

import com.aispeech.BuildConfig;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class AuthUtil {
    public static final String TAG = "AuthUtil";
    private static final String UNKNOW = "unknown";
    private static final String TRAIL = "trail";
    private static final String SCOPE = "scope";
    private static final String USEDTIMES = "usedTimes";

    private AuthUtil() {
    }

    /**
     * 拼接url
     * e.g. https://auth.duiopen.com/auth/device/register?productId=11&apiKey=23
     *
     * @param rawUrl
     * @param map
     * @return
     */
    public static String appendUrl(String rawUrl, Map<String, Object> map) {
        String retUrl = rawUrl;
        if (map != null && map.size() > 0) {
            if (!retUrl.contains("?")) {
                retUrl += "?";
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                sb.append(key).append("=").append(value).append("&");
            }
            String endUrl = sb.toString();
            if (endUrl.endsWith("&")) {
                endUrl = endUrl.substring(0, endUrl.length() - 1);
            }
            retUrl += endUrl;
        }
        Log.d(TAG, "appendUrl is " + retUrl);
        return retUrl;
    }

    public static String getSecretCode(Context context) {
        return getAuthPackageName(context) + ":" + AuthUtil.getKeyHash(context);
    }

    /**
     * 获取授权包名
     *
     * @return 授权包名
     */
    public static String getAuthPackageName(Context context) {
        String authPackageName = AISpeechBridge.authPackageName;
        return TextUtils.isEmpty(authPackageName) ? context.getPackageName() : authPackageName;
    }

    /**
     * 获取授权包签名
     *
     * @param context 上下文
     * @return 授权包签名
     */
    public static String getAuthKeyHash(Context context) {
        String authSHA256 = AISpeechBridge.authSHA256;
        return TextUtils.isEmpty(authSHA256) ? getKeyHash(context) : authSHA256;
    }

    public static String getSecretCode(Context context, String sharePkgName, String sharePkgSH256) {
        if (!TextUtils.isEmpty(sharePkgName) && !TextUtils.isEmpty(sharePkgSH256)) {
            return sharePkgName + ":" + sharePkgSH256;
        }
        return context.getPackageName() + ":" + AuthUtil.getKeyHash(context);
    }

    public static String getKeyHash(Context ctx) {
        // Add code to print out the key hash
        String hash = "";
        try {
            PackageInfo info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(signature.toByteArray());

                hash = byte2HexFormatted(md.digest());
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "getKeyHash: " + e.toString());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "getKeyHash: " + e.toString());
        }

        return hash;
    }

    public static String byte2HexFormatted(byte[] arr) {
        return byte2HexFormatted(arr, true);
    }

    public static String byte2HexFormatted(byte[] arr, boolean format) {
        StringBuilder str = new StringBuilder(arr.length * 2);
        for (int i = 0; i < arr.length; i++) {
            String h = Integer.toHexString(arr[i]);
            int l = h.length();
            if (l == 1)
                h = "0" + h;
            if (l > 2)
                h = h.substring(l - 2, l);
            str.append(h.toUpperCase());
            if (format && i < (arr.length - 1)) {
                str.append(':');
            }
        }
        return str.toString();
    }

    public static String getApplicationName(Context context) {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = context.getApplicationContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        String applicationName =
                (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }

    public static String getDisplayMatrix(Context context) {
        int[] ints = getScreenResolution(context);
        return ints[0] + "*" + ints[1];
    }

    public static int[] getScreenResolution(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int[] resolution = new int[]{dm.widthPixels, dm.heightPixels};
        return resolution;
    }

    public static String getImei(Context context) {
        String imei = null;
        TelephonyManager tm = (TelephonyManager) context.getSystemService("phone");
        try {
            if (tm == null || TextUtils.isEmpty(tm.getDeviceId())) {
                tm = (TelephonyManager) context.getSystemService("phone1");
            }
            if (tm != null) {
                imei = tm.getDeviceId();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        return imei != null ? imei.trim() : "";
    }

    public static String getDeviceId(Context context, String customDeviceId) {
        String deviceId = getOriginalDeviceId(context, customDeviceId);
        try {
            if (AISpeechBridge.encryptCustomDeviceName) {
                deviceId = SHAUtils.sha1(deviceId);
                Log.v(TAG, "SHAUtils.sha1 deviceId = " + deviceId);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return deviceId;
    }

    public static String getOriginalDeviceId(Context context, String customDeviceId) {
        if (!TextUtils.isEmpty(customDeviceId)) {
            Log.d(TAG, "customDeviceId is " + customDeviceId);
            return customDeviceId;
        }
        String imei = getImei(context.getApplicationContext());
        String serial = Build.SERIAL;
        String uuid;
        if ((TextUtils.isEmpty(imei) || TextUtils.equals(UNKNOW, imei))
                && (TextUtils.isEmpty(serial) || TextUtils.equals(UNKNOW, serial))) {
            uuid = Installation.id(context);
        } else {
            if (TextUtils.isEmpty(imei)) {
                imei = UNKNOW;
            } else if (TextUtils.isEmpty(serial)) {
                serial = UNKNOW;
            }
            uuid = UUID.nameUUIDFromBytes((imei + serial).getBytes()).toString();
        }

        return uuid;
    }

    public static String getVersionName(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException var2) {
            var2.printStackTrace();
            return null;
        }
    }

    public static String getDeviceData(Context context, Map<String, Object> map, String customDeviceId) {
        JSONObject object = new JSONObject();
        try {
            object.put("applicationLabel", getApplicationName(context));
            object.put("applicationVersion", getVersionName(context));
            object.put("buildDevice", Build.DEVICE);
            object.put("buildManufacture", Build.MANUFACTURER);
            object.put("buildModel", Build.MODEL);
            object.put("buildSdkInt", String.valueOf(Build.VERSION.SDK_INT));
            object.put("displayMatrix", getDisplayMatrix(context));
            object.put("packageName", getAuthPackageName(context));
            //Android9以上权限收紧，第一次解锁之前无法进行设备读写，故而需要客户设置deviceID做第一次的设备校验
            String deviceId = getDeviceId(context, customDeviceId);
            if (TextUtils.isEmpty(deviceId)) {
                if (!map.containsKey("deviceId")) {
                    Log.e(TAG, "getDeviceData: current deviceId is null,please set it");
                }
            } else {
                object.put("deviceId", deviceId);
            }
            object.put("platform", "android");
            object.put("buildVariant", getBuildVariant(context));
            object.put("imei", getImei(context));
            object.put("mac", DeviceUtil.getMacAddress(context));
            object.put("androidId", getAndroidId(context));
            if (map != null && map.size() > 0) {
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    Log.d(TAG, "custom key and value " + entry.getKey() + "\t" + entry.getValue());
                    if (TextUtils.equals(entry.getKey(), "deviceId") && AISpeechBridge.encryptCustomDeviceName) {
                        object.put(entry.getKey(), SHAUtils.sha1(entry.getValue().toString()));
                    } else {
                        object.put(entry.getKey(), entry.getValue());
                    }
                }
            }
            if (AISpeechBridge.encryptCustomDeviceName) {
                object.put("originalDeviceName", getOriginalDeviceId(context, customDeviceId));
            }

        } catch (JSONException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return object.toString();
    }

    public static JSONObject getDeviceInfo() {
        JSONObject deviceJson = new JSONObject();
        try {
            deviceJson.put("buildModel", Build.MODEL);
            deviceJson.put("buildManufacture", Build.MANUFACTURER);
            deviceJson.put("buildDevice", Build.DEVICE);
            deviceJson.put("buildSdkInt", String.valueOf(Build.VERSION.SDK_INT));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return deviceJson;
    }


    public static void logAuthFailureInfo(String errId, String error, Context context) {
        StringBuilder builder = new StringBuilder();
        builder.append("\n==================================================");
        builder.append("\n====================授权失败=======================");
        builder.append("\n============apk 版本 -> ");
        builder.append(AuthUtil.getBuildVariant(context));
        builder.append("\n============apk SHA256-> ");
        builder.append(AuthUtil.getAuthKeyHash(context));
        builder.append("\n============apk packageName -> ");
        builder.append(AuthUtil.getAuthPackageName(context));
        builder.append("\n============errorId -> ");
        builder.append(errId);
        builder.append("\n============errorInfo -> ");
        builder.append(error);
        builder.append("\n==================================================");
        Log.e("DUI-Auth", builder.toString());
    }

    private static String getAndroidId(Context context) {
        String androidId = Settings.Secure.getString(
                context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId;
    }


    public static String hexByte(byte b) {
        String s = "000000" + Integer.toHexString(b);
        return s.substring(s.length() - 2);
    }

    public static String getMacAddress() {
        Enumeration<NetworkInterface> el;
        String mac_s = "";
        try {
            el = NetworkInterface.getNetworkInterfaces();
            while (el.hasMoreElements()) {
                byte[] mac = el.nextElement().getHardwareAddress();
                if (mac == null)
                    continue;
                mac_s = hexByte(mac[0]) + "-" + hexByte(mac[1]) + "-"
                        + hexByte(mac[2]) + "-" + hexByte(mac[3]) + "-"
                        + hexByte(mac[4]) + "-" + hexByte(mac[5]);
            }
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
        return mac_s;
    }

    public static String readMetaData(Context context, String key) {
        String value = "";
        try {
            PackageManager manager = context.getPackageManager();
            String packageName = context.getPackageName();
            ApplicationInfo appInfo = manager.getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA);
            value = appInfo.metaData.getString(key);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return value;
    }

    public static String getBuildVariant(Context context) {
        try {
            ApplicationInfo info = context.getApplicationInfo();
            return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0 ? "debug" : "release";
        } catch (Exception e) {
            return "release";
        }
    }

    static class Installation {
        private static String sID = null;
        private static final String CHILD_INSTALLATION = "INSTALLATION";

        private Installation() {
        }

        public synchronized static String id(Context context) {
            if (sID == null) {
                File installation = new File(context.getFilesDir(), CHILD_INSTALLATION);
                try {
                    if (!installation.exists()) {
                        writeInstallationFile(installation);
                    }
                    sID = readInstallationFile(installation);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            }
            return sID;
        }

        private static String readInstallationFile(File installation) {
            byte[] bytes = null;
            try (RandomAccessFile f = new RandomAccessFile(installation, "r")) {
                bytes = new byte[(int) f.length()];
                f.readFully(bytes);
            } catch (Exception e) {
                Log.e(TAG, "readInstallationFile: " + e.toString());
            }
            if (bytes != null && bytes.length != 0) {
                return new String(bytes);
            } else {
                return "";
            }
        }

        private static void writeInstallationFile(File installation) {
            try (FileOutputStream out = new FileOutputStream(installation)) {
                String id = UUID.randomUUID().toString();
                out.write(id.getBytes());
            } catch (Exception e) {
                Log.e(TAG, "writeInstallationFile: " + e.toString());
            }
        }
    }

    private static final String HMAC_SHA1 = "HmacSHA1";

    /**
     * 生成签名数据
     *
     * @param data 待加密的数据
     * @param key  加密使用的key
     * @return 加密后的签名
     */
    public static String getSignature(String data, String key) {
        if (data == null || key == null)
            return "";
        if ("".equals(data) || "".equals(key))
            return "";

        byte[] keyBytes = key.getBytes();
        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, HMAC_SHA1);
        Mac mac = null;
        try {
            mac = Mac.getInstance(HMAC_SHA1);
            mac.init(signingKey);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        if (mac == null) {
            return "";
        }
        byte[] rawHmac = mac.doFinal(data.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : rawHmac) {
            sb.append(byteToHexString(b));
        }
        return sb.toString();
    }

    private static String byteToHexString(byte ib) {
        char[] Digit = {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
        };
        char[] ob = new char[2];
        ob[0] = Digit[(ib >>> 4) & 0X0f];
        ob[1] = Digit[ib & 0X0F];
        String s = new String(ob);
        return s;
    }


    private static String getTrailRecordPath(Context context, String profilePath) {
        String trailRecordPath = "";
        if (!TextUtils.isEmpty(profilePath)) {
            trailRecordPath = profilePath + File.separator + ".record";
        } else {
            trailRecordPath = context.getFilesDir() + File.separator + ".record";
        }
        return trailRecordPath;
    }


    /**
     * {
     * "trail":[
     * {
     * "scope":"wakeup",
     * "usedTimes":52
     * },
     * {
     * "scope":"tts",
     * "usedTimes":20
     * }
     * ]
     * }
     * 获取本地加密文件中的模块已经试用次数
     *
     * @return
     */
    public synchronized static int getUsedTimes(Context context, String scopeType, String profilePath) {
        int usedTimes = 0;
        String trialStr = decode(FileIOUtils.readFile2String(getTrailRecordPath(context, profilePath)));
        if (!TextUtils.isEmpty(trialStr)) {
            try {
                JSONObject trailJO = new JSONObject(trialStr);
                JSONArray scopeInfo = trailJO.getJSONArray(TRAIL);
                for (int i = 0; i < scopeInfo.length(); i++) {
                    JSONObject info = scopeInfo.getJSONObject(i);
                    if (TextUtils.equals(info.optString(SCOPE), scopeType)) {
                        usedTimes = info.optInt(AuthUtil.USEDTIMES);//含有该字段，则返回
                    }
                }
                //如果文件中不含有该scope字段，则认为还没有使用过，返回次数为0
            } catch (JSONException e) {
                e.printStackTrace();
                usedTimes = 0;//解析失败
            }
        } else {
            Log.d(TAG, "record file not exist");
        }
        return usedTimes;
    }


    public synchronized static void updateUsedTimes(Context context, String profilePath, String scopeType) {
        //首先获取文件中已有的次数，然后次数++
        //if files not exist, create it
        File file = new File(getTrailRecordPath(context, profilePath));
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            // 不存在则写入字段
            try {
                JSONObject jsonObject = new JSONObject("{\"trail\":[]}");
                FileIOUtils.writeFileFromString(getTrailRecordPath(context, profilePath), encode(jsonObject.toString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        String content = decode(FileIOUtils.readFile2String(getTrailRecordPath(context, profilePath)));
        try {
            JSONObject jsonObject = new JSONObject(content);
            JSONArray scopeInfo = jsonObject.getJSONArray(TRAIL);
            int usedTimes = getUsedTimes(context, scopeType, profilePath);
            if (usedTimes == 0) {
                Log.d(TAG, "no this scope info or file not exist");
                JSONObject inputJO = new JSONObject();
                inputJO.put(SCOPE, scopeType);
                inputJO.put(AuthUtil.USEDTIMES, ++usedTimes);
                scopeInfo.put(inputJO);
            } else {
                for (int i = 0; i < scopeInfo.length(); i++) {
                    JSONObject info = scopeInfo.getJSONObject(i);
                    if (TextUtils.equals(info.optString(SCOPE), scopeType)) {
                        info.put(AuthUtil.USEDTIMES, ++usedTimes);
                    }
                }
            }
            jsonObject.put(TRAIL, scopeInfo);
            FileIOUtils.writeFileFromString(getTrailRecordPath(context, profilePath), encode(jsonObject.toString()));
            if (BuildConfig.TRAIL_AUTH_USED_TIMES_LOG) {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                String recordLogContent = String.format("%s  %-9s %d%n", simpleDateFormat.format(new Date()), scopeType, usedTimes);
                String recordLogFilePath = getTrailRecordPath(context, profilePath) + ".log";
                FileIOUtils.writeFileFromString(recordLogFilePath, recordLogContent, true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * 加密
     *
     * @param data
     * @return
     */
    private static String encode(String data) {
        if (TextUtils.isEmpty(data)) {
            return null;
        }
        byte[] b = data.getBytes();
        //遍历
        for (int i = 0; i < b.length; i++) {
            b[i] += 2;//在原有的基础上+111
        }
        return new String(b);
    }

    /**
     * 解密
     *
     * @param data
     * @return
     */
    private static String decode(String data) {
        //把字符串转为字节数组
        if (TextUtils.isEmpty(data)) {
            return null;
        }
        byte[] b = data.getBytes();
        //遍历
        for (int i = 0; i < b.length; i++) {
            b[i] -= 2;//在原有的基础上-111
        }
        return new String(b);
    }


}
