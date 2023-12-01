package com.aispeech.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AITimer extends Timer {
    private static volatile AITimer mTimer;
    private static Map<String, TimerTask> mTaskMap = new HashMap<>();

    private AITimer(String name) {
        super(name);
    }

    public static Timer getInstance() {
        if (mTimer == null) {
            synchronized (AITimer.class) {
                if (mTimer == null)
                    mTimer = new AITimer(ThreadNameUtil.getSimpleThreadName("AITimer"));
            }
        }
        return mTimer;
    }

    public void startTimer(TimerTask task, String taskName, int millisec) {
        TimerTask timerTask = mTaskMap.get(taskName);
        if (timerTask != null) {
            timerTask.cancel();
            mTaskMap.remove(taskName);
        }
        mTaskMap.put(taskName, task);
        try {
            mTimer.schedule(task, millisec);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void cancelTimer(String taskName) {
        TimerTask timerTask = mTaskMap.get(taskName);
        if (timerTask != null) {
            timerTask.cancel();
            mTaskMap.remove(taskName);
        }
    }
}

