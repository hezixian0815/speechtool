package com.aispeech.export.config;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import com.aispeech.auth.AIAuthEngine;
import com.aispeech.common.DeviceUtil;
import com.aispeech.lite.AISpeech;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.lang.reflect.Array;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 就近唤醒的配置，包含 net 和 mds 的配置
 */
public class NearWakeupConfig {
    /***************** net config *****************/

    private boolean remoteJudge = true;

    private int wkpWnd = 350;

    private int debounceWnd = 450;

    // 以下设置需要 remoteJudge 为 true 才会有效
    private boolean negSelected = false;

    private String ip;

    private String mac;

    private String serverName = "";

    private double weight = 1;

    private int wsPort = 35791;

    /**
     * 设置就近唤醒的日志级别
     */
    private int logLevel = 1;

    /**
     * 就近唤醒的唤醒词组
     */
    private String wakeupWord;


    /**
     * 中间音频、原始音频的存放路径
     */
    private String debugPath;
    /**
     * 额外参数，目前有 waitDoa
     */
    private Map<String, Object> extraParam = null;

    /**
     * 就近唤醒是否采用带aec的原始音
     */
    private boolean audioAdoptAEC = false;

    /**
     *  可选，设置保存音频文件最大个数，默认是1000个，不可设置为0
     */
    private int saveAudioMaxNumber;


    public int getSaveAudioMaxNumber() {
        return saveAudioMaxNumber;
    }

    /**
     *  可选，设置保存音频文件最大个数，默认是1000个，不可设置为0
     */
    public void setSaveAudioMaxNumber(int saveAudioMaxNumber) {
        this.saveAudioMaxNumber = saveAudioMaxNumber;
    }



    public boolean getRemoteJudge() {
        return remoteJudge;
    }

    /**
     * （慎改）是否开启自组网服务（就近唤醒开关），默认值为 true
     *
     * <ul>
     *     <li>为 true 时设备之间会自动组网，并且自动选择，唤醒时自动决策出最优唤醒设备</li>
     *     <li>为false时唤醒结果会抛出，（fespx_cb输出）同时在结果中新增一个"snr"值</li>
     * </ul>
     *
     * @param remoteJudge 是否开启自组网服务
     */
    public void setRemoteJudge(boolean remoteJudge) {
        this.remoteJudge = remoteJudge;
    }

    public boolean getNegSelected() {
        return negSelected;
    }

    /**
     * 控制在决策失败时（即当前设备没有最优的snr值）是否抛出唤醒结果，默认值为false（remoteJudge开启时有效）
     * <ul>
     *     <li>为false时，不抛出</li>
     *     <li>为true时抛出，（fespx_cb输出）如果唤醒成功在结果中包含 {"nwtkType", "neglected" }，唤醒失败时包含 { "nwtkType", "selected" }</li>
     * </ul>
     *
     * @param negSelected 就近唤醒决策失败后是否通知上层
     */
    public void setNegSelected(boolean negSelected) {
        this.negSelected = negSelected;
    }

    public String getIp() {
        return ip;
    }


    /**
     * 设备ip地址（必须是真实联网IP地址，127.0.0.1不参与组网）
     *
     * @param ip 设备ip地址 示例: "127.0.0.1"
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getMac() {
        return mac;
    }

    /**
     * 设备mac地址
     *
     * @param mac mac地址 示例："BE-91-80-2F-66-61"
     */
    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getServerName() {
        return serverName;
    }

    /**
     * 服务点名称，相同的服务名才能相互发现和连接。用于不同设备间的隔离，服务名
     * 不同无法组网交换信息。默认 "" ,就是普通的字符串。
     *
     * @param serverName 服务点名称
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public double getWeight() {
        return weight;
    }

    /**
     * （慎改）选举权重，默认值1.0。取值范围大于等于0，值越大选举时优势越大
     *
     * @param weight 选举权重
     */
    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getWsPort() {
        return wsPort;
    }

    /**
     * （慎改）服务绑定的端口号
     *
     * @param wsPort 端口号
     */
    public void setWsPort(int wsPort) {
        this.wsPort = wsPort;
    }

    public int getWkpWnd() {
        return wkpWnd;
    }

    /**
     * 单次决策窗口期，单位毫秒，范围50-800，默认350ms。不同设备唤醒时间点有差异，且网络传输延
     * 时导致一次唤醒存在时间差，在窗口期范围之内，同一唤醒词的唤醒被合并为同一个唤醒，从中选出
     * 最优设备
     *
     * @param wkpWnd 单次决策窗口期
     */
    public void setWkpWnd(int wkpWnd) {
        this.wkpWnd = wkpWnd;
    }

    public int getDebounceWnd() {
        return debounceWnd;
    }

    /**
     * 防抖窗口期，单位毫秒，范围50-1000，默认450ms。唤醒窗口期结束之后开始进入防抖窗口期，一次唤醒之
     * 后，有可能还有部分设备不落在唤醒窗口期，在防抖窗口期内该设备也不会被唤醒。
     *
     * @param debounceWnd 防抖窗口期
     */
    public void setDebounceWnd(int debounceWnd) {
        this.debounceWnd = debounceWnd;
    }

    private static String intToIp(int ipInt) {
        StringBuilder sb = new StringBuilder();
        sb.append(ipInt & 0xFF).append(".");
        sb.append((ipInt >> 8) & 0xFF).append(".");
        sb.append((ipInt >> 16) & 0xFF).append(".");
        sb.append((ipInt >> 24) & 0xFF);
        return sb.toString();
    }

    public String getDebugPath() {
        return debugPath;
    }

    /**
     * 中间音频、原始音频的存放路径
     *
     * @param debugPath 音频的存放路径
     */
    public void setDebugPath(String debugPath) {
        this.debugPath = debugPath;
    }

    public Map<String, Object> getExtraParam() {
        return extraParam;
    }

    /**
     * 设置额外的参数, key:value 的形式。
     * 例如:["abc":"ABC","num":123,"bb":false,"list":["a","1","c"]]
     *
     * @param key   key，例如："abc" "num" "list"
     * @param value value，例如："ABC" 123 false ["a","1","c"]
     */
    public synchronized void putExtraParam(String key, Object value) {
        if (extraParam == null)
            extraParam = new HashMap<>();
        extraParam.put(key, value);
    }

    public int getLogLevel() {
        return logLevel;
    }

    /**
     * 设置就近唤醒的日志级别
     * 用来设置 decision 模块日志级别, 对应的数值越小可打印日志级别越多；
     * 反之至越少。数值 5 为不打印日志；默认数值是 1，打印大于等于 1 级别的日志。
     *
     * @param logLevel 0-5
     */
    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }


    public String getWakeupWord() {
        return wakeupWord;
    }

    /**
     * <pre>{@code
     *   config.setWakeupWord("ni hao xiao yi, ni hao xiao chi")
     * }</pre>
     *
     * @param wakeupWord
     */
    public void setWakeupWord(String wakeupWord) {
        this.wakeupWord = wakeupWord;
    }

    /**
     * 网络设置，默认配置如下
     * <pre>
     * {
     * 	"remoteJudge": 1,
     * 	"negSelected": 0,
     * 	"interface": "eth0",    //
     * 	"ip": "127.0.0.1",
     * 	"mac": "BE-91-80-2F-66-61",
     * 	"server_name": "nearwakeup:1",
     * 	"type": "ethernet",     //
     * 	"weight": 1,
     * 	"ws_port": 35791,
     * 	"wkpWnd": 350,
     * 	"debounceWnd": 450
     * }
     * </pre>
     *
     * @return 网络设置
     */
    public JSONObject toNetJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("remoteJudge", remoteJudge ? 1 : 0);
            jsonObject.put("negSelected", negSelected ? 1 : 0);

            jsonObject.put("serverName", serverName);
            jsonObject.put("weight", weight);
            jsonObject.put("wsPort", wsPort);
            jsonObject.put("wkpWnd", wkpWnd);
            jsonObject.put("debounceWnd", debounceWnd);
            jsonObject.put("logLevel", logLevel);
            jsonObject.put("wakeupWord", wakeupWord);
            if (saveAudioMaxNumber > 0) {
                jsonObject.put("saveAudioMaxNumber",saveAudioMaxNumber);
            }
            if (TextUtils.isEmpty(ip)) {
                String finalIp = getIPAddress(AISpeech.getContext().getApplicationContext());
                if (TextUtils.isEmpty(finalIp)) {
                    finalIp = "127.0.0.1";
                }
                jsonObject.put("ip", finalIp);
            } else
                jsonObject.put("ip", ip);

            if (TextUtils.isEmpty(mac)) {
                String innerMac = DeviceUtil.getMacAddress(AISpeech.getContext());
                if (TextUtils.isEmpty(innerMac)) {
                    // 没有 mac 就传设备的 deviceName,用于唯一标识
                    try {
                        jsonObject.put("mac", AIAuthEngine.getInstance().getProfile().getDeviceName());
                    } catch (Exception e) {
                    }
                } else {
                    jsonObject.put("mac", innerMac);
                }
            } else
                jsonObject.put("mac", mac);

            jsonObject.put("debugPath", debugPath);
            jsonObjectPutExtraParam(jsonObject, extraParam);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private static void jsonObjectPutExtraParam(JSONObject jsonObject, Map<String, Object> extraParam) {
        if (jsonObject != null && extraParam != null && !extraParam.isEmpty()) {
            Iterator<String> iter = extraParam.keySet().iterator();
            while (iter.hasNext()) {
                String key = iter.next();
                Object valueObject = extraParam.get(key);
                try {
                    if (valueObject == null) {
                        continue;
                    } else if (valueObject instanceof Iterable) {
                        JSONArray jsonArray = new JSONArray();
                        for (Object o : (Iterable) valueObject) {
                            jsonArray.put(o);
                        }
                        jsonObject.put(key, jsonArray);
                    } else if (valueObject.getClass().isArray()) {
                        JSONArray jsonArray = new JSONArray();
                        int length = Array.getLength(valueObject);
                        for (int i = 0; i < length; i++) {
                            jsonArray.put(Array.get(valueObject, i));
                        }
                        jsonObject.put(key, jsonArray);
                    } else
                        jsonObject.put(key, valueObject);
                } catch (Exception e) {
                }
            }
        }
    }

    /***************** mds config *****************/
    /**
     * mds 资源
     */
    private String mdsResource;
    /**
     * 输入音频通道数
     */
    private int channels;

    /**
     * mds 配置
     * <pre>
     * {
     *  "resBinPath": "./wakeup/md.bin",
     * 	"channels": 2
     * }
     * </pre>
     *
     * @return mds 配置
     */
    public JSONObject toMdsJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("resBinPath", mdsResource);
            jsonObject.put("channels", channels);
            jsonObject.put("audioAdoptAEC", audioAdoptAEC ? 1 : 0);
            jsonObject.put("libraryPath", "libmds.so");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public String getMdsResource() {
        return mdsResource;
    }

    /**
     * mds 资源
     * <p>1. 如在 sd 里设置为绝对路径 如/sdcard/speech/***.bin</p>
     * <p>2. 如在 assets 里设置为名称</p>
     *
     * @param mdsResource mds 资源
     */
    public void setMdsResource(String mdsResource) {
        this.mdsResource = mdsResource;
    }

    public int getChannels() {
        return channels;
    }

    /**
     * 输入音频通道数
     *
     * @param channels 通道数
     */
    public void setChannels(int channels) {
        this.channels = channels;
    }

    public boolean isAudioAdoptAEC() {
        return audioAdoptAEC;
    }

    /**
     * 就近唤醒是否采用带aec的音频
     *
     * @param audioAdoptAEC 采用带aec的原始音，默认为false
     */
    public void setAudioAdoptAEC(boolean audioAdoptAEC) {
        this.audioAdoptAEC = audioAdoptAEC;
    }

    public static String getIPAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo.getIpAddress() > 0) {
                    return intToIp(wifiInfo.getIpAddress());//得到IPV4地址
                }
            } else if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }
            }
        }
        return getLocalIpLinux();
    }

    private static String getLocalIpLinux() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("ip route show ");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String ip = "";
        InputStreamReader r = new InputStreamReader(process.getInputStream());
        LineNumberReader returnData = new LineNumberReader(r);
        String line = "";
        try {
            while ((line = returnData.readLine()) != null) {
                String[] string = line.split(" src ");
                ip = string[string.length - 1].trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                process.destroy();
                returnData.close();
                r.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!isIpAddress(ip)) {
            ip = "";
        }
        return ip;
    }

    /**
     * 校验字符串是否是合法IP
     *
     * @param address 传入的校验字段
     * @return
     */
    public static boolean isIpAddress(String address) {
        String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";

        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(address);

        return m.matches();
    }
}
