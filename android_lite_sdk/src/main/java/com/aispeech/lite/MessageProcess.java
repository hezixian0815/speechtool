package com.aispeech.lite;

import android.os.Message;
import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.common.ThreadNameUtil;
import com.aispeech.lite.message.MessageQueue;
import com.aispeech.util.Utils;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MessageProcess implements Runnable {

    public interface Handle {
        String getHandleName();

        void handleMessage(BaseProcessor.EngineMsg engineMsg, Message msg);
    }

    private static final MessageProcess instance = new MessageProcess();

    public static MessageProcess getInstance() {
        return instance;
    }

    /**
     * 对于一些耗时的消息 可以使用单独的processor进行处理
     *
     * @return
     */
    public static MessageProcess newInstance(String tag) {
        MessageProcess messageProcess = new MessageProcess();
        messageProcess.customTag = tag;
        return messageProcess;
    }

    private final Map<Message, Handle> HandleMap = new ConcurrentHashMap<>();
    private final MessageQueue<Message> queue = new MessageQueue();
    private static final String TAG = "MessageProcess";
    private String customTag = "";
    private ExecutorService threadPool = null;
    private Set<Handle> handles = new HashSet<>();
    /**
     * 0 没有运行线程池 1 运行线程池 2 正在停止线程池
     */
    private AtomicInteger status = new AtomicInteger(STATUS_STOPPED);
    private static final int STATUS_STOPPED = 0;
    private static final int STATUS_STARTED = 1;
    private static final int STATUS_STOPPING = 2;

    private MessageProcess() {
        queue.setTAG(TAG);
    }

    @Override
    public void run() {
        Log.v(TAG, "MessageProcess run START");
        Utils.checkThreadAffinity();
        Message msg;
        long timestamp;
        while (status.get() == STATUS_STARTED && (msg = queue.get()) != null) {
            // Log.v(TAG, "run() Thread Name: " + Thread.currentThread().getName());
//            Log.d(TAG, "dealing msg: " + msg.what);
            timestamp = System.currentTimeMillis();
            if (queue.size() > 10)
                Log.v(TAG, "queue.size() " + queue.size());
            BaseProcessor.EngineMsg engineMsg = BaseProcessor.EngineMsg.getMsgByValue(msg.what);
            Handle handle = HandleMap.remove(msg);
            if (handle != null) {
                handle.handleMessage(engineMsg, msg);
                // 耗时大于 10ms 的打印一下
                long cost = System.currentTimeMillis() - timestamp;
                if (cost > 2)
                    Log.v(TAG, "cost:" + cost + " engineMsg: " + engineMsg + " HandleName: " + handle.getHandleName() + " handle: " + handle);
            }
//            Log.d(TAG, "dealing msg: " + msg.what + " done");
        }
        status.set(STATUS_STOPPED);
        Log.v(TAG, "MessageProcess run END");
    }

    public synchronized void registerHandle(Handle handle) {
        while (status.get() == STATUS_STOPPING) {
            // 防止上一个线程池还未完全结束
            Log.d(TAG, "threadPool is not null");
            try {
                Thread.sleep(2);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        handles.add(handle);
        if (status.get() == STATUS_STOPPED) {
            status.set(STATUS_STARTED);
            Log.v(TAG, "threadPool execute");
            String realTag = TextUtils.isEmpty(customTag) ? TAG : customTag;
            threadPool = Executors.newSingleThreadExecutor(new DaemonThreadFactory(realTag, Thread.NORM_PRIORITY));
            threadPool.execute(this);
        }
    }

    public synchronized void unregisterHandle(Handle handle) {
        handles.remove(handle);
        clearMessage(handle);
        if (handles.isEmpty() && status.get() == STATUS_STARTED) {
            if (threadPool != null && !threadPool.isShutdown()) {
                Log.v(TAG, "threadPool shutdown");
                threadPool.shutdown();
                threadPool = null;
            }
            status.set(STATUS_STOPPING);
        }
    }

    private void clearMessage(Handle handle) {
        if (handle == null)
            return;
        // Log.v(TAG, "clearMessage() Thread Name: " + Thread.currentThread().getName());
        Iterator<Map.Entry<Message, Handle>> it = HandleMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Message, Handle> entry = it.next();
            if (handle.equals(entry.getValue())) {
                it.remove();
                queue.remove(entry.getKey());
            }
        }
    }

    public void sendMessage(Handle handle, Message message) {
//        if (BaseProcessor.EngineMsg.getMsgByValue(message.what) == BaseProcessor.EngineMsg.MSG_START)
//            Log.v(TAG, "sendMessage() Thread Name: " + Thread.currentThread().getName() + " handle: " + handle);
//        Log.d(TAG, "sendMessage,current size : " + queue.size());
//        Log.d(TAG, "sendMessage,current size : " + " handle: " + handle.getHandleName() +  " meessage:" + message.what);
        HandleMap.put(message, handle);
        queue.put(message);
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        private String mThreadName;
        private int mThreadPriority;

        public DaemonThreadFactory(String threadTag, int threadPriority) {
            this.mThreadName = ThreadNameUtil.getSimpleThreadName(threadTag);
            this.mThreadPriority = threadPriority;
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, mThreadName);
            t.setDaemon(true);
            t.setPriority(mThreadPriority);
            return t;
        }
    }
}
