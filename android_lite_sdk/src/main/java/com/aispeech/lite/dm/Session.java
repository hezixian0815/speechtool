package com.aispeech.lite.dm;

import android.text.TextUtils;

import com.aispeech.common.Log;
import com.aispeech.common.Util;

/**
 * session
 *
 * @author hehr
 */
public class Session {

    private static final String TAG = "Session";

    /**
     * sessionId
     */
    private String id;

    private void setId(String id) {
        this.id = id;
    }

    public String getId() {
        if(TextUtils.isEmpty(id)){
            id = Util.uuid();
        }
        return id;
    }

    /**
     * 清空对话id
     */
    public void clearId() {
        Log.d(TAG, "clear session id");
        id = "";
    }

    /**
     * 同步sessionId
     *
     * @param currentId String
     */
    public void syncId(String currentId) {
        if (!TextUtils.equals(id, currentId)) {
            Log.d(TAG, "sync session id");
            setId(currentId);
        }
    }
}
