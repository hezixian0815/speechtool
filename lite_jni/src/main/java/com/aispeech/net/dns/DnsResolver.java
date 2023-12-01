package com.aispeech.net.dns;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.common.URLUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import okhttp3.Dns;

/**
 *
 * @author yu
 * @date 2018/8/13
 */

public class DnsResolver implements Dns {
    private static final String TAG = "DnsResolver";
    @Override
    public List<InetAddress> lookup(String hostname) throws UnknownHostException {
        Log.i(TAG, "hostname : " + hostname);
        String ip;
        if (URLUtils.isIP(hostname)) {
            ip = hostname;
            Log.d(TAG,  hostname + " is ip, ignore dns resolve");
        } else {
            ip = DDSDnsClientManager.getHostByName(hostname);
        }
        Log.i(TAG, "ip " + ip);
        if(TextUtils.isEmpty(ip)) {
            throw new UnknownHostException("dns resolve failed");
        }
        InetAddress[] addresses = InetAddress.getAllByName(ip);
        for(InetAddress address: addresses) {
            Log.i(TAG, "address : " + address.getHostAddress());
        }
        return Arrays.asList(addresses);
    }
}
