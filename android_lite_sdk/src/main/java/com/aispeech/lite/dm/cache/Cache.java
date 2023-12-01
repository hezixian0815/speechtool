package com.aispeech.lite.dm.cache;

import com.aispeech.export.Command;

import java.util.LinkedList;
import java.util.Queue;


/**
 * cache 任务
 *
 * @author hehr
 */
public class Cache {

    private Queue<Item> mQueue = new LinkedList();

    /**
     * 清空队列
     */
    public void clear() {
        if (mQueue != null) {
            mQueue.clear();
        }
    }

    public boolean isEmpty() {
        if (mQueue != null) {
            return mQueue.isEmpty();
        }
        return true;
    }

    /**
     * key of command
     */
    public static final String KEY_COMMAND = "pending_command";
    /**
     * key of should listen
     */
    public static final String KEY_SHOULD_LISTEN = "pending_should_listen";
    /**
     * key of shouldEndSession
     */
    public static final String KEY_SHOULD_END_SESSION = "pending_should_end_session";

    /**
     * cache command
     *
     * @param command {@link Command}
     */
    public void setCommand(Command command) {
        if (mQueue != null) {
            mQueue.offer(new Item(KEY_COMMAND, command));
        }
    }


    /**
     * cache shouldListen
     *
     * @param shouldListen boolean
     */
    public void setShouldListen(boolean shouldListen) {
        if (mQueue != null) {
            mQueue.offer(new Item(KEY_SHOULD_LISTEN, shouldListen));
        }
    }

    /**
     * cache shouldEndSession
     *
     * @param shouldEndSession boolean
     */
    public void setShouldEndSession(boolean shouldEndSession) {
        if (mQueue != null) {
            mQueue.offer(new Item(KEY_SHOULD_END_SESSION, shouldEndSession));
        }
    }

    public TaskIterator iterator() {
        return new TaskIterator();
    }


    /**
     * Task 迭代器
     */
    public class TaskIterator implements Iterator {

        @Override
        public boolean hasNext() {
            return !mQueue.isEmpty();
        }

        @Override
        public Item next() {
            return mQueue.poll();
        }

    }

}
