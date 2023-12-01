package com.aispeech.common;

/*
 * Copyright (c) 2022  aispeech
 * @LastModified:2022-01-14 14:26:52
 */

import android.os.SystemClock;

public class TimeFilter {
    private static volatile long lastActionTime = 0L; //上一次执行的时间

    public static boolean filter(Long minTime) {

        long time = SystemClock.uptimeMillis();
        if (lastActionTime == 0L) {
            lastActionTime = time;
            return false;
        }

        if ((time - lastActionTime) > minTime) {
            lastActionTime = time;
            return false;
        }

        return true;

    }
}