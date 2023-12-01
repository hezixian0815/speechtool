package com.aispeech.lite.dm;

import com.aispeech.common.AITimer;
import com.aispeech.common.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TimerTask;

/**
 * timer
 *
 * @author hehr
 */
public class Timer {

    private static final String TAG = "Timer";

    private Map<String, TimerTask> taskMap;

    public Timer() {
        taskMap = new HashMap<>(16);
    }

    public Timer(int timeout) {
        this();
        this.timeout = timeout;
    }

    /**
     * 默认timeout 10s
     */
    private int timeout = 10 * 1000;

    /**
     * 设置计时器超时时间
     *
     * @param timeout 单位毫秒
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * 启动定时器任务
     *
     * @param aliasOfTask String
     * @param task        task
     */
    public void start(String aliasOfTask, TimerTask task) {
        if (taskMap != null) {
            cancel(aliasOfTask);
            taskMap.put(aliasOfTask, task);
            Log.d(TAG, "start task alias:" + aliasOfTask);
            AITimer.getInstance().schedule(task, timeout);

        }
    }

    /**
     * 启动定时器任务
     *
     * @param aliasOfTask String
     * @param task        task
     */
    public void start(String aliasOfTask, TimerTask task, int period) {
        if (taskMap != null) {
            cancel(aliasOfTask);
            taskMap.put(aliasOfTask, task);
            Log.d(TAG, "start task alias:" + aliasOfTask);
            AITimer.getInstance().schedule(task, timeout, period);
        }
    }

    /**
     * 取消计时器
     *
     * @param aliasOfTask string
     */
    public void cancel(String aliasOfTask) {
        TimerTask timerTask = taskMap.get(aliasOfTask);
        if (timerTask != null) {
            Log.d(TAG, "cancel task alias:" + aliasOfTask);
            timerTask.cancel();
            taskMap.remove(aliasOfTask);
        }
    }

    /**
     * 清空task map
     */
    public void clear() {
        if (taskMap != null && !taskMap.isEmpty()) {
            Iterator<Map.Entry<String, TimerTask>> iterator = taskMap.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, TimerTask> entry = iterator.next();
                cancel(entry.getKey());
            }
        }
    }


}
