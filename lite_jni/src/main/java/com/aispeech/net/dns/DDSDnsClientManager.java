package com.aispeech.net.dns;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.jnihelper.DDSDnsClientJniHelper;

import java.util.concurrent.ConcurrentHashMap;


/**
 * @author yu
 * @date 2018/8/13
 */

public class DDSDnsClientManager {
    private static final String TAG = "DDSDnsClientManager";
    private static final String HOSTNAME = "hostName : ";
    private static ConcurrentHashMap<String, DnsCache> dnsCacheMap = new ConcurrentHashMap<>();

    private DDSDnsClientManager() {
    }

    public static String getHostByName(String hostName) {
        String ip = null;
        if (dnsCacheMap.containsKey(hostName)) {
            DnsCache dnsCache = dnsCacheMap.get(hostName);
            long createTime = dnsCache.getCreateTime();
            long currentTime = System.currentTimeMillis();
            long offsetTime = currentTime - createTime;
            //update dns ip per 10min
            if (offsetTime >= 0 && offsetTime <= 10 * 60 * 1000) {
                ip = dnsCache.getIp();
                if (!TextUtils.isEmpty(ip)) {
                    Log.d(TAG, HOSTNAME + hostName + " query cache ip : " + ip);
                    return ip;
                }
            } else {//10分钟过期
                Log.d(TAG, HOSTNAME + hostName + " is expird, remove it ");
                dnsCacheMap.remove(hostName);
            }
        } else {
            Log.d(TAG, "no cache  : " + hostName);
        }

        Log.d(TAG, "dds_get_host_by_name start : " + hostName);
        ip = DDSDnsClientJniHelper.dds_get_host_by_name(hostName, 2, 3);
        Log.d(TAG, "dds_get_host_by_name return  : " + ip);
        if (!TextUtils.isEmpty(ip)) {
            Log.d(TAG, HOSTNAME + hostName + " ip : " + ip + " store it to cache");
            dnsCacheMap.put(hostName, new DnsCache(ip, System.currentTimeMillis()));
        }
        return ip;
    }

}
